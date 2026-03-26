package io.homeassistantcraft.mod.client;

public final class DebugOverlayClientState {
    private static volatile boolean debugEnabled = true;
    private static volatile String lastHaState = "<none>";
    private static volatile String lastServiceCallResult = "<none>";

    private DebugOverlayClientState() {
    }

    public static boolean debugEnabled() {
        return debugEnabled;
    }

    public static String lastHaState() {
        return lastHaState;
    }

    public static String lastServiceCallResult() {
        return lastServiceCallResult;
    }

    public static void update(boolean enabled, String state, String serviceResult) {
        debugEnabled = enabled;
        lastHaState = sanitize(state);
        lastServiceCallResult = sanitize(serviceResult);
    }

    public static void clear() {
        debugEnabled = true;
        lastHaState = "<none>";
        lastServiceCallResult = "<none>";
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "<none>";
        }
        return value;
    }
}
