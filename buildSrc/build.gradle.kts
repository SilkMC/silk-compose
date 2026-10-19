plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.quiltmc.org/repository/release")
    maven("https://repo.pauli.fyi/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    fun pluginDep(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"

    val kotlinVersion = "2.4.20"

    compileOnly(kotlin("gradle-plugin", embeddedKotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))
    compileOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", embeddedKotlinVersion))
    runtimeOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))

    implementation(pluginDep("net.fabricmc.fabric-loom", "1.15-SNAPSHOT"))
    implementation(pluginDep("com.modrinth.minotaur", "2.9.0"))

    implementation(pluginDep("io.papermc.paperweight.userdev", "2.0.0-beta.23"))
    implementation(pluginDep("xyz.jpenilla.run-paper", "3.1.0"))

    val compileDokkaVersion = "2.2.0"
    val dokkaVersion = "2.2.0"

    compileOnly("org.jetbrains.dokka:dokka-gradle-plugin:$compileDokkaVersion")
    runtimeOnly("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    compileOnly("org.jetbrains.dokka:dokka-base:$compileDokkaVersion")
    runtimeOnly("org.jetbrains.dokka:dokka-base:$dokkaVersion")
}
