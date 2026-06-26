# Fixed-wing new flight model configuration

This document is the audit and user-facing reference for fixed-wing aircraft that opt into the new flight model through `UseNewMobilitySystem`, `UseNewFlightModel`, `EnableNewFlightModel`, or the older `EnableRealisticFlightModel` alias. The implemented model adds data-driven mass/thrust simulation, configurable gravity, lift and energy management, AoA/stall behavior, control-authority penalties, combat flaps, flight ceilings, dive assist, and overspeed handling while preserving legacy behavior for aircraft that do not opt in.

The parser also accepts several real-world-unit aliases for new packs. Those aliases convert into the same internal gameplay values used by the current simulation; they do not introduce unfinished aerodynamic tables such as wing area, wingspan, aspect ratio, Oswald efficiency, or lift/drag coefficient lookup tables.

## Unit conventions

- Minecraft distance is treated as meters for config conversion: `1 block = 1 m`.
- The fixed simulation tick is `20 ticks/s`.
- Legacy speed fields are internal blocks/tick. Real-world speed aliases use `km/h`; conversion is `internal = km/h / 72`.
- Legacy force/mass fields are internal gameplay values. Real-world aliases convert so `EngineThrustN / MassKg` produces an acceleration in `m/s²`, then into per-tick velocity change.
- Angles are degrees. G limits are multiples of standard gravity.

## Complete audit of existing new-flight-model values

| Old config name | Purpose | Current internal usage | Proposed real-world unit / standard key |
| --- | --- | --- | --- |
| `BaseDrag` | Baseline airborne energy loss | Fractional horizontal speed loss in `getEnergyDrag` | Unitless gameplay tuning; documented as drag-energy fraction/tick |
| `InducedDrag` | Extra turn/high-load energy loss | Added from bank/control load in `getEnergyDrag` | Unitless gameplay tuning; documented |
| `ControlSurfaceDrag` | Drag while controls move | Added from body-rate control load | Unitless gameplay tuning; documented |
| `ClimbEnergyLoss` | Speed loss while climbing | Used by vertical-energy helper | Unitless gameplay tuning; documented |
| `DiveEnergyGain` | Speed recovered in dives | Used by vertical-energy helper | Unitless gameplay tuning; documented |
| `MaxLevelSpeed` | Sustainable full-power level speed | Internal speed cap, scaled by global plane speed | `MaximumLevelSpeed` in km/h; legacy remains supported |
| `IdleDrag` | Extra closed-throttle drag | Added when throttle is low | Unitless gameplay tuning; documented |
| `PitchTorque` | Pitch control authority | Angular acceleration input to `updateAngularVelocity` | Prefer `MaximumPitchRate` conceptually, but current value is internal torque; documented |
| `RollTorque` | Roll control authority | Angular acceleration input | Internal torque tuning; documented |
| `YawTorque` | Yaw control authority | Angular acceleration input | Internal torque tuning; documented |
| `PitchDamping` | Pitch angular damping | Angular drag in `updateAngularVelocity` | Internal damping/tick; documented |
| `RollDamping` | Roll angular damping | Angular drag | Internal damping/tick; documented |
| `YawDamping` | Yaw angular damping | Angular drag | Internal damping/tick; documented |
| `Mass` | Legacy alias that previously meant rotational inertia | Compatibility heuristic: values <=100 keep the old angular-inertia meaning; larger values are treated as kg | `MassKg` in kilograms; `Mass` supports kg only for values >100 to preserve old packs |
| `InertiaMultiplier` | Resistance to angular acceleration | Divides torque response only | Unitless gameplay tuning; documented |
| `PhysicalMass` | Translational mass | Divides thrust, lift, drag, weight forces | `MassKg`/`PhysicalMassKg` in kg; legacy remains supported |
| `EngineThrust` | Engine force | Multiplied by throttle before mass division | `EngineThrustN` in newtons; legacy remains supported |
| `TakeoffDistanceMultiplier` | Ground-roll rotation helper | Scales takeoff-assist threshold | Unitless gameplay tuning; documented |
| `ThrottleAcceleration` | Engine spool-up rate | Max engine output increase/tick | Unitless fraction/tick; documented |
| `EngineDrag` | Engine spool-down rate | Max engine output decrease/tick | Unitless fraction/tick; documented |
| `NewFlightThrottleResponse` | Nonlinear throttle curve | Raises commanded throttle by exponent | Unitless curve exponent; documented |
| `NewFlightThrottleChangeRateUp` | Pilot throttle increase rate | Fraction/tick | Percent/s equivalent is value × 2000; legacy unitless remains |
| `NewFlightThrottleChangeRateDown` | Pilot throttle decrease rate | Fraction/tick | Percent/s equivalent is value × 2000; legacy unitless remains |
| `NewFlightIdleThrottle` | Minimum effective engine power | Fraction at zero commanded throttle | Throttle percent can be read as value × 100 |
| `NewFlightEngineBrakeDrag` | Closed-throttle braking | Added to energy drag | Unitless gameplay tuning; documented |
| `NewFlightLowThrottleLiftRetention` | Lift retained at idle | Fraction blended into lift power | Percent/fraction; documented |
| `NewFlightThrottleControlAuthorityScale` | Control loss at idle | Fraction of authority removed | Percent/fraction; documented |
| `NewFlightThrottleHudDisplay` | HUD behavior | Shows normalized throttle | boolean |
| `NewFlightCombatFlaps` | Enables combat flap toggle | Gates flap lift/drag/control effects | boolean |
| `NewFlightCombatFlapLift` | Added low-speed lift when flaps deployed | Adds to lift power and takeoff helper | Unitless gameplay tuning; documented |
| `NewFlightCombatFlapDrag` | Added flap drag | Adds to engine-brake drag | Unitless gameplay tuning; documented |
| `NewFlightCombatFlapControl` | Added control authority with flaps | Multiplies control authority | Fraction |
| `NewFlightCombatFlapOverspeed` | Flap safe-speed fraction | Multiplies overspeed threshold when flaps deployed | Fraction of VNE |
| `NewFlightWingIncidenceAoA` / `NewFlightLiftBiasAoA` | Wing incidence/camber lift bias | Added to AoA for lift coefficient only | degrees |
| `StallSpeed` | Stall entry speed | Internal speed threshold | `StallSpeedKmh` in km/h; legacy remains supported |
| `CriticalAoA` | Stall angle threshold | AoA stall severity and AoA drag | degrees |
| `TimeUntilStallPastCriticalAoA` | Delay before high-AoA flight develops major energy/stall loss | Seconds above critical AoA gate deep-stall/AoA energy bleed | seconds |
| `TimeAfterStallUntilPitchDown` | Delay before low-energy stall forces nose-down recovery | Seconds after stalled low-forward-energy state before pitch-break recovery | seconds |
| `StallLiftLoss` | Lift loss in full stall | Multiplies stall severity into lift reduction | Fraction |
| `AoADragMultiplier` | Drag from angle of attack | Multiplies base drag by normalized AoA² | Unitless gameplay tuning; documented |
| `StallInstability` | Stall buffet/wing drop | Adds deterministic stall motion | Unitless gameplay tuning; documented |
| `StallRecoverySpeed` | Speed required to clear stall | Recovery gate with AoA reduction | `StallRecoverySpeedKmh` in km/h; legacy remains supported |
| `StallSpeedFactor` | Legacy derived stall speed | `Speed * factor` if no stall speed set | Legacy unitless compatibility only |
| `StallStrength` | Stall response strength | Scales stall effects | Unitless gameplay tuning; documented |
| `StallPitchRecoveryStrength` | Nose-down stall recovery | Adds pitch-down angular velocity | Internal angular impulse/tick; documented |
| `StallBreakStrength` | Deep-stall pitch break | Adds nonlinear pitch-break impulse | Internal angular impulse/tick; documented |
| `StallRecoveryRate` | Stall fade-out blend rate | Decays stall severity after unloading | Fraction/tick |
| `DiveSpeedMultiplier` | Dive speed headroom | Multiplies top speed while diving | Unitless speed multiplier |
| `MaxComfortableG` | High-G control fade starts | Start of G authority penalty | `PositiveGLimit` concept, in G; legacy key remains |
| `MaxStructuralG` | Structural/control limit | End of G authority penalty | `StructuralFailureG` concept, in G; legacy key remains |
| `GControlPenalty` | Authority removed at high G | Fractional control penalty | Fraction |
| `CompressibilitySpeed` | Pitch compressibility threshold | Pitch authority fades above it | `CompressibilitySpeedKmh` in km/h; legacy remains supported |
| `CompressibilityPitchPenalty` | Pitch authority lost near VNE | Fractional pitch penalty | Fraction |
| `MaxSafeSpeed` | Overspeed/damage threshold | Warning/damage threshold | `NeverExceedSpeed`/`MaxSafeSpeedKmh` in km/h; legacy remains supported |
| `OverspeedDamageRate` | Overspeed damage | Damage/tick at 100% overspeed | Damage points/tick |

## Documented standard keys for real-world values

Use these keys for new packs where practical:

```ini
EnableRealisticFlightModel = true
MassKg = 11000
EngineThrustN = 80000
MaximumLevelSpeed = 706
StallSpeedKmh = 220
StallRecoverySpeedKmh = 265
CompressibilitySpeedKmh = 635
NeverExceedSpeed = 780
CriticalAoA = 14
TimeUntilStallPastCriticalAoA = 1.2
TimeAfterStallUntilPitchDown = 1.0
MaxComfortableG = 7.5
MaxStructuralG = 8.5
```

Legacy aliases (`PhysicalMass`, `EngineThrust`, `MaxLevelSpeed`, `StallSpeed`, `StallRecoverySpeed`, `CompressibilitySpeed`, `MaxSafeSpeed`) still load to preserve existing packs.

## Validation guidance

The parser warns for internally contradictory values such as structural G below comfortable G, VNE at/below compressibility speed, or stall recovery speed below stall speed. Typical realistic ranges:

- `MassKg`: 300-250000 kg depending on aircraft class.
- `EngineThrustN`: prop aircraft can use equivalent static thrust; jets range from tens to hundreds of kN.
- `StallSpeedKmh`: 55-350 km/h.
- `MaximumLevelSpeed`: 120-4000 km/h.
- `CriticalAoA`: 10-20 degrees for most aircraft.
- `TimeUntilStallPastCriticalAoA`: 0.8-1.8 seconds for most fixed-wing aircraft; aerobatic/fighters are usually shorter, heavy aircraft longer.
- `TimeAfterStallUntilPitchDown`: 0.7-1.5 seconds, long enough to feel a stall develop but short enough to prevent indefinite vertical climbs.
- `MaxComfortableG`/`MaxStructuralG`: 2.5-12 G depending on class.

## Remaining unitless gameplay tuning

The current model still contains tuning values that are not direct real-world aerodynamic properties. Do not replace them with invented `WingArea`, `DragCoefficient`, `LiftCoefficient`, `AspectRatio`, or control-surface deflection fields unless a later physics redesign adds those systems. The documented unitless values are retained because they control the existing energy, stall, damping, and input-response approximations directly.

## Stall and idle-throttle behavior

`TimeUntilStallPastCriticalAoA` prevents instant balloon-like momentum loss when the nose briefly exceeds critical AoA. The AoA timer begins at or above `CriticalAoA`; before it expires, AoA drag and stall demand ramp in instead of instantly removing usable energy. `TimeAfterStallUntilPitchDown` begins only after the aircraft is already stalled and has lost most usable forward airspeed. When that second timer expires, the recovery system forces a nose-down pitch break so the aircraft falls, unloads the wing, and can regain airspeed.

At zero commanded throttle, airborne aircraft keep gliding and bleeding speed through drag/energy loss instead of having horizontal motion zeroed. The old throttle-zero stop remains active only while the aircraft is actually on the ground so friction can hold parked aircraft without suppressing airborne aerodynamics near the surface.
