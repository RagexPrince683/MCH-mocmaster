# Normal vehicle tracking across player respawn

## Root cause and lifecycle fix

Minecraft 1.7.10 replaces `EntityPlayerMP` on respawn while deliberately reusing
the old player's numeric entity ID. `Entity.equals` and `hashCode` use that ID,
and each `EntityTrackerEntry` stores its watching players in a `HashSet`. If an
old watcher survives the replacement boundary, the set therefore reports that
the replacement is already watching an existing vehicle. The normal spawn
packet and Forge `StartTracking` event are then suppressed. The server vehicle
continues to exist, but the client has no real entity to render, collide with,
select, mount, or use.

The server now queues one refresh from Forge's `PlayerRespawnEvent` (and the
corresponding dimension-change event). At the next server-tick end, after the
replacement belongs to the world and `PlayerManager` has installed its watched
chunks, the refresh removes only that player's membership from tracker entries
and asks vanilla `EntityTracker` to rebuild memberships. Vanilla consequently
sends the existing parent vehicle and dependent seat/hitbox entities with their
original IDs and UUIDs. Their ordinary `StartTracking` events retain
`syncCompleteAircraftState` as the post-spawn state synchronization step. The
refresh is removed from the queue immediately and is never repeated per tick.

Enable `EnableMCHLibDebugLog` to record the debug-only
`[MCH-RESPAWN-TRACK] queued`, `refreshed`, and `skipped stale refresh` lifecycle
tags. These include player entity ID, UUID and object identity, world identity,
dimension, chunk, and nearby normal-vehicle count without logging every vehicle
on every tick.

## Synchronization audit

- `3509e4419f6f37ef238d730b151302eb934211e4` is the pre-synchronization base.
- PR #587 (`1d293104335744b46da4cbf08dad58915f4dc3e9`, merged as
  `3287a81d3bfe00fa15eaa520c1e11df15d2765f3`) cleared client LOD and pending
  mount caches at death/world/player replacement, reset real vehicles' LOD
  render flag on join, and added bounded normal-mount correction. Clearing the
  render-only cache exposed the absent real entity; it did not remove a server
  vehicle or cause the interaction failure.
- PR #592 (`b328858acbf1b060be429d05cf3adbc2480558ac`, merged as
  `c3add9e638143c8de3889f9d9e37a24ee9e52b4e`) excluded the normal 200-block
  range from snapshots, keyed snapshots and mount generations by UUID, removed
  the unsafe global tracker-range reflection, and cleaned dead normal riders.
  The tighter snapshot boundary made the tracking hole unambiguous. The UUID,
  mount-sequence, and dead-rider changes remain intact because none supplies or
  replaces a missing entity spawn packet.

The former LOD image was only plain render data, so it could visually mask the
missing tracked entity while providing no collision or interaction target. This
fix restores authoritative real entities; it does not restore snapshots inside
200 blocks, create fake client entities, or make snapshots interactable. UAV and
NewUAV code paths are unchanged.

## Dedicated-server verification status

The automated source environment does not provide two graphical Forge clients,
so the full two-client death/respawn matrix must be exercised in a manual game
session. The source-level audit verifies that the refresh uses Forge 1.7.10's
server event bus and vanilla tracker APIs, is delayed until the player is in the
world, preserves entity identity, and runs once per lifecycle transition.
