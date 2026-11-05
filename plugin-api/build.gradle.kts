import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    `maven-publish`
}

group = "dev.logent"
version = "2.0.0"

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

publishing {
    publications {
        create<MavenPublication>("pluginApi") {
            from(components["java"])
            artifactId = "logent-plugin-api"
        }
    }
}
