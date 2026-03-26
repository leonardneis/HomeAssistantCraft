package io.homeassistantcraft.mod.ha.model;

import io.homeassistantcraft.mod.ha.transport.TransportMode;

public final class ServiceCallResult {
    private final boolean success;
    private final TransportMode mode;
    private final String message;

    public ServiceCallResult(boolean success, TransportMode mode, String message) {
        this.success = success;
        this.mode = mode;
        this.message = message;
    }

    public boolean success() {
        return success;
    }

    public TransportMode mode() {
        return mode;
    }

    public String message() {
        return message;
    }

    public static ServiceCallResult ok(TransportMode mode) {
        return new ServiceCallResult(true, mode, "ok");
    }

    public static ServiceCallResult fail(TransportMode mode, String message) {
        return new ServiceCallResult(false, mode, message);
    }
}
