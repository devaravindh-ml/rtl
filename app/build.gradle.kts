
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.objectbox")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")

}

android {
    namespace = "com.example.book"
    compileSdk = 36// Adjusted to a standard stable SDK, change to 36 if you explicitly need preview

    defaultConfig {
        applicationId = "com.example.book"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        // Required for ONNX and ObjectBox native libs
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    kapt {
        arguments {
            arg("objectbox.myObjectBoxPackage", "com.example.book")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Required for Readium 3.0.0 to handle modern Java time/collections
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 1. REMOVED the JVM datastore line that was here
    implementation(libs.androidx.remote.creation.compose)

    // Java desugaring (CRITICAL for Readium 3.0.0 stability)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // HILT
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // ObjectBox
    implementation("io.objectbox:objectbox-android:4.0.3")
    implementation("io.objectbox:objectbox-kotlin:4.0.3")
    kapt("io.objectbox:objectbox-processor:4.0.3")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // --- Readium Toolkit 3.0.0 ---
    val readiumVersion = "3.0.0"

    implementation("org.readium.kotlin-toolkit:readium-shared:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-streamer:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-adapter-pdfium:$readiumVersion")

    // Navigator with the EXCLUDE rule (Only list this ONCE)
    implementation("org.readium.kotlin-toolkit:readium-navigator:$readiumVersion") {
        exclude(group = "androidx.datastore", module = "datastore-preferences-core-jvm")
    }

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}