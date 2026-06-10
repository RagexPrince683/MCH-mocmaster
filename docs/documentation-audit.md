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
