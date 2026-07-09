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


## Overdrive naval, carrier, and submarine behavior

Ships are more than static boats in Overdrive. Large ship configs can use `BoundingBox` entries as walkable deck surfaces: the ship entity detects players standing on the base box or extra boxes, carries them with ship movement/yaw changes, corrects small upward deck movements, and keeps fall distance reset while the player remains on the deck. Use broad, flat extra boxes for carrier decks and set `PreventWaterBobbing = true` when a stable carrier surface is more important than visible buoyancy.

Carrier and vehicle-on-vehicle behavior uses the shared rack system. Parent ships define compatible parking/launch slots with `AddRack`, and child aircraft or vehicles can opt into specific parent racks with `RideRack`. Child vehicles can set `CanMountShip = false` to opt out of all ship/carrier racks, which is useful for oversized aircraft such as heavy bombers. Ship racks launch carried aircraft with forward/vertical assist and temporary no-collision grace against the carrier; non-launch dismounts can fall back to parachute behavior when there is no safe surface below.

Submarine behavior is implemented on the ship entity. Toggling the ship VTOL/Extra mode starts diving when the ship is submerged, disables normal floating while diving, and enables `KeySubmarineAscend` / `KeySubmarineDescend` depth controls. Diving submarines apply bounded vertical acceleration, clamp vertical speed, and damp vertical motion when no depth key is pressed. This is current ship behavior rather than a separate submarine config type.

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
