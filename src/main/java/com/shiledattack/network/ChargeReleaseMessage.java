package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：松开左键，结算一次蓄力攻击。
 *
 * <p>只上报“按住了多少 tick”作为参考；服务端会用自己的计时做权威判定，
 * 客户端无法凭包里的数字伪造满蓄力。</p>
 */
public class ChargeReleaseMessage {
    private final int targetId;
    private final int heldTicks;

    public ChargeReleaseMessage(int targetId, int heldTicks) {
        this.targetId = targetId;
        this.heldTicks = heldTicks;
    }

    public ChargeReleaseMessage(FriendlyByteBuf buf) {
        this.targetId = buf.readVarInt();
        this.heldTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
        buf.writeVarInt(heldTicks);
    }

    public int getTargetId() {
        return targetId;
    }

    public int getHeldTicks() {
        return heldTicks;
    }

    public static void handle(ChargeReleaseMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.shiledattack.combat.ChargeAttackHandler.onRelease(
                ctx.get().getSender(), msg.getTargetId(), msg.getHeldTicks()));
        ctx.get().setPacketHandled(true);
    }
}
