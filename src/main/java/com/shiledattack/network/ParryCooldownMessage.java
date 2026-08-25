package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client-bound message carrying how many ticks of parry cooldown remain
 * (played through the network. 0 = parry ready).
 */
public class ParryCooldownMessage {
    private final int remainingTicks;

    public ParryCooldownMessage(int remainingTicks) {
        this.remainingTicks = remainingTicks;
    }

    public ParryCooldownMessage(FriendlyByteBuf buf) {
        this.remainingTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(remainingTicks);
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public static void handle(ParryCooldownMessage msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientParryState.setRemaining(msg.remainingTicks));
        ctx.get().setPacketHandled(true);
    }
}