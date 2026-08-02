# Configuration Reference

MC Helicopter Overdrive+ writes `config/mcheli.cfg` during startup. The file is generated from source-defined defaults, then rewritten after loading so missing options are restored.

## Editing workflow

1. Start the game/server once.
2. Stop the game/server.
3. Edit `config/mcheli.cfg`.
4. Restart, or use `/mcheli reconfig` for server-side reloadable settings.

Client keybinds and rendering settings are safest to change while the client is closed.

## Value formats

- Booleans use `true` or `false`.
- Numeric keybinds use LWJGL key/mouse codes. Negative values are mouse buttons (`-100` = left click, `-99` = right click, `-98` = middle click).
- Block/material lists are comma-separated names.
- `HitMarkColor` uses `Alpha, Red, Green, Blue`, each clamped to 0-255.
- `CommandPermission` uses `commandName:Player1, Player2`.
- Damage factors use either a plain multiplier such as `1.0` or a multiplier with an entity-class filter, depending on the existing config format.
- Tank and turret fall damage is server-side vehicle behavior, not a separate config key: impacts above three effective blocks use the vehicle fall damage source and scale from current health, configured gravity/gravity-in-water, downward acceleration, and landing speed.

## General options

| Option | Default | Notes |
| --- | --- | --- |
| `TestMode` | `false` | Development/test toggle. |
| `EnableCommand` | `true` | Enables `/mcheli` subcommands. |
| `EnableNEIHandler` | `true` | `true` registers the MCHeli vehicle ammunition recipe and usage handler in NotEnoughItems; `false` prevents the handler from registering. Changing NEI registration requires a client restart and is not applied by `/mcheli reconfig`. |
| `PlaceableOnSpongeOnly` | `false` | Restricts vehicle placement to sponge blocks. |
| `ItemDamage` | `true` | Enables item damage behavior for applicable MCHeli items. |
| `ItemFuel` | `true` | Enables fuel item behavior. |
| `AutoRepairHP` | `0.0` | Global auto-repair HP value/threshold. |
| `AutoRepairEnabled` | `false` | Auto-repair feature toggle. The source contains a commented older initialization and an active generated-config entry. |
| `Explosion_DestroyBlock` | `true` | Allows explosions to destroy blocks. |
| `Explosion_FlamingBlock` | `true` | Allows explosions to create flaming blocks. |
| `BulletBreakableBlocks` | `glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily` | Blocks bullets can break. |
| `Collision_DestroyBlock` | `true` | Allows collision block destruction. |
| `Collision_Car_BreakableBlock` | `double_plant, glass_pane,stained_glass_pane` | Car/vehicle collision breakable block list. |
| `Collision_Car_BreakableMaterial` | `cactus, cake, gourd, leaves, vine, plants` | Car/vehicle collision breakable material list. |
| `Collision_Tank_BreakableBlock` | `nether_brick_fence` | Tank collision breakable block list. |
| `Collision_Tank_BreakableMaterial` | `cactus, cake, carpet, circuits, glass, gourd, leaves, vine, wood, plants` | Tank collision breakable material list. |
| `Collision_EntityDamage` | `true` | Enables entity damage from collisions. |
| `Collision_EntityTankDamage` | `false` | Enables tank collision entity damage. |
| `InfinityAmmo` | `false` | Unlimited ammunition. |
| `InfinityFuel` | `false` | Unlimited fuel. |
| `DismountAll` | `false` | Dismount behavior toggle. |
| `MountMinecartHeli` | `true` | Allows helicopters to mount/interact with minecarts where supported. |
| `MountMinecartPlane` | `true` | Allows planes to mount/interact with minecarts where supported. |
| `MountMinecartShip` | `false` | Allows ships to mount/interact with minecarts where supported. |
| `MountMinecartVehicle` | `false` | Allows vehicles to mount/interact with minecarts where supported. |
| `MountMinecartTank` | `true` | Allows tanks to mount/interact with minecarts where supported. |
| `PreventingBroken` | `false` | Prevents breakage behavior where implemented. |
| `DropItemInCreativeMode` | `false` | Allows item drops in creative mode for applicable actions. |
| `BreakableOnlyPickaxe` | `false` | Restricts breakability to pickaxe behavior where implemented. |
| `AllHeliSpeed` | `1.5` | Global helicopter speed scalar; clamped between 0 and 1000. |
| `AllPlaneSpeed` | `1000.0` | Global plane speed scalar/clamp value; clamped between 0 and 1000. Plane config speeds use `(mph / 1000) * 1.74` before this scalar is applied. AA-missile launch speed uses the matching normalized scale, `AllPlaneSpeed / 1000 * 1.74`, so the default makes AA missiles 1.74 times their configured `Acceleration`. |
| `NewFlightGravity` | `0.008` | Global per-tick downward acceleration for new-flight-model aircraft; individual vehicle configs can override with `NewFlightGravity`, `FlightGravity`, or `GravityOverride`. |
| `AllShipSpeed` | `2.0` | Global ship speed scalar; clamped between 0 and 1000. |
| `AllTankSpeed` | `1.0` | Global tank speed scalar; clamped between 0 and 1000. |
| `HurtResistantTime` | `0.0` | Hurt resistance timing; clamped from 0 to 10000. |
| `StingerLockRange` | `4988.0` | Lock range for Stinger-style light weapons. |
| `delayrangeloader` | `5` | Advanced loader timing option. Exact gameplay effect needs code-level tracing. |
| `bombletloader` | `10` | Advanced bomblet loader option. Exact gameplay effect needs code-level tracing. |
| `wrenchdropitem` | `false` | Wrench drop-item behavior toggle. |
| `placetimer` | `60` | Survival-mode vehicle hold-to-deploy timer; Creative placement is instant. |
| `RangeFinderSpotDist` | `400` | Rangefinder spot distance. |
| `RangeFinderSpotTime` | `15` | Rangefinder spot duration. |
| `RangeFinderConsume` | `true` | Rangefinder consumes required item/ammo when spotting. |
| `EnablePutRackInFlying` | `true` | Allows rack operations during flight. |
| `EnableDebugBoundingBox` | `false` | Enables debug bounding boxes. Can be toggled in memory with `/mcheli showboundingbox`. |
| `InvertMouse` | `false` | Inverts aircraft mouse controls. |
| `MouseSensitivity` | `30.0` | MCHeli mouse sensitivity. |
| `ZoomSensitivityEffect` | `100.0` | Client-side MCHeli mouse-input zoom reduction, valid from `0` to `100`. `0` keeps the old sensitivity behavior, while `100` scales sensitivity fully against optical magnification; intermediate values blend the two behaviors. It applies to vehicle cameras (including seats, turrets, and UAV control), GLTDs, scoped light weapons, and the rangefinder without modifying vanilla Minecraft sensitivity. |
| `MouseControlStickModeHeli` | `false` | Enables stick-style mouse mode for helicopters. |
| `MouseControlStickModePlane` | `false` | Enables stick-style mouse mode for planes. |
| `MouseControlFlightSimMode` | `true` | Flight-sim mouse mode (`Yaw:key, Roll=mouse` per source comment). |
| `AutoThrottleDownHeli` | `true` | Auto-throttle-down behavior for helicopters. |
| `AutoThrottleDownPlane` | `false` | Auto-throttle-down behavior for planes. |
| `AutoThrottleDownShip` | `false` | Auto-throttle-down behavior for ships. |
| `AutoThrottleDownTank` | `false` | Auto-throttle-down behavior for tanks. |
| `SwitchWeaponWithMouseWheel` | `true` | Allows mouse-wheel weapon switching. |
| `LWeaponAutoFire` | `false` | Auto-fire behavior for light weapons. |
| `ArtilleryRangeModifier` | `1.0` | Multiplies launch speed only for weapon text files with `UseGlobalArtilleryRangeModifier = true`. Values below `0.01` are clamped to `0.01`; `1.0` keeps original range. |
| `EnableHandheld` | `true` | Enables crafting recipes for hand-held weapons and their ammunition (`Stinger`, `Javelin`, and `RPG`). Set to `false` to prevent those recipes from registering. |
| `DisableItemRender` | `1` | Valid range noted in source: `0 ~ 3`; `1` recommended. |
| `Override3DItemIcon` | `false` | Global 3D vehicle item icon override. `true` forces 3D item icons off; `false` allows per-vehicle `Enable3DItemIcon` settings. |
| `Heli3DItemIconScale` | `1.0` | Global scale multiplier for helicopter 3D item icons. |
| `Plane3DItemIconScale` | `1.0` | Global scale multiplier for plane 3D item icons. |
| `Ship3DItemIconScale` | `1.0` | Global scale multiplier for ship 3D item icons. |
| `Tank3DItemIconScale` | `1.0` | Global scale multiplier for tank 3D item icons. |
| `Turret3DItemIconScale` | `1.0` | Global scale multiplier for turret/static vehicle 3D item icons. |
| 3D item icon rendering | n/a | Vehicle item models are queued and cached client-side in chunked OpenGL display lists after first render, preventing large creative tabs from compiling every visible model or full vehicle mesh in the same frame while keeping the same config toggles. |
| `HideKeybind` | `false` | Hides keybind display/help where implemented. |
| `RenderDistanceWeight` | `1000.0` | Render-distance weight for mod rendering. |
| `EnableAircraftLODRender` | `true` | Enables client-only far-distance model displays for aircraft, tanks, turrets, and ships. |
| `AircraftLODStartDistance` | `140.0` | Distance where tracked vehicles switch to cheaper model-only rendering, with the existing hysteresis around the transition. |
| `AircraftLODFarDistance` | `4800.0` | Hard maximum detection range for render-only vehicle snapshots; this is not a full-detail visibility distance. |
| `AircraftLODVisibilityDistance` | `4800.0` | Clear/hazy atmospheric visibility distance where Koschmieder contrast transmission reaches approximately two percent. |
| `AircraftLODRainVisibilityMultiplier` | `0.70` | Multiplier applied to atmospheric visibility distance in rain. |
| `AircraftLODThunderVisibilityMultiplier` | `0.45` | Multiplier applied to atmospheric visibility distance in thunder. |
| `AircraftLODThermalContrastExponent` | `0.35` | Raises atmospheric transmission to this exponent in thermal mode, improving contrast without extending the hard range. |
| `AircraftLODOpticalMinPixels` | `0.75` | Minimum projected target dimension in pixels for normal optical detection. |
| `AircraftLODThermalMinPixels` | `0.35` | Minimum projected target dimension in pixels for thermal detection. |
| `DebugVehicleLODVisibility` | `false` | Logs at most one snapshot visibility decision per second, including projection, atmosphere, projected size, camera, weather, and skip reason. |
| `MobRenderDistanceWeight` | `10.0` | Mob render-distance weight; clamped to 0.1-100. |
| `CreativeTabIconItem` | `fuel` | Icon item for general tab. |
| `CreativeTabIconHeli` | `ah-64` | Icon item for helicopter tab. |
| `CreativeTabIconPlane` | `f22a` | Icon item for plane tab. |
| `CreativeTabIconShip` | `project1204` | Icon item for ship tab. |
| `CreativeTabIconTank` | `merkava_mk4` | Icon item for tank tab. |
| `CreativeTabIconVehicle` | `mk15` | Icon item for vehicle tab. |
| `DisableShader` | `false` | Disables shader usage where implemented. |
| `DefaultExplosionParticle` | `false` | Uses default explosion particles where implemented. |
| `AliveTimeOfCartridge` | `200` | Cartridge casing lifetime. |
| `HitMarkColor` | `255, 255, 0, 0` | ARGB hit marker color. |
| `SmoothShading` | `true` | Smooth model shading toggle. |
| `EnableModEntityRender` | `true` | Enables mod entity rendering. |
| `DisableRenderLivingSpecials` | `true` | Disables living-special rendering such as nameplate-related render hooks. |
| `DisplayHUDThirdPerson` | `false` | Shows HUD in third person where supported. |
| `DisableThirdPersonCameraDistChange` | `false` | Prevents MCHeli third-person camera-distance changes. |
| `EnableReplaceTextureManager` | `true` | Texture manager replacement hook. |
| `DisplayEntityMarker` | `true` | Shows entity markers. |
| `EntityMarkerSize` | `10.0` | Entity marker size; minimum corrected to 0. |
| `BlockMarkerSize` | `10.0` | Block marker size; minimum corrected to 0. |
| `ReplaceRenderViewEntity` | `true` | Replaces render-view entity for MCHeli camera behavior. |
| `ItemRecipe_*` | See generated config/source defaults | Recipe strings for core MCHeli items. |
| `MultiThreadedModelLoading` | `true` | Enables threaded model loading on the client. |

Snapshot visibility uses the active projection matrix, so scopes and other FOV magnification increase projected target size naturally. Magnification does not change real distance or atmospheric transmission. Thermal vision lowers the projected-size threshold and improves contrast, but it never extends `AircraftLODFarDistance`. Snapshot rendering retains depth testing, allowing loaded nearby terrain and structures to occlude a target; unloaded terrain has no depth information and therefore cannot occlude this render-only data.

## Vehicle content-pack keys

## Weapon artillery range opt-in

Place the following key in an individual weapon text file to opt that weapon into the global launch-speed multiplier:

```text
UseGlobalArtilleryRangeModifier = true
```

The weapon-key default is `false`, so existing weapon files and explicitly disabled weapons retain their original speed and range. `ArtilleryRangeModifier = 1.0` also preserves the original range. `DisplayMortarDistance` controls only whether the HUD displays an impact distance; it neither classifies a weapon as artillery nor controls modifier eligibility. This is a per-weapon opt-in, not a per-vehicle setting, so one vehicle can combine a modified artillery weapon with an unchanged secondary weapon.

## Weapon damage factors

Weapon text files use comma-separated `DamageFactor = type, multiplier` entries. The existing `tank`, `plane`, `vehicle`, `heli`/`helicopter`, `ship`, and `player` types are supported. `other` and `others` select non-player, non-villager living entities:

```text
DamageFactor = player, 20.0
DamageFactor = other, 3.0
```

Players and villagers use `player`. Other living entities use `other` when it is present and fall back to `player` when it is absent. Explicit aircraft and vehicle factors take priority and apply to subclasses. Nonliving entities have a factor of `1.0` unless an explicit class factor applies.

Vehicle `.txt` definitions can opt individual items into or out of 3D item rendering and tune their own size after the global type scale is applied:

| Key | Default | Notes |
| --- | --- | --- |
| `Enable3DItemIcon` | `true` | Per-vehicle toggle. Set `false` to keep that vehicle on the normal flat item icon even when the global override allows 3D icons. |
| `ItemIconScaleFactor` | `1.0` | Per-vehicle 3D item icon scale multiplier, clamped to `0.01` through `100.0`. Alias: `3DItemIconScaleFactor`. |
| `MaximumExternalPayloadCapacity` | `0` | Maximum total vehicle weight, in pounds, that this vehicle can lift/tow with a chain or carry across all occupied configured racks. Rack mounting subtracts already-mounted vehicle `Weight` plus the incoming vehicle `Weight`; the remaining capacity is never allowed below zero. A value of `0` prevents chain-towing or rack-carrying other configured vehicles by default. Non-zero values are shown on the vehicle item tooltip as max payload. |
| `Weight` | `50000` | Vehicle weight, in pounds, used when another vehicle attempts to chain-tow it or mount it on a rack. Non-zero values are shown on the vehicle item tooltip. |
| `Float` | `false` | Allows a vehicle to float on water. Enabled floating is shown on the vehicle item tooltip for plane, helicopter, tank, and turret/static-vehicle items. |

## Hidden/advanced options initialized in source

These are initialized but not all are written through the active `General` array in the inspected source:

| Option | Default | Notes |
| --- | --- | --- |
| `Collision_Car_NoBreakBlock` | `torch` | Car collision no-break block list. Initialized and used by correction logic, but not written in the current `General` array. |
| `Collision_Tank_NoBreakBlock` | `torch, glowstone` | Tank collision no-break block list. Initialized and used by correction logic, but not written in the current `General` array. |
| `DespawnCount` | `25` | Initialized but not written in the current `General` array. |
| `HitBoxDelayTick` | `0` | Initialized but not written in the current `General` array. |
| `EnableRotationLimit` | `false` | Initialized but not written in the current `General` array. |
| `PitchLimitMax` | `10` | Initialized but not written in the current `General` array. |
| `PitchLimitMin` | `-10` | Initialized but not written in the current `General` array. |
| `RollLimit` | `35` | Initialized but not written in the current `General` array. |
| `RangeOfGunner_VsMonster_Horizontal` | `80` | Initialized but not written in the current `General` array. |
| `RangeOfGunner_VsMonster_Vertical` | `160` | Initialized but not written in the current `General` array. |
| `RangeOfGunner_VsPlayer_Horizontal` | `200` | Initialized but not written in the current `General` array. |
| `RangeOfGunner_VsPlayer_Vertical` | `300` | Initialized but not written in the current `General` array. |
| `FixVehicleAtPlacedPoint` | `true` | Initialized but not written in the current `General` array. |
| `KillPassengersWhenDestroyed` | `false` | Initialized but not written in the current `General` array. |

## Damage and ignored projectile entries

The generated config includes damage factor entries for:

```text
DamageVsEntity
DamageVsLiving
DamageVsPlayer
DamageVsMCHeliAircraft
DamageVsMCHeliTank
DamageVsMCHeliVehicle
DamageVsMCHeliOther
DamageMCHeliAircraftByExternal
DamageMCHeliTankByExternal
DamageMCHeliVehicleByExternal
DamageMCHeliOtherByExternal
```

If no entries are present, the code adds default `1.0` multipliers.

`IgnoreBulletHit` defaults are added when the list is empty:

```text
IgnoreBulletHit = flansmod.common.guns.EntityBullet
IgnoreBulletHit = flansmod.common.guns.EntityGrenade
```


### New-flight plane third-person chase camera

These options are client-side visual/readability settings for pilots flying planes that use the new flight/new mobility system. They do not change flight physics, stall behavior, pitch authority, throttle behavior, weapons, targeting, HUD rendering, or aircraft balance. Legacy aircraft, first-person view, helicopters, tanks, turrets, ships, passengers, and gunners keep the existing camera path unless the gated new-plane third-person pilot conditions are met.

The standard chase camera is intentionally stable: it stays behind the aircraft with configurable distance, aircraft-size scaling, speed distance scaling, collision avoidance, pitch readability, aircraft-below-center framing, and conservative roll influence. It does **not** automatically move focus based on throttle, steering input, turn rate, sideslip, or velocity direction. Situational camera movement comes from held player actions only when the matching control option is enabled:

* **Free Look** (`KeyFreeLook`, default Left Control): by default this remains the original toggle action, including in regular third person, so the view does not snap back when the key is released. Enabling **Hold Free Look** in the controls menu changes it to a held action that turns off on key release.
* **Hold Plane Look Ahead** (`KeyPlaneLookAhead`, default Left Alt): while held, the camera focus blends forward along the aircraft facing direction while the camera remains behind the aircraft at normal chase distance. Releasing the key smoothly returns to centered framing.
* **Interaction:** the new third-person chase camera no longer consumes freelook mouse movement for a custom orbit path, so freelook uses the shared aircraft freelook state instead of injecting camera-orbit deltas into flight controls. While free look is active, plane pilots can still use **A/D** to turn the aircraft in both regular controls and mouse flight-sim mode.

Recommended bindings: keep **Free Look** on a comfortable key such as Left Control, enable **Hold Free Look** only if you prefer hold-to-use behavior, and bind **Plane Look Ahead** to Left Alt, a thumb mouse button, or another hold key that can be pressed briefly during target tracking.

| Config key | Default | Purpose | Practical range |
| --- | ---: | --- | --- |
| `EnableNewPlaneThirdPersonCamera` | `false` | Enables the smooth chase camera only for third-person pilot view in new-flight planes. | `true`/`false` |
| `EnableSpeedBasedCameraDistance` | `false` | Optional speed distance scaling. Disabled by default to prevent zoom breathing with throttle/speed changes. | keep `false`; use only for small capped effects |
| `EnableNewPlaneCameraCollision` | `false` | Allows block ray tracing to shorten the camera when terrain/buildings/trees/hangars obstruct the view. | `true`/`false` |
| `EnablePlaneLookAhead` | `false` | Enables held Plane Look Ahead. This is player-initiated only, never automatic velocity/turn prediction. | `true`/`false` |
| `EnableHoldFreelook` | `false` | Enables hold-to-use Free Look globally; disabled preserves the original toggle behavior. | `true`/`false` |
| `EnableNewPlaneCameraRollInfluence` | `false` | Allows limited aircraft roll to affect the camera horizon. | `true`/`false` |
| `PlaneChaseBaseDistance` | `15.0` | Stable base chase distance in blocks before aircraft-size bonus and optional speed bonus. | default `15`; tune with wider FOV before large distance swings |
| `PlaneChaseMinDistance` | `13.0` | Minimum camera distance from the smoothed focus after scale handling, high enough to keep tails framed during slowdown. | `12`-`18` for normal chase |
| `PlaneChaseMaxDistance` | `21.0` | Maximum normal camera distance after scale and optional speed bonus. The full range is not used unless speed scaling is enabled. | `20`+ if very large aircraft need it |
| `NewPlaneCameraDebugDistance` | `0.0` | `DebugFlightControl`-only distance override for proof testing; `0` disables. Keep normal tuning in the non-debug keys. | `0` or `30`-`80` |
| `NewPlaneCameraDebugAbovePlane` | `false` | `DebugFlightControl`-only hard proof mode that places the dummy above the plane. | debug only |
| `NewPlaneCameraHeight` | `4.0` | Vertical camera offset above the combat focus. | `2`-`8`; large aircraft may prefer `6`-`12` |
| `NewPlaneCameraSideOffset` | `0.0` | Optional left/right offset for off-center chase views. | `-4`-`4` |
| `PlaneChaseSpeedDistanceScale` | `2.0` | Extra chase distance per block/tick of aircraft speed when speed scaling is explicitly enabled. | keep low; pair with `PlaneChaseSpeedDistanceMaxBonus` |
| `PlaneChaseSpeedDistanceMaxBonus` | `3.0` | Caps optional speed-based extra distance so acceleration cannot cause aggressive zoom. | `2`-`5` |
| `EnablePlaneChaseFOVOverride` | `false` | Enables the new chase-camera-only FOV override. | `true` |
| `PlaneChaseFOV` | `95.0` | Target FOV for new-flight third-person plane chase. | `85`-`105` |
| `PlaneChaseFreelookFOV` | `95.0` | Optional FOV while hold-freelook orbit is active. | usually match `PlaneChaseFOV` |
| `PlaneChaseFOVSmoothing` | `0.25` | Smoothly blends FOV entering/exiting chase and freelook. | `0.15`-`0.35` |
| `NewPlaneCameraSizeDistanceScale` | `1.25` | Extra chase distance per block of aircraft bounding-box size so larger aircraft frame comfortably. | fighters `0.5`-`1.5`, bombers `1.5`-`3.0` |
| `PlaneChaseFocusVerticalOffset` | `2.5` | Raises the chase focus above the aircraft so the aircraft sits below screen center and forward airspace remains visible. | `1`-`5` fighters/props, `3`-`8` large aircraft |
| `PlaneChaseScreenVerticalBias` | `4.0` | Raises the aim/framing point above the focus, keeping the crosshair/screen center out of the plane model. | `2`-`7`; reduce if the camera feels detached |
| `PlaneLookAheadDistance` | `12.0` | Held look-ahead focus distance along aircraft facing direction. The camera remains behind the aircraft. | `6`-`24` |
| `PlaneLookAheadSmoothing` | `0.35` | How quickly held Plane Look Ahead blends forward. | `0.20`-`0.55` |
| `PlaneLookAheadReturnSmoothing` | `0.25` | How quickly focus returns to centered chase framing after look-ahead release. | `0.15`-`0.45` |
| `FreelookReturnSmoothing` | `0.28` | How quickly released hold-freelook orbit offsets return to rear chase view. | `0.18`-`0.45` |
| `PlaneFreelookOrbitSensitivity` | `0.15` | Mouse sensitivity multiplier for hold-freelook target orbit yaw/pitch around the aircraft. | `0.08`-`0.30` |
| `PlaneFreelookYawSmoothing` | `0.28` | Smooths rendered freelook yaw separately from raw mouse target yaw. | `0.25`-`0.35` |
| `PlaneFreelookPitchSmoothing` | `0.28` | Smooths rendered freelook pitch separately from raw mouse target pitch. | `0.25`-`0.35` |
| `PlaneFreelookReturnSmoothing` | `0.18` | Smoothly blends raw and rendered orbit offsets back to rear chase after release. | `0.15`-`0.25` |
| `PlaneFreelookMaxPitchUp` | `75.0` | Clamps upward orbit pitch to prevent flips. | `60`-`80` |
| `PlaneFreelookMaxPitchDown` | `65.0` | Clamps downward orbit pitch to prevent flips. | `50`-`75` |
| `NewPlaneCameraPositionSmoothing` | `0.34` | How quickly camera position follows the desired chase point; higher values are snappier. | dogfight `0.28`-`0.50` |
| `NewPlaneCameraYawSmoothing` | `0.42` | How quickly normal chase yaw follows the aircraft-centered focus when freelook is not active. | `0.30`-`0.60` |
| `NewPlaneCameraPitchSmoothing` | `0.34` | How quickly normal chase pitch follows climbs/dives. | `0.24`-`0.50` |
| `NewPlaneCameraDistanceSmoothing` | `0.25` | How quickly speed/size/collision distance changes are restored or shortened. | `0.18`-`0.40` |
| `NewPlaneCameraFocusSmoothing` | `0.38` | How quickly the focus moves when held Plane Look Ahead is pressed/released. | `0.25`-`0.55` |
| `NewPlaneCameraPitchInfluenceSmoothing` | `0.30` | Smooths pitch influence so steep climbs/dives are readable without snapping above/below the aircraft. | `0.20`-`0.45` |
| `PlaneChasePitchInfluence` | `0.25` | Conservative pitch inheritance so the horizon feels more stable than the aircraft. | `0.15`-`0.35` |
| `NewPlaneCameraRollInfluence` | `0.12` | Conservative camera roll coupling; `0.0` keeps horizon stable and `1.0` fully rolls with the plane. | `0.0`-`0.15` for stable horizon |
| `PlaneChaseHorizonStabilization` | `true` | Documents/enables the stabilized chase-camera attitude profile. | `true` |
| `NewPlaneCameraCollision` | `false` | Deprecated compatibility alias for collision; enable with `EnableNewPlaneCameraCollision=true` for legacy config compatibility. | `true`/`false` |

Tuning guidance:

* **Fighters:** start around `PlaneChaseBaseDistance=14`-`24`, `PlaneChaseFocusVerticalOffset=2`-`4`, `PlaneChaseScreenVerticalBias=3`-`5`, `PlaneChaseSpeedDistanceScale=8`-`16`, `PlaneLookAheadDistance=10`-`16`, and keep roll influence near `0.10`-`0.18`.
* **Bombers / large aircraft:** use `PlaneChaseBaseDistance=24`-`45`, higher `NewPlaneCameraSizeDistanceScale`, a taller `NewPlaneCameraHeight`, and `PlaneChaseFocusVerticalOffset=4`-`8` so the full aircraft fits below center.
* **Jets:** use a higher max distance (`90`-`130+`), default-or-higher follow distance (`15`-`35`), and moderate held look-ahead (`12`-`24`) for target tracking without automatic drift.
* **Slow props:** keep distance lower (`12`-`22`), use modest screen bias (`2`-`4`), and keep speed scaling modest so the camera remains responsive while still framing the aircraft.


## New-flight plane mouse aim controls (currently in development)

Mouse aim is currently in development as a control foundation for fixed-wing planes using `UseNewMobilitySystem = true` only. It is disabled by default and does not affect legacy aircraft. When enabled globally, pilots can toggle it in a qualifying new-flight plane with `KeyPlaneMouseAim`; the mouse moves a separate desired aim yaw/pitch and the aircraft nose chases that aim through the existing new-flight authority, stall, energy, pitch-suppression, damping, and rotation-limit path. Advanced lead HUD polish is intentionally left for a later pass. Plane CCIP, where enabled by the aircraft ballistic computer, is a static ballistic bomb-impact pipper projected from the aircraft body frame and does not follow player freelook or mouse aim. If the predicted impact lies beyond currently loaded terrain or the ballistic trace cannot find a terrain collision during steep dives, extreme pitch angles, or high speed, CCIP continues to display by using a synthetic estimated impact based on aircraft altitude, motion/orientation-derived release velocity, bomb gravity, and acceleration; loaded real-terrain collisions still take priority and the debug overlay reports fallback state. In first-person pilot gunner mode, `KeyBombReticleMode` toggles a bomber sight that uses the same ballistic prediction, forces the first-person gunner camera yaw and pitch to the predicted impact point, locking vertical freelook to the calculated impact angle, and only appears while viewing from the gunner camera. The bomber-sight reticle is rendered as a local camera sight at screen center during this mode, so distant impact/focus points do not cull the sight itself. The bomber sight camera snap is first-person-only and remains active through short temporary unloaded-chunk or prediction gaps by reusing a recent bomber-sight impact or deriving a forward/down camera target; this fallback does not change CCIP reticle rendering or normal third-person CCIP behavior. Vehicle configs can set `EnableBombSight = false` to disable that bombsight on gunships or other non-bomber aircraft while leaving gunner mode and the normal CCIP ballistic computer behavior intact. When the bomber sight keybind hint is visible in third person, it reports `Bomb sight: OFF Third Person` to make the first-person-only requirement explicit.

This first implementation uses global client config keys. Per-plane mouse-aim overrides are not wired yet, so pack authors should tune conservatively.

| Key | Default | Notes |
| --- | --- | --- |
| `EnableMouseAimControls` | `false` | Master enable for the active-development mode; new-flight-model planes only. |
| `KeyPlaneMouseAim` | `49` | Toggle key while piloting a qualifying new-flight plane (N by default). |
| `KeyBombReticleMode` | `37` | Toggle first-person gunner bomber sight for bomb-capable planes (K by default); shown in the in-game key binding list. |
| `MouseAimSensitivity` | `0.18` | Scales raw mouse movement into desired aim yaw/pitch changes. |
| `MouseAimSmoothing` | `0.30` | Smooths desired aim motion to reduce jitter without snapping. |
| `MouseAimMaxPitchUp` | `70.0` | Nose-up aim clamp in degrees. |
| `MouseAimMaxPitchDown` | `55.0` | Nose-down aim clamp in degrees. |
| `MouseAimYawResponse` | `0.85` | Converts yaw error into yaw command before normal authority limits. |
| `MouseAimPitchResponse` | `0.85` | Converts pitch error into pitch command before normal authority limits. |
| `MouseAimAutoBankStrength` | `1.10` | Converts lateral aim error into coordinated auto-bank demand. |
| `MouseAimAutoBankMaxRoll` | `65.0` | Maximum target roll angle for auto-bank. |
| `MouseAimCenteringStrength` | `0.18` | Roll damping/leveling strength as the aim point returns toward center. |
| `MouseAimDebug` | `false` | Emits mouse-aim telemetry in the existing flight-control debug line even when `DebugFlightControl` is off. |
| `EnablePlaneMouseAimReticle` | `false` | Dedicated Forge overlay renderer draws the custom mouse-aim cursor and distinct plane/nose reticle while mouse aim is active. |
| `HideVanillaCrosshairInPlaneMouseAim` | `false` | Hides the vanilla Minecraft screen-center crosshair only in qualifying plane mouse-aim mode. |
| `PlaneMouseAimReticleTexture` | `textures/gui/plane_crosshair.png` | Texture path for the mouse aim cursor; the simple line fallback remains visible over it. |
| `PlaneMouseAimReticleScale` | `1.0` | Scales the mouse aim cursor. |
| `PlaneMouseAimReticleOpacity` | `0.90` | Mouse aim cursor opacity. |
| `PlaneNoseReticleScale` | `0.85` | Scales the plane/nose reticle drawn at screen center for the initial aligned-camera implementation. |
| `PlaneNoseReticleOpacity` | `0.70` | Plane/nose reticle opacity. |
| `PlaneMouseAimMaxScreenRadius` | `0.42` | Safe screen radius, as a fraction of the smaller screen dimension, that clamps the cursor on screen. |
| `PlaneMouseAimYawVisualRange` | `45.0` | Yaw error in degrees that maps to the configured safe screen radius. |
| `PlaneMouseAimReticleDebug` | `false` | Draws/logs reticle screen positions, aim angles/errors, and vanilla-crosshair suppression state. |

The custom cursor is required because the vanilla Minecraft crosshair is locked to screen center and would otherwise represent the plane/nose reticle, not the desired mouse-follow aim point. Mouse aim never directly sets aircraft rotation. It only generates pitch/yaw/roll commands that continue through the same new-flight control-authority, compressibility, unsupported-climb, stall, and angular-velocity integration code as other plane controls, so slow, stalled, damaged, or authority-limited aircraft may lag or fail to follow the cursor. Manual roll input is added to auto-bank in a predictable way, so roll keys remain available for corrections.

## Key config defaults

| Option | Default code | Default input |
| --- | --- | --- |
| `KeyUp` | `17` | W |
| `KeyDown` | `31` | S |
| `KeySubmarineAscend` | `200` | Up Arrow |
| `KeySubmarineDescend` | `208` | Down Arrow |
| `KeyRight` | `32` | D |
| `KeyLeft` | `30` | A |
| `KeySwitchGunner` | `35` | H |
| `KeySwitchHovering` | `57` | Space |
| `KeySwitchWeapon1` | `-98` | Middle Click |
| `KeySwitchWeapon2` | `34` | G |
| `KeySwitchWeaponMode` | `45` | X |
| `KeyZoom` | `44` | Z |
| `KeyCameraMode` | `46` | C |
| `KeyUnmountMob` | `21` | Y |
| `KeyFlare` | `47` | V |
| `KeyExtra` | `33` | F |
| `KeyCameraDistanceUp` | `201` | Page Up |
| `KeyCameraDistanceDown` | `209` | Page Down |
| `KeyFreeLook` | `29` | Left Control |
| `KeyPlaneLookAhead` | `56` | Left Alt |
| `KeyPlaneMouseAim` | `49` | N |
| `KeyBombReticleMode` | `37` | K |
| `KeyGUI` | `19` | R |
| `KeyGearUpDown` | `48` | B |
| `KeyPutToRack` | `36` | J |
| `KeyDownFromRack` | `22` | U |
| `KeyScoreboard` | `38` | L |
| `KeyMultiplayManager` | `50` | M |
| `KeyChaff` | `47` | V |
| `KeyMaintenance` | `47` | V |
| `KeyAPS` | `47` | V |
| `KeyUseWeapon` | `-99` | Right Click |
| `KeyAttack` | `-100` | Left Click |
| `KeyCurrentWeaponLock` | `-100` | Left Click |

`KeyEjectHeli` is initialized with `54` (Right Shift), but it is not included in the current `KeyConfig` array and therefore is not written with the generated key config.

## Item and block IDs

The source still initializes legacy numeric IDs for core items/blocks, including fuel, GLTD, chain, parachute, container, UAV stations, drafting table, wrench, rangefinder, Stinger/Javelin/RPG shared IDs, and Drafting Table block IDs. In modern Forge 1.7.10 modpacks, prefer resolving conflicts through generated config and registry names rather than hand-editing unless you know your pack's ID map.

## New plane third-person chase camera defaults

The new-flight-model plane chase camera is tuned around a stable follow distance instead of aggressive speed zoom. Its default base distance is 15 blocks, with the normal distance range held near 13-21 blocks before aircraft-size and collision handling. Speed-based distance scaling is disabled by default (`EnableSpeedBasedCameraDistance = false`); if enabled, keep `PlaneChaseSpeedDistanceScale` low and cap `PlaneChaseSpeedDistanceMaxBonus` around 2-5 blocks.

Plane camera tuning is exposed in the in-game MCHeli options opened from the aircraft `R` key menu: choose **MOD Options**, then **Render Settings**, then **Plane Camera** to adjust distance, FOV, speed bonus, freelook smoothing, and pitch/roll influence without restarting.

For awareness, prefer stable distance plus a wider FOV rather than large dynamic distance swings. `EnablePlaneChaseFOVOverride` is disabled by default for public testing; when manually enabled it uses `PlaneChaseFOV = 95`. Recommended chase FOV values are 85-105. `PlaneChaseFreelookFOV` defaults to the same value, and `PlaneChaseFOVSmoothing` blends the override when entering, freelooking, or leaving the camera.

Hold-freelook is hold-to-orbit: mouse input changes raw orbit yaw/pitch targets, while `PlaneFreelookYawSmoothing` and `PlaneFreelookPitchSmoothing` smooth the rendered orbit. `PlaneFreelookReturnSmoothing` controls the blend back to rear chase after release. Recommended starting values are sensitivity 0.15, yaw/pitch smoothing around 0.18-0.28, return smoothing around 0.12-0.22, max pitch up around 75 degrees, and max pitch down around 65 degrees. Freelook should feel smooth and camera-like, not raw or jittery.
# Vehicle access lock key

`KeyVehicleLock` defaults to LWJGL key code `24` (**O**). While directly riding
the pilot seat, press it to ask the server to lock or unlock vehicle entry.
The server, not the client key binding, decides whether the request is allowed.
