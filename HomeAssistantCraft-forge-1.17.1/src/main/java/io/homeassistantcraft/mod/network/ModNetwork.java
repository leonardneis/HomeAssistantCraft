package io.homeassistantcraft.mod.network;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.network.packet.DebugOverlaySyncPacket;
import io.homeassistantcraft.mod.network.packet.OpenServiceBlockScreenPacket;
import io.homeassistantcraft.mod.network.packet.SaveServiceBlockConfigPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(HomeAssistantCraftMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int nextMessageId = 0;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
            nextId(),
            OpenServiceBlockScreenPacket.class,
            OpenServiceBlockScreenPacket::encode,
            OpenServiceBlockScreenPacket::decode,
            OpenServiceBlockScreenPacket::handle
        );

        CHANNEL.registerMessage(
            nextId(),
            SaveServiceBlockConfigPacket.class,
            SaveServiceBlockConfigPacket::encode,
            SaveServiceBlockConfigPacket::decode,
            SaveServiceBlockConfigPacket::handle
        );

        CHANNEL.registerMessage(
            nextId(),
            DebugOverlaySyncPacket.class,
            DebugOverlaySyncPacket::encode,
            DebugOverlaySyncPacket::decode,
            DebugOverlaySyncPacket::handle
        );
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static int nextId() {
        return nextMessageId++;
    }
}
