import java.net.URI

plugins {
    kotlin("multiplatform") version "1.5.30"
    `maven-publish`
    signing
}

group = "org.araqnid.kotlin.arg-parser"
version = "0.1.2"

description = "Command-line arguments parser"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("11"))
    }
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

tasks {
    withType<Jar>().configureEach {
        manifest {
            attributes["Implementation-Title"] = project.description ?: project.name
            attributes["Implementation-Version"] = project.version
        }
    }
}

val javadocJar = tasks.register("javadocJar", Jar::class.java) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set(project.name)
                description.set(project.description)
                licenses {
                    license {
                        name.set("Apache")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                url.set("https://github.com/araqnid/arg-parser")
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/araqnid/arg-parser/issues")
                }
                scm {
                    connection.set("https://github.com/araqnid/arg-parser.git")
                    url.set("https://github.com/araqnid/arg-parser")
                }
                developers {
                    developer {
                        name.set("Steven Haslam")
                        email.set("araqnid@gmail.com")
                    }
                }
            }
        }
    }

    repositories {
        val sonatypeUser: String? by project
        if (sonatypeUser != null) {
            maven {
                name = "OSSRH"
                url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val sonatypePassword: String by project
                credentials {
                    username = sonatypeUser
                    password = sonatypePassword
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
