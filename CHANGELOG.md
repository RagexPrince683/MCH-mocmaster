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

2026-09-25 00:00 - Enforce server-authoritative three-second Sneak dismounts

- Confirmed that replacement or late-written movement input could bypass the
  `MovementInputFromOptions` RETURN filter and publish vanilla Sneak, causing the server's normal
  ridden-player update to detach an MC Heli rider without using the timed control-packet path.
- Added a final pre-packet client guard and a pre-update server guard for valid MC Heli vehicle and
  seat riders while preserving configured keyboard and mouse bindings and unrelated mounts.
- Added mount-bound hold start/cancel signals and server-owned monotonic timing. Normal pilot and
  passenger requests now reject incomplete, stale, duplicate, and wrong-mount holds and require a
  release before reuse; explicit ejection, parachute, seat-transfer, destruction, death, and cleanup
  paths remain independent.
- Documented the input boundary, server authority, lifecycle cancellation, and bounded diagnostics.

2026-09-25 21:46 - Guard the actual vanilla ridden-player detach boundary

- Moved the server Sneak guard from `EntityPlayerMP.onUpdate` to the head of
  `EntityPlayer.updateRidden`, before vanilla can call `mountEntity(null)` for a sneaking rider.
- Scheduled SimpleImpl packet callbacks on the client or server game thread before processing hold
  updates and vehicle controls, preserving server-owned three-second timing and entity thread safety.
- Added debug-controlled diagnostics for blocked early vanilla detach attempts with the side,
  caller, mount and parent identities, seat, hold state, and elapsed server time.

2026-09-25 21:54 - Correct three-second dismount packet boundaries

- Routed legacy SimpleImpl server callbacks through an ordered server-tick task queue and copied
  packet payloads before leaving Netty, keeping hold state and vehicle handling on the game thread.
- Corrected the final client Sneak guard to inject into Forge 1.7.10's mapped
  `EntityClientPlayerMP.sendMotionUpdates()V` method and require the injection to apply.
- Added the checked client-player conversion required by the shared client-player accessor.

2026-09-25 22:30 - Add focused dismount diagnostics

- Added a disabled-by-default `DebugDismount` trace independent of general debug logging, with startup
  and Mixin markers, physical input edges, both Sneak guards, hold/packet transitions, and server decisions.
- Added MC Heli-scoped mount-change observation on both sides, including bypass detection and one bounded
  caller stack for the first actual detach in each player/mount/hold session.
- Updated the dismount contract to treat the remaining instant detach cause as unconfirmed and documented
  configuration, log locations, the authoritative detach marker, and the requested reproduction details.
# Fix Ragecraft three-second dismount hold cancellation (PR pending)

- Traced the 145 ms failure to an unguarded vanilla Sneak dismount route: the captured launch log prepared zero MC Heli mixins, leaving `EntityPlayer.updateRidden` able to call `mountEntity(null)` after `START_SNEAKING`. The archived hold trace does not contain the new mount-context values at the reset, so the first changed field could not be proven from that recording. The hold-start seat packet only registered server hold state; it did not request an exit.
- Declared the client input and server riding guards in `mixins.mcheli.json` so UniMixins prepares them in the full modpack, and removed duplicate dynamic mixin registration.
- Made hold-start and hold-cancel packets return before all legacy dismount, seat switching, parachute, and placement branches. The completed request still uses the server's three-second validation and existing normal exit.
- Compared stable player, mount, and parent entity IDs and UUIDs with seat and world identity for hold continuity. Expanded focused diagnostics to print old and new mount context fields, the first changed field, the vanilla Sneak action receipt, and the caller of an unexpected riding-state mutation.

2026-09-26 00:00 - Stop repeated missing vehicle part model retries

- Remembered failed lazy vehicle registrations by weak definition identity so entity, item, NEI, and
  preview draws do not retry a confirmed missing required part every frame.
- Consolidated each failed attempt into one vehicle-context diagnostic while retaining normal model
  manager diagnostics for non-vehicle loads.
- Cleared failure state on complete resource registration and targeted vehicle reloads so newly added
  or corrected assets are retried without restarting the client.

2026-09-26 00:00 - Restore saved helicopter radar layout on first join

- Gave the built-in helicopter radar gauge its stable screen-space center as an explicit layout
  pivot, so saved offsets and scaling apply on the first normal HUD render instead of waiting for
  the layout editor to capture the gauge geometry.
- Kept radar availability and active state checks in the vehicle HUD renderer, independent from
  layout loading and transforms, so existing radar toggles and disabled radars remain unchanged.

2026-09-26 00:00 - Add Drafting Table vehicle paint customization

- Added an existing-vehicle input, translucent RGB/opacity controls, model-derived part selection,
  and a live painted vehicle preview to the Overdrive Drafting Table.
- Persisted paint on vehicle items and entities, synchronized it through normal and distant vehicle
  rendering paths, and retained existing textures and skin overlays beneath the paint layer.
- Added server-authoritative, per-player and per-vehicle-type defaults that survive world reloads,
  apply to unpainted inventory acquisitions and player placement, and can be disabled without
  stripping paint already saved on items or entities.

2026-09-26 00:00 - Add Drafting Table vehicle camo skins

- Added a validated Vehicle Camo Skin slot that previews and persists existing texture-overlay items,
  consumes one skin on the first application, and carries camo through per-player vehicle defaults.
- Limited both camo and RGB paint passes to enabled named model parts across vehicle bodies, animated
  parts, previews, placement synchronization, and distant rendering.
- Rearranged the Drafting Table controls and locked inventory interaction while its Paint screen is open.

2026-09-26 00:00 - Align painted overlays with moving vehicle parts

- Reused each body's or moving part's normal geometry path for camo and RGB overlay passes so turrets,
  barrels, wheels, and external part models retain their prepared animation transforms.
- Restricted the body overlay to an actual `$body` group and kept unselected parts on their normal
  texture without redrawing dynamic geometry from an all-model body pass.
- Restored OpenGL attributes and the previously bound texture after overlay rendering.

2026-09-26 04:30 - Fix Drafting Table screen layout

- Realigned the main-screen player inventory and hotbar slots with their background artwork, moved the
  paint action above the selected vehicle name, and separated the vehicle and camo input frames.
- Gave the Paint screen the clean list-screen background while keeping all inventory artwork, slot
  rendering, and slot interaction exclusive to the main screen.

2026-09-26 04:36 - Isolate painted vehicle overlay state

- Preserved the current OpenGL color around camo and RGB overlay passes so their tint and opacity
  cannot make later body, turret, barrel, wheel, or external-part base textures dark or transparent.
- Kept each selected part's normal textured draw ahead of its aligned overlay while leaving
  unselected parts on the normal base-texture path.

2026-09-27 00:00 - Restore Bugatti Chiron high-speed steering

- Added a contact-scaled minimum steering authority for opted-in civilian cars while retaining the
  lateral grip cap, allowing high-speed understeer instead of an imperceptible yaw response.
- Applied the setting only to the Bugatti Chiron and kept client prediction aligned with the server.
- Added opt-in client steering CSV diagnostics before the steering limiter so key input, contact,
  requested yaw, and applied yaw can be compared with the existing server trace.

2026-09-27 00:00 - Fix Bugatti Chiron wheel contact at speed

- Compared the wheel safety clamp with the pending body destination instead of the body's old
  position, preventing valid wheel travel above two blocks per tick from triggering recovery.
- Kept collision-resolved wheel height during horizontal recovery rather than lifting the wheel by
  half its step height and falsely losing flat-ground tire contact.
- Matched the client wheel-before-body update order to the authoritative server physics order so
  steering prediction and server grip sample the same suspension phase.

2026-09-27 00:00 - Add civilian car spring and shock suspension

- Added collision-shape suspension probes, per-wheel compression and compression-rate state, bounded
  spring response, and separate compression/rebound shock damping for opted-in civilian cars.
- Applied supported-wheel response to authoritative body height, pitch, and roll while client prediction
  smooths the visible pose and keeps fast-moving current/predicted contact samples consistent.
- Mapped nearby rendered wheel parts to collision wheels for suspension travel, with unchanged rendering
  for decorative or incompatible wheel layouts, and documented the new tuning keys and defaults.

2026-09-27 04:03 - Fix civilian suspension neutral height and roll recovery

- Initialized visible wheel travel from complete, level wheel support instead of each tire's first
  contact, preserving authored `AddPartWheel` neutral positions and independent `SetWheelPos` geometry.
- Applied the predicted civilian suspension pose on every client tick and snapped negligible level-ground
  targets to zero so body roll returns smoothly to neutral even after the vehicle stops.
- Derived terrain pitch and roll from collision-resolved wheel heights instead of compression, removing
  the feedback loop that could preserve an old body angle on flat ground.

2026-09-28 00:00 - Add synchronized civilian brake lights

- Added an opt-in tank brake-light setting with explicit rear-lamp definitions and a disabled default.
- Synchronized the existing Space and S input states so brake lamps remain independent from normal lights.
- Enabled bundled civilian cars and reused their authored rear-light geometry where available.

2026-09-27 21:54 — Set bundled civilian car reverse speed ceilings

- Added optional `CivilianCarReverseSpeed` in blocks/tick, preserving legacy movement when absent.
- Limited powered backward movement on the server and matched client extrapolation; preserved forward
  speed, acceleration, braking and authoritative position interpolation.
- Bounded reverse demand and made W respond immediately for opted-in cars. Kept S's forward braking
  and Space damping while preventing the shared brake-lamp state from damping powered reverse.
- Configured all 25 bundled passenger cars, including the R32 police car and armored Phantom, without
  changing existing `Speed` values or military/other vehicle definitions. Documented four calculated
  ceilings, 21 conservative gameplay estimates, variant choices, sources and identity/fitment gaps.
- Java 8-target compilation and targeted headless checks passed; changed production classes are major
  version 52. Live Forge 1.7.10 driving and multiplayer road testing remain unverified.

2026-09-27 22:14 — Add civilian FWD, RWD, and AWD propulsion

- Added optional `DriveType` values `FWD`, `RWD`, and `AWD`; omitted/invalid values and removed fields
  on reload retain legacy thrust. Weight and category do not infer a drivetrain or enable this path.
- Limited server forward/reverse engine force by collision-derived driven-wheel contact and remaining
  axle traction, preserving accumulated momentum and existing throttle, braking, steering, collision,
  gearing, lights, drag, and speed ceilings. AWD pools available capacity without a fixed torque split.
- Retained configured axle membership/counts so missing or dead wheels cannot increase traction.
  Added only drivetrain lines to 20 passenger-car definitions (2 FWD, 14 RWD, 4 AWD); left generic
  Impreza, custom Fresh Auto drift car, and unidentified armored Phantom unset. Police, military,
  tracked, aircraft, and boat definitions are unchanged.
- Updated configuration references, traction equations, identity evidence, and reverse-speed guidance.
  Offline `compileJava` passed with the supported cached Gradle setup; changed classes target Java 8
  (major version 52). Asset/diff audits passed. End-user in-game and multiplayer driving remain untested.

2026-09-27 22:45 — Add civilian car throttle, gearing, brakes and longitudinal slip

- Added explicit `CivilianCarDrivetrain` with disabled default and bounded gameplay throttle response,
  idle/redline RPM, automatic forward/reverse gears and torque blending without velocity resets.
- Separated S service braking, Space rear handbraking and synchronized lamp state. W remains available
  with Space; W+S combines throttle/braking without reverse. Direction changes brake before drive, and
  pilot/control loss or GUI entry releases held inputs. Steering direction follows actual travel while
  preserving the existing steering/grip limits.
- Integrated axle wheel inertia and longitudinal slip with existing collision support and lateral-first
  traction capacity. Existing `DriveType` selects powered axles; unset layouts use a neutral fallback.
  Added synchronized engine/gear/brake and axle speed/slip/contact state, RPM sound, independent axle
  wheel animation and restrained supported-wheel spin particles. Legacy controls/physics remain for
  vehicles without the new opt-in; suspension, lateral grip and collision systems are retained.
- Added only an opt-in line to 23 civilian passenger definitions, including the custom drift car and
  armored limousine, preserving every `Speed`, reverse ceiling and `ThrottleDownFactor`. Police and
  other excluded vehicle definitions are unchanged. Settings are gameplay defaults, not inferred specs.
- Updated tank/civilian references and end-user driving checks. Final offline `compileJava` succeeded
  with the supported cached Gradle setup; all nine changed/new classes target Java 8 (major 52).
  Source, asset, force-equation, encoding and diff checks passed. No packaging or game launch was done;
  shifts, hill holding, burnout intensity, suspension interaction and multiplayer visuals need feedback.

2026-09-28 00:00 — Finish civilian car force, gearing and instruments

- Traced the reported rapid acceleration/braking to oversized normalized drive/brake demands and
  equal Speed-based gear bands; the existing tachometers displayed throttle instead of engine RPM.
  Replaced those demands with ratio/final-drive torque, a bounded gameplay torque curve, wheel-RPM
  fuel cut and explicit quadratic drag. Speed is the final safety cap, not an acceleration target.
- Made throttle response, idle/redline, gear count, shift time and both brake settings authoritative
  in the opt-in server drivetrain. Shifts blend ratios/RPM without clearing momentum; stopped launches
  and driven-wheel spin affect RPM. S brakes gradually before near-stop reverse, Space supplies held
  rear brake torque, combined pedals remain available, and brake reaction cannot reverse body travel.
- Retained existing input packets, server authority, axle selection/contact/slip, brake lights and RPM
  sound. Added synchronized gear/RPM HUD arguments and RPM tachometers with fallback for shared
  non-car HUDs. Military/police engines, steering, suspension and collision paths are preserved.
- Wrote explicit drivetrain values in all 23 supported civilian car assets. Verified and representative
  factory gearing is distinguished from gameplay tuning; all existing reverse ceilings and
  ThrottleDownFactor values remain. Corrected Speed unit conversions for the 2016 Chiron (420 km/h),
  Carrera GT (330 km/h) and standard 1976 W123 240D (138 km/h); the other 20 ceilings remain pending
  exact variant/speed evidence. Chiron absolute ratios/final drive remain approximate gameplay tuning.
- Extended only opted-in tank Speed parsing to 8 blocks/tick, resolving the opt-in after all keys;
  other tanks retain the 4-block/tick parser limit. Added bounded defaults and reload handling for
  ratios, final drive, tire radius, drive force, drag and longitudinal traction. Updated existing
  configuration, drivetrain, reverse-speed and HUD documents; deprecated configreference is unused.
- Existing offline compileJava succeeded twice, including the final source. All six changed production
  classes target Java 8 (major 52). Static asset/documentation, steady-force and diff audits passed.
  No tests, packaging, remap/reobfuscation or game launch ran. Driving feel, shifts, braking distance,
  burnouts, contact/gradient effects, HUD readability and multiplayer behavior remain unobserved.

2026-09-28 01:08 — Fix low wheel anchors losing civilian car propulsion

- Source comparison identifies f5dde8e3 as the first of the three recent propulsion commits to
  expose the Chiron's existing support-probe error: its nominal invisible wheel bottom lies
  0.39 blocks below level ground, where a downward calculateYOffset sweep cannot find support.
  The later wheel-spin and gearing commits retain that probe, leaving both axle force limits zero.
- Normalize the shared grip query using the actual wheel AABB bottom offset/height and body
  collision floor. Preserve the wheel footprint, bounded reach, takeoff rejection and configured
  denominator; live suspension placement/travel, steering, brakes, AWD, ratios and speed limits
  are unchanged. Restored contact lets existing wheel reaction recover stored spin during fuel cut;
  unsupported axles still supply zero body force and cannot emit road smoke.
- Updated the existing contact document, including the distinction between pre-thrust contact and
  the later lateral-grip CSV snapshot. The available CSV predates these commits and cannot validate
  the reported failure. Offline compileJava passed; MCH_EntityWheel targets Java 8 (major 52).
  Source/equation checks covered different wheel heights, unsupported force, gradual launch and
  traction recovery. No current pre-thrust runtime trace or in-game driving was captured; launch,
  shifts, traction recovery, braking and unsupported smoke still require Forge 1.7.10 driving checks.

2026-09-28 02:29 — Recover Chiron client steering on tracked road positions

- Fresh internal-game diagnostics for Chiron entity 26904 show all 495 client turn samples
  rejecting yaw with zero contact, including after slowing down, while all 239 server snapshots
  retain four supported wheels. Both turn keys remain present; the user confirmed ordinary
  driving also cannot turn. The first rejection is the client grip limiter, before added yaw
  reaches the server's physics budget.
- Traced the matching collision failure to Minecraft's 1/32-block tracked-position rounding:
  the client body floor can overlap the road slightly, and normalization to that floor still
  leaves calculateYOffset unable to find downward support. Lift the shared grip query within
  its existing 0.05-block skin and anchored height, adding exactly the lift to the sweep so its
  lowest endpoint and unsupported-wheel rejection remain intact. Live wheel/suspension state,
  lateral grip, steering floor, AWD force, gearing, top speed and brakes are unchanged.
- Restored the Chiron's prior disabled diagnostic definition and updated the existing grip
  document with the capture and collision reconstruction. Offline compileJava passed; the
  changed wheel class is Java 8 (major 52). Collision-equation checks covered road recovery,
  stale airborne wheels, a wholly low box and missing terrain. No test files were added.
  Corrected in-game steering, high-speed understeer and multiplayer recovery remain unobserved.

2026-09-28 03:00 — Restore Chiron suspension support and level wheel position

- Source and Minecraft collision geometry confirm that the Chiron's level wheel anchor puts its
  collision bottom 0.39 blocks inside the road. The separate suspension sweep missed support,
  reported zero compression, and extended the invisible box to 0.84 blocks below the surface.
  A positive rest compression learned on earlier terrain would then produce negative visible wheel travel;
  body pitch samples terrain independently and does not explain that persistent level-ground offset.
- Normalize the suspension anchor using the real wheel bottom offset/height and the level body floor.
  Preserve terrain pose, use actual block collision boxes within the wheel footprint, and compensate
  the bounded tracking-skin lift in both sweep distance and compression. Place collision wheels and
  check body-movement wheel support against the same corrected anchor, with travel still bounded by
  SuspensionTravel. Current/predicted horizontal samples share one vertical reference so pending
  gravity cannot inflate compression. Other bundled civilian anchor heights need no normalization.
- Calibrate rendered rest compression only with complete, even collision-surface support at the body
  floor and settled body height, pitch, and roll. Retain the baseline on unsuitable terrain, then
  converge and snap its final small error after level support returns. The Chiron's level compression
  is now 0.45 blocks, its collision bottom is at the road, and settled render travel is zero relative
  to the authored model. No wheel model/config, rotation, steering/grip query, AWD force, gearing,
  speed ceiling, or brake tuning changed; the recent client contact correction remains intact.
- Updated the existing suspension description and historical grip findings. Offline compileJava
  succeeded with the existing cached convention/Jabel setup; changed classes target Java 8 (major 52).
  Source and collision-equation checks covered bundled anchor heights, client rounding, finite reach,
  travel bounds, and baseline recovery. No test files or temporary diagnostics were added. No current
  suspension runtime capture, packaging, game launch, or terrain driving was performed; wheel/body
  recovery over uneven terrain and multiplayer visuals still require end-user feedback.

2026-09-28 13:21 — Restore powered reverse in legacy military tanks

- Confirmed that 328847d8 first made S set the physical brake/lamp status, applying an extra
  0.5 reverse-demand reduction on top of the existing 0.8 decay. Comparing 16346a39 with its
  parent shows that its later exception covered only positive CivilianCarReverseSpeed controls.
- Exempt powered legacy military reverse from that extra brake damping once forward throttle
  reaches zero. Keep S forward braking, Space braking/pedal suppression, W forward recovery,
  existing reverse buildup/decay, speed caps and synchronized lamps. External legacy packs
  need no civilian drivetrain or reverse-speed setting. Explicit civilian controls and Category C
  definitions retain existing behavior, including civilian utilities without drivetrain opt-ins.
- Audited all 247 bundled tank definitions and documented every effective movement configuration
  in the existing tank control guide: 216 military (213 reverse-enabled, 212 mobile), 30 civilian
  and one police/security armored vehicle. Preserve stationary/creep-speed exceptions, parser
  clamps and last-key overrides; no configuration defect was established and no pack values changed.
- Existing offline compileJava/check passed; both edited Java classes target Java 8 (major 52).
  In-memory control/movement equation checks covered S/Space/W across the 212 mobile military
  definitions with automatic throttle-down both off/on and 1,792 unchanged legacy civilian input
  states; 53,840 legacy non-reverse combinations were unchanged, and 23 civilian drivetrain definitions
  still bypass the edited block. No validation files or diagnostics were added. Packaging, live
  Forge 1.7.10 driving, slopes/collisions, steering and multiplayer lamp visuals remain untested.

2026-09-28 17:35 - Exit destroyed vehicles immediately

- Let pilot and passenger Sneak input queue the existing vehicle exit request immediately when the
  ridden parent vehicle is destroyed, including when destruction occurs during an active hold.
- Keep exact mount, parent, and seat validation on the server while waiving only the three-second
  hold for a currently destroyed vehicle. Immediate acceptance clears any recorded hold and uses
  the existing pilot or passenger dismount and exit-position routines.
- Document the destroyed-vehicle exception. No test files were added, and in-game behavior was not
  observed.

2026-09-28 14:39 — Rotate civilian primary collision with the chassis

- Matched the saved Starion position/pitch to the screenshots and the user's observed static blue
  primary box. Its effective collider is 2.0 x 0.7; EntityWidth/EntityHeight control rider rendering.
  Saved-terrain collision equations identify the unpitched primary's flat bottom as an early floor;
  the extra boxes start clear. The existing grip CSV contains no Starion movement/contact capture.
- Use an oriented primary in civilian compound-body sweeps, intersections, damage rays, and debug
  rendering. Keep its enclosing AABB for vanilla compatibility and advance the vehicle origin by
  resolved motion. Refresh position/pose bounds and restore the legacy box when the opt-in is removed.
  Retain an explicit level chassis reference for terrain and wheel queries so envelope rotation
  cannot change suspension/grip reach. No collider is removed or shrunk; tanks, engine force, gearing,
  speed limits, brakes, wheel-contact gates, and step/solid-clearance rules remain unchanged.
- Updated the existing physics document and feedback cases. Offline compileJava passed; all six
  changed classes target Java 8 (major 52). Saved-world/geometry, source/lifecycle and diff checks ran.
  The reconstructed downward result changes from zero to -0.4302565 blocks before real contact.
  No test files, temporary repository artifacts, packaging, reobfuscation, or game launch were added
  or run. Escape from the photographed ledge, live wheel-contact recovery and multiplayer behavior
  have not been verified in-game.

2026-09-28 16:17 — Recover civilian cars at diagonal block edges

- Confirmed a saved Starion's level-ground +18-degree roll trap: only the low-side wheels
  are within suspension reach, and the missing side was assigned world height zero.
  Roll now requires measured surfaces on both sides and otherwise approaches level.
  A saved Chiron also has front underside overlap with a full grass/dirt block; its
  original overlap-producing tick and RPM/drive-force sequence remain unobserved.
- Compare X/Z, Z/X and continuous diagonal civilian body sweeps. Compare raised candidates
  after landing, retaining support, full-body clearance, headroom and configured step bounds.
  Check server pose changes through conservative angular envelopes; supported lift/settle
  and outward recovery are capped at 0.1 block per tick. New obstacles still clip recovery;
  unsafe angles and ordinary travel from an embedded start are rejected. Include pose Y in
  final displacement/fall accounting and deduct its rise from the same tick's step budget.
- Reconcile wheels with the accepted server position/pose without applying springs twice.
  Clear completely rejected momentum and supported brake residue below 1e-5 block/tick.
  Preserve existing oriented-primary working changes, body dimensions, suspension tuning,
  gearing, engine force, speed limits and brake forces. The military collision/step branch
  matches HEAD; drivetrain and vehicle tuning parser are unchanged.
- Extend the existing disabled-by-default CarGripDiagnostics opt-in with a server movement
  CSV covering W/S, force/contact, both axes, step candidates, pose and final position.
  Update the existing physics document with evidence, recovery rules and seven feedback cases.
- Two offline compileJava passes succeeded with the existing Java 25/Jabel/Forge 1.7.10 cache;
  all six changed Java classes have major version 52. Saved NBT/terrain inspection, source
  authority/lifecycle checks, diagnostic field-count checks and collision-equation checks ran.
  Mathematical cases covered mirrored forward/reverse corners, one-sided corner support,
  straight steps, wall/ceiling rejection, unsupported drops and continuous angular clearance.
  The saved Starion levels with four supported wheels by reconstructed tick 21; the Chiron
  overlap clears in four bounded outward moves. Final diff whitespace validation passed.
  No test/fixture files, temporary repository files, tuning changes, commits or packaging
  were added. The diagonal failure was not reproduced or verified fixed in-game; stairs into
  the hole, live W/S/braking recovery, tight-obstacle rotation, dedicated server and multiplayer
  interpolation still require driving feedback. Equation results are not gameplay verification.

2026-09-28 19:04 — Trace civilian stair and mixed-bump momentum loss

- Confirmed both failed attempts are fully reverted on MCHRgithub: each revert's tree matches
  its attempt's parent, and HEAD matches the tree before either attempt. Treat the reported
  flat stair pauses and worse mixed-bump/choppy driving as observations, not verified causes.
- Traced controls, drivetrain contact/force, drag, suspension/terrain targets, pose clearance,
  compound-body normal/step selection, wheel reconciliation and velocity cleanup. Source permits
  final-contact cleanup after a partial accepted step, but no failing-tick movement CSV exists
  in this checkout's logs or run/logs to distinguish it from lost contact or rejected pose/step.
- Extend the existing per-definition CarGripDiagnostics movement CSV with selected path and
  step gate, accepted translation, effective budget/support, exact horizontal cleanup reason,
  pre-reconciliation pose targets/decision, terrain sample result/heights/counts and engine/tire
  state. Record input before slowing-block scaling. Append a session header to identify the
  expanded schema when an old log exists; preserve captures. Document fields and the precise
  held-W stairs/mixed-bump capture plus wall control in the existing terrain document.
- Movement, step/pose selection, collision limits, velocity decisions, wheel reconciliation,
  client prediction, drivetrain, tuning and definitions are unchanged. No failed StepPose,
  terrain-seed walk, continuation probe or second movement solve was restored. A driving fix
  remains pending runtime evidence; this diagnostics-only change does not claim smooth travel.
- Offline compileJava passed with the repository's existing Gradle/Java 25/Jabel/Forge 1.7.10
  setup after authorized existing-cache access resolved a sandbox wrapper-lock denial. All four
  changed production classes and both changed/new diagnostic nested classes have Java 8 major
  version 52. Source/diff review covered full accepted steps, partial-step contact and solid-wall
  cleanup; CSV header/format counts match at 67 fields. No tests/harnesses, packaging,
  reobfuscation or game launch were performed. Runtime capture, driving continuity, live CSV
  emission and dedicated-server/multiplayer behavior remain unverified.

2026-09-28 19:38 — Resolve civilian pitch and contact across reachable block steps

- Started from clean `dab51cd493d08260e4095218919013d308e53808`, a diagnostics-only
  baseline. The Starion screenshot shows full throttle, about 6,500 RPM, zero pitch
  and about 0.01 speed on full grass-block steps; no Starion movement capture is
  available to identify the exact failing branch. Inspected both reverted attempts.
- Retry independently reachable axle collision surfaces after any failed terrain
  walk column, including intermediate gaps. Keep real-surface/headroom/StepHeight
  bounds and NaN for missing axles. Remove the level-body wall probe's premature
  zero-pitch override; actual body/rotation sweeps still enforce walls and ceilings.
- Evaluate supported step candidates with rotation before or after translation,
  using the unconsumed portion of the original two-degree pitch/roll target.
  Require continuous full-body clearance, real landing support and improved travel
  or equal travel with a lower landing. Allow the original gravity request to settle
  a rotated body at rest. Charge actual pose lift, not net pose Y, to the step budget.
  No velocity-continuation probe, extra rise, client-prediction rewrite or pedal reset.
- Align civilian grip queries with suspension's corrected, pitched anchor and full
  extension bound. The former level-floor clamp could erase supported rear contact.
  Require actual collision support with the existing skin and entity exclusions;
  terrain predictions and stale airborne wheels cannot supply traction. Preserve
  RWD force gates, engine/grip tuning, StepHeight 1.2, SuspensionTravel 0.45, Starion
  axle positions, all collider sizes and the non-civilian movement/contact paths.
- Extend existing diagnostic reasons/paths and append pose_rise; update the existing
  terrain and tire-grip documentation without adding end-user testing procedures.
  Source/call-site review, contact/budget arithmetic and 68-field CSV schema checks
  were performed. No tests, harnesses, fixtures or temporary Java files were added.
  Final offline compileJava passed with the existing Gradle/Java 25/Jabel/Forge
  1.7.10 setup; the six changed production classes and their nested classes have
  Java 8 major version 52. Compilation caught and resolved a local-variable naming
  conflict. Final diff whitespace checks passed; build configuration is unchanged.
  In-game climbing, driving continuity, live diagnostics, dedicated-server operation
  and multiplayer behavior remain unverified; the conservative angular envelope
  can still reject tight-clearance candidates. No packaging or reobfuscation was run.

2026-09-28 20:02 — Player-specific live wheel diagnostics

- Add `/mcheli debugwheels [true|false]` with existing subcommand permissions,
  boolean completion, and current-setting reporting. The session-local setting
  subscribes only the sender to the civilian car they directly ride or occupy
  through a seat; remote control and nearby vehicles are excluded.
- Send authoritative server wheel, suspension, drivetrain, terrain, pose, step,
  requested/accepted movement, cleanup, and candidate-path diagnostics through
  the existing packet channel after server ticks. Reuse decision traces without
  requiring CarGripDiagnostics or enabling its CSV writers. Add only diagnostic
  counters at rejected rotation-clearance decisions; physics and tuning are unchanged.
- Show readable server/client columns in the existing Test mode HUD, replacing
  its general variable dump while active. Label S/C values, show N/A for unavailable
  data, expire silent snapshots, and clear on dismount, disable, death, or world
  changes. Update the existing command reference with units and capture semantics.
- Inspected command permissions/completion, rider ownership, packet registration
  and main-thread delivery, HUD gating, suspension reconciliation, movement traces,
  drivetrain capture, and final diffs. Offline compileJava passed using existing
  Gradle/Java 25/Jabel tooling; inspected production class files have Java 8 major
  version 52. No tests, harnesses, fixtures, or temporary Java files were created.
  HUD readability, diagonal climbing diagnosis, dedicated-server and multiplayer
  behavior still require in-game verification. No packaging/reobfuscation was run.
