import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform").version("1.8.21")
    id("maven-publish")
    id("signing")
}

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "kotlin-multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = "com.pcarrier.graphed"
    version = "0.1.0-SNAPSHOT"

    kotlin {
        jvm {}
        js(IR) {
            browser()
            nodejs()
        }

        @Suppress("UNUSED_VARIABLE")
        sourceSets {
            val jvmTest by getting {
                dependencies {
                    implementation("com.graphql-java:graphql-java:20.2")
                    implementation("org.jline:jline:3.23.0")
                }
            }

            val commonTest by getting {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }

    publishing {
        publications {
            repositories {
                maven {
                    name = "oss"
                    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }

            withType<MavenPublication> {
                pom {
                    name.set(project.name)
                    description.set("GraphQL and co.")
                    licenses {
                        license {
                            name.set("0BSD")
                            url.set("https://opensource.org/license/0bsd/")
                        }
                    }
                    url.set("https://github.com/pcarrier/graphed")
                    issueManagement {
                        system.set("Github")
                        url.set("https://github.com/pcarrier/graphed/issues")
                    }
                    scm {
                        connection.set("https://github.com/pcarrier/graphed.git")
                        url.set("https://github.com/pcarrier/graphed")
                    }
                    developers {
                        developer {
                            name.set("Pierre Carrier")
                            email.set("pc@rrier.fr")
                        }
                    }
                }
            }
        }
    }

    signing {
        useInMemoryPgpKeys(
            "988CC12E",
            System.getenv("GPG_PRIVATE_KEY")?.let { File(it).readText() },
            System.getenv("GPG_PRIVATE_PASSWORD")
        )
        sign(publishing.publications)
    }
}
