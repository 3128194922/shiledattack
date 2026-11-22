package com.shiledattack.client.handler;

import com.shiledattack.ShiledAttackMod;
import com.shiledattack.combat.ChargeProfile;
import com.shiledattack.combat.ChargeProfiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * 客户端接收服务端同步的蓄力档案，并在断线时清理。
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ChargeProfileSyncHandler {

    private ChargeProfileSyncHandler() {
    }

    public static void accept(Map<ResourceLocation, ChargeProfile> profiles) {
        ChargeProfiles.acceptSynced(profiles);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ChargeProfiles.acceptSynced(Map.of());
    }
}
