package io.homeassistantcraft.mod.network.packet;

import io.homeassistantcraft.mod.client.ClientPacketHandlers;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

public final class DebugOverlaySyncPacket {
    private static final int MAX_TEXT_LENGTH = 256;

    private final boolean debugEnabled;
    private final String lastHaState;
    private final String lastServiceCallResult;

    public DebugOverlaySyncPacket(boolean debugEnabled, String lastHaState, String lastServiceCallResult) {
        this.debugEnabled = debugEnabled;
        this.lastHaState = lastHaState;
        this.lastServiceCallResult = lastServiceCallResult;
    }

    public boolean debugEnabled() {
        return debugEnabled;
    }

    public String lastHaState() {
        return lastHaState;
    }

    public String lastServiceCallResult() {
        return lastServiceCallResult;
    }

    public static void encode(DebugOverlaySyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.debugEnabled);
        buffer.writeUtf(message.lastHaState, MAX_TEXT_LENGTH);
        buffer.writeUtf(message.lastServiceCallResult, MAX_TEXT_LENGTH);
    }

    public static DebugOverlaySyncPacket decode(FriendlyByteBuf buffer) {
        return new DebugOverlaySyncPacket(
            buffer.readBoolean(),
            buffer.readUtf(MAX_TEXT_LENGTH),
            buffer.readUtf(MAX_TEXT_LENGTH)
        );
    }

    public static void handle(DebugOverlaySyncPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientPacketHandlers.updateDebugOverlay(message);
            }
        });
        context.setPacketHandled(true);
    }
}
