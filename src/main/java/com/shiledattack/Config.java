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

    public static final ForgeConfigSpec.BooleanValue SHIELD_DELAY_OVERRIDE = BUILDER
            .comment("Override the vanilla hardcoded 5-tick shield raise delay")
            .define("shieldDelayOverride", true);

    public static final ForgeConfigSpec.IntValue SHIELD_RAISE_DELAY = BUILDER
            .comment("Shield raise delay in ticks (0 = instant block, 5 = vanilla)")
            .defineInRange("shieldRaiseDelay", 0, 0, 5);

    // --- Shield Parry (盾反) ---

    public static final ForgeConfigSpec.BooleanValue PARRY_ENABLED = BUILDER
            .comment("Enable shield parry: a block landed within the parry window after raising the shield")
            .define("parryEnabled", true);

    public static final ForgeConfigSpec.IntValue PARRY_WINDOW = BUILDER
            .comment("Parry window in ticks after raising the shield where a parry can trigger (10 = half second)")
            .defineInRange("parryWindowTicks", 10, 1, 40);

    public static final ForgeConfigSpec.DoubleValue PARRY_COOLDOWN_SECONDS = BUILDER
            .comment("Shield parry cooldown in seconds (3 = 3 seconds). Custom cooldown, not tied to item cooldowns.")
            .defineInRange("parryCooldownSeconds", 3.0, 0.05, 60.0);

    public static final ForgeConfigSpec.DoubleValue PARRY_KNOCKBACK_MUL = BUILDER
            .comment("Parry knockback multiplier (damage / collision volume * this)")
            .defineInRange("parryKnockbackMul", 1.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue PARRY_KNOCKBACK_MIN = BUILDER
            .comment("Minimum horizontal knockback velocity applied to the attacker")
            .defineInRange("parryKnockbackMin", 0.4, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue PARRY_KNOCKBACK_MAX = BUILDER
            .comment("Maximum horizontal knockback velocity applied to the attacker")
            .defineInRange("parryKnockbackMax", 4.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue PARRY_KNOCK_UP = BUILDER
            .comment("Vertical impulse applied to the attacker on a parry")
            .defineInRange("parryKnockUp", 0.35, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue PARRY_SOUND_VOLUME = BUILDER
            .comment("Anvil sound volume played on a successful parry")
            .defineInRange("parrySoundVolume", 1.0, 0.0, 5.0);

    public static final ForgeConfigSpec.BooleanValue SPRINT_ATTACKS = BUILDER
            .comment("Attacking will no longer stop the player from sprinting or swimming",
                    "Ported from Combat Nouveau's sprint attacks feature")
            .define("sprintAttacks", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static boolean holdAttackButton;
    public static boolean sneakAutoShield;
    public static boolean shieldDelayOverride;
    public static int shieldRaiseDelay;

    public static boolean parryEnabled;
    public static int parryWindowTicks;
    public static int parryCooldownTicks;
    public static double parryKnockbackMul;
    public static double parryKnockbackMin;
    public static double parryKnockbackMax;
    public static double parryKnockUp;
    public static double parrySoundVolume;
    public static boolean sprintAttacks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        holdAttackButton = HOLD_ATTACK_BUTTON.get();
        sneakAutoShield = SNEAK_AUTO_SHIELD.get();
        shieldDelayOverride = SHIELD_DELAY_OVERRIDE.get();
        shieldRaiseDelay = SHIELD_RAISE_DELAY.get();

        parryEnabled = PARRY_ENABLED.get();
        parryWindowTicks = PARRY_WINDOW.get();
        parryCooldownTicks = Math.max(1, (int) Math.round(PARRY_COOLDOWN_SECONDS.get() * 20.0));
        parryKnockbackMul = PARRY_KNOCKBACK_MUL.get();
        parryKnockbackMin = PARRY_KNOCKBACK_MIN.get();
        parryKnockbackMax = PARRY_KNOCKBACK_MAX.get();
        parryKnockUp = PARRY_KNOCK_UP.get();
        parrySoundVolume = PARRY_SOUND_VOLUME.get();
        sprintAttacks = SPRINT_ATTACKS.get();
    }
}
