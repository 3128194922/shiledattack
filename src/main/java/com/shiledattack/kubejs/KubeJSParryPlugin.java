package com.shiledattack.kubejs;

import com.shiledattack.event.ShieldParriedEvent;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraftforge.common.MinecraftForge;

/**
 * KubeJS plugin hooking the mod's parry system into KubeJS.
 *
 * Registered through {@code kubejs.plugins.txt}. This class is only loaded by
 * KubeJS itself when the mod is present, so the core mod never depends on
 * KubeJS at runtime — absent KubeJS, this plugin is simply never loaded.
 */
public class KubeJSParryPlugin extends KubeJSPlugin {
    private static boolean forgeListenerRegistered;

    public KubeJSParryPlugin() {
        registerForgeListener();
    }

    @Override
    public void registerEvents() {
        ParryEvents.GROUP.register();
    }

    private static synchronized void registerForgeListener() {
        if (forgeListenerRegistered) return;
        MinecraftForge.EVENT_BUS.addListener(KubeJSParryPlugin::onShieldParried);
        forgeListenerRegistered = true;
    }

    private static void onShieldParried(ShieldParriedEvent event) {
        var player = event.getPlayer();
        if (player == null || player.getServer() == null) return;
        ParryEvents.PARRY.post(ScriptType.SERVER, new ShieldParriedEventJS(
                player.getServer(), player, event.getAttacker(),
                event.getDamageSource(), event.getBlockedDamage()));
    }
}