package io.homeassistantcraft.mod.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.block.entity.ServiceBlockEntity;
import io.homeassistantcraft.mod.init.ModBlocks;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HomeAssistantCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandRegistrationEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    private CommandRegistrationEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("hass")
            .then(Commands.literal("status")
                .executes(CommandRegistrationEvents::runStatus))
            .then(Commands.literal("list")
                .executes(CommandRegistrationEvents::runListPlaceholder)));

        dispatcher.register(Commands.literal("hac")
            .then(Commands.literal("set")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                    .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                            .then(Commands.argument("domain", StringArgumentType.word())
                                .then(Commands.argument("service", StringArgumentType.word())
                                    .then(Commands.argument("entity_id", StringArgumentType.word())
                                        .executes(CommandRegistrationEvents::runSetServiceBlockConfig)))))))));
    }

    private static int runStatus(CommandContext<CommandSourceStack> context) {
        String status = HomeAssistantServices.transportManager().statusSummary();
        context.getSource().sendSuccess(new TextComponent(status), false);
        return 1;
    }

    private static int runListPlaceholder(CommandContext<CommandSourceStack> context) {
        int size = HomeAssistantServices.entityStateCache().size();
        context.getSource().sendSuccess(new TextComponent("entities in cache=" + size), false);
        return 1;
    }

    private static int runSetServiceBlockConfig(CommandContext<CommandSourceStack> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        String domain = StringArgumentType.getString(context, "domain");
        String service = StringArgumentType.getString(context, "service");
        String entityId = StringArgumentType.getString(context, "entity_id");

        BlockPos pos = new BlockPos(x, y, z);
        Level level = context.getSource().getLevel();

        if (!level.isLoaded(pos)) {
            context.getSource().sendFailure(new TextComponent("hac set failed: chunk not loaded at " + pos));
            LOGGER.warn("hac set failed at {}: chunk not loaded", pos);
            return 0;
        }

        if (!level.getBlockState(pos).is(ModBlocks.SERVICE_BLOCK.get())) {
            context.getSource().sendFailure(new TextComponent("hac set failed: no ServiceBlock at " + pos));
            LOGGER.warn("hac set failed at {}: target is not ServiceBlock", pos);
            return 0;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ServiceBlockEntity serviceBlockEntity)) {
            context.getSource().sendFailure(new TextComponent("hac set failed: missing ServiceBlockEntity at " + pos));
            LOGGER.warn("hac set failed at {}: ServiceBlockEntity missing", pos);
            return 0;
        }

        serviceBlockEntity.setConfiguration(domain, service, entityId);
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);

        String message = "hac set success at " + pos + ": " + serviceBlockEntity.domain()
            + "." + serviceBlockEntity.service() + " -> " + serviceBlockEntity.entityId();
        context.getSource().sendSuccess(new TextComponent(message), true);
        LOGGER.info(message);
        return 1;
    }
}
