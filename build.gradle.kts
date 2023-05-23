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
        browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasm { d8() }
}

tasks.withType<ShadowJar> {
    manifest {
        attributes("Main-Class" to "CLI")
    }
    val main by kotlin.jvm().compilations
    from(main.output)
    configurations += main.compileDependencyFiles as Configuration
    configurations += main.runtimeDependencyFiles as Configuration
}
