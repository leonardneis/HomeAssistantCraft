package io.homeassistantcraft.mod;

import io.homeassistantcraft.mod.config.ModConfigs;
import io.homeassistantcraft.mod.init.ModBlocks;
import io.homeassistantcraft.mod.init.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HomeAssistantCraftMod.MOD_ID)
public final class HomeAssistantCraftMod {
    public static final String MOD_ID = "homeassistantcraft";
    private static final Logger LOGGER = LogManager.getLogger();

    public HomeAssistantCraftMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModConfigs.register();

        LOGGER.info("HomeAssistantCraft startup complete: registries initialized");
    }
}
