# Vehicle LOD moving-part audit

This audit compares the normal entity renderer, tracked-entity LOD hook, and entity-free snapshot renderer. Snapshot rendering is intentionally limited to separable models; monolithic models retain the safe all-model fallback because named bind-pose groups cannot be excluded.

| Category | Configuration / part list | Normal renderer / state source | Tracked LOD | Snapshot LOD | Action |
|---|---|---|---|---|---|
| All base vehicles | `partWeapon` | `renderWeapon`; weapon-set pose | Existing common-part path | Missing outside tanks | Render bounded generic weapon poses for helicopter, plane, and ship; stationary turrets remain separate |
| All base vehicles | `partRotPart` | `renderRotPart`; throttle/rotation counters | Existing common-part path | Not safely reconstructable from current synchronized state | Intentionally skipped pending an authoritative per-part state source |
| All base vehicles | `hatchList`, `lightHatchList` | `renderHatch`, `renderLightHatch`; entity parts/searchlight | Existing common-part path | Primarily close-range/access or lighting state and not yet authoritative | Intentionally skipped rather than guess state |
| Tanks | track rollers, crawler tracks, wheels | `renderTrackRoller`, `renderCrawlerTrack`, `renderWheel`; synchronized running gear | Working | Working | Preserved unchanged |
| All base vehicles | steering wheel, throttle, camera | `renderSteeringWheel`, `renderThrottle`, `renderCamera`; rider/cockpit input | Existing common-part path | Cockpit-oriented and not meaningful at snapshot distance | Intentionally skipped |
| Planes/ships | `landingGear` | `renderLandingGear`; current/previous gear rotation | Plane gear was missing | Missing | Added shared state overload and snapshot fields |
| All base vehicles | weapon bays, canopy | `renderWeaponBay`, `renderCanopy`; entity `MCH_Parts` | Existing common-part path | Access/cockpit geometry lacks authoritative snapshot state | Intentionally skipped rather than render bind pose |
| Planes | `nozzles` | `MCP_RenderPlane.renderNozzle`; nozzle rotation | Working in `renderBaseVehicle` | Missing | Added state overload and snapshot rendering |
| Planes | `wingList` / pylons | `MCP_RenderPlane.renderWing`; wing rotation | Working in `renderBaseVehicle` | Missing | Added state overload preserving parent-child transforms |
| Planes | `rotorList` / blades | `MCP_RenderPlane.renderRotor`; nozzle parent plus rotor phase | Working in `renderBaseVehicle` | Missing | Added state overload and continuous per-display phase |
| Helicopters | rotor/blades | `MCH_RenderHeli.drawModelBlade`; rotor phase/fold state | Working | Working | Preserved unchanged |
| Ships | nozzles, wings/pylons, rotors/blades | `MCH_RenderShip` plane-like methods; ship rotations | Working in `renderBaseVehicle` | Missing | Added type-safe ship state overloads and snapshot rendering |
| Tanks | category LOD hook | `MCH_RenderTank.renderAircraftLODParts` | Working | Working | Preserved unchanged |
| Stationary turrets | recursive `partList` | `MCH_RenderTurret.drawPart`; aim/weapon indexed state | Working | Missing | Added bounded DFS pose capture and entity-free recursive renderer |

## Snapshot design notes

Plane and ship propeller phase is not interpolated between one-second packet angles. Each display advances its own phase every render from the synchronized wrapped per-tick angular change, gently corrects toward each new authoritative phase, and clamps stale extrapolation. A synchronized zero angular change therefore stops the propeller.

Stationary turret part poses are captured and consumed in the same stable configuration-order, depth-first traversal. Recoil/cooldown weapon indexes advance only for type 2 and type 3 parts, matching the normal renderer, while the packet's bounded pose array is keyed by traversal position rather than object identity.
