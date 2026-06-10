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
| `sendss` | `/mcheli sendss <playerName>` | Sends a client packet requesting/triggering screenshot-related client handling for the named player. |
| `modlist` | `/mcheli modlist <playerName>` | Requests mod-list information from the named player. |
| `title` | `/mcheli title <timeSeconds> <position> <jsonMessage>` | Sends a JSON chat title/message packet to clients. Time is clamped to 1-180 seconds; position is clamped by code to 0-5. |
| `fill` | `/mcheli fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [metadata] [oldBlockHandling] [dataTag]` | MCHeli copy of a fill/setblock-style admin utility. `oldBlockHandling` supports `replace`, `destroy`, `keep`, and `override` in tab completion. |
| `status` | `/mcheli status <entity|tile> [minNum]` | Counts loaded entity or tile-entity classes in the sender's world and prints classes with at least `minNum` instances. |
| `killentity` | `/mcheli killentity <entityClassNameFragment>` | Calls `setDead()` on matching loaded non-player entities. |
| `removeentity` | `/mcheli removeentity <entityClassNameFragment>` | Marks matching loaded non-player entities dead by setting `isDead = true`. |
| `attackentity` | `/mcheli attackentity <entityClassNameFragment> <damage> [damageSource]` | Damages matching loaded non-player entities. |
| `showboundingbox` | `/mcheli showboundingbox <true|false>` | Toggles MCHeli debug bounding boxes and broadcasts server settings. This does not save the config file. |

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

## Safety notes

- `fill`, `killentity`, `removeentity`, and `attackentity` are destructive admin tools. Grant them only to trusted users.
- Entity matching uses case-insensitive substring matching against Java class names. A broad fragment can affect more entities than intended.
- `showboundingbox` changes the in-memory setting but the save call is commented out in source, so restart/reload behavior depends on `EnableDebugBoundingBox` in `mcheli.cfg`.
