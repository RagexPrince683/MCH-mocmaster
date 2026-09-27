# Civilian car terrain pitch

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

## Separate collision issue

`MCH_EntityTank.moveEntity` first clips vertical and horizontal movement. Its step
retry requires all of:

- positive body `stepHeight`;
- `onGround || (requestedY != clippedY && requestedY < 0)`;
- `ySize < 0.05` after its decay;
- horizontal movement clipped on X or Z.

Thus a car with wheel/suspension support but **body `onGround == false`**, whose
vertical movement is nonnegative or is downward without being clipped, does not
attempt a step. If horizontal collision clips movement at an otherwise reachable
stair, the affected `motionX`/`motionZ` is set to zero. Correct terrain pitch cannot
remove that stall. A focused collision fix must investigate that body-support gate.
The new test executes the actual existing `moveEntity` against a one-block rise
with `stepHeight = 1.2`: ungrounded zero/upward movement stops at the stair, while
body-grounded movement or downward movement clipped by the floor completes the
same step.
If a retry runs, its raised body still must clear the collision shapes and improve
horizontal distance; equal-or-worse progress restores the original clipped box.
The common collision collector also includes translated extra-body collision
shapes, so the complete body may be blocked even when a local wheel-point tread
has clearance. None of these collision paths are changed here.

## Expected in-game feedback

On a reachable higher front tread, the nose should rise; it should not point down
while climbing. When the front follows reachable lower terrain, the nose should
lower and retain that tilt while the rear remains higher. Matching axle terrain
heights should smoothly level the body. An impassable wall should bring the pitch
toward level. Suspension motion and roll keep their existing behavior, and no
vertical lift/fall correction has been added. Please distinguish a correct tilted
body that still stalls at a stair from the pitch behavior itself.
