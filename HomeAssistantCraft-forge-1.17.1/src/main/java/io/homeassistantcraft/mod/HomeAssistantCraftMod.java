package io.homeassistantcraft.mod;

import io.homeassistantcraft.mod.config.ModConfigs;
import net.minecraftforge.fml.common.Mod;

@Mod(HomeAssistantCraftMod.MOD_ID)
public final class HomeAssistantCraftMod {
    public static final String MOD_ID = "homeassistantcraft";

    public HomeAssistantCraftMod() {
        ModConfigs.register();
    }
}
