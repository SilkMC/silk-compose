plugins {
    `mod-build-script`
    id("org.jetbrains.compose")
    kotlin("plugin.compose") version "2.3.20"
}

description = "Testmod for Silk Compose"

dependencies {
    implementation(project(":${rootProject.name}"))
    implementation(Deps.Silk.commands)

    implementation(Deps.ColorMath.jvm)
    implementation(Deps.ColorPicker.jvm)
}
