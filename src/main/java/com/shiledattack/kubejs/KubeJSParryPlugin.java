package com.shiledattack.kubejs;

import com.shiledattack.event.ShieldParriedEvent;
import com.shiledattack.combat.ChargeAttackEvent;
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
        ChargeAttackEvents.GROUP.register();
    }

    @Override
    public void registerBindings(dev.latvian.mods.kubejs.script.BindingsEvent event) {
        event.add("ChargeProfiles", new ChargeProfilesKubeJS());
    }

    private static synchronized void registerForgeListener() {
        if (forgeListenerRegistered) return;
        MinecraftForge.EVENT_BUS.addListener(KubeJSParryPlugin::onShieldParried);
        MinecraftForge.EVENT_BUS.addListener(KubeJSParryPlugin::onChargeAttack);
        forgeListenerRegistered = true;
    }

    private static void onChargeAttack(ChargeAttackEvent event) {
        var player = event.getPlayer();
        if (player == null || player.getServer() == null) return;
        var result = ChargeAttackEvents.ATTACKED.post(ScriptType.SERVER,
                new ChargeAttackEventJS(player.getServer(), event));
        // KubeJS: cancel() -> INTERRUPT_FALSE，exit() -> INTERRUPT_DEFAULT，两者都视为取消。
        if (result.interruptFalse() || result.interruptDefault()) {
            event.setCanceled(true);
        }
    }

    private static void onShieldParried(ShieldParriedEvent event) {
        var blocker = event.getBlocker();
        if (blocker == null || blocker.getServer() == null) return;
        ParryEvents.PARRY.post(ScriptType.SERVER, new ShieldParriedEventJS(
                blocker.getServer(), blocker, event.getAttacker(),
                event.getDamageSource(), event.getBlockedDamage()));
    }
}
