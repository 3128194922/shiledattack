package com.shiledattack.dual;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;

/**
 * 服务端瞬态状态：仅在副手追击执行期间生效。
 *
 * <p>{@code begin/end} 成对包裹一次 {@code Player#attack} 调用，
 * {@code PlayerMixin} 在该窗口内把攻击管线读取的“主手物品”替换为副手武器，
 * 并把攻击冷却强制视为满蓄力。</p>
 */
public final class DualWieldState {

    private static ItemStack activeWeapon = ItemStack.EMPTY;
    private static boolean inProgress = false;
    private static float attackStrengthScale = 1.0F;
    private static LivingEntity activeOwner;

    private DualWieldState() {
    }

    public static ItemStack getActiveWeapon() {
        return activeWeapon;
    }

    /**
     * 仅当 {@code entity} 是当前正在执行副手追击的玩家时返回副手武器。
     *
     * <p>{@code getMainHandItem} 是高频方法，这里用引用比较而不是 UUID 查询。</p>
     */
    public static ItemStack getActiveWeaponFor(LivingEntity entity) {
        return inProgress && entity == activeOwner ? activeWeapon : ItemStack.EMPTY;
    }

    public static boolean isOffhandAttackInProgress() {
        return inProgress;
    }

    /**
     * 副手追击沿用主手那一击的攻击冷却倍率。
     *
     * <p>延迟 2 tick 后冷却已经推进，直接让原版重新取比例会把伤害压到两成多；
     * 但若一律按满蓄力，快速连点又会变成无代价的双倍伤害。
     * 因此在主手命中时记录比例，副手这一击复用同一比例。</p>
     */
    public static float getAttackStrengthScale() {
        return attackStrengthScale;
    }

    public static void begin(LivingEntity owner, ItemStack offhandWeapon, float strengthScale) {
        activeOwner = owner;
        activeWeapon = offhandWeapon;
        attackStrengthScale = strengthScale;
        inProgress = true;
    }

    public static void end() {
        activeWeapon = ItemStack.EMPTY;
        attackStrengthScale = 1.0F;
        activeOwner = null;
        inProgress = false;
    }
}
