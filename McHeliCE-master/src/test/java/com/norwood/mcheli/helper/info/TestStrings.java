package com.norwood.mcheli.helper.info;

public class TestStrings {

    public static final String MERKAVA = """
            DisplayName = MerkavaMk4\n
            AddDisplayName = ja_JP, Merkava Mk.4\n
            ItemID = 30075\n
            MaxHp = 300\n
            Speed = 0.6\n
            Sound = tank\n
            ThrottleUpDown = 20.0\n
            MobilityYawOnGround = 2.5\n
            MinRotationPitch = -50\n
            MaxRotationPitch = 5\n
            MobilityRoll = 0\n
            Gravity = -0.1\n
            GravityInWater = -0.1\n
            DamageFactor = 0.0\n
            CameraZoom = 3\n
            CameraPosition = 0.52, 3.70, -0.1, true\n
            EnableNightVision = true\n
            CameraRotationSpeed = 25\n
            StepHeight = 1.5\n
            DefaultFreelook = true\n
            TurretPosition = 0.0, 0.0, 0.25\n
            FlareType = 10\n
            ThirdPersonDist = 8\n
            \n
            OnGroundPitchFactor = 2.0\n
            OnGroundRollFactor  = 1.3\n
            \n
            ; 指定した高さまでの水に触れてもダメージを受けない\n
            ; 2であれば2ブロックまでダメージ無し\n
            SubmergedDamageHeight = 3\n
            \n
            HideEntity = true\n
            \n
            ; Tank or Car or Unknown\n
            WeightType = Tank\n
            \n
            ; 重心Z座標設定\n
            WeightedCenterZ = 0.0\n
            \n
            ;SetWheelPos =  X座標, Y座標,  Z座標1, Z座標2, Z座標3 ...\n
            ; X座標のマイナス側は必要なし\n
            SetWheelPos =  1.75,  -0.24,  4.85, 3.02, 1.44, -1.54, -2.91\n
            \n
            ;HUD = mbt_hud, gunner\n
            HUD = none, none
            ;HUD = ww2tank_hud, ww2tank_hud\n
            \n
            AddGunnerSeat = 0.00,  2.50,  0.25,   2.00,  3.50,  0.25, false, -90, 90, true\n
            AddGunnerSeat = 0.00,  1.00, -1.25,  -0.55,  3.80, -1.16, false, -90, 90, true\n
            \n
            ; 地面の滑りやすさ、小さいほど摩擦が大きくなり動きにくくなる\n
            MotionFactor = 0.9\n
            \n
            ;ArmorMinDamage 最小ダメージ、これより小さいダメージは無効化される\n
            ArmorMinDamage = 12\n
            ;ArmorMaxDamage 最大ダメージ、これより大きいダメージは最大ダメージに丸められる\n
            ArmorMaxDamage = 500\n
            ;ArmorDamageFactor ダメージ係数上記のMin/Max範囲チェック後のダメージの係数 1で100%, 0.5で50%\n
            ArmorDamageFactor = 0.90\n
            ;後退可能にする\n
            EnableBack = true\n
            \n
            ;PivotTurnThrottle を0より大きい値にすると超信地旋回ができなくなる。デフォルトは0\n
            PivotTurnThrottle = 0.0\n
            ;AddCrawlerTrack = 履帯の表裏逆転,  1つの履帯の間隔, 履帯のXの位置, 履帯の回転ポイントY/Z, 履帯の回転ポイントY/Z, 履帯の回転ポイントY/Z, ...\n
            ;転輪がなくても動作する\n
            AddCrawlerTrack = false, 0.37, -2.09,  1.03/-3.41, 0.72/-3.57, 0.37/-3.42, -0.15/-2.55, -0.25/-2.16, -0.25/3.88, -0.13/4.21, 0.52/5.29, 0.78/5.39, 1.03/5.28, 1.10/5.04, 1.15/-3.12\n
            AddCrawlerTrack = false, 0.37,  2.09,  1.03/-3.41, 0.72/-3.57, 0.37/-3.42, -0.15/-2.55, -0.25/-2.16, -0.25/3.88, -0.13/4.21, 0.52/5.29, 0.78/5.39, 1.03/5.28, 1.10/5.04, 1.15/-3.12\n
            \n
            ; 転輪の回転速度：0より小さい値を設定すると逆回転する\n
            TrackRollerRot = 30\n
            \n
            ;Rev Test\n
            ;AddCrawlerTrack = false, 0.30, -1.64, 1.47/3.60, 1.73/3.49, 1.84/3.23, 1.78/-3.60, 1.68/-3.86, 1.42/-3.96, 1.16/-3.86, 1.06/-3.60, -1.32/-2.47, -1.32/2.17, 1.22/3.49\n
            ;AddCrawlerTrack = false, 0.30,  1.64, 1.47/3.60, 1.73/3.49, 1.84/3.23, 1.78/-3.60, 1.68/-3.86, 1.42/-3.96, 1.16/-3.86, 1.06/-3.60, -1.32/-2.47, -1.32/2.17, 1.22/3.49\n
            ;AddCrawlerTrack = true,  0.30, -1.64, 0.22/3.49,-0.32/2.17, -0.32/-2.47, 0.06/-3.60, 0.16/-3.86, 0.42/-3.96, 0.68/-3.86, 0.78/-3.60, 0.84/3.23, 0.73/3.49, 0.47/3.60\n
            ;AddCrawlerTrack = false, 0.30,  1.64, 0.22/3.49,-0.32/2.17, -0.32/-2.47, 0.06/-3.60, 0.16/-3.86, 0.42/-3.96, 0.68/-3.86, 0.78/-3.60, 0.84/3.23, 0.73/3.49, 0.47/3.60\n
            \n
            ;転輪を追加する、設定は座標だけで、X軸が負の値だと右側、正の値だと左側の転輪となる\n
            ;履帯と同じ動きをするが、履帯がなくても設定可能\n
            AddTrackRoller = -1.72,  0.77,  5.04\n
            AddTrackRoller = -1.72,  0.18,  3.88\n
            AddTrackRoller = -1.72,  0.18,  2.80\n
            AddTrackRoller = -1.72,  0.18,  1.62\n
            AddTrackRoller = -1.72,  0.18,  0.37\n
            AddTrackRoller = -1.72,  0.18, -1.12\n
            AddTrackRoller = -1.72,  0.18, -2.19\n
            AddTrackRoller = -1.72,  0.73, -3.10\n
            \n
            AddTrackRoller =  1.72,  0.77,  5.04\n
            AddTrackRoller =  1.72,  0.18,  3.88\n
            AddTrackRoller =  1.72,  0.18,  2.80\n
            AddTrackRoller =  1.72,  0.18,  1.62\n
            AddTrackRoller =  1.72,  0.18,  0.37\n
            AddTrackRoller =  1.72,  0.18, -1.12\n
            AddTrackRoller =  1.72,  0.18, -2.19\n
            AddTrackRoller =  1.72,  0.73, -3.10\n
            \n
            \n
            AddPartWeapon = rehinmetall_apfsds / canistershell, false, true, false,  0.00, 1.52, 0.25, 0\n
            AddPartWeaponChild = false, true, 0.00, 2.01, 1.62, 0.0\n
            AddPartWeaponChild = false, true, 0.00, 2.01, 1.62, 0.5\n
            AddPartTurretWeapon = mg7_62mm, false, true, true,  -0.83, 3.39,-0.57, 0\n
            \n
            AddTurretWeapon = rehinmetall_apfsds, 0.00, 2.22, 0.00,  0.0,  0.0, true, 1, 0,-360,360, -50, 7\n
            AddTurretWeapon = canistershell,      0.00, 2.22, 0.00,  0.0,  0.0, true, 1, 0,-360,360, -50, 7\n
            AddTurretWeapon = 60mm_mortar,        0.90, 3.00, 0.52,  0.0,-45.0, true, 1, 0,-360,360, -85, 7\n
            AddTurretWeapon = mg12_7mm_l,         0.00, 2.92, 2.70,  0.0,  0.0, true, 1, 0,-360,360, -50, 7\n
            AddTurretWeapon = mg7_62mm,          -0.83, 3.39,-0.57,  0.0,  0.0, false,2, 0,-360,360, -80, 50\n
            \n
            MaxFuel         = 1200\n
            FuelConsumption = 2.5\n
            \n
            \n
            ;RideRack = 乗る機体名, ラック番号 (1～) \n
            RideRack = c5, 1\n
            RideRack = c5, 2\n
            \n
            AddRecipe = "XIX",  " X ",  "III",  X, iron_block,  I, iron_ingot\n
            \n
            BoundingBox =  0.0, 1.0,  4.0,  2.1, 2.0, 0.75\n
            BoundingBox = -0.8, 1.6,  2.5,  2.1, 4.0, 0.75\n
            BoundingBox =  0.8, 1.6,  2.5,  2.1, 4.0, 0.75\n
            \n
            BoundingBox =  0.0, 1.3, -0.5,  4.0, 3.2, 1.00\n
            BoundingBox =  0.8, 1.3, -3.5,  2.0, 2.0, 1.20\n
            BoundingBox = -0.8, 1.3, -3.5,  2.0, 2.0, 1.20\n
            \n
            BoundingBox =  0.0, 3.0,  0.0,  2.0, 1.0, 1.20\n
            
            """;

    public static final String EC665_TIGER_UHT = """
            DisplayName = EC665 Tiger UHT
            AddDisplayName = ja_JP, EC665 ティーガー UHT型
            ItemID = 28802
            MaxHp = 180
            EnableGunnerMode = true
            EnableNightVision = true
            EnableEntityRadar = true
            Speed = 0.8
            ThrottleUpDown = 1.1
            FlareType = 3
            CameraPosition = 0.0, 0.6, 4.8
            CameraZoom = 6
            MaxFuel         = 1100
            FuelConsumption = 1.0
            ThirdPersonDist = 16
            
            ; M = Military(軍用機).  A = Attacker(攻撃機)
            Category = M.A
            
            HUD = heli, heli_gnr
            
            AddTexture = ec665_black
            
            AddRotor = 1, 0, 0.00,  3.85, -1.25,  0.0, 1.0, 0.0
            AddRotor = 1, 0, -0.50,  3.43, -10.13,  1.0, 0.0, 0.0
            
            AddPartCamera = 0.0, 5.38, -1.22
            
            ;               name, HideGM, Yaw,  Pitch  PositionX,Y,Z
            ; RMK30
            AddPartWeapon = rmk30, true, true, true,  0.0, 0.43, 3.37
            ; 機首カメラ EC665の機首カメラは機銃と一緒に動く必要があるので武器として追加
            AddPartWeapon = rmk30, true, true, false, 0.0, 0.43, 3.37
            
            AddSeat       = 0.00,  0.91,  1.76
            AddGunnerSeat = 0.00,  1.32,  0.17,  0.0, 0.6, 4.8,  true
            
            AddWeapon = rmk30,  0.0, 0.43, 3.37,   0.0,-1.0, true,2, 0, -360,360, -20,80
            AddWeapon = 20mmgunpod,  2.31, 1.25, 0.32,   1.0, 0.0
            AddWeapon = 20mmgunpod, -2.31, 1.25, 0.32,  -1.0, 0.0
            
            AddWeapon = pars_3_lr,   1.25, 1.22,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,  -1.25, 1.22,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,   1.53, 0.22,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,  -1.53, 0.22,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,   1.24, 0.93,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,  -1.24, 0.93,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,   1.54, 0.92,-0.01,   1.0, 0.0, true,2
            AddWeapon = pars_3_lr,  -1.54, 0.92,-0.01,   1.0, 0.0, true,2
            
            AddWeapon = aim92,  0.79,0.55,-0.01,  1.0,0.0
            AddWeapon = aim92, -0.79,0.55,-0.01,  1.0,0.0
            
            AddWeapon = targeting_pod_mob,     0.0, 0.6, 4.8,   0.0, 0.0,  true, 2
            AddWeapon = targeting_pod_pv10s,   0.0, 0.6, 4.8,   0.0, 0.0,  false,2
            
            AddRecipe = " X ",  "X X",  " X ",  X, iron_block
            
            BoundingBox =  0.0, 2.2, -1.0,  2.0, 3.8
            BoundingBox =  0.0, 1.8, -3.5,  2.0, 2.4
            BoundingBox =  0.0, 2.5,-10.0,  2.0, 3.0
            """;


    public static final String ABRAMS = """
            DisplayName = M1A2 Abrams
            AddDisplayName = ja_JP, M1A2 エイブラムス
            ItemID = 30100
            MaxHp = 330
            Speed = 0.7
            Sound = tank_gte
            ThrottleUpDown = 20.0
            MobilityYawOnGround = 2.0
            MinRotationPitch = -50
            MaxRotationPitch = 5
            MobilityRoll = 0
            Gravity = -0.1
            GravityInWater = -0.1
            DamageFactor = 0.0
            CameraZoom = 3
            CameraPosition = 0.00,  3.50, 0.90, true
            EnableNightVision = true
            CameraRotationSpeed = 25
            ;EnableEntityRadar = true
            MaxFuel         = 1200
            FuelConsumption = 3.0
            StepHeight = 1.5
            DefaultFreelook = true
            OnGroundPitchFactor = 2.0
            OnGroundRollFactor  = 1.3
            FlareType = 10
            ThirdPersonDist = 12
            
            ; 指定した高さまでの水に触れてもダメージを受けない
            ; 2であれば2ブロックまでダメージ無し
            SubmergedDamageHeight = 3
            
            HideEntity = true
            
            AddTexture = m1a2_1
            
            ; Tank or Car or Unknown
            WeightType = Tank
            
            ; 重心Z座標設定
            WeightedCenterZ = 0.0
            
            ;SetWheelPos =  X座標, Y座標,  Z座標1, Z座標2, Z座標3 ...
            ; X座標のマイナス側は必要なし
            SetWheelPos =  1.82,  -0.24,  2.86,  0.70, -0.76, -2.22, -4.14
            
            HUD = mbt_hud, mbt_gnr, gunner
            
            AddGunnerSeat =  0.00,  2.50,  0.25,    0.00,  2.50,  0.25,  false, -90, 90, true
            AddGunnerSeat = -0.55,  1.00, -0.96,   -0.846, 3.60,  0.373, false, -90, 90, true
            AddGunnerSeat =  0.55,  2.50, -0.96,    0.55,  3.60, -0.96,  false, -90, 90, true
            
            ; 地面の滑りやすさ、小さいほど摩擦が大きくなり動きにくくなる
            MotionFactor = 0.9
            
            ;ArmorMinDamage 最小ダメージ、これより小さいダメージは無効化される
            ArmorMinDamage = 12
            ;ArmorMaxDamage 最大ダメージ、これより大きいダメージは最大ダメージに丸められる
            ArmorMaxDamage = 500
            ;ArmorDamageFactor ダメージ係数上記のMin/Max範囲チェック後のダメージの係数 1で100%, 0.5で50%
            ArmorDamageFactor = 0.95
            ;後退可能にする
            EnableBack = true
            
            ;PivotTurnThrottle を0より大きい値にすると超信地旋回ができなくなる。デフォルトは0
            PivotTurnThrottle = 0.0
            ;AddCrawlerTrack = 履帯の表裏逆転,  1つの履帯の間隔, 履帯のXの位置, 履帯の回転ポイントY/Z, 履帯の回転ポイントY/Z, 履帯の回転ポイントY/Z, ...
            ;転輪がなくても動作する
            AddCrawlerTrack = false, 0.37, -2.15,  1.14/ -4.62,  1.10/ -4.88,  0.80/ -4.90,  0.54/ -4.84,  -0.16/ -4.07,  -0.23/ -3.45,  -0.23/  2.17,  -0.11/ 2.73,  0.50/ 4.04,  0.78/ 4.12,  1.12/ 4.02,  1.22/ 3.73,
            AddCrawlerTrack = false, 0.37,  2.15,  1.14/ -4.62,  1.10/ -4.88,  0.80/ -4.90,  0.54/ -4.84,  -0.16/ -3.74,  -0.23/ -3.78,  -0.23/  2.49,  -0.11/ 2.86,  0.50/ 4.04,  0.78/ 4.12,  1.12/ 4.02,  1.22/ 3.73,
            
            
            ; 転輪の回転速度：0より小さい値を設定すると逆回転する
            ;TrackRollerRot = 30
            
            ;転輪を追加する、設定は座標だけで、X軸が負の値だと右側、正の値だと左側の転輪となる
            ;履帯と同じ動きをするが、履帯がなくても設定可能
            AddTrackRoller = -1.82,  0.81,  3.70
            AddTrackRoller = -1.82,  0.18,  2.22
            AddTrackRoller = -1.82,  0.18,  1.02
            AddTrackRoller = -1.82,  0.18,  0.06
            AddTrackRoller = -1.82,  0.18, -0.90
            AddTrackRoller = -1.82,  0.18, -1.86
            AddTrackRoller = -1.82,  0.18, -2.82
            AddTrackRoller = -1.82,  0.18, -3.78
            AddTrackRoller = -1.82,  0.81, -4.59
            
            AddTrackRoller =  1.82,  0.81,  3.70
            AddTrackRoller =  1.82,  0.18,  2.50
            AddTrackRoller =  1.82,  0.18,  1.30
            AddTrackRoller =  1.82,  0.18,  0.34
            AddTrackRoller =  1.82,  0.18, -0.62
            AddTrackRoller =  1.82,  0.18, -1.58
            AddTrackRoller =  1.82,  0.18, -2.54
            AddTrackRoller =  1.82,  0.18, -3.50
            AddTrackRoller =  1.82,  0.81, -4.59
            
            ;主砲
            AddPartWeapon = rehinmetall_apfsds / rehinmetall_he, false, true, false,  0.00, 2.10, 0.00, 0
            AddPartWeaponChild = false, true, 0.00, 2.10, 2.02, 0.0
            AddPartWeaponChild = false, true, 0.00, 2.10, 2.02, 0.5
            AddTurretWeapon = rehinmetall_apfsds, 0.00, 2.10, 2.02,  0.0, 0.0, true,1, 0,-360,360, -50, 7
            AddTurretWeapon = rehinmetall_he,     0.00, 2.10, 2.02,  0.0, 0.0, true,1, 0,-360,360, -50, 7
            AddTurretWeapon = mg7_62mm_mbt,      -0.43, 2.28, 2.02,  0.0, 0.0, true,1, 0,-360,360, -50, 7
            
            ;RWS M2
            AddPartTurretWeapon    = m2_rws,   true, true, true,  -0.846, 3.68, 0.373
            AddPartTurretWeapon    = m2_rws,  false, true, false, -0.846, 3.68, 0.373
            AddTurretWeapon = m2_rws,   -0.846, 3.68,  0.373,   0.0, 0.0, false, 2, 0, -360,360, -70,30
            
            ;M240
            AddPartTurretWeapon    = m240_r,   false, true, true,   1.243, 3.24, -0.77
            AddPartTurretWeapon    = m240_r,   false, true, false,  1.243, 3.24, -0.77
            AddTurretWeapon = m240_r,    1.243, 3.24, -0.77,   0.0, 0.0, false, 3, 0, -220,30, -50,30
            
            AddRecipe = "XXX",  " X ",  "III",  X, iron_block,  I, iron_ingot
            
            ;RideRack = 乗る機体名, ラック番号 (1～) 
            RideRack = c5, 1
            RideRack = c5, 2
            
            BoundingBox =  0.0, 0.6,  2.2,   3.4, 1.8,  0.70
            BoundingBox = -0.9, 1.6,  1.0,   2.4, 3.2,  0.70
            BoundingBox =  0.9, 1.6,  1.0,   2.4, 3.2,  0.70
            BoundingBox =  0.0, 1.4, -2.2,   3.8, 3.0,  1.00
            BoundingBox =  0.0, 3.0, -2.0,   2.0, 1.0,  1.25
            BoundingBox = -0.9, 1.2, -4.5,   2.4, 1.6,  1.25
            BoundingBox =  0.9, 1.2, -4.5,   2.4, 1.6,  1.25
            """;


}
