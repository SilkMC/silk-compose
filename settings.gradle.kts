rootProject.name = "silk-compose"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.fabricmc.net/")
    }
}

include(":${rootProject.name}")
include(":${rootProject.name}-ksp")
include(":${rootProject.name}-mojang-api")
include(":${rootProject.name}-testmod")
