@echo off
echo 🚀 EyedidTracker Release APK 빌드 시작...
echo.

cd /d "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored"

echo 📊 빌드 환경 확인...
echo 프로젝트 위치: %CD%
echo.

echo 🧹 이전 빌드 정리...
call gradlew clean

echo.
echo 🔨 Release APK 빌드 시작...
call gradlew assembleRelease --profile --info

echo.
echo ✅ 빌드 완료! 결과 확인...
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo 🎉 Release APK 생성 성공!
    dir "app\build\outputs\apk\release\app-release.apk"
) else (
    echo ❌ Release APK 생성 실패
    echo 빌드 로그를 확인하세요.
)

echo.
echo 📁 출력 폴더: app\build\outputs\apk\release\
echo.
pause
