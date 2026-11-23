package com.shiledattack.mixin;

import com.shiledattack.dual.DualWieldState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 副手追击期间让整个玩家的 {@code getMainHandItem()} 返回副手武器。
 *
 * <p>只重定向 {@code Player#attack} 内部的调用还不够：攻击入口
 * {@code ForgeHooks#onPlayerAttackTarget} 会另外读取主手物品，并用它的
 * {@code onLeftClickEntity} 决定是否跳过原版伤害。模组武器一旦返回 {@code true}
 * （自己处理点击），副手重放就会被整段跳过——表现就是只播动画、不掉血。</p>
 *
 * <p>把方法本身改成返回副手武器后，这条路径也会用副手武器判断，
 * 副手武器自己处理点击时同样能正常结算。</p>
 */
@Mixin(LivingEntity.class)
public abstract class DualWieldLivingEntityMixin {

    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void shiledattack$dualWieldMainHand(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack override = DualWieldState.getActiveWeaponFor((LivingEntity) (Object) this);
        if (!override.isEmpty()) {
            cir.setReturnValue(override);
        }
    }
}
