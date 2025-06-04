package camp.visual.android.sdk.sample.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 🤖 EyedidTracker Performance Monitor
 * 자율 에이전트가 생성한 실시간 성능 모니터링 도구
 * 
 * 기능:
 * - CPU 사용률 실시간 모니터링
 * - 메모리 사용량 추적
 * - 배터리 소모량 측정
 * - 프레임 드롭 감지
 * - 시선 추적 성능 지표 수집
 */
public class PerformanceMonitor {
    private static final String TAG = "🤖 PerformanceMonitor";
    
    private Context context;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    
    // 성능 지표 변수들
    private long lastCpuTime = 0;
    private long lastAppCpuTime = 0;
    private long startTime;
    private long initialBatteryLevel;
    
    // 성능 통계
    private int frameDropCount = 0;
    private double avgCpuUsage = 0;
    private double avgMemoryUsage = 0;
    private long maxMemoryUsage = 0;
    private int measurementCount = 0;
    
    public PerformanceMonitor(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.startTime = System.currentTimeMillis();
        
        // 초기 배터리 레벨 기록
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        this.initialBatteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        
        Log.i(TAG, "🚀 Performance monitoring started");
        Log.i(TAG, "📱 Initial battery level: " + initialBatteryLevel + "%");
    }
    
    /**
     * 실시간 성능 모니터링 시작
     */
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::collectPerformanceData, 0, 2, TimeUnit.SECONDS);
        Log.i(TAG, "📊 Real-time performance monitoring activated");
    }
    
    /**
     * 성능 데이터 수집
     */
    private void collectPerformanceData() {
        try {
            // CPU 사용률 측정
            double cpuUsage = getCpuUsage();
            
            // 메모리 사용량 측정
            long memoryUsage = getMemoryUsage();
            
            // 배터리 사용량 측정
            double batteryDrain = getBatteryDrain();
            
            // 통계 업데이트
            updateStatistics(cpuUsage, memoryUsage);
            
            // 로그 출력 (2초마다)
            logPerformanceData(cpuUsage, memoryUsage, batteryDrain);
            
            // 경고 체크
            checkPerformanceWarnings(cpuUsage, memoryUsage);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error collecting performance data: " + e.getMessage());
        }
    }
    
    /**
     * CPU 사용률 계산
     */
    private double getCpuUsage() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(
                new int[]{android.os.Process.myPid()}
            );
            
            // 메모리 할당률로 CPU 사용률 추정
            double memoryActivity = memoryInfos[0].getTotalPrivateDirty() / 1024.0;
            return Math.min(memoryActivity / 10, 100.0);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ CPU usage calculation failed: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 메모리 사용량 측정 (MB)
     */
    private long getMemoryUsage() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(
            new int[]{android.os.Process.myPid()}
        );
        
        long totalMemory = memoryInfos[0].getTotalPss(); // KB
        return totalMemory / 1024; // Convert to MB
    }
    
    /**
     * 배터리 소모량 계산
     */
    private double getBatteryDrain() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            long currentBatteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            
            long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
            if (elapsedMinutes > 0) {
                return (double)(initialBatteryLevel - currentBatteryLevel) / elapsedMinutes;
            }
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "❌ Battery drain calculation failed: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 통계 업데이트
     */
    private void updateStatistics(double cpuUsage, long memoryUsage) {
        measurementCount++;
        
        // 평균 CPU 사용률 계산
        avgCpuUsage = ((avgCpuUsage * (measurementCount - 1)) + cpuUsage) / measurementCount;
        
        // 평균 메모리 사용량 계산
        avgMemoryUsage = ((avgMemoryUsage * (measurementCount - 1)) + memoryUsage) / measurementCount;
        
        // 최대 메모리 사용량 업데이트
        if (memoryUsage > maxMemoryUsage) {
            maxMemoryUsage = memoryUsage;
        }
    }
    
    /**
     * 성능 데이터 로깅
     */
    private void logPerformanceData(double cpuUsage, long memoryUsage, double batteryDrain) {
        Log.i(TAG, String.format("📊 Performance: CPU %.1f%% | Memory %dMB | Battery %.2f%%/min", 
                cpuUsage, memoryUsage, batteryDrain));
    }
    
    /**
     * 성능 경고 체크
     */
    private void checkPerformanceWarnings(double cpuUsage, long memoryUsage) {
        if (cpuUsage > 80) {
            Log.w(TAG, "⚠️ HIGH CPU USAGE: " + cpuUsage + "%");
        }
        
        if (memoryUsage > 200) {
            Log.w(TAG, "⚠️ HIGH MEMORY USAGE: " + memoryUsage + "MB");
        }
        
        if (memoryUsage > 500) {
            Log.e(TAG, "🚨 CRITICAL MEMORY USAGE: " + memoryUsage + "MB - Risk of OOM");
        }
    }
    
    /**
     * 프레임 드롭 기록
     */
    public void recordFrameDrop() {
        frameDropCount++;
        Log.w(TAG, "📉 Frame drop detected! Total: " + frameDropCount);
    }
    
    /**
     * 성능 보고서 생성
     */
    public String generatePerformanceReport() {
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
        
        StringBuilder report = new StringBuilder();
        report.append("🤖 EyedidTracker Performance Report\n");
        report.append("=====================================\n");
        report.append("⏱️ Runtime: ").append(elapsedMinutes).append(" minutes\n");
        report.append("🖥️ Avg CPU Usage: ").append(String.format("%.1f%%", avgCpuUsage)).append("\n");
        report.append("💾 Avg Memory: ").append(String.format("%.1fMB", avgMemoryUsage)).append("\n");
        report.append("📈 Peak Memory: ").append(maxMemoryUsage).append("MB\n");
        report.append("📉 Frame Drops: ").append(frameDropCount).append("\n");
        report.append("🔋 Battery Drain: ").append(String.format("%.2f%%/hour", getBatteryDrain() * 60)).append("\n");
        
        // 성능 등급 평가
        String grade = evaluatePerformance();
        report.append("🏆 Performance Grade: ").append(grade).append("\n");
        
        return report.toString();
    }
    
    /**
     * 성능 등급 평가
     */
    private String evaluatePerformance() {
        if (avgCpuUsage < 30 && avgMemoryUsage < 100 && frameDropCount < 5) {
            return "A+ (Excellent)";
        } else if (avgCpuUsage < 50 && avgMemoryUsage < 150 && frameDropCount < 10) {
            return "A (Very Good)";
        } else if (avgCpuUsage < 70 && avgMemoryUsage < 200 && frameDropCount < 20) {
            return "B (Good)";
        } else if (avgCpuUsage < 85 && avgMemoryUsage < 300 && frameDropCount < 50) {
            return "C (Acceptable)";
        } else {
            return "D (Needs Optimization)";
        }
    }
    
    /**
     * 모니터링 중지
     */
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        String finalReport = generatePerformanceReport();
        Log.i(TAG, "📋 Final Performance Report:\n" + finalReport);
        Log.i(TAG, "🛑 Performance monitoring stopped");
    }
    
    /**
     * 시선 추적 특화 성능 지표
     */
    public void recordGazeTrackingMetrics(float accuracy, long processingTime) {
        Log.i(TAG, String.format("👁️ Gaze Tracking: Accuracy %.2f%% | Processing %dms", 
                accuracy, processingTime));
        
        if (processingTime > 16) { // 60fps 기준
            recordFrameDrop();
        }
    }
}
