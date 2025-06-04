# ============================================================================
# ProGuard Configuration for EyedidTracker-Refactored
# Optimized for performance, stability, and EyeDid SDK compatibility
# ============================================================================

# ============================================================================
# General Android Rules
# ============================================================================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view classes and their constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep activity classes that are referenced in the manifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference

# ============================================================================
# EyeDid/SeeSo SDK Preservation
# ============================================================================
# Keep all EyeDid SDK classes and methods (critical for gaze tracking)
-keep class camp.visual.gazetracker.** { *; }
-keep interface camp.visual.gazetracker.** { *; }
-keep enum camp.visual.gazetracker.** { *; }

# Keep any classes that might be called via reflection by EyeDid SDK
-keepclassmembers class * {
    @camp.visual.gazetracker.* <methods>;
}

# Keep callback interfaces for EyeDid SDK
-keep class * implements camp.visual.gazetracker.callback.** { *; }
-keep class * implements camp.visual.gazetracker.filter.** { *; }

# ============================================================================
# App-Specific Classes (EyedidTracker)
# ============================================================================
# Keep main application classes
-keep class camp.visual.android.sdk.sample.** { *; }

# Keep accessibility service classes (critical for system interaction)
-keep class * extends android.accessibilityservice.AccessibilityService {
    public <init>(...);
    public void onServiceConnected();
    public void onAccessibilityEvent(...);
    public void onInterrupt();
}

# Keep service classes for background gaze tracking
-keep class * extends android.app.Service {
    public <init>(...);
    public android.os.IBinder onBind(...);
    public void onCreate();
    public int onStartCommand(...);
    public void onDestroy();
}

# Keep data model classes (for serialization/settings)
-keep class camp.visual.android.sdk.sample.domain.model.** { *; }
-keep class camp.visual.android.sdk.sample.data.settings.** { *; }

# ============================================================================
# Performance Critical Classes (Real-time Gaze Tracking)
# ============================================================================
# Keep classes involved in real-time processing
-keep class camp.visual.android.sdk.sample.service.tracking.** { *; }
-keep class camp.visual.android.sdk.sample.domain.interaction.** { *; }
-keep class camp.visual.android.sdk.sample.ui.views.** { *; }

# Keep calibration related classes
-keep class * {
    *calibr*;
    *Calibr*;
}

# ============================================================================
# Android System Components
# ============================================================================
# Keep parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serializable classes
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# Debugging & Logging (Keep for release troubleshooting)
# ============================================================================
# Keep line numbers for debugging stack traces in production
-keepattributes SourceFile,LineNumberTable

# Keep method names for better crash reports
-keepattributes LocalVariableTable,LocalVariableTypeTable

# Keep annotations for debugging
-keepattributes *Annotation*

# ============================================================================
# AndroidX & Support Library Rules (🤖 AUTO-OPTIMIZED)
# ============================================================================
# Keep only essential AndroidX classes
-keep,allowshrinking class androidx.** { *; }
-keep,allowshrinking interface androidx.** { *; }

# Keep only essential Material Design Components
-keep,allowshrinking class com.google.android.material.** { *; }

# 🤖 AUTO: Allow shrinking of unnecessary AndroidX components
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# ============================================================================
# Memory & Performance Optimizations
# ============================================================================
# Optimize for real-time performance (aggressive optimization)
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Allow aggressive shrinking for smaller APK
-allowaccessmodification

# Optimize method calls for better performance
-optimizationpasses 5

# ============================================================================
# Security & Obfuscation (🤖 AUTO-ENHANCED for Size Reduction)
# ============================================================================
# Obfuscate everything except kept classes
-obfuscationdictionary proguard-obfuscation-dict.txt
-classobfuscationdictionary proguard-class-dict.txt
-packageobfuscationdictionary proguard-package-dict.txt

# 🤖 AUTO: Aggressive optimization for APK size reduction
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
-optimizationpasses 10
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses ''
-flattenpackagehierarchy

# Remove unused code aggressively
-dontwarn **
-ignorewarnings

# ============================================================================
# Logging & Debug Information (🤖 AUTO-ENHANCED)
# ============================================================================
# Remove all logging in release builds for better performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug-related method calls
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void err.println(...);
}

# 🤖 AUTO: Remove additional debug and development code
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

-assumenosideeffects class java.lang.Thread {
    public static void dumpStack();
}

# Remove BuildConfig debug fields
-assumenosideeffects class **.BuildConfig {
    public static boolean DEBUG;
    public static java.lang.String BUILD_TYPE;
}

# ============================================================================
# Special Cases & Workarounds
# ============================================================================
# Keep enum values (sometimes needed for proper enum handling)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep custom exceptions for better error handling
-keep public class * extends java.lang.Exception

# Keep any classes accessed via reflection (if any)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================================================
# Performance Monitoring (Comment out for production if not needed)
# ============================================================================
# Keep performance monitoring classes for optimization analysis
# -keep class * {
#     *performance*;
#     *Performance*;
#     *fps*;
#     *FPS*;
# }

# ============================================================================
# Final Notes
# ============================================================================
# This configuration is optimized for:
# 1. EyeDid SDK compatibility and performance
# 2. Real-time gaze tracking requirements
# 3. Accessibility service functionality
# 4. Small APK size and fast execution
# 5. Debugging capability in production

# Test thoroughly after any changes to these rules!
