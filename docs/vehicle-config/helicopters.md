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
| `MainRotorMaxThrust` | float >= 0 | 0.12 | Maximum main-rotor thrust in legacy force units per tick at full RPM and full collective. Default is conservative but allows hover/climb once RPM has spooled. |
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

The new keys above are inert unless `UseNewHelicopterFlightModel = true`. Existing helicopter asset files do not need to define any of the new values, and helicopters that leave the opt-in disabled keep the legacy flight path.

The first controlled asset opt-in pass is intentionally small and conservative. `ah-6`, `uh-60ja`, and `ach47a` provide light, medium utility, and heavy armed-transport test coverage without globally changing the helicopter folder. Their values are initial flyability probes, not final realism or balance targets.

Early in-game tuning should check the effective thrust-to-weight relationship before increasing cyclic, yaw, or hover assistance. At full spool (`normalizedRotorRPM` near `1.0`) and full collective, compare `MainRotorMaxThrust * rotorEfficiency` against `PhysicalMass * gravity`, then account for `VerticalDrag`, `ParasiteDrag`, and modest translational-lift/hover-assist modifiers. If an opted-in helicopter cannot lift off, prefer small `MainRotorMaxThrust` increases first, use `PhysicalMass` reductions sparingly, and avoid using extreme `TranslationalLiftCoefficient` or control-authority values as takeoff crutches.


### Rotor RPM foundation

When `UseNewHelicopterFlightModel = true`, helicopters now maintain a normalized runtime rotor state for future lift and yaw work. `targetRotorRPM` is derived from current throttle only while the engine can run, fuel is available, the canopy is closed, and blades are usable/unfolded. `normalizedRotorRPM` then moves toward that target using `RotorSpoolUpRate` while increasing, `RotorSpoolDownRate` while decreasing, and `RotorInertia` as resistance. This rotor state drives visual rotor rotation for opted-in helicopters only; legacy helicopters keep the original throttle-driven animation and lift behavior. Opted-in vertical lift now uses mass, rotor thrust, configured gravity, ceiling/vortex efficiency, and `VerticalDrag`; cyclic, hover mode, yaw, and horizontal motion still use the legacy systems in this phase.

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

; Optional initial-test new-model values. Tune per aircraft role/size.
UseNewHelicopterFlightModel = true
PhysicalMass = 1.0
MainRotorMaxThrust = 0.13
RotorInertia = 1.0
RotorSpoolUpRate = 0.024
RotorSpoolDownRate = 0.032
CollectiveResponse = 0.08
CyclicAuthority = 1.0
TailRotorAuthority = 1.0
YawDamping = 0.20
AngularInertia = 1.0
TranslationalLiftCoefficient = 0.004
VerticalDrag = 0.026
ParasiteDrag = 0.014
HoverAssistStrength = 0.25
```

## Legacy compatibility

You can omit `enablefoldblade` and all rotor visual keys if the model does not need animated rotors. Existing `addrotorold` packs remain valid.
