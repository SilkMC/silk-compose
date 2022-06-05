import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21" apply false
    kotlin("plugin.serialization") version "1.6.21" apply false
    id("com.google.devtools.ksp") version "1.6.21-1.0.5" apply false
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev686" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }

    group = "net.axay"
    version = "1.0.0"

    tasks {
        withType<JavaCompile> {
            options.release.set(17)
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }
    }
}

extra["kotlin.code.style"] = "official"
