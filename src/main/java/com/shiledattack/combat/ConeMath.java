package com.shiledattack.combat;

/**
 * 锥形攻击范围的纯几何判定。
 *
 * <p>刻意不出现任何 Minecraft 类型：判定逻辑与实体、世界解耦，便于复用与测试。
 * 坐标系跟随视线（look 向量即锥体前向），因此抬头可打到头顶、低头可打到脚下。</p>
 */
public final class ConeMath {

    private static final double EPSILON = 1.0E-6;

    /** 目标自身尺寸折算成额外容差的比例（半宽 / 半高）。 */
    public static final double LEASH_FRACTION = 0.65;

    /** 尺寸容差上限（格）。防止巨型生物让锥形判定失去意义。 */
    public static final double LEASH_CAP = 0.8;

    private ConeMath() {
    }

    /** 目标尺寸带来的额外容差，近身时弥补固定角度过窄的问题。 */
    public static double leash(double halfExtent) {
        if (halfExtent <= 0.0 || Double.isNaN(halfExtent)) {
            return 0.0;
        }
        return Math.min(halfExtent * LEASH_FRACTION, LEASH_CAP);
    }

    /** 把武器参数预先换算成切线，供一次扫描内多次判定复用。 */
    public static Prepared prepare(double maxDistance, double horizontalAngle, double verticalAngle) {
        return new Prepared(maxDistance, tangent(horizontalAngle), tangent(verticalAngle));
    }

    /** 角度转切线，越界或非法值一律收敛到安全范围。 */
    public static double tangent(double degrees) {
        if (Double.isNaN(degrees) || degrees <= 0.0) {
            return 0.0;
        }
        return Math.tan(Math.toRadians(Math.min(degrees, 89.0)));
    }

    /**
     * 判定目标点是否落在锥体内。
     *
     * @param leashLateral  目标宽度带来的横向容差（格）
     * @param leashVertical 目标高度带来的纵向容差（格）
     */
    public static boolean inCone(double originX, double originY, double originZ,
                                 double lookX, double lookY, double lookZ,
                                 double targetX, double targetY, double targetZ,
                                 Prepared limits, double leashLateral, double leashVertical) {
        double dx = targetX - originX;
        double dy = targetY - originY;
        double dz = targetZ - originZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > limits.maxDistance()) {
            return false;
        }
        if (distance < EPSILON) {
            return true;
        }

        double lookLen = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        if (lookLen < EPSILON) {
            return false;
        }
        double lx = lookX / lookLen;
        double ly = lookY / lookLen;
        double lz = lookZ / lookLen;

        Frame frame = frame(lx, ly, lz);

        double along = dx * frame.fx() + dy * frame.fy() + dz * frame.fz();
        double lateral = dx * frame.rx() + dy * frame.ry() + dz * frame.rz();
        double vertical = dx * frame.ux() + dy * frame.uy() + dz * frame.uz();

        // 目标在身后：容差是前向容差，不能把它放进来。
        if (along <= EPSILON) {
            return false;
        }

        double allowedLateral = Math.max(along * limits.tanHorizontal(), leashLateral);
        double allowedVertical = Math.max(along * limits.tanVertical(), leashVertical);

        return Math.abs(lateral) <= allowedLateral && Math.abs(vertical) <= allowedVertical;
    }

    /**
     * 由视线向量构造正交基。
     *
     * <p>正上 / 正下看时叉乘退化，右向回退到世界 X 轴，保证坐标系有限且不随机翻转。</p>
     */
    public static Frame frame(double lx, double ly, double lz) {
        double horizLen = Math.sqrt(lx * lx + lz * lz);
        double rx;
        double ry;
        double rz;
        if (horizLen < EPSILON) {
            rx = 1.0;
            ry = 0.0;
            rz = 0.0;
        } else {
            rx = -lz / horizLen;
            ry = 0.0;
            rz = lx / horizLen;
        }
        return new Frame(lx, ly, lz,
                rx, ry, rz,
                ry * lz - rz * ly, rz * lx - rx * lz, rx * ly - ry * lx);
    }

    /** 轴对齐盒上离给定点最近的坐标。 */
    public static double closestOnAxis(double point, double min, double max) {
        if (point < min) {
            return min;
        }
        return Math.min(point, max);
    }

    /** 预先换算好的锥体参数。 */
    public record Prepared(double maxDistance, double tanHorizontal, double tanVertical) {
    }

    /** 锥体的三个正交单位轴。 */
    public record Frame(double fx, double fy, double fz,
                        double rx, double ry, double rz,
                        double ux, double uy, double uz) {
    }
}
