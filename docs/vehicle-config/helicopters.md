# Helicopter config values and rotorcraft lift model

Helicopters inherit all shared keys from `base.md`. Helicopter-only parser keys are small; most helicopter behavior is controlled by shared `speed`, `gravity`, `MotionFactor`, mobility, throttle, and ceiling values.

## Helicopter-only keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `enablefoldblade` | boolean | false | Enables blade-folding support. |
| `addrotor` | `bladeNum,bladeRot,x,y,z,rx,ry,rz[,fold]` | none | Adds a rotor using the current renderer. Field 9 controls whether this rotor has fold functionality. |
| `addrotorold` | same as `addrotor` | none | Legacy rotor renderer. Compatibility-only. |
| `UseNewHelicopterFlightModel` / `EnableNewHelicopterFlightModel` | boolean | false | Helicopter-specific opt-in gate for the future rewritten helicopter model. This is intentionally separate from `UseNewMobilitySystem`; when false, all new helicopter physical tuning values are inert and legacy helicopter flight remains unchanged. |
| `PhysicalMass` | float >= 0.01 | 1.0 | Relative mass for future force calculations. `1.0` is the legacy-scale baseline rather than real kilograms. |
| `MainRotorMaxThrust` | float >= 0 | 0.06 | Future maximum main-rotor upward force in legacy motion units per tick at full collective. Default is near the current hover/lift scale. |
| `RotorInertia` | float >= 0.01 | 1.0 | Future rotor RPM acceleration resistance; larger values should make RPM change more slowly. |
| `RotorSpoolUpRate` | 0.0-1.0 | 0.02 | Future normalized rotor RPM gain per tick while spooling up. |
| `RotorSpoolDownRate` | 0.0-1.0 | 0.03 | Future normalized rotor RPM loss per tick while spooling down. |
| `CollectiveResponse` | 0.0-1.0 | 0.08 | Future smoothing rate for collective/lift input changes. |
| `CyclicAuthority` | float >= 0 | 1.0 | Future pitch/roll cyclic authority multiplier. |
| `TailRotorAuthority` | float >= 0 | 1.0 | Future yaw/tail-rotor authority multiplier. |
| `YawDamping` | float >= 0 | 0.15 | Future yaw stabilization/damping multiplier. |
| `AngularInertia` | float >= 0.01 | 1.0 | Relative rotational inertia for future angular acceleration. |
| `TranslationalLiftCoefficient` | float >= 0 | 0.004 | Future forward-speed lift bonus coefficient; default mirrors the current translational-lift cap scale. |
| `VerticalDrag` | float >= 0 | 0.02 | Future vertical climb/descent damping coefficient. |
| `ParasiteDrag` | float >= 0 | 0.01 | Future horizontal air-drag coefficient. |
| `HoverAssistStrength` | 0.0-1.0 | 0.0 | Future hover assistance strength. `0.0` keeps assist disabled by default. |

## Helicopter defaults that differ from base

- `MinRotationPitch = -20`, `MaxRotationPitch = 20`.
- Default `RotorSpeed = 79.99`.
- Default `SoundRange = 80`.
- Default `camerazoom = 8`.
- HUD defaults are `heli`, `heli_gnr`, then `gunner`.
- `speed` is multiplied by global `AllHeliSpeed` during validation.

## Future helicopter flight-model foundation

The new keys above only expose configuration and telemetry for a future helicopter-specific model. They do not change runtime helicopter physics unless `UseNewHelicopterFlightModel = true`, and the current implementation still leaves actual flight forces on the legacy path. Existing helicopter asset files do not need to define any of the new values.

## Rotorcraft lift formulas

When not floating in water, helicopter vertical motion uses shared gravity and throttle-dependent lift:

```text
motionY += gravity or gravityInWater
attitude = abs(pitch) + abs(roll)
liftAttitudeFactor = attitude * 0.6
if liftAttitudeFactor <= 50:
    liftAttitudeFactor = 1 - liftAttitudeFactor / 50
else:
    liftAttitudeFactor = 0
throttle = currentThrottle * (0.65 if destroyed else 1)
horizontalSpeed = sqrt(motionX^2 + motionZ^2)
ceilingLift = clamp((FlightCeiling - altitude) / max(1, FlightCeilingRange), 0, 1)
translationalLift = clamp(horizontalSpeed / max(0.1, Speed), 0, 1) * 0.004
if motionY < -0.12 and horizontalSpeed < 0.15 and throttle > 0.45:
    ceilingLift *= 0.55
    motionY -= 0.006
motionY += ((liftAttitudeFactor * 0.025 + 0.03) * throttle
            + translationalLift * throttle) * ceilingLift
```

Tuning notes:

- More `speed` increases top horizontal speed and reduces how quickly translational lift reaches its cap.
- More `throttleupdown` makes collective changes faster.
- Larger `FlightCeilingRange` makes high-altitude lift fade more gently.
- Vortex-ring state occurs in powered, near-vertical descents; recover by gaining forward speed or reducing collective.

## Minimal helicopter config

```ini
displayname = Minimal Helicopter
Category = EXAMPLE.HELI
addtexture = minimal_heli
AddSeat = 0.0, 1.0, 0.0
HUD = heli
speed = 0.85
maxhp = 100
addrotor = 4, 45, 0.0, 2.2, 0.0, 0.0, 0.0, 0.0
```

## Legacy compatibility

You can omit `enablefoldblade` and all rotor visual keys if the model does not need animated rotors. Existing `addrotorold` packs remain valid.
