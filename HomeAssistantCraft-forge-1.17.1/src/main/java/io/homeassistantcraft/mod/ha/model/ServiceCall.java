package io.homeassistantcraft.mod.ha.model;

import java.util.Map;

public final class ServiceCall {
    private final String domain;
    private final String service;
    private final Map<String, Object> data;
    private final Map<String, Object> target;

    public ServiceCall(String domain, String service, Map<String, Object> data, Map<String, Object> target) {
        this.domain = domain;
        this.service = service;
        this.data = data == null ? Map.of() : Map.copyOf(data);
        this.target = target == null ? Map.of() : Map.copyOf(target);
    }

    public String domain() {
        return domain;
    }

    public String service() {
        return service;
    }

    public Map<String, Object> data() {
        return data;
    }

    public Map<String, Object> target() {
        return target;
    }
}
