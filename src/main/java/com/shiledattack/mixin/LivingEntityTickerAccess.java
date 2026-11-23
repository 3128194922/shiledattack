package com.shiledattack.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取/写回玩家的攻击冷却计时，用于让副手追击不额外拖慢攻击节奏。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityTickerAccess {

    @Accessor("attackStrengthTicker")
    int shiledattack$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void shiledattack$setAttackStrengthTicker(int value);
}
