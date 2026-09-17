plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

allprojects {
    val rootPath = rootProject.projectDir.absolutePath
    if (rootPath.contains(";")) {
        val userHome = System.getProperty("user.home").replace("\\", "/")
        layout.buildDirectory.set(file("$userHome/.gradle-builds/PimsVault/${project.name}"))
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    val userHome = System.getProperty("user.home").replace("\\", "/")
    delete(file("$userHome/.gradle-builds/PimsVault"))
    delete(file("${rootProject.projectDir}/.kotlin"))
}


