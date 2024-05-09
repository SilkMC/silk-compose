import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Deps.Ksp.plugin) version Deps.Ksp.version apply false
    id("org.jetbrains.compose") version Deps.Compose.version apply false
    id("org.jetbrains.dokka")
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://androidx.dev/storage/compose-compiler/repository")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    group = "net.silkmc"
    version = "1.0.4"

    description = "Silk Compose brings Kotlin Compose Multiplatform to Minecraft"

    tasks {
        withType<JavaCompile> {
            options.release.set(21)
        }
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf("-Xcontext-receivers", "-Xskip-prerelease-check")
            }
        }
    }

    configurations.all {
        attributes {
            attribute(Attribute.of("ui", String::class.java), "awt")
        }
    }
}

extra["kotlin.code.style"] = "official"

tasks {
    withType<DokkaMultiModuleTask> {
        includes.from("dokka/includes/main.md")
    }
}
