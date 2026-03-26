package io.homeassistantcraft.mod.block;

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
        int targetPower = 0;

        try {
            Optional<String> haState = HomeAssistantServices.transportManager().fetchEntityState(HARDCODED_ENTITY_ID);
            if (haState.isPresent() && "on".equalsIgnoreCase(haState.get())) {
                targetPower = 15;
            }
        } catch (Exception ex) {
            LOGGER.warn("State Block polling failed at {}: {}", pos, ex.getMessage());
        }

        if (state.getValue(POWER) != targetPower) {
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
