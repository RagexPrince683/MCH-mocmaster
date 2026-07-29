# Getting Started with MC Helicopter Overdrive+

This guide explains the basic workflow for installing, finding, placing, and operating the mod's content.

## 1. Install the mod and assets

MC Helicopter Overdrive+ is code plus MCHeli-style content assets. The Java mod registers systems, items, commands, entities, and rendering; the content pack provides most vehicle definitions, models, textures, HUD files, sounds, and recipes.

Expected content folders include:

```text
assets/mcheli/hud
assets/mcheli/weapons
assets/mcheli/helicopters
assets/mcheli/planes
assets/mcheli/ships
assets/mcheli/tanks
assets/mcheli/vehicles
assets/mcheli/item
assets/mcheli/throwable
assets/mcheli/models
assets/mcheli/textures
assets/mcheli/sounds
assets/mcheli/sounds.json
```

In a normal Minecraft install, put the mod jar and matching content in `mods/`. In the development run directory, the code expects assets under `build/run/mods/mcheli/`.

## 2. Start the game once

The first launch creates `config/mcheli.cfg`. The mod writes the file back out with current defaults, command permission examples, damage multipliers, ignored projectile classes, and key configuration values.

## 3. Find content in creative mode

The mod creates several creative tabs:

- `MCHeliO Item`
- `MCHeliO Recipe Items`
- `MCHeliO Helicopters`
- `MCHeliO Planes`
- `MCHeliO Ships`
- `MCHeliO Tanks`
- `MCHeliO Vehicles`

If a tab is empty or an expected vehicle is missing, verify that the matching asset folder is installed and that the content definition loaded without errors.

## 4. Craft or place the Drafting Table

The mod registers a **Drafting Table** and a lit Drafting Table variant. The Drafting Table is the main recipe interface for MCHeli content when recipes are enabled.

Default Drafting Table recipe:

```text
"R  ", "PCP", "F F", R, redstone, C, crafting_table, P, planks, F, fence
```

Set `ItemRecipe_DraftingTable` in `mcheli.cfg` to change or disable the recipe according to the recipe parser behavior used by your pack.

## 5. Place and use vehicles

- Vehicle placement is controlled by item definitions and global settings.
- UAV vehicle items are not placed with the normal hold-to-deploy flow. Large UAVs must be placed and controlled from a UAV Station; small UAVs use a UAV Station or Portable UAV Controller when supported.
- If `PlaceableOnSpongeOnly = true`, vehicle placement is restricted to sponge blocks.
- Global speed scalars are controlled by `AllHeliSpeed`, `AllPlaneSpeed`, `AllShipSpeed`, and `AllTankSpeed`.
- Fuel and ammunition requirements are controlled by content definitions and global `InfinityFuel`/`InfinityAmmo` settings.

## 6. Default controls

The config stores key codes rather than names. Common defaults:

| Action | Config key | Default |
| --- | --- | --- |
| Forward/up | `KeyUp` | W |
| Back/down | `KeyDown` | S |
| Right | `KeyRight` | D |
| Left | `KeyLeft` | A |
| Switch gunner/mode | `KeySwitchGunner` | H |
| Switch hovering | `KeySwitchHovering` | Space |
| Use weapon | `KeyUseWeapon` | Right Click |
| Attack/current lock | `KeyAttack`, `KeyCurrentWeaponLock` | Left Click |
| Switch weapon 1 | `KeySwitchWeapon1` | Middle Click |
| Switch weapon 2 | `KeySwitchWeapon2` | G |
| Switch weapon mode | `KeySwitchWeaponMode` | X |
| Zoom | `KeyZoom` | Z |
| Camera mode | `KeyCameraMode` | C |
| Dismount vehicle | Minecraft Sneak control | Hold the configured Sneak key for 3 seconds (Left Shift by default) |
| Dismount mob/crew action | `KeyUnmountMob` | Y |
| Flares/chaff/maintenance/APS | `KeyFlare`, `KeyChaff`, `KeyMaintenance`, `KeyAPS` | V |
| Extra function | `KeyExtra` | F |
| Hold free look | `KeyFreeLook` | Left Control |
| Plane look ahead | `KeyPlaneLookAhead` | Left Alt |
| Open MCHeli GUI | `KeyGUI` | R |
| Gear | `KeyGearUpDown` | B |
| Rack up/down | `KeyPutToRack`, `KeyDownFromRack` | J / U |
| Scoreboard | `KeyScoreboard` | L |
| Multiplayer manager | `KeyMultiplayManager` | M |

## 7. Useful first configuration changes

For survival-friendly servers:

```text
Explosion_DestroyBlock = false
Collision_DestroyBlock = false
InfinityAmmo = false
InfinityFuel = false
PlaceableOnSpongeOnly = true
```

For creative testing:

```text
InfinityAmmo = true
InfinityFuel = true
EnableDebugBoundingBox = true
```

## 8. Next steps

- Read [Configuration Reference](configuration.md) for defaults and server/client impact.
- Read [Command and Permission Reference](commands.md) before granting admin tools.
- Read [Server Administration Guide](server-administration.md) for safe server defaults.
