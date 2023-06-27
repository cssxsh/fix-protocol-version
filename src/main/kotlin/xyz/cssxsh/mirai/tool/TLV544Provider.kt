package xyz.cssxsh.mirai.tool

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import kotlin.coroutines.*

public class TLV544Provider : EncryptService, CoroutineScope {
    internal companion object {
        val SALT_V1 = arrayOf("810_2", "810_7", "810_24", "810_25")
        val SALT_V2 = arrayOf("810_9", "810_a", "810_d", "810_f")
        val SALT_V3 = arrayOf("812_a")

        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(TLV544Provider::class)
    }

    @PublishedApi
    internal val http: HttpClient = HttpClient(OkHttp) {
        BrowserUserAgent()
        ContentEncoding()
        expectSuccess = true
        install(HttpTimeout) {
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = null
        }
        install(ContentNegotiation) {
            json()
        }
    }

    override val coroutineContext: CoroutineContext = CoroutineExceptionHandler { context, exception ->
        when (exception) {
            is CancellationException -> {
                // ...
            }
            else -> {
                logger.warning({ "TLV544Provider with ${context[CoroutineName]}" }, exception)
            }
        }
    }

    @OptIn(MiraiInternalApi::class)
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]
        val protocol = context.extraArgs[EncryptServiceContext.KEY_BOT_PROTOCOL]
        val device = context.extraArgs[EncryptServiceContext.KEY_DEVICE_INFO]

        logger.info("t544 command: $command with $protocol")

        val future = async(CoroutineName("encryptTlv(${context.id})")) {
            val impl = MiraiProtocolInternal.protocols[protocol]!!
            val json = http.get("http://127.0.0.1:8080/energy") {
                parameter("version", impl.sdkVer)
                parameter("uin", context.id)
                parameter("guid", device.guid.toUHexString(""))
                parameter("data", command)
            }.body<JsonObject>()

            val data = checkNotNull(json["data"]?.jsonPrimitive?.content) { json.toString() }

            data.hexToBytes()
        }.asCompletableFuture()
        return future.get()
    }

    override fun initialize(context: EncryptServiceContext) {
        // TODO
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        val qua = context.extraArgs[EncryptServiceContext.KEY_APP_QUA]
        val future = async(CoroutineName("qSecurityGetSign(${context.id})")) {
            val json = http.get("http://127.0.0.1:8080/sign") {
                parameter("uin", context.id)
                parameter("qua", qua)
                parameter("cmd", commandName)
                parameter("seq", sequenceId)
                parameter("buffer", payload.toUHexString(""))
            }.body<JsonObject>()

            val data = checkNotNull(json["data"]?.jsonObject) { json.toString() }

            EncryptService.SignResult(
                sign = data["sign"]!!.jsonPrimitive.content.hexToBytes(),
                token = data["token"]!!.jsonPrimitive.content.hexToBytes(),
                extra = data["extra"]!!.jsonPrimitive.content.hexToBytes()
            )
        }.asCompletableFuture()

        return future.get()
    }
}