plugins {
    kotlin("multiplatform") version "1.4.0-rc"
    `maven-publish`
}

group = "org.araqnid.kotlin.arg-parser"
version = "0.1.0"

repositories {
    jcenter()
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

publishing {
    val bintrayUser = (project.properties["bintray.user"] ?: "").toString()
    val bintrayKey = (project.properties["bintray.apiKey"] ?: "").toString()
    repositories {
        maven(url = "https://api.bintray.com/maven/araqnid/maven/arg-parser/;publish=1") {
            name = "bintray"
            credentials {
                username = bintrayUser
                password = bintrayKey
            }
        }
    }
}
