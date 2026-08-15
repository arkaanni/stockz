plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

group = "dev.arkaan"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("com.google.genai:google-genai:1.66.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("org.bouncycastle:bctls-jdk18on:1.78.1")
    implementation("org.conscrypt:conscrypt-openjdk-uber:2.5.2")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "dev.arkaan.AppKt"
        nativeDistributions {
            packageName = "stockz"
            packageVersion = "1.0.0"
        }
        jvmArgs += listOf(
            "-Xmx384m",
            "-XX:+UseG1GC"
        )
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}