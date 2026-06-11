# MCHeli Vehicle Naming Conventions

This codebase historically used `Aircraft` for several classes that are not flight-specific. Those classes are shared vehicle infrastructure used by helicopters, planes, ships, tanks, turrets/static weapons, seats, weapons, renderers, controls, and common state.

Use these names for new or refactored code:

- `BaseVehicle` for shared vehicle systems that apply to every rideable/controllable vehicle family.
- `Aircraft`, `Heli`, `Plane`, or `AirVehicle` only for logic that is actually flight-specific.
- `Turret` for the legacy `vehicles` config family, which represents static weapons/turrets rather than the shared vehicle base.

Compatibility rules:

- Keep existing config directory names, entity registration IDs, NBT keys, packet IDs, and public config keys unless an alias/migration path is added.
- The legacy `vehicles` config directory continues to load turret/static-weapon definitions through `MCH_TurretInfo`.
- Shared config parsing remains in `MCH_BaseVehicleInfo` so helicopter, plane, ship, tank, and turret definitions continue to use the same keys.

## Fixed-wing aerodynamic tuning

Realistic fixed-wing flight-model keys belong in the plane layer (`MCP_PlaneInfo` / `MCP_EntityPlane`), not in the shared `MCH_BaseVehicleInfo` base. Examples include torque/damping, stall, angle-of-attack drag, G-load, compressibility, overspeed, and level/dive energy tuning.

Plane configs may continue to use those keys with safe defaults when omitted. Helicopter, ship, tank, and turret/static-weapon configs should not parse or inherit those plane-only fields.
