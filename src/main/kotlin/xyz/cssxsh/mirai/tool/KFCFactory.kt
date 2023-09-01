package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.URL

public class KFCFactory(private val config: File) : EncryptService.Factory {
    public constructor() : this(config = File(System.getProperty(CONFIG_PATH_PROPERTY, "KFCFactory.json")))

    public companion object {

        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(KFCFactory::class)

        @JvmStatic
        public fun install() {
            Services.register(
                EncryptService.Factory::class.qualifiedName!!,
                KFCFactory::class.qualifiedName!!,
                ::KFCFactory
            )
        }

        @JvmStatic
        public fun info(): Map<String, String> {
            val config = File(System.getProperty(CONFIG_PATH_PROPERTY, "KFCFactory.json"))
            val serializer = MapSerializer(String.serializer(), Cola.serializer())
            val servers = Json.decodeFromString(serializer, config.readText())

            return servers.mapValues { (version, server) ->
                "v${version} by ${server.type} from ${server.base}"
            }
        }

        @JvmStatic
        public val CONFIG_PATH_PROPERTY: String = "xyz.cssxsh.mirai.tool.KFCFactory.config"

        @JvmStatic
        public val DEFAULT_CONFIG: String = """
            {
                "0.0.0": {
                    "base_url": "http://127.0.0.1:8080",
                    "type": "fuqiuluo/unidbg-fetch-qsign",
                    "key": "114514"
                },
                "0.1.0": {
                    "base_url": "http://127.0.0.1:8888",
                    "type": "kiliokuara/magic-signer-guide",
                    "server_identity_key": "vivo50",
                    "authorization_key": "kfc"
                },
                "8.8.88": {
                    "base_url": "",
                    "type": "TLV544Provider"
                }
            }
        """.trimIndent()

        @JvmStatic
        internal val created: MutableSet<Long> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    }

    init {
        with(config) {
            if (exists().not()) {
                writeText(DEFAULT_CONFIG)
            }
        }
    }

    override fun createForBot(context: EncryptServiceContext, serviceSubScope: CoroutineScope): EncryptService {
        if (created.add(context.id).not()) {
            throw UnsupportedOperationException("repeated create EncryptService")
        }
        serviceSubScope.coroutineContext.job.invokeOnCompletion {
            created.remove(context.id)
        }
        try {
            org.asynchttpclient.Dsl.config()
        } catch (cause: NoClassDefFoundError) {
            throw RuntimeException("请参照 https://search.maven.org/artifact/org.asynchttpclient/async-http-client/2.12.3/jar 添加依赖", cause)
        }
        return when (val protocol = context.extraArgs[EncryptServiceContext.KEY_BOT_PROTOCOL]) {
            BotConfiguration.MiraiProtocol.ANDROID_PHONE, BotConfiguration.MiraiProtocol.ANDROID_PAD -> {
                @Suppress("INVISIBLE_MEMBER")
                val version = MiraiProtocolInternal[protocol].ver
                val server = with(config) {
                    val serializer = MapSerializer(String.serializer(), Cola.serializer())
                    val servers = try {
                        Json.decodeFromString(serializer, readText())
                    } catch (cause: SerializationException) {
                        throw RuntimeException("配置文件格式错误，${toPath().toUri()}", cause)
                    } catch (cause: IOException) {
                        throw RuntimeException("配置文件读取错误，${toPath().toUri()}", cause)
                    }
                    servers[version]
                        ?: throw NoSuchElementException("没有找到对应 ${protocol}(${version}) 的服务配置，${toPath().toUri()}")
                }

                logger.info("${protocol}(${version}) EncryptService by ${server.type} from ${config.toPath().toUri()}")
                when (val type = server.type.ifEmpty { throw IllegalArgumentException("need server type") }) {
                    "fuqiuluo/unidbg-fetch-qsign", "fuqiuluo", "unidbg-fetch-qsign" -> {
                        try {
                            val about = URL(server.base).readText()
                            logger.info("unidbg-fetch-qsign by ${server.base} about \n" + about)
                            when {
                                "version" !in about -> {
                                    // 低于等于 1.1.3 的的版本 requestToken 不工作
                                    System.setProperty(UnidbgFetchQsign.REQUEST_TOKEN_INTERVAL, "0")
                                    logger.error("请更新 unidbg-fetch-qsign")
                                }
                                version !in about -> {
                                    throw IllegalStateException("unidbg-fetch-qsign by ${server.base} 与 ${protocol}(${version}) 似乎不匹配")
                                }
                                "IAA" !in about -> {
                                    logger.error("请确认服务类型为 unidbg-fetch-qsign")
                                }
                            }
                        } catch (cause: ConnectException) {
                            throw RuntimeException("请检查 unidbg-fetch-qsign by ${server.base} 的可用性", cause)
                        } catch (cause: java.io.FileNotFoundException) {
                            throw RuntimeException("请检查 unidbg-fetch-qsign by ${server.base} 的可用性", cause)
                        }
                        if (server.key.isEmpty()) {
                            logger.warning("unidbg-fetch-qsign key is empty")
                        }
                        UnidbgFetchQsign(
                            server = server.base,
                            key = server.key,
                            coroutineContext = serviceSubScope.coroutineContext
                        )
                    }
                    "kiliokuara/magic-signer-guide", "kiliokuara", "magic-signer-guide", "vivo50" -> {
                        try {
                            val about = URL(server.base).readText()
                            logger.info("magic-signer-guide by ${server.base} about \n" + about)
                            when {
                                "void" == about.trim() -> {
                                    logger.error("请更新 magic-signer-guide 的 docker 镜像")
                                }
                                version !in about -> {
                                    throw IllegalStateException("magic-signer-guide by ${server.base} 与 ${protocol}(${version}) 似乎不匹配")
                                }
                                "magic-signer-guide" !in about -> {
                                    logger.error("请确认服务类型为 magic-signer-guide")
                                }
                            }
                        } catch (cause: ConnectException) {
                            throw RuntimeException("请检查 magic-signer-guide by ${server.base} 的可用性", cause)
                        } catch (cause: java.io.FileNotFoundException) {
                            throw RuntimeException("请检查 unidbg-fetch-qsign by ${server.base} 的可用性", cause)
                        }
                        if (server.serverIdentityKey.isEmpty()) {
                            logger.warning("magic-signer-guide server_identity_key is empty")
                        }
                        if (server.authorizationKey.isEmpty()) {
                            logger.warning("magic-signer-guide authorization_key is empty")
                        }
                        ViVo50(
                            server = server.base,
                            serverIdentityKey = server.serverIdentityKey,
                            authorizationKey = server.authorizationKey,
                            coroutineContext = serviceSubScope.coroutineContext
                        )
                    }
                    "TLV544Provider" -> TLV544Provider()
                    else -> throw UnsupportedOperationException(type)
                }
            }
            BotConfiguration.MiraiProtocol.ANDROID_WATCH -> throw UnsupportedOperationException(protocol.name)
            BotConfiguration.MiraiProtocol.IPAD, BotConfiguration.MiraiProtocol.MACOS -> {
                logger.warning("$protocol 尚不支持签名服务，大概率登录失败")
                TLV544Provider()
            }
        }
    }

    override fun toString(): String {
        return "KFCFactory(config=${config.toPath().toUri()})"
    }
}
