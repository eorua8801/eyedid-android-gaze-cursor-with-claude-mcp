@echo off
REM ============================================================================
REM EyedidTracker Performance Analysis Script v2.1
REM Comprehensive performance analysis for gaze tracking applications
REM ============================================================================

title EyedidTracker Performance Analyzer

echo.
echo ============================================================================
echo   🎯 EyedidTracker Performance Analysis v2.1
echo ============================================================================
echo.

echo 📊 Starting comprehensive performance analysis...
echo.

REM ============================================================================
REM Build Performance Analysis
REM ============================================================================
echo 🏗️ BUILD PERFORMANCE ANALYSIS
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo ⏱️  Measuring build time...
set start_time=%time%
call gradlew assembleDebug --quiet
set end_time=%time%

echo ✅ Build completed!
echo 📈 Build time: %start_time% - %end_time%
echo.

REM ============================================================================
REM APK Size Analysis
REM ============================================================================
echo 📦 APK SIZE ANALYSIS
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 📱 Debug APK found:
    dir "app\build\outputs\apk\debug\app-debug.apk" | findstr "app-debug.apk"
    echo.
) else (
    echo ❌ Debug APK not found. Building first...
    call gradlew assembleDebug --quiet
)

if exist "app\build\outputs\apk\release\app-release.apk" (
    echo 🚀 Release APK found:
    dir "app\build\outputs\apk\release\app-release.apk" | findstr "app-release.apk"
    echo.
) else (
    echo 💡 Release APK not found. Run 'gradlew assembleRelease' to generate.
    echo.
)

REM ============================================================================
REM Code Quality Analysis
REM ============================================================================
echo 🔍 CODE QUALITY ANALYSIS
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo 🧪 Running lint analysis...
call gradlew lintDebug --quiet
if %errorlevel% equ 0 (
    echo ✅ Lint analysis completed successfully!
) else (
    echo ⚠️  Lint found issues. Check reports for details.
)

echo.
echo 🧪 Running unit tests...
call gradlew testDebugUnitTest --quiet
if %errorlevel% equ 0 (
    echo ✅ All unit tests passed!
) else (
    echo ⚠️  Some tests failed. Check reports for details.
)

echo.

REM ============================================================================
REM Project Structure Analysis
REM ============================================================================
echo 📊 PROJECT STRUCTURE ANALYSIS
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo 📁 Analyzing project structure...

REM Count Java files
set java_count=0
for /r %%i in (*.java) do (
    set /a java_count+=1
)

REM Count Kotlin files
set kotlin_count=0
for /r %%i in (*.kt) do (
    set /a kotlin_count+=1
)

REM Count XML files (excluding build directories)
set xml_count=0
for /r %%i in (*.xml) do (
    echo %%i | findstr /v "build" >nul
    if !errorlevel! equ 0 set /a xml_count+=1
)

echo 📝 Source files:
echo    • Java files: %java_count%
echo    • Kotlin files: %kotlin_count%
echo    • XML files: %xml_count%
echo.

REM ============================================================================
REM Performance Recommendations
REM ============================================================================
echo 💡 PERFORMANCE RECOMMENDATIONS
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo 🎯 Gaze Tracking Optimization Tips:
echo    • Ensure hardware acceleration is enabled
echo    • Monitor memory usage during extended sessions
echo    • Test on various device configurations
echo    • Profile camera initialization time
echo.

echo ⚡ Build Performance Tips:
echo    • Use 'gradlew build --parallel' for faster builds
echo    • Enable build cache with '--build-cache'
echo    • Use 'gradlew build --profile' for detailed analysis
echo    • Run 'gradlew cleanAll' if builds become inconsistent
echo.

echo 📱 Runtime Performance Tips:
echo    • Test calibration accuracy on different devices
echo    • Monitor battery usage during background tracking
echo    • Check accessibility service responsiveness
echo    • Verify overlay performance on various Android versions
echo.

REM ============================================================================
REM Report Generation
REM ============================================================================
echo 📋 REPORT GENERATION
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo 📊 Generating performance report...

set report_file=performance_report_%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%.txt
echo # EyedidTracker Performance Report > %report_file%
echo Generated: %date% %time% >> %report_file%
echo. >> %report_file%
echo ## Build Information >> %report_file%
echo - Java files: %java_count% >> %report_file%
echo - Kotlin files: %kotlin_count% >> %report_file%
echo - XML files: %xml_count% >> %report_file%
echo. >> %report_file%
echo ## APK Information >> %report_file%
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    dir "app\build\outputs\apk\debug\app-debug.apk" | findstr "app-debug.apk" >> %report_file%
)
echo. >> %report_file%
echo ## Recommendations >> %report_file%
echo - Run regular performance tests >> %report_file%
echo - Monitor memory usage >> %report_file%
echo - Check battery optimization >> %report_file%
echo - Test on multiple devices >> %report_file%

echo ✅ Performance report saved to: %report_file%
echo.

REM ============================================================================
REM Useful Commands Reference
REM ============================================================================
echo 🔧 USEFUL COMMANDS REFERENCE
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

echo 📚 Development Commands:
echo    gradlew build --profile           # Build with profiling
echo    gradlew lint                      # Code quality check
echo    gradlew test                      # Run unit tests
echo    gradlew assembleRelease          # Build release APK
echo.

echo 🔍 Analysis Commands:
echo    gradlew analyzeApkSize           # APK size analysis
echo    gradlew checkDependencyUpdates   # Check for updates
echo    gradlew performanceTest          # Performance tests
echo    gradlew analyzeProject           # Project analysis
echo.

echo 🧹 Maintenance Commands:
echo    gradlew cleanAll                 # Complete cleanup
echo    gradlew clean                    # Standard cleanup
echo    cleanup_project.bat              # Custom cleanup script
echo.

REM ============================================================================
REM Completion
REM ============================================================================
echo ============================================================================
echo   ✅ Performance Analysis Completed!
echo ============================================================================
echo.

echo 📊 Analysis Summary:
echo    • Build performance: Measured
echo    • APK size: Analyzed
echo    • Code quality: Checked
echo    • Project structure: Documented
echo    • Report generated: %report_file%
echo.

echo 💡 Next Steps:
echo    • Review the generated report
echo    • Address any lint or test issues
echo    • Monitor runtime performance on devices
echo    • Consider running 'gradlew performanceAnalysis' for deeper insights
echo.

echo 🎯 Happy gaze tracking development!
echo.

pause
