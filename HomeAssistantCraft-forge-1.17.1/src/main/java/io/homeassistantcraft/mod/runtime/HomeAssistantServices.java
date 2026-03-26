package io.homeassistantcraft.mod.runtime;

import io.homeassistantcraft.mod.ha.cache.EntityStateCache;
import io.homeassistantcraft.mod.ha.transport.TransportManager;
import io.homeassistantcraft.mod.ha.transport.rest.RestTransport;
import io.homeassistantcraft.mod.ha.transport.websocket.WebSocketTransport;

public final class HomeAssistantServices {
    private static final EntityStateCache ENTITY_STATE_CACHE = new EntityStateCache();
    private static final TransportManager TRANSPORT_MANAGER =
        new TransportManager(new WebSocketTransport(), new RestTransport());
    private static final HomeAssistantPollingService POLLING_SERVICE =
        new HomeAssistantPollingService(TRANSPORT_MANAGER, ENTITY_STATE_CACHE);

    private HomeAssistantServices() {
    }

    public static EntityStateCache entityStateCache() {
        return ENTITY_STATE_CACHE;
    }

    public static TransportManager transportManager() {
        return TRANSPORT_MANAGER;
    }

    public static HomeAssistantPollingService pollingService() {
        return POLLING_SERVICE;
    }
}
