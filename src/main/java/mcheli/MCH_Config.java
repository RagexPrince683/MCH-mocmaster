package mcheli;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mcheli.MCH_ConfigPrm;
import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_OutputFile;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.wrapper.W_Block;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

//everything config related
public class MCH_Config {

   public static String mcPath;
   public static String configFilePath;
   public static MCH_ConfigPrm EnableMCHLibLog;
   public static MCH_ConfigPrm EnableMCHLibDebugLog;
   public static MCH_ConfigPrm EnableNEIHandler;
   public static String configVer;
   public static int hitMarkColorRGB;
   public static float hitMarkColorAlpha;
   public static List bulletBreakableBlocks;
   public static final List dummyBreakableBlocks = new ArrayList();
   public static final List dummyBreakableMaterials = new ArrayList();
   public static List carNoBreakableBlocks;
   public static List carBreakableBlocks;
   public static List carBreakableMaterials;
   public static List tankNoBreakableBlocks;
   public static List tankBreakableBlocks;
   public static List tankBreakableMaterials;
   public static MCH_ConfigPrm KeyUp;
   public static MCH_ConfigPrm KeyDown;
   public static MCH_ConfigPrm KeySubmarineAscend;
   public static MCH_ConfigPrm KeySubmarineDescend;
   public static MCH_ConfigPrm KeyRight;
   public static MCH_ConfigPrm KeyLeft;
   public static MCH_ConfigPrm KeySwitchMode;

   public static MCH_ConfigPrm KeySwitchHovering;
   public static MCH_ConfigPrm KeyEjectHeli;
   public static MCH_ConfigPrm KeyAttack;
   public static MCH_ConfigPrm KeyUseWeapon;
   public static MCH_ConfigPrm KeyCurrentWeaponLock;
   public static MCH_ConfigPrm KeyVehicleLock;
   public static MCH_ConfigPrm KeyRadar;
   public static MCH_ConfigPrm KeySwitchWeapon1;
   public static MCH_ConfigPrm KeySwitchWeapon2;
   public static MCH_ConfigPrm KeySwWeaponMode;
   public static MCH_ConfigPrm KeyZoom;
   public static MCH_ConfigPrm KeyCameraMode;
   public static MCH_ConfigPrm KeyUnmount;
   public static MCH_ConfigPrm KeyFlare;
   public static MCH_ConfigPrm KeyExtra;
   public static MCH_ConfigPrm KeyCameraDistUp;
   public static MCH_ConfigPrm KeyCameraDistDown;
   public static MCH_ConfigPrm KeyFreeLook;
   public static MCH_ConfigPrm KeyPlaneLookAhead;
   public static MCH_ConfigPrm KeyPlaneMouseAim;
   public static MCH_ConfigPrm KeyBombReticleMode;
   public static MCH_ConfigPrm KeyGUI;
   public static MCH_ConfigPrm KeyGearUpDown;
   public static MCH_ConfigPrm KeyPutToRack;
   public static MCH_ConfigPrm KeyDownFromRack;
   public static MCH_ConfigPrm KeyScoreboard;
   public static MCH_ConfigPrm KeyMultiplayManager;
   public static MCH_ConfigPrm KeyChaff;
   public static MCH_ConfigPrm KeyMaintenance;
   public static MCH_ConfigPrm KeyAPS;
   public static List DamageVs;
   public static List IgnoreBulletHitList;
   public static MCH_ConfigPrm IgnoreBulletHitItem;
   public static MCH_Config.DamageFactor[] DamageFactorList;
   public static MCH_Config.DamageFactor DamageVsEntity;
   public static MCH_Config.DamageFactor DamageVsLiving;
   public static MCH_Config.DamageFactor DamageVsPlayer;
   public static MCH_Config.DamageFactor DamageVsMCHeliAircraft;
   public static MCH_Config.DamageFactor DamageVsMCHeliTank;
   public static MCH_Config.DamageFactor DamageVsMCHeliVehicle;
   public static MCH_Config.DamageFactor DamageVsMCHeliOther;
   public static MCH_Config.DamageFactor DamageAircraftByExternal;
   public static MCH_Config.DamageFactor DamageTankByExternal;
   public static MCH_Config.DamageFactor DamageVehicleByExternal;
   public static MCH_Config.DamageFactor DamageOtherByExternal;
   public static List CommandPermission;
   public static List CommandPermissionList;
   public static MCH_ConfigPrm TestMode;
   public static MCH_ConfigPrm EnableCommand;
   public static MCH_ConfigPrm PlaceableOnSpongeOnly;
   public static MCH_ConfigPrm HideKeybind;
   public static MCH_ConfigPrm ItemDamage;
   public static MCH_ConfigPrm ItemFuel;
   public static MCH_ConfigPrm AutoRepairHP;
   public static MCH_ConfigPrm Collision_DestroyBlock;
   public static MCH_ConfigPrm Explosion_DestroyBlock;
   public static MCH_ConfigPrm Explosion_FlamingBlock;
   public static MCH_ConfigPrm BulletBreakableBlock;
   public static MCH_ConfigPrm PiercingBlockHardnessLimit;
   public static MCH_ConfigPrm PiercingBlockBlastResistanceLimit;
   public static MCH_ConfigPrm DebugPiercingBlocks;
   public static MCH_ConfigPrm Collision_Car_BreakableBlock;
   public static MCH_ConfigPrm Collision_Car_NoBreakableBlock;
   public static MCH_ConfigPrm Collision_Car_BreakableMaterial;
   public static MCH_ConfigPrm Collision_Tank_BreakableBlock;
   public static MCH_ConfigPrm Collision_Tank_NoBreakableBlock;
   public static MCH_ConfigPrm Collision_Tank_BreakableMaterial;
   public static MCH_ConfigPrm Collision_EntityDamage;
   public static MCH_ConfigPrm Collision_EntityTankDamage;
   public static MCH_ConfigPrm LWeaponAutoFire;
   public static MCH_ConfigPrm ArtilleryRangeModifier;
   public static MCH_ConfigPrm EnableHandheld;
   public static MCH_ConfigPrm DismountAll;
   public static MCH_ConfigPrm MountMinecartHeli;
   public static MCH_ConfigPrm MountMinecartPlane;
   public static MCH_ConfigPrm MountMinecartShip;
   public static MCH_ConfigPrm MountMinecartVehicle;
   public static MCH_ConfigPrm MountMinecartTank;
   public static MCH_ConfigPrm AutoThrottleDownHeli;
   public static MCH_ConfigPrm AutoThrottleDownPlane;
   public static MCH_ConfigPrm AutoThrottleDownShip;

   public static MCH_ConfigPrm AutoThrottleDownTank;
   public static MCH_ConfigPrm DisableItemRender;
   public static MCH_ConfigPrm Override3DItemIcon;
   public static MCH_ConfigPrm Heli3DItemIconScale;
   public static MCH_ConfigPrm Plane3DItemIconScale;
   public static MCH_ConfigPrm Ship3DItemIconScale;
   public static MCH_ConfigPrm Tank3DItemIconScale;
   public static MCH_ConfigPrm Turret3DItemIconScale;
   public static MCH_ConfigPrm RenderDistanceWeight;
   public static MCH_ConfigPrm EnableAircraftLODRender;
   public static MCH_ConfigPrm AircraftLODStartDistance;
   public static MCH_ConfigPrm AircraftLODFarDistance;
   public static MCH_ConfigPrm AircraftLODVisibilityDistance;
   public static MCH_ConfigPrm AircraftLODRainVisibilityMultiplier;
   public static MCH_ConfigPrm AircraftLODThunderVisibilityMultiplier;
   public static MCH_ConfigPrm AircraftLODThermalContrastExponent;
   public static MCH_ConfigPrm AircraftLODOpticalMinPixels;
   public static MCH_ConfigPrm AircraftLODThermalMinPixels;
   public static MCH_ConfigPrm EnableModelTextureRepair, EnableModelUVCorrection;
   public static MCH_ConfigPrm ModelTextureMaxHoleArea, ModelTextureMaxHoleThickness;
   public static MCH_ConfigPrm ModelTextureRGBBleedRadius, ModelTextureAlphaExpansionRadius, ModelTextureUVCorrectionRadius;
   public static MCH_ConfigPrm ModelTextureRepairDebugLogging, ModelTextureRepairDebugPreviews;
   public static MCH_ConfigPrm MobRenderDistanceWeight;
   public static MCH_ConfigPrm CreativeTabIcon;
   public static MCH_ConfigPrm CreativeTabIconHeli;
   public static MCH_ConfigPrm CreativeTabIconPlane;
   public static MCH_ConfigPrm CreativeTabIconShip;
   public static MCH_ConfigPrm CreativeTabIconTank;
   public static MCH_ConfigPrm CreativeTabIconVehicle;
   public static MCH_ConfigPrm DisableShader;
   public static MCH_ConfigPrm AliveTimeOfCartridge;
   public static MCH_ConfigPrm InfinityAmmo;
   public static MCH_ConfigPrm InfinityFuel;
   public static MCH_ConfigPrm HitMarkColor;
   public static MCH_ConfigPrm SmoothShading;
   public static MCH_ConfigPrm EnableModEntityRender;
   public static MCH_ConfigPrm DisableRenderLivingSpecials;
   public static MCH_ConfigPrm PreventingBroken;
   public static MCH_ConfigPrm DropItemInCreativeMode;
   public static MCH_ConfigPrm BreakableOnlyPickaxe;
   public static MCH_ConfigPrm InvertMouse;
   public static MCH_ConfigPrm MouseSensitivity;
   public static MCH_ConfigPrm ZoomSensitivityEffect;
   public static MCH_ConfigPrm MouseControlStickModeHeli;
   public static MCH_ConfigPrm MouseControlStickModePlane;
   public static MCH_ConfigPrm MouseControlFlightSimMode;
   public static MCH_ConfigPrm EnableMouseAimControls;
   public static MCH_ConfigPrm MouseAimSensitivity;
   public static MCH_ConfigPrm MouseAimSmoothing;
   public static MCH_ConfigPrm MouseAimMaxPitchUp;
   public static MCH_ConfigPrm MouseAimMaxPitchDown;
   public static MCH_ConfigPrm MouseAimYawResponse;
   public static MCH_ConfigPrm MouseAimPitchResponse;
   public static MCH_ConfigPrm MouseAimAutoBankStrength;
   public static MCH_ConfigPrm MouseAimAutoBankMaxRoll;
   public static MCH_ConfigPrm MouseAimCenteringStrength;
   public static MCH_ConfigPrm MouseAimDebug;
   public static MCH_ConfigPrm EnablePlaneMouseAimReticle;
   public static MCH_ConfigPrm HideVanillaCrosshairInPlaneMouseAim;
   public static MCH_ConfigPrm PlaneMouseAimReticleTexture;
   public static MCH_ConfigPrm PlaneMouseAimReticleScale;
   public static MCH_ConfigPrm PlaneMouseAimReticleOpacity;
   public static MCH_ConfigPrm PlaneNoseReticleScale;
   public static MCH_ConfigPrm PlaneNoseReticleOpacity;
   public static MCH_ConfigPrm PlaneMouseAimMaxScreenRadius;
   public static MCH_ConfigPrm PlaneMouseAimYawVisualRange;
   public static MCH_ConfigPrm PlaneMouseAimReticleDebug;
   public static MCH_ConfigPrm EnableNewPlaneSimpleHud;
   public static MCH_ConfigPrm EnableNewPlaneWeaponHud;
   public static MCH_ConfigPrm EnableNewPlaneHudGlow;
   public static MCH_ConfigPrm EnableNewHeliHudSharedReadouts;
   public static MCH_ConfigPrm EnableNewHeliWeaponHud;
   public static MCH_ConfigPrm EnableNewVehicleHudGlow;
   public static MCH_ConfigPrm EnableNewVehicleStickInputGauge;
   public static MCH_ConfigPrm NewPlaneSimpleHudX;
   public static MCH_ConfigPrm NewPlaneSimpleHudY;
   public static MCH_ConfigPrm NewPlaneWeaponHudRightMargin;
   public static MCH_ConfigPrm NewPlaneWeaponHudY;
   public static MCH_ConfigPrm SwitchWeaponWithMouseWheel;
   public static MCH_ConfigPrm AllPlaneSpeed;
   public static MCH_ConfigPrm NewFlightGravity;
   public static MCH_ConfigPrm NewFlightDiveAssistEnabled;
   public static MCH_ConfigPrm NewFlightDiveAccelerationMultiplier;
   public static MCH_ConfigPrm NewFlightMaxDiveSpeedMultiplier;
   public static MCH_ConfigPrm AllShipSpeed;
   public static MCH_ConfigPrm AllHeliSpeed;
   public static MCH_ConfigPrm AllTankSpeed;
   public static MCH_ConfigPrm HurtResistantTime;
   public static MCH_ConfigPrm DisplayHUDThirdPerson;
   public static MCH_ConfigPrm EnableNewPlaneThirdPersonCamera;
   public static MCH_ConfigPrm NewPlaneCameraDistance;
   public static MCH_ConfigPrm NewPlaneCameraMinDistance;
   public static MCH_ConfigPrm NewPlaneCameraMaxDistance;
   public static MCH_ConfigPrm NewPlaneCameraDebugDistance;
   public static MCH_ConfigPrm NewPlaneCameraDebugAbovePlane;
   public static MCH_ConfigPrm NewPlaneCameraHeight;
   public static MCH_ConfigPrm NewPlaneCameraSideOffset;
   public static MCH_ConfigPrm PlaneChaseSpeedDistanceScale;
   public static MCH_ConfigPrm PlaneChaseSpeedDistanceMaxBonus;
   public static MCH_ConfigPrm EnablePlaneChaseFOVOverride;
   public static MCH_ConfigPrm PlaneChaseFOV;
   public static MCH_ConfigPrm PlaneChaseFreelookFOV;
   public static MCH_ConfigPrm PlaneChaseFOVSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraSizeDistanceScale;
   public static MCH_ConfigPrm PlaneChaseFocusVerticalOffset;
   public static MCH_ConfigPrm PlaneChaseScreenVerticalBias;
   public static MCH_ConfigPrm PlaneLookAheadDistance;
   public static MCH_ConfigPrm PlaneLookAheadSmoothing;
   public static MCH_ConfigPrm PlaneLookAheadReturnSmoothing;
   public static MCH_ConfigPrm FreelookReturnSmoothing;
   public static MCH_ConfigPrm PlaneFreelookOrbitSensitivity;
   public static MCH_ConfigPrm PlaneFreelookYawSmoothing;
   public static MCH_ConfigPrm PlaneFreelookPitchSmoothing;
   public static MCH_ConfigPrm PlaneFreelookReturnSmoothing;
   public static MCH_ConfigPrm PlaneFreelookMaxPitchUp;
   public static MCH_ConfigPrm PlaneFreelookMaxPitchDown;
   public static MCH_ConfigPrm NewPlaneCameraPositionSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraYawSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraPitchSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraDistanceSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraFocusSmoothing;
   public static MCH_ConfigPrm NewPlaneCameraPitchInfluenceSmoothing;
   public static MCH_ConfigPrm PlaneChasePitchInfluence;
   public static MCH_ConfigPrm NewPlaneCameraRollInfluence;
   public static MCH_ConfigPrm PlaneChaseHorizonStabilization;
   public static MCH_ConfigPrm EnableNewPlaneCameraSpeedDistance;
   public static MCH_ConfigPrm EnableNewPlaneCameraCollision;
   public static MCH_ConfigPrm EnablePlaneLookAhead;
   public static MCH_ConfigPrm EnableHoldFreelook;
   public static MCH_ConfigPrm EnableNewPlaneCameraRollInfluence;
   public static MCH_ConfigPrm NewPlaneCameraCollision;
   public static MCH_ConfigPrm DisableCameraDistChange;
   public static MCH_ConfigPrm EnableReplaceTextureManager;
   public static MCH_ConfigPrm DisplayEntityMarker;
   public static MCH_ConfigPrm EntityMarkerSize;
   public static MCH_ConfigPrm BlockMarkerSize;
   public static MCH_ConfigPrm DisplayMarkThroughWall;
   public static MCH_ConfigPrm ReplaceRenderViewEntity;
   public static MCH_ConfigPrm StingerLockRange;
   public static MCH_ConfigPrm DefaultExplosionParticle;
   public static MCH_ConfigPrm RangeFinderSpotDist;
   public static MCH_ConfigPrm RangeFinderSpotTime;
   public static MCH_ConfigPrm RangeFinderConsume;
   public static MCH_ConfigPrm EnablePutRackInFlying;
   public static MCH_ConfigPrm EnableDebugBoundingBox;
   public static MCH_ConfigPrm DebugVehicleBoxCache;
   public static MCH_ConfigPrm DebugVehicleLODVisibility;
   public static MCH_ConfigPrm DebugFlightControl;

   //TODOne mch1.0.5 -> mchr?
   public static MCH_ConfigPrm DespawnCount;

   public static MCH_ConfigPrm HitBoxDelayTick;

   public static MCH_ConfigPrm EnableRotationLimit;

   public static MCH_ConfigPrm PitchLimitMax;

   public static MCH_ConfigPrm PitchLimitMin;

   public static MCH_ConfigPrm RollLimit;

   public static MCH_ConfigPrm RangeOfGunner_VsMonster_Vertical;

   public static MCH_ConfigPrm RangeOfGunner_VsMonster_Horizontal;

   public static MCH_ConfigPrm RangeOfGunner_VsPlayer_Vertical;

   public static MCH_ConfigPrm RangeOfGunner_VsPlayer_Horizontal;

   public static MCH_ConfigPrm FixVehicleAtPlacedPoint;

   public static MCH_ConfigPrm KillPassengersWhenDestroyed;
   public static MCH_ConfigPrm ItemID_Fuel;
   public static MCH_ConfigPrm ItemID_GLTD;
   public static MCH_ConfigPrm ItemID_Chain;
   public static MCH_ConfigPrm ItemID_Parachute;
   public static MCH_ConfigPrm ItemID_Container;
   public static MCH_ConfigPrm ItemID_Stinger;
   public static MCH_ConfigPrm ItemID_StingerMissile;
   public static MCH_ConfigPrm[] ItemID_UavStation;
   public static MCH_ConfigPrm ItemID_InvisibleItem;
   public static MCH_ConfigPrm ItemID_DraftingTable;
   public static MCH_ConfigPrm ItemID_Wrench;
   public static MCH_ConfigPrm ItemID_RangeFinder;
   public static MCH_ConfigPrm BlockID_DraftingTableOFF;
   public static MCH_ConfigPrm BlockID_DraftingTableON;
   public static MCH_ConfigPrm ItemRecipe_Fuel;
   public static MCH_ConfigPrm ItemRecipe_GLTD;
   public static MCH_ConfigPrm ItemRecipe_Chain;
   public static MCH_ConfigPrm ItemRecipe_Parachute;
   public static MCH_ConfigPrm ItemRecipe_Container;
   public static MCH_ConfigPrm ItemRecipe_Stinger;
   public static MCH_ConfigPrm ItemRecipe_StingerMissile;
   public static MCH_ConfigPrm ItemRecipe_Javelin;
   public static MCH_ConfigPrm ItemRecipe_JavelinMissile;
   public static MCH_ConfigPrm ItemRecipe_Rpg;
   public static MCH_ConfigPrm ItemRecipe_RpgMissile;
   public static MCH_ConfigPrm[] ItemRecipe_UavStation;
   public static MCH_ConfigPrm ItemRecipe_DraftingTable;
   public static MCH_ConfigPrm ItemRecipe_Wrench;
   public static MCH_ConfigPrm ItemRecipe_RangeFinder;
   public static MCH_ConfigPrm[] KeyConfig;
   public static MCH_ConfigPrm[] General;
    public static MCH_ConfigPrm MultiThreadedModelLoading;
    public final String destroyBlockNames = "glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily";
    public static MCH_ConfigPrm delayrangeloader;
    public static MCH_ConfigPrm bombletloader;
   public static MCH_ConfigPrm wrenchdropitem;
   public static MCH_ConfigPrm AutoRepairEnabled;
   public static MCH_ConfigPrm placetimer;



   public MCH_Config(String minecraftPath, String cfgFile) {
      mcPath = minecraftPath;
      configFilePath = mcPath + cfgFile;
      configVer = "0.0.0";
      bulletBreakableBlocks = new ArrayList();
      carBreakableBlocks = new ArrayList();
      carNoBreakableBlocks = new ArrayList();
      carBreakableMaterials = new ArrayList();
      tankBreakableBlocks = new ArrayList();
      tankNoBreakableBlocks = new ArrayList();
      tankBreakableMaterials = new ArrayList();
      KeyUp = new MCH_ConfigPrm("KeyUp", 17);
      KeyDown = new MCH_ConfigPrm("KeyDown", 31);
      KeySubmarineAscend = new MCH_ConfigPrm("KeySubmarineAscend", 200);
      KeySubmarineDescend = new MCH_ConfigPrm("KeySubmarineDescend", 208);
      KeyRight = new MCH_ConfigPrm("KeyRight", 32);
      KeyLeft = new MCH_ConfigPrm("KeyLeft", 30);
      KeySwitchMode = new MCH_ConfigPrm("KeySwitchGunner", 35);
      KeySwitchHovering = new MCH_ConfigPrm("KeySwitchHovering", 57);
      KeyEjectHeli = new MCH_ConfigPrm("KeyEjectHeli", 54);
      KeyAttack = new MCH_ConfigPrm("KeyAttack", -100);
      KeyUseWeapon = new MCH_ConfigPrm("KeyUseWeapon", -99);
      KeyCurrentWeaponLock = new MCH_ConfigPrm("KeyCurrentWeaponLock", -100);
      KeyVehicleLock = new MCH_ConfigPrm("KeyVehicleLock", 24);
      // R is already the GUI key, so active radar defaults to P.
      KeyRadar = new MCH_ConfigPrm("KeyRadar", 25);
      KeySwitchWeapon1 = new MCH_ConfigPrm("KeySwitchWeapon1", -98);
      KeySwitchWeapon2 = new MCH_ConfigPrm("KeySwitchWeapon2", 34);
      KeySwWeaponMode = new MCH_ConfigPrm("KeySwitchWeaponMode", 45);
      KeyZoom = new MCH_ConfigPrm("KeyZoom", 44);
      KeyCameraMode = new MCH_ConfigPrm("KeyCameraMode", 46);
      KeyUnmount = new MCH_ConfigPrm("KeyUnmountMob", 21);
      KeyFlare = new MCH_ConfigPrm("KeyFlare", 47);
      KeyChaff = new MCH_ConfigPrm("KeyChaff", 47);
      KeyMaintenance = new MCH_ConfigPrm("KeyMaintenance", 47);
      KeyAPS = new MCH_ConfigPrm("KeyAPS", 47);
      KeyExtra = new MCH_ConfigPrm("KeyExtra", 33);
      KeyCameraDistUp = new MCH_ConfigPrm("KeyCameraDistanceUp", 201);
      KeyCameraDistDown = new MCH_ConfigPrm("KeyCameraDistanceDown", 209);
      KeyFreeLook = new MCH_ConfigPrm("KeyFreeLook", 29);
      KeyPlaneLookAhead = new MCH_ConfigPrm("KeyPlaneLookAhead", 56);
      KeyPlaneMouseAim = new MCH_ConfigPrm("KeyPlaneMouseAim", 49);
      KeyBombReticleMode = new MCH_ConfigPrm("KeyBombReticleMode", 37);
      KeyGUI = new MCH_ConfigPrm("KeyGUI", 19);
      KeyGearUpDown = new MCH_ConfigPrm("KeyGearUpDown", 48);
      KeyPutToRack = new MCH_ConfigPrm("KeyPutToRack", 36);
      KeyDownFromRack = new MCH_ConfigPrm("KeyDownFromRack", 22);
      KeyScoreboard = new MCH_ConfigPrm("KeyScoreboard", 38);
      KeyMultiplayManager = new MCH_ConfigPrm("KeyMultiplayManager", 50);
      KeyConfig = new MCH_ConfigPrm[]{
              KeyUp,
              KeyDown,
              KeySubmarineAscend,
              KeySubmarineDescend,
              KeyRight,
              KeyLeft,
              KeySwitchMode,
              KeySwitchHovering,
              KeySwitchWeapon1,
              KeySwitchWeapon2,
              KeySwWeaponMode,
              KeyZoom,
              KeyCameraMode,
              KeyUnmount,
              KeyFlare,
              KeyExtra,
              KeyCameraDistUp,
              KeyCameraDistDown,
              KeyFreeLook,
              KeyPlaneLookAhead,
              KeyPlaneMouseAim,
              KeyBombReticleMode,
              KeyGUI,
              KeyGearUpDown,
              KeyPutToRack,
              KeyDownFromRack,
              KeyScoreboard,
              KeyMultiplayManager,
              KeyChaff,
              KeyMaintenance,
              KeyAPS,
              KeyUseWeapon,
              KeyAttack,
              KeyCurrentWeaponLock,
              KeyRadar,
              KeyVehicleLock};
      DamageVs = new ArrayList();
      CommandPermission = new ArrayList();
      CommandPermissionList = new ArrayList();
      IgnoreBulletHitList = new ArrayList();
      IgnoreBulletHitItem = new MCH_ConfigPrm("IgnoreBulletHit", "");
      EnableMCHLibLog = new MCH_ConfigPrm("EnableMCHLibLog", true);
      EnableMCHLibLog.desc = ";Write normal MCH_Lib.Log messages to the Minecraft log.";
      EnableMCHLibDebugLog = new MCH_ConfigPrm("EnableMCHLibDebugLog", false);
      EnableMCHLibDebugLog.desc = ";Write verbose MCH_Lib.DbgLog messages to the Minecraft log.";
      EnableNEIHandler = new MCH_ConfigPrm("EnableNEIHandler", true);
      EnableNEIHandler.desc = ";Controls the MCHeli vehicle ammunition category in NotEnoughItems. Requires a client restart.";
      TestMode = new MCH_ConfigPrm("TestMode", false);
      EnableCommand = new MCH_ConfigPrm("EnableCommand", true);
      PlaceableOnSpongeOnly = new MCH_ConfigPrm("PlaceableOnSpongeOnly", false);
      MultiThreadedModelLoading = new MCH_ConfigPrm("MultiThreadedModelLoading", true);
      HideKeybind = new MCH_ConfigPrm("HideKeybind", false);
      ItemDamage = new MCH_ConfigPrm("ItemDamage", true);
      ItemFuel = new MCH_ConfigPrm("ItemFuel", true);
      AutoRepairHP = new MCH_ConfigPrm("AutoRepairHP", 0.0D);
      //AutoRepairEnabled = new MCH_ConfigPrm("AutoRepairEnabled", false);
      Collision_DestroyBlock = new MCH_ConfigPrm("Collision_DestroyBlock", true);
      Explosion_DestroyBlock = new MCH_ConfigPrm("Explosion_DestroyBlock", true);
      Explosion_FlamingBlock = new MCH_ConfigPrm("Explosion_FlamingBlock", true);
      PiercingBlockHardnessLimit = new MCH_ConfigPrm("PiercingBlockHardnessLimit", 10.0D);
      PiercingBlockHardnessLimit.desc = ";Stops a piercing projectile when block hardness reaches this value. Either resistance limit can stop piercing; these limits do not control block destruction and still apply when mobGriefing is false.";
      PiercingBlockBlastResistanceLimit = new MCH_ConfigPrm("PiercingBlockBlastResistanceLimit", 100.0D);
      PiercingBlockBlastResistanceLimit.desc = ";Stops a piercing projectile when block blast resistance reaches this value. Either resistance limit can stop piercing; these limits do not control block destruction and still apply when mobGriefing is false.";
      DebugPiercingBlocks = new MCH_ConfigPrm("DebugPiercingBlocks", false);
      DebugPiercingBlocks.desc = ";Logs server-side piercing block resistance checks, including the weapon, block, resistance values, remaining Piercing, and PASS or STOP result.";
      Collision_Car_BreakableBlock = new MCH_ConfigPrm("Collision_Car_BreakableBlock", "double_plant, glass_pane,stained_glass_pane");
      Collision_Car_NoBreakableBlock = new MCH_ConfigPrm("Collision_Car_NoBreakBlock", "torch");
      Collision_Car_BreakableMaterial = new MCH_ConfigPrm("Collision_Car_BreakableMaterial", "cactus, cake, gourd, leaves, vine, plants");
      Collision_Tank_BreakableBlock = new MCH_ConfigPrm("Collision_Tank_BreakableBlock", "nether_brick_fence");
      Collision_Tank_BreakableBlock.validVer = "1.0.0";
      Collision_Tank_NoBreakableBlock = new MCH_ConfigPrm("Collision_Tank_NoBreakBlock", "torch, glowstone");
      Collision_Tank_BreakableMaterial = new MCH_ConfigPrm("Collision_Tank_BreakableMaterial", "cactus, cake, carpet, circuits, glass, gourd, leaves, vine, wood, plants");
      Collision_EntityDamage = new MCH_ConfigPrm("Collision_EntityDamage", true);
      Collision_EntityTankDamage = new MCH_ConfigPrm("Collision_EntityTankDamage", false);
      LWeaponAutoFire = new MCH_ConfigPrm("LWeaponAutoFire", false);
      ArtilleryRangeModifier = new MCH_ConfigPrm("ArtilleryRangeModifier", 1.0D);
      ArtilleryRangeModifier.desc = ";Multiplies launch speed for weapon text files that enable UseGlobalArtilleryRangeModifier. 1.0 keeps the original speed.";
      EnableHandheld = new MCH_ConfigPrm("EnableHandheld", true);
      DismountAll = new MCH_ConfigPrm("DismountAll", false);
      MountMinecartHeli = new MCH_ConfigPrm("MountMinecartHeli", true);
      MountMinecartPlane = new MCH_ConfigPrm("MountMinecartPlane", true);
      MountMinecartShip = new MCH_ConfigPrm("MountMinecartShip", false);
      MountMinecartVehicle = new MCH_ConfigPrm("MountMinecartVehicle", false);
      MountMinecartTank = new MCH_ConfigPrm("MountMinecartTank", true);
      AutoThrottleDownHeli = new MCH_ConfigPrm("AutoThrottleDownHeli", true);
      AutoThrottleDownPlane = new MCH_ConfigPrm("AutoThrottleDownPlane", false);
      AutoThrottleDownShip = new MCH_ConfigPrm("AutoThrottleDownShip", false);
      AutoThrottleDownTank = new MCH_ConfigPrm("AutoThrottleDownTank", false);
      DisableItemRender = new MCH_ConfigPrm("DisableItemRender", 1);
      DisableItemRender.desc = ";DisableItemRender = 0 ~ 3 (1 = Recommended)";
      Override3DItemIcon = new MCH_ConfigPrm("Override3DItemIcon", false);
      Override3DItemIcon.desc = ";Global 3D vehicle item icon override. true = force 3D icons off, false = allow per-vehicle 3D icon settings.";
      Heli3DItemIconScale = new MCH_ConfigPrm("Heli3DItemIconScale", 0.3D);
      Heli3DItemIconScale.desc = ";Global scale multiplier for helicopter 3D item icons.";
      Plane3DItemIconScale = new MCH_ConfigPrm("Plane3DItemIconScale", 0.1D);
      Plane3DItemIconScale.desc = ";Global scale multiplier for plane 3D item icons.";
      Ship3DItemIconScale = new MCH_ConfigPrm("Ship3DItemIconScale", 0.1D);
      Ship3DItemIconScale.desc = ";Global scale multiplier for ship 3D item icons.";
      Tank3DItemIconScale = new MCH_ConfigPrm("Tank3DItemIconScale", 0.3D);
      Tank3DItemIconScale.desc = ";Global scale multiplier for tank 3D item icons.";
      Turret3DItemIconScale = new MCH_ConfigPrm("Turret3DItemIconScale", 0.3D);
      Turret3DItemIconScale.desc = ";Global scale multiplier for turret/static vehicle 3D item icons.";
      RenderDistanceWeight = new MCH_ConfigPrm("RenderDistanceWeight", 1000.0D);
      EnableAircraftLODRender = new MCH_ConfigPrm("EnableAircraftLODRender", true);
      EnableAircraftLODRender.desc = ";Enable client-only far-distance model displays for aircraft, tanks, turrets, and ships.";
      AircraftLODStartDistance = new MCH_ConfigPrm("AircraftLODStartDistance", 140.0D);
      AircraftLODStartDistance.desc = ";Distance in blocks where tracked aircraft rendering switches to its cheaper model-only pass.";
      AircraftLODFarDistance = new MCH_ConfigPrm("AircraftLODFarDistance", mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE);
      AircraftLODFarDistance.desc = ";Maximum distance for client-only vehicle LOD snapshots. Real vehicle entity tracking remains unchanged and aligned with child seats.";
      AircraftLODVisibilityDistance = new MCH_ConfigPrm("AircraftLODVisibilityDistance", mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE);
      AircraftLODVisibilityDistance.desc = ";Clear-air distance where snapshot optical contrast falls to approximately two percent.";
      AircraftLODRainVisibilityMultiplier = new MCH_ConfigPrm("AircraftLODRainVisibilityMultiplier", 0.70D);
      AircraftLODThunderVisibilityMultiplier = new MCH_ConfigPrm("AircraftLODThunderVisibilityMultiplier", 0.45D);
      AircraftLODThermalContrastExponent = new MCH_ConfigPrm("AircraftLODThermalContrastExponent", 0.35D);
      AircraftLODOpticalMinPixels = new MCH_ConfigPrm("AircraftLODOpticalMinPixels", 0.75D);
      AircraftLODThermalMinPixels = new MCH_ConfigPrm("AircraftLODThermalMinPixels", 0.35D);
      EnableModelTextureRepair = new MCH_ConfigPrm("EnableModelTextureRepair", true);
      EnableModelUVCorrection = new MCH_ConfigPrm("EnableModelUVCorrection", true);
      ModelTextureMaxHoleArea = new MCH_ConfigPrm("ModelTextureMaxHoleArea", 16);
      ModelTextureMaxHoleThickness = new MCH_ConfigPrm("ModelTextureMaxHoleThickness", 2);
      ModelTextureRGBBleedRadius = new MCH_ConfigPrm("ModelTextureRGBBleedRadius", 2);
      ModelTextureAlphaExpansionRadius = new MCH_ConfigPrm("ModelTextureAlphaExpansionRadius", 1);
      ModelTextureUVCorrectionRadius = new MCH_ConfigPrm("ModelTextureUVCorrectionRadius", 2);
      ModelTextureRepairDebugLogging = new MCH_ConfigPrm("ModelTextureRepairDebugLogging", false);
      ModelTextureRepairDebugPreviews = new MCH_ConfigPrm("ModelTextureRepairDebugPreviews", false);
      MobRenderDistanceWeight = new MCH_ConfigPrm("MobRenderDistanceWeight", 10.0D);
      CreativeTabIcon = new MCH_ConfigPrm("CreativeTabIconItem", "fuel");
      CreativeTabIconHeli = new MCH_ConfigPrm("CreativeTabIconHeli", "ah-64");
      CreativeTabIconPlane = new MCH_ConfigPrm("CreativeTabIconPlane", "f22a");
      CreativeTabIconShip = new MCH_ConfigPrm("CreativeTabIconShip", "project1204");
      CreativeTabIconTank = new MCH_ConfigPrm("CreativeTabIconTank", "merkava_mk4");
      CreativeTabIconVehicle = new MCH_ConfigPrm("CreativeTabIconVehicle", "mk15");
      DisableShader = new MCH_ConfigPrm("DisableShader", false);
      AliveTimeOfCartridge = new MCH_ConfigPrm("AliveTimeOfCartridge", 200);
      InfinityAmmo = new MCH_ConfigPrm("InfinityAmmo", false);
      InfinityFuel = new MCH_ConfigPrm("InfinityFuel", false);
      HitMarkColor = new MCH_ConfigPrm("HitMarkColor", "255, 255, 0, 0");
      HitMarkColor.desc = ";HitMarkColor = Alpha, Red, Green, Blue";
      SmoothShading = new MCH_ConfigPrm("SmoothShading", true);
      BulletBreakableBlock = new MCH_ConfigPrm("BulletBreakableBlocks", "glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily");
      BulletBreakableBlock.validVer = "0.10.4";
      EnableModEntityRender = new MCH_ConfigPrm("EnableModEntityRender", true);
      DisableRenderLivingSpecials = new MCH_ConfigPrm("DisableRenderLivingSpecials", true);
      PreventingBroken = new MCH_ConfigPrm("PreventingBroken", false);
      DropItemInCreativeMode = new MCH_ConfigPrm("DropItemInCreativeMode", false);
      BreakableOnlyPickaxe = new MCH_ConfigPrm("BreakableOnlyPickaxe", false);
      InvertMouse = new MCH_ConfigPrm("InvertMouse", false);
      MouseSensitivity = new MCH_ConfigPrm("MouseSensitivity", 30.0D);
      ZoomSensitivityEffect = new MCH_ConfigPrm("ZoomSensitivityEffect", 100.0D);
      MouseControlStickModeHeli = new MCH_ConfigPrm("MouseControlStickModeHeli", false);
      MouseControlStickModePlane = new MCH_ConfigPrm("MouseControlStickModePlane", false);
      MouseControlFlightSimMode = new MCH_ConfigPrm("MouseControlFlightSimMode", true);
      MouseControlFlightSimMode.desc = ";MouseControlFlightSimMode = true ( Yaw:key, Roll=mouse )";
      EnableMouseAimControls = new MCH_ConfigPrm("EnableMouseAimControls", false);
      EnableMouseAimControls.desc = ";Experimental client-side mouse-follow controls for new-flight-model planes only. KeyPlaneMouseAim toggles this mode while riding a qualifying plane.";
      MouseAimSensitivity = new MCH_ConfigPrm("MouseAimSensitivity", 0.18D);
      MouseAimSmoothing = new MCH_ConfigPrm("MouseAimSmoothing", 0.30D);
      MouseAimMaxPitchUp = new MCH_ConfigPrm("MouseAimMaxPitchUp", 70.0D);
      MouseAimMaxPitchDown = new MCH_ConfigPrm("MouseAimMaxPitchDown", 55.0D);
      MouseAimYawResponse = new MCH_ConfigPrm("MouseAimYawResponse", 0.85D);
      MouseAimPitchResponse = new MCH_ConfigPrm("MouseAimPitchResponse", 0.85D);
      MouseAimAutoBankStrength = new MCH_ConfigPrm("MouseAimAutoBankStrength", 1.10D);
      MouseAimAutoBankMaxRoll = new MCH_ConfigPrm("MouseAimAutoBankMaxRoll", 65.0D);
      MouseAimCenteringStrength = new MCH_ConfigPrm("MouseAimCenteringStrength", 0.18D);
      MouseAimDebug = new MCH_ConfigPrm("MouseAimDebug", false);
      EnablePlaneMouseAimReticle = new MCH_ConfigPrm("EnablePlaneMouseAimReticle", false);
      EnablePlaneMouseAimReticle.desc = ";Draws the custom mouse-aim and nose reticles for new-flight planes while mouse aim is active.";
      HideVanillaCrosshairInPlaneMouseAim = new MCH_ConfigPrm("HideVanillaCrosshairInPlaneMouseAim", false);
      HideVanillaCrosshairInPlaneMouseAim.desc = ";Suppresses the vanilla screen-center Minecraft crosshair only while the new-flight plane mouse-aim reticle is active.";
      PlaneMouseAimReticleTexture = new MCH_ConfigPrm("PlaneMouseAimReticleTexture", "textures/gui/plane_crosshair.png");
      PlaneMouseAimReticleScale = new MCH_ConfigPrm("PlaneMouseAimReticleScale", 1.0D);
      PlaneMouseAimReticleOpacity = new MCH_ConfigPrm("PlaneMouseAimReticleOpacity", 0.90D);
      PlaneNoseReticleScale = new MCH_ConfigPrm("PlaneNoseReticleScale", 0.85D);
      PlaneNoseReticleOpacity = new MCH_ConfigPrm("PlaneNoseReticleOpacity", 0.70D);
      PlaneMouseAimMaxScreenRadius = new MCH_ConfigPrm("PlaneMouseAimMaxScreenRadius", 0.42D);
      PlaneMouseAimYawVisualRange = new MCH_ConfigPrm("PlaneMouseAimYawVisualRange", 45.0D);
      PlaneMouseAimReticleDebug = new MCH_ConfigPrm("PlaneMouseAimReticleDebug", false);
      EnableNewPlaneSimpleHud = new MCH_ConfigPrm("EnableNewPlaneSimpleHud", true);
      EnableNewPlaneSimpleHud.desc = ";Render the compact top-left HUD for new-flight-model planes only.";
      EnableNewPlaneWeaponHud = new MCH_ConfigPrm("EnableNewPlaneWeaponHud", true);
      EnableNewPlaneWeaponHud.desc = ";Render the compact right-side weapon/ammo HUD for new-flight-model planes only.";
      EnableNewPlaneHudGlow = new MCH_ConfigPrm("EnableNewPlaneHudGlow", true);
      EnableNewPlaneHudGlow.desc = ";Deprecated plane-specific glow toggle; EnableNewVehicleHudGlow controls shared new vehicle HUD glow.";
      EnableNewHeliHudSharedReadouts = new MCH_ConfigPrm("EnableNewHeliHudSharedReadouts", true);
      EnableNewHeliHudSharedReadouts.desc = ";Render ALT/VS/FUEL readouts inside the existing helicopter HUD for new-flight helicopters only.";
      EnableNewHeliWeaponHud = new MCH_ConfigPrm("EnableNewHeliWeaponHud", true);
      EnableNewHeliWeaponHud.desc = ";Render the existing-style helicopter weapon/ammo list for new-flight helicopters only.";
      EnableNewVehicleHudGlow = new MCH_ConfigPrm("EnableNewVehicleHudGlow", true);
      EnableNewVehicleHudGlow.desc = ";Draw subtle background panels and one-pixel glow text for shared new vehicle HUD additions.";
      EnableNewVehicleStickInputGauge = new MCH_ConfigPrm("EnableNewVehicleStickInputGauge", true);
      EnableNewVehicleStickInputGauge.desc = ";Draw the stick/mouse input gauge beside new plane and helicopter HUD readouts.";
      NewPlaneSimpleHudX = new MCH_ConfigPrm("NewPlaneSimpleHudX", 12);
      NewPlaneSimpleHudX.desc = ";Top-left new-plane simple HUD X offset in scaled GUI pixels.";
      NewPlaneSimpleHudY = new MCH_ConfigPrm("NewPlaneSimpleHudY", 12);
      NewPlaneSimpleHudY.desc = ";Top-left new-plane simple HUD Y offset in scaled GUI pixels.";
      NewPlaneWeaponHudRightMargin = new MCH_ConfigPrm("NewPlaneWeaponHudRightMargin", 28);
      NewPlaneWeaponHudRightMargin.desc = ";Right-side new-plane weapon HUD margin in scaled GUI pixels.";
      NewPlaneWeaponHudY = new MCH_ConfigPrm("NewPlaneWeaponHudY", 42);
      NewPlaneWeaponHudY.desc = ";Right-side new-plane weapon HUD Y offset in scaled GUI pixels.";
      SwitchWeaponWithMouseWheel = new MCH_ConfigPrm("SwitchWeaponWithMouseWheel", true);
      AllHeliSpeed = new MCH_ConfigPrm("AllHeliSpeed", 1.5D);
      AllPlaneSpeed = new MCH_ConfigPrm("AllPlaneSpeed", 1000.00D);
      NewFlightGravity = new MCH_ConfigPrm("NewFlightGravity", 0.008D);
      NewFlightGravity.desc = ";Default per-tick downward acceleration for new-flight-model aircraft. Vehicle configs can override with NewFlightGravity, FlightGravity, or GravityOverride.";
      NewFlightDiveAssistEnabled = new MCH_ConfigPrm("NewFlightDiveAssistEnabled", true);
      NewFlightDiveAssistEnabled.desc = ";Allows airborne nose-down new-flight planes to convert a small amount of falling speed into forward horizontal speed without changing vertical motion.";
      NewFlightDiveAccelerationMultiplier = new MCH_ConfigPrm("NewFlightDiveAccelerationMultiplier", 0.25D);
      NewFlightDiveAccelerationMultiplier.desc = ";Scales the small horizontal speed boost applied while an airborne new-flight plane is nose-down and descending. Suggested range 0.15..0.35.";
      NewFlightMaxDiveSpeedMultiplier = new MCH_ConfigPrm("NewFlightMaxDiveSpeedMultiplier", 1.25D);
      NewFlightMaxDiveSpeedMultiplier.desc = ";Soft cap for horizontal speed added by new-flight dive assist, expressed as a multiplier of the plane max speed.";
      AllShipSpeed = new MCH_ConfigPrm("AllShipSpeed", 2.0D);
      AllTankSpeed = new MCH_ConfigPrm("AllTankSpeed", 1.0D);
      HurtResistantTime = new MCH_ConfigPrm("HurtResistantTime", 0.0D);
      DisplayHUDThirdPerson = new MCH_ConfigPrm("DisplayHUDThirdPerson", true);
      EnableNewPlaneThirdPersonCamera = new MCH_ConfigPrm("EnableNewPlaneThirdPersonCamera", false);
      EnableNewPlaneThirdPersonCamera.desc = ";Client-only visual chase camera for third-person new-flight planes. Does not change flight physics, weapons, or HUD rendering.";
      NewPlaneCameraDistance = new MCH_ConfigPrm("PlaneChaseBaseDistance", 15.0D);
      NewPlaneCameraDistance.desc = ";Base chase distance in blocks for new-flight third-person planes. default is 15; 16 is often too close and 45-80+ can be reasonable by scale/speed.";
      NewPlaneCameraMinDistance = new MCH_ConfigPrm("PlaneChaseMinDistance", 13.0D);
      NewPlaneCameraMinDistance.desc = ";Minimum chase distance in blocks after size and optional speed tuning; default 13 keeps aircraft framed when slowing down.";
      NewPlaneCameraMaxDistance = new MCH_ConfigPrm("PlaneChaseMaxDistance", 21.0D);
      NewPlaneCameraMaxDistance.desc = ";Maximum chase distance in blocks after size and optional speed tuning; default 21 avoids aggressive zoom breathing.";
      NewPlaneCameraDebugDistance = new MCH_ConfigPrm("NewPlaneCameraDebugDistance", 0.0D);
      NewPlaneCameraDebugDistance.desc = ";DebugFlightControl-only chase camera distance override. Set 40 to verify the render path consumes the custom camera; 0 disables.";
      NewPlaneCameraDebugAbovePlane = new MCH_ConfigPrm("NewPlaneCameraDebugAbovePlane", false);
      NewPlaneCameraDebugAbovePlane.desc = ";DebugFlightControl-only hard test: places the chase dummy at plane.posY + 20 with third-person distance zeroed.";
      NewPlaneCameraHeight = new MCH_ConfigPrm("NewPlaneCameraHeight", 4.0D);
      NewPlaneCameraHeight.desc = ";Blocks above the aircraft for the new third-person plane chase camera.";
      NewPlaneCameraSideOffset = new MCH_ConfigPrm("NewPlaneCameraSideOffset", 0.0D);
      NewPlaneCameraSideOffset.desc = ";Optional horizontal side offset for the new third-person plane chase camera.";
      PlaneChaseSpeedDistanceScale = new MCH_ConfigPrm("PlaneChaseSpeedDistanceScale", 2.0D);
      PlaneChaseSpeedDistanceScale.desc = ";Extra chase distance per block/tick of aircraft speed when speed-based distance is explicitly enabled; kept low to avoid zoom breathing.";
      PlaneChaseSpeedDistanceMaxBonus = new MCH_ConfigPrm("PlaneChaseSpeedDistanceMaxBonus", 3.0D);
      PlaneChaseSpeedDistanceMaxBonus.desc = ";Maximum extra blocks optional speed-based distance may add; default small for stable framing.";
      EnablePlaneChaseFOVOverride = new MCH_ConfigPrm("EnablePlaneChaseFOVOverride", false);
      EnablePlaneChaseFOVOverride.desc = ";Overrides FOV only for the new-flight third-person plane chase camera; does not affect first person, legacy aircraft cameras, or normal Minecraft third person.";
      PlaneChaseFOV = new MCH_ConfigPrm("PlaneChaseFOV", 95.0D);
      PlaneChaseFOV.desc = ";Target new plane chase camera FOV. Recommended range is 85-105; wider FOV improves awareness without excessive camera distance.";
      PlaneChaseFreelookFOV = new MCH_ConfigPrm("PlaneChaseFreelookFOV", 95.0D);
      PlaneChaseFreelookFOV.desc = ";Optional FOV while hold-freelook orbit is active; defaults to match PlaneChaseFOV.";
      PlaneChaseFOVSmoothing = new MCH_ConfigPrm("PlaneChaseFOVSmoothing", 0.25D);
      PlaneChaseFOVSmoothing.desc = ";How quickly the chase FOV override blends in, changes for freelook, and restores when leaving chase camera.";
      NewPlaneCameraSizeDistanceScale = new MCH_ConfigPrm("NewPlaneCameraSizeDistanceScale", 1.25D);
      NewPlaneCameraSizeDistanceScale.desc = ";Extra chase distance per block of aircraft bounding-box size; helps bombers and large planes frame comfortably.";
      PlaneChaseFocusVerticalOffset = new MCH_ConfigPrm("PlaneChaseFocusVerticalOffset", 2.5D);
      PlaneChaseFocusVerticalOffset.desc = ";Raises the new chase camera focus above the aircraft so the model sits lower on screen.";
      PlaneChaseScreenVerticalBias = new MCH_ConfigPrm("PlaneChaseScreenVerticalBias", 4.0D);
      PlaneChaseScreenVerticalBias.desc = ";Additional aim/framing height above the focus so the crosshair favors forward airspace over the plane model.";
      PlaneLookAheadDistance = new MCH_ConfigPrm("PlaneLookAheadDistance", 12.0D);
      PlaneLookAheadDistance.desc = ";Held Plane Look Ahead focus distance in blocks along aircraft facing. Does not alter aircraft controls.";
      PlaneLookAheadSmoothing = new MCH_ConfigPrm("PlaneLookAheadSmoothing", 0.35D);
      PlaneLookAheadSmoothing.desc = ";How quickly held Plane Look Ahead blends into its forward focus.";
      PlaneLookAheadReturnSmoothing = new MCH_ConfigPrm("PlaneLookAheadReturnSmoothing", 0.25D);
      PlaneLookAheadReturnSmoothing.desc = ";How quickly Plane Look Ahead returns to standard centered chase framing after release.";
      FreelookReturnSmoothing = new MCH_ConfigPrm("FreelookReturnSmoothing", 0.28D);
      FreelookReturnSmoothing.desc = ";How quickly hold-freelook returns to normal chase-camera orientation after release.";
      PlaneFreelookOrbitSensitivity = new MCH_ConfigPrm("PlaneFreelookOrbitSensitivity", 0.15D);
      PlaneFreelookOrbitSensitivity.desc = ";Mouse sensitivity multiplier for new chase-camera hold freelook target orbit yaw/pitch.";
      PlaneFreelookYawSmoothing = new MCH_ConfigPrm("PlaneFreelookYawSmoothing", 0.20D);
      PlaneFreelookPitchSmoothing = new MCH_ConfigPrm("PlaneFreelookPitchSmoothing", 0.20D);
      PlaneFreelookReturnSmoothing = new MCH_ConfigPrm("PlaneFreelookReturnSmoothing", 0.14D);
      PlaneFreelookMaxPitchUp = new MCH_ConfigPrm("PlaneFreelookMaxPitchUp", 75.0D);
      PlaneFreelookMaxPitchDown = new MCH_ConfigPrm("PlaneFreelookMaxPitchDown", 65.0D);
      NewPlaneCameraPositionSmoothing = new MCH_ConfigPrm("NewPlaneCameraPositionSmoothing", 0.22D);
      NewPlaneCameraPositionSmoothing.desc = ";How quickly camera position follows the desired point. Higher is snappier.";
      NewPlaneCameraYawSmoothing = new MCH_ConfigPrm("NewPlaneCameraYawSmoothing", 0.26D);
      NewPlaneCameraYawSmoothing.desc = ";How quickly normal chase camera yaw follows the raised aim focus.";
      NewPlaneCameraPitchSmoothing = new MCH_ConfigPrm("NewPlaneCameraPitchSmoothing", 0.22D);
      NewPlaneCameraPitchSmoothing.desc = ";How quickly chase camera pitch follows climbs/dives.";
      NewPlaneCameraDistanceSmoothing = new MCH_ConfigPrm("NewPlaneCameraDistanceSmoothing", 0.16D);
      NewPlaneCameraDistanceSmoothing.desc = ";How quickly speed/size/collision distance changes are applied.";
      NewPlaneCameraFocusSmoothing = new MCH_ConfigPrm("NewPlaneCameraFocusSmoothing", 0.24D);
      NewPlaneCameraFocusSmoothing.desc = ";How quickly the chase focus moves when held look-ahead or framing offsets change.";
      NewPlaneCameraPitchInfluenceSmoothing = new MCH_ConfigPrm("NewPlaneCameraPitchInfluenceSmoothing", 0.30D);
      NewPlaneCameraPitchInfluenceSmoothing.desc = ";Smoothing for plane pitch influence so steep climbs/dives read without snapping.";
      PlaneChasePitchInfluence = new MCH_ConfigPrm("PlaneChasePitchInfluence", 0.25D);
      PlaneChasePitchInfluence.desc = ";How much aircraft pitch affects the new chase camera direction; conservative defaults keep the camera observational instead of glued to the plane.";
      NewPlaneCameraRollInfluence = new MCH_ConfigPrm("NewPlaneCameraRollInfluence", 0.12D);
      NewPlaneCameraRollInfluence.desc = ";0.0 keeps the horizon stable; 1.0 fully rolls the chase camera with the aircraft. Conservative defaults avoid nausea.";
      PlaneChaseHorizonStabilization = new MCH_ConfigPrm("PlaneChaseHorizonStabilization", true);
      PlaneChaseHorizonStabilization.desc = ";Keeps the new chase camera horizon more stable than the aircraft by applying conservative roll and pitch inheritance.";
      EnableNewPlaneCameraSpeedDistance = new MCH_ConfigPrm("EnableSpeedBasedCameraDistance", false);
      EnableNewPlaneCameraSpeedDistance.desc = ";Disabled by default so the new plane chase camera stays near its stable base distance instead of breathing with throttle/speed.";
      EnableNewPlaneCameraCollision = new MCH_ConfigPrm("EnableNewPlaneCameraCollision", false);
      EnablePlaneLookAhead = new MCH_ConfigPrm("EnablePlaneLookAhead", false);
      EnableHoldFreelook = new MCH_ConfigPrm("EnableHoldFreelook", false);
      EnableHoldFreelook.desc = ";When enabled, the Free Look key must be held; when disabled, Free Look toggles on/off with each key press.";
      EnableNewPlaneCameraRollInfluence = new MCH_ConfigPrm("EnableNewPlaneCameraRollInfluence", false);
      NewPlaneCameraCollision = new MCH_ConfigPrm("NewPlaneCameraCollision", false);
      NewPlaneCameraCollision.desc = ";Deprecated alias for EnableNewPlaneCameraCollision; moves the chase camera in front of solid blocks when line-of-sight collision is detected.";
      DisableCameraDistChange = new MCH_ConfigPrm("DisableThirdPersonCameraDistChange", false);
      EnableReplaceTextureManager = new MCH_ConfigPrm("EnableReplaceTextureManager", true);
      DisplayEntityMarker = new MCH_ConfigPrm("DisplayEntityMarker", true);
      DisplayMarkThroughWall = new MCH_ConfigPrm("DisplayMarkThroughWall", true);
      EntityMarkerSize = new MCH_ConfigPrm("EntityMarkerSize", 10.0D);
      BlockMarkerSize = new MCH_ConfigPrm("BlockMarkerSize", 10.0D);
      ReplaceRenderViewEntity = new MCH_ConfigPrm("ReplaceRenderViewEntity", true);
      StingerLockRange = new MCH_ConfigPrm("StingerLockRange", 4988.0D);
      delayrangeloader = new MCH_ConfigPrm("delayrangeloader", 6);
      bombletloader = new MCH_ConfigPrm("bombletloader", 10);
      wrenchdropitem = new MCH_ConfigPrm("wrenchdropitem", false);
      AutoRepairEnabled = new MCH_ConfigPrm("AutoRepairEnabled", false);
      placetimer = new MCH_ConfigPrm("placetimer", 60);
      StingerLockRange.validVer = "1.0.0";
      DefaultExplosionParticle = new MCH_ConfigPrm("DefaultExplosionParticle", false);
      RangeFinderSpotDist = new MCH_ConfigPrm("RangeFinderSpotDist", 400);
      RangeFinderSpotTime = new MCH_ConfigPrm("RangeFinderSpotTime", 15);
      RangeFinderConsume = new MCH_ConfigPrm("RangeFinderConsume", true);
      EnablePutRackInFlying = new MCH_ConfigPrm("EnablePutRackInFlying", true);
      EnableDebugBoundingBox = new MCH_ConfigPrm("EnableDebugBoundingBox", false);
      DebugVehicleBoxCache = new MCH_ConfigPrm("DebugVehicleBoxCache", false);
      DebugVehicleBoxCache.desc = ";Print vehicle collision/hit box cache hits, rebuilds, invalidation reasons, and generated box counts.";
      DebugVehicleLODVisibility = new MCH_ConfigPrm("DebugVehicleLODVisibility", false);
      DebugVehicleLODVisibility.desc = ";Print one bounded distant snapshot visibility diagnostic per second.";
      DebugFlightControl = new MCH_ConfigPrm("DebugFlightControl", false);
      DebugFlightControl.desc = ";Print FPS, elapsed tick fraction, control inputs, angular velocity, pitch/yaw/roll, new-flight gravity/lift, and placement motion-lock state once per second while piloting.";
      DespawnCount = new MCH_ConfigPrm("DespawnCount", 25);
      HitBoxDelayTick = new MCH_ConfigPrm("HitBoxDelayTick", 0);
      EnableRotationLimit = new MCH_ConfigPrm("EnableRotationLimit", false);
      PitchLimitMax = new MCH_ConfigPrm("PitchLimitMax", 10);
      PitchLimitMin = new MCH_ConfigPrm("PitchLimitMin", -10);
      RollLimit = new MCH_ConfigPrm("RollLimit", 35);
      RangeOfGunner_VsMonster_Horizontal = new MCH_ConfigPrm("RangeOfGunner_VsMonster_Horizontal", 80);
      RangeOfGunner_VsMonster_Vertical = new MCH_ConfigPrm("RangeOfGunner_VsMonster_Vertical", 160);
      RangeOfGunner_VsPlayer_Horizontal = new MCH_ConfigPrm("RangeOfGunner_VsPlayer_Horizontal", 200);
      RangeOfGunner_VsPlayer_Vertical = new MCH_ConfigPrm("RangeOfGunner_VsPlayer_Vertical", 300);
      FixVehicleAtPlacedPoint = new MCH_ConfigPrm("FixVehicleAtPlacedPoint", true);
      KillPassengersWhenDestroyed = new MCH_ConfigPrm("KillPassengersWhenDestroyed", false);
      hitMarkColorAlpha = 1.0F;
      hitMarkColorRGB = 16711680;
      ItemRecipe_Fuel = new MCH_ConfigPrm("ItemRecipe_Fuel", "\"ICI\", \"III\", I, iron_ingot, C, coal");
      ItemRecipe_GLTD = new MCH_ConfigPrm("ItemRecipe_GLTD", "\" B \", \"IDI\", \"IRI\", B, mcheli:item.ingot_steel, I, iron_ingot, D, mcheli:fcs, R, redstone");
      ItemRecipe_Chain = new MCH_ConfigPrm("ItemRecipe_Chain", "\"I I\", \"III\", \"I I\", I,  mcheli:item.ingot_steel");
      ItemRecipe_Parachute = new MCH_ConfigPrm("ItemRecipe_Parachute", "\"WWW\", \"S S\", \" W \", W, wool, S, string");
      ItemRecipe_Container = new MCH_ConfigPrm("ItemRecipe_Container", "\"CCI\", C, chest, I, iron_ingot");
      ItemRecipe_UavStation = new MCH_ConfigPrm[]{new MCH_ConfigPrm("ItemRecipe_UavStation", "\"III\", \"IDI\", \"IRI\", I, mcheli:item.ingot_steel, D, mcheli:fcs, R, redstone_block"), new MCH_ConfigPrm("ItemRecipe_UavStation2", "\"IDI\", \"IRI\", I, mcheli:item.ingot_steel, D, mcheli:fcs, R, redstone")};
      ItemRecipe_DraftingTable = new MCH_ConfigPrm("ItemRecipe_DraftingTable", "\"R  \", \"PCP\", \"F F\", R, redstone, C, crafting_table, P, planks, F, fence");
      ItemRecipe_Wrench = new MCH_ConfigPrm("ItemRecipe_Wrench", "\" I \", \" II\", \"I  \", I, mcheli:item.ingot_steel");
      ItemRecipe_RangeFinder = new MCH_ConfigPrm("ItemRecipe_RangeFinder", "\"III\", \"RGR\", \"III\", I, iron_ingot, G, glass, R, redstone");
      ItemRecipe_Stinger = new MCH_ConfigPrm("ItemRecipe_Stinger", "\"G  \", \"III\", \"RI \", G, glass, I, iron_ingot, R, redstone");
      ItemRecipe_StingerMissile = new MCH_ConfigPrm("ItemRecipe_StingerMissile", "\"R  \", \" I \", \"  G\", G, gunpowder, I, iron_ingot, R, redstone");
      ItemRecipe_Javelin = new MCH_ConfigPrm("ItemRecipe_Javelin", "\"III\", \"GR \", G, glass, I, iron_ingot, R, redstone");
      ItemRecipe_JavelinMissile = new MCH_ConfigPrm("ItemRecipe_JavelinMissile", "\" R \", \" I \", \" G \", G, gunpowder, I, iron_ingot, R, redstone");
      ItemRecipe_Rpg = new MCH_ConfigPrm("ItemRecipe_RPG", "\"III\", \"GI \", G, glass, I, iron_ingot");
      ItemRecipe_RpgMissile = new MCH_ConfigPrm("ItemRecipe_RPGMissile", "\" R \", \" I \", \" G \", G, gunpowder, I, iron_ingot, R, redstone");
      ItemID_GLTD = new MCH_ConfigPrm("ItemID_GLTD", 28799);
      ItemID_Chain = new MCH_ConfigPrm("ItemID_Chain", 28798);
      ItemID_Parachute = new MCH_ConfigPrm("ItemID_Parachute", 28797);
      ItemID_Container = new MCH_ConfigPrm("ItemID_Container", 28796);
      ItemID_UavStation = new MCH_ConfigPrm[]{new MCH_ConfigPrm("ItemID_UavStation", 28795), new MCH_ConfigPrm("ItemID_UavStation2", 28790)};
      ItemID_InvisibleItem = new MCH_ConfigPrm("ItemID_Internal", 28794);
      ItemID_Fuel = new MCH_ConfigPrm("ItemID_Fuel", 28793);
      ItemID_DraftingTable = new MCH_ConfigPrm("ItemID_DraftingTable", 28792);
      ItemID_Wrench = new MCH_ConfigPrm("ItemID_Wrench", 28791);
      ItemID_RangeFinder = new MCH_ConfigPrm("ItemID_RangeFinder", 28789);
      ItemID_Stinger = new MCH_ConfigPrm("ItemID_Stinger", 28900);
      ItemID_StingerMissile = new MCH_ConfigPrm("ItemID_StingerMissile", 28901);
      BlockID_DraftingTableOFF = new MCH_ConfigPrm("BlockID_DraftingTable", 3450);
      BlockID_DraftingTableON = new MCH_ConfigPrm("BlockID_DraftingTableON", 3451);
      General = new MCH_ConfigPrm[]{
              EnableMCHLibLog,
              EnableMCHLibDebugLog,
              EnableNEIHandler,
              TestMode,
              EnableCommand,
              EnableModelTextureRepair, EnableModelUVCorrection,
              ModelTextureMaxHoleArea, ModelTextureMaxHoleThickness,
              ModelTextureRGBBleedRadius, ModelTextureAlphaExpansionRadius,
              ModelTextureUVCorrectionRadius, ModelTextureRepairDebugLogging,
              ModelTextureRepairDebugPreviews,
              null,
              PlaceableOnSpongeOnly,
              ItemDamage,
              ItemFuel,
              AutoRepairHP,
              AutoRepairEnabled,
              Explosion_DestroyBlock,
              Explosion_FlamingBlock,
              PiercingBlockHardnessLimit,
              PiercingBlockBlastResistanceLimit,
              DebugPiercingBlocks,
              BulletBreakableBlock,
              Collision_DestroyBlock,
              Collision_Car_BreakableBlock,
              Collision_Car_BreakableMaterial,
              Collision_Tank_BreakableBlock,
              Collision_Tank_BreakableMaterial,
              Collision_EntityDamage,
              Collision_EntityTankDamage,
              InfinityAmmo,
              InfinityFuel,
              DismountAll,
              MountMinecartHeli,
              MountMinecartPlane,
              MountMinecartShip,
              MountMinecartVehicle,
              MountMinecartTank,
              PreventingBroken,
              DropItemInCreativeMode,
              BreakableOnlyPickaxe,
              AllHeliSpeed,
              AllPlaneSpeed,
              NewFlightGravity,
              NewFlightDiveAssistEnabled,
              NewFlightDiveAccelerationMultiplier,
              NewFlightMaxDiveSpeedMultiplier,
              AllShipSpeed,
              AllTankSpeed,
              HurtResistantTime,
              StingerLockRange,
              delayrangeloader,
              bombletloader,
              wrenchdropitem,
              placetimer,
              RangeFinderSpotDist,
              RangeFinderSpotTime,
              RangeFinderConsume,
              EnablePutRackInFlying,
              EnableDebugBoundingBox,
              DebugVehicleBoxCache,
              DebugFlightControl,
              null,
              InvertMouse,
              MouseSensitivity,
              ZoomSensitivityEffect,
              MouseControlStickModeHeli,
              MouseControlStickModePlane,
              MouseControlFlightSimMode,
              EnableMouseAimControls,
              MouseAimSensitivity,
              MouseAimSmoothing,
              MouseAimMaxPitchUp,
              MouseAimMaxPitchDown,
              MouseAimYawResponse,
              MouseAimPitchResponse,
              MouseAimAutoBankStrength,
              MouseAimAutoBankMaxRoll,
              MouseAimCenteringStrength,
              MouseAimDebug,
              EnablePlaneMouseAimReticle,
              HideVanillaCrosshairInPlaneMouseAim,
              PlaneMouseAimReticleTexture,
              PlaneMouseAimReticleScale,
              PlaneMouseAimReticleOpacity,
              PlaneNoseReticleScale,
              PlaneNoseReticleOpacity,
              PlaneMouseAimMaxScreenRadius,
              PlaneMouseAimYawVisualRange,
              PlaneMouseAimReticleDebug,
              EnableNewPlaneSimpleHud,
              EnableNewPlaneWeaponHud,
              EnableNewPlaneHudGlow,
              EnableNewHeliHudSharedReadouts,
              EnableNewHeliWeaponHud,
              EnableNewVehicleHudGlow,
              EnableNewVehicleStickInputGauge,
              NewPlaneSimpleHudX,
              NewPlaneSimpleHudY,
              NewPlaneWeaponHudRightMargin,
              NewPlaneWeaponHudY,
              AutoThrottleDownHeli,
              AutoThrottleDownPlane,
              AutoThrottleDownShip,
              AutoThrottleDownTank,
              SwitchWeaponWithMouseWheel,
              ArtilleryRangeModifier,
              LWeaponAutoFire,
              EnableHandheld,
              DisableItemRender,
              Override3DItemIcon,
              Heli3DItemIconScale,
              Plane3DItemIconScale,
              Ship3DItemIconScale,
              Tank3DItemIconScale,
              Turret3DItemIconScale,
              HideKeybind,
              RenderDistanceWeight,
              EnableAircraftLODRender,
              AircraftLODStartDistance,
              AircraftLODFarDistance,
              AircraftLODVisibilityDistance,
              AircraftLODRainVisibilityMultiplier,
              AircraftLODThunderVisibilityMultiplier,
              AircraftLODThermalContrastExponent,
              AircraftLODOpticalMinPixels,
              AircraftLODThermalMinPixels,
              DebugVehicleLODVisibility,
              MobRenderDistanceWeight,
              CreativeTabIcon,
              CreativeTabIconHeli,
              CreativeTabIconPlane,
              CreativeTabIconShip,
              CreativeTabIconTank,
              CreativeTabIconVehicle,
              DisableShader,
              DefaultExplosionParticle,
              AliveTimeOfCartridge,
              HitMarkColor,
              SmoothShading,
              EnableModEntityRender,
              DisableRenderLivingSpecials,
              DisplayHUDThirdPerson,
              EnableNewPlaneThirdPersonCamera,
              NewPlaneCameraDistance,
              NewPlaneCameraMinDistance,
              NewPlaneCameraMaxDistance,
              NewPlaneCameraDebugDistance,
              NewPlaneCameraDebugAbovePlane,
              NewPlaneCameraHeight,
              NewPlaneCameraSideOffset,
              PlaneChaseSpeedDistanceScale,
              PlaneChaseSpeedDistanceMaxBonus,
              EnablePlaneChaseFOVOverride,
              PlaneChaseFOV,
              PlaneChaseFreelookFOV,
              PlaneChaseFOVSmoothing,
              NewPlaneCameraSizeDistanceScale,
              PlaneChaseFocusVerticalOffset,
              PlaneChaseScreenVerticalBias,
              PlaneLookAheadDistance,
              PlaneLookAheadSmoothing,
              PlaneLookAheadReturnSmoothing,
              FreelookReturnSmoothing,
              PlaneFreelookOrbitSensitivity,
              PlaneFreelookYawSmoothing,
              PlaneFreelookPitchSmoothing,
              PlaneFreelookReturnSmoothing,
              PlaneFreelookMaxPitchUp,
              PlaneFreelookMaxPitchDown,
              NewPlaneCameraPositionSmoothing,
              NewPlaneCameraYawSmoothing,
              NewPlaneCameraPitchSmoothing,
              NewPlaneCameraDistanceSmoothing,
              NewPlaneCameraFocusSmoothing,
              NewPlaneCameraPitchInfluenceSmoothing,
              PlaneChasePitchInfluence,
              NewPlaneCameraRollInfluence,
              PlaneChaseHorizonStabilization,
              EnableNewPlaneCameraSpeedDistance,
              EnableNewPlaneCameraCollision,
              EnablePlaneLookAhead,
              EnableHoldFreelook,
              EnableNewPlaneCameraRollInfluence,
              NewPlaneCameraCollision,
              DisableCameraDistChange,
              EnableReplaceTextureManager,
              DisplayEntityMarker,
              EntityMarkerSize,
              BlockMarkerSize,
              ReplaceRenderViewEntity,
              null,
              ItemRecipe_Fuel,
              ItemRecipe_GLTD,
              ItemRecipe_Chain,
              ItemRecipe_Parachute,
              ItemRecipe_Container,
              ItemRecipe_UavStation[0],
              ItemRecipe_UavStation[1],
              ItemRecipe_DraftingTable,
              ItemRecipe_Wrench,
              ItemRecipe_RangeFinder,
              ItemRecipe_Stinger,
              ItemRecipe_StingerMissile,
              ItemRecipe_Javelin,
              ItemRecipe_JavelinMissile,
              ItemRecipe_Rpg,
              ItemRecipe_RpgMissile,
              MultiThreadedModelLoading
      };
      DamageVsEntity = new MCH_Config.DamageFactor("DamageVsEntity");
      DamageVsLiving = new MCH_Config.DamageFactor("DamageVsLiving");
      DamageVsPlayer = new MCH_Config.DamageFactor("DamageVsPlayer");
      DamageVsMCHeliAircraft = new MCH_Config.DamageFactor("DamageVsMCHeliAircraft");
      DamageVsMCHeliTank = new MCH_Config.DamageFactor("DamageVsMCHeliTank");
      DamageVsMCHeliVehicle = new MCH_Config.DamageFactor("DamageVsMCHeliVehicle");
      DamageVsMCHeliOther = new MCH_Config.DamageFactor("DamageVsMCHeliOther");
      DamageAircraftByExternal = new MCH_Config.DamageFactor("DamageMCHeliAircraftByExternal");
      DamageTankByExternal = new MCH_Config.DamageFactor("DamageMCHeliTankByExternal");
      DamageVehicleByExternal = new MCH_Config.DamageFactor("DamageMCHeliVehicleByExternal");
      DamageOtherByExternal = new MCH_Config.DamageFactor("DamageMCHeliOtherByExternal");
      DamageFactorList = new MCH_Config.DamageFactor[]{DamageVsEntity, DamageVsLiving, DamageVsPlayer, DamageVsMCHeliAircraft, DamageVsMCHeliTank, DamageVsMCHeliVehicle, DamageVsMCHeliOther, DamageAircraftByExternal, DamageTankByExternal, DamageVehicleByExternal, DamageOtherByExternal};
   }

   public void setBlockListFromString(List list, String str) {
      list.clear();
      String[] s = str.split("\\s*,\\s*");
      String[] arr$ = s;
      int len$ = s.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String blockName = arr$[i$];
         Block b = W_Block.getBlockFromName(blockName);
         if(b != null) {
            list.add(b);
         }
      }

   }

   public void setMaterialListFromString(List list, String str) {
      list.clear();
      String[] s = str.split("\\s*,\\s*");
      String[] arr$ = s;
      int len$ = s.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String name = arr$[i$];
         Material m = MCH_Lib.getMaterialFromName(name);
         if(m != null) {
            list.add(m);
         }
      }

   }

   public void correctionParameter() {
      String[] s = HitMarkColor.prmString.split("\\s*,\\s*");
      if(s.length == 4) {
         hitMarkColorAlpha = (float)this.toInt255(s[0]) / 255.0F;
         hitMarkColorRGB = this.toInt255(s[1]) << 16 | this.toInt255(s[2]) << 8 | this.toInt255(s[3]);
      }

      AllHeliSpeed.prmDouble = MCH_Lib.RNG(AllHeliSpeed.prmDouble, 0.0D, 1000.0D);
      AllPlaneSpeed.prmDouble = MCH_Lib.RNG(AllPlaneSpeed.prmDouble, 0.0D, 1000.0D);
      NewFlightGravity.prmDouble = MCH_Lib.RNG(NewFlightGravity.prmDouble, 0.001D, 0.2D);
      NewFlightDiveAccelerationMultiplier.prmDouble = MCH_Lib.RNG(NewFlightDiveAccelerationMultiplier.prmDouble, 0.15D, 0.35D);
      NewFlightMaxDiveSpeedMultiplier.prmDouble = MCH_Lib.RNG(NewFlightMaxDiveSpeedMultiplier.prmDouble, 1.0D, 2.0D);
      if(Math.abs(NewPlaneCameraDistance.prmDouble - 36.0D) < 0.001D || Math.abs(NewPlaneCameraDistance.prmDouble - 45.0D) < 0.001D || Math.abs(NewPlaneCameraDistance.prmDouble - 30.0D) < 0.001D) {
         NewPlaneCameraDistance.prmDouble = 15.0D;
      }
      if(Math.abs(NewPlaneCameraMinDistance.prmDouble - 18.0D) < 0.001D || Math.abs(NewPlaneCameraMinDistance.prmDouble - 24.0D) < 0.001D || Math.abs(NewPlaneCameraMinDistance.prmDouble - 40.0D) < 0.001D || Math.abs(NewPlaneCameraMinDistance.prmDouble - 26.0D) < 0.001D) {
         NewPlaneCameraMinDistance.prmDouble = 13.0D;
      }
      if(Math.abs(NewPlaneCameraMaxDistance.prmDouble - 70.0D) < 0.001D || Math.abs(NewPlaneCameraMaxDistance.prmDouble - 90.0D) < 0.001D || Math.abs(NewPlaneCameraMaxDistance.prmDouble - 55.0D) < 0.001D || Math.abs(NewPlaneCameraMaxDistance.prmDouble - 42.0D) < 0.001D) {
         NewPlaneCameraMaxDistance.prmDouble = 21.0D;
      }
      NewPlaneCameraDistance.prmDouble = MCH_Lib.RNG(NewPlaneCameraDistance.prmDouble, 8.0D, 120.0D);
      NewPlaneCameraMinDistance.prmDouble = MCH_Lib.RNG(NewPlaneCameraMinDistance.prmDouble, 4.0D, NewPlaneCameraDistance.prmDouble);
      NewPlaneCameraMaxDistance.prmDouble = MCH_Lib.RNG(NewPlaneCameraMaxDistance.prmDouble, NewPlaneCameraMinDistance.prmDouble, 160.0D);
      NewPlaneCameraDebugDistance.prmDouble = MCH_Lib.RNG(NewPlaneCameraDebugDistance.prmDouble, 0.0D, 120.0D);
      NewPlaneCameraHeight.prmDouble = MCH_Lib.RNG(NewPlaneCameraHeight.prmDouble, -4.0D, 30.0D);
      NewPlaneCameraSideOffset.prmDouble = MCH_Lib.RNG(NewPlaneCameraSideOffset.prmDouble, -20.0D, 20.0D);
      PlaneChaseSpeedDistanceScale.prmDouble = MCH_Lib.RNG(PlaneChaseSpeedDistanceScale.prmDouble, 0.0D, 80.0D);
      NewPlaneCameraSizeDistanceScale.prmDouble = MCH_Lib.RNG(NewPlaneCameraSizeDistanceScale.prmDouble, 0.0D, 5.0D);
      PlaneChaseFocusVerticalOffset.prmDouble = MCH_Lib.RNG(PlaneChaseFocusVerticalOffset.prmDouble, -10.0D, 30.0D);
      PlaneChaseScreenVerticalBias.prmDouble = MCH_Lib.RNG(PlaneChaseScreenVerticalBias.prmDouble, -10.0D, 30.0D);
      PlaneLookAheadDistance.prmDouble = MCH_Lib.RNG(PlaneLookAheadDistance.prmDouble, 0.0D, 80.0D);
      PlaneLookAheadSmoothing.prmDouble = MCH_Lib.RNG(PlaneLookAheadSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneLookAheadReturnSmoothing.prmDouble = MCH_Lib.RNG(PlaneLookAheadReturnSmoothing.prmDouble, 0.01D, 1.0D);
      FreelookReturnSmoothing.prmDouble = MCH_Lib.RNG(FreelookReturnSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneFreelookOrbitSensitivity.prmDouble = MCH_Lib.RNG(PlaneFreelookOrbitSensitivity.prmDouble, 0.01D, 2.0D);
      NewPlaneCameraPositionSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraPositionSmoothing.prmDouble, 0.01D, 1.0D);
      NewPlaneCameraYawSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraYawSmoothing.prmDouble, 0.01D, 1.0D);
      NewPlaneCameraPitchSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraPitchSmoothing.prmDouble, 0.01D, 1.0D);
      NewPlaneCameraDistanceSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraDistanceSmoothing.prmDouble, 0.01D, 1.0D);
      NewPlaneCameraFocusSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraFocusSmoothing.prmDouble, 0.01D, 1.0D);
      NewPlaneCameraPitchInfluenceSmoothing.prmDouble = MCH_Lib.RNG(NewPlaneCameraPitchInfluenceSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneChasePitchInfluence.prmDouble = MCH_Lib.RNG(PlaneChasePitchInfluence.prmDouble, 0.0D, 1.0D);
      PlaneChaseSpeedDistanceMaxBonus.prmDouble = MCH_Lib.RNG(PlaneChaseSpeedDistanceMaxBonus.prmDouble, 0.0D, 20.0D);
      PlaneChaseFOV.prmDouble = MCH_Lib.RNG(PlaneChaseFOV.prmDouble, 30.0D, 120.0D);
      PlaneChaseFreelookFOV.prmDouble = MCH_Lib.RNG(PlaneChaseFreelookFOV.prmDouble, 30.0D, 120.0D);
      PlaneChaseFOVSmoothing.prmDouble = MCH_Lib.RNG(PlaneChaseFOVSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneFreelookYawSmoothing.prmDouble = MCH_Lib.RNG(PlaneFreelookYawSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneFreelookPitchSmoothing.prmDouble = MCH_Lib.RNG(PlaneFreelookPitchSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneFreelookReturnSmoothing.prmDouble = MCH_Lib.RNG(PlaneFreelookReturnSmoothing.prmDouble, 0.01D, 1.0D);
      PlaneFreelookMaxPitchUp.prmDouble = MCH_Lib.RNG(PlaneFreelookMaxPitchUp.prmDouble, 0.0D, 89.0D);
      PlaneFreelookMaxPitchDown.prmDouble = MCH_Lib.RNG(PlaneFreelookMaxPitchDown.prmDouble, 0.0D, 89.0D);
      NewPlaneCameraRollInfluence.prmDouble = MCH_Lib.RNG(NewPlaneCameraRollInfluence.prmDouble, 0.0D, 1.0D);
      AllTankSpeed.prmDouble = MCH_Lib.RNG(AllTankSpeed.prmDouble, 0.0D, 1000.0D);
      AllShipSpeed.prmDouble = MCH_Lib.RNG(AllShipSpeed.prmDouble, 0.0D, 1000.0D);
      ZoomSensitivityEffect.prmDouble = finiteRange(ZoomSensitivityEffect.prmDouble, 0.0D, 100.0D, 100.0D);
      this.setBlockListFromString(bulletBreakableBlocks, BulletBreakableBlock.prmString);
      this.setBlockListFromString(carBreakableBlocks, Collision_Car_BreakableBlock.prmString);
      this.setBlockListFromString(carNoBreakableBlocks, Collision_Car_NoBreakableBlock.prmString);
      this.setMaterialListFromString(carBreakableMaterials, Collision_Car_BreakableMaterial.prmString);
      this.setBlockListFromString(tankBreakableBlocks, Collision_Tank_BreakableBlock.prmString);
      this.setBlockListFromString(tankNoBreakableBlocks, Collision_Tank_NoBreakableBlock.prmString);
      this.setMaterialListFromString(tankBreakableMaterials, Collision_Tank_BreakableMaterial.prmString);
      if(EntityMarkerSize.prmDouble < 0.0D) {
         EntityMarkerSize.prmDouble = 0.0D;
      }

      if(BlockMarkerSize.prmDouble < 0.0D) {
         BlockMarkerSize.prmDouble = 0.0D;
      }

      if(HurtResistantTime.prmDouble < 0.0D) {
         HurtResistantTime.prmDouble = 0.0D;
      }

      if(HurtResistantTime.prmDouble > 10000.0D) {
         HurtResistantTime.prmDouble = 10000.0D;
      }

      if(MobRenderDistanceWeight.prmDouble < 0.1D) {
         MobRenderDistanceWeight.prmDouble = 0.1D;
      } else if(MobRenderDistanceWeight.prmDouble > 100.0D) {
         //why is this here?
         MobRenderDistanceWeight.prmDouble = 100.0D;
      }

      AircraftLODStartDistance.prmDouble = finiteRange(AircraftLODStartDistance.prmDouble, 0.0D, mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE, 140.0D);

      AircraftLODFarDistance.prmDouble = finiteRange(AircraftLODFarDistance.prmDouble, 1.0D, mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE, mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE);

      if(AircraftLODFarDistance.prmDouble > 0.0D && AircraftLODFarDistance.prmDouble < AircraftLODStartDistance.prmDouble) {
         AircraftLODFarDistance.prmDouble = AircraftLODStartDistance.prmDouble;
      }

      AircraftLODVisibilityDistance.prmDouble = finiteRange(AircraftLODVisibilityDistance.prmDouble, 1.0D, mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE, mcheli.lod.MCH_VehicleLODVisibility.MAX_LOD_DISTANCE);
      AircraftLODRainVisibilityMultiplier.prmDouble = finiteRange(AircraftLODRainVisibilityMultiplier.prmDouble, 0.01D, 1.0D, 0.70D);
      AircraftLODThunderVisibilityMultiplier.prmDouble = finiteRange(AircraftLODThunderVisibilityMultiplier.prmDouble, 0.01D, 1.0D, 0.45D);
      AircraftLODThunderVisibilityMultiplier.prmDouble = Math.min(AircraftLODThunderVisibilityMultiplier.prmDouble,
         AircraftLODRainVisibilityMultiplier.prmDouble);
      AircraftLODThermalContrastExponent.prmDouble = finiteRange(AircraftLODThermalContrastExponent.prmDouble, 0.01D, 1.0D, 0.35D);
      AircraftLODOpticalMinPixels.prmDouble = finiteRange(AircraftLODOpticalMinPixels.prmDouble, 0.0D, 64.0D, 0.75D);
      AircraftLODThermalMinPixels.prmDouble = finiteRange(AircraftLODThermalMinPixels.prmDouble, 0.0D, 64.0D, 0.35D);

      Iterator isNoDamageVsSetting = CommandPermission.iterator();

      while(isNoDamageVsSetting.hasNext()) {
         MCH_ConfigPrm arr$ = (MCH_ConfigPrm)isNoDamageVsSetting.next();
         MCH_Config.CommandPermission len$ = new MCH_Config.CommandPermission(arr$.prmString);
         if(!len$.name.isEmpty()) {
            CommandPermissionList.add(len$);
         }
      }

      if(IgnoreBulletHitList.size() <= 0) {
         IgnoreBulletHitList.add("flansmod.common.guns.EntityBullet");
         IgnoreBulletHitList.add("flansmod.common.guns.EntityGrenade");
      }

      boolean var10 = DamageVs.size() <= 0;
      Iterator var11 = DamageVs.iterator();

      while(var11.hasNext()) {
         MCH_ConfigPrm var13 = (MCH_ConfigPrm)var11.next();
         MCH_Config.DamageFactor[] i$ = DamageFactorList;
         int df = i$.length;

         for(int foundCommon = 0; foundCommon < df; ++foundCommon) {
            MCH_Config.DamageFactor i$1 = i$[foundCommon];
            if(var13.name.equals(i$1.itemName)) {
               i$1.list.add(this.newDamageEntity(var13.prmString));
            }
         }
      }

      MCH_Config.DamageFactor[] var12 = DamageFactorList;
      int var14 = var12.length;

      for(int var15 = 0; var15 < var14; ++var15) {
         MCH_Config.DamageFactor var16 = var12[var15];
         if(var16.list.size() <= 0) {
            DamageVs.add(new MCH_ConfigPrm(var16.itemName, "1.0"));
         } else {
            boolean var17 = false;
            Iterator var18 = var16.list.iterator();

            while(var18.hasNext()) {
               MCH_Config.DamageEntity n = (MCH_Config.DamageEntity)var18.next();
               if(n.name.isEmpty()) {
                  var17 = true;
                  break;
               }
            }

            if(!var17) {
               DamageVs.add(new MCH_ConfigPrm(var16.itemName, "1.0"));
            }
         }
      }
      if (DespawnCount.prmInt <= 0)
         DespawnCount.prmInt = 1;
      if (HitBoxDelayTick.prmInt < 0)
         HitBoxDelayTick.prmInt = 0;
      if (HitBoxDelayTick.prmInt > 50)
         HitBoxDelayTick.prmInt = 50;
      PitchLimitMax.prmInt = (PitchLimitMax.prmInt > 80) ? 80 : (Math.max(PitchLimitMax.prmInt, 0));
      PitchLimitMin.prmInt = (PitchLimitMin.prmInt < -80) ? -80 : (Math.min(PitchLimitMin.prmInt, 0));
      RollLimit.prmInt = (RollLimit.prmInt > 80) ? 80 : (Math.max(RollLimit.prmInt, 0));
      if(var10) {
         DamageVs.add(new MCH_ConfigPrm("DamageVsEntity", "3.0, flansmod"));
         DamageVs.add(new MCH_ConfigPrm("DamageMCHeliAircraftByExternal", "0.5, flansmod"));
         DamageVs.add(new MCH_ConfigPrm("DamageMCHeliVehicleByExternal", "0.5, flansmod"));
      }

   }

   public MCH_Config.DamageEntity newDamageEntity(String s) {
      String[] splt = s.split("\\s*,\\s*");
      return splt.length == 1?new MCH_Config.DamageEntity(Double.parseDouble(splt[0]), ""):(splt.length == 2?new MCH_Config.DamageEntity(Double.parseDouble(splt[0]), splt[1]):new MCH_Config.DamageEntity(1.0D, ""));
   }

   public static float applyDamageByExternal(Entity target, DamageSource ds, float damage) {
      List list;
      if(!(target instanceof MCH_EntityHeli) && !(target instanceof MCP_EntityPlane)) {
         if(target instanceof MCH_EntityTank) {
            list = DamageTankByExternal.list;
         } else if(target instanceof MCH_EntityTurret) {
            list = DamageVehicleByExternal.list;
         } else {
            list = DamageOtherByExternal.list;
         }
      } else {
         list = DamageAircraftByExternal.list;
      }

      Entity attacker = ds.getEntity();
      Entity attackerSource = ds.getSourceOfDamage();
      Iterator i$ = list.iterator();

      while(i$.hasNext()) {
         MCH_Config.DamageEntity de = (MCH_Config.DamageEntity)i$.next();
         if(de.name.isEmpty() || attacker != null && attacker.getClass().toString().indexOf(de.name) > 0 || attackerSource != null && attackerSource.getClass().toString().indexOf(de.name) > 0) {
            damage = (float)((double)damage * de.factor);
         }
      }

      return damage;
   }

   public static float applyDamageVsEntity(Entity target, DamageSource ds, float damage) {
      if(target == null) {
         return damage;
      } else {
         String targetName = target.getClass().toString();
         List list;
         if(!(target instanceof MCH_EntityHeli) && !(target instanceof MCP_EntityPlane)) {
            if(target instanceof MCH_EntityTank) {
               list = DamageVsMCHeliTank.list;
            } else if(target instanceof MCH_EntityTurret) {
               list = DamageVsMCHeliVehicle.list;
            } else if(target instanceof EntityPlayer || target instanceof EntityVillager) {
               list = DamageVsPlayer.list;
            } else if(targetName.indexOf("mcheli.") > 0) {
               list = DamageVsMCHeliOther.list;
            } else if(target instanceof EntityLivingBase) {
               list = DamageVsLiving.list;
            } else {
               list = DamageVsEntity.list;
            }
         } else {
            list = DamageVsMCHeliAircraft.list;
         }

         Iterator i$ = list.iterator();

         while(i$.hasNext()) {
            MCH_Config.DamageEntity de = (MCH_Config.DamageEntity)i$.next();
            if(de.name.isEmpty() || targetName.indexOf(de.name) > 0) {
               damage = (float)((double)damage * de.factor);
            }
         }

         return damage;
      }
   }

   public static List getBreakableBlockListFromType(int n) {
      return n == 2?tankBreakableBlocks:(n == 1?carBreakableBlocks:dummyBreakableBlocks);
   }

   public static List getNoBreakableBlockListFromType(int n) {
      return n == 2?tankNoBreakableBlocks:(n == 1?carNoBreakableBlocks:dummyBreakableBlocks);
   }

   public static List getBreakableMaterialListFromType(int n) {
      return n == 2?tankBreakableMaterials:(n == 1?carBreakableMaterials:dummyBreakableMaterials);
   }

   public int toInt255(String s) {
      int a = Integer.valueOf(s).intValue();
      return a < 0?0:(a > 255?255:a);
   }

   public void load() {
      MCH_InputFile file = new MCH_InputFile();
      if(file.open(configFilePath)) {
         for(String str = file.readLine(); str != null; str = file.readLine()) {
            if(str.trim().equalsIgnoreCase("McHeliOutputDebugLog")) {
               EnableMCHLibDebugLog.prmBool = true;
            } else {
               this.readConfigData(str);
            }
         }

         file.close();
         MCH_Lib.Log("loaded " + file.file.getAbsolutePath(), new Object[0]);
      } else {
         MCH_Lib.Log("" + (new File(configFilePath)).getAbsolutePath() + " not found.", new Object[0]);
      }

      this.correctionParameter();
   }

   private void readConfigData(String str) {
      String[] s = str.split("=");
      if(s.length == 2) {
         s[0] = s[0].trim();
         s[1] = s[1].trim();
         if(s[0].equalsIgnoreCase("MOD_Version")) {
            configVer = s[1];
         } else {
            if(s[0].equalsIgnoreCase("CommandPermission")) {
               CommandPermission.add(new MCH_ConfigPrm("CommandPermission", s[1]));
            }

            MCH_Config.DamageFactor[] arr$ = DamageFactorList;
            int len$ = arr$.length;

            int i$;
            for(i$ = 0; i$ < len$; ++i$) {
               MCH_Config.DamageFactor p = arr$[i$];
               if(p.itemName.equalsIgnoreCase(s[0])) {
                  DamageVs.add(new MCH_ConfigPrm(p.itemName, s[1]));
               }
            }

            if(IgnoreBulletHitItem.compare(s[0])) {
               IgnoreBulletHitList.add(s[1]);
            }

            MCH_ConfigPrm[] var7 = KeyConfig;
            len$ = var7.length;

            MCH_ConfigPrm var8;
            for(i$ = 0; i$ < len$; ++i$) {
               var8 = var7[i$];
               if(var8 != null && var8.compare(s[0]) && var8.isValidVer(configVer)) {
                  var8.setPrm(s[1]);
                  return;
               }
            }

            var7 = General;
            len$ = var7.length;

            for(i$ = 0; i$ < len$; ++i$) {
               var8 = var7[i$];
               if(var8 != null && var8.compare(s[0]) && var8.isValidVer(configVer)) {
                  var8.setPrm(s[1]);
                  if(var8 == ArtilleryRangeModifier && ArtilleryRangeModifier.prmDouble < 0.01D) {
                     ArtilleryRangeModifier.prmDouble = 0.01D;
                  }
                  return;
               }
            }

         }
      }
   }

   public void write() {
      MCH_OutputFile file = new MCH_OutputFile();
      if(file.open(configFilePath)) {
         this.writeConfigData(file.pw);
         file.close();
         MCH_Lib.Log("update " + file.file.getAbsolutePath(), new Object[0]);
      } else {
         MCH_Lib.Log("" + (new File(configFilePath)).getAbsolutePath() + " cannot open.", new Object[0]);
      }

   }

   private void writeConfigData(PrintWriter pw) {
      pw.println("[General]");
      pw.println("MOD_Name = mcheli");
      pw.println("MOD_Version = " + MCH_MOD.VER);
      pw.println("MOD_MC_Version = 1.7.10");
      pw.println();
      MCH_ConfigPrm[] arr$ = General;
      int len$ = arr$.length;

      int i$;
      MCH_ConfigPrm p;
      for(i$ = 0; i$ < len$; ++i$) {
         p = arr$[i$];
         if(p != null) {
            if(!p.desc.isEmpty()) {
               pw.println(p.desc);
            }

            pw.println(p.name + " = " + p);
         } else {
            pw.println("");
         }
      }

      pw.println();
      Iterator var6 = DamageVs.iterator();

      MCH_ConfigPrm var7;
      while(var6.hasNext()) {
         var7 = (MCH_ConfigPrm)var6.next();
         pw.println(var7.name + " = " + var7);
      }

      pw.println();
      var6 = IgnoreBulletHitList.iterator();

      while(var6.hasNext()) {
         String var8 = (String)var6.next();
         pw.println(IgnoreBulletHitItem.name + " = " + var8);
      }

      pw.println();
      pw.println(";CommandPermission = commandName(eg, modlist, status, fill...):playerName1, playerName2, playerName3...");
      if(CommandPermission.size() == 0) {
         pw.println(";CommandPermission = modlist :example1, example2");
         pw.println(";CommandPermission = status :  example2");
      }

      var6 = CommandPermission.iterator();

      while(var6.hasNext()) {
         var7 = (MCH_ConfigPrm)var6.next();
         pw.println(var7.name + " = " + var7);
      }

      pw.println();
      pw.println();
      pw.println("[Key config]");
      pw.println("http://minecraft.gamepedia.com/Key_codes");
      pw.println();
      arr$ = KeyConfig;
      len$ = arr$.length;

      for(i$ = 0; i$ < len$; ++i$) {
         p = arr$[i$];
         pw.println(p.name + " = " + p);
      }

   }


   private static double finitePositive(double value, double fallback) {
      return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0.0D ? value : fallback;
   }

   private static double finiteRange(double value, double minimum, double maximum, double fallback) {
      if(Double.isNaN(value) || Double.isInfinite(value)) return fallback;
      return Math.max(minimum, Math.min(maximum, value));
   }

   class DamageFactor {

      public final String itemName;
      public List list;


      public DamageFactor(String itemName) {
         this.itemName = itemName;
         this.list = new ArrayList();
      }
   }

   class DamageEntity {

      public final double factor;
      public final String name;


      public DamageEntity(double factor, String name) {
         this.factor = factor;
         this.name = name;
      }
   }

   public class CommandPermission {

      public final String name;
      public final String[] players;


      public CommandPermission(String param) {
         String[] s = param.split(":");
         if(s.length == 2) {
            this.name = s[0].toLowerCase().trim();
            this.players = s[1].trim().split("\\s*,\\s*");
         } else {
            this.name = "";
            this.players = new String[0];
         }

      }
   }
}
