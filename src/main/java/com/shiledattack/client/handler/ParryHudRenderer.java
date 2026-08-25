package com.shiledattack.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.shiledattack.Config;
import com.shiledattack.ShieldUtil;
import com.shiledattack.ShiledAttackMod;
import com.shiledattack.network.ClientParryState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws a shield icon to the left of the player's offhand slot whenever the
 * shield parry is ready. The icon disappears while the parry is on cooldown.
 * It is anchored to the offhand slot position and only shows when a shield is
 * held in the offhand.
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, value = Dist.CLIENT)
public class ParryHudRenderer {
    private static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "textures/gui/parry_shield.png");
    private static final int ICON_SIZE = 16;
    private static final int GAP = 4;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int before = ClientParryState.getRemaining();
        ClientParryState.tick();
        // Play the vanilla button sound the moment the parry cooldown finishes.
        if (before > 0 && ClientParryState.isReady() && Config.parryEnabled) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(),
                        (float) Config.parrySoundVolume, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!Config.parryEnabled || !ClientParryState.isReady()) return;

        ItemStack offhand = mc.player.getOffhandItem();
        if (!ShieldUtil.isShield(offhand)) return;

        int cx = event.getWindow().getGuiScaledWidth() / 2;
        int slotLeft;
        if (mc.player.getMainArm().getOpposite() == HumanoidArm.RIGHT) {
            slotLeft = cx + 91;
        } else {
            slotLeft = cx - 91 - 29;
        }

        int x = slotLeft - ICON_SIZE - GAP;
        int y = event.getWindow().getGuiScaledHeight() - 23 + (24 - ICON_SIZE) / 2;

        RenderSystem.enableBlend();
        event.getGuiGraphics().blit(ICON, x, y, 0, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }
}