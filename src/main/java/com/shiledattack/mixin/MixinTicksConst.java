package com.shiledattack.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.shiledattack.Config;

import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public class MixinTicksConst {
    @ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
    private int shiledattack$setShieldUseDelay(int constant) {
        if (Config.shieldDelayOverride) {
            return Config.shieldRaiseDelay;
        }
        return constant;
    }
}
