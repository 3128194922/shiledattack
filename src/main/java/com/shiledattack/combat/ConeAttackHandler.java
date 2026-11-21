package com.shiledattack.combat;

import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.network.CameraShakeMessage;
import com.shiledattack.network.ParryNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端锥形攻击结算与命中屏幕抖动。
 *
 * <p>流程：{@link AttackEntityEvent} 记录本次挥击的主目标；主目标真正受伤时
 * （{@link LivingDamageEvent}，此时护甲与附魔已结算）再以主目标的最终伤害为基准，
 * 结算锥形范围内的其他目标，
 * 并给攻击者发送相机抖动。</p>
 *
 * <p>以主目标的伤害为基准而不是自己重算，是为了复用原版（以及其它模组修改过的）
 * 暴击、附魔、伤害加成结果，避免两套数值各说各话。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
public final class ConeAttackHandler {

    /** UUID -> 本 tick 的挥击记录。 */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** 防止锥形伤害再次进入本处理器。 */
    private static boolean sweeping = false;

    private ConeAttackHandler() {
    }

    private record Pending(int targetId, long gameTime) {
    }

    /** 清除某个玩家的挥击登记（换武器、攻击被取消或改走其它结算路径时使用）。 */
    public static void clearSwing(Player player) {
        PENDING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;
        PENDING.put(player.getUUID(),
                new Pending(event.getTarget().getId(), player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (sweeping) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (event.getEntity() == player) return;

        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null) return;
        // 只认同一 tick 的挥击，避免陈旧记录误伤。
        if (pending.gameTime() != event.getEntity().level().getGameTime()) return;
        if (pending.targetId() != event.getEntity().getId()) return;

        float amount = event.getAmount();
        // 被完全格挡（伤害为 0）不算命中：不抖动，也不扩散。
        if (amount <= 0.0F) return;

        applyConeAndShake(player, event.getEntity(), amount);
    }

    /**
     * 命中后的公共处理：攻击者屏幕抖动 + 锥形范围额外目标。
     *
     * <p>原版攻击与蓄力攻击共用这一处，保证两条路径的手感与范围规则一致。</p>
     *
     * @param amount 主目标已经承受的伤害
     */
    public static void applyConeAndShake(Player player, LivingEntity target, float amount) {
        if (amount <= 0.0F) return;

        if (Config.attackShakeEnabled && player instanceof ServerPlayer server) {
            ParryNetwork.sendShake(server, new CameraShakeMessage(
                    target.getX(), target.getY(), target.getZ(),
                    (float) Config.attackShakeDegrees,
                    Config.attackShakeTicks,
                    (float) Config.attackShakeReach));
        }

        if (!Config.coneAttackEnabled) return;

        int extra = Config.coneMaxTargets - 1;
        if (extra <= 0) return;

        ReachProfile reach = Config.coneProfile();
        if (reach.isNone()) return;

        List<LivingEntity> others = ConeTargets.nearestInCone(
                player.level(), player, reach, extra + 1);
        if (others.isEmpty()) return;

        DamageSource source = player.damageSources().playerAttack(player);
        float multiplier = (float) Config.coneDamageMultiplier;

        sweeping = true;
        try {
            int landed = 0;
            for (LivingEntity other : others) {
                if (other == target) continue;
                if (landed >= extra) break;
                // 以主目标的最终伤害为基准并按倍率缩放，不重复计算附魔与暴击。
                float damage = amount * multiplier;
                if (damage > 0.0F) {
                    other.hurt(source, damage);
                    landed++;
                }
            }
        } finally {
            sweeping = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}
