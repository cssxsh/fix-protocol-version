package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.utils.*

public object FixProtocolVersionPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.fix-protocol-version",
        name = "fix-protocol-version",
        version = "1.5.1",
    ) {
        author("cssxsh")
    }
) {
    override fun PluginComponentStorage.onLoad() {
        logger.info("协议版本检查更新...")
        try {
            FixProtocolVersion.update()
        } catch (cause: Throwable) {
            logger.error("协议版本升级失败", cause)
        }
    }

    override fun onEnable() {
        logger.info {
            buildString {
                appendLine("当前各协议版本日期: ")
                for ((_, info) in FixProtocolVersion.info()) {
                    appendLine(info)
                }
            }
        }
    }
}