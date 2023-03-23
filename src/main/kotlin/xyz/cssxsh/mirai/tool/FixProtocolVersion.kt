package xyz.cssxsh.mirai.tool

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.*
import com.fasterxml.jackson.module.kotlin.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.time.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object FixProtocolVersion {
    /**
     * 协议配置
     * 注解对应在线配置的key
     */
    public data class ProtocolConfig(
        @JsonProperty("apk_id") val apkId: String,
        @JsonProperty("app_id") val id: Long,
        @JsonProperty("sort_version_name") val ver: String,
        @JsonProperty("sdk_version") val sdkVer: String,
        @JsonProperty("misc_bitmap") val miscBitMap: Int,
        @JsonProperty("sub_sig_map") val subSigMap: Int,
        @JsonProperty("main_sig_map") val mainSigMap: Int,
        @JsonProperty("apk_sign") val sign: String,
        @JsonProperty("build_time") val buildTime: Long,
        @JsonProperty("sso_version") val ssoVersion: Int,
    )

    // 记录当前协议配置
    private val currentProtocols: MutableMap<BotConfiguration.MiraiProtocol, ProtocolConfig> =
        EnumMap(BotConfiguration.MiraiProtocol::class)

    // 预置更新url
    private val protocolFetchUrls: MutableMap<BotConfiguration.MiraiProtocol, String> =
        EnumMap(BotConfiguration.MiraiProtocol::class)

    init {
        // 定义默认配置
        currentProtocols[BotConfiguration.MiraiProtocol.ANDROID_PHONE] = ProtocolConfig(
            apkId = "com.tencent.mobileqq",
            id = 537151682,
            ver = "8.9.33.10335",
            sdkVer = "6.0.0.2534",
            miscBitMap = 150470524,
            subSigMap = 0x10400,
            mainSigMap = 16724722,
            sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
            buildTime = 1673599898L,
            ssoVersion = 19,
        )
        currentProtocols[BotConfiguration.MiraiProtocol.ANDROID_PAD] = ProtocolConfig(
            apkId = "com.tencent.mobileqq",
            id = 537151218,
            ver = "8.9.33.10335",
            sdkVer = "6.0.0.2534",
            miscBitMap = 150470524,
            subSigMap = 0x10400,
            mainSigMap = 16724722,
            sign = "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
            buildTime = 1673599898L,
            ssoVersion = 19,
        )
        currentProtocols[BotConfiguration.MiraiProtocol.IPAD] = ProtocolConfig(
            apkId = "com.tencent.minihd.qq",
            id = 537151363,
            ver = "8.9.33.614",
            sdkVer = "6.0.0.2433",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = 1970400,
            sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
            buildTime = 1640921786L,
            ssoVersion = 12,
        )
        currentProtocols[BotConfiguration.MiraiProtocol.MACOS] = ProtocolConfig(
            apkId = "com.tencent.minihd.qq",
            id = 537128930,
            ver = "5.8.9",
            sdkVer = "6.0.0.2433",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = 1970400,
            sign = "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7",
            buildTime = 1595836208L,
            ssoVersion = 12,
        )

        // 定义预置更新url
        // url来自 https://github.com/RomiChan/protocol-versions
        protocolFetchUrls[BotConfiguration.MiraiProtocol.ANDROID_PHONE] =
            "https://raw.githubusercontent.com/RomiChan/protocol-versions/master/android_phone.json"
        protocolFetchUrls[BotConfiguration.MiraiProtocol.ANDROID_PAD] =
            "https://raw.githubusercontent.com/RomiChan/protocol-versions/master/android_pad.json"
    }

    private val defaultHttpClient
        get() = HttpClient(OkHttp) {
            engine {
                proxy = KtorProxyUtil.getProxy(defaultHttpClientUseSystemProxy)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
        }
    private val defaultJsonMapper get() = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    public var defaultHttpClientUseSystemProxy: Boolean = true
        set(value) {
            defaultHttpClient.engineConfig.proxy = KtorProxyUtil.getProxy(value)
            field = value
        }

    /**
     * 更新包含预置url的所有协议
     */
    public suspend fun fetchOnline() {
        val http = defaultHttpClient
        val json = defaultJsonMapper
        protocolFetchUrls.forEach { (name, url) -> fetchOnline(name, url, http, json) }
    }

    /**
     * 通过预置url更新指定协议
     */
    public suspend fun fetchOnline(protocol: BotConfiguration.MiraiProtocol): Unit =
        fetchOnline(protocol, protocolFetchUrls.getValue(protocol))

    /**
     * 通过指定url更新指定协议
     */
    public suspend fun fetchOnline(protocol: BotConfiguration.MiraiProtocol, url: String): Unit =
        fetchOnline(protocol, url, defaultHttpClient, defaultJsonMapper)

    private suspend fun fetchOnline(
        protocol: BotConfiguration.MiraiProtocol,
        url: String,
        http: HttpClient,
        json: ObjectMapper
    ) {
        // 在线获取协议配置
        val response = http.get(url)
        if (!response.status.isSuccess()) throw RuntimeException("HTTP ${response.status.value} ${response.status.description}")

        // 解析并保存协议配置
        val body = response.body<ByteArray>()
        val config = json.readValue<ProtocolConfig>(body)
        currentProtocols[protocol] = config
    }


    @JvmStatic
    internal fun last(current: MiraiProtocolInternal, target: MiraiProtocolInternal): MiraiProtocolInternal {
        return if (current.id < target.id) {
            target
        } else {
            current
        }
    }

    @JvmStatic
    public fun update() {
        currentProtocols.forEach { (name, config) ->
            val protocol = with(config) {
                MiraiProtocolInternal(
                    apkId = apkId,
                    id = id,
                    ver = ver,
                    sdkVer = sdkVer,
                    miscBitMap = miscBitMap,
                    subSigMap = subSigMap,
                    mainSigMap = mainSigMap,
                    sign = sign,
                    buildTime = buildTime,
                    ssoVersion = ssoVersion
                )
            }
            MiraiProtocolInternal.protocols.merge(name, protocol, ::last)
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