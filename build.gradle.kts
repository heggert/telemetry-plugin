import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
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
    // Dagger runtime and annotation processor
    implementation("com.google.dagger:dagger:2.56.1")
    annotationProcessor("com.google.dagger:dagger-compiler:2.56.1")

    // Logging & Paper API
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.withType<ShadowJar> {
    // We set baseName to "Telemetry" and version to project.version, no classifier.
    archiveBaseName.set("Telemetry")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Optionally add a manifest entry for Main-Class
    manifest {
        attributes["Main-Class"] = "art.chibi.telemetry.TelemetryPlugin"
    }

    // Enable minimization to remove unused classes
    minimize()
}
