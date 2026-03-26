# HomeAssistantCraft Forge 1.17.1

This module is the clean reimplementation target for HomeAssistantCraft.

## Current Implementation Slice

- Forge 1.17.1 project scaffold
- Mod entrypoint and common config registration
- Home Assistant connection settings resolver
  - URL from config
  - Access token from environment variable override, then config fallback
- Transport abstraction
  - WebSocket transport placeholder (planned)
  - REST transport (service-call baseline)
  - Transport manager with fallback routing
- In-memory entity cache service
- Initial diagnostics commands
  - `/hass status`
  - `/hass list` (cache count placeholder)

## Next Steps

1. Implement WebSocket transport lifecycle (auth, state sync, event subscription, reconnect backoff).
2. Add typed service payload validation and command-side test endpoint.
3. Implement State Block and Service Block block-entity architecture.
4. Add packet layer for client screens and server validation.
5. Replace command placeholders with full entity pagination and diagnostics.
