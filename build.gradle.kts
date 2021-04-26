import io.wisetime.version.GitVersionCalc
import io.wisetime.version.GitVersionCalc.WiFiGitVersionInfo

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
}

apply(from = "$rootDir/gradle/conf/checkstyle.gradle")
apply(from = "$rootDir/gradle/conf/jacoco.gradle")
group = "io.wisetime"

val versionInfo: WiFiGitVersionInfo = GitVersionCalc.getVersionInfoForLibrary(project.rootDir)
val branchName: String = versionInfo.branchName
val versionStr: String = versionInfo.gitVersionStr
project.version = versionStr

tasks.register<DefaultTask>("printVersionStr") {
    doLast {
        println(versionStr)
    }
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

apply(from = "$rootDir/build_base.gradle")
