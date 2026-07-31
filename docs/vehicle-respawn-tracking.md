# Normal vehicle tracking across player respawn

## Proven evidence and diagnosis

Commit `769e711ad3b77d2063c99ec7265d1967e1e11d46` (PR #594) did not fix the issue. The supplied dedicated-server run is authoritative: the replacement player started tracking parent IDs 60, 206, and 2792, and all three received complete-state syncs. Its `nearby=3`, `tracked=3`, `targetedAttempts=0` result proves that the real server entities and replacement-player tracker memberships were already valid. Watcher membership is therefore not evidence that the replacement client has an entity.

The same run reports `tracked=3` with `chunkReady=0`. Parent `StartTracking` occurred before `PlayerRespawnEvent`, and seats began later. PR #594's watcher-only success condition was invalid. `MCH_EntityBaseVehicle.forceSpawn=true` permits this ordering: a parent can start tracking without `PlayerManager` watching its chunk. The tracker spawn path is independent of the subsequent `S07PacketRespawn`; client respawn handling replaces `Minecraft.theWorld`, so a parent inserted in the old `WorldClient` can be discarded. The server-only run establishes this timing hazard but cannot prove client packet handling order. The first proven client divergence is now recorded only when a probe result reaches the server.

## Client-confirmed protocol and repair

For 60 server ticks after `PlayerRespawnEvent`, the server inventories at most 128 normal parent vehicles within 200 blocks. UAV and NewUAV parents are excluded. Probes are sent at ticks 1, 5, 10, 20, 40, and 60, plus after repair—not every tick. They carry the respawn generation, replacement player and server-world identities, and expected parent IDs, UUIDs, types, positions, and chunks.

Client inspection is scheduled with `Minecraft.func_152344_a`. It resolves each parent by numeric ID and independently by UUID and returns player/world/view identities, respawn readiness and age, entity lifecycle/chunk state, aircraft info, renderer flags, collision state and box, and seat/hitbox parent counts. Result handling is scheduled on the server thread. The audit classifies missing entities, ID collisions, dead or unchunked entities, missing aircraft info, render suppression, invalid collision, and invalid dependent parents. It succeeds only after the client confirms every original ID and UUID in the replacement world.

For a missing or ID-collided parent, the server removes only the exact replacement-player object from that parent's `EntityTrackerEntry`, then asks the same entry to watch the same original server entity. This uses the normal Forge spawn path and preserves the instance, ID, and UUID. The parent is resent before targeted reconsideration of its seats and hitboxes and complete-state synchronization. Correctly resolved parents and unrelated tracker entries are untouched. A parent receives at most two resends per respawn generation, all packet lists are bounded, and a visible timeout is logged at tick 60.

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
