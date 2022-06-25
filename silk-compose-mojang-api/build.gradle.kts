plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

description = "FabrikMC Compose Mojang API extracts Minecraft assets from the client jar"

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}
