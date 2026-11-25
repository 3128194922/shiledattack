package com.shiledattack.combat;

import com.shiledattack.ShiledAttackMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 玩家属性 {@code shiledattack:charge_speed}：蓄力速度加成。
 *
 * <p>取值 0..1，默认 0。实际蓄力耗时按 {@code 1 - value} 缩放：
 * 0 保持武器原始档位，0.5 时耗时减半，1.0 时降到最低 1 tick。</p>
 */
@Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ChargeSpeed {

    public static final String ATTRIBUTE_NAME = "charge_speed";
    public static final String TRANSLATION_KEY = "attribute.shiledattack.charge_speed";
    public static final double DEFAULT_VALUE = 0.0D;
    public static final double MAX_VALUE = 1.0D;

    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ShiledAttackMod.MODID);

    public static final RegistryObject<Attribute> CHARGE_SPEED = ATTRIBUTES.register(
            ATTRIBUTE_NAME,
            () -> new RangedAttribute(TRANSLATION_KEY, DEFAULT_VALUE, 0.0D, MAX_VALUE)
                    .setSyncable(true));

    private ChargeSpeed() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    /** 新玩家默认自带该属性；缺省值为 0，不改变原有蓄力手感。 */
    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, CHARGE_SPEED.get())) {
            event.add(EntityType.PLAYER, CHARGE_SPEED.get());
        }
    }

    /** 读取实体当前属性值；属性不存在时按默认值处理。 */
    public static double of(LivingEntity entity) {
        if (entity == null) {
            return DEFAULT_VALUE;
        }
        AttributeInstance instance = entity.getAttribute(CHARGE_SPEED.get());
        if (instance == null) {
            return DEFAULT_VALUE;
        }
        return clamp(instance.getValue());
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(MAX_VALUE, value));
    }
}
