package com.shiledattack.kubejs;

import com.shiledattack.combat.ChargeAttackEvent;
import com.shiledattack.combat.ChargeProfile;
import dev.latvian.mods.kubejs.server.ServerEventJS;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * KubeJS 事件对象：{@code ChargeAttackEvents.attacked}。
 *
 * <pre>{@code
 * ChargeAttackEvents.attacked(event => {
 *     event.damage *= 1.5
 *     // event.chargeRatio, event.target, event.player, event.item
 *     // event.cancel() 可取消本次伤害
 * });
 * }</pre>
 */
public class ChargeAttackEventJS extends ServerEventJS {

    private final ChargeAttackEvent event;

    public ChargeAttackEventJS(MinecraftServer server, ChargeAttackEvent event) {
        super(server);
        this.event = event;
    }

    public ServerPlayer getPlayer() {
        return event.getPlayer();
    }

    /** 被攻击目标；空挥时为 null。 */
    public LivingEntity getTarget() {
        return event.getTarget();
    }

    public ItemStack getItem() {
        return event.getItem();
    }

    /** 0..1 的蓄力比例。 */
    public float getChargeRatio() {
        return event.getChargeRatio();
    }

    /** 本次武器的蓄力属性（满蓄力 tick、加成等）。 */
    public ChargeProfile getProfile() {
        return event.getProfile();
    }

    /** 满蓄力所需 tick。 */
    public int getFullTicks() {
        return event.getProfile().fullTicks();
    }

    /** 最短按住 tick。 */
    public int getMinHoldTicks() {
        return event.getProfile().minHoldTicks();
    }

    /** 满蓄力额外伤害倍率。 */
    public float getDamageBonus() {
        return event.getProfile().damageBonus();
    }

    public float getDamage() {
        return event.getDamage();
    }

    /** 修改本次蓄力攻击的最终伤害。 */
    public void setDamage(float damage) {
        event.setDamage(damage);
    }

    /** 读取当前事件是否已被取消。 */
    public boolean isCanceled() {
        return event.isCanceled();
    }
}
