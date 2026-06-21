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
| `MaxClimbRate` / `HelicopterMaxClimbRate` / `NewHelicopterMaxClimbRate` | blocks/tick >= 0 | 0.16 | New-model maximum upward velocity after lift and vertical drag. Use lower values on light helicopters that climb too quickly at Minecraft scale; `0` disables the cap. |
| `ParasiteDrag` | float >= 0 | 0.01 | Future horizontal air-drag coefficient. Increase this to damp horizontal speed and make larger helicopters feel heavier. |
| `HorizontalRotorThrustScale` / `HelicopterHorizontalThrustScale` | float >= 0 | 0.35 | New-model-only Minecraft-scale multiplier for converting pitch-attitude rotor thrust into forward/backward acceleration. Lower values preserve hover/climb tuning while reducing map-scale forward acceleration. |
| `HelicopterLateralThrustScale` | float >= 0 | 0.45 | New-model-only multiplier applied only to roll-attitude sideways thrust. Values below `1.0` keep A/D roll responsive but make sideways acceleration less efficient than pitch-driven forward flight. |
| `HelicopterLateralDrag` | float >= 0 | 0.055 | New-model-only drag applied to velocity along the helicopter right/left axis after decomposing horizontal motion. Usually higher than `ParasiteDrag` so sideways sliding bleeds off faster than forward flight. |
| `HelicopterMaxLateralSpeedScale` | float >= 0 | 0.45 | New-model-only lateral speed cap as a scale of the normal new-heli horizontal safety limit. Clamps only the right/left velocity component, not total horizontal speed. |
| `HelicopterBackwardThrustScale` | float >= 0 | omitted = 1.0 | Optional new-model-only multiplier for backward pitch-attitude thrust. Omit to preserve previous forward/backward symmetry; set below `1.0` for weaker backward acceleration. |
| `HelicopterMaxBackwardSpeedScale` | float >= 0 | omitted = 1.0 | Optional new-model-only backward speed cap as a scale of the normal new-heli horizontal safety limit. Omit to preserve previous backward speed; set below `1.0` to make backward flight less efficient. |
| `HoverAssistStrength` | 0.0-1.0 | 0.75 | New-model hover assistance strength. `0.0` disables assist; the default now provides strong altitude and horizontal-drift stabilization when hover mode is active and the pilot is not commanding collective/cyclic input. |

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

Early in-game tuning should check the effective thrust-to-weight relationship before increasing cyclic, yaw, or hover assistance. At full spool (`normalizedRotorRPM` near `1.0`) and full collective, `MainRotorMaxThrust` is the primary hover/climb power knob: compare `MainRotorMaxThrust * rotorEfficiency` against `PhysicalMass * gravity`, then account for `VerticalDrag`, `ParasiteDrag`, and modest translational-lift/hover-assist modifiers. If an opted-in helicopter cannot lift off, prefer small `MainRotorMaxThrust` increases first, use `PhysicalMass` reductions sparingly, and do not use cyclic authority or extreme `TranslationalLiftCoefficient` as takeoff crutches. After lift is correct, tune pitch-driven forward feel primarily with `HorizontalRotorThrustScale`, then use `ParasiteDrag` as forward speed damping and `CyclicAuthority` for role-based responsiveness; heavier helicopters should generally use lower cyclic/scale and higher drag than light helicopters. Sideways movement comes from roll attitude and is intentionally less efficient: tune `HelicopterLateralThrustScale`, `HelicopterLateralDrag`, and `HelicopterMaxLateralSpeedScale` instead of reducing roll authority. Backward movement can be made weaker than forward flight with `HelicopterBackwardThrustScale` and `HelicopterMaxBackwardSpeedScale`; when those optional values are omitted, backward thrust and speed limits inherit the forward behavior for compatibility. Avoid stacking very low thrust scale with very high drag, because that makes cyclic input feel stuck instead of merely controlled.


### Rotor RPM foundation

When `UseNewHelicopterFlightModel = true`, helicopters now maintain a normalized runtime rotor state for future lift and yaw work. `targetRotorRPM` is derived from current throttle only while the engine can run, fuel is available, the canopy is closed, and blades are usable/unfolded. `normalizedRotorRPM` then moves toward that target using `RotorSpoolUpRate` while increasing, `RotorSpoolDownRate` while decreasing, and `RotorInertia` as resistance. This rotor state drives visual rotor rotation for opted-in helicopters only; legacy helicopters keep the original throttle-driven animation and lift behavior. Opted-in vertical lift now uses mass, rotor thrust, configured gravity, ceiling/vortex efficiency, a squared RPM lift-spool curve, `VerticalDrag`, and `MaxClimbRate`. W/S are treated as collective-only controls in the new helicopter model; they do not directly request forward/backward horizontal thrust. Forward movement comes primarily from pitch attitude feeding horizontal rotor thrust, while sideways movement comes from roll attitude. The new model calculates pitch-derived and roll-derived thrust independently so roll controls can stay immediate while lateral acceleration, drag, and speed are tuned separately from forward flight. Backward pitch can also use optional weaker thrust and speed caps without changing collective, hover mode, roll authority, cyclic authority, or pilot input responsiveness. New-model hover assist uses `HoverAssistStrength` to add bounded collective and cyclic corrections that resist altitude changes and drift without overriding active pilot input.

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
MaxClimbRate = 0.16
ParasiteDrag = 0.035
HorizontalRotorThrustScale = 0.35
HelicopterLateralThrustScale = 0.45
HelicopterLateralDrag = 0.055
HelicopterMaxLateralSpeedScale = 0.45
HelicopterBackwardThrustScale = 0.35
HelicopterMaxBackwardSpeedScale = 0.35
HoverAssistStrength = 0.75
```

## Legacy compatibility

You can omit `enablefoldblade` and all rotor visual keys if the model does not need animated rotors. Existing `addrotorold` packs remain valid.
