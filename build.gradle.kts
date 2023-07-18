plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"

    id("net.mamoe.mirai-console") version "2.15.0"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "1.9.7"

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
    implementation(platform("net.mamoe:mirai-bom:2.15.0"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    compileOnly("net.mamoe:mirai-console-compiler-common")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    //
    implementation("org.asynchttpclient:async-http-client:2.12.3")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.7"))
    testImplementation("org.slf4j:slf4j-simple")
}

kotlin {
    explicitApi()
}

mirai {
    coreVersion = "2.15.0"
    consoleVersion = "2.15.0"
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
            """
                @echo off
                setlocal
                set JAVA_BINARY="java"
                if exist "java-home" set JAVA_BINARY=".\java-home\bin\java.exe"
                
                %JAVA_BINARY% -version
                %JAVA_BINARY% -D"file.encoding=utf-8" -cp "./libs/*" "net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader"
                
                set EL=%ERRORLEVEL%
                if %EL% NEQ 0 (
                    echo Process exited with %EL%
                    pause
                )
            """.trimIndent().let {
                folder.resolve("start.cmd").writeText(it)
            }
            """
                java -D"file.encoding=utf-8" -cp "./libs/*" net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
            """.trimIndent().let {
                folder.resolve("start.sh").writeText(it)
            }
            """
                登陆协议选 ANDROID_PAD
                例如
                login 123456 password ANDROID_PAD
                
                原理是回退到风控较轻的 旧版本协议 8.8.88，顺便这个 8.8.88 的信息实际上是 ANDROID_PHONE 的，所以会顶号
                
                这是独立的开发版本整合包，不能替换 mcl 的文件来使用
                
                如果出现 'java' 不是内部或外部命令，也不是可运行的程序
                说明你没有安装好 Java，你可以选择本地补充安装
                
                本地补充安装（Windows）：
                
                自行二选一下载，然后解压全部文件 到 start.cmd 同目录，并将 jdk-17.0.7+7-jre 重命名为 java-home
                
                windows x64 版本
                https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jre/x64/windows/OpenJDK17U-jre_x64_windows_hotspot_17.0.7_7.zip
                
                windows x86 版本
                https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jre/x32/windows/OpenJDK17U-jre_x86-32_windows_hotspot_17.0.7_7.zip
                
                目录结构如下
                ├───java-home (本地java目录，注意要解压全部文件，这里仅列出部分目录结构)
                │   └───bin
                │       └───java.exe
                ├───start.cmd (WIN启动脚本)
                ├───start.sh (Linux/MACOS启动脚本)
                ├───libs (库目录，里面有mirai本体)
                ├───logs (日志目录，里面有日志文件)
                ├───plugins (插件目录)
                └───android_pad.json (协议版本信息)
                
                整合包 by https://github.com/cssxsh
            """.trimIndent().let {
                folder.resolve("README.txt").writeText(it)
            }
        }
    }
}