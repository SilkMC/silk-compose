plugins {
    kotlin("jvm")
    id("fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19")
    mappings(loom.layered {
        //mappings("net.fabricmc:yarn:1.19+build.4")
        officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:0.14.8")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.58.0+1.19")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.8.1+kotlin.1.7.0")
}
