plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The website and its /api/* routes share this origin (see DEPLOY.md).
val PRODUCTION_BASE_URL = "https://birddex.fun"

android {
    // Code package. Differs from applicationId because `fun` is a Kotlin keyword.
    namespace = "com.birddex.app"
    compileSdk = 37

    defaultConfig {
        // Permanent Play Store ID (matches the birddex.fun domain).
        applicationId = "fun.birddex.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Installs next to the release app, as "BirdDex Debug" (src/debug/res).
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Production API by default. Point debug builds elsewhere with
            // -Pbirddex.debugBaseUrl=https://... or the same key in ~/.gradle/gradle.properties.
            val debugBaseUrl = providers.gradleProperty("birddex.debugBaseUrl")
                .getOrElse(PRODUCTION_BASE_URL)
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$PRODUCTION_BASE_URL\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
