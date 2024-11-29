import io.wisetime.version.model.LegebuildConst

plugins {
  java
  `java-library`
  `maven-publish`

  id("com.github.ben-manes.versions")
  id("com.google.cloud.artifactregistry.gradle-plugin")
  id("io.wisetime.versionChecker")
  // to update openapi.generator to > 6.0 null handlers are needed (picked up by unit tests)
  id("org.openapi.generator") version "6.0.0"
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

val openApiUrl = "https://raw.githubusercontent.com/wisetime-io/connect-api-spec/master/spec/openapi.yaml"
val specFile = "$projectDir/openapi.yaml"

tasks {
  register<DefaultTask>("printVersionStr") {
    doLast {
      println(versionStr)
    }
  }

  val openApiGenerate = named("openApiGenerate") {
    inputs.file(specFile)
    doLast {
      // after creating latest auto-generated object model, delete irrelevant files
      delete("${projectDir}/.openapi-generator")
      delete("${projectDir}/pom.xml")
      delete("${projectDir}/src/gen/java/org")
      delete("${projectDir}/src/main/openapi")
      delete("${projectDir}/src/gen/java/io/wisetime/generated/RestApplication.java")
    }
    dependsOn("downloadSpecFile")
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

  sourcesJar {
    dependsOn(openApiGenerate)
  }
}

tasks.register("downloadSpecFile") {
  download(openApiUrl, specFile)
}

fun download(url: String, path: String) {
  val destFile = File(path)
  logger.info("Downloading file: $url -> $destFile")
  ant.invokeMethod("get", mapOf("src" to url, "dest" to destFile))
}

openApiGenerate {
  generatorName.set("jaxrs-spec")
  inputSpec.set(specFile)
  outputDir.set("$projectDir")
  modelPackage.set("io.wisetime.generated.connect")
  generateApiTests.set(false)
  generateModelDocumentation.set(false)
  generateModelTests.set(false)
  verbose.set(false)

  // prefer java8 native over joda time
  configOptions.put(
    "dateLibrary",
    "java8"
  )
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  // Workaround for @javax.annotation.Generated see: github.com/grpc/grpc-java/issues/3633
  implementation("javax.annotation:javax.annotation-api:1.3.2")

  // swagger-annotations and validation-api used in openApiGenerate
  implementation("io.swagger:swagger-annotations:1.6.3")
  implementation("javax.validation:validation-api:2.0.1.Final")
}

publishing {
  publications {
    create<MavenPublication>("OpenApiGen") {
      artifactId = project.name
      from(components["java"])
    }
  }
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
    eachDependency {
      if (requested.group.startsWith("com.fasterxml.jackson")) {
        useVersion(LegebuildConst.JACKSON_FASTER)
      }
    }
  }
}
