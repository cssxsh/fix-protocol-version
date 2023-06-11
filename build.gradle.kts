plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.22"

    id("net.mamoe.mirai-console") version "2.15.0-M1"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "1.7.0"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("cssxsh", "fix-protocol-version")
    licenseFromGitHubProject("AGPL-3.0")
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: buildDir.resolve("publishing-tmp")
    publication {
        artifact(tasks["buildPlugin"])
    }
}

repositories {
    mavenCentral()
    maven("https://repo.mirai.mamoe.net/snapshots")
}

dependencies {
    testImplementation(kotlin("test"))
    //
    implementation(platform("net.mamoe:mirai-bom:2.15.0-dev-98"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    compileOnly("net.mamoe:mirai-console-compiler-common")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.6"))
    testImplementation("org.slf4j:slf4j-simple")
}

kotlin {
    explicitApi()
}

mirai {
    coreVersion = "2.15.0-dev-98"
    consoleVersion = "2.15.0-dev-98"
}

tasks {
    test {
        useJUnitPlatform()
    }
    register("copyConsoleRuntime") {
        group = "mirai"

        val folder = file("run/console")
        val libs = folder.resolve("libs")
        libs.mkdirs()
        val plugins = folder.resolve("plugins")
        plugins.mkdirs()

        doLast {
            configurations.getByName("testConsoleRuntime").resolve().forEach { lib ->
                val dest = libs.resolve(lib.name)
                dest.delete()
                lib.copyTo(dest, true)
            }
            buildDir.resolve("mirai/fix-protocol-version-${version}.mirai2.jar").let { plugin ->
                val dest = plugins.resolve(plugin.name)
                dest.delete()
                plugin.copyTo(dest, true)
            }
            uri("https://raw.githubusercontent.com/RomiChan/protocol-versions/master/android_pad.json").toURL().let {
                folder.resolve("android_pad.json").writeBytes(it.readBytes())
            }
            """java -D"file.encoding=utf-8" -cp "./libs/*" net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader""".let {
                folder.resolve("start.cmd").writeText(it)
                folder.resolve("start.sh").writeText(it)
            }
            folder.resolve("README.txt").writeText("""
                登陆协议选 ANDROID_PAD
                例如
                login 123456 password ANDROID_PAD
                
                原理是回退到风控较轻的 旧版本协议 8.8.88，顺便这个 8.8.88 的信息实际上是 ANDROID_PHONE 的，所以会顶号
                
                这是独立的开发版本整合包，不能替换 mcl 的文件来使用
                
                整合包 by https://github.com/cssxsh
            """.trimIndent())
        }
    }
}