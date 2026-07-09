# Changelog

## Bomb Reticle Camera and Sight Update
- Bomb reticle mode now forces the first-person gunner camera to look directly at the predicted bomb impact point while active.
- Replaced the bomber sight drawing with a large black bomb-sight reticle instead of reusing the green CCIP pipper.

## Unreleased

### Changed

- Added the `EnableHandheld` config toggle for disabling Stinger, Javelin, RPG, and matching ammunition crafting recipes.

- Fixed debug extra bounding-box rendering to use the vehicle model transform before box offsets, so boxes roll with the vehicle instead of staying visually upright while only their centers move.

- Fixed plane free-look steering so A/D turn input is still sent and applied while free look is active in both mouse flight-sim and regular control modes.

- Added a first-person bomber reticle mode for plane gunner view, toggled by the new `KeyBombReticleMode` keybind, using the existing ballistic bomb-impact predictor for accurate impact placement. The key now appears in the in-game key binding list, and its HUD hint is shown at bottom-center to avoid overlapping flap prompts.

- Added `/mcheli enablenukes [true|false]` for MCHeli nuclear-weapon gating, with usage/status output when run without an argument, colored broadcast messages, Wither spawn sound feedback, and status/updates mirrored to HBM NTM's `/ntmenablenukes` command when that mod command is available.

- Added the `CanMountShip` vehicle config key so pack makers can prevent oversized aircraft, such as heavy bombers, from mounting ship/carrier racks while preserving existing behavior by default.

- Added chain towing weight checks for configured vehicles with `MaximumExternalPayloadCapacity` and `Weight` content-pack keys, including an in-game warning when a vehicle exceeds the towing vehicle's payload capacity.

- Added 3D vehicle item rendering so placeable helicopters, planes, ships, tanks, and turret/static vehicles use their loaded vehicle models in inventories, held views, and dropped item form instead of relying on flat item icons. Added `Override3DItemIcon`, per-type global scale settings, and per-vehicle `Enable3DItemIcon` / `ItemIconScaleFactor` content-pack keys.

- Replaced vehicle extra bounding-box narrow-phase checks with oriented bounding boxes while retaining enclosing AABBs for vanilla broad-phase compatibility. Added optional rectangular `depth` syntax for diagonal/rotated OBB footprints.

### Documentation

- Rewrote `README.md` with a modern project overview, feature list, installation steps, compatibility/client-server requirements, configuration overview, command examples, troubleshooting, FAQ, and confirmed resource links.
- Added `docs/getting-started.md` for new-player installation, asset layout, creative tabs, basic controls, and first configuration changes.
- Added `docs/configuration.md` with source-verified configuration defaults, keybind defaults, damage/ignore-list behavior, and hidden/advanced config notes.
- Added `docs/commands.md` with source-verified `/mcheli` command syntax, permission model, examples, and safety notes.
- Added `docs/server-administration.md` with server installation, safe survival defaults, permission guidance, runtime reloads, diagnostics, and performance notes.
- Added `docs/documentation-audit.md` listing discovered undocumented systems and remaining documentation gaps.

## CCIP Static Bomb Reticle Fix
- Fixed the plane CCIP pipper so it draws directly at the predicted bomb impact projection instead of smoothing toward a look-following cursor.
- Documented that CCIP is projected in the aircraft body frame and is not driven by player freelook or mouse aim.
