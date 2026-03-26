package io.homeassistantcraft.mod.ha.transport.websocket;

import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import io.homeassistantcraft.mod.ha.transport.HomeAssistantTransport;
import io.homeassistantcraft.mod.ha.transport.TransportMode;
import io.homeassistantcraft.mod.ha.transport.TransportState;
import java.util.Optional;

public final class WebSocketTransport implements HomeAssistantTransport {
    private TransportState state = TransportState.DISCONNECTED;

    @Override
    public TransportMode mode() {
        return TransportMode.WEBSOCKET;
    }

    @Override
    public void connect(HomeAssistantConnectionSettings settings) {
        state = TransportState.DEGRADED;
    }

    @Override
    public void disconnect() {
        state = TransportState.DISCONNECTED;
    }

    @Override
    public TransportState state() {
        return state;
    }

    @Override
    public Optional<String> getState(String entityId) {
        return Optional.empty();
    }

    @Override
    public ServiceCallResult callService(ServiceCall serviceCall) {
        return ServiceCallResult.fail(mode(), "websocket transport is not implemented yet");
    }

    @Override
    public String statusSummary() {
        return "websocket=" + state.name().toLowerCase();
    }
}
