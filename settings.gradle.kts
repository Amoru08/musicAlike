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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Ensures no project-level repositories are defined.
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("app/libs") // Make sure your .aar files are in this directory.
        }
    }
}

rootProject.name = "MusicAlike"
include(":app")
