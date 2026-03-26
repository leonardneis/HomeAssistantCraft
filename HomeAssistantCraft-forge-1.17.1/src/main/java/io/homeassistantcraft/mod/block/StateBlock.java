package io.homeassistantcraft.mod.block;

import io.homeassistantcraft.mod.config.ModConfigs;
import io.homeassistantcraft.mod.ha.transport.TransportMode;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class StateBlock extends Block {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int POLL_INTERVAL_TICKS = 20;
    private static final String HARDCODED_ENTITY_ID = "light.living_room";

    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public StateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            HomeAssistantServices.pollingService().trackEntity(HARDCODED_ENTITY_ID);
            level.getBlockTicks().scheduleTick(pos, this, 1);
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
            level.getBlockTicks().scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (HomeAssistantServices.transportManager().activeMode() == TransportMode.NONE) {
            // Defensive bootstrap for integrated-server timing: ensure HA services are up before reading cache.
            ModConfigs.resolveSettings().ifPresent(settings -> {
                HomeAssistantServices.transportManager().connect(settings);
                HomeAssistantServices.pollingService().start();
            });
        }

        HomeAssistantServices.pollingService().trackEntity(HARDCODED_ENTITY_ID);

        Optional<String> cachedState = HomeAssistantServices.entityStateCache().getState(HARDCODED_ENTITY_ID);
        int targetPower = cachedState.isPresent() && "on".equalsIgnoreCase(cachedState.get()) ? 15 : 0;

        if (state.getValue(POWER) != targetPower) {
            LOGGER.info(
                "StateBlock power update at {}: {} -> {} (entity={} state={})",
                pos,
                state.getValue(POWER),
                targetPower,
                HARDCODED_ENTITY_ID,
                cachedState.orElse("<missing>")
            );
            level.setBlock(pos, state.setValue(POWER, targetPower), Block.UPDATE_ALL);
        }

        level.getBlockTicks().scheduleTick(pos, this, POLL_INTERVAL_TICKS);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter blockGetter, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }
}
