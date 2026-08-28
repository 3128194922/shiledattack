package com.shiledattack.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * KubeJS event group exposed when KubeJS is installed. Scripts can listen with:
 *
 * <pre>{@code
 * ParryEvents.parried(event => {
 *     // event.blocker, event.attacker, event.damageSource, event.blockedDamage
 *     // event.player is null when the parry was performed by a non-player entity (e.g. a mob)
 * });
 * }</pre>
 */
public interface ParryEvents {
    EventGroup GROUP = EventGroup.of("ParryEvents");

    /** Fires on the server each time a shield parry succeeds. */
    EventHandler PARRY = GROUP.server("parried", () -> ShieldParriedEventJS.class);
}