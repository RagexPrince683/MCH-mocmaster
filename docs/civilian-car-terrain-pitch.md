# Civilian car terrain pitch

The earlier sections record the pitch and body-collision work after `a94f5f0`,
including its historical fixture results. `fa6d246` subsequently introduced oriented
extra-body sweeps, uses a 45-degree terrain pitch limit, and removed the test
sources mentioned below. The final section describes the current pause correction;
those historical test results are not validation of this correction.

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

There were also geometry and step-accounting problems. Starion configures an
unchanged **0.85 x 0.85** primary box, `StepHeight = 1.2`, and these extra boxes
(local center; width x height x depth):

| Component | Center | Dimensions |
| --- | --- | --- |
| Engine | (0, 0.50, 1.9) | 1.0 x 0.6 x 1.0 |
| Middle | (0, 0.20, 0) | 2.1 x 0.2 x 2.1 |
| Front | (0, 0.20, 1.8) | 2.1 x 0.2 x 2.1 |
| Rear | (0, 0.20, -1.6) | 2.1 x 0.2 x 2.1 |

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
`MCH_CarBodyMovementTest` parses Starion's real body dimensions, four extra boxes,
step height, suspension travel, and wheel layout from the configuration. It exercises
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
