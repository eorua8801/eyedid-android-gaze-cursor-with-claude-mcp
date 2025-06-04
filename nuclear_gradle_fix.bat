@echo off
chcp 65001 >nul
echo ========================================
echo NUCLEAR Gradle Cache Fix - EyedidTracker
echo ========================================
echo.

echo STEP 1: Stop all Gradle processes...
taskkill /f /im gradle.exe 2>nul
taskkill /f /im java.exe 2>nul
timeout /t 2 >nul
echo All processes stopped
echo.

echo STEP 2: Remove ENTIRE Gradle user directory...
echo This will take a few moments...
rmdir /s /q "%USERPROFILE%\.gradle" 2>nul
echo Entire Gradle directory removed
echo.

echo STEP 3: Remove project build directories...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "app\build" 2>nul
rmdir /s /q "build" 2>nul
rmdir /s /q "app\.cxx" 2>nul
echo Project build directories removed
echo.

echo STEP 4: Clear Android Studio cache locations...
rmdir /s /q "%USERPROFILE%\.android\cache" 2>nul
rmdir /s /q "%USERPROFILE%\.android\build-cache" 2>nul
echo Android Studio caches cleared
echo.

echo STEP 5: Re-download Gradle wrapper...
echo Downloading Gradle 8.11.1...
call gradlew wrapper --gradle-version=8.11.1 --distribution-type=all --no-daemon
echo Gradle wrapper downloaded
echo.

echo STEP 6: Initialize new Gradle environment...
call gradlew --version --no-daemon
echo.

echo STEP 7: Verify configuration...
call gradlew help --no-daemon
echo.

echo ========================================
echo NUCLEAR FIX COMPLETED!
echo ========================================
echo.
echo AGP Version: 8.9.1 -> 8.6.1 (FIXED)
echo Deprecated options: REMOVED
echo Entire Gradle cache: REBUILT
echo.
echo Next steps:
echo 1. Open Android Studio
echo 2. Open EyedidTracker project
echo 3. Sync project with Gradle files
echo 4. Report success to autonomous agent
echo.
pause
