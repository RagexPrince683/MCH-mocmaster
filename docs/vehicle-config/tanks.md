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
| `WeightedCenterZ` | float[-1000..1000], local blocks | 0 | Fore/aft center of mass relative to the vehicle origin; positive is toward the +Z/front axle. For opted-in civilian cars settling on narrow support, a center beyond the present wheel contact footprint (or body contact if no wheel reaches ground) makes the body tip. |
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

### Legacy military tank controls

Military tracked and wheeled vehicles use the legacy controls unless an explicit civilian option is
authored. W adds forward throttle. S first reduces positive forward throttle by
`0.01 * ThrottleUpDown` per tick, plus `0.02 * ThrottleUpDown` while the synchronized brake
state is active. Once forward throttle is zero, S adds
`0.0025 * ThrottleUpDown * ThrottleDownFactor` to reverse demand when `EnableBack = true`.
Reverse thrust opposes any remaining forward movement, then continues powering backward travel.
Releasing S removes that increment; W drains reverse demand and restores forward throttle.

Reverse demand retains its existing 0.8 decay each tick. The S lamp signal no longer applies the
additional 0.5 brake damping during powered legacy military reverse: S must be held without W,
forward throttle must be zero, and `EnableBack` must be true. Space remains the dedicated brake;
the legacy input handler clears both pedals while Space is held, so it adds no reverse demand and
retains both the 0.8 decay and 0.5 brake damping. S/Space lamp synchronization is unchanged.
The civilian drivetrain, positive `CivilianCarReverseSpeed` exception, and controls explicitly
declared civilian through `CivilianCarGrip` or `Category = C` retain their existing behavior.
`WeightType = Car` does not select civilian controls: military trucks and wheeled armor receive
the legacy fix. External packs need no new option or reverse-factor changes; an unclassified
legacy tank uses the same fix. No civilian engine, gears, grip, or reverse-speed limit is added.

The shared parser clamps `ThrottleUpDown` to `[0, 3]` and `ThrottleDownFactor` to `[0, 10]`.
With sustained S, the legacy demand approaches
`0.0025 * ThrottleUpDown * ThrottleDownFactor / (1 - 0.8)`; it does not grow indefinitely.
Actual movement also depends on ground drag, pitch, collisions, fuel/engine/track state and the
validated `Speed` cap. See the [complete bundled audit](#bundled-tank-definition-audit).

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

For legacy civilian cars, holding S first reduces positive forward throttle by
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
damping are unchanged for civilian cars. Legacy military controls use the exception described above.

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

## Bundled tank definition audit

2026-09-28 — Reverse-control regression audit of all 247 definitions under
`src/main/resources/assets/mcheli/tanks/`. This is a source/configuration audit, not measured
vehicle performance. The authored names, categories, weapons and vehicle roles identify 216
military definitions, 30 declared civilian definitions (`Category = C`, including police car
variants), and one police/security armored vehicle (`bearcat.txt`, `P/NMAV`).

Tables show effective parser values with the default `AllTankSpeed = 1`; other global values
multiply `Speed`. `*` marks an omitted field using its default. Parentheses show an authored
value clamped by the parser. Later active keys win; commented keys are inactive. `WeightType`
is the effective enum: unrecognized values, including `Unknown` and `Plane`, become `normal`.
A civilian engine uses `CarDrag` rather than the displayed legacy `MotionFactor`.

### Military definitions (216)

All 216 omit `CivilianCarDrivetrain`, `CivilianCarGrip`, `CivilianCarReverseSpeed`, `DriveType`
and `EnableBrakeLights`; none needs a civilian option to reverse. 213 enable reverse; 212 of
those have positive `Speed`. All allow ground movement by default except `jtac.txt`, which
explicitly sets `CanMoveOnGround = false`. Track and wheel rendering entries do not change
the reverse-demand equation. Fuel availability, engine shutdown, destroyed tracks and gunner
mode still gate powered movement in the existing control path.

| Definition | Category | WeightType | EnableBack | ThrottleUpDown | ThrottleDownFactor | Speed | MotionFactor | PivotTurnThrottle |
|---|---|---|---|---:|---:|---:|---:|---:|
| `2k22.txt` | SPAAG | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0* |
| `2s1-gvozdika.txt` | SPH | tank | true | 1 | 0.4 | 0.37 | 0.9 | 0 |
| `2s19.txt` | SPH | tank | true | 2 | 0.4 | 0.37 | 0.9 | 0* |
| `2s25 sprut_sd.txt` | SPALT/ATD | tank | true | 2 | 0.4 | 0.43 | 0.925 | 0 |
| `2s3.txt` | SPH | tank | true | 2 | 0.4 | 0.39 | 0.9 | 0* |
| `2s9.txt` | SPM | tank | true | 2 | 0.45 | 0.37 | 0.9 | 0* |
| `59d.txt` | MBT | tank | true | 3 | 0.4 | 0.31 | 0.9 | 0 |
| `82ccv.txt` | CCV | tank | true | 2 | 0.4 | 0.65 | 0.9 | 0.2 |
| `87rcv.txt` | RCV | tank | true | 2 | 0.5 | 0.62 | 0.9 | 0.2 |
| `90tk.txt` | MBT | tank | true | 2 | 0.4 | 0.43 | 0.9 | 0 |
| `96wapc.txt` | APC | tank | true | 2 | 0.4 | 0.62 | 0.9 | 0.2 |
| `99a2.txt` | MBT | tank | true | 3 (10) | 0.4 | 0.47 | 0.9 | 0 |
| `a-27mcromwell.txt` | MED | tank | true | 2 | 0.4 | 0.38 | 0.9 | 0* |
| `aav7.txt` | AAPC | tank | true | 2 | 0.4 | 0.45 | 0.9 | 0* |
| `ags-17.txt` | AGL | normal | true | 1 | 0.4 | 0.001 | 0.96* | 0 |
| `amx-30b.txt` | MBT | tank | true | 3 | 0.4 | 0.4 | 0.925 | 0 |
| `amx13.txt` | LT | tank | true | 2 | 0.4 | 0.37 | 0.95 | 0* |
| `amx56.txt` | MBT | tank | true | 3 (3.5) | 0.4 | 0.44 | 0.9 | 0 |
| `ariete.txt` | MBT | tank | true | 2.5 | 0.4 | 0.4 | 0.9 | 0 |
| `armata.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.5 | 0.925 | 0 |
| `aslav-c.txt` | RCV | tank | true | 2 | 0.5 | 0.62 | 0.96* | 0.1 |
| `b2a5.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.6 | 0.9 | 0 |
| `b2a6.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.6 | 0.9 | 0 |
| `b2a7.txt` | MBT | tank | true | 3 | 0.4 | 0.45 | 0.9 | 0 |
| `bal.txt` | TEL/TMT | car | true | 2 | 0.4 | 0.8 | 0.9 | 0.2 |
| `bm21.txt` | MLRS | car | true | 0.81 | 0.4 | 0.47 | 0.97 | 0.5 |
| `bmp-t.txt` | FSCV | tank | true | 2 | 0.4 | 0.37 | 0.9 | 0* |
| `bmp1.txt` | AIFV/AAPC | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0* |
| `bmp2.txt` | AIFV/AAPC | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0* |
| `bmp3.txt` | AIFV/AAPC | tank | true | 3 (5) | 0.4 | 0.45 | 0.9 | 0* |
| `bradley.txt` | RIFV/ARV | tank | true | 2 | 0.4 | 0.41 | 0.9 | 0 |
| `bradleym2.txt` | ARIFV/AARV | tank | true | 2 | 0.4 | 0.41 | 0.9 | 0 |
| `brdm-s.txt` | AASC | tank | true | 2 | 0.5 | 0.62 | 0.96* | 0.1 |
| `brdm2.txt` | AASC | tank | true | 2 | 0.5 | 0.62 | 0.9 | 0.1 |
| `bt-2.txt` | LCT | tank | true | 1.6 | 0.4 | 0.62 | 0.92 | 0 |
| `bt-5.txt` | LCT | tank | true | 1.6 | 0.4 | 0.45 | 0.92 | 0 |
| `btr3.txt` | APC | tank | true | 2 | 0.5 | 0.53 | 0.9 | 0.2 |
| `btr4.txt` | IFV | tank | true | 2 | 0.5 | 0.68 | 0.9 | 0.1 |
| `btr80.txt` | APC | tank | true | 2 | 0.5 | 0.55 | 0.9 | 0.2 |
| `btr82.txt` | APC | tank | true | 2 | 0.5 | 0.62 | 0.9 | 0.2 |
| `btr90.txt` | APC | tank | true | 2 | 0.5 | 0.62 | 0.9 | 0.2 |
| `c1.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.35 | 0.9 | 0 |
| `c2.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.37 | 0.9 | 0 |
| `centauro.txt` | WTD | tank | true | 2 | 0.5 | 0.67 | 0.9 | 0.2 |
| `centauro120.txt` | WTD | tank | true | 2 | 0.5 | 0.67 | 0.9 | 0.2 |
| `centurion3.txt` | MED | tank | true | 1 | 0.4 | 0.22 | 0.95 | 0 |
| `chi-he.txt` | MED | tank | true | 1.8 | 0.6 | 0.27 | 0.92 | 0 |
| `chi-nu.txt` | MED | tank | true | 1.8 | 0.6 | 0.24 | 0.92 | 0 |
| `czech.txt` | LT | tank | true | 1 | 0.4 | 0.28 | 0.9 | 0 |
| `davycrockett.txt` | NRG | normal* | true | 3 (9.9) | 0.4 | 0.01 | 0.96* | 0* |
| `df41.txt` | SST/TEL | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0.2 |
| `dra_vdv.txt` | LUV | car | true | 0.95 | 0.4 | 0.7 | 0.96* | 0.1 |
| `elbrus.txt` | TEL | car | true | 2 | 0.4 | 0.37 | 0.9 | 0.1 |
| `fh77.txt` | H | normal | true | 0.81 | 0.4 | 0.037 | 0.96* | 0 |
| `flarakrad.txt` | SPAAMS | car | true | 0.81 | 0.45 | 0.62 | 0.925 | 0.1 |
| `g250-wolf.txt` | LUV | car | true | 0.81 | 0.4 | 0.85 | 0.96* | 0.1 |
| `gaz66zu23.txt` | LUV | car | true | 0.81 | 0.4 | 0.56 | 0.96* | 0.1 |
| `gepard.txt` | SPAAG | tank | true | 2 | 0.4 | 0.4 | 0.95 | 0 |
| `gepard2.txt` | SPAAG | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0 |
| `growler.txt` | LUV | car | true | 0.81 | 0.4 | 0.85 | 0.9 | 0.1 |
| `hel.txt` | IMV | car | true | 0.81 | 0.4 | 0.6 | 0.96* | 0.1 |
| `hel2.txt` | IMV | car | true | 0.81 | 0.4 | 0.6 | 0.96* | 0.1 |
| `hemtt.txt` | HTT | car | true | 2 | 0.45 | 0.62 | 0.9 | 0.2 |
| `hetzer.txt` | TD | tank | true | 1 | 0.6 | 0.26 | 0.94 | 0 |
| `humvee_mk19.txt` | IMV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `humvee_tow.txt` | IMV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `humveewithm240.txt` | IMV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `humveewithweapon.txt` | IMV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `humveewithweaponrws.txt` | IMV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `ikv91.txt` | ATAG/AHMAG/ALT | tank | true | 2 | 0.6 | 0.4 | 0.95 | 0 |
| `iltis-kdow.txt` | LUV | car | true | 0.82 | 0.4 | 0.758 | 0.96* | 0.1 |
| `imshorad.txt` | SPAA/MSRAD/MADS | tank | true | 2 | 0.5 | 0.62 | 0.9 | 0.2 |
| `is-1.txt` | HT | tank | true | 1.3 | 0.4 | 0.23 | 0.9 | 0 |
| `is-2.txt` | HT | tank | true | 1.3 | 0.4 | 0.23 | 0.9 | 0 |
| `jagdpanther.txt` | TD | tank | true | 1 | 0.6 | 0.29 | 0.94 | 0 |
| `jgsdf-mcv.txt` | WTD/AAC/AAG/ATD/AWAFV | tank | true | 2 | 0.5 | 0.62 | 0.96* | 0.4 |
| `jtac.txt` | JTAC | normal* | false* | 1* | 0.4 | 0 | 0.96* | 0* |
| `jtiger.txt` | TD | tank | true | 1 | 0.6 | 0.21 | 0.9 | 0* |
| `kamaz.txt` | HUT/GUT | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `ktorosomak.txt` | AAPC | tank | true | 3 (5) | 0.4 | 0.62 | 0.925 | 0.1 |
| `kub.txt` | SAM/SPAA | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0* |
| `kurganets25.txt` | AIFV/AAPC | tank | true | 3 (5) | 0.4 | 0.55 | 0.9 | 0 |
| `kv-1.txt` | HT | tank | true | 1.3 | 0.6 | 0.23 | 0.9 | 0* |
| `kv-2.txt` | HT/AG | tank | true | 1 | 0.6 | 0.17 | 0.9 | 0* |
| `lav25.txt` | AARV/AIFV | tank | true | 3 | 0.5 | 0.62 | 0.925 | 0.1 |
| `lav3.txt` | IFV | tank | true | 3 | 0.5 | 0.62 | 0.925 | 0.1 |
| `leopard1.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.4 | 0.95 | 0 |
| `leopard1a1.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.4 | 0.95 | 0 |
| `leopard2a4.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.7 | 0.9 | 0 |
| `m_41_90.txt` | LT | tank | true | 2 | 0.6 | 0.6 | 0.95 | 0 |
| `m101.txt` | TFG/H | normal | true | 0.81 | 0.4 | 0.01 | 0.03 | 0* |
| `m106.txt` | MCV | tank | true | 2 | 0.5 | 0.42 | 0.92 | 0 |
| `m109.txt` | SPH/SPA | tank | true | 3 (5) | 0.4 | 0.38 | 0.9 | 0* |
| `m109105.txt` | SPH/SPA | tank | true | 3 (5) | 0.4 | 0.38 | 0.9 | 0* |
| `m1128.txt` | WTD | tank | true | 2 | 0.5 | 0.6 | 0.9 | 0.2 |
| `m1129.txt` | MCV | tank | true | 2 | 0.45 | 0.62 | 0.9 | 0.2 |
| `m113.txt` | APC | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m113m2.txt` | APC/ACAV | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m113mk19.txt` | APC/ACAV | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m113unarmed.txt` | APC | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m132.txt` | APC | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m142.txt` | MLRS | car | true | 2 | 0.4 | 0.53 | 0.9 | 0.1 |
| `m142atacms.txt` | TBM | car | true | 2 | 0.4 | 0.53 | 0.9 | 0.1 |
| `m150.txt` | APC/ATM | tank | true | 2 | 0.4 | 0.42 | 0.92 | 0 |
| `m151_m2.txt` | LUV | car | true | 0.81 | 0.4 | 0.63 | 0.96* | 0.1 |
| `m151.txt` | LUV | car | true | 0.81 | 0.4 | 0.63 | 0.96* | 0.1 |
| `m151a1c.txt` | LUV | car | true | 0.81 | 0.4 | 0.63 | 0.96* | 0.1 |
| `m151a2.txt` | LUV | car | true | 0.81 | 0.4 | 0.63 | 0.96* | 0.1 |
| `m163.txt` | SPAAG | tank | true | 2 | 0.4 | 0.4 | 0.92 | 0 |
| `m18.txt` | LTD/LT | tank | true | 1.2 | 0.4 | 0.55 | 0.94 | 0* |
| `m1a1_cev.txt` | MEV/ABV | tank | true | 3 (5) | 0.4 | 0.45 | 0.95 | 0 |
| `m1a1.txt` | MBT | tank | true | 3 (4.5) | 0.4 | 0.43 | 0.92 | 0 |
| `m1a1nt.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.45 | 0.95 | 0 |
| `m1a2.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.42 | 0.92 | 0 |
| `m1a2tusk.txt` | MBT | tank | true | 3 (4) | 0.4 | 0.41 | 0.9 | 0 |
| `m1abrams.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.45 | 0.95 | 0 |
| `m2.txt` | HMG | normal | true | 3 (9.9) | 0.4 | 0.005 | 0.96* | 0 |
| `m26.txt` | HT/MED | tank | true | 1 | 0.4 | 0.3 | 0.95 | 0 |
| `m3_stuart.txt` | LT | tank | true | 1.5 | 0.6 | 0.36 | 0.95 | 0 |
| `m4_calliope.txt` | MED | tank | true | 2 | 0.6 | 0.24 | 0.9 | 0* |
| `m4.txt` | MED | tank | true | 2 | 0.6 | 0.24 | 0.9 | 0* |
| `m40.txt` | SPH/SPA | tank | true | 0.7 | 0.45 | 0.24 | 0.9 | 0* |
| `m4105.txt` | MED | tank | true | 2 | 0.6 | 0.24 | 0.9 | 0* |
| `m42.txt` | SPAAG | tank | true | 2 | 0.6 | 0.45 | 0.95 | 0 |
| `m46.txt` | MED | tank | true | 1 | 0.6 | 0.3 | 0.95 | 0 |
| `m47.txt` | MED | tank | true | 1 | 0.6 | 0.3 | 0.95 | 0 |
| `m48.txt` | MBT/MED | tank | true | 1.5 | 0.6 | 0.3 | 0.95 | 0 |
| `m4a1.txt` | MED | tank | true | 2 | 0.6 | 0.24 | 0.9 | 0* |
| `m4a176.txt` | MED | tank | true | 2 | 0.6 | 0.24 | 0.9 | 0* |
| `m4a3e2.txt` | HT | tank | true | 2 | 0.6 | 0.22 | 0.9 | 0* |
| `m4a3e8.txt` | HT | tank | true | 2 | 0.6 | 0.22 | 0.9 | 0* |
| `m4firefly.txt` | MED | tank | true | 2 | 0.6 | 0.25 | 0.9 | 0* |
| `m4zip.txt` | FT | tank | true | 2 | 0.6 | 0.22 | 0.9 | 0* |
| `m50.txt` | TD | normal* | true | 1 | 0.4 | 0.3 | 0.96* | 0* |
| `m60a1.txt` | MBT | tank | true | 1.5 | 0.4 | 0.45 | 0.95 | 0 |
| `m60pat.txt` | MBT | tank | true | 1.5 | 0.4 | 0.45 | 0.95 | 0 |
| `m67.txt` | MFT/MED/FT | tank | true | 1.5 | 0.4 | 0.3 | 0.95 | 0 |
| `m7.txt` | SPG/SPH/SPA | tank | true | 1 | 0.4 | 0.24 | 0.9 | 0 |
| `m777.txt` | TH/H | normal | true | 1 | 0.4 | 0 | 0.96* | 0 |
| `m8.txt` | LT/AC | tank | true | 1.2 | 0.4 | 0.55 | 0.9 | 0.1 |
| `magach5.txt` | MBT/MED | tank | true | 1.5 | 0.4 | 0.3 | 0.95 | 0 |
| `marder.txt` | IFV | tank | true | 3 (10) | 0.4 | 0.4 | 0.9 | 0* |
| `merkava_mk1.txt` | MBT | tank | true | 3 (10) | 0.4 | 0.4 | 0.92 | 0 |
| `merkava_mk4.txt` | MBT | tank | true | 3 (10) | 0.4 | 0.4 | 0.92 | 0 |
| `merkava_mk4b.txt` | MBT | tank | true | 3 (10) | 0.4 | 0.4 | 0.92 | 0 |
| `mlrs.txt` | MLRS | tank | true | 2 | 0.4 | 0.4 | 0.9 | 0* |
| `mstab.txt` | H | normal | true | 0.81 | 0.4 | 0.01 | 0.1 | 0 |
| `mt-12_rapira.txt` | ATG | tank | true | 0.81 | 0.4 | 0.01 | 0.96* | 0 |
| `mxtmv.txt` | IMV | car | true | 0.81 | 0.4 | 0.7 | 0.96* | 0.1 |
| `mxtmv50.txt` | IMV | car | true | 0.81 | 0.4 | 0.7 | 0.96* | 0.1 |
| `mxtmvbase.txt` | IMV | car | true | 0.81 | 0.4 | 0.7 | 0.96* | 0.1 |
| `mxtmvm240.txt` | IMV | car | true | 0.81 | 0.4 | 0.7 | 0.96* | 0.1 |
| `mxtmvrws.txt` | IMV | car | true | 0.81 | 0.4 | 0.7 | 0.96* | 0.1 |
| `namer.txt` | HAPC | tank | true | 3 (10) | 0.4 | 0.5 | 0.92 | 0 |
| `p-s1.txt` | SPAAWS | car | true | 0.81 | 0.4 | 0.43 | 0.9 | 0.2 |
| `p4-pc.txt` | LUV | car | true | 0.81 | 0.4 | 0.73 | 0.96* | 0.1 |
| `p40.txt` | HT/MED | tank | true | 1 | 0.4 | 0.25 | 0.9 | 0* |
| `panther.txt` | MED | tank | true | 1 | 0.6 | 0.285 | 0.92 | 0 |
| `panzerwerfer.txt` | HTRACMLRS | car | true | 2 | 0.4 | 0.24 | 0.95 | 0.5 |
| `patriot_t.txt` | MSAM | tank | false* | 1* | 0.4 | 0 | 0.96* | 0.2 |
| `pgz-95.txt` | SPAAG | tank | true | 2 | 0.4 | 0.33 | 0.9 | 0 |
| `phz89.txt` | MLRS/SPMRL | tank | true | 2 | 0.4 | 0.34 | 0.9 | 0 |
| `pion.txt` | SPH/SPA | tank | true | 1 | 0.4 | 0.31 | 0.9 | 0* |
| `plz45.txt` | SPH/SPA | tank | true | 2 | 0.4 | 0.34 | 0.95 | 0 |
| `puma.txt` | IFV | tank | true | 2.2 | 0.4 | 0.43 | 0.9 | 0 |
| `pumabase.txt` | IFV | tank | true | 2.2 | 0.4 | 0.43 | 0.9 | 0 |
| `rok_k2.txt` | MBT | tank | true | 3 (17) | 0.6 | 0.43 | 0.9 | 0 |
| `rozvidka.txt` | UGV | normal | true | 1 | 0.4 | 0.3 | 0.93 | 0* |
| `s500.txt` | MSAM | car | true | 2 | 0.4 | 0.43 | 0.9 | 0.2 |
| `sa8.txt` | ASAM | car | true | 2 | 0.4 | 0.5 | 0.96* | 0.2 |
| `sdkfz234_2.txt` | LT/AC | tank | true | 1.2 | 0.4 | 0.56 | 0.95 | 0.15 |
| `shkhondava.txt` | SPH/SPG/SPA | tank | true | 1.5 | 0.4 | 0.5 | 0.95 | 0.1 |
| `spg-9.txt` | ATG | normal* | true | 3 (9.9) | 0.4 | 0.01 | 0.96* | 0* |
| `strf 9040b.txt` | IFV | tank | true | 2 | 0.4 | 0.43 | 0.9 | 0* |
| `strv103.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.37 | 0.95 | 0* |
| `strykericv.txt` | APC/ICV | tank | true | 2 | 0.45 | 0.62 | 0.9 | 0.2 |
| `t-34-85.txt` | MED | tank | true | 1.2 | 0.4 | 0.34 | 0.9 | 0* |
| `t-72b.txt` | MBT | tank | true | 1.4 | 0.4 | 0.37 | 0.95 | 0 |
| `t-80u.txt` | MBT | tank | true | 1.4 | 0.4 | 0.5 | 0.95 | 0* |
| `t-90.txt` | MBT | tank | true | 3 | 0.4 | 0.37 | 0.95 | 0* |
| `t-90a.txt` | MBT | tank | true | 3 | 0.4 | 0.37 | 0.95 | 0* |
| `t-90s.txt` | MBT | tank | true | 3 | 0.4 | 0.37 | 0.95 | 0* |
| `t26.txt` | LT | tank | true | 0.8 | 0.4 | 0.193 | 0.95 | 0 |
| `t55.txt` | MED | tank | true | 1 | 0.4 | 0.32 | 0.9 | 0 |
| `t62.txt` | MED | tank | true | 1.4 | 0.4 | 0.31 | 0.9 | 0* |
| `t64b.txt` | MBT | tank | true | 1.2 | 0.4 | 0.43 | 0.9 | 0 |
| `t64bm.txt` | MBT | tank | true | 1.2 | 0.4 | 0.43 | 0.9 | 0 |
| `t64bv.txt` | MBT | tank | true | 1.2 | 0.4 | 0.43 | 0.9 | 0 |
| `t64u.txt` | MBT | tank | true | 1.2 | 0.4 | 0.43 | 0.9 | 0 |
| `t72b3real.txt` | MBT | tank | true | 1.2 | 0.4 | 0.37 | 0.95 | 0* |
| `t84.txt` | MBT | tank | true | 2 | 0.4 | 0.43 | 0.95 | 0 |
| `tiger1.txt` | HT | tank | true | 1.5 | 0.6 | 0.28 | 0.9 | 0 |
| `tiger2.txt` | HT | tank | true | 1 | 0.6 | 0.26 | 0.9 | 0 |
| `tigr-a.txt` | IMV | car | true | 0.81 | 0.4 | 0.87 | 0.93 | 0.1 |
| `tigr-k.txt` | IMV | car | true | 0.81 | 0.4 | 0.87 | 0.93 | 0.1 |
| `tigr-m.txt` | IMV | car | true | 0.81 | 0.4 | 0.87 | 0.93 | 0.1 |
| `tigr.txt` | IMV | car | true | 0.81 | 0.4 | 0.87 | 0.93 | 0.1 |
| `tochkau.txt` | TEL/TBM | tank | true | 0.81 | 0.45 | 0.37 | 0.95 | 0.1 |
| `tos.txt` | MLRS | tank | true | 3 | 0.4 | 0.37 | 0.9 | 0* |
| `toyota_dshk.txt` | TFV | car | true | 0.81 | 0.45 | 0.81 | 0.96* | 0.1 |
| `toyota_nurs.txt` | TFV | car | true | 0.81 | 0.45 | 0.81 | 0.96* | 0.1 |
| `toyota_ub32.txt` | TFV | car | true | 0.81 | 0.45 | 0.81 | 0.96* | 0.1 |
| `type10.txt` | MBT | tank | true | 3 (5) | 0.4 | 0.43 | 0.95 | 0 |
| `type62.txt` | LT | tank | true | 1 | 0.4 | 0.37 | 0.9 | 0 |
| `type74.txt` | MBT | tank | true | 1.2 | 0.4 | 0.33 | 0.95 | 0 |
| `type89.txt` | IFV | tank | true | 2 | 0.4 | 0.43 | 0.95 | 0 |
| `type95_lt.txt` | LT | tank | true | 1 | 0.4 | 0.28 | 0.9 | 0* |
| `type97_new.txt` | MED | tank | true | 1.1 | 0.4 | 0.24 | 0.95 | 0* |
| `uaz_kpvt.txt` | LUV | car | true | 0.81 | 0.45 | 0.6 | 0.96* | 0.1 |
| `uaz-469k-m-2.txt` | LUV | car | true | 0.81 | 0.45 | 0.62 | 0.96* | 0.1 |
| `uragan.txt` | MLRS | tank | true | 2 | 0.4 | 0.4 | 0.96* | 0.2 |
| `ural4320.txt` | GUT | car | true | 0.81 | 0.45 | 0.51 | 0.96* | 0.1 |
| `vn4.txt` | IMV | car | true | 0.81 | 0.4 | 0.71 | 0.9 | 0.2 |
| `zsu23.txt` | SPAAG | tank | true | 1 | 0.4 | 0.31 | 0.96* | 0* |
| `ztz-96a.txt` | MBT | tank | true | 1 | 0.4 | 0.4 | 0.95 | 0 |
| `zu23.txt` | AAAC | normal* | false* | 0.81 | 0.4 | 0 | 0.96* | 0.1 |

### Outliers and authored exceptions

- Stationary definitions: `jtac.txt`, `patriot_t.txt` and `zu23.txt` omit `EnableBack`
  (effective false) and have `Speed = 0`; `jtac.txt` also forbids ground movement.
  `m777.txt` enables reverse but has `Speed = 0`, so its movement remains zero.
  These are consistent with a designator, trailer and stationary/towed guns, not evidence of
  defective reverse tuning.
- Creep-speed emplacements/towed weapons: `ags-17.txt` (0.001), `m2.txt` (0.005),
  `davycrockett.txt`, `spg-9.txt`, `mt-12_rapira.txt`, `m101.txt` and `mstab.txt`
  (0.01), and `fh77.txt` (0.037). Preserve these authored caps.
  `m101.txt` has `MotionFactor = 0.03` and `mstab.txt` effectively uses 0.1
  (overriding an earlier 0.03); both intentionally-looking high-drag definitions still creep.
  No configuration defect was established; their intended towing/repositioning feel needs in-game
  confirmation, not a blanket reverse-factor increase.
- The following authored `ThrottleUpDown` values exceed 3 and all effectively use 3:
  `99a2.txt`, `amx56.txt`, `armata.txt`, `b2a5.txt`, `b2a6.txt`, `bmp3.txt`, `c1.txt`, `c2.txt`, `davycrockett.txt`, `ktorosomak.txt`, `kurganets25.txt`, `leopard1.txt`, `leopard1a1.txt`, `leopard2a4.txt`, `m109.txt`, `m109105.txt`, `m1a1_cev.txt`, `m1a1.txt`, `m1a1nt.txt`, `m1a2.txt`, `m1a2tusk.txt`, `m1abrams.txt`, `m2.txt`, `marder.txt`, `merkava_mk1.txt`, `merkava_mk4.txt`, `merkava_mk4b.txt`, `namer.txt`, `rok_k2.txt`, `spg-9.txt`, `strv103.txt`, `type10.txt`.
  Their exact authored values appear in parentheses above. This is existing parser behavior,
  not a new clamp or a reason to rewrite pack tuning.
- Default `MotionFactor = 0.96` applies to: `ags-17.txt`, `aslav-c.txt`, `brdm-s.txt`, `davycrockett.txt`, `dra_vdv.txt`, `fh77.txt`, `g250-wolf.txt`, `gaz66zu23.txt`, `hel.txt`, `hel2.txt`, `humvee_mk19.txt`, `humvee_tow.txt`, `humveewithm240.txt`, `humveewithweapon.txt`, `humveewithweaponrws.txt`, `iltis-kdow.txt`, `jgsdf-mcv.txt`, `jtac.txt`, `kamaz.txt`, `m151_m2.txt`, `m151.txt`, `m151a1c.txt`, `m151a2.txt`, `m2.txt`, `m50.txt`, `m777.txt`, `mt-12_rapira.txt`, `mxtmv.txt`, `mxtmv50.txt`, `mxtmvbase.txt`, `mxtmvm240.txt`, `mxtmvrws.txt`, `p4-pc.txt`, `patriot_t.txt`, `sa8.txt`, `spg-9.txt`, `toyota_dshk.txt`, `toyota_nurs.txt`, `toyota_ub32.txt`, `uaz_kpvt.txt`, `uaz-469k-m-2.txt`, `uragan.txt`, `ural4320.txt`, `zsu23.txt`, `zu23.txt`.
  The higher retention explains why otherwise similar military wheel vehicles can reverse faster
  than definitions using 0.9. It is not evidence of a missing reverse option.
- Last-key differences relevant to this audit: `centurion3.txt`, `m26.txt`, `m46.txt`
  and `m47.txt` end at `MotionFactor = 0.95`; `gepard2.txt` and `type62.txt` end at 0.9.
  `mt-12_rapira.txt` ends at `ThrottleUpDown = 0.81`, overriding 3.
  `m777.txt` ends at `WeightType = Unknown`, overriding Tank.
  Identical duplicate movement keys do not change the values.
- `m40.txt` has the lowest throttle response among mobile military vehicles (0.7);
  `t26.txt` uses 0.8. Neither loses its deliberately lower acceleration.
  `rok_k2.txt` authors 17 but uses 3, with a 0.6 reverse factor.
  `strv103.txt` and `ztz-96a.txt` are tracked tank designs despite omitting
  `AddCrawlerTrack`; absence of that visual helper does not select civilian driving.
  `rozvidka.txt` authors `WeightType = Plane` (effective normal) and remains a legacy UGV.
- All military reverse factors are positive (0.4, 0.45, 0.5 or 0.6). Military `WeightType = Car`
  definitions include light vehicles, trucks, technicals and missile carriers; they use the same
  legacy reverse correction. No bundled definition was changed.

### Police/security armored vehicle

`bearcat.txt` is a police/security vehicle rather than a civilian passenger car or a military
tank. It has no civilian control opt-ins, so its legacy reverse receives the same correction.

| Definition | Category | WeightType | EnableBack | ThrottleUpDown | ThrottleDownFactor | Speed | MotionFactor | PivotTurnThrottle |
|---|---|---|---|---:|---:|---:|---:|---:|
| `bearcat.txt` | P/NMAV | car | true | 0.81 | 0.4 | 0.75 | 0.93 | 0.1 |

### Civilian definitions (30; behavior preserved)

All 30 retain their existing control behavior. 23 explicitly enable `CivilianCarDrivetrain`.
The two additional positive-reverse-speed cars, `bnr32_police.txt` and `fordpolice.txt`, retain
their legacy civilian reverse exception (the Ford also opts into civilian grip).
The remaining five, `mc_atv_normal.txt`, `mc_bicycle.txt`, `mc-tractor.txt`,
`opel_blitz_fuel.txt` and `toyota_unarmed.txt`, have no civilian reverse-speed or drivetrain
opt-in; their declared civilian category preserves their existing reverse damping.
No civilian acceleration, gearing, reverse ceiling, service brake, handbrake or lamp configuration
was changed.

| Definition | Category | WeightType | EnableBack | ThrottleUpDown | ThrottleDownFactor | Speed | MotionFactor | PivotTurnThrottle |
|---|---|---|---|---:|---:|---:|---:|---:|
| `2102.txt` | C | car | true | 0.81 | 1.6 | 0.85 | 0.96* | 0.1 |
| `2105.txt` | C | car | true | 0.81 | 1.6 | 0.92 | 0.96* | 0.1 |
| `350z.txt` | C | car | true | 0.83 | 4.2 | 1.74 | 0.96* | 0.1 |
| `ae86.txt` | C | car | true | 0.815 | 2.4 | 1.2 | 0.96* | 0.2 |
| `altis.txt` | C | car | true | 0.81 | 2 | 1.17 | 0.96* | 0.2 |
| `bcnr33.txt` | C | car | true | 0.95 | 2.4 | 1.55 | 0.96* | 0.1 |
| `bnr32_police.txt` | C | car | true | 0.918 | 2.4 | 1.56 | 0.96* | 0.1 |
| `bnr32.txt` | C | car | true | 0.92 | 2.4 | 1.56 | 0.96* | 0.1 |
| `bnr34.txt` | C | car | true | 0.93 | 2.6 | 1.74 | 0.96* | 0.1 |
| `bugattichiron.txt` | C | car | true | 1.2 | 2 | 5.833333 | 0.96* | 1 (500.1) |
| `carrera_gt.txt` | C | car | true | 0.98 | 4.6 | 4.583333 | 0.96* | 0.1 |
| `challenger.txt` | C | car | true | 0.91 | 4 | 1.55 | 0.96* | 0.1 |
| `dacia.txt` | C | car | true | 0.81 | 2 | 1.09 | 0.96* | 0.1 |
| `delorean.txt` | C | car | true | 0.815 | 2.4 | 1.3 | 0.96* | 0.1 |
| `fordpolice.txt` | C | car | true | 0.81 | 2 | 1.55 | 0.96* | 0.1 |
| `fresh_auto.txt` | C | car | true | 0.9 | 1.5 | 0.95 | 0.96* | 0.1 |
| `impreza.txt` | C | car | true | 0.925 | 2.2 | 1.44 | 0.96* | 0.1 |
| `mc_atv_normal.txt` | C | car | true | 0.9 | 0.4 | 0.9 | 0.91 | 0.03 |
| `mc_bicycle.txt` | C | normal | true | 0.32 | 0.4 | 0.3 | 0.9 | 0.1 |
| `mc-tractor.txt` | C | normal* | true | 1.5 | 0.4 | 0.25 | 0.9 | 0.1 |
| `opel_blitz_fuel.txt` | C | car | true | 0.81 | 0.45 | 0.55 | 0.96* | 0.1 |
| `phantom.txt` | C | car | true | 0.9 | 2 | 1.55 | 0.96* | 0.1 |
| `phantomarmored.txt` | C | car | true | 0.89 | 1.6 | 1.55 | 0.96* | 0.1 |
| `rx-8.txt` | C | car | true | 0.86 | 4.2 | 1.47 | 0.96* | 1 |
| `rx7.txt` | C | car | true | 0.82 | 2.6 | 1.43 | 0.96* | 0.1 |
| `s15.txt` | C | car | true | 0.87 | 2.6 | 1.55 | 0.96* | 0.1 |
| `silvia_s14.txt` | C | car | true | 0.84 | 2.6 | 1.76 | 0.96* | 1 |
| `starion.txt` | C | car | true | 0.825 | 2.4 | 1.47 | 0.96* | 0.6 |
| `toyota_unarmed.txt` | C | car | true | 0.81 | 0.45 | 0.81 | 0.96* | 0.1 |
| `w123.txt` | C | car | true | 0.81 | 2 | 1.916667 | 0.96* | 0.1 |

### Confirmed cause and validation

Commit [328847d8](https://github.com/RagexPrince683/MCH-mocmaster/commit/328847d868eba0bbf4ed21fb7adfa3996df310ab),
compared with parent `fa6d2467`, first changed the packet handler from
`setBrake(pc.useBrake)` to `setBrake(pc.useBrake || pc.throttleDown)`.
The legacy physical brake path already multiplied reverse demand by 0.8 and, while that flag was
set, by another 0.5. Thus S began damping military reverse demand as a dedicated brake.
Commit [16346a39](https://github.com/RagexPrince683/MCH-mocmaster/commit/16346a3992552a1d4f6550e2c3a68b4c2aecbe7f),
compared with parent `602ed9b6`, exempted positive `CivilianCarReverseSpeed` controls and added
civilian demand/speed bounds. Its military branch retained the regression; neither commit
retuned military definitions.

Default-key trace: S (`KeyDown = 31`) supplies `throttleDown`; Space
(`KeySwitchHovering = 57`, used by `KeyBrake`) supplies `useBrake` and clears both pedals
for legacy tanks. The packet base carries those separate bits (2 and 8). The network wrapper
queues handling on the server thread; the tank handler applies pilot controls and synchronizes
status bit 11 for lamps/braking. Legacy control builds `throttleBack`; server movement subtracts
that demand along the heading, applies the existing horizontal `Speed` clamp, then
`MotionFactor` and collision movement. The opted-in civilian drivetrain returns before the
changed legacy control block.

For `m1a2.txt`, effective `ThrottleUpDown = 3`, `ThrottleDownFactor = 0.4`,
`Speed = 0.42`, `MotionFactor = 0.92` and `EnableBack = true` produce a reverse increment
of 0.003 per tick. Under the regression, sustained demand approached `0.003 / (1 - 0.4) = 0.005`;
the correction restores `0.003 / (1 - 0.8) = 0.015`.
A level-ground calculation using the existing -10-degree thrust pitch predicts approximately
4.08 versus 12.23 km/h after settling. These are equation results, not observed driving speeds.

In-memory control/thrust/clamp/drag checks covered all 212 mobile military definitions:
S slows forward travel then sustains backward power, Space supplies no reverse increment and
reduces existing demand/speed, and releasing S for W restores forward control. Representative
cases included M1A2, T-26, K2, ZSU-23-4, BTR-82, Type 16 MCV, armed Humvee, M40 and the high-drag
M101/Msta-B exceptions. Both the default `AutoThrottleDownTank = false` and optional true setting
passed (424 transition runs), including Space while moving forward and simultaneous S+Space.
Comparisons across 1,792 legacy civilian input/throttle/demand combinations were unchanged;
53,840 legacy non-reverse control combinations were also unchanged, and all 23 civilian drivetrain
definitions still bypass the edited block. The existing offline `compileJava check` tasks passed;
both edited Java classes target Java 8 bytecode (major 52). Live in-game driving, slopes/collisions,
steering, packet timing and multiplayer brake-light visuals were not tested and still require
Forge 1.7.10 validation.
