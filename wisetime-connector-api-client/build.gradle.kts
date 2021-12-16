import io.wisetime.version.GitVersionCalc
import io.wisetime.version.GitVersionCalc.WiFiGitVersionInfo
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  java
  `java-library`
  jacoco
  `maven-publish`
  checkstyle

  id("com.github.ben-manes.versions")
  id("io.wisetime.versionChecker")
  id("io.freefair.lombok")
}

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

apply(from = "${project.rootDir}/gradle/conf/jacoco.gradle")
group = "io.wisetime"

val versionInfo: WiFiGitVersionInfo = GitVersionCalc.getVersionInfoForLibrary(project.rootDir)
val branchName: String = versionInfo.branchName
val versionStr: String = versionInfo.gitVersionStr
project.version = versionStr

tasks {
  register<DefaultTask>("printVersionStr") {
    doLast {
      println(versionStr)
    }
  }

  clean {
    delete("${projectDir}/out")
    delete("${projectDir}/pom.xml")
  }

  test {
    useJUnitPlatform()
    testLogging {
      events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
  }

  checkstyleMain {
    exclude("com/google/**", "**/generated/**")
  }

  jar {
    manifest {
      attributes("Implementation-Version" to project.version)
    }
  }
}

checkstyle {
  configFile = File("${project.rootDir}/gradle/conf/checkstyle.xml")
  configProperties["checkstyleConfigDir"] = File("${project.rootDir}/gradle/conf")
  configProperties["suppressionFile"] = File("${project.rootDir}/gradle/conf/checkstyle_suppressions.xml")
  toolVersion = "8.45.1"
}

dependencies {
  api(project(":wisetime-connector-openapi-gen"))

  api("org.slf4j:slf4j-api:1.7.32")
  api("org.apache.httpcomponents:httpclient:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
  }

  implementation("org.apache.commons:commons-lang3:3.12.0")

  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

  @Suppress("GradlePackageUpdate")
  implementation("com.google.guava:guava:30.1-jre")

  implementation("org.apache.httpcomponents:httpcore:4.4.11")
  implementation("org.apache.httpcomponents:fluent-hc:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
  }
  @Suppress("GradlePackageUpdate")
  implementation("commons-codec:commons-codec:1.12")

  implementation("ch.qos.logback:logback-core:1.2.5")
  implementation("ch.qos.logback:logback-classic:1.2.5")

  testImplementation("com.github.javafaker:javafaker:1.0.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
  testImplementation("org.mockito:mockito-core:3.12.4")
  testImplementation("org.assertj:assertj-core:3.21.0")
}

val taskRequestString = gradle.startParameter.taskRequests.toString()
if (taskRequestString.contains("publish")) {
  apply(from = "$rootDir/gradle/publish_s3_repo.gradle")
}
if (taskRequestString.contains("dependencyUpdates")) {
  // add exclusions for reporting on updates and vulnerabilities
  apply(from = "$rootDir/gradle/versionPluginConfig.gradle")
}

configurations.all {
  resolutionStrategy {

    force(
      "org.apache.commons:commons-lang3:3.12.0",
      "com.fasterxml.jackson.core:jackson-databind:2.12.3",
      "com.fasterxml.jackson.core:jackson-core:2.12.3",
      "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.12.3",
      "commons-codec:commons-codec:1.12",
      "org.slf4j:slf4j-api:1.7.32",
    )
  }
}
