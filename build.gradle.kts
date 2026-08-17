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
    implementation("org.jetbrains.compose.material3:material3-desktop:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-core-desktop:1.7.3")
    implementation("com.google.genai:google-genai:1.66.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

    implementation("org.jetbrains.exposed:exposed-core:1.4.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.4.0")
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")

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

            modules("java.sql")
        }
        jvmArgs += listOf(
            "-Xmx1G",
            "-XX:+UseG1GC"
        )
        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}

tasks.test {
    useJUnitPlatform()
}