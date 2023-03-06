package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.time.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object FixProtocolVersion : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.fix-protocol-version",
        name = "fix-protocol-version",
        version = "1.0.0",
    ) {
        author("cssxsh")
    }
) {

    private fun fix() {
        MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PHONE] = MiraiProtocolInternal(
            "com.tencent.mobileqq",
            537151682,
            "8.9.33.10335",
            "6.0.0.2534",
            150470524,
            0x10400,
            16724722,
            "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
            1673599898L,
            19,
        )
        MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PAD] = MiraiProtocolInternal(
            "com.tencent.mobileqq",
            537151218,
            "8.9.33.10335",
            "6.0.0.2534",
            150470524,
            0x10400,
            16724722,
            "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
            1673599898L,
            19,
        )
    }

    override fun PluginComponentStorage.onLoad() {
        if (SemVersion.parseRangeRequirement("<= 2.14.0").test(MiraiConsole.version)) {
            fix()
        }
    }

    override fun onEnable() {
        logger.info {
            buildString {
                appendLine("当前各版本协议信息: ")
                for ((protocol, info) in MiraiProtocolInternal.protocols) {
                    val version = info.ver
                    val datetime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(info.buildTime), ZoneId.systemDefault())
                    appendLine("$protocol $version $datetime")
                }
            }
        }
    }
}