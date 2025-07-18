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
  id("com.google.cloud.artifactregistry.gradle-plugin")
  id("io.wisetime.versionChecker")
  id("io.freefair.lombok")
}

repositories {
  mavenCentral()
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

dependencies {
  api(project(":wisetime-connector-openapi-gen"))
  api(project(":wisetime-connector-api-client"))
  api(project(":wisetime-connector-template-formatter"))

  api("joda-time:joda-time:2.13.1")
  api("org.thymeleaf:thymeleaf:3.1.3.RELEASE")
  api("org.slf4j:slf4j-api:${LegebuildConst.SLF4J}")

  implementation("org.apache.commons:commons-configuration2:2.12.0") {
    exclude(group = "commons-logging", module = "commons-logging")
  }
  implementation("org.apache.commons:commons-lang3:3.18.0")


  implementation("software.amazon.awssdk:cloudwatchlogs:2.20.157")

  //  required by AWS SDK to log to logback via slf4j
  implementation("org.slf4j:jcl-over-slf4j:${LegebuildConst.SLF4J}")
  implementation("org.slf4j:jul-to-slf4j:${LegebuildConst.SLF4J}")

  // lightweight json lib (no dependencies) https://github.com/ralfstx/minimal-json
  implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")
  implementation("com.fasterxml.jackson.core:jackson-databind:${LegebuildConst.JACKSON_FASTER}")

  @Suppress("GradlePackageUpdate")
  implementation("com.google.guava:guava:${LegebuildConst.GUAVA_VERSION}")

  api("org.apache.commons:commons-collections4:4.5.0")
  @Suppress("GradlePackageUpdate")
  implementation("commons-io:commons-io:2.18.0")

  implementation("org.xerial:sqlite-jdbc:3.50.1.0")
  implementation("org.codejargon:fluentjdbc:1.8.6")

  implementation("commons-codec:commons-codec:1.18.0")

  implementation("ch.qos.logback:logback-core:1.5.17")
  implementation("ch.qos.logback:logback-classic:1.5.17")

  testImplementation("com.github.javafaker:javafaker:1.0.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.3")
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("org.mockito:mockito-core:5.16.1")
  testImplementation("org.assertj:assertj-core:3.27.2")
  testImplementation("io.github.benas:random-beans:3.9.0")
}

publishing {
  publications {
    create<MavenPublication>("Connector") {
      artifactId = project.name
      from(components["java"])
    }
  }
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
