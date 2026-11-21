package com.shiledattack.client.handler;

import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.network.CameraShakeMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端相机抖动：把服务端发来的冲量叠加到相机 pitch / roll 上。
 *
 * <p>幅度随时间线性衰减到 0，避免抖动被硬切掉而看起来像卡顿。
 * 相位按冲量年龄计算并取模，防止 {@link Mth#sin} 的查表索引在长时间运行后饱和。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class CameraShakeHandler {

    private static final float PITCH_HZ = 3.1F;
    private static final float ROLL_HZ = 4.7F;
    /** 角度表的一圈，注意不是 65536。 */
    private static final float TURN = (float) (Math.PI * 2.0);
    /** 多个冲量叠加后的总幅度上限（度）。 */
    private static final float MAX_TOTAL_DEGREES = 10.0F;

    private static final List<Impulse> ACTIVE = new ArrayList<>();

    private CameraShakeHandler() {
    }

    private record Impulse(ResourceKey<Level> dimension, double x, double y, double z,
                           float degrees, float bornTick, float diesTick, float reach) {
    }

    public static void accept(CameraShakeMessage msg) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || msg.getDegrees() <= 0.0F || msg.getTicks() <= 0) {
            return;
        }
        float now = minecraft.level.getGameTime();
        ACTIVE.add(new Impulse(minecraft.level.dimension(), msg.getX(), msg.getY(), msg.getZ(),
                msg.getDegrees(), now, now + msg.getTicks(), msg.getReach()));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ACTIVE.isEmpty() || !Config.attackShakeEnabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ResourceKey<Level> here = minecraft.level.dimension();
        float time = minecraft.level.getGameTime() + (float) event.getPartialTick();
        // 过期的、以及上个维度残留的冲量都清掉，否则相机可能永远在抖。
        ACTIVE.removeIf(impulse ->
                !here.equals(impulse.dimension()) || time > impulse.diesTick());
        if (ACTIVE.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float pitch = 0.0F;
        float roll = 0.0F;
        for (Impulse impulse : ACTIVE) {
            double distance = Math.sqrt(camera.distanceToSqr(impulse.x(), impulse.y(), impulse.z()));
            float amplitude = amplitudeAt(distance, impulse.degrees(), impulse.reach());
            if (amplitude <= 0.0F) {
                continue;
            }
            float age = time - impulse.bornTick();
            float life = (impulse.diesTick() - time) / (impulse.diesTick() - impulse.bornTick());
            float energy = amplitude * Math.max(0.0F, life);
            pitch += energy * Mth.sin(angle(age, PITCH_HZ));
            roll += energy * Mth.cos(angle(age, ROLL_HZ));
        }
        if (pitch != 0.0F || roll != 0.0F) {
            event.setPitch(event.getPitch() + clampTotal(pitch));
            event.setRoll(event.getRoll() + clampTotal(roll));
        }
    }

    private static float angle(float time, float frequency) {
        return (time * frequency) % TURN;
    }

    private static float clampTotal(float degrees) {
        return Mth.clamp(degrees, -MAX_TOTAL_DEGREES, MAX_TOTAL_DEGREES);
    }

    /** 距离衰减：平方衰减，边缘几乎为零。 */
    private static float amplitudeAt(double distance, float max, double reach) {
        if (reach <= 0.0 || distance >= reach) {
            return 0.0F;
        }
        double t = 1.0 - distance / reach;
        return (float) (max * t * t);
    }
}
