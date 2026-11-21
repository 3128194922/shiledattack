package com.shiledattack.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：在指定位置触发一次相机抖动。
 *
 * <p>传输位置而不是算好的幅度，让客户端按自身距离做衰减；传输持续时间，客户端到期自动遗忘。</p>
 */
public class CameraShakeMessage {
    private final double x;
    private final double y;
    private final double z;
    private final float degrees;
    private final int ticks;
    private final float reach;

    public CameraShakeMessage(double x, double y, double z, float degrees, int ticks, float reach) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.degrees = degrees;
        this.ticks = ticks;
        this.reach = reach;
    }

    public CameraShakeMessage(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.degrees = buf.readFloat();
        this.ticks = buf.readVarInt();
        this.reach = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(degrees);
        buf.writeVarInt(ticks);
        buf.writeFloat(reach);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getDegrees() {
        return degrees;
    }

    public int getTicks() {
        return ticks;
    }

    public float getReach() {
        return reach;
    }

    public static void handle(CameraShakeMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.shiledattack.client.handler.CameraShakeHandler.accept(msg)));
        ctx.get().setPacketHandled(true);
    }
}
