# MCHeli Overdrive feature overview

This page summarizes the major project-wide systems that distinguish MCHeli Overdrive from the original MCHeli codebase. It is written for players, server owners, and pack makers who need to understand what this fork adds, why each system exists, and which configuration surfaces control it.

## Content expansion

### Expanded vehicle roster and roles

**What it does:** Overdrive supports a much larger combined-arms roster than original MCHeli. The repository's `configreference/` tree contains hundreds of reference vehicle configs across fixed-wing aircraft, helicopters, ships, tanks, and other vehicles, and the loader continues to support larger external MCHeli-style asset packs.

**Why it exists:** Overdrive is intended for warfare, faction, and survival packs that need more than a small aircraft set. A pack can include strategic bombers, UAVs, gunships, transports, naval vessels, submarines, armored vehicles, civilian vehicles, logistics vehicles, turrets, carriers, and support platforms.

**How it differs from original MCHeli:** Original MCHeli focused primarily on aircraft and a smaller content set. Overdrive treats multiple vehicle categories and battlefield roles as first-class content targets while preserving the MCHeli text-config format.

**Configuration:** Vehicle definitions remain data-driven under `assets/mcheli/` or compatible external content packs. Pack makers use the shared `MCH_BaseVehicleInfo` keys plus type-specific guides in `docs/vehicle-config/` for planes, helicopters, ships, tanks, turrets, and base vehicle behavior.

## Weapons and combat systems

### Expanded weapon roles and guidance systems

**What it does:** Overdrive keeps MCHeli's weapon-definition format and expands battlefield roles for anti-air, anti-armor, anti-ship, close-air-support, torpedo, guided-weapon, bomb, rocket, cannon, dispenser, chemical/nuclear-compatible, and support-equipment loadouts. The framework is designed for very large weapon catalogs in external content packs, including packs that scale to hundreds or thousands of weapon definitions.

**Why it exists:** Combined-arms packs need more than generic rockets and guns. Vehicles need role-specific payloads, lock behavior, radar identity, countermeasures, naval weapons, support dispensers, and strategic payloads.

**How it differs from original MCHeli:** Overdrive adds or documents richer missile guidance behavior, torpedo/dispenser paths, lockable entity synchronization, RWR/radar identity, flares/chaff separation, active protection support, and HBM-compatible explosion hooks.

**Configuration:** Pack makers configure weapon stations with `AddWeapon`, `AddTurretWeapon`, and weapon-part keys, and tune weapon definitions through MCHeli-style weapon files. Vehicle combat/support keys include `RadarType`, `RWRType`, `Stealth`, `FlareType`, `FlareOption`, `HasChaff`, `ChaffUseTime`, `ChaffWaitTime`, `HasAPS`, `APSUseTime`, `APSWaitTime`, `APSRange`, `AmmoSupplyRange`, and `RepairOtherVehicles`.

### Warnings, countermeasures, and protection

**What it does:** Vehicles can expose radar-warning identity, lock warnings, flare countermeasures, chaff countermeasures, maintenance, and active protection systems. Flares and chaff are separate systems so IR-style and radar-style threats can be handled differently by content and tuning.

**Why it exists:** Overdrive's air and ground combat needs survivability tools and target-identification data for missiles, radars, and battlefield awareness rather than relying only on vehicle HP.

**How it differs from original MCHeli:** The fork adds a stronger framework around warning/sensor identity and defensive subsystems while retaining existing MCHeli weapon and HUD compatibility.

**Configuration:** Users/server owners can globally tune collision and damage rules; pack makers tune each vehicle's radar/RWR names, stealth, countermeasure timing, APS range/timing, armor damage clamps, and weapon loadouts.

## Nuclear warfare and HBM integration

### HBM Nuclear Tech compatibility

**What it does:** Overdrive includes reflection-based compatibility hooks for HBM Nuclear Tech classes. Supported weapon configs can invoke HBM nuclear explosion entities, mushroom-cloud effects, `ExplosionNT`, and chemical cloud effects when HBM is present.

**Why it exists:** Many 1.7.10 warfare packs combine MCHeli-style vehicles with HBM Nuclear Tech. Overdrive provides strategic delivery paths for bombers, missiles, artillery, and nuclear-capable vehicle configs without hard-requiring HBM in every pack.

**How it differs from original MCHeli:** Original MCHeli did not provide this server-scale nuclear warfare bridge. Overdrive can connect vehicle-delivered payloads to HBM effects when matching content and HBM classes are available.

**Configuration:** Weapon configs use HBM-aware fields such as `explosionType`, `nukeYield`, and chemical effect fields where supported by the weapon code. Content packs can pair those weapons with strategic bombers, missile launchers, naval platforms, or artillery vehicles. Non-HBM packs continue using normal MCHeli explosion behavior.

## Vehicle physics and aircraft systems

### New fixed-wing flight model

**What it does:** Fixed-wing aircraft can opt into the new flight model with `UseNewMobilitySystem`, `UseNewFlightModel`, `EnableNewFlightModel`, or the older `EnableRealisticFlightModel` alias. Opted-in planes use data-driven mass/thrust simulation, configurable gravity, lift/energy management, AoA and stall behavior, control-authority penalties, combat flaps, flight ceilings, dive assist, and overspeed handling.

**Why it exists:** Original MCHeli aircraft behavior is simple and highly throttle-centric. Overdrive adds enough physical state for aircraft to feel different by role: light props, jets, transports, bombers, and UAVs can stall, bleed energy, overspeed, and respond differently to throttle and mass.

**How it differs from original MCHeli:** Legacy planes that do not opt in keep the original behavior. New-flight planes calculate forward airspeed, AoA, stall demand, lift loss, G/compressibility penalties, drag, throttle response, idle behavior, and optional per-aircraft gravity instead of using only legacy speed and mobility scalars.

**Configuration:** Pack makers tune plane keys such as `PhysicalMass`, `EngineThrust`, `NewFlightGravity`/`FlightGravity`/`GravityOverride`, `BaseDrag`, `InducedDrag`, `ControlSurfaceDrag`, `StallSpeed`, `CriticalAoA`, `StallRecoverySpeed`, `MaxSafeSpeed`, `OverspeedDamageRate`, and `NewFlightCombatFlaps`. Users and server owners can adjust global `AllPlaneSpeed`, `NewFlightGravity`, `NewFlightDiveAssistEnabled`, `NewFlightDiveAccelerationMultiplier`, and `NewFlightMaxDiveSpeedMultiplier`.

### New helicopter system

**What it does:** Helicopters can opt into `UseNewHelicopterFlightModel` / `EnableNewHelicopterFlightModel`. Opted-in helicopters maintain rotor RPM state, use collective-style vertical control, calculate rotor thrust against mass and gravity, apply vertical and parasite drag, limit climb performance, and support hover assist and explicit hover-mode stabilization.

**Why it exists:** Original MCHeli helicopters mostly use legacy throttle and mobility values. Overdrive adds a tuning framework so light scouts, utility helicopters, attack helicopters, and heavy transports can climb, hover, drift, and accelerate differently without replacing legacy packs.

**How it differs from original MCHeli:** New-heli keys are inert until the helicopter opts in. Once enabled, W/S are treated as collective-only controls, forward movement comes primarily from pitch attitude, sideways movement comes from roll attitude, and hover assist applies bounded collective/cyclic corrections rather than hard-locking the aircraft.

**Configuration:** Pack makers tune `PhysicalMass`, `MainRotorMaxThrust`, `RotorInertia`, `RotorSpoolUpRate`, `RotorSpoolDownRate`, `CollectiveResponse`, `CyclicAuthority`, `TailRotorAuthority`, `YawDamping`, `AngularInertia`, `VerticalDrag`, `MaxClimbRate`, `ParasiteDrag`, `HorizontalRotorThrustScale`, `HelicopterLateralThrustScale`, `HelicopterLateralDrag`, `HelicopterMaxLateralSpeedScale`, optional backward-flight scales, `HoverAssistStrength`, and `NewHeliControlHudDisplay`. Users/server owners still control global `AllHeliSpeed` and shared keybinds.

## Naval and submarine systems

### Walkable and moving ship decks

**What it does:** Ships use their base box and rotated extra bounding-box OBBs as deck surfaces. Players standing on a moving or turning ship are carried with the deck, receive small clearance corrections when the deck moves upward, keep grounded state, and have fall distance reset while standing on the surface.

**Why it exists:** Original MCHeli did not provide a robust moving-deck experience for large ships. Overdrive makes ships more useful as mobile platforms instead of decorative or single-seat vehicles.

**How it differs from original MCHeli:** The ship entity now searches for players standing on its calculated deck surfaces, rotates their relative position with ship yaw changes, and adjusts vertical placement to prevent one-tick freezing or clipping when water bobbing moves the deck.

**Configuration:** Pack makers create useful deck surfaces with normal body dimensions plus `BoundingBox` entries. Extra boxes retain their configured damage factors and armor behavior, and projectile hit processing ray-traces them as vehicle-rotated oriented boxes instead of unrotated AABBs. Large ships should define broad, flat extra boxes for decks and collision. `PreventWaterBobbing = true` can stabilize ship vertical movement for smoother walking and carrier operations.

### Aircraft carrier and vehicle-on-vehicle interaction

**What it does:** The shared rack system lets vehicles carry other vehicles. Carrier ships can define rack seats with `AddRack`, while aircraft or vehicles can advertise compatible parent racks with `RideRack`. Launch racks apply forward/vertical launch velocity, temporary no-collision grace against the carrier, and rack-aware unmount placement.

**Why it exists:** Overdrive supports aircraft carrier gameplay and vehicle transport beyond simple passenger seats: aircraft can park on or launch from configured ship racks, and vehicle-on-vehicle interactions can be made data-driven by pack authors.

**How it differs from original MCHeli:** Original MCHeli did not include this generalized carrier/rack workflow for vehicle-to-vehicle mounting. Overdrive adds rack discovery, compatibility checks, occupied-rack rejection, launch assistance, parachute fallback for non-launch dismounts, and debug logging for interaction failures.

**Configuration:** Parent vehicles use `AddRack = name/kind, x, y, z, entryX, entryY, entryZ[, range, parachuteAlt, fixYaw, fixPitch, rotSeat, launchRack]`. Child vehicles can use `RideRack = parentName, rackId`. Users use the rack keybinds (`KeyPutToRack`, `KeyDownFromRack`) exposed in the common vehicle controls.

### Functional diving submarines

**What it does:** Ship entities include a diving mode. When a ship is submerged and diving is toggled, it disables normal floating behavior, accepts separate ascend/descend input, applies bounded vertical acceleration, clamps vertical speed, and damps vertical motion when no submarine input is pressed.

**Why it exists:** Submarines need underwater depth control rather than surface-boat flotation. Overdrive adds a ship-based submarine behavior path without creating a separate content format.

**How it differs from original MCHeli:** Diving is bound to the ship VTOL/Extra-mode path instead of being a normal boat throttle behavior. It adds underwater navigation controls and HUD prompts for ascend/descend while preserving normal ship behavior when diving is off.

**Configuration:** Pack makers enable the switchable diving path with the ship VTOL/nozzle support (`enablevtol` plus a nozzle part where needed) and tune normal ship water behavior with shared `float`, `floatoffset`, `gravity`, `gravityinwater`, and ship `PreventWaterBobbing`. Users bind `KeySubmarineAscend` and `KeySubmarineDescend` for depth control.

## Rendering and performance

### Vehicle LOD and long-range rendering

**What it does:** Servers periodically send lightweight, read-only vehicle snapshots to clients. Clients render far vehicles through a model-only LOD path without spawning proxy entities or changing Forge's normal entity tracking.

**Why it exists:** Large combined-arms servers often need aircraft, ships, tanks, and turrets to remain visually identifiable at distances where full entity tracking/rendering is too expensive or unreliable.

**How it differs from original MCHeli:** Overdrive adds a dedicated snapshot packet and client LOD manager. Snapshots include entity identity, category, type, texture, position, rotation, scale, and sampled world lighting, and the render path switches to cheaper displays beyond `AircraftLODStartDistance`.

**Configuration:** Users/server owners configure `EnableAircraftLODRender`, `AircraftLODStartDistance`, and `AircraftLODFarDistance`. `AircraftLODFarDistance` also guides the extended tracking range helper so the render-only system has useful data without replacing real entity simulation.

### Visual and client performance options

**What it does:** Overdrive exposes render and model-loading controls such as shader enablement, smooth shading, render-distance weighting, third-person HUD behavior, and optional multi-threaded model loading.

**Why it exists:** MCHeli content packs can include large models and many vehicle types. These options let clients choose visual quality and loading behavior appropriate for their hardware.

**How it differs from original MCHeli:** The fork documents and centralizes client performance toggles alongside the new LOD system rather than relying only on the original rendering path.

**Configuration:** User-facing options include `EnableModEntityRender`, `TestMode`, `EnableShader`, `SmoothShading`, `RenderDistanceWeight`, `DisplayHUDThirdPerson`, and `MultiThreadedModelLoading`.

## Vehicle interaction, persistence, and synchronization

### New UAV persistence and duplicate protection

**What it does:** New UAVs can persist station exit locations through server-side JSON storage. A runtime registry resolves live UAVs by persistent UUID, entity UUID, common ID, and owner, and avoids destructive duplicate cleanup while station/player restoration is still in progress.

**Why it exists:** Original MCHeli UAV behavior is fragile around restarts and station control. Overdrive adds persistence and duplicate-detection infrastructure so NewUAV/NewSmallUAV content can survive common server lifecycle events more safely.

**How it differs from original MCHeli:** Overdrive stores NewUAV station/location data in `data/mcheli_new_uavs.json`, tracks destroyed stations, restores item identity/damage, and resolves duplicate live entities through canonical selection instead of blindly deleting potentially recoverable vehicles.

**Configuration:** Pack makers use shared `UAV`, `SmallUAV`, `NewUAV`, `NewSmallUAV`, and `TargetDrone` flags. Server owners should preserve the world `data/` folder with normal backups because it contains the NewUAV JSON persistence file.

### Entity information and multiplayer synchronization

**What it does:** Overdrive tracks selected entities server-side and synchronizes compact entity info to clients for systems such as RWR/radar-style awareness. It also adds render-only LOD snapshots and several rack/seat repair paths to reduce stale interaction state.

**Why it exists:** Combined-arms gameplay depends on clients having enough information about fast-moving aircraft, chaff, lockable entities, seats, and distant vehicles without overloading normal Minecraft tracking.

**How it differs from original MCHeli:** The fork adds dedicated info-sync packets, client-side trackers, rack debug/repair logic, and snapshot packets that are separate from normal entity spawning.

**Configuration:** Pack-facing sensor keys include `RadarType`, `RWRType`, `NameOnModernAARadar`, `NameOnEarlyAARadar`, `NameOnModernASRadar`, `NameOnEarlyASRadar`, `Stealth`, `EnableEntityRadar`, and `EnableNightVision`. Server-facing collision and management behavior is controlled through the standard `Collision_*`, `IgnoreBulletHit`, placement, command, and speed/global config keys.

### Placement, management, and server controls

**What it does:** Overdrive retains the Drafting Table workflow while adding broad server controls for placement restrictions, world damage, entity collision damage, recipe toggles, creative-tab icons, command permissions, and administrative `/mcheli` utilities.

**Why it exists:** Server owners need to run military vehicle content in protected or performance-sensitive worlds without giving every feature unrestricted block damage or placement rights.

**How it differs from original MCHeli:** This fork documents the controls as first-class administration tools and expands vehicle categories to include ships, tanks, turrets/static weapons, UAVs, and carrier/rack interactions.

**Configuration:** Important user/server keys include `EnableCommand`, `CommandPermission`, `PlaceableOnSpongeOnly`, `Explosion_DestroyBlock`, `Explosion_FlamingBlock`, `Collision_DestroyBlock`, `Collision_EntityDamage`, `InfinityAmmo`, `InfinityFuel`, `DisableItemRecipe`, and the `CreativeTabIcon*` options.

## Camera, controls, HUD, and interface

### New camera system and control refinements

**What it does:** New-flight planes can use a client-only chase camera with configurable distance, aircraft-size scaling, speed distance scaling, collision handling, pitch readability, screen bias, FOV override, freelook smoothing, and held look-ahead. Shared controls include rack operations, submarine ascend/descend, free look, aircraft GUI access, and updated mouse/flight-sim modes.

**Why it exists:** The original camera and control path can be hard to use with fast aircraft, large models, carriers, and new flight dynamics. Overdrive adds usability tools while keeping legacy views intact for vehicles that do not qualify.

**How it differs from original MCHeli:** The new camera path is gated to third-person pilot view in qualifying new-flight planes and does not change physics or weapons. Aircraft follow-camera improvements remain currently in development and under active refinement. Mouse-aim controls also remain currently in development; they are global, disabled by default, and not per-plane yet.

**Configuration:** User-facing keys include `EnableNewPlaneThirdPersonCamera`, `PlaneChaseBaseDistance`, `PlaneChaseMinDistance`, `PlaneChaseMaxDistance`, `EnableSpeedBasedCameraDistance`, `PlaneChaseFOV`, `PlaneLookAheadDistance`, freelook smoothing keys, `EnableMouseAimControls`, `KeyPlaneMouseAim`, `MouseAim*`, `EnablePlaneMouseAimReticle`, and reticle scale/opacity/texture keys.

### New HUD systems

**What it does:** New-flight planes and new-heli configurations can display compact telemetry such as throttle/engine state, speed/altitude, warnings, damage, weapon and ammunition details, combat-flap status, hover-assist state, and debug/development telemetry where enabled.

**Why it exists:** The new physics systems expose information pilots need to fly and tune aircraft: throttle percent, energy/stall state, flaps, hover assist, weapon readiness, and damage are more important in Overdrive than in original MCHeli's simpler flight model.

**How it differs from original MCHeli:** Legacy HUD files still load through the existing `HUD` config system. Overdrive adds gated new HUD overlays and extra telemetry for opted-in systems without forcing existing content packs to rewrite HUD definitions.

**Configuration:** Users can toggle and position new-plane HUD pieces with `EnableNewPlaneSimpleHud`, `EnableNewPlaneWeaponHud`, `EnableNewPlaneHudGlow`, `NewPlaneSimpleHudX`, `NewPlaneSimpleHudY`, `NewPlaneWeaponHudRightMargin`, and `NewPlaneWeaponHudY`. Pack makers use `NewFlightThrottleHudDisplay` and `NewHeliControlHudDisplay` / `NewHelicopterControlHudDisplay` plus normal `HUD` assignments.

## Progression, crafting, and interoperability

### Drafting Table and OreDictionary recipes

**What it does:** Overdrive's Drafting Table supports shaped, shapeless, shaped OreDictionary, and shapeless OreDictionary recipes. Recipe creation is validated server-side by output item, and the GUI can render ore recipes correctly. Item definitions can register OreDictionary names for use by other mods and by Overdrive recipes.

**Why it exists:** Survival and faction servers need vehicle production to fit industrial modpacks instead of relying only on creative tabs. OreDictionary support allows steel, electronics, plates, engines, ammunition components, and other modded ingredients to be interchangeable where recipes allow it.

**How it differs from original MCHeli:** The fork improves the Drafting Table's mod-interoperability path and recipe validation/display behavior so it can support industrialized vehicle and ammunition production chains in large modpacks.

**Configuration:** Pack makers use `AddRecipe`, `AddShapelessRecipe`, item `OreDict` / `AddOreDict`, and standard Forge ore names in recipe definitions. Server owners can control recipe availability with `DisableItemRecipe`, Drafting Table IDs/config, and normal modpack recipe policy.

### Survival and faction progression support

**What it does:** Overdrive documentation and config surfaces support vehicle component systems, ammunition/support items, supply vehicles, ammo/fuel supply ranges, and equipment progression built by content packs.

**Why it exists:** The project is meant to fit survival, faction, and warfare-focused servers where vehicles are produced, supplied, repaired, armed, and countered through server economies rather than spawned as isolated creative items.

**How it differs from original MCHeli:** Original MCHeli's Drafting Table remains, but Overdrive expands the compatibility and configuration surface needed for larger production chains and industrial mod integration.

**Configuration:** Pack makers combine recipes, OreDictionary entries, item definitions, `AmmoSupplyRange`, `FuelSupplyRange`, `RepairOtherVehicles`, weapon/ammo items, and vehicle support roles. Server owners can pair these with external recipe mods and permission rules.

## Framework and asset-configuration improvements

### Expanded vehicle configuration framework

**What it does:** Overdrive keeps MCHeli's text-based content format and expands it with new aircraft physics keys, new helicopter keys, ship/submarine behavior, rack/carrier keys, radar/RWR identity, collision boxes, track hitboxes, visual parts, camera offsets, and mobility toggles.

**Why it exists:** Pack makers can migrate content gradually. A vehicle can remain legacy-compatible or opt into new systems per aircraft/helicopter/ship instead of forcing a global behavior switch.

**How it differs from original MCHeli:** New systems are layered into existing parsers with explicit opt-in gates and compatibility aliases. This lets original MCHeli-style packs continue loading while newer packs can tune more detailed physics, HUD, camera, and interaction behavior.

**Configuration:** Start with `docs/vehicle-config-reference.md`, then use the type-specific guides under `docs/vehicle-config/`. The most important migration distinction is legacy behavior versus vehicles that opt into `UseNewMobilitySystem` or `UseNewHelicopterFlightModel`.

### Internal refactors and architecture improvements

**What it does:** The fork includes bounded fixed-tick helpers for frame-rate-sensitive physics, LOD snapshot managers, additional packet types, NewUAV persistence/registry helpers, vehicle naming documentation, and centralized config references.

**Why it exists:** MCHeli's original architecture predates many of the Overdrive systems. These refactors reduce frame-rate coupling, make far rendering and persistence more explicit, and document shared vehicle infrastructure that historically used aircraft naming for non-aircraft classes.

**How it differs from original MCHeli:** The project treats shared vehicle infrastructure as a cross-category framework for planes, helicopters, ships, tanks, turrets, UAVs, seats, weapons, renderers, and controls rather than a helicopter-only or aircraft-only layer.

**Configuration:** Most refactor-facing options are documented in `docs/configuration.md`; developer-facing boundaries are documented in `docs/frame-rate-physics.md` and `docs/vehicle-naming-conventions.md`.
