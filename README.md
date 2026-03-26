# HomeAssistantCraft

HomeAssistantCraft is a modern Home Assistant integration for Minecraft.

This project is a continuation of the original [HomeAssistantMC](https://github.com/Codestian/HomeAssistantMC/) mod, which is no longer maintained.

## Features (planned)

- Real-time Home Assistant entity integration
- In-game automation blocks
- Player and item-based triggers
- Improved UI (no YAML-style configuration)
- Secure credential handling

## Goals

- Support modern Minecraft versions
- Provide a clean and user-friendly interface
- Enable deeper integration between Minecraft and real-world automation

## Status

Implementation started.

Current active module:

- `HomeAssistantCraft-forge-1.17.1/` (clean reimplementation target)

Initial foundation implemented:

- Forge 1.17.1 module scaffold
- Configuration system with TOML + environment token override
- Transport abstraction with WebSocket-primary/REST-fallback manager
- Initial `/hass status` and `/hass list` diagnostics command stubs

Legacy reference module remains read-only:

- `HomeAssistantMC-legacy/`

## Getting Started (Current)

1. Open `HomeAssistantCraft-forge-1.17.1/` in your IDE.
2. Import as a Gradle project.
3. Run Gradle tasks for your IDE run configurations.
4. Configure Home Assistant values in the generated common config file.

## Credits

Based on the original HomeAssistantMC project (MIT License).
