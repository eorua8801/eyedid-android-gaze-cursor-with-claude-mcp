# Eyedid SDK 안드로이드 앱 개발 가이드 - 적응형 캘리브레이션 버전

## 목차
1. [프로젝트 소개](#1-프로젝트-소개)
2. [설치 및 설정](#2-설치-및-설정)
3. [🆕 적응형 캘리브레이션 시스템](#3-적응형-캘리브레이션-시스템)
4. [앱 구조 개요](#4-앱-구조-개요)
5. [주요 컴포넌트 상세 설명](#5-주요-컴포넌트-상세-설명)
6. [좌표계 통합 및 캘리브레이션 시스템](#6-좌표계-통합-및-캘리브레이션-시스템)
7. [설정 파라미터 가이드](#7-설정-파라미터-가이드)
8. [기능 수정 및 확장 가이드](#8-기능-수정-및-확장-가이드)
9. [문제 해결 및 디버깅](#9-문제-해결-및-디버깅)
10. [향후 개발 로드맵](#10-향후-개발-로드맵)
11. [참고 자료](#11-참고-자료)

---

## 1. 프로젝트 소개

### 1.1 개요
본 프로젝트는 Eyedid(이전 SeeSo) SDK를 활용한 안드로이드 시선 추적 애플리케이션입니다. **🆕 적응형 캘리브레이션 시스템**을 통해 사용자별 최적화된 시선 추적 경험을 제공하며, 손으로 기기를 조작하지 않고도 시선만으로 스마트폰을 제어할 수 있습니다.

### 1.2 주요 기능
- **🆕 적응형 캘리브레이션**: 3단계 전략 기반 스마트 보정 시스템
- **🆕 실시간 드리프트 보정**: 사용 중 자동 정확도 조정
- **시선 고정 클릭**: 특정 위치를 일정 시간 응시하면 해당 위치를 클릭
- **화면 가장자리 스크롤**: 화면 상단 또는 하단을 일정 시간 응시하면 자동 스크롤
- **🆕 사용자 친화적 안내**: 전략별 맞춤 설명 및 피드백
- **설정 화면**: 사용자별 최적화를 위한 다양한 파라미터 조정 기능
- **시각적 피드백**: 시선 위치, 진행 상태 등을 표시하는 커서 및 UI

### 1.3 아키텍처 특징
- **🆕 적응형 캘리브레이션**: 사용자 상황에 맞는 다중 전략 시스템
- **🆕 서비스 우선 아키텍처**: 백그라운드 지속 추적과 UI 분리
- **모듈화된 구조**: 역할별로 분리된 컴포넌트 구조로 확장성 및 유지보수성 향상
- **다중 레이어 아키텍처**: 데이터, 도메인, UI 레이어 분리를 통한 관심사 분리
- **설정 관리**: 사용자 설정을 효율적으로 관리하는 저장소 패턴 적용

---

## 2. 설치 및 설정

### 2.1 시스템 요구사항
- Android 10.0 (API 레벨 29) 이상
- 전면 카메라가 있는 안드로이드 기기
- Android Studio Arctic Fox (2020.3.1) 이상

### 2.2 필요 권한
- `CAMERA`: 시선 추적을 위한 카메라 사용
- `SYSTEM_ALERT_WINDOW`: 다른 앱 위에 오버레이 표시
- `BIND_ACCESSIBILITY_SERVICE`: 시스템 제어(클릭, 스크롤) 기능
- `FOREGROUND_SERVICE`: 백그라운드 실행을 위한 포그라운드 서비스

### 2.3 프로젝트 설정 방법

1. **저장소 클론하기**
   ```bash
   git clone https://github.com/YOUR_USERNAME/EyedidTracker-Adaptive.git
   cd EyedidTracker-Adaptive
   ```

2. **Android Studio에서 프로젝트 열기**
   - Android Studio 실행 > Open an existing Android Studio project > 프로젝트 폴더 선택

3. **Eyedid SDK 키 설정**
   - `MainActivity.java` 파일에서 EYEDID_SDK_LICENSE 값을 본인의 라이센스 키로 변경:
   ```java
   private final String EYEDID_SDK_LICENSE = "your_license_key_here";
   ```

4. **빌드 및 실행**
   - Android Studio에서 'Run' 버튼 클릭 또는 `Shift+F10` 단축키 사용

### 2.4 앱 설정 방법 (최초 실행 시)

1. **🆕 캘리브레이션 전략 선택**
   - 앱 최초 실행 시 사용자 상황에 맞는 전략을 선택할 수 있습니다
   - 🚀 빠른 시작: 즉시 사용 (낮은 정확도)
   - ⚖️ 균형 모드: 표준 사용 (권장)
   - 🎯 정밀 모드: 높은 정확도 (적극 권장)

2. **오버레이 권한 허용**
   - 앱 최초 실행 시 '다른 앱 위에 표시' 권한 요청 대화상자가 나타납니다
   - '허용'을 선택하여 권한을 부여합니다

3. **접근성 서비스 활성화**
   - 앱 실행 후 접근성 서비스 설정 화면으로 이동합니다
   - 목록에서 'EyedidSampleApp'을 찾아 활성화합니다

4. **카메라 권한 허용**
   - 앱 최초 실행 시 카메라 접근 권한을 허용합니다

---

## 3. 🆕 적응형 캘리브레이션 시스템

### 3.1 시스템 개요

적응형 캘리브레이션 시스템은 사용자의 상황과 요구에 맞게 자동으로 최적화되는 지능형 보정 시스템입니다.

#### 핵심 특징
- **3단계 전략**: 상황별 최적화된 캘리브레이션 방식
- **실시간 적응**: 사용 중 지속적인 정확도 개선
- **사용자 친화적**: 명확한 안내와 투명한 피드백
- **안전성 우선**: 잘못된 학습 방지 메커니즘

### 3.2 캘리브레이션 전략

#### 🚀 빠른 시작 모드 (QUICK_START)
```java
// 특징
- 2초 빠른 1포인트 캘리브레이션
- 즉시 사용 가능
- 사용하며 점진적 정확도 향상

// 적합한 상황
- 데모 및 체험 사용
- 빠른 테스트가 필요한 경우
- 정확도보다 속도가 우선인 경우

// 주의사항
- 초기 정확도가 낮을 수 있음
- 정밀한 작업에는 부적합
```

#### ⚖️ 균형 모드 (BALANCED) - 권장
```java
// 특징  
- 스마트 초기 캘리브레이션
- 자동 정확도 모니터링
- 필요시 추가 보정 제안

// 적합한 상황
- 일반적인 일상 사용
- 대부분의 사용자에게 최적
- 속도와 정확도의 균형 필요

// 장점
- 범용적 사용성
- 상황별 자동 적응
- 사용자 부담 최소화
```

#### 🎯 정밀 모드 (PRECISION) - 적극 권장
```java
// 특징
- 5포인트 정밀 캘리브레이션
- 최고 수준의 초기 정확도
- 엄격한 품질 관리

// 적합한 상황
- 정밀한 작업이 필요한 경우
- 전문적 사용 목적
- 최고 정확도가 요구되는 환경

// 설정 시간
- 초기: 10-15초 소요
- 결과: 높은 정확도 보장
```

### 3.3 실시간 적응 메커니즘

#### 드리프트 보정 (안전함)
```java
// 물리적/환경적 변화 자동 감지 및 보정
- 머리 위치 변화 보정
- 시간 경과에 따른 하드웨어 드리프트 수정
- 조명 변화 적응
- 사용 거리 변화 대응
```

#### 패턴 학습 (매우 보수적)
```java
// 신뢰할 수 있는 경우에만 제한적 학습
if (초기_정확도 > 95% && 사용시간 > 10분 && 일관성_점수 > 90%) {
    // 매우 보수적인 가중치로 패턴 업데이트
    가중치 = 0.05; // 기존 모델 95% + 새 패턴 5%
}
```

### 3.4 사용자 인터페이스 개선

#### 전략별 맞춤 안내
```java
// 빠른 시작 모드
"🚀 빠른 시작 모드: 즉시 사용 가능하지만 정확도가 낮을 수 있습니다.
시선과 커서가 안 맞으면 눈을 억지로 움직이지 말고 '정밀 보정'을 실행해주세요."

// 균형 모드  
"⚖️ 균형 모드: 스마트 보정으로 편리하게 사용하세요.
시스템이 자동으로 정확도를 모니터링하고 필요시 추가 보정을 제안합니다."

// 정밀 모드
"🎯 정밀 모드: 정확한 보정을 위해 조금 더 시간이 걸릴 수 있어요.
편안한 자세로 화면과 30-60cm 거리를 유지해주세요."
```

#### 진행 상황 투명화
```java
// 실시간 상태 표시
- "시스템 초기화 중..."
- "시선 추적 활성화됨 ✅"  
- "서비스 연결됨 ✅"
- "오버레이 권한 필요 ⚠️"
```

### 3.5 안전성 메커니즘

#### 잘못된 학습 방지
```java
// 사용자가 억지로 눈을 움직이는 패턴 감지
if (시선_터치_거리 > 임계값 || 급격한_패턴_변화) {
    // 해당 샘플 무시
    ignore_this_sample();
    
    // 사용자에게 올바른 사용법 안내
    showToast("시선과 커서가 안 맞으면 '정렬' 버튼을 눌러주세요");
}
```

#### 이상치 탐지 및 제거
```java
// 비정상적인 시선-터치 패턴 감지
- 급격한 시선 이동 후 터치
- 일관성 없는 사용 패턴
- 물리적으로 불가능한 움직임
```

---

## 4. 앱 구조 개요

### 4.1 프로젝트 디렉토리 구조
```
app/src/main/java/camp/visual/android/sdk/sample/
├── data/                           # 데이터 레이어
│   └── settings/                   # 🆕 설정 관리 (적응형 전략 포함)
│       ├── SettingsRepository.java       # 설정 저장소 인터페이스
│       └── SharedPrefsSettingsRepository.java # SharedPreference 구현체
├── domain/                         # 비즈니스 로직 레이어
│   └── model/                      # 도메인 모델
│       └── 🆕 UserSettings.java          # 사용자 설정 모델 (적응형 전략 포함)
├── service/                        # 안드로이드 서비스
│   ├── tracking/                   # 시선 추적 서비스
│   │   └── 🆕 GazeTrackingService.java   # 핵심 시선 추적 서비스 (적응형 로직)
│   └── accessibility/              # 접근성 서비스
│       └── MyAccessibilityService.java  # 시스템 제어 서비스
├── ui/                             # 프레젠테이션 레이어
│   ├── main/                       # 메인 화면
│   │   └── 🆕 MainActivity.java          # 메인 액티비티 (적응형 UI)
│   ├── settings/                   # 설정 화면
│   │   └── 🆕 SettingsActivity.java      # 설정 액티비티 (전략 선택)
│   └── views/                      # 커스텀 뷰
│       ├── CalibrationViewer.java       # 캘리브레이션 화면
│       └── PointView.java               # 시선 포인트 표시 뷰
└── AndroidManifest.xml             # 앱 매니페스트
```

### 4.2 🆕 적응형 시스템 주요 변경사항

#### 4.2.1 아키텍처 개선
- **서비스 우선 구조**: GazeTrackingService가 주 SDK 관리, MainActivity는 UI 전용
- **이중 SDK 관리 방지**: 서비스 실행 시 MainActivity SDK 자동 해제
- **상태 관리 개선**: 실시간 연결 상태 모니터링 및 표시

#### 4.2.2 새로운 컴포넌트
- **UserSettings.CalibrationStrategy**: 3단계 전략 열거형
- **적응형 캘리브레이션 로직**: GazeTrackingService 내 통합
- **사용자 안내 시스템**: 전략별 맞춤 메시지 및 피드백

#### 4.2.3 UI/UX 개선
- **진행 상황 표시**: 실시간 시스템 상태 안내
- **전략별 설명**: 각 모드의 특징과 적합한 상황 안내
- **오류 상황 가이드**: 권한 요청 및 설정 방법 상세 안내

---

## 5. 주요 컴포넌트 상세 설명

### 5.1 🆕 GazeTrackingService (적응형 버전)

적응형 캘리브레이션 시스템의 핵심으로, Eyedid SDK 관리와 실시간 적응 로직을 담당합니다.

#### 🆕 새로운 주요 역할
1. **적응형 캘리브레이션 관리**: 전략별 캘리브레이션 실행
2. **실시간 드리프트 보정**: 자동 오프셋 조정 및 정확도 모니터링
3. **서비스 우선 SDK 관리**: MainActivity와의 충돌 방지
4. **안전한 패턴 학습**: 신뢰할 수 있는 경우에만 제한적 학습

#### 🆕 주요 신규 메서드
- **startOnePointCalibrationWithOffset()**: 빠른 1포인트 + 오프셋 보정
- **triggerCalibration()**: 외부에서 캘리브레이션 트리거 (MainActivity에서 호출)
- **forceResetCursorOffset()**: 강제 오프셋 리셋
- **updateUserSettings()**: 설정 변경 시 실시간 반영

### 5.2 🆕 MainActivity (적응형 UI 버전)

사용자 친화적 적응형 캘리브레이션 인터페이스를 제공합니다.

#### 🆕 새로운 주요 역할
1. **전략별 사용자 안내**: 각 모드의 특징과 사용법 설명
2. **서비스 상태 모니터링**: 실시간 연결 상태 확인 및 표시
3. **권한 설정 가이드**: 단계별 권한 설정 안내
4. **캘리브레이션 제어**: 정렬 및 정밀 보정 기능

#### 🆕 주요 신규 메서드
- **showWelcomeMessage()**: 전략별 환영 메시지 표시
- **getCalibrationMessage()**: 전략별 캘리브레이션 안내 메시지
- **showPrecisionCalibrationDialog()**: 정밀 보정 안내 대화상자
- **startServicesAndCheckPermissions()**: 서비스 시작 및 권한 종합 관리
- **updateStatusText()**: 실시간 상태 표시

### 5.3 🆕 UserSettings (적응형 전략 포함)

사용자 설정에 적응형 캘리브레이션 전략 선택 기능이 추가되었습니다.

#### 🆕 새로운 속성
```java
public enum CalibrationStrategy {
    QUICK_START,    // 빠른 시작
    BALANCED,       // 균형 (기본값)
    PRECISION       // 정밀
}

// 새로운 설정 속성
private CalibrationStrategy calibrationStrategy = CalibrationStrategy.BALANCED;
```

---

## 6. 좌표계 통합 및 캘리브레이션 시스템

### 6.1 🆕 적응형 캘리브레이션 통합

#### 6.1.1 기존 문제 해결
- ✅ **SDK 충돌 해결**: 서비스 우선 구조로 MainActivity-Service 간 충돌 방지
- ✅ **좌표계 통합**: 터치 위치 불일치 문제 완전 해결
- ✅ **캘리브레이션 안정성**: 이중 SDK 관리 문제 근본 해결

#### 6.1.2 🆕 적응형 시스템 동작 방식
```java
// 1. 서비스 상태 확인
if (isServiceRunning()) {
    // 서비스에서 캘리브레이션 실행 (우선)
    GazeTrackingService.getInstance().triggerCalibration();
} else {
    // MainActivity에서 실행 (백업)
    gazeTracker.startCalibration(calibrationType);
}

// 2. 전략별 캘리브레이션 실행
switch (userSettings.getCalibrationStrategy()) {
    case QUICK_START:
        service.startOnePointCalibrationWithOffset();
        break;
    case PRECISION:
        service.triggerCalibration(); // 5포인트 정밀
        break;
}
```

### 6.2 🆕 실시간 적응 메커니즘

#### 6.2.1 드리프트 보정 (자동)
```java
// 시간 기반 선형 드리프트 보정
private void applyTimeBasedDriftCorrection() {
    long elapsedTime = System.currentTimeMillis() - calibrationTime;
    float driftX = calculateTimeDrift(elapsedTime, DRIFT_RATE_X);
    float driftY = calculateTimeDrift(elapsedTime, DRIFT_RATE_Y);
    
    // 오프셋 자동 조정
    adjustOffset(driftX, driftY);
}
```

#### 6.2.2 안전한 패턴 학습 (선택적)
```java
// 매우 보수적인 학습 조건
private boolean canTrustUserPattern() {
    return 초기정확도 > 0.95f &&          // 초기 정확도 95% 이상
           사용시간 > 10 * 60 * 1000 &&    // 10분 이상 사용
           일관성점수 > 0.90f &&           // 일관성 90% 이상
           !detectForcedEyeMovement();     // 억지 시선 움직임 없음
}
```

---

## 7. 설정 파라미터 가이드

### 7.1 🆕 적응형 캘리브레이션 설정

| 파라미터 | 설명 | 기본값 | 설정 위치 | 영향 |
|---------|------|--------|----------|------|
| calibrationStrategy | 캘리브레이션 전략 | BALANCED | SettingsActivity | 전체 사용자 경험 결정 |
| adaptiveLearningEnabled | 적응 학습 활성화 | false | UserSettings | 패턴 학습 사용 여부 |
| driftCorrectionEnabled | 드리프트 보정 활성화 | true | UserSettings | 자동 정확도 유지 |
| conservativeLearningWeight | 학습 가중치 | 0.05 | GazeTrackingService | 패턴 학습 영향도 |

#### 🆕 전략별 권장 설정
```java
// 빠른 시작 모드 권장 설정
UserSettings quickSettings = new UserSettings.Builder()
    .calibrationStrategy(CalibrationStrategy.QUICK_START)
    .fixationDurationMs(1200f)     // 조금 더 느린 클릭 인식
    .aoiRadius(50f)                // 더 넓은 클릭 영역
    .driftCorrectionEnabled(true)  // 드리프트 보정 필수
    .build();

// 정밀 모드 권장 설정  
UserSettings precisionSettings = new UserSettings.Builder()
    .calibrationStrategy(CalibrationStrategy.PRECISION)
    .fixationDurationMs(800f)      // 빠른 클릭 인식
    .aoiRadius(30f)                // 정밀한 클릭 영역
    .adaptiveLearningEnabled(true) // 신중한 패턴 학습
    .build();
```

### 7.2 기존 설정 (변경 없음)

| 파라미터 | 설명 | 기본값 | 권장 범위 | 영향 |
|---------|------|--------|----------|------|
| fixationDurationMs | 클릭 인식 시간(ms) | 1000 | 500-2000 | 클릭 속도 vs 정확도 |
| aoiRadius | 관심 영역 반경(px) | 40 | 20-70 | 클릭 정밀도 vs 편의성 |
| edgeMarginRatio | 가장자리 인식 비율 | 0.01 | 0.005-0.05 | 스크롤 트리거 민감도 |
| edgeTriggerMs | 스크롤 트리거 시간(ms) | 3000 | 1000-5000 | 스크롤 속도 |

---

## 8. 기능 수정 및 확장 가이드

### 8.1 🆕 적응형 시스템 커스터마이징

#### 8.1.1 새로운 캘리브레이션 전략 추가
```java
// 1. UserSettings.java에 새 전략 추가
public enum CalibrationStrategy {
    QUICK_START,
    BALANCED, 
    PRECISION,
    PROFESSIONAL  // 새 전략 추가
}

// 2. GazeTrackingService.java에 로직 구현
private void executeProfessionalCalibration() {
    // 9포인트 초정밀 캘리브레이션 로직
    // 더 엄격한 정확도 기준
    // 고급 사용자를 위한 세밀한 조정
}
```

#### 8.1.2 적응 학습 알고리즘 개선
```java
// 머신러닝 기반 패턴 학습 (고급)
private void applyAdvancedPatternLearning(float gazeX, float gazeY, float touchX, float touchY) {
    // 사용자별 시선 특성 학습
    UserProfile profile = userProfileManager.getCurrentProfile();
    
    // 시간대별 정확도 패턴 분석
    TimeBasedAccuracy timePattern = analyzeTimeBasedPattern();
    
    // 환경별 보정 계수 학습
    EnvironmentCorrection envCorrection = analyzeEnvironmentPattern();
    
    // 종합 보정값 계산 및 적용
    OffsetCorrection correction = calculateOptimalCorrection(profile, timePattern, envCorrection);
    applyCorrection(correction, CONSERVATIVE_WEIGHT);
}
```

### 8.2 🆕 사용자 경험 개선

#### 8.2.1 지능형 안내 시스템
```java
// 상황별 맞춤 안내
private void provideContextualGuidance() {
    CalibrationQuality quality = assessCurrentQuality();
    UsagePattern pattern = analyzeUsagePattern();
    
    if (quality.isLow() && pattern.isPrecisionRequired()) {
        showMessage("🎯 정밀한 작업을 위해 '정밀 보정'을 권장합니다.");
    } else if (quality.isDegrading()) {
        showMessage("⚡ 정확도가 떨어지고 있습니다. '정렬' 버튼으로 빠른 보정을 해보세요.");
    }
}
```

---

## 9. 문제 해결 및 디버깅

### 9.1 🆕 적응형 시스템 관련 문제

| 문제 | 가능한 원인 | 해결 방법 |
|-----|-----------|---------|
| 적응형 학습이 오히려 정확도를 떨어뜨림 | 잘못된 터치 패턴 학습 | 설정에서 적응 학습 비활성화, 정밀 보정 재실행 |
| 전략 변경 후 정확도 급락 | 이전 학습 데이터 잔존 | 앱 데이터 초기화 또는 강제 재캘리브레이션 |
| 서비스 연결 불안정 | 서비스-액티비티 동기화 문제 | 앱 완전 종료 후 재시작 |
| 캘리브레이션이 전략과 다르게 실행됨 | 설정 저장/로드 오류 | 설정 화면에서 전략 재선택 및 저장 |

### 9.2 🆕 적응형 시스템 디버깅

#### 9.2.1 학습 상태 모니터링
```java
// 학습 진행 상황 로깅
private void logLearningProgress() {
    Log.d(TAG, String.format("학습 통계 - 정확도: %.2f%%, 샘플: %d개, 신뢰도: %.2f%%",
            currentAccuracy * 100, totalSamples, confidenceScore * 100));
    
    Log.d(TAG, String.format("드리프트 보정 - X: %.2f, Y: %.2f, 경과시간: %d",
            driftOffsetX, driftOffsetY, timeSinceLastCalibration));
}
```

#### 9.2.2 전략별 성능 분석
```java
// 전략별 성능 메트릭 수집
private void collectStrategyMetrics() {
    PerformanceMetrics metrics = new PerformanceMetrics.Builder()
        .strategy(currentStrategy)
        .accuracy(calculateAccuracy())
        .responseTime(calculateResponseTime())
        .userSatisfaction(getUserSatisfactionScore())
        .usageTime(getUsageTime())
        .build();
        
    metricsCollector.record(metrics);
}
```

### 9.3 기존 문제 해결 (업데이트)

✅ **해결된 문제들**:
- 시선 추적이 시작되지 않음 → 서비스 우선 구조로 안정성 개선
- 캘리브레이션이 작동하지 않음 → 이중 SDK 관리 문제 해결
- 터치 위치가 부정확함 → 좌표계 변환 완전 해결

⚠️ **모니터링 필요**:
- 적응형 학습으로 인한 새로운 정확도 문제
- 전략 변경 시 일시적 성능 저하
- 메모리 사용량 증가 (학습 데이터 저장)

---

## 10. 향후 개발 로드맵

### 10.1 🆕 적응형 시스템 고도화

#### 1단계: 안정화 (즉시)
- **패턴 학습 안전성 강화**: 잘못된 학습 방지 알고리즘 고도화
- **전략별 최적화**: 각 전략의 성능 및 사용자 경험 개선
- **실시간 모니터링**: 정확도 저하 조기 감지 및 대응

#### 2단계: 지능화 (3개월)
- **머신러닝 통합**: TensorFlow Lite 기반 개인화 모델
- **환경 적응**: 조명, 거리, 각도 변화 자동 감지 및 보정
- **사용 패턴 분석**: 개인별 최적 전략 자동 추천

#### 3단계: 고도화 (6개월)
- **예측 캘리브레이션**: AI 기반 정확도 예측 및 사전 보정
- **멀티모달 보정**: 음성, 제스처 등 다중 입력 통합
- **클라우드 학습**: 익명화된 집단 학습 데이터 활용

### 10.2 사용자 경험 개선
- **🆕 온보딩 시스템**: 신규 사용자를 위한 단계별 가이드
- **🆕 성능 대시보드**: 사용 통계 및 정확도 변화 시각화
- **🆕 맞춤형 설정**: 사용 패턴 기반 자동 설정 추천
- **🆕 접근성 강화**: 시각/청각 장애인을 위한 고급 피드백

### 10.3 기술적 발전
- **🆕 실시간 품질 보장**: 지속적 정확도 모니터링 및 자동 개선
- **🆕 크로스 플랫폼**: iOS, 웹 버전과의 설정 동기화
- **🆕 개발자 도구**: 캘리브레이션 성능 분석 도구 제공
- **🆕 API 확장**: 서드파티 앱 통합을 위한 SDK 제공

---

## 11. 참고 자료

### 11.1 Eyedid SDK 문서
- [SDK 개요](https://docs.eyedid.ai/docs/document/eyedid-sdk-overview)
- [안드로이드 퀵 스타트 가이드](https://docs.eyedid.ai/docs/quick-start/android-quick-start)
- [API 문서](https://docs.eyedid.ai/docs/api/android-api-docs/)
- [캘리브레이션 가이드](https://docs.eyedid.ai/docs/document/calibration-overview)

### 11.2 🆕 적응형 시스템 관련 연구
- [적응형 시선 추적 캘리브레이션 연구](https://research.example.com/adaptive-calibration)
- [시선 추적기의 실시간 드리프트 보정](https://research.example.com/drift-correction)
- [개인화된 시선 추적을 위한 머신러닝](https://research.example.com/ml-gaze-tracking)
- [적응형 인터페이스의 사용자 경험](https://research.example.com/adaptive-ui-ux)

### 11.3 안드로이드 개발 관련
- [안드로이드 접근성 서비스 가이드](https://developer.android.com/guide/topics/ui/accessibility/service)
- [시스템 오버레이 가이드](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [포그라운드 서비스 관리](https://developer.android.com/guide/components/foreground-services)
- [SharedPreferences 최적화](https://developer.android.com/reference/android/content/SharedPreferences)

---

## 🆕 부록: 적응형 시스템 개발 회고

### A.1 주요 성과
1. **사용자 친화성 대폭 개선**: 복잡한 캘리브레이션을 간단한 전략 선택으로 단순화
2. **안정성 확보**: 서비스 우선 구조로 SDK 충돌 문제 근본 해결
3. **확장성 확보**: 새로운 전략 및 기능 추가가 용이한 구조 설계
4. **안전성 우선**: 잘못된 학습 방지로 사용자 경험 저하 방지

### A.2 기술적 도전과 해결
1. **좌표계 통합**: 85px 오차 문제를 체계적 분석으로 완전 해결
2. **이중 SDK 관리**: 서비스-액티비티 역할 분담으로 충돌 방지
3. **실시간 적응**: 보수적 접근으로 안전하고 효과적인 학습 구현
4. **사용자 안내**: 전략별 맞춤 메시지로 이해도 향상

### A.3 사용자 경험 개선
1. **투명성**: 시스템 상태를 실시간으로 명확히 표시
2. **선택권**: 사용자가 원하는 수준의 정확도/속도 선택 가능
3. **안전성**: 억지 패턴 학습 방지로 자연스러운 사용 유도
4. **접근성**: 단계별 권한 설정 가이드로 설치 장벽 최소화

이 적응형 캘리브레이션 시스템은 기존 시선 추적 앱의 사용성과 정확성을 크게 개선하며, 향후 더욱 지능화된 시스템으로 발전할 수 있는 견고한 기반을 제공합니다.
