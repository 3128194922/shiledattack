package com.shiledattack.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiledattack.client.handler.ChargeViewAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在物品自己的坐标系里应用蓄力姿态。
 *
 * <p>{@code applyItemArmTransform} 已经把物品移到手部位置；
 * 紧接着的 {@code applyItemArmAttackTransform} 是原版放挥击弧线的位置，
 * 也就是“物品的帧”。在它之前旋转，武器绕握把抬起，而不是整条手臂绕相机转。</p>
 *
 * <p>用调用点而不是方法尾：持物时 {@code applyItemArmTransform} 会被调用两次
 * （使用物品分支 + 通用路径），方法尾会让姿态叠加两遍。</p>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ChargeViewMixin {

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;"
                            + "applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    shift = At.Shift.BEFORE))
    private void shiledattack$chargeWindup(AbstractClientPlayer player, float partialTicks, float rot,
                                           InteractionHand hand, float swingProgress, ItemStack stack,
                                           float equipProgress, PoseStack pose,
                                           MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        ChargeViewAnimation.apply(pose, player.getMainArm());
    }
}
