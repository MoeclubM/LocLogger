import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
// Release 签名：密钥库提交在仓库 keystore/loclogger-release.keystore；
// 密码本地读 keystore.properties（不入库），CI 由 GH Secrets 通过 ORG_GRADLE_PROJECT_* 注入
fun signingValue(name: String): String {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.isFile) {
        val props = Properties()
        propsFile.inputStream().use { props.load(it) }
        props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return providers.gradleProperty(name).orNull?.trim().orEmpty()
}

val releaseStoreFile = providers.environmentVariable("LOCLOGGER_RELEASE_STORE_FILE").orNull
    ?: "keystore/loclogger-release.keystore"
val releaseStorePassword = signingValue("LOCLOGGER_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("LOCLOGGER_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("LOCLOGGER_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

android {
    namespace = "moe.telecom.loclogger"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseStoreFile)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    defaultConfig {
        applicationId = "moe.telecom.loclogger"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    implementation(libs.miuix.blur)

    // Material Kolor - 动态取色
    implementation(libs.material.kolor)

    // MapLibre - 多源地图（OSM/Google/高德）
    implementation(libs.maplibre.android)

    // Google Play Services Location
    implementation(libs.playservices.location)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}
