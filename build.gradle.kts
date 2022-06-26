import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.devtools.ksp") version "1.7.0-1.0.6" apply false
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev725" apply false
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
    version = "1.0.0"

    tasks {
        withType<JavaCompile> {
            options.release.set(17)
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }
    }

    configurations.all {
        attributes {
            attribute(Attribute.of("ui", String::class.java), "awt")
        }

        resolutionStrategy.dependencySubstitution {
            substitute(module("org.jetbrains.compose.compiler:compiler")).apply {
                using(module("androidx.compose.compiler:compiler:1.2.0-dev-k1.7.0-53370d83bb1"))
            }
        }
    }
}

extra["kotlin.code.style"] = "official"

tasks {
    withType<DokkaMultiModuleTask> {
        includes.from("dokka/includes/main.md")
    }
}
