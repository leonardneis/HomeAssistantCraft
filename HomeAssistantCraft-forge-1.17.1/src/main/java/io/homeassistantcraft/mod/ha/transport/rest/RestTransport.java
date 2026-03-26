package io.homeassistantcraft.mod.ha.transport.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import io.homeassistantcraft.mod.ha.transport.HomeAssistantTransport;
import io.homeassistantcraft.mod.ha.transport.TransportMode;
import io.homeassistantcraft.mod.ha.transport.TransportState;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public final class RestTransport implements HomeAssistantTransport {
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    private HomeAssistantConnectionSettings settings;
    private TransportState state = TransportState.DISCONNECTED;
    private String lastError = "none";

    @Override
    public TransportMode mode() {
        return TransportMode.REST;
    }

    @Override
    public void connect(HomeAssistantConnectionSettings settings) {
        this.settings = settings;
        this.state = TransportState.READY;
        this.lastError = "none";
    }

    @Override
    public void disconnect() {
        this.settings = null;
        this.state = TransportState.DISCONNECTED;
    }

    @Override
    public TransportState state() {
        return state;
    }

    @Override
    public Optional<String> getState(String entityId) {
        if (state == TransportState.DISCONNECTED || settings == null) {
            return Optional.empty();
        }

        HttpRequest request = HttpRequest.newBuilder(settings.stateEndpoint(entityId))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + settings.accessToken())
            .header("Content-Type", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                lastError = "http " + response.statusCode() + ": " + response.body();
                state = TransportState.DEGRADED;
                return Optional.empty();
            }

            JsonObject responseJson = GSON.fromJson(response.body(), JsonObject.class);
            if (responseJson == null || !responseJson.has("state") || responseJson.get("state").isJsonNull()) {
                lastError = "invalid response body";
                state = TransportState.DEGRADED;
                return Optional.empty();
            }

            state = TransportState.READY;
            return Optional.of(responseJson.get("state").getAsString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            state = TransportState.DEGRADED;
            return Optional.empty();
        }
    }

    @Override
    public ServiceCallResult callService(ServiceCall serviceCall) {
        if (state == TransportState.DISCONNECTED || settings == null) {
            return ServiceCallResult.fail(mode(), "rest transport is not connected");
        }

        JsonObject payload = new JsonObject();
        if (!serviceCall.data().isEmpty()) {
            payload.add("data", GSON.toJsonTree(serviceCall.data()));
        }
        if (!serviceCall.target().isEmpty()) {
            payload.add("target", GSON.toJsonTree(serviceCall.target()));
        }

        HttpRequest request = HttpRequest.newBuilder(settings.serviceEndpoint(serviceCall.domain(), serviceCall.service()))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + settings.accessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                state = TransportState.READY;
                return ServiceCallResult.ok(mode());
            }
            lastError = "http " + response.statusCode() + ": " + response.body();
            state = TransportState.DEGRADED;
            return ServiceCallResult.fail(mode(), lastError);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            state = TransportState.DEGRADED;
            return ServiceCallResult.fail(mode(), lastError);
        }
    }

    @Override
    public String statusSummary() {
        return "rest=" + state.name().toLowerCase() + ", lastError=" + lastError;
    }
}
