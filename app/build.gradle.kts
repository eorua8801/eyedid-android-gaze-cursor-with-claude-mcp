plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "camp.visual.android.sdk.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "camp.visual.android.sdk.sample"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // ====================================================================
        // Performance Optimizations for Gaze Tracking
        // ====================================================================
        // Vector drawable support for older APIs
        vectorDrawables.useSupportLibrary = true
        
        // Enable multidex for large apps
        multiDexEnabled = true
        
        // Optimize for gaze tracking performance
        ndk {
            // 🤖 AUTO: Optimized ABI filtering for minimum size
            abiFilters += listOf("arm64-v8a") // Removed armeabi-v7a for size optimization
        }
    }

    buildTypes {
        debug {
            // Debug build optimizations
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            
            // Enable debug logging for development
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            
            // Faster debug builds
            manifestPlaceholders["crashlyticsCollectionEnabled"] = false
        }
        
        release {
            // ================================================================
            // Production Release Optimizations (Auto-Enhanced for APK Size)
            // ================================================================
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            // 🤖 AUTO: Enhanced ProGuard/R8 optimization for size reduction
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 🤖 AUTO: Aggressive APK size optimization
            isCrunchPngs = true
            
            // Build config for production
            buildConfigField("boolean", "DEBUG_LOGGING", "false")
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            
            // Production configurations
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            
            // ================================================================
            // Performance & Security for Gaze Tracking
            // ================================================================
            // Optimize native libraries
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    
    // ========================================================================
    // Build Features & Performance
    // ========================================================================
    buildFeatures {
        buildConfig = true
        viewBinding = true  // Enable view binding for better performance
        dataBinding = false // Disable if not needed to reduce build time
    }
    
    // ========================================================================
    // Compilation & Language Settings
    // ========================================================================
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        
        // Enable incremental compilation
        isCoreLibraryDesugaringEnabled = true
    }
    
    // ========================================================================
    // Packaging & APK Optimization (🤖 AUTO-UPDATED for latest Gradle)
    // ========================================================================
    packaging {
        // 🤖 AUTO: Aggressive file exclusion for size reduction
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "META-INF/androidx.*",
                "META-INF/proguard/androidx-*",
                // 🤖 AUTO: Additional exclusions for size optimization
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/MANIFEST.MF",
                "**/*.properties",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "RELEASE",
                "VERSION",
                "**/DEPENDENCIES",
                "**/LICENSE*",
                "**/NOTICE*",
                "**/README*",
                "**/CHANGELOG*"
            )
        }
        
        // 🤖 AUTO-FIX: Updated to latest API - Pick first occurrence of duplicate .so files
        jniLibs {
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libjsc.so",
                "**/libfbjni.so",
                "**/libhybrid.so"
            )
        }
    }
    
    // ========================================================================
    // Testing Configuration
    // ========================================================================
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        // Use Gradle managed devices for consistent testing
        managedDevices {
            allDevices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
            }
        }
    }
    
    // ========================================================================
    // Lint Configuration for Code Quality
    // ========================================================================
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        
        // Disable specific lint checks that are not relevant
        disable += listOf(
            "MissingTranslation",
            "ExtraTranslation",
            "VectorDrawableCompat"
        )
        
        // Enable important checks for gaze tracking apps
        enable += listOf(
            "LogNotTimber",
            "StringFormatInTimber",
            "ThrowableNotAtBeginning",
            "BinaryOperationInTimber",
            "TimberArgCount",
            "TimberArgTypes",
            "TimberTagLength"
        )
        
        // Generate reports
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }
}

dependencies {
    // ========================================================================
    // Core Android Dependencies
    // ========================================================================
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // ========================================================================
    // EyeDid SDK for Gaze Tracking
    // ========================================================================
    implementation(libs.eyedid.gazetracker)
    
    // ========================================================================
    // Performance & Utilities
    // ========================================================================
    // Core library desugaring for Java 8+ features on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Multidex support for large apps
    implementation("androidx.multidex:multidex:2.0.1")
    
    // ========================================================================
    // Testing Dependencies
    // ========================================================================
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.robolectric:robolectric:4.10.3")
    
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    
    // ========================================================================
    // Debug Tools (debug builds only)
    // ========================================================================
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// ============================================================================
// Custom Build Tasks for Performance Analysis
// ============================================================================
tasks.register("analyzeApkSize") {
    group = "analysis"
    description = "Analyze APK size and provide optimization suggestions"
    
    doLast {
        println("=".repeat(60))
        println("APK Size Analysis for EyedidTracker")
        println("=".repeat(60))
        println("Run this after building to analyze APK size:")
        println("./gradlew assembleRelease")
        println("bundletool build-apks --bundle=app-release.aab --output=app.apks")
        println("=".repeat(60))
    }
}

tasks.register("performanceTest") {
    group = "verification" 
    description = "Run performance tests for gaze tracking functionality"
    dependsOn("testDebugUnitTest")
    
    doLast {
        println("=".repeat(60))
        println("Performance Test Completed for EyedidTracker")
        println("=".repeat(60))
        println("Check test reports in build/reports/tests/")
        println("=".repeat(60))
    }
}
