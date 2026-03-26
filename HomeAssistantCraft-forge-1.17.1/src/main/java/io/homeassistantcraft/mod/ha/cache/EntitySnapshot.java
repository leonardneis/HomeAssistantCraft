package io.homeassistantcraft.mod.ha.cache;

import java.util.Map;

public final class EntitySnapshot {
    private final String entityId;
    private final String state;
    private final Map<String, Object> attributes;
    private final long updatedAtMillis;

    public EntitySnapshot(String entityId, String state, Map<String, Object> attributes, long updatedAtMillis) {
        this.entityId = entityId;
        this.state = state;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.updatedAtMillis = updatedAtMillis;
    }

    public String entityId() {
        return entityId;
    }

    public String state() {
        return state;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public long updatedAtMillis() {
        return updatedAtMillis;
    }
}
