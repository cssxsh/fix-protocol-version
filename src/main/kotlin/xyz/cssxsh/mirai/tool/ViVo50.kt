package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
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

    private val packet: MutableMap<String, CompletableFuture<JsonElement>> = ConcurrentHashMap()

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
        websocket.sendTextFrame(Json.encodeToString(JsonElement.serializer(), packet))
    }

    override fun initialize(context: EncryptServiceContext) {
        val device = context.extraArgs[EncryptServiceContext.KEY_DEVICE_INFO]
        val qimei36 = context.extraArgs[EncryptServiceContext.KEY_QIMEI36]
        channel = context.extraArgs[EncryptServiceContext.KEY_CHANNEL_PROXY]

        handshake(uin = context.id)
        openSession(token = token)
        sendPacket(type = "rpc.initialize", id = "") {
            putJsonObject("extArgs") {
                put("KEY_QIMEI36", qimei36)
                putJsonObject("BOT_PROTOCOL") {
                    putJsonObject("protocolValue") {
                        put("ver", "8.9.58")
                    }
                }
                put("device", Json.parseToJsonElement(DeviceInfo.serializeToString(device)))
            }
        }
        sendPacket(type = "rpc.get_cmd_white_list", id = "initialize") {
            // ...
        }
    }

    private fun handshake(uin: Long) {
        val config = client.prepareGet("${server}/service/rpc/handshake/config")
            .execute().getBody(HandshakeConfig.serializer())

        val pKeyRsaSha1 = (serverIdentityKey + config.publicKey)
            .toByteArray().sha1().toUHexString("")
        val clientKeySignature = (pKeyRsaSha1 + serverIdentityKey)
            .toByteArray().sha1().toUHexString("")

        check(clientKeySignature == config.keySignature) {
            "client calculated key signature doesn't match the server provides."
        }

        val rsaKeyPair = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

        val secret = buildJsonObject {
            put("authorizationKey", authorizationKey)
            put("sharedKey", sharedKey.encoded.toUHexString(""))
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
                put("clientRsa", rsaKeyPair.public.encoded.toUHexString(""))
                put("secret", secret.toUHexString(""))
            }))
            .execute().getBody(HandshakeResult.serializer())

        check(result.status == 200) { result.reason }

        token = result.token
    }

    private fun openSession(token: String) {
        val listener = object : WebSocketListener {
            override fun onOpen(websocket: WebSocket) {
                // TODO("Not yet implemented")
            }

            override fun onClose(websocket: WebSocket, code: Int, reason: String?) {
                // TODO("Not yet implemented")
            }

            override fun onError(cause: Throwable) {
                // TODO("Not yet implemented")
            }

            override fun onTextFrame(payload: String, finalFragment: Boolean, rsv: Int) {
                val json = Json.parseToJsonElement(payload).jsonObject
                when (json["packetType"]?.jsonPrimitive?.content) {
                    "rpc.service.send" -> {
                        val id = json["packetId"]!!.jsonPrimitive.content
                        val uin = json["botUin"]!!.jsonPrimitive.long
                        launch(CoroutineName(id)) {
                            val result = channel.sendMessage(
                                remark = json["remark"]!!.jsonPrimitive.content,
                                commandName = json["command"]!!.jsonPrimitive.content,
                                uin = uin,
                                data = json["data"]!!.jsonPrimitive.content.hexToBytes()
                            )

                            if (result == null) {
                                logger.debug("Bot(${uin}) ChannelResult is null")
                                return@launch
                            }
                            logger.debug("Bot(${uin}) sendMessage -> ${result.cmd}")

                            sendPacket(type = "rpc.service.send", id = id) {
                                put("command", result.cmd)
                                put("data", result.data.toUHexString(""))
                            }
                        }
                    }
                    "rpc.sign", "rpc.tlv" -> {
                        val id = json["packetId"]!!.jsonPrimitive.content
                        val response = json["response"]!!
                        val future = packet[id]!!
                        future.complete(response)
                    }
                    "rpc.get_cmd_white_list" -> {
                        cmd= Json.decodeFromJsonElement(ListSerializer(String.serializer()), json["response"]!!)
                    }
                    "rpc.initialize" -> Unit
                    else -> {
                        logger.error(payload)
                    }
                }
            }
        }
        val current = System.currentTimeMillis()
        websocket = client.prepareGet("${server}/service/rpc/session".replace("http", "ws"))
            .addHeader("Authorization", token)
            .addHeader("X-SEC-Time", current.toString())
            .addHeader("X-SEC-Signature", current.toString().let {
                val cipher = Cipher.getInstance("SHA256withRSA")
                cipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.private)

                val bytes = cipher.doFinal(it.encodeToByteArray())
                Base64.getEncoder().encodeToString(bytes)
            })
            .execute(
                WebSocketUpgradeHandler
                    .Builder()
                    .addWebSocketListener(listener)
                    .build()
            )
            .get()
    }

    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]
        val id = UUID.randomUUID().toString()
        val future = CompletableFuture<JsonElement>()
        packet[id] = future

        sendPacket(type = "rpc.tlv", id = id) {
            put("tlvType", tlvType)
            putJsonObject("extArgs") {
                put("command", command)
            }
            put("content", payload.toUHexString(""))
        }

        val response = try {
            future.get(60, TimeUnit.SECONDS)
        } catch (cause: Throwable) {
            logger.error(cause)
            return null
        }

        return response.jsonPrimitive.content.hexToBytes()
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        if (commandName !in cmd) return null

        val id = UUID.randomUUID().toString()
        val future = CompletableFuture<JsonElement>()
        packet[id] = future

        sendPacket(type = "rpc.sign", id = id) {
            put("seqId", sequenceId)
            put("command", commandName)
            putJsonObject("extArgs") {
                // ...
            }
            put("content", payload.toUHexString(""))
        }

        val response = try {
            future.get(60, TimeUnit.SECONDS).jsonObject
        } catch (cause: Throwable) {
            logger.error(cause)
            return null
        }

        return EncryptService.SignResult(
            sign = response["sign"]!!.jsonPrimitive.content.hexToBytes(),
            extra = response["extra"]!!.jsonPrimitive.content.hexToBytes(),
            token = response["token"]!!.jsonPrimitive.content.hexToBytes(),
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