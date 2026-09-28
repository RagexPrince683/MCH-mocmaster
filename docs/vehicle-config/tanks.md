# Tank config values

Tanks inherit all shared keys from `base.md` and add track/weight keys. Tank `speed` is multiplied by global `AllTankSpeed` during validation.

Set the shared `LWR = true` option to enable the existing tank laser warning alert sound. Its default is `false`, so `LWR = false`, an invalid value, or an omitted entry disables that sound. LWR detection does not require flares and does not grant or alter flares, chaff, APS, smoke launchers, or the separate `RWRType` radar warning receiver.

## Tank-only keys

| Key | Type/range | Default | Notes |
|---|---:|---:|---|
| `WeightType` | enum `normal`, `car`, `tank` | `normal` / 0 | Parser maps `car` to 1 and `tank` to 2; any other text is 0. |
| `CivilianCarGrip` | boolean | `false` | Explicit civilian passenger-car grip and steering opt-in, independent of `WeightType` and `Category`. |
| `CivilianCarDrivetrain` | boolean | `false` | Explicit passenger-car engine, automatic gears, longitudinal slip, and separate service/handbrake opt-in. Independent of grip, weight, category and `DriveType`. |
| `CarThrottleResponse` | float[0.05..1], fraction/tick | 0.25 | Pedal and released drive demand approach the target at 20 Hz; does not set body speed. |
| `CarIdleRPM` | float[500..2000], RPM | 800 | Running idle and launch clutch/converter RPM floor. |
| `CarRedlineRPM` | float[3000..12000], RPM | 6500 | Engine ceiling, gear speed ranges, shift thresholds and wheel-speed fuel cut. |
| `CarForwardGears` | integer[1..8] | 5 | Ratio list must have exactly this many descending entries. |
| `CarGearRatios` | comma-separated float[0.2..6], strictly descending | generated | First through top gear. Invalid/count-mismatched lists use geometric gameplay ratios from 3.5 to 0.77; one gear uses 1.0. |
| `CarReverseGearRatio` | float[0.2..6] | 3.2 | Positive reverse ratio; selected direction supplies the sign. |
| `CarFinalDrive` | float[1..8] | 4.0 | Multiplies wheel demand and wheel-to-engine RPM coupling. |
| `CarWheelRadius` | float[0.2..0.6], metres | 0.32 | Nominal driven tire radius for RPM/gearing, independent of grip metadata and visual scale. Unverified radii are gameplay tuning. |
| `CarDriveForce` | float[0.0001..0.02], blocks/tick² per unit overall ratio | 0.0015 | Peak normalized drive demand before gearing, throttle, torque curve, shift reduction and fuel cut. Gameplay tuning, not newtons or measured engine torque. |
| `CarDrag` | float[0.00001..0.1], inverse blocks | 0.0015 | Quadratic horizontal deceleration coefficient; replaces `MotionFactor` in this engine path. |
| `CarLongitudinalGrip` | float[0.005..0.24], blocks/tick² | 0.03 | Full-wheel-set traction budget, apportioned by support after reserving existing lateral grip; does not change steering/suspension. |
| `CarShiftTicks` | integer[1..40], ticks | 8 | Ratio/RPM blend and reduced drive torque during a shift; default 0.4 s. Retains vehicle momentum. |
| `CarServiceBrake` | float[0.001..0.5], blocks/tick² equivalent wheel torque | 0.025 | S demand distributed across all configured wheels; body deceleration is contact/traction limited. |
| `CarHandbrake` | float[0.001..0.5], blocks/tick² equivalent wheel torque | 0.015 | Separate rear-axle brake demand while Space is held, including at rest. |
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

`CivilianCarDrivetrain = true` selects the server engine/transmission and longitudinal wheel state.
All 23 supported passenger definitions explicitly configure these fields in the authoritative
`src/main/resources/assets/mcheli/tanks/` assets. The deprecated `configreference` directory is not
used. Published gearing and gameplay tuning are distinguished in each asset and the
[car inventory](../car-tire-grip.md#bundled-drivetrain-values-and-speed-audit).

Malformed/nonfinite scalar settings use the defaults above; finite values clamp to their ranges
(integer settings truncate). Invalid, non-descending or count-mismatched ratio lists use generated
ratios. Removed fields restore defaults on reload. Omission preserves the legacy engine/brake path
and independent grip, reverse, axle and lamp opt-ins.

W supplies throttle: `throttle += (pedal-throttle)*CarThrottleResponse` each tick. Release decays
remaining drive demand through the same response. S cuts forward drive and service-brakes forward
motion, selecting reverse only after four consecutive ticks below 0.025 blocks/tick longitudinal
and 0.06 blocks/tick horizontal speed (1.8 and 4.32 km/h). W brakes backward motion before selecting
forward. W+S combines throttle and service braking without requesting reverse. Space supplies rear
brake torque while held and keeps both pedals available. Brakes oppose wheel rotation even at rest;
combined pedals cannot brake the body through zero into opposite travel. S and Space still illuminate
opted-in lamps, including S in reverse. Pilot/control loss, GUI entry, fuel loss and shutdown release inputs.

For ratio `G`, final drive `F`, tire radius `r`, and wheel surface speed `v`,
`wheelRPM = abs(v)*1200*G*F/(2*pi*r)`. A gear's nominal redline road speed is
`CarRedlineRPM*2*pi*r/(1200*G*F)`. Each descending ratio therefore supplies less wheel torque and
a higher speed range. `Speed` does not define gear bands or supply thrust. With W and neither brake
held, road RPM upshifts at 88% of redline; it downshifts below 30% only if the preceding gear would
remain below 70%. The box returns to first near rest. Road RPM chooses gears so stationary burnouts
and airborne spin do not race through the transmission.

A shift changes the active gear immediately, blends the previous ratio into the new ratio over
`CarShiftTicks`, and recovers drive torque from 25% toward full. It never writes body velocity.
Engine RPM follows the blended ratio and powered-wheel surface speed, smoothed by 0.3/tick.
A gameplay slipping launch clutch/converter supplies a floor
`idle+(redline-idle)*throttle*0.4`, so RPM responds at a stop against brakes. Wheel spin can raise RPM
independently of road speed.

Forward demand is `CarDriveForce*G*F*throttle*torqueCurve*shiftFactor*fuelCut`. The gameplay curve
rises from 0.7 toward 1.0 at half redline, then falls to 0.75 at redline. Wheel-RPM fuel cut tapers from
full at 95% to zero at 103% of redline. Reverse multiplies the same demand by
`-ThrottleUpDown*ThrottleDownFactor/2.4`; existing bundled factors are preserved. The reverse
wheel-speed governor and absolute `CivilianCarReverseSpeed` ceiling remain separate.

`DriveType` selects powered axles; unset/invalid uses all configured wheels equally without assigning
an identity. Axles receive their configured share of engine/service brake demand; Space adds rear
braking. Existing collision support and lateral-first reservation bound road reaction, scaled to
`CarLongitudinalGrip`. Unsupported wheels spin without propelling/braking the body; missing wheels
retain their configured denominator. Excess torque becomes powered-wheel slip. Combined pedals
can create restrained launches/burnouts where torque exceeds available traction and brake torque;
a strong held rear brake can instead lock powered rear wheels.

For an axle with `n` configured wheels out of `T`, inverse gameplay inertia is `k=4*T/n`.
Drive adds `driveDemand*k` to surface speed; braking opposes rotation without reversing it.
Reaction `(wheelSpeed-bodySpeed)/(k+1)` is traction limited, accelerates the body, and subtracts
`reaction*k` from wheel speed. Unused brake torque holds a stopped wheel against road reaction.
Wheel speed remains bounded to ±8 blocks/tick. This is gameplay inertia, not measured differential,
load-transfer or tire dynamics.

After engine/contact force, horizontal drag removes
`CarDrag*horizontalSpeed^2 + 0.00015` blocks/tick of speed; the rolling term requires wheel support.
Drag cannot reverse motion. Bundled coefficients balance sustained drive near 99.8% of the configured
ceiling on a straight, fully supported level road. This is an equation-based tuning target, not an
observed road result; slip, cornering, gradients and contact loss change it. `Speed` is then the final
horizontal safety cap before movement. Opted-in car definitions accept up to 8 blocks/tick; other
tanks retain their 4-block/tick parser limit, independent of key order. `AllTankSpeed` scales the safety
cap; raising it alone cannot add engine power or raise force/drag terminal speed. Retune force/drag/
gearing for another desired road speed. Other tanks retain `MotionFactor`. Client extrapolation uses
car drag between server updates. Gravity, steering, suspension and collision retain their existing paths.

Three existing tank DataWatcher integers synchronize throttle, RPM, gear, brake states, running state,
axle speeds, slip and contact. Existing sound follows smoothed RPM; each axle animates its wheel
surface speed through `PartWheelRot`. Supported powered spin above 0.12 blocks/tick and normalized
slip 0.35 emits restrained smoke; unsupported spin emits none. Bundled car HUDs show the forward
gear or `R` plus RPM, with RPM-driven tachometers. Non-car uses retain throttle needles.
HUD expressions expose `car_drivetrain`, `car_gear`, `car_rpm`, `car_rpm_norm`; string arguments
`CAR_GEAR` (`%s`) and `CAR_RPM` (`%4.0f`) use synchronized server state.

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
a heightmap, entity collision, or paired `onGround` flag. A wheel whose level anchor puts its collision
bottom below the body floor uses a correction bounded by that wheel's actual collision height. The
same corrected anchor controls its collision position and travel bounds; authored model positions
are unchanged. The probe also lifts within a 0.05-block tracking skin and adds exactly that lift to
the sweep, preserving its lower endpoint and compression reference. Support requires a block collision
surface underneath the wheel footprint. Both current and next-tick horizontal positions use the same
current vertical reference, so pending gravity cannot inflate compression. Only supported wheels
generate spring/damper response; an entirely unsupported car follows normal gravity, falling, body
collision, and crash-damage behavior. The separate grip query retains its own contact reach and client
tracking correction.

Each `AddPartWheel` is matched to the nearest mirrored `SetWheelPos` collision wheel in local X/Z and
moves vertically with its interpolated compression. Its authored `AddPartWheel` Y position is the
neutral rendered position; it is not replaced by the collision wheel's `SetWheelPos` Y coordinate.
The neutral compression is initialized or adjusted only with complete support on one level collision
surface at the body floor, even compression, and settled body height/pitch/roll. Equal compression on
a slope, partial contact, or a car still falling through suspension reach cannot establish the baseline.
The baseline follows settled level compression slowly and snaps the final sub-0.001-block difference to
that compression, returning rendered travel to zero without removing suspension movement on terrain.
Body pitch and roll converge every tick, including while stopped, and settle exactly at zero after
level support is restored. Roll uses supported collision-wheel heights; pitch independently samples
reachable block collision surfaces at yaw-only wheel locations, so wheel extension and the previous
pitch do not determine its terrain selection. A match farther than 0.85 blocks is
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

`WeightType`, `WeightedCenterZ`, `TrackMaxHP`, `AddTrackHitBox`, `EnableTurretPop`, `EnableBrakeLights`, `DriveType`, `CivilianCarReverseSpeed`, and `LWR` are optional. Without the engine opt-in, omitting `DriveType` keeps legacy propulsion and omitting `CivilianCarReverseSpeed` keeps legacy reverse controls/limiting. With the engine opt-in, missing `DriveType` uses axle-neutral wheel propulsion and missing reverse speed leaves the gear/RPM governor plus `Speed` safety cap. Omitting `EnableTurretPop` keeps the turret attached when destroyed. Omitting `EnableBrakeLights` disables the brake-light pass. Omitting `LWR` leaves tank alert audio disabled; other omitted keys retain default ground behavior and no explicit track hitboxes.

`EnableTurretPop = true` enables a catastrophic destruction effect which launches the exact `$turret` model group and the main (`weapon0`) gun's configured child parts off the chassis. Models without `$turret` skip the effect safely; geometry baked into `$body` cannot be detached.
