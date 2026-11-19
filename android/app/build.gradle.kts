plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

room {
    // Committed JSON snapshots of each database version, needed to write and test migrations.
    schemaDirectory("$projectDir/schemas")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The backend's Google *Web* OAuth client (better-auth's GOOGLE_CLIENT_ID). Google
        // issues the app's ID tokens for this audience so the backend accepts them. The
        // Android clients (package + SHA-1) live in the same Google Cloud project.
        // Not a secret: it appears in every Google sign-in URL the website opens.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"219791307207-uvak3f3mnb684sevl7j974ttbqm7brcl.apps.googleusercontent.com\"",
        )
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

    sourceSets {
        // Room migration tests read the exported schemas (app/schemas/) as assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    testOptions {
        // android.util.Log etc. return defaults instead of throwing in JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Pinned so the app and its tests use the same coroutines (AndroidX alone pulls in 1.9).
    implementation(libs.kotlinx.coroutines.android)
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
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Photo rotation from the camera's EXIF tag (Detect, A4.2).
    implementation(libs.androidx.exifinterface)
    // Gallery feed that loads more as you scroll (A5.1).
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)

    // On-device tests (./gradlew connectedDebugAndroidTest, needs an emulator or phone).
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
}
