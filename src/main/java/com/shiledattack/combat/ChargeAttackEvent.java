package com.shiledattack.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * 蓄力攻击结算前的服务端事件。
 *
 * <p>KubeJS 侧对应 {@code ChargeAttackEvents.attacked}，脚本可以读取/修改
 * {@link #getDamage()}，修改结果会直接用于本次伤害。取消事件则本次攻击不造成伤害。</p>
 */
@Cancelable
public class ChargeAttackEvent extends Event {

    private final ServerPlayer player;
    private final LivingEntity target;
    private final ItemStack item;
    private final float chargeRatio;
    private final ChargeProfile profile;
    private float damage;

    public ChargeAttackEvent(ServerPlayer player, LivingEntity target, ItemStack item,
                             float chargeRatio, ChargeProfile profile, float damage) {
        this.player = player;
        this.target = target;
        this.item = item;
        this.chargeRatio = chargeRatio;
        this.profile = profile;
        this.damage = damage;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ItemStack getItem() {
        return item;
    }

    /** 0..1 的蓄力比例。 */
    public float getChargeRatio() {
        return chargeRatio;
    }

    /** 本次攻击使用的武器蓄力属性。 */
    public ChargeProfile getProfile() {
        return profile;
    }

    public float getDamage() {
        return damage;
    }

    /** 修改本次蓄力攻击的最终伤害。 */
    public void setDamage(float damage) {
        this.damage = damage;
    }
}
