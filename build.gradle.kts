import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

repositories {
    mavenCentral()
}

plugins {
    kotlin("multiplatform").version("1.8.21")
    java
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

kotlin {
    jvm {}
    js(IR) {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasm { d8() }

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

tasks.withType<ShadowJar> {
    manifest {
        attributes("Main-Class" to "com.pcarrier.graphed.graphql.ParseAndPrint")
    }
    val main by kotlin.jvm().compilations
    from(main.output)
    configurations += main.compileDependencyFiles as Configuration
    configurations += main.runtimeDependencyFiles as Configuration
}
