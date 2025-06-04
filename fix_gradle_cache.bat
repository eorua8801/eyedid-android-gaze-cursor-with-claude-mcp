@echo off
echo ========================================
echo 🔧 EyedidTracker Gradle 캐시 수정 도구
echo ========================================
echo.

echo 📂 현재 프로젝트 정리 중...
call gradlew clean
echo ✅ 프로젝트 정리 완료

echo.
echo 🗑️  Gradle 캐시 정리 중...
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\jars-9" 2>nul
echo ✅ Gradle 캐시 정리 완료

echo.
echo 🧹 로컬 빌드 캐시 정리 중...
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "app\build" 2>nul
rmdir /s /q "build" 2>nul
echo ✅ 로컬 캐시 정리 완료

echo.
echo 🔄 Gradle 래퍼 재검증 중...
call gradlew wrapper --gradle-version=8.11.1 --distribution-type=all
echo ✅ Gradle 래퍼 재검증 완료

echo.
echo 🎯 새로운 캐시 생성 중...
call gradlew tasks --all
echo ✅ 새로운 캐시 생성 완료

echo.
echo ========================================
echo ✅ Gradle 캐시 수정 완료!
echo ========================================
echo.
echo 📋 다음 단계:
echo 1. Android Studio에서 프로젝트 재오픈
echo 2. "Sync Project with Gradle Files" 실행
echo 3. 성공 여부를 자율 에이전트에게 보고
echo.
pause
