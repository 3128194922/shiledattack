package com.shiledattack.combat;

/**
 * 锥形攻击范围的形状：距离 + 水平半角 + 垂直半角。
 *
 * <p>垂直角通常比水平角更窄，避免挥击连带打到脚下或头顶的目标。</p>
 */
public record ReachProfile(float distance, float horizontalAngle, float verticalAngle) {

    /** 空锥体：距离为 0，任何目标都不可能落在里面。 */
    public static final ReachProfile NONE = new ReachProfile(0.0F, 0.0F, 0.0F);

    public boolean isNone() {
        return distance <= 0.0F;
    }
}
