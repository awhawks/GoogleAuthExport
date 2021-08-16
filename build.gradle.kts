import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.ADOPTOPENJDK)
        implementation.set(JvmImplementation.J9)
    }
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation( kotlin("stdlib") )
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.2")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
