package io.homeassistantcraft.mod.ha.transport;

import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;

public interface HomeAssistantTransport {
    TransportMode mode();

    void connect(HomeAssistantConnectionSettings settings);

    void disconnect();

    TransportState state();

    ServiceCallResult callService(ServiceCall serviceCall);

    String statusSummary();
}
