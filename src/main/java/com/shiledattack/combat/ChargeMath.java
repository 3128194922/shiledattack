package com.shiledattack.combat;

/**
 * 蓄力换算：按住时长 -> 蓄力比例 -> 伤害倍率。
 *
 * <p>刻意不依赖 Minecraft 类型，便于测试与复用。低于最短按住时长时比例恒为 0，
 * 也就是一次普通点击不会偷偷吃到蓄力加成。</p>
 */
public final class ChargeMath {

    private ChargeMath() {
    }

    /**
     * @param heldTicks    实际按住 tick 数
     * @param fullTicks    满蓄力所需 tick 数
     * @param minHoldTicks 判定为蓄力的最短按住 tick 数
     * @return 0..1，低于阈值恒为 0
     */
    public static float ratio(int heldTicks, int fullTicks, int minHoldTicks) {
        int min = Math.max(0, minHoldTicks);
        if (heldTicks < min || fullTicks <= 0) {
            return 0.0F;
        }
        if (fullTicks <= min) {
            return 1.0F;
        }
        float value = (float) (heldTicks - min) / (float) (fullTicks - min);
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /**
     * 蓄力伤害倍率。蓄力只加不减：比例 0 时返回 1.0。
     *
     * @param bonus 满蓄力时的额外倍率（1.0 表示满蓄力伤害翻倍）
     */
    public static float multiplier(float bonus, float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        return 1.0F + Math.max(0.0F, bonus) * clamped;
    }
}
