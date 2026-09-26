# Civilian car tire grip

`CivilianCarGrip = true` explicitly opts a passenger-car definition into the grip and steering model. It defaults to **false**. Neither `WeightType` nor `Category` selects this behavior, and neither is changed. Existing definitions without the field retain their legacy handling, including old `WeightType = Car` packs. Tire metadata alone never enables grip.

This is bounded gameplay slip damping, not a measured tire/suspension/weight simulation. The wheel movement, pitch/roll suspension, rendering, thrust, speed clamp, isotropic `MotionFactor` drag, and all aircraft/boat code remain unchanged. Only opted-in tank-backed civilian cars receive the new correction and steering coupling.

## Configuration

| Field | Units / values | Default |
|---|---|---|
| `CivilianCarGrip` | Boolean, explicit passenger-car opt-in | `false` |
| `FrontTireSize` | Optional metric radial dimensions, e.g. `225/50R16`, `265/35ZR19`, `175R14` | Unset; neutral response 1.0 |
| `RearTireSize` | Same, independently optional | Unset; neutral response 1.0 |
| `CarLateralGrip` | Maximum sideways velocity change per 20 Hz tick, blocks/tick²; 0–0.25 | `0.12`; `0` disables grip and its steering coupling |
| `CarGripDiagnostics` | Boolean; separate server CSV and in-memory snapshot | `false` |

```ini
; Representative stock 2018 Dodge Challenger R/T
WeightType = Car
CivilianCarGrip = true
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

The grip-only collision sweep reaches `0.05 + max(0, parent.yOffset + wheel.localY - wheel.yOffset)` below the wheel box: **0.16 blocks** for those three definitions. This accounts for the configured box rest gap, not `stepHeight` or the suspension's 0.6-block terrain tolerance. If a wheel lags below its current transformed body target during takeoff, the query box is raised to the target before probing, so stale wheels cannot provide airborne grip. The live wheel position/box and flags are never changed. Support must be underneath the actual wheel footprint; walls, ceilings, water alone and terrain elsewhere do not qualify. A 0.05-block collision skin remains intentional.

The manager samples each wheel once per correction. Missing/dead wheels still count in the configured denominator. Front/rear grouping uses the midpoint of min/max local Z, not copied axle `onGround` flags. Removing front support gives **2/4**, losing another wheel gives **1/4**, and an airborne body with stale wheels near the road gives **0/4**. Those cases are covered by collision regressions.

## Grounded turn trace and tuning

1. Steering input originally changed body yaw through `onUpdateAngles` while momentum retained its world direction. Rotation packets deliver that yaw to the server. With zero wheel contact, the old grip performed no correction. Even with hypothetical full contact, 25% damping retained 75% of each tick's lateral slip, allowing yaw to outrun the path.
2. Existing thrust, the validated `Speed` clamp and `MotionFactor` drag still run in their existing order. The server then updates wheels, takes the contact snapshot, bounds the yaw change accumulated since the preceding physics tick, recomputes the horizontal basis from that applied yaw, applies one lateral correction, and moves the body.
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

No drivetrain field is added. The old global longitudinal-demand reduction is removed: throttle and brake inputs do **not** directly consume axle grip in this model. Existing thrust, reverse and drag still change the velocity presented to the grip calculation, but identical velocity/contact yields identical lateral grip when coasting or accelerating. There is no RWD rear-axle power reduction, AWD torque allocation, invented split, or axle-load simulation. Front/rear grouping exists solely for contact and tire metadata. Adding drivetrain metadata without an axle power model would falsely imply behavior.

## Diagnostics

Set `CarGripDiagnostics = true` in the one vehicle definition being investigated, then reload/restart normally. It is safe to enable on an excluded vehicle: it records `not_opted_in` and applies no force. All bundled files leave diagnostics disabled.

The server appends one record per physics tick to **`logs/car-tire-grip.csv`**, relative to the game/server working directory. It never writes these records to normal console output. Columns identify vehicle/entity/tick, configured wheels, front/rear contacts, the original raw 0.05 probe count, suspension paired flags, signed sideways speed, unbounded requested correction, actual applied correction, limit, reason, and requested/applied yaw delta. Velocity is blocks/tick, correction is blocks/tick², yaw is degrees/tick. The old raw and paired counts are diagnostic comparisons only.

Reasons are `not_opted_in`, `grip_disabled`, `no_wheels`, `no_contact` (airborne or unsupported wheels), `no_sideways_speed`, `invalid_input`, and `applied`. An excluded/disabled vehicle may show a hypothetical formula request but its **applied** correction is always zero. The current snapshot is also available through `MCH_EntityTank.getCarGripDiagnostic()`; disabled vehicles return null. File failures disable CSV writes and remain inspectable through `MCH_CarGripDiagnostics.getWriteError()` without log spam or interrupting physics. Turn diagnostics off after capture.

## Bundled eligibility

Exactly **22** runtime definitions opt in; matching `configreference/tanks` definitions mirror the relevant fields:

`2102`, `2105`, `350z`, `ae86`, `altis`, `bcnr33`, `bnr32`, `bnr34`, `bugattichiron`, `carrera_gt`, `challenger`, `dacia`, `delorean`, `fresh_auto`, `impreza`, `phantom`, `rx-8`, `rx7`, `s15`, `silvia_s14`, `starion`, `w123`.

`fresh_auto` is an unarmed civilian drift-car build, so it opts in without claiming stock racing tires. There are 74 bundled `WeightType = Car` definitions; the other 52 retain legacy handling. Explicit exclusions include `bm21`, `bnr32_police`, `fordpolice`, `phantomarmored`, `mc_atv_normal`, `opel_blitz_fuel`, `toyota_unarmed` and all armed Hilux variants, military trucks/utility vehicles, launchers, and military buggies. A car body, `Category = C`, or an unarmed loadout alone does not opt any of them in. Horns, backfire and drift effects in civilian definitions are not armed conversions. Tracked tanks, other military vehicles, aircraft and boats receive no opt-in edits.

## Stock identity and tire sources

The bundled `ae86` is treated as a **representative stock 1983 Toyota Corolla Levin GT APEX**, as requested. Its 1983 `TechYear` stays unchanged. [Toyota's period launch release, dated May 12, 1983](https://www.toyota.co.jp/jpn/company/history/75years/vehicle_lineage/car/id60003763/news/60003763.pdf), pp.12–13, identifies 2/3-door GT APEX grades but the inspected specification tables do not establish their standard tire size. Later databases commonly report 185/70HR13, while [Toyota's retrospective GAZOO listing](https://gazoo.com/catalog/maker/TOYOTA/COROLLA_LEVIN/198301/990003040/) conflicts on rim size. A reliable period tire fitment was not verified, so **both tire fields remain unset and grip stays enabled**; neutral response does not imply generic factory tires.

The bundled `challenger` is explicitly treated as a **stock 2018 Dodge Challenger R/T**, and **`TechYear` changes from 1970 to 2018** to match this chosen definition identity. [Dodge's official 2018 Challenger specifications](https://www.media.stellantis.com/uploads/me/ME/2018/Dodge/Technical-sheet/1806_Dodge_Challenger.pdf), p.13, lists **P245/45R20** all-season performance tires as standard for R/T (stored as `245/45R20` front and rear); pp.15–16 identifies the standard 20×8-inch R/T wheel. The separate SRT/Hellcat table and the model-credit URL are not evidence for R/T tires or handling. No tire compound-specific gameplay coefficient is inferred.

### Verified dimensions stored (8 definitions)

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

The remaining 14 eligible definitions have unset sizes. Their identities/trim/market do not establish a verified period front/rear fitment; existing research gaps are not filled with wider tires to tune handling. The seven previously sourced dimensions are unchanged.

## Verification

Executed `gradlew.bat compileJava test --offline --no-daemon` on 2026-09-26 with the existing cached Gradle/JDK environment: **BUILD SUCCESSFUL**, **49 tests, zero failures/errors**. The six changed/new production classes have class-file major version **52 (Java 8)**. The asset audit confirms the exact 22 eligible definitions in both trees, all bundled diagnostics disabled, and every existing `WeightType`/`Category` value unchanged.

Regression coverage includes real wheel settling/contact, front/rear support loss, dead wheels and stale airborne contact, unmodified collision boxes, opt-in defaults/reload, zero-force diagnostics, bounded forces/size response, low-speed continuity, forward/reverse sustained turns, actual server correction and yaw limits, energy/forward-speed preservation, and the exact bundled eligibility/exclusion list.

A headless collision/physics regression is not an in-game multiplayer playtest. The opt-in CSV makes flat-road driving, slopes, jumps, braking and networked interpolation observable in a running Forge 1.7.10 session without changing normal console output.
