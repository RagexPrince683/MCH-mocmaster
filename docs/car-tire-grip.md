# Civilian car tire grip

`CivilianCarGrip = true` explicitly opts a passenger-car definition into the grip and steering model. It defaults to **false**. Neither `WeightType` nor `Category` selects this behavior, and neither is changed. Existing definitions without the field retain their legacy handling, including old `WeightType = Car` packs. Tire metadata alone never enables grip.

This is bounded gameplay slip damping, not a measured tire/suspension/weight simulation. Only opted-in tank-backed civilian cars receive the correction and steering coupling. `DriveType` separately opts into axle-based propulsion. Without `CivilianCarDrivetrain`, absent `DriveType` keeps legacy thrust; with the new engine opt-in, it uses an axle-neutral wheel model. Legacy engines retain isotropic `MotionFactor` drag. Opted-in car engines use explicit force/drag/gearing with a final safety cap, described below. Aircraft/boat code is unchanged. Civilian spring/shock suspension is documented in the [tank reference](vehicle-config/tanks.md).

## Configuration

| Field | Units / values | Default |
|---|---|---|
| `CivilianCarGrip` | Boolean, explicit passenger-car opt-in | `false` |
| `DriveType` | `FWD`, `RWD`, `AWD`; case-insensitive explicit propulsion opt-in | Unset/invalid: legacy thrust, or axle-neutral with the engine opt-in |
| `FrontTireSize` | Optional metric radial dimensions, e.g. `225/50R16`, `265/35ZR19`, `175R14` | Unset; neutral response 1.0 |
| `RearTireSize` | Same, independently optional | Unset; neutral response 1.0 |
| `CarLateralGrip` | Maximum sideways velocity change per 20 Hz tick, blocks/tick²; 0–0.25 | `0.12`; `0` disables grip and its steering coupling |
| `CarMinimumSteering` | Minimum high-speed yaw authority, degrees per 20 Hz tick; 0–10 | `0` (use only the grip-derived limit) |
| `CarGripDiagnostics` | Boolean; separate server CSV and in-memory snapshot | `false` |

`CarMinimumSteering` is a body-yaw authority floor, not additional tire force. It is scaled by elapsed tick time and wheel contact, and both client prediction and server authority apply the same value. `CarLateralGrip` remains the cap on sideways velocity correction; when the yaw requested through `CarMinimumSteering` exceeds that force budget, the car follows a wider path (controlled understeer) rather than gaining free lateral grip.

```ini
; Representative stock 2018 Dodge Challenger R/T
WeightType = Car
CivilianCarGrip = true
DriveType = RWD
FrontTireSize = 245/45R20
RearTireSize = 245/45R20
CarLateralGrip = 0.12
; Enable only while investigating a specific vehicle:
CarGripDiagnostics = false
```

Tire sizes are **sourced metadata**, separate from **gameplay tuning** (`CarLateralGrip`, damping, and steering budget). Unknown sizes stay unset without disabling grip. Width is not the main grip control. The original geometry response remains bounded to 0.95–1.05 and never increases the per-tick limit.

The parser ignores case and spaces. Accepted dimensions are width 100–500 mm, explicit aspect 20–100%, rim 10–30 inches including half-inch rims. Full-profile `175R14` has no invented aspect ratio. Unsupported/malformed notation, P-metric prefixes, service suffixes, bias-ply, flotation and metric PAX diameters remain neutral. Normalize a sourced P245/45R20 to `245/45R20`; strip H/speed/service designations from a verified metric size. Invalid/nonfinite grip uses 0.12; finite values clamp to 0–0.25. Reload resets removed opt-in, diagnostic and tire fields to their defaults.

## Contact findings before tuning

The old 0.05-block probe did **not** reach the road under the invisible passenger-car wheel boxes. `MCH_EntityTank` sets `yOffset = 0.35`. The AE86, Challenger and R32 define wheel Y = −0.24, so a level, grounded body at Y = 0.35 has a target wheel bottom at **Y = 0.11** above the road. `MCH_WheelManager.move` damps vertical movement by 0.15 and converges to that target rather than forcing wheels down to the road. The original `hasGroundContact` therefore counts **0/4** wheels after settling. Increasing tire width or the force limit alone cannot fix a zero contact multiplier.

`MCH_CarWheelContactTest` established this **before changing the grip constants** using real `MCH_EntityWheel.moveEntity`, Minecraft collision AABBs and the manager's settling/placement sequence. A headless fixture supplies a flat collision floor; mod/render/chunk initialization is bypassed. This is a reproducible collision test, not a recorded in-game driving session.

| Bundled layout | Local Y / front Z / rear Z | Original probe, settled | Grip support sweep, settled |
|---|---|---|---|
| `ae86` | −0.24 / 1.99 / −1.70 | 0/4 | 4/4 (front 2, rear 2) |
| `challenger` | −0.24 / 1.851 / −1.934 | 0/4 | 4/4 (front 2, rear 2) |
| `bnr32` | −0.24 / 1.965 / −1.789 | 0/4 | 4/4 (front 2, rear 2) |

The grip-only collision sweep uses the wheel's actual AABB bottom offset (`box.minY - wheel.posY`) and height at its current transformed body anchor. Let `B` be that anchored bottom, `H` its collision height, and `F` the body's current collision-box bottom. The query bottom is raised to at least `max(B, min(F, B+H))`, and its downward reach is `0.05 + max(0, B-F)`: **0.16 blocks** for the three level layouts above. This also handles low authored anchors whose invisible boxes overlap the road: Minecraft's downward `calculateYOffset` requires the query to start above the supporting surface. The normalization is bounded by the anchored box's real height, so a box wholly below the body floor cannot gain support merely by being lifted to that floor. A wheel already above the normalized plane is never lowered. Stale wheels left below the body on takeoff cannot supply contact. The live wheel position/box, suspension travel and flags are unchanged. Support must be underneath the actual wheel footprint; walls, ceilings, water alone and terrain elsewhere do not qualify. A 0.05-block collision skin remains intentional.

The Chiron's `SetWheelPos = 0.8, -0.74, 1.70, -1.80` exposes the low-anchor case. On a level road with settled body `yOffset = 0.35`, its nominal wheel bottom is **0.39 blocks below** the road, and civilian suspension places the box another 0.45 blocks down when its own downward sweep finds no support. The old grip query raised that box only to the nominal bottom and swept 0.05 blocks downward; it therefore counted **0/4**. Source comparison identifies `f5dde8e3` as the first of the three propulsion commits to turn this missing contact into zero body thrust; `da36b064` and `40cc139e` retain the same faulty probe. The corrected query starts at the body floor within the wheel's one-block collision height. These are collision-geometry/source findings, not an in-game trace. The wheel-inertia model still transfers stored spin into traction-limited body force when contact returns, including during wheel-RPM fuel cut; no gearing or engine-force tuning is changed.

The manager samples each wheel once per correction. Missing/dead wheels still count in the configured denominator. Front/rear grouping uses the midpoint of min/max local Z, not copied axle `onGround` flags. Removing front support gives **2/4**, losing another wheel gives **1/4**, and an airborne body with stale wheels near the road gives **0/4**. Those cases are covered by collision regressions.

## Grounded turn trace and tuning

1. Steering input originally changed body yaw through `onUpdateAngles` while momentum retained its world direction. Rotation packets deliver that yaw to the server. With zero wheel contact, the old grip performed no correction. Even with hypothetical full contact, 25% damping retained 75% of each tick's lateral slip, allowing yaw to outrun the path.
2. In the legacy engine path, thrust, the validated `Speed` clamp and `MotionFactor` drag retain their existing order. The opted-in car engine uses the force/drag/final-cap sequence documented below. When `DriveType` is configured, current collision support and lateral demand bound added thrust before the clamp/drag. The server then updates wheels, takes the lateral contact snapshot, bounds the yaw change accumulated since the preceding physics tick, recomputes the horizontal basis from that applied yaw, applies one lateral correction, and moves the body.
3. Forward `f = (−sin θ, cos θ)` and sideways `s = (cos θ, sin θ)`. `side = velocity·s`. Contact fraction `C = supported/configured`. Front/rear contact weights the existing tire response `R`.
4. Requested signed correction is `side * 0.85 * R * C`. Limit is `clamp(CarLateralGrip,0,0.25) * C`. Applied correction is `sign(side) * min(abs(requested), limit)`, subtracted along `s`. This preserves forward velocity relative to the applied heading, never reverses slip, never increases horizontal kinetic energy, and fades continuously to zero at low slip.
5. Steering may use 75% of that lateral acceleration budget, retaining 25% for residual slip. The yaw limit in degrees is `asin(clamp(0.75*grip*C/speed,0,1)) * tickDelta * speed/(speed+0.05)`, converted to degrees. Requests keep their sign and clamp to that bound. Zero speed/contact gives zero added steering; low-speed response is continuous. Client key steering respects this bound and elapsed tick time; the server also bounds the cumulative yaw delivered by rotation packets once per physics tick. The opted-in reverse path omits the old second, opposing yaw update in `onUpdate_ControlSub`; reverse steering is handled by the same angle path and force budget.

The default force bound rises from **0.06 to 0.12 blocks/tick²** (48 blocks/s² at full contact). Slip damping rises from **0.25 to 0.85**. These are deliberate gameplay choices after repairing contact. Tire geometry is unchanged: its width term is a bounded logarithm and its sidewall term uses a neutral 205/55R16 reference. Its entire variation remains ±5%, independent of the acceleration cap.

At speed **0.8 blocks/tick**, a 6° heading change produces **0.083623 blocks/tick** sideways velocity. With neutral tire response:

| Case | Requested / applied correction (blocks/tick) | Remaining sideways speed |
|---|---|---|
| Old actual contact 0/4 | 0 / **0** | 0.083623 |
| Old formula if contact had been 4/4 | 0.020906 / **0.020906** | 0.062717 |
| New actual contact 4/4 | 0.071079 / **0.071079** | 0.012543 |
| New airborne contact 0/4 | 0 / **0** | 0.083623 |

For a larger `side = 0.30` at full contact, old coasting requested/applied is **0.075/0.060** (under the old 0.10 longitudinal demand, applied was **0.051962**); new requested/applied is **0.255/0.120**. At `side = 0.001`, new correction is **0.000850**, not a snap to zero. Forward/reverse sustained-turn tests at 0.8 and 1.5 blocks/tick keep the velocity direction within 1.5° of the applied body heading. At 1.5, a 6° request is bounded to approximately **3.33°/tick**; at 0.8, it remains 6°.

## Throttle and drivetrain

This section records the earlier `DriveType`-only propulsion path. Cars with
`CivilianCarDrivetrain = true` use the engine and longitudinal wheel state described below,
reusing the same axle layout, contact sampling and lateral-first traction capacity.

`DriveType = FWD` selects front wheels, `RWD` selects rear wheels, and `AWD` selects both
axles for **server-authoritative forward and reverse propulsion**. Omitted, empty, or invalid
values preserve legacy engine force, including on `CivilianCarGrip = true` cars. Reload resets
removed values to unset. Neither `WeightType` nor `Category` selects a drivetrain. `DriveType`
does not implicitly enable grip, steering, or suspension.

The manager retains front/rear membership and configured counts when creating the mirrored
`SetWheelPos` wheels. It divides axles at `(minZ + maxZ)/2`, with greater local Z forward.
Support is the existing read-only collision sweep, not body `onGround`, copied paired flags,
water, or the suspension heightmap. Missing, dead, or absent wheel slots contribute no support
and cannot shrink configured counts. No configured wheels or no configured selected axle means
zero propulsion. An unsupported driven axle does not borrow a non-driven axle's contact.

Before engine force, let signed horizontal demand be `A` (the existing thrust vector's horizontal
magnitude times forward throttle or negative reverse demand), supported driven wheels `S`,
configured driven wheels `N`, and configured total wheels `T`:

1. Contact-scaled demand is `abs(A) * S/N`.
2. Each driven axle with `s` supported wheels has longitudinal capacity `P = 0.24 * s/T`
   blocks/tick². This fixed gameplay budget preserves ordinary straight-road thrust on a fully
   supported two-wheel axle of a four-wheel car (capacity 0.12); it is not a manufacturer friction
   coefficient or a simulated axle load.
3. If civilian lateral grip is active, calculate that axle's existing lateral request with
   `sideways * 0.85 * tireResponse * s/T`, bounded by `CarLateralGrip * s/T`. Its utilization
   `u` is the absolute bounded correction divided by that lateral limit. Remaining propulsion
   capacity is `P * sqrt(max(0, 1-u²))`. Otherwise `u = 0`: disabling lateral grip does not
   disable drivetrain contact or the longitudinal cap.
4. Add the selected axles' remaining capacities. Applied demand is `sign(A) * min(contact-scaled
   demand, pooled capacity)`. AWD pools available capacity without prescribing a torque split.
   Apply the resulting scale only to added engine force, including the pre-existing forward
   throttle-linked vertical term; never multiply accumulated velocity by wheel contact.

For a neutral-tire four-wheel car with demand 0.10 and no sideways slip:

| Collision support | FWD added acceleration | RWD added acceleration | AWD added acceleration |
|---|---:|---:|---:|
| Both axles, 2 front + 2 rear | 0.10 | 0.10 | 0.10 |
| Front only, 2 front + 0 rear | 0.10 | 0 | 0.05 |
| Rear only, 0 front + 2 rear | 0 | 0.10 | 0.05 |
| One front wheel, 1 front + 0 rear | 0.05 | 0 | 0.025 |
| No support | 0 | 0 | 0 |

At full support and sideways speed 0.10, neutral tires and default lateral grip use
`u = 0.085/0.12`; each driven two-wheel axle retains approximately **0.084706** propulsion
capacity. Demand 0.10 is therefore traction-limited for FWD/RWD while AWD can pool both
axles. At sideways speed at least `0.12/0.85`, lateral demand saturates the budget and added
propulsion is zero. These are equation examples, not recorded road tests.

Lateral correction and steering retain their existing implementation and priority. Throttle/brake
inputs do not take lateral authority away; identical velocity/contact still gives identical lateral
correction. Existing momentum continues through contact loss under normal drag/gravity/collision.
Throttle buildup, braking, burnouts, gearing, speed ceilings, brake lights, and vehicle collision are
unchanged. Traction limits can reduce attainable speed without changing the configured ceilings.
That earlier path has no differential, tire rotation/slip simulation, load transfer, or manufacturer torque allocation.

## Engine, gears, brakes and longitudinal slip

`CivilianCarDrivetrain = true` explicitly selects the server engine and longitudinal wheel model;
its default is false. Grip, weight, category and tire metadata do not enable it. The
[tank reference](vehicle-config/tanks.md#civilian-car-drivetrain) defines the force/RPM/drag equations,
bounded parser defaults, reload behavior and synchronization contract.

The 23 passenger definitions below configure throttle response, idle/redline, forward ratios/count,
reverse ratio, final drive, wheel radius, engine force, drag, longitudinal traction, shift duration,
service brake and rear handbrake explicitly. Car-specific force, drag, response, shift and brake values
are gameplay tuning. Even a published gearbox uses the automatic gameplay shift controller; this
is not a factory torque, clutch, differential or braking simulation. Existing `ThrottleDownFactor`,
reverse ceilings, tire metadata, suspension and `DriveType` are retained. `fresh_auto`, `impreza`,
and `phantomarmored` keep the axle-neutral fallback because their powered-wheel identity is unresolved.
Police definitions `fordpolice` and `bnr32_police`, military/utility vehicles, aircraft and boats retain
their existing engines and controls.

W builds drive force over time. S service-brakes forward travel before a near-stop dwell permits
reverse; W+S supplies both throttle and braking without reverse. Space adds held rear brake torque
and leaves W/S available. Axle contact and `DriveType` select powered-wheel traction and slip.
A sufficiently powerful car can spin tires against either brake; a brake can also lock a powered axle.
Airborne spin supplies neither body force nor road smoke. S and Space retain brake-light behavior.
Shifts alter active gear, wheel torque and RPM without resetting body velocity. Sound and axle
animation follow synchronized engine/wheel state. The existing car HUDs now show gear/RPM and
use RPM for their tachometers; other vehicles using those HUDs retain their previous needles.

### Bundled drivetrain values and speed audit

At 20 Hz, `Speed * 72` is km/h, not mph. The first numeric column below audits every original
ceiling against the named year/trim. Only Chiron, Carrera GT and the standard 1976 240D have corrected
ceilings here: their identities and cited factory speeds support the change. Other top speeds remain
explicit gameplay ceilings while market, trim, fitted gearbox or limiter evidence is unresolved.
The authoritative files are `src/main/resources/assets/mcheli/tanks/<definition>.txt`; the deprecated
`configreference` tree is not used or recreated. No 400R, Hellcat, WRX/STi or tuned-build performance
is inferred from a generic model name.

| Definition | Original km/h | Current Speed / km/h | Identity, verified basis and remaining gaps |
|---|---:|---:|---|
| `2102` | 61.20 | 0.85 / 61.20 | 1971 VAZ-2102; selected 4MT gameplay ratios. Period engine/gearing/top speed incomplete. |
| `2105` | 66.24 | 0.92 / 66.24 | 1980 Lada 2105; engine/gearbox variant unresolved. Four-speed gameplay tuning. |
| `350z` | 125.28 | 1.74 / 125.28 | 2002 launch identity; representative early VQ35DE 6MT [factory MT][car-z-mt]/[final drive][car-z-fd]. Market/trim/top speed unresolved; 6600 RPM from Canadian brochure in the reverse inventory. |
| `ae86` | 86.40 | 1.2 / 86.40 | 1983 AE86; [Toyota][car-ae86] establishes five-speed GT baseline. Exact grade/gearing/top speed unresolved; ratios/RPM/radius tuned. |
| `altis` | 84.24 | 1.17 / 84.24 | 2014 Corolla/Altis: [Toyota][car-altis] documents 4AT, 6MT and CVT alternatives. Representative four-speed gameplay gearing; actual market/trim/transmission/ratios/RPM/top speed unresolved. Radius uses the representative US L tire in the reverse inventory. |
| `bcnr33` | 111.60 | 1.55 / 111.60 | 1995 BCNR33; selected five-speed baseline. Exact factory ratios and limiter/top speed not verified; gameplay gearing. |
| `bnr32` | 112.32 | 1.56 / 112.32 | 1989 BNR32; [Nissan][car-r32] verifies five speeds. Representative [factory manual][car-r32-manual] C2/C5 supplies ratios/final drive. Exact 1989 market match, usable redline and top speed unresolved. |
| `bnr34` | 125.28 | 1.74 / 125.28 | 1999 BNR34; six-speed ratios from [Nissan's 2000 table][car-r34]. Exact 1999 match/redline/limiter/top speed unresolved. |
| `bugattichiron` | 187.92 | 5.83333 / 420.00 | 2016 Chiron: [factory launch][car-chiron] verifies seven gears and 420 km/h. [Published gear speeds][car-chiron-spec] at 6700 RPM inform approximate ratios; absolute gearbox/final-drive ratios remain unverified. |
| `carrera_gt` | 147.60 | 4.58333 / 330.00 | 2004 Carrera GT: [Porsche][car-carrera] verifies 330 km/h; [factory sheet][car-carrera-sheet] supplies six ratios, final drive and 8400 RPM. Nominal radius from 335/30ZR20. |
| `challenger` | 111.60 | 1.55 / 111.60 | 2018 R/T 5.7: [Dodge][car-challenger] verifies standard six-speed ratios/final drive and 5800 RPM. Actual 6MT/8AT and trim-specific top speed unresolved. |
| `dacia` | 78.48 | 1.09 / 78.48 | Display says 2009 Sandero but TechYear is 1969. Engine/trim/transmission unresolved; five-speed gameplay gearing. [2009 brochure][car-dacia] covers different engines/speeds. |
| `delorean` | 93.60 | 1.3 / 93.60 | 1981 DMC-12: representative five-speed [factory handbook][car-dmc] ratios/final drive. Actual 5MT/3AT and top speed unresolved; RPM tuned. |
| `fresh_auto` | 68.40 | 0.95 / 68.40 | 2020 Tsar Zhiga custom drift build: build-specific gearbox, final drive, engine, layout and top speed unresolved. Entire drivetrain is gameplay tuning. |
| `impreza` | 103.68 | 1.44 / 103.68 | Generic 1992 Impreza: engine/market/trim/gearbox/layout unresolved. Five-speed gameplay tuning; no WRX/STi performance assumed. |
| `phantom` | 111.60 | 1.55 / 111.60 | 2003 Phantom VII: [manufacturer][car-phantom] verifies six speeds, 240 km/h summer/208 km/h all-season limits. Market/tires unresolved, so old ceiling retained. Representative [ZF ratios][car-zf]; installed variant/final drive/RPM unverified. |
| `phantomarmored` | 111.60 | 1.55 / 111.60 | 2003 armored Phantom: conversion, transmission, final drive, axle identity, limiter/top speed unresolved. Six-speed gameplay tuning. |
| `rx-8` | 105.84 | 1.47 / 105.84 | 2003 launch RX-8: representative [2004 US 6MT][car-rx8] ratios/final drive and 9000 RPM. Exact market/6MT versus other gearbox/top speed unresolved; reverse ratio retained from the 2008 factory table in the reverse inventory. |
| `rx7` | 102.96 | 1.43 / 102.96 | 1985 RX-7 FC: turbo/non-turbo, market and fitted transmission unresolved. Five-speed ratios/RPM/radius and ceiling are gameplay tuning. |
| `s15` | 111.60 | 1.55 / 111.60 | 1999 S15: representative [spec-R 6MT][car-s15] factory ratios/final drive. Bundled spec-R/spec-S, usable redline and top speed unresolved; RPM/radius tuned. |
| `silvia_s14` | 126.72 | 1.76 / 126.72 | 1993 S14: K/Q grade, transmission, factory ratios/redline/limiter/top speed unresolved. Five-speed gameplay gearing. |
| `starion` | 105.84 | 1.47 / 105.84 | ESI-R display name conflicts with introduction-year 1982. Exact year/transmission/gearing/top speed unresolved; five-speed gameplay tuning. |
| `w123` | 64.08 | 1.91667 / 138.00 | 1976 W123 240D standard 4MT: [Mercedes archive][car-w123] verifies four ratios/final drive and 138 km/h. Full-profile tire radius and usable redline unverified and tuned. |

The following is the explicit forward transmission and acceleration inventory; reverse ratios,
radii, pedal response, shift/brake/traction settings remain readable alongside it in each asset.
A published or representative ratio does not validate the remaining engine tuning.

| Definition | Forward ratios (first to top) | Final drive | Idle / redline RPM | Drive force | Drag |
|---|---|---:|---:|---:|---:|
| `2102` | 3.75, 2.3, 1.5, 1 | 4.44 | 850 / 5500 | 0.00065 | 0.00525234 |
| `2105` | 3.67, 2.1, 1.36, 1 | 4.3 | 850 / 5600 | 0.0007 | 0.00430593 |
| `350z` | 3.794, 2.324, 1.624, 1.271, 1, 0.794 | 3.538 | 750 / 6600 | 0.0021 | 0.00320084 |
| `ae86` | 3.6, 2.02, 1.38, 1, 0.86 | 4.3 | 900 / 7500 | 0.0014 | 0.00669428 |
| `altis` | 2.85, 1.55, 1, 0.7 | 4.2 | 750 / 6500 | 0.00105 | 0.00433714 |
| `bcnr33` | 3.25, 1.95, 1.31, 1, 0.76 | 4.1 | 850 / 8000 | 0.00215 | 0.00443421 |
| `bnr32` | 3.214, 1.925, 1.302, 1, 0.752 | 4.111 | 850 / 8000 | 0.0021 | 0.00421173 |
| `bnr34` | 3.827, 2.36, 1.685, 1.312, 1, 0.793 | 3.545 | 850 / 8000 | 0.0022 | 0.00369136 |
| `bugattichiron` | 3.325, 1.995, 1.496, 1.151, 0.935, 0.767, 0.713 | 3 | 800 / 6700 | 0.004 | 0.00006930 |
| `carrera_gt` | 3.2, 1.87, 1.36, 1.07, 0.9, 0.75 | 4.44 | 900 / 8400 | 0.0045 | 0.00034252 |
| `challenger` | 2.97, 2.1, 1.46, 1, 0.74, 0.5 | 3.9 | 750 / 5800 | 0.0023 | 0.00451870 |
| `dacia` | 3.73, 2.05, 1.39, 1.03, 0.82 | 4.5 | 800 / 6000 | 0.0009 | 0.00411579 |
| `delorean` | 3.364, 2.059, 1.381, 1.057, 0.8205 | 3.44 | 850 / 6500 | 0.0011 | 0.00376410 |
| `fresh_auto` | 3.1, 2.25, 1.72, 1.36, 1.1, 0.88 | 4.7 | 950 / 7800 | 0.0035 | 0.03254031 |
| `impreza` | 3.4, 2.02, 1.44, 1.07, 0.82 | 4.2 | 850 / 6800 | 0.0017 | 0.00411460 |
| `phantom` | 4.17, 2.34, 1.52, 1.14, 0.87, 0.69 | 3.15 | 650 / 6000 | 0.0018 | 0.00332359 |
| `phantomarmored` | 4.2, 2.5, 1.65, 1.22, 0.96, 0.75 | 3.2 | 650 / 6000 | 0.0013 | 0.00255608 |
| `rx-8` | 3.76, 2.27, 1.65, 1.19, 1, 0.84 | 4.44 | 900 / 9000 | 0.0018 | 0.00536628 |
| `rx7` | 3.45, 2.02, 1.39, 1, 0.76 | 4.1 | 900 / 7500 | 0.0015 | 0.00372219 |
| `s15` | 3.626, 2.2, 1.541, 1.213, 1, 0.767 | 3.692 | 850 / 7000 | 0.002 | 0.00401146 |
| `silvia_s14` | 3.3, 1.95, 1.32, 1, 0.76 | 4.1 | 850 / 7000 | 0.0019 | 0.00271635 |
| `starion` | 3.35, 1.96, 1.36, 1, 0.82 | 3.55 | 850 / 6500 | 0.0016 | 0.00319441 |
| `w123` | 3.9, 2.3, 1.41, 1 | 3.69 | 750 / 4800 | 0.00042 | 0.00029913 |

Drive/drag coefficients target about 99.8% of each configured ceiling through the force balance,
with full contact, no sideways demand and the active road-RPM gear. That mathematical target does
not demonstrate acceleration times or in-game terminal speed. Some retained low gameplay ceilings
are reached before top gear; the higher ratios remain distinct usable ranges, not fictitious equal
bands forced below the cap. Changing only Speed raises a safety boundary and cannot add engine power.

Compilation and static review do not establish road feel. Gradients, braking distance, shift hunting,
contact loss, burnout intensity, tire animation, multiplayer interpolation and HUD readability still
require runtime observation. No game launch or live driving was performed for this change.

[car-z-mt]: https://boredmder.com/FSMs/Nissan/350z/2003/MT.pdf
[car-z-fd]: https://boredmder.com/FSMs/Nissan/350z/2003/RFD.pdf
[car-ae86]: https://www.toyota.co.jp/jpn/company/history/75years/vehicle_lineage/car/id60003763/
[car-altis]: https://pressroom.toyota.com/toyota-2014-corolla-efficiency-driving-dynamics/
[car-r32]: https://www.nissan-global.com/EN/HERITAGE_COLLECTION/skyline_gt-r_1989.html
[car-r32-manual]: https://www.scribd.com/doc/4967393/BNR32-Service-Manual-Bookmarked
[car-r34]: https://global.nissannews.com/en/releases/skyline-and-skyline-gt-r-undergo-minor-model-change
[car-chiron]: https://newsroom.bugatti.com/press-releases/geneva-international-motor-show-2016
[car-chiron-spec]: https://bugatti-newsroom.imgix.net/66703700d9bf8f4b7ce9211c/211122_BU_Chiron%20ENG.pdf
[car-carrera]: https://newsroom.porsche.com/en/2025/history/porsche-25-years-world-premiere-carrera-gt-40609.html
[car-carrera-sheet]: https://www.ausmotive.com/downloads/Porsche/Carrera-GT-specs.pdf
[car-challenger]: https://www.media.stellantis.com/uploads/me/ME/2018/Dodge/Technical-sheet/1806_Dodge_Challenger.pdf
[car-dacia]: https://daciaclubnederland.nl/storage/downloads/netherlands/nl-brochure-dacia-sandero-2009-08.pdf
[car-dmc]: https://grupomotor.net/descriptions/zss1199_0.6.pdf
[car-phantom]: https://www.press.bmwgroup.com/south-africa/article/detail/T0129326EN/the-rolls-royce-phantom
[car-zf]: https://aston1936.com/wp-content/uploads/2018/12/ZF-6HP26-DataSheet.pdf
[car-rx8]: https://news.mazdausa.com/download/RX-8-Spec-Sheet-Final.pdf
[car-s15]: https://global.nissannews.com/ja-JP/releases/19990119-e_presskit
[car-w123]: https://mercedes-benz-publicarchive.com/marsClassic/en/instance/ko/240-D--W-123-D-24-1976---1985.xhtml?oid=5094

## Earlier drivetrain identity audit

**20** passenger-car definitions receive a field: **2 FWD, 14 RWD, 4 AWD**. Only `DriveType`
lines are added to their assets. The stock identities support these choices; generic weight/category
and cosmetic drift effects are not drivetrain evidence.

| Definition(s) | DriveType | Identity/evidence |
|---|---|---|
| `2102`, `2105` | RWD | Stock VAZ-2102 estate / Lada 2105 classic rear-drive chassis; [LADA rear-axle service instruction TI 3100.25100.20405 (reproduction)](https://autodetal-pro.ru/tekhnicheskaya-informatsiya/ti-3100-25100-20405-reduktory-zadnego-perednego-mosta-avtomobiley-lada-snyatiye-i-ustanovka). No modified-build specification inferred. |
| `350z` | RWD | [Nissan 2003 350Z launch description](https://usa.nissannews.com/en-US/releases/2003-350z-press-kit) specifies front engine/rear-wheel drive. |
| `ae86` | RWD | [Toyota's AE86 lineage](https://www.toyota-global.com/company/history_of_toyota/75years/vehicle_lineage/car/id60003763/index.html) explicitly retains the FR layout. |
| `altis` | FWD | Display name identifies a 2014 Corolla; [Toyota's 2014 brochure, specification table (mirror)](https://xr793.com/wp-content/uploads/2017/12/2014-Toyota-Corolla.pdf) lists FWD throughout. No trim-specific torque claim. |
| `bnr32` | AWD | [Nissan's 1989 BNR32 description](https://www.nissan-global.com/EN/HERITAGE_COLLECTION/skyline_gt-r_1989.html) identifies ATTESA E-TS 4WD. |
| `bcnr33` | AWD | [Nissan's BCNR33 heritage description](https://www.nissan.co.jp/HERITAGE/DETAIL/152.html) distinguishes production ATTESA E-TS 4WD from the RWD Le Mans conversion. This definition names the production GT-R. |
| `bnr34` | AWD | [Nissan Fact File 2001–2002, printed p.19 / PDF p.12](https://www.nissan-global.com/PDF/ff_fy01j.pdf) lists BNR34 GT-R as 4WD. No racing conversion assumed. |
| `bugattichiron` | AWD | [Bugatti's 2016 launch release](https://newsroom.bugatti.com/press-releases/geneva-international-motor-show-2016) identifies permanent four-wheel drive. |
| `carrera_gt` | RWD | [Porsche Rennsport Reunion 7 press kit, Carrera GT section](https://newsroom.porsche.com/dam/jcr%3Ae134f175-bf3b-46f6-9434-de51f6ab6442/RR7%2520Press%2520Kit.pdf) identifies drive exclusively to the rear wheels. |
| `challenger` | RWD | [Dodge's 2018 specifications](https://www.media.stellantis.com/uploads/me/ME/2018/Dodge/Technical-sheet/1806_Dodge_Challenger.pdf): the configured R/T 5.7 is RWD; the V6 GT AWD is a different variant. |
| `dacia` | FWD | Display name identifies 2009 Sandero, despite the existing inconsistent `TechYear`. Standard Sandero front drive; [Dacia's drive-type table](https://cdn.group.renault.com/dac/ie/transversal-assets/brochures/model-brochures/sandero-brochure-oct.pdf) corroborates the model layout, not period gearing or trim. |
| `delorean` | RWD | DMC-12 rear-engine/rear-transaxle layout, [DMC owner's handbook, transmission/final drive section (mirror)](https://grupomotor.net/descriptions/zss1199_0.6.pdf). |
| `phantom` | RWD | [Rolls-Royce's Phantom VII description](https://www.press.bmwgroup.com/south-africa/article/detail/T0129326EN/the-rolls-royce-phantom) explicitly drives the rear wheels. |
| `rx7`, `rx-8` | RWD | [Mazda's rear-drive history](https://www.mazda.com.au/mazda-news/rear-wheel-drive-a-new-beginning-for-mazda/) identifies both model lines; [RX-8 specifications](https://news.mazdausa.com/download/2008-RX-8-Spec.pdf) confirm RWD. |
| `s15`, `silvia_s14` | RWD | Nissan Silvia S15 / S14 road-car identities; [Nissan's S15 launch release](https://global.nissannews.com/ja-JP/releases/19990119-e_presskit) identifies the rear-drive coupe, and [S14 heritage](https://www.nissan-global.com/EN/HERITAGE_COLLECTION/silvia_ks_s14.html) establishes the stock S14 identity. |
| `starion` | RWD | Named Starion ESI-R; [Mitsubishi's official parts-store model description](https://parts.mitsubishicars.com/v-mitsubishi-starion) identifies the rear-drive drivetrain. |
| `w123` | RWD | [Mercedes-Benz 240D factory archive](https://mercedes-benz-publicarchive.com/marsClassic/en/instance/ko/240-D--W-123-D-24-1976---1985.xhtml?oid=5094) lists rear driven wheels. |

Civilian passenger cars left **without `DriveType`** because identity is insufficient:

- `impreza`: generic name and introduction-year 1992 do not identify market/trim. Early Imprezas
  offered FWD and AWD; the representative AWD choice in the reverse-speed research does not establish
  the actual bundled car's drivetrain.
- `fresh_auto`: a modified VAZ-2105 “Tsar Zhiga” Fresh Auto drift build; no build-specific drivetrain
  evidence was established. Stock 2105 specifications and drift effects cannot prove this conversion.
- `phantomarmored`: unidentified armored conversion. The stock Phantom's RWD layout does not establish
  the conversion's mechanical specification; axle identity stays unset. Its new engine opt-in uses
  the axle-neutral fallback and leaves its grip/suspension selection unchanged.

`bnr32_police` and `fordpolice` are excluded by the task's police-vehicle limit, not assigned a drivetrain.
Military/utility vehicles, unarmed military-family Hilux, trucks, ATV, bicycle, tractor, tracked tanks,
aircraft, and boats receive no drivetrain edits.

## Diagnostics

Set `CarGripDiagnostics = true` in the one vehicle definition being investigated, then reload/restart normally. It is safe to enable on an excluded vehicle: it records `not_opted_in` and applies no force. All bundled files leave diagnostics disabled.

The server appends one record per physics tick to **`logs/car-tire-grip.csv`**, relative to the game/server working directory. It never writes these records to normal console output. Columns identify vehicle/entity/tick, configured wheels, front/rear contacts, the original raw 0.05 probe count, suspension paired flags, signed sideways speed, unbounded requested correction, actual applied correction, limit, reason, and requested/applied yaw delta. Velocity is blocks/tick, correction is blocks/tick², yaw is degrees/tick. The old raw and paired counts are diagnostic comparisons only. This snapshot runs **after** the wheel/suspension update; propulsion samples contact **before** that update. CSV `requested`, `applied`, and `limit` describe lateral correction, not engine/body force, so they cannot establish the earlier thrust contact or a fuel-cut/brake fault.

Reasons are `not_opted_in`, `grip_disabled`, `no_wheels`, `no_contact` (airborne or unsupported wheels), `no_sideways_speed`, `invalid_input`, and `applied`. An excluded/disabled vehicle may show a hypothetical formula request but its **applied** correction is always zero. The current snapshot is also available through `MCH_EntityTank.getCarGripDiagnostic()`; disabled vehicles return null. File failures disable CSV writes and remain inspectable through `MCH_CarGripDiagnostics.getWriteError()` without log spam or interrupting physics. Turn diagnostics off after capture.

On the client, the same opt-in also appends **`logs/car-steering-client.csv`** before the steering limiter runs. It records the left/right key state, raw requested yaw, limited yaw, horizontal speed, wheel-contact fraction, and elapsed tick factor. This distinguishes missing input from client-side contact or authority limiting before a rotation packet reaches the server.

## Bundled grip eligibility

The current checkout has **23** runtime definitions with `CivilianCarGrip = true` (a separate
setting from `DriveType`):

`2102`, `2105`, `350z`, `ae86`, `altis`, `bcnr33`, `bnr32`, `bnr34`, `bugattichiron`, `carrera_gt`, `challenger`, `dacia`, `delorean`, `fordpolice`, `fresh_auto`, `impreza`, `phantom`, `rx-8`, `rx7`, `s15`, `silvia_s14`, `starion`, `w123`.

`fresh_auto` is an unarmed civilian drift-car build, so it opts into lateral grip without claiming
stock tires or a known drivetrain. `fordpolice` already has a grip opt-in; its configuration is
unchanged by the drivetrain task. Explicit grip exclusions include `bm21`, `bnr32_police`,
`phantomarmored`, `mc_atv_normal`, `opel_blitz_fuel`, `toyota_unarmed` and the armed Hilux variants,
military trucks/utility vehicles, launchers, and military buggies. A car body, `Category = C`, or an
unarmed loadout alone selects neither grip nor a drivetrain. Horns, backfire and drift effects are
not armed conversions. No existing grip opt-ins are changed by this task.

## Stock identity and tire sources

The bundled `ae86` is treated as a **representative stock 1983 Toyota Corolla Levin GT APEX**, as requested. Its 1983 `TechYear` stays unchanged. [Toyota's period launch release, dated May 12, 1983](https://www.toyota.co.jp/jpn/company/history/75years/vehicle_lineage/car/id60003763/news/60003763.pdf), pp.12–13, identifies 2/3-door GT APEX grades but the inspected specification tables do not establish their standard tire size. Later databases commonly report 185/70HR13, while [Toyota's retrospective GAZOO listing](https://gazoo.com/catalog/maker/TOYOTA/COROLLA_LEVIN/198301/990003040/) conflicts on rim size. A reliable period tire fitment was not verified, so **both tire fields remain unset and grip stays enabled**; neutral response does not imply generic factory tires.

The bundled `challenger` is explicitly treated as a **stock 2018 Dodge Challenger R/T**, and **`TechYear` changes from 1970 to 2018** to match this chosen definition identity. [Dodge's official 2018 Challenger specifications](https://www.media.stellantis.com/uploads/me/ME/2018/Dodge/Technical-sheet/1806_Dodge_Challenger.pdf), p.13, lists **P245/45R20** all-season performance tires as standard for R/T (stored as `245/45R20` front and rear); pp.15–16 identifies the standard 20×8-inch R/T wheel. The separate SRT/Hellcat table and the model-credit URL are not evidence for R/T tires or handling. No tire compound-specific gameplay coefficient is inferred.

### Original tire metadata audit (8 definitions)

| Definition | Supported identity | Front | Rear | Source and uncertainty |
|---|---|---|---|---|
| `bnr32` | 1989 Nissan Skyline GT-R BNR32; ordinary GT-R named | 225/50R16 | 225/50R16 | [Nissan's 1989 heritage specification](https://www.nissan-global.com/EN/HERITAGE_COLLECTION/skyline_gt-r_1989.html). Standard 1989 GT-R; no later V-Spec wheel fitment assumed. |
| `bcnr33` | 1995 Nissan Skyline GT-R BCNR33; ordinary GT-R named | 245/45ZR17 | 245/45ZR17 | [Longstone specialist factory-fitment guide](https://www.longstonetires.com/classic-car-tires/nissan/nissan-skyline.html). It distinguishes standard GT-R/V-Spec from the 275/35R18 NISMO 400R. Specific special edition is not claimed. |
| `bnr34` | 1999 Nissan Skyline GT-R BNR34 | 245/40ZR18 | 245/40ZR18 | [Nissan factory wheel/tire table, January 1999 section](https://faq2.nissan.co.jp/faq/show/988?category_id=63&site_domain=default). Same tire size for GT-R and V-Spec; no trim selection necessary. |
| `bugattichiron` | 2016 launch Bugatti Chiron, no derivative named | 285/30R20 | 355/25R21 | [Bugatti's 2016 launch release](https://newsroom.bugatti.com/press-releases/geneva-international-motor-show-2016). Uses launch-car size, not a later derivative or the appearance implied by an alternate texture. |
| `carrera_gt` | 2004 Porsche Carrera GT | 265/35ZR19 | 335/30ZR20 | [Porsche technical specification, mirrored by AUSmotive, p.175/PDF p.3](https://www.ausmotive.com/downloads/Porsche/Carrera-GT-specs.pdf), corroborated for MY2004–2006 by [Porsche's approved-size table](https://files.porsche.com/filestore/download/canada/en/porscheservice-tire-currentsummertires/default/19cea30a-33d9-11e6-9225-0019999cd470/Overview-summer-tires-current-vehicles.pdf). Size only; no compound inferred from later approved replacement tires. |
| `delorean` | 1981 DMC-12 DeLorean | 195/60R14 | 235/60R15 | [Classic DeLorean Motor Company factory-fitment statement](https://support.delorean.com/kb/a31/tire-choices-updated.aspx). Uses original sizes rather than the article's smaller optional replacement rear. |
| `w123` | 1976 Mercedes-Benz W123 240D, standard sedan | 175R14 | 175R14 | [Longstone W123 240D guide](https://www.longstonetyres.co.uk/classic-car-tyres/mercedes/240.html). Excludes long-wheelbase and estate alternatives. Original full-profile notation retained; aspect ratio is deliberately unknown. |

| `challenger` | Representative stock 2018 Dodge Challenger R/T | 245/45R20 | 245/45R20 | Dodge 2018 specifications, p.13, standard R/T fitment; normalized P-metric notation. No Hellcat/Scat Pack upgrade assumed. |

The remaining 14 definitions from the original 22-car grip audit had unset sizes. Their identities/trim/market did not establish a verified period front/rear fitment; existing research gaps were not filled with wider tires to tune handling. This drivetrain change leaves all existing tire metadata untouched.

## Verification

Engine/gearing/brake/slip verification on **2026-09-27**:
`gradlew.bat compileJava --offline --no-daemon --gradle-user-home C:/Users/Owner/.gradle`
completed **BUILD SUCCESSFUL** using launch JDK 21, the existing cached convention/Jabel compiler,
and `GRADLE_USER_HOME=C:/Users/Owner/.gradle`. All nine changed/new production classes are
class-file major version **52 (Java 8)**. No build configuration/dependency changes were needed.
The asset diff is exactly 23 added opt-in lines, with no changes to speed ceilings, reverse factors,
drivetrain identities or excluded assets. `git diff --check` passed. Source review covered pilot-only
packets, the existing main-thread network task dispatch, brake/lamp separation, direction transitions,
legacy path guards, contact/traction bounds, observer state and control cleanup. One-step mathematical
checks covered traction-limited launch, stationary handbrake torque, locked braking and zero airborne
body force; 36 boundary encoding checks covered synchronized engine/axle fields. These are equation
and static checks, not execution of Java physics tests or in-game driving. No game launch, packaging,
remap/reobfuscation or multiplayer drive test was performed. Use the driving checks above for feedback.

Drivetrain verification on **2026-09-27**: with launch JDK 21 and the existing cached convention/Jabel
Java 8 compiler, `gradlew.bat compileJava --offline --no-daemon --gradle-user-home C:/Users/Owner/.gradle`
completed **BUILD SUCCESSFUL** against Forge 1.7.10-10.13.4.1614. The wrapper also required
`GRADLE_USER_HOME=C:/Users/Owner/.gradle` in this sandbox; no build configuration was changed.
The four changed production classes and the new/changed nested enum/contact classes are class-file
major version **52 (Java 8)**. The asset diff contains exactly 20 additions of `DriveType` and no other
asset changes; unresolved cars and police definitions are unchanged. `git diff --check` passed.
Source review covered both signed thrust paths, configured contact denominators, default/reload
behavior, and momentum preservation. No drivetrain runtime tests or in-game driving were performed.
The examples above are calculations, not test results. End-user Forge driving and multiplayer
feedback remains necessary, particularly on slopes, jumps, single-axle support, and sustained turns.
Existing grip CSVs report lateral behavior/contact; they do not record applied propulsion.

### Earlier grip-only verification

Executed `gradlew.bat compileJava test --offline --no-daemon` on 2026-09-26 with the existing cached Gradle/JDK environment: **BUILD SUCCESSFUL**, **49 tests, zero failures/errors**. The six changed/new production classes have class-file major version **52 (Java 8)**. The asset audit confirms the exact 22 eligible definitions in both trees, all bundled diagnostics disabled, and every existing `WeightType`/`Category` value unchanged.

Regression coverage includes real wheel settling/contact, front/rear support loss, dead wheels and stale airborne contact, unmodified collision boxes, opt-in defaults/reload, zero-force diagnostics, bounded forces/size response, low-speed continuity, forward/reverse sustained turns, actual server correction and yaw limits, energy/forward-speed preservation, and the exact bundled eligibility/exclusion list.

A headless collision/physics regression is not an in-game multiplayer playtest. The opt-in CSV makes flat-road driving, slopes, jumps, braking and networked interpolation observable in a running Forge 1.7.10 session without changing normal console output.
