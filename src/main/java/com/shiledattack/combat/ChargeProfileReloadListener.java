package com.shiledattack.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 数据包监听：读取 {@code charge_profiles/<item>.json}。
 *
 * <p>服务端与客户端各注册一次，保证单机与联机下 HUD 比例一致。</p>
 */
public class ChargeProfileReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ChargeProfileReloadListener() {
        super(GSON, "charge_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        ChargeProfiles.acceptJson(data);
    }
}
