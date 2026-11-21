package com.shiledattack.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 枚举锥形范围内的生物，按距离从近到远排序。
 *
 * <p>近的优先：两个目标都在锥内时，玩家真正想打的是更近的那个。
 * 过滤规则与原版横扫一致：排除自己、友方与盔甲架标记。</p>
 */
public final class ConeTargets {

    private ConeTargets() {
    }

    /**
     * 锥内所有生物，最近的在前。
     *
     * @param limit 返回上限；小于 1 表示不限制
     */
    public static List<LivingEntity> nearestInCone(Level level, LivingEntity source,
                                                   ReachProfile reach, int limit) {
        if (reach == null || reach.isNone()) {
            return List.of();
        }
        List<LivingEntity> found = new ArrayList<>();
        // 多扩一格：判定用的是目标包围盒，脚刚过线的目标仍应被扫到。
        AABB box = source.getBoundingBox().inflate(reach.distance() + 1.0);
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getLookAngle();
        ConeMath.Prepared limits = ConeMath.prepare(
                reach.distance(), reach.horizontalAngle(), reach.verticalAngle());
        for (Entity candidate : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!isValidTarget(source, candidate)) {
                continue;
            }
            if (inCone(eye, look, candidate, limits)) {
                found.add((LivingEntity) candidate);
            }
        }
        found.sort((a, b) -> Double.compare(a.distanceToSqr(source), b.distanceToSqr(source)));
        return limit > 0 && found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    /** 锥内最近的生物，没有则返回 null。 */
    public static LivingEntity nearest(Level level, LivingEntity source, ReachProfile reach) {
        List<LivingEntity> found = nearestInCone(level, source, reach, 1);
        return found.isEmpty() ? null : found.get(0);
    }

    public static boolean inCone(Vec3 origin, Vec3 look, Entity target, ConeMath.Prepared limits) {
        AABB targetBox = target.getBoundingBox();
        double x = ConeMath.closestOnAxis(origin.x, targetBox.minX, targetBox.maxX);
        double y = ConeMath.closestOnAxis(origin.y, targetBox.minY, targetBox.maxY);
        double z = ConeMath.closestOnAxis(origin.z, targetBox.minZ, targetBox.maxZ);
        return ConeMath.inCone(origin.x, origin.y, origin.z, look.x, look.y, look.z, x, y, z,
                limits,
                ConeMath.leash(halfWidth(targetBox)),
                ConeMath.leash((targetBox.maxY - targetBox.minY) * 0.5));
    }

    private static boolean isValidTarget(LivingEntity source, Entity candidate) {
        if (candidate == source || !candidate.isAlive()
                || candidate.isSpectator() || candidate.isRemoved()) {
            return false;
        }
        if (source.isAlliedTo(candidate)) {
            return false;
        }
        return !(candidate instanceof ArmorStand stand && stand.isMarker());
    }

    private static double halfWidth(AABB box) {
        return Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.5;
    }
}
