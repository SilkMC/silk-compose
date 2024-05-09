plugins {
    kotlin("jvm")
    id("fabric-loom")
}

val excludedDeps: Configuration by configurations.creating

dependencies {
    minecraft(Deps.Fabric.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
    })
    excludedDeps(modImplementation(Deps.Fabric.loader)!!)
    excludedDeps(modImplementation(Deps.Fabric.kotlin)!!)
}

configurations.namedElements {
    attributes.attribute(Attribute.of("temp_disambiguation", String::class.java), "temp_value")
}
