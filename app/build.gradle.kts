import java.util.Properties
import java.io.FileInputStream

// Enterprise Secrets Loader: Environment Variables > secrets.properties > local.properties
val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        load(FileInputStream(secretsFile))
    }
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}

fun resolveSecret(key: String): String? {
    return System.getenv(key)
        ?: secretsProperties.getProperty(key)
        ?: localProperties.getProperty(key)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pims.vault"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "personal.info"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        val releaseKeystorePath = resolveSecret("RELEASE_KEYSTORE_PATH")
        val releaseKeystorePassword = resolveSecret("RELEASE_KEYSTORE_PASSWORD")
        val releaseKeyAlias = resolveSecret("RELEASE_KEY_ALIAS")
        val releaseKeyPassword = resolveSecret("RELEASE_KEY_PASSWORD")

        if (!releaseKeystorePath.isNullOrBlank() && file(releaseKeystorePath).exists()) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "IS_DEBUG_CRYPTO_ALLOWED", "true")
            buildConfigField("Boolean", "ENABLE_STRICT_SECURITY_LOGS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "IS_DEBUG_CRYPTO_ALLOWED", "false")
            buildConfigField("Boolean", "ENABLE_STRICT_SECURITY_LOGS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    exclude("**/byRounds/**")
}

ksp {
    // Room schema export for migration verification and release auditing
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "false")
    arg("room.expandProjection", "true")
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-process:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // Jetpack Compose BOM
    implementation(enforcedPlatform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Rive Vector Animation Runtime & Jetpack Startup
    implementation(libs.rive.android) {
        exclude(group = "androidx.compose")
        exclude(group = "androidx.core")
        exclude(group = "androidx.lifecycle")
    }
    implementation(libs.androidx.startup.runtime)

    // Credentials & Identity & Network
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.okhttp)
    implementation(libs.zxing.core)

    // Room & Encrypted Database (SQLCipher)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kspDebug(libs.androidx.room.compiler)
    kspRelease(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kspDebug(libs.hilt.compiler)
    kspRelease(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase authentication and remote storage
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)

    // Unit & Integration Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
