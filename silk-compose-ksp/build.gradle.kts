plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.0-1.0.6")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
    implementation(project(":${rootProject.name}-mojang-api"))
    implementation("org.slf4j:slf4j-simple:1.7.36")
}
