package com.shiledattack.mixin;

import com.shiledattack.Config;
import com.shiledattack.dual.DualWieldState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    // --- 双持：副手追击期间让 Player#attack 用副手武器结算 ---

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack shiledattack$dualWieldMainHand(Player instance) {
        ItemStack override = DualWieldState.getActiveWeapon();
        return override.isEmpty() ? instance.getMainHandItem() : override;
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack shiledattack$dualWieldItemInHand(Player instance, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack override = DualWieldState.getActiveWeapon();
            if (!override.isEmpty()) {
                return override;
            }
        }
        return instance.getItemInHand(hand);
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void shiledattack$dualWieldSetItemInHand(Player instance, InteractionHand hand, ItemStack stack) {
        if (DualWieldState.isOffhandAttackInProgress()
                && hand == InteractionHand.MAIN_HAND
                && stack.isEmpty()) {
            // 副手武器在攻击中耐久耗尽：清空动作要落回副手槽，保住主手物品
            instance.setItemInHand(InteractionHand.OFF_HAND, stack);
            return;
        }
        instance.setItemInHand(hand, stack);
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"))
    private float shiledattack$dualWieldAttackStrength(Player instance, float partialTicks) {
        // 延迟命中时攻击冷却已经走了一部分，按原版公式会把伤害砍到 20%~30%，
        // 因此副手追击沿用主手那一击的冷却比例。
        if (DualWieldState.isOffhandAttackInProgress()) {
            return DualWieldState.getAttackStrengthScale();
        }
        return instance.getAttackStrengthScale(partialTicks);
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void shiledattack$dualWieldExhaustion(Player instance, float amount) {
        // 一次左键视为一次攻击动作：副手追击不再额外扣饱食度
        if (DualWieldState.isOffhandAttackInProgress()) {
            return;
        }
        instance.causeFoodExhaustion(amount);
    }

}
