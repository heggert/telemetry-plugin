import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.32"
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "art.chibi"
version = "dev"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")

    // Dagger runtime and annotation processor.
    implementation("com.google.dagger:dagger:2.56.1")
    annotationProcessor("com.google.dagger:dagger-compiler:2.56.1")
    ksp("com.google.dagger:dagger-compiler:2.56.1")

    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
    // Paper API is provided at runtime by the server.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.withType<ShadowJar> {
    // Set the classifier; this will typically result in a jar named like TelemetryPlugin-all.jar.
    archiveClassifier.set("all")

    // Optionally add a manifest entry for Main-Class.
    manifest {
        attributes["Main-Class"] = "art.chibi.telemetry.TelemetryPlugin"
    }

    // Enable minimization to reduce the jar size by removing unused classes.
    minimize()
}