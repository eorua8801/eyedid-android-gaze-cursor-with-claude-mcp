# 🔄 새 채팅 연결 체크리스트

## ⚡ 30초 빠른 연결 가이드

### 1️⃣ MCP 환경 확인 (필수)
```bash
# 첫 번째로 실행할 명령어들
list_allowed_directories
# 결과: C:\ 가 있어야 함

directory_tree "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored"
# 프로젝트 구조 확인
```

### 2️⃣ 프로젝트 상태 파악 (1분)
```bash
# Git 상태 확인
read_file "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored\NEW_CHAT_CONTEXT.md"

# 최신 작업 내용 확인  
read_file "C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored\v2.1_COMPLETION_REPORT.md"

# Git 상태 확인 (터미널에서)
# git status
# git log --oneline -5
```

### 3️⃣ 핵심 파일 위치 확인
```bash
✅ 빌드 설정: app/build.gradle.kts, gradle.properties
✅ 코드 품질: config/lint.xml
✅ CI/CD: .github/workflows/ci.yml  
✅ 테스트: app/src/test/.../performance/, .../quality/
✅ 도구: analyze_performance.bat, cleanup_project.bat
```

## 🎯 작업 재개 템플릿

### 🆕 새 채팅 시작 메시지 템플릿
```
안녕하세요! EyedidTracker v2.1 프로젝트 작업을 이어서 진행하려고 합니다.

📍 프로젝트 정보:
- 위치: C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored  
- 상태: v2.1 대개조 완료 (빌드 최적화, CI/CD 구축, 테스트 완비)
- MCP: 파일 시스템 접근 필요

🎯 오늘 작업 목표:
[구체적인 목표 작성]

먼저 MCP 환경과 프로젝트 상태를 확인해주세요:
1. list_allowed_directories (C:\ 접근 확인)
2. NEW_CHAT_CONTEXT.md 파일 읽기 (전체 컨텍스트 파악)
```

### 📋 작업별 빠른 시작 명령어

#### 🏗️ 빌드 관련 작업
```bash
cd C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored
./gradlew build --profile
./gradlew lint
```

#### 🔍 코드 분석 작업
```bash
read_file "app/build.gradle.kts"
read_file "config/lint.xml"
list_directory "app/src/main/java/camp/visual/android/sdk/sample"
```

#### 🧪 테스트 관련 작업
```bash
./gradlew test
read_file "app/src/test/.../performance/GazeTrackingPerformanceTest.java"
```

#### 📊 성능 분석 작업
```bash
analyze_performance.bat
./gradlew analyzeApkSize
```

## 🚨 주의사항 & 금지사항

### ❌ 절대 수정하면 안 되는 것들
- **v2.0 안전 장치**: 5단계 안전 검사 시스템
- **정밀 모드 기본값**: 95% 정확도 보장 설정
- **보수적 학습**: 1%만 반영, 50번마다 적용

### ⚠️ 주의할 점
- 빌드 전에 항상 `./gradlew clean` 실행
- 큰 변경 전에 Git 커밋 필수
- 성능 관련 수정 시 테스트 실행 필수

## 🎯 우선순위 작업 목록 (참고용)

### 🥇 1순위: 즉시 가능한 작업
- APK 빌드 및 크기 확인
- 실제 성능 테스트 실행
- Lint 검사 결과 확인
- Git 커밋 상태 정리

### 🥈 2순위: 중기 작업
- EyeDid SDK 라이센스 키 설정
- 실제 기기에서 테스트
- UI/UX 개선
- 추가 최적화

### 🥉 3순위: 장기 작업  
- iOS 버전 개발
- AI 기반 적응 시스템
- 고급 분석 도구
- 크로스 플랫폼 확장

## 💡 효율적인 작업 진행 팁

1. **작업 시작 시**: 항상 컨텍스트 파일부터 읽기
2. **코드 수정 시**: 작은 단위로 커밋하며 진행
3. **테스트 중요**: 성능 관련 수정 시 반드시 테스트
4. **문서화**: 중요한 변경사항은 즉시 기록
5. **백업**: 대규모 수정 전 Git 커밋 필수

---

💡 **이 체크리스트로 새 채팅에서도 5분 안에 작업 재개 가능!**
