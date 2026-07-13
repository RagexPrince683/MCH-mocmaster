## Bomber Sight Vertical Camera Lock

- Fixed first-person bomber sight tracking so the camera pitch is locked to the predicted bomb-impact angle, preventing freelook from moving above or below the calculated impact point while preserving existing CCIP prediction and horizontal tracking behavior.

## Plane CCIP Estimated Fallback Stability

- Fixed plane CCIP disappearing or randomly entering short grace-only rendering during steep dives, extreme pitch angles, high speed, unloaded chunks, or no-terrain-collision prediction gaps.
- Added a synthetic estimated impact fallback that keeps the CCIP marker visible from ballistic altitude, velocity, aircraft motion/orientation, bomb gravity, and acceleration data while preserving normal real-terrain hits when loaded terrain is available.
- Kept grace-mode pippers stable by reusing the last smoothed screen position if the cached impact cannot be projected for a frame.

## Plane CCIP Projection Diagnostics

- Added CCIP projection diagnostics for the plane debug overlay, including projection path, rejection reason, screen coordinates, winZ, camera distance, and camera depth.

## Plane CCIP Last Valid Impact Grace Cache

- Added a short normal-CCIP last-valid-impact grace cache so transient invalid predictions or projection gaps draw a dimmed, edge-clamped cached pipper instead of hiding the cue.
- Added CCIP debug output for `ccipGrace=true` while cached impact rendering is active.

## CCIP Unloaded Chunk Fallback Stability

- Added CCIP prediction diagnostics for unloaded chunk coordinates, fallback reason, fallback target height, and whether an impact came from real terrain or synthetic fallback.
- Improved unloaded chunk fallback to use nearby loaded terrain height when available while keeping a deterministic Y=0 fallback for truly unloaded areas.
- Added short GUI hysteresis so transient invalid/unloaded fallback predictions reuse the last stable impact briefly, while preserving fallback diagnostics in the debug overlay.

# Changelog

## CCIP Far-Plane Projection Fallback
- Changed plane CCIP projection so `gluProject` points beyond the active render far plane fall back to camera-vector projection/clamping instead of suppressing an otherwise valid impact solution.
- Added CCIP debug projection metadata for project mode, `winZ`, and far-plane rejection state.

## CCIP Edge-Clamped Offscreen Indicator
- Added an edge-clamped fallback CCIP projection for valid ballistic impacts that are in front of the camera but outside the viewport or beyond the exact projection far clip.
- Added CCIP debug projection status text for exact, fallback, clamped, behind-camera, and invalid pipper projection states.

## Bomber Sight Local Reticle Culling Fix
- Fixed the `HasBombSight` first-person bomber sight reticle so it renders as a local camera sight at screen center during bomber-sight mode instead of being culled by the distant predicted impact projection. CCIP projection and culling behavior are unchanged.

## BOMBER SIGHT-only Unloaded Prediction Gap Camera Snap Fix
- Fixed first-person bomber sight camera snap so temporary prediction gaps, such as unloaded terrain during bombing runs, no longer stop forced sight tracking. The fallback is scoped only to bomber sight camera forcing and does not alter CCIP reticle rendering or third-person CCIP behavior.

## CCIP-only Unloaded Chunk Fallback
- Fixed plane CCIP prediction so reaching unloaded terrain immediately returns a valid unloaded-chunk fallback impact, preserving CCIP debug fallback metadata without relying on bomber-sight camera forcing.

## Bomb Sight Unloaded Chunk Fallback
- Kept the plane CCIP/bomb-sight reticle rendering when the predicted bomb path reaches unloaded or far-away chunks by falling back to a non-mutating ballistic ground projection instead of treating the missing chunk ray trace as no impact.

## Debug-gated spam logging
- Gated noisy startup, item registration, ore dictionary confirmation, language registration, reload, and auto-ore diagnostic logs behind `McHeliOutputDebugLog` while preserving normal startup milestones, model completion checks, warnings, and errors.

## MQ-8B Fire Scout Speed Correction
- Slowed the MQ-8B Fire Scout config reference speed so it no longer outruns the MQ-9 Reaper, matching the real-world MQ-8B/MQ-9A speed relationship.

## Config Reference Fl 282 Speed Nerf
- Reduced the Flettner Fl 282 config reference top-speed and forward rotor-thrust tuning so the ultralight helicopter no longer outruns its intended scout role.

## Tank wheel grass trampling
- Added server-side tank wheel trampling so each simulated `SetWheelPos` contact point turns grass blocks beneath moving tank wheels into dirt.

## Config Reference Weight Audit
- Corrected config reference vehicle weights to integer pounds using DisplayName/AddDisplayName identification and category-appropriate real-world weight definitions.
- Added a config weight plausibility validator for cars, motorcycles, consumer drones, helicopters, tanks/armored vehicles, ships, aircraft, static weapons, trailers, and equipment.

## Vehicle tooltip floating indicator
- Added a vehicle item tooltip line for configs with `Float = true` so plane, helicopter, tank, and turret/static-vehicle descriptions show when the vehicle floats on water.

## Cargo Paradrop Weight Limit
- Added a 34,800 lb cargo airdrop limit so rack-dropped vehicles over the historical maximum cargo airdrop weight are released without spawning a paradrop parachute.
- Added the 34,800 lb cargo airdrop limit to the parachute item tooltip.

## Instant Turret Placement
- Turret items now deploy immediately on right-click after the normal placement validation, bypassing vehicle hold-to-deploy timer setup, ready messages, and release-delay checks.

## Clear UAV placement guidance
- Vehicle item tooltips now identify small versus large UAVs and state whether to use a UAV Station or Portable UAV Controller.
- Direct right-click placement for UAV vehicle items now stops before hold-to-deploy starts and immediately tells players to use the correct UAV controller instead of failing at deployment time.

## Tank and Turret Fall Damage
- Fixed tank and turret fall damage tracking when the shared vehicle update loop clears vanilla `fallDistance`; landings now calculate server-side impact damage from drop height, gravity, downward acceleration, landing speed, and current health.
- Added server-side fall damage for tanks and turrets after drops greater than three blocks, using fall damage sources so armor and existing vehicle damage handling apply consistently.

## Vehicle tooltip payload stats
- Added vehicle item tooltip lines for non-zero configured `Weight` and `MaximumExternalPayloadCapacity` values so players can see vehicle weight and max payload capacity before spawning.

## Rack payload capacity cumulative enforcement
- Updated rack mounting so `MaximumExternalPayloadCapacity` is checked against the total `Weight` already mounted on occupied rack seats plus the incoming vehicle, preventing the remaining payload capacity from ever going negative.

## Normal Bomb Gravity Calibration
- Recalibrated normal `Type = Bomb` ballistic weapon references and the bomb default gravity to use standard Earth gravity in MCHeli's 20 Hz per-tick units (`9.80665 / 20^2 = 0.02452` blocks/tick²), while leaving glide weapons, mines, fuel tanks, dispensers, and other special payloads on their custom tuning.

## Guided Bomb Gravity Calibration
- Recalibrated conventional guided-bomb config references to use real Earth gravity converted to MCHeli's 20 Hz per-tick velocity units (`9.80665 / 20^2 = 0.02452` blocks/tick²), replacing glidey `-0.001` to `-0.02` gravity values on GBU/LJDAM/KAB-style bomb drops while leaving purpose-built glide/SDB/UMPK weapons untouched.


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

## Engine Waterboarding
- Added waterboarding behavior for tank, plane, and helicopter engines: submerged non-floating vehicles now have throttle forced to zero while waterlogged, and recurring water damage stops once the configured `EngineShutdownThreshold` health percentage is reached instead of always continuing to destruction.
