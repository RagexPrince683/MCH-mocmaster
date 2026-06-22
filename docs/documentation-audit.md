# Documentation Audit Notes

This file records features/configuration discovered during the documentation pass and gaps that could not be completed confidently from the available source and repository contents.

## Undocumented features, commands, configs, or systems discovered

- Ships are a first-class vehicle category with loading, item registration, entity registration, renderer registration, and creative tab support.
- Client-only long-distance vehicle LOD rendering is configurable through `EnableAircraftLODRender`, `AircraftLODStartDistance`, and `AircraftLODFarDistance`.
- Multi-threaded model loading is enabled by default through `MultiThreadedModelLoading`.
- The mod registers a custom light block named `mcheli_lightblock` in addition to Drafting Table blocks.
- Three light weapons are registered from the same base light-weapon class: FIM-92 Stinger, FGM-148 Javelin, and RPG-7.
- Gunner spawn eggs/items include vs-monster, vs-player, and evil variants.
- `/mcheli showboundingbox` toggles `EnableDebugBoundingBox` in memory and broadcasts settings, but the save call is commented out.
- `/mcheli fill` includes an `override` mode in addition to `replace`, `destroy`, and `keep` tab-completion values.
- `IgnoreBulletHit` defaults include Flan's Mod bullet and grenade entity classes when the config list is empty.
- Several initialized config options are not written through the active generated config arrays, including rotation limits, gunner ranges, and no-break collision lists.

## Documentation gaps and limitations

- No current Git remote, official Discord, Modrinth page, wiki URL, or screenshot assets were present in the checkout, so the README only links confirmed URLs from existing metadata/README text.
- The repository does not include the full `assets/mcheli/` content pack, so individual vehicle names, stats, recipes, HUD layouts, sounds, and screenshots could not be documented comprehensively.
- Some advanced options (`delayrangeloader`, `bombletloader`, `placetimer`, `wrenchdropitem`, several rotation/gunner options) need deeper feature tracing or maintainer confirmation for user-facing explanations.
- The exact behavior of `sendss` and `modlist` depends on client packet handlers and UI flow; this pass documented their source-visible purpose without promising file upload/storage behavior.
- Numeric legacy item/block ID behavior can vary by Forge 1.7.10 pack state and ID map; users should validate in their own modpacks.
- Build verification may depend on legacy ForgeGradle/Maven repositories and bundled local jars.

## Source areas inspected

- Mod metadata and Forge declaration: `src/main/resources/mcmod.info`, `src/main/java/mcheli/MCH_MOD.java`
- Configuration defaults/read/write logic: `src/main/java/mcheli/MCH_Config.java`, `src/main/java/mcheli/MCH_ConfigPrm.java`
- Commands and permissions: `src/main/java/mcheli/command/MCH_Command.java`
- Client/server config loading and rendering hooks: `src/main/java/mcheli/MCH_CommonProxy.java`, `src/main/java/mcheli/MCH_ClientProxy.java`
- Build/development setup: `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`

## 2026-06-22 documentation overhaul pass

### Obsolete wording removed

- Replaced status language that framed implemented new aircraft, helicopter, camera, HUD, mobility, gravity, stall/AoA, and control-authority systems as not yet available.
- Preserved active-development language only for aircraft follow-camera refinement and mouse-aim controls, which remain configurable but unfinished.
- Removed duplicated fixed-wing derived-behavior and mouse-aim sections from the plane configuration guide.

### New documentation added or emphasized

- README now leads with MCHeli Overdrive-specific aircraft systems: new flight model, stall/AoA, mass/thrust, gravity, drag/energy retention, overspeed, dive behavior, ceilings, combat flaps, new helicopter model, hover assist/mode, rotor thrust/RPM, new HUD systems, camera controls, performance/refactor work, and legacy compatibility.
- Fixed-wing flight-model reference now describes the implemented opt-in path and current behavior rather than an alias-only refactor.
- Helicopter config reference now documents current new helicopter behavior for collective, rotor RPM/thrust, cyclic/tail authority, climb-rate caps, lateral/backward tuning, and hover-assist stabilization.
- Configuration references now call mouse aim an active-development feature instead of a completed control system.

### Inaccuracies corrected

- `UseNewHelicopterFlightModel` / `EnableNewHelicopterFlightModel` are documented as the active new helicopter opt-in gate, separate from legacy helicopter behavior.
- Helicopter values formerly labeled as deferred calculations are documented as current parser/runtime inputs when the new helicopter model is enabled.
- README feature summaries no longer primarily restate original MCHeli functionality before describing Overdrive-specific systems.
- Camera and mouse-aim documentation now explicitly states current limitations, disabled-by-default status where applicable, and global-not-per-plane mouse-aim configuration.

### Mismatches or limitations that remain

- The repository does not include the full external asset/content pack, so vehicle-by-vehicle configuration stats, HUD layouts, recipes, sounds, screenshots, and balance recommendations cannot be fully audited from this checkout alone.
- Camera and mouse-aim systems have many client configuration keys and in-game GUI controls; the documentation reflects source-visible defaults, but final gameplay recommendations should be revisited as those active-development features are refined.


## 2026-06-22 scope expansion

### Unique Overdrive features identified

- New fixed-wing flight model with AoA/stall, mass/thrust, gravity, drag/energy, flight ceilings, dive assist, combat flaps, and overspeed behavior.
- New helicopter flight model with rotor RPM/thrust, collective, cyclic/tail authority, hover assist, climb-rate caps, and lateral/backward tuning.
- Walkable moving ship decks using ship base/extra bounding boxes as carrier-capable support surfaces.
- Carrier and vehicle-on-vehicle rack interactions with `AddRack`, `RideRack`, launch assist, and temporary launch no-collision grace.
- Ship-based submarine diving mode with ascend/descend controls and bounded underwater vertical motion.
- Long-range vehicle LOD snapshots and model-only far rendering for aircraft, ships, tanks, and turrets.
- NewUAV JSON persistence, runtime UAV identity registry, and duplicate protection.
- Entity-info synchronization for multiplayer awareness systems.
- New camera, freelook, held look-ahead, active-development follow-camera, active-development mouse-aim, and new HUD overlay configuration.
- Expanded cross-category vehicle config framework with legacy/new-system opt-in behavior.

### New documentation added

- Added `docs/overdrive-features.md`, a project-wide feature overview explaining what each major Overdrive system does, why it exists, how it differs from original MCHeli, and the user-facing or pack-maker-facing configuration where applicable.
- Expanded README feature summaries and linked the new overview from the main documentation list.
- Expanded ship config documentation with current walkable-deck, carrier/rack, and submarine behavior.

### Existing documentation updated

- README now presents Overdrive as a modernization and expansion rather than a simple fork.
- Ship config docs now cover naval, carrier, vehicle-on-vehicle, and submarine systems in addition to parser keys.
- The prior documentation audit now records broader project-wide features instead of only aircraft/camera/config cleanup.

### Legacy or obsolete documentation removed

- Avoided README wording that primarily listed original MCHeli content categories before describing Overdrive-specific systems.
- Kept legacy content compatibility in a separate section so original MCHeli functionality is not confused with unique Overdrive functionality.

### Features discovered in code that were previously underdocumented

- Moving deck carry/correction logic for players standing on ship bounding boxes.
- Submarine dive state, HUD prompts, and networked ascend/descend control path.
- Rack launch assistance and no-collision grace for carrier launches.
- NewUAV JSON persistence file and duplicate-resolution registry.
- Server-to-client LOD snapshot packets and client model-only display path.
