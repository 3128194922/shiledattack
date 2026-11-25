package com.shiledattack.combat;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * 单件武器的蓄力属性。
 *
 * <p>纯数据，不依赖 Minecraft 类型：JSON、KubeJS 脚本、标签档位、全局配置
 * 最终都会被解析成同一个 {@code ChargeProfile}，结算与 HUD 只认它。</p>
 *
 * @param enabled    该武器是否可蓄力
 * @param minHoldTicks 判定为蓄力的最短按住 tick
 * @param fullTicks  满蓄力所需 tick
 * @param damageBonus 满蓄力时的额外伤害倍率（1.0 = 双倍）
 */
public record ChargeProfile(boolean enabled, int minHoldTicks, int fullTicks, float damageBonus) {

    /** 不可蓄力。 */
    public static final ChargeProfile DISABLED = new ChargeProfile(false, 0, 0, 0.0F);

    /** 归一化：把非法或越界数值收敛到安全范围。 */
    public static ChargeProfile of(boolean enabled, int minHoldTicks, int fullTicks, float damageBonus) {
        if (!enabled) {
            return DISABLED;
        }
        int minHold = clamp(minHoldTicks, 1, 40);
        int full = clamp(fullTicks, 2, 400);
        if (full <= minHold) {
            full = minHold + 1;
        }
        float bonus = Math.max(0.0F, Math.min(10.0F, damageBonus));
        return new ChargeProfile(true, minHold, full, bonus);
    }

    /** 当前按住时长对应的蓄力比例 0..1。 */
    public float ratio(int heldTicks) {
        if (!enabled) {
            return 0.0F;
        }
        return ChargeMath.ratio(heldTicks, fullTicks, minHoldTicks);
    }

    /** 当前按住时长与玩家蓄力速度属性共同决定的蓄力比例 0..1。 */
    public float ratio(int heldTicks, double chargeSpeed) {
        if (!enabled) {
            return 0.0F;
        }
        return ChargeMath.ratio(heldTicks, fullTicks, minHoldTicks, chargeSpeed);
    }

    /** 当前蓄力比例对应的伤害倍率。 */
    public float multiplier(float ratio) {
        if (!enabled) {
            return 1.0F;
        }
        return ChargeMath.multiplier(damageBonus, ratio);
    }

    /** 从数据包 JSON 解析；缺省字段使用常规默认值。 */
    public static ChargeProfile fromJson(JsonObject json) {
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        int minHold = json.has("min_hold") ? json.get("min_hold").getAsInt() : 4;
        int full = json.has("full_ticks") ? json.get("full_ticks").getAsInt() : 20;
        float bonus = json.has("damage_bonus") ? json.get("damage_bonus").getAsFloat() : 1.0F;
        return of(enabled, minHold, full, bonus);
    }

    /**
     * 从 KubeJS 传入的对象解析。
     *
     * <p>脚本里的数字通常是 {@code Double}，布尔是 {@code Boolean}，
     * 所以统一按 {@link Number} / {@link Boolean} 读取，避免类型转换异常。</p>
     */
    public static ChargeProfile fromMap(Map<?, ?> map) {
        if (map == null) {
            return of(true, 4, 20, 1.0F);
        }
        boolean enabled = readBoolean(map, "enabled", true);
        int minHold = readInt(map, "min_hold", readInt(map, "minHold", 4));
        int full = readInt(map, "full_ticks", readInt(map, "fullTicks", 20));
        float bonus = readFloat(map, "damage_bonus", readFloat(map, "damageBonus", 1.0F));
        return of(enabled, minHold, full, bonus);
    }

    private static boolean readBoolean(Map<?, ?> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    private static int readInt(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static float readFloat(Map<?, ?> map, String key, float fallback) {
        Object value = map.get(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
