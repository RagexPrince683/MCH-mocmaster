# Plane config values and fixed-wing flight model

Planes inherit every shared key from `base.md` and add visual VTOL/sweep-wing keys plus the overhauled fixed-wing flight-model values.

## Plane-only keys

| Key | Type/range | Default | Status and notes |
|---|---:|---:|---|
| `addpartrotor` | `x,y,z,rx,ry,rz[,maxRot]` | none | Visual rotor; optional. |
| `addblade` | `numBlade,rotBlade,x,y,z,rx,ry,rz` | none | Adds blades to the last rotor. |
| `addpartwing` | `x,y,z,rx,ry,rz,maxRot` | none | Visual sweep wing. |
| `AddPartPylon` | `x,y,z,rx,ry,rz,maxRot` | none | Pylon attached to the last wing. |
| `addpartnozzle` | `x,y,z,rx,ry,rz` | none | VTOL/nozzle visual and particle point. |
| `variablesweepwing` | boolean | false | Enables sweep-wing behavior. |
| `sweepwingspeed` | float[0..5] | initial shared `speed` | Speed while swept; scaled by global plane speed config. |
| `enablevtol` | boolean | false | Enables VTOL mode. |
| `defaultvtol` | boolean | false | On ground, switches into VTOL by default. |
| `vtolyaw` | float[0..1] | 0.3 | VTOL yaw and roll factor. |
| `vtolpitch` | float[0.01..1] | 0.2 | VTOL pitch factor. |
| `enableautopilot` | boolean | false | Legacy autopilot flag. |


## Config scale rules

* Normalized fractions, authority values, penalties, retention values, and severities use `0.0` to `1.0` unless a row explicitly lists a narrower asset-grounded range.
* Percent values exposed as percentages should use `0` to `100`; the fixed-wing flight-model keys in this table are stored as fractions rather than percent text.
* Angles use degrees. G-limit values use G. Altitude keys such as `FlightCeiling` and `FlightCeilingRange` are Minecraft blocks/meters.
* Speed keys in this legacy parser remain MCHeli internal speed units and are scaled by `AllPlaneSpeed`; avoid adding new speed keys unless they can be converted explicitly at parse time.
* Physical/derived internals such as climb sustain, energy deficit, and unsupported vertical-climb pressure should stay calculated from mass, thrust, drag, lift, speed, gravity, AoA, and altitude instead of becoming config keys.

## Fixed-wing flight-model keys

All values are optional. Speed-like values (`Speed`, `MaxLevelSpeed`, `StallSpeed`, `StallRecoverySpeed`, `CompressibilitySpeed`, `MaxSafeSpeed`, `sweepwingspeed`) are multiplied by the global `AllPlaneSpeed` config during validation.

To author a plane speed from miles per hour, use:

```text
plane config speed = (mph / 1000) * 1.74
validated speed = plane config speed * AllPlaneSpeed
```

With the default `AllPlaneSpeed = 1000`, a 500 mph plane therefore uses `Speed 0.87` before global scaling. AA-missile launch speed follows the same normalized `AllPlaneSpeed / 1000 * 1.74` scale; at the default, an AA missile travels at 1.74 times its configured `Acceleration`. AA missiles also keep homing for their full lifetime rather than stopping at the generic `TickEndHoming` cutoff.

| Key | Type/range | Default | What it changes |
|---|---:|---:|---|
| `BaseDrag` | normalized drag coefficient [0..0.01] | 0.0023 | Baseline drag. Scales with speed ratio squared and also participates in AoA drag. |
| `InducedDrag` | normalized drag coefficient [0..0.04] | 0.015 | Extra drag from bank angle and body-rate load. |
| `ControlSurfaceDrag` | normalized drag coefficient [0..0.02] | 0.0048 | Extra drag while pitch/roll/yaw angular rates are high. |
| `MaxLevelSpeed` | internal speed [0..6] | 0 = use `Speed` | Sustainable full-power speed in level flight. |
| `IdleDrag` | normalized drag coefficient [0..0.02] | 0.0034 | Extra drag as engine output approaches zero. |
| `PitchTorque`, `RollTorque`, `YawTorque` | normalized authority [0..1] | 0.380 / 0.420 / 0.320 | Local-axis torque applied to requested angular rates. Higher values respond faster. |
| `PitchDamping`, `RollDamping`, `YawDamping` | normalized damping [0..1] | 0.430 / 0.390 / 0.460 | Angular damping. Higher values lower steady body rate for the same torque. |
| `InertiaMultiplier` | multiplier [0.1..5] | 1.350 | Resistance to angular acceleration. Higher values make controls feel heavier. |
| `Mass` | legacy float | alias of `InertiaMultiplier` | Legacy/compatibility alias for angular inertia only; does not set physical weight. |
| `PhysicalMass` | internal mass [0.1..10] | 1.650 | **New flight model only.** Translational mass used for thrust acceleration, drag response, lift-to-weight, weight, climb, and takeoff behavior. |
| `EngineThrust` | internal force [0..10] | derived from speed | **New flight model only.** Engine force used as `thrust / PhysicalMass`; affects acceleration, climb, and energy recovery without being a top-speed cap. |
| `ThrottleAcceleration` | float[0..1] | 0.026 | Legacy engine-output spool-up. New-flight planes still use it only to smooth commanded throttle into engine output; pilot input rate is controlled by `NewFlightThrottleChangeRateUp`. |
| `EngineDrag` | float[0..1] | 0.011 | Legacy engine-output spool-down. New-flight planes still use it only to smooth commanded throttle into engine output; pilot input rate is controlled by `NewFlightThrottleChangeRateDown`. |
| `NewFlightThrottleResponse` | multiplier [0.5..2] | 1.15 | **New flight model only.** Curves smoothed engine output before thrust. `1` is linear, `<1` gives more low-throttle thrust, `>1` softens the low end. |
| `NewFlightThrottleChangeRateUp` | throttle fraction/tick [0.001..0.02] | 0.0045 | **New flight model only.** Pilot-commanded throttle increase per tick. Full idle-to-max travel is about `1 / value` ticks. |
| `NewFlightThrottleChangeRateDown` | throttle fraction/tick [0.001..0.02] | 0.0065 | **New flight model only.** Pilot-commanded throttle decrease per tick. Usually slightly faster than increase for approach and dogfight energy control. |
| `NewFlightIdleThrottle` | normalized fraction [0..0.20] | 0.090 | **New flight model only.** Minimum effective engine power at 0% commanded throttle; keeps idle physically plausible without making idle accelerate like cruise. |
| `NewFlightEngineBrakeDrag` | normalized drag coefficient [0..0.02] | 0.0034 | **New flight model only.** Closed-throttle/low-power drag used by the energy model. Replaces `IdleDrag` for opted-in planes. |
| `NewFlightLowThrottleLiftRetention` | float[0..1] | 0.700 | **New flight model only.** Retains this fraction of the legacy throttle-coupled vertical support at idle so lift does not vanish immediately when throttle is chopped. Stall is still driven by airspeed/AoA. |
| `NewFlightThrottleControlAuthorityScale` | float[0..1] | 0.10 | **New flight model only.** Maximum control-authority penalty at idle. Keep low so glide/landing controls remain useful. |
| `NewFlightThrottleHudDisplay` | boolean | true | **New flight model only.** Shows pilot HUD text like `THR 85%`. Legacy HUDs are unchanged for planes that do not opt in. |
| `NewFlightDisableForwardAirspeedControlScaling` | boolean | true | **New flight model only.** Compatibility switch that keeps `forwardAirspeed / StallSpeed` as stall/energy telemetry instead of a direct pilot-control multiplier. Leave true unless intentionally testing active-development control scaling. |
| `NewFlightCombatFlaps` | boolean | true | **New flight model only.** Enables the combat-flap toggle on the Extra key. Inactive on legacy planes even if present. |
| `NewFlightCombatFlapLift` | normalized lift bonus [0..0.30] | 0.120 | **New flight model only.** Added low-speed lift/support and induced-load contribution while combat flaps are deployed. |
| `NewFlightCombatFlapDrag` | normalized drag coefficient [0..0.05] | 0.014 | **New flight model only.** Extra drag while combat flaps are deployed. |
| `NewFlightCombatFlapControl` | normalized authority bonus [0..0.40] | 0.14 | **New flight model only.** Control-authority boost while combat flaps are deployed. |
| `NewFlightCombatFlapOverspeed` | normalized multiplier [0.50..1] | 0.72 | **New flight model only.** Multiplier applied to `MaxSafeSpeed` while flaps are deployed; lower values punish high-speed flap use earlier. |
| `NewFlightWingIncidenceAoA` | degrees [0..8] | 3.0 | **New flight model only.** Lift-only wing incidence/camber bias so a level fuselage can generate fixed-wing lift without fake vertical thrust or hover support. |
| `NewFlightLiftBiasAoA` | degrees [0..8] | 3.0 | Alias for `NewFlightWingIncidenceAoA`. |
| `NewPlaneCameraFocusOffsetX` | local blocks | 0 | **New flight model chase-camera only.** Local-space visual focus X coordinate for the smooth chase camera. Visual only; no physics effect. |
| `NewPlaneCameraFocusOffsetY` | local blocks | 1 | **New flight model chase-camera only.** Local-space visual focus Y coordinate for the smooth chase camera. Visual only; no physics effect. |
| `NewPlaneCameraFocusOffsetZ` | local blocks | 0 | **New flight model chase-camera only.** Local-space visual focus Z coordinate for the smooth chase camera. Visual only; no physics effect. |
| `StallSpeed` | internal speed [0..2] | 0 | Absolute stall threshold; if 0, uses `max(0.05, topSpeed * StallSpeedFactor)`. |
| `TimeUntilStallPastCriticalAoA` | seconds [0..8] | 1.20 | **New flight model only.** Seconds allowed at/past critical AoA before major AoA stall energy loss ramps fully. |
| `TimeAfterStallUntilPitchDown` | seconds [0..8] | 1.00 | **New flight model only.** Seconds after a low-forward-energy stall before forced nose-down/falling recovery starts. |
| `CriticalAoA` | degrees [5..32] | 14.40 | AoA in degrees where stall demand begins. |
| `StallLiftLoss` | float[0..1] | 0.820 | Fraction of lift removed at full stall. |
| `AoADragMultiplier` | multiplier [0..5] | 2.550 | Scales quadratic AoA drag. |
| `StallInstability` | normalized severity [0..1] | 0.440 | Buffet, yaw shake, and deterministic wing-drop strength. |
| `StallRecoverySpeed` | internal speed [0..2.5] | 0 = `StallSpeed*1.2` | Speed required, with low AoA, to exit a stall. |
| `StallSpeedFactor` | multiplier [0..0.5] | 0.18 | Legacy derived stall threshold when `StallSpeed` is omitted. |
| `StallStrength` | multiplier [0..2] | 1.150 | Legacy stall response scale retained for older packs. |
| `StallPitchRecoveryStrength` | multiplier [0..2] | 0.55 | Nose-down angular-velocity moment applied during stalls. Higher values make light fighters break/recover more aggressively. |
| `StallBreakStrength` | multiplier [0..2] | 0.65 | Extra nonlinear nose-down impulse for deep stalls. Lower values suit transports or stable aircraft. |
| `StallRecoveryRate` | float[0.01..1] | 0.18 | Blend rate used as stall severity fades after the aircraft unloads and meets recovery conditions. |
| `DiveSpeedMultiplier` | multiplier [1..1.6] | 1.24 | Maximum speed cap multiplier in a dive. |
| `MaxComfortableG` | G [1..12] | 5.8 | G where high-G control fade starts. |
| `MaxStructuralG` | G [1..16] | 8.5 | G where high-G fade reaches the configured penalty and structural hook begins. Warns if below `MaxComfortableG`. |
| `GControlPenalty` | normalized fraction [0..1] | 0.720 | Max fraction of authority removed by high G. |
| `CompressibilitySpeed` | internal speed [0..6] | 0 = 90% of level speed | Speed where pitch authority starts fading. |
| `CompressibilityPitchPenalty` | float[0..1] | 0.5 | Max pitch-authority loss by `MaxSafeSpeed`. |
| `MaxSafeSpeed` | internal speed [0..7] | 0 = 110% of level speed | Overspeed threshold. Warns if not above `CompressibilitySpeed`. |
| `OverspeedDamageRate` | damage/tick [0..1] | 0.14 | Damage per tick at 100% overspeed. Set 0 to disable damage. |

## Derived behavior

The new fixed-wing model intentionally keeps climb sustain, unsupported vertical climb, energy deficit, stall recovery pressure, and takeoff rotation as internal calculations instead of pack-maker knobs. These behaviors are derived from `PhysicalMass`, `EngineThrust`, drag (`BaseDrag`, `InducedDrag`, `ControlSurfaceDrag`, AoA drag), lift/stall values (`StallSpeed`, `CriticalAoA`, `StallLiftLoss`), gravity, airspeed, pitch/AoA, throttle, and altitude. Debug flight logging still reports the calculated energy and climb values so aircraft can be audited without exposing every intermediate calculation as config.

Removed unreleased new-flight-model keys are treated as invalid cleanup targets, not compatibility aliases: `ClimbEnergyLoss`, `DiveEnergyGain`, `TakeoffDistanceMultiplier`, `NewFlightIdleNoseUpLimit`, `EnergyRetentionMultiplier`, `ClimbEnergyCostMultiplier`, `PitchEnergyCostMultiplier`, `VerticalClimbEnergyCostMultiplier`, `StallRecoveryEnergyThreshold`, and `SustainedClimbEnergyRequirement`. Use the core physical and aerodynamic values above instead.

## Formulas and interactions

### Engine output and throttle

Legacy planes still change pilot throttle through shared `throttleupdown` and keep the old HUD behavior. Planes with `useNewMobilitySystem = true` use a normalized pilot throttle (`0.0` to `1.0`) for input and display it to the pilot as `THR 0%` through `THR 100%` when `NewFlightThrottleHudDisplay = true`.

New-flight pilot input rates are independent of legacy `throttleupdown`:

```text
if throttle-up key:   currentThrottle += NewFlightThrottleChangeRateUp
if throttle-down key: currentThrottle -= NewFlightThrottleChangeRateDown
currentThrottle = clamp(currentThrottle, 0, 1)
```

Planes then smooth engine output:

```text
engineThrottle = approach(engineThrottle, currentThrottle,
                         ThrottleAcceleration when increasing,
                         EngineDrag when decreasing)
```

New-flight thrust uses an additional curve and idle floor:

```text
effectiveThrottle = NewFlightIdleThrottle
                  + (1 - NewFlightIdleThrottle)
                  * pow(engineThrottle, NewFlightThrottleResponse)
```

`effectiveThrottle`, not raw pilot throttle, drives new-flight thrust and sustainable speed. This makes 30-70% useful for cruise/formation/approach instead of forcing pilots to live near 100%. Cutting throttle reduces acceleration and adds engine-brake drag; it no longer directly deletes lift. New-flight planes also always integrate configurable downward acceleration while airborne; valid wing lift is added upward against that gravity instead of replacing it, so low-speed or stalled aircraft descend naturally. The global `NewFlightGravity` config defaults to `0.008` per tick, and individual vehicles can set `NewFlightGravity`, `FlightGravity`, or `GravityOverride` to tune heavy bombers, light fighters, jets, props, and prototype or unusual aircraft independently.

Recommended starting ranges:

- WW2 props / dogfighters: response `0.85-1.2`, up `0.004-0.008`, down `0.006-0.012`, idle `0.06-0.12`, brake drag `0.003-0.008`, lift retention `0.78-0.9`, authority scale `0.10-0.25`.
- Jets: response `1.1-1.6`, up `0.003-0.006`, down `0.004-0.008`, idle `0.08-0.15`, brake drag `0.002-0.006`, lift retention `0.82-0.94`, authority scale `0.05-0.18`.
- Heavy aircraft: response `1.0-1.4`, up `0.002-0.005`, down `0.003-0.007`, idle `0.08-0.16`, brake drag `0.002-0.005`, lift retention `0.85-0.95`, authority scale `0.05-0.15`.

### Angular response and input smoothing

Legacy mouse/stick input is first clamped and multiplied by `MobilityYaw/Pitch/Roll`, VTOL factors, and control authority. The requested angular rate is then integrated:

```text
force = input * Torque
if Damping <= epsilon:
    angularVelocity += force / InertiaMultiplier * deltaTicks
else:
    targetVelocity = force / Damping
    response = 1 - exp(-Damping * deltaTicks / InertiaMultiplier)
    angularVelocity += (targetVelocity - angularVelocity) * response
rotation += angularVelocity * deltaTicks
```

Use higher `Torque` for snappier response, higher `Damping` for lower maximum rate, and higher `InertiaMultiplier`/`Mass` for slower acceleration into the same eventual rate.

### Drag and energy

The energy model runs only in conventional airborne flight: not on ground, not VTOL nozzle mode, and not special level-off mode.

```text
speedRatio = horizontalSpeed / max(0.05, MaxLevelSpeed or Speed)
drag = BaseDrag * (0.5 + 0.5 * speedRatio^2)
drag += InducedDrag * turnLoad^2
drag += ControlSurfaceDrag * controlLoad
drag += IdleDrag * (1 - engineThrottle)
sustainableSpeed = levelSpeed * (0.35 + 0.65 * engineThrottle)
if horizontalSpeed > sustainableSpeed:
    drag += (BaseDrag + IdleDrag) * clamp((horizontalSpeed - sustainableSpeed) / levelSpeed, 0, 2)
drag += BaseDrag * AoADragMultiplier * (abs(AoA) / max(1, CriticalAoA))^2
drag = clamp(drag, 0, 0.5)
```

Vertical energy exchange:

```text
climb = clamp(motionY / 0.35, 0, 1)
dive  = clamp(-motionY / 0.35, 0, 1)
targetHorizontalSpeed = horizontalSpeed * (1 - drag)
```


### Internal energy model for new-flight fixed-wing planes

Planes with `useNewMobilitySystem = true` now run an internal per-tick energy check in addition to lift, AoA, drag, and stall logic. Legacy planes do not use these calculations. The model records debug kinetic energy from `PhysicalMass` and full 3D velocity, potential energy from `PhysicalMass`, resolved new-flight gravity, and altitude, total energy, specific energy, tick-to-tick energy delta, and an approximate excess-power value. For climb and pitch authority, however, it compares usable retained kinetic energy from positive horizontal forward airspeed plus current thrust contribution against climb and pitch demands. Vertical motion is energy-state evidence, not lift-producing forward airflow.

```text
kineticEnergy = 0.5 * PhysicalMass * (motionX^2 + motionY^2 + motionZ^2)  // debug total
forwardAirspeed = max(0, horizontal velocity dot horizontal nose heading)
usableRetainedEnergy = 0.5 * PhysicalMass * forwardAirspeed^2
potentialEnergy = PhysicalMass * resolvedGravity * max(0, altitude)
totalEnergy = kineticEnergy + potentialEnergy
specificEnergy = totalEnergy / PhysicalMass
energyDelta = totalEnergy - previousTotalEnergy
excessPower ~= energyDelta per tick

climbEnergyDemand = positiveVerticalSpeed * PhysicalMass * resolvedGravity
pitchEnergyDemand = noseUpDemand * stall/aoa/body-rate demand
energyDeficitSeverity = clamp(shortfall and specific-energy deficit, 0, 1)
```

`energyDeficitSeverity` is not a Y-motion or pitch-angle clamp. When it rises, the aircraft loses nose-up pitch authority, receives extra energy/induced drag, can trigger pitch-break recovery, and biases the nose downward through the normal angular-velocity path. The player can still command any pitch attitude; the model determines whether that attitude can be sustained from AoA severity, positive forward airflow, lift margin, energy state, and control authority. This preserves brief zoom climbs when the aircraft enters with enough usable forward airspeed/energy, but a low-speed or low-throttle aircraft at 80-90 degrees nose-up will bleed energy, see AoA rise, lose authority, stall, and rotate down instead of hovering upward. Low horizontal speed is handled by using positive horizontal forward airspeed for stall recovery, climb validation, pitch authority, and energy-deficit checks, so vertical climbing/falling speed cannot masquerade as lift-producing airflow when `motionX`/`motionZ` are near zero.

Forced nose-down recovery is applied after normal pilot pitch input has been scaled by control authority, low-speed/stall nose-up suppression, and body-rate damping. Severe stall or energy-deficit moments are queued as positive nose-down pitch velocity, then applied as a bounded post-control pitch delta in the same physics tick. This prevents pilot input or the next damping pass from being the first place where recovery affects aircraft attitude.

Tuning guidance:

* Heavy fighters: use similar retention but slightly higher pitch/climb costs so mass and pitch demand matter.
* Jets: may use slightly better retention or lower sustained-climb requirement only when `EngineThrust`, drag, mass, and gravity already justify strong climb performance.
* Bombers/transports: lower retention and raise climb, pitch, recovery, and vertical-climb requirements so they recover poorly from steep nose-up flight.
* Poor-energy aircraft, UAVs, and utility planes: use the bomber-style direction with even higher vertical-climb costs.


### Stall, AoA, lift loss, and recovery

```text
AoA = degrees_between(nose_forward_vector, velocity_vector)
stallSpeed = StallSpeed if >0 else max(0.05, topSpeed * StallSpeedFactor)
forwardAirspeed = max(0, horizontal velocity dot horizontal nose heading)
speedSeverity = max(clamp((stallSpeed - forwardAirspeed) / stallSpeed, 0, 1),
                    clamp((stallSpeed - horizontalSpeed) / stallSpeed, 0, 1))
aoaSeverity = clamp((abs(AoA) - CriticalAoA) / CriticalAoA, 0, 1)
demand = max(speedSeverity, aoaSeverity)
```

`AoA` is the angle between where the aircraft nose points and where the aircraft is actually moving. It is not the pitch angle by itself. At extremely low airspeed, the velocity vector is blended with nose-vs-horizontal attitude so a nose-high aircraft with collapsed airspeed remains aerodynamically stalled instead of appearing clean.

If not already stalling, demand above the small entry hysteresis band starts a stall. While stalling, one good frame is not enough to recover unless both recovery conditions are true in the same tick:

```text
demand > 0.03 starts a stall when not already stalling
forwardAirspeed >= (StallRecoverySpeed if >0 else stallSpeed * 1.2)
abs(AoA) <= CriticalAoA * 0.75
```

Smoothed stall severity approaches demand quickly when entering and decays more slowly when leaving:

```text
target = stalling ? max(0.12, demand) : 0
stallSeverity += (target - stallSeverity) * (0.35 if target is rising else 0.18)
```

Airborne wing lift is then filtered by airspeed, AoA, throttle/flap lift power, and stall lift loss:

```text
airspeedLift = clamp((airspeed - stallSpeed * 0.45) / max(0.05, stallSpeed * 1.35), 0, 1.25)
aoaLift = clamp(1 - max(0, abs(AoA) - CriticalAoA) / max(1, CriticalAoA), 0, 1)
liftLoss = clamp(stallSeverity * StallLiftLoss, 0, 1)
stallLift = 1 - liftLoss
weightForce = gravity * PhysicalMass
liftBeforeStallLoss = weightForce * clamp(liftPower, 0, 2.5) * airspeedLift * aoaLift
liftForce = liftBeforeStallLoss * stallLift
```

`StallLiftLoss` is applied after throttle lift retention, combat flap lift, takeoff lift, and other lift bonuses are resolved. Stalled aircraft suppress takeoff/climb lift headroom until recovery, so `validTakeoff` or `validClimb` cannot bypass stall penalties.

This means pointing the nose near vertical does not create maximum lift unless the velocity vector, positive horizontal forward airspeed, lift-to-weight, and thrust-to-weight are still within valid flying conditions. Conventional fixed-wing thrust is also limited during unsupported vertical climbs: if thrust-to-weight is below 1.0, upward powered climb is scaled by speed headroom and remaining unstalled authority. High nose-up climbs add extra AoA/energy drag and bleed vertical energy unless the aircraft is truly tuned with enough thrust and speed to support them. Lowering throttle while holding a nose-up attitude now also adds a nose-down pitch moment, so the aircraft cannot keep the same climb angle without enough effective thrust/lift.

Developed stalls remove lift through the normal lift model and apply a deterministic nose-down pitch moment:

```text
aerodynamicDemand = max(stallDemand, speedSeverity, aoaSeverity)
liftDeficit = clamp(1 - liftToWeight, 0, 1)
authorityLoss = clamp(1 - controlAuthority, 0, 1)
legacyStrengthScale = clamp(StallStrength / 0.6, 0, 4)
pitchRecovery = stallSeverity * StallPitchRecoveryStrength * legacyStrengthScale
              * (0.25 + 0.75 * aerodynamicDemand)
              * (0.55 + 0.45 * max(noseUpAttitude, liftDeficit))
deepStallBreak = stallSeverity^2 * StallBreakStrength * legacyStrengthScale
               * (0.35 + 0.65 * max(aoaSeverity, authorityLoss))
pitchAngularVelocity += clamp((pitchRecovery + deepStallBreak) * 0.45, 0, 1.8)
```

Pitch break uses the same pitch/angular velocity path as visible fixed-wing rotation. The moment is deterministic, scales with `stallSeverity`, the existing aerodynamic state, and the configured recovery/break strengths, then adds bounded nose-down angular velocity instead of directly injecting stall-only downward Y velocity.

Pilot nose-up input is also suppressed when the aircraft lacks energy or lift to support a climb:

```text
unsupportedClimb = nose-up attitude while climbing * max(0, 1 - thrustToWeight)
energyDeficit = max(stallSeverity, aoaSeverity, speedSeverity, liftDeficit, unsupportedClimb, energyDeficitSeverity)
noseUpPitchInput *= 1 - clamp(energyDeficit * (0.35 + 0.65 * noseUpAttitude), 0, 1)
```

This does not set a sustainable-pitch ceiling and does not treat attitude as the stall metric. It only fades nose-up authority from aerodynamic/energy deficits and leaves nose-down unloading input available.

`pitchBreak` is applied as a nose-down pitch moment and damps excessive nose-up pitch angular velocity. MCHeli/Minecraft rotation pitch uses inverted sign convention: nose-up attitude is negative numeric pitch, and nose-down attitude is positive numeric pitch. The pitch-break code therefore adds positive pitch angular velocity to lower the nose. It is separate from `StallInstability`: `StallInstability` still adds repeatable roll/yaw buffet and wing drop, while `StallPitchRecoveryStrength` and `StallBreakStrength` control how strongly a stalled aircraft lowers the nose, regains airspeed, and recovers only after both speed and AoA meet the recovery limits. `StallStrength` remains a legacy multiplier over both pitch moments so older aircraft packs keep their relative stall-break tuning. `StallRecoveryRate` controls how smoothly the smoothed stall severity fades out after recovery begins.

If stalling feels too weak for a specific aircraft, tune the asset first. Lower `CriticalAoA` to make excessive-AoA stalls begin earlier, raise `StallLiftLoss` to remove more lift at full stall, raise `StallPitchRecoveryStrength` for stronger normal stall unloading, raise `StallBreakStrength` for more aggressive deep-stall nose drops, and raise `StallInstability` only when you want more buffet/wing drop. Change code only when the same weakness appears across many correctly tuned `useNewMobilitySystem = true` planes or when the documented formulas no longer match observed behavior.

### G-force, speed scaling, and compressibility

```text
gForce = sqrt(1 + (airspeed * radians(turnRateDegreesPerTick))^2 / 0.08^2)
highGSeverity = clamp((gForce - MaxComfortableG) / (MaxStructuralG - MaxComfortableG), 0, 1)
highGAuthority = clamp(1 - highGSeverity * GControlPenalty, 0.05, 1)
stallAuthority = clamp(1 - stallSeverity * 0.75, 0.25, 1)
controlAuthority = highGAuthority * stallAuthority
```

Throttle does not directly scale this control authority; cutting power reduces thrust-to-weight, lift-to-weight, and energy state, which then increases stall/unsupported-climb suppression and pitch-break recovery.

Pitch also loses authority above compressibility speed:

```text
compressSeverity = clamp((airspeed - CompressibilitySpeed) / (MaxSafeSpeed - CompressibilitySpeed), 0, 1)
pitchAuthority = clamp(1 - compressSeverity * CompressibilityPitchPenalty, 0.05, 1)
```

Dive speed cap:

```text
dive = max(clamp(pitch / 60, 0, 1), clamp(-motionY / 0.35, 0, 1))
speedLimit = levelSpeed * (1 + (DiveSpeedMultiplier - 1) * dive)
```

Overspeed severity is `max(0, airspeed / MaxSafeSpeed - 1)`. Server damage accumulates by `severity * OverspeedDamageRate` each tick.

## Minimal plane config

```ini
displayname = Minimal Plane
Category = EXAMPLE.PLANE
addtexture = minimal_plane
AddSeat = 0.0, 0.8, 0.0
HUD = plane
speed = 1.2
maxhp = 80
```

## Tuned realistic flight-model example

```ini
displayname = Trainer Realistic FM
Category = EXAMPLE.PLANE
addtexture = trainer_realistic
AddSeat = 0.0, 0.9, 0.0
HUD = plane
maxhp = 120
speed = 1.55
MaxLevelSpeed = 1.45
MotionFactor = 0.97
BaseDrag = 0.0023
InducedDrag = 0.015
ControlSurfaceDrag = 0.0030
IdleDrag = 0.0034
PitchTorque = 0.380
RollTorque = 0.420
YawTorque = 0.320
PitchDamping = 0.410
RollDamping = 0.405
YawDamping = 0.475
InertiaMultiplier = 1.550
ThrottleAcceleration = 0.026
EngineDrag = 0.011
StallSpeed = 0.36
CriticalAoA = 16.0
StallLiftLoss = 0.820
AoADragMultiplier = 2.550
StallInstability = 0.560
StallRecoverySpeed = 0.46
StallPitchRecoveryStrength = 0.75
StallBreakStrength = 0.90
StallRecoveryRate = 0.22
DiveSpeedMultiplier = 1.28
MaxComfortableG = 7.5
MaxStructuralG = 8.5
GControlPenalty = 0.680
CompressibilitySpeed = 1.75
CompressibilityPitchPenalty = 0.5
MaxSafeSpeed = 2.05
OverspeedDamageRate = 0.14
FlightCeiling = 260
FlightCeilingRange = 48
```

### Physical mass and thrust


```text
forwardAccelerationPerTick = internalThrust / internalMass
```

`InertiaMultiplier` remains the separate unitless angular-inertia tuning value. Do not use it as aircraft mass.

Recommended real-world starting ranges:

| --- | ---: | ---: |
| WW2 fighters | 2500 - 6000 | 12000 - 45000 |
| Early jets | 4000 - 12000 | 15000 - 60000 |
| Modern fighters | 9000 - 35000 | 50000 - 250000 |
| Attack aircraft | 7000 - 25000 | 30000 - 150000 |
| Strategic bombers/cargo | 50000 - 250000 | 150000 - 1000000+ |

### Takeoff distance multiplier


```text
```

Minecraft runways are short and there is no single perfect block-distance formula, but the practical relationship is approximately:

```text
takeoffDistance ∝ requiredTakeoffSpeed² / forwardAcceleration
```

With the multiplier applied:

```text
```

For a rough runway estimate in Minecraft blocks, treat speed as blocks/tick and forward acceleration as blocks/tick². The constant-acceleration estimate is:

```text
```

The `max(..., 0.001)` guard is only for estimation so the formula does not divide by zero when an aircraft has too little excess thrust to accelerate. In game, use the debug `netForward` value as the practical acceleration term when validating runway length:

```text
takeoffDistanceBlocks ≈ effectiveTakeoffSpeed² / (2 * max(netForward, 0.001))
```


Use this key only as a takeoff/runway correction after the aircraft's mass, thrust, drag, and stall tuning are broadly correct. If a plane lifts off too early with a low multiplier, normal airborne stall and climb behavior still applies: it may mush, sink, or stall if it lacks speed or power.

## Safe-to-omit legacy notes


### Combat flaps and throttle interaction

Combat flaps are intentionally gated by `useNewMobilitySystem = true`; legacy packs are unaffected unless they opt in. With `NewFlightCombatFlaps = true`, the pilot toggles flaps with the Extra key, the HUD appends `FLP`, and the new flight model applies lift/control help plus extra drag and a lower safe overspeed threshold.

Use flaps with low or moderate throttle for landing and low-speed control. High throttle with flaps can improve a short turn, but the extra drag and reduced `MaxSafeSpeed * NewFlightCombatFlapOverspeed` should punish extended high-speed use. Throttle chopping plus flaps helps manage speed but should not be tuned into an instant brake; raise `NewFlightCombatFlapDrag` gradually and keep `NewFlightEngineBrakeDrag` modest.
