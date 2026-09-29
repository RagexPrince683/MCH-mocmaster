# Command and Permission Reference

All commands use the root command:

```text
/mcheli <subcommand> ...
```

Commands only run when `EnableCommand = true` in `config/mcheli.cfg`.

## Permission model

The command class allows the root command for everyone, then checks each subcommand before execution. Operators/players who can use vanilla `/gamemode` pass automatically. Non-operators need `CommandPermission` entries in `mcheli.cfg`.

Format:

```text
CommandPermission = commandName:PlayerName1, PlayerName2
```

Examples:

```text
CommandPermission = modlist:Alice, Bob
CommandPermission = status:ServerMod
CommandPermission = reconfig:AdminHelper
```

Permissions are per subcommand. Granting `status` does not grant `fill` or `killentity`.

## Subcommands

| Command | Syntax | Purpose |
| --- | --- | --- |
| `list` | `/mcheli list` | Prints the available MCHeli subcommands. |
| `reconfig` | `/mcheli reconfig` | Reloads `mcheli.cfg`; on servers, broadcasts updated server settings to clients. |
| `reload` | `/mcheli reload` | Re-scans configuration and client assets, then notifies connected clients. |
| `sendss` | `/mcheli sendss <playerName>` | Sends a client packet requesting/triggering screenshot-related client handling for the named player. |
| `modlist` | `/mcheli modlist <playerName>` | Requests mod-list information from the named player. |
| `title` | `/mcheli title <timeSeconds> <position> <jsonMessage>` | Sends a JSON chat title/message packet to clients. Time is clamped to 1-180 seconds; position is clamped by code to 0-5. |
| `fill` | `/mcheli fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [metadata] [oldBlockHandling] [dataTag]` | MCHeli copy of a fill/setblock-style admin utility. `oldBlockHandling` supports `replace`, `destroy`, `keep`, and `override` in tab completion. |
| `status` | `/mcheli status <entity|tile> [minNum]` | Counts loaded entity or tile-entity classes in the sender's world and prints classes with at least `minNum` instances. |
| `killentity` | `/mcheli killentity <entityClassNameFragment>` | Calls `setDead()` on matching loaded non-player entities. |
| `removeentity` | `/mcheli removeentity <entityClassNameFragment>` | Marks matching loaded non-player entities dead by setting `isDead = true`. |
| `attackentity` | `/mcheli attackentity <entityClassNameFragment> <damage> [damageSource]` | Damages matching loaded non-player entities. |
| `showboundingbox` | `/mcheli showboundingbox <true|false>` | Toggles MCHeli debug bounding boxes and broadcasts server settings. This does not save the config file. |
| `debugwheels` | `/mcheli debugwheels [true|false]` | Toggles live wheel/suspension diagnostics for the civilian car the sender is riding. Omit the argument to report the current setting. Players only; the client HUD requires Test mode. |
| `enablenukes` | `/mcheli enablenukes [true|false]` | Without an argument, prints usage and the current MCHeli nuke status. With `true` or `false`, changes whether MCHeli HBM-style nuclear weapon effects are enabled, broadcasts the colored `ENABLED`/`DISABLED` state, and plays the Wither spawn sound for players. When HBM/NTM registers `/ntmenablenukes`, this subcommand mirrors that command's current status and forwards changes back to HBM. |

## Live wheel diagnostics

`/mcheli debugwheels true` enables a session-local setting for the sender;
`/mcheli debugwheels false` disables it and clears the display. The normal command
permissions apply (`CommandPermission = debugwheels:PlayerName` for non-operators).
Enable Test Mode in the client configuration, then ride a civilian car directly or
in one of its seats. Remote control and nearby vehicles do not qualify. Leaving
the car clears its snapshot; the setting remains enabled for the next car ride.
Reconnects start with the setting disabled. `CarGripDiagnostics` is not required,
and this command does not enable CSV output or continuous chat/console logging.

The Test mode HUD replaces its general variable dump with two diagnostic columns
while this display is active. Every value is prefixed `S` (authoritative server)
or `C` (client). Server snapshots are sent only to subscribed riders after each
server tick through the existing packet channel. The server supplies each wheel's
configured axle, authored local position, settled world position, physical grip
contact probe, suspension support, compression, filtered compression rate,
support height, and configured travel limit. Compression and support are the
post-movement reconciliation values; rate is the rate used by that tick's spring
and damping calculation. Distances are blocks, movement/velocity are blocks per
tick, compression rate is blocks per tick, pitch is degrees, RPM is revolutions
per minute, throttle is 0–1, and drive force is the drivetrain's longitudinal
velocity increment per tick.

Server contact counts and horizontal velocity are end-of-tick state. Drive contact
counts and force are captured at acceleration; terrain sample result, axle heights
and counts, terrain pitch, target pitch, accepted body pitch, pose decision, step
gate, selected path, effective step budget, requested/accepted movement, and
horizontal velocity cleanup describe the actual server movement decision.
Accepted XYZ is the collision solver's translation; total accepted Y also includes
the pose lift/settle. Input X/Z precedes movement's existing slowdown handling.
The displayed normal/step candidates compare X-then-Z, Z-then-X, and diagonal
sweeps. Every evaluated step/rotation candidate reports rise, horizontal progress,
final support, clearance and its decision against the best path at that point.
`eligible` means it became the best candidate at evaluation; `selected path` identifies
the final winner. A rotation rejected before its sweep/landing has unsampled fields
marked `N/A`. Clipped sweeps and rejected rotations identify the configured collision
component (0 is primary, subsequent indices follow BoundingBox order) and the block
position or entity ID supplied by the existing collision collector. These are existing
decisions, not alternate movement simulations.

Pre-move velocity is captured on entry to movement, after drive, drag and grip;
post-move velocity is captured after blocked-axis cleanup. Requested translation
includes movement's slowing-block adjustment. Forward/side translations use the
displayed accepted server yaw. Each blocked axis explicitly reports the condition
that zeroes its velocity; translation and retained velocity are separate quantities.

Client body pitch and horizontal velocity are displayed separately for comparison.
Unavailable, uninitialized, missing, or expired snapshot values show `N/A`.
The header remains on every page and shows the server vehicle tick and packet age
in monotonic milliseconds since client receipt. A current movement capture has the
same decision tick as the vehicle tick; uncaptured decisions are explicitly labeled.
Missing snapshots distinguish no server car, observer/dimension packet rejection,
vehicle entity ID or synchronized identity mismatch, and expiry. Identity uses the
mod's common vehicle ID synchronized in spawn data, rather than the independent
vanilla entity UUID generated by the client. Packet age is delivery freshness, not
a measurement of network latency. Values use six decimal places so small force and
sideways translation remain visible.
Both columns wrap inside the active GUI viewport. Long traces use PgUp/PgDn pages
instead of shrinking text below native font size (two physical pixels per font pixel
when GUI scaling permits); the background encloses both columns and the footer.
Snapshots expire after one second without an update and are discarded on vehicle
changes, death, world unload, or reconnect. Detailed per-column terrain
rejection causes are not inferred by the HUD. Normal-path landing is not
sampled by the existing trace and is explicitly `N/A`.

## `attackentity` damage sources

Recognized names include:

```text
player, anvil, cactus, drown, fall, fallingBlock, generic,
inFire, inWall, lava, magic, onFire, starve, wither
```

Tab completion also advertises `outOfWorld`, but the implementation does not assign a special `DamageSource` for it; unrecognized values fall back to generic damage.

## Examples

Reload server config:

```text
/mcheli reconfig
```

Show classes for loaded entities with at least 10 instances:

```text
/mcheli status entity 10
```

Remove all loaded entities whose class name contains `EntityBullet`:

```text
/mcheli removeentity EntityBullet
```

Display a JSON title for 5 seconds at position 2:

```text
/mcheli title 5 2 {"text":"Objective updated","color":"gold"}
```

Enable debug bounding boxes for connected clients:

```text
/mcheli showboundingbox true
```

## Development live reload

The Development GUI deliberately provides four separate scopes:

- **Reload vehicle configuration settings** targets only the definition selected by the
  MCHeli vehicle the player currently rides or controls. The server validates that entity,
  replaces one manager entry, applies it only to that entity, and the client reloads only
  that definition's main/part models and item/LOD caches. Pressing the button immediately
  closes the GUI and unpauses an integrated game; completion is reported in chat. A pending
  request is owned by the client proxy rather than the closed screen and is cleared by a
  response, a ten-second timeout, disconnect, or world replacement. The button stays
  disabled while a request is pending; seat-count changes are rejected as restart-required.
- **Reload All Weapons** reloads weapon definitions. It does not reload vehicle definitions.
- **Reload All HUD** reloads HUD definitions. It does not trigger the vehicle button.
- **`/mcheli reload`** remains the expensive complete development reload for all information
  managers and client assets, including resource, texture, sound, HUD, and model work.

The targeted vehicle button never scans loaded worlds or other vehicles with the same name,
and does not recreate seats or reset the selected vehicle's riders and runtime state.
Most definition values are read through the newly installed information snapshot. Runtime
dimensions, step height, extra collision boxes, camera bounds, force-spawn policy, and APS
timings/range/capacity are refreshed explicitly. Changing the combined seat/rack count still
requires a restart, as does adding content that requires registration of a new Minecraft item.

In a repository development run, `/mcheli reload` reads the editable
`src/main/resources/assets/mcheli` tree directly. Running `processResources`, relogging, and
restarting the world are not required. Resolution is deterministic: an external
`mcheli_addons` override wins first, followed by the editable source tree, an ordinary
classpath resource directory, and finally a packaged/development JAR. When the editable
tree is present it is authoritative, so deleting a source asset cannot reveal an old copy
from `build/resources/main` or a development JAR.

The client-thread reload covers vehicle, weapon, item, throwable, and HUD definitions;
MQO, OBJ, and TCN models; textures; `sounds.json`; and referenced OGG files. Existing
vehicles retain seats, riders, ownership, locks, ammunition, fuel, health, damage,
throttle, and weapon state while definition-derived objects are refreshed. Definitions
which require a newly registered Minecraft item still print the existing restart warning.

### Manual verification

For the targeted vehicle button:

1. Start `runClient`, enter an existing MCHeli vehicle, and open the Development GUI.
2. Change a visible value in that vehicle's source `.txt` and press **Reload vehicle configuration settings**.
3. Confirm the GUI closes immediately, gameplay resumes without **Save & Close**, chat reports success, and the edited value is active.
4. Confirm no controlled-vehicle-change error appears, then edit the selected MQO/OBJ model and repeat.
5. Confirm only the selected entity/model changes and that seats, riders, fuel, health, ammunition, ownership, locks, and throttle remain unchanged.
6. Repeat several times and check for crashes, stale requests, duplicate seats, or display-list growth.

For the complete reload command:

1. Start `runClient`, enter a world, and use an existing MCHeli vehicle.
2. Edit its source `.txt`, run `/mcheli reload`, and confirm the value changes in place.
3. Repeat after editing its MQO or OBJ model and then a texture.
4. Repeat after editing a HUD file, `sounds.json`, or an OGG asset.
5. Add a configuration, reload, and confirm discovery or the explicit item-registration restart warning.
6. Delete a configuration, reload, and confirm no generated copy restores it.
7. Reload several times and check for crashes, duplicate seats, leaked display lists, or overlapping model jobs.

## Safety notes

- `fill`, `killentity`, `removeentity`, and `attackentity` are destructive admin tools. Grant them only to trusted users.
- Entity matching uses case-insensitive substring matching against Java class names. A broad fragment can affect more entities than intended.
- `showboundingbox` changes the in-memory setting but the save call is commented out in source, so restart/reload behavior depends on `EnableDebugBoundingBox` in `mcheli.cfg`.
## Technology progression

- `/mcheli tier get` reports the world's unlocked MC Heli tier.
- `/mcheli tier set <tier>` accepts `0.0` through `5.0` in `0.5` increments and immediately synchronizes connected clients.
- `/mcheli tier item` inspects the held vehicle item's identifier, year, override, resolved requirement, server tier, and lock state.

The existing MC Heli command permission system applies. MC Heli progression is persisted and synchronized independently of HMG.
