pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "indonavv"

if (System.getenv("RENDER") == "true" || System.getenv("DOCKER_BUILD") == "true") {
    println("Build detected as Cloud/Docker - Including only backend")
    include(":backend")
} else {
    include(":app")
    include(":backend")
    include(":admin-app")
    include(":core")
}
