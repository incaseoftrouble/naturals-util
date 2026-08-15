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
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
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
        ),
    )
  }
}

tasks.javadoc {
  (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.test {
  useJUnitPlatform()
  maxHeapSize = "10g"
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
  api("com.github.spotbugs:spotbugs-annotations:4.10.3")
  // https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
  api("it.unimi.dsi:fastutil:8.5.19")
  // https://mvnrepository.com/artifact/com.zaxxer/SparseBitSet
  api("com.zaxxer:SparseBitSet:1.3")
  // https://mvnrepository.com/artifact/org.roaringbitmap/RoaringBitmap
  api("org.roaringbitmap:RoaringBitmap:1.6.20")

  // https://mvnrepository.com/artifact/org.hamcrest/hamcrest
  testImplementation("org.hamcrest:hamcrest:2.2")
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.4")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")

  // https://mvnrepository.com/artifact/com.google.guava/guava-testlib
  testImplementation("com.google.guava:guava-testlib:32.1.3-jre")
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  testImplementation("org.junit.vintage:junit-vintage-engine:5.14.4")

  implementation("org.jspecify:jspecify:1.0.0") // Apache 2.0
  // https://mvnrepository.com/artifact/com.google.errorprone/error_prone_core
  errorprone("com.google.errorprone:error_prone_core:2.50.0")
  // https://mvnrepository.com/artifact/com.uber.nullaway/nullaway
  errorprone("com.uber.nullaway:nullaway:0.13.8")
}

spotless {
  java {
    // https://central.sonatype.com/artifact/com.palantir.javaformat/palantir-java-format
    palantirJavaFormat("2.89.0")
  }
  kotlinGradle {
    ktlint()
    ktfmt()
  }
}

jmh {
  // The benchmarks only need the main source set; pulling in the test classpath here breaks resolution.
  includeTests.set(false)
}

nullaway {
  // Replace with your project's root package
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

tasks.withType<Pmd> {
  reports {
    xml.required.set(false)
    html.required.set(true)
  }
}

// Deployment - run with -Prelease clean publishToSonatype closeAndReleaseSonatypeStagingRepository
// Key: signing.gnupg.keyName in ~/.gradle/gradle.properties
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
