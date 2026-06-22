# Frame-rate independent vehicle controls

MCHeli simulation code must treat Minecraft's 20 Hz game tick as the authority for vehicle physics. Client render callbacks may arrive at 30, 60, 144, 240, or more frames per second, so render partial ticks must not be used as raw control authority.

## Fixed-tick control rules

- Aircraft, helicopter, ship, tank, and turret control code should convert render callback deltas with `MCH_FlightModel.getBoundedTickDelta` before integrating pitch, yaw, roll, turret rotation, recoil, or stabilization.
- Per-tick damping that can be reached from the render loop should use `MCH_FlightModel.decayPerTick` so multiple high-FPS render frames produce the same decay as one full tick.
- Legacy client prediction may still run from the render callback, but only with an elapsed tick fraction. The server receives the same resulting orientation packet stream without high-FPS clients gaining additional authority.
- Debugging can be enabled with `DebugFlightControl=true` to print FPS, elapsed tick fraction, control inputs, angular velocities, and pitch/yaw/roll once per second while piloting.

## Visual-only interpolation

The following systems intentionally remain frame-interpolated because they affect only presentation and should stay smooth between fixed simulation updates:

- Entity/model rendering that uses `calcRotYaw`, `calcRotPitch`, `calcRotRoll`, previous rotation fields, or `tickTime` to draw an interpolated pose.
- LOD display snapshots in `MCH_VehicleLODManager`, which interpolate remote display positions and angles for far-distance rendering only.
- GUI/drafting-table preview rendering, particles, camera roll display, and rider render-position setup.
- Network position interpolation fields such as `aircraftPosRotInc` and vanilla entity previous/current position interpolation.

If a later change uses render interpolation to mutate authoritative rotation, control inputs, angular velocity, recoil, or stabilization, move that calculation back to fixed-tick code or feed it only a bounded elapsed tick fraction.
