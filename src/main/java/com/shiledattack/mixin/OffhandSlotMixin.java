package com.shiledattack.mixin;

import com.shiledattack.OffhandRestrictions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁止受限物品通过 GUI 点击、拖拽或 Shift 快速移动进入副手栏位。
 *
 * <p>原版 {@code AbstractContainerMenu} 在点击放置、快速移动与快捷栏交换时
 * 都会先调用 {@link Slot#mayPlace(ItemStack)}，因此在这里拒绝即可覆盖全部
 * 容器操作路径。</p>
 */
@Mixin(Slot.class)
public abstract class OffhandSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void shiledattack$blockOffhandPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (slot.container instanceof Inventory && slot.index == Inventory.SLOT_OFFHAND
                && OffhandRestrictions.isBlocked(stack)) {
            cir.setReturnValue(false);
        }
    }
}
