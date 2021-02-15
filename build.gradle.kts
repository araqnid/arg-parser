plugins {
    kotlin("multiplatform") version "1.4.30"
    `maven-publish`
}

group = "org.araqnid.kotlin.arg-parser"
version = "0.1.1"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvm { }
    js {
        nodejs { }
        useCommonJs()
    }
}

dependencies {
    commonMainImplementation(kotlin("stdlib-common"))
    commonTestImplementation(kotlin("test-common"))
    commonTestImplementation(kotlin("test-annotations-common"))
    "jvmMainImplementation"(kotlin("stdlib"))
    "jvmTestImplementation"(kotlin("test-junit"))
    "jsMainImplementation"(kotlin("stdlib-js"))
    "jsTestImplementation"(kotlin("test-js"))
}

publishing {
    repositories {
        maven(url = "https://maven.pkg.github.com/araqnid/arg-parser") {
            name = "github"
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
