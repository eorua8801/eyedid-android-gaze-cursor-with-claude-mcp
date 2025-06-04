// ============================================================================
// Top-level build file for EyedidTracker-Refactored
// Optimized for performance, modern Android development, and gaze tracking apps
// ============================================================================

plugins {
    alias(libs.plugins.android.application) apply false
    
    // Additional useful plugins for development and optimization
    id("org.gradle.android.cache-fix") version "3.0.1" apply false
    id("com.github.ben-manes.versions") version "0.51.0" apply false
}

// ============================================================================
// Project-wide Configuration (🤖 AUTO-FIXED: Repositories moved to settings.gradle.kts)
// ============================================================================
// Note: All repositories are now configured in settings.gradle.kts for better Gradle 8.x compatibility

// ============================================================================
// Dependency Version Management
// ============================================================================
subprojects {
    // Apply common configurations to all subprojects
    apply {
        plugin("com.github.ben-manes.versions")
    }
    
    // Configure dependency updates checking
    tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
        rejectVersionIf {
            candidate.version.contains("alpha") ||
            candidate.version.contains("beta") ||
            candidate.version.contains("rc") ||
            candidate.version.contains("CR") ||
            candidate.version.contains("M") ||
            candidate.version.contains("preview") ||
            candidate.version.contains("b") ||
            candidate.version.contains("ea")
        }
        
        checkForGradleUpdate = true
        outputFormatter = "plain"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}

// ============================================================================
// Custom Tasks for Project Management
// ============================================================================

// Task for cleaning all build directories
tasks.register("cleanAll") {
    group = "build"
    description = "Clean all build directories and caches"
    
    doLast {
        // 🤖 AUTO-FIX: Updated to use latest Gradle API
        delete(rootProject.layout.buildDirectory.get().asFile)
        subprojects.forEach { subproject ->
            delete(subproject.layout.buildDirectory.get().asFile)
        }
        
        // Clean Gradle caches if needed
        println("🧹 All build directories cleaned!")
        println("📁 Run './gradlew build' to rebuild the project")
    }
}

// Task for checking dependency updates
tasks.register("checkDependencyUpdates") {
    group = "help"
    description = "Check for dependency updates across all modules"
    dependsOn(":app:dependencyUpdates")
    
    doLast {
        println("📊 Dependency update check completed!")
        println("📋 Check build/dependencyUpdates/report.txt for details")
    }
}

// Task for analyzing project structure
tasks.register("analyzeProject") {
    group = "help"
    description = "Analyze project structure and provide optimization suggestions"
    
    doLast {
        println("📊 EyedidTracker Project Analysis")
        println("=".repeat(50))
        
        // Count files by type
        val javaFiles = fileTree(rootDir) { 
            include("**/*.java")
            exclude("**/build/**")
        }.files.size
        
        val kotlinFiles = fileTree(rootDir) { 
            include("**/*.kt")
            exclude("**/build/**") 
        }.files.size
        
        val xmlFiles = fileTree(rootDir) { 
            include("**/*.xml")
            exclude("**/build/**")
        }.files.size
        
        println("📝 Source files:")
        println("   • Java files: $javaFiles")
        println("   • Kotlin files: $kotlinFiles") 
        println("   • XML files: $xmlFiles")
        println()
        
        // Gradle files
        val gradleFiles = fileTree(rootDir) {
            include("**/*.gradle", "**/*.gradle.kts")
        }.files.size
        
        println("⚙️  Build files:")
        println("   • Gradle files: $gradleFiles")
        println()
        
        // Calculate approximate lines of code
        var totalLines = 0
        fileTree(rootDir) {
            include("**/*.java", "**/*.kt")
            exclude("**/build/**")
        }.visit {
            if (file.isFile) {
                totalLines += file.readLines().size
            }
        }
        
        println("📏 Estimated lines of code: $totalLines")
        println()
        
        // Optimization suggestions
        println("💡 Optimization suggestions:")
        println("   • Run './gradlew checkDependencyUpdates' to check for updates")
        println("   • Run './gradlew analyzeApkSize' to analyze APK size")
        println("   • Run './gradlew performanceTest' to run performance tests")
        println("   • Use './gradlew build --profile' for build performance analysis")
        println("=".repeat(50))
    }
}

// Task for performance analysis
tasks.register("performanceAnalysis") {
    group = "analysis"
    description = "Run comprehensive performance analysis for gaze tracking app"
    
    doLast {
        println("🎯 EyedidTracker Performance Analysis")
        println("=".repeat(60))
        println("For comprehensive performance analysis:")
        println()
        
        println("📱 APK Analysis:")
        println("   ./gradlew assembleRelease")
        println("   ./gradlew analyzeApkSize")
        println()
        
        println("🏃‍♂️ Runtime Performance:")
        println("   • Test gaze tracking latency with different settings")
        println("   • Monitor memory usage during extended sessions")
        println("   • Check battery drain during background tracking")
        println()
        
        println("🔍 Code Quality:")
        println("   ./gradlew lint")
        println("   ./gradlew test")
        println()
        
        println("🚀 Build Performance:")
        println("   ./gradlew build --profile")
        println("   ./gradlew checkDependencyUpdates")
        println("=".repeat(60))
    }
}

// ============================================================================
// Gradle Wrapper Configuration
// ============================================================================
tasks.wrapper {
    gradleVersion = "8.11.1"
    distributionType = Wrapper.DistributionType.ALL
    
    doLast {
        println("🔄 Gradle wrapper updated to version $gradleVersion")
        println("📥 Distribution type: ALL (includes sources and docs)")
    }
}

// ============================================================================
// Build Lifecycle Hooks (🤖 AUTO-FIXED: Removed preBuild reference)
// ============================================================================
// 🤖 AUTO-FIX: Replaced with safe project configuration message
gradle.projectsLoaded {
    println("✅ EyedidTracker project configuration completed!")
    println("🎯 Ready for gaze tracking development!")
    println("🚀 Use './gradlew build' to start building")
}
