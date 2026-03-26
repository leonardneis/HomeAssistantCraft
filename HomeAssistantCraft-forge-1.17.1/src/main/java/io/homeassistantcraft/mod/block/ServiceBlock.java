package io.homeassistantcraft.mod.block;

import io.homeassistantcraft.mod.block.entity.ServiceBlockEntity;
import io.homeassistantcraft.mod.debug.DebugSettings;
import io.homeassistantcraft.mod.debug.DebugRuntimeState;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import io.homeassistantcraft.mod.init.ModBlockEntities;
import io.homeassistantcraft.mod.network.ModNetwork;
import io.homeassistantcraft.mod.network.packet.OpenServiceBlockScreenPacket;
import io.homeassistantcraft.mod.runtime.HomeAssistantServices;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ServiceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TRIGGER_COOLDOWN_TICKS = 10;

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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        ServiceBlockEntity serviceBlockEntity = getServiceBlockEntity(level, pos);
        if (serviceBlockEntity == null) {
            player.sendMessage(new TextComponent("ServiceBlock: missing config entity"), Util.NIL_UUID);
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(new OpenServiceBlockScreenPacket(
                pos,
                serviceBlockEntity.domain(),
                serviceBlockEntity.service(),
                serviceBlockEntity.entityId()
            ), serverPlayer);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SERVICE_BLOCK_ENTITY.get().create(pos, state);
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
        ServiceBlockEntity serviceBlockEntity = getServiceBlockEntity(level, pos);
        if (serviceBlockEntity == null) {
            DebugRuntimeState.recordServiceCallResult("failed: missing ServiceBlockEntity at " + pos);
            if (DebugSettings.isEnabled()) {
                LOGGER.warn("ServiceBlock trigger failed at {}: missing block entity", pos);
                DebugSettings.broadcastToPlayers(level, "HA call failed: missing ServiceBlockEntity at " + pos);
            }
            return;
        }

        String domain = serviceBlockEntity.domain();
        String service = serviceBlockEntity.service();
        String entityId = serviceBlockEntity.entityId();

        ServiceCall call = new ServiceCall(
            domain,
            service,
            Map.of(),
            Map.of("entity_id", entityId)
        );

        ServiceCallResult result = HomeAssistantServices.transportManager().callService(call);
        if (result.success()) {
            DebugRuntimeState.recordServiceCallResult(
                "success: " + domain + "." + service + " -> " + entityId + " ("
                    + result.mode().name().toLowerCase() + ")"
            );
            if (DebugSettings.isEnabled()) {
                LOGGER.info(
                    "ServiceBlock trigger at {}: {}.{} -> {} via {}",
                    pos,
                    domain,
                    service,
                    entityId,
                    result.mode().name().toLowerCase()
                );
                DebugSettings.broadcastToPlayers(
                    level,
                    "HA call success: " + domain + "." + service + " -> " + entityId
                );
            }
            return;
        }

        DebugRuntimeState.recordServiceCallResult(
            "failed: " + domain + "." + service + " -> " + entityId + " ("
                + result.mode().name().toLowerCase() + ", " + result.message() + ")"
        );

        if (DebugSettings.isEnabled()) {
            LOGGER.warn(
                "ServiceBlock trigger failed at {}: {}.{} -> {} (mode={} reason={})",
                pos,
                domain,
                service,
                entityId,
                result.mode().name().toLowerCase(),
                result.message()
            );
            DebugSettings.broadcastToPlayers(
                level,
                "HA call failed: " + domain + "." + service + " -> " + entityId + " | " + result.message()
            );
        }
    }

    private static ServiceBlockEntity getServiceBlockEntity(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ServiceBlockEntity serviceBlockEntity)) {
            return null;
        }
        return serviceBlockEntity;
    }

    private static String cooldownKey(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.dimension().location() + ":" + pos.asLong();
        }

        return "client:" + pos.asLong();
    }
}
