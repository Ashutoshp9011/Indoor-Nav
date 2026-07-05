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

// Local OpenCV Android SDK module — imported as a full Gradle module rather
// than the Maven AAR, since the AAR is missing the stitching module you need
// for PanoramaStitcher.kt. Path assumes you've dropped the OpenCV Android SDK
// folder at Corridor360/sdk/ — adjust if yours lives elsewhere.
include(":opencv")
project(":opencv").projectDir = File(rootDir, "sdk")