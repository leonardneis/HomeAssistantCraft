package io.homeassistantcraft.mod.runtime;

import io.homeassistantcraft.mod.ha.cache.EntityStateCache;
import io.homeassistantcraft.mod.ha.transport.TransportManager;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HomeAssistantPollingService {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int POLL_INTERVAL_SECONDS = 2;

    private final TransportManager transportManager;
    private final EntityStateCache entityStateCache;

    private final Set<String> trackedEntityIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> failureLoggedByEntity = new ConcurrentHashMap<>();

    private ScheduledExecutorService pollExecutor;

    public HomeAssistantPollingService(TransportManager transportManager, EntityStateCache entityStateCache) {
        this.transportManager = transportManager;
        this.entityStateCache = entityStateCache;
    }

    public synchronized void start() {
        if (pollExecutor != null && !pollExecutor.isShutdown()) {
            return;
        }

        pollExecutor = Executors.newSingleThreadScheduledExecutor(new PollThreadFactory());
        pollExecutor.scheduleAtFixedRate(this::pollTrackedEntities, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (pollExecutor != null) {
            pollExecutor.shutdownNow();
            pollExecutor = null;
        }
        failureLoggedByEntity.clear();
    }

    public void trackEntity(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return;
        }
        trackedEntityIds.add(entityId);
    }

    public int trackedEntityCount() {
        return trackedEntityIds.size();
    }

    private void pollTrackedEntities() {
        for (String entityId : trackedEntityIds) {
            try {
                Optional<String> latestState = transportManager.getState(entityId);
                if (latestState.isPresent()) {
                    entityStateCache.updateState(entityId, latestState.get());
                    if (failureLoggedByEntity.remove(entityId) != null) {
                        LOGGER.info("Recovered Home Assistant polling for entity '{}'", entityId);
                    }
                    continue;
                }

                if (failureLoggedByEntity.putIfAbsent(entityId, true) == null) {
                    LOGGER.warn("Home Assistant polling failed for entity '{}'; keeping last known state", entityId);
                }
            } catch (Exception ex) {
                if (failureLoggedByEntity.putIfAbsent(entityId, true) == null) {
                    LOGGER.warn("Home Assistant polling error for entity '{}': {}", entityId, ex.getMessage());
                }
            }
        }
    }

    private static final class PollThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "HomeAssistantCraft-HA-Poller");
            thread.setDaemon(true);
            return thread;
        }
    }
}
