package com.shiledattack;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Data-driven damage_type tags used by this mod.
 *
 * Unlike a config list, these tags live in the datapack
 * ({@code data}/shiledattack/tags/damage_type/...) so they can be edited or
 * appended by other datapacks / KubeJS at runtime and are checked by reading
 * the source's damage type tags directly via {@code DamageSource.is(tag)}.
 */
public final class ShiledAttackTags {
    private ShiledAttackTags() {
    }

    /** Damage types carrying this tag can never be parried. */
    public static final TagKey<DamageType> UNPARRYABLE = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "unparryable"));
}