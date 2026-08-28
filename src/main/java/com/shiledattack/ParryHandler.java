package com.shiledattack;

import com.shiledattack.event.ShieldParriedEvent;
import com.shiledattack.network.ParryNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side shield parry (盾反) logic.
 *
 * A parry triggers when the player raises a shield and a successful
 * {@link ShieldBlockEvent} fires within {@link Config#parryWindowTicks}
 * ticks of raising it. On a parry the shield takes no durability damage,
 * the attacker is knocked back based on the blocked damage and the
 * attacker's bounding-box collision volume, and an anvil sound plays.
 * The parry then enters a {@link Config#parryCooldownTicks}-tick cooldown
 * (a custom per-player cooldown, not an item cooldown).
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
public class ParryHandler {
    /** UUID -> game tick at which the player most recently raised a shield. */
    private static final Map<UUID, Long> RAISED_TICK = new HashMap<>();
    /** UUID -> game tick at which the parry cooldown expires. */
    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    @SubscribeEvent
    public static void onShieldRaised(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ShieldUtil.isShield(event.getItem())) return;
        RAISED_TICK.put(event.getEntity().getUUID(), event.getEntity().level().getGameTime());
    }

    @SubscribeEvent
    public static void onShieldLowered(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity().level().isClientSide) return;
        RAISED_TICK.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        syncCooldown(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) return;
        UUID id = event.getEntity().getUUID();
        RAISED_TICK.remove(id);
        COOLDOWN_END.remove(id);
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        if (blocker.level().isClientSide) return;
        if (!Config.parryEnabled) return;

        // Damage types carrying the shiledattack:unparryable tag can never be parried.
        // The tag is data-driven (data/shiledattack/tags/damage_type/unparryable.json),
        // read directly via the damage source so datapacks/KubeJS can edit it at runtime.
        if (event.getDamageSource().is(ShiledAttackTags.UNPARRYABLE)) return;

        long now = blocker.level().getGameTime();
        UUID id = blocker.getUUID();

        // Custom cooldown gate (not item cooldown).
        Long end = COOLDOWN_END.get(id);
        if (end != null && now < end) return;

        // The block must land within the parry window after raising the shield.
        Long raised = RAISED_TICK.get(id);
        if (raised == null || now - raised > Config.parryWindowTicks) return;

        // --- Successful parry ---
        event.setShieldTakesDamage(false);

        Entity attacker = event.getDamageSource().getEntity();
        LivingEntity livingAttacker = attacker instanceof LivingEntity living ? living : null;
        if (livingAttacker != null) {
            knockbackAttacker(livingAttacker, blocker, event.getBlockedDamage());
        }

        blocker.level().playSound(null, blocker.blockPosition(), SoundEvents.ANVIL_HIT,
                SoundSource.PLAYERS, (float) Config.parrySoundVolume, 1.0F);

        COOLDOWN_END.put(id, now + Config.parryCooldownTicks);
        syncCooldown(blocker);

        // Notify other systems (e.g. the KubeJS ParryEvents plugin) about the parry.
        // The blocker may be a mob (e.g. MobsUseShields shield-wielders), not just a player.
        MinecraftForge.EVENT_BUS.post(new ShieldParriedEvent(
                blocker, livingAttacker, event.getDamageSource(), event.getBlockedDamage()));
    }

    /** Mobs never fire PlayerLoggedOutEvent, so clear their state on death to avoid leaking map entries. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer) return; // players are cleaned up on logout
        UUID id = event.getEntity().getUUID();
        RAISED_TICK.remove(id);
        COOLDOWN_END.remove(id);
    }

    /**
     * Knock the attacker away from the blocker. The horizontal force scales with
     * the blocked damage and is damped by the attacker's collision volume
     * (width * height), so bigger creatures resist being pushed more.
     */
    private static void knockbackAttacker(LivingEntity attacker, LivingEntity blocker, float damage) {
        double volume = Math.max(attacker.getBbWidth(), 0.01) * Math.max(attacker.getBbHeight(), 0.01);
        double force = (damage / volume) * Config.parryKnockbackMul;
        force = Mth.clamp(force, Config.parryKnockbackMin, Config.parryKnockbackMax);

        Vec3 delta = attacker.position().subtract(blocker.position());
        double dx = delta.x;
        double dz = delta.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            Vec3 look = blocker.getLookAngle();
            dx = look.x;
            dz = look.z;
            len = Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4);
        }
        double nx = dx / len;
        double nz = dz / len;

        attacker.setDeltaMovement(attacker.getDeltaMovement()
                .multiply(0.6, 1.0, 0.6)
                .add(nx * force, Config.parryKnockUp, nz * force));
        attacker.hurtMarked = true;
    }

    private static void syncCooldown(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        long now = entity.level().getGameTime();
        Long end = COOLDOWN_END.get(entity.getUUID());
        int remaining = end == null ? 0 : (int) Math.max(0, end - now);
        ParryNetwork.sendCooldown(player, remaining);
    }
}