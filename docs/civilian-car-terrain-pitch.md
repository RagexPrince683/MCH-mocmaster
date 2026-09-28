# Civilian car terrain pitch

The earlier sections record the pitch and body-collision work after `a94f5f0`,
including its historical fixture results. `fa6d246` subsequently introduced oriented
extra-body sweeps, uses a 45-degree terrain pitch limit, and removed the test
sources mentioned below. The final sections describe the oriented-primary correction
and diagonal collision/pose recovery; historical test results do not validate them.
The final straight-stair section supersedes the earlier terrain-seed, step-pose,
momentum-cleanup and client-prediction descriptions.

This change applies only to `CivilianCarGrip` vehicles. The checkout was clean before
the change. `MCH_EntityTank`, `MCH_WheelManager`, `MCH_RenderTank`, and the common
vehicle collision code were inspected before implementation. No wheel sampling or
movement changes from reverted commit `dc3ad8a21359594ffc74735873082c56c99ebdff`
are included.

## Previous behavior

`moveCivilianSuspension` calculated pitch from the front and rear averages of
**suspension-supported wheel entity positions**. Those positions reflect suspension
travel, body rotation, and compression, rather than independent terrain heights.
The formula was `atan2(frontHeight - rearHeight, wheelbase)`.

The vehicle model faces local +Z. `MCH_RenderTank.renderBaseVehicle` applies
`glRotatef(pitch, 1, 0, 0)`, which maps a +Z point to
`y = -z * sin(pitch)`. A higher front therefore needs **negative** pitch. The old
positive result lowered the nose on a climb; the negative result raised it on a
descent. The entity's `RotVec3(..., -pitch, ...)` transformation uses the matching
Minecraft vector convention.

Only wheels with `suspensionSupported` contributed. An axle without support was
assigned height zero, conflating missing terrain with an actual surface at Y=0.
No suspension support caused unconditional pitch decay. Even with support, pitch
could approach zero when the suspension-derived axle positions were equal despite
different terrain heights. These are separate from the sign error.

## Read-only terrain selection

The existing wheel definitions provide the sampling locations. Their local X/Z
coordinates are transformed by yaw and the pending horizontal body displacement.
Wheel entity positions, compression, contact flags, and previous pitch do not
select the terrain points. No additional wheels or moving probes are introduced.

At each point, a narrow column is queried from the current body collision-box
bottom minus configured `StepHeight` to that bottom plus `StepHeight`, with extra
space above for the existing body-box height. Blocks supply their collision shapes
through `addCollisionBoxesToList`. Individual stair parts, slabs, and other shapes
therefore contribute their actual top faces. Liquids and blocks without collision
shapes supply no terrain support. Unloaded columns supply no sample.

The highest top face within that vertical reach is selected only if the column
above it has clearance for the current body-box height. A buried stair tread,
a wall whose top exceeds `StepHeight`, or a reachable top underneath insufficient
headroom cannot become a valid step. A separate short forward clearance query
uses a **copy** of the unchanged body box: if even a collision-limited lift within
`StepHeight` cannot clear the obstacle, the desired pitch is level. This is a
geometry query, not the movement algorithm, and does not set `onGround` or step
the body.

Available terrain heights are averaged per axle. Missing heights are `NaN` and
are excluded; there must be a sample on both axles to calculate a terrain angle.
Actual Y=0 and negative heights remain valid. The desired angle is
`-atan2(frontHeight - rearHeight, axleSpacing)`, retaining the existing 18-degree
limit and client/server smoothing factors. If the old target has the opposite
sign, smoothing starts from zero so a valid climb cannot retain nose-down pitch.
Both sampled axles at the same height approach level. A supported sampled descent
continues toward nose-down pitch even when the suspension support flags are absent.
If an axle has no reachable terrain and no wall is detected, the existing 0.94
pitch decay is used instead of manufacturing a terrain height.

## Scope and verification

The new query only returns a pitch. It cannot write body position, velocity,
collision boxes, grounded state, or wheel suspension state. The wheel movement,
spring/damping force, compression, travel, render travel, roll calculations, and
update order remain in their existing paths. Applying a different body pitch still
feeds the existing body-relative transforms on subsequent ticks; this change does
not introduce an independent vertical correction or replace that existing coupling.
Non-civilian vehicles retain their previous path.

`MCH_CarTerrainPitchTest` covers render sign, opposite-sign transitions, sustained
descent, leveling, stair parts, slabs, exact step limits, clearance, walls, absent
samples, zero/negative heights, and the actual wheel layout. Its repeated query
test snapshots all body/wheel fields and their collision-box heights to check that
the terrain query does not mutate them.

Validation in this checkout: the Java 8-compatible runtime JAR assembled and was
reobfuscated successfully. The changed production classes in that JAR have class
major version 52 (Java 8). This checkout uses its existing modern Gradle daemon and
Jabel compiler to produce Java 8 bytecode; tests were explicitly launched on
Temurin Java 8 (`jdk-8.0.462.8-hotspot`). All 10 new tests passed. The complete
suite ran 59 tests with 58 passes and one existing failure:
`MCH_CarTireGripTest.onlyAuditedCivilianDefinitionsOptInAndReferencesMatch` expects
an opt-in set that excludes `fordpolice`, while the unchanged HEAD configuration
has `CivilianCarGrip = true` for that vehicle. No configurations or existing audit
expectations were changed to hide that failure.

## Civilian body collision correction after a94f5f0

The screenshots show throttle `1.00` and speed `0.00`, including a car already
pitched approximately `-16.60` degrees. This is evidence of stalled body movement
with corrected pitch, rather than evidence that more pitch is needed. The visible
obstacles include full dirt/grass blocks and cobblestone stair treads/risers. The
images do not provide block metadata or exact collision coordinates; the automated
reproductions below identify the blocking component and face at explicit coordinates.
In-game confirmation of the photographed layouts remains necessary.

### Where the old movement was lost

`onUpdate_Server` applies gravity and thrust, then runs `updateWheels`, lateral
grip, and `moveEntity`, followed by fall-damage handling. The pitch correction can
change the transform during the wheel update. That order and all suspension forces
remain unchanged. Body movement originally queried collisions, clipped Y, then X
and Z. A step required positive `stepHeight`, `ySize < 0.05`, blocked X or Z, and
`onGround || (requestedY != clippedY && requestedY < 0)`.

That support gate can skip a reachable step when the primary box is unsupported,
even though an extra body component or a current suspension wheel physically rests
on terrain. Zero/upward Y, or a downward request that is not clipped by the primary
box, cannot satisfy its second branch. A retry that **does run** is a separate case:
it can fail upward clearance or fail to improve horizontal squared distance. The
`result * result + minX * minX >= parX * parX + parZ * parZ` branch then restores
the normal clipped movement. Finally, `if(mx != parX) motionX = 0` and
`if(mz != parZ) motionZ = 0` discard velocity on blocked axes. This is the exact
cleanup branch that turns a clipped request into a persistent throttle/speed stall.

There were also geometry and step-accounting problems. The historical fixture used
a **0.85 x 0.85** primary box, `StepHeight = 1.2`, and these Starion extra boxes
(local center; width x height x depth):

| Component | Center | Dimensions |
| --- | --- | --- |
| Engine | (0, 0.50, 1.9) | 1.0 x 0.6 x 1.0 |
| Middle | (0, 0.20, 0) | 2.1 x 0.2 x 2.1 |
| Front | (0, 0.20, 1.8) | 2.1 x 0.2 x 2.1 |
| Rear | (0, 0.20, -1.6) | 2.1 x 0.2 x 2.1 |

Correction from the 2026-09-28 saved-world investigation: Starion's
`EntityWidth = 0.85` / `EntityHeight = 0.85` affect rider rendering, not the primary
collision dimensions. `Width` / `Height` are the collision fields; Starion leaves
them unset. Its effective primary is the tank constructor/default **2.0 x 0.7**.
The 0.85-primary numerical examples and historical fixtures below therefore do
not reproduce the effective primary collider in the reported game.

At level pitch and primary bottom Y=0, entity Y is 0.35: the constructor's
`yOffset` remains 0.35 after the configured primary size is applied. The front box
extends to Z=2.85 and its bottom is Y=0.45. A full block face at Z=2.90, top Y=1,
therefore clips a +Z request of 0.20 to 0.05 at the **front lower body**; clearing
it requires a 0.55 body rise, not a one-block rise of the primary box. The engine
box, whose top is Y=1.15, can independently block upward clearance under a low
ceiling even when the primary box has room. Stair fixtures use separate half-height
lower treads and upper risers; the next riser similarly catches the low front box.
The pitched fixture supports the rear box outside the primary footprint and raises
the front over the next reachable face.

`addExtraBoundingBoxBlockCollisions` formerly translated each obstacle by
`primaryCenter - extraCenter` and clipped the differently sized primary box against
it. That preserves center separation, but not face distances or overlap on the
other axes. At level pitch it misreports the front-face gap by 0.625 on Z and the
settling height by 0.325 on Y. It can miss an outer-edge wall, return an already
overlapping obstacle that offset clipping ignores, or settle above the actual tread.
A correct conversion would have to account separately for the component's minimum
and maximum extents, including their changes with rotation. The civilian path now
avoids this conversion entirely.

The legacy collector's `getCalculatedExtraBoundingBoxes` cache already invalidates
for pitch/roll changes; stale pitch is not established as the cause. The new path
builds freshly transformed **copies** before the first query, so it also avoids the
old query-then-update sequence and cannot use candidate transforms from another
retry. It retains the existing enclosing AABBs of every rotated body component.

The old retry ignored the returned upward offset and settled by the full configured
`-stepHeight`; then `parY` contained only that downward leg. Collision state,
vertical velocity cleanup and `updateFallState` compared/consumed that leg instead
of the completed rise plus settling. This could report a false vertical collision
and fall distance during repeated ascent.

### Completed correction and floating prevention

Only `CivilianCarGrip = true` tanks enter `MCH_CarBodyMovement`. Each axis sweep
queries world-space collision shapes separately for the primary and every current
extra AABB, takes the most restrictive offset, then translates all components by
that same offset. Blocks retain their actual slab/stair shapes. Existing entity
collision filters are reused through a narrowly scoped raw-component collector;
other vehicles continue using the original collector and movement algorithm.

A retry requires blocked horizontal movement and present physical support: a
1e-5-block downward contact query of the current body components, an actual clipped
downward normal movement, or a contact query at a current axle footprint. The wheel
check reads copies, recenters X/Z on the current body-relative axle rather than the
pending destination, bounds the wheel bottom to configured suspension travel, and
requires present collision contact. It reads no grounded/suspension flags and
rejects wheels left below the body. It does not search `StepHeight` below the car
for permission, borrow future support, or force `onGround` from a remembered flag.

The candidate rises by at most unchanged `StepHeight`, with the entire body limiting
ceiling clearance. It sweeps X and Z against the raised components, then settles
by **the actual permitted rise**. A step is accepted only if it lands on a reachable
collision surface, stays within the configured rise, leaves every body component
clear, and strictly improves horizontal squared distance. Otherwise the normal
movement wins. Walls, over-limit rises, and insufficient headroom stop blocked
axes. Fully permitted horizontal movement retains its velocity. The final Y result
is rise plus settling; collision state, vertical cleanup and fall distance use that
completed displacement. A successful landing supplies a real grounded contact.

This avoids the previous floating regression: no upward velocity, repeated height
nudges, movement segmentation, box shrinking, or extra-collision bypass is added.
Unblocked movement cannot trigger a step; unsupported cars cannot climb; a candidate
without a landing is rejected; and settling uses real component bottoms rather than
center-remapped primary-box clearance. Wheel movement, `MCH_EntityWheel`, suspension
forces/damping/compression/travel, pitch smoothing, roll behavior, and update order
are unchanged. No wheel changes from reverted `dc3ad8a` are restored.

### Verification and remaining feedback

The original pitch test's collision fixture has `extraBoundingBox = new
MCH_BoundingBox[0]` and no civilian configuration. It still exercises the untouched
legacy step gate; it is not proof of full Starion clearance. The separate
`MCH_CarBodyMovementTest` covered Starion's four extra boxes, step height,
suspension travel, and wheel layout. The earlier description here incorrectly
identified the rider dimensions as the real primary dimensions. It exercises
the production `moveEntity` and collision collector against explicit full blocks,
stair parts, outer-edge walls, engine ceilings, rear support at -16.60-degree pitch,
new pitch transforms, stale body/wheel flags, clipped upward clearance, vertical
accounting, exact step limits, landing at the original floor height, and repeated
ascent. All 14 new body tests and all 10 terrain-pitch tests pass on Temurin Java 8
(`jdk-8.0.462.8-hotspot`). The complete suite ran 73 tests: 72 passed and the same
existing `MCH_CarTireGripTest.onlyAuditedCivilianDefinitionsOptInAndReferencesMatch`
audit failed because unchanged `fordpolice` opts in but its expected set omits it.
No configuration or audit expectation was changed to hide that failure.

The existing Gradle `test assemble --continue` tasks were run offline with a local
init script selecting Java 8 for tests and the existing modern daemon/Jabel compiler.
`jar`, `reobfJar`, and `assemble` completed; the combined command returns failure
because of the audit test above. The changed production classes have class major
version 52 (Java 8). The runtime output is
`build/libs/mcheli-a94f5f0-MCHRgithub+a94f5f032f-dirty.jar`. `git diff --check` passes.

In-game feedback is still required for the photographed block/stair layouts,
rotated approaches, low ceilings, sustained acceleration, takeoff/landing, and
suspension behavior across repeated stairs. Compilation and shape-fixture tests
cannot prove live stair climbing. Expected behavior is preserved nose-up terrain
pitch with forward movement over reachable steps, settling onto their surfaces,
and a stop at real walls or insufficient clearance, without accumulating height
while stationary or airborne.

## Brief stop before full-block ascent after fa6d246

This correction is confined to the `CivilianCarGrip = true` body movement path.
The existing stair step sequence is retained. Travel direction, terrain pitch, roll, wheel movement,
suspension, update order, and vehicle definitions are unchanged. No test files were
created or restored.

### Confirmed branches and remaining runtime uncertainty

`moveCivilianSuspension` applies the smoothed terrain pitch before `moveEntity`
constructs the current oriented extra boxes. Rotation can therefore introduce body
overlap before translation. The `!clear(normal, collisions)` branch cancels both
horizontal offsets when that overlap survives the normal Y/X/Z sweeps. That guard
remains: an uncleared body candidate is not permitted progress. A supported step
still has to clear every component. The screenshots do not establish which box was
overlapping on a particular tick.

There is also a definite landing mismatch independent of overlap. The normal Y
sweep can move downward and reach support before the horizontal sweep is blocked.
Previously the step retry discarded that completed Y movement, started at the
original higher pose, and settled only by its own rise. It could clear the block
horizontally but remain above the reachable tread. `supportedBody(step, collisions)`
then rejected it and returned normal movement, often with zero horizontal progress.
This is a rejected step, rather than an accepted climb losing speed afterward.

For example, consider the configured Starion boxes at yaw/roll zero, pitch -6,
entity Y=0.65, Z=0.20, above a floor at Y=0 with a row of full blocks occupying
X=[-4,4], Z=[3,4], Y=[0,1]. With requested Y=-0.57 and Z=0.12, the front underside reaches
the blocks during the downward sweep (approximately -0.045 Y), then blocks Z.
The old raised candidate permits Z=0.12 but settles back to the original Y=0.65,
where it has no landing contact. Starting from the completed downward sweep instead
lands at approximately Y=0.618 while permitting the full Z request. These numbers
illustrate the geometric branch using all configured body components; they are
not telemetry from the photographed run or a gameplay test.

The strict `netY >= 0` / `netY <= stepHeight` comparisons were inspected. The old
settling request was `-rise`, so its clipped result could not be more negative than
that request: `rise + down` stayed between zero and the permitted rise. Floating
point error in those bounds is not established as the pause's cause. The rejected
landing above is a mismatch of starting positions. The completed displacement can
legitimately be negative when the car descends onto support while stepping forward.

Separately, both accepted partial steps and microscopic clipping differences reached
`if(mx != parX) motionX = 0` / `if(mz != parZ) motionZ = 0`. Those exact comparisons
erased an entire velocity component even if settling had cleared the clipping
contact. This is a successful-step cleanup failure, distinct from the landing
rejection. Pitch sampling and smoothing continue while stationary; changes in pose
and present support can make a later candidate pass, after which thrust rebuilds
the erased speed. These code paths explain a possible stop/resume sequence; which
path dominated the reported pause still requires in-game feedback.

### Same-update landing and momentum

The retry now retains the completed downward Y sweep as its starting pose. Positive
Y requests keep the original retry base. It still needs blocked horizontal movement
and current physical body/wheel support or support reached by the downward sweep.
It lifts by at most `StepHeight`, clips the raised X/Z movement, then settles by
the actual permitted rise. The final Y displacement includes the completed descent,
rise, and settling; collision state and fall accounting consume that displacement.
A landing, full-body clearance, improved horizontal progress, and the configured
rise bound remain mandatory. Existing stair retries with no downward displacement
use the same starting geometry as before.

For a meaningfully clipped horizontal axis, the resolver checks a 1e-5-block probe
in the requested direction at the final accepted body pose. Velocity is removed
only if that axis is still blocked there. A raised-sweep contact that disappeared
after settling no longer erases permitted momentum. Fully permitted axes retain
their existing velocity; this adds no acceleration or boost. Real walls and low
ceilings still limit candidate translation/rise, and remaining wall contacts still
clear velocity directed into them.

Movement comparisons, support/progress decisions, and SAT contact classification
use the existing 1e-7-block geometric tolerance. SAT entry time is a fraction of a
movement request, so it is no longer compared to that distance tolerance. Instead,
near-contact classification uses penetration along normalized SAT axes, permits
escape, and blocks motion into the contact. The tolerance does not excuse a real
penetration: final `clear` remains required. No rise-limit tolerance is added.

Floating prevention is preserved: no stale wheel flags or distant terrain grants
permission, no candidate without a landing is accepted, and no impulse, height
nudge, forced grounded state, box shrinking, or collision bypass is introduced.
The military/other-vehicle path retains its original movement and exact cleanup.

### Current validation

`gradlew.bat compileJava --offline --no-configuration-cache` passed using the
existing Gradle cache and configured Java 25 daemon/Jabel compiler. The three
changed production classes have class major version 52 (Java 8). This verifies
compilation against the existing project dependencies and bytecode target; no test
files were created/restored and no gameplay checks were run. `git diff --check`
passes. Final scope review contains only the two civilian collision helpers,
`MCH_EntityTank`'s civilian result handling, and this document.

Gameplay verification of the reported pause, sustained block/stair ascent, wall
stops, and headroom remains with the user's feedback. Compilation alone cannot
verify that the pause is gone or that live staircase climbing is preserved.

## Oriented civilian primary collider — 2026-09-28

The user identified the static blue primary box in the reported game. Read-only
inspection of `run/client/saves/___/region/r.3.-4.mca` identifies the vehicle as
`starion`, texture `starion`, at (1781.9936197, 12.349999994, -1537.6528281), yaw
2.2108634, pitch -41.8111763, roll 0, zero saved motion, and `OnGround = true`.
This matches the screenshots. The saved nearby collision terrain is full grass/dirt
blocks; the adjacent stone stairs are not the blocking shape at this pose.

The confirmed geometry error is that `moveEntity` put the unpitched primary AABB
into the compound SAT sweep while transforming the extra boxes. A collision-equation
reconstruction using the saved blocks, effective 2.0 x 0.7 primary, configured extra
boxes, and Minecraft's float/trigonometric conventions clips a -0.617 downward
request to zero at the primary's flat bottom. None of the extra boxes initially
penetrates a saved block. With the primary transformed about the same vehicle
origin, the same request permits -0.4302565 Y, then stops at an actual rotated face.
These are saved-runtime geometry calculations, not execution of a game tick.

For `CivilianCarGrip` vehicles, `getPrimaryBoundingBox()` now supplies a cached
oriented primary with the entity's actual width/height and original local center
(`height/2 - yOffset + ySize`). The primary and extras use the same SAT movement
resolver. The ordinary entity AABB encloses that primary for vanilla compatibility;
it is not used as the civilian car's primary block collider. Position is advanced
from the vehicle origin by the accepted XYZ displacement, never recovered from the
enclosing minimum Y. Position/pose updates and definition changes refresh the bounds;
removing the opt-in restores the legacy AABB. Primary intersections, damage rays,
and the blue debug model use the oriented volume. Legacy tanks retain their path.

Terrain selection, suspension anchor correction, grip normalization, and level-road
render calibration use the explicit **unrotated chassis floor**, not the new
envelope's lowest corner. Suspension travel, grip reach/skin, contact gates, drive
force, gearing, speed limits, and brakes are unchanged. All body dimensions and
solid-block collision sweeps remain present. Step support, rise/headroom limits,
landing, final clearance, and the normal overlap-rejection guard remain required.

Missing wheel support is a separate remaining question: at the saved pose the
nominal wheel anchors are beyond suspension reach of their respective surfaces.
That is consistent with a chassis held above its wheels; the RWD car cannot gain
propulsion from unsupported rear wheels. The existing CSV is an earlier Chiron
capture and contains no Starion pre-drive or movement trace. High RPM alone does
not establish applied drive force. The saved pose does not demonstrate an embedded
extra box rejecting both travel directions, and it does not prove that contact will
return or that driving will escape after the geometry correction.

Validation: `gradlew.bat compileJava --offline --no-configuration-cache --no-daemon
--gradle-user-home C:/Users/Owner/.gradle` passed with the existing cached Java 25
launcher/Jabel compiler and Forge 1.7.10 dependencies. All six changed production
classes have class-file major version 52 (Java 8). Read-only world/NBT inspection,
collision-equation reconstruction, lifecycle/call-site review, and `git diff --check`
were performed. No test sources, fixtures, temporary repository files, diagnostic
capture, packaging, reobfuscation, game launch, or in-game fix verification were done.

### Driving feedback for the corrected build

Release Space and pedals first, allow the body to settle, then test each case:

| Case | Feedback to check |
| --- | --- |
| Pictured ledge, forward | Blue primary rotates with the chassis; W in gear 1 makes progress when driven-wheel contact and full-body clearance permit it. Report continued RPM-only stalls. |
| Pictured ledge, reverse | Hold S through the normal direction change to R; check descent/backward progress and rear contact, without clipping through the grass edge. |
| Level ground | Forward/reverse launch, stopping height, wheel position, and released-pedal settling retain their previous behavior. |
| Full blocks | Reachable steps within configured StepHeight climb only with current support, landing, and clearance; over-height rises stop the car. |
| Stone stairs | Both ascent and descent follow individual treads; stop/restart and reverse without hanging on the primary's old flat bottom. |
| Solid wall | Face the wall in forward and reverse runs: motion into it stops, with no body penetration or unsupported step over it. |

The photographed stuck position was recovered from the save, but was **not
reproduced or verified fixed in-game** during this work. Full release, wheel-contact
recovery, stair behavior, ceiling clearance, and multiplayer interpolation need
feedback from the corrected build.

## Diagonal corners, supported roll and bounded recovery — 2026-09-28

### Confirmed evidence and remaining hypotheses

The latest saved runtime state in `run/client/saves/___/region/r.3.-4.mca` contains
a Starion at (1769.4165221, 4.64182984, -1923.6273878), yaw -187.75746, pitch 0,
roll +17.999998, and `OnGround = true`. Reconstructing its configured wheel anchors
and the saved full grass/dirt collision surfaces gives two low-side wheels supported
at Y=4 and two high-side anchors outside suspension reach. The old roll calculation
substitutes Y=0 for the unsupported side, so the desired roll clamps to +18 degrees
again. This is a confirmed self-maintaining roll trap on level terrain, rather than
evidence that the terrain itself requires that roll.

The same saved region contains a stationary Chiron at
(1782.0373094, 5.4117375, -1661.0832613), yaw 3.576782, pitch 8.130101, roll -6.387097.
Its configured front underside components overlap the saved full block at
(1782, 4, -1659). Applying pitch/roll before translation can introduce this overlap;
previously the normal candidate rejected horizontal movement while still permitting
an overlap to be interpreted as a downward landing. The save confirms the overlap,
but does not record the earlier tick that introduced it.

Separately, a collision-equation corner check with the unchanged Starion components,
yaw -45, origin (-1.2, 0.349999994, 1.32), level floor Y=0, and a full block occupying
X/Z=[0,1], Y=[0,1] demonstrates axis-order clipping. For requested X=Z=0.12 and
Y=-0.617, X then Z permits (0.035075852, 0.12); Z then X and the direct diagonal
sweep permit (0.12, 0.12). The old raised retry succeeds in this example, but is
unnecessary: the diagonal path is already clear. This establishes the ordering
limitation, not a captured in-game failure of the retry. Opposite travel and mirrored
approaches are feedback cases below.

The existing grip CSV contains only earlier Chiron lateral/contact samples, with
no current movement, step, engine force, or RPM trace. High RPM alone cannot prove
that drive force was applied, that contact was absent, or which candidate failed.
The contribution of sweep order to the reported stall, the exact stair-to-hole
sequence, client prediction effects, and the original overlap-producing tick remain
runtime hypotheses. The diagonal failure has **not been reproduced or verified fixed
in-game** during this task.

### Current authoritative rules

- Roll uses measured collision-surface heights and requires samples on both sides.
  With either side missing, it decays toward level instead of inventing a height.
  The existing 18-degree limit and spring/damping tuning are retained.
- The server treats suspension pitch/roll and incoming yaw as candidate angles.
  Pitch/roll advance by at most 2 degrees per tick, with smaller fractions when
  clearance requires them. A conservative midpoint volume encloses the continuous
  angular path, including the Minecraft trigonometric lookup error allowance.
  Pure level yaw does not gain a vertical envelope merely because it turns.
- A supported pose transition may sweep up at most 0.1 block, rotate only with
  clearance throughout its angular path, then sweep down by that actual rise.
  A ceiling clips the lift; missing support forbids it. No position or angle is
  reset to a presumed road height, and gravity still resolves the final descent.
- An already embedded body may recover upward by at most 0.1 block per tick only
  when every existing overlap has an outward separating face within `StepHeight`.
  That separating distance decreases throughout the movement. Non-overlapping
  obstacles still clip the entire sweep. A blocked ceiling or an overlap with no
  outward upward exit prevents recovery. Until the start is clear, ordinary travel
  and stepping are rejected, and rejected horizontal velocity is removed.
- Normal and raised horizontal candidates compare X→Z, Z→X, and continuous
  diagonal SAT sweeps. A fully permitted first path avoids unnecessary retries.
  Raised paths are compared after settling and must improve progress, land on
  actual support, and leave every body component clear. Recovery/pose rise is
  deducted from the remaining step budget for the same tick.
- Final position includes pose/recovery Y and resolved translation. Fall accounting
  and vertical cleanup use that total. Wheels are reconciled to this accepted
  position/pose without another spring impulse, so rejected predicted movement
  does not supply the next tick's wheel locations. An identical current/final wheel
  anchor needs only one suspension query.
- Completely rejected travel clears its residual velocity. With wheel
  contact and either brake held, horizontal residue below 1e-5 block/tick snaps to
  rest. Engine force, inertia, traction limits, direction-change dwell, gears,
  speed limits and brake forces are unchanged. The military movement branch is
  unchanged; all new body rules require `CivilianCarGrip`.

### Diagnostics and feedback

Opt in with the existing per-vehicle `CarGripDiagnostics = true` setting and restart
or use the existing definition reload. The server now also writes
`logs/car-body-movement.csv`: W/S, gear, throttle, RPM, brakes, pre-drive front/rear
contact and applied drive force; requested movement; pose displacement and angles;
normal/raised candidates; blocked axes, landing, reconciled contact, final position
and velocity. This is optional diagnostic I/O, disabled in unchanged definitions.
Pre-drive contact -1 and force NaN mean no drivetrain sample was available.
`paths` entries contain `stage:x|y|z|rise|landing|clear`, separated by semicolons.
Candidate order 0 is X→Z, 1 is Z→X, and 2 is direct diagonal; straight or fully clear
requests omit redundant candidates. `rise` records permitted headroom even when
no raised candidate can run. `embedded` records rejection pending recovery.

Use the same vehicle and surface for each feedback case, release Space before
launching, and capture the movement CSV when a failure occurs:

| Case | Feedback to check |
| --- | --- |
| Diagonal, left wheels leading | Cross a full-block corner at about 45 degrees in W and then S/R. Confirm progress with driven-wheel contact, bounded roll and no body penetration; repeat slowly and at ordinary road speed. |
| Diagonal, right wheels leading | Mirror the course and repeat forward/reverse. Report a directional difference and capture both candidate axes, step landing and final position. |
| Straight crossing | Ascend/descend the same reachable block edge squarely. Existing step reach, speed and throttle response should remain; an over-height rise must stop travel. |
| Stairs → hole → level | Descend the original stairs into the hole, return to level terrain, release pedals, then launch with W and S. Body and wheels should settle, with no persistent RPM-only stall. |
| Level-ground recovery | Return a rolled car to a flat supported surface, stop, and let it settle. Expect gradual level pose and restored contact; apply each brake and check that residual drift stops. No instantaneous height/angle reset should occur. |
| Unsupported drop | Drive off an edge with no reachable wheel/body support. It must fall normally, gain no recovery/step lift in free space, and gain no tire drive force without contact. |
| Solid wall / low ceiling | Hold W, test S/R, and brake against an unstepable wall. The body must stop at contact; reversing along a clear supported path should respond normally. A low ceiling must reject unsafe rise/rotation/recovery. |

### Checks actually run

Read all six requested implementations, their server/client ordering, raw component
collision collector, pose packet handler, definitions, existing diagnostics and
physics documentation. Read the saved NBT and nearby full-block terrain without
modifying the world. Ran in-memory collision-equation checks, not test classes or
game ticks: both travel signs and mirrored diagonal corners; X→Z/Z→X/direct comparison;
straight supported ascent (0.12 horizontal, 0.550000007 Y); held wall contact;
low-ceiling rejection; unsupported descent (-0.617 Y, no step); shallow overlap
escape with ceiling rejection; and 101 samples of an accepted angular envelope.
An additional Chiron corner calculation at (-2, 5.39, 0.85), yaw -45, over a floor
at Y=4 and a full block X/Z=[0,1], Y=[4,5], has one supported right-front wheel
and no supported left wheels. The diagonal request (0.12, -0.617, 0.12) remains
fully permitted. The unchanged stationary AWD launch equation with one contacting
front tire gives +0.004105134 block/tick², with zero force from the unsupported axle;
this is an equation check, not a captured engine tick.
The saved Starion reconstruction reached roll 0, Y=4.349999994 and four supported
wheels by tick 21; the saved Chiron overlap cleared in four outward moves totaling
0.315857737 Y. These are mathematical reconstructions, not in-game fix verification.

`gradlew.bat compileJava --offline --no-configuration-cache --no-daemon
--gradle-user-home C:/Users/Owner/.gradle` ran with `GRADLE_USER_HOME` set to the same
existing cache, using the configured Java 25/Jabel compiler and Forge 1.7.10
dependencies. The first default wrapper invocation could not create its lock under
`C:\.gradle`; the explicit existing cache resolved that environment issue. Final
compilation, bytecode and diff results are recorded in `CHANGELOG.md`. No test
classes, fixtures, temporary repository files, configuration tuning, game launch,
packaging or reobfuscation were added or performed. The seven driving cases,
dedicated-server operation and multiplayer pose interpolation need runtime feedback;
the conservative angular envelope can limit rotation near tight obstacles.

## Straight stairs and slab transitions — 2026-09-28

### Supported causes and limits of the evidence

The supplied screenshots show throttle 1.00, nearly level pitch and horizontal
motion around 0.15–0.16 block/tick. The car HUD's 91/100 or 100/100 is HP, and its
tachometer is synchronized engine RPM. The debug `speed` expression is the length
of XYZ velocity, not measured horizontal displacement; with the pictured zero Y
velocity it approximately equals horizontal velocity. Throttle/RPM alone do not
prove tire force or accepted body travel. The drivetrain reads signed horizontal
velocity along yaw and collision-derived driven-wheel contact.

Source inspection and straight collision-equation calculations identify three
related failure paths. These calculations use Starion's effective 2.0 x 0.7 primary,
four configured extra components, actual axle coordinates and StepHeight 1.2. They
are not captured game ticks or a reproduction of the exact photographed layout.

- The pitch walk starts at the level chassis reference near its center. On a
  staircase a flat car can be supported by its front underside while that center
  surface and the rear axle lie more than StepHeight below the reference. The
  front axle remains directly reachable, but the old fallback does not walk from
  it to the rear. A missing rear sample returns NaN and decays pitch toward level.
- The front lower component can touch the next vertical riser. Its conservative
  angular envelope extends into that riser even for a small nose-up rotation;
  the independent 0.1-block pose lift cannot clear a half-block riser. A later
  supported step has more clearance, but previously retained the flat pose.
- A faster request can accept a partial raised step, land on a tread and still
  touch the next reachable riser. The final contact probe classifies that axis as
  blocked and erases its whole velocity, despite successful climbing progress.
  This is distinct from an invalid landing or an unsupported drivetrain axle.

For an illustrative half-tread staircase with tops rising 0.5 every 0.5 Z, a level
Starion at Y=2.9, Z=0.15 rests its front underside at Z=3, Y=3. The level chassis
floor is 2.55, center tread 0.5, front axle tread 2.5 and rear axle tread -1.0.
Only the front axle is directly within reference reach. Walking adjacent treads
from it supplies the rear height and a desired pitch capped at -45 degrees.
With a 1.4-block forward request, the previous step resolves about 1.0 Z and
1.0 Y, then clears forward velocity at the next riser. Raised rotation resolves
about 1.005226 Z, 0.918058 Y and -2 degrees; the next-riser clearance check retains
velocity. Movement still obeys the per-update rise budget and can be shorter than
the retained velocity request. The HUD speed is not a promise of full requested
displacement on that update.

### Current movement rules

`MCH_WheelManager` retains the center-to-axle walk and direct axle queries. If an
axle is still missing, a directly reachable axle can seed a bounded walk over
actual collision surfaces to it. Every sample retains StepHeight and column
headroom checks; a missing surface terminates that path. Terrain samples propose
angles only and never grant physical wheel contact, step permission or propulsion.

`MCH_CarBodyMovement` compares unchanged raised-body sweeps with a candidate
rotated at the same permitted lift before horizontal translation. The angular
envelope and final rotated body must clear all primary/extra components. Both
variants must improve horizontal progress, settle by their actual lift, land on
physical support, remain within the existing rise budget and finish clear. Equal
progress may select the valid terrain pose. `MCH_EntityTank` commits candidate
angles only when that complete path is accepted; total pitch/roll changes stay
within the existing two-degree update limit. No yaw or lateral movement is added.

After an accepted partial step, a blocked-axis probe may retain velocity only if
a copy of the landed body can lift within the same step limit, advance a 1e-5-block
probe on that axis, settle onto support and remain fully clear. This query grants
no further movement or height in the current update. Walls above the step reach,
insufficient headroom and invalid landings still remove blocked velocity. Ordinary
blocked movement, embedded-start rejection and unsupported movement retain their
existing rules. Stationary movement cannot trigger a step or this continuation.

Client extrapolation now uses the same civilian pose and compound-body resolver.
Authoritative position interpolation remains unchanged; the local pilot also
interpolates the server's accepted pitch/roll instead of retaining unchecked wheel
angles. Direct suspension/frame-angle application is removed for civilian cars.
Both sides reconcile wheels to the accepted position/pose without another spring
impulse. Engine/control authority, contact gates, tire grip, gearing, speed limits,
definitions, persistence and the military movement branch are unchanged.

### Verification and remaining gameplay checks

Completed source review covers throttle/control and drivetrain contact, wheel
prediction, terrain selection, oriented primary/extras, angular clearance, normal
and step sweeps, landing, velocity/fall cleanup, pose packets and interpolation,
HUD values, and changes from fa6d246 through 2d1900bce344c7560217c0928f832a9456fc9f15.
Read-only save inspection found no car at the photographed stopped coordinates;
the exact failure tick and block metadata remain unobserved.

In-memory straight collision-equation checks covered slow and partial fast stair
climbs, slab-to-full-block ascent, level travel, walls, an over-height step, an
engine-limited ceiling, an unsupported drop, and a valid partial climb ending at
an unstepable wall. Eight repeated fast stair requests retain forward velocity,
advance pitch to -16 degrees and finish clear with each Y rise below 1.2. Twenty
subsequent stationary requests gain zero height. These calculations approximate
the straight SAT geometry; they do not execute Minecraft, the drivetrain or network
ticks, and they are not test classes or gameplay verification.

`gradlew.bat compileJava --offline --no-configuration-cache --no-daemon
--gradle-user-home C:/Users/Owner/.gradle` passed with the existing Java 25/Jabel
setup and Forge 1.7.10 dependencies. All three changed production classes have
class major version 52 (Java 8). The sandboxed attempt could not open the existing
Gradle wrapper lock; the same task passed with authorized cache access.
No test sources, fixtures, harnesses, debug mods, tuning changes, packaging,
reobfuscation or game launch were added or performed.

In-game confirmation remains with the user: straight ascent/descent and stopping
or restarting on the pictured stairs and slab transition; sustained throttle and
wheel contact after returning to level ground; wall, over-height and low-ceiling
stops; stationary/airborne height and lateral drift; dedicated server and local/
remote-player prediction. The exact photographed obstacle and the conservative
angular envelope near tight headroom remain runtime uncertainties.
