plugins {
    kotlin("jvm") version "2.0.21"
    application
}


dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    // Don't add routing separately - it's included in core
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("com.typesafe:config:1.4.2")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Forces JDK 21 for Gradle
    }
}

kotlin {
    jvmToolchain(21) // Forces Kotlin compiler to target JVM 21
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
