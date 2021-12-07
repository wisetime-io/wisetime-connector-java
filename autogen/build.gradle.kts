plugins {
  java
  `java-library`

  id("io.wisetime.versionChecker")
  id("org.openapi.generator") version "4.3.1"
}

group = "io.wisetime"

repositories {
    mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks {
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
    // fail eagerly on version conflict (includes transitive dependencies)
    // e.g. multiple different versions of the same dependency (group and name are equal)
    // used to check and determine conflicts
    failOnVersionConflict()

    force(
      "com.fasterxml.jackson.core:jackson-databind:2.12.3",
      "com.fasterxml.jackson.core:jackson-core:2.12.3",
      "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.12.3",
    )
  }
}
