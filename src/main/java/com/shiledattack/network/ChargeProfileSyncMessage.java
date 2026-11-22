package com.shiledattack.network;

import com.shiledattack.combat.ChargeProfile;
import com.shiledattack.combat.ChargeProfiles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步逐武器蓄力档案。
 *
 * <p>数据包 JSON 属于 {@code data/} 目录，客户端资源包读不到；
 * 因此由服务端在登录与 {@code /reload} 后推送权威结果，
 * 保证 HUD 的蓄力比例与实际结算一致。</p>
 */
public class ChargeProfileSyncMessage {

    private final Map<ResourceLocation, ChargeProfile> profiles;

    public ChargeProfileSyncMessage(Map<ResourceLocation, ChargeProfile> profiles) {
        this.profiles = profiles;
    }

    public ChargeProfileSyncMessage(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, ChargeProfile> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            boolean enabled = buf.readBoolean();
            int minHold = buf.readVarInt();
            int full = buf.readVarInt();
            float bonus = buf.readFloat();
            map.put(id, new ChargeProfile(enabled, minHold, full, bonus));
        }
        this.profiles = map;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(profiles.size());
        for (Map.Entry<ResourceLocation, ChargeProfile> entry : profiles.entrySet()) {
            ChargeProfile profile = entry.getValue();
            buf.writeResourceLocation(entry.getKey());
            buf.writeBoolean(profile.enabled());
            buf.writeVarInt(profile.minHoldTicks());
            buf.writeVarInt(profile.fullTicks());
            buf.writeFloat(profile.damageBonus());
        }
    }

    public static void handle(ChargeProfileSyncMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.shiledattack.client.handler.ChargeProfileSyncHandler.accept(msg.profiles)));
        ctx.get().setPacketHandled(true);
    }

    public Map<ResourceLocation, ChargeProfile> getProfiles() {
        return profiles;
    }
}
