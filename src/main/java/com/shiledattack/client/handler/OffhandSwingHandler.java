package com.shiledattack.client.handler;

import com.shiledattack.network.OffhandSwingMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 客户端：把服务端下发的副手挥击应用到目标实体上。
 *
 * <p>第一人称由 {@code ItemInHandRenderer} 读取 {@code swingingArm} 决定哪只手播放攻击动画，
 * 第三人称由 {@code HumanoidModel#setupAttackAnimation} 读取同一字段，
 * 因此只需写入挥击状态即可让两侧动画同时正常播放。</p>
 */
public final class OffhandSwingHandler {

    private OffhandSwingHandler() {
    }

    public static void accept(OffhandSwingMessage msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(msg.getEntityId());
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        com.shiledattack.dual.DualWieldSwings.forceSwing(living, net.minecraft.world.InteractionHand.OFF_HAND);
    }
}
