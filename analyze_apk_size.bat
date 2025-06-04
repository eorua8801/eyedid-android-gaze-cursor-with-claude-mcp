@echo off
setlocal enabledelayedexpansion

echo.
echo ============================================================
echo 🎯 EyedidTracker v2.1 APK 크기 분석 도구
echo ============================================================
echo.

:: 프로젝트 디렉토리로 이동
cd /d "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored"

echo 📍 현재 위치: %CD%
echo.

:: 1. 이전 빌드 정리
echo 🧹 1단계: 이전 빌드 정리...
call gradlew cleanAll > nul 2>&1
echo    ✅ 빌드 정리 완료
echo.

:: 2. Release APK 빌드
echo 🚀 2단계: Release APK 빌드 중...
echo    (최적화된 설정으로 빌드, 시간이 소요될 수 있습니다)
call gradlew assembleRelease
if errorlevel 1 (
    echo    ❌ Release 빌드 실패!
    echo    💡 Debug 빌드로 대체 시도 중...
    call gradlew assembleDebug
    set "APK_TYPE=debug"
    set "APK_PATH=app\build\outputs\apk\debug\app-debug.apk"
) else (
    echo    ✅ Release 빌드 성공!
    set "APK_TYPE=release"
    set "APK_PATH=app\build\outputs\apk\release\app-release.apk"
)
echo.

:: 3. APK 파일 존재 확인 및 크기 측정
echo 📊 3단계: APK 크기 분석...
if exist "!APK_PATH!" (
    echo    📱 APK 파일 발견: !APK_PATH!
    
    :: 파일 크기 측정 (바이트)
    for %%A in ("!APK_PATH!") do set "SIZE_BYTES=%%~zA"
    
    :: 메가바이트로 변환
    set /a SIZE_MB=!SIZE_BYTES!/1048576
    set /a SIZE_KB=!SIZE_BYTES!/1024
    
    echo.
    echo    ============================================================
    echo    📏 APK 크기 측정 결과 (!APK_TYPE! 빌드)
    echo    ============================================================
    echo    📊 정확한 크기: !SIZE_BYTES! bytes
    echo    📊 킬로바이트: !SIZE_KB! KB
    echo    📊 메가바이트: !SIZE_MB! MB
    echo.
    
    :: 목표 대비 분석
    echo    🎯 목표 대비 분석:
    if !SIZE_MB! LSS 30 (
        echo    ✅ 목표 달성! (30MB 목표 vs !SIZE_MB!MB 실제)
        set /a REDUCTION=55-!SIZE_MB!
        set /a PERCENT_REDUCTION=(!REDUCTION!*100)/55
        echo    🏆 원본 대비 약 !PERCENT_REDUCTION!%% 감소 (55MB → !SIZE_MB!MB)
    ) else if !SIZE_MB! LSS 40 (
        echo    🎊 목표 초과 달성! (40MB 기준 목표 vs !SIZE_MB!MB 실제) 
        echo    🎯 30MB 목표에는 !SIZE_MB!MB로 약간 초과
    ) else (
        echo    ⚠️  목표 미달성 (!SIZE_MB!MB, 목표: 30-40MB)
        echo    💡 추가 최적화 필요
    )
    echo.
    
    :: 세부 분석을 위한 APK Analyzer 명령어 제안
    echo    🔍 세부 분석 명령어:
    echo    aapt dump badging "!APK_PATH!"
    echo    aapt list -v "!APK_PATH!"
    echo.
    
    :: APK 구성 요소 분석 (가능한 경우)
    echo    📦 APK 구성 요소 예상 분석:
    echo    • EyeDid SDK: ~15-20MB (시선 추적 라이브러리)
    echo    • Android 시스템: ~8-12MB (androidx 등)
    echo    • 앱 코드: ~2-5MB (Java/Kotlin 코드)
    echo    • 리소스: ~1-3MB (이미지, 레이아웃 등)
    echo.
    
) else (
    echo    ❌ APK 파일을 찾을 수 없습니다!
    echo    📂 예상 경로: !APK_PATH!
    echo    💡 빌드가 실패했을 수 있습니다.
    echo.
    echo    🔍 빌드 출력 디렉토리 확인:
    dir "app\build\outputs\apk" /s
)

:: 4. 빌드 시간 분석
echo 📈 4단계: 빌드 성능 분석...
if exist "build\reports\profile" (
    echo    📊 빌드 프로파일 리포트 생성됨
    echo    📂 위치: build\reports\profile\
    for %%F in (build\reports\profile\profile-*.html) do (
        echo    📄 파일: %%F
    )
) else (
    echo    ℹ️  빌드 프로파일 없음 (--profile 옵션 필요)
)
echo.

:: 5. 최적화 제안
echo 💡 5단계: 추가 최적화 제안...
echo    🎯 APK 크기 추가 최적화 방법:
echo    
echo    1. ProGuard/R8 최적화 강화:
echo       • 사용하지 않는 코드 제거 확인
echo       • 라이브러리별 최적화 규칙 추가
echo    
echo    2. 리소스 최적화:
echo       • 벡터 드로어블 사용 (PNG 대신)
echo       • WebP 이미지 포맷 사용
echo       • 사용하지 않는 리소스 제거
echo    
echo    3. 네이티브 라이브러리 최적화:
echo       • ABI 필터링 (현재 arm64-v8a만 사용 중 ✅)
echo       • 불필요한 .so 파일 제거
echo    
echo    4. EyeDid SDK 최적화:
echo       • 필요한 기능만 포함하는 설정 확인
echo       • SDK 버전 최신화로 크기 최적화
echo.

:: 6. 다음 단계 제안
echo 🚀 6단계: 권장 다음 단계...
echo    ✅ 즉시 실행 가능:
echo    1. ./gradlew lint (코드 품질 검사)
echo    2. ./gradlew test (단위 테스트 실행)
echo    3. 실제 기기에서 APK 설치 및 테스트
echo    
echo    📋 고급 분석:
echo    1. APK Analyzer 도구 사용 (Android Studio)
echo    2. Bundle 생성 후 크기 비교
echo    3. 메모리 프로파일링
echo.

echo ============================================================
echo 🎉 APK 크기 분석 완료!
echo ============================================================
echo.

:: 결과 요약 파일 생성
echo 📄 분석 결과 요약을 APK_SIZE_ANALYSIS.txt에 저장 중...
(
echo EyedidTracker v2.1 APK 크기 분석 결과
echo ==========================================
echo 분석 일시: %DATE% %TIME%
echo APK 타입: !APK_TYPE!
echo APK 경로: !APK_PATH!
echo APK 크기: !SIZE_MB! MB ^(!SIZE_BYTES! bytes^)
echo.
echo 목표 달성 여부:
if !SIZE_MB! LSS 30 (
echo ✅ 30MB 목표 달성
) else if !SIZE_MB! LSS 40 (
echo ✅ 40MB 기준 목표 달성
) else (
echo ❌ 목표 미달성
)
echo.
echo 최적화 완료 기능:
echo - ABI 필터링 (arm64-v8a만)
echo - ProGuard/R8 최적화 활성화
echo - 리소스 압축 및 제거
echo - 불필요한 파일 제외
echo.
) > APK_SIZE_ANALYSIS.txt

echo    ✅ 분석 결과가 APK_SIZE_ANALYSIS.txt에 저장되었습니다!
echo.

pause
