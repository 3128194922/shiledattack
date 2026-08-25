package com.shiledattack.network;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ParryNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "parry"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, ParryCooldownMessage.class,
                ParryCooldownMessage::encode,
                ParryCooldownMessage::new,
                ParryCooldownMessage::handle);
    }

    public static void sendCooldown(ServerPlayer player, int remainingTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ParryCooldownMessage(remainingTicks));
    }

    @Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Register {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(ParryNetwork::register);
        }
    }
}