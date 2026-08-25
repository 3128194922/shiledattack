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
    private final ServerPlayer player;
    private final LivingEntity attacker;
    private final DamageSource damageSource;
    private final float blockedDamage;

    public ShieldParriedEventJS(MinecraftServer server, ServerPlayer player,
                                LivingEntity attacker, DamageSource damageSource, float blockedDamage) {
        super(server);
        this.player = player;
        this.attacker = attacker;
        this.damageSource = damageSource;
        this.blockedDamage = blockedDamage;
    }

    /** The player who performed the shield parry. */
    public ServerPlayer getPlayer() {
        return player;
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