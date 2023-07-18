package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.utils.*
import java.io.File

@PublishedApi
internal object FixProtocolVersionPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.fix-protocol-version",
        name = "fix-protocol-version",
        version = "1.9.7"
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
                    logger.info("$protocol load from ${file.toPath().toUri()}")
                    FixProtocolVersion.load(protocol)
                }
            }
        } catch (cause: Throwable) {
            logger.error("协议版本升级失败", cause)
        }
        logger.info("注册服务...")
        try {
            KFCFactory.install()
            with(File("KFCFactory.json")) {
                if (exists().not()) {
                    writeText(KFCFactory.DEFAULT_CONFIG)
                }
                logger.info("服务配置文件 ${toPath().toUri()}")
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
                appendLine("当前各协议版本日期: ")
                for ((_, info) in FixProtocolVersion.info()) {
                    appendLine(info)
                }

                if ("8.8.88" in this) appendLine().append("Android 8.8.88 协议疑似被拉黑，请谨慎尝试登录")
            }
        }
        FixProtocolVersionCommand.register()
    }

    override fun onDisable() {
        FixProtocolVersionCommand.unregister()
    }
}