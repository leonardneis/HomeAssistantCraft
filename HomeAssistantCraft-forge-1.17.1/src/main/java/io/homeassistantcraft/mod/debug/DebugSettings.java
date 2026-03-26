package io.homeassistantcraft.mod.debug;

import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class DebugSettings {
    public static volatile boolean DEBUG_ENABLED = true;

    private DebugSettings() {
    }

    public static boolean isEnabled() {
        return DEBUG_ENABLED;
    }

    public static void setEnabled(boolean enabled) {
        DEBUG_ENABLED = enabled;
    }

    public static void broadcastToPlayers(Level level, String message) {
        if (!DEBUG_ENABLED || level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.sendMessage(new TextComponent(message), Util.NIL_UUID);
        }
    }
}
