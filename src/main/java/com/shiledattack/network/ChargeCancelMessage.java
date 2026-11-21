package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：放弃本次蓄力（换武器、开界面、开始使用物品）。
 *
 * <p>没有这个包时，服务端会残留一个旧的起始 tick，下一次松开可能被误判为
 * 蓄力了很久。</p>
 */
public class ChargeCancelMessage {

    public ChargeCancelMessage() {
    }

    public ChargeCancelMessage(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void handle(ChargeCancelMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                com.shiledattack.combat.ChargeAttackHandler.onCancel(ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
