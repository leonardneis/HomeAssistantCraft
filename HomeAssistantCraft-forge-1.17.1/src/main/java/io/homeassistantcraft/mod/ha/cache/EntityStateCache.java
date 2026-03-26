package io.homeassistantcraft.mod.ha.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityStateCache {
    private final Map<String, EntitySnapshot> entities = new ConcurrentHashMap<>();

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
