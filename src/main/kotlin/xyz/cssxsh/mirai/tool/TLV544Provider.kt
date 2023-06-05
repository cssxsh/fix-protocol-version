package xyz.cssxsh.mirai.tool

import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.net.URL
import java.util.*

public class TLV544Provider : EncryptService {
    internal companion object {
        val SALT_V1 = arrayOf("810_7", "810_24", "810_25")
        val SALT_V2 = arrayOf("810_9", "810_a", "810_d", "810_f")
        val SALT_V3 = arrayOf("812_a")
    }

    private val logger: MiraiLogger = MiraiLogger.Factory.create(this::class)

    @Suppress("INVISIBLE_MEMBER")
    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        val name = MiraiProtocolInternal[BotConfiguration.MiraiProtocol.ANDROID_PAD].ver
        val version = MiraiProtocolInternal[BotConfiguration.MiraiProtocol.ANDROID_PAD].sdkVer
        val guid = payload.sliceArray(if (payload.last().toInt() == 0) 6 until 22 else 10 until 26).toUHexString("")
        val mode = when (command) {
            in SALT_V1 -> "v1"
            in SALT_V2 -> "v2"
            in SALT_V3 -> "v3"
            else -> "v2"
        }

        return TODO()
    }
}