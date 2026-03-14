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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        // JitPack (for PDF viewer)
        maven("https://jitpack.io")

        // Readium repository
        maven("https://s01.oss.sonatype.org/content/repositories/releases/")
    }
}
rootProject.name = "Book"
include(":app")