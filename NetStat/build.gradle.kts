import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.plugin)
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish") version "0.35.0"
}

group = "com.avikmakwana"
version = "1.0.1"

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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.dagger)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
}

/**
 * Vanniktech Maven Publish configuration for Maven Central Portal.
 */
mavenPublishing {
    coordinates(
        groupId = "com.avikmakwana",
        artifactId = "netstate",
        version = "1.0.1",
    )

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("NetState")
        description.set("Android library for monitoring and observing network connectivity state.")
        url.set("https://github.com/AvikMakwana/Networky")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("avikmakwana")
                name.set("Avik Makwana")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/AvikMakwana/Networky.git")
            developerConnection.set("scm:git:ssh://git@github.com/AvikMakwana/Networky.git")
            url.set("https://github.com/AvikMakwana/Networky")
        }
    }
}