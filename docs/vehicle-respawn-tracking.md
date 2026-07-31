# Normal vehicle tracking across player respawn

## Corrected diagnosis

PR #593 / merge `1244d0ca86765817bf89e11b4a6079086bd2a7a5` claimed that a stale
`EntityPlayerMP` remained in `EntityTrackerEntry.trackingPlayers`. The supplied run
contains no lifecycle messages (they were hidden behind `EnableMCHLibDebugLog=false`),
so that run neither proves the claim nor proves that the old refresh ran.

The 1.7.10 source audit rejects the claim as a generally valid root cause. During
`ServerConfigurationManager.respawnPlayer`, vanilla calls
`removePlayerFromTrackers(oldPlayer)` before constructing and spawning the replacement.
A reused numeric ID alone therefore does not leave an old watcher. A stale watcher is
now reported if it actually occurs; it is not assumed.

The failed fix introduced a deterministic tracking hole: one tick after respawn it
sent destroy packets with `removePlayerFromTrackers(replacement)`, then called global
`updateTrackedEntities()`. That update is movement-driven and does not promise to
reconsider every stationary tracker entry for that player. It also removed the queue
record regardless of the result. The first divergence created by that path is stage 3:
the server vehicle and tracker entry exist, while the replacement player is absent
from the entry; consequently no real client entity can remain after the destroy packet.

## Repair

The unconditional destructive removal and global update are gone. A lifecycle record
is keyed by UUID plus replacement-object identity and retains the exact replacement
reference. For up to 40 server ticks it verifies, by exact object identity, every
nearby normal parent vehicle in Forge's tracker view and independently verifies that
`PlayerManager` watches its chunk. Missing entries are reconsidered only with
`EntityTracker.func_85172_a(replacement, chunk)`, and only after that chunk is watched.
The record ends immediately when all expected parents track the replacement, or logs a
visible timeout. Ordinary `StartTracking` remains responsible for dependent entities
and calls `syncCompleteAircraftState` only after the real parent starts tracking.

This does not expand tracker ranges or alter LOD snapshots. Snapshot displays remain
render-only and excluded inside 200 blocks. UAV, NewUAV, station, inventory, remote
camera, and safe-return paths are unchanged.

## Audit logging

Essential evidence uses the normal `mcheli` logger even when debug logging is off and
has prefix `[MCH-RESPAWN-AUDIT]`:

- startup `registered` lines identify the Forge and FML event buses;
- `death`, `clone`, `queue`, and client `dead`/`replacement+N` lines delimit lifecycle;
- `readiness`, `attempt`, `success`, and `timeout` show the bounded targeted repair;
- `start`, `stop`, and `syncCompleteAircraftState` prove Forge tracking transitions;
- client snapshots at replacement ticks 0, 1, 5, 20, and 40 report player/world/view
  identity and real-vehicle versus LOD-display counts.

A graphical dedicated-server/client run is still required to capture per-vehicle
server/client resolution evidence. This repository change does not claim that such a
run was performed in the source-only environment.

## Commit and PR audit

PR #587 (`1d293104...`, merge `3287a81...`) reset client LOD/mount caches and render
state. PR #592 (`b328858...`, merge `c3add9e...`) restored the 200-block snapshot
boundary and UUID mount generations. PR #593 (`75610d3...`, merge `1244d0c...`) added
the unverified one-tick destructive refresh described above. The current repair keeps
the useful #587/#592 LOD and mount behavior and replaces only #593's refresh.
