package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.utils.*
import org.asynchttpclient.*
import kotlin.coroutines.*

@OptIn(MiraiInternalApi::class)
public class UnidbgFetchQsign(private val server: String, private val key: String, coroutineContext: CoroutineContext)
    : EncryptService, CoroutineScope {

    override val coroutineContext: CoroutineContext =
        coroutineContext + SupervisorJob(coroutineContext[Job]) + CoroutineExceptionHandler { context, exception ->
            when (exception) {
                is CancellationException -> {
                    // ...
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
            .setRequestTimeout(30_000)
            .setConnectTimeout(30_000)
            .setReadTimeout(180_000)
    )

    private var channel0: EncryptService.ChannelProxy? = null

    private val channel: EncryptService.ChannelProxy get() = channel0 ?: throw IllegalStateException("need initialize")

    override fun initialize(context: EncryptServiceContext) {
        val device = context.extraArgs[EncryptServiceContext.KEY_DEVICE_INFO]
        val qimei36 = context.extraArgs[EncryptServiceContext.KEY_QIMEI36]
        val channel = context.extraArgs[EncryptServiceContext.KEY_CHANNEL_PROXY]

        register(uin = context.id, androidId = device.androidId.decodeToString(), guid = device.guid.toUHexString(), qimei36 = qimei36)

        channel0 = channel
        launch(CoroutineName("requestToken")) {
            delay(180_000)
            while (isActive) {
                requestToken(uin = context.id)

                delay((30 .. 40).random() * 60_000L)
            }
        }
    }

    private fun register(uin: Long, androidId: String, guid: String, qimei36: String): String {
        val response = client.prepareGet("${server}/register")
            .addQueryParam("uin", uin.toString())
            .addQueryParam("android_id", androidId)
            .addQueryParam("guid", guid)
            .addQueryParam("qimei36", qimei36)
            .addQueryParam("key", key)
            .execute()
        val body = Json.decodeFromString(DataWrapper.serializer(), response.get().responseBody)
        check(body.code == 0) { body.message }

        return body.message
    }

    private fun requestToken(uin: Long) {
        val response = client.prepareGet("${server}/request_token")
            .addQueryParam("uin", uin.toString())
            .execute()
        val body = Json.decodeFromString(DataWrapper.serializer(), response.get().responseBody)
        check(body.code == 0) { body.message }
    }

    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        val data = customEnergy(uin = context.id, salt = payload, data = command)

        return data.hexToBytes()
    }

    private fun customEnergy(uin: Long, salt: ByteArray, data: String): String {
        val response = client.prepareGet("${server}/custom_energy")
            .addQueryParam("uin", uin.toString())
            .addQueryParam("salt", salt.toUHexString(""))
            .addQueryParam("data", data)
            .execute()
        val body = Json.decodeFromString(DataWrapper.serializer(), response.get().responseBody)
        check(body.code == 0) { body.message }

        return Json.decodeFromJsonElement(String.serializer(), body.data)
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        if (commandName !in CMD_WHITE_LIST) return null

        val data = sign(uin = context.id, cmd = commandName, seq = sequenceId, buffer = payload)

        launch(CoroutineName("RequestCallback")) {
            for (callback in data.requestCallback) {
                val result = channel.sendMessage(
                    remark = "callback.callbackId",
                    commandName = callback.cmd,
                    uin = context.id,
                    data = callback.body.encodeToByteArray()
                )
                if (result == null) {
                    logger.debug("${callback.cmd} ChannelResult is null")
                    continue
                }

                submit(uin = context.id, cmd = result.cmd, callbackId = callback.id, buffer = result.data)
            }
        }

        return EncryptService.SignResult(
            sign = data.sign.hexToBytes(),
            token = data.token.hexToBytes(),
            extra = data.extra.hexToBytes(),
        )
    }

    private fun sign(uin: Long, cmd: String, seq: Int, buffer: ByteArray) : SignResult {
        val response = client.preparePost("${server}/sign")
            .addFormParam("uin", uin.toString())
            .addFormParam("cmd", cmd)
            .addFormParam("seq", seq.toString())
            .addFormParam("buffer", buffer.toUHexString(""))
            .execute()
        val body = Json.decodeFromString(DataWrapper.serializer(), response.get().responseBody)
        check(body.code == 0) { body.message }

        return Json.decodeFromJsonElement(SignResult.serializer(), body.data)
    }

    private fun submit(uin: Long, cmd: String, callbackId: Int, buffer: ByteArray) {
        val response = client.prepareGet("${server}/submit")
            .addQueryParam("uin", uin.toString())
            .addQueryParam("cmd", cmd)
            .addQueryParam("callbackId", callbackId.toString())
            .addQueryParam("buffer", buffer.toUHexString(""))
            .execute()
        val body = Json.decodeFromString(DataWrapper.serializer(), response.get().responseBody)
        check(body.code == 0) { body.message }
    }

    public companion object {
        @JvmStatic
        internal val CMD_WHITE_LIST = UnidbgFetchQsign::class.java.getResource("cmd.txt")!!.readText().lines()

        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(UnidbgFetchQsign::class)
    }
}

@Serializable
private data class DataWrapper(
    @SerialName("code")
    val code: Int = 0,
    @SerialName("msg")
    val message: String = "",
    @SerialName("data")
    val data: JsonElement
)

@Serializable
private data class SignResult(
    @SerialName("token")
    val token: String = "",
    @SerialName("extra")
    val extra: String = "",
    @SerialName("sign")
    val sign: String = "",
    @SerialName("o3did")
    val o3did: String = "",
    @SerialName("requestCallback")
    val requestCallback: List<RequestCallback> = emptyList()
)

@Serializable
private data class RequestCallback(
    @SerialName("body")
    val body: String = "",
    @SerialName("callbackId")
    val id: Int = 0,
    @SerialName("cmd")
    val cmd: String = ""
)