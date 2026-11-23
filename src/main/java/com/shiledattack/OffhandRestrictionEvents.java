package com.shiledattack;

import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 禁止把受限物品通过 F 键交换到副手。 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
public final class OffhandRestrictionEvents {
    private OffhandRestrictionEvents() {
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (OffhandRestrictions.isBlocked(event.getItemSwappedToOffHand())) {
            event.setCanceled(true);
        }
    }
}
