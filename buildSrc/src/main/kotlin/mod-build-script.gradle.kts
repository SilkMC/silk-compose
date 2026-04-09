plugins {
    kotlin("jvm")
    id("net.fabricmc.fabric-loom")
}

val excludedDeps: Configuration by configurations.creating

dependencies {
    minecraft(Deps.Fabric.minecraft)
    excludedDeps(implementation(Deps.Fabric.loader)!!)
    excludedDeps(implementation(Deps.Fabric.kotlin)!!)
}
// Disambiguate consumable configurations for Gradle 9.4+ compatibility
configurations.archives {
    attributes.attribute(Attribute.of("temp_disambiguation", String::class.java), "archives")
}
