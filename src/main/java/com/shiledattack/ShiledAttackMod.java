package com.shiledattack;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ShiledAttackMod.MODID)
public class ShiledAttackMod {
    public static final String MODID = "shiledattack";

    public ShiledAttackMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
