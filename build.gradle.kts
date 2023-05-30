val extra = project.extra;
extra.set("skipList", listOf("alpha", "beta", "m1", "rc", "cr", "m", "preview", "snapshot", "b", "ea"))

subprojects {
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
