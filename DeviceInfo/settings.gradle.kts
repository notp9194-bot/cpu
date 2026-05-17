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
rootProject.name = "DeviceInfo"
include(":app")
include(":core")
include(":feature-soc")
include(":feature-device")
include(":feature-system")
include(":feature-battery")
include(":feature-thermal")
include(":feature-sensors")
include(":feature-about")
include(":feature-network")
include(":feature-camera")
