package com.shiledattack.mixin;

import com.shiledattack.dual.DualWieldState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 副手追击期间武器被打碎时，把“主手碎裂”的广播改到副手。
 *
 * <p>原版武器物品的 {@code hurtEnemy} 会硬编码 {@link EquipmentSlot#MAINHAND}，
 * 而副手追击复用的是同一段代码，不修正就会在主手播放碎裂动画。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBreakEventMixin {

    @Inject(method = "broadcastBreakEvent(Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void shiledattack$redirectBreakSlot(EquipmentSlot slot, CallbackInfo ci) {
        if (slot == EquipmentSlot.MAINHAND && DualWieldState.isOffhandAttackInProgress()) {
            ((LivingEntity) (Object) this).broadcastBreakEvent(EquipmentSlot.OFFHAND);
            ci.cancel();
        }
    }
}
