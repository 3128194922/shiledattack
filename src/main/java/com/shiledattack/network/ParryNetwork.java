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
        CHANNEL.registerMessage(1, CameraShakeMessage.class,
                CameraShakeMessage::encode,
                CameraShakeMessage::new,
                CameraShakeMessage::handle);
        CHANNEL.registerMessage(2, ChargeStartMessage.class,
                ChargeStartMessage::encode,
                ChargeStartMessage::new,
                ChargeStartMessage::handle);
        CHANNEL.registerMessage(3, ChargeReleaseMessage.class,
                ChargeReleaseMessage::encode,
                ChargeReleaseMessage::new,
                ChargeReleaseMessage::handle);
        CHANNEL.registerMessage(4, ChargeCancelMessage.class,
                ChargeCancelMessage::encode,
                ChargeCancelMessage::new,
                ChargeCancelMessage::handle);
    }

    public static void sendCooldown(ServerPlayer player, int remainingTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ParryCooldownMessage(remainingTicks));
    }

    public static void sendShake(ServerPlayer player, CameraShakeMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendChargeStart() {
        CHANNEL.sendToServer(new ChargeStartMessage());
    }

    public static void sendChargeRelease(int targetId, int heldTicks) {
        CHANNEL.sendToServer(new ChargeReleaseMessage(targetId, heldTicks));
    }

    public static void sendChargeCancel() {
        CHANNEL.sendToServer(new ChargeCancelMessage());
    }

    @Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Register {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(ParryNetwork::register);
        }
    }
}
