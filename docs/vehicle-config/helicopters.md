# Helicopter config values and rotorcraft lift model

Helicopters inherit all shared keys from `base.md`. Helicopter-only parser keys are small; most helicopter behavior is controlled by shared `speed`, `gravity`, `MotionFactor`, mobility, throttle, and ceiling values.

## Helicopter-only keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `enablefoldblade` | boolean | false | Enables blade-folding support. |
| `addrotor` | `bladeNum,bladeRot,x,y,z,rx,ry,rz[,fold]` | none | Adds a rotor using the current renderer. Field 9 controls whether this rotor has fold functionality. |
| `addrotorold` | same as `addrotor` | none | Legacy rotor renderer. Compatibility-only. |

## Helicopter defaults that differ from base

- `MinRotationPitch = -20`, `MaxRotationPitch = 20`.
- Default `RotorSpeed = 79.99`.
- Default `SoundRange = 80`.
- Default `camerazoom = 8`.
- HUD defaults are `heli`, `heli_gnr`, then `gunner`.
- `speed` is multiplied by global `AllHeliSpeed` during validation.

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
