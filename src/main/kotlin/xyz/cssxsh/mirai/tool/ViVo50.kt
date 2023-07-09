package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import org.asynchttpclient.*
import org.asynchttpclient.netty.ws.*
import org.asynchttpclient.ws.*
import java.security.*
import java.security.spec.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.*
import javax.crypto.spec.*
import kotlin.coroutines.*

public class ViVo50(
    private val server: String,
    private val serverIdentityKey: String,
    private val authorizationKey: String,
    coroutineContext: CoroutineContext
) : EncryptService, CoroutineScope {

    public companion object {
        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(ViVo50::class)
    }

    override val coroutineContext: CoroutineContext =
        coroutineContext + SupervisorJob(coroutineContext[Job]) + CoroutineExceptionHandler { context, exception ->
            when (exception) {
                is kotlinx.coroutines.CancellationException -> {
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

    private val sharedKey = SecretKeySpec(UUID.randomUUID().toString().substring(0, 16).encodeToByteArray(), "AES")

    private val rsaKeyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    private lateinit var token: String

    private lateinit var websocket: NettyWebSocket

    private lateinit var channel: EncryptService.ChannelProxy

    private var cmd: List<String> = emptyList()

    private val packet: MutableMap<String, CompletableFuture<JsonObject>> = ConcurrentHashMap()

    private fun <T> ListenableFuture<Response>.getBody(deserializer: DeserializationStrategy<T>): T {
        val response = get()
        return Json.decodeFromString(deserializer, response.responseBody)
    }

    private fun sendPacket(type: String, id: String, block: JsonObjectBuilder.() -> Unit) {
        val packet = buildJsonObject {
            put("packetId", id)
            put("packetType", type)
            block.invoke(this)
        }
        val text = Json.encodeToString(JsonElement.serializer(), packet)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey)
        websocket.sendBinaryFrame(cipher.doFinal(text.encodeToByteArray()))
    }

    private fun <T> sendCommand(
        type: String,
        deserializer: DeserializationStrategy<T>,
        block: JsonObjectBuilder.() -> Unit
    ): T? {

        val uuid = UUID.randomUUID().toString()
        val future = CompletableFuture<JsonObject>()
        packet[uuid] = future

        sendPacket(type = type, id = uuid, block = block)

        val json = future.get(60, TimeUnit.SECONDS)

        json["message"]?.jsonPrimitive?.content?.let {
            throw IllegalStateException(it)
        }

        val response = json["response"] ?: return null

        return Json.decodeFromJsonElement(deserializer, response)
    }

    override fun initialize(context: EncryptServiceContext) {
        val device = context.extraArgs[EncryptServiceContext.KEY_DEVICE_INFO]
        val qimei36 = context.extraArgs[EncryptServiceContext.KEY_QIMEI36]
        val protocol = context.extraArgs[EncryptServiceContext.KEY_BOT_PROTOCOL]
        channel = context.extraArgs[EncryptServiceContext.KEY_CHANNEL_PROXY]

        logger.info("Bot(${context.id} initialize ...")

        handshake(uin = context.id)
        openSession(token = token, bot = context.id)
        coroutineContext[Job]?.invokeOnCompletion {
            try {
                deleteSession(token = token)
            } catch (cause: Throwable) {
                logger.error(cause)
            }
            try {
                websocket.sendCloseFrame()
            } catch (cause: Throwable) {
                logger.error(cause)
            }
        }
        sendCommand(type = "rpc.initialize", deserializer = JsonElement.serializer()) {
            putJsonObject("extArgs") {
                put("KEY_QIMEI36", qimei36)
                putJsonObject("BOT_PROTOCOL") {
                    putJsonObject("protocolValue") {
                        @Suppress("INVISIBLE_MEMBER")
                        put("ver", MiraiProtocolInternal[protocol].ver)
                    }
                }
            }
            putJsonObject("device") {
                put("display", device.display.toUHexString(""))
                put("product", device.product.toUHexString(""))
                put("device", device.device.toUHexString(""))
                put("board", device.board.toUHexString(""))
                put("brand", device.brand.toUHexString(""))
                put("model", device.model.toUHexString(""))
                put("bootloader", device.bootloader.toUHexString(""))
                put("fingerprint", device.fingerprint.toUHexString(""))
                put("bootId", device.bootId.toUHexString(""))
                put("procVersion", device.procVersion.toUHexString(""))
                put("baseBand", device.baseBand.toUHexString(""))
                putJsonObject("version") {
                    put("incremental", device.version.incremental.toUHexString(""))
                    put("release", device.version.release.toUHexString(""))
                    put("codename", device.version.codename.toUHexString(""))
                    put("sdk", device.version.sdk)
                }
                put("simInfo", device.simInfo.toUHexString(""))
                put("osType", device.osType.toUHexString(""))
                put("macAddress", device.macAddress.toUHexString(""))
                put("wifiBSSID", device.wifiBSSID.toUHexString(""))
                put("wifiSSID", device.wifiSSID.toUHexString(""))
                put("imsiMd5", device.imsiMd5.toUHexString(""))
                put("imei", device.imei)
                put("apn", device.apn.toUHexString(""))
                put("androidId", device.androidId.toUHexString(""))
                @OptIn(MiraiInternalApi::class)
                put("guid", device.guid.toUHexString(""))
            }
        }
        sendCommand(type = "rpc.get_cmd_white_list", deserializer = ListSerializer(String.serializer())) {
            // ...
        }.also {
            cmd = checkNotNull(it)
        }

        logger.info("Bot(${context.id} initialize complete")
    }

    private fun handshake(uin: Long) {
        val config = client.prepareGet("${server}/service/rpc/handshake/config")
            .execute().getBody(HandshakeConfig.serializer())

        val pKeyRsaSha1 = (serverIdentityKey + config.publicKey)
            .toByteArray().sha1().toUHexString("").lowercase()
        val clientKeySignature = (pKeyRsaSha1 + serverIdentityKey)
            .toByteArray().sha1().toUHexString("").lowercase()

        check(clientKeySignature == config.keySignature) {
            "client calculated key signature doesn't match the server provides."
        }

        val secret = buildJsonObject {
            put("authorizationKey", authorizationKey)
            put("sharedKey", sharedKey.encoded.decodeToString())
            put("botid", uin)
        }.let {
            val text = Json.encodeToString(JsonElement.serializer(), it)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            val publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(config.publicKey)))
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            cipher.doFinal(text.encodeToByteArray())
        }


        val result = client.preparePost("${server}/service/rpc/handshake/handshake")
            .setBody(Json.encodeToString(JsonElement.serializer(), buildJsonObject {
                put("clientRsa", Base64.getEncoder().encodeToString(rsaKeyPair.public.encoded))
                put("secret", Base64.getEncoder().encodeToString(secret))
            }))
            .execute().getBody(HandshakeResult.serializer())

        check(result.status == 200) { result.reason }

        token = Base64.getDecoder().decode(result.token).decodeToString()
    }

    private fun openSession(token: String, bot: Long) {
        val listener = object : WebSocketListener {
            override fun onOpen(websocket: WebSocket) {
                // ...
            }

            override fun onClose(websocket: WebSocket, code: Int, reason: String?) {
                logger.error(reason)
            }

            override fun onError(cause: Throwable) {
                logger.error(cause)
            }

            override fun onBinaryFrame(payload: ByteArray, finalFragment: Boolean, rsv: Int) {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, sharedKey)
                val text = cipher.doFinal(payload).decodeToString()

                val json = Json.parseToJsonElement(text).jsonObject
                val id = json["packetId"]!!.jsonPrimitive.content
                packet[id]?.complete(json)

                when (json["packetType"]?.jsonPrimitive?.content) {
                    "rpc.service.send" -> {
                        val uin = json["botUin"]!!.jsonPrimitive.long
                        val cmd = json["command"]!!.jsonPrimitive.content
                        launch(CoroutineName(id)) {
                            logger.verbose("Bot(${bot}) sendMessage <- $cmd")

                            val result = channel.sendMessage(
                                remark = json["remark"]!!.jsonPrimitive.content,
                                commandName = cmd,
                                uin = uin,
                                data = json["data"]!!.jsonPrimitive.content.hexToBytes()
                            )

                            if (result == null) {
                                logger.debug("Bot(${bot}) ChannelResult is null")
                                return@launch
                            }
                            logger.verbose("Bot(${bot}) sendMessage -> ${result.cmd}")

                            sendPacket(type = "rpc.service.send", id = id) {
                                put("command", result.cmd)
                                put("data", result.data.toUHexString(""))
                            }
                        }
                    }
                    "service.interrupt" -> {
                        logger.error("Bot(${bot}) $text")
                    }
                    else -> {
                        // ...
                    }
                }
            }
        }
        val current = System.currentTimeMillis()
        websocket = client.prepareGet("${server}/service/rpc/session".replace("http", "ws"))
            .addHeader("Authorization", token)
            .addHeader("X-SEC-Time", current.toString())
            .addHeader("X-SEC-Signature", current.toString().let {
                val privateSignature = Signature.getInstance("SHA256withRSA")
                privateSignature.initSign(rsaKeyPair.private)
                privateSignature.update(it.encodeToByteArray())

                Base64.getEncoder().encodeToString(privateSignature.sign())
            })
            .execute(
                WebSocketUpgradeHandler
                    .Builder()
                    .addWebSocketListener(listener)
                    .build()
            )
            .get() ?: throw IllegalStateException("...")
    }

    private fun checkSession(token: String) {
        val current = System.currentTimeMillis()
        val response = client.prepareGet("${server}/service/rpc/session/check")
            .addHeader("Authorization", token)
            .addHeader("X-SEC-Time", current.toString())
            .addHeader("X-SEC-Signature", current.toString().let {
                val privateSignature = Signature.getInstance("SHA256withRSA")
                privateSignature.initSign(rsaKeyPair.private)
                privateSignature.update(it.encodeToByteArray())

                Base64.getEncoder().encodeToString(privateSignature.sign())
            })
            .execute().get()

        check(response.statusCode < 400) { response.responseBody }
    }

    private fun deleteSession(token: String) {
        val current = System.currentTimeMillis()
        val response = client.prepareDelete("${server}/service/rpc/session")
            .addHeader("Authorization", token)
            .addHeader("X-SEC-Time", current.toString())
            .addHeader("X-SEC-Signature", current.toString().let {
                val privateSignature = Signature.getInstance("SHA256withRSA")
                privateSignature.initSign(rsaKeyPair.private)
                privateSignature.update(it.encodeToByteArray())

                Base64.getEncoder().encodeToString(privateSignature.sign())
            })
            .execute().get()

        check(response.statusCode < 400) { response.responseBody }
    }

    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        val hex = sendCommand(type = "rpc.tlv", deserializer = String.serializer()) {
            put("tlvType", tlvType)
            putJsonObject("extArgs") {
                put("KEY_COMMAND_STR", command)
            }
            put("content", payload.toUHexString(""))
        } ?: return null

        return hex.hexToBytes()
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        if (commandName !in cmd) return null

        logger.debug("Bot(${context.id}) sign $commandName")

        val response = sendCommand(type = "rpc.sign", deserializer = RpcSignResult.serializer()) {
            put("seqId", sequenceId)
            put("command", commandName)
            putJsonObject("extArgs") {
                // ...
            }
            put("content", payload.toUHexString(""))
        } ?: return null

        return EncryptService.SignResult(
            sign = response.sign.hexToBytes(),
            extra = response.extra.hexToBytes(),
            token = response.token.hexToBytes(),
        )
    }
}

@Serializable
private data class HandshakeConfig(
    @SerialName("publicKey")
    val publicKey: String,
    @SerialName("timeout")
    val timeout: Long,
    @SerialName("keySignature")
    val keySignature: String
)

@Serializable
private data class HandshakeResult(
    @SerialName("status")
    val status: Int,
    @SerialName("reason")
    val reason: String = "",
    @SerialName("token")
    val token: String = ""
)

@Serializable
private data class RpcSignResult(
    val sign: String,
    val token: String,
    val extra: String,
)