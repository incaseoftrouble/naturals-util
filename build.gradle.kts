plugins {
    `java-library`

    pmd
    checkstyle
    idea

    `maven-publish`
    signing

    // https://plugins.gradle.org/plugin/io.github.gradle-nexus.publish-plugin
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

group = "de.tum.in"
version = "0.17.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()
}

var defaultEncoding = "UTF-8"
tasks.withType<JavaCompile> { options.encoding = defaultEncoding }
tasks.withType<Javadoc> { options.encoding = defaultEncoding }
tasks.withType<Test> { systemProperty("file.encoding", "UTF-8") }

tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        ))
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.google.code.findbugs/jsr305
    api("com.google.code.findbugs", "jsr305", "3.0.2")
    // https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    api("it.unimi.dsi", "fastutil", "8.5.8")
    // https://mvnrepository.com/artifact/com.zaxxer/SparseBitSet
    api("com.zaxxer", "SparseBitSet", "1.2")
    // https://mvnrepository.com/artifact/org.roaringbitmap/RoaringBitmap
    api("org.roaringbitmap", "RoaringBitmap", "0.9.24")

    // https://mvnrepository.com/artifact/org.hamcrest/hamcrest
    testImplementation("org.hamcrest", "hamcrest", "2.2")
    // https://mvnrepository.com/artifact/com.google.guava/guava-testlib
    // testImplementation("com.google.guava", "guava-testlib", "31.1-jre")
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.8.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "10g"
}

// PMD
// https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.Pmd.html

pmd {
    toolVersion = "6.42.0" // https://pmd.github.io/
    reportsDir = file("${project.buildDir}/reports/pmd")
    ruleSetFiles = files("${project.rootDir}/config/pmd-rules.xml")
    ruleSets = listOf() // We specify all rules in rules.xml
    isConsoleOutput = false
    isIgnoreFailures = false
}

tasks.withType<Pmd> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

// Checkstyle
// https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.Checkstyle.html
checkstyle {
    toolVersion = "9.3" // http://checkstyle.sourceforge.net/releasenotes.html
    configFile = file("${project.rootDir}/config/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
    isShowViolations = false // Don't litter console
}
tasks.checkstyleMain {
    configProperties = mapOf("suppression-file" to "${project.rootDir}/config/checkstyle-main-suppression.xml")
}
tasks.checkstyleTest {
    configProperties = mapOf("suppression-file" to "${project.rootDir}/config/checkstyle-test-suppression.xml")
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

// Deployment - run with -Prelease clean publishToSonatype closeAndReleaseSonatypeStagingRepository
// Authentication: sonatypeUsername+sonatypePassword in ~/.gradle/gradle.properties
if (project.hasProperty("release")) {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(project.components["java"])

                signing {
                    useGpgCmd()
                    sign(publishing.publications)
                }

                pom {
                    name.set("naturals-util")
                    description.set("Datastructures and utility classes for non-negative integers")
                    url.set("https://github.com/incaseoftrouble/naturals-util")

                    licenses {
                        license {
                            name.set("The GNU General Public License, Version 3")
                            url.set("https://www.gnu.org/licenses/gpl.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("incaseoftrouble")
                            name.set("Tobias Meggendorfer")
                            email.set("tobias@meggendorfer.de")
                            url.set("https://github.com/incaseoftrouble")
                            timezone.set("Europe/Berlin")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://github.com/incaseoftrouble/naturals-util.git")
                        developerConnection.set("scm:git:git@github.com:incaseoftrouble/naturals-util.git")
                        url.set("https://github.com/incaseoftrouble/naturals-util")
                    }
                }
            }
        }
    }

    nexusPublishing {
        repositories {
            sonatype()
        }
    }
}