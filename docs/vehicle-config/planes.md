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

## Fixed-wing flight-model keys

All values are optional. Speed-like values (`Speed`, `MaxLevelSpeed`, `StallSpeed`, `StallRecoverySpeed`, `CompressibilitySpeed`, `MaxSafeSpeed`, `sweepwingspeed`) are multiplied by the global `AllPlaneSpeed` config during validation.

| Key | Type/range | Default | What it changes |
|---|---:|---:|---|
| `BaseDrag` | float[0..0.25] | 0.0015 | Baseline drag. Scales with speed ratio squared and also participates in AoA drag. |
| `InducedDrag` | float[0..0.25] | 0.006 | Extra drag from bank angle and body-rate load. |
| `ControlSurfaceDrag` | float[0..0.25] | 0.0025 | Extra drag while pitch/roll/yaw angular rates are high. |
| `ClimbEnergyLoss` | float[0..0.25] | 0.006 | Horizontal speed removed while climbing. |
| `DiveEnergyGain` | float[0..0.25] | 0.008 | Horizontal speed added while descending. |
| `MaxLevelSpeed` | float[0..4] | 0 = use `Speed` | Sustainable full-power speed in level flight. |
| `IdleDrag` | float[0..0.25] | 0.004 | Extra drag as engine output approaches zero. |
| `PitchTorque`, `RollTorque`, `YawTorque` | float[0..100] | 0.35 | Local-axis torque applied to requested angular rates. Higher values respond faster. |
| `PitchDamping`, `RollDamping`, `YawDamping` | float[0..100] | 0.35 | Angular damping. Higher values lower steady body rate for the same torque. |
| `InertiaMultiplier` | float[0.05..100] | 1 | Resistance to angular acceleration. Higher values make controls feel heavier. |
| `Mass` | float[0.05..100] | alias of `InertiaMultiplier` | Legacy/compatibility alias; no separate weight simulation exists. |
| `ThrottleAcceleration` | float[0..1] | 0.02 | Max engine-output increase per tick toward pilot throttle. |
| `EngineDrag` | float[0..1] | 0.015 | Max engine-output decrease per tick. |
| `StallSpeed` | float[0..10] | 0 | Absolute stall threshold; if 0, uses `max(0.05, topSpeed * StallSpeedFactor)`. |
| `CriticalAoA` | float[1..90] | 18 | AoA in degrees where stall demand begins. |
| `StallLiftLoss` | float[0..1] | 0.65 | Fraction of lift removed at full stall. |
| `AoADragMultiplier` | float[0..10] | 1.5 | Scales quadratic AoA drag. |
| `StallInstability` | float[0..5] | 0.35 | Buffet, yaw shake, and deterministic wing-drop strength. |
| `StallRecoverySpeed` | float[0..10] | 0 = `StallSpeed*1.2` | Speed required, with low AoA, to exit a stall. |
| `StallSpeedFactor` | float[0..0.95] | 0.22 | Legacy derived stall threshold when `StallSpeed` is omitted. |
| `StallStrength` | float[0..4] | 0.6 | Legacy sink strength multiplier during stall. |
| `DiveSpeedMultiplier` | float[1..2] | 1.25 | Maximum speed cap multiplier in a dive. |
| `MaxComfortableG` | float[1..30] | 4 | G where high-G control fade starts. |
| `MaxStructuralG` | float[1..50] | 8 | G where high-G fade reaches the configured penalty and structural hook begins. Warns if below `MaxComfortableG`. |
| `GControlPenalty` | float[0..1] | 0.7 | Max fraction of authority removed by high G. |
| `CompressibilitySpeed` | float[0..10] | 0 = 90% of level speed | Speed where pitch authority starts fading. |
| `CompressibilityPitchPenalty` | float[0..1] | 0.65 | Max pitch-authority loss by `MaxSafeSpeed`. |
| `MaxSafeSpeed` | float[0..10] | 0 = 110% of level speed | Overspeed threshold. Warns if not above `CompressibilitySpeed`. |
| `OverspeedDamageRate` | float[0..100] | 0.2 | Damage per tick at 100% overspeed. Set 0 to disable damage. |

## Formulas and interactions

### Engine output and throttle

Pilot throttle still changes through shared `throttleupdown`. Planes then smooth engine output:

```text
engineThrottle = approach(engineThrottle, currentThrottle,
                         ThrottleAcceleration when increasing,
                         EngineDrag when decreasing)
```

`engineThrottle`, not raw pilot throttle, drives idle drag and sustainable speed.

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
                      + dive * DiveEnergyGain
                      - climb * ClimbEnergyLoss
```

### Stall, AoA, lift loss, and recovery

```text
AoA = degrees_between(nose_forward_vector, velocity_vector)
stallSpeed = StallSpeed if >0 else max(0.05, topSpeed * StallSpeedFactor)
speedSeverity = clamp((stallSpeed - airspeed) / stallSpeed, 0, 1)
aoaSeverity = clamp((abs(AoA) - CriticalAoA) / CriticalAoA, 0, 1)
demand = max(speedSeverity, aoaSeverity)
```

If not already stalling, any positive demand starts a stall. While stalling, recovery requires both:

```text
airspeed >= (StallRecoverySpeed if >0 else stallSpeed * 1.2)
AoA <= CriticalAoA * 0.75
```

Smoothed stall severity approaches demand quickly when entering and decays more slowly when leaving:

```text
target = stalling ? max(0.12, demand) : 0
stallSeverity += (target - stallSeverity) * (0.35 if target is rising else 0.18)
liftLoss = clamp(stallSeverity * StallLiftLoss, 0, 1)
if motionY > 0: motionY *= 1 - liftLoss * 0.12
motionY -= 0.018 * liftLoss * StallStrength
```

Stall instability adds repeatable roll/yaw/pitch buffet using `StallInstability * stallSeverity`.

### G-force, speed scaling, and compressibility

```text
gForce = sqrt(1 + (airspeed * radians(turnRateDegreesPerTick))^2 / 0.08^2)
highGSeverity = clamp((gForce - MaxComfortableG) / (MaxStructuralG - MaxComfortableG), 0, 1)
highGAuthority = clamp(1 - highGSeverity * GControlPenalty, 0.05, 1)
stallAuthority = clamp(1 - stallSeverity * 0.75, 0.25, 1)
controlAuthority = highGAuthority * stallAuthority
```

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
MotionFactor = 0.975
BaseDrag = 0.0020
InducedDrag = 0.0080
ControlSurfaceDrag = 0.0030
IdleDrag = 0.0060
ClimbEnergyLoss = 0.0080
DiveEnergyGain = 0.0100
PitchTorque = 0.42
RollTorque = 0.55
YawTorque = 0.22
PitchDamping = 0.40
RollDamping = 0.42
YawDamping = 0.35
InertiaMultiplier = 1.35
ThrottleAcceleration = 0.018
EngineDrag = 0.014
StallSpeed = 0.36
CriticalAoA = 16.0
StallLiftLoss = 0.70
AoADragMultiplier = 1.8
StallInstability = 0.45
StallRecoverySpeed = 0.46
DiveSpeedMultiplier = 1.22
MaxComfortableG = 4.5
MaxStructuralG = 8.5
GControlPenalty = 0.65
CompressibilitySpeed = 1.75
CompressibilityPitchPenalty = 0.50
MaxSafeSpeed = 2.05
OverspeedDamageRate = 0.10
FlightCeiling = 260
FlightCeilingRange = 48
```

## Safe-to-omit legacy notes

Old plane packs can omit every new aerodynamic key. Defaults are applied and `StallSpeedFactor` preserves derived low-speed stall behavior. Use `Mass` only for compatibility with packs that already chose that name; prefer `InertiaMultiplier` for new configs.
