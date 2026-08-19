import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
  `java-library`

  pmd
  idea

  // https://plugins.gradle.org/plugin/com.diffplug.spotless
  id("com.diffplug.spotless") version "8.9.0"
  // https://plugins.gradle.org/plugin/net.ltgt.errorprone
  id("net.ltgt.errorprone") version "5.1.0"
  // https://plugins.gradle.org/plugin/net.ltgt.nullaway
  id("net.ltgt.nullaway") version "3.1.0"
  // https://plugins.gradle.org/plugin/me.champeau.jmh
  id("me.champeau.jmh") version "0.7.3"

  `maven-publish`
  signing
  // https://plugins.gradle.org/plugin/io.github.gradle-nexus.publish-plugin
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "de.tum.in"

version = "0.20.0"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11

  withSourcesJar()
  withJavadocJar()
}

tasks.jar {
  manifest {
    attributes(
        mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "de.tum.in.naturals",
        ),
    )
  }
}

tasks.javadoc {
  (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.test {
  useJUnitPlatform {
    excludeTags("slow")
  }
}

tasks.register<Test>("testSlow") {
  group = "verification"
  description =
      "Runs the full test suite, including tests tagged \"slow\" (e.g. NatBitSetTheories). Not part of `check`."
  useJUnitPlatform()
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
}

tasks.withType<Test> {
  minHeapSize = "1g"
  maxHeapSize = "8g"
  testLogging {
    events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
  }
  reports.html.required = false
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
  // Source: https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
  // Pinned to the last release whose junit-bom is 5.x; 4.10.x forces JUnit 6, which needs Java 17
  api("com.github.spotbugs:spotbugs-annotations:4.9.6")
  // https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
  api("it.unimi.dsi:fastutil:8.5.19")
  // https://mvnrepository.com/artifact/org.roaringbitmap/RoaringBitmap
  api("org.roaringbitmap:RoaringBitmap:1.6.20")

  // https://mvnrepository.com/artifact/org.hamcrest/hamcrest
  testImplementation("org.hamcrest:hamcrest:2.2")
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.4")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
  // Gradle 9 no longer injects the launcher
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")

  // https://mvnrepository.com/artifact/com.google.guava/guava-testlib
  testImplementation("com.google.guava:guava-testlib:32.1.3-jre")
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  testImplementation("org.junit.vintage:junit-vintage-engine:5.14.4")

  // https://mvnrepository.com/artifact/org.jetbrains/annotations
  compileOnly("org.jetbrains:annotations:26.1.0") // Apache 2.0

  implementation("org.jspecify:jspecify:1.0.0") // Apache 2.0
  // https://mvnrepository.com/artifact/com.google.errorprone/error_prone_core
  errorprone("com.google.errorprone:error_prone_core:2.50.0")
  // https://mvnrepository.com/artifact/com.uber.nullaway/nullaway
  errorprone("com.uber.nullaway:nullaway:0.13.8")
}

jmh {
  includeTests = false
}

nullaway {
  annotatedPackages.add("de.tum.in.naturals")
  jspecifyMode = true
}

tasks.withType<JavaCompile> {
  options.errorprone {
    disable(
        "ArrayRecordComponent",
        "EffectivelyPrivate",
        "StringSplitter",
        "ReferenceEquality",
        "OperatorPrecedence",
        "UnusedVariable",
    )

    nullaway {
      assertsEnabled = true
    }
  }
}

spotless {
  java {
    // https://central.sonatype.com/artifact/com.palantir.javaformat/palantir-java-format
    palantirJavaFormat("2.89.0")
    licenseHeader("// SPDX-License-Identifier: Apache-2.0\n\n")
  }
  kotlinGradle {
    ktlint()
    ktfmt()
  }
}

// PMD
// https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.Pmd.html

pmd {
  toolVersion = "7.26.0" // https://pmd.github.io/
  reportsDir = project.layout.buildDirectory.dir("reports/pmd").get().asFile
  ruleSetFiles = project.layout.projectDirectory.files("config/pmd-rules.xml")
  ruleSets = listOf() // We specify all rules in rules.xml
  isConsoleOutput = false
  isIgnoreFailures = false
}

// Benchmarks legitimately do things PMD dislikes: explicit GC, console output, tight hand-rolled
// loops
tasks.named("pmdJmh") { enabled = false }

tasks.withType<Pmd> {
  reports {
    xml.required = false
    html.required = true
  }
}

// Deployment - run with:
//   -Prelease --no-configuration-cache clean publishToSonatype
// closeAndReleaseSonatypeStagingRepository
// Key: signing.gnupg.keyName in ~/.gradle/gradle.properties
// Authentication: sonatypeUsername+sonatypePassword in ~/.gradle/gradle.properties
//   Central portal user token (https://central.sonatype.com/usertoken)
if (project.hasProperty("release")) {
  publishing {
    publications {
      create<MavenPublication>("mavenJava") {
        from(project.components["java"])

        pom {
          name = "naturals-util"
          description = "Datastructures and utility classes for non-negative integers"
          url = "https://github.com/incaseoftrouble/naturals-util"

          licenses {
            license {
              name = "The GNU General Public License, Version 3"
              url = "https://www.gnu.org/licenses/gpl.txt"
            }
          }

          developers {
            developer {
              id = "incaseoftrouble"
              name = "Tobias Meggendorfer"
              email = "tobias@meggendorfer.de"
              url = "https://github.com/incaseoftrouble"
              timezone = "Europe/Berlin"
            }
          }

          scm {
            connection = "scm:git:https://github.com/incaseoftrouble/naturals-util.git"
            developerConnection = "scm:git:git@github.com:incaseoftrouble/naturals-util.git"
            url = "https://github.com/incaseoftrouble/naturals-util"
          }
        }
      }
    }
  }

  nexusPublishing {
    repositories {
      sonatype {
        nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
        snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
      }
    }
  }

  signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
  }
}
