package com.shiledattack.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.shiledattack.ShiledAttackMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 准星下方的长条蓄力进度条。
 *
 * <p>贴图移植自 {@code cortes-combat-rainbow}：30×15 的底槽 + 高光条，
 * 高光按蓄力比例横向裁切，满蓄力时末端的溅射细节自然出现。</p>
 *
 * <p>比例低于最小值时不绘制：一次普通点击不应该闪一下蓄力条。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ChargeHudRenderer {

    private static final ResourceLocation CHARGE =
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID,
                    "textures/gui/charge_bar.png");
    private static final ResourceLocation CHARGE_EMPTY =
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID,
                    "textures/gui/charge_bar_empty.png");

    /** 贴图原始尺寸。 */
    private static final int WIDTH = 30;
    private static final int HEIGHT = 15;
    /** 与准星之间的空隙。 */
    private static final int GAP = 9;
    /** 满蓄力时的高光色，其余时候用原色。 */
    private static final int TINT_READY = 0xFF56D8FF;

    private ChargeHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;
        if (!ChargeInputHandler.isCharging()) return;

        float ratio = ChargeInputHandler.chargeRatio();
        if (ratio <= 0.0F) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        int x = centerX - WIDTH / 2;
        int y = centerY + GAP;

        int tint = ratio >= 1.0F ? TINT_READY : 0xFFFFFFFF;
        renderBar(graphics, x, y, ratio, tint);
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, float ratio, int tint) {
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        graphics.setColor(red, green, blue, alpha);
        graphics.blit(CHARGE_EMPTY, x, y, WIDTH, HEIGHT,
                0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);

        int filled = Math.max(1, Math.round(WIDTH * Math.min(1.0F, ratio)));
        graphics.blit(CHARGE, x, y, filled, HEIGHT,
                0.0F, 0.0F, filled, HEIGHT, WIDTH, HEIGHT);

        // 颜色必须还原：GuiGraphics 是共享缓冲，残留的染色会影响后续所有绘制。
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
