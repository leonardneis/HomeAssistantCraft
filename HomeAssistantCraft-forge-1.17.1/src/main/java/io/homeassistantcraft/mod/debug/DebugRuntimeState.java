package io.homeassistantcraft.mod.debug;

import io.homeassistantcraft.mod.network.ModNetwork;
import io.homeassistantcraft.mod.network.packet.DebugOverlaySyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

public final class DebugRuntimeState {
    private static volatile String lastHaState = "<none>";
    private static volatile String lastServiceCallResult = "<none>";

    private DebugRuntimeState() {
    }

    public static void recordHaState(String entityId, String state) {
        lastHaState = sanitize(entityId) + " = " + sanitize(state);
        scheduleSyncIfEnabled();
    }

    public static void recordServiceCallResult(String result) {
        lastServiceCallResult = sanitize(result);
        scheduleSyncIfEnabled();
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ModNetwork.sendToPlayer(new DebugOverlaySyncPacket(
            DebugSettings.isEnabled(),
            lastHaState,
            lastServiceCallResult
        ), player);
    }

    public static void syncToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    private static void scheduleSyncIfEnabled() {
        if (!DebugSettings.isEnabled()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(DebugRuntimeState::syncToAllPlayers);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "<none>";
        }

        String trimmed = value.trim();
        if (trimmed.length() <= 240) {
            return trimmed;
        }
        return trimmed.substring(0, 240);
    }
}
