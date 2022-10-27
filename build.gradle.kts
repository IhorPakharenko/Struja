val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("io.ktor.plugin") version "2.1.2"
}

group = "com.isao"
version = "0.0.1"
application {
    mainClass.set("com.isao.application.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

//    implementation "io.ktor:ktor-server-core:2.1.2"
//    implementation "io.ktor:ktor-server-netty:2.1.2"
//    implementation "io.ktor:ktor-server-status-pages:2.1.2"
//    implementation "io.ktor:ktor-server-default-headers:2.1.2"

    implementation("it.skrape:skrapeit:1.2.2")

    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.7")

    val exposed_version = "0.40.1"
    val h2_version = "2.1.214"
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("com.h2database:h2:$h2_version")


    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}