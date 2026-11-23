package com.shiledattack.dual;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 双持武器的标签与判定。
 *
 * <p>只有主手物品带有 {@code shiledattack:dual_wield} 标签、且副手物品确实提供
 * 攻击力时才会触发副手追击；空手、火把、盾牌等不提供攻击力的副手物品不会触发。</p>
 */
public final class DualWield {

    /** 主手物品带此标签时，一次左键攻击会追加一次副手攻击。 */
    public static final TagKey<Item> DUAL_WIELD = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ShiledAttackMod.MODID, "dual_wield"));

    private DualWield() {
    }

    public static boolean isDualWieldMain(ItemStack stack) {
        return !stack.isEmpty() && stack.is(DUAL_WIELD);
    }

    /**
     * 物品作为主手武器时是否提供攻击力。
     *
     * <p>原版与绝大多数模组武器都用 {@link AttributeModifier.Operation#ADDITION}
     * 给出基础攻击力，少数用乘法修饰符。这里不区分运算方式，只判断是否存在
     * 非零的攻击力修饰符：盾牌、火把、食物等没有该修饰符，因此不会被当成武器。</p>
     */
    public static boolean providesAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getAmount() != 0.0D) {
                return true;
            }
        }
        return false;
    }

    /** 副手物品是否算作一把可以挥击的武器。 */
    public static boolean isOffhandWeapon(ItemStack stack) {
        return providesAttackDamage(stack);
    }

    /** 当前主副手组合是否满足双持追击条件。 */
    public static boolean canDualWield(Player player) {
        return isDualWieldMain(player.getMainHandItem())
                && isOffhandWeapon(player.getOffhandItem());
    }
}
