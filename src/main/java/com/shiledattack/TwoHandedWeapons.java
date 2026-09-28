package com.shiledattack;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Tag-based two-handed weapon rules shared by gameplay and client rendering. */
public final class TwoHandedWeapons {
    public static final TagKey<Item> IS_HANDS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "is_hands"));
    public static final TagKey<Item> CAN_HANDS_USE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "can_hands_use"));

    private TwoHandedWeapons() {
    }

    public static boolean shouldBlockOffhandUse(Player player, ItemStack offhand) {
        if (offhand.is(CAN_HANDS_USE)) return false;
        return offhand.is(IS_HANDS) || player.getMainHandItem().is(IS_HANDS);
    }

    public static boolean shouldSuppressOffhandRender(Player player) {
        ItemStack offhand = player.getOffhandItem();
        return !offhand.is(CAN_HANDS_USE) && player.getMainHandItem().is(IS_HANDS);
    }
}
