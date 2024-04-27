plugins {
    kotlin("jvm") version "1.9.23"
    application
    kotlin("plugin.serialization") version "1.9.22"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.varabyte.kotter:kotter-jvm:1.1.2")
    testImplementation("com.varabyte.kotterx:kotter-test-support-jvm:1.1.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("pyfixer.MainKt")
}