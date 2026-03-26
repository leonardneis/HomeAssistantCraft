package io.homeassistantcraft.mod.client;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HomeAssistantCraftMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DebugOverlayClientEvents {
    private DebugOverlayClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderTextOverlay(RenderGameOverlayEvent.Text event) {
        if (!DebugOverlayClientState.debugEnabled()) {
            return;
        }

        event.getLeft().add("HA Debug:");
        event.getLeft().add("Last HA state: " + DebugOverlayClientState.lastHaState());
        event.getLeft().add("Last service call: " + DebugOverlayClientState.lastServiceCallResult());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        DebugOverlayClientState.clear();
    }
}
