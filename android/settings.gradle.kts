pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IGNGmc"
include(
    ":app",
    ":core:model",
    ":core:designsystem",
    ":core:network",
    ":core:auth",
    ":core:data",
    ":feature:explore",
    ":feature:status",
    ":feature:map",
    ":feature:chat",
    ":feature:wiki",
    ":feature:player",
    ":feature:ticket",
    ":feature:admin",
)
