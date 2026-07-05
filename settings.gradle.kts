pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Corridor360"

include(":corridor360")
include(":entrance-mapper")

// Local OpenCV Android SDK module
include(":opencv")
project(":opencv").projectDir = File(rootDir, "sdk")
