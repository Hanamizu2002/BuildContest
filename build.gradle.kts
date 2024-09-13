import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    kotlin("jvm") version "2.0.20-Beta1"
    kotlin("kapt") version "2.0.20-Beta1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("eclipse")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

group = "work.alsace"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap") {
        name = "ktor-eap"
    }
}

dependencies {
    // velocity
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    // kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // docker
    implementation("com.github.docker-java:docker-java:3.3.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.0")
    // ktor
    implementation("io.ktor:ktor-server-core:2.3.12") // 核心模块
    implementation("io.ktor:ktor-server-netty:2.3.12") // Netty 服务器引擎
    implementation("io.ktor:ktor-server-auth:2.3.12") // 鉴权模块
    implementation("io.ktor:ktor-server-cors:2.3.12") // CORS 插件
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12") // 内容协商插件
    implementation("io.ktor:ktor-serialization-jackson:2.3.12") // Jackson 序列化
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)

    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets.main.configure { java.srcDir(generateTemplates.map { it.outputs }) }

project.idea.project.settings.taskTriggers.afterSync(generateTemplates)
project.eclipse.synchronizationTasks(generateTemplates)
