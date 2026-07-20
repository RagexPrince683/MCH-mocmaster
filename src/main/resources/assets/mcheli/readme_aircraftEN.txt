2016/04/17

; ★ Important ★
; The configuration file and model can be reloaded even while Minecraft is running.
; [ Enter the aircraft → Press R key for the supply screen → MOD Option → Development → Reload aircraft setting ]
; For reloading textures and sounds, use Minecraft’s default function instead of the Helicopter MOD.
; [ Press Esc for the game menu → Settings → Resource Pack → Done ]
Common Settings for Helicopters, Fighter Jets, Ground Vehicles, and Cars

DisplayName = AH-6 Killer Egg
; The displayed name Do not use full-width characters; use only half-width alphanumeric characters and symbols.

AddDisplayName = ja_JP, AH-6 キラーエッグ
; The name displayed when holding the item.
; If using full-width Japanese characters, set the file encoding to UTF-8.

ItemID = 28801
; Item ID (in Minecraft, it is used as +256).
; ItemID is not used in 1.7.2 and later, but must be set if supporting 1.6.4 or earlier.

AddTexture = sh60-us-1
AddTexture = sh60-jp-1
AddTexture = sh60-jp-2
; Additional textures (multiple allowed).
; By default, the PNG with the same name as the configuration file is used.
; This setting allows adding extra textures (file extension not required).

CameraPosition = 0.0, 1.1, 3.7
CameraPosition = 0.0, 1.1, 3.7, false
CameraPosition = 0.0, 1.1, 3.7, true, 30, 45
; Camera coordinates.
; Setting multiple values allows switching views with the H key.
; 1st, 2nd, and 3rd values: Coordinates.
; 4th value: Setting this to true always locks the view to the camera.
; 5th value: Horizontal angle.
; 6th value: Vertical angle.

CameraZoom = 3
; Maximum camera zoom.

HUD = heli, heli_gnr, none, gunner
; HUD configuration file per seat.
; In this example:
; - The pilot uses heli.txt.
; - The second seat uses heli_gnr.txt.
; - The third seat has no HUD.
; - The fourth seat uses gunner.txt.
; If fewer HUDs are specified than the total number of seats, the omitted seats follow these defaults:
; - Helicopter: HUD = heli, heli_gnr, gunner, gunner, gunner...
; - Fixed-wing aircraft: HUD = plane, plane, gunner, gunner...
; - Ground vehicles: HUD = vehicle only
; If the pilot seat has gunner mode enabled, the second seat’s HUD settings are used. Even single-seat aircraft should set a second seat HUD if gunner mode is available.
; Example: HUD = heli, heli_gnr

EnableGunnerMode = true
; Enables gunner mode (true = enabled, false = disabled).

EnableNightVision = true
; Enables night vision (true = enabled, false = disabled).

EnableEntityRadar = false
; Enables radar (true = enabled, false = disabled).

Speed = 0.6
; Aircraft speed (higher values = faster).

MotionFactor = 0.96
; Movement deceleration factor. Range: 0.0 to 1.0.
; Lower values cause stronger deceleration and slower speed.
; Default is 0.96; even slight changes affect performance.

Gravity = -0.04
; Gravity applied to the aircraft.
; Negative values make it fall downward.

GravityInWater = -0.04
; Gravity applied underwater.
; Negative values make it sink.

StepHeight = 2.5
; The maximum height of blocks the vehicle can step over.

MobilityYaw
MobilityYawOnGround
; Horizontal rotation capability (higher values = more maneuverable).
; MobilityYawOnGround affects only movement on land (not water).

MobilityRoll
; Roll rotation capability (higher values = faster roll).

MobilityPitch
; Vertical rotation capability (higher values = more maneuverable).
; For ground vehicles, this sets pitch angle limits.

MinRotationPitch
MaxRotationPitch
; Minimum and maximum pitch rotation range.
; - MinRotationPitch: -80 to 0
; - MaxRotationPitch: 0 to 80
; For helicopters and jets, enabling this also limits roll rotation.

MinRotationRoll
MaxRotationRoll
; Minimum and maximum roll rotation range.
; MinRotationRoll: -80 to 0
; MaxRotationRoll: 0 to 80
; For helicopters and jets, enabling this also limits vertical rotation.

UnmountPosition = 3.0, 1.0, -2.0
; Coordinates where the player appears after exiting the vehicle.

AddSeat = -0.45, 0.80, 1.20
AddSeat = 0.45, -0.50, 1.20
AddSeat = -0.90, -0.50, 0.20
AddSeat = 0.90, -0.50, 0.20, true
; Adds seats (except UAVs, every vehicle must have at least one seat).
; The first seat is the pilot seat.
; Parameters: Position (X, Y, Z).
; The fourth parameter (true/false) determines if the seat position adjusts to the pilot’s facing direction (mainly for tank turrets).

AddGunnerSeat = -0.45, 0.80, 1.20, 0.0, 2.00, -1.01, true
AddGunnerSeat = -0.45, 0.80, 1.20, 0.0, 2.00, -1.01, true, -60, 78, true
; Adds a gunner seat.
; Parameters: Seat position (X, Y, Z), camera position (X, Y, Z).
; The seventh parameter (true/false) determines if the player can switch views.
; The ninth and tenth parameters set the camera's vertical limits.
; The last parameter (true/false) adjusts the seat based on the turret’s direction.

AddFixRotSeat = -0.45, 0.80, 1.20, 0.0, 2.00, -1.01, true, -50, 40
; Similar to AddGunnerSeat, but the camera is fixed.
; The ninth and tenth parameters set fixed camera angles (horizontal and vertical).
; The camera cannot be moved with the mouse unless using FreeLook mode (Ctrl key).
★★★★★ Aircraft Loading Function

; The aircraft loading function works when either:
; The carrier (host) specifies the aircraft it can load (AddRack).
; The boarding aircraft specifies the rack number (RideRack).

AddRack = container, 0.0, 1.4, -4.7, 0.0, 1.0, -16.1
AddRack = container / ah-64, 0.0, 1.4, -4.7, 0.0, 1.0, -16.1, 5.0, 20
AddRack = helicopter/vehicle / t-4, 0.0, 1.4, -4.7, 0.0, 1.0, -16.1, 5.0, 100000, 0.0, 0.0
; This is the carrier’s setting.
; Param1: Entity name that can be loaded.
; Param2~4: Rack coordinates.
;Param5~7: Rack entrance/exit X, Y, Z. The entity can be loaded when it is near these coordinates. It is also unloaded from here.
;Example: For an AC-130, set the rack coordinates inside the aircraft and the entrance/exit near the rear hatch.
;Param8: Range (radius) from the entrance. This and later parameters are optional.
;Param9: Altitude at which the parachute deploys. Setting a very high value will prevent parachute deployment.
;Param10: Horizontal angle of the loaded entity.
;Param11: Vertical angle of the loaded entity.

A rack that allows loading containers, helicopters, and other entities onto an aircraft like mobs.
Entities can be specified by name using a combination of:
;container → All containers
;helicopter → All helicopters
;plane → All fixed-wing aircraft
;vehicle → All ground vehicles
;Specific aircraft name → e.g., ah-64, t-4, s-75

RideRack = c5, 1
;This is for setting the entity that will ride.
;RideRack = [Aircraft Name], [Rack Number (1~)]
;Used when an entity needs to ride on another aircraft.

ExclusionSeat = 15, 17
;Multiple parameters can be set.
;Specifies seat or rack exclusivity, meaning if one seat/rack is occupied, others cannot be used.
;Example: ExclusionSeat = 3, 4, 5
;If a mob is in seat 3, seats 4 and 5 cannot be used.
;If a mob is in seat 4, seats 3 and 5 cannot be used.
;If a mob is in seat 5, seats 3 and 4 cannot be used.
Seat/rack numbers are assigned sequentially in order of appearance in the configuration file:
;AddSeat → Seat 1
;AddGunnerSeat → Seat 2
;AddSeat → Seat 3
;AddRack → Rack 4
;AddRack → Rack 5
;AddRack → Rack 6
;It is recommended to list all seats first, then racks, for clarity.

TurretPosition = 0.0, 0.0, 0.25
;Sets the turret's rotation center.
;Not recommended—ideally, the X, Z coordinates should be 0.

AddWeapon = m230, 0.00, 0.90, 2.54, 0.0, 0.0, true, 2
;Adds a weapon. The name must match a file in the weapons folder (excluding the extension).
;If the same weapon is added multiple times (e.g., m134), they are treated as a single weapon fired in sequence from different points.
;Parameters:
;Weapon file name
;Position (X, Y, Z)
;Rotation angles (horizontal, vertical)
;Whether the pilot can use it
;Seat number
;Default yaw angle
;MinYaw (counterclockwise limit)
;MaxYaw (clockwise limit)
;MinPitch (upward limit)
;MaxPitch (downward limit)
;Pilot usage rules:
;true, 2 → Seat 2 player can use it, pilot can use if seat 2 is empty.
;false, 2 → Only seat 2 player can use it, pilot cannot.
;false, 1 → Only the pilot can use it (not recommended).
;true, 1 → Only the pilot can use it.
;If omitted, defaults to true, 1.
;Difference between AddWeapon and AddTurretWeapon:
;AddTurretWeapon adjusts the firing position based on turret orientation.

AddSearchLight = 0.71, -0.02, 0.02, 0x50FFFFFF, 0x10FFFFC0, 60.0, 20.0, 0, 0
;Adds a searchlight.
;Types:
;AddSearchLight: Light follows the player's aim.
;AddFixedSearchLight: Fixed light that always faces the same direction.
;AddSteeringSearchLight: Light moves with vehicle steering.
;Parameters:
;Position (X, Y, Z)
;Start color
;End color
;Distance
;Beam radius
;Yaw
;Pitch
;Steering angle (for AddSteeringSearchLight).

AddPartLightHatch = 0.32, 0.23, 1.83, -1, 0, -0.024, 90
;Adds a part that opens when the searchlight is ON.
;Important: AddSearchLight or AddFixedSearchLight must be present for this to work.

AddRecipe = " Y ", "YXY", " YD", X, iron_block, Y, iron_ingot, D, dye,2
;Adds a crafting recipe.
;"YXY" represents a row in the crafting grid.
;Uses the same format as Forge's GameRegistry.addRecipe.
;Example:
;X = iron_block
;Y = iron_ingot
;D = dye (e.g., green dye).
;For modded items: Use modname:itemname format, e.g., mcheli:ah-6.
;AddShapelessRecipe defines a shapeless recipe.

FlareType = 1
;Flare type settings:
;0 → No flares
;1 → Standard flares
;2 → Large aircraft flares
;3 → Spreads flares sideways
;4 → Fires flares forward
;5 → Fires flares downward
;10 → Tank smoke discharger

Float = true
;Determines if the vehicle floats on water.
FloatOffset = -1.0
;Adjusts floating height (negative values allowed).

SubmergedDamageHeight = 2
;Defines the height up to which water does not cause damage.
;Example: 2 → No damage when submerged up to 2 blocks.

MaxHP = 100
; Durability

ArmorDamageFactor = 0.5
; Coefficient of damage taken by the vehicle
; 1.0 for 100%, 0.5 for 50% damage reduction

ArmorMinDamage = 5
; Minimum damage value
; Any damage smaller than this will be ignored

ArmorMaxDamage = 500
; Maximum damage
; Damage greater than this will be rounded down to the specified value
; For example, setting it to 100 will only inflict 100 damage even if 300 is dealt

InventorySize = 18
; Vehicle inventory size (must be a multiple of 9)

DamageFactor = 0.2
; Damage coefficient for when the player takes damage
; If set to 0.2, only 20% of the original damage is taken
; Regardless of this, the vehicle also takes the same damage as the player

Sound = heli
; The sound file played when the throttle is increased
; In this example, sounds/heli.ogg will be used

UAV = true
SmallUAV = true
; When true, the vehicle becomes a drone and cannot be piloted.
; Cannot be generated or controlled from stations other than the UAV station.
; If UAV = true: Considered a large UAV, and cannot be controlled by handheld UAV devices.
; If SmallUAV = true: Considered a small UAV and can be controlled by handheld UAV devices.
; Note: The UAV station can control all UAVs regardless of size.
; The handheld UAV controller can only control small UAVs.

TargetDrone = true
; Only applicable to fighter aircraft. When true, the vehicle becomes an unmanned target drone and cannot be piloted.
; Cannot be generated from stations other than the UAV station.
; Upon generation, it will fly to a low altitude and continue to circle.

OnGroundPitch = Pitch angle
; Set the pitch angle when the vehicle is on the ground.
; For example, aircraft like the Zero fighter will have a slight upward pitch when on the ground.

AddPartHatch = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0-180
; Adds a hatch that can be opened/closed with the Z key.
; Model file name AircraftName_hatch?.obj
; ? starts from 0 and increases sequentially.
; The first AddPartHatch will use AircraftName_hatch0.obj, the second will use AircraftName_hatch1.obj.
; If the model file is not found, it won't be displayed (no model is needed if display is not required).

AddPartSlideHatch = MovementX, MovementY, MovementZ
; Adds a sliding hatch type.
; Model file name (for both rotating/sliding) AircraftName_hatch?.obj (refer to AddPartHatch naming rules).

AddPartCamera = CoordX, CoordY, CoordZ, Yaw Linked, Pitch Linked
; Adds a part that always faces the player's direction.
; If there is a mob in the second seat, it will face the direction of that mob.
; Model file name AircraftName_camera?.obj (refer to AddPartHatch naming rules).

AddPartRotation = 0.00, 9.00, -31.17, 0,-1,0, 1.3, false
; AddPartRotation = PositionX, Y, Z RotationAxisX,Y,Z RotationSpeed, AlwaysRotate
; Adds a part that rotates at regular intervals.

AddPartWeapon = m230, false, true, true, -2.51, 1.29, -1.51
AddPartWeapon = m102_105mm, false, true, true, -2.51, 1.29, -1.51, 1.00
AddPartWeapon = rehinmetall_apfsds / rehinmetall_he, false, true, false, 0.00, 2.10, 0.00, 0
AddPartTurretWeapon = mg7_62mm, false, true, true, -0.83, 3.39, -0.57, 0
AddPartRotWeapon = m134_r50, false, true, true, -1.825, 1.475, -0.25, 1,0,0
AddPartWeaponChild = false, true, 0.00, 0.5, 3.00
AddPartWeaponMissile = aim120, false, false, false, -2.51, 1.29, -1.51
; Weapon part settings for helicopters & fighters.
; AddPartWeapon = weapon name (none if no weapon), hidden during gunner use?, Yaw linked, Pitch linked, Rotation position X,Y,Z, Recoil distance
; AddPartRotWeapon = weapon name (none if no weapon), hidden during gunner use?, Yaw linked, Pitch linked, Rotation position X,Y,Z, Rotation axis X,Y,Z
; AddPartWeaponChild = Yaw linked, Pitch linked, Rotation position X,Y,Z
; Weapons linked with AddWeapon will change angles accordingly.
; Weapon angle range will follow the angle set in AddWeapon.
; Recoil distance is how far the weapon recoils.
; AddPartRotWeapon is used for Gatling-type weapons, and will rotate when the weapon is used.
; Model file name AircraftName_weapon?.obj (refer to AddPartHatch naming rules).
;
; AddPartWeaponChild can be added as a smaller part under AddPartWeapon.
; Add it after the AddPartWeapon line.
; Can add multiple smaller parts like ground-based weapons.
; Model file name AircraftName_weapon?_0.obj (? is the number of the parent part).
; Linked weapon name and hidden during gunner use will be the same as the parent part.
;
; AddPartWeaponMissile hides the weapon until it's usable after firing.
; Example for missiles or bombs.
;
; AddPartTurretWeapon adjusts the display position based on turret rotation, otherwise same as AddPartWeapon.

AddPartWeaponBay = WeaponName, PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0-180
; Adds a rotating weapon bay.
AddPartSlideWeaponBay = WeaponName, MovementX, MovementY, MovementZ
; Adds a sliding weapon bay.
; Model file name (for rotating/sliding) AircraftName_wb?.obj
; Model file name AircraftName_wb?.obj (refer to hatch naming rules).

AddPartCanopy = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0-180
; Adds a rotating canopy.
AddPartSlideCanopy = MovementX, MovementY, MovementZ
; Adds a sliding canopy.
; Model file name (for rotating/sliding) AircraftName_canopy?.obj
; Canopies can be added multiple times.
; Model file name AircraftName_canopy?.obj (refer to hatch naming rules).
; For compatibility, leaving the number part out will treat the model as AircraftName_canopy0.obj.

AddPartThrottle = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle (0-180), MovementX, MovementY, MovementZ
; Adds a part that rotates/moves with throttle linkage.
; Rotation angle is required.

AddPartLG = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180 [, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180]
AddPartLGRev = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180 [, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180]
AddPartSlideRotLG = MoveAmountX, MoveAmountY, MoveAmountZ, PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180
AddPartLGHatch = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180 [, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0~180]
; Add landing gear
; Automatically extends/retracts during takeoff and other events
; Model file name: AircraftName_lg?.obj (naming convention is based on hatch)
; AddPartLGRev behaves opposite to AddPartLG.
; AddPartLGHatch opens only when the gear is folding or unfolding.
;
; AddPartLG         ... Gear folding 0 → 90
; AddPartLGRev      ... Gear folding 90 → 0
; AddPartSlideRotLG ... Gear folding 0 → 90
; AddPartLGHatch    ... Gear folding 0 → 90 → 0

TrackRollerRot = 30
; Track roller rotation speed: setting a value smaller than 0 should reverse the rotation

AddTrackRoller = -1.72, 0.77, 5.04
; Adds a track roller, position is specified, negative X value places on the right, positive on the left
; Works similarly to the tracks, but can be set without tracks

AddCrawlerTrack = false, 0.37, -2.09, 1.03/-3.41, 0.72/-3.57, 0.37/-3.42, -0.15/-2.55, -0.25/-2.16, -0.25/3.88, -0.13/4.21, 0.52/5.29, 0.78/5.39, 1.03/5.28, 1.10/5.04, 1.15/-3.12
; AddCrawlerTrack = ReversedTrack, DistanceBetweenTracks, TrackXPosition, TrackRotationPointY/Z, TrackRotationPointY/Z, TrackRotationPointY/Z, ...
; Reversing the track direction may fix strange movements.
; For clockwise settings when viewed from the left side of the tank, use false. If counter-clockwise, use true.
; Works even without rollers.
; When tested in the game, the positions will be shown as red-to-blue points.

PartWheelRot = 40
; Tire rotation speed, larger values make it rotate faster

AddPartWheel = -1.05, 0.157, 1.965, 30
; Adds a wheel at X, Y, Z with steering angle
AddPartWheel = 0.68, 0.19, 1.20, 30, 0.0, 1.0, 0.2, 0.68, 0.19, 0.70
; Adds a wheel at X, Y, Z with steering angle, rotation axis (X, Y, Z), and rotation position (X, Y, Z)
; Steering angle is the maximum Y-axis angle when turning
; If rotation axis is omitted, (0, 1, 0) is used by default

AddPartSteeringWheel = -0.54, 0.88, 0.48, 0.0, 1.0, -1.7, 130
; Adds a steering wheel at X, Y, Z with rotation axis (X, Y, Z) and max rotation angle

ThrottleUpDown = 1.0
ThrottleUpDownOnEntity = 2.0
; Throttle up/down factor
; A smaller value makes throttle less responsive, taking longer to take off
;
; ThrottleUpDownOnEntity is the throttle up/down factor when the vehicle is on another entity (default 2.0)
; When on another entity, the calculation is as follows:
; ThrottleUpDown * entity speed * ThrottleUpDownOnEntity -> throttle responsiveness
; Example: With ThrottleUpDownOnEntity = 2.0, if the vehicle is in a minecart with a max speed of ~1.7, the formula is:
; 1.7 * 2.0 = 3.4, meaning the vehicle can take off at about 1/3 the distance

AutoPilotRot = -0.4
; Autopilot rotation angle. The larger the value, the bigger the rotation.
; A value of 0 keeps the vehicle going straight.
; Negative values rotate left, positive values rotate right.

ConcurrentGunnerMode = true
; The second seat can also enter gunner mode.

Regeneration = true
; Crew members in seats after the first automatically regenerate health

ParticlesScale = 0.1
; Dust and other effects size adjustment. 0.1 is the default.
; Larger values increase the size of effects.

FuelSupplyRange = 25
; The range at which the vehicle can supply fuel to others
; All vehicles within this range will be supplied with fuel. In this case, 25 meters.
; The vehicle supplying fuel will not lose fuel during this process.

AmmoSupplyRange = 35
; The range at which the vehicle can supply ammo to others
; All vehicles within this range will be supplied with ammo. In this case, 35 meters.
; The vehicle supplying ammo will not lose ammo during this process.

MaxFuel = 600
; Maximum fuel capacity
FuelConsumption = 0.5
; Fuel consumption per second
; Flight time [seconds] = MaxFuel / FuelConsumption
; Example: 1200 sec = 600 / 0.5

Stealth = 0.5
; Stealth setting (0.0~1.0).
; Default = 0.0
; Larger values make it harder for missiles to lock onto the vehicle.
; Lock-on time increases, and lockable distance decreases.

SmoothShading = false
; Switch between enabling and disabling smooth shading
; false results in flat shading with no angle interpolation
; true enables smooth shading, making angles appear smoother
; Setting SmoothShading to false in mcheli.cfg will make all aircraft use flat shading

HideEntity = false
; Set whether the player riding the vehicle should be hidden
; true = hide
; false = show

EntityWidth  = 0.9
EntityHeight = 0.9
; Set the drawing size (width and height) of the mob riding the vehicle
; Range is -100.0 to 0.0 to 100.0
; Setting 0.5 will reduce the size by half

EntityPitch = 45
EntityRoll  = 20
; Set the drawing angle of the mob riding the vehicle (-360 to 360)

CanRide = false
; Set whether the vehicle can be ridden
; true = can ride (default)
; false = cannot ride

BoundingBox = CenterX, CenterY, CenterZ, Width, Height, DamageMultiplier
; Add hitbox for the vehicle
; Only hits from machine guns and missiles from this mod
; Does not hit blocks or other entities
; Can be displayed by turning on TestMode in the config or in the in-game MOD Option
; Damage multiplier applies to received damage. If not specified, it defaults to 1.0
; Example: Damage multiplier of 0.5 means half damage, 3.0 means triple damage

Category = W.A
; Set the category for the vehicle
; Used only for rearranging in the creative tab

CanMoveOnGround = false
CanRotOnGround  = false
; Set whether movement and rotation are allowed on the ground
; false = disables movement or rotation on the ground
; CanMoveOnGround prevents movement on the ground
; CanRotOnGround prevents rotation on the ground

EnableParachuting = true
; If true, enables parachuting
; Mobs from seat 3 and onwards, and players from seat 3 onwards can parachute by pressing space

MobDropOption  = 0.0, 0.0, -11.5,  10
; MobDropOption = Drop position X, Drop position Y, Drop position Z, Drop interval (1/20 seconds)
; Additional options for dropping mobs

RotorSpeed = 50.0
; Blade rotation speed. Larger values mean faster rotation.
; Negative values will reverse the rotation, but using negative values is not recommended.

;***********************************************************************************
■ Helicopter settings file helicopters/abc.txt, models/helicopters/abc.obj, textures/helicopters/abc.png, textures/items/abc.png
;***********************************************************************************

; To add a helicopter, the following four files are required (all in lowercase)
; ・Helicopter config file in the helicopters folder
; ・Helicopter model in the models/helicopters folder
; ・Helicopter texture file in the textures/helicopters folder
; ・Item texture file in the textures/items folder

EnableFoldBlade = false
; Enable blade folding functionality (true = enabled, false = disabled)

AddRotor = 6, 60, 0.00, 3.35, 0.00, 0.0, 1.0, 0.0, true
AddRotor = 2, 60, 0.50, 1.90, -6.55, 1.0, 0.0, 0.0
; Add rotors (any number is fine)
; In this example, the first is the main rotor, and the second is the tail rotor
; Only the blades of the first rotor can be folded
; Parameters are: number of blades, blade angle, position (X, Y, Z), rotation axis (X, Y, Z), blade folding functionality (true/false)
; Model file name: AircraftName_rotor?.obj (naming convention is based on hatch)

※ AddRotorOld is for older models and should not be used.

AddRepellingHook = 0.60, 2.75, -14.21, 30
; AddRepellingHook = Hook coordinates X, Y, Z, Interval to release mobs

;***********************************************************************************
■ Fighter Aircraft Configuration Files: planes/abc.txt, models/planes/abc.obj, textures/planes/abc.png, textures/items/abc.png
;***********************************************************************************

; To add a fighter aircraft, the following 4 files are necessary (all in lowercase): ; ・Aircraft configuration file in the planes folder ; ・Aircraft model in the models/planes folder ; ・Aircraft texture file in the textures/planes folder ; ・Item texture file in the textures/items folder

AddPartRotor = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle(-180~180) ; Adds a rotor ; It will not rotate when the throttle is raised. It will rotate during VTOL. ; Model file name: AircraftName_rotor?.obj (follow the hatch naming convention) ; (No model is needed for most aircraft except for the Osprey)

AddBlade = NumberOfBladesToRender, AngleBetweenBlades, PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ ; Must be added after AddPartRotor. ; Each rotor blade ; Model file name: AircraftName_blade?.obj (follow the hatch naming convention)

AddPartWing = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0～180 ; Adds foldable main wings ; Model file name: AircraftName_wing?.obj (follow the hatch naming convention)

AddPartPylon = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0～180 ; Adds pylons for foldable main wings ; Model file name: AircraftName_wing?_pylon?.obj (follow the hatch naming convention) ; AddPartWing must be added before AddPartPylon. ; Example: ; AddPartWing = 1.50, 2.50, -4.57, 0, 1,0, 35; Model name: AircraftName_wing0.obj ; AddPartPylon = 6.69, 2.50, -7.18, 0,-1,0, 35; Model name: AircraftName_wing0_pylon0.obj ; AddPartPylon = 3.92, 2.50, -6.34, 0,-1,0, 35; Model name: AircraftName_wing0_pylon1.obj

PivotTurnThrottle = 0.0 ; Ground movement speed when turning ; Setting to 0 will make the vehicle rotate on the spot. ; Setting a value greater than 0 will move while turning (used for tanks). ; Example: ; For extreme turns = 0 ; For regular turns = a value greater than 0

EnableBack = true ; Enables backward movement

VariableSweepWing = true SweepWingSpeed = 1.2 ; Variable wing settings ; Only valid when foldable wings are added using AddPartWing ; VariableSweepWing = true will allow wings to fold in the air ; SweepWingSpeed = 1.2 sets the speed at which the wings fold

AddPartNozzle = PositionX, PositionY, PositionZ, RotationAxisX, RotationAxisY, RotationAxisZ, RotationAngle 0～180 ; Adds a nozzle to the aircraft ; Smoke effects appear when the throttle is raised ; The nozzle will rotate during VTOL ; Model file name: AircraftName_nozzle?.obj (follow the hatch naming convention) ; Smoke effects from the nozzle can be resized using ParticlesScale

EnableVtol = true ; Whether VTOL functionality is enabled (false = no VTOL, true = VTOL capable)

DefaultVtol = true ; Default VTOL state when VTOL functionality is enabled ; If set to true, the aircraft will automatically be in VTOL mode on the ground

VtolYaw = 0.3 ; Horizontal rotation amount in VTOL mode

VtolPitch = 0.3 ; Vertical rotation amount in VTOL mode

EnableEjectionSeat = true ; Enables ejection seat functionality ; If enabled, an ejection seat option will appear in the GUI ; If there is 1 seat, 1 can be stored, if there are 2 seats, both can be stored

AddParticleSplash = 1.0, 0.97, 13.19, 3, 9.0, 1.1, 20, 0.30, -0.03 ; AddParticleSplash = PositionX, Y, Z, DisplayCount, Size, Speed, DisplayTime, Rise, Gravity ; Generates water splashes when moving on water ; This is unrelated to EnableSeaSurfaceParticle, and particles will appear even if it's set to false.

EnableSeaSurfaceParticle = true ; Whether to generate water splashes when flying over water ; It is related to ParticlesScale and increasing it will make the splash effect larger (recommended value around 0.7) ; This is independent of AddParticleSplash

;***********************************************************************************
■ Ground Vehicle Configuration Files: vehicles/abc.txt, models/vehicles/abc.obj, textures/vehicles/abc.png, textures/items/abc.png
;***********************************************************************************

; To add a ground vehicle, the following 4 files are necessary (all in lowercase):
; ・Ground vehicle configuration file in the vehicles folder
; ・Ground vehicle model in the models/vehicles folder
; ・Ground vehicle texture file in the textures/vehicles folder
; ・Item texture file in the textures/items folder

AddPart = Param1, Param2, Param3, Param4, PositionX, PositionY, PositionZ
; Adds a part that rotates with the player
; Param1 = Whether the part should be hidden in first-person view (true = visible, false = hidden)
; Param2 = Whether the part should rotate horizontally with the player (true = rotate, false = do not rotate)
; Param3 = Whether the part should rotate vertically with the player (true = rotate, false = do not rotate)
; Param4 = Part type, 0 = normal (no functionality), 1 = rotates when using a weapon, 2 = recoils when using a weapon
; Model file name: VehicleName_part?.obj (follow the hatch naming convention)

AddChildPart = Param1, Param2, Param3, Param4, PositionX, PositionY, PositionZ
; Additional part to be added after AddPart
; Parameters are the same as AddPart.
; This part has a special naming convention.
; Model file name: VehicleName_part?_#.obj
; The model added by AddPart needs to have _# added to its name (# starts from 0 and increments).
; Example:
; AddPart = true, true, false, 0, 0.00, 0.00, 0.00 → Model name: VehicleName_part0.obj
; AddChildPart = false, false, true, 0, -1.00, 0.00, 2.00 → Model name: VehicleName_part0_0.obj
; AddChildPart = false, false, true, 0, 1.00, 0.00, 2.00 → Model name: VehicleName_part0_1.obj

; RotationPitchMax and RotationPitchMin are old settings and should not be used.

;***********************************************************************************
■ Vehicle Configuration Files: tanks/abc.txt, models/tanks/abc.obj, textures/tanks/abc.png, textures/items/abc.png
;***********************************************************************************

; To add a vehicle, the following 4 files are necessary (all in lowercase):
; ・Vehicle configuration file in the tanks folder
; ・Vehicle model in the models/tanks folder
; ・Vehicle texture file in the textures/tanks folder
; ・Item texture file in the textures/items folder

DefaultFreelook = true
; Whether to enable freelook immediately after getting into the vehicle
; Mainly used for tanks

OnGroundPitchFactor = 2.0
OnGroundRollFactor = 1.3
; Speed at which the vehicle tilts based on the terrain
; A higher value will make the vehicle tilt more quickly to match the terrain
; Faster vehicles should have higher values, slower vehicles should have smaller values
; Too large a value will cause the screen to shake intensely
; Too small a value will cause the vehicle to not adjust well to terrain and may cause clipping with blocks

CameraRotationSpeed = 25
; Camera rotation speed
; For tanks, this can be used to limit the turret rotation speed

WeightType = Tank
; Tank or Car or Unknown
; Vehicle weight type
; Tank : No damage from colliding with mobs, destroys more blocks
; Car  : Takes damage from colliding with mobs, destroys fewer blocks
; Behavior for types other than Tank or Car is undefined
; The number of blocks destroyed can be configured in mcheli.cfg

WeightedCenterZ = 0.0
; Center of mass Z-coordinate setting
; Z-coordinate of the center of mass when the vehicle tilts to match the terrain
; * This may not function properly, so if it feels off, it's better not to use it

SetWheelPos = 1.75, -0.24, 4.85, 3.02, 1.44, -1.54, -2.91
; SetWheelPos = X-coordinate, Y-coordinate, Z-coordinate1, Z-coordinate2, Z-coordinate3 ...
; Specifies the contact point with the ground. The vehicle will tilt according to this contact point.
; No need to set a negative value for the X-coordinate
; ★ It is strongly recommended to set the Y-coordinate to -0.24 regardless of the vehicle
