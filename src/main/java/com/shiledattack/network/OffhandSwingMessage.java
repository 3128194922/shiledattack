package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：强制播放某个玩家的副手挥击动画。
 *
 * <p>原版 animate 包会走 {@code LivingEntity#swing} 的“挥到一半不许重开”限制，
 * 副手追击恰好落在主手挥击的前半段，因此需要独立包直接写入挥击状态。</p>
 */
public class OffhandSwingMessage {

    private final int entityId;

    public OffhandSwingMessage(int entityId) {
        this.entityId = entityId;
    }

    public OffhandSwingMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public int getEntityId() {
        return entityId;
    }

    public static void handle(OffhandSwingMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.shiledattack.client.handler.OffhandSwingHandler.accept(msg)));
        ctx.get().setPacketHandled(true);
    }
}
