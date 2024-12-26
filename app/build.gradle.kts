/*
The build.gradle.kts file in the 'app' module serves several crucial purposes in modern Android development.
Here's an explanation coming from a traditional Java perspective:

Project Configuration
-Think of it like a super-powered build.xml (if you used Ant) or pom.xml (if you used Maven)
-Defines how your Android app should be built, packaged, and configured
-Sets critical app identifiers (like applicationId, which is your app's unique ID in the Play Store)

Dependency Management
-Like Maven's dependency system, but more flexible
-Declares all external libraries your app needs
-Handles transitive dependencies automatically
-Manages different dependency scopes (test, debug, release)

Build Process Configuration
-Sets Java version (like old javac flags)
-Configures Android-specific settings (minimum/target Android versions)
-Defines build types (debug/release, like different Maven profiles)
-Sets up code shrinking/obfuscation (ProGuard settings)

Module-Level Settings
-This specific file is for the 'app' module, which is your actual application
-Android projects can have multiple modules (like library modules)
-The 'app' module is special - it's the one that becomes your final APK

Tool Integration
-Configures Android Studio tooling
-Sets up testing frameworks
-Enables modern features like Jetpack Compose

Coming from old-school Java, you can think of it as combining:
-The role of a build.xml or pom.xml
-JDK configuration
-Library management
-Packaging instructions
-Tool configuration

Key differences from traditional Java build systems:

Build System:
-Gradle replaces Ant/Maven
-Kotlin DSL instead of XML or Groovy
-More flexible and powerful configuration

Dependencies:
-Modern dependency configurations (implementation, api, etc.)
-More granular control than old compile/runtime
-Version management through BOM (Bill of Materials)

Android Specific:
-SDK version management
-Build types and flavors
-Resource packaging
-ProGuard integration

Testing:
-Different configurations for unit vs. instrumented tests
-Modern testing frameworks
-Device-specific testing support

Modern Features:
-Jetpack Compose support
-Vector drawable support
-AndroidX libraries
-Material Design components
 */

// Gradle plugins using Kotlin DSL syntax (equivalent to old buildscript {} blocks)
plugins {
    // Main Android application plugin - replaces old Eclipse/Ant build system
    id("com.android.application")
}

// Main Android configuration block - central to all Android projects
android {
    // Package name for your app (like old Java package names)
    namespace = "com.example.audiospectrograph"
    // Android SDK version to compile against (like Java's source/target version)
    compileSdk = 34

    // Default configuration block - core app settings
    defaultConfig {
        // Unique identifier for Play Store (like old Java package names)
        applicationId = "com.example.audiospectrograph"
        // Minimum Android version supported (API level, not version number)
        minSdk = 24
        // Target Android version (optimal API level)
        targetSdk = 34
        // Internal version number (for Play Store updates)
        versionCode = 1
        // User-visible version string
        versionName = "1.0"

        // Testing configuration - modern Android testing framework
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Vector drawable support configuration
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Build types configuration (debug/release variants)
    buildTypes {
        // Release build configuration
        release {
            // Code shrinking/obfuscation (like old ProGuard)
            isMinifyEnabled = false
            // ProGuard rules files
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java version configuration (similar to old javac settings)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Modern Android UI toolkit features
    buildFeatures {
        compose = true  // Enable Jetpack Compose (modern UI toolkit)
    }

    // Compose-specific compiler options
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    // APK packaging options
    packaging {
        resources {
            // Exclude specific files from final APK
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Dependencies block - modern replacement for Maven/Ivy dependencies
dependencies {
    // 'implementation' replaces old 'compile' configuration
    // Format is typically: group:artifact:version

    // Third-party library for FFT
    implementation("com.github.wendykierp:JTransforms:3.1")

    // AndroidX libraries (modern Android support libraries)
    // These replace old android.support.* packages
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")

    // Jetpack Compose dependencies (modern UI toolkit)
    implementation("androidx.activity:activity-compose:1.8.2")
    // BOM (Bill of Materials) for consistent Compose versions
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Testing dependencies
    // 'testImplementation' is for local unit tests (like old JUnit)
    testImplementation("junit:junit:4.13.2")
    // 'androidTestImplementation' is for instrumented tests (on device/emulator)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug-only dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
