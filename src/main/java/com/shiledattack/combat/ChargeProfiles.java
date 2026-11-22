package com.shiledattack.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shiledattack.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 逐武器蓄力属性的解析入口。
 *
 * <p>优先级从高到低：</p>
 * <ol>
 *   <li>数据包 JSON（{@code data/<namespace>/charge_profiles/<path>.json}）</li>
 *   <li>KubeJS 启动脚本注册</li>
 *   <li>标签档位（light / normal / heavy）</li>
 *   <li>全局配置兜底</li>
 * </ol>
 *
 * <p>标签 {@code shiledattack:charge_attack} 仍是“能不能蓄力”的总开关；
 * JSON 与脚本可以通过 {@code enabled} 显式开启或关闭某件武器。</p>
 */
public final class ChargeProfiles {

    private static final Map<ResourceLocation, ChargeProfile> JSON = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ChargeProfile> SCRIPT = new ConcurrentHashMap<>();
    /** 服务端同步下来的权威档案（客户端使用）。 */
    private static final Map<ResourceLocation, ChargeProfile> SYNCED = new ConcurrentHashMap<>();

    private ChargeProfiles() {
    }

    /** 数据包重载：整体替换 JSON 层。 */
    public static void acceptJson(Map<ResourceLocation, JsonElement> data) {
        JSON.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            JSON.put(stripDirectory(entry.getKey()), ChargeProfile.fromJson(json));
        }
    }

    /** 监听器给的 key 可能带 {@code charge_profiles/} 前缀，统一去掉。 */
    private static ResourceLocation stripDirectory(ResourceLocation id) {
        String path = id.getPath();
        String prefix = "charge_profiles/";
        if (path.startsWith(prefix)) {
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    path.substring(prefix.length()));
        }
        return id;
    }

    /** 服务端同步：整体替换同步层。 */
    public static void acceptSynced(Map<ResourceLocation, ChargeProfile> data) {
        SYNCED.clear();
        if (data != null) {
            SYNCED.putAll(data);
        }
    }

    /** 当前 JSON + 脚本层，供服务端同步给客户端。 */
    public static Map<ResourceLocation, ChargeProfile> exportConfigured() {
        Map<ResourceLocation, ChargeProfile> all = new java.util.HashMap<>(JSON);
        all.putAll(SCRIPT);
        return all;
    }

    /** KubeJS 注册一条武器档案。 */
    public static void registerScript(ResourceLocation itemId, ChargeProfile profile) {
        if (itemId == null || profile == null) {
            return;
        }
        SCRIPT.put(itemId, profile);
    }

    /** 清空脚本层（供测试或手动重置）。 */
    public static void clearScript() {
        SCRIPT.clear();
    }

    /**
     * 解析主手物品的蓄力属性。
     *
     * @return 永不为 null；不可蓄力时返回 {@link ChargeProfile#DISABLED}
     */
    public static ChargeProfile resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ChargeProfile.DISABLED;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ChargeProfile profile = JSON.get(id);
        if (profile != null) {
            return profile;
        }
        profile = SYNCED.get(id);
        if (profile != null) {
            return profile;
        }
        profile = SCRIPT.get(id);
        if (profile != null) {
            return profile;
        }
        // 标签档位只对带总标签的物品生效。
        if (stack.is(ChargeAttackTag.CHARGE_ATTACK)) {
            if (stack.is(ChargeAttackTag.HEAVY)) {
                return Config.heavyChargeProfile();
            }
            if (stack.is(ChargeAttackTag.LIGHT)) {
                return Config.lightChargeProfile();
            }
            return Config.defaultChargeProfile();
        }
        return ChargeProfile.DISABLED;
    }

    /** 该物品是否可蓄力。 */
    public static boolean isChargeable(ItemStack stack) {
        return resolve(stack).enabled();
    }
}
