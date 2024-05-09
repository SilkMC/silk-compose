plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

description = "Silk Compose Mojang API extracts Minecraft assets from the client jar"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(Deps.KotlinX.Serialization.json)
}
