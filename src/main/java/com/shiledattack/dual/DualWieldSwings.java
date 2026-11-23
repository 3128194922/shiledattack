package com.shiledattack.dual;

import com.shiledattack.network.OffhandSwingMessage;
import com.shiledattack.network.ParryNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

/**
 * 副手挥击动画的强制播放。
 *
 * <p>原版 {@code LivingEntity#swing} 有一条保护：一次挥击进行到一半之前不会重新开始，
 * 因此“主手挥击后 2 tick 再挥副手”会被这条判定直接丢弃，第一人称与第三人称都不会动。
 * 这里直接写入挥击状态，并用自定义包同步给所有能看到该玩家的客户端，
 * 保证动画一定播放且不依赖原版 {@code ClientboundAnimatePacket} 的同一套限制。</p>
 */
public final class DualWieldSwings {

    private DualWieldSwings() {
    }

    /** 服务端/客户端通用的挥击状态写入。 */
    public static void forceSwing(LivingEntity entity, InteractionHand hand) {
        entity.swingingArm = hand;
        entity.swingTime = -1;
        entity.swinging = true;
        // 清掉上一段挥击的进度，避免这一帧用旧进度渲染副手（会闪一下）
        entity.attackAnim = 0.0F;
        entity.oAttackAnim = 0.0F;
    }

    /** 服务端：写入本地状态并同步给追踪该玩家的所有客户端（含本人）。 */
    public static void broadcast(ServerPlayer player) {
        forceSwing(player, InteractionHand.OFF_HAND);
        ParryNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new OffhandSwingMessage(player.getId()));
    }
}
