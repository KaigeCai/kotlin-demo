@file:Suppress("SpellCheckingInspection")

plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.demo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("commons-io:commons-io:2.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}