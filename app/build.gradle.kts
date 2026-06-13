plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nightpos.geckoview"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nightpos.geckoview"
        // 21 = Android 5.0 — GeckoView 142 supports API 21+.
        // Sunmi D2s runs Android 7.1.2 (API 25), well above this floor.
        // GeckoView 143+ raised minSdk to 26, so 142 is the highest version for D2s.
        minSdk = 21
        targetSdk = 35
        versionCode = 23
        versionName = "2.0.18"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GECKOVIEW_VERSION", "\"142.0\"")
    }

    signingConfigs {
        // Committed keystore so every build (local or CI) produces the same signing
        // certificate fingerprint — required so assetlinks.json verification for the
        // Trusted Web Activity keeps matching regardless of where the APK was built.
        create("nightpos") {
            storeFile = file("${rootProject.projectDir}/keystore/nightpos-release.keystore")
            storePassword = "nightpos123"
            keyAlias = "nightpos"
            keyPassword = "nightpos123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("nightpos")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("nightpos")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material)
    implementation(libs.androidbrowserhelper)
    implementation(libs.androidx.appcompat)
    implementation(libs.sunmi.printerlibrary)
    // 32-bit ARM variant — Sunmi D2s is armeabi-v7a, Android 7.1.2 (API 25).
    // v142 is the highest GeckoView that supports API 21+ (v143+ raised floor to API 26).
    // Firefox 130+ added 'camera' to navigator.permissions.query PermissionName enum,
    // fixing the UncaughtPromiseError seen with GeckoView 105.
    implementation("org.mozilla.geckoview:geckoview-armeabi-v7a:142.0.20250827004350")
    // Local HTTP server for printer bridge (avoids SELinux socket-ioctl restriction
    // that prevents GeckoView IPC from working on kernel 3.10 / Android 6.0.1).
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    debugImplementation(libs.androidx.ui.tooling)
}
