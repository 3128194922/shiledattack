package com.shiledattack.combat;

import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端蓄力攻击结算。
 *
 * <p>客户端只上报“开始蓄力 / 松开”，服务端用 {@link net.minecraft.world.level.Level#getGameTime()}
 * 自行计时，因此蓄力比例不可被客户端伪造。结算时依次：</p>
 * <ol>
 *   <li>按服务端计时算出蓄力比例；</li>
 *   <li>以玩家攻击力 × 蓄力倍率得到基础伤害；</li>
 *   <li>广播 {@link ChargeAttackEvent}，允许 KubeJS 修改伤害或取消；</li>
 *   <li>造成伤害（锥形范围仍由 {@link ConeAttackHandler} 扩展）。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
public final class ChargeAttackHandler {

    /** 常量：没有目标。 */
    public static final int NO_TARGET = -1;

    private static final Map<UUID, Start> START_TICK = new HashMap<>();

    /** 蓄力起点：服务端 tick + 所在维度。维度切换后旧计时作废。 */
    private record Start(long tick, ResourceKey<Level> dimension) {
    }

    private ChargeAttackHandler() {
    }

    /** 客户端开始蓄力。 */
    public static void onStart(ServerPlayer player) {
        if (player == null) return;
        if (!Config.chargeAttackEnabled) return;
        if (!ChargeProfiles.isChargeable(player.getMainHandItem())) return;
        START_TICK.put(player.getUUID(),
                new Start(player.level().getGameTime(), player.level().dimension()));
    }

    /** 客户端放弃蓄力（换武器、开界面、开始使用物品）。 */
    public static void onCancel(ServerPlayer player) {
        if (player == null) return;
        START_TICK.remove(player.getUUID());
    }

    /**
     * 客户端松开左键，结算一次蓄力攻击。
     *
     * @param clientHeldTicks 客户端按住时长，仅作兜底参考
     */
    public static void onRelease(ServerPlayer player, int targetId, int clientHeldTicks) {
        if (player == null) return;
        if (!Config.chargeAttackEnabled) return;
        Start start = START_TICK.remove(player.getUUID());

        ItemStack item = player.getMainHandItem();
        ChargeProfile profile = ChargeProfiles.resolve(item);
        if (!profile.enabled()) return;
        if (player.isSpectator() || player.isUsingItem()) return;

        long now = player.level().getGameTime();
        int heldTicks = -1;
        if (start != null && start.dimension().equals(player.level().dimension())) {
            heldTicks = (int) Math.max(0, now - start.tick());
        }
        // 服务端计时缺失时（开始包丢失、刚登录或换维度），退化为客户端上报值。
        if (heldTicks <= 0) {
            heldTicks = Math.max(0, clientHeldTicks);
        }

        float ratio = profile.ratio(heldTicks);
        float multiplier = profile.multiplier(ratio);
        // 原版点击伤害会随攻击冷却进度衰减：damage = 攻击力 × (0.2 + 冷却² × 0.8)。
        // 蓄力沿用同一公式，让“快速连点”与“满蓄力”之间保持合理的节奏差。
        float cooldownScale = player.getAttackStrengthScale(0.5F);
        float scale = Math.max(0.0F, Math.min(1.0F, cooldownScale));
        float attackDamage = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = attackDamage * (0.2F + scale * scale * 0.8F) * multiplier;
        // 附魔加成（锋利 / 亡灵杀手等）与被击目标有关，这里按主目标计算一次。
        LivingEntity target = resolveTarget(player, targetId);
        if (target != null) {
            if (!withinReach(player, target)) {
                target = null;
            }
        }
        if (target != null) {
            damage += EnchantmentHelper.getDamageBonus(item, target.getMobType()) * scale * multiplier;
        }

        // 跳劈（下落暴击）：与同一次蓄力挥击合并，不再额外触发一次普通攻击。
        // 这里只负责判定，真正的暴击结算与特效交给原版机制（见下方 hurt 后的 criticalHit）。
        boolean critical = target != null
                && Config.chargeCriticalEnabled
                && isFallingCritical(player, target);

        if (target == null || damage <= 0.0F) {
            // 空挥也要让脚本收到事件，方便播放特效/音效。
            ChargeAttackEvent miss = new ChargeAttackEvent(player, null, item, ratio, profile,
                    Math.max(0.0F, damage));
            MinecraftForge.EVENT_BUS.post(miss);
            swingOnly(player);
            return;
        }

        ChargeAttackEvent event = new ChargeAttackEvent(player, target, item, ratio, profile, damage);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            // 脚本取消伤害，但动作照常播放，避免“点了没反应”。
            swingOnly(player);
            return;
        }
        float finalDamage = event.getDamage();
        if (finalDamage <= 0.0F) {
            swingOnly(player);
            return;
        }

        // 蓄力攻击走独立结算：先把无敌帧清零，确保这一击一定落下，
        // 再由 ConeAttackHandler 的扩展点处理锥形扩散与屏幕抖动。
        // 清掉可能残留的原版挥击登记，避免同 tick 内被重复结算一次。
        ConeAttackHandler.clearSwing(player);
        target.invulnerableTime = 0;
        boolean landed = critical
                ? criticalHit(player, target, finalDamage)
                : target.hurt(player.damageSources().playerAttack(player), finalDamage);
        if (landed) {
            ConeAttackHandler.applyConeAndShake(player, target, finalDamage);
        }
        swingOnly(player);
    }

    /**
     * 原版跳劈判定条件，与 {@code Player#attack} 保持一致。
     *
     * <p>原版字节码里的条件是：{@code fallDistance > 0}、未落地、不在梯子上、
     * 不在水中、无漂浮效果、非骑乘、目标为生物，并且不处于疾跑状态。</p>
     */
    private static boolean isFallingCritical(ServerPlayer player, LivingEntity target) {
        if (target == null) return false;
        if (player.fallDistance <= 0.0F) return false;
        if (player.onGround()) return false;
        if (player.onClimbable()) return false;
        if (player.isInWater()) return false;
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) return false;
        if (player.isPassenger()) return false;
        // 原版在疾跑时不触发跳劈（疾跑击退与暴击互斥）。
        return !player.isSprinting();
    }

    /**
     * 走原版跳劈的完整结算：先经过 {@code CriticalHitEvent}，命中后调用 {@code Player#crit}
     * 播放原版粒子与音效。
     *
     * <p>这样 {@code morecallback} 的 {@code VanillaCriticalHitEvent} 也会随
     * {@code CriticalHitEvent} 一起触发，优先级与抑制逻辑都与原版一致。</p>
     */
    private static boolean criticalHit(ServerPlayer player, LivingEntity target, float damage) {
        var criticalEvent = net.minecraftforge.common.ForgeHooks.getCriticalHit(
                player, target, true, (float) Config.chargeCriticalMultiplier);
        if (criticalEvent == null) {
            // 被监听器取消（例如 morecallback 的抑制逻辑），退化为普通伤害。
            return target.hurt(player.damageSources().playerAttack(player), damage);
        }
        float modified = damage * criticalEvent.getDamageModifier();
        boolean landed = target.hurt(player.damageSources().playerAttack(player), modified);
        if (landed) {
            // 原版暴击特效：粒子 + PLAYER_ATTACK_CRIT 音效。
            player.crit(target);
        }
        return landed;
    }

    private static void swingOnly(ServerPlayer player) {
        player.resetAttackStrengthTicker();
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private static LivingEntity resolveTarget(ServerPlayer player, int targetId) {
        if (targetId == NO_TARGET) return null;
        Entity entity = player.level().getEntity(targetId);
        if (!(entity instanceof LivingEntity living)) return null;
        if (living == player || !living.isAlive() || living.isSpectator() || living.isRemoved()) {
            return null;
        }
        return living;
    }

    /**
     * 服务端校验目标距离与角度，避免客户端上报一个够不到的目标。
     * 多留一格容差，抵消网络延迟带来的位置误差。
     */
    private static boolean withinReach(ServerPlayer player, LivingEntity target) {
        com.shiledattack.combat.ReachProfile reach = Config.coneProfile();
        if (reach.isNone()) {
            return player.distanceToSqr(target) <= 36.0;
        }
        ConeMath.Prepared limits = ConeMath.prepare(
                reach.distance() + 1.0, reach.horizontalAngle(), reach.verticalAngle());
        return ConeTargets.inCone(player.getEyePosition(), player.getLookAngle(), target, limits);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        START_TICK.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        START_TICK.remove(event.getEntity().getUUID());
    }
}
