# PR #597 - Runtime-correct respawn identity and delayed targeted repair

Commit `04542189dc3b28f9073d7e7ea40ed6f2f8edfe1d` (PR #596) did not fix the runtime issue. The supplied runtime log is authoritative: normal parent IDs 63, 198, and 6125 all started tracking while their chunks were not ready because `forceSpawn=true`. The first client probe nevertheless found the correct MCHeli objects, numeric IDs, classes, and aircraft types. Their vanilla client entity UUIDs differed from the server UUIDs. The failed probe called those differences ID collisions, required the replacement player's client UUID to equal its server UUID, consequently left replacement readiness false, prevented every targeted resend, and deleted the audit at age 60 before processing the final result.

## Corrected client readiness and vehicle identity

Replacement readiness now requires the current Minecraft player and world references, the replacement numeric player ID, the server dimension, object identity with the player observed by `MCH_ClientCommonTickHandler`, and a client respawn audit age of at least one tick. The server player UUID remains in the probe and its match is logged as diagnostic evidence only.

A normal parent is resolved primarily with `WorldClient.getEntityByID`. Confirmation requires an MCHeli base vehicle at that ID, matching aircraft-info type, probe dimension, current client world, a position within 32 blocks, a live entity, usable aircraft info, normal rendering and collision state, and valid seat/hitbox parent references. Vanilla entity UUID agreement is diagnostic only and cannot create an ID-collision classification. Missing-after-create history is bounded to the current respawn generation and numeric entity IDs and is cleared on world unload.

`commonUniqueId` was inspected rather than assumed: `MCH_EntityBaseVehicle.writeSpawnData` writes it and `readSpawnData` installs it on the spawned client vehicle. The probe therefore carries a non-empty server common ID as an additional synchronized MCHeli identity check; empty IDs do not block the numeric-ID/type/dimension/position rules. This is distinct from vanilla `Entity.getUniqueID()`.

## Delayed targeted repair and timeout

A ready client report of a genuinely missing or collided parent marks that parent repair-pending. The next server tick keeps it pending until the exact replacement player remains active, the original server vehicle remains alive, and `PlayerManager.isPlayerWatchingChunk` is true. It then removes the exact watcher object by identity (Forge 1.7.10's `HashSet.contains/remove` path uses equality), invokes the normal tracker removal/re-add path, verifies exact membership, resends the original parent before only its own seats and hitboxes, and schedules a post-resend probe. Confirmed parents are not retracked, UAV/NewUAV parents remain excluded, and each parent is limited to two attempts.

Probe ages remain 1, 5, 10, 20, 40, and 60, but the hard timeout is age 80. Thus the age-60 response has a 20-tick processing window and is not made stale by same-tick audit deletion. LOD snapshots remain render-only and excluded inside 200 blocks. A full two-client graphical respawn test is still required; this source change does not claim runtime success.

# Normal vehicle tracking across player respawn

## Proven evidence and diagnosis

Commit `769e711ad3b77d2063c99ec7265d1967e1e11d46` (PR #594) did not fix the issue. The supplied dedicated-server run is authoritative: the replacement player started tracking parent IDs 60, 206, and 2792, and all three received complete-state syncs. Its `nearby=3`, `tracked=3`, `targetedAttempts=0` result proves that the real server entities and replacement-player tracker memberships were already valid. Watcher membership is therefore not evidence that the replacement client has an entity.

The same run reports `tracked=3` with `chunkReady=0`. Parent `StartTracking` occurred before `PlayerRespawnEvent`, and seats began later. PR #594's watcher-only success condition was invalid. `MCH_EntityBaseVehicle.forceSpawn=true` permits this ordering: a parent can start tracking without `PlayerManager` watching its chunk. The tracker spawn path is independent of the subsequent `S07PacketRespawn`; client respawn handling replaces `Minecraft.theWorld`, so a parent inserted in the old `WorldClient` can be discarded. The server-only run establishes this timing hazard but cannot prove client packet handling order. The first proven client divergence is now recorded only when a probe result reaches the server.

## Client-confirmed protocol and repair

For 60 server ticks after `PlayerRespawnEvent`, the server inventories at most 128 normal parent vehicles within 200 blocks. UAV and NewUAV parents are excluded. Probes are sent at ticks 1, 5, 10, 20, 40, and 60, plus after repair—not every tick. They carry the respawn generation, replacement player and server-world identities, and expected parent IDs, UUIDs, types, positions, and chunks.

Client inspection is scheduled with `Minecraft.func_152344_a`. It resolves each parent by numeric ID and independently by UUID and returns player/world/view identities, respawn readiness and age, entity lifecycle/chunk state, aircraft info, renderer flags, collision state and box, and seat/hitbox parent counts. Decoded results enter a `ConcurrentLinkedQueue`; every `ServerTickEvent` end phase discards stale player instances and handles active results on the server thread. The audit classifies missing entities, ID collisions, dead or unchunked entities, missing aircraft info, render suppression, invalid collision, and invalid dependent parents. It succeeds only after the client confirms every original ID and UUID in the replacement world.

For a missing or ID-collided parent, the server calls Forge 1.7.10's `EntityTrackerEntry.removeFromWatchingList`, then asks the same entry to watch the same original server entity with `tryStartWachingThis`. This uses the normal Forge spawn path, preserves the instance, ID, and UUID, and fires `StartTracking`, whose existing event handler performs the single complete-state synchronization. The parent is resent before targeted reconsideration of its seats and hitboxes. Correctly resolved parents and unrelated tracker entries are untouched. A parent receives at most two resends per respawn generation, all packet lists are bounded, and a visible timeout is logged at tick 60.

`forceSpawn=true` remains because removing it without normal-boundary and distant-LOD runtime coverage risks a separate regression. LOD snapshots remain render-only and excluded inside the normal 200-block range; they are not a fallback for a missing real parent.

A graphical two-client dedicated-server run is still required to learn whether the first classification is `3-missing-entity` (proving premature spawn/world replacement), removal after creation, or later client-state corruption. This source change does not claim that run passed.

## Commit and pull-request audit

* `3509e441...` only updated the uppercase changelog.
* PR #587 (`1d293104...`, merge `3287a81...`) reset client LOD/mount caches and render state.
* PR #592 (`b328858...`, merge `c3add9e...`) restored the 200-block snapshot boundary and UUID mount generations.
* PR #593 (`75610d3...`, merge `1244d0c...`) added an unverified destructive global tracker refresh.
* PR #594 (`7bb2b52...`, merge `769e711...`) removed that global refresh but incorrectly accepted tracker membership as success and logged client evidence only on the client.
* Current commit `e6d4b10...` only updated the uppercase changelog.

This repair retains #587/#592's LOD and mount work, does not revive #593's global refresh, and replaces #594's incomplete audit with bounded client confirmation and targeted resend.
