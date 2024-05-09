plugins {
    `mod-build-script`
    `project-publish-script`
    `mod-upload-script`
    `dokka-script`
    id(Deps.Ksp.plugin)
    id("org.jetbrains.compose")
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
        extendsFrom(namedElements.get(), implementation.get(), api.get(), compileOnly.get())
    }
}

dependencies {
    ksp(project(":${rootProject.name}-ksp"))
    include(compileOnly(project(":${rootProject.name}-mojang-api"))!!)

    modApi(Deps.Silk.core)

    includeTransitive(implementation(Deps.KotlinX.MultiK.jvm)!!)
    includeTransitive(implementation(Deps.ColorMath.jvm)!!)

    listOf(
        compose.desktop.common,
        compose.material3,
    ).forEach {
        includeTransitive(api(it)!!)
    }

    listOf(
        compose.desktop.linux_x64,
        compose.desktop.linux_arm64,
        compose.desktop.windows_x64,
        compose.desktop.macos_x64,
        compose.desktop.macos_arm64,
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
            }
        }
    }
}
