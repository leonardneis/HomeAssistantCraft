package io.homeassistantcraft.mod.client;

import io.homeassistantcraft.mod.client.gui.ServiceBlockScreen;
import io.homeassistantcraft.mod.network.packet.DebugOverlaySyncPacket;
import io.homeassistantcraft.mod.network.packet.OpenServiceBlockScreenPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openServiceBlockScreen(OpenServiceBlockScreenPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.setScreen(new ServiceBlockScreen(
            message.pos(),
            message.domain(),
            message.service(),
            message.entityId()
        ));
    }

    public static void updateDebugOverlay(DebugOverlaySyncPacket message) {
        DebugOverlayClientState.update(
            message.debugEnabled(),
            message.lastHaState(),
            message.lastServiceCallResult()
        );
    }
}
