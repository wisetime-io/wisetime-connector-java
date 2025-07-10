pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.52.0"
    id("io.wisetime.versionChecker") version "10.13.65"
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.1"
    id("io.freefair.lombok") version "8.12.1"
  }
  repositories {
    gradlePluginPortal()
    maven {
      setUrl("https://europe-west3-maven.pkg.dev/wise-pub/gradle")
      content {
        includeGroup("io.wisetime.versionChecker")
      }
    }
  }
}

rootProject.name = "wisetime-connector-java"
include(":wisetime-connector-openapi-gen")
include(":wisetime-connector")
include(":wisetime-connector-api-client")
include(":wisetime-connector-template-formatter")
