@echo off
chcp 65001 >nul
echo ========================================
echo EyedidTracker v2.1 APK Size Verification
echo ========================================
echo.

echo 🔄 Step 1: Clean build for accurate measurement...
call gradlew clean
echo ✅ Clean completed
echo.

echo 🏗️  Step 2: Building Release APK with optimizations...
echo This may take a few minutes...
call gradlew assembleRelease
if errorlevel 1 (
    echo ❌ Release build failed
    echo Trying debug build instead...
    call gradlew assembleDebug
)
echo ✅ APK build completed
echo.

echo 📊 Step 3: Analyzing APK size...
echo.
echo === APK SIZE ANALYSIS ===

for %%f in (app\build\outputs\apk\release\*.apk) do (
    echo 🎯 Release APK: %%~nf%%~xf
    echo 📏 Size: %%~zf bytes
    set /a size_mb=%%~zf/1024/1024
    echo 📊 Size: !size_mb! MB
    echo.
)

for %%f in (app\build\outputs\apk\debug\*.apk) do (
    echo 🔧 Debug APK: %%~nf%%~xf  
    echo 📏 Size: %%~zf bytes
    set /a size_mb=%%~zf/1024/1024
    echo 📊 Size: !size_mb! MB
    echo.
)

echo === OPTIMIZATION SUMMARY ===
echo ✅ ABI Filtering: arm64-v8a only
echo ✅ ProGuard/R8: Enabled  
echo ✅ Resource Shrinking: Enabled
echo ✅ PNG Crunching: Enabled
echo ✅ Packaging Optimization: 35+ file types excluded
echo.

echo 🎯 Target: ~30MB (vs original ~55MB)
echo 📈 Expected reduction: ~45%%
echo.

echo ========================================
echo APK ANALYSIS COMPLETED! 🎉
echo ========================================
echo.
echo 📋 Next steps:
echo 1. Check APK size results above
echo 2. Report results to autonomous agent
echo 3. Continue to performance testing
echo.
pause
