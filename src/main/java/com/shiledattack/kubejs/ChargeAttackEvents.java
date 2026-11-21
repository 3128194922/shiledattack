package com.shiledattack.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * 蓄力攻击的 KubeJS 事件组。
 *
 * <pre>{@code
 * ChargeAttackEvents.attacked(event => {
 *     event.damage *= 1.2
 * })
 * }</pre>
 */
public interface ChargeAttackEvents {
    EventGroup GROUP = EventGroup.of("ChargeAttackEvents");

    /** 蓄力攻击结算前触发；可修改 damage 或取消。 */
    EventHandler ATTACKED = GROUP.server("attacked", () -> ChargeAttackEventJS.class)
            .hasResult();
}
