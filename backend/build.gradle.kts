plugins {
    kotlin("jvm") version "2.0.21"
    application
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-jre")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    implementation("io.ktor:ktor-server-auth-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-jackson:3.0.0")

    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("com.typesafe:config:1.4.2")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.2.0")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}