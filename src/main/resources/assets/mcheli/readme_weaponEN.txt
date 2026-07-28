
;***********************************************************************************
;■ Weapon Configuration File: weapons/***.txt, sound/***_snd.ogg
;***********************************************************************************

; ★ Important ★
; Weapon configuration files can be reloaded even while Minecraft is running.
; Mount the vehicle → Press the R key to open the supply screen → MOD Option → Development → Reload All Weapons
; This will reload all weapons, including handheld ones.

; To add a weapon, the following 2 files are required (all in lowercase):
; ・Add the weapon configuration file (txt) in the weapons folder
; ・Add the weapon sound file with the same name + _snd in the sound folder
; For example, for a weapon named abc, you need weapons/abc.txt and sound/abc_snd.ogg
; ※ As of version 0.9.4, sound files are no longer mandatory.

; Some numerical parameters have upper and lower limits.

DisplayName = M134 Minigun
; Display name (use only half-width characters and symbols)

Type = MachineGun1
; Weapon type, choose one from the following:
; MachineGun1  : Fixed direction machine gun ... M134
; MachineGun2  : Machine gun that aligns with the player's view ... M230
; Torpedo      : Torpedo that moves towards a target underwater ... Mk46
; CAS          : Close Air Support to target area ... A-10
; Rocket       : Fixed direction unguided rocket ... Hydra70 or SNEB68mm
; ASMissile    : Missile targeting ground area ... AGM119
; AAMissile    : Missile tracking flying mobs ... AIM92
; TVMissile    : Missile that the player can control after launch ... AGM114[TV]
; ATMissile    : Missile tracking ground mobs ... AGM114
; Bomb         : Bomb dropped directly downwards ... CBU-100
; MkRocket     : Rocket that targets impact area for artillery strike ... Hydra 70mm M264RP
; Dummy        : Non-functional weapon, used to display text in the weapon slot
; Smoke        : Creates a contrail ... Smoke white
; Dispenser    : Uses an item at impact location ... Water Dispenser (Till)
; TargetingPod : Spots mobs, players, and blocks ... targeting_pod_block

Power = 8
; Damage

DamageFactor = tank, 2.0
; Damage multiplier
; The first parameter can be set to one of the following:
; player   : Player
; heli or helicopter : Helicopter
; plane    : Fixed-wing aircraft
; tank     : Tank or car
; vehicle  : Ground vehicle
; The second parameter is the damage multiplier [0~]. For example, if Power is 10 and multiplier is 3.4, the damage will be 34.
; This setting applies if multiple lines are written.

Acceleration = 4.0
; Bullet speed (maximum 4.0 for most, but MachineGun1, MachineGun2, and Rocket can go up to 100.0)

AccelerationInWater = 4.0
; Speed of torpedoes underwater (maximum 4.0)

VelocityInWater = 0.5
; Acceleration underwater
; Underwater, this value is multiplied by the velocity every tick.

Explosion = 0
; Explosion power at impact (0 = no explosion, 1 = gas shell power, 2~)

ExplosionInWater = 0
; Explosion power at impact underwater

ExplosionBlock = 0
; Block destruction power at impact. 0 means no block destruction

ExplosionAltitude = 10
; Height from ground at which the explosion will occur
; Setting it to 10 means it will explode if within 10m of the ground

DelayFuse = 30
; Delay fuse: counts until the bullet disappears after impact
; If Explosion/ExplosionInWater is not 0, it will explode upon disappearing

Bound = 0.4
; Rebound strength at impact
; If Bound is used, DelayFuse must also be set, otherwise, it will explode immediately after impact.

TimeFuse = 30
; Time fuse: counts until the bullet disappears after launch
; If Explosion/ExplosionInWater is not 0, it will explode upon disappearing

Flaming = false
; Whether to spread fire upon impact (false = disabled, true = enabled)
; ※ Only enabled when Explosion > 0

Sight = MoveSight or None or MissileSight
; Type of sight displayed on screen
; MoveSight  : Sight that moves with the vehicle's orientation
; MissileSight : Lock-on type sight for mobs (required for AAMissile/ATMissile)

Zoom = 4.2, 9.2
; Only for handheld weapons
; Zoom level when the scope is not equipped. If multiple values are separated by commas, they can be toggled with the Z key.

Group = MainGun
; Set the weapon group
; When one weapon in the same group is used, all will be reloaded.
; For example, if you divide the main gun of a tank into three types of shells as follows,
; rehinmetall_apfsds.txt, rehinmetall_he.txt, canistershell.txt, and set Group = MainGun,
; when one is used, all the others will reload as well. The ammo won't decrease.
; 1st: rehinmetall_apfsds
; 2nd: rehinmetall_he
; 3rd: canistershell
; This setting prevents switching to the next weapon immediately after using the first one.

Delay=5
; Wait time before next use (approx. 1/20 second units), smaller means faster.

ReloadTime=80
; Wait time until reload is complete (1/20 second units), smaller means faster
; *When setting reload time to 0, set the ammo count to a non-zero value.

Round=100
; Ammo count. Set to 0 if not used, or omit this parameter entirely.

SoundVolume=3
; Volume when the weapon is used.
; In Minecraft, if set above 1.0, the maximum volume is reached.
; (To decrease volume, set it below 1.0)
; If it's above 1.0, the sound can be heard from far away.

SoundPitch=1.0
; Pitch of the sound (0.0 ~ 1.0).

SoundPitchRandom=0.1
; Range for random pitch variation (0.0 ~ 1.0)
; For example:
; SoundPitch = 0.8
; SoundPitchRandom = 0.2
; The resulting pitch of the sound will range between 0.6 and 0.8.

SoundDelay=1
; Wait time between each sound when firing repeatedly.
; Without this, sounds like M134 will overlap and other sounds may be drowned out.

Sound=rocket_snd
; Specify the sound file. No need for file extension.
; If this is not set, the default weapon name_snd.ogg will be used.

LockTime=20
; Time to lock on for missile types. A larger value means longer lock time.

RidableOnly=true
; Set to allow lock only when the player is riding the vehicle.

ProximityFuseDist=1.0
; The operating range of the proximity fuse for lock-on missile types.
; Set to 1, it will explode within 1 meter.

RigidityTime=0
; Count before missile tracking begins after firing.
; If omitted, the default value is 7.

Accuracy=1
; Accuracy for unguided bullets or rockets. The higher the value, the greater the error.

Bomblet=25
; The number of sub-munitions that will deploy after use. Used for cluster bombs.

BombletSTime=5
; Time until sub-munitions deploy.

BombletDiff=0.7
; Spread rate of sub-munitions.

ModeNum=2
; Number of weapon modes that can be switched using the X key.
; Only applicable for certain weapon types:
; Type = MachineGun2 → Switch to HE rounds (explosive rounds). Disabled if Explosion is 0.
; Type = TVMissile → Switch to a regular guided missile (non-TV missile).
; Type = ATMissile → Switch to TA mode (Top Attack).
; Type = Rocket → Switch to HEIAP rounds (drop multiple sub-munitions in the air).
; Only 1 or 2 modes are allowed.
; Default is 1 if omitted.

Piercing=2
; Number of blocks the weapon can penetrate.

HeatCount=20
; The amount of heat generated per use for weapons with barrel heat.

MaxHeatCount=150
; Maximum heat capacity.

FAE=true
; ON/OFF for fuel-air explosives.
; Set to true to enable FAE.
; FAE won't destroy blocks.

ModelBullet=bullet
ModelBomblet=cbc
; Specify the model file for bullets and sub-munitions.
; In this case, models/bullets/bullet.obj and textures/bullets/bullet.png are used for bullets.
; Cluster bomb sub-munitions will use models/bullets/cbc.obj and textures/bullets/cbc.png.

Destruct = true
; Activates self-destruction of the vehicle when used
; Only available when Type = Bomb.
; Effective only when the vehicle is a UAV helicopter.

Gravity = -0.04
GravityInWater = 0.0
; The falling speed of the warhead.
; A larger absolute value means it falls faster.
; GravityInWater is the falling speed in water.

GuidedTorpedo = true
; Switches between guided/unguided torpedoes.
; When set to true, the torpedo will home in on the specified block.
; When set to false, the torpedo is unguided and will travel straight from the point of impact.

TrajectoryParticle = flame
; Specifies the "trail" effect when certain weapons are used (such as particles when a missile is tracking).
; Options include:
; none          ... No effect
; explode       ... Smoke effect
; flame         ... Fire effect
; hugeexplosion ... Huge explosion effect
; largeexplode  ... Large explosion effect
; largesmoke    ... Large smoke effect
; smoke         ... Smoke effect
;
; For advanced users: This is a string used in spawnParticle, so other strings can be specified as well.
; Particle is deprecated from version 1.0.0, use AddMuzzleFlash or AddMuzzleFlashSmoke instead.

TrajectoryParticleStartTick = 10
; The count before the TrajectoryParticle effect starts.

DisableSmoke = true
; Disables smoke effects for specific weapons
; (Not the firing effects, but the smoke when moving).

AddMuzzleFlash  =  0.5,             0.20,   1,        150,254,219,184
; AddMuzzleFlash = Distance from the origin, Size, Display time, A, R, G, B
; ★ Note: If the weapon's usage interval is around 5, the display may not function correctly, so avoid using this.

AddMuzzleFlashSmoke  =  2.2,             1,       5.0,    2.0,  15,      180,250,245,240
; AddMuzzleFlashSmoke = Distance from the origin, Display count, Size, Range, Display time, A, R, G, B
; ★ Note: If the weapon's usage interval is around 5, the display may not function correctly, so avoid using this.

SetCartridge = cartridge, 0.0, 0, 0, 2.00, -0.04, 0.40
; Settings for dropping empty cartridges when the weapon is used
; SetCartridge = model_name, Acceleration, Yaw, Pitch, ModelScale, Gravity, Bound
; model_name   : Model name (lowercase, half-width)
; Acceleration : Strength of the throw (0 means it falls straight down)
; Yaw          : Angle in the horizontal direction from the weapon (positive: left, negative: right)
; Pitch        : Angle in the vertical direction from the weapon (positive: down, negative: up)
; ModelScale   : Display scale
; Gravity      : Gravity strength
; Bound        : Bounciness when hitting a block

MaxAmmo = 40
; Maximum ammo that the vehicle can hold.
SuppliedNum = 10
; Number of rounds added per ammo supply.
Item =  3, iron_ingot
Item =  4, gunpowder
Item =  2, redstone
; Items and stack quantities required for supply (iron ingot, gunpowder, redstone only).

; MaxAmmo = 40
; SuppliedNum = 10
; Item =  3, iron_ingot
; Item =  4, gunpowder
; Item =  2, redstone
; This example shows that a reloading action will consume 3 iron ingots, 4 gunpowder, and 2 redstone
; to replenish 10 rounds. To reach the max 40 rounds, the following would be consumed:
; 12 iron ingots, 16 gunpowder, 8 redstone.

BulletColor        = 255, 255, 255, 255
BulletColorInWater = 255,  25,  25,  75
; Bullet color settings (0 to 255). In order: Alpha, Red, Green, Blue.
; BulletColorInWater specifies the color in water.

SmokeColor  = 230, 200, 20, 80
; Smoke color settings (0 to 255). In order: Alpha, Red, Green, Blue.
SmokeSize   = 2.0
; Smoke size.
SmokeMaxAge = 500
; Maximum duration the smoke is displayed.

DisplayMortarDistance = true
; Displays the distance to impact for mortars.

FixCameraPitch = true
; Fixes the vertical camera angle to 0.

CameraRotationSpeedPitch = 0.3
; The multiplier for the camera's rotation speed (smaller values allow for finer adjustments to the impact point).

DispenseItem = flint_and_steel
; Use the specified item upon impact.
; There are items that will have an effect and others that will not.
; You won't know the effect until you try.
; In the above setting, flint and steel will be used at the impact point.
; An exception is the water bucket (water_bucket), which will not set water,
; but will instead extinguish fire and lava near the impact point.

DispenseRange = 4
; The range (in blocks) at which the item specified in DispenseItem will be used.

Recoil = 1.1
; The strength of the shake of the vehicle when the weapon is used.

RecoilBufCount = 40, 5
; RecoilBufCount = Recoil count, Recoil count multiplier during recoil
; Increasing the recoil count extends the total recoil time.
; Increasing the multiplier during recoil speeds up the recoil time.

Target = monsters/others
; Specifies the types of entities to target or whether to mark blocks.
; Multiple settings can be specified, separated by /.
; Specifying "block" disables all other settings.
;
; planes        ... Fixed-wing aircraft from the Helicopter Mod
; helicopters   ... Helicopters from the Helicopter Mod
; vehicles      ... Ground vehicles from the Helicopter Mod
; players       ... Other players
; monsters      ... Monsters
; others        ... Other mobs
; block         ... Marks blocks instead of using the spotting function.

Length = 100
; Specifies the spotting distance (in blocks).
; For example, a setting of 15 will mark a distance of 15 blocks in a straight line.

Radius = 45
; Specifies the angle for the spotting range.
; A setting of 45 will result in a 45-degree radius.

MarkTime = 10
; Specifies the time in seconds the spotting mark will be displayed.
; A setting of 10 will display the mark for 10 seconds.
