import io.wisetime.version.GitVersionCalc
import io.wisetime.version.GitVersionCalc.WiFiGitVersionInfo
import io.wisetime.version.model.LegebuildConst
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
  withSourcesJar()
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

  withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
      candidate.version.isNonStable()
    }
  }

}

checkstyle {
  configFile = File("${project.rootDir}/gradle/conf/checkstyle.xml")
  configProperties["checkstyleConfigDir"] = File("${project.rootDir}/gradle/conf")
  configProperties["suppressionFile"] = File("${project.rootDir}/gradle/conf/checkstyle_suppressions.xml")
  toolVersion = "8.45.1"
}

val slf4jVersion = LegebuildConst.SLF4J
dependencies {
  api(project(":wisetime-connector-openapi-gen"))

  api("org.slf4j:slf4j-api:$slf4jVersion")
  api("org.apache.httpcomponents:httpclient:4.5.14") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
  }

  implementation("org.apache.commons:commons-lang3:3.12.0")

  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

  @Suppress("GradlePackageUpdate")
  implementation("com.google.guava:guava:${LegebuildConst.GUAVA_VERSION}")

  implementation("org.apache.httpcomponents:httpcore:4.4.16")
  implementation("org.apache.httpcomponents:fluent-hc:4.5.14") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
  }

  implementation("commons-codec:commons-codec:1.15")

  implementation("org.slf4j:jcl-over-slf4j:$slf4jVersion")
  implementation("org.slf4j:jul-to-slf4j:$slf4jVersion")

  implementation("ch.qos.logback:logback-core:1.4.7")
  implementation("ch.qos.logback:logback-classic:1.4.7")

  testImplementation("com.github.javafaker:javafaker:1.0.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.1")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
  testImplementation("org.mockito:mockito-core:5.3.1")
  testImplementation("org.assertj:assertj-core:3.24.2")
}

val taskRequestString = gradle.startParameter.taskRequests.toString()
if (taskRequestString.contains("publish")) {
  apply(from = "$rootDir/gradle/publish_s3_repo.gradle")
}

configurations.all {
  resolutionStrategy {
    eachDependency {
      if (requested.group.startsWith("com.fasterxml.jackson")) {
        useVersion(LegebuildConst.JACKSON_FASTER)
      }
      if (requested.group == "joda-time") {
        useVersion(LegebuildConst.JODA_TIME)
      }
      if (requested.group == "org.slf4j") {
        useVersion(LegebuildConst.SLF4J)
      }
    }
  }
}

fun String.isNonStable(): Boolean {
  @Suppress("UNCHECKED_CAST") val skipList = rootProject.ext["skipList"] as List<String>
  for (skipItem in skipList) {
    if (this.lowercase().contains(skipItem)) {
      return true
    }
  }
  return false
}
