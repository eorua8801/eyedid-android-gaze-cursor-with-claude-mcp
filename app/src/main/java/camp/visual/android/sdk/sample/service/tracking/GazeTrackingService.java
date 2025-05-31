package camp.visual.android.sdk.sample.service.tracking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.data.repository.EyeTrackingRepository;
import camp.visual.android.sdk.sample.data.repository.EyedidTrackingRepository;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.android.sdk.sample.domain.interaction.ClickDetector;
import camp.visual.android.sdk.sample.domain.interaction.EdgeScrollDetector;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.ui.main.MainActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.OverlayCursorView;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.filter.OneEuroFilterManager;
import camp.visual.eyedid.gazetracker.metrics.BlinkInfo;
import camp.visual.eyedid.gazetracker.metrics.FaceInfo;
import camp.visual.eyedid.gazetracker.metrics.GazeInfo;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class GazeTrackingService extends Service {

    private static final String TAG = "GazeTrackingService";
    private static final String CHANNEL_ID = "GazeTrackingServiceChannel";

    // 컴포넌트
    private EyeTrackingRepository trackingRepository;
    private SettingsRepository settingsRepository;
    private UserSettings userSettings;
    private ClickDetector clickDetector;
    private EdgeScrollDetector edgeScrollDetector;

    // 시스템 서비스 및 UI
    private WindowManager windowManager;
    private OverlayCursorView overlayCursorView;
    private CalibrationViewer calibrationViewer;
    private Vibrator vibrator;
    private OneEuroFilterManager oneEuroFilterManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 상태 변수
    private long lastValidTimestamp = 0;
    private long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN = 1500;
    private boolean isCalibrating = false;
    private boolean skipProgress = false;

    // 1포인트 캘리브레이션 및 통합 오프셋 관련 변수
    private boolean isOnePointCalibration = false;
    private boolean offsetApplied = false;

    // 오프셋 계산 관련 변수들
    private boolean waitingForOffsetCalculation = false;
    private float targetX = 0f;
    private float targetY = 0f;
    private int validGazeCount = 0;
    private float sumGazeX = 0f;
    private float sumGazeY = 0f;

    // 🚨 백그라운드 학습 관련 필드들 - 매우 제한적으로 변경
    private boolean backgroundLearningEnabled = false;
    private float learningOffsetX = 0f;
    private float learningOffsetY = 0f;
    private int learningCount = 0;
    private static final int LEARNING_UPDATE_INTERVAL = 50; // 10번에서 50번으로 변경 (매우 보수적)
    private static final float LEARNING_RATE = 0.01f; // 0.05f에서 0.01f로 변경 (매우 보수적)
    private static final float LEARNING_THRESHOLD = 100f; // 새로 추가: 오차 100px 이상일 때만 학습

    // 📊 정확도 모니터링 관련 필드들 - 더 엄격하게 변경
    private int totalInteractions = 0;
    private int accurateInteractions = 0;
    private long lastAccuracyCheck = 0;
    private static final long ACCURACY_CHECK_INTERVAL = 120000; // 1분에서 2분으로 변경
    private static final float ACCURACY_THRESHOLD = 60f; // 70%에서 60%로 변경 (더 엄격)

    // 서비스 인스턴스 (캘리브레이션 트리거용)
    private static GazeTrackingService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 초기화
        initRepositories();
        resetCursorOffset(); // 서비스 시작 시 오프셋 초기화
        initDetectors();
        createNotificationChannel();
        initSystemServices();
        initViews();
        initGazeTracker();

        // 서비스 실행 상태 확인
        checkAccessibilityService();
    }

    private void initRepositories() {
        trackingRepository = new EyedidTrackingRepository();
        settingsRepository = new SharedPrefsSettingsRepository(this);
        userSettings = settingsRepository.getUserSettings();
    }

    /**
     * 커서 오프셋을 초기화하는 메서드
     */
    private void resetCursorOffset() {
        Log.d(TAG, "커서 오프셋 초기화 시작");
        if (settingsRepository instanceof SharedPrefsSettingsRepository) {
            ((SharedPrefsSettingsRepository) settingsRepository)
                    .saveIntegratedCursorOffset(0f, 0f);
            Log.d(TAG, "SharedPreferences에 오프셋 (0, 0) 저장 완료");
        }
        // 설정 새로고침
        userSettings = settingsRepository.getUserSettings();
        Log.d(TAG, "설정 새로고침 후 오프셋: X=" + userSettings.getCursorOffsetX() +
                ", Y=" + userSettings.getCursorOffsetY());
    }

    private void initDetectors() {
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);

        // OneEuroFilterManager를 프리셋 또는 사용자 설정값으로 초기화 (float 캐스팅)
        oneEuroFilterManager = new OneEuroFilterManager(
                2,  // count (x, y 좌표)
                (float) userSettings.getOneEuroFreq(),
                (float) userSettings.getOneEuroMinCutoff(),
                (float) userSettings.getOneEuroBeta(),
                (float) userSettings.getOneEuroDCutoff()
        );

        // 기존에 저장된 커서 오프셋이 있으면 바로 적용
        if (userSettings.getCursorOffsetX() != 0f || userSettings.getCursorOffsetY() != 0f) {
            offsetApplied = true;
            Log.d(TAG, "기존 커서 오프셋 적용: X=" + userSettings.getCursorOffsetX() + ", Y=" + userSettings.getCursorOffsetY());
        }

        Log.d(TAG, "OneEuroFilter 초기화 - 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
        Log.d(TAG, "OneEuroFilter 파라미터 - freq: " + userSettings.getOneEuroFreq() +
                ", minCutoff: " + userSettings.getOneEuroMinCutoff() +
                ", beta: " + userSettings.getOneEuroBeta() +
                ", dCutoff: " + userSettings.getOneEuroDCutoff());
    }

    private void initSystemServices() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("시선 추적 실행 중")
                .setContentText("백그라운드에서 시선을 추적하고 있습니다")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
    }

    private void initViews() {
        // 시선 커서 뷰 초기화 및 추가
        overlayCursorView = new OverlayCursorView(this);

        WindowManager.LayoutParams cursorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlayCursorView, cursorParams);

        // 캘리브레이션 뷰 초기화 및 추가 (숨겨진 상태로)
        calibrationViewer = new CalibrationViewer(this);
        WindowManager.LayoutParams calibrationParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        calibrationParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(calibrationViewer, calibrationParams);
        calibrationViewer.setVisibility(View.INVISIBLE);

        // 커서 뷰가 생성된 후 좌표 보정 확인
        handler.post(() -> {
            int statusBarHeight = getStatusBarHeight();
            Log.d(TAG, "오버레이 뷰 상태바 높이: " + statusBarHeight);
        });
    }

    private void initGazeTracker() {
        trackingRepository.initialize(this, (tracker, error) -> {
            if (tracker != null) {
                trackingRepository.setTrackingCallback(trackingCallback);
                trackingRepository.setCalibrationCallback(calibrationCallback);
                trackingRepository.startTracking();
                Log.d(TAG, "GazeTracker 초기화 성공");

                // 🎯 스마트 캘리브레이션 시작 (기존 자동 캘리브레이션 대체)
                if (userSettings.isAutoOnePointCalibrationEnabled() && !isCalibrating) {
                    Log.d(TAG, "스마트 캘리브레이션 시작");
                    startSmartCalibration();
                }
            } else {
                Log.e(TAG, "GazeTracker 초기화 실패: " + error);
                Toast.makeText(this, "시선 추적 초기화 실패", Toast.LENGTH_LONG).show();
            }
        });
    }

    // 🎯 스마트 캘리브레이션 시스템 (기본값을 정밀 보정으로 변경)
    private void startSmartCalibration() {
        if (!userSettings.isAutoOnePointCalibrationEnabled()) {
            Log.d(TAG, "자동 캘리브레이션 비활성화됨");
            return;
        }

        UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();
        Log.d(TAG, "캘리브레이션 전략: " + strategy.getDisplayName());

        switch (strategy) {
            case QUICK_START:
                // ⚠️ 빠른 시작이지만 경고와 함께 실행
                Log.w(TAG, "빠른 시작 모드 - 정확도 주의 필요");
                showStrategyWarning("⚠️ 빠른 시작 모드입니다. 시선이 정확하지 않으면 '정밀 보정'을 권장합니다.");
                startQuickStartCalibration();
                break;
            case BALANCED:
                Log.d(TAG, "균형 모드 - 기본 보정 후 선택적 정밀 보정");
                showStrategyWarning("⚖️ 균형 모드입니다. 필요시 정밀 보정을 추천드립니다.");
                startBalancedCalibration();
                break;
            case PRECISION:
                // 🎯 정밀 모드가 기본값이므로 가장 안전
                Log.d(TAG, "정밀 모드 - 정확한 보정 우선");
                startPrecisionCalibration();
                break;
            default:
                // 기본값도 정밀 보정으로 변경
                Log.d(TAG, "기본값으로 정밀 보정 실행");
                startPrecisionCalibration();
                break;
        }
    }

    // 🚨 새로 추가: 전략별 경고 메시지
    private void showStrategyWarning(String message) {
        handler.post(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void startQuickStartCalibration() {
        Log.d(TAG, "빠른 시작 캘리브레이션 실행");
        Toast.makeText(this, "🚀 빠른 보정 시작 (2초)", Toast.LENGTH_SHORT).show();

        // 대기시간 단축: 2초 → 1초
        handler.postDelayed(() -> {
            startOptimizedOnePointCalibration();
        }, 1000);
    }

    private void startBalancedCalibration() {
        Log.d(TAG, "균형 캘리브레이션 실행");
        Toast.makeText(this, "⚖️ 스마트 보정 시작", Toast.LENGTH_SHORT).show();

        startOptimizedOnePointCalibration();

        // 5번 상호작용 후 정밀 보정 제안
        scheduleOptionalPrecisionSuggestion();
    }

    private void startPrecisionCalibration() {
        Log.d(TAG, "정밀 캘리브레이션 실행 (기존 방식)");
        Toast.makeText(this, "🎯 정밀 보정 시작", Toast.LENGTH_SHORT).show();

        // 기존 방식 그대로 실행
        startOnePointCalibrationWithOffset();
    }

    // 🚀 최적화된 1포인트 캘리브레이션 (기존 메서드 개선)
    private void startOptimizedOnePointCalibration() {
        if (trackingRepository == null || trackingRepository.getTracker() == null) {
            Log.e(TAG, "trackingRepository 또는 tracker가 null입니다");
            return;
        }

        if (isCalibrating) {
            Log.w(TAG, "이미 캘리브레이션 진행 중입니다");
            return;
        }

        isCalibrating = true;
        isOnePointCalibration = true;
        offsetApplied = false;

        overlayCursorView.setVisibility(View.INVISIBLE);
        calibrationViewer.setVisibility(View.VISIBLE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        targetX = dm.widthPixels / 2f;
        targetY = dm.heightPixels / 2f;

        // 1초 후 캘리브레이션 시작 (기존 2초에서 단축)
        handler.postDelayed(() -> {
            if (trackingRepository.getTracker() != null) {
                boolean ok = trackingRepository.getTracker().startCalibration(CalibrationModeType.ONE_POINT);
                if (!ok) {
                    resetCalibrationState();
                    Toast.makeText(GazeTrackingService.this, "캘리브레이션 시작 실패", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "최적화된 1포인트 캘리브레이션 시작");
                }
            }
        }, 1000);
    }

    // 🔄 빠른 오프셋 계산 (기존 메서드 최적화)
    private void calculateIntegratedOffset() {
        waitingForOffsetCalculation = true;
        validGazeCount = 0;
        sumGazeX = 0f;
        sumGazeY = 0f;

        Log.d(TAG, "빠른 오프셋 계산 시작");
        Toast.makeText(this, "시선 보정 중...", Toast.LENGTH_SHORT).show();

        // 타임아웃 단축: 5초 → 3초
        handler.postDelayed(() -> {
            if (waitingForOffsetCalculation) {
                waitingForOffsetCalculation = false;
                offsetApplied = true;
                overlayCursorView.setVisibility(View.VISIBLE);
                enableBackgroundLearning(); // 백그라운드 학습 활성화
                Log.w(TAG, "오프셋 계산 완료");
                showCompletionMessage();
            }
        }, 3000);
    }

    // 📊 매우 제한적인 백그라운드 학습 시스템 (새로 추가)
    private void enableBackgroundLearning() {
        // 🚨 정밀 보정 모드에서는 백그라운드 학습 비활성화
        if (userSettings.getCalibrationStrategy() == UserSettings.CalibrationStrategy.PRECISION) {
            backgroundLearningEnabled = false;
            Log.d(TAG, "정밀 모드에서는 백그라운드 학습 비활성화");
            return;
        }

        // 사용자가 명시적으로 활성화한 경우에만 제한적으로 활성화
        if (userSettings.isBackgroundLearningEnabled()) {
            backgroundLearningEnabled = true;
            Log.w(TAG, "⚠️ 백그라운드 학습 활성화 - 매우 제한적으로 동작");
        } else {
            backgroundLearningEnabled = false;
            Log.d(TAG, "백그라운드 학습 비활성화됨");
        }
    }

    // 🚨 recordUserInteraction 메서드 - 대폭 제한
    private void recordUserInteraction(float gazeX, float gazeY, float targetX, float targetY) {
        // 🛡️ 안전 검사: 백그라운드 학습이 비활성화되었으면 아무것도 하지 않음
        if (!backgroundLearningEnabled) {
            return;
        }

        // 🛡️ 추가 안전 검사: 정밀 모드에서는 절대 학습하지 않음
        if (userSettings.getCalibrationStrategy() == UserSettings.CalibrationStrategy.PRECISION) {
            return;
        }

        totalInteractions++;

        // 간단한 오차 계산
        float errorX = targetX - gazeX;
        float errorY = targetY - gazeY;
        float errorDistance = (float) Math.sqrt(errorX * errorX + errorY * errorY);

        // 정확도 기록 (오차 40px 이하를 정확한 상호작용으로 간주)
        if (errorDistance < 40) {
            accurateInteractions++;
        }

        // 🚨 매우 제한적인 학습 조건들:
        // 1. 오차가 임계값 이상일 때만
        // 2. 극단적인 값이 아닐 때만
        // 3. 연속된 비슷한 오차일 때만
        if (errorDistance > LEARNING_THRESHOLD && errorDistance < 300) { // 100px~300px 사이의 오차만
            // 매우 보수적인 학습 (기존보다 5배 더 보수적)
            learningOffsetX = learningOffsetX * (1 - LEARNING_RATE) + errorX * LEARNING_RATE;
            learningOffsetY = learningOffsetY * (1 - LEARNING_RATE) + errorY * LEARNING_RATE;
            learningCount++;

            Log.d(TAG, String.format("제한적 학습 기록: 오차=%.1fpx, 누적=%d회", errorDistance, learningCount));

            // 🚨 업데이트 빈도를 10배 줄임 (10번 → 50번)
            if (learningCount % LEARNING_UPDATE_INTERVAL == 0) {
                applyLearningOffsetWithValidation();
            }
        } else {
            Log.d(TAG, String.format("학습 제외: 오차=%.1fpx (임계값 밖)", errorDistance));
        }

        // 주기적 정확도 체크 (기존보다 2배 길게)
        checkAccuracyPeriodically();
    }

    // 🛡️ 검증을 포함한 학습 오프셋 적용
    private void applyLearningOffsetWithValidation() {
        // 🚨 추가 안전 검사들
        float learningMagnitude = (float) Math.sqrt(learningOffsetX * learningOffsetX + learningOffsetY * learningOffsetY);

        // 학습된 오프셋이 너무 크면 무시 (20px 이하만 허용)
        if (learningMagnitude > 20) {
            Log.w(TAG, String.format("학습 오프셋이 너무 큼 (%.1fpx) - 무시됨", learningMagnitude));
            learningOffsetX = 0f;
            learningOffsetY = 0f;
            return;
        }

        // 기존 오프셋과 학습된 오프셋 결합
        float newOffsetX = userSettings.getCursorOffsetX() + learningOffsetX;
        float newOffsetY = userSettings.getCursorOffsetY() + learningOffsetY;

        // 극단적인 값 방지 (화면 크기의 5% 이내로 제한 - 기존 10%에서 더 엄격하게)
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float maxOffset = Math.min(dm.widthPixels, dm.heightPixels) * 0.05f; // 0.1f에서 0.05f로 변경

        newOffsetX = Math.max(-maxOffset, Math.min(maxOffset, newOffsetX));
        newOffsetY = Math.max(-maxOffset, Math.min(maxOffset, newOffsetY));

        // 설정 업데이트
        if (settingsRepository instanceof SharedPrefsSettingsRepository) {
            ((SharedPrefsSettingsRepository) settingsRepository)
                    .saveIntegratedCursorOffset(newOffsetX, newOffsetY);
        }

        refreshSettings();

        Log.d(TAG, String.format("⚠️ 제한적 학습 오프셋 적용: (%.1f, %.1f) → 총 오프셋: (%.1f, %.1f)",
                learningOffsetX, learningOffsetY, newOffsetX, newOffsetY));

        // 학습 오프셋 리셋
        learningOffsetX = 0f;
        learningOffsetY = 0f;
    }

    // 📊 더 엄격한 정확도 체크
    private void checkAccuracyPeriodically() {
        long currentTime = System.currentTimeMillis();

        // 2분마다 정확도 체크 (기존 1분에서 변경)
        if (currentTime - lastAccuracyCheck > ACCURACY_CHECK_INTERVAL && totalInteractions >= 20) { // 최소 20회 상호작용
            float accuracy = (float) accurateInteractions / totalInteractions * 100;

            Log.d(TAG, String.format("현재 정확도: %.1f%% (%d/%d)", accuracy, accurateInteractions, totalInteractions));

            // 🚨 더 엄격한 기준으로 재보정 제안 (70% → 60%)
            if (accuracy < ACCURACY_THRESHOLD) {
                suggestRecalibration();

                // 정확도가 낮으면 백그라운드 학습도 중단
                if (accuracy < 50f) {
                    backgroundLearningEnabled = false;
                    Log.w(TAG, "정확도가 너무 낮아 백그라운드 학습 중단");
                }
            }

            lastAccuracyCheck = currentTime;
        }
    }

    // 1포인트 캘리브레이션 + 오프셋 계산 메서드
    public void startOnePointCalibrationWithOffset() {
        Log.d(TAG, "1포인트 캘리브레이션 + 통합 오프셋 정렬 시작");

        if (trackingRepository == null || trackingRepository.getTracker() == null) {
            Log.e(TAG, "trackingRepository 또는 tracker가 null입니다");
            return;
        }

        if (isCalibrating) {
            Log.w(TAG, "이미 캘리브레이션 진행 중입니다");
            return;
        }

        isCalibrating = true;
        isOnePointCalibration = true;
        offsetApplied = false;

        overlayCursorView.setVisibility(View.INVISIBLE);
        calibrationViewer.setVisibility(View.VISIBLE);

        // 화면 중앙 계산
        DisplayMetrics dm = getResources().getDisplayMetrics();
        targetX = dm.widthPixels / 2f;
        targetY = dm.heightPixels / 2f;

        // 안내 메시지
        Toast.makeText(this, "잠시 후 나타나는 점을 응시해주세요", Toast.LENGTH_SHORT).show();

        // 2초 후 캘리브레이션 시작 (정밀 보정은 기존 2초 유지)
        handler.postDelayed(() -> {
            if (trackingRepository.getTracker() != null) {
                boolean ok = trackingRepository.getTracker().startCalibration(CalibrationModeType.ONE_POINT);
                if (!ok) {
                    resetCalibrationState();
                    Toast.makeText(GazeTrackingService.this, "1포인트 캘리브레이션 시작 실패", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "1포인트 캘리브레이션 시작 성공");
                }
            }
        }, 2000);
    }

    // 상태 초기화 메서드
    private void resetCalibrationState() {
        isCalibrating = false;
        isOnePointCalibration = false;
        waitingForOffsetCalculation = false;
        calibrationViewer.setVisibility(View.INVISIBLE);
        overlayCursorView.setVisibility(View.VISIBLE);
    }

    // 🔄 기존 TrackingCallback 수정 (백그라운드 학습 통합)
    private final TrackingCallback trackingCallback = new TrackingCallback() {
        @Override
        public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, BlinkInfo blinkInfo, UserStatusInfo userStatusInfo) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float screenWidth = dm.widthPixels;
            float screenHeight = dm.heightPixels;

            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
                // 통합 오프셋 계산 대기 중이라면
                if (waitingForOffsetCalculation) {
                    sumGazeX += gazeInfo.x;
                    sumGazeY += gazeInfo.y;
                    validGazeCount++;

                    // 샘플 개수 단축: 10 → 5
                    if (validGazeCount >= 5) {
                        float avgGazeX = sumGazeX / validGazeCount;
                        float avgGazeY = sumGazeY / validGazeCount;

                        float newOffsetX = targetX - avgGazeX;
                        float newOffsetY = targetY - avgGazeY;

                        float maxOffset = Math.min(screenWidth, screenHeight) * 0.3f;

                        if (Math.abs(newOffsetX) <= maxOffset && Math.abs(newOffsetY) <= maxOffset) {
                            if (settingsRepository instanceof SharedPrefsSettingsRepository) {
                                ((SharedPrefsSettingsRepository) settingsRepository)
                                        .saveIntegratedCursorOffset(newOffsetX, newOffsetY);
                            }

                            refreshSettings();
                            offsetApplied = true;
                            enableBackgroundLearning(); // 백그라운드 학습 활성화

                            Log.d(TAG, String.format("빠른 오프셋 적용: X=%.1f, Y=%.1f", newOffsetX, newOffsetY));
                            showCompletionMessage();
                        } else {
                            offsetApplied = true;
                            enableBackgroundLearning();
                            Log.w(TAG, "오프셋이 너무 커서 기본값 사용");
                            showCompletionMessage();
                        }

                        waitingForOffsetCalculation = false;
                        overlayCursorView.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                // 기존 필터링 및 오프셋 적용 로직...
                float filteredX, filteredY;
                long filterTime = android.os.SystemClock.elapsedRealtime();

                if (oneEuroFilterManager.filterValues(filterTime, gazeInfo.x, gazeInfo.y)) {
                    float[] filtered = oneEuroFilterManager.getFilteredValues();
                    filteredX = filtered[0];
                    filteredY = filtered[1];
                } else {
                    filteredX = gazeInfo.x;
                    filteredY = gazeInfo.y;
                }

                if (offsetApplied) {
                    filteredX += userSettings.getCursorOffsetX();
                    filteredY += userSettings.getCursorOffsetY();
                }

                float safeX = Math.max(0, Math.min(filteredX, screenWidth - 1));
                float safeY = Math.max(0, Math.min(filteredY, screenHeight - 1));

                if (!isCalibrating) {
                    overlayCursorView.updatePosition(safeX, safeY);
                    lastValidTimestamp = System.currentTimeMillis();

                    // 기존 엣지 스크롤 및 클릭 감지 로직...
                    EdgeScrollDetector.Edge edge = edgeScrollDetector.update(safeY, screenHeight);

                    if (edge == EdgeScrollDetector.Edge.TOP) {
                        overlayCursorView.setTextPosition(false);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processTopEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.SCROLL_DOWN) {
                            overlayCursorView.setCursorText("③");
                            scrollDown(userSettings.getContinuousScrollCount());
                            // 백그라운드 학습을 위한 상호작용 기록
                            recordUserInteraction(safeX, safeY, safeX, safeY - 100); // 스크롤은 예상 위치로 기록
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (edge == EdgeScrollDetector.Edge.BOTTOM) {
                        overlayCursorView.setTextPosition(true);
                        EdgeScrollDetector.ScrollAction action = edgeScrollDetector.processBottomEdge();
                        overlayCursorView.setCursorText(edgeScrollDetector.getEdgeStateText());

                        if (action == EdgeScrollDetector.ScrollAction.SCROLL_UP) {
                            overlayCursorView.setCursorText("③");
                            scrollUp(userSettings.getContinuousScrollCount());
                            // 백그라운드 학습을 위한 상호작용 기록
                            recordUserInteraction(safeX, safeY, safeX, safeY + 100); // 스크롤은 예상 위치로 기록
                            handler.postDelayed(() -> resetAll(), 500);
                        }
                    } else if (!edgeScrollDetector.isActive()) {
                        boolean clicked = clickDetector.update(safeX, safeY);
                        overlayCursorView.setProgress(clickDetector.getProgress());
                        overlayCursorView.setCursorText("●");

                        if (clicked) {
                            performClick(safeX, safeY);
                            // 백그라운드 학습을 위한 상호작용 기록 (클릭 위치 그대로)
                            recordUserInteraction(safeX, safeY, safeX, safeY);
                        }
                    }
                }
            }
        }

        @Override
        public void onDrop(long timestamp) {}
    };

    private void resetAll() {
        edgeScrollDetector.resetAll();
        clickDetector.reset();
        overlayCursorView.setCursorText("●"); // 기본 커서로 복귀
        overlayCursorView.setTextPosition(false); // 기본 위치 복원
        overlayCursorView.setProgress(0f);
    }

    private void scrollUp(int count) {
        if (MyAccessibilityService.getInstance() != null) {
            Log.d(TAG, "위로 스크롤 실행 (" + count + "회)");

            if (count <= 1) {
                // 단일 스크롤
                MyAccessibilityService.getInstance().performScroll(MyAccessibilityService.Direction.UP);
            } else {
                // 연속 스크롤
                MyAccessibilityService.getInstance().performContinuousScroll(MyAccessibilityService.Direction.UP, count);
            }

            // 스크롤 쿨다운 설정
            lastScrollTime = System.currentTimeMillis();
        } else {
            Log.e(TAG, "접근성 서비스가 실행되지 않았습니다");
            Toast.makeText(this, "접근성 서비스를 활성화해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollDown(int count) {
        if (MyAccessibilityService.getInstance() != null) {
            Log.d(TAG, "아래로 스크롤 실행 (" + count + "회)");

            if (count <= 1) {
                // 단일 스크롤
                MyAccessibilityService.getInstance().performScroll(MyAccessibilityService.Direction.DOWN);
            } else {
                // 연속 스크롤
                MyAccessibilityService.getInstance().performContinuousScroll(MyAccessibilityService.Direction.DOWN, count);
            }

            // 스크롤 쿨다운 설정
            lastScrollTime = System.currentTimeMillis();
        } else {
            Log.e(TAG, "접근성 서비스가 실행되지 않았습니다");
            Toast.makeText(this, "접근성 서비스를 활성화해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void performClick(float x, float y) {
        Log.d(TAG, "클릭 실행 (커서 위치): (" + x + ", " + y + ")");

        // 🎯 커서가 표시된 위치에서 정확히 클릭하도록 함
        // 커서 위치는 이미 모든 오프셋이 적용된 상태
        float cursorX = x;
        float cursorY = y;

        // 화면 정보 수집
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int statusBarHeight = getStatusBarHeight();
        int navigationBarHeight = getNavigationBarHeight();

        Log.d(TAG, "앱 영역: " + dm.widthPixels + "x" + dm.heightPixels);
        Log.d(TAG, "상태바: " + statusBarHeight + "px, 네비게이션바: " + navigationBarHeight + "px");
        Log.d(TAG, "커서 위치 (오프셋 적용됨): (" + cursorX + ", " + cursorY + ")");

        // 커서는 앱 영역 기준이므로 접근성 서비스용으로 상태바 높이 추가
        float adjustedX = cursorX;
        float adjustedY = cursorY + statusBarHeight;

        Log.d(TAG, "클릭 실행 (최종 위치): (" + adjustedX + ", " + adjustedY + ")");

        vibrator.vibrate(100);
        MyAccessibilityService.performClickAt(adjustedX, adjustedY);
    }

    // 상태바 높이 계산 (화면 위쪽)
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // 네비게이션바 높이 계산 (화면 아래쪽)
    private int getNavigationBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            if (!skipProgress) {
                calibrationViewer.setPointAnimationPower(progress);
            }
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            new Handler(Looper.getMainLooper()).post(() -> {
                calibrationViewer.setVisibility(View.VISIBLE);
                showCalibrationPointView(x, y);
            });
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            if (isOnePointCalibration) {
                // 1포인트 캘리브레이션 완료
                hideCalibrationView();
                isCalibrating = false;
                isOnePointCalibration = false;

                // 통합 오프셋 계산 시작
                calculateIntegratedOffset();
                Log.d(TAG, "1포인트 캘리브레이션 완료 - 통합 오프셋 계산 시작");
            } else {
                // 기존 풀 캘리브레이션 완료
                hideCalibrationView();
                isCalibrating = false;
                Toast.makeText(GazeTrackingService.this, "정밀 캘리브레이션 완료", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCalibrationCanceled(double[] calibrationData) {
            resetCalibrationState();
            Toast.makeText(GazeTrackingService.this, "캘리브레이션 취소됨", Toast.LENGTH_SHORT).show();
        }
    };

    private void showCalibrationPointView(final float x, final float y) {
        Log.d(TAG, "캘리브레이션 포인트 (SDK 좌표): (" + x + ", " + y + ")");

        // 캘리브레이션 포인트는 오버레이에 표시되므로
        // SDK에서 제공하는 좌표를 그대로 사용 (변환하지 않음)
        float adjustedX = x;
        float adjustedY = y;

        Log.d(TAG, "캘리브레이션 포인트 (최종): (" + adjustedX + ", " + adjustedY + ")");

        skipProgress = true;
        calibrationViewer.setPointAnimationPower(0);
        calibrationViewer.setEnableText(true);
        calibrationViewer.nextPointColor();
        calibrationViewer.setPointPosition(adjustedX, adjustedY);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (trackingRepository.getTracker() != null) {
                trackingRepository.getTracker().startCollectSamples();
                skipProgress = false;
            }
        }, 1000);
    }

    private void hideCalibrationView() {
        new Handler(Looper.getMainLooper()).post(() -> {
            calibrationViewer.setVisibility(View.INVISIBLE);
            overlayCursorView.setVisibility(View.VISIBLE);
            overlayCursorView.setCursorText("●"); // 기본 커서로 복귀
            overlayCursorView.setTextPosition(false); // 기본 위치 복원
        });
    }

    /**
     * 서비스에서 5포인트 캘리브레이션을 트리거하는 메서드
     */
    public void triggerCalibration() {
        Log.d(TAG, "5포인트 캘리브레이션 트리거 요청됨");

        if (trackingRepository == null) {
            Log.e(TAG, "trackingRepository가 null입니다");
            Toast.makeText(this, "시선 추적 시스템이 초기화되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (trackingRepository.getTracker() == null) {
            Log.e(TAG, "GazeTracker가 null입니다");
            Toast.makeText(this, "시선 추적기가 초기화되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCalibrating) {
            Log.w(TAG, "이미 캘리브레이션 진행 중입니다");
            Toast.makeText(this, "이미 캘리브레이션이 진행 중입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "5포인트 캘리브레이션 시작 시도");

        // UI 업데이트 (메인 스레드에서)
        handler.post(() -> {
            isCalibrating = true;
            isOnePointCalibration = false; // 5포인트 캘리브레이션임을 명시
            overlayCursorView.setVisibility(View.INVISIBLE);

            // 캘리브레이션 시작
            boolean ok = trackingRepository.getTracker().startCalibration(CalibrationModeType.DEFAULT);
            Log.d(TAG, "GazeTracker.startCalibration() 결과: " + ok);

            if (!ok) {
                Log.e(TAG, "5포인트 캘리브레이션 시작 실패");
                resetCalibrationState();
                Toast.makeText(GazeTrackingService.this, "캘리브레이션 시작 실패", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "5포인트 캘리브레이션 시작 성공");
                Toast.makeText(GazeTrackingService.this, "정밀 캘리브레이션을 시작합니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * MainActivity에서 캘리브레이션을 실행할 수 있도록 하는 메서드
     */
    public static void triggerMainActivityCalibration() {
        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().triggerCalibrationFromService();
        } else {
            Log.w(TAG, "MainActivity 인스턴스를 찾을 수 없습니다");
        }
    }

    /**
     * 외부에서 호출 가능한 오프셋 리셋 메서드
     */
    public void forceResetCursorOffset() {
        Log.d(TAG, "외부 요청으로 커서 오프셋 강제 초기화");
        resetCursorOffset();
        refreshSettings();
    }

    /**
     * 현재 서비스 인스턴스를 반환
     */
    public static GazeTrackingService getInstance() {
        return instance;
    }

    /**
     * 사용자 설정을 새로고침하는 메서드
     */
    public void refreshSettings() {
        userSettings = settingsRepository.getUserSettings();
        clickDetector = new ClickDetector(userSettings);
        edgeScrollDetector = new EdgeScrollDetector(userSettings, this);

        // OneEuroFilterManager도 새 설정으로 재초기화 (float 캐스팅)
        oneEuroFilterManager = new OneEuroFilterManager(
                2,
                (float) userSettings.getOneEuroFreq(),
                (float) userSettings.getOneEuroMinCutoff(),
                (float) userSettings.getOneEuroBeta(),
                (float) userSettings.getOneEuroDCutoff()
        );

        Log.d(TAG, "사용자 설정이 새로고침되었습니다");
        Log.d(TAG, "현재 커서 오프셋: X=" + userSettings.getCursorOffsetX() + ", Y=" + userSettings.getCursorOffsetY());
        Log.d(TAG, "현재 OneEuroFilter 프리셋: " + userSettings.getOneEuroFilterPreset().getDisplayName());
        Log.d(TAG, "현재 OneEuroFilter 파라미터 - freq: " + userSettings.getOneEuroFreq() +
                ", minCutoff: " + userSettings.getOneEuroMinCutoff() +
                ", beta: " + userSettings.getOneEuroBeta() +
                ", dCutoff: " + userSettings.getOneEuroDCutoff());
    }

    // 📋 개선된 사용자 안내 메서드들
    private void showCompletionMessage() {
        UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();
        String message;

        switch (strategy) {
            case QUICK_START:
                message = "⚠️ 빠른 보정 완료! 시선이 부정확하면 앱에서 '정밀 보정'을 권장합니다.";
                break;
            case BALANCED:
                message = "⚖️ 기본 보정 완료! 더 정확한 사용을 위해 '정밀 보정'을 권장합니다.";
                break;
            case PRECISION:
                message = "🎯 정밀 보정 완료! 높은 정확도로 사용하실 수 있습니다.";
                break;
            default:
                message = "보정 완료!";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void scheduleOptionalPrecisionSuggestion() {
        // 빠른 시작이나 균형 모드에서만 정밀 보정 제안
        if (userSettings.getCalibrationStrategy() != UserSettings.CalibrationStrategy.PRECISION) {
            handler.postDelayed(() -> {
                if (totalInteractions >= 3) { // 3번 상호작용 후 더 빨리 제안
                    handler.post(() -> {
                        Toast.makeText(this, "💡 더 정확한 시선 추적을 위해 앱에서 '정밀 보정'을 권장합니다.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            }, 5000); // 5초 후 (기존 10초에서 단축)
        }
    }

    private void suggestRecalibration() {
        handler.post(() -> {
            UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();
            String message;

            if (strategy == UserSettings.CalibrationStrategy.PRECISION) {
                message = "📊 시선 정확도가 떨어졌습니다. 앱에서 재보정을 권장합니다.";
            } else {
                message = "📊 시선 정확도가 떨어졌습니다. 앱에서 '정밀 보정'을 강력히 권장합니다.";
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    // 추가된 메소드: 접근성 서비스 활성화 여부 확인
    private void checkAccessibilityService() {
        if (MyAccessibilityService.getInstance() == null) {
            // 메인 액티비티가 없을 때도 작동하도록 Toast로 간단하게 알림
            Toast.makeText(this, "접근성 서비스가 활성화되지 않았습니다. 설정에서 활성화해주세요.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "접근성 서비스가 활성화되지 않음. 기능 제한됨.");
        } else {
            Toast.makeText(this, "시선 추적 서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "접근성 서비스 활성화됨. 모든 기능 사용 가능.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "서비스 시작됨");

        // 서비스가 재시작될 때도 오프셋 초기화
        if (intent != null && intent.getBooleanExtra("reset_offset", false)) {
            Log.d(TAG, "인텐트로부터 오프셋 리셋 요청됨");
            resetCursorOffset();
        }

        return START_STICKY; // 시스템에 의해 종료되어도 자동 재시작
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "서비스 종료됨");

        // 뷰 제거
        if (overlayCursorView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayCursorView);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "커서 뷰 제거 중 오류: " + e.getMessage());
            }
        }
        if (calibrationViewer != null && windowManager != null) {
            try {
                windowManager.removeView(calibrationViewer);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "캘리브레이션 뷰 제거 중 오류: " + e.getMessage());
            }
        }

        // 시선 추적 중지
        if (trackingRepository != null && trackingRepository.getTracker() != null) {
            trackingRepository.stopTracking();
        }

        // 인스턴스 정리
        instance = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 바인드 서비스가 아님
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "시선 추적 서비스 채널",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("시선 추적 서비스가 백그라운드에서 실행 중입니다");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}