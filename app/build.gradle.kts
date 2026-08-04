plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
// Release 签名由 GitHub Actions 通过 Secret 注入（LOCLOGGER_RELEASE_*），本地不配置密钥
val releaseStoreFile = providers.environmentVariable("LOCLOGGER_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("LOCLOGGER_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("LOCLOGGER_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("LOCLOGGER_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = !releaseStoreFile.isNullOrEmpty() &&
    !releaseStorePassword.isNullOrEmpty() &&
    !releaseKeyAlias.isNullOrEmpty() &&
    !releaseKeyPassword.isNullOrEmpty()

android {
    namespace = "moe.telecom.loclogger"
    compileSdk = 36

    signingConfigs {
        create("release") {
            releaseStoreFile?.let { storeFile = rootProject.file(it) }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    defaultConfig {
        applicationId = "moe.telecom.loclogger"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Activity
    implementation(libs.androidx.activity.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Miuix KMP - Liquid Glass 效果
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)

    // Material Kolor - 动态取色
    implementation(libs.material.kolor)

    // osmdroid - 多源地图
    implementation(libs.osmdroid.android)

    // Google Play Services Location
    implementation(libs.playservices.location)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}
