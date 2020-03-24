plugins {
    kotlin("multiplatform") version "1.3.71"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

group = "org.araqnid.kotlin.arg-parser"
version = "0.0.2"

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

tasks.withType<com.jfrog.bintray.gradle.tasks.BintrayUploadTask> {
    doFirst {
        publishing.publications
            .filterIsInstance<MavenPublication>()
            .forEach { publication ->
                val moduleFile = buildDir.resolve("publications/${publication.name}/module.json")
                if (moduleFile.exists()) {
                    publication.artifact(object : org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact(moduleFile) {
                        override fun getDefaultExtension() = "module"
                    })
                }
            }
    }
}

bintray {
    user = (project.properties["bintray.user"] ?: "").toString()
    key = (project.properties["bintray.apiKey"] ?: "").toString()
    publish = true
    setPublications("js", "jvm", "kotlinMultiplatform", "metadata")
    pkg.repo = "maven"
    pkg.name = "arg-parser"
    pkg.setLicenses("Apache-2.0")
    pkg.vcsUrl = "https://github.com/araqnid/arg-parser"
    pkg.desc = "Command-line parser for Kotlin"
    if (project.version != Project.DEFAULT_VERSION) {
        pkg.version.name = project.version.toString()
        pkg.version.vcsTag = "v" + project.version
    }
}
