# Server Administration Guide

This guide focuses on safe defaults and operational practices for multiplayer servers.

## Installation checklist

- Install Forge 1.7.10 on the server.
- Put the MC Helicopter Overdrive+ jar in `mods/`.
- Install the same MCHeli asset/content pack expected by your clients.
- Start once to generate `config/mcheli.cfg`.
- Stop the server and review world-damage, command, and permission settings.
- Require clients to use the same mod and compatible assets.

## Recommended survival-server baseline

```text
EnableCommand = true
PlaceableOnSpongeOnly = true
Explosion_DestroyBlock = false
Explosion_FlamingBlock = false
Collision_DestroyBlock = false
Collision_EntityDamage = true
Collision_EntityTankDamage = false
InfinityAmmo = false
InfinityFuel = false
DropItemInCreativeMode = false
EnableDebugBoundingBox = false
```

This limits accidental terrain damage while preserving vehicle combat and admin tooling.

## Command permissions

Operators can use all MCHeli commands. For non-operators, grant only the exact subcommands required:

```text
CommandPermission = status:Moderator1
CommandPermission = modlist:Moderator1, Moderator2
CommandPermission = reconfig:AdminHelper
```

Avoid granting destructive commands (`fill`, `killentity`, `removeentity`, `attackentity`) to normal users.

## Runtime reloads

Use:

```text
/mcheli reconfig
```

This reloads the server config and sends updated server settings to clients. Some client-only rendering/keybind changes still require client restart or local config reload.

## Entity cleanup and diagnostics

Useful diagnostics:

```text
/mcheli status entity 5
/mcheli status tile 5
```

Emergency cleanup examples:

```text
/mcheli removeentity EntityBullet
/mcheli killentity EntityParachute
```

Be careful: matching is a case-insensitive substring search against full Java class names and excludes players only.

## Logging and diagnostics

- Leave the legacy `McHeliOutputDebugLog` marker disabled for normal servers. Startup milestones, system checks, warnings, and errors still log without it.
- Enable `McHeliOutputDebugLog` only while diagnosing noisy internals such as per-item registration, ore dictionary confirmations, language-entry registration, reload traces, and similar spam-level debug output.

## Performance notes

- `EnableAircraftLODRender`, `AircraftLODStartDistance`, and `AircraftLODFarDistance` are client-side rendering aids for far vehicle snapshots.
- `MultiThreadedModelLoading = true` enables threaded model loading and is on by default.
- Large asset packs can increase startup time and memory use.
- `RenderDistanceWeight` and `MobRenderDistanceWeight` can affect how far mod entities render.

## Pack policy recommendations

- Publish the exact asset pack version with your server pack.
- Keep a known-good copy of `mcheli.cfg` under version control outside the live server.
- Disable terrain damage before public events unless vehicle griefing is intended.
- Test every added vehicle definition in a staging world before deployment.
