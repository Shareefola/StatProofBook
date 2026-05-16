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
    implementation(project(":proofengine"))
    implementation(project(":core"))

    // Serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.coroutines.core)

    // Unit Tests
    testImplementation(libs.bundles.testing.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
