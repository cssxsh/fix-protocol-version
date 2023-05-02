package xyz.cssxsh.mirai.tool

import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.time.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object FixProtocolVersion {

    private val constructor = MiraiProtocolInternal::class.java.constructors.single()

    @PublishedApi
    internal fun MiraiProtocolInternal(vararg args: Any): MiraiProtocolInternal {
        return constructor.newInstance(*args) as MiraiProtocolInternal
    }

    @JvmStatic
    public fun update() {
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_PHONE) { _, impl ->
            when {
                null == impl -> null
                constructor.parameterCount == 10 -> MiraiProtocolInternal(
                    "com.tencent.mobileqq",
                    537153294,
                    "8.9.35.10440",
                    "6.0.0.2535",
                    0x08F7_FF7C,
                    0x0001_0400,
                    0x00FF_32F2,
                    "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    1676531414L,
                    19
                )
                impl.id < 537153294 -> MiraiProtocolInternal(
                    apkId = "com.tencent.mobileqq",
                    id = 537153294,
                    ver = "8.9.35.10440",
                    sdkVer = "6.0.0.2535",
                    miscBitMap = 0x08F7_FF7C,
                    subSigMap = 0x0001_0400,
                    mainSigMap = 0x00FF_32F2,
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    buildTime = 1676531414L,
                    ssoVersion = 19,
                    supportsQRLogin = false
                )
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_PAD) { _, impl ->
            when {
                null == impl -> null
                constructor.parameterCount == 10 -> MiraiProtocolInternal(
                    "com.tencent.mobileqq",
                    537152242,
                    "8.9.35.10440",
                    "6.0.0.2535",
                    0x08F7_FF7C,
                    0x0001_0400,
                    0x00FF_32F2,
                    "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    1676531414L,
                    19
                )
                impl.id < 537152242 -> MiraiProtocolInternal(
                    apkId = "com.tencent.mobileqq",
                    id = 537152242,
                    ver = "8.9.35.10440",
                    sdkVer = "6.0.0.2535",
                    miscBitMap = 0x08F7_FF7C,
                    subSigMap = 0x0001_0400,
                    mainSigMap = 0x00FF_32F2,
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    buildTime = 1676531414L,
                    ssoVersion = 19,
                    supportsQRLogin = false
                )
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.ANDROID_WATCH) { _, impl ->
            when {
                null == impl -> null
                constructor.parameterCount == 10 -> MiraiProtocolInternal(
                    "com.tencent.qqlite",
                    537065138,
                    "2.0.8",
                    "6.0.0.2365",
                    0x00F7_FF7C,
                    0x0001_0400,
                    0x00FF_32F2,
                    "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    1559564731L,
                    5
                )
                impl.id < 537065138 -> MiraiProtocolInternal(
                    apkId = "com.tencent.qqlite",
                    id = 537065138,
                    ver = "2.0.8",
                    sdkVer = "6.0.0.2365",
                    miscBitMap = 0x00F7_FF7C,
                    subSigMap = 0x0001_0400,
                    mainSigMap = 0x00FF_32F2,
                    sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
                    buildTime = 1559564731L,
                    ssoVersion = 5,
                    supportsQRLogin = true
                )
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.IPAD) { _, impl ->
            when {
                null == impl -> null
                constructor.parameterCount == 10 -> MiraiProtocolInternal(
                    "com.tencent.minihd.qq",
                    537151363,
                    "8.9.33.614",
                    "6.0.0.2433",
                    0x08F7_FF7C,
                    0x0001_0400,
                    0x001E_10E0,
                    "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
                    1640921786L,
                    19
                )
                impl.id < 537151363 -> MiraiProtocolInternal(
                    apkId = "com.tencent.minihd.qq",
                    id = 537151363,
                    ver = "8.9.33.614",
                    sdkVer = "6.0.0.2433",
                    miscBitMap = 0x08F7_FF7C,
                    subSigMap = 0x0001_0400,
                    mainSigMap = 0x001E_10E0,
                    sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
                    buildTime = 1640921786L,
                    ssoVersion = 19,
                    supportsQRLogin = false
                )
                else -> impl
            }
        }
        MiraiProtocolInternal.protocols.compute(BotConfiguration.MiraiProtocol.MACOS) { _, impl ->
            when {
                null == impl -> null
                constructor.parameterCount == 10 -> MiraiProtocolInternal(
                    "com.tencent.minihd.qq",
                    537128930,
                    "5.8.9",
                    "6.0.0.2433",
                    0x08F7_FF7C,
                    0x0001_0400,
                    0x001E_10E0,
                    "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
                    1595836208L,
                    12
                )
                impl.id < 537128930 -> impl.apply {
                    // TODO
                }
                else -> impl
            }
        }
    }

    @JvmStatic
    public fun info(): Map<BotConfiguration.MiraiProtocol, String> {
        return MiraiProtocolInternal.protocols.mapValues { (protocol, info) ->
            val version = info.ver
            val datetime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(info.buildTime), ZoneId.systemDefault())

            "%-13s  %-12s  %s".format(protocol, version, datetime)
        }
    }
}