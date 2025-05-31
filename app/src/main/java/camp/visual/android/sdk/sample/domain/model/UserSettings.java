// UserSettings.java - 정밀 보정을 기본값으로 변경 (패키지명 수정)
package camp.visual.android.sdk.sample.domain.model;

public class UserSettings {

    // 캘리브레이션 전략 enum 추가
    public enum CalibrationStrategy {
        QUICK_START("빠른 시작", "2초 간단 보정 + 자동 학습 (정확도 주의)"),
        BALANCED("균형", "간단 보정 + 선택적 정밀 보정 (표준)"),
        PRECISION("정밀", "정확한 초기 보정 (적극 권장)");

        private final String displayName;
        private final String description;

        CalibrationStrategy(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // 기존 필드들...
    private final float fixationDurationMs;
    private final float aoiRadius;
    private final boolean scrollEnabled;
    private final float edgeMarginRatio;
    private final long edgeTriggerMs;
    private final int continuousScrollCount;
    private final boolean clickEnabled;
    private final boolean edgeScrollEnabled;
    private final boolean blinkDetectionEnabled;
    private final boolean autoOnePointCalibrationEnabled;
    private final float cursorOffsetX;
    private final float cursorOffsetY;
    private final OneEuroFilterPreset oneEuroFilterPreset;
    private final double oneEuroFreq;
    private final double oneEuroMinCutoff;
    private final double oneEuroBeta;
    private final double oneEuroDCutoff;

    // 새로운 필드들 추가
    private final CalibrationStrategy calibrationStrategy;
    private final boolean backgroundLearningEnabled;

    private UserSettings(Builder builder) {
        // 기존 초기화...
        this.fixationDurationMs = builder.fixationDurationMs;
        this.aoiRadius = builder.aoiRadius;
        this.scrollEnabled = builder.scrollEnabled;
        this.edgeMarginRatio = builder.edgeMarginRatio;
        this.edgeTriggerMs = builder.edgeTriggerMs;
        this.continuousScrollCount = builder.continuousScrollCount;
        this.clickEnabled = builder.clickEnabled;
        this.edgeScrollEnabled = builder.edgeScrollEnabled;
        this.blinkDetectionEnabled = builder.blinkDetectionEnabled;
        this.autoOnePointCalibrationEnabled = builder.autoOnePointCalibrationEnabled;
        this.cursorOffsetX = builder.cursorOffsetX;
        this.cursorOffsetY = builder.cursorOffsetY;
        this.oneEuroFilterPreset = builder.oneEuroFilterPreset;

        // OneEuroFilter 값 설정
        if (this.oneEuroFilterPreset == OneEuroFilterPreset.CUSTOM) {
            this.oneEuroFreq = builder.oneEuroFreq;
            this.oneEuroMinCutoff = builder.oneEuroMinCutoff;
            this.oneEuroBeta = builder.oneEuroBeta;
            this.oneEuroDCutoff = builder.oneEuroDCutoff;
        } else {
            this.oneEuroFreq = this.oneEuroFilterPreset.getFreq();
            this.oneEuroMinCutoff = this.oneEuroFilterPreset.getMinCutoff();
            this.oneEuroBeta = this.oneEuroFilterPreset.getBeta();
            this.oneEuroDCutoff = this.oneEuroFilterPreset.getDCutoff();
        }

        // 새로운 필드 초기화
        this.calibrationStrategy = builder.calibrationStrategy;
        this.backgroundLearningEnabled = builder.backgroundLearningEnabled;
    }

    // 기존 getter 메서드들...
    public float getFixationDurationMs() { return fixationDurationMs; }
    public float getAoiRadius() { return aoiRadius; }
    public boolean isScrollEnabled() { return scrollEnabled; }
    public float getEdgeMarginRatio() { return edgeMarginRatio; }
    public long getEdgeTriggerMs() { return edgeTriggerMs; }
    public int getContinuousScrollCount() { return continuousScrollCount; }
    public boolean isClickEnabled() { return clickEnabled; }
    public boolean isEdgeScrollEnabled() { return edgeScrollEnabled; }
    public boolean isBlinkDetectionEnabled() { return blinkDetectionEnabled; }
    public boolean isAutoOnePointCalibrationEnabled() { return autoOnePointCalibrationEnabled; }
    public float getCursorOffsetX() { return cursorOffsetX; }
    public float getCursorOffsetY() { return cursorOffsetY; }
    public OneEuroFilterPreset getOneEuroFilterPreset() { return oneEuroFilterPreset; }
    public double getOneEuroFreq() { return oneEuroFreq; }
    public double getOneEuroMinCutoff() { return oneEuroMinCutoff; }
    public double getOneEuroBeta() { return oneEuroBeta; }
    public double getOneEuroDCutoff() { return oneEuroDCutoff; }

    // 새로운 getter 메서드들
    public CalibrationStrategy getCalibrationStrategy() { return calibrationStrategy; }
    public boolean isBackgroundLearningEnabled() { return backgroundLearningEnabled; }

    public static class Builder {
        // 기존 필드들...
        private float fixationDurationMs = 1000f;
        private float aoiRadius = 40f;
        private boolean scrollEnabled = true;
        private float edgeMarginRatio = 0.01f;
        private long edgeTriggerMs = 3000L;
        private int continuousScrollCount = 2;
        private boolean clickEnabled = true;
        private boolean edgeScrollEnabled = true;
        private boolean blinkDetectionEnabled = false;
        private boolean autoOnePointCalibrationEnabled = true;
        private float cursorOffsetX = 0f;
        private float cursorOffsetY = 0f;
        private OneEuroFilterPreset oneEuroFilterPreset = OneEuroFilterPreset.BALANCED;
        private double oneEuroFreq = 30.0;
        private double oneEuroMinCutoff = 1.0;
        private double oneEuroBeta = 0.007;
        private double oneEuroDCutoff = 1.0;

        // 🎯 새로운 필드들 - 기본값 변경 (정밀 보정 우선)
        private CalibrationStrategy calibrationStrategy = CalibrationStrategy.PRECISION; // QUICK_START에서 변경
        private boolean backgroundLearningEnabled = false; // 기본값을 false로 변경 (안전 우선)

        // 기존 Builder 메서드들...
        public Builder fixationDurationMs(float val) { fixationDurationMs = val; return this; }
        public Builder aoiRadius(float val) { aoiRadius = val; return this; }
        public Builder scrollEnabled(boolean val) { scrollEnabled = val; return this; }
        public Builder edgeMarginRatio(float val) { edgeMarginRatio = val; return this; }
        public Builder edgeTriggerMs(long val) { edgeTriggerMs = val; return this; }
        public Builder continuousScrollCount(int val) { continuousScrollCount = val; return this; }
        public Builder clickEnabled(boolean val) { clickEnabled = val; return this; }
        public Builder edgeScrollEnabled(boolean val) { edgeScrollEnabled = val; return this; }
        public Builder blinkDetectionEnabled(boolean val) { blinkDetectionEnabled = val; return this; }
        public Builder autoOnePointCalibrationEnabled(boolean val) { autoOnePointCalibrationEnabled = val; return this; }
        public Builder cursorOffsetX(float val) { cursorOffsetX = val; return this; }
        public Builder cursorOffsetY(float val) { cursorOffsetY = val; return this; }
        public Builder oneEuroFilterPreset(OneEuroFilterPreset val) { oneEuroFilterPreset = val; return this; }
        public Builder oneEuroFreq(double val) { oneEuroFreq = val; return this; }
        public Builder oneEuroMinCutoff(double val) { oneEuroMinCutoff = val; return this; }
        public Builder oneEuroBeta(double val) { oneEuroBeta = val; return this; }
        public Builder oneEuroDCutoff(double val) { oneEuroDCutoff = val; return this; }

        // 새로운 Builder 메서드들
        public Builder calibrationStrategy(CalibrationStrategy val) { calibrationStrategy = val; return this; }
        public Builder backgroundLearningEnabled(boolean val) { backgroundLearningEnabled = val; return this; }

        public UserSettings build() {
            return new UserSettings(this);
        }
    }
}