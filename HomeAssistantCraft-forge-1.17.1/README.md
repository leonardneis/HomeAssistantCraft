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
- Working minimal State Block vertical slice
  - placeable block, registered and visible in creative inventory
  - hardcoded Home Assistant entity polling over REST
  - redstone output: 15 when state is `on`, otherwise 0
  - safe failure handling with logging
- Simple dev test block
  - registered and visible in creative inventory
  - placeholder stone-based model/texture
- Dev runtime validated
  - `runClient` launches, world loads, and mod startup log is emitted

## Next Steps

1. Replace hardcoded entity id with block-level configuration storage.
2. Add UI for State Block and Service Block configuration.
3. Implement WebSocket transport lifecycle (auth, state sync, event subscription, reconnect backoff).
4. Add packet layer for client screens and server validation.
5. Expand command diagnostics with entity state inspection and transport health details.
