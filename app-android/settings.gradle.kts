pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppEnglish"
include(":app")

// Auto-increment versionCode and versionName patch on every release build
val tasks = gradle.startParameter.taskNames
if (tasks.isNotEmpty() && tasks.any { it.contains("Release", ignoreCase = true) }) {
    val incVersion = java.util.Properties()
    val incFile = file("../version.properties")
    if (incFile.exists()) {
        incFile.reader().use { incVersion.load(it) }
        val code = incVersion.getProperty("versionCode", "1").toInt() + 1
        incVersion.setProperty("versionCode", code.toString())
        val name = incVersion.getProperty("versionName", "3.2.0")
        val parts = name.split(".").toMutableList()
        if (parts.size >= 3) {
            parts[parts.size - 1] = (parts[parts.size - 1].toInt() + 1).toString()
        }
        incVersion.setProperty("versionName", parts.joinToString("."))
        incFile.writer().use { incVersion.store(it, "Auto-incremented by release build") }
        val backendIncFile = file("../backend/version.properties")
        if (backendIncFile.exists()) {
            backendIncFile.writer().use { incVersion.store(it, "Auto-incremented by release build") }
        }
        println("Version bumped to ${incVersion.getProperty("versionCode")} (${incVersion.getProperty("versionName")})")
    }
}
