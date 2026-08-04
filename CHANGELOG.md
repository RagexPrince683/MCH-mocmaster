# Toggleable Active Radar

Added pilot-controlled, persistent active radar, capability-aware scanning, synchronized emitter
  state, and RWR filtering so disabled or unequipped vehicles do not emit signatures.
- Added `KeyRadar` and documented `HasRadar`, including compatibility with explicit `RadarType` and
  enabled `EnableEntityRadar` vehicle configurations.

# Finite cargo-backed service vehicles (PR pending)

- Added opt-in `GasPump`, `AmmoLoader`, and `ForceY` shared vehicle configuration while retaining infinite legacy range-only suppliers.
- Added persistent, overflow-safe service fuel reserves, cargo fuel-can draining, deterministic target ordering, and transactional configured ammunition-package consumption.
- Documented finite support inventory, range, fuel conversion, and same-level requirements.

# Prevent Small-Explosion Knockback (PR pending)

- Prevented entity explosions below size `3.0F` from adding MCHeli or vanilla damage knockback while preserving each affected entity's existing motion.
- Kept damage, hit reporting, effects, fire, and block behavior unchanged, and retained existing knockback behavior at or above the threshold for normal and underwater explosions.

# PR pending - Pre-contact Tank Physical-Hull Step Solver

- Replaced selected-riser collision permission with a tank-only, scratch-geometry route planner that stops before first contact, lifts clear, traverses normally, and lands on the real collision-shape top.
- Kept normal physical-hull collision active on every route segment and retained safely clipped normal movement whenever a complete step route cannot be validated.
- Added complete solver regressions using the M1A2's ten physical hull boxes and a shorter comparison hull, including block, slab, stair, wall, pillar, ceiling, occupancy, steering, and stability cases.

# Configurable NEI Vehicle Ammunition Handler (PR pending)

- Added the `EnableNEIHandler` boolean startup option, defaulting to `true`, to allow clients to prevent the optional NEI vehicle ammunition recipe and usage handler from registering.

# Complete deterministic texture/UV repair validation (PR pending)

- Replaced unsupported per-channel `putchannel` mutation with split/dilate/merge RGB bleeding that preserves alpha byte-for-byte across every pass.
- Corrected premultiplied-alpha resizing, removed deprecated Pillow pixel access, and expanded command and image edge-case coverage.
- Made empty, unresolved, missing-source, and hash-failure repair states explicit and fail-safe, and added ranked suspicious-asset CSV output.

# Texture downscale and MQO UV audit tooling (PR pending)

- Added a deterministic, manifest-driven Python audit/repair/verification tool for runtime-selected vehicle model and texture pairs.
- Added parser, affine UV conversion, premultiplied-alpha resizing, transparent-edge bleed, alpha-coverage, and deterministic-output tests.
- Documented that normalized MQO UVs do not change under proportional resizing and that validated pre-downscale sources remain unavailable, so no binary texture or model asset was modified.

# Targeted vehicle configuration live reload (PR pending)

- Make the Development GUI vehicle button reload one server-validated controlled vehicle definition instead of scanning and rebuilding every matching loaded entity.
- Resolve the definition through addon/development/classpath/JAR precedence, atomically replace one manager entry, and preserve the previous snapshot on missing or invalid input.
- Reload only the selected vehicle's model dependencies and related item, LOD, and turret-pop caches while preserving seats, riders, identity, and runtime state.
- Separate the vehicle, weapon, and HUD buttons and retain `/mcheli reload` as the complete expensive asset reload.

# Fix AA missile target validation and server null target handling (PR pending)

- Reject null, dead, self, cross-world, unloaded, stale, and vehicle-occupant targets through the shared client/server guidance validator.
- Keep ordinary players and mobs in the Ground domain regardless of jumping or falling, while classifying aircraft separately by real collision contact.
- Clear stale client entity options before lock acquisition and reject zero or invalid target IDs before server missile creation, sound, ammunition, or cooldown handling.
- Audit all runtime and reference AA missile assets; no weapon configuration changes were required.

# Forge 1.7.10 development asset live reload (PR pending)

- Make `/mcheli reload` prefer external addons and the editable `src/main/resources/assets/mcheli` tree over generated classpath output and development JARs.
- Refresh configuration, HUD, model, texture, and sound resources on the client thread while preserving loaded vehicle seats and persistent state.
- Add deterministic source-selection coverage and document the development verification workflow.

# Missile target classification and locking correction (PR pending)

- Classify planes and helicopters by deterministic full-footprint ground contact while retaining stable domains for tanks, turrets, UAV stations, ships, and compatible external vehicles.
- Share domain and countermeasure eligibility across acquisition, server launch validation, active scanning, and in-flight guidance; reject forged or stale wrong-domain target IDs.
- Select the nearest eligible lock target, expose the last completed lock correctly, and correct AA/AT proximity-fuze squared-distance comparisons.
- Audit all missile weapon assets without changing their configuration or loadouts.

# Catastrophic tank turret-pop correction (PR pending)

- Detach the exact `$turret` model group with its configured weapon children, preserving the destruction-time turret yaw and gun elevation in synchronized/NBT state.
- Add server-authoritative swept solid-block landing, unloaded-chunk protection, and a client-created dark smoke trail following the synchronized detached turret.
- Keep the canonical turret suppressed in both close and LOD attached weapon passes while leaving unrelated turreted stations visible.

# Audit tank laser-warning receiver configuration (PR pending)

- Added an explicit `LWR` capability value to every tank configuration, enabling it only for exact vehicle variants with sufficiently reliable laser-warning receiver evidence.

# Tank LWR alert opt-in (PR pending)

- Added the shared `LWR` vehicle option, defaulting to false, and require tanks to enable it before direct, UAV, packet, or client-tick warning audio can play.
- Decoupled tank warning detection and audio from flare availability without changing flare deployment or missile-destruction behavior; non-tank alert behavior remains unchanged.

# Optional NEI Vehicle Ammunition Compatibility (PR pending)

- Added an optional, client-only NotEnoughItems category relating complete weapon reload inputs to helicopters, planes, ships, tanks, vehicles, and turrets.
- Rebuild NEI query results from live vehicle and weapon definitions so `/mcheli reload` is reflected without duplicate handler registration.

# Replay Mod playback camera ownership compatibility (PR pending)

- Detect Replay Mod's playback-only `CameraEntity` without linking Replay Mod, release stale MCHeli chase/view state, and leave `renderViewEntity`, camera transforms, FOV, and view-mode ownership to Replay Mod.
- Suppress local MCHeli vehicle controls, outbound control packets, recoil/bombsight mutations, and mounted-vehicle HUDs during playback while keeping passive vehicle state updates and rendering active.
- Restore normal MCHeli camera handling automatically after playback ends; Replay Mod installation and live recording do not activate the compatibility path.

# Tank countermeasure smoke thermal visibility (PR pending)

- Keep smoke emitted by vehicle smoke weapons and countermeasure flares visible as cold smoke in thermal vision, without changing its normal particle lifetime, movement, scale, opacity, or fade.
- Continue filtering unrelated MCHeli and vanilla smoke particles from the thermal render pass.

# Client-side thermal particle visibility fix (PR pending)

- Filter MCHeli smoke plus vanilla normal and large smoke at render time while the local active camera is in thermal mode.
- Preserve particle spawning, lifetime, motion, networking, and world state so hidden smoke becomes visible again immediately after thermal vision ends.
- Keep fire, flame, flashes, tracers, sparks, flares, debris, dust, and water particles unchanged.

# Normal vehicle respawn ordering (PR pending)

- Replaced failed tracker resend/repair logic from PRs #593–#598 with watched-chunk normal spawning, unwatched-chunk render-only LOD snapshots, and bounded observer-directed mount graphs.
- Preserved UAV/NewUAV force-spawn behavior and documented that the required two-client graphical acceptance matrix remains pending.

## PR - Prevent tracker reflection from crashing respawn repair

- Replace the crashing mixed MCP/SRG reflection lookup with a cached, hierarchy-aware, type-validated `IntHashMap` resolver and bounded diagnostics.
- Use Forge's public watcher view and its single normal remove/re-add path, count only initiated parent resends, retry unavailable entries every five ticks, and isolate parent/dependent failures.
- Preserve PR #597's corrected client identity, readiness, classification, chunk gating, timeout, resend limits, and UAV exclusions; full two-client graphical verification remains outstanding.

## PR #597 - Correct Normal-Vehicle Respawn Repair Identity

- Treat Forge numeric entity IDs plus MCHeli type, dimension, bounded position, and synchronized non-empty common ID as vehicle identity; keep vanilla UUID comparisons diagnostic.
- Gate client readiness on the replacement player/world runtime objects and defer bounded parent-first tracker resends until the replacement watches the vehicle chunk.
- Preserve age-60 probe results through an age-80 timeout and clear generation-scoped client resolution history on world unload.
- Compilation passed; the required two-client graphical runtime verification remains outstanding, so runtime success is not claimed.

## PR #595 - Forge 1.7.10 Respawn Probe API Corrections

- Corrected PR #595's incompatible `EntityTrackerEntry.removeFromTrackedPlayers` call to Forge 1.7.10's `EntityTrackerEntry.removeFromWatchingList` while retaining the normal `tryStartWachingThis` retrack path.
- Replaced the unavailable `MinecraftServer.func_152344_a` scheduler with a thread-safe `ConcurrentLinkedQueue` processed during every `ServerTickEvent` end phase.
- Compilation verifies the API corrections; the dedicated-server, two-client runtime procedure remains required before claiming the invisible-vehicle issue is fixed.

## PR #594 - Verify and Repair Respawn Tracking

- Replaced PR #593's unverified one-tick global tracker reset with a 40-tick, chunk-ready, per-player targeted verification and repair.
- Added bounded, always-visible `[MCH-RESPAWN-AUDIT]` server and client lifecycle evidence while preserving the 200-block LOD exclusion and UAV behavior.
- Corrected the respawn tracking documentation to distinguish verified vanilla lifecycle behavior from the former stale-watcher theory.

## PR #593 - Restore Real Vehicle Tracking After Respawn

- Rebuild the replacement server player's vanilla tracker membership once, after Forge's respawn or dimension-change lifecycle has installed the player and its watched chunks.
- Remove the replacement player's stale watcher generation before the rebuild so existing vehicle, seat, passenger-seat, and hitbox spawn packets are not suppressed by Minecraft 1.7.10's entity-ID-based player equality.
- Keep normal-range LOD snapshots suppressed and preserve UUID-validated mount correction, dead-rider cleanup, and all UAV and NewUAV paths.
- Add debug-only `[MCH-RESPAWN-TRACK]` transition and failure diagnostics, including player/world identity, dimension, chunk, and nearby normal-vehicle count.

# Fix Tank LOD Running-Gear Rendering

- Restored track rollers, crawler tracks, and wheels in both tracked-entity and entity-free snapshot tank LOD passes without restoring the monolithic common-part renderer.
- Added fixed two-side running-gear snapshot state and wrap-aware interpolation for independent forward, reverse, and turning track movement.
- Shared the entity and snapshot OpenGL transforms while preserving tank weapon poses, helicopter rotors, skin overlays, and legacy monolithic model fallback behavior.

# Snapshot-only Tank Turret and Helicopter Rotor Animation

- Extended distant-vehicle packets with bounded, index-stable tank weapon poses and helicopter rotor phase, per-tick angular change, and folded state.
- Changed the entity-free snapshot renderer to separate `$body` from animated tank weapons and helicopter rotor blades while preserving external models, embedded named parts, skin overlays, and the monolithic-model fallback.
- Added per-display weapon interpolation and continuously advanced, smoothly corrected rotor phases so one-second snapshots do not alias fast rotors.
- Documented that the earlier real-entity LOD change could not affect vehicles which had already left client tracking.

## Fix Tank LOD Part Transforms

- Made the tank far-model pass render all configured dynamic named parts through the same common-part renderers used by the full-detail pass.
- Preserved the interpolated hull transform as the parent of turret, weapon, recoil, hatch, wheel, track, suspension, and nested weapon-part transforms while retaining per-part matrix isolation and optional-part handling.

## Multiplayer Vehicle Yaw and Running-Gear Synchronization

- Removed the tank's extra per-packet interpolation delay and made remote hull yaw corrections follow the shortest wrapped angle over the server-provided interpolation window.
- Derived wheel, track-roller, and crawler-track motion from synchronized vehicle displacement and wrapped hull rotation instead of delayed client control flags and throttle.
- Corrected crawler-track render interpolation to use the frame partial tick and the shortest path across phase wrap boundaries.

## Configurable MCH_Lib Logging

- Added independent normal and verbose MCHLib logging controls backed by standard config properties.
- Routed MCHLib messages through Forge's Log4j logger with safe printf formatting and side/world labels.
- Preserved legacy `McHeliOutputDebugLog` configs while migrating rewritten files to the new debug property.
- Loaded the configuration before configurable startup and content-loading messages.

## Player-Style Villager and Living Damage Factors

- Added inheritance-aware weapon damage-factor matching, with explicit aircraft and vehicle types taking priority over player and living fallbacks.
- Added `other` and `others` weapon factor aliases for non-player, non-villager living entities; without one, living targets inherit the player factor.
- Routed villagers through global player damage configuration and removed duplicate direct-hit player-factor multiplication.

## Global Artillery Range Modifier

- Added the reloadable, minimum-clamped `ArtilleryRangeModifier` global setting and the opt-in `UseGlobalArtilleryRangeModifier` weapon text key.
- Kept existing weapons unchanged by default while using one effective launch acceleration for projectile spawning, high-speed projectile stepping, mortar distance output, and client ballistic prediction.
- Documented that mortar-distance display is independent of modifier eligibility and that vehicles do not receive a separate artillery modifier.

## Creative Instant Vehicle Placement

- Made all normal non-UAV vehicle items place instantly for Creative-mode players after the existing target, world-age, sponge-only, entity, and collision checks.
- Preserved the configured hold-to-deploy timer for Survival-mode players and the existing instant-placement behavior of vehicle types such as turrets.
- Updated the player and configuration documentation to distinguish Creative and Survival placement behavior.

## Fix local vehicle appearing on RWR

- Excluded the locally controlled aircraft from RWR contacts by synchronized entity ID before applying contact type or display-distance filters.
- Reused the shared direct-ride, seat-parent, and UAV-control aircraft resolution without changing shared entity synchronization or BVR tracking.

## Changed

- Added shared Air, Ground, Surface, Underwater, and Unknown target-domain classification for missile guidance.

# Fix missile target-domain classification

Added shared Air, Ground, Surface, Underwater, and Unknown target-domain classification for missile guidance.
- Made MCHeli planes and helicopters stable air-role targets; tanks, turrets, and ground stations stable ground-role targets; and ships stable surface/underwater-role targets.
- Reused the shared classification for initial locks, continued locks, active missile scans, and AA missile terminal validation.
- Kept flare/chaff filtering, manual/TV/laser guidance, custom lock checkers, water permissions, and missile-lock permissions intact.
- Added a repository-wide AT/AS/TV missile configuration audit and documented configurations needing manual review.


## New Plane Simple HUD Coordinates

- Added signed, one-decimal X, Y, and Z aircraft coordinates directly below altitude in the new plane simple HUD.
- Made the simple HUD vertical clamp use the rendered main and warning panel heights so the expanded stack remains on screen at small GUI resolutions.

## Real-Time Dismount Countdown

- Measure the three-second MCHeli dismount hold with Java's monotonic clock so low client TPS no longer extends the real-world wait.
- Show the synchronized `3s`, `2s`, and `1s` remaining hold time beside the configured Sneak key in the vehicle HUD.

## Three-Second MCHeli Dismount

- Require pilots and passengers to hold the configured Minecraft Sneak key for 60 continuous client ticks before requesting a server-authoritative dismount.
- Prevent vanilla's mounted-player Sneak handling from dismounting early while leaving non-MCHeli mounts and unmounted sneaking unchanged.

## PR #568 - Binary-density Smoke Reconstruction

- Confirmed that PR #567 enlarged the bundled texture's binary-alpha density samples before a zero-skipping blur, producing block-shaped alpha in the generated atlas even though the renderer correctly bound it.
- Reconstruct each 8 by 8 frame independently by padding and Gaussian-blurring its low-resolution density field (radius 2, sigma 1.0), globally normalizing all eight frames, applying gamma 0.8, and only then bilinearly scaling into the 288 by 36 atlas.
- Added opt-in generated-atlas dumping with `-Dmcheli.debugParticleAtlas=true`, reconstruction diagnostics, clamped half-texel frame UVs, and image-only regressions for source alpha, soft gaps, global fade, transparent gutters, frame isolation, and clean transparent RGB.

## Fix Reconstructed Smoke Particle Backgrounds

- Removed reconstruction haze by scaling per-frame alpha coverage instead of bicubic ARGB, applying a silhouette-limited blur, discarding alpha at or below 4/255, and renormalizing the retained soft coverage.
- Increased each reconstructed frame's transparent gutter to two pixels, forced content edges and zero-alpha RGB transparent, and sampled the complete cell with half-texel UV insets.
- Preserved source-alpha blending while adding a 1/255 alpha-test guard and restoring all fixed-function state changed by standalone smoke rendering.
- Added image-only regressions for the bundled 64 by 8 smoke texture, transparent borders/corners, soft coverage, frame isolation, edge-touching coverage, and transparent RGB.

## PR #566 - Soft Particle Texture Runtime Reconstruction

- Kept the crunched eight-frame smoke asset and added one-time, client-side runtime reconstruction of soft alpha edges for undersized or binary-alpha resource-pack textures.
- Added isolated-frame bicubic scaling, alpha/luminance reconstruction, blur, transparent gutters, inset UVs, linear filtering, and resource-reload-safe dynamic texture replacement for smoke and custom splash particles.
- Added image-only regression coverage for the current 64 by 8 atlas, frame isolation, alpha preservation, direct-use bypass, invalid widths, and missing resources.

## PR #564 - Drafting Table Description Textures

- Fixed drafting table description bindings so classpath and addon existence checks resolve the same texture path used by Minecraft's resource manager.
- Preserved numbered description pages, addon overrides, and the intended description crop while supporting textures with dimensions other than 512 by 512 pixels.
- Added one-time warnings for unresolved description pages instead of adding textures that render as the missing-texture checkerboard.

## Hold Shift Vehicle Dismount

- Changed vehicle dismounting to require holding Left Shift continuously for three seconds, preventing accidental exits from pilot seats.
- Updated the in-game vehicle control hint and getting-started control reference to document the hold-to-dismount behavior.

## Angelica Dynamic Vehicle Part Render Compatibility

- Added an Angelica-only Tessellator compatibility path for dynamically transformed MCHeli model parts so per-part `glPushMatrix`/translate/rotate transforms are consumed by Angelica's fixed-function state emulation.
- Kept normal static whole-model/body rendering on the existing VBO path and preserved the existing non-Angelica dynamic part path.
- Added opt-in one-shot diagnostics for Angelica dynamic part path selection via `-Dmcheli.debugAngelicaDynamicPartRender=true`.

## Bomb Sight No-Terrain Stability

- Fixed first-person bomb-sight camera forcing so no-terrain predictions use a stable ballistic target angle instead of bobbing with nearby fallback terrain heights.
- Kept the bomb sight continuously visible by deriving fallback target direction from aircraft motion and the predicted bomb solution when terrain cannot be detected.

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

# Missile lock target classification

- Keep client-side smoke and visual flare emitters out of missile lock candidate lists.
- Restrict heat guidance to active flare countermeasures and radar guidance to active chaff,
  including revalidation while a weapon or active-radar missile continues tracking.
## Fix Active Protection System reliability

- Made APS activation, timers, threat selection, interception, and sounds server-authoritative.
- Added typed MCHeli projectile filtering, shooter/vehicle/seat ownership checks, approach and projected-path checks, and once-only harmless interception effects.
- Preserved legacy `APSRange = 100` Iron Curtain content while limiting its protection to projectile and explosion damage.
- Enabled the Merkava Mk.4 Barak APS with an explicit active duration, cooldown, and range.
- Made optional FMUR reflection cached and quiet when FMUR is absent or incompatible.
# Server-authoritative hard-kill APS

- Replaced the timed protection bubble and hidden Iron Curtain immunity with a five-state automatic hard-kill APS.
- Added native projectile interception, closest-approach prediction, finite ammunition persistence/refill, state/effect packets, HUD values, and `APSInterceptable` weapon overrides.
- Removed FMUR/Flan's Mod reflection integration and updated Merkava and APS configuration documentation.

## Fix `/mcheli reload` concurrency crash

- Reload manager data into ordered, private snapshots and publish it atomically only after successful parsing.
- Preserve registered items, runtime item IDs, and reusable vehicle/throwable models across matching definitions.
- Schedule client reload, model, HUD, sound, renderer, and OpenGL cache work on the Minecraft client thread.
- Clear stale vehicle item display lists and pending model builds during the client reload.
- Rebuild the shared vehicle registry from current manager snapshots without exposing partially loaded data.
# Persistent server-authoritative vehicle access locks

- Added placing-player ownership and legacy pilot claiming for player-ridable vehicles.
- Added an operator-aware, persistent entry lock toggled by the pilot with O.
- Added synchronized lock HUD state and owner/lock entity and item NBT data.
# Fix vehicle LOD and mount synchronization

* Fixed aircraft LOD selection retaining render state across local-player death, respawn, world changes, and entity retracking.
* Restored normal vehicle and passenger-seat mount relationships with an authoritative server notification and a bounded client retry when tracking packets arrive out of order.
* Kept UAV and NewUAV mounting paths excluded from the new normal-vehicle correction flow.

# Fix post-respawn vehicle tracking and mount generations

* Removed the LOD-only reflection change to vanilla's global entity-tracker range; packet snapshots now begin outside MCHeli's normal 200-block vehicle tracking range.
* Made snapshot handoff use the server vehicle UUID, preventing a reused entity ID from hiding an otherwise valid distant snapshot.
* Added UUID- and sequence-validated normal-vehicle attach/detach corrections, bounded retries that contain IDs rather than entity references, and invalidation when the client player or world instance changes.
* Cleared dead-player pilot and passenger back-references at the death lifecycle event while preserving UAV and NewUAV remote-control paths.

## Vehicle LOD named-part completion

- Restored tracked and snapshot landing-gear rendering for separable plane models, including reverse, hatch, secondary-axis, and sliding gear definitions.
- Added continuous snapshot propeller animation plus nozzle/nacelle, wing, pylon, and generic weapon rendering for planes and ships.
- Added bounded, depth-first stationary-turret pose snapshots and recursive snapshot rendering for yaw, pitch, barrel rotation, recoil, cooldown visibility, and nested parts.
- Resolved turret snapshot textures from each turret info's legacy `vehicles` or newer `turrets` asset directory.

# Client-confirmed normal vehicle respawn repair

* Replaced watcher-only respawn success with bounded server-to-client probes and detailed client-to-server resolution evidence.
* Targetedly resend only missing normal parents and then their dependents, with two-attempt and 60-tick limits; UAV paths remain unchanged.
# Fix targeted vehicle configuration reload

- Match client and server vehicles by their synchronized entity ID instead of comparing side-local entity UUIDs.
- Close the development GUI as soon as a targeted request is sent, while a client-side pending request tracks the response, timeout, and world lifetime.
- Reload and apply only the selected definition and its models, preserving runtime vehicle state and rejecting seat-count changes before publication.
# PR: Restore full-block tank climbing

- Fixed physical hull step-up validation so an existing climbable riser contact may transition
  from a side-face SAT contact to a top-face contact while the hull moves vertically toward
  clearance, without weakening elevated horizontal, wall, or ceiling collision checks.
- Added regression coverage using the Abrams and Toyota Hilux physical hull dimensions, full and
  diagonal segmented climbs, a stable following tick, tall/stacked obstacles, ceilings, and
  partial-height collision shapes.
# PR: Scope tank hull phasing to validated step risers

- Record the exact physical hull and block collision shape that activates an Abrams step attempt,
  and permit that contact only during the upward and top-clearing portions of the raised route.
- Keep normal movement, rotation, tall obstacles, ceilings, adjacent shapes, and failed raised
  routes subject to ordinary physical hull collision.
- Add deterministic Abrams route-policy coverage using its configured `StepHeight` and front hull.
