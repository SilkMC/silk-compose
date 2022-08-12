plugins {
    `mod-build-script`
    `project-publish-script`
    `mod-upload-script`
    `dokka-script`
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
}

val includeTransitive: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "com.mojang")
}

val excludedDeps: Configuration by configurations.getting

// workaround for project dependencies on this module (needed for the testmod)
configurations {
    register("developmentElements") {
        extendsFrom(implementation.get(), namedElements.get(), api.get())
    }
}

dependencies {
    ksp(project(":${rootProject.name}-ksp"))
    include(compileOnly(project(":${rootProject.name}-mojang-api"))!!)

    modApi("net.silkmc:silk-core:1.9.0")

    includeTransitive(implementation("org.jetbrains.kotlinx:multik-default-jvm:0.2.0")!!)
    includeTransitive(implementation("com.github.ajalt.colormath:colormath-jvm:3.2.0")!!)

    includeTransitive(api(compose.desktop.common)!!)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    includeTransitive(api(compose.material3)!!)

    includeTransitive(implementation(compose.desktop.linux_x64)!!)
    includeTransitive(implementation(compose.desktop.linux_arm64)!!)
    includeTransitive(implementation(compose.desktop.windows_x64)!!)
    includeTransitive(implementation(compose.desktop.macos_x64)!!)
    includeTransitive(implementation(compose.desktop.macos_arm64)!!)

    val excludedModules = excludedDeps.resolvedConfiguration.resolvedArtifacts
        .map { it.moduleVersion.id.run { group to name } }

    includeTransitive.resolvedConfiguration.resolvedArtifacts.forEach {
        val id = it.moduleVersion.id
        if (!excludedModules.contains(id.group to id.name)) {
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
    arg("minecraft-version", "1.19")
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
