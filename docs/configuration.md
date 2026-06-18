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

## General options

| Option | Default | Notes |
| --- | --- | --- |
| `TestMode` | `false` | Development/test toggle. |
| `EnableCommand` | `true` | Enables `/mcheli` subcommands. |
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
| `placetimer` | `60` | Placement timer value. |
| `RangeFinderSpotDist` | `400` | Rangefinder spot distance. |
| `RangeFinderSpotTime` | `15` | Rangefinder spot duration. |
| `RangeFinderConsume` | `true` | Rangefinder consumes required item/ammo when spotting. |
| `EnablePutRackInFlying` | `true` | Allows rack operations during flight. |
| `EnableDebugBoundingBox` | `false` | Enables debug bounding boxes. Can be toggled in memory with `/mcheli showboundingbox`. |
| `InvertMouse` | `false` | Inverts aircraft mouse controls. |
| `MouseSensitivity` | `30.0` | MCHeli mouse sensitivity. |
| `MouseControlStickModeHeli` | `false` | Enables stick-style mouse mode for helicopters. |
| `MouseControlStickModePlane` | `false` | Enables stick-style mouse mode for planes. |
| `MouseControlFlightSimMode` | `true` | Flight-sim mouse mode (`Yaw:key, Roll=mouse` per source comment). |
| `AutoThrottleDownHeli` | `true` | Auto-throttle-down behavior for helicopters. |
| `AutoThrottleDownPlane` | `false` | Auto-throttle-down behavior for planes. |
| `AutoThrottleDownShip` | `false` | Auto-throttle-down behavior for ships. |
| `AutoThrottleDownTank` | `false` | Auto-throttle-down behavior for tanks. |
| `SwitchWeaponWithMouseWheel` | `true` | Allows mouse-wheel weapon switching. |
| `LWeaponAutoFire` | `false` | Auto-fire behavior for light weapons. |
| `DisableItemRender` | `1` | Valid range noted in source: `0 ~ 3`; `1` recommended. |
| `HideKeybind` | `false` | Hides keybind display/help where implemented. |
| `RenderDistanceWeight` | `1000.0` | Render-distance weight for mod rendering. |
| `EnableAircraftLODRender` | `true` | Enables client-only far-distance model displays for aircraft, tanks, turrets, and ships. |
| `AircraftLODStartDistance` | `256.0` | Distance where tracked vehicles switch to cheaper model-only rendering. |
| `AircraftLODFarDistance` | `4096.0` | Maximum distance for client-only LOD snapshots. If positive and below start distance, it is corrected upward. |
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

These options are client-side visual/readability settings for pilots flying planes that use the new flight/new mobility system. They do not change flight physics, stall behavior, pitch authority, throttle behavior, weapons, targeting, HUD rendering, or aircraft balance. Legacy aircraft, helicopters, tanks, turrets, ships, passengers, and gunners keep the existing camera path unless the gated plane conditions are met.

| Config key | Default | Purpose |
| --- | ---: | --- |
| `EnableNewPlaneThirdPersonCamera` | `true` | Enables the smooth chase camera only for third-person pilot view in new-flight planes. |
| `NewPlaneCameraDistance` | `9.0` | Camera distance in blocks behind the plane. |
| `NewPlaneCameraDebugDistance` | `0.0` | `DebugFlightControl`-only distance override; set to `20`-`30` to prove the render path is consuming the custom camera, or `0` to disable. |
| `NewPlaneCameraHeight` | `2.4` | Vertical offset in blocks above the plane. |
| `NewPlaneCameraSideOffset` | `0.0` | Optional left/right offset in blocks for off-center chase views. |
| `NewPlaneCameraPositionSmoothing` | `0.22` | How quickly the camera position catches up to the desired chase point; higher values are snappier. |
| `NewPlaneCameraRotationSmoothing` | `0.16` | How quickly camera yaw and pitch recenter behind the aircraft; higher values are snappier. |
| `NewPlaneCameraRollInfluence` | `0.15` | How much aircraft roll is applied to the camera horizon, from `0.0` stable horizon to `1.0` full roll coupling. |
| `NewPlaneCameraCollision` | `true` | Shortens/moves the chase camera when a block is between the aircraft and desired camera point. |

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
