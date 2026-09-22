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
