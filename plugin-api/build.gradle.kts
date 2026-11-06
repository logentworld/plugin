import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    `java-library`
    `maven-publish`
}

group = "dev.logent"
version = providers.gradleProperty("pluginApiVersion").get()

val sourceCommit = providers.exec {
    commandLine("git", "-C", rootProject.projectDir.absolutePath, "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        referenceDumpDir.set(layout.projectDirectory.dir("api"))
    }
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

tasks.jar {
    inputs.property("sourceCommit", sourceCommit)
    manifest {
        attributes(
            "Implementation-Version" to project.version.toString(),
            "Logent-Plugin-Api-Commit" to sourceCommit.get(),
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginApi") {
            from(components["java"])
            artifactId = "logent-plugin-api"
            pom {
                name.set("Logent Plugin API")
                description.set("Logent 호스트와 기능 플러그인이 공유하는 공개 JVM 계약이에요.")
                url.set("https://github.com/logentworld/plugin")
                properties.put("logent.pluginApi.commit", sourceCommit)
                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                scm {
                    url.set("https://github.com/logentworld/plugin")
                    connection.set("scm:git:https://github.com/logentworld/plugin.git")
                    tag.set("v${project.version}")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/logentworld/plugin")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}

// 로컬 발행도 같은 ABI 기준을 통과해야 해요.
tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn("checkKotlinAbi")
}
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn("checkKotlinAbi")
}
