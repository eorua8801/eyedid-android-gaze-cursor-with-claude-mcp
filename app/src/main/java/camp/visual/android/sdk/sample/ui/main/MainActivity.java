package camp.visual.android.sdk.sample.ui.main;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import camp.visual.android.sdk.sample.R;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;
import camp.visual.android.sdk.sample.ui.settings.SettingsActivity;
import camp.visual.android.sdk.sample.ui.views.CalibrationViewer;
import camp.visual.android.sdk.sample.ui.views.PointView;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;
import camp.visual.eyedid.gazetracker.constant.StatusErrorType;
import camp.visual.eyedid.gazetracker.metrics.BlinkInfo;
import camp.visual.eyedid.gazetracker.metrics.FaceInfo;
import camp.visual.eyedid.gazetracker.metrics.GazeInfo;
import camp.visual.eyedid.gazetracker.metrics.UserStatusInfo;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;
import camp.visual.eyedid.gazetracker.util.ViewLayoutChecker;

public class MainActivity extends AppCompatActivity {
    private GazeTracker gazeTracker;
    private final String EYEDID_SDK_LICENSE = "dev_plnp4o1ya7d0tif2rmgko169l1z4jnali2q4f63f";
    private final CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };
    private final int REQ_PERMISSION = 1000;
    private final int REQ_OVERLAY_PERMISSION = 1001;

    private View layoutProgress;
    private PointView viewPoint;
    private boolean skipProgress = false;
    private Button btnAlignment, btnStartCalibration, btnSettings;
    private CalibrationViewer viewCalibration;
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private Handler backgroundHandler;
    private final HandlerThread backgroundThread = new HandlerThread("background");

    // 🎯 새로 추가된 UI 요소들
    private TextView statusText;
    private ProgressBar progressBar;
    private Handler handler = new Handler();
    private SettingsRepository settingsRepository;
    private UserSettings userSettings;

    // 서비스에서 캘리브레이션을 트리거하기 위한 인스턴스 참조
    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    private final TrackingCallback trackingCallback = new TrackingCallback() {
        @Override
        public void onMetrics(long timestamp, GazeInfo gazeInfo, FaceInfo faceInfo, BlinkInfo blinkInfo,
                              UserStatusInfo userStatusInfo) {
            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
                viewPoint.setPosition(gazeInfo.x, gazeInfo.y);
            }
        }

        @Override
        public void onDrop(long timestamp) {
            Log.d("MainActivity", "drop frame " + timestamp);
        }
    };

    private boolean isFirstPoint = false;

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {

        @Override
        public void onCalibrationProgress(float progress) {
            if (!skipProgress)  {
                runOnUiThread(() -> viewCalibration.setPointAnimationPower(progress));
            }
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            runOnUiThread(() -> {
                viewCalibration.setVisibility(View.VISIBLE);
                if (isFirstPoint) {
                    backgroundHandler.postDelayed(() -> showCalibrationPointView(x, y), 2500);
                } else {
                    showCalibrationPointView(x, y);
                }
            });
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            hideCalibrationView();
            showToast("캘리브레이션 완료", true);
        }

        @Override
        public void onCalibrationCanceled(double[] doubles) {
            hideCalibrationView();
            showToast("캘리브레이션 취소됨", true);
        }
    };

    private final StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            // 추적이 시작되면 캘리브레이션 가능
            runOnUiThread(() -> {
                btnAlignment.setEnabled(true);
                btnStartCalibration.setEnabled(true);
                updateStatusText("시선 추적 활성화됨 ✅");
            });
        }

        @Override
        public void onStopped(StatusErrorType error) {
            runOnUiThread(() -> {
                btnAlignment.setEnabled(false);
                btnStartCalibration.setEnabled(false);
                updateStatusText("시선 추적 중지됨 ❌");
            });
            if (error != StatusErrorType.ERROR_NONE) {
                if (error == StatusErrorType.ERROR_CAMERA_START) {
                    showToast("카메라 시작 오류", false);
                } else if (error == StatusErrorType.ERROR_CAMERA_INTERRUPT) {
                    showToast("카메라 중단 오류", false);
                }
            }
        }
    };

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("MainActivity", "버튼 클릭됨: " + v.getId());

            if (v == btnAlignment) {
                Log.d("MainActivity", "정렬 버튼 클릭됨");

                // 서비스에서 1포인트 캘리브레이션 실행
                if (isServiceRunning()) {
                    GazeTrackingService service = GazeTrackingService.getInstance();
                    if (service != null) {
                        // 🎯 현재 설정된 전략에 따른 적절한 안내 메시지
                        String message = getCalibrationMessage();
                        showToast(message, true);
                        service.startOnePointCalibrationWithOffset();
                    } else {
                        showToast("❌ 서비스에 연결할 수 없습니다", false);
                    }
                } else {
                    showToast("❌ 시선 추적 서비스가 실행되지 않았습니다", false);
                    startServicesAndCheckPermissions();
                }
            } else if (v == btnStartCalibration) {
                Log.d("MainActivity", "캘리브레이션 버튼 클릭됨");

                // 🎯 정밀 캘리브레이션 확인 대화상자
                showPrecisionCalibrationDialog();
            }
        }
    };

    private final InitializationCallback initializationCallback = (gazeTracker, error) -> {
        if (gazeTracker == null) {
            showToast("초기화 오류: " + error.name(), true);
            hideProgress();
        } else {
            // 서비스가 이미 실행 중이면 MainActivity에서는 SDK 사용하지 않음
            if (isServiceRunning()) {
                Log.d("MainActivity", "서비스가 실행 중이므로 MainActivity SDK 사용하지 않음");
                gazeTracker.stopTracking();
                btnAlignment.setEnabled(true);
                btnStartCalibration.setEnabled(true);
                updateStatusText("서비스 연결됨 ✅");
            } else {
                // 서비스가 없는 경우에만 MainActivity에서 SDK 사용
                this.gazeTracker = gazeTracker;
                this.gazeTracker.setTrackingCallback(trackingCallback);
                this.gazeTracker.setCalibrationCallback(calibrationCallback);
                this.gazeTracker.setStatusCallback(statusCallback);

                // 자동으로 추적 시작
                this.gazeTracker.startTracking();

                // UI 업데이트
                runOnUiThread(() -> {
                    btnAlignment.setEnabled(true);
                    btnStartCalibration.setEnabled(true);
                    updateStatusText("시선 추적 초기화됨 ✅");
                });
            }
        }
        hideProgress();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🎯 설정 저장소 초기화
        settingsRepository = new SharedPrefsSettingsRepository(this);
        userSettings = settingsRepository.getUserSettings();

        initViews();
        checkPermission();
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        // 서비스 시작 및 권한 확인
        startServicesAndCheckPermissions();

        // 🎯 사용자 친화적 시작 메시지
        showWelcomeMessage();
    }

    // 🎯 사용자 친화적 메시지들 (새로 추가)
    private void showWelcomeMessage() {
        UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();
        final String welcomeMsg;

        switch (strategy) {
            case QUICK_START:
                welcomeMsg = "🚀 빠른 시작 모드로 설정되어 있습니다.\n2초만 기다리면 바로 사용할 수 있어요!";
                break;
            case BALANCED:
                welcomeMsg = "⚖️ 균형 모드로 설정되어 있습니다.\n빠른 보정 후 필요시 정밀 보정을 제안드려요.";
                break;
            case PRECISION:
                welcomeMsg = "🎯 정밀 모드로 설정되어 있습니다.\n정확한 보정을 위해 조금 더 시간이 걸릴 수 있어요.";
                break;
            default:
                welcomeMsg = "시선 추적이 시작되었습니다.";
                break;
        }

        // 3초 후에 환영 메시지 표시
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, welcomeMsg, Toast.LENGTH_LONG).show();
            }
        }, 3000);
    }

    // 🎯 캘리브레이션 관련 사용자 친화적 메서드들 (새로 추가)
    private String getCalibrationMessage() {
        UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();

        switch (strategy) {
            case QUICK_START:
                return "🚀 빠른 보정을 시작합니다 (2초 소요)";
            case BALANCED:
                return "⚖️ 스마트 보정을 시작합니다";
            case PRECISION:
                return "🎯 정밀 보정을 시작합니다";
            default:
                return "시선 보정을 시작합니다";
        }
    }

    private void showPrecisionCalibrationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎯 정밀 보정")
                .setMessage("더 정확한 시선 추적을 위해 5포인트 정밀 보정을 실행합니다.\n\n" +
                        "📋 진행 방법:\n" +
                        "• 화면에 나타나는 점들을 차례로 응시해 주세요\n" +
                        "• 각 점당 약 2초씩 소요됩니다\n" +
                        "• 총 10-15초 정도 걸립니다\n\n" +
                        "💡 팁: 편안한 자세로 화면과 30-60cm 거리를 유지해 주세요")
                .setPositiveButton("✅ 시작하기", (dialog, which) -> {
                    if (isServiceRunning()) {
                        startCalibration();
                        showToast("🎯 정밀 보정을 시작합니다", true);
                    } else {
                        showToast("⏳ 시선 추적 시스템을 초기화하는 중입니다", false);
                        showProgress();
                        initTracker();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 서비스 시작 및 권한 확인
    private void startServicesAndCheckPermissions() {
        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
        } else {
            // 접근성 서비스 활성화 확인
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityPermissionDialog();
            }

            // 오버레이 권한이 있으면 서비스 시작 (오프셋 리셋 포함)
            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            serviceIntent.putExtra("reset_offset", true); // 오프셋 리셋 요청
            startForegroundService(serviceIntent);

            // 🎯 사용자에게 친근한 시작 안내
            showServiceStartMessage();
        }
    }

    // 🎯 서비스 시작 메시지 (새로 추가)
    private void showServiceStartMessage() {
        handler.postDelayed(() -> {
            if (isServiceRunning()) {
                UserSettings.CalibrationStrategy strategy = userSettings.getCalibrationStrategy();
                final String message;

                switch (strategy) {
                    case QUICK_START:
                        message = "✨ 시선 추적이 시작되었습니다!\n2초 보정 후 바로 사용하며 더 정확해집니다.";
                        break;
                    case BALANCED:
                        message = "✨ 시선 추적이 시작되었습니다!\n스마트 보정으로 편리하게 사용하세요.";
                        break;
                    case PRECISION:
                        message = "✨ 시선 추적이 시작되었습니다!\n정밀 보정으로 높은 정확도를 제공합니다.";
                        break;
                    default:
                        message = "✨ 시선 추적이 시작되었습니다!";
                        break;
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                updateStatusText("시선 추적 활성화됨 ✅");
            }
        }, 2000);
    }

    // 오버레이 권한 요청 다이얼로그
    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🖥️ 화면 오버레이 권한 필요")
                .setMessage("시선 커서를 표시하기 위해 '다른 앱 위에 표시' 권한이 필요합니다.\n\n" +
                        "📱 설정 방법:\n" +
                        "1. 설정 화면이 열리면\n" +
                        "2. 'EyedidSampleApp' 찾기\n" +
                        "3. '허용' 또는 '사용' 버튼 누르기\n" +
                        "4. 앱으로 돌아오기\n\n" +
                        "💡 이 권한은 시선 커서 표시에만 사용됩니다.")
                .setPositiveButton("⚙️ 설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
                })
                .setNegativeButton("나중에", null)
                .setCancelable(false)
                .show();
    }

    // 접근성 서비스 확인
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // 설정을 찾을 수 없음
        }

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (settingValue != null) {
                return settingValue.contains(service);
            }
        }
        return false;
    }

    // 접근성 권한 요청 다이얼로그
    private void showAccessibilityPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("♿ 접근성 서비스 설정")
                .setMessage("시선으로 화면을 터치하고 스크롤하기 위해 접근성 서비스 권한이 필요합니다.\n\n" +
                        "📱 설정 방법:\n" +
                        "1. 설정 > 접근성 (또는 디지털 웰빙 > 접근성)\n" +
                        "2. '다운로드한 앱' 또는 '설치된 앱'에서\n" +
                        "3. 'EyedidSampleApp' 찾기\n" +
                        "4. '사용 안 함' → '사용' 변경\n" +
                        "5. '확인' 버튼 누르기\n\n" +
                        "💡 이 권한은 시선으로 터치/스크롤하는 데만 사용됩니다.")
                .setPositiveButton("⚙️ 설정으로 이동", (d, which) -> {
                    openAccessibilitySettings();
                })
                .setNegativeButton("나중에", null)
                .show();
    }

    // 접근성 설정 열기 - 가능하면 앱별 설정으로 직접 이동
    private void openAccessibilitySettings() {
        try {
            // 방법 1: 특정 접근성 서비스 설정으로 직접 이동 시도
            ComponentName componentName = new ComponentName(getPackageName(),
                    MyAccessibilityService.class.getName());
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

            // Android 5.0+ (API 21+)에서 지원하는 특정 서비스로 이동
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = new Bundle();
                String showArgs = componentName.flattenToString();
                bundle.putString(":settings:fragment_args_key", showArgs);
                intent.putExtra(":settings:show_fragment_args", bundle);
                intent.putExtra(":settings:fragment_args_key", showArgs);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // 설정 화면 이동 성공 메시지
            showToast("'EyedidSampleApp'을 찾아 활성화해주세요", false);

        } catch (Exception e) {
            // 실패시 일반 접근성 설정으로 이동
            Log.d("MainActivity", "특정 서비스 설정 이동 실패, 일반 설정으로 이동");
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallbackIntent);
                showToast("접근성 설정에서 'EyedidSampleApp'을 찾아 활성화해주세요", false);
            } catch (Exception ex) {
                showToast("설정 화면을 열 수 없습니다", false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            // 오버레이 권한 확인 후 처리
            if (Settings.canDrawOverlays(this)) {
                showToast("✅ 오버레이 권한이 허용되었습니다!", true);
                // 접근성 서비스 확인
                if (!isAccessibilityServiceEnabled()) {
                    showAccessibilityPermissionDialog();
                }

                // 서비스 시작 (오프셋 리셋 포함)
                Intent serviceIntent = new Intent(this, GazeTrackingService.class);
                serviceIntent.putExtra("reset_offset", true);
                startForegroundService(serviceIntent);
            } else {
                showToast("❌ 오버레이 권한이 필요합니다", false);
                updateStatusText("오버레이 권한 필요 ⚠️");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🎯 설정 다시 로드 (설정 화면에서 돌아올 때 반영)
        userSettings = settingsRepository.getUserSettings();

        // 권한 상태 확인
        if (!Settings.canDrawOverlays(this)) {
            updateStatusText("오버레이 권한 필요 ⚠️");
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            updateStatusText("접근성 서비스 권한 필요 ⚠️");
            // 더 친화적인 메시지
            handler.postDelayed(() -> {
                showToast("💡 접근성 서비스를 활성화하면 시선으로 터치할 수 있습니다", true);
            }, 1000);
        }

        // 서비스 상태 확인 및 연동
        if (isServiceRunning()) {
            // 서비스가 실행 중이면 UI 활성화
            Log.d("MainActivity", "서비스 실행 중 - UI 활성화");

            // 서비스가 이미 실행 중일 때도 오프셋 리셋 보장
            GazeTrackingService service = GazeTrackingService.getInstance();
            if (service != null) {
                service.forceResetCursorOffset();
                Log.d("MainActivity", "기존 서비스의 오프셋 강제 리셋 완료");
            }

            btnAlignment.setEnabled(true);
            btnStartCalibration.setEnabled(true);
            hideProgress();
            updateStatusText("시선 추적 활성화됨 ✅");

            // 서비스에 이미 SDK가 있으면 MainActivity의 tracker는 해제
            if (gazeTracker != null) {
                Log.d("MainActivity", "서비스 실행 중이므로 MainActivity tracker 해제");
                gazeTracker.stopTracking();
                gazeTracker = null;
            }
        } else {
            // 서비스가 없으면 새로 시작 (오프셋 리셋 포함)
            Log.d("MainActivity", "서비스 시작");
            updateStatusText("서비스 시작 중...");

            Intent serviceIntent = new Intent(this, GazeTrackingService.class);
            serviceIntent.putExtra("reset_offset", true);
            startForegroundService(serviceIntent);

            // 서비스 시작 후 잠시 대기 후 상태 확인
            backgroundHandler.postDelayed(() -> {
                runOnUiThread(() -> {
                    if (isServiceRunning()) {
                        btnAlignment.setEnabled(true);
                        btnStartCalibration.setEnabled(true);
                        hideProgress();
                        updateStatusText("시선 추적 활성화됨 ✅");
                        showServiceStartMessage();
                    } else {
                        updateStatusText("서비스 시작 실패 ❌");
                    }
                });
            }, 2000);
        }
    }

    private void initViews() {
        TextView txtSDKVersion = findViewById(R.id.txt_sdk_version);
        txtSDKVersion.setText(GazeTracker.getVersionName());
        layoutProgress = findViewById(R.id.layout_progress);
        viewCalibration = findViewById(R.id.view_calibration);
        viewPoint = findViewById(R.id.view_point);

        // 🎯 새로운 UI 요소들 추가
        statusText = findViewById(R.id.text_status);
        progressBar = findViewById(R.id.progress_bar);

        // 정렬 버튼 추가
        btnAlignment = findViewById(R.id.btn_alignment);
        btnAlignment.setOnClickListener(onClickListener);

        // 캘리브레이션 버튼만 활성화 (텍스트 변경)
        btnStartCalibration = findViewById(R.id.btn_start_calibration);
        btnStartCalibration.setOnClickListener(onClickListener);
        btnStartCalibration.setText("정밀 보정"); // 버튼 텍스트 변경

        // 설정 버튼
        btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 초기 상태 설정
        btnAlignment.setEnabled(false);
        btnStartCalibration.setEnabled(false);
        viewPoint.setPosition(-999,-999);
        updateStatusText("시스템 초기화 중...");

        // 오프셋 설정 개선 - 뷰가 완전히 그려진 후 계산
        viewCalibration.post(() -> {
            viewLayoutChecker.setOverlayView(viewPoint, (x, y) -> {
                viewPoint.setOffset(x, y);
                viewCalibration.setOffset(x, y);
                Log.d("MainActivity", "Offset 설정됨: x=" + x + ", y=" + y);
            });
        });
    }

    // 🎯 UI 상태 업데이트 메서드들 (새로 추가)
    private void updateStatusText(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.VISIBLE));
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        updateStatusText("시스템 초기화 중...");
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.GONE));
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showToast(String message, boolean isSuccess) {
        Toast.makeText(this, message, isSuccess ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

    private void checkPermission() {
        if (hasPermissions()) {
            checkPermission(true);
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_PERMISSION);
        }
    }

    private boolean hasPermissions() {
        int result;
        for (String perms : PERMISSIONS) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (!isGranted) {
            showToast("권한이 필요합니다", true);
            finish();
        } else {
            permissionGranted();
        }
    }

    private void permissionGranted() {
        // 서비스가 이미 실행 중인지 확인
        if (isServiceRunning()) {
            Log.d("MainActivity", "서비스가 이미 실행 중입니다. SDK 재초기화를 건너뜁니다.");
            // 서비스가 실행 중이면 바로 UI 활성화
            hideProgress();
            btnAlignment.setEnabled(true);
            btnStartCalibration.setEnabled(true);
            updateStatusText("서비스 연결됨 ✅");
            showToast("시선 추적 서비스 연결됨", true);
        } else {
            // 서비스가 없으면 새로 초기화
            showProgress();
            initTracker();
        }
    }

    private boolean isServiceRunning() {
        return GazeTrackingService.getInstance() != null;
    }

    private void initTracker() {
        // 서비스에서 SDK를 관리하므로 MainActivity에서는 간단하게 처리
        // 서비스가 시작되었다면 callback 설정만
        if (isServiceRunning()) {
            Log.d("MainActivity", "서비스 연결 완료");
            btnAlignment.setEnabled(true);
            btnStartCalibration.setEnabled(true);
            hideProgress();
            updateStatusText("서비스 연결됨 ✅");
        } else {
            // 서비스가 없는 경우에만 SDK 초기화
            Log.d("MainActivity", "새로운 SDK 초기화 시작");
            GazeTrackerOptions options = new GazeTrackerOptions.Builder().build();
            GazeTracker.initGazeTracker(this, EYEDID_SDK_LICENSE, initializationCallback, options);
        }
    }

    private void hideCalibrationView() {
        runOnUiThread(() -> {
            viewCalibration.setVisibility(View.INVISIBLE);
            btnAlignment.setEnabled(true);
            btnStartCalibration.setEnabled(true);
            viewPoint.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                checkPermission(cameraPermissionAccepted);
            }
        }
    }

    private void showCalibrationPointView(final float x, final float y){
        skipProgress = true;
        viewCalibration.setPointAnimationPower(0);
        viewCalibration.setEnableText(false);
        viewCalibration.nextPointColor();
        viewCalibration.setPointPosition(x, y);
        long delay = isFirstPoint ? 0 : 1200;

        backgroundHandler.postDelayed(() -> {
            if(gazeTracker != null)
                gazeTracker.startCollectSamples();
            skipProgress = false;
        }, delay);

        isFirstPoint = false;
    }

    private void startCalibration() {
        Log.d("MainActivity", "캘리브레이션 시작 요청");

        // 1. 먼저 서비스 상태 확인
        boolean serviceRunning = isServiceRunning();
        Log.d("MainActivity", "서비스 실행 상태: " + serviceRunning);

        if (serviceRunning) {
            // 서비스가 실행 중이면 서비스에서 캘리브레이션 실행
            Log.d("MainActivity", "서비스에서 캘리브레이션 실행 시도");
            try {
                GazeTrackingService service = GazeTrackingService.getInstance();
                Log.d("MainActivity", "서비스 인스턴스: " + (service != null ? "OK" : "NULL"));

                if (service != null) {
                    Log.d("MainActivity", "서비스 triggerCalibration() 호출 시작");
                    service.triggerCalibration();
                    Log.d("MainActivity", "서비스 triggerCalibration() 호출 완료");
                } else {
                    Log.e("MainActivity", "서비스 인스턴스가 null입니다");
                    showToast("서비스에 연결할 수 없습니다", false);

                    // 서비스 재시작 시도
                    Intent serviceIntent = new Intent(this, GazeTrackingService.class);
                    startForegroundService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "서비스 캘리브레이션 호출 중 오류: " + e.getMessage(), e);
                showToast("캘리브레이션 실행 오류: " + e.getMessage(), false);

                // 오류 발생시 MainActivity에서 실행 시도
                attemptMainActivityCalibration();
            }
            return;
        }

        // 2. 서비스가 없는 경우 MainActivity에서 실행
        attemptMainActivityCalibration();
    }

    private void attemptMainActivityCalibration() {
        Log.d("MainActivity", "MainActivity에서 캘리브레이션 실행 시도");

        if (gazeTracker == null) {
            Log.e("MainActivity", "GazeTracker가 null입니다");
            showToast("시선 추적기가 초기화되지 않았습니다", false);

            // 다시 초기화 시도
            showProgress();
            initTracker();
            return;
        }

        Log.d("MainActivity", "GazeTracker로 캘리브레이션 시작");
        boolean isSuccess = gazeTracker.startCalibration(calibrationType);
        Log.d("MainActivity", "캘리브레이션 시작 결과: " + isSuccess);

        if (isSuccess) {
            isFirstPoint = true;
            runOnUiThread(() -> {
                viewCalibration.setPointPosition(-9999, -9999);
                viewCalibration.setEnableText(true);
                viewPoint.setVisibility(View.INVISIBLE);
                btnAlignment.setEnabled(false);
                btnStartCalibration.setEnabled(false);
                Log.d("MainActivity", "캘리브레이션 UI 설정 완료");
            });
        } else {
            showToast("캘리브레이션 시작 실패", false);
            Log.e("MainActivity", "GazeTracker.startCalibration() 실패");
        }
    }

    // 서비스에서 호출할 수 있는 캘리브레이션 메서드
    public void triggerCalibrationFromService() {
        runOnUiThread(() -> {
            if (btnStartCalibration.isEnabled()) {
                startCalibration();
            } else {
                showToast("캘리브레이션을 시작할 수 없습니다", false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
        }
        backgroundThread.quitSafely();
        instance = null;
    }
}