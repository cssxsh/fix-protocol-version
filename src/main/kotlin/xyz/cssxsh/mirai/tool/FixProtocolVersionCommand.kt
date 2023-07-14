package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.utils.*

@PublishedApi
internal object FixProtocolVersionCommand : CompositeCommand(
    owner = FixProtocolVersionPlugin,
    "protocol",
    description = "FixProtocolVersionCommand"
) {
    @SubCommand
    suspend fun CommandSender.sync(protocol: BotConfiguration.MiraiProtocol, version: String = "latest") {
        try {
            FixProtocolVersion.sync(protocol, version)
            sendMessage(FixProtocolVersion.info()[protocol] ?: "找不到协议信息")
        } catch (cause: Throwable) {
            FixProtocolVersionPlugin.logger.warning(cause)
            sendMessage("出现错误")
        }
    }

    @SubCommand
    suspend fun CommandSender.load(protocol: BotConfiguration.MiraiProtocol) {
        try {
            FixProtocolVersion.load(protocol)
            sendMessage(FixProtocolVersion.info()[protocol] ?: "找不到协议信息")
        } catch (cause: Throwable) {
            FixProtocolVersionPlugin.logger.warning(cause)
            sendMessage("出现错误")
        }
    }

    @SubCommand
    suspend fun CommandSender.info() {
        sendMessage(buildString {
            appendLine("当前各协议版本日期: ")
            for ((_, info) in FixProtocolVersion.info()) {
                appendLine(info)
            }
        })
    }
}