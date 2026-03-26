package io.homeassistantcraft.mod.event;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.debug.DebugRuntimeState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HomeAssistantCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerDebugSyncEvents {
    private PlayerDebugSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            DebugRuntimeState.syncToPlayer(serverPlayer);
        }
    }
}
