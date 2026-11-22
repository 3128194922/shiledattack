package com.shiledattack.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.shiledattack.Config;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.combat.ReachProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 准星处的锥形范围指示器，样式移植自 {@code cortes-combat-rainbow}。
 *
 * <p>由两部分组成：</p>
 * <ul>
 *   <li><b>括号</b>：落在锥体水平半角投影出的屏幕边缘，直观显示攻击宽度；</li>
 *   <li><b>渐变条</b>：从准星向括号延伸，表示当前蓄力进度；攻击间隔仍由原版准星指示条显示。</li>
 * </ul>
 *
 * <p>只在主手持有带攻击力的物品且锥形攻击开启时显示，避免空手或持方块时也画。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ConeIndicatorRenderer {

    /** 指示器颜色（24 位 RGB）。 */
    private static final int RGB = 0xFFFFFF;
    /** 准星静止时的半高，用于让进度条从准星外开始。 */
    private static final int CROSSHAIR_REACH = 7;

    private ConeIndicatorRenderer() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;
        if (!Config.coneAttackEnabled) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) return;
        if (!holdsWeapon(player)) return;

        ReachProfile reach = Config.coneProfile();
        if (reach.isNone()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int centreX = graphics.guiWidth() / 2;
        int centreY = graphics.guiHeight() / 2;
        double fov = minecraft.options.fov().get();

        ConeGauge.Geometry geometry = ConeGauge.geometry(
                reach.horizontalAngle(), graphics.guiWidth(), graphics.guiHeight(),
                fov, CROSSHAIR_REACH);

        // 两侧进度条显示蓄力进度；攻击间隔仍由原版准星指示条负责。
        // 未蓄力时长度为 0，只保留括号显示锥形范围。
        float ratio = ChargeInputHandler.isCharging()
                ? Math.max(0.0F, Math.min(1.0F, ChargeInputHandler.chargeRatio()))
                : 0.0F;

        RenderSystem.enableBlend();
        drawBar(graphics, centreX, centreY, geometry, ratio, -1);
        drawBar(graphics, centreX, centreY, geometry, ratio, 1);
        drawBrackets(graphics, centreX, centreY, geometry.halfSpan());
        RenderSystem.disableBlend();
    }

    /** 主手是否是一件有攻击力的武器。 */
    private static boolean holdsWeapon(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // 玩家基础攻击力为 1.0；高于它说明手里有武器或工具。
        return attackDamage > 1.05D;
    }

    private static void drawBar(GuiGraphics graphics, int centreX, int centreY,
                                ConeGauge.Geometry geometry, float ratio, int side) {
        int length = geometry.barLength();
        if (length <= 0) return;
        int filled = Math.round(length * ratio);
        int y = centreY - ConeGauge.BAR_THICKNESS / 2;
        int originX = centreX + side * (geometry.barOuter() + 1);

        for (int i = 0; i < length; i++) {
            int alpha = ConeGauge.barAlpha(i, length, filled);
            if (alpha <= 0) continue;
            int x = originX + side * i;
            graphics.fill(x, y, x + 1, y + ConeGauge.BAR_THICKNESS, (alpha << 24) | RGB);
        }
    }

    private static void drawBrackets(GuiGraphics graphics, int centreX, int centreY, int halfSpan) {
        List<ConeBracketMask.Run> runs = ConeBracketMask.runs();
        ConeBracketMask.Anchors anchors = ConeBracketMask.anchors();
        if (runs.isEmpty() || anchors.left() == ConeBracketMask.NO_INK) return;

        int spriteHeight = ConeBracketMask.height();
        int y0 = centreY - spriteHeight / 2;
        int leftBase = centreX - halfSpan - anchors.left();
        int rightBase = centreX + halfSpan - anchors.right();
        float presence = ConeGauge.BRACKET_ALPHA;

        for (ConeBracketMask.Run run : runs) {
            int y = y0 + run.y();
            int leftX = leftBase + run.leftX();
            int rightX = rightBase + run.rightX();
            float shape = ConeBracketMask.normalised(run.alpha());
            int colour = pack(shape, presence);
            graphics.fill(leftX, y, leftX + run.width(), y + 1, colour);
            graphics.fill(rightX, y, rightX + run.width(), y + 1, colour);
        }
    }

    /** 把形状 alpha 与存在感合成 GUI 颜色。 */
    private static int pack(float normalisedAlpha, float presence) {
        int a = Math.round(normalisedAlpha * presence * 255.0F);
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | RGB;
    }
}
