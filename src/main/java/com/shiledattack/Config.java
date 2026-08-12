package com.shiledattack;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable shield auto-release on attack")
            .define("enabled", true);

    public static final ForgeConfigSpec.BooleanValue HOLD_ATTACK_BUTTON = BUILDER
            .comment("Holding down the attack button keeps attacking continuously")
            .define("holdAttackButton", true);

    public static final ForgeConfigSpec.BooleanValue SNEAK_AUTO_SHIELD = BUILDER
            .comment("Automatically raise shield when sneaking, allowing right-click on main hand items")
            .define("sneakAutoShield", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static boolean holdAttackButton;
    public static boolean sneakAutoShield;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        holdAttackButton = HOLD_ATTACK_BUTTON.get();
        sneakAutoShield = SNEAK_AUTO_SHIELD.get();
    }
}
