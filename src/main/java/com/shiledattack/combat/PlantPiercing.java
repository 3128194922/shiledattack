package com.shiledattack.combat;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 穿草攻击（准星穿过草、花等植物选中后面的实体）的标签与判定。
 *
 * <p>只有手持（主手或副手）带有 {@code shiledattack:attack_through_plants}
 * 标签的物品时才会生效；该标签默认留空，由整合包通过数据包 / KubeJS 自行指定武器。</p>
 *
 * <p>植物识别以原版 {@link BushBlock} 为主（草、花、树苗、作物等），再补充
 * 甘蔗、竹子、藤蔓；其它模组的植物可以用方块标签
 * {@code shiledattack:pierceable_plants} 追加。</p>
 */
public final class PlantPiercing {

    /** 主手或副手物品带此标签时，攻击准星可以穿过植物。 */
    public static final TagKey<Item> ATTACK_THROUGH_PLANTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "attack_through_plants"));

    /** 额外可穿透的方块；原版常见植物已由代码覆盖，这里用于扩展模组植物。 */
    public static final TagKey<Block> PIERCEABLE_PLANTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "pierceable_plants"));

    private PlantPiercing() {
    }

    /** 玩家当前是否手持带标签的物品（主手优先，其次副手）。 */
    public static boolean holdsPiercingItem(Player player) {
        return isTagged(player.getMainHandItem()) || isTagged(player.getOffhandItem());
    }

    private static boolean isTagged(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ATTACK_THROUGH_PLANTS);
    }

    /** 该方块是否属于“草”一类可以穿过的植物。 */
    public static boolean isPlant(BlockState state) {
        if (state.is(PIERCEABLE_PLANTS)) {
            return true;
        }
        Block block = state.getBlock();
        return block instanceof BushBlock
                || block instanceof SugarCaneBlock
                || block instanceof BambooStalkBlock
                || block instanceof BambooSaplingBlock
                || block instanceof VineBlock;
    }
}
