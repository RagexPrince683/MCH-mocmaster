# MC Helicopter Overdrive+

MC Helicopter Overdrive+ is a Minecraft Forge 1.7.10 vehicle-combat mod based on the original MC Helicopter codebase. It expands MCHeli with a larger vehicle ecosystem, modernized rendering work, ships, tanks, drones/UAV support, man-portable weapons, deployable equipment, and extensive server-side configuration.

The mod is intended for players, modpack authors, and server administrators who want military-style vehicles and combined-arms gameplay in legacy Minecraft 1.7.10 packs.

> **Project status:** active fork/overhaul of the original MCHeli lineage. The codebase targets Minecraft **1.7.10** and Forge **10.13.4.1614** for builds, while declaring compatibility with Forge **10.13.2.1230 or newer**.

## What the mod adds

MCHeli Overdrive+ is a modernization and expansion of the original MCHeli project. It keeps MCHeli's data-driven content format, but the fork is built around new vehicle physics, carrier/naval gameplay, submarine behavior, long-range rendering, persistence, HUD/camera work, and server-management systems.

### Overdrive-specific systems

- **New aircraft systems:** fixed-wing aircraft can opt into the new flight model with stall and angle-of-attack simulation, control-authority modeling, aircraft mass and thrust simulation, configurable gravity, energy retention, drag, overspeed damage, dive behavior, flight ceilings, and combat flaps.
- **New helicopter system:** helicopters can opt into the new helicopter flight model with collective controls, rotor thrust and RPM modeling, cyclic/tail-rotor authority, configurable climb performance, lateral/backward thrust tuning, hover assist, and explicit hover-mode stabilization.
- **Naval and carrier gameplay:** ships support moving walkable deck behavior, ship-deck interaction fixes, carrier-style vehicle racks, plane attachment/parking, launch assistance, and vehicle-on-vehicle transport/interaction improvements.
- **Submarine behavior:** ship-based submarine mode supports diving, underwater ascend/descend controls, bounded vertical acceleration, vertical speed limits, water-depth behavior, and HUD prompts for submarine navigation.
- **Rendering and performance:** client-side vehicle LOD snapshots, long-range model-only rendering, distance-based model optimization, shader/smooth-shading toggles, render-distance weighting, and optional multi-threaded model loading improve large-pack and large-server usability.
- **Vehicle interaction and persistence:** rack/carrier compatibility checks, vehicle launch grace periods, improved seat/rack repair paths, NewUAV JSON persistence, UAV duplicate protection, and entity-info synchronization improve multiplayer reliability.
- **Camera and controls:** new aircraft systems include a modernized third-person chase camera, improved freelook behavior, held look-ahead, rack operation controls, submarine depth controls, and pilot usability refinements. Aircraft follow-camera improvements and mouse-aim controls are currently in development and under active refinement; they are configurable, disabled by default where appropriate, and documented with their current limitations.
- **New HUD systems:** opted-in aircraft expose compact pilot telemetry, throttle/engine indicators, altitude/speed readouts, flight-state warnings, hover-assist state, damage readouts, and weapon/ammunition displays while preserving legacy HUD definitions for existing content.
- **Framework and compatibility:** expanded vehicle configuration keys, legacy/new mobility-system opt-in support, backwards-compatible aliases, shared vehicle documentation, and server configuration references let old content continue to run while new packs adopt Overdrive systems incrementally.

### Legacy content retained

Existing MCHeli-style helicopters, fixed-wing planes, tanks, ships, ground vehicles, weapons, HUDs, models, textures, sounds, recipes, UAV equipment, portable weapons, deployable items, and Drafting Table workflows remain supported. Vehicles that do not opt into new systems continue to use legacy behavior.

For a detailed project-wide audit of Overdrive-only systems, see [Overdrive Feature Overview](docs/overdrive-features.md).

## Requirements and compatibility

| Requirement | Details |
| --- | --- |
| Minecraft | 1.7.10 only |
| Mod loader | Minecraft Forge 1.7.10; build script uses `1.7.10-10.13.4.1614-1.7.10` |
| Java | Java 8 recommended/required for development and runtime compatibility with ForgeGradle 1.x |
| Client | Required for players connecting to servers that use the mod |
| Server | Install on dedicated servers that need MCHeli entities, recipes, commands, and configuration |
| Asset pack | Required for actual vehicles/weapons/content beyond the registered core items; assets are read from `assets/mcheli/...` in the mod jar or from `mods/mcheli/` in the development run directory |

The Forge metadata marks the mod as client-required and server-optional, but multiplayer worlds that spawn or simulate MCHeli vehicles should run the mod on the server and all participating clients.

## Installation

### Single-player or client

1. Install Minecraft Forge for Minecraft 1.7.10.
2. Place the built MC Helicopter Overdrive+ jar in `.minecraft/mods/`.
3. Install the matching MCHeli asset/content pack so the folder structure includes `assets/mcheli/` content such as `helicopters`, `planes`, `ships`, `tanks`, `vehicles`, `weapons`, `hud`, `models`, `textures`, `sounds`, `item`, and `throwable` where applicable.
4. Start the game once to generate `.minecraft/config/mcheli.cfg`.
5. Adjust configuration options as needed, then restart or use `/mcheli reconfig` for server-side config reloads.

### Dedicated server

1. Install Forge 1.7.10 on the server.
2. Place the mod jar and matching asset/content pack in the server `mods/` directory.
3. Start the server once to generate `config/mcheli.cfg`.
4. Stop the server, edit the configuration, then restart. Some server settings can be reloaded with `/mcheli reconfig`.
5. Ensure every joining client has the same mod and compatible content/assets.

### Development builds

This repository uses ForgeGradle 1.x through the Gradle wrapper.

```bash
./gradlew setupDecompWorkspace
./gradlew build
```

Build output is written under `build/libs/`. The Gradle build increments `version.properties` as part of `build`, so avoid running a release build casually if you do not intend to bump the patch version.

For development asset testing, the build/run setup expects assets in `build/run/mods/mcheli/` with the same structure used by normal MCHeli installations.

## Basic usage

1. Open the MCHeli creative tabs (`MCHeliO Item`, `MCHeliO Helicopters`, `MCHeliO Planes`, `MCHeliO Ships`, `MCHeliO Tanks`, `MCHeliO Vehicles`, and `MCHeliO Recipe Items`).
2. Place or craft a **Drafting Table** to access recipes when recipes are enabled.
3. Use vehicle item icons to place vehicles. If `PlaceableOnSpongeOnly` is enabled, vehicles must be placed on sponge blocks.
4. Enter vehicles, use the configured movement/weapon keys, and refuel/repair according to the content pack's vehicle definitions.
5. Server operators can use `/mcheli list` to discover available administrative subcommands.

See the extended documentation for step-by-step guides:

- [Getting Started Guide](docs/getting-started.md)
- [Overdrive Feature Overview](docs/overdrive-features.md)
- [Configuration Reference](docs/configuration.md)
- [Command and Permission Reference](docs/commands.md)
- [Server Administration Guide](docs/server-administration.md)
- [Documentation Audit Notes](docs/documentation-audit.md)

## Configuration overview

The mod writes `config/mcheli.cfg` on startup. Important options include:

- `EnableCommand` - enables or disables `/mcheli` subcommands.
- `PlaceableOnSpongeOnly` - restricts vehicle placement to sponge blocks.
- `Explosion_DestroyBlock`, `Explosion_FlamingBlock`, and `Collision_DestroyBlock` - control world damage behavior.
- `InfinityAmmo` and `InfinityFuel` - enable unlimited ammunition/fuel globally.
- `AllHeliSpeed`, `AllPlaneSpeed`, `AllShipSpeed`, and `AllTankSpeed` - global speed multipliers/clamps.
- `EnableAircraftLODRender`, `AircraftLODStartDistance`, and `AircraftLODFarDistance` - control long-distance vehicle model rendering.
- `MultiThreadedModelLoading` - toggles threaded model loading on the client.
- `CommandPermission` entries - grant specific `/mcheli` subcommands to named non-operator players.
- `Key*` entries - default key and mouse bindings for MCHeli controls.

For every documented option and default value found in source, see [docs/configuration.md](docs/configuration.md).

## Commands

All commands use the `/mcheli` root command and can be disabled globally with `EnableCommand = false`.

Common examples:

```text
/mcheli list
/mcheli reconfig
/mcheli status entity 5
/mcheli showboundingbox true
/mcheli title 5 2 {"text":"Mission start"}
```

Operators can use all subcommands. Non-operators need matching `CommandPermission` entries in `mcheli.cfg`. See [docs/commands.md](docs/commands.md) for syntax, examples, and permission configuration.

## Fixed-wing flight-model tuning

Realistic fixed-wing aircraft use data-driven stall, speed, throttle, and pitch-protection values. See [docs/flight-model.md](docs/flight-model.md) for the idle-throttle nose-up limiter (`NewFlightIdleNoseUpLimit`) and related tuning guidance for preventing zero-throttle vertical pitch exploits.

## Troubleshooting

### Vehicles, models, textures, or sounds are missing

Verify that the MCHeli asset/content pack is installed with the expected `assets/mcheli/` structure. The code loads definitions from folders such as `assets/mcheli/helicopters`, `assets/mcheli/planes`, `assets/mcheli/ships`, `assets/mcheli/tanks`, `assets/mcheli/vehicles`, `assets/mcheli/weapons`, `assets/mcheli/hud`, `assets/mcheli/item`, and `assets/mcheli/throwable`.

### The config did not change in-game

Restart the game/server after editing `mcheli.cfg`, or use `/mcheli reconfig` for server-side reloadable settings. Client keybind changes may require reopening the game or reloading the client-side config.

### Commands say I do not have permission

Use an operator account or add `CommandPermission = commandName:PlayerName` entries to `mcheli.cfg`, then reload/restart. Permissions are per subcommand, not for the whole `/mcheli` tree.

### A build fails in a clean environment

This is an old ForgeGradle 1.x Minecraft 1.7.10 project. Use Java 8, run `./gradlew setupDecompWorkspace`, and ensure legacy dependency repositories are reachable.

## Resources

- CurseForge/update page from mod metadata: <https://www.curseforge.com/minecraft/mc-mods/mcheli-overdrive-loader-mod>
- Project website from mod metadata: <https://ragexprince.wordpress.com/>
- Original derivative source noted by the previous README: <https://github.com/RagexPrince683/MCH-defaultmaster>
- Current repository: this checkout (`MCH-mocmaster`)

If this project has an official Discord, Modrinth page, wiki, or current GitHub remote, add those URLs here once confirmed.

## Screenshots

Screenshots are not currently stored in this repository. Suggested screenshot topics for later documentation updates:

- Vehicle selection in creative tabs
- Drafting Table recipe screen
- Aircraft HUD in flight
- Server/admin command examples

## FAQ

### Is this the original MC Helicopter mod?

No. It is a fork/overhaul derived from the MCHeli code lineage, with additional systems and compatibility work.

### Does it work on modern Minecraft versions?

No. The current codebase targets Minecraft 1.7.10.

### Do clients need the mod on multiplayer servers?

Yes. Players need the mod and compatible assets to render and control vehicles correctly. Dedicated servers should also install it to simulate entities, recipes, commands, and config behavior.

### Can I disable terrain damage?

Yes. Start with `Explosion_DestroyBlock = false`, `Collision_DestroyBlock = false`, and related collision/breakable-block options in `config/mcheli.cfg`.

### Where are vehicles defined?

Vehicle and weapon content is data-driven through MCHeli asset folders under `assets/mcheli/`. This repository contains the Java implementation; a matching asset/content pack supplies most vehicle definitions, models, textures, HUDs, and sounds.
