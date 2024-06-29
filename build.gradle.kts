plugins {
    kotlin("jvm") version "1.7.21"
    application
}
application {
    mainClass.set("MainKt")
}

group = "org.traderepublic.candlesticks"
version = "1.1.4"

repositories {
    mavenCentral()
}

object DependencyVersions {
    const val coroutines = "1.6.4"
    const val http4k = "4.34.0.3"
    const val jackson = "2.14.0"
    const val mockk = "1.13.2"
    const val logback = "1.2.11"
    const val fakerVersion = "1.9.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))

    implementation(platform("org.http4k:http4k-bom:${DependencyVersions.http4k}"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-netty")
    implementation("org.http4k:http4k-client-websocket")
    implementation("org.http4k:http4k-format-jackson")
    implementation("redis.clients:jedis:5.1.2")
    implementation("io.github.config4k:config4k:0.4.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.coroutines}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${DependencyVersions.jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${DependencyVersions.jackson}")
    implementation("ch.qos.logback:logback-classic:${DependencyVersions.logback}")

    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.31")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation("org.slf4j:slf4j-api:1.7.30")

    testImplementation("io.mockk:mockk:${DependencyVersions.mockk}")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("io.github.serpro69:kotlin-faker:${DependencyVersions.fakerVersion}")
}
tasks {
    distZip {
        dependsOn(installDist)
    }
}

tasks.test {
    useJUnitPlatform()
}
