package com.shiledattack.mixin.client;

import com.shiledattack.client.handler.PlantPiercingPick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 穿草攻击：在原版准星选取结束后，允许带标签的武器选中植物后面的实体。
 *
 * <p>只在客户端生效。原版服务端只校验攻击距离，不做视线遮挡检查，
 * 因此客户端选中实体后即可正常发包攻击。</p>
 */
@Mixin(GameRenderer.class)
public abstract class PlantPiercingPickMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void shiledattack$pickThroughPlants(float partialTicks, CallbackInfo callback) {
        PlantPiercingPick.apply(this.minecraft, partialTicks);
    }
}
