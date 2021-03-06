plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

description = "Silk Compose Mojang API extracts Minecraft assets from the client jar"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}
