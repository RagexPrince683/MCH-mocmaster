2016/03/28

;***********************************************************************************
;■HUD Settings File hud/***.txt
;***********************************************************************************

; ★ Important ★
; HUD can be reloaded while Minecraft is running.
; [ Ride the vehicle → Press R for the supply screen → MOD Option → Development → Reload All HUD ]
; The texture reload uses Minecraft's default function, not from the Helicopter Mod.
; [ Press Esc to open the game menu → Settings → Resource Packs → Done ]

;************************************************************************************************************
; Only the HUD setting files' order matters.
; The processing is executed in the order specified in the configuration file, and this becomes the drawing order.

;************************************************************************************************************
; About formulas
; ■ Formulas can be applied to values other than the data parts of DrawString and DrawCenteredString.
;   Example: Color = 123 + 456
; ■ You can use hexadecimal in formulas by adding a # or 0x before the number.
;   Example: Color = #123 + 0x456 + 789
; ■ You can use "variables" in formulas.
;   Example: Color = hp_rto * 100
;       hp_rto is the remaining durability of the vehicle, ranging from 0.0 to 1.0. By multiplying it by 100, it becomes a value between 0.0 and 100.0.
; ■ Conditions can be used in formulas.
;   Format: condition ? value if true : value if false
;   Example: Color = hp_rto > 0.2 ? 0xFFFFFFFF : 0x00000000
;       When hp_rto is greater than 0.2, the color will be set to 0xFFFFFFFF, and if it is 0.2 or below, it will be set to 0x00000000.

; "List of Variables"
; center_x    : X coordinate of the screen center
; center_y    : Y coordinate of the screen center
; width       : Screen width
; height      : Screen height
; yaw         : Vehicle yaw (horizontal rotation) (-180.0 to 180.0)
; pitch       : Vehicle pitch (vertical rotation) (-90.0 to 90.0)
; roll        : Vehicle roll (-180.0 to 180.0)
; plyr_yaw    : Player yaw (horizontal rotation) (-180.0 to 180.0)
; plyr_pitch  : Player pitch (vertical rotation) (-90.0 to 90.0)
; altitude    : Vehicle altitude (0 or greater), distance from the vehicle to the block below (never negative)
; sea_alt     : Vehicle altitude (0 or greater), distance from sea level (never negative)
; hp          : Remaining vehicle durability (0 or greater)
; max_hp      : Maximum vehicle durability (0 or greater)
; hp_rto      : Remaining HP ratio of the vehicle (0.0 to 1.0)
; throttle    : Current throttle of the vehicle (0.0 to 1.0)
; pos_x       : X coordinate of the vehicle
; pos_y       : Y coordinate of the vehicle
; pos_z       : Z coordinate of the vehicle
; motion_x    : Vehicle acceleration in the X direction
; motion_y    : Vehicle acceleration in the Y direction
; motion_z    : Vehicle acceleration in the Z direction
; fuel        : Remaining fuel ratio of the vehicle (0.0 to 1.0)
; low_fuel    : Fuel warning display, flashes between 0 and 1 when low fuel (0 = fuel present, 1 = fuel low)
; stick_x     : Amount of stick movement in the X direction (-1.0 to 1.0)
; stick_y     : Amount of stick movement in the Y direction (-1.0 to 1.0)
; reloading   : Weapon reload state (0 = not reloading, 1 = reloading)
; reload_time : Remaining reload time ratio (0.0 to 1.0), 0.0 means reload complete
; wpn_heat    : Weapon heat (0.0 to 1.0)
; is_heat_wpn : Whether the weapon uses heat (0 = no, 1 = yes)
; dsp_mt_dist : Whether to display impact distance (0 = do not display, 1 = display)
; mt_dist     : Impact distance (cannot calculate if less than 0.0)
; have_radar  : Whether a radar is equipped (0 = no radar, 1 = radar present)
; radar_rot   : Radar rotation angle. The entity position is updated when the radar completes a full rotation.
; vtol_stat   : Fixed-wing aircraft only. 0 = normal, 1 = switching to VTOL, 2 = in VTOL mode
; free_look   : 0 = normal, 1 = in FREE LOOK mode
; cam_mode    : 0 = normal, 1 = NIGHT VISION, 2 = THERMAL VISION
; cam_zoom    : Camera zoom level. 1.0 ~ 10.0
; auto_pilot  : 0 = normal, 1 = in Auto Pilot mode
; have_flare  : Whether a flare ejector is equipped (0 = no, 1 = yes)
; can_flare   : Whether flares can be used (0 = can be used, 1 = ready but not available)
; sight_type  : Type of sight (0 = none, 1 = MoveSight, 2 = MissileSight)
; lock        : Missile lock-on state (0.0 to 1.0)
; color       : Current color setting (0x00000000 ~ 0xFFFFFFFF)
; inventory   : Number of inventory slots (0 ~ 54)
; hovering    : Hovering state (0 = not hovering, 1 = hovering)
; is_uav      : Whether it is a UAV (0 = not a UAV, 1 = UAV)
; uav_fs      : UAV signal strength, the larger the value, the closer to the UAV station (0.0 ~ 1.0)
; gunner_mode : Whether in gunner mode (0 = normal mode, 1 = gunner mode)
; time        : Minecraft in-game time (1 day = 24000 counts, 0 = 6:00 AM, 6000 = noon)
; test_mode   : Whether in test mode (0 = normal mode, 1 = test mode)

;************************************************************************************************************
; Call Other HUD Drawing Files
; By specifying the HUD text file name, the drawing process of that configuration file will be executed.
;
; For example, if Call = common_pilot is specified, the common_pilot.txt will be executed.
; You can also call additional files within the configuration, but it is not possible to call the same file twice.
; For example, if Call = heli is specified within heli.txt, it will be ignored since heli.txt is already running.
; Additionally, if Call = plane is specified in heli.txt and Call = heli is specified in plane.txt,
; heli.txt will be ignored since it is already running.

Call = common_pilot


;************************************************************************************************************
; End the current HUD file drawing
;
; If called via a Call, it returns to the caller
; When heli.txt calls uav_fs.txt and Exit is used in uav_fs.txt, the drawing of uav_fs ends,
; and the next line in heli.txt where uav_fs.txt was called will continue processing.
;
Exit


;************************************************************************************************************
; Conditional Branching
; Only execute processing if the specified condition is met
;
; If the condition is not 0, process1, process2, and process3 will be executed.
; If the condition is 0, process1, process2, and process3 will not be executed.
; If = condition
;     process1
;     process2
;     process3
; EndIf
; Nested If is not supported (You cannot put an If inside another If between If and EndIf).
;
; In the example below, if is_heat_wpn is 1, Color and DrawRect will be executed.
If = is_heat_wpn==1
    Color = 0xFF28d448
    DrawRect = -145, 57, 43, 10
EndIf


;************************************************************************************************************
; Color Settings
; After setting the color, the color of subsequent text or lines will be drawn with this color.
;
; There are 2 ways to set the color.
; 1. By specifying 1 parameter
; 2. By specifying 4 parameters
;
; If setting with 1 parameter: The order is opacity, red, green, blue.
; Example: 0xAABBCCDD means opacity=AA, red=BB, green=CC, blue=DD
;
; If setting with 4 parameters: The order is opacity, red, green, blue.
; Example: 12, 34, 56, 78 means opacity=12, red=34, green=56, blue=78
;
; You can specify either in hexadecimal or decimal.
; Example: The following settings are all valid:
Color = 0xFFFFFFFF
Color = #FFFFFFFF
Color = 0xFF,  40, 212,  72


;************************************************************************************************************
; Draw Text Strings
; DrawString: Draws text from the specified coordinates to the right
; DrawCenteredString: Draws text centered on the specified coordinates
;
; Parameter 1 = X coordinate relative to the center of the screen
; Parameter 2 = Y coordinate relative to the center of the screen
; Parameter 3 = Display format (printf or String.format style, commas are not allowed)
;              The number of % symbols must match the number of subsequent parameters.
; Parameter 4 and beyond = ★ Data (optional, no upper limit)
;
; Example (Altitude): DrawString = -20,  20, "Hello"
;                     Displays the string "Hello" on the screen.
;
; Example (Altitude): DrawString =  0,  40, "[ %3d ]", ALTITUDE
;                     Displays ALTITUDE (height) as a 3-digit integer → [ 123 ]
;
; Example (Date): DrawString =  0, -30, "%tY/%tm/%td", DATE, DATE, DATE
;                 Displays DATE (date) formatted as / separated → 2014/10/11
;
; ★ Data (case-insensitive)
; Data cannot include calculations.
;
; As shown below, %s, %d, %f are specific to each data type. However, you can specify the number of digits between % and the letter.
; For example, %f will display an undefined number of digits after the decimal point → 123.45678
; %.2f would always display 2 digits after the decimal point → 123.45
; %.0f → 123    %.1f → 123.4    %.2f → 123.45
; The same applies to %d. You can refer to printf for more details.
;
;  NAME        : %s : Display the vehicle's name (same as the item name)
;  ALTITUDE    : %d : Display altitude as an integer.
;  DATE        : %tY %tm %td %td %tH %tM %tS ...
;              : Displays the current date and time. %tY=Year, %tm=Month, %td=Day, %tH=Hour, %tM=Minute, %tS=Second
;  MC_THOR     : %d : Displays Minecraft time in hours. 0-23. Minecraft time is 72 times faster than real life.
;  MC_TMIN     : %d : Displays Minecraft time in minutes. 0-59.
;  MC_TSEC     : %d : Displays Minecraft time in seconds. 0-59.
;  MAX_HP      : %d : Displays maximum durability as an integer.
;  HP          : %d : Displays remaining durability as an integer.
;  HP_PER      : %f : Displays remaining percentage as a decimal (100.0-0.0).
;  THROTTLE    : %f : Displays throttle as a decimal (100.0-0.0).
;  POS_X       : %f : Displays the X coordinate of the vehicle as a decimal.
;  POS_Y       : %f : Displays the Y coordinate of the vehicle as a decimal.
;  POS_Z       : %f : Displays the Z coordinate of the vehicle as a decimal.
;  MOTION_X    : %f : Displays the acceleration in the X direction as a decimal.
;  MOTION_Y    : %f : Displays the acceleration in the Y direction as a decimal.
;  MOTION_Z    : %f : Displays the acceleration in the Z direction as a decimal.
;  YAW         : %f : Displays the vehicle's yaw (horizontal angle) as a decimal.
;  PITCH       : %f : Displays the vehicle's pitch (vertical angle) as a decimal.
;  ROLL        : %f : Displays the vehicle's roll angle as a decimal.
;  PLYR_YAW    : %f : Displays the player's yaw (horizontal angle) as a decimal.
;  PLYR_PITCH  : %f : Displays the player's pitch (vertical angle) as a decimal.
;  INVENTORY   : %d : Displays the number of items in the vehicle's inventory.
;  WPN_NAME    : %s : Displays the current weapon name.
;  WPN_AMMO    : %s : Displays the current ammo count for the selected weapon.
;  WPN_RM_AMMO : %s : Displays the remaining ammo for the selected weapon.
;  RELOAD_PER  : %f : Displays reload status as a decimal (100.0-0.0).
;  RELOAD_SEC  : %f : Displays remaining reload time as a decimal (0.0-).
;  MORTAR_DIST : %f : Displays mortar distance (if less than 0.0, calculation is not possible).
;  MC_VER      : %s : Displays the Minecraft version.
;  MOD_VER     : %s : Displays the mod version.
;  MOD_NAME    : %s : Always displays "MC Helicopter MOD".
;  TVM_POS_X   : %f : Displays TV missile X coordinate.
;  TVM_POS_Y   : %f : Displays TV missile Y coordinate.
;  TVM_POS_Z   : %f : Displays TV missile Z coordinate.
;  TVM_DIFF    : %f : Displays the distance between the TV missile and the vehicle.
;  CAM_ZOOM    : %f : Displays the camera zoom level.
;  UAV_DIST    : %f : Displays the distance to the UAV station.
;  KEY_GUI     : %s : Displays the GUI key name (default key is R).
;
DrawCenteredString = 0,  40, "[ %3d ]", ALTITUDE
→ When ALTITUDE is 12, the screen will show [  12 ] as a 3-digit number. Since 12 is 2 digits, the hundreds place will be a space.
DrawString         = 0,  30, "%tY %tm %td  [ %02d:%02d:%02d ]", DATE, DATE, DATE, MC_THOR, MC_TMIN, MC_TSEC
→ If the in-game time is 12:34:56 on October 24, 2014, the screen will display 2014 10 24  [ 12:34:56 ].
DrawString         = 0,  20, "%3d/%3d  = %.1f%%", HP, MAX_HP, HP_PER
→ If the current HP is 50 and the max HP is 100, the screen will show  50/100  = 50%.
DrawString         = 0,  10, "[ %s ]", name
→ If the vehicle is AH-64, the screen will display AH-64D Apache Longbow.
DrawCenteredString = 0, -10, "HUD Test"
→ If no data is provided, the screen will always display HUD Test.


;************************************************************************************************************
; Draw texture
; Recommended texture size is 256x256. Parameters 2 and onwards can be integers or decimals.
; Parameter 1: Texture file name (without extension, file in assets\mcheli\textures\gui)
; Parameter 2: X-coordinate from the center of the screen
; Parameter 3: Y-coordinate from the center of the screen
; Parameter 4: Width on the screen
; Parameter 5: Height on the screen
; Parameter 6: X-coordinate to read the texture from
; Parameter 7: Y-coordinate to read the texture from
; Parameter 8: Width to read the texture from
; Parameter 9: Height to read the texture from
; Parameter 10: Rotation angle on the screen (optional)
DrawTexture = heli_hud, -100.0, 20, 50, 20, 0, 0, 64, 64, 90.0


;************************************************************************************************************
; Draw rectangle
; Parameter 1: X-coordinate from the center of the screen
; Parameter 2: Y-coordinate from the center of the screen
; Parameter 3: Width on the screen
; Parameter 4: Height on the screen
DrawRect = -20, -30, 40*throttle, -20


;************************************************************************************************************
; Draw line
; If parameters 5 and onwards are specified, multiple lines will be drawn.
; 1, 2 → 3, 4 → 5, 6 → 7, 8 ...
; Parameters 1-4 are required, 5 and onwards are optional if needed.
; Parameter 1: X-coordinate from the center of the screen
; Parameter 2: Y-coordinate from the center of the screen
; Parameter 3: X-coordinate from the center of the screen
; Parameter 4: Y-coordinate from the center of the screen
; Parameters 5 onwards: X-coordinate, Y-coordinate, X-coordinate, Y-coordinate...
DrawLine = -40, 30, 40, 30
DrawLine = -20, -30, -20+40*throttle, -30, -20+40*throttle, -20, -20, -20, -20, -30


;************************************************************************************************************
; Draw dashed line
; If parameters 7 and onwards are specified, multiple dashed lines will be drawn.
; 3, 4 → 5, 6 → 7, 8 ...
; Parameters 1-6 are required, 7 and onwards are optional if needed.
; Parameter 1: The factor for glLineStipple
; Parameter 2: The pattern for glLineStipple
; Parameter 3: X-coordinate from the center of the screen
; Parameter 4: Y-coordinate from the center of the screen
; Parameter 5: X-coordinate from the center of the screen
; Parameter 6: Y-coordinate from the center of the screen
; Parameters 7 onwards: X-coordinate, Y-coordinate, X-coordinate, Y-coordinate...
DrawLineStipple = 0xF0F0, 1, -40, 30, 40, 30
DrawLineStipple = 0xFF00, 1, -20, -30, -20+40*throttle, -30, -20


;************************************************************************************************************
; Draw entity positions (radar)
; DrawEntityRadar draws non-monster mobs, DrawEnemyRadar draws monsters
; Parameter 1: Rotation angle
; Parameter 2: X-coordinate from the center of the screen
; Parameter 3: Y-coordinate from the center of the screen
; Parameter 4: Width
; Parameter 5: Height
DrawEntityRadar = plyr_yaw, 40, 30, 32, 32
DrawEnemyRadar = plyr_yaw, 40, 30, 32, 32


;************************************************************************************************************
; Draw angular graduations
; DrawGraduationYaw: Horizontal rotation angle graduation
; DrawGraduationPitch1: Vertical rotation angle graduation
; DrawGraduationPitch2: Vertical and roll angle graduations
; Parameter 1: Rotation angle
; Parameter 2: Roll rotation angle
; Parameter 3: X-coordinate from the center of the screen
; Parameter 4: Y-coordinate from the center of the screen
DrawGraduationYaw = plyr_yaw, 0, 0, -100
DrawGraduationPitch1 = plyr_pitch, 0, 0, 0
DrawGraduationPitch2 = plyr_pitch, -roll, 0, 0


;************************************************************************************************************
; Draw angle difference between player and aircraft
; Small rectangle indicating where the player is facing relative to the aircraft
; Parameter 1: X-coordinate from the center of the screen
; Parameter 2: Y-coordinate from the center of the screen
DrawCameraRot = 0, 60
