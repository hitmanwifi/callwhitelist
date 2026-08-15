import org.gradle.api.initialization.resolve.RepositoriesMode

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

rootProject.name = "WhiteList"

include(":app")
include(":core:model")
include(":core:database")
include(":core:preferences")
include(":core:designsystem")
include(":domain")
include(":data")
include(":callfilter")
