package com.shiledattack.combat;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 蓄力攻击物品标签。
 *
 * <p>只有主手物品带有 {@code shiledattack:charge_attack} 时，长按左键才会进入蓄力，
 * 松开时结算攻击。标签默认留空，由整合包 / 数据包 / KubeJS 自行添加武器。</p>
 */
public final class ChargeAttackTag {

    public static final TagKey<Item> CHARGE_ATTACK = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "charge_attack"));

    /** 轻档：蓄力快、加成低。 */
    public static final TagKey<Item> LIGHT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "charge_attack/light"));

    /** 重档：蓄力慢、加成高。 */
    public static final TagKey<Item> HEAVY = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "charge_attack/heavy"));

    private ChargeAttackTag() {
    }
}
