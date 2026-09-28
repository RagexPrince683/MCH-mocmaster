# Tank config values

Tanks inherit all shared keys from `base.md` and add track/weight keys. Tank `speed` is multiplied by global `AllTankSpeed` during validation.

Set the shared `LWR = true` option to enable the existing tank laser warning alert sound. Its default is `false`, so `LWR = false`, an invalid value, or an omitted entry disables that sound. LWR detection does not require flares and does not grant or alter flares, chaff, APS, smoke launchers, or the separate `RWRType` radar warning receiver.

## Tank-only keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `WeightType` | enum `normal`, `car`, `tank` | `normal` / 0 | Parser maps `car` to 1 and `tank` to 2; any other text is 0. |
| `CivilianCarGrip` | boolean | `false` | Explicit civilian passenger-car grip and steering opt-in, independent of `WeightType` and `Category`. |
| `CivilianCarDrivetrain` | boolean | `false` | Explicit passenger-car engine, automatic gears, longitudinal slip, and separate service/handbrake opt-in. Independent of grip, weight, category and `DriveType`. |
| `CarThrottleResponse` | float[0.05..1], fraction/tick | 0.25 | Pedal response toward full/released throttle, at 20 Hz. |
| `CarIdleRPM` | float[500..2000], RPM | 800 | Gameplay idle engine speed. |
| `CarRedlineRPM` | float[3000..12000], RPM | 6500 | Gameplay redline; wheel-speed governor softens engine torque above the current gear's band. |
| `CarForwardGears` | integer[1..8] | 5 | Automatic forward gear count; reverse is a separate gear. |
| `CarShiftTicks` | integer[1..40], ticks | 8 | Torque ratio blend duration; default 0.4 seconds. No velocity reset or shift pause. |
| `CarServiceBrake` | float[0.01..0.5], blocks/tick² equivalent wheel torque | 0.24 | Total service brake demand, distributed across all configured wheels. Contact/traction bounds body force. |
| `CarHandbrake` | float[0.01..0.5], blocks/tick² equivalent wheel torque | 0.20 | Separate rear-axle brake demand while Space is held, including at rest. |
| `DriveType` | enum `FWD`, `RWD`, `AWD` | unset | Front, rear, or both axles supply drive torque/traction. Case-insensitive; unset/invalid keeps legacy thrust without `CivilianCarDrivetrain`, or uses axle-neutral propulsion with it. |
| `CivilianCarReverseSpeed` | float[0..4], blocks/tick | 0 | Explicit civilian reverse-control opt-in, independent of grip/weight/category. Positive values cap powered backward horizontal movement. Zero, omitted, malformed, NaN or infinity preserves legacy behavior. Finite values are clamped to the range. |
| `EnableBrakeLights` | boolean | `false` | Enables brake-light rendering for this vehicle. This is an independent opt-in and does not infer a civilian car from `CivilianCarGrip`, weight, or category. |
| `FrontTireSize` | metric radial size, e.g. `225/50R16`, `265/35ZR19`, or `175R14` | unset | Optional front tire dimensions for opted-in civilian cars. Unset/invalid sizes use neutral tuning. |
| `RearTireSize` | same format as `FrontTireSize` | unset | Optional rear tire dimensions, independent of the front. |
| `CarLateralGrip` | float[0..0.25], blocks/tick² | 0.12 | Server sideways correction limit for opted-in cars; wheel contact scales it. `0` disables lateral grip and its steering coupling. With `DriveType`, lateral demand also reduces available propulsion; propulsion never reduces the existing lateral correction. |
| `CarMinimumSteering` | float[0..10], degrees/tick | 0 | Optional contact-scaled lower bound on steering yaw authority at speed. Unlike `CarLateralGrip`, it turns the body but does not add sideways force, so excess demand produces understeer. |
| `SuspensionSpring` | float[0..0.25], blocks/tick² at full compression | 0.055 | Bounded spring acceleration. Used only when `CivilianCarGrip = true`; it is not derived from `Weight`. |
| `SuspensionCompressionDamping` | float[0..0.25], acceleration per block/tick of compression speed | 0.035 | Shock damping while a supported wheel moves upward into the body. |
| `SuspensionReboundDamping` | float[0..0.25], acceleration per block/tick of rebound speed | 0.050 | Shock damping while a supported wheel extends; the higher default settles the body after a bump. |
| `SuspensionTravel` | float[0.05..1.5], blocks | 0.45 | Vertical collision-shape sweep available to each wheel. |
| `CarGripDiagnostics` | boolean | `false` | Opt-in per-tick server diagnostics in `logs/car-tire-grip.csv`, separate from console output. |
| `WeightedCenterZ` | float[-1000..1000] | 0 | Moves the simulated center of weight forward/back. Positive/negative effect depends on model orientation. |
| `TrackMaxHP` | int[1..1000000] | 100 | Track durability. |
| `EnableTurretPop` | boolean | `false` | When `true`, enables the catastrophic detached-turret destruction effect. Requires a configured dynamic turret assembly. |
| `AddTrackHitBox` | `x,y,z,width,height[,damageFactor]` | none; damage factor 1 | Adds a track-typed extra bounding box. |

## Tank defaults that differ from base

- Default wheel contact points are `(±1.5, -0.24, z=2/-2)` pairs generated by `getDefaultWheelList`.
- Default `SoundRange = 50`.
- Default `RotorSpeed = 47.94` for compatible wheel/roller visuals.
- Default `camerazoom = 8`.
- HUD defaults are `tank`, `tank`, then `gunner`.

## Practical tuning

Civilian car grip uses wheel collision support adjusted for the invisible wheel box rest gap, a bounded sideways correction, and a steering limit tied to the same contact/grip budget. Tire sizes alter damping response by at most ±5%; tire width does not directly multiply the grip limit. See [car tire grip](../car-tire-grip.md) for the contact reproduction, before/after values, bundled eligibility, sources and diagnostics. Omit `CivilianCarGrip` to preserve existing handling; tire fields, `WeightType` and `Category` alone never enable it.

### Civilian car drivetrain

`CivilianCarDrivetrain = true` selects the new server-owned passenger-car engine/transmission and
longitudinal wheel-speed model. All eight settings above are gameplay defaults, not specifications
inferred from a car name. Malformed/nonfinite numeric settings use their documented defaults;
finite values clamp to their ranges (integer settings truncate after clamping). Removed settings reset
on reload. Omission preserves the existing engine/brake path, including any separately configured
`DriveType`, grip, reverse ceiling and lamp behavior.

W supplies engine throttle; S service-brakes forward motion, then selects reverse after four consecutive
ticks within 0.025 blocks/tick longitudinal and 0.06 blocks/tick horizontal speed. W brakes backward
motion before selecting forward drive. W+S always supplies throttle and service braking and cannot
select reverse. Space independently brakes the rear wheels without suppressing either pedal key.
S and Space still illuminate opted-in brake lamps, including S during powered reverse; lamps do not
control physical brake torque. Leaving the pilot seat, changing pilot, entering a GUI, or ending control
releases inputs. Engine shutdown/fuel loss also releases drive and brake inputs.
Steering retains its existing force/yaw limits; the direction sign uses actual forward/backward motion
rather than engine throttle, which now rises in both gears. Braking with S while still moving forward
therefore keeps forward steering, and W while braking backward motion keeps reverse steering.

Forward gears divide the configured `Speed` into equal gameplay bands, upshifting at 88% of the current
band and downshifting below 65% of the preceding band; shifts blend ratios from 3.0 in first to 1.0 in
top gear. A one-gear setup uses 1.0. At rest the transmission returns to first; reverse uses 1.0.
Shifts never clear momentum. RPM follows powered wheel speed and pedal demand between idle and
redline; an above-band soft torque governor limits sustained wheel overspeed. `Speed` still bounds
vehicle speed, `CivilianCarReverseSpeed` still bounds powered reverse, and `ThrottleDownFactor`
still scales reverse torque (the old steady demand, bounded to 0.1). These fields are unchanged in
bundled definitions. Gear ratios, inertia, RPM and brake torques make no manufacturer accuracy claim.

`DriveType` selects which axles receive engine torque. If it is absent/invalid, this opt-in uses all
configured wheels equally without assigning a drivetrain identity. Front/rear wheel surface speeds
integrate drive torque, independently opposed by service and rear handbrake torque. Contact reaction
is bounded by the existing 0.24 blocks/tick² longitudinal budget, with the existing lateral demand
reserved first. Unsupported wheels can spin but cannot propel/brake the body; missing wheels keep
their configured denominator. Braking at rest opposes drive torque rather than relying on motion.
Excess wheel torque becomes longitudinal spin; brake-only body force cannot push motion through zero.
For an axle with `n` configured wheels out of `T`, gameplay inverse wheel inertia is `k = 4*T/n`.
Its surface speed first receives `driveDemand*k`; brake demand opposes that speed without reversing it.
Free tire reaction is `(wheelSurfaceSpeed - bodyLongitudinalSpeed)/(k+1)`, clamped to the axle's
remaining traction. Reaction accelerates the body and subtracts `reaction*k` from wheel surface speed.
Unused brake torque holds a stopped wheel against that reaction, so brakes also work at low speed/rest.
Wheel surface speed is bounded to ±8 blocks/tick; spin is the excess magnitude over the resulting body
speed divided by `max(0.1, abs(bodySpeed))`, bounded to 3 for synchronization/effects.
Engine force is horizontal; the new path omits the old pitch-linked vertical thrust while preserving
gravity, drag, suspension, lateral grip, steering, collision and speed clamps.

Three changed-only tank DataWatcher integers synchronize RPM, throttle, gear, both brake states,
engine-running state, axle surface speeds, spin slip and axle contact. Clients smooth sound pitch/volume
and independently animate each axle using the existing `PartWheelRot` visual scale. Supported powered
axles emit at most one small smoke puff every four ticks when excess wheel speed exceeds 0.12
blocks/tick and normalized spin exceeds 0.35; air-spinning wheels produce no road smoke. This is a
gameplay surface-speed/inertia model, not a differential, load-transfer or measured tire simulation.
See [the civilian opt-in list and driving checks](../car-tire-grip.md#engine-gears-brakes-and-longitudinal-slip).

The following `DriveType`-only rules describe the legacy engine path when
`CivilianCarDrivetrain` is omitted/false:

`DriveType = FWD` uses front wheel support; `RWD` uses rear wheel support; `AWD` pools
both axles' available traction without a prescribed torque split. The field enables propulsion
limiting independently of `CivilianCarGrip`; it does not enable suspension, lateral grip, or steering.
Omit it to retain legacy thrust even on a grip-enabled car. Invalid values also select legacy thrust,
and reload clears a removed field.

The server samples current collision-derived wheel support before adding forward or reverse thrust.
Front/rear membership and counts come from the configured mirrored `SetWheelPos` layout, divided at
the midpoint of its minimum/maximum local Z. Positive Z is forward. Missing/dead wheels keep their
configured place in the denominator; a missing driven axle supplies no propulsion.

Added acceleration scales by supported/configured **driven** wheels and is capped by the driven
axles' remaining traction. The gameplay longitudinal budget is 0.24 blocks/tick² for the complete
configured wheel set, apportioned by supported wheel count: a fully supported two-wheel axle on a
four-wheel car supplies 0.12. When civilian lateral grip is active, each driven axle's lateral demand
reserves part of that capacity through a bounded traction ellipse. AWD pools the remaining capacity;
this is a gameplay force budget, not a manufacturer torque split, differential, or load-transfer model.
See [the equations and car audit](../car-tire-grip.md#throttle-and-drivetrain).

No driven contact means no added engine force, including the existing throttle-linked vertical term.
Existing momentum remains subject to normal drag, gravity, collision, and speed limits. Throttle ramp,
braking inputs, gearing, steering, brake lights, and collision are unchanged. Braking is not redistributed
to the driven axle. Reaching a traction limit can lower acceleration or attainable speed without changing
either configured speed ceiling.

Civilian suspension sweeps each individual wheel collision box through `SuspensionTravel`, so full
blocks, slabs, stairs, and other collision-box terrain contribute their actual top surface rather than
a heightmap or paired `onGround` flag. Both the current and next-tick horizontal wheel positions are
sampled. Only supported wheels generate spring/damper response; an entirely unsupported car follows
normal gravity, falling, body collision, and crash-damage behavior.

Each `AddPartWheel` is matched to the nearest mirrored `SetWheelPos` collision wheel in local X/Z and
moves vertically with its interpolated compression. Its authored `AddPartWheel` Y position is the
neutral rendered position; it is not replaced by the collision wheel's `SetWheelPos` Y coordinate.
The neutral compression is initialized only when the complete wheel set has even support, then follows
the settled level-ground compression slowly. This prevents a single tire's first spawn/landing contact
from raising or lowering that tire permanently while preserving differential travel on uneven blocks.
Body pitch and roll continue converging toward the supported wheel heights every tick, including while
stopped, and settle exactly at zero after level support is restored. These angles use the wheels'
collision-resolved contact heights rather than compression alone, so an existing body angle cannot feed
itself back into the next terrain-angle calculation. A match farther than 0.85 blocks is
considered a different/decorative layout and receives the legacy wheel animation without suspension
translation. This fallback permits model packs to use a different number or arrangement of visible wheels safely.
The four suspension defaults are written explicitly into bundled `CivilianCarGrip = true` definitions;
other tanks, military vehicles, aircraft, and boats remain on their existing wheel behavior.

### Civilian car reverse controls

The legacy throttle sequence below applies when `CivilianCarDrivetrain` is omitted/false.
With the new opt-in, the engine/gearing controls above replace that sequence, retaining the same
absolute reverse ceiling and `ThrottleDownFactor` acceleration tuning.

Use `CivilianCarReverseSpeed` for an absolute reverse speed ceiling, and shared
`ThrottleDownFactor` for reverse acceleration. At 20 ticks/second, one block/tick is
72 km/h; this is the conversion used by `MCH_HudShared.getRawSpeedKmh`. For example,
25 km/h is `CivilianCarReverseSpeed = 0.347222`. Do not divide by the existing
`Speed` value. `AllTankSpeed` still scales `Speed`, but does not scale this field;
the effective reverse ceiling is the smaller of those two validated speeds.

The server applies the new ceiling after drag, wheel updates and civilian grip,
immediately before moving. It scales horizontal velocity only when reverse thrust
is active and the velocity points backward relative to the final vehicle heading.
Forward motion while S brakes, unpowered rolling, lateral-only motion and vertical
motion keep their existing handling. Client extrapolation uses the same clamp;
position interpolation continues to follow the server. Existing synchronized
throttle/reverse controls are used, with no new network packet.

In tanks, holding S first reduces positive forward throttle by
`0.01 * ThrottleUpDown` per tick, plus the existing brake-state reduction of
`0.02 * ThrottleUpDown` while that state is active. After forward throttle reaches zero, it adds
`0.0025 * ThrottleUpDown * ThrottleDownFactor` to reverse demand. Demand already
decays by 0.8 each tick (and by another 0.5 while braking). S also sets the existing
synchronized brake-lamp state. For opted-in cars, that additional 0.5 damping
stops once S requests reverse with zero forward throttle; Space clears S in the
input handler and retains the original brake damping. With a positive
`CivilianCarReverseSpeed`, demand is additionally bounded to `[0, 0.1]`, matching
maximum forward thrust, and W clears it immediately so forward acceleration starts
on that tick. Without the field these additions do not run. Forward throttle
increments, drag, the existing `Speed` clamp, forward braking and Space brake
damping are unchanged.

The new bundled factors are gameplay acceleration choices, not measured 0-to-speed
times. They allow each target to be reached against the existing ground drag;
holding S longer does not store increasing force. See the [25-car research and
configuration table](civilian-car-reverse-speeds.md) for variants, tire evidence,
calculations, conservative estimates and unresolved identities.

### Brake lights

Set `EnableBrakeLights = true` and identify each applicable fixed rear lamp with `AddBrakeLight`.
`AddBrakeLight` accepts the same `x,y,z,startColor,endColor,height,width,yaw,pitch` values as
`AddFixedSearchLight`; the distinct name prevents headlights, reverse lamps, military lamps, and other
searchlights from being selected by position or color heuristics. Identified lamps continue to behave as
normal vehicle lights when the regular light control is on. While the driver holds Space (the existing
brake input) or S (the existing reverse/throttle-down input), they are also rendered as an additive brake
pass. Consequently they work with normal lights off and become visibly brighter without replacing the
existing rear-light pass when normal lights are on. The pressed state is server-authoritative and uses the
vehicle's synchronized status, so nearby multiplayer clients see the same car-attached lamp geometry.

Omitting `EnableBrakeLights`, setting it to `false`, or using an invalid value disables the brake-light
pass. `AddBrakeLight` entries remain ordinary fixed lights in that case. Bundled civilian cars opt in;
`fordpolice.txt` currently has no authored rear light positions, so it is enabled but cannot display brake
lights until suitable `AddBrakeLight` entries are supplied.

- Use shared `speed`, `MotionFactor`, `MobilityYawOnGround`, `CanMoveOnGround`, `CanRotOnGround`, and `PivotTurnThrottle` for driving feel.
- Use `SetWheelPos` for wheel/contact layout and `AddTrackHitBox` for damageable tracks. Moving tanks also use their `SetWheelPos` contact points to trample grass blocks under their wheels into dirt.
- `TrackRollerRot`, `PartWheelRot`, `AddCrawlerTrack`, `AddTrackRoller`, and `AddPartWheel` are visual helpers inherited from the shared parser.

## Minimal tank config

```ini
displayname = Minimal Tank
Category = EXAMPLE.TANK
addtexture = minimal_tank
AddSeat = 0.0, 1.0, 0.0
HUD = tank
maxhp = 250
speed = 0.35
WeightType = tank
TrackMaxHP = 120
EnableTurretPop = false
LWR = false
AddTrackHitBox = 1.2, 0.0, 0.0, 0.5, 0.5, 1.0
AddTrackHitBox = -1.2, 0.0, 0.0, 0.5, 0.5, 1.0
```

## Safe-to-omit notes

`WeightType`, `WeightedCenterZ`, `TrackMaxHP`, `AddTrackHitBox`, `EnableTurretPop`, `EnableBrakeLights`, `DriveType`, `CivilianCarReverseSpeed`, and `LWR` are optional. Omitting `DriveType` keeps legacy propulsion. Omitting `CivilianCarReverseSpeed` keeps legacy reverse controls and speed limiting; a separately configured `DriveType` can still limit reverse traction. Omitting `EnableTurretPop` keeps the turret attached when the tank is destroyed. Omitting `EnableBrakeLights` disables the brake-light pass. Omitting `LWR` leaves tank alert audio disabled; omitting the other keys leaves default ground behavior and no explicit track hitboxes.

`EnableTurretPop = true` enables a catastrophic destruction effect which launches the exact `$turret` model group and the main (`weapon0`) gun's configured child parts off the chassis. Models without `$turret` skip the effect safely; geometry baked into `$body` cannot be detached.
