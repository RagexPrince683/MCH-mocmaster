package mcheli;

import mcheli.helicopter.MCH_EntityHeli;
import mcheli.plane.MCP_EntityPlane;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_Block;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_Config {

   public static String mcPath;
   public static String configFilePath;
   public static boolean DebugLog;
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
   public static MCH_ConfigPrm KeyRight;
   public static MCH_ConfigPrm KeyLeft;
   public static MCH_ConfigPrm KeySwitchMode;
   public static MCH_ConfigPrm KeySwitchHovering;
   public static MCH_ConfigPrm KeyAttack;
   public static MCH_ConfigPrm KeyUseWeapon;
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
   public static MCH_ConfigPrm KeyGUI;
   public static MCH_ConfigPrm KeyGearUpDown;
   public static MCH_ConfigPrm KeyPutToRack;
   public static MCH_ConfigPrm KeyDownFromRack;
   public static MCH_ConfigPrm KeyScoreboard;
   public static MCH_ConfigPrm KeyMultiplayManager;
   
   public static MCH_ConfigPrm KeyTDCUp;
   public static MCH_ConfigPrm KeyTDCDown;
   public static MCH_ConfigPrm KeyTDCLeft;
   public static MCH_ConfigPrm KeyTDCRight;
   public static MCH_ConfigPrm KeyTDCModeIncr;
   public static MCH_ConfigPrm KeyTDCModeDecr;
   public static MCH_ConfigPrm KeyTDCLock;
   public static MCH_ConfigPrm KeyThrottleUp;
   public static MCH_ConfigPrm KeyThrottleDown;
   
   
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
   public static MCH_ConfigPrm Collision_Car_BreakableBlock;
   public static MCH_ConfigPrm Collision_Car_NoBreakableBlock;
   public static MCH_ConfigPrm Collision_Car_BreakableMaterial;
   public static MCH_ConfigPrm Collision_Tank_BreakableBlock;
   public static MCH_ConfigPrm Collision_Tank_NoBreakableBlock;
   public static MCH_ConfigPrm Collision_Tank_BreakableMaterial;
   public static MCH_ConfigPrm Collision_EntityDamage;
   public static MCH_ConfigPrm Collision_EntityTankDamage;
   public static MCH_ConfigPrm LWeaponAutoFire;
   public static MCH_ConfigPrm DismountAll;
   public static MCH_ConfigPrm MountMinecartHeli;
   public static MCH_ConfigPrm MountMinecartPlane;
   public static MCH_ConfigPrm MountMinecartVehicle;
   public static MCH_ConfigPrm MountMinecartTank;
   public static MCH_ConfigPrm AutoThrottleDownHeli;
   public static MCH_ConfigPrm AutoThrottleDownPlane;
   public static MCH_ConfigPrm AutoThrottleDownTank;
   public static MCH_ConfigPrm DisableItemRender;
   public static MCH_ConfigPrm RenderDistanceWeight;
   public static MCH_ConfigPrm MobRenderDistanceWeight;
   public static MCH_ConfigPrm CreativeTabIcon;
   public static MCH_ConfigPrm CreativeTabIconHeli;
   public static MCH_ConfigPrm CreativeTabIconPlane;
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
   public static MCH_ConfigPrm MouseControlStickModeHeli;
   public static MCH_ConfigPrm MouseControlStickModePlane;
   public static MCH_ConfigPrm MouseControlFlightSimMode;
   public static MCH_ConfigPrm SwitchWeaponWithMouseWheel;
   public static MCH_ConfigPrm AllPlaneSpeed;
   public static MCH_ConfigPrm AllHeliSpeed;
   public static MCH_ConfigPrm AllTankSpeed;
   public static MCH_ConfigPrm HurtResistantTime;
   public static MCH_ConfigPrm DisplayHUDThirdPerson;
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
   public static MCH_ConfigPrm[] ItemRecipe_UavStation;
   public static MCH_ConfigPrm ItemRecipe_DraftingTable;
   public static MCH_ConfigPrm ItemRecipe_Wrench;
   public static MCH_ConfigPrm ItemRecipe_RangeFinder;
   public static MCH_ConfigPrm[] KeyConfig;
   public static MCH_ConfigPrm[] General;
   public final String destroyBlockNames = "glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily";


   public MCH_Config(String minecraftPath, String cfgFile) {
      mcPath = minecraftPath;
      configFilePath = mcPath + cfgFile;
      DebugLog = false;
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
      KeyRight = new MCH_ConfigPrm("KeyRight", 32);
      KeyLeft = new MCH_ConfigPrm("KeyLeft", 30);
      KeySwitchMode = new MCH_ConfigPrm("KeySwitchGunner", 35);
      KeySwitchHovering = new MCH_ConfigPrm("KeySwitchHovering", 57);
      KeyAttack = new MCH_ConfigPrm("KeyAttack", -100);
      KeyUseWeapon = new MCH_ConfigPrm("KeyUseWeapon", -99);
      KeySwitchWeapon1 = new MCH_ConfigPrm("KeySwitchWeapon1", -98);
      KeySwitchWeapon2 = new MCH_ConfigPrm("KeySwitchWeapon2", 34);
      KeySwWeaponMode = new MCH_ConfigPrm("KeySwitchWeaponMode", 45);
      KeyZoom = new MCH_ConfigPrm("KeyZoom", 44);
      KeyCameraMode = new MCH_ConfigPrm("KeyCameraMode", 46);
      KeyUnmount = new MCH_ConfigPrm("KeyUnmountMob", 21);
      KeyFlare = new MCH_ConfigPrm("KeyFlare", 47);
      KeyExtra = new MCH_ConfigPrm("KeyExtra", 33);
      KeyCameraDistUp = new MCH_ConfigPrm("KeyCameraDistanceUp", 201);
      KeyCameraDistDown = new MCH_ConfigPrm("KeyCameraDistanceDown", 209);
      KeyFreeLook = new MCH_ConfigPrm("KeyFreeLook", 29);
      KeyGUI = new MCH_ConfigPrm("KeyGUI", 19);
      KeyGearUpDown = new MCH_ConfigPrm("KeyGearUpDown", 48);
      KeyPutToRack = new MCH_ConfigPrm("KeyPutToRack", 36);
      KeyDownFromRack = new MCH_ConfigPrm("KeyDownFromRack", 22);
      KeyScoreboard = new MCH_ConfigPrm("KeyScoreboard", 38);
      KeyMultiplayManager = new MCH_ConfigPrm("KeyMultiplayManager", 50);
      
      KeyTDCUp = new MCH_ConfigPrm("KeyTDCUp", Keyboard.KEY_UP);
      KeyTDCDown = new MCH_ConfigPrm("KeyTDCDown", Keyboard.KEY_DOWN);
      KeyTDCLeft = new MCH_ConfigPrm("KeyTDCLeft", Keyboard.KEY_LEFT);
      KeyTDCRight = new MCH_ConfigPrm("KeyTDCRight", Keyboard.KEY_RIGHT);
      KeyTDCModeIncr = new MCH_ConfigPrm("KeyTDCModeIncr", Keyboard.KEY_HOME);
      KeyTDCModeDecr = new MCH_ConfigPrm("KeyTDCModeDecr", Keyboard.KEY_END);
      KeyTDCLock = new MCH_ConfigPrm("KeyTDCLock", Keyboard.KEY_RETURN);
      KeyThrottleUp = new MCH_ConfigPrm("KeyThrottleUp", Keyboard.KEY_RBRACKET);
      KeyThrottleDown = new MCH_ConfigPrm("KeyThrottleDown", Keyboard.KEY_LBRACKET);
      
      
      KeyConfig = new MCH_ConfigPrm[]{KeyUp, KeyDown, KeyRight, KeyLeft, KeySwitchMode, KeySwitchHovering, KeySwitchWeapon1, KeySwitchWeapon2, KeySwWeaponMode, KeyZoom, KeyCameraMode, KeyUnmount, KeyFlare, KeyExtra, KeyCameraDistUp, KeyCameraDistDown, KeyFreeLook, KeyGUI, KeyGearUpDown, KeyPutToRack, KeyDownFromRack, KeyScoreboard, KeyMultiplayManager, KeyTDCUp, KeyTDCDown, KeyTDCLeft,KeyTDCRight,KeyTDCModeIncr,KeyTDCModeDecr,KeyTDCLock,KeyThrottleUp,KeyThrottleDown};
      DamageVs = new ArrayList();
      CommandPermission = new ArrayList();
      CommandPermissionList = new ArrayList();
      IgnoreBulletHitList = new ArrayList();
      IgnoreBulletHitItem = new MCH_ConfigPrm("IgnoreBulletHit", "");
      TestMode = new MCH_ConfigPrm("TestMode", false);
      EnableCommand = new MCH_ConfigPrm("EnableCommand", true);
      PlaceableOnSpongeOnly = new MCH_ConfigPrm("PlaceableOnSpongeOnly", false);
      HideKeybind = new MCH_ConfigPrm("HideKeybind", false);
      ItemDamage = new MCH_ConfigPrm("ItemDamage", true);
      ItemFuel = new MCH_ConfigPrm("ItemFuel", true);
      AutoRepairHP = new MCH_ConfigPrm("AutoRepairHP", 0.5D);
      Collision_DestroyBlock = new MCH_ConfigPrm("Collision_DestroyBlock", true);
      Explosion_DestroyBlock = new MCH_ConfigPrm("Explosion_DestroyBlock", true);
      Explosion_FlamingBlock = new MCH_ConfigPrm("Explosion_FlamingBlock", true);
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
      DismountAll = new MCH_ConfigPrm("DismountAll", false);
      MountMinecartHeli = new MCH_ConfigPrm("MountMinecartHeli", true);
      MountMinecartPlane = new MCH_ConfigPrm("MountMinecartPlane", true);
      MountMinecartVehicle = new MCH_ConfigPrm("MountMinecartVehicle", false);
      MountMinecartTank = new MCH_ConfigPrm("MountMinecartTank", true);
      AutoThrottleDownHeli = new MCH_ConfigPrm("AutoThrottleDownHeli", true);
      AutoThrottleDownPlane = new MCH_ConfigPrm("AutoThrottleDownPlane", false);
      AutoThrottleDownTank = new MCH_ConfigPrm("AutoThrottleDownTank", false);
      DisableItemRender = new MCH_ConfigPrm("DisableItemRender", 1);
      DisableItemRender.desc = ";DisableItemRender = 0 ~ 3 (1 = Recommended)";
      RenderDistanceWeight = new MCH_ConfigPrm("RenderDistanceWeight", 10.0D);
      MobRenderDistanceWeight = new MCH_ConfigPrm("MobRenderDistanceWeight", 1.0D);
      CreativeTabIcon = new MCH_ConfigPrm("CreativeTabIconItem", "fuel");
      CreativeTabIconHeli = new MCH_ConfigPrm("CreativeTabIconHeli", "ah-64");
      CreativeTabIconPlane = new MCH_ConfigPrm("CreativeTabIconPlane", "f22a");
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
      MouseSensitivity = new MCH_ConfigPrm("MouseSensitivity", 10.0D);
      MouseControlStickModeHeli = new MCH_ConfigPrm("MouseControlStickModeHeli", false);
      MouseControlStickModePlane = new MCH_ConfigPrm("MouseControlStickModePlane", false);
      MouseControlFlightSimMode = new MCH_ConfigPrm("MouseControlFlightSimMode", false);
      MouseControlFlightSimMode.desc = ";MouseControlFlightSimMode = true ( Yaw:key, Roll=mouse )";
      SwitchWeaponWithMouseWheel = new MCH_ConfigPrm("SwitchWeaponWithMouseWheel", true);
      AllHeliSpeed = new MCH_ConfigPrm("AllHeliSpeed", 1.0D);
      AllPlaneSpeed = new MCH_ConfigPrm("AllPlaneSpeed", 1.0D);
      AllTankSpeed = new MCH_ConfigPrm("AllTankSpeed", 1.0D);
      HurtResistantTime = new MCH_ConfigPrm("HurtResistantTime", 0.0D);
      DisplayHUDThirdPerson = new MCH_ConfigPrm("DisplayHUDThirdPerson", false);
      DisableCameraDistChange = new MCH_ConfigPrm("DisableThirdPersonCameraDistChange", false);
      EnableReplaceTextureManager = new MCH_ConfigPrm("EnableReplaceTextureManager", true);
      DisplayEntityMarker = new MCH_ConfigPrm("DisplayEntityMarker", true);
      DisplayMarkThroughWall = new MCH_ConfigPrm("DisplayMarkThroughWall", true);
      EntityMarkerSize = new MCH_ConfigPrm("EntityMarkerSize", 10.0D);
      BlockMarkerSize = new MCH_ConfigPrm("BlockMarkerSize", 10.0D);
      ReplaceRenderViewEntity = new MCH_ConfigPrm("ReplaceRenderViewEntity", true);
      StingerLockRange = new MCH_ConfigPrm("StingerLockRange", 320.0D);
      StingerLockRange.validVer = "1.0.0";
      DefaultExplosionParticle = new MCH_ConfigPrm("DefaultExplosionParticle", false);
      RangeFinderSpotDist = new MCH_ConfigPrm("RangeFinderSpotDist", 400);
      RangeFinderSpotTime = new MCH_ConfigPrm("RangeFinderSpotTime", 15);
      RangeFinderConsume = new MCH_ConfigPrm("RangeFinderConsume", true);
      EnablePutRackInFlying = new MCH_ConfigPrm("EnablePutRackInFlying", true);
      EnableDebugBoundingBox = new MCH_ConfigPrm("EnableDebugBoundingBox", true);
      hitMarkColorAlpha = 1.0F;
      hitMarkColorRGB = 16711680;
      ItemRecipe_Fuel = new MCH_ConfigPrm("ItemRecipe_Fuel", "\"ICI\", \"III\", I, iron_ingot, C, coal");
      ItemRecipe_GLTD = new MCH_ConfigPrm("ItemRecipe_GLTD", "\" B \", \"IDI\", \"IRI\", B, iron_block, I, iron_ingot, D, diamond, R, redstone");
      ItemRecipe_Chain = new MCH_ConfigPrm("ItemRecipe_Chain", "\"I I\", \"III\", \"I I\", I, iron_ingot");
      ItemRecipe_Parachute = new MCH_ConfigPrm("ItemRecipe_Parachute", "\"WWW\", \"S S\", \" W \", W, wool, S, string");
      ItemRecipe_Container = new MCH_ConfigPrm("ItemRecipe_Container", "\"CCI\", C, chest, I, iron_ingot");
      ItemRecipe_UavStation = new MCH_ConfigPrm[]{new MCH_ConfigPrm("ItemRecipe_UavStation", "\"III\", \"IDI\", \"IRI\", I, iron_ingot, D, diamond, R, redstone_block"), new MCH_ConfigPrm("ItemRecipe_UavStation2", "\"IDI\", \"IRI\", I, iron_ingot, D, diamond, R, redstone")};
      ItemRecipe_DraftingTable = new MCH_ConfigPrm("ItemRecipe_DraftingTable", "\"R  \", \"PCP\", \"F F\", R, redstone, C, crafting_table, P, planks, F, fence");
      ItemRecipe_Wrench = new MCH_ConfigPrm("ItemRecipe_Wrench", "\" I \", \" II\", \"I  \", I, iron_ingot");
      ItemRecipe_RangeFinder = new MCH_ConfigPrm("ItemRecipe_RangeFinder", "\"III\", \"RGR\", \"III\", I, iron_ingot, G, glass, R, redstone");
      ItemRecipe_Stinger = new MCH_ConfigPrm("ItemRecipe_Stinger", "\"G  \", \"III\", \"RI \", G, glass, I, iron_ingot, R, redstone");
      ItemRecipe_StingerMissile = new MCH_ConfigPrm("ItemRecipe_StingerMissile", "\"R  \", \" I \", \"  G\", G, gunpowder, I, iron_ingot, R, redstone");
      ItemRecipe_Javelin = new MCH_ConfigPrm("ItemRecipe_Javelin", "\"III\", \"GR \", G, glass, I, iron_ingot, R, redstone");
      ItemRecipe_JavelinMissile = new MCH_ConfigPrm("ItemRecipe_JavelinMissile", "\" R \", \" I \", \" G \", G, gunpowder, I, iron_ingot, R, redstone");
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
      General = new MCH_ConfigPrm[]{TestMode, EnableCommand, null, PlaceableOnSpongeOnly, ItemDamage, ItemFuel, AutoRepairHP, Explosion_DestroyBlock, Explosion_FlamingBlock, BulletBreakableBlock, Collision_DestroyBlock, Collision_Car_BreakableBlock, Collision_Car_BreakableMaterial, Collision_Tank_BreakableBlock, Collision_Tank_BreakableMaterial, Collision_EntityDamage, Collision_EntityTankDamage, InfinityAmmo, InfinityFuel, DismountAll, MountMinecartHeli, MountMinecartPlane, MountMinecartVehicle, MountMinecartTank, PreventingBroken, DropItemInCreativeMode, BreakableOnlyPickaxe, AllHeliSpeed, AllPlaneSpeed, AllTankSpeed, HurtResistantTime, StingerLockRange, RangeFinderSpotDist, RangeFinderSpotTime, RangeFinderConsume, EnablePutRackInFlying, EnableDebugBoundingBox, null, InvertMouse, MouseSensitivity, MouseControlStickModeHeli, MouseControlStickModePlane, MouseControlFlightSimMode, AutoThrottleDownHeli, AutoThrottleDownPlane, AutoThrottleDownTank, SwitchWeaponWithMouseWheel, LWeaponAutoFire, DisableItemRender, HideKeybind, RenderDistanceWeight, MobRenderDistanceWeight, CreativeTabIcon, CreativeTabIconHeli, CreativeTabIconPlane, CreativeTabIconTank, CreativeTabIconVehicle, DisableShader, DefaultExplosionParticle, AliveTimeOfCartridge, HitMarkColor, SmoothShading, EnableModEntityRender, DisableRenderLivingSpecials, DisplayHUDThirdPerson, DisableCameraDistChange, EnableReplaceTextureManager, DisplayEntityMarker, EntityMarkerSize, BlockMarkerSize, ReplaceRenderViewEntity, null, ItemRecipe_Fuel, ItemRecipe_GLTD, ItemRecipe_Chain, ItemRecipe_Parachute, ItemRecipe_Container, ItemRecipe_UavStation[0], ItemRecipe_UavStation[1], ItemRecipe_DraftingTable, ItemRecipe_Wrench, ItemRecipe_RangeFinder, ItemRecipe_Stinger, ItemRecipe_StingerMissile, ItemRecipe_Javelin, ItemRecipe_JavelinMissile};
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
      AllTankSpeed.prmDouble = MCH_Lib.RNG(AllTankSpeed.prmDouble, 0.0D, 1000.0D);
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
      } else if(MobRenderDistanceWeight.prmDouble > 10.0D) {
         MobRenderDistanceWeight.prmDouble = 10.0D;
      }

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
         } else if(target instanceof MCH_EntityVehicle) {
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
            } else if(target instanceof MCH_EntityVehicle) {
               list = DamageVsMCHeliVehicle.list;
            } else if(targetName.indexOf("mcheli.") > 0) {
               list = DamageVsMCHeliOther.list;
            } else if(target instanceof EntityPlayer) {
               list = DamageVsPlayer.list;
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
               DebugLog = true;
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
      if(DebugLog) {
         pw.println("McHeliOutputDebugLog");
         pw.println();
      }

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
