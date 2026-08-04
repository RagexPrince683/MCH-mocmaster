# Shared/base vehicle config values

Applies to planes, helicopters, tanks, turret/static weapons, ships, and any other subclass of `MCH_BaseVehicleInfo`. These keys are inherited by vehicle-specific parsers, so a plane config can use both the shared keys here and the plane-only keys in `planes.md`.

## Omission and compatibility rules

- All shared values are optional, but a vehicle must end up with at least one seat. UAV-style configs can rely on `UAV`, `NewUAV`, or `TargetDrone` to create a hidden seat during validation.
- If a key is omitted, the constructor default in the main reference is used.
- Legacy lowercase keys (`displayname`, `itemid`, `speed`, `throttleupdown`, etc.) are still accepted.
- Several visual keys are repeatable and attach to the most recently created parent part, such as weapon children, pylon children, or turret children.

## Identity, recipes, textures

| Key | Type | Default | Notes |
|---|---|---:|---|
| `displayname` | string | config name | Main translated/untranslated display name. |
| `adddisplayname` | `lang, text` | none | Adds a localized display name entry. |
| `Category` | string | `zzz` | Uppercased; commas/semicolons/colons become dots and spaces are removed. |
| `itemid` | int[0..65535] | 0 | Legacy compatibility id. |
| `addtexture` | string | config name | Adds a lowercase texture variant. |
| `addrecipe` / `addshapelessrecipe` | recipe | none | Recipe definitions consumed by the base info manager. |

## Seats, camera, HUD

| Key | Type | Default | Notes |
|---|---|---:|---|
| `AddSeat` | `x,y,z[,rotYaw,rotPitch,...]` | none | Adds a normal seat. At least one seat is required unless a UAV flag creates one. A vehicle can define up to 500 combined seats and racks. |
| `AddGunnerSeat` | list | none | Adds a gunner seat. |
| `AddFixRotSeat` | list | none | Adds a fixed-rotation seat. |
| `ExclusionSeat` | seat ids | none | One-based seat ids; parser stores zero-based exclusions. |
| `HUD` | HUD names | family default | Client-side; clears previous HUD list and loads named HUDs. |
| `cameraposition` | `x,y,z[,alwaysCameraView,fixRot,fixYaw]` | auto `0,0,0` | Also controls the `alwaysCameraView` flag through field 4. |
| `camerazoom` | int[1..10] | family default | Base default 1; plane/heli/tank/ship use 8. |
| `CameraRotationSpeed` | float[0..10000] | 1000 | Camera-part rotation speed. |
| `DefaultFreelook` | boolean | false | Enables freelook by default. |
| `ThirdPersonDist` | float[4..100] | 4 | Third-person camera distance. |
| `UnmountPosition` | vec3 | vanilla/MCHeli default | Custom dismount point. |

## Movement and shared flight/ground physics

| Key | Type/range | Default | Practical tuning notes |
|---|---:|---:|---|
| `speed` | float[0..family max] | 0.1 | Family global multipliers are applied during validation. Plane/tank max is 4, ship max is 1.8. |
| `MotionFactor` | float[0..1] | 0.96; plane constructor 0.97 | Per-tick horizontal damping after acceleration. Higher values retain more speed. |
| `gravity` | float[-50..50] | -0.04 | Vertical acceleration in air. |
| `gravityinwater` | float[-50..50] | -0.04 | Vertical acceleration while in water. |
| `NewFlightGravity` / `FlightGravity` / `GravityOverride` | float[0..1] | global `NewFlightGravity` | Per-vehicle downward acceleration override for new-flight-model aircraft only. Omit to use the global config default. |
| `float` | boolean | false | Enables floating/water behavior. |
| `floatoffset` | float | 0 | Parser stores the negative of the supplied value. |
| `FlightCeiling` | float[32..37650] | 9100 | Soft ceiling. Lift fades instead of hard-clamping altitude. |
| `FlightCeilingRange` | float[1..128] | 24 | Vertical fade band below `FlightCeiling`. Larger values make the ceiling less abrupt. |
| `MobilityYaw` / `MobilityPitch` / `MobilityRoll` | float[0..100] | 1 | Axis control multipliers. Planes feed these into the newer damped angular-rate model. |
| `MobilityYawOnGround` | float[0..100] | 1 | Ground yaw multiplier. |
| `MinRotationPitch` / `MaxRotationPitch` | float | family default | Setting either key enables pitch limiting. |
| `MinRotationRoll` / `MaxRotationRoll` | float | family default | Setting either key enables roll limiting. |
| `throttleupdown` | float[0..3] | 1 | Pilot throttle step multiplier. |
| `ThrottleUpDownOnEntity` | float[0..100000] | 2 | Throttle multiplier when moving on a carried/ridden entity. |
| `EnableBack` | boolean | false | Enables reverse throttle/backing behavior. |
| `ThrottleDownFactor` | float[0..10] | 1 | Reverse/deceleration tuning. |
| `PivotTurnThrottle` | float[0..1] | 0 | If turning on the ground below this throttle, code can auto-raise throttle for pivot turning. |
| `CanMoveOnGround` / `CanRotOnGround` | boolean | true | Gates ground movement/yaw. |
| `ongroundpitch` | float[-90..90] | 0 | Parser stores the negative value. |
| `OnGroundPitchFactor` / `OnGroundRollFactor` | float[0..180] | 0 | Ground alignment helpers. |

### Ceiling formula

```text
ceilingLift = clamp((FlightCeiling - altitude) / max(1, FlightCeilingRange), 0, 1)
if ceilingLift < 1:
    upward motion is damped
    vehicle receives extra sink proportional to (1 - ceilingLift)
```

Planes use a sink of `0.012 * (1 - ceilingLift)` away from the runway; helicopters use `0.014 * (1 - ceilingLift)`.

For fixed-wing planes using `useNewMobilitySystem = true`, this gravity value is the weight side of the lift/weight calculation documented in `planes.md`; valid wing lift is still limited by airspeed, velocity-derived AoA, stall lift loss, takeoff/climb headroom suppression while stalled, thrust-to-weight during vertical climb, and pitch-break stall behavior rather than by pitch attitude alone. Legacy planes and non-fixed-wing vehicles continue to use their family-specific gravity behavior.

## Health, damage, resources, support

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `maxhp` | int[1..100000] | 50 | Vehicle health. |
| `damagefactor` | float[0..1] | 0.2 | Global incoming damage multiplier. |
| `SubmergedDamageHeight` | float[-1000..1000] | 0 | Height threshold for submerged damage behavior. |
| `ArmorDamageFactor` | float[0..10000] | 1 | Armor multiplier. |
| `ArmorMinDamage` / `ArmorMaxDamage` | float[0..1000000] | 0 / 100000 | Armor damage clamp. Constructor field for max is 100000, parser allows up to 1000000. |
| `ExplosionSizeByCrash` | int[0..100] | 5 | Explosion size when destroyed/crashed. |
| `EngineShutdownThreshold` | int[0..100] | 20 | Engine shutdown below this health percentage. For non-floating tanks, planes, and helicopters submerged by `SubmergedDamageHeight`, waterboarding damage now stops at this threshold and throttle is forced to 0 while the engine is waterlogged. |
| `MaxFuel` | int[0..100000000] | 0 | Capacity; zero effectively disables fuel limit. |
| `FuelConsumption` | float[0..10000] | 1 | Consumption multiplier. |
| `FuelSupplyRange` / `AmmoSupplyRange` | float[0..1000] | 0 | Fuel and ammunition support radii, respectively. |
| `GasPump` | boolean | false | Makes fuel support finite and cargo-backed. Requires `InventorySize > 0`; accepts coal (100 units), charcoal (75), coal blocks (900), and remaining charge in `MCH_ItemFuel` cans. |
| `AmmoLoader` | boolean | false | Makes ammunition support finite and consumes each weapon's configured `RoundItem` package from cargo. Requires `InventorySize > 0`. |
| `ForceY` | boolean | false | Restricts finite gas-pump and ammo-loader service to vehicles on the same block Y level. |
| `RepairOtherVehicles` | `range[,value]` | `0,10` | Support repair radius and repair value. |
| `inventorysize` | int[0..54] | 0 | Inventory slots. |
| `regeneration` | boolean | false | Enables regeneration flag used by vehicle logic. |

`FuelSupplyRange` controls gas-pump range and `AmmoSupplyRange` controls ammo-loader range. A vehicle may enable both services. Legacy vehicles that set only either range remain infinite suppliers for compatibility. Example finite support vehicle:

```text
InventorySize = 27
FuelSupplyRange = 4
GasPump = true
AmmoSupplyRange = 4
AmmoLoader = true
ForceY = true
```

## Sensors, countermeasures, UAV flags

Active radar is enabled by default only for equipped vehicles. `HasRadar false` disables it even
when older radar keys exist; otherwise an explicit `RadarType` or `EnableEntityRadar true` preserves
existing pack behavior. With no active-radar declaration, a vehicle never emits an RWR signature.
`RWRType` is a passive receiver and does not imply radar equipment. Passive RWR continues operating
while active radar is off; switching radar off clears contacts and removes the vehicle's emission.

| Key | Type | Default | Notes |
|---|---|---:|---|
| `HasRadar` | boolean | inferred | Explicit capability override; takes precedence over all inferred values. |
| `RadarType` | enum | `EARLY_AA` | Explicit presence implies radar equipment; invalid values fall back to `MODERN_AA`. The default alone does not imply equipment. |
| `RWRType` | enum | `NONE` | Invalid values fall back to `NONE`; set `DIGITAL` to enable the current RWR display. |

| `LWR` | boolean | false | Controls the tank laser warning alert sound. Tanks play the warning only when this is `true`; it is separate from `RWRType`, flares, chaff, APS, and smoke launchers. |
| `NameOnModernAARadar`, `NameOnEarlyAARadar`, `NameOnModernASRadar`, `NameOnEarlyASRadar` | string | `?` | Radar labels. |
| `Stealth` | float[0..1] | 0 | Visibility modifier. |
| `UAV`, `SmallUAV`, `NewUAV`, `NewSmallUAV`, `TargetDrone` | boolean | false | UAV/NewUAV force camera view and add a hidden seat when needed. UAV vehicle items skip normal hold-to-deploy placement: large UAVs tell players to use a UAV Station, while small UAVs tell players to use a UAV Station or Portable UAV Controller. `TargetDrone` also sets `UAV`. |
| `enablegunnermode`, `concurrentgunnermode`, `enablenightvision`, `enableentityradar`, `EnableEjectionSeat`, `EnableParachuting` | boolean | false | Shared feature toggles. Parachuting is disabled if repelling hooks exist. |
| `HasBombSight` / `EnableBombSight` / `EnableBomberSight` | boolean | true | Enables the first-person pilot gunner bombsight toggle when ballistic prediction is available. Set false for gunships or other aircraft that should keep gunner mode without a bombsight. |
| `FlareType` | int list[1..10] | none | Enables flares when at least one type is present. |
| `FlareOption` | vec3 | default flare position | Flare spawn position. |
| `HasChaff`, `HasMaintenance`, `HasAPS` | flag | absent object | Value text is ignored; presence creates the subsystem object. `HasAPS` enables the server-owned hard-kill APS. |
| `APSUseTime`, `APSWaitTime`, `APSRange`, `APSAmmo` | int | reload 0..12000, arming 0..12000, range 1..128, ammo -1..1024 | APS reload ticks after an interception, arming delay, detection range, and charge capacity. `APSAmmo = -1` (the compatibility default) is unlimited, zero is empty, and positive values are finite. |

APS only considers native MCHeli projectile entities. Weapon definitions may set `APSInterceptable = true` or `false`; when omitted, guided AA/AS/AT/TV missiles and rockets are interceptable, while bombs, guns/cannon shells, marker rockets, dispensers, bomblets, and torpedoes are not. Flares and chaff are never projectile candidates.

## Visual and attachment keys

Visual keys do not usually affect physics unless explicitly noted.

- Weapons: `AddWeapon`, `AddTurretWeapon`, `AddPartWeapon`, `AddPartRotWeapon`, `AddPartTurretWeapon`, `AddPartTurretRotWeapon`, `AddPartWeaponChild`, `AddPartWeaponMissile`, `AddPartWeaponBay`, `AddPartSlideWeaponBay`.
- Racks/hooks: `AddRack`, `RideRack`, `CanMountShip`, `MobDropOption`, `AddRepellingHook`.
- Ship rack eligibility: `CanMountShip = false` prevents this vehicle from mounting racks on ship entities, which is useful for oversized aircraft such as strategic bombers that should not land on carriers. The default is `true` for legacy content-pack compatibility. The bundled plane config references now set this key explicitly: large bombers, large transports, AWACS/tanker aircraft, and other oversized non-carrier aircraft use `false`, while carrier-suitable planes use `true`.
- External payload limits: `MaximumExternalPayloadCapacity` sets how much total vehicle weight this vehicle can chain-tow or carry across all occupied racks in pounds, and `Weight` sets this vehicle's own chain/rack payload weight in pounds. Rack mounting subtracts the weight of every already-mounted rack vehicle plus the incoming vehicle and rejects mounts that would make the remaining payload negative. Defaults are `0` lb capacity and `50000` lb weight, so vehicles cannot chain-lift or rack-carry other configured vehicles unless a payload capacity is explicitly configured. Rack paradrop fallback also uses `Weight`: vehicles over 34,800 lb exceed the cargo airdrop parachute limit and are released without spawning a paradrop parachute. Planes cannot use chains because chain towing is treated as cargo-hook transport.
- Part animation: `AddPartHatch`, `AddPartSlideHatch`, `AddPartCanopy`, `AddPartSlideCanopy`, `AddPartLG`, `AddPartLGRev`, `AddPartLGHatch`, `AddPartSlideRotLG`, `AddPartThrottle`, `AddPartRotation`, `AddPartLightHatch`.
- Ground visuals: `SetWheelPos`, `AddCrawlerTrack`, `AddTrackRoller`, `TrackRollerRot`, `AddPartWheel`, `PartWheelRot`, `AddPartSteeringWheel`.
- Effects/sound/render: `AddParticleSplash`, `particlesscale`, `EnableSeaSurfaceParticle`, `Sound`, `SoundRange`, `SoundVolume`, `SoundPitch`, `hideentity`, `SmoothShading`, `AddSearchLight`, `AddFixedSearchLight`, `AddSteeringSearchLight`, `RotorSpeed`.

## Minimal shared skeleton

```ini
displayname = Example Vehicle
Category = EXAMPLE
addtexture = example_vehicle
AddSeat = 0.0, 1.0, 0.0
HUD = none
maxhp = 50
speed = 0.1
```
