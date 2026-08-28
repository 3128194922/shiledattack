package com.shiledattack.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge event bus whenever a shield parry succeeds.
 *
 * Modelled after {@code ExtremeEvasionTriggeredEvent}: it is a plain transport
 * event that carries the parrying entity, the attacker, the damage source and
 * the blocked damage. KubeJS reposts it as a {@code ParryEvents.parried} event
 * so scripts can react to parries.
 *
 * The blocker may be any {@link LivingEntity} (e.g. a shield-wielding mob from
 * MobsUseShields); {@link #getPlayer()} returns null when the parry was
 * performed by a non-player entity.
 */
public class ShieldParriedEvent extends Event {
    private final LivingEntity blocker;
    private final LivingEntity attacker;
    private final DamageSource damageSource;
    private final float blockedDamage;

    public ShieldParriedEvent(LivingEntity blocker, LivingEntity attacker,
                              DamageSource damageSource, float blockedDamage) {
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

    public LivingEntity getAttacker() {
        return attacker;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getBlockedDamage() {
        return blockedDamage;
    }
}