package io.homeassistantcraft.mod.event;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.config.ModConfigs;
import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import java.util.Optional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = HomeAssistantCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerLifecycleEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    private ServerLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        Optional<HomeAssistantConnectionSettings> settings = ModConfigs.resolveSettings();
        if (settings.isEmpty()) {
            HomeAssistantServices.transportManager().disconnect();
            HomeAssistantServices.pollingService().stop();
            LOGGER.warn("Home Assistant integration disabled: invalid URL or missing token for non-local host.");
            return;
        }

        HomeAssistantServices.transportManager().connect(settings.get());
        HomeAssistantServices.pollingService().start();
        LOGGER.info("Home Assistant integration enabled for {}", settings.get().baseUri());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        HomeAssistantServices.pollingService().stop();
        HomeAssistantServices.transportManager().disconnect();
        HomeAssistantServices.entityStateCache().clear();
    }
}
