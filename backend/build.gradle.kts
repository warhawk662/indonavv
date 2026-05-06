plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    kotlin("plugin.serialization")
}

group = "com.example.indonavv"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.4.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.4.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.4.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.4.1")
    implementation("io.ktor:ktor-server-cors-jvm:3.4.1")
    implementation("io.ktor:ktor-server-websockets-jvm:3.4.1")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("com.google.firebase:firebase-admin:9.2.0")
}

ktor {
    application {
        mainClass.set("com.example.indonavv.backend.ApplicationKt")
    }
}
