package io.homeassistantcraft.mod.ha;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HomeAssistantConnectionSettings {
    private final URI baseUri;
    private final String accessToken;

    public HomeAssistantConnectionSettings(URI baseUri, String accessToken) {
        this.baseUri = baseUri;
        this.accessToken = accessToken;
    }

    public URI baseUri() {
        return baseUri;
    }

    public String accessToken() {
        return accessToken;
    }

    public URI serviceEndpoint(String domain, String service) {
        String root = baseUri.toString();
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        return URI.create(root + "/api/services/" + domain + "/" + service);
    }

    public URI stateEndpoint(String entityId) {
        String root = baseUri.toString();
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }

        String encodedEntityId = URLEncoder.encode(entityId, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create(root + "/api/states/" + encodedEntityId);
    }

    public URI websocketEndpoint() {
        String scheme = "https".equalsIgnoreCase(baseUri.getScheme()) ? "wss" : "ws";
        String host = baseUri.getHost();
        int port = baseUri.getPort();
        String authority = port >= 0 ? host + ":" + port : host;
        return URI.create(scheme + "://" + authority + "/api/websocket");
    }
}
