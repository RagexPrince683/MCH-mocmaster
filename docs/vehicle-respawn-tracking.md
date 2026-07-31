# Normal-vehicle respawn tracking and mount ordering

## Status

This document replaces the tracker-repair design from PRs #593 through #598. Those attempts are considered failed. The implementation has been structurally validated, but the required graphical two-client matrix has **not** been run in this environment; consequently this document does not claim that the user-visible issue is fixed.

## Evidence and root cause

In the authoritative capture, the attacker can drive and see the tank while the respawned victim sees neither the tank nor a moving driver. The stationary remote-player body is important: this was not a renderer-only failure. The replacement client world lacked the real parent, vanilla discarded an attach packet whose target did not exist, and the remote player remained an unmounted ghost.

PRs #593–#598 tried to infer client health from server tracker membership and then remove and re-add tracker watchers. That inference was invalid: `tryStartWachingThis` only describes server tracker state, not whether the replacement `WorldClient` retained a spawn. Reflection over `EntityTracker`/`IntHashMap` also proved unsafe on a dedicated server. Probes, reflected tracker lookup, destructive retries, parent/seat/hitbox resends, and watcher mutation have therefore been removed.

History (`git log -S"forceSpawn = true"`) attributes the original flag to `fb6330d`, before the independent render-only LOD snapshot path. The requested LOD commit objects `59d0ce5` and `7f2891f` are not present in this checkout's local object database, but the current snapshot implementation is explicit: snapshots are data held by `MCH_VehicleLODManager`, never entities inserted into `World`. Real-entity force spawning is therefore no longer necessary for normal distant rendering.

## Spawn policy

Normal helicopters, planes, ships, tanks, turrets, and other directly controlled vehicles use `forceSpawn=false`. They follow Forge's normal ordering: respawn installs the replacement world, `PlayerManager` marks the chunk watched, Forge spawns the real parent, and only then does `StartTracking` synchronize MCHeli state. UAV and NewUAV vehicles retain their legacy force-spawn policy after aircraft information is assigned.

LOD snapshots now cover every eligible normal vehicle inside `AircraftLODFarDistance` whose chunk is **not** watched. The former 200-block lower bound is gone, avoiding a gap with low server view distance. A watched chunk receives no snapshot. Snapshots remain render-only, non-ticking, non-collidable, non-interactable data and cannot be mounts.

LOD suppression no longer requires equality with the server's vanilla UUID. A real client vehicle wins first by numeric entity ID, then by non-empty `commonUniqueId` with aircraft type validation. Snapshot state is cleared when the client world unloads or is replaced.

## Observer-directed mount graph

Vanilla `S1BPacketEntityAttach` remains primary. MCHeli additionally sends a bounded `PacketVehicleMountGraph` to **each observer that starts tracking the normal parent**, not merely to the rider. A seat tracking event schedules the same graph without repeating full aircraft state. Thus the respawned victim is an addressed packet recipient whenever Forge starts tracking the parent for that victim.

The graph includes a protocol version, observer identity and dimension, vehicle numeric ID, vehicle `commonUniqueId` and type, a sequence, and bounded pilot/passenger records. Player riders use numeric entity ID for immediate lookup and GameProfile UUID for persistent validation. Seats use numeric ID, index, parent `commonUniqueId`, and the resolved parent. Vanilla entity UUID equality is not a required client identity.

The server queue is keyed by the exact observer object, dimension, vehicle ID, and `commonUniqueId`. It sends at ticks 1, 5, and 10, then expires. It cancels for stale/replaced observers, dimension/world changes, dead vehicles, identity changes, unwatched chunks, range exit, and explicit parent stop-tracking. It never alters tracker entries or sends destroy/spawn packets.

The client queue is scoped to the current world and local-player object, deduplicates vehicle identity and sequence, and retries for at most 100 client ticks. It waits until the real parent, spawn-provided `commonUniqueId`, expected type, seats, and riders all resolve. Player GameProfile identity, seat index, seat parent identity, world membership, and entity liveness are validated before `mountEntity`; both mount back-references are verified afterward. The queue creates, teleports, hides, destroys, and respawns nothing. Bounded `[MCH-MOUNT-SYNC]` diagnostics report receive, wait, apply, or timeout.

The older rider-directed mount notification remains for existing local mount/dismount behavior, but its correction path no longer rejects client entities merely because their vanilla entity UUID differs.

## Runtime test result

No graphical Forge 1.7.10 dedicated-server session with two interactive clients was available in this non-interactive workspace. The exact tank respawn scenario, collision/interaction/seat checks, vehicle category matrix, UAV/NewUAV regression checks, dimension transitions, and low-view-distance LOD transition therefore remain required manual acceptance tests. Do not interpret compilation, packet-send logs, entity IDs, or attacker-client behavior as proof of success. Acceptance requires the respawned victim to see and interact with the original real vehicle and see the remote driver mounted and moving without duplicates or a stationary ghost.
