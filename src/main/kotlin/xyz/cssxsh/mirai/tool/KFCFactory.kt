package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*

public class KFCFactory : EncryptService.Factory {
    public companion object {
        @JvmStatic
        public fun install() {
            Services.register(
                EncryptService.Factory::class.qualifiedName!!,
                KFCFactory::class.qualifiedName!!,
                ::KFCFactory
            )
        }
    }

    @Suppress("INVISIBLE_MEMBER")
    override fun createForBot(context: EncryptServiceContext, serviceSubScope: CoroutineScope): EncryptService {
        return when (val protocol = context.extraArgs[EncryptServiceContext.KEY_BOT_PROTOCOL]) {
            BotConfiguration.MiraiProtocol.ANDROID_PHONE, BotConfiguration.MiraiProtocol.ANDROID_PAD -> {
                val impl = MiraiProtocolInternal[protocol]

                if (impl.ver == "8.8.88") return TLV544Provider()

                val server = with(java.io.File("KFCFactory.json")) {
                    if (exists().not()) {
                        writeText(
                            """
                            {
                                "0.0.0": {
                                    "base_url": "http://127.0.0.1:8080",
                                    "key": "114514"
                                }
                                "0.1.0": {
                                    "base_url": "http://127.0.0.1:8888",
                                    "serverIdentityKey": "vivo50",
                                    "authorizationKey": "kfc"
                                }
                            }
                        """.trimIndent()
                        )
                    }
                    val serializer = MapSerializer(String.serializer(), ServerConfig.serializer())
                    val servers = Json.decodeFromString(serializer, readText())
                    servers[impl.ver]
                        ?: throw NoSuchElementException("没有找到对应 ${impl.ver} 的服务配置，${toPath().toUri()}")
                }

                when (val type = server.type.ifEmpty { if (server.key.isNotEmpty()) "unidbg-fetch-qsign" else "magic-signer-guide" }) {
                    "unidbg-fetch-qsign" -> UnidbgFetchQsign(
                        server = server.base,
                        key = server.key,
                        coroutineContext = serviceSubScope.coroutineContext
                    )
                    "magic-signer-guide" -> ViVo50(
                        server = server.base,
                        serverIdentityKey = server.serverIdentityKey,
                        authorizationKey = server.authorizationKey,
                        coroutineContext = serviceSubScope.coroutineContext
                    )
                    else -> throw UnsupportedOperationException(type)
                }
            }
            BotConfiguration.MiraiProtocol.ANDROID_WATCH -> throw UnsupportedOperationException(protocol.name)
            BotConfiguration.MiraiProtocol.IPAD -> TLV544Provider()
            BotConfiguration.MiraiProtocol.MACOS -> TLV544Provider()
        }
    }
}

@Serializable
private data class ServerConfig(
    @SerialName("base_url")
    val base: String,
    @SerialName("type")
    val type: String = "",
    @SerialName("key")
    val key: String = "",
    @SerialName("serverIdentityKey")
    val serverIdentityKey: String = "",
    @SerialName("authorizationKey")
    val authorizationKey: String = ""
)