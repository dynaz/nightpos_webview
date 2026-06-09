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
        // 23 = Android 6.0/6.0.1 (Marshmallow) — required to support Sunmi T1 POS terminals.
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging)
    implementation(libs.gson)

    debugImplementation(libs.androidx.ui.tooling)
}
