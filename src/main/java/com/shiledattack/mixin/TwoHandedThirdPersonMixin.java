package com.shiledattack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiledattack.TwoHandedWeapons;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class TwoHandedThirdPersonMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void shiledattack$suppressOffhandRender(LivingEntity entity, ItemStack itemStack,
            ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (entity instanceof Player player && itemStack == player.getOffhandItem()
                && TwoHandedWeapons.shouldSuppressOffhandRender(player)) {
            ci.cancel();
        }
    }
}
