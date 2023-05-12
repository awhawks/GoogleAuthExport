plugins {
    kotlin("jvm")                  version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.beryx.jlink")          version "2.24.1"
    application
}

jlink {
    mergedModule {
        additive = true
    }
    launcher {
        name = "OtpDecode"
    }
    imageZip.set( file("build/image-zip/OtpDecode-image.zip") )
}

application {
    mainModule.set( "com.hawkstech.otpauth" )
    mainClass.set( "com.hawkstech.otpauth.Decode" )
}

val jvmLang = JavaLanguageVersion.of( 11 )
val jvmVend = JvmVendorSpec.ADOPTOPENJDK
val jvmImpl = JvmImplementation.J9

java {
    modularity.inferModulePath.set(true)
    toolchain {
        languageVersion.set( jvmLang )
        vendor.set(          jvmVend )
        implementation.set(  jvmImpl )
    }
}

kotlin {
    jvmToolchain {
        val jts = this as JavaToolchainSpec
        jts.languageVersion.set( jvmLang )
        jts.vendor.set(          jvmVend )
        jts.implementation.set(  jvmImpl )
    }
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation( kotlin("stdlib") )
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
