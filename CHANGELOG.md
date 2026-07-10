# Changelog

## Config Reference Non-Bomber Gunner Bombsights Follow-up

- Added `HasBombSight = false` to the newly gunner-enabled fixed-wing config references that have fire-control/ballistic computers but are not equipped with dedicated bomber sights.

## Config Reference Plane Gunner Eligibility

- Added `EnableGunnerMode = true` to fixed-wing config references that have ballistic/fire-control computers or bomber roles so pilot gunner mode is available wherever the supporting criteria exist.

## Config Reference Non-Bomber Gunner Bombsights

- Disabled `HasBombSight` for non-bomber fixed-wing config references that use pilot gunner mode, including gunships, drones, fighters, transports, and reconnaissance aircraft, while confirming the AC-130 remains opted out via `EnableBombSight = false`.

## Config Reference Plane Carrier Eligibility

- Added explicit `CanMountShip` entries to every plane config reference. Oversized bombers, strategic aircraft, large transports, AWACS/tanker, and other large non-carrier aircraft are set to `false`; remaining plane references are set to `true` to preserve carrier rack compatibility.

## Rack payload capacity enforcement
- Enforced `MaximumExternalPayloadCapacity` against carried vehicle `Weight` for plane, helicopter, and tank rack mounting, rejecting overweight rack attempts with an in-game message.
- Disabled chain use for planes and now informs players that planes cannot use cargo hooks to transport cargo.

## Config Reference Max External Payload Capacities

- Added `MaximumExternalPayloadCapacity = {lbs}` entries to every active `AddRack` carrier/tow-capable vehicle config reference under `configreference/`, using each platform's maximum towing, external-lift, payload, or cargo capacity in pounds.

## Config Reference Vehicle Weights

- Added `Weight = {weight in lbs}` entries to every vehicle config reference under `configreference/` for aircraft, helicopters, ships, tanks, and static/ground vehicles using real-world curb, empty, combat, gross, or displacement weights as appropriate to the platform.

## Freelook Indicator GUI

- Added a visible `FREELOOK` indicator to the new plane overlay whenever the pilot is in regular freelook or the new third-person hold-freelook camera mode.
- Added `FREELOOK` to the new simple plane HUD warning stack so pilots using the compact HUD can still confirm freelook state.

## Bomb Reticle Camera and Sight Update
- Added a third-person bomber sight HUD hint that displays `Bomb sight: OFF Third Person` when the bomber sight popup is visible outside first person.
- Bomb reticle mode now forces the first-person gunner camera to look directly at the predicted bomb impact point while active.
- Replaced the bomber sight drawing with a large black bomb-sight reticle instead of reusing the green CCIP pipper.

## Seat Interaction Recovery Fix
- Fixed stale client-side seat occupants left behind by lag spikes or chunk reloads so vehicle seats become interactable again without requiring a relog.

## Unreleased

### Changed

- Added the `HasBombSight` / `EnableBombSight` / `EnableBomberSight` vehicle config boolean for disabling the first-person gunner bombsight on aircraft such as the AC-130 while leaving gunner mode available.
- Fixed the bombsight GUI gate to resolve plane config data before checking the new bombsight toggle.

- New UAV inventory handoff now warns operators at 10 seconds, 5 seconds, and timeout while they remain within 15 blocks of the station for refuel/rearm access, and stores/clears inventory immediately if they move farther away.

- Destroying a UAV station now unlinks the live UAV and returns any active operator to the station position instead of destroying the UAV and dismounting the player at the aircraft.
- Destroying a UAV station while its operator is controlling a linked New UAV now restores the operator's stored inventory immediately after returning them to the station.

- Active New UAVs now keep their linked UAV station chunk loaded while piloted so the station continue flow can resolve the tied drone outside spawn chunks.

- Fixed New UAV station pilot rendering so the station keeps showing the operator fake player after control transfers to an active New UAV.

- Fixed passenger seats becoming non-enterable after vehicles cross chunk load boundaries or lag spikes by recreating missing server seat entities on interaction, clearing stale client seat slots, and resyncing authoritative seat occupant IDs.

- Cached 3D vehicle item icon model rendering in OpenGL display lists so inventories, held items, and dropped item icons reuse compiled geometry instead of re-submitting full vehicle models every frame.
- Queued 3D vehicle item icon display-list builds in small face batches so scrolling large creative vehicle tabs compiles models gradually without single-model display-list spikes dropping FPS to zero.

- Fixed dispenser weapons using HBM mine block items such as `hbm:tile.mine_he` to place the mine block directly on impact when vanilla-style fake-player item use does not handle the block item reliably.

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
