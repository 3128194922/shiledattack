package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：开始蓄力。
 *
 * <p>服务端在这里记录起始 tick，后续松开时的蓄力比例以服务端计时为准。</p>
 */
public class ChargeStartMessage {

    public ChargeStartMessage() {
    }

    public ChargeStartMessage(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void handle(ChargeStartMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                com.shiledattack.combat.ChargeAttackHandler.onStart(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
