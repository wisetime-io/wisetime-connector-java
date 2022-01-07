plugins {
  java
  `java-library`
  `maven-publish`

  id("com.github.ben-manes.versions")
  id("io.wisetime.versionChecker")
  id("org.openapi.generator") version "4.3.1"
}

group = "io.wisetime"
val versionInfo: io.wisetime.version.GitVersionCalc.WiFiGitVersionInfo =
  io.wisetime.version.GitVersionCalc.getVersionInfoForLibrary(project.rootDir)
val branchName: String = versionInfo.branchName
val versionStr: String = versionInfo.gitVersionStr
project.version = versionStr

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
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
}

openApiGenerate {
  generatorName.set("jaxrs-spec")
  inputSpec.set("https://raw.githubusercontent.com/wisetime-io/connect-api-spec/43a1a539f97d325d5221895f4a18cf86b74be412/spec/openapi.yaml")
  outputDir.set("$projectDir")
  modelPackage.set("io.wisetime.generated.connect")
  generateApiTests.set(false)
  generateModelDocumentation.set(false)
  generateModelTests.set(false)
  verbose.set(false)
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

  // Workaround for @javax.annotation.Generated see: github.com/grpc/grpc-java/issues/3633
  implementation("javax.annotation:javax.annotation-api:1.3.2")

  // swagger-annotations and validation-api used in openApiGenerate
  implementation("io.swagger:swagger-annotations:1.6.3")
  implementation("javax.validation:validation-api:2.0.1.Final")
}

val taskRequestString = gradle.startParameter.taskRequests.toString()
if (taskRequestString.contains("publish")) {
  apply(from = "$rootDir/gradle/publish_s3_repo.gradle")
}

sourceSets {
  main {
    java {
      // include auto-generated model files from spec in additional to our main bundle
      srcDir("src/gen/java")
    }
  }
}

configurations.all {
  resolutionStrategy {

    force(
      "com.fasterxml.jackson.core:jackson-databind:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "com.fasterxml.jackson.core:jackson-core:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
      "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:${io.wisetime.version.model.LegebuildConst.JACKSON_FASTER}",
    )
  }
}