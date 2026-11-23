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

    // --- Cone attack (锥形攻击范围) ---

    public static final ForgeConfigSpec.BooleanValue CONE_ATTACK_ENABLED = BUILDER
            .comment("Enable cone-shaped melee range: one swing also hits extra targets inside the cone,",
                    "and hold-to-attack picks the nearest entity inside the cone when the crosshair is not on one")
            .define("coneAttackEnabled", true);

    public static final ForgeConfigSpec.DoubleValue CONE_REACH = BUILDER
            .comment("Cone forward reach in blocks (vanilla server rejects attacks beyond 6)")
            .defineInRange("coneReach", 3.6, 1.0, 6.0);

    public static final ForgeConfigSpec.DoubleValue CONE_HORIZONTAL_ANGLE = BUILDER
            .comment("Cone half-angle to each side, in degrees")
            .defineInRange("coneHorizontalAngle", 35.0, 1.0, 89.0);

    public static final ForgeConfigSpec.DoubleValue CONE_VERTICAL_ANGLE = BUILDER
            .comment("Cone half-angle up and down, in degrees (tighter than horizontal by design)")
            .defineInRange("coneVerticalAngle", 30.0, 1.0, 89.0);

    public static final ForgeConfigSpec.IntValue CONE_MAX_TARGETS = BUILDER
            .comment("Maximum bodies one swing can hit, including the aimed one")
            .defineInRange("coneMaxTargets", 3, 1, 10);

    public static final ForgeConfigSpec.DoubleValue CONE_DAMAGE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for the extra cone targets (1.0 = same as the aimed target)")
            .defineInRange("coneDamageMultiplier", 0.6, 0.0, 1.0);

    // --- Attack camera shake (攻击屏幕抖动) ---

    public static final ForgeConfigSpec.BooleanValue ATTACK_SHAKE_ENABLED = BUILDER
            .comment("Shake the attacker's camera when an attack lands")
            .define("attackShakeEnabled", true);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SHAKE_DEGREES = BUILDER
            .comment("Camera shake amplitude in degrees at point blank")
            .defineInRange("attackShakeDegrees", 1.6, 0.0, 10.0);

    public static final ForgeConfigSpec.IntValue ATTACK_SHAKE_TICKS = BUILDER
            .comment("Camera shake duration in ticks")
            .defineInRange("attackShakeTicks", 5, 1, 40);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SHAKE_REACH = BUILDER
            .comment("Camera shake reach in blocks; amplitude falls off with the square of the distance")
            .defineInRange("attackShakeReach", 16.0, 1.0, 128.0);

    public static final ForgeConfigSpec.DoubleValue CHARGE_SHAKE_MULTIPLIER = BUILDER
            .comment("Camera shake multiplier for charged attacks at full charge (1.0 = same as a normal hit)")
            .defineInRange("chargeShakeMultiplier", 1.8, 1.0, 6.0);

    // --- Charged attack (蓄力攻击) ---

    public static final ForgeConfigSpec.BooleanValue CHARGE_ATTACK_ENABLED = BUILDER
            .comment("Enable charged attacks for items tagged shiledattack:charge_attack:",
                    "hold the left button to charge, release to strike")
            .define("chargeAttackEnabled", true);

    public static final ForgeConfigSpec.IntValue CHARGE_MIN_HOLD_TICKS = BUILDER
            .comment("Ticks the button must be held before the swing counts as charged")
            .defineInRange("chargeMinHoldTicks", 4, 1, 20);

    public static final ForgeConfigSpec.IntValue CHARGE_FULL_TICKS = BUILDER
            .comment("Ticks of holding needed for a full charge")
            .defineInRange("chargeFullTicks", 20, 5, 200);

    public static final ForgeConfigSpec.DoubleValue CHARGE_DAMAGE_BONUS = BUILDER
            .comment("Extra damage multiplier at full charge (1.0 = double damage, 0.0 = no bonus)")
            .defineInRange("chargeDamageBonus", 1.0, 0.0, 10.0);

    public static final ForgeConfigSpec.IntValue CHARGE_LIGHT_FULL_TICKS = BUILDER
            .comment("Light tier (shiledattack:charge_attack/light): ticks needed for a full charge")
            .defineInRange("chargeLightFullTicks", 12, 2, 400);

    public static final ForgeConfigSpec.DoubleValue CHARGE_LIGHT_BONUS = BUILDER
            .comment("Light tier: extra damage multiplier at full charge")
            .defineInRange("chargeLightBonus", 0.5, 0.0, 10.0);

    public static final ForgeConfigSpec.IntValue CHARGE_HEAVY_FULL_TICKS = BUILDER
            .comment("Heavy tier (shiledattack:charge_attack/heavy): ticks needed for a full charge")
            .defineInRange("chargeHeavyFullTicks", 32, 2, 400);

    public static final ForgeConfigSpec.DoubleValue CHARGE_HEAVY_BONUS = BUILDER
            .comment("Heavy tier: extra damage multiplier at full charge")
            .defineInRange("chargeHeavyBonus", 1.8, 0.0, 10.0);

    public static final ForgeConfigSpec.BooleanValue CHARGE_CRITICAL_ENABLED = BUILDER
            .comment("Allow charged attacks to land a falling critical (jump attack) like vanilla",
                    "Requires: falling, not flying/riding/swimming/levitating, target is a living entity")
            .define("chargeCriticalEnabled", true);

    public static final ForgeConfigSpec.DoubleValue CHARGE_CRITICAL_MULTIPLIER = BUILDER
            .comment("Damage multiplier applied on a charged falling critical")
            .defineInRange("chargeCriticalMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.BooleanValue SPRINT_ATTACKS = BUILDER
            .comment("Attacking will no longer stop the player from sprinting or swimming",
                    "Ported from Combat Nouveau's sprint attacks feature")
            .define("sprintAttacks", true);

    // --- Dual wield (双持武器) ---

    public static final ForgeConfigSpec.BooleanValue DUAL_WIELD_ENABLED = BUILDER
            .comment("Enable dual wield follow-up: when the main hand item is tagged",
                    "shiledattack:dual_wield, one attack also strikes with the offhand weapon")
            .define("dualWieldEnabled", true);

    public static final ForgeConfigSpec.IntValue DUAL_WIELD_OFFHAND_DELAY_TICKS = BUILDER
            .comment("Ticks between the main hand hit and the offhand follow-up hit",
                    "Vanilla animates only one arm at a time, so the offhand swing starts",
                    "when the main hand swing finishes: 6 keeps both animations complete.",
                    "Lower values cut the main hand animation short; 0 lands both in one tick")
            .defineInRange("dualWieldOffhandDelayTicks", 6, 0, 20);

    // --- Attack through plants (穿草攻击) ---

    public static final ForgeConfigSpec.BooleanValue PIERCE_PLANTS_ENABLED = BUILDER
            .comment("Let the crosshair target entities behind grass and similar plants",
                    "when the held item is tagged shiledattack:attack_through_plants",
                    "Only entity targeting changes; block breaking still works as vanilla")
            .define("piercePlantsEnabled", true);

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
    public static boolean coneAttackEnabled;
    public static double coneReach;
    public static double coneHorizontalAngle;
    public static double coneVerticalAngle;
    public static int coneMaxTargets;
    public static double coneDamageMultiplier;
    public static boolean attackShakeEnabled;
    public static double attackShakeDegrees;
    public static int attackShakeTicks;
    public static double attackShakeReach;
    public static double chargeShakeMultiplier;
    public static boolean chargeAttackEnabled;
    public static int chargeMinHoldTicks;
    public static int chargeFullTicks;
    public static double chargeDamageBonus;
    public static int chargeLightFullTicks;
    public static double chargeLightBonus;
    public static int chargeHeavyFullTicks;
    public static double chargeHeavyBonus;
    public static boolean chargeCriticalEnabled;
    public static double chargeCriticalMultiplier;
    public static boolean sprintAttacks;
    public static boolean dualWieldEnabled;
    public static int dualWieldOffhandDelayTicks;
    public static boolean piercePlantsEnabled;

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

        coneAttackEnabled = CONE_ATTACK_ENABLED.get();
        coneReach = CONE_REACH.get();
        coneHorizontalAngle = CONE_HORIZONTAL_ANGLE.get();
        coneVerticalAngle = CONE_VERTICAL_ANGLE.get();
        coneMaxTargets = CONE_MAX_TARGETS.get();
        coneDamageMultiplier = CONE_DAMAGE_MULTIPLIER.get();

        attackShakeEnabled = ATTACK_SHAKE_ENABLED.get();
        attackShakeDegrees = ATTACK_SHAKE_DEGREES.get();
        attackShakeTicks = ATTACK_SHAKE_TICKS.get();
        attackShakeReach = ATTACK_SHAKE_REACH.get();
        chargeShakeMultiplier = CHARGE_SHAKE_MULTIPLIER.get();

        chargeAttackEnabled = CHARGE_ATTACK_ENABLED.get();
        chargeMinHoldTicks = CHARGE_MIN_HOLD_TICKS.get();
        chargeFullTicks = CHARGE_FULL_TICKS.get();
        chargeDamageBonus = CHARGE_DAMAGE_BONUS.get();
        chargeLightFullTicks = CHARGE_LIGHT_FULL_TICKS.get();
        chargeLightBonus = CHARGE_LIGHT_BONUS.get();
        chargeHeavyFullTicks = CHARGE_HEAVY_FULL_TICKS.get();
        chargeHeavyBonus = CHARGE_HEAVY_BONUS.get();
        chargeCriticalEnabled = CHARGE_CRITICAL_ENABLED.get();
        chargeCriticalMultiplier = CHARGE_CRITICAL_MULTIPLIER.get();

        sprintAttacks = SPRINT_ATTACKS.get();

        dualWieldEnabled = DUAL_WIELD_ENABLED.get();
        dualWieldOffhandDelayTicks = DUAL_WIELD_OFFHAND_DELAY_TICKS.get();

        piercePlantsEnabled = PIERCE_PLANTS_ENABLED.get();
    }

    /** 当前配置对应的锥形范围。 */
    public static com.shiledattack.combat.ReachProfile coneProfile() {
        if (!coneAttackEnabled) {
            return com.shiledattack.combat.ReachProfile.NONE;
        }
        return new com.shiledattack.combat.ReachProfile(
                (float) coneReach, (float) coneHorizontalAngle, (float) coneVerticalAngle);
    }

    /** 无标签档位时的默认蓄力属性（全局配置）。 */
    public static com.shiledattack.combat.ChargeProfile defaultChargeProfile() {
        if (!chargeAttackEnabled) {
            return com.shiledattack.combat.ChargeProfile.DISABLED;
        }
        return com.shiledattack.combat.ChargeProfile.of(true,
                chargeMinHoldTicks, chargeFullTicks, (float) chargeDamageBonus);
    }

    /** 轻档蓄力属性。 */
    public static com.shiledattack.combat.ChargeProfile lightChargeProfile() {
        if (!chargeAttackEnabled) {
            return com.shiledattack.combat.ChargeProfile.DISABLED;
        }
        return com.shiledattack.combat.ChargeProfile.of(true,
                chargeMinHoldTicks, chargeLightFullTicks, (float) chargeLightBonus);
    }

    /** 重档蓄力属性。 */
    public static com.shiledattack.combat.ChargeProfile heavyChargeProfile() {
        if (!chargeAttackEnabled) {
            return com.shiledattack.combat.ChargeProfile.DISABLED;
        }
        return com.shiledattack.combat.ChargeProfile.of(true,
                chargeMinHoldTicks, chargeHeavyFullTicks, (float) chargeHeavyBonus);
    }
}
