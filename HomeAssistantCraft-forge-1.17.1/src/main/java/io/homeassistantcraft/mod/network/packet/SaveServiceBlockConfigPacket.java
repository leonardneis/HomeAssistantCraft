package io.homeassistantcraft.mod.network.packet;

import io.homeassistantcraft.mod.block.entity.ServiceBlockEntity;
import io.homeassistantcraft.mod.init.ModBlocks;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class SaveServiceBlockConfigPacket {
    private static final int MAX_FIELD_LENGTH = 128;

    private final BlockPos pos;
    private final String domain;
    private final String service;
    private final String entityId;

    public SaveServiceBlockConfigPacket(BlockPos pos, String domain, String service, String entityId) {
        this.pos = pos;
        this.domain = domain;
        this.service = service;
        this.entityId = entityId;
    }

    public static void encode(SaveServiceBlockConfigPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.domain, MAX_FIELD_LENGTH);
        buffer.writeUtf(message.service, MAX_FIELD_LENGTH);
        buffer.writeUtf(message.entityId, MAX_FIELD_LENGTH);
    }

    public static SaveServiceBlockConfigPacket decode(FriendlyByteBuf buffer) {
        return new SaveServiceBlockConfigPacket(
            buffer.readBlockPos(),
            buffer.readUtf(MAX_FIELD_LENGTH),
            buffer.readUtf(MAX_FIELD_LENGTH),
            buffer.readUtf(MAX_FIELD_LENGTH)
        );
    }

    public static void handle(SaveServiceBlockConfigPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            Level level = player.getLevel();
            BlockPos pos = message.pos;
            if (!level.isLoaded(pos)) {
                player.sendMessage(new TextComponent("ServiceBlock save failed: chunk not loaded at " + pos), Util.NIL_UUID);
                return;
            }

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                player.sendMessage(new TextComponent("ServiceBlock save failed: too far away"), Util.NIL_UUID);
                return;
            }

            BlockState blockState = level.getBlockState(pos);
            if (!blockState.is(ModBlocks.SERVICE_BLOCK.get())) {
                String foundBlockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(blockState.getBlock()));
                player.sendMessage(new TextComponent(
                    "ServiceBlock save failed: expected service_block but found " + foundBlockId
                ), Util.NIL_UUID);
                return;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof ServiceBlockEntity serviceBlockEntity)) {
                player.sendMessage(new TextComponent("ServiceBlock save failed: block entity missing"), Util.NIL_UUID);
                return;
            }

            serviceBlockEntity.setConfiguration(message.domain, message.service, message.entityId);
            level.sendBlockUpdated(pos, blockState, blockState, net.minecraft.world.level.block.Block.UPDATE_ALL);

            String confirmation = "ServiceBlock saved: " + serviceBlockEntity.domain() + "."
                + serviceBlockEntity.service() + " -> " + serviceBlockEntity.entityId();
            player.sendMessage(new TextComponent(confirmation), Util.NIL_UUID);
        });
        context.setPacketHandled(true);
    }
}
