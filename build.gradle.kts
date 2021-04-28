import io.wisetime.version.GitVersionCalc
import io.wisetime.version.GitVersionCalc.WiFiGitVersionInfo
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  java
  maven
  application
  `maven-publish`

  id("io.codearte.nexus-staging") version "0.30.0"
  id("com.github.ben-manes.versions") version "0.38.0"
  id("io.wisetime.versionChecker") version "10.11.46"
  id("de.marcphilipp.nexus-publish").version("0.3.1").apply(false)
  id("org.openapi.generator") version "4.2.1"
}

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
  withSourcesJar()
}

apply(from = "$rootDir/gradle/conf/checkstyle.gradle")
apply(from = "$rootDir/gradle/conf/jacoco.gradle")
group = "io.wisetime"

val versionInfo: WiFiGitVersionInfo = GitVersionCalc.getVersionInfoForLibrary(project.rootDir)
val branchName: String = versionInfo.branchName
val versionStr: String = versionInfo.gitVersionStr
project.version = versionStr

openApiGenerate {
  generatorName.set("jaxrs-spec")
  inputSpec.set("https://raw.githubusercontent.com/wisetime-io/connect-api-spec/8808e4404cfd2fcf58cfe8b89d9ccf1cefb2002a/spec/openapi.yaml")
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

  jar {
    manifest {
      attributes("Implementation-Version" to project.version)
    }
  }
}

dependencies {

  compile("com.sparkjava:spark-core:2.9.1")
  compile("com.sparkjava:spark-template-thymeleaf:2.7.1") {
    exclude(group = "com.sparkjava", module = "spark-core")
    exclude(group = "org.thymeleaf", module = "thymeleaf")
    exclude(group = "org.slf4j", module = "slf4j-api")
  }
  compile("org.thymeleaf:thymeleaf:3.0.11.RELEASE")

  compile("joda-time:joda-time:2.10.10")

  compile("org.apache.commons:commons-configuration2:2.4") {
    exclude(group = "commons-logging", module = "commons-logging")
  }
  compile("org.apache.commons:commons-lang3:3.11")
  compile("net.jodah:failsafe:1.1.0")

  compileOnly("org.projectlombok:lombok:1.18.20")
  annotationProcessor("org.projectlombok:lombok:1.18.20")
  testCompileOnly("org.projectlombok:lombok:1.18.20")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.20")

  // AWS dependencies
  compile("com.amazonaws:aws-java-sdk-logs:1.11.611") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "joda-time", module = "joda-time")
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
  }

  compile("com.amazonaws:aws-java-sdk-cloudwatch:1.11.611") {
    exclude(group = "commons-logging", module = "commons-logging")
  }

  //  required by AWS SDK to log to logback via slf4j
  compile("org.slf4j:jcl-over-slf4j:1.7.30")
  compile("org.slf4j:jul-to-slf4j:1.7.30")

  // lightweight json lib (no dependencies) https://github.com/ralfstx/minimal-json
  compile("com.eclipsesource.minimal-json:minimal-json:0.9.5")

  compile("com.fasterxml.jackson.core:jackson-databind")
  compile("com.fasterxml.jackson.core:jackson-core")
  compile("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

  compile("org.apache.commons:commons-collections4:4.4")
  compile("com.google.guava:guava:30.1-jre")
  compile("commons-io:commons-io:2.8.0")

  compile("org.xerial:sqlite-jdbc:3.28.0")
  compile("org.codejargon:fluentjdbc:1.8.6")

  compile("org.apache.httpcomponents:httpclient:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
  }

  compile("org.apache.httpcomponents:httpcore:4.4.11")
  compile("org.apache.httpcomponents:fluent-hc:4.5.9") {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "commons-codec", module = "commons-codec")
  }
  compile("commons-codec:commons-codec:1.12")

  compile("ch.qos.logback:logback-core:1.2.3")
  compile("ch.qos.logback:logback-classic:1.2.3")
  compile("org.slf4j:slf4j-api:1.7.30")

  // required by activity text template engine
  compile("org.freemarker:freemarker:2.3.28")

  // swagger-annotations and validation-api used in openApiGenerate
  compile("io.swagger:swagger-annotations:1.5.3")
  compile("javax.validation:validation-api:2.0.1.Final")

  testCompile("com.github.javafaker:javafaker:0.17.2") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  testCompile("org.skyscreamer:jsonassert:1.5.0")
  testCompile("org.junit.jupiter:junit-jupiter:5.7.1")
  testCompile("org.mockito:mockito-core:3.6.0")
  testCompile("org.assertj:assertj-core:3.18.0")
  testCompile("io.github.benas:random-beans:3.9.0")
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
      "org.slf4j:slf4j-api:1.7.30",
      "org.apache.httpcomponents:httpclient:4.5.9"
    )
  }
}
