package com.shiledattack.client.handler;

import com.shiledattack.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.shiledattack.ShiledAttackMod.MODID, value = Dist.CLIENT)
@SuppressWarnings("deprecation")
public class ShieldAutoHandler {
    private static final TagKey<Item> SHIELD_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/shields"));
    private static final TagKey<Item> AUTO_SWING_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("shiledattack", "auto_swing"));
    private static boolean shieldWasReleased = false;
    private static InteractionHand shieldHand = InteractionHand.OFF_HAND;
    // 潜行自动举盾状态
    private static boolean wasSneaking = false;
    private static boolean wasRealKeyUseDown = false;
    private static boolean fakeKeyUseDown = false;

    private static boolean isShield(ItemStack stack) {
        return stack.is(SHIELD_TAG);
    }

    /**
     * 通过 gameMode.useItem 发包举盾，确保服务端同步。
     * 优先副手，其次主手。
     */
    private static void raiseShield(Minecraft mc) {
        if (mc.gameMode == null) return;
        ItemStack offHand = mc.player.getOffhandItem();
        ItemStack mainHand = mc.player.getMainHandItem();
        if (isShield(offHand)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
        } else if (isShield(mainHand)) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    @SubscribeEvent
    public static void onPreClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // --- Sneak auto-shield with seamless right-click switching ---
        // Phase.START 在原版 handleKeybinds() 之前触发，可在原版处理前调整状态
        if (Config.sneakAutoShield) {
            boolean isSneaking = mc.player.isShiftKeyDown();
            boolean realKeyUseDown = mc.options.keyUse.isDown();

            if (isSneaking) {
                if (!realKeyUseDown) {
                    // 玩家未按右键：需要自动举盾
                    if (!mc.player.isUsingItem()) {
                        // 未使用物品：举盾
                        raiseShield(mc);
                    }
                    // 防止原版 handleKeybinds 释放盾牌：
                    // 原版在 isUsingItem()=true 且 keyUse.isDown()=false 时会调用 releaseUsingItem
                    // 设置 keyUse.setDown(true) 让原版认为右键按住，不释放
                    if (mc.player.isUsingItem() && isShield(mc.player.getUseItem())) {
                        mc.options.keyUse.setDown(true);
                        fakeKeyUseDown = true;
                    }
                } else {
                    // 玩家按了右键
                    if (!wasRealKeyUseDown) {
                        // 右键按下瞬间：放盾让原版 handleKeybinds 处理主手右键
                        // 必须用 gameMode.releaseUsingItem 发包，否则服务端仍认为在举盾
                        // stopUsingItem 是纯本地操作，不通知服务端
                        if (mc.player.isUsingItem() && isShield(mc.player.getUseItem())) {
                            mc.gameMode.releaseUsingItem(mc.player);
                        }
                    }
                    // 持续按住右键时不干预，让原版处理主手物品或副手盾牌
                }
            } else {
                // 未潜行：停止潜行时放下自动举起的盾牌
                if (wasSneaking && mc.player.isUsingItem() && isShield(mc.player.getUseItem())
                        && !realKeyUseDown) {
                    mc.gameMode.releaseUsingItem(mc.player);
                }
            }

            wasSneaking = isSneaking;
            wasRealKeyUseDown = realKeyUseDown;
        }

        // --- Shield auto-release on attack ---
        if (Config.enabled && mc.options.keyAttack.isDown() && mc.player.isUsingItem()) {
            ItemStack useItem = mc.player.getUseItem();
            if (isShield(useItem)) {
                shieldHand = mc.player.getUsedItemHand();
                // 用 gameMode.releaseUsingItem 发包释放，确保服务端同步
                mc.gameMode.releaseUsingItem(mc.player);
                shieldWasReleased = true;
                // 潜行自动举盾场景下，之前为了保护盾牌而假按了右键（fakeKeyUseDown）。
                // 放盾后必须立即解除假按，否则原版 handleKeybinds 会把该假右键当作
                // 真实右键，对主手物品误触发一次 use（右键）事件（此时未在使用物品）。
                // 这里仅解除假按，不影响用户真实按住的右键。
                if (fakeKeyUseDown) {
                    fakeKeyUseDown = false;
                    mc.options.keyUse.setDown(false);
                }
            }
        }

        // --- Auto-attack when holding the attack button ---
        // 攻击间隔由武器攻击冷却决定：冷却满时才攻击
        // Runs after shield release so the shield is down when the attack fires.
        if (Config.holdAttackButton && mc.options.keyAttack.isDown()
                && !mc.player.isUsingItem() && mc.gameMode != null) {
            if (mc.player.getAttackStrengthScale(0.5F) >= 1.0F) {
                ItemStack mainHandItem = mc.player.getMainHandItem();
                if (mainHandItem.is(AUTO_SWING_TAG)) {
                    // 拥有 auto_swing tag 的武器：冷却满立即挥动，不检测实体
                    if (mc.hitResult instanceof EntityHitResult entityHitResult) {
                        // 有实体目标：正常攻击（触发 hurtEnemy）
                        mc.gameMode.attack(mc.player, entityHitResult.getEntity());
                    } else {
                        // 空挥：触发 LeftClickEmpty 事件，兼容 cataclysm 等 ILeftClick 武器
                        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickEmpty(mc.player));
                    }
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    mc.player.resetAttackStrengthTicker();
                } else if (mc.hitResult instanceof EntityHitResult entityHitResult) {
                    // 普通武器：需要准星对准实体才攻击
                    mc.gameMode.attack(mc.player, entityHitResult.getEntity());
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    mc.player.resetAttackStrengthTicker();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPostClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 恢复 fakeKeyUseDown（在 handleKeybinds 之后，不影响原版处理）
        if (fakeKeyUseDown) {
            mc.options.keyUse.setDown(false);
            fakeKeyUseDown = false;
        }

        // 攻击后重新举盾
        if (!shieldWasReleased) return;
        shieldWasReleased = false;

        if (mc.player.isUsingItem()) return;

        if (Config.enabled && mc.options.keyUse.isDown()) {
            // 手动按右键路径：客户端本地恢复
            ItemStack stack = mc.player.getItemInHand(shieldHand);
            if (isShield(stack)) {
                mc.player.startUsingItem(shieldHand);
            } else {
                InteractionHand otherHand = shieldHand == InteractionHand.OFF_HAND
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;
                stack = mc.player.getItemInHand(otherHand);
                if (isShield(stack)) {
                    mc.player.startUsingItem(otherHand);
                }
            }
        } else if (Config.sneakAutoShield && mc.player.isShiftKeyDown()
                && !mc.options.keyUse.isDown()) {
            // 潜行路径：攻击后重新举盾（通过发包确保服务端同步）
            raiseShield(mc);
        }
    }
}
