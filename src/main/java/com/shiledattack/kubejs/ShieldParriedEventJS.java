package com.shiledattack.kubejs;

import dev.latvian.mods.kubejs.server.ServerEventJS;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * KubeJS event payload for a successful parry ({@code ParryEvents.parried}).
 */
public class ShieldParriedEventJS extends ServerEventJS {
    private final LivingEntity blocker;
    private final LivingEntity attacker;
    private final DamageSource damageSource;
    private final float blockedDamage;

    public ShieldParriedEventJS(MinecraftServer server, LivingEntity blocker,
                                LivingEntity attacker, DamageSource damageSource, float blockedDamage) {
        super(server);
        this.blocker = blocker;
        this.attacker = attacker;
        this.damageSource = damageSource;
        this.blockedDamage = blockedDamage;
    }

    /** The entity that performed the shield parry (player or mob). */
    public LivingEntity getBlocker() {
        return blocker;
    }

    /** The parrying entity as a player, or null if the parry was performed by a non-player entity. */
    public ServerPlayer getPlayer() {
        return blocker instanceof ServerPlayer player ? player : null;
    }

    /** The attacking entity that was parried; may be null. */
    public LivingEntity getAttacker() {
        return attacker;
    }

    /** The source of the damage that was parried. */
    public DamageSource getDamageSource() {
        return damageSource;
    }

    /** The amount of damage that was blocked by the parry. */
    public float getBlockedDamage() {
        return blockedDamage;
    }
}