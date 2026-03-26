package io.homeassistantcraft.mod.block;

import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ServiceBlock extends Block {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TRIGGER_COOLDOWN_TICKS = 10;
    private static final String HARDCODED_DOMAIN = "light";
    private static final String HARDCODED_SERVICE = "toggle";
    private static final String HARDCODED_ENTITY_ID = "light.living_room";

    private static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<String, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();

    public ServiceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            syncPowerState(state, level, pos);
        }
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            syncPowerState(state, level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            LAST_TRIGGER_TICK.remove(cooldownKey(level, pos));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    private void syncPowerState(BlockState state, Level level, BlockPos pos) {
        boolean currentlyPowered = level.hasNeighborSignal(pos);
        boolean previouslyPowered = state.getValue(POWERED);

        if (currentlyPowered && !previouslyPowered && canTrigger(level, pos)) {
            triggerServiceCall(level, pos);
        }

        if (previouslyPowered != currentlyPowered) {
            level.setBlock(pos, state.setValue(POWERED, currentlyPowered), Block.UPDATE_ALL);
        }
    }

    private boolean canTrigger(Level level, BlockPos pos) {
        long now = level.getGameTime();
        String key = cooldownKey(level, pos);
        Long lastTick = LAST_TRIGGER_TICK.get(key);
        if (lastTick != null && now - lastTick < TRIGGER_COOLDOWN_TICKS) {
            return false;
        }

        LAST_TRIGGER_TICK.put(key, now);
        return true;
    }

    private void triggerServiceCall(Level level, BlockPos pos) {
        ServiceCall call = new ServiceCall(
            HARDCODED_DOMAIN,
            HARDCODED_SERVICE,
            Map.of(),
            Map.of("entity_id", HARDCODED_ENTITY_ID)
        );

        ServiceCallResult result = HomeAssistantServices.transportManager().callService(call);
        if (result.success()) {
            LOGGER.info(
                "ServiceBlock trigger at {}: {}.{} -> {} via {}",
                pos,
                HARDCODED_DOMAIN,
                HARDCODED_SERVICE,
                HARDCODED_ENTITY_ID,
                result.mode().name().toLowerCase()
            );
            return;
        }

        LOGGER.warn(
            "ServiceBlock trigger failed at {}: {}.{} -> {} (mode={} reason={})",
            pos,
            HARDCODED_DOMAIN,
            HARDCODED_SERVICE,
            HARDCODED_ENTITY_ID,
            result.mode().name().toLowerCase(),
            result.message()
        );
    }

    private static String cooldownKey(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.dimension().location() + ":" + pos.asLong();
        }

        return "client:" + pos.asLong();
    }
}
