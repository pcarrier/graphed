plugins {
    kotlin("multiplatform").version("1.8.21")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka").version("1.6.21")
}

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "kotlin-multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    group = "com.pcarrier.graphed"
    version = "0.2.0-SNAPSHOT"

    kotlin {
        jvm {}
        js(IR) {
            browser()
            nodejs()
        }

        @Suppress("UNUSED_VARIABLE")
        sourceSets {
            val commonTest by getting {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }

    val dokkaJar by tasks.creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(tasks.dokkaHtml)
    }

    publishing {
        publications {
            repositories {
                maven {
                    name = "oss"
                    url = if (version.toString().endsWith("SNAPSHOT"))
                        uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    else uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("SONATYPE_USERNAME")
                        password = System.getenv("SONATYPE_PASSWORD")
                    }
                }
            }

            withType<MavenPublication> {
                artifact(dokkaJar)

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

    // Please the Gradle gods.
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}
