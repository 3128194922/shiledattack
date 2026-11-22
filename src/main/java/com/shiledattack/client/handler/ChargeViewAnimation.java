package com.shiledattack.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shiledattack.ShiledAttackMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/**
 * 蓄力时的手持物品动画：向后上方抬起，并随蓄力增强轻微颤抖。
 *
 * <p>注入点在 {@code renderArmWithItem} 调用 {@code applyItemArmAttackTransform} 之前，
 * 也就是物品已经被放到手部位置、但原版挥击弧线尚未应用的帧。
 * 在这里旋转是绕握把转动武器，而不是让整条手臂绕相机摆动。</p>
 *
 * <p>只处理主手：蓄力状态描述的是主手武器，若同时作用于副手会让盾牌跟着动。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ChargeViewAnimation {

    /** 抬手幅度（度）。 */
    private static final float PITCH_DEGREES = 12.0F;
    /** 向后拉的距离（物品单位）。 */
    private static final double BACK_Z = -0.16;
    private static final double LIFT_Y = 0.18;
    /** 颤抖频率与幅度。 */
    private static final float TREMBLE_HZ = 3.4F;
    private static final float TREMBLE_DEGREES = 1.1F;
    private static final double TREMBLE_Z = 0.010;

    private static float partialTick;
    /** 当前渲染帧序号，用于防止同帧重复应用。 */
    private static long frameId;
    private static long appliedFrame = -1L;

    private ChargeViewAnimation() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            partialTick = event.renderTickTime;
            frameId++;
        }
    }

    /**
     * 应用蓄力姿态。
     *
     * @param pose 当前手部帧
     * @param arm  正在绘制的手臂
     */
    public static void apply(PoseStack pose, HumanoidArm arm) {
        if (!ChargeInputHandler.isCharging()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || arm != player.getMainArm()) {
            return;
        }
        // 1.20.1 的 renderArmWithItem 有多条路径都会调用攻击变换，
        // 同帧重复应用会让姿态叠加两遍，这里只认每帧第一次。
        if (appliedFrame == frameId) {
            return;
        }
        float ratio = ChargeInputHandler.chargeRatio();
        if (ratio <= 0.0F) {
            return;
        }
        appliedFrame = frameId;
        // 蓄力比例做一次缓入，让刚按下时动作轻、接近满蓄力时明显。
        float eased = ratio * ratio * (3.0F - 2.0F * ratio);

        pose.translate(0.0, LIFT_Y * eased, BACK_Z * eased);
        pose.mulPose(Axis.XP.rotationDegrees(-PITCH_DEGREES * eased));

        // 颤抖：两条不同频率的正弦叠加，避免看出固定周期。
        float time = player.tickCount + partialTick;
        float phase = time * TREMBLE_HZ;
        float wobble = Mth.sin(phase) * 0.7F + Mth.sin(phase * 2.37F) * 0.3F;
        float energy = TREMBLE_DEGREES * eased * wobble;
        pose.translate(0.0, 0.0, TREMBLE_Z * eased * wobble);
        pose.mulPose(Axis.ZP.rotationDegrees(energy));
        pose.mulPose(Axis.YP.rotationDegrees(energy * 0.6F));
    }
}
