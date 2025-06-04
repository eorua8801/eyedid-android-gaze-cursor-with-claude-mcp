@echo off
echo 🚀 EyedidTracker v2.1 빌드 테스트 시작
echo.

echo 📍 프로젝트 디렉토리로 이동...
cd /d "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored"

echo 🧹 이전 빌드 정리...
call gradlew cleanAll

echo 📊 최적화된 빌드 실행 (프로파일링 포함)...
call gradlew build --profile --info

echo.
echo ✅ 빌드 테스트 완료!
echo 📄 빌드 리포트는 build\reports\profile\ 폴더에서 확인 가능합니다.
pause
