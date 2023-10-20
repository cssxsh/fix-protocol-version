package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.utils.*
import org.asynchttpclient.*
import java.time.Duration
import kotlin.coroutines.*

public class Shamrock(private val server: String, coroutineContext: CoroutineContext) : EncryptService, CoroutineScope {

    override val coroutineContext: CoroutineContext =
        coroutineContext + SupervisorJob(coroutineContext[Job]) + CoroutineExceptionHandler { context, exception ->
            when (exception) {
                is CancellationException, is InterruptedException -> {
                    // ignored
                }
                is KFCStateException -> {
                    // ignored
                }
                else -> {
                    logger.warning({ "with ${context[CoroutineName]}" }, exception)
                }
            }
        }

    private val client = Dsl.asyncHttpClient(
        DefaultAsyncHttpClientConfig.Builder()
            .setKeepAlive(true)
            .setUserAgent("curl/7.61.0")
            .setRequestTimeout(Duration.ofSeconds(90))
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(180))
    )

    private fun ShamrockData.check() {
        if (code == 0) return
        throw KFCStateException("Shamrock 服务异常, 请检查其日志, '$status'")
    }

    private val token = java.util.concurrent.atomic.AtomicLong(0)

    private val whitelist = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    override fun initialize(context: EncryptServiceContext) {

        logger.info("Bot(${context.id}) initialize by $server")

        val service = checkNotNull(services().find { it.uid == context.id }) { "No Match ${context.id}" }
        check(token.compareAndSet(0, service.uid)) { "???" }
        whitelist.addAll(whitelist())
        coroutineContext.job.invokeOnCompletion {
            whitelist.clear()
            token.compareAndSet(context.id, 0)
        }

        logger.info("Bot(${context.id}) initialize complete")
    }

    private fun services(): List<ShamrockService> {
        val response = client.prepareGet("${server}/get_running_service")
            .execute().get()

        return Json.decodeFromString(ListSerializer(ShamrockService.serializer()), response.responseBody)
    }

    private fun whitelist(): List<String> {
        val response = client.prepareGet("${server}/get_cmd_whitelist")
            .execute().get()
        val body = Json.decodeFromString(ShamrockData.serializer(), response.responseBody)
        body.check()

        return Json.decodeFromJsonElement(ListSerializer(String.serializer()), body.data)
    }

    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        val data = energy(salt = payload, data = command)

        return data.hexToBytes()
    }

    private fun energy(salt: ByteArray, data: String): String {
        val response = client.prepareGet("${server}/custom_energy")
            .addQueryParam("salt", salt.toUHexString(""))
            .addQueryParam("data", data)
            .execute().get()
        val body = Json.decodeFromString(ShamrockData.serializer(), response.responseBody)
        body.check()

        logger.debug("Bot(${token.get()}) energy ${data}, ${body.status}")

        return Json.decodeFromJsonElement(String.serializer(), body.data)
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        if (whitelist.isEmpty().not() && commandName !in whitelist) return null

        val data = sign(cmd = commandName, seq = sequenceId, buffer = payload)

        return EncryptService.SignResult(
            sign = data.sign.hexToBytes(),
            token = data.token.hexToBytes(),
            extra = data.extra.hexToBytes()
        )
    }

    private fun sign(cmd: String, seq: Int, buffer: ByteArray): ShamrockSignResult {
        val response = client.preparePost("${server}/sign")
            .addFormParam("uin", token.get().toString())
            .addFormParam("cmd", cmd)
            .addFormParam("seq", seq.toString())
            .addFormParam("buffer", buffer.toUHexString(""))
            .execute().get()
        val body = Json.decodeFromString(ShamrockData.serializer(), response.responseBody)
        body.check()

        logger.debug("Bot(${token.get()}) sign ${cmd}, ${body.status}")

        return Json.decodeFromJsonElement(ShamrockSignResult.serializer(), body.data)
    }

    public companion object {

        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(Shamrock::class)

    }
}

@Serializable
private data class ShamrockData(
    @SerialName("status")
    val status: String = "",
    @SerialName("code")
    val code: Int = 0,
    @SerialName("data")
    val data: JsonElement
)

@Serializable
private data class ShamrockService(
    @SerialName("service")
    val service: String = "",
    @SerialName("pid")
    val pid: Long = 0,
    @SerialName("uid")
    val uid: Long = 0
)

@Serializable
private data class ShamrockSignResult(
    @SerialName("token")
    val token: String = "",
    @SerialName("extra")
    val extra: String = "",
    @SerialName("sign")
    val sign: String = "",
    @SerialName("o3did")
    val o3did: String = "",
    @SerialName("requestCallback")
    val requestCallback: List<Int> = emptyList()
)