plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.21-1.0.5")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    implementation(project(":${rootProject.name}-mojang-api"))
    implementation("org.slf4j:slf4j-simple:1.7.36")
}
