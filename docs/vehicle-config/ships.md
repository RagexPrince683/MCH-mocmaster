# Ship/boat config values

Ships are an existing vehicle category in this codebase. They inherit shared keys and reuse several plane visual/VTOL parser keys, but do not use the fixed-wing aerodynamic keys from `MCP_PlaneInfo`.

## Ship-only and ship-supported keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `PreventWaterBobbing` | boolean | false | Ship-specific water motion flag. |
| `addpartrotor`, `addblade`, `addpartwing`, `AddPartPylon`, `addpartnozzle` | lists matching plane visual keys | none | Visual/animation compatibility with plane-style parts. |
| `variablesweepwing`, `sweepwingspeed`, `enablevtol`, `defaultvtol`, `vtolyaw`, `vtolpitch`, `enableautopilot` | same as plane visual/VTOL keys | same as plane defaults | Parsed by ships for compatibility/animation behavior. |

## Ship defaults that differ from base

- `getMaxSpeed()` is 1.8, so `speed` and `sweepwingspeed` clamp to lower values than planes.
- `speed` and `sweepwingspeed` are multiplied by global `AllShipSpeed` during validation.
- Default `RotorSpeed = 47.94`, `camerazoom = 8`.
- HUD defaults currently mirror planes: `plane`, `plane`, then `gunner`.

## Minimal ship config

```ini
displayname = Minimal Boat
Category = EXAMPLE.SHIP
addtexture = minimal_boat
AddSeat = 0.0, 0.8, 0.0
HUD = plane
float = true
speed = 0.45
maxhp = 140
PreventWaterBobbing = true
```
