plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":${rootProject.name}-mojang-api"))

    implementation(Deps.Ksp.symbolProcessingApi)
    implementation(Deps.Ksp.kotlinPoetExtension)
    implementation(Deps.Logging.slf4jSimple)
}
