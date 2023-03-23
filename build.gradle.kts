plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.22"

    id("net.mamoe.mirai-console") version "2.14.0"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "1.3.0"

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
}

dependencies {
    testImplementation(kotlin("test"))
    //
    implementation(platform("net.mamoe:mirai-bom:2.14.0"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    compileOnly("net.mamoe:mirai-console-compiler-common")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.6"))
    testImplementation("org.slf4j:slf4j-simple")

    // ktor
    implementation("io.ktor:ktor-client-core-jvm:2.2.4")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.2.4")
    // jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    // 编译mirai-core使用的jar包时使用，将依赖打包进jar
    // "shadowLink"("io.ktor:ktor-client-core-jvm")
    // "shadowLink"("io.ktor:ktor-client-okhttp-jvm")
    // "shadowLink"("com.fasterxml.jackson.core:jackson-core")
    // "shadowLink"("com.fasterxml.jackson.module:jackson-module-kotlin")
}

kotlin {
    explicitApi()
}

tasks {
    test {
        useJUnitPlatform()
    }
}