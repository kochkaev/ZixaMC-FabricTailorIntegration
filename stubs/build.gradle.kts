plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    id("fabric-loom") version "1.10.1"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven { url = uri("https://libraries.minecraft.net/") }
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    gradlePluginPortal()
}
loom {
    // Отключаем генерацию fabric.mod.json и обработку как мода
//    generateModJson = false
}
dependencies {
    minecraft("net.minecraft:minecraft:1.21.5")
    mappings("net.fabricmc:yarn:1.21.5+build.1:v2")
//    modImplementation("net.fabricmc:fabric-api:0.102.0+1.21")
}
sourceSets {
    main {
        java {
            srcDir("java")
        }
    }
}
tasks {
    // Отключаем задачу remapJar, чтобы не создавать JAR мода
    remapJar {
        enabled = false
    }
    // Отключаем генерацию resources для мода, если они не нужны
    processResources {
        enabled = false
    }
}