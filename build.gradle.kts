val extra = project.extra;
extra.set("skipList", listOf("alpha", "beta", "m1", "rc", "cr", "m", "preview", "snapshot", "b", "ea"))

subprojects {
  apply<JavaLibraryPlugin>()
  apply<MavenPublishPlugin>()

  group = "io.wisetime"
  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
  }

  configure<PublishingExtension> {
    repositories {
      maven {
        url = uri("artifactregistry://europe-west3-maven.pkg.dev/wise-pub/java")
      }
    }
  }

  repositories {
    mavenCentral()
  }
}
