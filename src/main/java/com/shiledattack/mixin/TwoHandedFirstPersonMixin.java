package com.shiledattack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiledattack.TwoHandedWeapons;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class TwoHandedFirstPersonMixin {
    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void shiledattack$suppressOffhandRender(AbstractClientPlayer player, float partialTicks, float rot,
            InteractionHand hand, float swingProgress, ItemStack itemStack, float equipProgress,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && TwoHandedWeapons.shouldSuppressOffhandRender(player)) {
            ci.cancel();
        }
    }
}
