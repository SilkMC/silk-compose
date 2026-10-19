plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
}

description = "Silk Compose for Paper"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

evaluationDependsOn(":${rootProject.name}")

val includeInJar = configurations.register("includeInJar") { isTransitive = false }

@Suppress("UNCHECKED_CAST")
val bundledDeps = project(":${rootProject.name}").extra["bundledDeps"] as List<String>

dependencies {
    paperweight.paperDevBundle("$minecraftVersion.build.+")
    compileOnly(Deps.Silk.paper)
    compileOnly(Deps.KotlinX.MultiK.jvm)

    includeInJar(compileOnly(project(":${rootProject.name}"))!!)
    includeInJar(project(":${rootProject.name}-mojang-api"))
    bundledDeps.forEach { includeInJar(it) }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to version,
            "description" to description,
            "mcVersion" to minecraftVersion,
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    // ponytail: fat jar instead of a PluginLoader, paper's library classloader can't see silk-paper's kotlin
    jar {
        dependsOn(includeInJar)
        from({ includeInJar.get().filter { it.name.endsWith("jar") }.map(::zipTree) }) {
            exclude(
                "fabric.mod.json", "*.mixins.json", "*-refmap.json", "META-INF/jars/**", "net/silkmc/silk/compose/mixin/**",
                "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "**/module-info.class",
            )
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
