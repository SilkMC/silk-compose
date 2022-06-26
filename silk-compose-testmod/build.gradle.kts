plugins {
    `mod-build-script`
    id("org.jetbrains.compose")
}

description = "Testmod for Silk Compose"

dependencies {
    modImplementation(project(":${rootProject.name}"))
    modImplementation("net.silkmc:silk-commands:1.9.0")

    implementation(compose.desktop.currentOs)
}
