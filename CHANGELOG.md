# Fix Mounted Pilot Seat Height Regression (PR pending)

- Restored the pilot rider model to the configured, transformed seat anchor without adding the rider's `yOffset` twice.
- Kept the rider's `yOffset` unchanged so Forge maintains a consistent `posY` and bounding-box anchor relationship.
- Added pilot and passenger mounted-position diagnostics behind the existing MCHeli debug logging control.

# Rename Decompiled Local Variables (PR pending)

- Replaced numbered decompiler-style local variables and parameters throughout maintained Java sources with purpose-based names.
- Renamed synthetic enhanced-loop array, length, and index locals and removed a stale commented duplicate wheel implementation.
- Preserved gameplay, packet, rendering, model-loading, and Forge 1.7.10 behavior while improving source readability.

# Fix Lazy Model Loading Regression (PR pending)

- Fixed MQO parsing after compact geometry finalization and made model groups safe both before and after finalization.
- Separated case-insensitive model cache keys from case-preserving resource paths across loading, rendering, lookup, and reload paths.
- Restored OpenGL buffer and client-array state after VBO rendering, including failed VBO attempts.

# Fix Throwable Model Registration Compile Failure (PR pending)

- Restored the throwable model-loading loop with a scoped iterator and descriptive throwable-info variable.
- Renamed the remaining understandable numbered exception variable in `MCH_ClientProxy`.

# Optimize Lazy Model Geometry Loading (PR pending)

- Replaced retained per-face model graphs with group-sized primitive geometry and face-boundary arrays.
- Changed vehicle meshes to load on first entity or item-renderer use instead of eagerly parsing every vehicle during startup.
- Canonicalized and atomically cached model requests, preserved the previous model after failed targeted reloads, and retired unsafe worker-thread vehicle registration.
- Updated UV repair to mutate compact geometry and defer VBO recreation to the render thread.
- Added disabled-by-default model loading diagnostics through `-Dmcheli.debugModelLoading=true`.

# Fix Three-Second MCHeli Dismount Hold (#673)

- Replaced tick/event-order-dependent Sneak mutation with one monotonic, physical-input hold state machine for direct vehicle and seat riders.
- Added a narrow movement-input transformer that keeps incomplete MCHeli dismount holds out of vanilla's server Sneak state, even when other mods reorder client tick handlers.
- Added mount-context IDs to normal dismount requests, server-side stale-request rejection, and focused lifecycle diagnostics behind the existing MCHeli debug option.

# Fix Drafting Table Crawler Track Preview Crash (PR pending)

- Fixed vehicle previews with crawler tracks crashing when the drafting table rendered without a live vehicle entity.
- Kept preview tracks visible at a stable static phase while preserving live vehicle and LOD crawler track animation.

# Treat RWR as single HUD element and integrate it into the HUD Layout Editor (PR pending)

- Registered the complete RWR display as the single `builtin:rwr` / `rwr.display` HUD layout element.
- Added the RWR to the layout editor preview and suppressed its event-driven render while the editor is open.
- Added complete circle and threat-label bounds capture, stable center-based scaling, and saved offset/scale persistence through the existing HUD layout JSON system.
- Preserved the RWR texture, blend functions, blend enable state, and render color for subsequent HUD rendering.

# HUD rectangle selection (PR pending)

- Added live click-and-drag rectangle selection for movable HUD editor elements, including Shift-add and snapshot-based Control-toggle behavior.
- Preserved group dragging, overlap cycling, scaling, editor controls, grid snapping, and fixed HUD category exclusions.

## HUD Layout Editor

- Added client-side, JSON-backed HUD layout profiles and a non-pausing editor entry on Render Settings.
- Added parsed HUD element identities, safe OpenGL offset scopes, nested `Call` paths, and optional `LayoutGroup` metadata.
- Documented HUD pack layout metadata and fixed aiming/full-screen element requirements.

# Fix Active Radar Toggle Input (PR pending)

- Updated the shared radar key in every vehicle input handler so one press sends one synchronized radar toggle request and disabling radar hides the active radar HUD.
- Radar toggles now play the standard local cockpit click without broadcasting a duplicate server-world sound.

# Hide Disabled Active Radar HUD Widgets (PR pending)

- Active radar HUD widgets now hide completely when radar is disabled while passive RWR and unrelated HUD elements remain visible.

# Toggleable Active Radar

Added pilot-controlled, persistent active radar, capability-aware scanning, synchronized emitter
  state, and RWR filtering so disabled or unequipped vehicles do not emit signatures.
- Added `KeyRadar` and documented `HasRadar`, including compatibility with explicit `RadarType` and
  enabled `EnableEntityRadar` vehicle configurations.
- Fixed the creative inventory crash caused by radar initialization on temporary vehicle tooltip entities.

# Finite cargo-backed service vehicles (PR pending)

- Added opt-in `GasPump`, `AmmoLoader`, and `ForceY` shared vehicle configuration while retaining infinite legacy range-only suppliers.
- Added persistent, overflow-safe service fuel reserves, cargo fuel-can draining, deterministic target ordering, and transactional configured ammunition-package consumption.
- Documented finite support inventory, range, fuel conversion, and same-level requirements.

# Prevent Small-Explosion Knockback (PR pending)

- Prevented entity explosions below size `3.0F` from adding MCHeli or vanilla damage knockback while preserving each affected entity's existing motion.
- Kept damage, hit reporting, effects, fire, and block behavior unchanged, and retained existing knockback behavior at or above the threshold for normal and underwater explosions.

# PR pending - Pre-contact Tank Physical-Hull Step Solver - DID NOT WORK! Was reverted.

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

# Catastrophic tank turret-pop correction (PR pending) - DID NOT WORK.

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
# Synchronized active radar HUD and control hint (PR pending)

- Read active radar power directly from the synchronized vehicle status watcher on clients, so all
  active radar panels and contacts disappear immediately when the server turns radar off.
- Add a shared, authority-aware `Radar ON`/`Radar OFF` vehicle HUD line that follows the configured
  `KeyRadar` binding, including turret and remote-pilot control HUDs.
- Preserve passive RWR and warning displays independently of active radar visibility.
# HUD Layout Editor direct manipulation

- Replaced the profile/coordinate list with a live, non-darkening vehicle HUD preview.
- Added mouse hover, overlap-aware selection, dragging, grid snapping, keyboard nudging, and immediate reset previews.
- Added transactional multi-profile edit sessions so Save persists changed profiles and Cancel discards all edits.
- Added logical screen-space bounds capture for HUD text, textures, rectangles, lines, and compound built-in scopes.

## HUD layout scaling and multi-selection

- Added persistent, center-pivoted HUD element scaling with schema version 2 migration.
- Added right-click multi-selection, group dragging, group arrow movement, and multi-element reset controls to the live HUD layout editor.

# (17b2efa Fix dedicated server addon resource loading)

- Moved addon resource-pack registration behind the sided proxy so dedicated servers no longer
  resolve Forge client-only resource classes during pre-initialization.
- Preserved live addon resource registration and resource-refresh behavior on clients.
- Documented the dedicated-server/client split for addon resource handling.

# (b48a875 Isolate remaining client runtime links)

- Removed direct Minecraft client references from the shared packet handler and delegated client
  player and packet-queue access through the sided proxy.
- Replaced the common proxy and vehicle entity's client-only sound-updater type with a common-side
  update contract while retaining the existing client implementation.
- Audited the common mod entry point and shared packet initialization path for client linkage.

2026-09-14 05:55 — Make custom vehicle exits single authoritative placements

- Stopped populating and replaying the shared post-dismount placement queue that forced former
  riders back to MC Heli's selected exit position for five to eight subsequent vehicle ticks and
  copied vehicle fall distance over their independent post-dismount movement state.
- Preserved normal pilot, passenger, gunner, and rack exit selection; rack releases now retain the
  selected coordinates across vanilla detachment with one final post-detach placement.
- Restored immediate ownership of detached player position to normal Minecraft movement and
  position-history integrations without adding mod-specific compatibility behavior. Existing
  public reserve types remain as deprecated no-op API surface for addon compatibility.

2026-09-14 07:05 — Synchronize custom exits with vanilla player movement authority

- Traced MC Heli dismounts through `EntityPlayerMP.mountEntity(null)`: vanilla published its
  provisional safe exit and reset the server movement handshake before MC Heli replaced the
  player coordinates with a custom exit using plain `setPosition`.
- Commit the one final MC Heli-selected player exit through `NetHandlerPlayServer#setPlayerLocation`
  after detachment, keeping its accepted-position baseline and client correction packet on the
  same coordinates. Client-side and non-player exit placement remains local and single-shot.
- Applied the shared final-placement path to pilots, passenger seats, gunners, and rack exits.
  Java compilation and in-game compatibility validation remain to be performed.
2026-09-20 — Persistent vehicle inventory snapshots (PR pending)

- Replaced live per-icon vehicle geometry in inventories and NEI with persistent, schema-versioned
  PNG snapshots stored under the Minecraft game directory, while retaining live models for held,
  dropped, and spawned vehicles.
- Added one-per-frame scheduling, visibility expiry, FPS gating, conservative generation pacing,
  deduplicated requests, lazy texture loading, and an LRU texture limit with render-thread cleanup.
- Added client settings and diagnostics for snapshot generation and documented safe cache clearing.

2026-09-20 09:24 — Add independent MC Heli technology progression

- Added optional `TechYear` and half-step `TechTier` vehicle metadata with one configurable year resolver, compatibility-safe unrestricted behavior for unclassified addons, vehicle-item tooltips, and held-item inspection.
- Added a separate `mcheli_tech.cfg`, per-world persistent unlocked tier, existing-command-permission integration, live server-settings synchronization, and configurable operator/creative bypass.
- Enforced the server-owned tier decision in drafting, ordinary crafting completion, placement, dispenser deployment, UAV-station spawning, vehicle entry, and active vehicle-control packets while preserving already-owned items and entities. Added variant-specific years to a maintained representative set of aircraft, helicopters, tanks, and ships; ambiguous and fictional definitions remain unclassified rather than receiving invented tiers.
- Java 8 offline compilation passed. No Minecraft launch or in-game validation was performed; persistence, live command sync, each bypass mode, blocked crafting/placement/entry/UAV paths, and tooltips still require multiplayer runtime testing.
# Fix Inventory Icon Snapshot Regression (PR pending)

- Fixed the vehicle inventory icon snapshot regression: inventory and NEI now keep a 3D-looking
  placeholder while missing snapshots are generated by a fair, adaptive queue, load completed PNGs
  immediately, retain persistent cache files across reloads, and use framebuffer hardware
  capability independently of the vanilla framebuffer preference.

2026-09-20 09:44 — Replace dismount ASM with UniMixins

- Added the legacy UniMixins 0.3.1 build, refmap, manifest, and early-loader foundation while
  retaining `devmods/` as the single development runtime-mod location.
- Replaced the raw `MovementInputFromOptions` bytecode transformer with a required RETURN Mixin
  that calls the unchanged vehicle-only dismount input gate after vanilla polls movement keys.
- Removed the old transformer registration and class. Java 8 compilation, refmap generation,
  packaging, reobfuscation, and a bounded development-client bootstrap passed; UniMixins found
  the MC Heli early loader and offered the new mixin. In-world dismount behavior still requires
  gameplay validation.

2026-09-20 10:08 — Complete bundled vehicle technology-year metadata

- Inventoried all 503 bundled vehicle definitions: 59 helicopters, 128 planes, 18 ships, 247 tanks
  and ground vehicles, and 51 static/deployable vehicle definitions.
- Added explicit `TechYear` metadata to the 474 previously unclassified definitions. Every bundled
  definition now has exactly one year/tier declaration, including drones, civilian vehicles,
  support vehicles, boats, artillery, launchers, and alternate weapon/loadout variants.
- Corrected the represented T-72A configuration from the original T-72 family's 1973 date to its
  1979 production-variant introduction. Generic, fictional, improvised, and near-future definitions
  use consistent approximate years instead of falling through to unrestricted behavior. Static
  completeness validation passed; in-game tier display and gating remain to be tested.

2026-09-20 14:58 — Cache model-derived vehicle icons as persistent PNGs

- Replaced repeated inventory/creative/NEI model draws with an independent model-to-PNG pipeline.
  It resolves shipped prebakes first, persistent `cache/mcheli/icons/` PNGs second, and otherwise
  queues one canonical 128×128 offscreen capture. Ready icons are ordinary textured quads; equipped,
  dropped, and world/entity rendering remains live.
- Preserved the 30° X/45° Y icon transform, `1.35 / max(bodyWidth, bodyHeight, 1)` normalization,
  per-type and vehicle scale, model texture repair, skin overlay, deterministic lighting/depth, and
  transparent crop/padding. Cache keys hash actual definition/model/texture/overlay bytes plus all
  relevant scales, repair settings, renderer identity, and cache schema.
- Added deduplicated queued states, one capture per 350 ms, a reusable framebuffer/read buffer,
  immediate dynamic upload, bounded single-thread atomic PNG writes, resource-reload cleanup, aggregate
  diagnostics, ordinary-sprite pending/failure behavior, and same-generator developer prebake export.
- User runtime testing exposed that requests remained queued because the service call was attached to
  a handler registered on Forge's gameplay event bus. Queue servicing now also runs from MC Heli's
  established FML render-tick handler. Offline Java compilation passed after this correction; the
  corrected MC Heli icon transition and cache reuse still require another in-game verification.

2026-09-21 01:22 — Bound vehicle icon preparation and finalize disk writes

Player-facing

- Vehicle inventory icons continue to use their authored sprite while a first icon is prepared, then
  switch to the captured model image. Complex OBJ/MQO buffer uploads are spread across frames and
  completed captures remain spaced by at least 350 ms to reduce NEI inventory stalls.
- Generated icons use compact cache filenames under `cache/mcheli/icons/`; unchanged disk hits upload
  the saved PNG directly without model preparation, framebuffer capture, or readback.

Developer/backend

- Reused the vehicle info's loaded model, ran texture repair before incremental VBO preparation, and
  capped preparation at 8,192 vertices per render frame using the existing retained group geometry.
- Made PNG completion explicit: create the cache parent, encode and verify a sibling `.tmp`, attempt
  atomic replacement with a normal replace fallback, verify the final file, preserve queued writes
  across resource reload, and drain the non-daemon writer for a bounded period at shutdown.
- Replaced the earlier snapshot diagnostic name with the disabled-by-default
  `DebugVehicleIconCache` lifecycle, timing, cache, VBO, write, and queue diagnostics.

2026-09-21 14:27 — Smooth first-time vehicle icon preparation

- Changed icon VBO preparation from an 8,192-vertex-only allowance to a 0.75 ms render-thread
  budget, using 512-vertex chunks with a 2,048-vertex hard ceiling per frame. Capture now rechecks
  every OBJ/MQO group and returns to incremental preparation if any VBO could still be built by
  `renderAll()`.
- Kept inventory generation lower priority than gameplay: cache misses no longer synchronously load
  vehicle models or initiate texture repair. They retain the authored sprite and rotate behind other
  requests until the normal model lifecycle has supplied the model. An already repaired texture is
  reused; otherwise capture uses the authored base texture without invalidating geometry VBOs.
- Split production diagnostics into average/worst request resolution, model and texture lookup,
  per-frame VBO work, framebuffer setup, GPU draw submission, `glReadPixels`, pixel copy/processing,
  final texture upload, and READY latency. Normal texture repair now separately reports image load,
  coverage, repair, UV correction, repaired-texture upload, corrected vertices, and invalidated VBO
  groups.
- Replaced byte-at-a-time framebuffer copying with one bulk buffer copy. GPU readback, background
  crop/processing, later final-image upload, 350 ms capture pacing, request deduplication, bounded
  queues, and persistent-cache behavior remain separate. Pending cache lookups continue alongside
  an uncached model's preparation, and completed disk hits are promoted to upload before capture work.
  Offline Java compilation passed; first-time frame pacing and the diagnostic worst-stage
  measurements still require in-game validation.

2026-09-21 16:45 — Retain vehicle icon requests under resolver backpressure

- Fixed NEI bursts permanently losing otherwise valid MC Heli icons when the bounded resolver
  executor rejected a submission. Temporary saturation now leaves the entry in `WAIT_RESOLVE` and
  retries it on a later render tick instead of transitioning it to `FAILED`.
- Separated the deduplicated lightweight resolve backlog from uncached capture work. Both backlogs
  are bounded at 1,024 entries, while the expensive single-worker executor remains bounded at eight
  queued jobs and receives at most one new resolver admission per render tick. A backlog safety-limit
  rejection remains retryable when NEI asks for that item again.
- Preserved completed disk-hit upload priority and made post-readback pixel-processing admission
  retryable if the shared worker is temporarily saturated. Added aggregate resolver submission,
  saturation deferral, genuine resolver failure, processing deferral, backlog depth/high-water, and
  existing deduplication diagnostics without per-item queue-full exceptions.
- The time-budgeted VBO path, prepared-buffer capture guard, 350 ms capture pacing, asynchronous CPU
  processing/PNG persistence, and persistent cache format are unchanged. Offline compilation passed;
  a fresh-cache NEI burst still requires in-game validation.

2026-09-21 23:10 — Drive uncached vehicle icons from inventory requests

- Confirmed inventory/NEI requests were registered by `handleRenderType`, but cache misses stopped
  waiting for `info.model`; only equipped/world rendering called the lazy model initializer. Added
  explicit `WAIT_MODEL`/`LOADING_MODEL` states that parse the body through the existing bounded worker
  and install it on the client thread, so holding or spawning the vehicle is no longer a prerequisite.
- Moved first-use texture residency and fixed-size framebuffer/PBO allocation into explicit stages
  before capture. Capture now rechecks model, prepared VBO, resident base/overlay textures, and the
  reusable framebuffer before drawing, returning to preparation if any invariant changes.
- Added capability-checked asynchronous framebuffer readback using two reusable pixel-pack buffers
  and OpenGL sync fences. Supported drivers issue the 128×128 transfer without mapping it, poll the
  fence on later render frames, then copy completed pixels for worker processing. Drivers lacking
  both PBO and sync support retain the existing synchronous fallback; resource reload deletes the
  buffers/fence.
- Split diagnostics across inventory callback sources, model waits/loads, texture warmup, framebuffer
  creation/bind/clear, state setup, texture bind, base and overlay VBO draws, capture restore, PBO
  issue/completion latency, synchronous `glReadPixels`, pixel copying, and final texture allocation,
  CPU copy, and GPU upload. Persistent cache priority, resolver backpressure, VBO budgets, 350 ms
  capture pacing, and the cache schema are unchanged. Offline compilation passed; uncached NEI
  generation and capture frame pacing still require in-game validation.

2026-09-21 23:56 — Compose vehicle icons from rendered silhouettes

- Replaced the single generic inventory capture plus pixel crop with a two-pass orthographic
  compositor. A conservative preview uses model bounds only to keep geometry in frame, then its
  rendered alpha silhouette supplies the final uniform scale and visual center.
- Added plane, helicopter, ground, ship, and fallback presentation presets. Their angles and occupancy
  targets give aircraft and long ships more useful canvas presence while retaining a stable elevated
  view for tanks and other ground vehicles. Existing global and per-vehicle scale controls remain
  relative adjustments around the normalized class target.
- Added hard class margins and a final silhouette check. A final capture that approaches an edge is
  recentered and reduced once before acceptance, preventing already-clipped geometry from being
  hidden by post-process padding. Bumped the icon cache schema so older compositions regenerate.
- Preserved request deduplication, authored-sprite fallback, bounded model/VBO preparation, texture
  repair reuse, paced framebuffer capture, asynchronous PBO readback, background processing and PNG
  persistence, and ready textured-quad rendering. Offline Java compilation passed; fresh-cache NEI
  visual composition and the corrective edge pass still require in-game validation.
