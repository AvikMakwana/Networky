plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.plugin)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.avikmakwana.netstat"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.dagger)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
}