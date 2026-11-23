package com.shiledattack;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 带指定 tag 的物品不允许放进副手栏位。 */
public final class OffhandRestrictions {
    public static final TagKey<Item> CANNOT_EQUIP_OFFHAND = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "cannot_equip_offhand"));

    private OffhandRestrictions() {
    }

    public static boolean isBlocked(ItemStack stack) {
        return !stack.isEmpty() && stack.is(CANNOT_EQUIP_OFFHAND);
    }
}
