plugins {
    `mod-build-script`
    id("org.jetbrains.compose")
}

description = "Testmod for Silk Compose"

dependencies {
    implementation(project(":${rootProject.name}", configuration = "developmentElements"))
    modImplementation(Deps.Silk.commands)

    implementation(Deps.ColorMath.jvm)
    implementation(Deps.ColorPicker.jvm)
}
