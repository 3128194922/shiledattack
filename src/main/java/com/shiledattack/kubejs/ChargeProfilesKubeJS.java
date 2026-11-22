package com.shiledattack.kubejs;

import com.shiledattack.combat.ChargeProfile;
import com.shiledattack.combat.ChargeProfiles;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * KubeJS 侧注册入口：{@code ChargeProfiles.register(...)}。
 *
 * <pre>{@code
 * // startup_scripts
 * ChargeProfiles.register('cortescombat:greatsword', {
 *     min_hold: 4,
 *     full_ticks: 30,
 *     damage_bonus: 1.5
 * })
 *
 * // 也可以直接关闭某件武器的蓄力
 * ChargeProfiles.register('minecraft:stick', { enabled: false })
 * }</pre>
 *
 * <p>脚本层优先级高于标签档位、低于数据包 JSON，因此整合包作者可以用
 * 数据包覆盖脚本设置。</p>
 */
public final class ChargeProfilesKubeJS {

    public ChargeProfilesKubeJS() {
    }

    /**
     * 注册一条武器蓄力属性。
     *
     * @param itemId  物品 ID，例如 {@code "minecraft:diamond_sword"}
     * @param options 可省略；支持 {@code enabled} / {@code min_hold} / {@code full_ticks} /
     *                {@code damage_bonus}，也接受驼峰写法
     */
    public void register(String itemId, Map<?, ?> options) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return;
        }
        ChargeProfiles.registerScript(id, ChargeProfile.fromMap(options));
    }

    /** 只给物品 ID 时使用常规默认值。 */
    public void register(String itemId) {
        register(itemId, null);
    }

    /** 移除脚本层注册，回到标签 / 全局配置。 */
    public void clear() {
        ChargeProfiles.clearScript();
    }
}
