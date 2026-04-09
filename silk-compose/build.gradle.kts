import org.jetbrains.compose.compose

plugins {
    `mod-build-script`
    `project-publish-script`
    `mod-upload-script`
    `dokka-script`
    id(Deps.Ksp.plugin)
    id("org.jetbrains.compose")
    kotlin("plugin.compose") version "2.3.20"
}

val includeTransitive: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "com.mojang")
}

val excludedDeps: Configuration by configurations.getting

// workaround for project dependencies on this module (needed for the testmod)
// adding compileOnly here is a workaround for a different issue where includes won't get loaded
configurations {
    register("developmentElements") {
        extendsFrom(implementation.get(), api.get(), compileOnly.get())
    }
}

dependencies {
    ksp(project(":${rootProject.name}-ksp"))
    include(compileOnly(project(":${rootProject.name}-mojang-api"))!!)

    api(Deps.Silk.core)

    includeTransitive(implementation(Deps.KotlinX.MultiK.jvm)!!)
    includeTransitive(implementation(Deps.ColorMath.jvm)!!)

    listOf(
        "org.jetbrains.compose.desktop:desktop:1.10.3",
        "org.jetbrains.compose.material3:material3:1.9.0-beta03",
        "org.jetbrains.compose.material:material-icons-extended:1.7.3"
    ).forEach {
        includeTransitive(api(it)!!)
    }

    listOf(
        "org.jetbrains.compose.desktop:desktop-jvm-linux-x64:1.10.3",
        "org.jetbrains.compose.desktop:desktop-jvm-linux-arm64:1.10.3",
        "org.jetbrains.compose.desktop:desktop-jvm-windows-x64:1.10.3",
        "org.jetbrains.compose.desktop:desktop-jvm-macos-x64:1.10.3",
        "org.jetbrains.compose.desktop:desktop-jvm-macos-arm64:1.10.3",
    ).forEach {
        includeTransitive(implementation(it)!!)
    }

    val excludedModules = excludedDeps.resolvedConfiguration.resolvedArtifacts
        .map { it.moduleVersion.id.run { group to name } }

    includeTransitive.resolvedConfiguration.resolvedArtifacts.forEach {
        val id = it.moduleVersion.id
        if (!excludedModules.contains(id.group to id.name)) {
            println("Including $id")
            include(id.toString())
        }
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
    arg("minecraft-version", minecraftVersion)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }

        all {
            languageSettings {
                optIn("net.silkmc.silk.core.annotations.DelicateSilkApi")
                optIn("net.silkmc.silk.core.annotations.InternalSilkApi")
            }
        }
    }
}
