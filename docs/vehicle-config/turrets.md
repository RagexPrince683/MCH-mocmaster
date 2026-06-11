# Turret/static weapon config values

Turret/static weapons live in the `vehicles` directory, inherit shared base keys, and add a small set of movement/part keys.

## Turret-only keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `canmove` | boolean | false | Enables movement for the turret vehicle. Static weapons usually leave this false. |
| `canrotation` | boolean | false | Enables base/body rotation. |
| `rotationpitchmin` | float | `-90` | Legacy alias that forwards to `MinRotationPitch`. |
| `rotationpitchmax` | float | `90` | Legacy alias that forwards to `MaxRotationPitch`. |
| `addpart` | `drawFP,rotYaw,rotPitch,type,x,y,z[,recoilBuf]` | none | Adds a visual part. `drawFP` controls first-person draw, `rotYaw`/`rotPitch` follow aim, `type` is the part type id, `recoilBuf` defaults 0. |
| `addchildpart` | same as `addpart` | none | Adds a child part to the most recently added part. |

## Turret defaults that differ from base

- Default pitch range is `-90..90`.
- Default HUD is `turret` for every seat.
- Directory/kind names are `vehicles` / `turret`.

## Minimal static weapon config

```ini
displayname = Minimal Static MG
Category = EXAMPLE.TURRET
addtexture = minimal_static_mg
AddSeat = 0.0, 0.6, 0.0
HUD = turret
maxhp = 60
canmove = false
canrotation = true
rotationpitchmin = -20
rotationpitchmax = 45
addpart = true, true, true, 0, 0.0, 0.8, 0.0, 0.05
```

## Safe-to-omit notes

For a simple invisible-base static weapon, omit `addpart`/`addchildpart`. For a fully static emplacement, omit or set `canmove=false` and `canrotation=false`.
