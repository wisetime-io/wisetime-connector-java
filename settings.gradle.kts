pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            setUrl("https://s3.eu-central-1.amazonaws.com/artifacts.wisetime.com/mvn2/plugins")
            content {
                includeGroup("io.wisetime.versionChecker")
            }
        }
    }
}

rootProject.name = "wisetime-connector"
