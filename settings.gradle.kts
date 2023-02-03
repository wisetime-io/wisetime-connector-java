pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.45.0"
    id("io.wisetime.versionChecker") version "10.12.64"
    id("io.freefair.lombok") version "6.6.1"
  }
  repositories {
    gradlePluginPortal()
    google()
    maven {
      setUrl("https://s3.eu-central-1.amazonaws.com/artifacts.wisetime.com/mvn2/plugins")
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
