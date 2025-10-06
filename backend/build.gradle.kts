plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.example.backend"
version = "1.0.0"

dependencies {
    // --- Ktor core dependencies ---
    implementation("io.ktor:ktor-server-core-jvm:3.0.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")

    // --- Logging ---
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // --- Database (PostgreSQL + Exposed ORM) ---
    implementation("org.jetbrains.exposed:exposed-core:0.50.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.0")
    implementation("org.postgresql:postgresql:42.7.3")

    // --- Testing ---
    testImplementation(kotlin("test"))
}

application {
    // ✅ Match your Application.kt file
    mainClass.set("com.example.backend.ApplicationKt")
}
