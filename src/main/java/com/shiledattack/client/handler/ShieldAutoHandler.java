package com.shiledattack.client.handler;

import com.shiledattack.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.shiledattack.ShiledAttackMod.MODID, value = Dist.CLIENT)
public class ShieldAutoHandler {
    private static boolean shieldWasReleased = false;
    private static InteractionHand shieldHand = InteractionHand.OFF_HAND;

    @SubscribeEvent
    public static void onPreClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!Config.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (mc.options.keyAttack.isDown() && mc.player.isUsingItem()) {
            ItemStack useItem = mc.player.getUseItem();
            if (useItem.getItem() instanceof ShieldItem) {
                shieldHand = mc.player.getUsedItemHand();
                mc.player.stopUsingItem();
                shieldWasReleased = true;
            }
        }
    }

    @SubscribeEvent
    public static void onPostClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.enabled) return;
        if (!shieldWasReleased) return;

        shieldWasReleased = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.options.keyUse.isDown() && !mc.player.isUsingItem()) {
            ItemStack stack = mc.player.getItemInHand(shieldHand);
            if (stack.getItem() instanceof ShieldItem) {
                mc.player.startUsingItem(shieldHand);
            } else {
                InteractionHand otherHand = shieldHand == InteractionHand.OFF_HAND
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;
                stack = mc.player.getItemInHand(otherHand);
                if (stack.getItem() instanceof ShieldItem) {
                    mc.player.startUsingItem(otherHand);
                }
            }
        }
    }
}
