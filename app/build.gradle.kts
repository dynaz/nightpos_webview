plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nightpos.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nightpos.app"
        minSdk = 21          // floor set by legacy flavor; modern overrides to 26
        targetSdk = 35
        versionCode = 15
        versionName = "2.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // ── Product flavors ────────────────────────────────────────────────────────
    // One APK per flavor; device differences are handled at runtime inside each.
    //
    //   legacy  → Sunmi T1 / T2  (Android 6/7, armeabi-v7a)  — GeckoView 142
    //   modern  → Sunmi D2s family (Android 11, arm64-v8a)   — system WebView
    //
    flavorDimensions += "target"

    productFlavors {
        create("legacy") {
            dimension = "target"
            minSdk = 21                              // Android 5+ (T1 = 23, T2 = 25)
            ndk { abiFilters += "armeabi-v7a" }      // 32-bit Sunmi hardware
            buildConfigField("Boolean", "USE_GECKO", "true")
            buildConfigField("String", "GECKOVIEW_VERSION", "\"142.0\"")
            versionNameSuffix = "-legacy"
        }
        create("modern") {
            dimension = "target"
            minSdk = 26                              // Android 8+ (D2s family = API 30)
            ndk { abiFilters += "arm64-v8a" }        // 64-bit Sunmi hardware
            buildConfigField("Boolean", "USE_GECKO", "false")
            buildConfigField("String", "GECKOVIEW_VERSION", "\"\"")
            versionNameSuffix = "-modern"
        }
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
    // GeckoView only in the legacy flavor (T1/T2, armeabi-v7a, Android 6/7).
    // v142 is the highest GeckoView supporting API 21+; v143+ raised floor to API 26.
    // Firefox 130+ added 'camera' to navigator.permissions.query PermissionName enum,
    // fixing the UncaughtPromiseError seen with GeckoView 105.
    "legacyImplementation"("org.mozilla.geckoview:geckoview-armeabi-v7a:142.0.20250827004350")
    // Local HTTP server — used by both flavors for the Sunmi printer bridge.
    // legacy: avoids SELinux socket-ioctl restriction on kernel 3.10 / Android 6.
    // modern: Chrome has a localhost mixed-content exception so fetch() works directly.
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    debugImplementation(libs.androidx.ui.tooling)
}
