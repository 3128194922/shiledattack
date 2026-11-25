package com.shiledattack.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端蓄力预测状态（仅用于手感与 HUD，服务端计时才是权威）。
 */
public final class ClientChargeState {

    private static boolean holding;
    private static int heldTicks;
    private static ItemStack chargingItem = ItemStack.EMPTY;

    private ClientChargeState() {
    }

    public static void begin(ItemStack item) {
        holding = true;
        heldTicks = 0;
        chargingItem = item.copy();
    }

    public static void tick() {
        if (holding) {
            heldTicks++;
        }
    }

    public static void end() {
        holding = false;
        heldTicks = 0;
        chargingItem = ItemStack.EMPTY;
    }

    public static boolean isHolding() {
        return holding;
    }

    public static int heldTicks() {
        return heldTicks;
    }

    public static ItemStack chargingItem() {
        return chargingItem;
    }

    /** 当前蓄力比例，供 HUD 与调试使用（按开始蓄力时那件武器的档案计算）。 */
    public static float ratio() {
        if (!holding) {
            return 0.0F;
        }
        double chargeSpeed = Minecraft.getInstance().player == null
                ? ChargeSpeed.DEFAULT_VALUE
                : ChargeSpeed.of(Minecraft.getInstance().player);
        return ChargeProfiles.resolve(chargingItem).ratio(heldTicks, chargeSpeed);
    }

    /** 主手物品是否已不是开始蓄力的那件，用于中途换武器时放弃蓄力。 */
    public static boolean itemChanged() {
        if (!holding) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return true;
        }
        ItemStack current = minecraft.player.getMainHandItem();
        return !ItemStack.isSameItemSameTags(current, chargingItem);
    }
}
