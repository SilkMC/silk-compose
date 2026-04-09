import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        maven("https://repo.pauli.fyi/releases")
    }

    group = "net.silkmc"
    version = "1.1.0"

    description = "Silk Compose brings Kotlin Compose Multiplatform to Minecraft"

    tasks {
        withType<JavaCompile> {
            options.release.set(25)
        }
        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_25
                freeCompilerArgs.addAll("-Xcontext-parameters")
            }
        }
    }

    configurations.configureEach {
        if (isCanBeResolved || isCanBeConsumed) {
            attributes {
                attribute(Attribute.of("ui", String::class.java), "awt")
            }
        }
    }
}

extra["kotlin.code.style"] = "official"

tasks {
    dokka {
        dokkaSourceSets.configureEach {
            includes.from("dokka/includes/main.md")
        }
    }
}
