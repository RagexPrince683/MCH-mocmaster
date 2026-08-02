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
| `enablenukes` | `/mcheli enablenukes [true|false]` | Without an argument, prints usage and the current MCHeli nuke status. With `true` or `false`, changes whether MCHeli HBM-style nuclear weapon effects are enabled, broadcasts the colored `ENABLED`/`DISABLED` state, and plays the Wither spawn sound for players. When HBM/NTM registers `/ntmenablenukes`, this subcommand mirrors that command's current status and forwards changes back to HBM. |

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
