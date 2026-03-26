package io.homeassistantcraft.mod.init;

import io.homeassistantcraft.mod.HomeAssistantCraftMod;
import io.homeassistantcraft.mod.block.entity.ServiceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, HomeAssistantCraftMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ServiceBlockEntity>> SERVICE_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("service_block",
            () -> BlockEntityType.Builder.of(ServiceBlockEntity::new, ModBlocks.SERVICE_BLOCK.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
