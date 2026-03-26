package io.homeassistantcraft.mod.ha.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityStateCache {
    private final Map<String, EntitySnapshot> entities = new ConcurrentHashMap<>();

    public void updateState(String entityId, String state) {
        if (entityId == null || entityId.isBlank() || state == null) {
            return;
        }

        entities.compute(entityId, (id, existing) -> {
            if (existing == null) {
                return new EntitySnapshot(id, state, Map.of(), System.currentTimeMillis());
            }
            return new EntitySnapshot(id, state, existing.attributes(), System.currentTimeMillis());
        });
    }

    public Optional<String> getState(String entityId) {
        EntitySnapshot snapshot = entities.get(entityId);
        if (snapshot == null) {
            return Optional.empty();
        }
        return Optional.of(snapshot.state());
    }

    public void upsert(EntitySnapshot snapshot) {
        entities.put(snapshot.entityId(), snapshot);
    }

    public Optional<EntitySnapshot> get(String entityId) {
        return Optional.ofNullable(entities.get(entityId));
    }

    public Collection<EntitySnapshot> all() {
        return entities.values();
    }

    public int size() {
        return entities.size();
    }

    public void clear() {
        entities.clear();
    }
}
