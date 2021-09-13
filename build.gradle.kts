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
  id("org.openapi.generator") version "4.2.1"
}

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

apply(from = "$projectDir/gradle/conf/jacoco.gradle")
group = "io.wisetime"

val versionInfo: WiFiGitVersionInfo = GitVersionCalc.getVersionInfoForLibrary(project.rootDir)
val branchName: String = versionInfo.branchName
val versionStr: String = versionInfo.gitVersionStr
project.version = versionStr

openApiGenerate {
  generatorName.set("jaxrs-spec")
  inputSpec.set("https://raw.githubusercontent.com/wisetime-io/connect-api-spec/160911f5793ff39c591284247dfea35d60d48ee5/spec/openapi.yaml")
  outputDir.set("$projectDir")
  modelPackage.set("io.wisetime.generated.connect")
  generateApiTests.set(false)
  generateModelDocumentation.set(false)
  generateModelTests.set(false)
  verbose.set(false)
}

tasks {
  register<DefaultTask>("printVersionStr") {
    doLast {
      println(versionStr)
    }
  }

  val openApiGenerate = named("openApiGenerate") {
    onlyIf { !File("${projectDir.absolutePath}/src/gen/java").exists() }
    doLast {
      // after creating latest auto-generated object model, delete irrelevant files
      delete("${projectDir}/.openapi-generator")
      delete("${projectDir}/pom.xml")
      delete("${projectDir}/src/gen/java/org")
      delete("${projectDir}/src/main/openapi")
      delete("${projectDir}/src/gen/java/io/wisetime/generated/RestApplication.java")
    }
  }

  compileJava {
    dependsOn(openApiGenerate)
  }

  clean {
    delete("${projectDir}/out")
    delete("${projectDir}/.openapi-generator")
    delete("${projectDir}/pom.xml")
    delete("${projectDir}/src/gen/java")
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
  configFile = File("${projectDir}/gradle/conf/checkstyle.xml")
  configProperties["checkstyleConfigDir"] = File("${projectDir}/gradle/conf")
  configProperties["suppressionFile"] = File("${projectDir}/gradle/conf/checkstyle_suppressions.xml")
  toolVersion = "8.45.1"
}

dependencies {

  api("joda-time:joda-time:2.10.10")
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

  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
  implementation("com.google.guava:guava:30.1-jre")

  api("org.apache.commons:commons-collections4:4.4")
  implementation("commons-io:commons-io:2.8.0")

  implementation("org.xerial:sqlite-jdbc:3.36.0.2")
  implementation("org.codejargon:fluentjdbc:1.8.6")

  implementation("org.apache.httpcomponents:httpclient:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
  }

  implementation("org.apache.httpcomponents:httpcore:4.4.11")
  implementation("org.apache.httpcomponents:fluent-hc:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
  }
  implementation("commons-codec:commons-codec:1.12")

  implementation("ch.qos.logback:logback-core:1.2.5")
  implementation("ch.qos.logback:logback-classic:1.2.5")

  // required by activity text template engine
  implementation("org.freemarker:freemarker:2.3.31")

  // swagger-annotations and validation-api used in openApiGenerate
  implementation("io.swagger:swagger-annotations:1.6.2")
  implementation("javax.validation:validation-api:2.0.1.Final")

  testImplementation("com.github.javafaker:javafaker:1.0.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
  testImplementation("org.mockito:mockito-core:3.12.4")
  testImplementation("org.assertj:assertj-core:3.20.2")
  testImplementation("io.github.benas:random-beans:3.9.0")
}

sourceSets {
  main {
    java {
      // include auto-generated model files from spec in additional to our main bundle
      srcDir("src/gen/java")
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
    // fail eagerly on version conflict (includes transitive dependencies)
    // e.g. multiple different versions of the same dependency (group and name are equal)
    // used to check and determine conflicts
    failOnVersionConflict()

    force(
      "joda-time:joda-time:2.10.10",
      "org.apache.commons:commons-lang3:3.12.0",
      "com.fasterxml.jackson.core:jackson-databind:2.12.3",
      "com.fasterxml.jackson.core:jackson-core:2.12.3",
      "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.12.3",
      "commons-codec:commons-codec:1.12",
      "org.objenesis:objenesis:3.0.1",
      "org.slf4j:slf4j-api:1.7.32",
      "org.apache.httpcomponents:httpclient:4.5.9"
    )
  }
}
