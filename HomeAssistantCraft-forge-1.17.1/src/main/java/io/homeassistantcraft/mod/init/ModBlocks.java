package io.homeassistantcraft.mod.init;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.block.StateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, HomeAssistantCraftMod.MOD_ID);

    public static final RegistryObject<Block> STATE_BLOCK = BLOCKS.register("state_block",
        () -> new StateBlock(BlockBehaviour.Properties.of(Material.METAL).strength(2.0F)));

    public static final RegistryObject<Block> DEV_TEST_BLOCK = BLOCKS.register("dev_test_block",
        () -> new Block(BlockBehaviour.Properties.of(Material.STONE).strength(1.5F)));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
