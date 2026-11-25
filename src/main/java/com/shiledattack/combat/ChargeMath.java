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
     * 按玩家蓄力速度属性缩放武器档位后计算比例。
     *
     * <p>最短按住与满蓄力时长按同一比例缩放，最低均为 1 tick。
     * 属性为 0 时与原公式完全一致。</p>
     *
     * @param chargeSpeed 玩家蓄力速度属性，0..1
     */
    public static float ratio(int heldTicks, int fullTicks, int minHoldTicks, double chargeSpeed) {
        int min = scaleTicks(Math.max(1, minHoldTicks), chargeSpeed);
        int full = scaleTicks(Math.max(min, fullTicks), chargeSpeed);
        return ratio(heldTicks, full, min);
    }

    /**
     * 按蓄力速度属性缩放 tick 数，最低保留 1 tick。
     *
     * <p>属性为比例值 0..1；0 不缩放，0.5 耗时减半，1 直接落到 1 tick。</p>
     */
    public static int scaleTicks(int baseTicks, double chargeSpeed) {
        double factor = 1.0D - Math.max(0.0D, Math.min(1.0D, chargeSpeed));
        return Math.max(1, (int) Math.round(Math.max(1, baseTicks) * factor));
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
