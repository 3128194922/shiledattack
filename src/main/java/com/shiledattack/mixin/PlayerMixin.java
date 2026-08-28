package com.shiledattack.mixin;

import com.shiledattack.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 移植自 Combat Nouveau 的 sprint attacks 特性：
 * 攻击不再打断玩家的疾跑/游泳状态。
 * 原理：攻击前记录 sprint 状态，原版 attack() 中应用疾跑击退加成后
 * 会调用 setSprinting(false)，在此时恢复 sprint 状态即可。
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Unique
    private boolean shiledattack$sprintsDuringAttack;

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void shiledattack$recordSprint(Entity target, CallbackInfo callback) {
        this.shiledattack$sprintsDuringAttack = this.isSprinting();
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", shift = At.Shift.AFTER))
    private void shiledattack$restoreSprint(Entity target, CallbackInfo callback) {
        // 原版攻击命中后会停止疾跑，这里恢复它，使水下攻击也不会中断游泳
        if (Config.sprintAttacks && this.shiledattack$sprintsDuringAttack) {
            this.setSprinting(true);
        }
        this.shiledattack$sprintsDuringAttack = false;
    }
}
