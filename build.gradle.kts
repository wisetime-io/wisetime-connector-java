val extra = project.extra;
extra.set("skipList", listOf("alpha", "beta", "m1", "rc", "cr", "m", "preview", "snapshot", "b", "ea"))

subprojects {
  apply<JavaLibraryPlugin>()
  apply<MavenPublishPlugin>()

  group = "io.wisetime"
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
  }

  configure<PublishingExtension> {
    repositories {
      maven {
        url = uri("artifactregistry://europe-west3-maven.pkg.dev/wise-pub/java")
        credentials {
          username = "_json_key_base64"
          password = System.getenv("PUBLISH_KEY")
        }
        authentication {
          create<BasicAuthentication>("basic")
        }
      }
    }
  }

  repositories {
    mavenCentral()
    maven {
      // WiseTime artifacts
      setUrl("https://s3.eu-central-1.amazonaws.com/artifacts.wisetime.com/mvn2/releases")
      content {
        includeGroup("io.wisetime")
      }
    }
  }
}
