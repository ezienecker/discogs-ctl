plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "de.ezienecker"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(23)
}

application {
    mainClass.set("de.ezienecker.MainKt")
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.bundles.mordant)
    implementation(libs.ksoup)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

tasks.processResources {
    filesMatching("application.properties") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}

tasks.distZip {
    dependsOn(tasks.shadowJar)
}

tasks.distTar {
    dependsOn(tasks.shadowJar)
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}
