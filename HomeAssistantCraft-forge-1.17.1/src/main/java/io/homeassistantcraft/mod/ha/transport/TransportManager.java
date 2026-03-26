package io.homeassistantcraft.mod.ha.transport;

import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TransportManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final HomeAssistantTransport primary;
    private final HomeAssistantTransport fallback;

    private TransportMode activeMode = TransportMode.NONE;
    private String lastFallbackReason = "none";

    public TransportManager(HomeAssistantTransport primary, HomeAssistantTransport fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public synchronized void connect(HomeAssistantConnectionSettings settings) {
        disconnect();

        primary.connect(settings);
        if (primary.state() == TransportState.READY) {
            activeMode = primary.mode();
            lastFallbackReason = "none";
            return;
        }

        fallback.connect(settings);
        if (fallback.state() == TransportState.READY) {
            activeMode = fallback.mode();
            lastFallbackReason = "primary transport unavailable";
            return;
        }

        activeMode = TransportMode.NONE;
        lastFallbackReason = "no transport available";
    }

    public synchronized void disconnect() {
        primary.disconnect();
        fallback.disconnect();
        activeMode = TransportMode.NONE;
    }

    public synchronized ServiceCallResult callService(ServiceCall serviceCall) {
        if (activeMode == primary.mode()) {
            ServiceCallResult primaryResult = primary.callService(serviceCall);
            if (primaryResult.success()) {
                return primaryResult;
            }
            ServiceCallResult fallbackResult = fallback.callService(serviceCall);
            if (fallbackResult.success()) {
                activeMode = fallback.mode();
                lastFallbackReason = primaryResult.message();
                return fallbackResult;
            }
            return fallbackResult;
        }

        if (activeMode == fallback.mode()) {
            return fallback.callService(serviceCall);
        }

        return ServiceCallResult.fail(TransportMode.NONE, "transport manager is not connected");
    }

    public synchronized Optional<String> fetchEntityState(String entityId) {
        if (activeMode == primary.mode()) {
            Optional<String> primaryState = primary.fetchEntityState(entityId);
            if (primaryState.isPresent()) {
                return primaryState;
            }

            Optional<String> fallbackState = fallback.fetchEntityState(entityId);
            if (fallbackState.isPresent()) {
                activeMode = fallback.mode();
                lastFallbackReason = "primary state read unavailable";
                return fallbackState;
            }

            LOGGER.warn("Failed to read Home Assistant entity state '{}' from both transports", entityId);
            return Optional.empty();
        }

        if (activeMode == fallback.mode()) {
            Optional<String> fallbackState = fallback.fetchEntityState(entityId);
            return fallbackState;
        }

        LOGGER.debug("Transport manager is not connected; cannot read Home Assistant entity state '{}'", entityId);
        return Optional.empty();
    }

    public synchronized TransportMode activeMode() {
        return activeMode;
    }

    public synchronized TransportState activeState() {
        if (activeMode == primary.mode()) {
            return primary.state();
        }
        if (activeMode == fallback.mode()) {
            return fallback.state();
        }
        return TransportState.DISCONNECTED;
    }

    public synchronized Optional<String> lastFallbackReason() {
        if ("none".equals(lastFallbackReason)) {
            return Optional.empty();
        }
        return Optional.of(lastFallbackReason);
    }

    public synchronized String statusSummary() {
        return "active=" + activeMode.name().toLowerCase() + ", "
            + primary.statusSummary() + ", "
            + fallback.statusSummary();
    }
}
