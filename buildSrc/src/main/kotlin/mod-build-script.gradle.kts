plugins {
    kotlin("jvm")
    id("fabric-loom")
}

val excludedDeps: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    mappings(loom.layered {
        //mappings("net.fabricmc:yarn:1.19+build.4")
        officialMojangMappings()
    })
    excludedDeps(modImplementation("net.fabricmc:fabric-loader:0.14.9")!!)
    excludedDeps(modImplementation("net.fabricmc.fabric-api:fabric-api:0.59.0+1.19.2")!!)
    excludedDeps(modImplementation("net.fabricmc:fabric-language-kotlin:1.8.1+kotlin.1.7.0")!!)
}
