import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    id("org.beryx.jlink") version "2.24.1"
    //id("de.jjohannes.extra-java-module-info") version "0.9"
    application
}

//extraJavaModuleInfo.automaticModule( "kotlin-stdlib-common-1.5.21.jar", "kotlin.stdlib.common")

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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.0-RC")
}

//val compiler = javaToolchains.compilerFor {
//    languageVersion.set( jvmLang )
//    vendor.set(          jvmVend )
//    implementation.set(  jvmImpl )
//}
//
//tasks.withType<KotlinJvmCompile>().configureEach {
//    val jdkHome = compiler.get().metadata.installationPath.asFile.absolutePath
//    println("using JAVA_HOME=${jdkHome}")
//    kotlinOptions.jdkHome = jdkHome
//}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
