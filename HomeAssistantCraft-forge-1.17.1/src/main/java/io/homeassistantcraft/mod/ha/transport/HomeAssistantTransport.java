package io.homeassistantcraft.mod.ha.transport;

import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import java.util.Optional;

public interface HomeAssistantTransport {
    TransportMode mode();

    void connect(HomeAssistantConnectionSettings settings);

    void disconnect();

    TransportState state();

    Optional<String> fetchEntityState(String entityId);

    ServiceCallResult callService(ServiceCall serviceCall);

    String statusSummary();
}
