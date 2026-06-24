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
        minSdk = 21          // floor set by arm32 flavor; arm64 overrides to 26
        targetSdk = 35
        versionCode = 15
        versionName = "2.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // ── Product flavors ────────────────────────────────────────────────────────
    // All flavors use GeckoView. Flavor = device, determines CPU ABI and minSdk.
    //
    //   arm32   → armeabi-v7a — Sunmi T1, T2 (Android 6/7, Rockchip)
    //   d2splus → armeabi-v7a — Sunmi D2s Plus (Android 11, Rockchip RK30)
    //   arm64   → arm64-v8a   — Sunmi D2s original (Android 11, Qualcomm Snapdragon)
    //
    // Pick by: adb shell getprop ro.product.cpu.abi
    //   armeabi-v7a → arm32 or d2splus APK
    //   arm64-v8a   → arm64 APK
    //
    flavorDimensions += "target"

    productFlavors {
        create("arm32") {
            dimension = "target"
            minSdk = 21                              // Android 5+ (T1 = 23, T2 = 25)
            ndk { abiFilters += "armeabi-v7a" }
            buildConfigField("Boolean", "USE_GECKO", "true")
            buildConfigField("String", "GECKOVIEW_VERSION", "\"142.0\"")
            versionNameSuffix = "-arm32"
        }
        create("d2splus") {
            dimension = "target"
            minSdk = 30                              // Android 11 (D2s Plus = API 30)
            ndk { abiFilters += "armeabi-v7a" }      // Rockchip RK30 is 32-bit ARM
            buildConfigField("Boolean", "USE_GECKO", "true")
            buildConfigField("String", "GECKOVIEW_VERSION", "\"142.0\"")
            versionNameSuffix = "-d2splus"
        }
        create("arm64") {
            dimension = "target"
            minSdk = 26                              // Android 8+ (D2s original = API 30)
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("Boolean", "USE_GECKO", "true")
            buildConfigField("String", "GECKOVIEW_VERSION", "\"142.0\"")
            versionNameSuffix = "-arm64"
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
        jniLibs {
            // GeckoView .so files must be extracted at install time; without this
            // Android 6+ skips extraction and the APK fails with INSTALL_FAILED_NO_MATCHING_ABIS.
            useLegacyPackaging = true
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
    // GeckoView per flavor — same version, different ABI to match device hardware.
    // v142 is the highest GeckoView supporting API 21+; v143+ raised minSdk to 26.
    // Firefox 130+ added 'camera' to navigator.permissions.query PermissionName enum,
    // fixing the UncaughtPromiseError seen with GeckoView 105.
    // arm32/d2splus: armeabi-v7a GeckoView (T1, T2, D2s Plus)
    "arm32Implementation"("org.mozilla.geckoview:geckoview-armeabi-v7a:142.0.20250827004350")
    "d2splusImplementation"("org.mozilla.geckoview:geckoview-armeabi-v7a:142.0.20250827004350")
    // arm64: arm64-v8a GeckoView (D2s original)
    "arm64Implementation"("org.mozilla.geckoview:geckoview-arm64-v8a:142.0.20250827004350")
    // Local HTTP server — used by both flavors for the Sunmi printer bridge.
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    debugImplementation(libs.androidx.ui.tooling)
}
