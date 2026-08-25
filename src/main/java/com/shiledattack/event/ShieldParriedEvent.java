package com.shiledattack.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge event bus whenever a shield parry succeeds.
 *
 * Modelled after {@code ExtremeEvasionTriggeredEvent}: it is a plain transport
 * event that carries the parrying player, the attacker, the damage source and
 * the blocked damage. KubeJS reposts it as a {@code ParryEvents.parried} event
 * so scripts can react to parries.
 */
public class ShieldParriedEvent extends Event {
    private final ServerPlayer player;
    private final LivingEntity attacker;
    private final DamageSource damageSource;
    private final float blockedDamage;

    public ShieldParriedEvent(ServerPlayer player, LivingEntity attacker,
                              DamageSource damageSource, float blockedDamage) {
        this.player = player;
        this.attacker = attacker;
        this.damageSource = damageSource;
        this.blockedDamage = blockedDamage;
    }

    public ServerPlayer getPlayer() {
        return player;
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