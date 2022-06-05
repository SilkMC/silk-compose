rootProject.name = "silk-compose"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.fabricmc.net/")
        maven("https://server.bbkr.space/artifactory/libs-release/")
        maven("https://maven.quiltmc.org/repository/release/")
    }
}

include(":${rootProject.name}")
include(":${rootProject.name}-ksp")
include(":${rootProject.name}-mojang-api")
include(":${rootProject.name}-resolver")
