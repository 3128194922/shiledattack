package com.shiledattack.client.handler;

import com.shiledattack.Config;
import com.shiledattack.combat.PlantPiercing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 穿草攻击的客户端准星重算。
 *
 * <p>原版 {@code GameRenderer#pick} 用 {@link ClipContext.Block#OUTLINE} 做方块射线，
 * 草、花这类有轮廓但没有碰撞的植物会挡住准星，导致后面的生物无法被选中。
 * 这里在准星命中植物时，从命中点继续向后做方块射线（跳过所有植物），
 * 再用第一面非植物方块的距离限制实体搜索，最后把准星改到后面的实体上。</p>
 *
 * <p>只改实体选中，不改方块破坏：准星后面没有实体时仍然正常挖掘植物。</p>
 */
public final class PlantPiercingPick {

    /** 每次跳过植物时沿视线前进的距离（格）。 */
    private static final double STEP = 1.0E-3D;
    /** 最多连续跳过的植物数量，防止异常数据导致死循环。 */
    private static final int MAX_PLANTS = 32;

    private PlantPiercingPick() {
    }

    /** 在 {@code GameRenderer#pick} 结束后调用。 */
    public static void apply(Minecraft mc, float partialTicks) {
        if (!Config.piercePlantsEnabled || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (!PlantPiercing.holdsPiercingItem(mc.player)) {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult blockHit)
                || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!PlantPiercing.isPlant(mc.level.getBlockState(blockHit.getBlockPos()))) {
            return;
        }

        EntityHitResult entityHit = findEntity(mc.player, blockHit, partialTicks);
        if (entityHit != null) {
            mc.hitResult = entityHit;
            mc.crosshairPickEntity = entityHit.getEntity();
        }
    }

    private static EntityHitResult findEntity(LocalPlayer player, BlockHitResult plantHit,
                                              float partialTicks) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(partialTicks);
        Vec3 look = player.getViewVector(partialTicks);
        double entityReach = player.getEntityReach();
        if (entityReach <= 0.0D) {
            return null;
        }
        double scan = Math.max(player.getBlockReach(), entityReach);

        Vec3 end = eye.add(look.scale(scan));
        double limitSqr = entityReach * entityReach;

        // 从已经命中的植物后面继续射线，直到遇到第一面非植物方块。
        Vec3 from = plantHit.getLocation().add(look.scale(STEP));
        for (int i = 0; i < MAX_PLANTS; i++) {
            BlockHitResult hit = level.clip(new ClipContext(from, end,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                break;
            }
            if (!PlantPiercing.isPlant(level.getBlockState(hit.getBlockPos()))) {
                limitSqr = Math.min(limitSqr, hit.getLocation().distanceToSqr(eye));
                break;
            }
            Vec3 next = hit.getLocation().add(look.scale(STEP));
            if (next.distanceToSqr(eye) <= from.distanceToSqr(eye)) {
                break;
            }
            from = next;
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(scan)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> !entity.isSpectator() && entity.isPickable(), limitSqr);
    }
}
