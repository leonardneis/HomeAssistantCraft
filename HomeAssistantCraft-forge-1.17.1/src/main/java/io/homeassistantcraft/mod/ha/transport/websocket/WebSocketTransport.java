package io.homeassistantcraft.mod.ha.transport.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.homeassistantcraft.mod.ha.HomeAssistantConnectionSettings;
import io.homeassistantcraft.mod.ha.model.ServiceCall;
import io.homeassistantcraft.mod.ha.model.ServiceCallResult;
import io.homeassistantcraft.mod.ha.transport.HomeAssistantTransport;
import io.homeassistantcraft.mod.ha.transport.TransportMode;
import io.homeassistantcraft.mod.ha.transport.TransportState;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WebSocketTransport implements HomeAssistantTransport {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final ScheduledExecutorService reconnectExecutor =
        Executors.newSingleThreadScheduledExecutor(new ReconnectThreadFactory());

    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicInteger requestId = new AtomicInteger(10);

    private volatile TransportState state = TransportState.DISCONNECTED;
    private volatile HomeAssistantConnectionSettings settings;
    private volatile WebSocket webSocket;
    private volatile BiConsumer<String, String> stateUpdateListener = (entityId, entityState) -> {
    };

    private volatile boolean shuttingDown;
    private volatile long reconnectDelaySeconds = 1;

    @Override
    public TransportMode mode() {
        return TransportMode.WEBSOCKET;
    }

    @Override
    public void connect(HomeAssistantConnectionSettings settings) {
        this.settings = settings;
        this.shuttingDown = false;
        this.reconnectDelaySeconds = 1;
        this.reconnectScheduled.set(false);

        WebSocket existingSocket = this.webSocket;
        if (existingSocket != null) {
            existingSocket.abort();
        }

        connectInternal();
    }

    @Override
    public void disconnect() {
        shuttingDown = true;
        reconnectScheduled.set(false);

        WebSocket currentSocket = webSocket;
        webSocket = null;
        if (currentSocket != null) {
            currentSocket.abort();
        }

        setState(TransportState.DISCONNECTED, "disconnected");
    }

    @Override
    public TransportState state() {
        return state;
    }

    @Override
    public void setStateUpdateListener(BiConsumer<String, String> stateUpdateListener) {
        this.stateUpdateListener = stateUpdateListener == null ? (entityId, entityState) -> {
        } : stateUpdateListener;
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

    private void connectInternal() {
        if (shuttingDown || settings == null) {
            return;
        }

        setState(TransportState.CONNECTING, "connecting");

        httpClient.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(settings.websocketEndpoint(), new HomeAssistantWebSocketListener())
            .whenComplete((socket, error) -> {
                if (error != null) {
                    handleDisconnect("connect failed: " + safeMessage(error));
                    return;
                }
                webSocket = socket;
            });
    }

    private void sendAuth() {
        if (settings == null) {
            return;
        }

        JsonObject authPayload = new JsonObject();
        authPayload.addProperty("type", "auth");
        authPayload.addProperty("access_token", settings.accessToken());
        sendJson(authPayload);
    }

    private void subscribeStateChangedEvents() {
        JsonObject subscribePayload = new JsonObject();
        subscribePayload.addProperty("id", requestId.getAndIncrement());
        subscribePayload.addProperty("type", "subscribe_events");
        subscribePayload.addProperty("event_type", "state_changed");
        sendJson(subscribePayload);
    }

    private void sendJson(JsonObject payload) {
        WebSocket currentSocket = webSocket;
        if (currentSocket == null) {
            return;
        }
        currentSocket.sendText(GSON.toJson(payload), true);
    }

    private void handleMessage(String rawMessage) {
        JsonObject message;
        try {
            message = GSON.fromJson(rawMessage, JsonObject.class);
        } catch (Exception ignored) {
            return;
        }
        if (message == null) {
            return;
        }

        String type = getString(message, "type");
        if (type == null) {
            return;
        }

        switch (type) {
            case "auth_required":
                sendAuth();
                break;
            case "auth_ok":
                reconnectDelaySeconds = 1;
                setState(TransportState.READY, "authenticated");
                subscribeStateChangedEvents();
                break;
            case "auth_invalid":
                setState(TransportState.ERROR, "auth invalid");
                scheduleReconnect();
                break;
            case "event":
                handleStateChangedEvent(message);
                break;
            default:
                break;
        }
    }

    private void handleStateChangedEvent(JsonObject message) {
        JsonObject eventPayload = getObject(message, "event");
        if (eventPayload == null) {
            return;
        }

        if (!"state_changed".equals(getString(eventPayload, "event_type"))) {
            return;
        }

        JsonObject dataPayload = getObject(eventPayload, "data");
        if (dataPayload == null) {
            return;
        }

        String entityId = getString(dataPayload, "entity_id");
        JsonObject newState = getObject(dataPayload, "new_state");
        String stateValue = newState == null ? null : getString(newState, "state");

        if (entityId == null || stateValue == null) {
            return;
        }

        stateUpdateListener.accept(entityId, stateValue);
    }

    private void handleDisconnect(String reason) {
        if (shuttingDown) {
            setState(TransportState.DISCONNECTED, reason);
            return;
        }

        webSocket = null;
        setState(TransportState.DEGRADED, reason);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (shuttingDown || settings == null) {
            return;
        }

        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        long delay = reconnectDelaySeconds;
        reconnectDelaySeconds = Math.min(reconnectDelaySeconds * 2, 30);

        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            if (!shuttingDown) {
                connectInternal();
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void setState(TransportState newState, String reason) {
        TransportState previous = this.state;
        this.state = newState;

        if (previous == newState) {
            return;
        }

        switch (newState) {
            case CONNECTING:
                LOGGER.info("Home Assistant WebSocket connecting");
                break;
            case READY:
                LOGGER.info("Home Assistant WebSocket connected");
                break;
            case DEGRADED:
                LOGGER.warn("Home Assistant WebSocket degraded: {}", reason);
                break;
            case ERROR:
                LOGGER.warn("Home Assistant WebSocket error: {}", reason);
                break;
            case DISCONNECTED:
                LOGGER.info("Home Assistant WebSocket disconnected");
                break;
            default:
                break;
        }
    }

    private static JsonObject getObject(JsonObject source, String key) {
        if (!source.has(key) || source.get(key).isJsonNull() || !source.get(key).isJsonObject()) {
            return null;
        }
        return source.getAsJsonObject(key);
    }

    private static String getString(JsonObject source, String key) {
        if (!source.has(key) || source.get(key).isJsonNull()) {
            return null;
        }
        try {
            return source.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getMessage();
    }

    private final class HomeAssistantWebSocketListener implements WebSocket.Listener {
        private final StringBuilder frameBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            frameBuffer.append(data);
            if (last) {
                String message = frameBuffer.toString();
                frameBuffer.setLength(0);
                handleMessage(message);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            handleDisconnect("error: " + safeMessage(error));
            WebSocket.Listener.super.onError(webSocket, error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            handleDisconnect("closed (" + statusCode + "): " + reason);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ReconnectThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "HomeAssistantCraft-HA-WebSocket");
            thread.setDaemon(true);
            return thread;
        }
    }
}
