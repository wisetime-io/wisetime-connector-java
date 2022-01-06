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
  api(project(":wisetime-connector-api-client"))
  api(project(":wisetime-connector-template-formatter"))

  api("joda-time:joda-time")
  api("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
  api("org.slf4j:slf4j-api:1.7.32")

  implementation("org.apache.commons:commons-configuration2:2.4") {
    exclude(group = "commons-logging", module = "commons-logging")
  }
  implementation("org.apache.commons:commons-lang3:3.12.0")

  // AWS dependencies
  implementation("com.amazonaws:aws-java-sdk-logs:1.12.62") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "joda-time", module = "joda-time")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
  }

  implementation("com.amazonaws:aws-java-sdk-cloudwatch:1.12.62") {
    exclude(group = "commons-logging", module = "commons-logging")
  }

  //  required by AWS SDK to log to logback via slf4j
  implementation("org.slf4j:jcl-over-slf4j:1.7.32")
  implementation("org.slf4j:jul-to-slf4j:1.7.32")

  // lightweight json lib (no dependencies) https://github.com/ralfstx/minimal-json
  implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")

  @Suppress("GradlePackageUpdate")
  implementation("com.google.guava:guava:30.1-jre")

  api("org.apache.commons:commons-collections4:4.4")
  @Suppress("GradlePackageUpdate")
  implementation("commons-io:commons-io:2.8.0")

  implementation("org.xerial:sqlite-jdbc:3.36.0.2")
  implementation("org.codejargon:fluentjdbc:1.8.6")

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
  testImplementation("io.github.benas:random-beans:3.9.0")
}

sourceSets {
  main {
    java {
      srcDir("src/main/java")
    }
  }
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
      "joda-time:joda-time:${io.wisetime.version.model.LegebuildConst.JODA_TIME}",
      "org.apache.commons:commons-lang3:3.12.0",
      "com.fasterxml.jackson.core:jackson-databind:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "com.fasterxml.jackson.core:jackson-core:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "commons-codec:commons-codec:1.12",
      "org.slf4j:slf4j-api:1.7.32",
      "org.apache.httpcomponents:httpclient:4.5.9"
    )
  }
}
