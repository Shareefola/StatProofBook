plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // Serialization for proof JSON models
    implementation(libs.serialization.json)

    // Coroutines (for async proof generation)
    implementation(libs.coroutines.core)

    // Unit Tests
    testImplementation(libs.bundles.testing.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
