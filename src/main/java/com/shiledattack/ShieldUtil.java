package com.shiledattack;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ShieldUtil {
    public static final TagKey<Item> SHIELD_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "tools/shields"));

    public static boolean isShield(ItemStack stack) {
        return stack.is(SHIELD_TAG);
    }

    private ShieldUtil() {
    }
}