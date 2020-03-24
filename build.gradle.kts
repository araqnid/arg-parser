plugins {
    kotlin("multiplatform") version "1.4-M1"
    `maven-publish`
}

group = "org.araqnid.kotlin.arg-parser"
version = "0.0.0"

repositories {
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvm { }
    js {
        nodejs { }
    }
    linuxX64 {
        binaries {
            executable {
                entryPoint = "org.araqnid.kotlin.argv.main"
            }
        }
    }
    macosX64 {
        binaries {
            executable {
                entryPoint = "org.araqnid.kotlin.argv.main"
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
        kotlinOptions.moduleKind = "commonjs"
    }

    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib-common"))
    }

    sourceSets["commonTest"].dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
    }

    sourceSets["jvmMain"].dependencies {
        implementation(kotlin("stdlib"))
    }

    sourceSets["jvmTest"].dependencies {
        implementation(kotlin("test-junit"))
    }

    sourceSets["jsMain"].dependencies {
        implementation(kotlin("stdlib-js"))
    }

    sourceSets["jsTest"].dependencies {
        implementation(kotlin("test-js"))
    }
}

dependencies {
}
