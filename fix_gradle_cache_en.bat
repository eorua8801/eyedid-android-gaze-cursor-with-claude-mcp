@echo off
chcp 65001 >nul
echo ========================================
echo EyedidTracker Gradle Cache Fix Tool
echo ========================================
echo.

echo Step 1: Clean project...
call gradlew clean
if errorlevel 1 (
    echo Warning: Clean failed, continuing...
)
echo Project clean completed
echo.

echo Step 2: Remove corrupted Gradle caches...
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\jars-9" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\daemon" 2>nul
echo Gradle caches cleared
echo.

echo Step 3: Clean local build caches...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "app\build" 2>nul
rmdir /s /q "build" 2>nul
echo Local caches cleared
echo.

echo Step 4: Stop Gradle daemon...
call gradlew --stop
echo Gradle daemon stopped
echo.

echo Step 5: Regenerate Gradle wrapper...
call gradlew wrapper --gradle-version=8.11.1 --distribution-type=all
echo Gradle wrapper regenerated
echo.

echo Step 6: Create new caches...
call gradlew tasks
echo New caches created
echo.

echo ========================================
echo SUCCESS: Gradle cache fix completed!
echo ========================================
echo.
echo Next steps:
echo 1. Close Android Studio completely
echo 2. Reopen EyedidTracker project
echo 3. Click 'Sync Project with Gradle Files'
echo 4. Report result to autonomous agent
echo.
pause
