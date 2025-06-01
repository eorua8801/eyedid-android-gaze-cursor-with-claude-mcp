@echo off
echo Cleaning Android project...
echo.

echo Removing app/build folder...
if exist "app\build" (
    rmdir /s /q "app\build"
    echo app/build deleted successfully
) else (
    echo app/build folder not found
)

echo.
echo Removing .gradle folder...
if exist ".gradle" (
    rmdir /s /q ".gradle"
    echo .gradle deleted successfully  
) else (
    echo .gradle folder not found
)

echo.
echo Removing .idea/caches folder...
if exist ".idea\caches" (
    rmdir /s /q ".idea\caches"
    echo .idea/caches deleted successfully
) else (
    echo .idea/caches folder not found
)

echo.
echo Cleanup completed!
echo Please restart Android Studio and rebuild project.
echo.
pause
