package io.homeassistantcraft.mod.network.packet;

import io.homeassistantcraft.mod.client.ClientPacketHandlers;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

public final class OpenServiceBlockScreenPacket {
    private static final int MAX_FIELD_LENGTH = 128;

    private final BlockPos pos;
    private final String domain;
    private final String service;
    private final String entityId;

    public OpenServiceBlockScreenPacket(BlockPos pos, String domain, String service, String entityId) {
        this.pos = pos;
        this.domain = domain;
        this.service = service;
        this.entityId = entityId;
    }

    public BlockPos pos() {
        return pos;
    }

    public String domain() {
        return domain;
    }

    public String service() {
        return service;
    }

    public String entityId() {
        return entityId;
    }

    public static void encode(OpenServiceBlockScreenPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.domain, MAX_FIELD_LENGTH);
        buffer.writeUtf(message.service, MAX_FIELD_LENGTH);
        buffer.writeUtf(message.entityId, MAX_FIELD_LENGTH);
    }

    public static OpenServiceBlockScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenServiceBlockScreenPacket(
            buffer.readBlockPos(),
            buffer.readUtf(MAX_FIELD_LENGTH),
            buffer.readUtf(MAX_FIELD_LENGTH),
            buffer.readUtf(MAX_FIELD_LENGTH)
        );
    }

    public static void handle(OpenServiceBlockScreenPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientPacketHandlers.openServiceBlockScreen(message);
            }
        });
        context.setPacketHandled(true);
    }
}
