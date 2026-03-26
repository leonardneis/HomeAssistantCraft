package io.homeassistantcraft.mod.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HomeAssistantCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandRegistrationEvents {
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
}
