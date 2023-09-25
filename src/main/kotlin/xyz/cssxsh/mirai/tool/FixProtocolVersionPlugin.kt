package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.io.File

@PublishedApi
internal object FixProtocolVersionPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.fix-protocol-version",
        name = "fix-protocol-version",
        version = "1.12.0"
    ) {
        author("cssxsh")
    }
) {
    override fun PluginComponentStorage.onLoad() {
        logger.info("协议版本检查更新...")
        try {
            FixProtocolVersion.update()
            for (protocol in BotConfiguration.MiraiProtocol.values()) {
                val file = File("${protocol.name.lowercase()}.json")
                if (file.exists()) {
                    logger.info("$protocol load from \n ${file.toPath().toUri()}")
                    FixProtocolVersion.load(protocol, file)
                }
            }
        } catch (cause: Throwable) {
            logger.error("协议版本升级失败", cause)
        }
        logger.info("注册服务...")
        try {
            KFCFactory.install()
            with(File(System.getProperty(KFCFactory.CONFIG_PATH_PROPERTY, "KFCFactory.json"))) {
                if (exists().not()) {
                    writeText(KFCFactory.DEFAULT_CONFIG)
                }
                logger.info("服务配置文件: \n ${toPath().toUri()}")
            }
        } catch (_: NoClassDefFoundError) {
            logger.warning("注册服务失败，请在 2.15.0-dev-105 或更高版本下运行")
            TLV544Provider.install()
        } catch (cause: Throwable) {
            logger.error("注册服务失败", cause)
        }
    }

    override fun onEnable() {
        logger.info {
            buildString {
                appendLine("protocol commands: info, load, fetch")
                appendLine("example:")
                appendLine("  protocol info")
                appendLine("  protocol load ANDROID_PHONE")
                appendLine("  protocol fetch ANDROID_PAD 8.9.63")
            }
        }
        logger.info {
            buildString {
                appendLine("当前各登录协议版本日期: ")
                for ((_, info) in FixProtocolVersion.info()) {
                    appendLine(info)
                }

                if ("8.8.88" in this) appendLine().append("Android 8.8.88 协议疑似被拉黑，请谨慎尝试登录")
            }
        }
        logger.info {
            buildString {
                appendLine("当前签名服务配置信息: ")
                for ((version, info) in KFCFactory.info()) {
                    if (version == "0.0.0" || version == "0.1.0") continue
                    appendLine(info)
                }

                @Suppress("INVISIBLE_MEMBER")
                setOf(
                    MiraiProtocolInternal[BotConfiguration.MiraiProtocol.ANDROID_PHONE].ver,
                    MiraiProtocolInternal[BotConfiguration.MiraiProtocol.ANDROID_PAD].ver
                ).forEach { version ->
                    if (version !in this) appendLine().append("缺少 v${version} 配置")
                }
            }
        }
        FixProtocolVersionCommand.register()
    }

    override fun onDisable() {
        FixProtocolVersionCommand.unregister()
    }
}