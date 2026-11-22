package com.shiledattack.combat;

import com.shiledattack.ShiledAttackMod;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 注册逐武器蓄力档案的数据包监听器。
 */
public final class ChargeProfileEvents {

    private ChargeProfileEvents() {
    }

    /** 服务端数据包：{@code /reload} 与进入世界时加载。 */
    @Mod.EventBusSubscriber(modid = ShiledAttackMod.MODID)
    public static final class Server {
        private Server() {
        }

        @SubscribeEvent
        public static void onAddReloadListener(AddReloadListenerEvent event) {
            event.addListener(new ChargeProfileReloadListener());
        }

        /**
         * 数据包同步时机：登录与 {@code /reload} 后都会触发。
         *
         * <p>JSON 监听器已先执行，这里把解析结果推给客户端，
         * 保证 HUD 的蓄力比例与结算一致。</p>
         */
        @SubscribeEvent
        public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                com.shiledattack.network.ParryNetwork.sendProfiles(event.getPlayer());
                return;
            }
            for (net.minecraft.server.level.ServerPlayer player : event.getPlayers()) {
                com.shiledattack.network.ParryNetwork.sendProfiles(player);
            }
        }
    }

}
