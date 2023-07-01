package xyz.cssxsh.mirai.tool

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.internal.spi.*
import net.mamoe.mirai.internal.utils.*
import net.mamoe.mirai.utils.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.*
import kotlin.coroutines.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public class TLV544Provider : EncryptService, CoroutineScope {
    public companion object {
        internal val SALT_V1 = arrayOf("810_2", "810_7", "810_24", "810_25")
        internal val SALT_V2 = arrayOf("810_9", "810_a", "810_d", "810_f")
        internal val SALT_V3 = arrayOf("812_a")
        internal val SALT_V4 = arrayOf("812_5")
        internal val CMD_WHITE_LIST = TLV544Provider::class.java.getResource("cmd.txt")!!.readText().lines()

        @JvmStatic
        internal val logger: MiraiLogger = MiraiLogger.Factory.create(TLV544Provider::class)

        @JvmStatic
        public fun install() {
            Services.register(
                "net.mamoe.mirai.internal.spi.EncryptService",
                "xyz.cssxsh.mirai.tool.TLV544Provider",
                ::TLV544Provider
            )
        }
    }

    @PublishedApi
    internal val http: HttpClient = HttpClient(OkHttp) {
        BrowserUserAgent()
        ContentEncoding()
        expectSuccess = true
        install(HttpTimeout) {
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = null
        }
        install(ContentNegotiation) {
            json()
        }
    }

    override val coroutineContext: CoroutineContext = CoroutineExceptionHandler { context, exception ->
        when (exception) {
            is CancellationException -> {
                // ...
            }
            else -> {
                logger.warning({ "TLV544Provider with ${context[CoroutineName]}" }, exception)
            }
        }
    }

    private val server: MutableMap<Long, Process> = HashMap()

    private val ports: MutableMap<Long, Int> = HashMap()

    @Synchronized
    @PublishedApi
    internal fun server(id: Long, ver: String, uuid: String): Process {
        val impl = server[id]
        if (impl?.isAlive == true) return impl


        val fail = mutableListOf<Int>()
        val port = kotlin.run {
            val random = kotlin.random.Random(id)
            for (i in (0 until 5)) {
                val temp = random.nextInt(5001, 65535)
                try {
                    ServerSocket().use { socket ->
                        socket.reuseAddress = true
                        socket.bind(InetSocketAddress("127.0.0.1", temp))
                    }
                    return@run temp
                } catch (cause: Throwable) {
                    fail.add(temp)
                    continue
                }
            }
            throw UnsupportedOperationException("端口被占用 $fail")
        }

        val folder = java.io.File("./").listFiles()
            ?.find { it.name.startsWith("unidbg-fetch-qsign") }
            ?: throw java.io.FileNotFoundException("unidbg-fetch-qsign")

        logger.info("sign server folder ${folder.toPath().toUri()}")
        val log = folder.resolve("${id}.${port}.log")
        if (log.exists().not()) log.createNewFile()
        val error = folder.resolve("${id}.${port}.error.log")
        if (error.exists().not()) error.createNewFile()
        val script = when (System.getProperty("os.name")) {
            "Mac OS X" -> folder.resolve("bin").resolve("unidbg-fetch-qsign")
            "Linux" -> folder.resolve("bin").resolve("unidbg-fetch-qsign")
            else -> folder.resolve("bin").resolve("unidbg-fetch-qsign.bat")
        }
        script.setExecutable(true)
        val process = async(CoroutineName("server<${id}>")) {
            val process = runInterruptible(Dispatchers.IO) {
                ProcessBuilder(
                    script.absolutePath,
                    "--host=0.0.0.0",
                    "--port=${port}",
                    "--count=2",
                    "--library=txlib/${ver}",
                    "--android_id=${uuid}"
                )
                    .directory(folder)
                    .redirectOutput(log)
                    .redirectError(error)
                    .start()
            }

            logger.info("sign server start, process ${process}, log ${log.toPath().toUri()}")

            Runtime.getRuntime().addShutdownHook(Thread {
                with(process.toHandle()) {
                    children().forEach {
                        it.destroy()
                    }
                    destroy()
                }
            })

            var i = 0
            while (isActive) {
                delay(1_000)
                i++
                try {
                    http.get("http://127.0.0.1:${port}/custom_energy?salt=&data=114514").body<JsonObject>()
                    break
                } catch (cause: java.net.ConnectException) {
                    if (i > 180) throw cause
                    continue
                }
            }

            logger.info("sign server ready, url http://127.0.0.1:${port}")

            process
        }.asCompletableFuture().get()

        launch(CoroutineName("server<${id}>")) {
            val code = runInterruptible(Dispatchers.IO) {
                process.waitFor()
            }
            logger.info("sign server exit(${re})")
        }
        server[id] = process
        ports[id] = port
        return process
    }

    override fun encryptTlv(context: EncryptServiceContext, tlvType: Int, payload: ByteArray): ByteArray? {
        if (tlvType != 0x544) return null
        val command = context.extraArgs[EncryptServiceContext.KEY_COMMAND_STR]

        logger.info("t544 command: $command")

        val port = ports[context.id]!!

        val future = async(CoroutineName("encryptTlv(${context.id})")) {
            val json = http.get("http://127.0.0.1:${port}/custom_energy") {
                parameter("salt", payload.toUHexString(""))
                parameter("data", command)
            }.body<JsonObject>()

            check(json["code"]?.jsonPrimitive?.int == 0) { json.toString() }
            val data = checkNotNull(json["data"]?.jsonPrimitive?.content) { json.toString() }

            data.hexToBytes()
        }.asCompletableFuture()
        return future.get()
    }

    override fun initialize(context: EncryptServiceContext) {
        val protocol = context.extraArgs[EncryptServiceContext.KEY_BOT_PROTOCOL]
        val impl = MiraiProtocolInternal.protocols[protocol]!!
        val device = context.extraArgs[EncryptServiceContext.KEY_DEVICE_INFO]

        server(id = context.id, ver = impl.ver, uuid = device.androidId.decodeToString())
    }

    override fun qSecurityGetSign(
        context: EncryptServiceContext,
        sequenceId: Int,
        commandName: String,
        payload: ByteArray
    ): EncryptService.SignResult? {
        if (commandName !in CMD_WHITE_LIST) return null

        val qua = context.extraArgs[EncryptServiceContext.KEY_APP_QUA]

        logger.info("sign command: $commandName with $qua")

        val port = ports[context.id]!!

        val future = async(CoroutineName("qSecurityGetSign(${context.id})")) {
            val json = http.get("http://127.0.0.1:$port/sign") {
                parameter("uin", context.id)
                parameter("qua", qua)
                parameter("cmd", commandName)
                parameter("seq", sequenceId)
                parameter("buffer", payload.toUHexString(""))
            }.body<JsonObject>()

            check(json["code"]?.jsonPrimitive?.int == 0) { json.toString() }
            val data = checkNotNull(json["data"]?.jsonObject) { json.toString() }

            EncryptService.SignResult(
                sign = data["sign"]!!.jsonPrimitive.content.hexToBytes(),
                token = data["token"]!!.jsonPrimitive.content.hexToBytes(),
                extra = data["extra"]!!.jsonPrimitive.content.hexToBytes()
            )
        }.asCompletableFuture()

        return try {
            future.get()
        } catch (cause: Throwable) {
            logger.error("qSecurityGetSign<${context.id}>", cause)
            null
        }
    }
}