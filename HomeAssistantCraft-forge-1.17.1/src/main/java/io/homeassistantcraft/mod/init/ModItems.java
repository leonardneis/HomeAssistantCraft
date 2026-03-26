package io.homeassistantcraft.mod.init;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, HomeAssistantCraftMod.MOD_ID);

    public static final RegistryObject<Item> STATE_BLOCK_ITEM = ITEMS.register("state_block",
        () -> new BlockItem(ModBlocks.STATE_BLOCK.get(),
            new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)));

    public static final RegistryObject<Item> DEV_TEST_BLOCK_ITEM = ITEMS.register("dev_test_block",
        () -> new BlockItem(ModBlocks.DEV_TEST_BLOCK.get(),
            new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
