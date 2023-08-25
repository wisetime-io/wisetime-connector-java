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

  api("org.slf4j:slf4j-api:${io.wisetime.version.model.LegebuildConst.SLF4J}")
  implementation("ch.qos.logback:logback-core:1.4.7")
  implementation("ch.qos.logback:logback-classic:1.4.7")

  @Suppress("GradlePackageUpdate")
  implementation("com.google.guava:guava:${io.wisetime.version.model.LegebuildConst.GUAVA_VERSION}")

  implementation("com.fasterxml.jackson.core:jackson-databind")

  api("joda-time:joda-time:${io.wisetime.version.model.LegebuildConst.JODA_TIME}")

  // required by activity text template engine
  implementation("org.freemarker:freemarker:2.3.32")

  testImplementation("com.github.javafaker:javafaker:1.0.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.1")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.mockito:mockito-core:5.5.0")
  testImplementation("org.assertj:assertj-core:3.24.2")
}

publishing {
  publications {
    create<MavenPublication>("Formatter") {
      artifactId = project.name
      from(components["java"])
    }
  }
}

configurations.all {
  resolutionStrategy {

    force(
      "com.fasterxml.jackson.core:jackson-databind:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "org.slf4j:slf4j-api:${io.wisetime.version.model.LegebuildConst.SLF4J}",
    )
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
