val githubRepo = "SilkMC/silk-compose"
val minecraftVersion = "26.1"

object Deps {
    // https://fabricmc.net/develop/
    // https://jakobk.net/mcdev
    object Fabric {
        val minecraft = "com.mojang:minecraft:$minecraftVersion"
        val loader = "net.fabricmc:fabric-loader:0.19.0"
        val kotlin = "net.fabricmc:fabric-language-kotlin:1.13.10+kotlin.2.3.20"
    }

    // https://github.com/SilkMC/silk
    object Silk {
        val silkVersion = "1.11.6"
        val core = "net.silkmc:silk-core:$silkVersion"
        val commands = "net.silkmc:silk-commands:$silkVersion"
    }

    object KotlinX {
        // https://github.com/Kotlin/multik
        object MultiK {
            val version = "0.2.3"
            val jvm = "org.jetbrains.kotlinx:multik-default-jvm:$version"
        }

        // https://github.com/Kotlin/kotlinx.serialization
        object Serialization {
            val version = "1.10.0"
            val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:$version"
        }
    }

    // https://github.com/JetBrains/compose-multiplatform
    object Compose {
        val version = "1.10.3"
    }

    // https://github.com/ajalt/colormath
    object ColorMath {
        val version = "3.6.1"
        val jvm = "com.github.ajalt.colormath:colormath-jvm:$version"
    }

    // https://github.com/google/ksp
    object Ksp {
        val version = "2.3.6"
        val plugin = "com.google.devtools.ksp"
        val symbolProcessingApi = "com.google.devtools.ksp:symbol-processing-api:$version"

        // https://square.github.io/kotlinpoet/interop-ksp/
        val kotlinPoetExtension = "com.squareup:kotlinpoet-ksp:2.3.0"
    }

    object Logging {
        // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
        val slf4jSimple = "org.slf4j:slf4j-simple:2.0.17"
    }

    // https://github.com/godaddy/compose-color-picker
    object ColorPicker {
        val version = "0.7.0"
        val jvm = "com.godaddy.android.colorpicker:compose-color-picker-jvm:$version"
    }
}
