plugins {
    `mod-build-script`
    id("org.jetbrains.compose")
}

description = "Testmod for Silk Compose"

dependencies {
    implementation(project(":${rootProject.name}", configuration = "namedElements"))
    modImplementation("net.silkmc:silk-commands:1.9.0")

    implementation(compose.desktop.currentOs)
}
