plugins {
    kotlin("jvm")
    id("fabric-loom")
    id("org.quiltmc.quilt-mappings-on-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19")
    mappings(loom.layered {
        addLayer(quiltMappings.mappings("org.quiltmc:quilt-mappings:1.19+build.1:v2"))
        officialMojangMappings()
    })
    modImplementation("net.fabricmc:fabric-loader:0.14.8")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.56.3+1.19")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.8.0+kotlin.1.7.0")
}
