package com.shiledattack.client.handler;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 准星下方的蓄力条。
 *
 * <p>比例低于最小值时不绘制：一次普通点击不应该闪一下蓄力条。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public final class ChargeHudRenderer {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 4;
    private static final int GAP = 12;

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

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0x90000000);
        int filled = Math.max(1, Math.round(WIDTH * ratio));
        int color = ratio >= 1.0F ? 0xFF56D8FF : 0xFF9BE3FF;
        graphics.fill(x, y, x + filled, y + HEIGHT, color);
    }
}
