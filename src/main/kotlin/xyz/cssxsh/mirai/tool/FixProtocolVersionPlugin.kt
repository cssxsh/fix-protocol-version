package xyz.cssxsh.mirai.tool

import kotlinx.coroutines.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.utils.*

public object FixProtocolVersionPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.fix-protocol-version",
        name = "fix-protocol-version",
        version = "1.3.0",
    ) {
        author("cssxsh")
    }
) {
    override fun PluginComponentStorage.onLoad() {
        if (SemVersion.parseRangeRequirement("<= 2.14.0").test(MiraiConsole.version)) {
            logger.warning { "Mirai版本低于预期，将升级协议版本" }
            runBlocking {
                try {
                    FixProtocolVersion.fetchOnline()
                } catch (e: Exception) {
                    logger.warning("在线更新协议时发生错误: ${e.message}; 将使用默认版本。")
                }
            }
            FixProtocolVersion.update()
        }
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
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