package io.homeassistantcraft.mod.block.entity;

import io.homeassistantcraft.mod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ServiceBlockEntity extends BlockEntity {
    public static final String DEFAULT_DOMAIN = "light";
    public static final String DEFAULT_SERVICE = "toggle";
    public static final String DEFAULT_ENTITY_ID = "light.living_room";

    private static final String TAG_DOMAIN = "domain";
    private static final String TAG_SERVICE = "service";
    private static final String TAG_ENTITY_ID = "entity_id";

    private String domain = DEFAULT_DOMAIN;
    private String service = DEFAULT_SERVICE;
    private String entityId = DEFAULT_ENTITY_ID;

    public ServiceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    public String domain() {
        return domain;
    }

    public String service() {
        return service;
    }

    public String entityId() {
        return entityId;
    }

    public void setConfiguration(String domain, String service, String entityId) {
        this.domain = sanitize(domain, DEFAULT_DOMAIN);
        this.service = sanitize(service, DEFAULT_SERVICE);
        this.entityId = sanitize(entityId, DEFAULT_ENTITY_ID);
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        domain = sanitize(tag.getString(TAG_DOMAIN), DEFAULT_DOMAIN);
        service = sanitize(tag.getString(TAG_SERVICE), DEFAULT_SERVICE);
        entityId = sanitize(tag.getString(TAG_ENTITY_ID), DEFAULT_ENTITY_ID);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        tag.putString(TAG_DOMAIN, domain);
        tag.putString(TAG_SERVICE, service);
        tag.putString(TAG_ENTITY_ID, entityId);
        return tag;
    }

    private static String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }

        return trimmed;
    }
}
