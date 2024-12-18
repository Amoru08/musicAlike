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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Puede ser FAIL_ON_PROJECT_REPOS o PREFER_SETTINGS
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("app/libs") // Ajusta el directorio según la ubicación de tus archivos .aar
        }
    }

}



rootProject.name = "MusicAlike"
include(":app")
 