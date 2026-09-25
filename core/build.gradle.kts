// Pure Kotlin/JVM module: SMS parsing, analytics and formatting.
// No Android dependencies, so it builds and tests on any JVM (and in CI) quickly.
plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.expensetracker"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
