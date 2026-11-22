package com.shiledattack.client.handler;

/**
 * 锥形范围指示器的几何计算，移植自 {@code cortes-combat-rainbow} 的 {@code ConeGauge}。
 *
 * <p>核心是把锥体的水平半角投影成屏幕像素：</p>
 *
 * <pre>span = (height / 2) / tan(fov / 2) * tan(angle)</pre>
 *
 * <p>像素每弧度由垂直 FOV 与屏幕高度决定，因此任何分辨率、GUI 缩放、FOV 下都成立，
 * 不需要按分辨率写死数字。宽角度的切线再做饱和压缩，避免 75° 的锥体直接铺满屏幕。</p>
 */
public final class ConeGauge {

    /** 与真实投影的比例，1 表示不再额外缩放。 */
    private static final double SCALE = 1.0;
    /** 切线饱和系数，把 17:1 的角度范围压到约 7:1。 */
    private static final double SATURATION = 2.0;
    /** 括号最多占屏幕宽度的比例。 */
    private static final float MAX_SCREEN_FRACTION = 0.42F;
    /** 再窄也不会贴到准星上。 */
    private static final int MIN_HALF_SPAN = 42;

    /** 进度条靠近括号一端的 alpha。 */
    public static final int ALPHA_NEAR_BRACKET = 175;
    /** 靠近准星一端的 alpha，0 表示从无到有地淡出。 */
    public static final int ALPHA_NEAR_CROSSHAIR = 0;
    /** 整个部件的存在感，压低是为了不和准星抢视觉。 */
    public static final float WIDGET_ALPHA = 0.42F;
    /** 括号自身的 alpha。 */
    public static final float BRACKET_ALPHA = 0.22F;
    /** 进度条与括号之间留的像素空隙。 */
    public static final int BAR_INSET = 5;
    /** 进度条粗细。 */
    public static final int BAR_THICKNESS = 2;

    private ConeGauge() {
    }

    /** 锥体水平半角在屏幕上的半跨度（像素）。 */
    public static int halfSpan(float horizontalAngle, int guiHeight, double fovDegrees, int guiWidth) {
        if (guiHeight <= 0 || fovDegrees <= 0.0 || fovDegrees >= 180.0) {
            return MIN_HALF_SPAN;
        }
        double fovHalf = Math.toRadians(fovDegrees) / 2.0;
        double pixelsPerRadian = (guiHeight / 2.0) / Math.tan(fovHalf);
        double angle = Math.min(Math.max(horizontalAngle, 0.0F), 89.0F);
        double tangent = Math.tan(Math.toRadians(angle));
        double compressed = tangent / (1.0 + tangent / SATURATION);
        double span = pixelsPerRadian * compressed * SCALE;
        if (!Double.isFinite(span) || span <= 0.0) {
            return MIN_HALF_SPAN;
        }
        int result = (int) Math.round(span);
        if (guiWidth > 0) {
            result = Math.min(result, Math.round(guiWidth * MAX_SCREEN_FRACTION));
        }
        return Math.max(MIN_HALF_SPAN, result);
    }

    /** 一次绘制需要的全部数值。 */
    public record Geometry(int halfSpan, int barInner, int barOuter, int barLength) {

        public int barEnd() {
            return barOuter + barLength;
        }
    }

    public static Geometry geometry(float horizontalAngle, int guiWidth, int guiHeight,
                                    double fov, int crosshairReach) {
        int halfSpan = halfSpan(horizontalAngle, guiHeight, fov, guiWidth);
        int inner = halfSpan - BAR_INSET;
        int length = Math.max(0, inner - crosshairReach);
        return new Geometry(halfSpan, inner, crosshairReach, length);
    }

    /**
     * 进度条某一列的 alpha。
     *
     * @param i      列号，0 在准星端
     * @param length 总长度
     * @param filled 已填充列数
     */
    public static int barAlpha(int i, int length, int filled) {
        if (length <= 0 || i < 0 || i >= filled) {
            return 0;
        }
        float t = length <= 1 ? 1.0F : (float) i / (length - 1);
        int shape = Math.round(ALPHA_NEAR_CROSSHAIR
                + (ALPHA_NEAR_BRACKET - ALPHA_NEAR_CROSSHAIR) * smoothstep(t));
        return Math.round(shape * WIDGET_ALPHA);
    }

    private static float smoothstep(float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
