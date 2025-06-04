@echo off
chcp 65001 >nul
echo ========================================
echo JNI Problem Fix - EyedidTracker
echo ========================================
echo.

echo STEP 1: Clear Java temp directories...
rmdir /s /q "%TEMP%\hsperfdata_%USERNAME%" 2>nul
rmdir /s /q "%LOCALAPPDATA%\Temp\gradle" 2>nul
rmdir /s /q "%TEMP%\gradle" 2>nul
echo Java temp cleared
echo.

echo STEP 2: Set clean Java environment...
set JAVA_HOME=
set GRADLE_USER_HOME=%USERPROFILE%\.gradle
echo Environment variables reset
echo.

echo STEP 3: Force kill any remaining Java processes...
taskkill /f /im java.exe 2>nul
taskkill /f /im javaw.exe 2>nul
timeout /t 3 >nul
echo All Java processes terminated
echo.

echo STEP 4: Try Gradle with clean environment...
gradlew.bat --version --no-daemon --console=plain
echo.

echo ========================================
echo JNI Fix Completed!
echo ========================================
echo.
echo If still failing, try Android Studio directly
echo The issue may be limited to command-line only
echo.
pause
