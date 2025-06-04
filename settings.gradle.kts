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

// 🤖 AUTO-FIX: Updated dependency resolution for Gradle 8.11.1
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        
        // EyeDid SDK repository (updated URL)
        maven {
            url = uri("https://seeso.jfrog.io/artifactory/visualcamp-eyedid-sdk-android-release")
        }
        
        // EyeDid SDK GitHub repository (backup)
        maven {
            url = uri("https://maven.pkg.github.com/visualcamp/eyedid-android-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("USERNAME")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("TOKEN")).orNull
            }
        }
    }
}

rootProject.name = "EyedidSampleApp"
include(":app")