package com.shiledattack.mixin.client;

import com.shiledattack.client.handler.ChargeInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蓄力期间接管原版攻击入口。
 *
 * <p>原版 {@code startAttack} 在按下瞬间就会打一下，{@code continueAttack} 在按住期间
 * 每 tick 推进挖掘进度。蓄力要的是“按住蓄力、松开才攻击”，所以两者都必须让路：</p>
 * <ul>
 *   <li>按下时由 {@link ChargeInputHandler} 判定是否接管，接管则取消原版攻击；</li>
 *   <li>蓄力期间取消 {@code continueAttack}，防止看向方块时一边蓄力一边挖。</li>
 * </ul>
 *
 * <p>只有带 {@code shiledattack:charge_attack} 标签的物品会被接管，
 * 其余物品的挖掘与攻击完全走原版。</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftChargeMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void shiledattack$claimChargedPress(CallbackInfoReturnable<Boolean> cir) {
        if (ChargeInputHandler.consumeVanillaPress()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void shiledattack$noMiningWhileCharging(boolean swingStart, CallbackInfo ci) {
        if (ChargeInputHandler.shouldBlockVanillaAttack()) {
            ci.cancel();
        }
    }
}
