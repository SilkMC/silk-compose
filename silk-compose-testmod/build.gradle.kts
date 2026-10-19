plugins {
    `mod-build-script`
    id("org.jetbrains.compose")
    kotlin("plugin.compose") version "2.4.20"
}

description = "Testmod for Silk Compose"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperPlugins = configurations.register("paperPlugins") { isTransitive = false }
val bundled = configurations.register("bundled") { isTransitive = false }

dependencies {
    implementation(project(":${rootProject.name}"))
    implementation(Deps.Silk.commands)

    implementation(Deps.ColorMath.jvm)
    bundled(implementation(Deps.ColorPicker.jvm)!!)

    compileOnly("io.papermc.paper:paper-api:$minecraftVersion.build.+")
    paperPlugins(Deps.Silk.paper)
    paperPlugins(project(":${rootProject.name}-paper"))
}

tasks {
    // the color picker is only on the dev classpath on fabric, paper needs it inside the jar
    jar {
        from({ bundled.get().map(::zipTree) })
    }

    register<xyz.jpenilla.runpaper.task.RunServer>("runPaper") {
        group = "paper"
        minecraftVersion(minecraftVersion)
        runDirectory(layout.projectDirectory.dir("run-paper").asFile)
        pluginJars(paperPlugins)
        pluginJars(jar.flatMap { it.archiveFile })
    }
}
