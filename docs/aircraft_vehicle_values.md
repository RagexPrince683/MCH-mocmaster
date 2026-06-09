# Aircraft and Vehicle Definition Values

This file documents the config keys accepted by MCHeli aircraft-style definitions. All keys remain optional unless an older vehicle type already required them for geometry, weapons, or seats. Missing values use the safe defaults in code, so existing packs continue to load.

## Compatibility-first realistic flight

Realistic fixed-wing behavior is **opt-in**:

```txt
EnableRealisticFlightModel = true
```

If the key is absent or `false`, legacy packs keep the old immediate control response, no aerodynamic stall, no energy drag, no soft ceiling penalty, no high-G authority loss, and no overspeed damage. Pack makers can then add the values below one at a time and test each change.

For debug logging, enable both the global option and the per-aircraft option:

```txt
# mcheli.cfg
DebugFlightModel = true

# aircraft definition
EnableFlightModelDebug = true
```

The log prints speed, horizontal speed, angle of attack (AoA), stall state, stall severity, G-force, drag, control authority, and whether the realistic model is active.

## Common aircraft values

These values are accepted by every `MCH_AircraftInfo` subtype: helicopters, planes, ships, tanks, and turrets/vehicles. Flight-model-specific values only change behavior when `EnableRealisticFlightModel=true`.

| Key | Default | Meaning and tuning guidance |
| --- | ---: | --- |
| `EnableRealisticFlightModel` | `false` | Master compatibility switch for new realistic flight behavior. Leave absent for legacy behavior. Set to `true` only on aircraft that have been tested with the new model. |
| `EnableFlightModelDebug` | `false` | Per-aircraft telemetry switch. Requires global `DebugFlightModel=true`. Useful while tuning packs; leave off in releases unless diagnostics are wanted. |
| `PitchTorque` | `0.35` | Realistic-mode pitch acceleration strength. Higher values make pitch respond faster. Safe to leave unset. |
| `RollTorque` | `0.35` | Realistic-mode roll acceleration strength. Higher values make roll respond faster. |
| `YawTorque` | `0.35` | Realistic-mode yaw acceleration strength. Higher values make yaw respond faster. |
| `PitchDamping` | `0.35` | Realistic-mode pitch angular damping. Higher values reduce wobble and limit pitch rate. |
| `RollDamping` | `0.35` | Realistic-mode roll angular damping. Higher values reduce roll overshoot. |
| `YawDamping` | `0.35` | Realistic-mode yaw angular damping. Higher values reduce yaw overshoot. |
| `InertiaMultiplier` | `1.0` | Scales resistance to angular acceleration. Larger/heavier aircraft should use higher values. |
| `ThrottleAcceleration` | `0.02` | Maximum engine output increase per tick in systems that use smoothed engine output. Higher values spool faster. |
| `EngineDrag` | `0.015` | Maximum engine output decrease per tick. Higher values spool down faster. |
| `FlightCeiling` | `250.0` | Realistic-mode soft altitude ceiling. Lift begins fading near this Y level. Use `0` to effectively disable. |
| `FlightCeilingRange` | `24.0` | Vertical fade band below `FlightCeiling`. Larger values make the ceiling gentler. |
| `StallSpeed` | `0.0` | Absolute airspeed stall threshold. `0` derives from `Speed * StallSpeedFactor`, preserving older tuning styles. |
| `CriticalAoA` | `18.0` | Angle between nose direction and velocity where airflow starts separating. Higher values make aggressive pitch safer. |
| `StallLiftLoss` | `0.65` | Fraction of lift removed at a full stall. Lower values are forgiving; higher values are harsher. |
| `AoADragMultiplier` | `1.5` | Scales drag from high AoA. Increase to punish high-alpha flight. |
| `StallInstability` | `0.35` | Stall buffet and wing-drop strength. `0` disables the shake/drop while leaving lift loss. |
| `StallRecoverySpeed` | `0.0` | Speed needed to recover from a stall. `0` uses `StallSpeed * 1.2`. |
| `StallSpeedFactor` | `0.22` | Legacy stall-speed fraction of top speed when `StallSpeed=0`. Lower is more forgiving. |
| `StallStrength` | `0.6` | Scales stall sink response for packs that already tuned older stall behavior. |
| `DiveSpeedMultiplier` | `1.25` | Realistic-mode dive speed cap multiplier. `1.0` means no extra dive speed. |
| `MaxComfortableG` | `4.0` | Load factor where high-G control loss begins. Higher values fit fighters. |
| `MaxStructuralG` | `8.0` | Load factor treated as the structural limit. Above this, overload hooks can apply consequences. |
| `GControlPenalty` | `0.7` | Maximum control authority removed between comfortable and structural G. `0` disables high-G control loss. |
| `CompressibilitySpeed` | `0.0` | Speed where pitch compressibility starts. `0` derives from `Speed * 0.9`. |
| `CompressibilityPitchPenalty` | `0.65` | Maximum pitch authority loss from compressibility. Lower is more forgiving. |
| `MaxSafeSpeed` | `0.0` | Overspeed warning/damage threshold. `0` derives from `Speed * 1.1`. |
| `OverspeedDamageRate` | `0.2` | Damage points per tick at full overspeed severity. Use `0` to disable damage. |

Existing common vehicle values such as seats, weapons, HUD names, camera positions, recipes, racks, bounding boxes, health, armor, fuel, mobility, throttle, rotor speed, particles, sounds, UAV settings, radar/RWR names, landing parts, hatches, wheels, canopies, tracks, and search lights continue to be parsed by `MCH_AircraftInfo`. Their existing defaults and parser behavior are unchanged by the realistic-flight compatibility pass.

## Plane-specific values

Plane definitions (`planes`) can opt into these fixed-wing energy values after enabling realistic flight:

| Key | Default | Meaning and tuning guidance |
| --- | ---: | --- |
| `BaseDrag` | `0.0015` | Baseline airborne drag per tick. Raise for blunt/draggy aircraft. |
| `InducedDrag` | `0.006` | Extra drag in banked or high-load turns. Raise to make hard turns bleed speed. |
| `ControlSurfaceDrag` | `0.0025` | Extra drag from active pitch/roll/yaw control movement. |
| `ClimbEnergyLoss` | `0.006` | Horizontal speed traded away while climbing. |
| `DiveEnergyGain` | `0.008` | Horizontal speed recovered while descending. |
| `MaxLevelSpeed` | `0.0` | Full-power sustainable level speed. `0` uses the normal `Speed`. |
| `IdleDrag` | `0.004` | Extra drag at closed throttle. Raise to make aircraft slow quickly at idle. |
| `EnableVtol` | `false` | Allows VTOL/nozzle behavior when the model has nozzles. Existing behavior unchanged. |
| `DefaultVtol` | `false` | Starts in VTOL mode where supported. |
| `VtolYaw` | `0.3` | Yaw control while in VTOL mode. |
| `VtolPitch` | `0.2` | Pitch control while in VTOL mode. |
| `EnableAutoPilot` | `false` | Enables plane autopilot behavior. |
| `VariableSweepWing` | `false` | Enables sweep-wing animation support. |
| `SweepWingSpeed` | `Speed` | Speed used by variable sweep-wing behavior. |
| `AddPartRotor`, `AddBlade`, `AddPartWing`, `AddPartPylon`, `AddPartNozzle` | none | Model/animation geometry declarations. These remain positional data and are not required by the flight model. |

## Helicopter-specific values

| Key | Default | Meaning |
| --- | ---: | --- |
| `EnableFoldBlade` | `false` | Allows foldable rotor blades where rotor data supports it. |
| `AddRotor` / `AddRotorOld` | none | Adds rotor geometry. The optional ninth value marks whether the rotor supports folding. |

## Ship-specific values

Ships share many plane-style part keys (`AddPartRotor`, `AddBlade`, `AddPartWing`, `AddPartPylon`, `AddPartNozzle`, VTOL and sweep-wing keys) because they reuse aircraft drawing parts. Existing ship packs do not need realistic flight keys unless a ship-derived aircraft intentionally wants them.

Additional ship keys include:

| Key | Default | Meaning |
| --- | ---: | --- |
| `IsCarrier` | `false` | Marks a ship as carrier-capable in ship-specific logic. |
| `PreventWaterBobbing` | `false` | Suppresses water bobbing when supported by the entity implementation. |

## Tank-specific values

| Key | Default | Meaning |
| --- | ---: | --- |
| `WeightType` | `0` | `tank`, `car`, or default handling for weight/suspension behavior. |
| `WeightedCenterZ` | `0.0` | Forward/back center-of-mass offset. |
| `TrackRollerRot` | `30.0` | Track roller visual rotation scale inherited from common aircraft data. |
| `PartWheelRot` | `30.0` | Wheel visual rotation scale inherited from common aircraft data. |
| `AddTrackHitBox` | none | Adds a damageable track bounding box: x, y, z, width, height, optional damage factor. |

## Turret/vehicle-specific values

| Key | Default | Meaning |
| --- | ---: | --- |
| `CanMove` | `false` | Enables vehicle movement. |
| `CanRotation` | `false` | Enables vehicle body/turret rotation. |
| `RotationPitchMin` | inherited | Legacy alias for `MinRotationPitch`. |
| `RotationPitchMax` | inherited | Legacy alias for `MaxRotationPitch`. |
| `AddPart` | none | Adds a rendered part: first-person draw flag, yaw flag, pitch flag, type, x, y, z, optional recoil buffer. |
| `AddChildPart` | none | Adds a child to the most recent vehicle part using the same value layout. |

## Recommended gradual opt-in workflow

1. Add `EnableRealisticFlightModel=true` to one test aircraft only.
2. Add `EnableFlightModelDebug=true` and set global `DebugFlightModel=true` while testing.
3. Tune `MaxLevelSpeed`, `BaseDrag`, `InducedDrag`, and `IdleDrag` until cruise and turns feel right.
4. Tune `StallSpeed` or `StallSpeedFactor`, then `CriticalAoA`, `StallLiftLoss`, and `StallInstability`.
5. Tune `MaxComfortableG`, `MaxStructuralG`, and `CompressibilitySpeed` last; these should not be used to fix basic speed or stall issues.
6. Remove debug options from release definitions unless pack makers want support logs.

## Common legacy key reference

The following older keys are intentionally still accepted and do not require any of the realistic-flight keys above:

### Identity, item, recipe, textures, and presentation

- `DisplayName`, `AddDisplayName`: set the default visible name and localized names.
- `Category`: assigns the creative/category grouping shown to players.
- `ItemID`: legacy numeric item identifier support.
- `AddTexture`: registers additional texture names for texture cycling.
- `HideEntity`, `SmoothShading`: controls entity visibility and model smoothing.
- `AddRecipe`, `AddShapelessRecipe`: adds crafting recipes.
- `HUD`: assigns HUD layouts per seat; missing HUDs use type defaults.
- `CameraZoom`, `DefaultFreelook`, `ThirdPersonDist`, `CameraPosition`, `UnmountPosition`, `CameraRotationSpeed`: camera and view behavior.

### Survivability, collision, supplies, and inventory

- `MaxHP`, `DamageFactor`, `SubmergedDamageHeight`: health and environmental damage tuning.
- `ArmorDamageFactor`, `ArmorMinDamage`, `ArmorMaxDamage`: armor damage scaling and clamp range.
- `InventorySize`: vehicle inventory capacity.
- `MaxFuel`, `FuelConsumption`, `FuelSupplyRange`, `AmmoSupplyRange`, `RepairOtherVehicles`: fuel usage and support radius/value tuning.
- `Regeneration`: enables built-in regeneration where supported.
- `ExplosionSizeByCrash`: crash explosion strength.
- `BoundingBox`, `Width`, `Height`, `EntityWidth`, `EntityHeight`, `EntityPitch`, `EntityRoll`, `StepHeight`: physical size, rendered attachment pose, and stepping behavior.
- `Float`, `FloatOffset`, `Gravity`, `GravityInWater`: buoyancy and gravity values.

### Sensors, countermeasures, and special equipment

- `RadarType`, `RWRType`, `NameOnModernAARadar`, `NameOnEarlyAARadar`, `NameOnModernASRadar`, `NameOnEarlyASRadar`: radar/RWR classification and display names.
- `EnableNightVision`, `EnableEntityRadar`, `Stealth`: sensor equipment and detectability.
- `FlareType`, `FlareOption`, `HasChaff`, `ChaffUseTime`, `ChaffWaitTime`: flare/chaff defensive systems.
- `HasMaintenance`, `MaintenanceUseTime`, `MaintenanceWaitTime`, `EngineShutdownThreshold`: maintenance/repair system behavior.
- `HasAPS`, `APSUseTime`, `APSWaitTime`, `APSRange`: active-protection-system availability and timing.
- `EnableEjectionSeat`, `EnableParachuting`: crew escape features.
- `UAV`, `SmallUAV`, `NewUAV`, `NewSmallUAV`, `TargetDrone`: UAV/drone classification.
- `MobDropOption`: controls mob drop behavior.

### Movement, controls, and ground handling

- `Speed`, `MotionFactor`, `EnableBack`, `ThrottleDownFactor`, `ThrottleUpDown`, `ThrottleUpDownOnEntity`: base speed, damping, reverse, and throttle response.
- `MobilityYaw`, `MobilityPitch`, `MobilityRoll`, `MobilityYawOnGround`: legacy control-rate scalars.
- `MinRotationPitch`, `MaxRotationPitch`, `MinRotationRoll`, `MaxRotationRoll`: optional rotation clamps.
- `CanMoveOnGround`, `CanRotOnGround`, `OnGroundPitch`, `OnGroundPitchFactor`, `OnGroundRollFactor`: runway/ground behavior.
- `AutoPilotRot`: autopilot rotation strength.
- `PivotTurnThrottle`: throttle threshold for pivot-turn behavior.
- `RotorSpeed`: rotor visual speed.
- `Mass`: legacy alias that feeds `InertiaMultiplier` for realistic angular acceleration.

### Seats, racks, wheels, hooks, lights, and particles

- `CanRide`, `AddSeat`, `AddGunnerSeat`, `AddFixRotSeat`, `ExclusionSeat`: passenger/gunner seat layout and exclusions.
- `AddRack`, `RideRack`, `AddRepellingHook`: rack mounting and rope/repelling hook data.
- `SetWheelPos`, `AddPartWheel`, `AddPartSteeringWheel`, `AddTrackRoller`, `AddCrawlerTrack`, `TrackRollerRot`, `PartWheelRot`: wheels, tracks, rollers, and their visual rotation.
- `AddParticleSplash`, `ParticlesScale`, `EnableSeaSurfaceParticle`: water/particle effects.
- `AddSearchLight`, `AddFixedSearchLight`, `AddSteeringSearchLight`, `AddPartLightHatch`: searchlight and light-hatch parts.
- `TurretPosition`: shared turret origin for compatible vehicles.

### Weapons and animated model parts

- `AddWeapon`, `AddTurretWeapon`: weapon definitions.
- `AddPartWeapon`, `AddPartRotWeapon`, `AddPartTurretWeapon`, `AddPartTurretRotWeapon`, `AddPartWeaponMissile`, `AddPartWeaponChild`: visible weapon part hierarchies.
- `AddPartWeaponBay`, `AddPartSlideWeaponBay`: weapon bay parts and sliding bays.
- `AddPartHatch`, `AddPartSlideHatch`, `AddPartCanopy`, `AddPartSlideCanopy`: hatches/canopies.
- `AddPartLG`, `AddPartSlideRotLG`, `AddPartLGRev`, `AddPartLGHatch`: landing gear and gear hatches.
- `AddPartThrottle`, `AddPartRotation`, `AddPartCamera`: throttle, generic rotating, and camera model parts.

### Sound

- `Sound`, `SoundRange`, `SoundVolume`, `SoundPitch`: movement sound name and playback tuning.

These common keys are parsed before subtype-specific keys. Subtypes may add aliases such as turret `RotationPitchMin`/`RotationPitchMax`, but the compatibility rule is the same: absent values keep defaults, and unknown new flight values are not required for old definitions.
