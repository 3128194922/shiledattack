package com.shiledattack.client.handler;

import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.combat.ChargeAttackHandler;
import com.shiledattack.combat.ChargeAttackTag;
import com.shiledattack.combat.ChargeProfiles;
import com.shiledattack.combat.ClientChargeState;
import com.shiledattack.combat.ConeTargets;
import com.shiledattack.combat.ReachProfile;
import com.shiledattack.network.ParryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端蓄力输入：按下开始、按住计时、松开结算。
 *
 * <p>只有主手物品带有 {@code shiledattack:charge_attack} 标签时才接管左键。
 * 其它物品完全交还原版，保证挖掘、弓、方块交互等行为不受影响。</p>
 *
 * <p>原版攻击入口由 {@code MinecraftChargeMixin} 取消，本类只负责计时与松手结算。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ChargeInputHandler {

    /** 记录上一次左键是否按下，用于识别边沿。 */
    private static boolean wasAttackDown;
    /** 本次蓄力是否由本模组接管。 */
    private static boolean charging;
    /** 本 tick 的原版攻击是否已被接管（由 Mixin 读取后清除）。 */
    private static boolean claimedThisTick;

    private ChargeInputHandler() {
    }

    /**
     * 原版 {@code startAttack} 的拦截点：是否由蓄力系统接管本次按下。
     *
     * <p>每次只回答一次，避免同一 tick 内多次调用时重复消耗。</p>
     */
    public static boolean consumeVanillaPress() {
        if (claimedThisTick) {
            return false;
        }
        if (!shouldTakeOver()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        // 这里只登记意图，真正的计时在 tick 里统一推进。
        if (!charging && !beginCharging(mc.player)) {
            return false;
        }
        claimedThisTick = true;
        return true;
    }

    /** 蓄力期间禁止原版持续攻击/挖掘。 */
    public static boolean shouldBlockVanillaAttack() {
        return charging;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            abortCharging();
            wasAttackDown = false;
            return;
        }

        boolean down = mc.options.keyAttack.isDown();

        if (charging) {
            if (!down) {
                // 同一 tick 内按下又松开的极快点击，或丢失松手事件：这里补结算。
                releaseCharging(mc, player);
            } else if (ClientChargeState.itemChanged() || player.isUsingItem()
                    || player.isDeadOrDying() || player.isSpectator()) {
                // 换武器 / 开始使用物品时放弃蓄力。
                // 看向方块不放弃：蓄力期间的挖掘已由 Mixin 取消 continueAttack 拦住。
                abortCharging();
            } else {
                ClientChargeState.tick();
            }
        }

        // 按键边沿：若 Mixin 尚未接管（例如在没有调用 startAttack 的场景），
        // 这里补一次接管，保证长按始终进入蓄力。
        if (down && !wasAttackDown && !charging && canCharge(player, mc)) {
            beginCharging(player);
        }

        wasAttackDown = down;
        claimedThisTick = false;
    }

    private static boolean canCharge(LocalPlayer player, Minecraft mc) {
        if (!Config.chargeAttackEnabled) return false;
        if (mc.gameMode == null || player.isSpectator()) return false;
        if (player.isUsingItem()) return false;
        ItemStack main = player.getMainHandItem();
        if (!ChargeProfiles.isChargeable(main)) return false;
        // 指向方块时留给挖掘。
        return mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK;
    }

    private static boolean shouldTakeOver() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return false;
        if (!Config.chargeAttackEnabled) return false;
        if (player.isUsingItem() || player.isSpectator()) return false;
        if (!ChargeProfiles.isChargeable(player.getMainHandItem())) return false;
        // 看向方块时属于挖掘，不接管。
        return mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK;
    }

    private static boolean beginCharging(LocalPlayer player) {
        if (!canSend()) return false;
        charging = true;
        ClientChargeState.begin(player.getMainHandItem());
        ParryNetwork.sendChargeStart();
        return true;
    }

    private static void releaseCharging(Minecraft mc, LocalPlayer player) {
        int held = ClientChargeState.heldTicks();
        charging = false;
        ClientChargeState.end();
        if (canSend()) {
            ParryNetwork.sendChargeRelease(findTargetId(mc, player), held);
        }
        player.resetAttackStrengthTicker();
    }

    private static void abortCharging() {
        if (!charging) return;
        charging = false;
        ClientChargeState.end();
        if (canSend()) {
            ParryNetwork.sendChargeCancel();
        }
    }

    /** 断线或正在连接时没有可用连接，发包会抛异常。 */
    private static boolean canSend() {
        return Minecraft.getInstance().getConnection() != null;
    }

    /**
     * 松手时的目标：优先准星实体，其次锥形范围内最近的生物。
     * 两者都没有时上报“无目标”，让服务端仍然广播事件。
     */
    private static int findTargetId(Minecraft mc, LocalPlayer player) {
        if (mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity) {
            return hit.getEntity().getId();
        }
        if (Config.coneAttackEnabled) {
            ReachProfile reach = Config.coneProfile();
            if (!reach.isNone()) {
                LivingEntity cone = ConeTargets.nearest(player.level(), player, reach);
                if (cone != null) {
                    return cone.getId();
                }
            }
        }
        return ChargeAttackHandler.NO_TARGET;
    }

    /** 是否正在蓄力，供 HUD 使用。 */
    public static boolean isCharging() {
        return charging;
    }

    /** 当前蓄力比例。 */
    public static float chargeRatio() {
        return ClientChargeState.ratio();
    }
}
