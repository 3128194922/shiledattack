package com.shiledattack.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * KubeJS event group exposed when KubeJS is installed. Scripts can listen with:
 *
 * <pre>{@code
 * ParryEvents.parried(event => {
 *     // event.player, event.attacker, event.damageSource, event.blockedDamage
 * });
 * }</pre>
 */
public interface ParryEvents {
    EventGroup GROUP = EventGroup.of("ParryEvents");

    /** Fires on the server each time a shield parry succeeds. */
    EventHandler PARRY = GROUP.server("parried", () -> ShieldParriedEventJS.class);
}