description = "FabrikMC Compose brings Kotlin compose-jb to Minecraft"

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("fabric-loom") version "0.12-SNAPSHOT"
    id("org.quiltmc.quilt-mappings-on-loom") version "4.2.0"
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val includeTransitive: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "com.mojang")

    attributes {
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
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

    include(api(project(":${rootProject.name}-mojang-api"))!!)
    ksp(project(":${rootProject.name}-ksp"))

    modApi("net.silkmc:silk-core:1.9.0")

    includeTransitive(api("org.jetbrains.kotlinx:multik-api:0.1.1")!!)
    includeTransitive(api("org.jetbrains.kotlinx:multik-jvm:0.1.1")!!)

    includeTransitive(api("com.github.ajalt.colormath:colormath:3.2.0")!!)

    includeTransitive(api(compose.desktop.common)!!)
    includeTransitive(compose.desktop.linux_x64)
    includeTransitive(compose.desktop.linux_arm64)
    includeTransitive(compose.desktop.windows_x64)
    includeTransitive(compose.desktop.macos_x64)
    includeTransitive(compose.desktop.macos_arm64)

    includeTransitive.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to version,
            "description" to description,
            "githubUrl" to "https://github.com/SilkMC/silk-compose"
        )

        inputs.properties(props)

        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }
}

ksp {
    arg("minecraft-version", "1.19")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
