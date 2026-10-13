plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ganhard"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ganhard"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { viewBinding = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
    sourceSets {
        // Los esquemas exportados de Room permiten probar las migraciones.
        getByName("test").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
