package mcheli.aircraft;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.*;

import mcheli.*;
import mcheli.chain.MCH_EntityChain;
import mcheli.command.MCH_Command;
import mcheli.flare.MCH_APS;
import mcheli.flare.MCH_Chaff;
import mcheli.flare.MCH_Flare;
import mcheli.flare.MCH_Maintenance;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.item.MCH_ItemInfo;
import mcheli.item.MCH_ItemInfoManager;
import mcheli.lod.MCH_VehicleLODVisibility;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.parachute.MCH_EntityParachute;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneChaseCamera;
import mcheli.ship.MCH_EntityShip;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_UavInventory;
import mcheli.uav.MCH_UavJsonStore;
import mcheli.uav.MCH_UavRegistry;
import mcheli.weapon.*;
import mcheli.wrapper.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;

import mcheli.light.BlockLight;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.oredict.OreDictionary;

import static mcheli.hud.MCH_HudItem.player;
//import static net.minecraft.command.CommandBase.getCommandSenderAsPlayer;
//import static net.minecraft.command.CommandBase.getPlayer;

import net.minecraft.world.EnumSkyBlock;


import mcheli.mob.MCH_EntityGunner;
import org.lwjgl.Sys;


/** Shared controllable vehicle entity base used by aircraft, ground vehicles, ships, and turrets. */

public abstract class MCH_EntityBaseVehicle extends W_EntityContainer implements MCH_IEntityLockChecker, MCH_IEntityCanRideBaseVehicle, IEntityAdditionalSpawnData {
   private static final Map<UUID, NewUavSafeReturn> NEW_UAV_SAFE_RETURNS = new HashMap<UUID, NewUavSafeReturn>();
   private final MCH_VehicleBoxCache vehicleBoxCache = new MCH_VehicleBoxCache();
   private static final int NEW_UAV_SAFE_RETURN_MIN_TICKS = 20;
   private static MCH_EntityBaseVehicle aircraft;
    private ForgeChunkManager.Ticket chunkTicket;
   private ForgeChunkManager.Ticket newUavStationChunkTicket;
   private ChunkCoordIntPair newUavForcedStationChunk;
   //MCH_EntityBaseVehicle ac = null;
   private static final int DATAWT_ID_DAMAGE = 19;
   private static final int DATAWT_ID_TYPE = 20;
   private static final int DATAWT_ID_TEXTURE_NAME = 21;
   private static final int DATAWT_ID_UAV_STATION = 22;
   private static final int DATAWT_ID_STATUS = 23;
   private static final int CMN_ID_FLARE = 0;
   private static final int CMN_ID_FREE_LOOK = 1;
   private static final int CMN_ID_RELOADING = 2;
   private static final int CMN_ID_INGINITY_AMMO = 3;
   private static final int CMN_ID_INGINITY_FUEL = 4;
   private static final int CMN_ID_RAPELLING = 5;
   private static final int CMN_ID_SEARCHLIGHT = 6;
   private static final int CMN_ID_CNTRL_LEFT = 7;
   private static final int CMN_ID_CNTRL_RIGHT = 8;
   private static final int CMN_ID_CNTRL_UP = 9;
   private static final int CMN_ID_CNTRL_DOWN = 10;
   private static final int CMN_ID_CNTRL_BRAKE = 11;
   /** Vehicle access lock occupies bit 12 in the shared status watcher. */
   private static final int CMN_ID_VEHICLE_ACCESS_LOCK = 12;
   private static final int CMN_ID_ACTIVE_RADAR = 13;
   private static final int DATAWT_ID_USE_WEAPON = 24;
   private static final int DATAWT_ID_FUEL = 25;
   private static final int DATAWT_ID_ROT_ROLL = 26;
   private static final int DATAWT_ID_COMMAND = 27;
   private static final int DATAWT_ID_THROTTLE = 29;
   protected static final int DATAWT_ID_FOLD_STAT = 30;
   protected static final int DATAWT_ID_PART_STAT = 31;
   protected static final int PART_ID_CANOPY = 0;
   protected static final int PART_ID_NOZZLE = 1;
   protected static final int PART_ID_LANDINGGEAR = 2;
   protected static final int PART_ID_WING = 3;
   protected static final int PART_ID_HATCH = 4;
   public static final byte LIMIT_GROUND_PITCH = 40;
   private double groundVehicleFallStartY = Double.MAX_VALUE;
   private double groundVehicleMaxFallSpeed = 0.0D;
   private double groundVehicleMaxDownwardAcceleration = 0.0D;
   public static final byte LIMIT_GROUND_ROLL = 40;
   public boolean isRequestedSyncStatus = false;
   private MCH_BaseVehicleInfo acInfo;
   private int commonStatus;
   private Entity[] partEntities;
   private MCH_EntityHitBox pilotSeat;
   private MCH_EntitySeat[] seats;
   private MCH_SeatInfo[] seatsInfo;
   private String commonUniqueId;
   private int seatSearchCount;
   protected double velocityX;
   protected double velocityY;
   protected double velocityZ;
   public boolean keepOnRideRotation;
   protected int aircraftPosRotInc;
   protected double aircraftX;
   protected double aircraftY;
   protected double aircraftZ;
   protected double aircraftYaw;
   protected double aircraftPitch;
   public boolean aircraftRollRev;
   public boolean aircraftRotChanged;
   public float rotationRoll;
   public float prevRotationRoll;
   private double currentThrottle;
   private double prevCurrentThrottle;
   public double currentSpeed;
   public int currentFuel;
   /** Fuel held for finite gas-pump service, separate from propulsion fuel. */
   private long serviceFuel;
   public float throttleBack = 0.0F;
   public double beforeHoverThrottle;
   public int waitMountEntity = 0;
   public boolean throttleUp = false;
   public boolean throttleDown = false;
   public boolean moveLeft = false;
   public boolean moveRight = false;
   public MCH_LowPassFilterFloat lowPassPartialTicks;
   private MCH_Radar entityRadar;
   private int radarRotate;
   private Boolean pendingRadarActive;
   private MCH_Flare flareDv;
   private int currentFlareIndex;
   public MCH_WeaponSet[] weapons;
   protected int[] currentWeaponID;
   public float lastRiderYaw;
   public float prevLastRiderYaw;
   public float lastRiderPitch;
   public float prevLastRiderPitch;
   public boolean isRenderingLOD;
   protected MCH_WeaponSet dummyWeapon;
   protected int useWeaponStat;
   protected int hitStatus;
   protected final MCH_IEntitySoundUpdater soundUpdater;
   protected Entity lastRiddenByEntity;
   protected Entity lastRidingEntity;
   public List listUnmountReserve = new ArrayList();
   private int countOnUpdate;
   private MCH_EntityChain towChainEntity;
   private MCH_EntityChain towedChainEntity;
   public MCH_Camera camera;
   private int cameraId;
   protected boolean isGunnerMode = false;
   protected boolean isGunnerModeOtherSeat = false;
   private boolean isHoveringMode = false;
   public static final int CAMERA_PITCH_MIN = -30;
   public static final int CAMERA_PITCH_MAX = 70;
   private MCH_EntityTvMissile TVmissile;
   protected boolean isGunnerFreeLookMode = false;
   public final MCH_MissileDetector missileDetector;
   public int serverNoMoveCount = 0;
   public int repairCount;
   public int beforeDamageTaken;
   public int timeSinceHit;
   private int despawnCount;
   public float rotDestroyedYaw;
   public float rotDestroyedPitch;
   public float rotDestroyedRoll;
   public int damageSinceDestroyed;
   public boolean isFirstDamageSmoke = true;
   public Vec3[] prevDamageSmokePos = new Vec3[0];
   private MCH_EntityUavStation uavStation;
   public boolean cs_dismountAll;
   public boolean cs_heliAutoThrottleDown;
   public boolean cs_planeAutoThrottleDown;
   public boolean cs_shipAutoThrottleDown;
   public boolean cs_tankAutoThrottleDown;
   public MCH_Parts partHatch;
   public MCH_Parts partCanopy;
   public MCH_Parts partLandingGear;
   public double prevRidingEntityPosX;
   public double prevRidingEntityPosY;
   public double prevRidingEntityPosZ;
   public boolean canRideRackStatus;
   private int modeSwitchCooldown;
   public Vec3 target = Vec3.createVectorHelper(0, 0, 0);
   public MCH_BoundingBox[] extraBoundingBox;
   //public wheelBoundingBox[] extrawheelboundingbox;
   public float lastBBDamageFactor;
   public EnumBoundingBoxType lastHitBoundingBoxType;
   private final MCH_BaseVehicleInventory inventory;
   private double fuelConsumption;
   private int fuelSuppliedCount;
   private int supplyAmmoWait;
   private boolean beforeSupplyAmmo;
   public MCH_EntityBaseVehicle.WeaponBay[] weaponBays;
   public float[] rotPartRotation;
   public float[] prevRotPartRotation;
   public float[] rotCrawlerTrack = new float[2];
   public float[] prevRotCrawlerTrack = new float[2];
   public float[] throttleCrawlerTrack = new float[2];
   public float[] rotTrackRoller = new float[2];
   public float[] prevRotTrackRoller = new float[2];
   public float rotWheel = 0.0F;
   public float prevRotWheel = 0.0F;
   public float rotYawWheel = 0.0F;
   public float prevRotYawWheel = 0.0F;
   private boolean partAnimationPositionInitialized;
   private double lastPartAnimationPosX;
   private double lastPartAnimationPosZ;
   private float lastPartAnimationYaw;
   private double partAnimationForwardTravel;
   private double partAnimationYawTravel;
   private boolean isParachuting;
   public float ropesLength = 0.0F;
   private MCH_Queue prevPosition;
   private int tickRepelling;
   private int lastUsedRopeIndex;
   private boolean dismountedUserCtrl;
   private boolean placementMotionLocked;
   public float lastSearchLightYaw;
   public float lastSearchLightPitch;
   public float rotLightHatch = 0.0F;
   public float prevRotLightHatch = 0.0F;
   public int recoilCount = 0;
   public float recoilYaw = 0.0F;
   public float recoilValue = 0.0F;
   public int brightnessHigh = 240;
   public int brightnessLow = 240;
   public final HashMap noCollisionEntities = new HashMap();
   private MCH_EntityBaseVehicle rackMountParent;
   private boolean rackThrottleInput;
   private String pendingRackParentUniqueId = "";
   private int pendingRackSeatId = -1;
   private double pendingRackPosX;
   private double pendingRackPosY;
   private double pendingRackPosZ;
   private int pendingRackRestoreTicks;
   private int rackLaunchAssistTicks;
   private double rackLaunchVelocityX;
   private double rackLaunchVelocityZ;
   private double lastCalcLandInDistanceCount;
   private double lastLandInDistance;
   public float thirdPersonDist = 4.0F;
   public Entity lastAttackedEntity = null;
   private static final MCH_EntitySeat[] seatsDummy = new MCH_EntitySeat[0];
   private int delayedUavInventoryTicks;
   private UUID uavPersistentUUID;
   private UUID uavOwnerUUID;
   private UUID vehicleOwnerUUID;
   private UUID linkedUavStationUUID;
   private int linkedUavStationDimension;
   private double linkedUavStationX;
   private double linkedUavStationY;
   private double linkedUavStationZ;
   //public static boolean isNewUAV = MCH_BaseVehicleInfo.isNewUAV;
   //public static Entity rider = lastRidingEntity;
   //MCH_EntityBaseVehicle MCH_EntityUavStation;

   private boolean switchSeat = false;
   //public EntityPlayerMP playerEntity = (EntityPlayerMP) getCommandSenderAsPlayer(player);\

   public MCH_Chaff chaff;
   public MCH_Maintenance maintenance;
   public MCH_APS aps;

   private boolean hasLinkedUavStationPosition;
   private int newUavMountSyncTicks;
   private boolean newUavShiftExitInProgress;

   private final Set<ChunkCoordinates> activeLights = new HashSet<>();



   public MCH_EntityBaseVehicle(World world) {
      super(world);
      this.commonStatus = 0;
      this.entityRadar = new MCH_Radar(world);
      this.radarRotate = 0;
      this.pendingRadarActive = null;
      this.setAcInfo(null);
      super.dropContentsWhenDead = false;
      super.ignoreFrustumCheck = true;
      // Normal vehicles must follow Forge's watched-chunk spawn lifecycle.
      super.forceSpawn = false;
      this.flareDv = new MCH_Flare(world, this);
      this.chaff = new MCH_Chaff(world, this);
      this.maintenance = new MCH_Maintenance(world, this);
      this.aps = new MCH_APS(world, this);
      this.currentFlareIndex = 0;
      this.currentWeaponID = new int[0];
      //this.aircraftPosRotInc = 0;
      this.aircraftX = 0.0D;
      this.aircraftY = 0.0D;
      this.aircraftZ = 0.0D;
      //this.aircraftYaw = 0.0D;
      this.aircraftPitch = 0.0D;
      this.currentSpeed = 0.0D;
      this.setCurrentThrottle(0.0D);
      this.currentFuel = 0;
      this.cs_dismountAll = false;
      this.cs_heliAutoThrottleDown = true;
      this.cs_planeAutoThrottleDown = false;
      this.cs_shipAutoThrottleDown = true;
      MCH_Config var10001 = MCH_MOD.config;
      super.renderDistanceWeight = MCH_Config.RenderDistanceWeight.prmDouble;
      //todo test change this
      this.setCommonUniqueId("");
      this.seatSearchCount = 0;
      this.seatsInfo = null;
      this.seats = new MCH_EntitySeat[0];
      this.pilotSeat = new MCH_EntityHitBox(world, this, 1.0F, 1.0F);
      this.pilotSeat.parent = this;
      this.partEntities = new Entity[]{this.pilotSeat};
      this.setTextureName("");
      this.camera = new MCH_Camera(world, this, super.posX, super.posY, super.posZ);
      this.setCameraId(0);
      this.lastRiddenByEntity = null;
      this.lastRidingEntity = null;
      this.soundUpdater = MCH_MOD.proxy.CreateSoundUpdater(this);
      this.countOnUpdate = 0;
      this.setTowChainEntity((MCH_EntityChain)null);
      this.dummyWeapon = new MCH_WeaponSet(new MCH_WeaponDummy(super.worldObj, Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, "", (MCH_WeaponInfo)null));
      this.useWeaponStat = 0;
      this.hitStatus = 0;
      this.repairCount = 0;
      this.beforeDamageTaken = 0;
      this.timeSinceHit = 0;
      this.setDespawnCount(0);
      this.missileDetector = new MCH_MissileDetector(this, world);
      this.uavStation = null;
      this.delayedUavInventoryTicks = 0;
      this.uavPersistentUUID = null;
      this.uavOwnerUUID = null;
      this.linkedUavStationUUID = null;
      this.linkedUavStationDimension = 0;
      this.linkedUavStationX = 0.0D;
      this.linkedUavStationY = 0.0D;
      this.linkedUavStationZ = 0.0D;
      this.hasLinkedUavStationPosition = false;
      this.newUavMountSyncTicks = 0;
      this.newUavShiftExitInProgress = false;
      this.modeSwitchCooldown = 0;
      this.partHatch = null;
      this.partCanopy = null;
      this.partLandingGear = null;
      this.weaponBays = new MCH_EntityBaseVehicle.WeaponBay[0];
      this.rotPartRotation = new float[0];
      this.prevRotPartRotation = new float[0];
      this.lastRiderYaw = 0.0F;
      this.prevLastRiderYaw = 0.0F;
      this.lastRiderPitch = 0.0F;
      this.prevLastRiderPitch = 0.0F;
      this.isRenderingLOD = false;
      this.rotationRoll = 0.0F;
      this.prevRotationRoll = 0.0F;
      this.lowPassPartialTicks = new MCH_LowPassFilterFloat(10);
      this.extraBoundingBox = new MCH_BoundingBox[0];
      //this.extrawheelboundingbox = new wheelBoundingBox[0];
      W_Reflection.setBoundingBox(this, new MCH_BaseVehicleBoundingBox(this));
      this.lastBBDamageFactor = 1.0F;
      this.lastHitBoundingBoxType = EnumBoundingBoxType.DEFAULT;
      this.inventory = new MCH_BaseVehicleInventory(this);
      this.fuelConsumption = 0.0D;
      this.fuelSuppliedCount = 0;
      this.canRideRackStatus = false;
      this.isParachuting = false;
      this.placementMotionLocked = false;
      this.prevPosition = new MCH_Queue(10, Vec3.createVectorHelper(0.0D, 0.0D, 0.0D));
      this.lastSearchLightYaw = this.lastSearchLightPitch = 0.0F;
   }

   public boolean isInRangeToRenderDist(double distanceSq) {
      boolean lodEnabled = MCH_Config.EnableAircraftLODRender != null && MCH_Config.EnableAircraftLODRender.prmBool;
      double farDistance = MCH_Config.AircraftLODFarDistance != null
         ? MCH_Config.AircraftLODFarDistance.prmDouble : MCH_VehicleLODVisibility.MAX_LOD_DISTANCE;
      return MCH_VehicleLODVisibility.isTrackedEntityRenderEligible(
         lodEnabled, distanceSq, farDistance, super.isInRangeToRenderDist(distanceSq));
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataWatcher().addObject(20, "");
      this.getDataWatcher().addObject(19, new Integer(0));
      this.getDataWatcher().addObject(23, new Integer(0));
      this.getDataWatcher().addObject(24, new Integer(0));
      this.getDataWatcher().addObject(25, new Integer(0));
      this.getDataWatcher().addObject(21, "");
      this.getDataWatcher().addObject(22, new Integer(0));
      this.getDataWatcher().addObject(26, new Short((short)0));
      this.getDataWatcher().addObject(27, new String(""));
      this.getDataWatcher().addObject(28, Integer.valueOf(0));
      this.getDataWatcher().addObject(29, new Integer(0));
      this.getDataWatcher().addObject(31, new Integer(0));
      if(!super.worldObj.isRemote) {
         MCH_Config var10002 = MCH_MOD.config;
         this.setCommonStatus(3, MCH_Config.InfinityAmmo.prmBool);
         var10002 = MCH_MOD.config;
         this.setCommonStatus(4, MCH_Config.InfinityFuel.prmBool);
         setGunnerStatus(true);
         //if (isUAV());
      }

      this.getEntityData().setString("EntityType", this.getEntityType());
   }

   public float getServerRoll() {
      return (float)this.getDataWatcher().getWatchableObjectShort(26);
   }

   public float getRotYaw() {
      return super.rotationYaw;
   }

   public float getRotPitch() {
      return super.rotationPitch;
   }

   public float getRotRoll() {
      return this.rotationRoll;
   }

   public void setRotYaw(float f) {
      super.rotationYaw = f;
   }

   public void setRotPitch(float f) {
      super.rotationPitch = f;
   }

   public void setRotPitch(float f, String msg) {
      this.setRotPitch(f);
   }

   public void setRotRoll(float f) {
      this.rotationRoll = f;
   }

   public void applyOnGroundPitch(float factor) {
      if(this.getAcInfo() != null) {
         float ogp = this.getAcInfo().onGroundPitch;
         float pitch = this.getRotPitch();
         pitch -= ogp;
         pitch *= factor;
         pitch += ogp;
         this.setRotPitch(pitch, "applyOnGroundPitch");
      }

      this.setRotRoll(this.getRotRoll() * factor);
   }

   public float calcRotYaw(float partialTicks) {
      float prevYaw = super.prevRotationYaw;
      float currentYaw = this.getRotYaw();

      // Normalize the angles to avoid interpolation issues across the 360° boundary
      while (currentYaw - prevYaw < -180.0F) {
         currentYaw += 360.0F;
      }
      while (currentYaw - prevYaw >= 180.0F) {
         currentYaw -= 360.0F;
      }

      return prevYaw + (currentYaw - prevYaw) * partialTicks;
   }

   public float calcRotPitch(float partialTicks) {
      return super.prevRotationPitch + (this.getRotPitch() - super.prevRotationPitch) * partialTicks;
   }

   public float calcRotRoll(float partialTicks) {
      return this.prevRotationRoll + (this.getRotRoll() - this.prevRotationRoll) * partialTicks;
   }

   protected void setRotation(float y, float p) {
      this.setRotYaw(y % 360.0F);
      this.setRotPitch(p % 360.0F);
   }

   public boolean isInfinityAmmo(Entity player) {
      return this.isCreative(player) || this.getCommonStatus(3);
   }

   public boolean isInfinityFuel(Entity player, boolean checkOtherSeet) {
      if(!this.isCreative(player) && !this.getCommonStatus(4)) {
         if(checkOtherSeet) {
            MCH_EntitySeat[] arr$ = this.getSeats();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MCH_EntitySeat seat = arr$[i$];
               if(seat != null && this.isCreative(seat.riddenByEntity)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public void setCommand(String s, EntityPlayer player) {
      if(!super.worldObj.isRemote && MCH_Command.canUseCommand(player)) {
         this.setCommandForce(s);
      }

   }

   public void setCommandForce(String s) {
      if(!super.worldObj.isRemote) {
         this.getDataWatcher().updateObject(27, s);
      }

   }

   public String getCommand() {
      return this.getDataWatcher().getWatchableObjectString(27);
   }

   public String getKindName() {
      return "";
   }

   public String getEntityType() {
      return "";
   }

   public void setTypeName(String s) {
      String beforeType = this.getTypeName();
      if(s != null && !s.isEmpty() && s.compareTo(beforeType) != 0) {
         this.getDataWatcher().updateObject(20, String.valueOf(s));
         this.changeType(s);
         this.initRotationYaw(this.getRotYaw());
      }

   }

   public String getTypeName() {
      return this.getDataWatcher().getWatchableObjectString(20);
   }

   public abstract void changeType(String var1);

   public boolean isTargetDrone() {
      return this.getAcInfo() != null && this.getAcInfo().isTargetDrone;
   }

   public boolean isUAV() {
      return this.getAcInfo() != null && this.getAcInfo().isUAV;
   }

   public boolean isNewUAV() {
      return this.getAcInfo() != null && this.getAcInfo().isNewUAV;
   }

   public boolean isSmallUAV() {
      return this.getAcInfo() != null && this.getAcInfo().isSmallUAV;
   }

   public boolean isAlwaysCameraView() {
      return this.getAcInfo() != null && this.getAcInfo().alwaysCameraView;
   }

   private void saveUavStationPosition(MCH_EntityUavStation station) {
      this.linkedUavStationUUID = station.getUniqueID();
      this.linkedUavStationDimension = station.dimension;
      this.linkedUavStationX = station.posX;
      this.linkedUavStationY = station.posY;
      this.linkedUavStationZ = station.posZ;
      this.hasLinkedUavStationPosition = true;
   }

   public void setUavStation(MCH_EntityUavStation uavSt) {
      this.uavStation = uavSt;
      if(uavSt != null) {
         saveUavStationPosition(uavSt);
         if(uavSt.getOwnerUUID() != null) {
            this.setOwnerUUID(uavSt.getOwnerUUID());
         }
      }
      if(!super.worldObj.isRemote) {
         if(uavSt != null) {
            this.getDataWatcher().updateObject(22, Integer.valueOf(W_Entity.getEntityId(uavSt)));
            MCH_UavRegistry.register(this);
         } else {
            this.getDataWatcher().updateObject(22, Integer.valueOf(0));
         }
      }

   }

   public void unlinkUavStation() {
      this.uavStation = null;
      this.linkedUavStationUUID = null;
      this.linkedUavStationDimension = 0;
      this.linkedUavStationX = 0.0D;
      this.linkedUavStationY = 0.0D;
      this.linkedUavStationZ = 0.0D;
      this.hasLinkedUavStationPosition = false;
      if(!super.worldObj.isRemote) {
         this.getDataWatcher().updateObject(22, Integer.valueOf(0));
      }
   }

   public UUID getUavPersistentUUID() {
      return getUavPersistentUUID(true);
   }

   public UUID getUavPersistentUUID(boolean createIfMissing) {
      if(this.uavPersistentUUID == null && createIfMissing && (this.isUAV() || this.isNewUAV())) {
         this.uavPersistentUUID = this.getUniqueID();
      }
      return this.uavPersistentUUID;
   }

   public UUID getOwnerUUID() {
      return this.uavOwnerUUID;
   }

   public void setOwnerUUID(UUID uuid) {
      if(!super.worldObj.isRemote && this.uavOwnerUUID != null && !this.uavOwnerUUID.equals(uuid)) {
         MCH_UavRegistry.unregister(this);
      }
      this.uavOwnerUUID = uuid;
      if(uuid != null && !super.worldObj.isRemote) {
         MCH_UavRegistry.register(this);
      }
   }

   public UUID getLinkedUavStationUUID() {
      return this.linkedUavStationUUID;
   }

   public boolean isLinkedToStation(MCH_EntityUavStation station) {
      return station != null && this.linkedUavStationUUID != null && this.linkedUavStationUUID.equals(station.getUniqueID());
   }

   public float getStealth() {
      return this.getAcInfo() != null?this.getAcInfo().stealth:0.0F;
   }

   public MCH_BaseVehicleInventory getGuiInventory() {
      return this.inventory;
   }

   public void openGui(EntityPlayer player) {
      if(!super.worldObj.isRemote) {
         player.openGui(MCH_MOD.instance, 1, super.worldObj, (int)super.posX, (int)super.posY, (int)super.posZ);
      }

   }

   public MCH_EntityUavStation getUavStation() {
      return (isUAV() || isNewUAV()) ? this.uavStation : null;
      //todone store uav pos, not here! in updatecontrol.
         }

   public static MCH_EntityBaseVehicle getAircraft_RiddenOrControl(Entity rider) {
      if(rider != null) {
         if(rider.ridingEntity instanceof MCH_EntityBaseVehicle) {
            return (MCH_EntityBaseVehicle)rider.ridingEntity;
         }

         if(rider.ridingEntity instanceof MCH_EntitySeat) {
            return ((MCH_EntitySeat)rider.ridingEntity).getParent();
         }

         if(rider.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation uavStation = (MCH_EntityUavStation)rider.ridingEntity;
            return uavStation.getControlAircract();
         }
      }

      return null;
   }

   public boolean isCreative(Entity entity) {
      if(entity instanceof EntityPlayer && ((EntityPlayer)entity).capabilities.isCreativeMode)
         return true;
      if(entity instanceof MCH_EntityGunner && ((MCH_EntityGunner)entity).isCreative)
         return true;
      return false;
   }

   public Entity getRiddenByEntity() {
      // NewUAVs are directly ridden. Many content definitions also retain the legacy UAV
      // flag, so NewUAV must take precedence or the direct rider is hidden as soon as the
      // player leaves the station and the normal update loop treats that as a dismount.
      if(this.isNewUAV()) {
         return super.riddenByEntity;
      }
      return this.isUAV() && this.uavStation != null ? this.uavStation.riddenByEntity : super.riddenByEntity;
   }

   public boolean mountNewUavPilot(EntityPlayerMP player, MCH_EntityUavStation station) {
      if(super.worldObj.isRemote || player == null || station == null || !this.isNewUAV()
            || this.isDead || this.isDestroyed() || station.isDead
            || player.ridingEntity != station || !this.isLinkedToStation(station)) {
         return false;
      }
      if(super.riddenByEntity != null && super.riddenByEntity != player) {
         return false;
      }

      this.setUavStation(station);
      this.lastRiddenByEntity = null;
      if(!station.detachRiderForNewUavControl(player)) {
         return false;
      }
      // Move the server-authoritative player into the pinned UAV chunk before applying
      // the direct mount. This prevents UAV controls from becoming active while the
      // player's physical position and chunk tracking are still at the station.
      player.setPositionAndUpdate(this.posX, this.posY + this.getMountedYOffset(), this.posZ);
      this.clearPlacementMotionLock();
      player.mountEntity(this);
      if(player.ridingEntity == this && super.riddenByEntity == player) {
         this.lastRiddenByEntity = player;
         this.newUavMountSyncTicks = 40;
         player.fallDistance = 0.0F;
         this.syncCompleteAircraftState(player);
         return true;
      }

      // A failed cross-chunk handoff must leave the operator at the station rather than
      // detached at the aircraft. The station can retry after entity synchronization.
      if(player.ridingEntity == null) {
         player.mountEntity(station);
      }
      return false;
   }

   public boolean getCommonStatus(int bit) {
      return (this.getSynchronizedCommonStatus() >> bit & 1) != 0;
   }

   /**
    * The server owns commonStatus. On clients the data watcher is the synchronized
    * value, while commonStatus is only a once-per-tick cache and can be stale while
    * a HUD is being rendered between entity updates.
    */
   private int getSynchronizedCommonStatus() {
      if(super.worldObj != null && super.worldObj.isRemote && this.getDataWatcher() != null) {
         return this.getDataWatcher().getWatchableObjectInt(DATAWT_ID_STATUS);
      }
      return this.commonStatus;
   }

   public UUID getVehicleOwnerUUID() {
      return this.vehicleOwnerUUID;
   }

   public void setVehicleOwnerUUID(UUID ownerUUID) {
      this.vehicleOwnerUUID = ownerUUID;
   }

   public boolean isVehicleAccessLocked() {
      return this.getCommonStatus(CMN_ID_VEHICLE_ACCESS_LOCK);
   }

   public void setVehicleAccessLocked(boolean locked) {
      this.setCommonStatus(CMN_ID_VEHICLE_ACCESS_LOCK, locked);
   }

   private boolean isVehicleAccessOperator(EntityPlayer player) {
      return player != null && (new net.minecraft.command.CommandGameMode()).canCommandSenderUseCommand(player);
   }

   public boolean canPlayerEnterVehicle(EntityPlayer player) {
      return player != null && (!this.isVehicleAccessLocked()
              || player.getUniqueID().equals(this.vehicleOwnerUUID)
              || this.isVehicleAccessOperator(player));
   }

   private void notifyVehicleAccessDenied(EntityPlayer player) {
      if(!super.worldObj.isRemote) {
         player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "This vehicle is locked."));
      }
   }

   public void requestVehicleAccessLockToggle(EntityPlayer player) {
      if(super.worldObj.isRemote || player == null || this.isDead || this.isDestroyed()
              || this.getRiddenByEntity() != player || player.ridingEntity != this) {
         return;
      }
      if(this.vehicleOwnerUUID == null) {
         this.vehicleOwnerUUID = player.getUniqueID();
      }
      if(!player.getUniqueID().equals(this.vehicleOwnerUUID) && !this.isVehicleAccessOperator(player)) {
         player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "You do not own this vehicle."));
         return;
      }
      this.setVehicleAccessLocked(!this.isVehicleAccessLocked());
      player.addChatMessage(new ChatComponentText(this.isVehicleAccessLocked() ? "Vehicle locked." : "Vehicle unlocked."));
      W_WorldFunc.DEF_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "random.click", 1.0F, 1.0F);
   }

   public void setCommonStatus(int bit, boolean b) {
      this.setCommonStatus(bit, b, false);
   }

   public void setCommonStatus(int bit, boolean b, boolean writeClient) {
      if(!super.worldObj.isRemote || writeClient) {
         // Client-predicted bits (for example free look) must start from the latest
         // watched value. Starting from the tick cache can write an old radar bit
         // back into ID 23 after the server's radar update has arrived.
         int bofore = this.getSynchronizedCommonStatus();
         int updatedStatus = bofore;
         int mask = 1 << bit;
         if(b) {
            updatedStatus |= mask;
         } else {
            updatedStatus &= ~mask;
         }

         this.commonStatus = updatedStatus;
         if(bofore != updatedStatus) {
            this.getDataWatcher().updateObject(DATAWT_ID_STATUS, Integer.valueOf(updatedStatus));
         }
      }

   }

   public double getThrottle() {
      return 0.05D * (double)this.getDataWatcher().getWatchableObjectInt(29);
   }

   public void setThrottle(double t) {
      int n = (int)(t * 20.0D);
      if(n == 0 && t > 0.0D) {
         n = 1;
      }

      this.getDataWatcher().updateObject(29, Integer.valueOf(n));
   }

   public int getMaxHP() {
      return this.getAcInfo() != null?this.getAcInfo().maxHp:100;
   }

   public int getMaxWheelDamage() {
      return this.getAcInfo() != null?this.getAcInfo().maxWheelDMG:20;
   }


   public int getHP() {
      return Math.max(this.getMaxHP() - this.getDamageTaken(), 0);
   }

   public void setDamageTaken(int par1) {
      if(par1 < 0) {
         par1 = 0;
      }

      //if(this instanceof wheelBoundingBox) {
      //   causewheeldamage;
      //}

      //incompatible types

      if(par1 > this.getMaxHP()) {
         par1 = this.getMaxHP();
      }

      this.getDataWatcher().updateObject(19, Integer.valueOf(par1));
   }

   // TileEntitySearchlight.java
   public class TileEntitySearchlight extends TileEntity {
      private int ownerEntityId = -1;
      private long lastSeen = 0L;
      // small threshold (in ticks) - tune as needed (1-5)
      private static final long MAX_TICK_GAP = 5L;

      public void setOwner(int entityId) {
         this.ownerEntityId = entityId;
         this.lastSeen = worldObj.getTotalWorldTime();
         markDirty();
      }

      public void refreshLastSeen() {
         this.lastSeen = worldObj.getTotalWorldTime();
         markDirty();
      }

      @Override
      public void updateEntity() {
         if (worldObj.isRemote) return;

         long now = worldObj.getTotalWorldTime();

         // If owner exists and alive, keep the block
         if (ownerEntityId != -1) {
            Entity owner = worldObj.getEntityByID(ownerEntityId);
            if (owner != null && !owner.isDead) {
               // owner alive: nothing to do
               return;
            }
         }

         // If owner not present or dead, remove after grace period
         if (now - lastSeen > MAX_TICK_GAP) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
         }
      }

      @Override
      public void readFromNBT(NBTTagCompound nbt) {
         super.readFromNBT(nbt);
         ownerEntityId = nbt.getInteger("OwnerId");
         lastSeen = nbt.getLong("LastSeen");
      }

      @Override
      public void writeToNBT(NBTTagCompound nbt) {
         super.writeToNBT(nbt);
         nbt.setInteger("OwnerId", ownerEntityId);
         nbt.setLong("LastSeen", lastSeen);
      }
   }

   // BlockSearchlight.java
   public class BlockSearchlight extends BlockContainer {
      public BlockSearchlight() {
         super(Material.glass); // choose material you want
         setTickRandomly(true);
      }

      @Override
      public TileEntity createNewTileEntity(World world, int meta) {
         return new TileEntitySearchlight();
      }

      @Override
      public void breakBlock(World world, int x, int y, int z, Block oldBlock, int oldMeta) {
         // remove TE properly
         world.removeTileEntity(x, y, z);
         super.breakBlock(world, x, y, z, oldBlock, oldMeta);
      }
   }



   public int getDamageTaken() {
      return this.getDataWatcher().getWatchableObjectInt(19);
   }

   public int getWheelDamageTaken() {
      return this.getDataWatcher().getWatchableObjectInt(19);
   }

   public void destroyAircraft() {
      //this.clearSearchlightBlocks();
      this.hardClearSearchlights();
      this.spawndropitems();
      this.setSearchLight(false);
      this.switchHoveringMode(false);
      this.switchGunnerMode(false);

      for(int entity = 0; entity < this.getSeatNum() + 1; ++entity) {
         Entity e = this.getEntityBySeatId(entity);
         if(e instanceof EntityPlayer) {
            this.switchCameraMode((EntityPlayer)e, 0);
         }
      }

      if (isTargetDrone()) {
         setDespawnCount(20 * MCH_Config.DespawnCount.prmInt / 10);
      } else {
         setDespawnCount(20 * MCH_Config.DespawnCount.prmInt);
      }

      if(!super.worldObj.isRemote && this.isNewUAV()) {
         notifyLinkedStationNewUavRemoved();
      }

      this.rotDestroyedPitch = super.rand.nextFloat() - 0.5F;
      this.rotDestroyedRoll = (super.rand.nextFloat() - 0.5F) * 0.5F;
      this.rotDestroyedYaw = 0.0F;



      Entity rider = this.getRiddenByEntity();
      if(rider != null) {
         // NewUAV may also carry the legacy UAV flag, so always handle it first.
         if(this.isNewUAV()) {
            if(!super.worldObj.isRemote) {
               this.returnNewUavPilotToStation(rider, "uav_destroyed");
            }
            if(rider instanceof EntityPlayer) {
               EntityPlayer player = (EntityPlayer)rider;
               player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Drone destroyed!"));
               player.addPotionEffect(new PotionEffect(11, 20, 50));
               player.addPotionEffect(new PotionEffect(12, 20, 0));
            }
         } else if(this.isUAV()) {
            rider.mountEntity((Entity)null);
         }
      }


      if(!super.worldObj.isRemote) {
         this.ejectSeat(this.getRiddenByEntity());
         Entity var3 = this.getEntityBySeatId(1);
         if(var3 != null) {
            this.ejectSeat(var3);
         }


         //float dmg = MCH_Config.KillPassengersWhenDestroyed.prmBool ? 100000.0F : 0.001F;
         //DamageSource damageSource = DamageSource.generic; // Default damage source isgeneric
         //if (this.worldObj.difficultySetting.getDifficultyId() == 0) {
         //   // If the last attacker of this entity is a player, creates a player-based damage source
         //   if (this.lastAttackedEntity instanceof EntityPlayer) {
         //      damageSource = DamageSource.causePlayerDamage((EntityPlayer) this.lastAttackedEntity);
         //   }
         //} else {
         //   // If world difficulty is not Peaceful, creates an explosion-based damage source
         //   damageSource = DamageSource.setExplosionSource(new Explosion(this.worldObj, this.lastAttackedEntity,
         //           this.posX, this.posY, this.posZ, 1.0F));
         //}
         //// If the current entity exists, applies damage
         //if (this.riddenByEntity != null) {
         //   this.riddenByEntity.attackEntityFrom(damageSource, dmg);
         //}
         //// Iterates all seat entities and applies damage if a seat is occupied
         //for (MCH_EntitySeat seat : getSeats()) {
         //   if (seat != null && seat.riddenByEntity != null) {
         //      seat.riddenByEntity.attackEntityFrom(damageSource, dmg);
         //   }
         //}
         //nah
      }
   }

   public void spawndropitems() {

      //todo please just fucking kill me
      MCH_BaseVehicleInfo info = this.getAcInfo();

      if (info != null && info.recipe != null && !info.recipe.isEmpty()) {
         //TODO I think this is broken with oredicts
         // Unknown recipe object: net.minecraftforge.oredict.ShapedOreRecipe
         System.out.println("[MCH] Vehicle destroyed: attempting to drop recipe items...");

         Random rand = new Random();
         int maxTotalDrops = 3;
         int itemsDropped = 0;
         Set<Integer> usedIndexes = new HashSet<>();

         try {

            while (itemsDropped < maxTotalDrops && usedIndexes.size() < info.recipe.size()) {
               int index;
               do {
                  index = rand.nextInt(info.recipe.size());
               } while (usedIndexes.contains(index));
               usedIndexes.add(index);

               Object obj = info.recipe.get(index);
               ItemStack stack = null;

               if (obj instanceof Item) {
                  stack = new ItemStack((Item) obj, 1);
                  System.out.println("[MCH] Selected Item: " + ((Item) obj).getUnlocalizedName());
               } else if (obj instanceof Block) {
                  stack = new ItemStack((Block) obj, 1);
                  System.out.println("[MCH] Selected Block: " + ((Block) obj).getUnlocalizedName());
               } else if (obj instanceof ItemStack) {
                  stack = ((ItemStack) obj).copy();
                  stack.stackSize = 1;
                  System.out.println("[MCH] Selected ItemStack: " + stack.getUnlocalizedName());
               } else if (obj instanceof String) {
                  List<ItemStack> ores = OreDictionary.getOres((String) obj);
                  if (!ores.isEmpty()) {
                     stack = ores.get(rand.nextInt(ores.size())).copy();
                     stack.stackSize = 1;
                     System.out.println("[MCH] Selected OreDict: " + obj + " → " + stack.getUnlocalizedName());
                  } else {
                     System.out.println("[MCH] OreDict empty: " + obj);
                  }
               } else if (obj instanceof ShapedRecipes) {
                  ItemStack[] items = ((ShapedRecipes) obj).recipeItems;
                  List<ItemStack> valid = new ArrayList<ItemStack>();
                  for (ItemStack is : items) if (is != null) valid.add(is);
                  if (!valid.isEmpty()) {
                     stack = valid.get(rand.nextInt(valid.size())).copy();
                     stack.stackSize = 1;
                     System.out.println("[MCH] Selected from ShapedRecipes: " + stack.getDisplayName());
                  } else {
                     System.out.println("[MCH] ShapedRecipes had no valid items: " + obj);
                  }
               } else {
                  System.out.println("[MCH] Unknown recipe object: " + obj.getClass().getName());
               }

               if (stack != null && stack.getItem() != null) {
                  System.out.println("[MCH] Spawning drop: " + stack.getDisplayName());
                  this.worldObj.spawnEntityInWorld(new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, stack));
                  itemsDropped++;
               } else {
                  System.out.println("[MCH] Failed to create ItemStack from: " + obj);
               }
            }
         } catch (Exception ex) {
            System.out.println("[MCH] Error while dropping recipe items: " + ex.getMessage());
            ex.printStackTrace();
         }

      } else {
         System.out.println("[MCH] No recipe found for this vehicle.");
      }
   }

   public boolean isDestroyed() {
      return this.getDespawnCount() > 0;
   }

   public int getDespawnCount() {
      return this.despawnCount;
   }

   public void setDespawnCount(int despawnCount) {
      this.despawnCount = despawnCount;
   }

   public boolean isEntityRadarMounted() {
      return this.hasRadar();
   }

   public boolean hasRadar() { return this.getAcInfo() != null && this.getAcInfo().hasRadar(); }

   public boolean isRadarActive() { return this.hasRadar() && this.getCommonStatus(CMN_ID_ACTIVE_RADAR); }

   public void setRadarActive(boolean active) {
      boolean enabled = active && this.hasRadar();
      boolean wasEnabled = this.getCommonStatus(CMN_ID_ACTIVE_RADAR);
      this.setCommonStatus(CMN_ID_ACTIVE_RADAR, enabled);
      if(wasEnabled && !enabled) this.initRadar();
   }

   public boolean toggleRadar(EntityPlayer player) {
      if(this.worldObj.isRemote || !this.hasRadar() || !this.isPilot(player)) return false;
      this.setRadarActive(!this.isRadarActive());
      player.addChatMessage(new ChatComponentTranslation(this.isRadarActive() ? "mcheli.radar.on" : "mcheli.radar.off"));
      return true;
   }

   public boolean canFloatWater() {
      return this.getAcInfo() != null && this.getAcInfo().isFloat && !this.isDestroyed();
   }

   @SideOnly(Side.CLIENT)
   public int getBrightnessForRender(float par1) {
      if(this.haveSearchLight() && this.isSearchLightON()) {
         return 15728880;
      } else {
         int i = MathHelper.floor_double(super.posX);
         int j = MathHelper.floor_double(super.posZ);
         if(super.worldObj.blockExists(i, 0, j)) {
            double d0 = (super.boundingBox.maxY - super.boundingBox.minY) * 0.66D;
            float fo = this.getAcInfo() != null?this.getAcInfo().submergedDamageHeight:0.0F;
            if(this.canFloatWater()) {
               fo = this.getAcInfo().floatOffset;
               if(fo < 0.0F) {
                  fo = -fo;
               }

               ++fo;
            }

            int k = MathHelper.floor_double(super.posY + (double)fo - (double)super.yOffset + d0);
            int val = super.worldObj.getLightBrightnessForSkyBlocks(i, k, j, 0);
            int low = val & '\uffff';
            int high = val >> 16 & '\uffff';
            if(high < this.brightnessHigh) {
               if(this.brightnessHigh > 0 && this.getCountOnUpdate() % 2 == 0) {
                  --this.brightnessHigh;
               }
            } else if(high > this.brightnessHigh) {
               this.brightnessHigh += 4;
               if(this.brightnessHigh > 240) {
                  this.brightnessHigh = 240;
               }
            }

            return this.brightnessHigh << 16 | low;
         } else {
            return 0;
         }
      }
   }

   public MCH_BaseVehicleInfo.CameraPosition getCameraPosInfo() {
      if(this.getAcInfo() == null) {
         return null;
      } else {
         Entity player = MCH_Lib.getClientPlayer();
         int sid = this.getSeatIdByEntity(player);
         return sid == 0 && this.canSwitchCameraPos() && this.getCameraId() > 0 && this.getCameraId() < this.getAcInfo().cameraPosition.size()?(MCH_BaseVehicleInfo.CameraPosition)this.getAcInfo().cameraPosition.get(this.getCameraId()):(sid > 0 && sid < this.getSeatsInfo().length && this.getSeatsInfo()[sid].invCamPos?this.getSeatsInfo()[sid].getCamPos():(MCH_BaseVehicleInfo.CameraPosition)this.getAcInfo().cameraPosition.get(0));
      }
   }

   public int getCameraId() {
      return this.cameraId;
   }

   public void setCameraId(int cameraId) {
      MCH_Lib.DbgLog(true, "MCH_EntityBaseVehicle.setCameraId %d -> %d", new Object[]{Integer.valueOf(this.cameraId), Integer.valueOf(cameraId)});
      this.cameraId = cameraId;
   }

   public boolean canSwitchCameraPos() {
      return this.getCameraPosNum() >= 2;
   }

   public int getCameraPosNum() {
      return this.getAcInfo() != null?this.getAcInfo().cameraPosition.size():1;
   }

   public void onAcInfoReloaded() {
      if(this.getAcInfo() != null) {
         if(!this.hasRadar()) this.setRadarActive(false);
         this.setSize(this.getAcInfo().bodyWidth, this.getAcInfo().bodyHeight);
         this.aps.configure(this.getAcInfo().apsUseTime, this.getAcInfo().apsWaitTime,
                 this.getAcInfo().apsRange, this.getAcInfo().apsAmmo);
      }
   }

   public void writeSpawnData(ByteBuf buffer) {
      if(this.getAcInfo() != null) {
         buffer.writeFloat(this.getAcInfo().bodyHeight);
         buffer.writeFloat(this.getAcInfo().bodyWidth);
         buffer.writeFloat(this.getAcInfo().thirdPersonDist);
         byte[] name = getTypeName().getBytes(StandardCharsets.UTF_8);
         buffer.writeShort(name.length);
         buffer.writeBytes(name);
      } else {
         buffer.writeFloat(super.height);
         buffer.writeFloat(super.width);
         buffer.writeFloat(4.0F);
         buffer.writeShort(0);
      }

      byte[] commonId = this.getCommonUniqueId().getBytes(StandardCharsets.UTF_8);
      buffer.writeShort(commonId.length);
      buffer.writeBytes(commonId);
      buffer.writeByte(this.aps.getState().ordinal());
      buffer.writeBoolean(this.aps.isArmed());
      buffer.writeInt(this.aps.getAmmoRemaining());
      buffer.writeInt(this.aps.getArmingTimer());
      buffer.writeInt(this.aps.getReloadTimer());
   }

   public void readSpawnData(ByteBuf additionalData) {
      try {
         float e = additionalData.readFloat();
         float width = additionalData.readFloat();
         this.thirdPersonDist = additionalData.readFloat();
         this.setSize(width, e);
         int len = additionalData.readShort();
         if (len > 0) {
            byte[] dst = new byte[len];
            additionalData.readBytes(dst);
            changeType(new String(dst, StandardCharsets.UTF_8));
         }
         int commonIdLength = additionalData.readUnsignedShort();
         if(commonIdLength > 0) {
            byte[] commonId = new byte[commonIdLength];
            additionalData.readBytes(commonId);
            this.setCommonUniqueId(new String(commonId, StandardCharsets.UTF_8));
         }
         this.aps.applyClientState(additionalData.readByte(), additionalData.readBoolean(),
                 additionalData.readInt(), additionalData.readInt(), additionalData.readInt());
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   protected void readEntityFromNBT(NBTTagCompound nbt) {
      MCH_Lib.DbgLog(super.worldObj, "[MCH-STATE][NBT-READ-BEGIN] entity=%s nbtType=%s nbtCommonId=%s rackParent=%s rackSeat=%d",
              new Object[]{this.debugEntity(this), nbt.getString("TypeName"), nbt.getString("AircraftUniqueId"),
                      nbt.getString("MCH_RackParentUniqueId"), Integer.valueOf(nbt.hasKey("MCH_RackSeatId")?nbt.getInteger("MCH_RackSeatId"):-1)});
      this.setDespawnCount(nbt.getInteger("AcDespawnCount"));
      this.setTextureName(nbt.getString("TextureName"));
      this.setCommonUniqueId(nbt.getString("AircraftUniqueId"));
      this.setRotRoll(nbt.getFloat("AcRoll"));
      this.prevRotationRoll = this.getRotRoll();
      this.prevLastRiderYaw = this.lastRiderYaw = nbt.getFloat("AcLastRYaw");
      this.prevLastRiderPitch = this.lastRiderPitch = nbt.getFloat("AcLastRPitch");
      this.setPartStatus(nbt.getInteger("PartStatus"));
      this.setTypeName(nbt.getString("TypeName"));
      super.readEntityFromNBT(nbt);
      boolean savedRadarActive = nbt.hasKey("MCH_RadarActive") ? nbt.getBoolean("MCH_RadarActive") : true;
      if(this.getAcInfo() != null) {
         this.setRadarActive(savedRadarActive);
      } else {
         this.pendingRadarActive = Boolean.valueOf(savedRadarActive);
      }
      this.getGuiInventory().readEntityFromNBT(nbt);
      this.setCommandForce(nbt.getString("AcCommand"));
      this.setFuel(nbt.getInteger("AcFuel"));
      this.serviceFuel = Math.max(0L, nbt.getLong("MCH_ServiceFuel"));
      setGunnerStatus(nbt.getBoolean("AcGunnerStatus"));
      int[] wa_list = nbt.getIntArray("AcWeaponsAmmo");
      if(this.getAcInfo() != null) {
         this.aps.configure(this.getAcInfo().apsUseTime, this.getAcInfo().apsWaitTime,
                 this.getAcInfo().apsRange, this.getAcInfo().apsAmmo);
         this.aps.loadAmmo(nbt.hasKey("AcAPSAmmo") ? nbt.getInteger("AcAPSAmmo") : this.getAcInfo().apsAmmo);
      }

      for(int i = 0; i < wa_list.length; ++i) {
         this.getWeapon(i).setRestAllAmmoNum(wa_list[i]);
         this.getWeapon(i).reloadMag();
      }

      if(this.getDespawnCount() > 0) {
         this.setDamageTaken(this.getMaxHP());
      } else if(nbt.hasKey("AcDamage")) {
         this.setDamageTaken(nbt.getInteger("AcDamage"));
      }

      if(this.haveSearchLight() && nbt.hasKey("SearchLight")) {
         this.setSearchLight(nbt.getBoolean("SearchLight"));
      }

      this.dismountedUserCtrl = nbt.getBoolean("AcDismounted");
      this.pendingRackParentUniqueId = nbt.getString("MCH_RackParentUniqueId");
      this.pendingRackSeatId = nbt.hasKey("MCH_RackSeatId")?nbt.getInteger("MCH_RackSeatId"):-1;
      this.pendingRackPosX = nbt.hasKey("MCH_RackPosX")?nbt.getDouble("MCH_RackPosX"):super.posX;
      this.pendingRackPosY = nbt.hasKey("MCH_RackPosY")?nbt.getDouble("MCH_RackPosY"):super.posY;
      this.pendingRackPosZ = nbt.hasKey("MCH_RackPosZ")?nbt.getDouble("MCH_RackPosZ"):super.posZ;
      this.pendingRackRestoreTicks = this.pendingRackParentUniqueId.isEmpty()?0:1200;
      this.uavPersistentUUID = parseUUID(nbt.getString("MCH_UavPersistentUUID"));
      if(this.uavPersistentUUID == null && (this.isUAV() || this.isNewUAV())) {
         this.uavPersistentUUID = this.getUniqueID();
      }
      this.uavOwnerUUID = parseUUID(nbt.getString("MCH_UavOwnerUUID"));
      this.vehicleOwnerUUID = parseUUID(nbt.getString("MCH_VehicleOwnerUUID"));
      this.setVehicleAccessLocked(this.vehicleOwnerUUID != null && nbt.getBoolean("MCH_VehicleAccessLocked"));
      this.linkedUavStationUUID = parseUUID(nbt.getString("MCH_UavStationUUID"));
      this.linkedUavStationDimension = nbt.getInteger("MCH_UavStationDim");
      this.linkedUavStationX = nbt.getDouble("MCH_UavStationX");
      this.linkedUavStationY = nbt.getDouble("MCH_UavStationY");
      this.linkedUavStationZ = nbt.getDouble("MCH_UavStationZ");
      this.hasLinkedUavStationPosition = nbt.hasKey("MCH_HasUavStationPosition")
              ? nbt.getBoolean("MCH_HasUavStationPosition")
              : this.linkedUavStationUUID != null;
      if(!super.worldObj.isRemote && (this.isUAV() || this.isNewUAV())) {
         MCH_UavRegistry.register(this);
         if(this.uavStation != null && !this.uavStation.isDead) {
            this.uavStation.linkUav(this);
         }
      }
      this.debugVehicleState("NBT-READ-END", null);
      this.debugRackState("NBT-READ-END");
   }

   protected void writeEntityToNBT(NBTTagCompound nbt) {
      this.debugVehicleState("NBT-WRITE", null);
      this.debugRackState("NBT-WRITE");
      nbt.setString("TextureName", this.getTextureName());
      nbt.setString("AircraftUniqueId", this.getCommonUniqueId());
      nbt.setString("TypeName", this.getTypeName());
      nbt.setInteger("PartStatus", this.getPartStatus() & this.getLastPartStatusMask());
      nbt.setInteger("AcFuel", this.getFuel());
      nbt.setLong("MCH_ServiceFuel", this.serviceFuel);
      nbt.setInteger("AcDespawnCount", this.getDespawnCount());
      nbt.setFloat("AcRoll", this.getRotRoll());
      nbt.setBoolean("SearchLight", this.isSearchLightON());
      nbt.setBoolean("MCH_RadarActive", this.isRadarActive());
      nbt.setFloat("AcLastRYaw", this.getLastRiderYaw());
      nbt.setFloat("AcLastRPitch", this.getLastRiderPitch());
      nbt.setString("AcCommand", this.getCommand());
      if (!nbt.hasKey("AcGunnerStatus"))
         setGunnerStatus(true);
      nbt.setBoolean("AcGunnerStatus", getGunnerStatus());
      MCH_EntityBaseVehicle rackParent = this.getRackParent();
      MCH_EntitySeat rackSeat = super.ridingEntity instanceof MCH_EntitySeat?(MCH_EntitySeat)super.ridingEntity:null;
      String rackParentUniqueId = rackParent != null?rackParent.getCommonUniqueId():(rackSeat != null?rackSeat.parentUniqueID:"");
      if(rackSeat != null && rackParentUniqueId != null && !rackParentUniqueId.isEmpty()) {
         nbt.setString("MCH_RackParentUniqueId", rackParentUniqueId);
         nbt.setInteger("MCH_RackSeatId", rackSeat.seatID);
         nbt.setDouble("MCH_RackPosX", super.posX);
         nbt.setDouble("MCH_RackPosY", super.posY);
         nbt.setDouble("MCH_RackPosZ", super.posZ);
      } else if(!this.pendingRackParentUniqueId.isEmpty() && this.pendingRackSeatId >= 0) {
         nbt.setString("MCH_RackParentUniqueId", this.pendingRackParentUniqueId);
         nbt.setInteger("MCH_RackSeatId", this.pendingRackSeatId);
         nbt.setDouble("MCH_RackPosX", this.pendingRackPosX);
         nbt.setDouble("MCH_RackPosY", this.pendingRackPosY);
         nbt.setDouble("MCH_RackPosZ", this.pendingRackPosZ);
      } else {
         nbt.setString("MCH_RackParentUniqueId", "");
         nbt.setInteger("MCH_RackSeatId", -1);
      }
      super.writeEntityToNBT(nbt);
      this.getGuiInventory().writeEntityToNBT(nbt);
      int[] wa_list = new int[this.getWeaponNum()];

      for(int i = 0; i < wa_list.length; ++i) {
         wa_list[i] = this.getWeapon(i).getRestAllAmmoNum() + this.getWeapon(i).getAmmoNum();
      }

      nbt.setTag("AcWeaponsAmmo", W_NBTTag.newTagIntArray("AcWeaponsAmmo", wa_list));
      nbt.setInteger("AcDamage", this.getDamageTaken());
      if(this.getAcInfo() != null && this.getAcInfo().apsAmmo >= 0) nbt.setInteger("AcAPSAmmo", this.aps.getAmmoRemaining());
      nbt.setBoolean("AcDismounted", this.dismountedUserCtrl);
      UUID persistentId = this.getUavPersistentUUID(false);
      if(persistentId == null && (this.isUAV() || this.isNewUAV())) {
         persistentId = this.getUniqueID();
         this.uavPersistentUUID = persistentId;
      }
      nbt.setString("MCH_UavPersistentUUID", persistentId == null ? "" : persistentId.toString());
      nbt.setString("MCH_UavOwnerUUID", this.uavOwnerUUID == null ? "" : this.uavOwnerUUID.toString());
      nbt.setString("MCH_VehicleOwnerUUID", this.vehicleOwnerUUID == null ? "" : this.vehicleOwnerUUID.toString());
      nbt.setBoolean("MCH_VehicleAccessLocked", this.isVehicleAccessLocked());
      nbt.setString("MCH_UavStationUUID", this.linkedUavStationUUID == null ? "" : this.linkedUavStationUUID.toString());
      nbt.setInteger("MCH_UavStationDim", this.linkedUavStationDimension);
      nbt.setDouble("MCH_UavStationX", this.linkedUavStationX);
      nbt.setDouble("MCH_UavStationY", this.linkedUavStationY);
      nbt.setDouble("MCH_UavStationZ", this.linkedUavStationZ);
      nbt.setBoolean("MCH_HasUavStationPosition", this.hasLinkedUavStationPosition);
   }

   public boolean attackEntityFrom(DamageSource damageSource, float org_damage) {

      this.clearPlacementMotionLock();
      Entity src = damageSource.getEntity();
      String srcName = (src == null ? "null" : src.getClass().getName());
      //System.out.println("[DBG] attackEntityFrom: dmgType=" + damageSource.getDamageType()
      //        + " src=" + srcName + " org=" + org_damage);


      //System.out.println("the damage source is " + damageSource.getDamageType());
      //System.out.println("org damage: " + org_damage);
      //System.out.println("damage taken: " + getDamageTaken());
      float damageFactor = this.lastBBDamageFactor;
      this.lastBBDamageFactor = 1.0F;
      this.lastHitBoundingBoxType = EnumBoundingBoxType.DEFAULT;
      if(this.isEntityInvulnerable()) {
         return false;
      } else if(super.isDead) {
         return false;
      } else if(this.timeSinceHit > 0) {
         return false;
      } else {
         String dmt = damageSource.getDamageType();
         if(dmt.equalsIgnoreCase("inFire") && !damageSource.isProjectile()) {
            return false;
         } else if(dmt.equalsIgnoreCase("cactus")) {
            return false;
         } else if(super.worldObj.isRemote) {
            return true;
         } else {

            // ===============================
            // HMG anti-tank explosion bypass
            // ===============================
            if (src != null) {
               String cls = src.getClass().getName();

               // HandmadeGuns projectile family (modular, no hard dep)
               if (cls.startsWith("handmadeguns.entity.bullets.")) {
                  System.out.println("test hmg");
                  this.setDamageTaken(this.getDamageTaken() + (int)org_damage);
                  this.timeSinceHit = 1;
                  //maybe set this to 0?
                  return true;
               }
            }

            MCH_Config var10000 = MCH_MOD.config;
            float damage = MCH_Config.applyDamageByExternal(this, damageSource, org_damage);
            if(!MCH_Multiplay.canAttackEntity(damageSource, this)) {
               return false;
            } else {
               if(dmt.equalsIgnoreCase("lava")) {
                  if (!damageSource.isProjectile()) { //attempt to check for hand made guns projectiles
                     //damage *= (float) (super.rand.nextInt(50) + 2);
                     this.setDamageTaken(this.getDamageTaken() + (int)damage);
                     //System.out.println("testing" + " damage taken:" + this.getDamageTaken());
                     //it does not work

                     //if (worldObj.getWorldTime() % 20 == 0) { // Apply damage every second (20 ticks)
                        //attackEntityFrom(DamageSource.lava, 5); //JUST WORK
                     //}
                  }
                  //damage = org_damage; that isn't a number
                  //this.setOnFireFromLava(); crashes game for some reason

                  this.timeSinceHit = 1;
               }

               if(dmt.startsWith("explosion")) {
                  this.timeSinceHit = 1;
                  this.setDamageTaken(this.getDamageTaken() + (int)damage);
                  System.out.println("testing" + " explosion damage taken:" + this.getDamageTaken());
               } else if(this.isMountedEntity(damageSource.getEntity())) {
                  return false;
               }

               //todo HMG compat
               //if(dmt.startsWith("projectile")) {
               //
               //}
               //idk what the proper thing is for just projectiles in general

               if(dmt.equalsIgnoreCase("onFire")) {
                  //fun TODO: maybe something here for HMG???
                  this.setDamageTaken(this.getDamageTaken() + (int)damage);
                  System.out.println("testing" + " fire damage taken:" + this.getDamageTaken());
                  this.timeSinceHit = 1;
               }

               boolean isCreative = false;
               boolean isSneaking = false;
               Entity entity = damageSource.getEntity();
               boolean isDamegeSourcePlayer = false;
               boolean playDamageSound = false;
               if(entity instanceof EntityPlayer) {
                  EntityPlayer cmd = (EntityPlayer)entity;
                  isCreative = cmd.capabilities.isCreativeMode;
                  isSneaking = cmd.isSneaking();
                  if(dmt.equalsIgnoreCase("player")) {
                     if(isCreative) {
                        isDamegeSourcePlayer = true;
                     } else {
                        var10000 = MCH_MOD.config;
                        if(!MCH_Config.PreventingBroken.prmBool) {
                           var10000 = MCH_MOD.config;
                           if(MCH_Config.BreakableOnlyPickaxe.prmBool) {
                              if(cmd.getCurrentEquippedItem() != null && cmd.getCurrentEquippedItem().getItem() instanceof ItemPickaxe) {
                                 isDamegeSourcePlayer = true;
                              }
                           } else {
                              isDamegeSourcePlayer = !this.isRidePlayer();
                           }
                        }
                     }
                  }

                  W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.0F);
               } else {
                  playDamageSound = true;
               }

               if(!this.isDestroyed()) {



                  if(!isDamegeSourcePlayer) {

                     //add wheel damage

                     //if(this.getWheelDamageTaken() >= this.getMaxWheelDamage()) {
                     //   setCurrentThrottle(0);
                     //   throttleUp = false;
                     //   throttleBack = 0;
//
                     //   //this.getWheelDamageTaken() += (int)damage;
                     //   //todo add a slow repairing effect, togglable via config to be only after wrench repair
//
                     //   //&& this.attackedboundingbox is a wheel boundingbox
                     //   //todo get a way to detect if the wheel boundingbox was impacted, we go from there.
                     //}

                     MCH_BaseVehicleInfo cmd1 = this.getAcInfo();
                     if(cmd1 != null) {
                        //deranged statements below removed from above statement
                        //&& !dmt.equalsIgnoreCase("lava") && !dmt.equalsIgnoreCase("onFire")
                        if(damage > cmd1.armorMaxDamage) {
                           damage = cmd1.armorMaxDamage;
                        }

                        if(damageFactor <= 1.0F) {
                           damage *= damageFactor;
                        }

                        damage *= cmd1.armorDamageFactor;
                        damage -= cmd1.armorMinDamage;
                        if(damage <= 0.0F) {
                           MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.attackEntityFrom:no damage=%.1f -> %.1f(factor=%.2f):%s", new Object[]{Float.valueOf(org_damage), Float.valueOf(damage), Float.valueOf(damageFactor), dmt});
                           return false;
                        }

                        if(damageFactor > 1.0F) {
                           damage *= damageFactor;
                        }
                     }

                     MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.attackEntityFrom:damage=%.1f(factor=%.2f):%s", new Object[]{Float.valueOf(damage), Float.valueOf(damageFactor), dmt});
                     this.setDamageTaken(this.getDamageTaken() + (int)damage);
                  }

                  this.setBeenAttacked();
                  if(this.getDamageTaken() >= this.getMaxHP() || isDamegeSourcePlayer) {

                     //this entire block is dedicated to death

                     if(!isDamegeSourcePlayer) {
                        this.clearSearchlightBlocks();
                        this.setDamageTaken(this.getMaxHP());
                        this.destroyAircraft();
                        this.timeSinceHit = 20;
                        String cmd2 = this.getCommand().trim();
                        if(cmd2.startsWith("/")) {
                           cmd2 = cmd2.substring(1);
                        }

                        if(!cmd2.isEmpty()) {
                           MCH_DummyCommandSender.execCommand(cmd2);
                        }

                        if(dmt.equalsIgnoreCase("inWall")) {
                           //todone may need clearsearchlightblocks here too
                           this.clearSearchlightBlocks();
                           this.explosionByCrash(0.0D);
                           this.damageSinceDestroyed = this.getMaxHP();
                        } else {
                           //todo this is death
                           this.clearSearchlightBlocks();
                           MCH_Explosion.newExplosion(super.worldObj, (Entity)null, entity, super.posX, super.posY, super.posZ, 2.0F, 2.0F, true, true, true, true, 5);
                        }
                     } else {
                        if(this.getAcInfo() != null && this.getAcInfo().getItem() != null) {
                           if(isCreative) {
                              var10000 = MCH_MOD.config;
                              if(MCH_Config.DropItemInCreativeMode.prmBool && !isSneaking) {
                                 //this.clearSearchlightBlocks();
                                 this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
                              }

                              var10000 = MCH_MOD.config;
                              if(!MCH_Config.DropItemInCreativeMode.prmBool && isSneaking) {
                                 //this.clearSearchlightBlocks();
                                 this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
                              }
                           } else {
                              //this.clearSearchlightBlocks();
                              this.dropItemWithOffset(this.getAcInfo().getItem(), 1, 0.0F);
                           }
                        }

                        //no more ghost light blocks
                        if (this.haveSearchLight() && this.isSearchLightON()) {
                           //this.isSearchLightON() = false; bit flip, not boolean. can't do this!
                           //why is this a bit flip btw this mod is actual jank
                           this.clearSearchlightBlocks();
                        }
                        this.setDead(true);
                     }
                  }
               } else if(isDamegeSourcePlayer && isCreative) {
                  //this.clearSearchlightBlocks();
                  this.setDead(true);
               }

               if(playDamageSound) { //90% sure this is the logic that does:
                  // if the vehicle is occupied, we cannot pick it up
                  W_WorldFunc.MOD_playSoundAtEntity(this, "helidmg", 1.0F, 0.9F + super.rand.nextFloat() * 0.1F);
               }

               return true;
            }
         }
      }
   }

   public boolean isExploded() {
      return this.isDestroyed() && this.damageSinceDestroyed > this.getMaxHP() / 10 + 1;
   }

   public void destruct() {
      Entity pilot = this.getRiddenByEntity();
      if(pilot != null) {
         if(this.isNewUAV() && !super.worldObj.isRemote) {
            this.returnNewUavPilotToStation(pilot, "uav_destructed");
         } else {
            pilot.mountEntity((Entity)null);
         }
      }
      this.setDead(true);
   }



   public EntityItem entityDropItem(ItemStack is, float par2) {
      if(is.stackSize == 0) {
         return null;
      } else {
         this.setAcDataToItem(is);
         return super.entityDropItem(is, par2);
      }
   }

   public void setAcDataToItem(ItemStack is) {
      if(!is.hasTagCompound()) {
         is.setTagCompound(new NBTTagCompound());
      }

      NBTTagCompound nbt = is.getTagCompound();
      nbt.setString("MCH_Command", this.getCommand());
      nbt.setLong("MCH_ServiceFuel", this.serviceFuel);
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.ItemFuel.prmBool) {
         nbt.setInteger("MCH_Fuel", this.getFuel());
      }

      var10000 = MCH_MOD.config;
      if(MCH_Config.ItemDamage.prmBool) {
         is.setItemDamage(this.getDamageTaken());
      }

      nbt.setString("MCH_VehicleOwnerUUID", this.vehicleOwnerUUID == null ? "" : this.vehicleOwnerUUID.toString());
      nbt.setBoolean("MCH_VehicleAccessLocked", this.isVehicleAccessLocked());

   }

   public void getAcDataFromItem(ItemStack is) {
      if(is.hasTagCompound()) {
         NBTTagCompound nbt = is.getTagCompound();
         this.setCommandForce(nbt.getString("MCH_Command"));
         this.serviceFuel = Math.max(0L, nbt.getLong("MCH_ServiceFuel"));
         MCH_Config var10000 = MCH_MOD.config;
         if(MCH_Config.ItemFuel.prmBool) {
            this.setFuel(nbt.getInteger("MCH_Fuel"));
         }

         var10000 = MCH_MOD.config;
         if(MCH_Config.ItemDamage.prmBool) {
            this.setDamageTaken(is.getItemDamage());
         }

      }
   }

   public boolean isUseableByPlayer(EntityPlayer player) {
      return this.isUAV()?super.isUseableByPlayer(player):(!super.isDead?(this.getSeatIdByEntity(player) >= 0?player.getDistanceSqToEntity(this) <= 4096.0D:player.getDistanceSqToEntity(this) <= 64.0D):false);
   }

   public void applyEntityCollision(Entity par1Entity) {
      this.clearPlacementMotionLock();
   }

   public void addVelocity(double par1, double par3, double par5) {
      this.clearPlacementMotionLock();
   }

   public void setVelocity(double par1, double par3, double par5) {
      if(this.placementMotionLocked) {
         this.clearPlacementMotionState();
         return;
      }
      this.velocityX = super.motionX = par1;
      this.velocityY = super.motionY = par3;
      this.velocityZ = super.motionZ = par5;
   }

   public void markFreshlyPlaced() {
      this.placementMotionLocked = true;
      this.clearPlacementMotionState();
   }

   public void clearPlacementMotionLock() {
      this.placementMotionLocked = false;
   }

   public boolean isPlacementMotionLocked() {
      return this.placementMotionLocked;
   }

   protected void clearPlacementMotionState() {
      super.motionX = super.motionY = super.motionZ = 0.0D;
      this.velocityX = this.velocityY = this.velocityZ = 0.0D;
      this.aircraftPosRotInc = 0;
      this.prevPosition.clear(Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
   }

   public double getCachedVelocityX() {
      return this.velocityX;
   }

   public double getCachedVelocityY() {
      return this.velocityY;
   }

   public double getCachedVelocityZ() {
      return this.velocityZ;
   }

   public void onFirstUpdate() {
      if(!super.worldObj.isRemote) {
         MCH_Config var10002 = MCH_MOD.config;
         this.setCommonStatus(3, MCH_Config.InfinityAmmo.prmBool);
         var10002 = MCH_MOD.config;
         this.setCommonStatus(4, MCH_Config.InfinityFuel.prmBool);
      }

   }

   public void onRidePilotFirstUpdate() {
      if(super.worldObj.isRemote && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
         this.updateClientSettings(0);
      }

      Entity pilot = this.getRiddenByEntity();
      if(pilot != null) {
         pilot.rotationYaw = this.getLastRiderYaw();
         pilot.rotationPitch = this.getLastRiderPitch();
      }

      this.keepOnRideRotation = false;
      if(this.getAcInfo() != null) {
         this.switchFreeLookModeClient(this.getAcInfo().defaultFreelook);
      }

   }

   public double getCurrentThrottle() {
      return this.currentThrottle;
   }

   public void setCurrentThrottle(double throttle) {
      this.currentThrottle = throttle;
   }

   public void addCurrentThrottle(double throttle) {
      this.setCurrentThrottle(this.getCurrentThrottle() + throttle);
   }

   public double getPrevCurrentThrottle() {
      return this.prevCurrentThrottle;
   }

   public boolean canMouseRot() {
      return !super.isDead && this.getRiddenByEntity() != null && !this.isDestroyed();
   }

   public boolean canUpdateYaw(Entity player) {
      return this.getRidingEntity() != null?false:(this.getCountOnUpdate() < 30?false:MCH_Lib.getBlockIdY(this, 3, -2) == 0);
   }

   public boolean canUpdatePitch(Entity player) {
      return this.getCountOnUpdate() < 30?false:MCH_Lib.getBlockIdY(this, 3, -2) == 0;
   }

   public boolean canUpdateRoll(Entity player) {
      return this.getRidingEntity() != null?false:(this.getCountOnUpdate() < 30?false:MCH_Lib.getBlockIdY(this, 3, -2) == 0);
   }

   public boolean isOverridePlayerYaw() {
      return !this.isFreeLookMode();
   }

   public boolean isOverridePlayerPitch() {
      return !this.isFreeLookMode();
   }

   public double getAddRotationYawLimit() {
      return this.getAcInfo() != null?40.0D * (double)this.getAcInfo().mobilityYaw:40.0D;
   }

   public double getAddRotationPitchLimit() {
      return this.getAcInfo() != null?40.0D * (double)this.getAcInfo().mobilityPitch:40.0D;
   }

   public double getAddRotationRollLimit() {
      return this.getAcInfo() != null?40.0D * (double)this.getAcInfo().mobilityRoll:40.0D;
   }

   public float getYawFactor() {
      return 1.0F;
   }

   public float getPitchFactor() {
      return 1.0F;
   }

   public float getRollFactor() {
      return 1.0F;
   }

   public abstract void onUpdateAngles(float var1);

   public float getControlRotYaw(float mouseX, float mouseY, float tick) {
      return 0.0F;
   }

   public float getControlRotPitch(float mouseX, float mouseY, float tick) {
      return 0.0F;
   }

   public float getControlRotRoll(float mouseX, float mouseY, float tick) {
      return 0.0F;
   }

   /** Vehicle families can override this to reduce pilot control authority. */
   protected float getControlAuthorityFactor() {
      return 1.0F;
   }

   /** Current simplified load factor. Generic vehicles do not run fixed-wing stress simulation. */
   public double getCurrentGForce() {
      return 1.0D;
   }

   public float getPitchAngularVelocity() {
      return 0.0F;
   }

   public float getRollAngularVelocity() {
      return 0.0F;
   }

   public float getYawAngularVelocity() {
      return 0.0F;
   }

   /** Current fixed-wing angle of attack in degrees. Non-fixed-wing vehicles report zero. */
   public double getAngleOfAttackDegrees() {
      return 0.0D;
   }

   /** Smoothed 0..1 stall severity used by debug output. Non-fixed-wing vehicles report zero. */
   public double getStallSeverity() {
      return 0.0D;
   }

   public double getSpeedStallSeverity() {
      return 0.0D;
   }

   public double getAoAStallSeverity() {
      return 0.0D;
   }

   public double getStallDemand() {
      return 0.0D;
   }

   public double getCriticalAoA() {
      return 0.0D;
   }

   public boolean isPitchBreakActive() {
      return false;
   }

   public double getLastStallPitchMoment() {
      return 0.0D;
   }

   public double getLastLiftCoefficient() {
      return 0.0D;
   }

   public boolean isStallRecovering() {
      return false;
   }

   public double getLastNoseUpPitchSuppression() {
      return 0.0D;
   }

   public double getLastUnsupportedClimbSeverity() {
      return 0.0D;
   }

   public boolean isUnsupportedClimb() {
      return false;
   }

   public boolean isLastIdleUnsupportedClimb() {
      return false;
   }

   public String getLastIdleThrottleWarning() {
      return "";
   }

   public double getLastHorizontalSpeed() {
      return 0.0D;
   }

   public String getLastLowHorizontalSpeedWarning() {
      return "";
   }

   public double getLastPitchAuthority() {
      return 1.0D;
   }

   public double getLastControlAuthority() {
      return this.getDebugControlAuthority();
   }

   /** Most recent fixed-wing drag fraction applied by the energy model. */
   public double getLastAerodynamicDrag() {
      return 0.0D;
   }

   /** Most recent fixed-wing lift-loss fraction from stall logic. */
   public double getLastLiftLoss() {
      return 0.0D;
   }

   public double getLastGravityAcceleration() {
      return 0.0D;
   }

   public double getLastLiftAcceleration() {
      return 0.0D;
   }

   public double getLastNetVerticalAcceleration() {
      return 0.0D;
   }

   public boolean isLastAirborne() {
      return !super.onGround;
   }

   public double getResolvedNewFlightGravity() {
      return 0.0D;
   }

   public boolean isUsingNewFlightGravityOverride() {
      return false;
   }

   /** Effective 0..1 control authority after stall and high-G penalties. */
   public float getDebugControlAuthority() {
      return this.getControlAuthorityFactor();
   }


   public int getFuelRemainingTicks() {
      if(this.getMaxFuel() <= 0 || this.getFuel() <= 0 || this.isInfinityFuel(this.getRiddenByEntity(), true)) {
         return -1;
      }
      if(this.getAcInfo() == null || this.getAcInfo().fuelConsumption <= 0.0F) {
         return -1;
      }
      double throttle = MathHelper.clamp_double(this.getNormalizedThrottle(), 0.0D, 1.0D);
      double burnPerSecond = Math.min(throttle * 1.4D, 1.0D) * (double)this.getAcInfo().fuelConsumption * (double)this.getFuelConsumptionFactor();
      if(burnPerSecond <= 0.01D || Double.isNaN(burnPerSecond) || Double.isInfinite(burnPerSecond)) {
         return -1;
      }
      return Math.max(0, (int)Math.round((double)this.getFuel() / burnPerSecond * 20.0D));
   }

   public int getFuelRemainingSeconds() {
      int ticks = this.getFuelRemainingTicks();
      return ticks < 0 ? -1 : Math.max(0, (int)Math.round((double)ticks / 20.0D));
   }

   /** Normalized pilot throttle for debug/HUD text. */
   public double getNormalizedThrottle() {
      return this.getCurrentThrottle();
   }

   /** New-flight combat flap state for debug/HUD text. */
   public boolean isCombatFlapsDeployed() {
      return false;
   }

   /** True when the vehicle is currently beyond its new-flight safe speed. */
   public boolean isOverspeeding() {
      return false;
   }

   /** Hook for vehicle-family-specific stress or aerodynamic updates. */
   protected void updateVehicleStress() {
   }

   protected void setAnglesLegacy(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY, float x, float y, float partialTicks) {
      //System.out.println("set angles");
      if(partialTicks < 0.03F) {
         partialTicks = 0.4F;
         //System.out.println("partial ticks = 0.4");
      }

      if(partialTicks > 0.9F) {
         partialTicks = 0.6F;
         //System.out.println("Partial ticks = 0.6");
      }

      this.lowPassPartialTicks.put(partialTicks);
      partialTicks = this.lowPassPartialTicks.getAvg();
      float ac_pitch = this.getRotPitch();
      float ac_yaw = this.getRotYaw();
      float ac_roll = this.getRotRoll();
      if(this.isFreeLookMode()) {
         y = 0.0F;
         x = 0.0F;
      }

      float yaw = 0.0F;
      float pitch = 0.0F;
      float roll = 0.0F;
      double m_add;
      if(this.canUpdateYaw(player)) {
         m_add = this.getAddRotationYawLimit();
         yaw = this.getControlRotYaw(x, y, partialTicks);
         if((double)yaw < -m_add) {
            yaw = (float)(-m_add);
         }

         if((double)yaw > m_add) {
            yaw = (float)m_add;
         }

         yaw = (float)((double)(yaw * this.getYawFactor()) * 0.06D * (double)partialTicks);
      }

      if(this.canUpdatePitch(player)) {
         m_add = this.getAddRotationPitchLimit();
         pitch = this.getControlRotPitch(x, y, partialTicks);
         if((double)pitch < -m_add) {
            pitch = (float)(-m_add);
         }

         if((double)pitch > m_add) {
            pitch = (float)m_add;
         }

         pitch = (float)((double)(-pitch * this.getPitchFactor()) * 0.06D * (double)partialTicks);
      }

      if(this.canUpdateRoll(player)) {
         m_add = this.getAddRotationRollLimit();
         roll = this.getControlRotRoll(x, y, partialTicks);
         if((double)roll < -m_add) {
            roll = (float)(-m_add);
         }

         if((double)roll > m_add) {
            roll = (float)m_add;
         }

         roll = roll * this.getRollFactor() * 0.06F * partialTicks;
      }

      MCH_Math.FMatrix m_add1 = MCH_Math.newMatrix();
      MCH_Math.MatTurnZ(m_add1, roll / 180.0F * 3.1415927F);
      MCH_Math.MatTurnX(m_add1, pitch / 180.0F * 3.1415927F);
      MCH_Math.MatTurnY(m_add1, yaw / 180.0F * 3.1415927F);
      MCH_Math.MatTurnZ(m_add1, (float)((double)(this.getRotRoll() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnX(m_add1, (float)((double)(this.getRotPitch() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnY(m_add1, (float)((double)(this.getRotYaw() / 180.0F) * 3.141592653589793D));
      MCH_Math.FVector3D v = MCH_Math.MatrixToEuler(m_add1);
      if(this.getAcInfo().limitRotation) {
         v.x = MCH_Lib.RNG(v.x, this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch);
         v.z = MCH_Lib.RNG(v.z, this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll);
      }

      if(v.z > 180.0F) {
         v.z -= 360.0F;
      }

      if(v.z < -180.0F) {
         v.z += 360.0F;
      }

      this.setRotYaw(v.y);
      this.setRotPitch(v.x);
      this.setRotRoll(v.z);
      this.onUpdateAngles(partialTicks);
      if(this.getAcInfo().limitRotation) {
         v.x = MCH_Lib.RNG(this.getRotPitch(), this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch);
         v.z = MCH_Lib.RNG(this.getRotRoll(), this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll);
         this.setRotPitch(v.x);
         this.setRotRoll(v.z);
      }

      float RV = 180.0F;
      if(MathHelper.abs(this.getRotPitch()) > 90.0F) {
         MCH_Lib.DbgLog(true, "MCH_EntityBaseVehicle.setAngles Error:Pitch=%.1f", new Object[]{Float.valueOf(this.getRotPitch())});
      }

      if(this.getRotRoll() > 180.0F) {
         this.setRotRoll(this.getRotRoll() - 360.0F);
      }

      if(this.getRotRoll() < -180.0F) {
         this.setRotRoll(this.getRotRoll() + 360.0F);
      }

      this.prevRotationRoll = this.getRotRoll();
      super.prevRotationPitch = this.getRotPitch();
      if(this.getRidingEntity() == null) {
         super.prevRotationYaw = this.getRotYaw();
      }

      if(!this.isOverridePlayerYaw() && !fixRot) {
         player.setAngles(deltaX, 0.0F);
      } else {
         if(this.getRidingEntity() == null) {
            player.prevRotationYaw = this.getRotYaw() + (fixRot?fixYaw:0.0F);
         } else {
            if(this.getRotYaw() - player.rotationYaw > 180.0F) {
               player.prevRotationYaw += 360.0F;
            }

            if(this.getRotYaw() - player.rotationYaw < -180.0F) {
               player.prevRotationYaw -= 360.0F;
            }
         }

         player.rotationYaw = this.getRotYaw() + (fixRot?fixYaw:0.0F);
      }

      if(!this.isOverridePlayerPitch() && !fixRot) {
         //System.out.println("this is when the helicopter is hovering");
         player.setAngles(0.0F, deltaY);
      } else {
         //System.out.println("God's unholy retribution");
         player.prevRotationPitch = this.getRotPitch() + (fixRot?fixPitch:0.0F);
         player.rotationPitch = this.getRotPitch() + (fixRot?fixPitch:0.0F);
      }

      if(this.getRidingEntity() == null && ac_yaw != this.getRotYaw() || ac_pitch != this.getRotPitch() || ac_roll != this.getRotRoll()) {
         this.aircraftRotChanged = true;
         //System.out.println("aircraft rot changed");
      }

   }

   protected boolean useNewMobilitySystem() {
      return this.getAcInfo() != null && this.getAcInfo().useNewMobilitySystem;
   }

   protected float decayMobilityValue(float value, float factor, float partialTicks) {
      return this.useNewMobilitySystem() ? MCH_FlightModel.decayPerTick(value, factor, partialTicks) : value * factor;
   }

   public void setAngles(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY, float x, float y, float partialTicks) {
      if(!this.useNewMobilitySystem()) {
         this.setAnglesLegacy(player, fixRot, fixYaw, fixPitch, deltaX, deltaY, x, y, partialTicks);
         return;
      }

      // Render tick callbacks pass a fraction of a Minecraft tick. Treat that
      // value only as elapsed simulation time; never clamp tiny high-FPS frames
      // to a large fixed value or smooth it with previous render frames.
      partialTicks = MCH_FlightModel.getBoundedTickDelta(partialTicks);
      float ac_pitch = this.getRotPitch();
      float ac_yaw = this.getRotYaw();
      float ac_roll = this.getRotRoll();
      if(this.isFreeLookMode()) {
         y = 0.0F;
         x = 0.0F;
      }

      float yaw = 0.0F;
      float pitch = 0.0F;
      float roll = 0.0F;
      double m_add;
      if(this.canUpdateYaw(player)) {
         m_add = this.getAddRotationYawLimit();
         yaw = this.getControlRotYaw(x, y, partialTicks);
         if((double)yaw < -m_add) {
            yaw = (float)(-m_add);
         }

         if((double)yaw > m_add) {
            yaw = (float)m_add;
         }

         yaw = (float)((double)(yaw * this.getYawFactor()) * 0.06D);
      }

      if(this.canUpdatePitch(player)) {
         m_add = this.getAddRotationPitchLimit();
         pitch = this.getControlRotPitch(x, y, partialTicks);
         if((double)pitch < -m_add) {
            pitch = (float)(-m_add);
         }

         if((double)pitch > m_add) {
            pitch = (float)m_add;
         }

         pitch = (float)((double)(-pitch * this.getPitchFactor()) * 0.06D);
      }

      if(this.canUpdateRoll(player)) {
         m_add = this.getAddRotationRollLimit();
         roll = this.getControlRotRoll(x, y, partialTicks);
         if((double)roll < -m_add) {
            roll = (float)(-m_add);
         }

         if((double)roll > m_add) {
            roll = (float)m_add;
         }

         roll = roll * this.getRollFactor() * 0.06F;
      }

      float controlAuthority = this.getControlAuthorityFactor();
      pitch *= controlAuthority;
      roll *= controlAuthority;
      yaw *= controlAuthority;

      MCH_Math.FMatrix m_add1 = MCH_Math.newMatrix();
      MCH_Math.MatTurnZ(m_add1, roll / 180.0F * 3.1415927F);
      MCH_Math.MatTurnX(m_add1, pitch / 180.0F * 3.1415927F);
      MCH_Math.MatTurnY(m_add1, yaw / 180.0F * 3.1415927F);
      MCH_Math.MatTurnZ(m_add1, (float)((double)(this.getRotRoll() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnX(m_add1, (float)((double)(this.getRotPitch() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnY(m_add1, (float)((double)(this.getRotYaw() / 180.0F) * 3.141592653589793D));
      MCH_Math.FVector3D v = MCH_Math.MatrixToEuler(m_add1);
      if(this.getAcInfo().limitRotation) {
         v.x = MCH_Lib.RNG(v.x, this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch);
         v.z = MCH_Lib.RNG(v.z, this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll);
      }

      if(v.z > 180.0F) {
         v.z -= 360.0F;
      }

      if(v.z < -180.0F) {
         v.z += 360.0F;
      }

      this.setRotYaw(v.y);
      this.setRotPitch(v.x);
      this.setRotRoll(v.z);
      this.onUpdateAngles(partialTicks);
      if(this.getAcInfo().limitRotation) {
         v.x = MCH_Lib.RNG(this.getRotPitch(), this.getAcInfo().minRotationPitch, this.getAcInfo().maxRotationPitch);
         v.z = MCH_Lib.RNG(this.getRotRoll(), this.getAcInfo().minRotationRoll, this.getAcInfo().maxRotationRoll);
         this.setRotPitch(v.x);
         this.setRotRoll(v.z);
      }

      float RV = 180.0F;
      if(MathHelper.abs(this.getRotPitch()) > 90.0F) {
         MCH_Lib.DbgLog(true, "MCH_EntityBaseVehicle.setAngles Error:Pitch=%.1f", new Object[]{Float.valueOf(this.getRotPitch())});
      }

      if(this.getRotRoll() > 180.0F) {
         this.setRotRoll(this.getRotRoll() - 360.0F);
      }

      if(this.getRotRoll() < -180.0F) {
         this.setRotRoll(this.getRotRoll() + 360.0F);
      }

      this.prevRotationRoll = this.getRotRoll();
      super.prevRotationPitch = this.getRotPitch();
      if(this.getRidingEntity() == null) {
         super.prevRotationYaw = this.getRotYaw();
      }

      if(!this.isOverridePlayerYaw() && !fixRot) {
         player.setAngles(deltaX, 0.0F);
      } else {
         if(this.getRidingEntity() == null) {
            player.prevRotationYaw = this.getRotYaw() + (fixRot?fixYaw:0.0F);
         } else {
            if(this.getRotYaw() - player.rotationYaw > 180.0F) {
               player.prevRotationYaw += 360.0F;
            }

            if(this.getRotYaw() - player.rotationYaw < -180.0F) {
               player.prevRotationYaw -= 360.0F;
            }
         }

         player.rotationYaw = this.getRotYaw() + (fixRot?fixYaw:0.0F);
      }

      if(!this.isOverridePlayerPitch() && !fixRot) {
         //System.out.println("this is when the helicopter is hovering");
         player.setAngles(0.0F, deltaY);
      } else {
         //System.out.println("God's unholy retribution");
         player.prevRotationPitch = this.getRotPitch() + (fixRot?fixPitch:0.0F);
         player.rotationPitch = this.getRotPitch() + (fixRot?fixPitch:0.0F);
      }

      if(this.getRidingEntity() == null && ac_yaw != this.getRotYaw() || ac_pitch != this.getRotPitch() || ac_roll != this.getRotRoll()) {
         this.aircraftRotChanged = true;
         //System.out.println("aircraft rot changed");
      }

   }

   public boolean canSwitchSearchLight(Entity entity) {
      return this.haveSearchLight() && this.getSeatIdByEntity(entity) <= 1;
   }

   public boolean isSearchLightON() {
      //todone fix lights
      return this.getCommonStatus(6);
   }

   public void setSearchLight(boolean onoff) {
      this.setCommonStatus(6, onoff);
   }

   public boolean haveSearchLight() {
      return this.getAcInfo() != null && this.getAcInfo().searchLights.size() > 0;
   }

   //private EntityMCH_Light searchLightEntity;

   //todo improve search light stuff here
   //@Override
   //we aren't overriding anything
   public float getSearchLightValue(Entity entity) {
      if(this.haveSearchLight() && this.isSearchLightON()) {




         Iterator i$ = this.getAcInfo().searchLights.iterator();

         while(i$.hasNext()) {
            MCH_BaseVehicleInfo.SearchLight sl = (MCH_BaseVehicleInfo.SearchLight)i$.next();
            Vec3 pos = this.getTransformedPosition(sl.pos);
            double dist = entity.getDistanceSq(pos.xCoord, pos.yCoord, pos.zCoord);
            if(dist > 2.0D && dist < (double)(sl.height * sl.height + 20.0F)) {
               double cx = entity.posX - pos.xCoord;
               double cy = entity.posY - pos.yCoord;
               double cz = entity.posZ - pos.zCoord;
               double h = 0.0D;
               double v = 0.0D;
               float angle1;
               if(!sl.fixDir) {
                  Vec3 angle = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -this.lastSearchLightYaw + sl.yaw, -this.lastSearchLightPitch + sl.pitch, -this.getRotRoll());
                  h = (double)MCH_Lib.getPosAngle(angle.xCoord, angle.zCoord, cx, cz);
                  v = Math.atan2(cy, Math.sqrt(cx * cx + cz * cz)) * 180.0D / 3.141592653589793D;
                  v = Math.abs(v + (double)this.lastSearchLightPitch + (double)sl.pitch);
               } else {
                  angle1 = 0.0F;
                  if(sl.steering) {
                     angle1 = this.rotYawWheel * sl.stRot;
                  }

                  Vec3 value = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -this.getRotYaw() + sl.yaw + angle1, -this.getRotPitch() + sl.pitch, -this.getRotRoll());
                  h = (double)MCH_Lib.getPosAngle(value.xCoord, value.zCoord, cx, cz);
                  v = Math.atan2(cy, Math.sqrt(cx * cx + cz * cz)) * 180.0D / 3.141592653589793D;
                  v = Math.abs(v + (double)this.getRotPitch() + (double)sl.pitch);
               }

               angle1 = sl.angle * 3.0F;
               if(h < (double)angle1 && v < (double)angle1) {
                  float value1 = 0.0F;
                  if(h + v < (double)angle1) {
                     value1 = (float)(1440.0D * (1.0D - (h + v) / (double)angle1));
                  }

                  return value1 <= 240.0F?value1:240.0F;
               }
            }
         }
      }

      return 0.0F;
   }

   public abstract void onUpdateAircraft();

   public void onUpdate() {
      if(super.worldObj.isRemote && this.getAcInfo() == null) {
         String typeName = this.getTypeName();
         if(typeName != null && !typeName.isEmpty()) {
            this.changeType(typeName);
         }
      }
      if(this.getCountOnUpdate() < 2) {
         this.prevPosition.clear(Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
      }
      if(this.placementMotionLocked && this.getRiddenByEntity() != null) {
         this.clearPlacementMotionLock();
      }
      if(this.placementMotionLocked && !super.worldObj.isRemote) {
         this.clearPlacementMotionState();
      }



      //if (this.haveSearchLight() && this.isSearchLightON()) {
      //   for (MCH_BaseVehicleInfo.SearchLight sl : this.getAcInfo().searchLights) {
      //      Vec3 pos = this.getTransformedPosition(sl.pos);
//
      //      int bx = (int)Math.floor(pos.xCoord);
      //      int by = (int)Math.floor(pos.yCoord);
      //      int bz = (int)Math.floor(pos.zCoord);
//
      //      Block blockAt = worldObj.getBlock(bx, by, bz);
      //      if (!(blockAt instanceof BlockLight)) {
      //         worldObj.setBlock(bx, by, bz, MCH_LightBlock.INSTANCE, 0, 2); // your custom light block
      //      }
      //   }
      //} else {
      //   // Light OFF: remove previously placed light blocks
      //   for (MCH_BaseVehicleInfo.SearchLight sl : this.getAcInfo().searchLights) {
      //      Vec3 pos = this.getTransformedPosition(sl.pos);
//
      //      int bx = (int)Math.floor(pos.xCoord);
      //      int by = (int)Math.floor(pos.yCoord);
      //      int bz = (int)Math.floor(pos.zCoord);
//
      //      Block blockAt = worldObj.getBlock(bx, by, bz);
      //      if (blockAt instanceof BlockLight) {
      //         worldObj.setBlockToAir(bx, by, bz);
      //      }
      //   }
      //}

      this.prevCurrentThrottle = this.getCurrentThrottle();
      this.lastBBDamageFactor = 1.0F;
      //this.updateVehicleStress();
      //no? only planes or helis right? unless we want to add like bridge collapse for maus or some shit/mud
      // but that's AFTER planes are fully fixed
      this.updateControl();
      this.checkServerNoMove();
      this.onUpdate_RidingEntity();

      Iterator itr = this.listUnmountReserve.iterator();

      while(itr.hasNext()) {
         MCH_EntityBaseVehicle.UnmountReserve ft = (MCH_EntityBaseVehicle.UnmountReserve)itr.next();
         if(ft.entity != null && !ft.entity.isDead) {
            ft.entity.setPosition(ft.posX, ft.posY, ft.posZ);
            ft.entity.fallDistance = super.fallDistance;
         }

         if(ft.cnt > 0) {
            --ft.cnt;
         }

         if(ft.cnt == 0) {
            itr.remove();
         }
      }

      //TODO: better damage calc?

      //if (isInLava()) {
      //   // Apply lava damage at regular intervals
      //   if (worldObj.getWorldTime() % 20 == 0) { // Apply damage every second (20 ticks)
      //      attackEntityFrom(DamageSource.LAVA, lavaDamageAmount);
      //   }
      //}

      Entity e;
      int var7;
      if (isDestroyed() && getCountOnUpdate() % 20 == 0) {

         //todo wait about 15-20 seconds bc fire
                for (int i = 0; i < getSeatNum() + 1; i++) {
                     Entity entity = getEntityBySeatId(i);
                     if (entity != null && (i != 0 || !isUAV() || !isNewUAV())) {
                          MCH_Config var10000 = MCH_MOD.config;
                          if (MCH_Config.applyDamageVsEntity(entity, DamageSource.inFire, 1.0F) > 0.0F) {
                             //todo get and summon a random few items used in the recipe for the vehicle here
                             //this is where vehicle dies; we want people killing things to at least return some profit
                             //MCH_BaseVehicleInfo.RotPart)this.getAcInfo()
                             //MCH_BaseVehicleInfo.
                             entity.setFire(5);
                             //this.getAcInfo().recipe.


                             //item drop logic

                             //MCH_BaseVehicleInfo info = this.getAcInfo();
                             //if (info != null && info.recipe != null && !info.recipe.isEmpty()) {
                             //   System.out.println("[MCH] Vehicle destroyed: attempting to drop recipe items...");
//
                             //   Random rand = new Random();
                             //   int maxTotalDrops = 3;
                             //   int itemsDropped = 0;
                             //   Set<Integer> usedIndexes = new HashSet<>();
//
                             //   try {
//
                             //      while (itemsDropped < maxTotalDrops && usedIndexes.size() < info.recipe.size()) {
                             //         int index;
                             //         do {
                             //            index = rand.nextInt(info.recipe.size());
                             //         } while (usedIndexes.contains(index));
                             //         usedIndexes.add(index);
//
                             //         Object obj = info.recipe.get(index);
                             //         ItemStack stack = null;
//
                             //         if (obj instanceof Item) {
                             //            stack = new ItemStack((Item) obj, 1);
                             //            System.out.println("[MCH] Selected Item: " + ((Item) obj).getUnlocalizedName());
                             //         } else if (obj instanceof Block) {
                             //            stack = new ItemStack((Block) obj, 1);
                             //            System.out.println("[MCH] Selected Block: " + ((Block) obj).getUnlocalizedName());
                             //         } else if (obj instanceof ItemStack) {
                             //            stack = ((ItemStack) obj).copy();
                             //            stack.stackSize = 1;
                             //            System.out.println("[MCH] Selected ItemStack: " + stack.getUnlocalizedName());
                             //         } else if (obj instanceof String) {
                             //            List<ItemStack> ores = OreDictionary.getOres((String) obj);
                             //            if (!ores.isEmpty()) {
                             //               stack = ores.get(rand.nextInt(ores.size())).copy();
                             //               stack.stackSize = 1;
                             //               System.out.println("[MCH] Selected OreDict: " + obj + " → " + stack.getUnlocalizedName());
                             //            } else {
                             //               System.out.println("[MCH] OreDict empty: " + obj);
                             //            }
                             //         } else if (obj instanceof ShapedRecipes) {
                             //            ItemStack[] items = ((ShapedRecipes) obj).recipeItems;
                             //            List<ItemStack> valid = new ArrayList<ItemStack>();
                             //            for (ItemStack is : items) if (is != null) valid.add(is);
                             //            if (!valid.isEmpty()) {
                             //               stack = valid.get(rand.nextInt(valid.size())).copy();
                             //               stack.stackSize = 1;
                             //               System.out.println("[MCH] Selected from ShapedRecipes: " + stack.getDisplayName());
                             //            } else {
                             //               System.out.println("[MCH] ShapedRecipes had no valid items: " + obj);
                             //            }
                             //         } else {
                             //            System.out.println("[MCH] Unknown recipe object: " + obj.getClass().getName());
                             //         }
//
                             //         if (stack != null && stack.getItem() != null) {
                             //            System.out.println("[MCH] Spawning drop: " + stack.getDisplayName());
                             //            entity.worldObj.spawnEntityInWorld(new EntityItem(entity.worldObj, entity.posX, entity.posY, entity.posZ, stack));
                             //            itemsDropped++;
                             //         } else {
                             //            System.out.println("[MCH] Failed to create ItemStack from: " + obj);
                             //         }
                             //      }
                             //   } catch (Exception ex) {
                             //      System.out.println("[MCH] Error while dropping recipe items: " + ex.getMessage());
                             //      ex.printStackTrace();
                             //   }
//
                             //} else {
                             //   System.out.println("[MCH] No recipe found for this vehicle.");
                             //}
                             //todo AHHHHHHHHHHHHHHHHHHHHHHH
                             //doesnt work
                             //no wait no it does wtf it just drops a LOT of items for some reason.
                             //no wait it drops a lot of items if the vehicle is destroyed and it's a ai gunner in the seat for some reason?
                             //this makes sense because of that one time I nuked an ai gunner and like 17 more vehicles appeared
                             //in the same position as the 1 vehicle and you could get like 16 more of the vehicle or something..
                             //might just be a creative mode thing idk tho
                             //we should probably move this to the smoked out dead vehicle logic instead of the explosion logic.


                             }
                        }
                   }
              }


      //if (this.isDestroyed) { //this.isExploded()
      //
      //   }


     // if (isDestroyed() && isNewUAV()) {
     //
     // }

      if((this.aircraftRotChanged || this.aircraftRollRev) && super.worldObj.isRemote && this.getRiddenByEntity() != null) {
         MCH_PacketIndRotation.send(this);
         this.aircraftRotChanged = false;
         this.aircraftRollRev = false;
      }

      if(!super.worldObj.isRemote && (int)this.prevRotationRoll != (int)this.getRotRoll()) {
         float var8 = MathHelper.wrapAngleTo180_float(this.getRotRoll());
         this.getDataWatcher().updateObject(26, new Short((short)((int)var8)));
      }

      this.prevRotationRoll = this.getRotRoll();
      if(!super.worldObj.isRemote && this.isTargetDrone() && !this.isDestroyed() && this.getCountOnUpdate() > 200 && !this.canUseFuel()) {
         System.out.println("target uav set dead for being idle");
         this.setDamageTaken(this.getMaxHP());
         this.destroyAircraft();
         MCH_Explosion.newExplosion(super.worldObj, (Entity)null, (Entity)null, super.posX, super.posY, super.posZ, 2.0F, 2.0F, true, true, true, true, 5);
      }

      if(super.worldObj.isRemote && this.getAcInfo() != null && this.getHP() <= 0 && this.getDespawnCount() <= 0) {
         this.destroyAircraft();
      }

      if(!super.worldObj.isRemote && this.getDespawnCount() > 0) {
         this.setDespawnCount(this.getDespawnCount() - 1);
         if(this.getDespawnCount() <= 1) {
            //this.clearSearchlightBlocks();
            this.setDead(true);
         }
      }

      updateSearchlightBlocks();

      super.onUpdate();
      if(this.getParts() != null) {
         Entity[] var9 = this.getParts();
         int var10 = var9.length;

         for(int prevOnGround = 0; prevOnGround < var10; ++prevOnGround) {
            Entity prevMotionY = var9[prevOnGround];
            if(prevMotionY != null) {
               prevMotionY.onUpdate();
            }
         }
      }

      this.updateNoCollisionEntities();
      this.updateUAV();
      this.supplyFuel();
      this.supplyAmmoToOtherAircraft();
      this.updateFuel();
      this.repairOtherAircraft();
      if(this.modeSwitchCooldown > 0) {
         --this.modeSwitchCooldown;
      }

      if(this.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
         this.onRidePilotFirstUpdate();
      }

      if(this.countOnUpdate == 0) {
         this.onFirstUpdate();
      }

      ++this.countOnUpdate;
      if(this.countOnUpdate >= 1000000) {
         this.countOnUpdate = 1;
      }

      if(super.worldObj.isRemote) {
         this.commonStatus = this.getDataWatcher().getWatchableObjectInt(23);
      }

      super.fallDistance = 0.0F;
      if(super.riddenByEntity != null) {
         super.riddenByEntity.fallDistance = 0.0F;
      }

      if(this.missileDetector != null) {
         this.missileDetector.update();
      }

      if(this.soundUpdater != null) {
         this.soundUpdater.update();
      }

      if(this.getTowChainEntity() != null && this.getTowChainEntity().isDead) {
         this.setTowChainEntity((MCH_EntityChain)null);
      }

      this.updateSupplyAmmo();

      //MCH_Config var10001 = MCH_MOD.config;


      this.autoRepair();

      var7 = this.getFlareTick();
      this.flareDv.update();
      if(this.getAcInfo() != null && this.chaff != null) {
         this.chaff.chaffUseTime = getAcInfo().chaffUseTime;
         this.chaff.chaffWaitTime = getAcInfo().chaffWaitTime;
         this.chaff.onUpdate();
      }
      if(this.getAcInfo() != null && this.maintenance != null) {
         this.maintenance.useTime = getAcInfo().maintenanceUseTime;
         this.maintenance.waitTime = getAcInfo().maintenanceWaitTime;
         this.maintenance.onUpdate();
      }
      if(this.getAcInfo() != null && this.aps != null) {
         this.aps.configure(getAcInfo().apsUseTime, getAcInfo().apsWaitTime, getAcInfo().apsRange, getAcInfo().apsAmmo);
         this.aps.onUpdate();
      }
      if(!super.worldObj.isRemote && this.getFlareTick() == 0 && var7 != 0) {
         this.setCommonStatus(0, false);
      }

      e = this.getRiddenByEntity();
      if(e != null && !e.isDead && !this.isDestroyed()) {
         this.lastRiderYaw = e.rotationYaw;
         this.prevLastRiderYaw = e.prevRotationYaw;
         this.lastRiderPitch = e.rotationPitch;
         this.prevLastRiderPitch = e.prevRotationPitch;
      } else if(this.getTowedChainEntity() != null || super.ridingEntity != null) {
         this.lastRiderYaw = super.rotationYaw;
         this.prevLastRiderYaw = super.prevRotationYaw;
         this.lastRiderPitch = super.rotationPitch;
         this.prevLastRiderPitch = super.prevRotationPitch;
      }

      this.syncLastRiderAngles();
      this.updatePartCameraRotate();
      this.updatePartWheel();
      this.updatePartCrawlerTrack();
      this.updatePartLightHatch();
      this.regenerationMob();
      if(this.getRiddenByEntity() == null && this.lastRiddenByEntity != null) {
         if(!super.worldObj.isRemote && !this.isUAV() && !this.isNewUAV()
               && this.lastRiddenByEntity instanceof EntityPlayer) {
            MCH_PacketNotifyOnMountEntity.sendDismount(this, (EntityPlayer)this.lastRiddenByEntity);
         }
         this.unmountEntity();
      }

      this.getCalculatedExtraBoundingBoxes();

      //this.updateExtraWheelBoundingBox();

      boolean var11 = super.onGround;
      double var12 = super.motionY;
      double lockedPosX = super.posX;
      double lockedPosY = super.posY;
      double lockedPosZ = super.posZ;
      this.onUpdateAircraft();
      if(this.placementMotionLocked && this.getRiddenByEntity() != null) {
         this.clearPlacementMotionLock();
      }
      if(this.placementMotionLocked && !super.worldObj.isRemote) {
         this.setPosition(lockedPosX, lockedPosY, lockedPosZ);
         this.clearPlacementMotionState();
      }
      this.updateRackLaunchAssist();
      if(this.getAcInfo() != null) {
         this.updateParts(this.getPartStatus());
      }

      if(this.recoilCount > 0) {
         --this.recoilCount;
      }

      if(!W_Entity.isEqual(MCH_MOD.proxy.getClientPlayer(), this.getRiddenByEntity())) {
         this.updateRecoil(1.0F);
      }

      if(!super.worldObj.isRemote && this.isDestroyed() && !this.isExploded() && !var11 && super.onGround && var12 < -0.2D) {
         this.explosionByCrash(var12);
         this.damageSinceDestroyed = this.getMaxHP();
         //basic crash physic system
         //todo maybe mend? change?
      }

      this.onUpdate_PartRotation();
      this.onUpdate_ParticleSmoke();
      this.updateSeatsPosition(super.posX, super.posY, super.posZ, false);
      this.updateHitBoxPosition();
      this.onUpdate_CollisionGroundDamage();
      this.onUpdate_UnmountCrew();
      this.onUpdate_Repelling();
      this.checkRideRack();
      if(this.lastRidingEntity == null && this.getRidingEntity() != null) {
         this.onRideEntity(this.getRidingEntity());
      }

      this.lastRiddenByEntity = this.getRiddenByEntity();
      this.lastRidingEntity = this.getRidingEntity();
      this.prevPosition.put(Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
   }

   private void updateSearchlightBlocks() {
      if (worldObj.isRemote) return;

      Set<ChunkCoordinates> newLights = new HashSet<ChunkCoordinates>();

      // Only place / refresh lights if the searchlight is actually on
      if (haveSearchLight() && isSearchLightON()) {
         for (Object o : this.getAcInfo().searchLights) {
            MCH_BaseVehicleInfo.SearchLight sl = (MCH_BaseVehicleInfo.SearchLight) o;
            Vec3 p = getTransformedPosition(sl.pos);

            int bx = MathHelper.floor_double(p.xCoord);
            int by = MathHelper.floor_double(p.yCoord);
            int bz = MathHelper.floor_double(p.zCoord);

            ChunkCoordinates coord = new ChunkCoordinates(bx, by, bz);
            newLights.add(coord);

            // Place the light block if needed
            if (worldObj.isAirBlock(bx, by, bz)) {
               worldObj.setBlock(bx, by, bz, MCH_MOD.lightBlock, 0, 2);
               worldObj.markBlockForUpdate(bx, by, bz);
               worldObj.updateLightByType(EnumSkyBlock.Block, bx, by, bz);
            }

            // Immediately track it (one-shot safe)
            activeLights.add(coord);

            // Assign / refresh tile entity ownership
            TileEntity te = worldObj.getTileEntity(bx, by, bz);
            if (te instanceof TileEntitySearchlight) {
               TileEntitySearchlight slte = (TileEntitySearchlight) te;
               slte.setOwner(this.getEntityId());
               slte.refreshLastSeen();
            }
         }
      }

      // Remove lights that are no longer part of this aircraft's searchlight set
      for (ChunkCoordinates oldCoord : activeLights) {
         if (!newLights.contains(oldCoord)) {
            int x = oldCoord.posX;
            int y = oldCoord.posY;
            int z = oldCoord.posZ;

            if (worldObj.getBlock(x, y, z) == MCH_MOD.lightBlock) {
               worldObj.setBlockToAir(x, y, z);
               worldObj.markBlockForUpdate(x, y, z);
               worldObj.updateLightByType(EnumSkyBlock.Block, x, y, z);
            }
         }
      }

      // Replace active set only AFTER cleanup
      activeLights.clear();
      activeLights.addAll(newLights);
   }


   private void clearSearchlightBlocks() {
      if (worldObj.isRemote) return;

      for (ChunkCoordinates coord : activeLights) {
         int x = coord.posX;
         int y = coord.posY;
         int z = coord.posZ;

         if (worldObj.getBlock(x, y, z) == MCH_MOD.lightBlock) {
            worldObj.setBlockToAir(x, y, z);
            worldObj.markBlockForUpdate(x, y, z);
            worldObj.updateLightByType(EnumSkyBlock.Block, x, y, z);
         }
      }

      activeLights.clear();
   }

   private void hardClearSearchlights() {
      if (worldObj.isRemote) return;

      int r = 12; // search radius, adjust if needed
      int cx = MathHelper.floor_double(posX);
      int cy = MathHelper.floor_double(posY);
      int cz = MathHelper.floor_double(posZ);

      for (int x = cx - r; x <= cx + r; x++) {
         for (int y = cy - r; y <= cy + r; y++) {
            for (int z = cz - r; z <= cz + r; z++) {
               if (worldObj.getBlock(x, y, z) == MCH_MOD.lightBlock) {
                  worldObj.setBlockToAir(x, y, z);
                  worldObj.markBlockForUpdate(x, y, z);
                  worldObj.updateLightByType(EnumSkyBlock.Block, x, y, z);
               }
            }
         }
      }
   }



   private void updateNoCollisionEntities() {
      if(!super.worldObj.isRemote) {
         if(this.getCountOnUpdate() % 10 == 0) {
            Entity key1;
            for(int key = 0; key < 1 + this.getSeatNum(); ++key) {
               key1 = this.getEntityBySeatId(key);
               if(key1 != null) {
                  this.noCollisionEntities.put(key1, Integer.valueOf(8));
               }
            }

            if(this.getTowChainEntity() != null && this.getTowChainEntity().towedEntity != null) {
               this.noCollisionEntities.put(this.getTowChainEntity().towedEntity, 60);
            }

            if(this.getTowedChainEntity() != null && this.getTowedChainEntity().towEntity != null) {
               this.noCollisionEntities.put(this.getTowedChainEntity().towEntity, 60);
            }

            if(super.ridingEntity instanceof MCH_EntitySeat) {
               MCH_EntityBaseVehicle var3 = ((MCH_EntitySeat)super.ridingEntity).getParent();
               if(var3 != null) {
                  this.noCollisionEntities.put(var3, 60);
               }
            } else if(super.ridingEntity != null) {
               this.noCollisionEntities.put(super.ridingEntity, 60);
            }

            Iterator var4 = this.noCollisionEntities.keySet().iterator();

            while(var4.hasNext()) {
               key1 = (Entity)var4.next();
               this.noCollisionEntities.put(key1, (Integer) this.noCollisionEntities.get(key1) - 1);
            }

            var4 = this.noCollisionEntities.values().iterator();

            while(var4.hasNext()) {
               if(((Integer)var4.next()).intValue() <= 0) {
                  var4.remove();
               }
            }

         }
      }
   }


   private void updateDelayedUavInventoryStore() {
      if(super.worldObj.isRemote || !this.isNewUAV()) {
         return;
      }
      Entity rider = super.riddenByEntity;
      if(rider instanceof EntityPlayerMP) {
         EntityPlayerMP player = (EntityPlayerMP)rider;
         if(MCH_UavInventory.hasStoredPilotInventory(player)) {
            return;
         }
         updateNewUavReturnPositionFromStation();
         if(hasNewUavReturnPosition()) {
            double dx = player.posX - this.linkedUavStationX;
            double dy = player.posY - this.linkedUavStationY;
            double dz = player.posZ - this.linkedUavStationZ;
            if(dx * dx + dy * dy + dz * dz > 225.0D) {
               MCH_UavInventory.storeAndClearPilotInventory(player, this.getUniqueID().toString());
               player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "You moved more than 15 blocks from the UAV station. Your inventory has been stored until drone control ends."));
               return;
            }
         }
         ++this.delayedUavInventoryTicks;
         if(this.delayedUavInventoryTicks == 1) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "You have 10 seconds left to refuel or rearm the drone before your inventory is stored."));
         } else if(this.delayedUavInventoryTicks == 100) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "You have 5 seconds left to refuel or rearm the drone before your inventory is stored."));
         } else if(this.delayedUavInventoryTicks == 200) {
            MCH_UavInventory.storeAndClearPilotInventory(player, this.getUniqueID().toString());
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "You have no more time left to refuel or rearm the drone. Your inventory has been stored until drone control ends."));
         }
      } else {
         this.delayedUavInventoryTicks = 0;
      }
   }

   public void updateControl() {
      if(!super.worldObj.isRemote) {
         updateDelayedUavInventoryStore();
         updateNewUavMountSynchronization();

         if(this.uavStation != null && !this.uavStation.isDead) {
            this.saveUavStationPosition(this.uavStation);
            if(this.isNewUAV()) {
               this.uavStation.updateLinkedUavPosition(this);
            }
         }

         this.setCommonStatus(7, this.moveLeft);
         this.setCommonStatus(8, this.moveRight);
         this.setCommonStatus(9, this.throttleUp);
         this.setCommonStatus(10, this.throttleDown);
      } else if(MCH_MOD.proxy.getClientPlayer() != this.getRiddenByEntity()) {
         this.moveLeft = this.getCommonStatus(7);
         this.moveRight = this.getCommonStatus(8);
         this.throttleUp = this.getCommonStatus(9);
         this.throttleDown = this.getCommonStatus(10);
      }

   }

   private void updateNewUavMountSynchronization() {
      if(!this.isNewUAV()) {
         return;
      }
      Entity rider = super.riddenByEntity;
      if(this.newUavMountSyncTicks <= 0 && rider instanceof EntityPlayerMP
            && rider.ridingEntity == this && this.lastRiddenByEntity != rider) {
         // Rider relationships are restored separately from entity NBT. Start the same
         // synchronization window when a relog/restart restores the direct NewUAV rider.
         this.newUavMountSyncTicks = 40;
      }
      if(this.newUavMountSyncTicks <= 0) {
         return;
      }
      if(!(rider instanceof EntityPlayerMP) || rider.ridingEntity != this) {
         this.newUavMountSyncTicks = 0;
         return;
      }
      if(this.newUavMountSyncTicks % 5 == 0) {
         // The player starts at a distant station, so the first attach packet can arrive
         // before the client has begun tracking this aircraft. Repeat the authoritative
         // state after tracking catches up instead of teleporting the player ahead of mount.
         this.syncCompleteAircraftState((EntityPlayerMP)rider);
      }
      --this.newUavMountSyncTicks;
   }

   public void updateRecoil(float partialTicks) {
      if(this.recoilCount > 0 && this.recoilCount >= 12) {
         float pitch = MathHelper.cos((float)((double)(this.recoilYaw - this.getRotRoll()) * 3.141592653589793D / 180.0D));
         float roll = MathHelper.sin((float)((double)(this.recoilYaw - this.getRotRoll()) * 3.141592653589793D / 180.0D));
         float recoil = MathHelper.cos((float)((double)(this.recoilCount * 6) * 3.141592653589793D / 180.0D)) * this.recoilValue;
         this.setRotPitch(this.getRotPitch() + recoil * pitch * partialTicks);
         this.setRotRoll(this.getRotRoll() + recoil * roll * partialTicks);
      }

   }

   private void updatePartLightHatch() {
      this.prevRotLightHatch = this.rotLightHatch;
      if(this.isSearchLightON()) {
         this.rotLightHatch = (float)((double)this.rotLightHatch + 0.5D);
      } else {
         this.rotLightHatch = (float)((double)this.rotLightHatch - 0.5D);
      }

      if(this.rotLightHatch > 1.0F) {
         this.rotLightHatch = 1.0F;
      }

      if(this.rotLightHatch < 0.0F) {
         this.rotLightHatch = 0.0F;
      }

   }


   protected void updateGroundVehicleFallDamage(boolean wasOnGroundBeforeMove, double motionYBeforeGravity, double motionYBeforeMove) {
      if(super.worldObj.isRemote || this.isDestroyed()) {
         return;
      }

      double downwardAcceleration = Math.max(0.0D, motionYBeforeGravity - motionYBeforeMove);
      double gravity = this.getAcInfo() != null?Math.abs((double)(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater)):0.04D;
      if(gravity < 1.0E-4D) {
         gravity = 0.04D;
      }

      if(!super.onGround) {
         if(!wasOnGroundBeforeMove && this.groundVehicleFallStartY == Double.MAX_VALUE) {
            this.groundVehicleFallStartY = super.prevPosY;
         }

         if(this.groundVehicleFallStartY == Double.MAX_VALUE) {
            this.groundVehicleFallStartY = super.posY;
         }

         this.groundVehicleMaxFallSpeed = Math.max(this.groundVehicleMaxFallSpeed, Math.max(0.0D, -motionYBeforeMove));
         this.groundVehicleMaxDownwardAcceleration = Math.max(this.groundVehicleMaxDownwardAcceleration, downwardAcceleration);
         return;
      }

      if(!wasOnGroundBeforeMove && this.groundVehicleFallStartY != Double.MAX_VALUE) {
         double dropDistance = Math.max(0.0D, this.groundVehicleFallStartY - super.posY);
         double speedDistance = this.groundVehicleMaxFallSpeed * this.groundVehicleMaxFallSpeed / (2.0D * gravity);
         double effectiveDistance = Math.max(dropDistance, speedDistance);
         if(effectiveDistance > 3.0D) {
            double healthRatio = this.getMaxHP() > 0?(double)this.getHP() / (double)this.getMaxHP():1.0D;
            double healthFactor = 1.0D + (1.0D - MathHelper.clamp_double(healthRatio, 0.0D, 1.0D)) * 0.5D;
            double gravityFactor = MathHelper.clamp_double(gravity / 0.04D, 0.5D, 2.0D);
            double accelerationFactor = MathHelper.clamp_double(this.groundVehicleMaxDownwardAcceleration / gravity, 0.75D, 2.0D);
            float damage = (float)((effectiveDistance - 3.0D) * 2.0D * healthFactor * gravityFactor * accelerationFactor);
            if(damage > 0.0F) {
               this.attackEntityFrom(DamageSource.fall, damage);
            }
         }
      }

      this.groundVehicleFallStartY = Double.MAX_VALUE;
      this.groundVehicleMaxFallSpeed = 0.0D;
      this.groundVehicleMaxDownwardAcceleration = 0.0D;
   }

   public void updateExtraBoundingBox() {
      this.markVehicleBoxCacheDirty("legacy updateExtraBoundingBox request");
      this.getCalculatedExtraBoundingBoxes();
   }

   /**
    * Returns the current calculated extra collision / hit boxes for this vehicle.
    *
    * This method returns oriented MCH_BoundingBox instances with enclosing AxisAlignedBBs
    * for vanilla broad-phase compatibility. It centralizes transform calculation and caches
    * stationary vehicles so collision, damage, and debug-render callers share the same geometry.
    */
   public MCH_BoundingBox[] getCalculatedExtraBoundingBoxes() {
      return this.vehicleBoxCache.getBoxes(this);
   }

   public void markVehicleBoxCacheDirty(String reason) {
      this.vehicleBoxCache.markDirty(reason);
   }

   //public void updateExtraWheelBoundingBox() {
   //   //wheelBoundingBox[] arr2$ = this.extrawheelboundingbox;
   //   int len2$ = arr2$.length;
//
   //   for(int i2$ = 0; i2$ < len2$; ++i2$) {
   //      wheelBoundingBox bb2 = arr2$[i2$];
   //      bb2.updatePosition(super.posX, super.posY, super.posZ, this.getRotYaw(), this.getRotPitch(), this.getRotRoll());
   //   }
//
   //}

   public void updatePartWheel() {
      if(super.worldObj.isRemote) {
         if(this.getAcInfo() != null) {
            this.updatePartAnimationTravel();
            this.prevRotWheel = this.rotWheel;
            this.prevRotYawWheel = this.rotYawWheel;
            boolean localMoveLeft = this.moveLeft;
            boolean localMoveRight = this.moveRight;

            if(localMoveLeft && !localMoveRight) {
               this.rotYawWheel += 0.1F;
               if(this.rotYawWheel > 1.0F) {
                  this.rotYawWheel = 1.0F;
               }
            } else if(!localMoveLeft && localMoveRight) {
               this.rotYawWheel -= 0.1F;
               if(this.rotYawWheel < -1.0F) {
                  this.rotYawWheel = -1.0F;
               }
            } else {
               this.rotYawWheel *= 0.9F;
            }

            this.rotWheel += (float)(this.partAnimationForwardTravel * (double)this.getAcInfo().partWheelRot);
            this.wrapWheelAngle();

         }
      }
   }

   public void updatePartCrawlerTrack() {
      if(super.worldObj.isRemote) {
         if(this.getAcInfo() != null) {
            this.prevRotTrackRoller[0] = this.rotTrackRoller[0];
            this.prevRotTrackRoller[1] = this.rotTrackRoller[1];
            this.prevRotCrawlerTrack[0] = this.rotCrawlerTrack[0];
            this.prevRotCrawlerTrack[1] = this.rotCrawlerTrack[1];
            this.throttleCrawlerTrack[0] = (float)(this.partAnimationForwardTravel + this.partAnimationYawTravel);
            this.throttleCrawlerTrack[1] = (float)(this.partAnimationForwardTravel - this.partAnimationYawTravel);

            for(int var11 = 0; var11 < 2; ++var11) {
               this.rotTrackRoller[var11] += this.throttleCrawlerTrack[var11] * this.getAcInfo().trackRollerRot;
               while(this.rotTrackRoller[var11] >= 360.0F) {
                  this.rotTrackRoller[var11] -= 360.0F;
                  this.prevRotTrackRoller[var11] -= 360.0F;
               }
               while(this.rotTrackRoller[var11] < 0.0F) {
                  this.rotTrackRoller[var11] += 360.0F;
                  this.prevRotTrackRoller[var11] += 360.0F;
               }

               for(this.rotCrawlerTrack[var11] -= this.throttleCrawlerTrack[var11]; this.rotCrawlerTrack[var11] >= 1.0F; --this.prevRotCrawlerTrack[var11]) {
                  --this.rotCrawlerTrack[var11];
               }

               while(this.rotCrawlerTrack[var11] < 0.0F) {
                  ++this.rotCrawlerTrack[var11];
               }

               while(this.prevRotCrawlerTrack[var11] < 0.0F) {
                  ++this.prevRotCrawlerTrack[var11];
               }

            }

         }
      }
   }

   private void updatePartAnimationTravel() {
      if(!this.partAnimationPositionInitialized) {
         this.partAnimationPositionInitialized = true;
         this.lastPartAnimationPosX = super.posX;
         this.lastPartAnimationPosZ = super.posZ;
         this.lastPartAnimationYaw = this.getRotYaw();
         this.partAnimationForwardTravel = 0.0D;
         this.partAnimationYawTravel = 0.0D;
         return;
      }

      double dx = super.posX - this.lastPartAnimationPosX;
      double dz = super.posZ - this.lastPartAnimationPosZ;
      float yawChange = MathHelper.wrapAngleTo180_float(this.getRotYaw() - this.lastPartAnimationYaw);
      float middleYaw = this.lastPartAnimationYaw + yawChange * 0.5F;
      double yawRadians = (double)middleYaw * Math.PI / 180.0D;
      double travel = dx * -Math.sin(yawRadians) + dz * Math.cos(yawRadians);
      if(dx * dx + dz * dz < 1.0E-6D) {
         travel = 0.0D;
      }

      this.partAnimationForwardTravel = travel;
      this.partAnimationYawTravel = (double)yawChange * Math.PI / 180.0D * this.getTrackHalfWidth();
      this.lastPartAnimationPosX = super.posX;
      this.lastPartAnimationPosZ = super.posZ;
      this.lastPartAnimationYaw = this.getRotYaw();
   }

   private double getTrackHalfWidth() {
      double halfWidth = 0.0D;
      Iterator i$ = this.getAcInfo().wheels.iterator();
      while(i$.hasNext()) {
         MCH_BaseVehicleInfo.Wheel wheel = (MCH_BaseVehicleInfo.Wheel)i$.next();
         halfWidth = Math.max(halfWidth, Math.abs(wheel.pos.xCoord));
      }
      return halfWidth > 0.01D?halfWidth:Math.max(0.5D, (double)this.getAcInfo().bodyWidth * 0.5D);
   }

   private void wrapWheelAngle() {
      while(this.rotWheel >= 360.0F) {
         this.rotWheel -= 360.0F;
         this.prevRotWheel -= 360.0F;
      }
      while(this.rotWheel < 0.0F) {
         this.rotWheel += 360.0F;
         this.prevRotWheel += 360.0F;
      }
   }

   public void checkServerNoMove() {
      if(!super.worldObj.isRemote) {
         double moti = super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ;
         if(moti < 1.0E-4D) {
            if(this.serverNoMoveCount < 20) {
               ++this.serverNoMoveCount;
               if(this.serverNoMoveCount >= 20) {
                  this.serverNoMoveCount = 0;
                  if(super.worldObj instanceof WorldServer) {
                     ((WorldServer)super.worldObj).getEntityTracker().func_151247_a(this, new S12PacketEntityVelocity(this.getEntityId(), 0.0D, 0.0D, 0.0D));
                  }
               }
            }
         } else {
            this.serverNoMoveCount = 0;
         }
      }

   }

   public boolean haveRotPart() {
      return super.worldObj.isRemote && this.getAcInfo() != null && this.rotPartRotation.length > 0 && this.rotPartRotation.length == this.getAcInfo().partRotPart.size();
   }

   public void onUpdate_PartRotation() {
      if(this.haveRotPart()) {
         for(int i = 0; i < this.rotPartRotation.length; ++i) {
            this.prevRotPartRotation[i] = this.rotPartRotation[i];
            if(!this.isDestroyed() && ((MCH_BaseVehicleInfo.RotPart)this.getAcInfo().partRotPart.get(i)).rotAlways || this.getRiddenByEntity() != null) {
               this.rotPartRotation[i] += ((MCH_BaseVehicleInfo.RotPart)this.getAcInfo().partRotPart.get(i)).rotSpeed;
               if(this.rotPartRotation[i] < 0.0F) {
                  this.rotPartRotation[i] += 360.0F;
               }

               if(this.rotPartRotation[i] >= 360.0F) {
                  this.rotPartRotation[i] -= 360.0F;
               }
            }
         }
      }

   }

   public void onRideEntity(Entity ridingEntity) {}

   public int getAlt(double px, double py, double pz) {
      int i;
      for(i = 0; i < 256 && py - (double)i > 0.0D && (py - (double)i >= 256.0D || 0 == W_WorldFunc.getBlockId(super.worldObj, (int)px, (int)py - i, (int)pz)); ++i) {
         ;
      }

      return i;
   }

   public boolean canRepelling(Entity entity) {
      return this.isRepelling() && this.tickRepelling > 50;
   }

   private void onUpdate_Repelling() {
      if(this.getAcInfo() != null && this.getAcInfo().haveRepellingHook()) {
         if(this.isRepelling()) {
            int alt = this.getAlt(super.posX, super.posY, super.posZ);
            if(this.ropesLength > -50.0F && this.ropesLength > (float)(-alt)) {
               this.ropesLength = (float)((double)this.ropesLength - (super.worldObj.isRemote?0.30000001192092896D:0.25D));
            }
         } else {
            this.ropesLength = 0.0F;
         }
      }

      this.onUpdate_UnmountCrewRepelling();
   }

   private void onUpdate_UnmountCrewRepelling() {
      if(this.getAcInfo() != null) {
         if(!this.isRepelling()) {
            this.tickRepelling = 0;
         } else if(this.tickRepelling < 60) {
            ++this.tickRepelling;
         } else if(!super.worldObj.isRemote) {
            for(int ropeIdx = 0; ropeIdx < this.getAcInfo().repellingHooks.size(); ++ropeIdx) {
               MCH_BaseVehicleInfo.RepellingHook hook = (MCH_BaseVehicleInfo.RepellingHook)this.getAcInfo().repellingHooks.get(ropeIdx);
               if(this.getCountOnUpdate() % hook.interval == 0) {
                  for(int i = 1; i < this.getSeatNum(); ++i) {
                     MCH_EntitySeat seat = this.getSeat(i);
                     if(seat != null && seat.riddenByEntity != null && !W_EntityPlayer.isPlayer(seat.riddenByEntity) && !(this.getSeatInfo(i + 1) instanceof MCH_SeatRackInfo) && !(seat.riddenByEntity instanceof MCH_EntityGunner)) {
                        //todo entity gunner hmg style ai
                        Entity entity = seat.riddenByEntity;
                        Vec3 dropPos = this.getTransformedPosition(hook.pos, (Vec3)this.prevPosition.oldest());
                        seat.posX = dropPos.xCoord;
                        seat.posY = dropPos.yCoord - 2.0D;
                        seat.posZ = dropPos.zCoord;
                        entity.mountEntity((Entity)null);
                        this.unmountEntityRepelling(entity, dropPos, ropeIdx);
                        this.lastUsedRopeIndex = ropeIdx;
                        break;
                     }
                  }
               }
            }

         }
      }
   }

   public void unmountEntityRepelling(Entity entity, Vec3 dropPos, int ropeIdx) {
      entity.posX = dropPos.xCoord;
      entity.posY = dropPos.yCoord - 2.0D;
      entity.posZ = dropPos.zCoord;
      MCH_EntityHide hideEntity = new MCH_EntityHide(super.worldObj, entity.posX, entity.posY, entity.posZ);
      hideEntity.setParent(this, entity, ropeIdx);
      hideEntity.motionX = entity.motionX = 0.0D;
      hideEntity.motionY = entity.motionY = 0.0D;
      hideEntity.motionZ = entity.motionZ = 0.0D;
      hideEntity.fallDistance = entity.fallDistance = 0.0F;
      super.worldObj.spawnEntityInWorld(hideEntity);
   }

   private void onUpdate_UnmountCrew() {
      if(this.getAcInfo() != null) {
         if(this.isParachuting) {
            if(MCH_Lib.getBlockIdY(this, 3, -10) != 0) {
               this.stopUnmountCrew();
            } else if((!this.haveHatch() || this.getHatchRotation() > 89.0F) && this.getCountOnUpdate() % this.getAcInfo().mobDropOption.interval == 0 && !this.unmountCrew(true)) {
               this.stopUnmountCrew();
            }
         }

      }
   }

   public void unmountAircraft() {
      //if this is a newUAV go back to the station pos.
      Vec3 v = Vec3.createVectorHelper(super.posX, super.posY, super.posZ);
      float yaw = this.getRotYaw();
      float pitch = this.getRotPitch();
      MCH_EntityBaseVehicle rackParent = null;
      MCH_SeatRackInfo rackInfo = null;
      if(super.ridingEntity instanceof MCH_EntitySeat) {
         MCH_EntityBaseVehicle ac = ((MCH_EntitySeat)super.ridingEntity).getParent();
         MCH_SeatInfo seatInfo = ac != null?ac.getSeatInfo(this):null;
         if(seatInfo instanceof MCH_SeatRackInfo) {
            rackParent = ac;
            rackInfo = (MCH_SeatRackInfo)seatInfo;
            Vec3 rackUnmountPosition = ac.getRackUnmountPosition(rackInfo);
            if(rackUnmountPosition != null) {
               v = rackUnmountPosition;
               yaw = ac.getRotYaw() + rackInfo.fixYaw;
               pitch = rackInfo.fixPitch;
            } else {
               v = ac.getTransformedPosition(rackInfo.getEntryPos());
            }
         }
      } else if(super.ridingEntity instanceof EntityMinecartEmpty) {
         this.dismountedUserCtrl = true;
      }

      if (this.isNewUAV()) {
         System.out.println("unmountaircraft method working");
         this.mountEntity((Entity) null);
         //TODO GET UAV STATION POSITION HERE
         //this.setLocationAndAngles(getUavStation().uav);
      } else {
         this.setLocationAndAngles(v.xCoord, v.yCoord, v.zCoord, yaw, pitch);
         this.mountEntity((Entity) null);
         this.setLocationAndAngles(v.xCoord, v.yCoord, v.zCoord, yaw, pitch);
         if(rackParent != null && rackInfo != null) {
            this.applyRackLaunch(rackParent, rackInfo);
         }
      }
   }

   public boolean canUnmount(Entity entity) {
      return this.getAcInfo() == null?false:(!this.getAcInfo().isEnableParachuting?false:(this.getSeatIdByEntity(entity) <= 1?false:!this.haveHatch() || this.getHatchRotation() >= 89.0F));
   }

   public void unmount(Entity entity) {
      //same here (maybe?) sir actually nvm this is parachuting unmount lololo
      if(this.getAcInfo() != null) {
         MCH_EntitySeat seat;
         Vec3 dropPos;
         if(this.canRepelling(entity) && this.getAcInfo().haveRepellingHook()) {
            seat = this.getSeatByEntity(entity);
            if(seat != null) {
               this.lastUsedRopeIndex = (this.lastUsedRopeIndex + 1) % this.getAcInfo().repellingHooks.size();
               dropPos = this.getTransformedPosition(((MCH_BaseVehicleInfo.RepellingHook)this.getAcInfo().repellingHooks.get(this.lastUsedRopeIndex)).pos, (Vec3)this.prevPosition.oldest());
               dropPos = dropPos.addVector(0.0D, -2.0D, 0.0D);
               seat.posX = dropPos.xCoord;
               seat.posY = dropPos.yCoord;
               seat.posZ = dropPos.zCoord;
               entity.mountEntity((Entity)null);
               entity.posX = dropPos.xCoord;
               entity.posY = dropPos.yCoord;
               entity.posZ = dropPos.zCoord;
               this.unmountEntityRepelling(entity, dropPos, this.lastUsedRopeIndex);
            } else {
               MCH_Lib.Log((Entity)this, "Error:MCH_EntityBaseVehicle.unmount seat=null : " + entity, new Object[0]);
            }
         } else if(this.canUnmount(entity)) {
            seat = this.getSeatByEntity(entity);
            if(seat != null) {
               dropPos = this.getTransformedPosition(this.getAcInfo().mobDropOption.pos, (Vec3)this.prevPosition.oldest());
               seat.posX = dropPos.xCoord;
               seat.posY = dropPos.yCoord;
               seat.posZ = dropPos.zCoord;
               entity.mountEntity((Entity)null);
               entity.posX = dropPos.xCoord;
               entity.posY = dropPos.yCoord;
               entity.posZ = dropPos.zCoord;
               this.dropEntityParachute(entity);
            } else {
               MCH_Lib.Log((Entity)this, "Error:MCH_EntityBaseVehicle.unmount seat=null : " + entity, new Object[0]);
            }
         }

      }
   }

   public boolean canParachuting(Entity entity) {
      return this.getAcInfo() != null && this.getAcInfo().isEnableParachuting && this.getSeatIdByEntity(entity) > 1 && MCH_Lib.getBlockIdY(this, 3, -13) == 0?(this.haveHatch() && this.getHatchRotation() > 89.0F?this.getSeatIdByEntity(entity) > 1:this.getSeatIdByEntity(entity) > 1):false;
   }

   public MCH_EntityBaseVehicle getRackParent() {
      if(super.ridingEntity instanceof MCH_EntitySeat) {
         MCH_EntitySeat seat = (MCH_EntitySeat)super.ridingEntity;
         MCH_EntityBaseVehicle parent = seat.getParent();
         if(parent != null && parent.getSeatInfo(seat.seatID + 1) instanceof MCH_SeatRackInfo) {
            return parent;
         }
      }
      return null;
   }

   public boolean isMountedOnRack() {
      return this.getRackParent() != null;
   }

   private void restoreRackMountAfterLoad() {
      if(this.isMountedOnRack()) {
         this.pendingRackParentUniqueId = "";
         this.pendingRackSeatId = -1;
         this.pendingRackRestoreTicks = 0;
         return;
      }
      if(super.worldObj.isRemote || this.pendingRackRestoreTicks <= 0 || this.pendingRackParentUniqueId.isEmpty()) {
         return;
      }
      --this.pendingRackRestoreTicks;
      this.setPosition(this.pendingRackPosX, this.pendingRackPosY, this.pendingRackPosZ);
      this.setVelocity(0.0D, 0.0D, 0.0D);
      for(Object object : super.worldObj.loadedEntityList) {
         if(object instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle parent = (MCH_EntityBaseVehicle)object;
            if(this.pendingRackParentUniqueId.equals(parent.getCommonUniqueId())) {
               parent.repairInvalidOccupantsForInteraction("rack_restore");
               parent.searchSeat();
               this.noCollisionEntities.put(parent, Integer.valueOf(10));
               parent.noCollisionEntities.put(this, Integer.valueOf(10));
               MCH_EntitySeat seat = parent.getSeat(this.pendingRackSeatId);
               if(seat != null && (seat.riddenByEntity == null || seat.riddenByEntity == this)
                       && parent.getSeatInfo(this.pendingRackSeatId + 1) instanceof MCH_SeatRackInfo) {
                  this.mountEntity(seat);
                  this.pendingRackParentUniqueId = "";
                  this.pendingRackSeatId = -1;
                  this.pendingRackRestoreTicks = 0;
               }
               return;
            }
         }
      }
      if(this.pendingRackRestoreTicks == 0) {
         this.pendingRackParentUniqueId = "";
         this.pendingRackSeatId = -1;
      }
   }

   public void onUpdate_RidingEntity() {
      this.restoreRackMountAfterLoad();
      if(!super.worldObj.isRemote && this.waitMountEntity == 0 && this.getCountOnUpdate() > 20 && this.canMountWithNearEmptyMinecart()) {
         this.mountWithNearEmptyMinecart();
      }

      if(this.waitMountEntity > 0) {
         --this.waitMountEntity;
      }

      if(!super.worldObj.isRemote && this.getRidingEntity() != null) {
         this.setRotRoll(this.getRotRoll() * 0.9F);
         this.setRotPitch(this.getRotPitch() * 0.95F);
         Entity re = this.getRidingEntity();
         float target = MathHelper.wrapAngleTo180_float(re.rotationYaw + 90.0F);
         if(target - super.rotationYaw > 180.0F) {
            target -= 360.0F;
         }

         if(target - super.rotationYaw < -180.0F) {
            target += 360.0F;
         }

         if(super.ticksExisted % 2 == 0) {
            ;
         }

         float dist = 50.0F * (float)re.getDistanceSq(re.prevPosX, re.prevPosY, re.prevPosZ);
         if((double)dist > 0.001D) {
            dist = MathHelper.sqrt_double((double)dist);
            float bkPosX = MCH_Lib.RNG(target - super.rotationYaw, -dist, dist);
            super.rotationYaw += bkPosX;
         }

         double var10 = super.posX;
         double bkPosY = super.posY;
         double bkPosZ = super.posZ;
         if(this.getRidingEntity().isDead) {
            this.mountEntity((Entity)null);
            this.waitMountEntity = 20;
         } else if(super.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntityBaseVehicle parent = ((MCH_EntitySeat)super.ridingEntity).getParent();
            if(parent != this.rackMountParent) {
               this.rackMountParent = parent;
               this.rackThrottleInput = false;
            }
            if(this.throttleUp || this.throttleDown) {
               this.rackThrottleInput = true;
            }
         }

         super.posX = var10;
         super.posY = bkPosY;
         super.posZ = bkPosZ;
      }

   }

   private boolean isLaunchRack(MCH_EntityBaseVehicle parent, MCH_SeatRackInfo rackInfo) {
      return parent instanceof MCH_EntityShip || rackInfo.launchRack;
   }

   private void applyRackLaunch(MCH_EntityBaseVehicle parent, MCH_SeatRackInfo rackInfo) {
      if(this.throttleUp || this.throttleDown) {
         this.rackThrottleInput = true;
      }
      if(!this.isLaunchRack(parent, rackInfo)) {
         this.rackMountParent = null;
         this.rackThrottleInput = false;
         return;
      }

      if(!this.rackThrottleInput && this.getCurrentThrottle() < 0.9D) {
         this.setCurrentThrottle(0.9D);
      }

      // Values in noCollisionEntities are reduced once every ten ticks.
      // Ten gives this aircraft and only its launching parent a five-second grace period.
      this.noCollisionEntities.put(parent, Integer.valueOf(10));
      parent.noCollisionEntities.put(this, Integer.valueOf(10));

      double throttle = MathHelper.clamp_double(this.getCurrentThrottle(), 0.0D, 1.0D);
      double launchSpeed;
      double verticalAssist = 0.0D;
      if(this instanceof MCP_EntityPlane) {
         launchSpeed = 0.9D + throttle * 1.1D;
         verticalAssist = 0.08D + throttle * 0.04D;
      } else if(this instanceof MCH_EntityHeli) {
         launchSpeed = 0.35D + throttle * 0.45D;
         verticalAssist = 0.12D + throttle * 0.08D;
      } else {
         launchSpeed = 0.4D + throttle * 0.4D;
      }

      Vec3 forward = MCH_Lib.Rot2Vec3(parent.getRotYaw() + rackInfo.fixYaw, rackInfo.fixPitch);
      this.rackLaunchVelocityX = forward.xCoord * launchSpeed;
      this.rackLaunchVelocityZ = forward.zCoord * launchSpeed;
      this.rackLaunchAssistTicks = 20;
      this.currentSpeed = Math.max(this.currentSpeed, launchSpeed);
      this.setVelocity(parent.motionX + this.rackLaunchVelocityX,
              parent.motionY + verticalAssist, parent.motionZ + this.rackLaunchVelocityZ);
      this.fallDistance = 0.0F;
      this.rackMountParent = null;
      this.rackThrottleInput = false;
   }

   private void updateRackLaunchAssist() {
      if(this.rackLaunchAssistTicks <= 0 || this.isMountedOnRack()) {
         return;
      }
      --this.rackLaunchAssistTicks;
      double assistSpeed = Math.sqrt(this.rackLaunchVelocityX * this.rackLaunchVelocityX
              + this.rackLaunchVelocityZ * this.rackLaunchVelocityZ);
      if(assistSpeed > 0.0D) {
         double forwardX = this.rackLaunchVelocityX / assistSpeed;
         double forwardZ = this.rackLaunchVelocityZ / assistSpeed;
         double forwardSpeed = super.motionX * forwardX + super.motionZ * forwardZ;
         if(forwardSpeed < assistSpeed) {
            double missingSpeed = assistSpeed - forwardSpeed;
            super.motionX += forwardX * missingSpeed;
            super.motionZ += forwardZ * missingSpeed;
         }
      }
   }

   public void explosionByCrash(double prevMotionY) {
      float exp = getAcInfo().explosionSizeByCrash;
      MCH_Lib.DbgLog(super.worldObj, "OnGroundAfterDestroyed:motionY=%.3f", new Object[]{Float.valueOf((float)prevMotionY)});
      MCH_Explosion.newExplosion(super.worldObj, (Entity)null, (Entity)null, super.posX, super.posY, super.posZ, exp, exp >= 2.0F?exp * 0.5F:1.0F, true, true, true, true, 5);
   }

   public void onUpdate_CollisionGroundDamage() {
      if(this.pendingRackRestoreTicks > 0) {
         return;
      }
      if(!this.isDestroyed()) {
         //this method for crash collision detection just fucking sucks but it works
         if(MCH_Lib.getBlockIdY(this, 3, -3) > 0 && !super.worldObj.isRemote) {
            float hp = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotRoll()));
            float pitch = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotPitch()));
            if(hp > this.getGiveDamageRot() || pitch > this.getGiveDamageRot()) {
               float dmg = MathHelper.abs(hp) + MathHelper.abs(pitch);
               if(dmg < 90.0F) {
                  dmg *= 0.4F * (float)this.getDistance(super.prevPosX, super.prevPosY, super.prevPosZ);
               } else {
                  dmg *= 0.4F;
               }

               if(dmg > 1.0F && super.rand.nextInt(4) == 0) {
                  this.attackEntityFrom(DamageSource.inWall, dmg);
               }
            }
         }

         if(this.getCountOnUpdate() % 30 == 0 && this.isSubmergedForDamage()) {
            int hp1 = this.getMaxHP() / 30;
            //int hp1 = this.ArmorMinDamage / 10;
            if(hp1 <= 0) {
               hp1 = 1;
            }

            if(this.isEngineWaterboarded()) {
               this.attackEntityFromWaterboarding(hp1);
            } else {
               this.attackEntityFrom(DamageSource.inWall, hp1);
            }

            if(this instanceof MCH_EntityTank) {
               MCH_BaseVehicleInfo cmd1 = this.getAcInfo();
               if(cmd1 != null) {
                  // Tanks keep their armor-minimum water pressure damage, but waterboarding
                  // still stops applying damage once the engine shutdown threshold is reached.
                  if(this.isEngineWaterboarded()) {
                     this.attackEntityFromWaterboarding((int) (hp1 + cmd1.armorMinDamage));
                  } else {
                     this.attackEntityFrom(DamageSource.inWall, hp1 + cmd1.armorMinDamage);
                  }
               }
            }

         }



      }
   }

   public float getGiveDamageRot() {
      return 40.0F;
   }

   public void applyServerPositionAndRotation() {
      double increment = (double) this.aircraftPosRotInc;

      // Accurate wrapping and double precision
      double yawDiff = MathHelper.wrapAngleTo180_double(this.aircraftYaw - this.getRotYaw());
      double rollDiff = MathHelper.wrapAngleTo180_double(this.getServerRoll() - this.getRotRoll());

      //System.out.println("applyServerPositionAndRotation called:");
      //System.out.println("  Current Yaw: " + this.getRotYaw() + ", Target Yaw: " + this.aircraftYaw + ", Yaw Difference: " + yawDiff);
      //System.out.println("  Current Roll: " + this.getRotRoll() + ", Target Roll: " + this.getServerRoll() + ", Roll Difference: " + rollDiff);

      if (!this.isDestroyed() && (!W_Lib.isClientPlayer(this.getRiddenByEntity()) || this.getRidingEntity() != null)) {
         // Smooth interpolation
         float newRotYaw = (float) (this.getRotYaw() + yawDiff / increment);
         float newRotPitch = (float) (this.getRotPitch() + (this.aircraftPitch - this.getRotPitch()) / increment);
         float newRotRoll = (float) (this.getRotRoll() + rollDiff / increment);

         // Apply the new rotations
         this.setRotYaw(newRotYaw);
         this.setRotPitch(newRotPitch);
         this.setRotRoll(newRotRoll);

         //System.out.println("  New Rotations: Yaw=" + newRotYaw + ", Pitch=" + newRotPitch + ", Roll=" + newRotRoll);
      }

      // Smooth position interpolation
      this.setPosition(
              super.posX + (this.aircraftX - super.posX) / increment,
              super.posY + (this.aircraftY - super.posY) / increment,
              super.posZ + (this.aircraftZ - super.posZ) / increment
      );
      this.setRotation(this.getRotYaw(), this.getRotPitch());

      //possible culprit of the bullshit??????
      //commenting this out makes vehicles behave like leap frogger but it might also cause the vehicle shake bug so I'm like actually lost as to what to do here
      --this.aircraftPosRotInc;

      //System.out.println("  New Position: X=" + super.posX + ", Y=" + super.posY + ", Z=" + super.posZ);
      //System.out.println("  Remaining Increment: " + this.aircraftPosRotInc);
   }
   protected void autoRepair() {

         if (this.timeSinceHit > 0) {
            --this.timeSinceHit;
         }
         //this keeps missile damage working somehow, this fucking mod I swear

      if (MCH_Config.AutoRepairEnabled.prmBool) {
         if (this.getMaxHP() > 0) {
            if (!this.isDestroyed()) {
               if (this.getDamageTaken() > this.beforeDamageTaken) {
                  this.repairCount = 600;
               } else if (this.repairCount > 0) {
                  --this.repairCount;
               } else {
                  this.repairCount = 40;
                  double hpp = (double) this.getHP() / (double) this.getMaxHP();
                  MCH_Config var10001 = MCH_MOD.config;
                  if (hpp >= MCH_Config.AutoRepairHP.prmDouble) {
                     this.repair(this.getMaxHP() / 100);
                  }
               }
            }

            this.beforeDamageTaken = this.getDamageTaken();
         }
      }
   }

   public boolean repair(int tpd) {
      if(tpd < 1) {
         tpd = 1;
      }

      int damage = this.getDamageTaken();
      if(damage > 0) {
         if(!super.worldObj.isRemote) {
            this.setDamageTaken(damage - tpd);
         }

         return true;
      } else {
         return false;
      }
   }

   public void repairOtherAircraft() {
      float range = this.getAcInfo() != null?this.getAcInfo().repairOtherVehiclesRange:0.0F;
      if(range > 0.0F) {
         if(!super.worldObj.isRemote && this.getCountOnUpdate() % 20 == 0) {
            List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityBaseVehicle.class, this.getBoundingBox().expand((double)range, (double)range, (double)range));

            for(int i = 0; i < list.size(); ++i) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)list.get(i);
               if(!W_Entity.isEqual(this, ac) && ac.getHP() < ac.getMaxHP()) {
                  ac.setDamageTaken(ac.getDamageTaken() - this.getAcInfo().repairOtherVehiclesValue);
               }
            }
         }

      }
   }

   protected void regenerationMob() {
      if(!this.isDestroyed()) {
         if(!super.worldObj.isRemote) {
            if(this.getAcInfo() != null && this.getAcInfo().regeneration && this.getRiddenByEntity() != null) {
               MCH_EntitySeat[] st = this.getSeats();
               MCH_EntitySeat[] arr$ = st;
               int len$ = st.length;

               for(int i$ = 0; i$ < len$; ++i$) {
                  MCH_EntitySeat s = arr$[i$];
                  if(s != null && !s.isDead) {
                     Entity e = s.riddenByEntity;
                     if(W_Lib.isEntityLivingBase(e) && !e.isDead) {
                        PotionEffect pe = W_Entity.getActivePotionEffect(e, Potion.regeneration);
                        if(pe == null || pe != null && pe.getDuration() < 500) {
                           W_Entity.addPotionEffect(e, new PotionEffect(Potion.regeneration.id, 250, 0, true));
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public double getWaterDepth() {
      byte b0 = 5;
      double d0 = 0.0D;

      for(int i = 0; i < b0; ++i) {
         double d1 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * (double)(i + 0) / (double)b0 - 0.125D;
         double d2 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * (double)(i + 1) / (double)b0 - 0.125D;
         d1 += (double)this.getAcInfo().floatOffset;
         d2 += (double)this.getAcInfo().floatOffset;
         AxisAlignedBB axisalignedbb = W_AxisAlignedBB.getAABB(super.boundingBox.minX, d1, super.boundingBox.minZ, super.boundingBox.maxX, d2, super.boundingBox.maxZ);
         if(super.worldObj.isAABBInMaterial(axisalignedbb, Material.water)) {
            d0 += 1.0D / (double)b0;
         }
      }

      return d0;
   }

   public int getCountOnUpdate() {
      return this.countOnUpdate;
   }

   public boolean canSupply() {
      return this.canFloatWater()?MCH_Lib.getBlockIdY(this, 1, -3) != 0:MCH_Lib.getBlockIdY(this, 1, -3) != 0 && !this.isInWater();
   }

   public void setFuel(int fuel) {
      if(!super.worldObj.isRemote) {
         if(fuel < 0) {
            fuel = 0;
         }

         if(fuel > this.getMaxFuel()) {
            fuel = this.getMaxFuel();
         }

         if(fuel != this.getFuel()) {
            this.getDataWatcher().updateObject(25, Integer.valueOf(fuel));
         }
      }

   }

   public int getFuel() {
      return this.getDataWatcher().getWatchableObjectInt(25);
   }

   public float getFuelP() {
      int m = this.getMaxFuel();
      return m == 0?0.0F:(float)this.getFuel() / (float)m;
   }

   public boolean canUseFuel(boolean checkOtherSeet) {
      return this.getMaxFuel() <= 0 || this.getFuel() > 1 || this.isInfinityFuel(this.getRiddenByEntity(), checkOtherSeet);
   }

   public boolean canUseFuel() {
      return this.canUseFuel(false);
   }

   public int getMaxFuel() {
      return this.getAcInfo() != null?this.getAcInfo().maxFuel:0;
   }

   public long getServiceFuel() {
      return this.serviceFuel;
   }

   /** Server-authoritative, saturating addition to the service reserve. */
   public void addServiceFuel(long amount) {
      if(!super.worldObj.isRemote && amount > 0L) {
         this.serviceFuel = Long.MAX_VALUE - this.serviceFuel < amount ? Long.MAX_VALUE : this.serviceFuel + amount;
      }
   }

   /** Server-authoritative deduction; returns the amount actually removed. */
   public long removeServiceFuel(long amount) {
      if(super.worldObj.isRemote || amount <= 0L) {
         return 0L;
      }
      long removed = Math.min(amount, this.serviceFuel);
      this.serviceFuel -= removed;
      return removed;
   }

   public void supplyFuel() {
      float range = this.getAcInfo() != null?this.getAcInfo().fuelSupplyRange:0.0F;
      if(range > 0.0F) {
         if(!super.worldObj.isRemote && this.getCountOnUpdate() % 10 == 0) {
            List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityBaseVehicle.class, this.getBoundingBox().expand((double)range, (double)range, (double)range));

            if(this.getAcInfo().gasPump) {
               if(this.isDestroyed() || this.getSizeInventory() <= 0) {
                  return;
               }
               this.convertSolidServiceFuel();
               this.sortServiceTargets(list);
               for(int i = 0; i < list.size(); ++i) {
                  MCH_EntityBaseVehicle target = (MCH_EntityBaseVehicle)list.get(i);
                  if(this.isFiniteServiceTarget(target) && (!super.onGround || target.canSupply())
                          && target.getMaxFuel() > 0 && target.getFuel() < target.getMaxFuel()) {
                     int wanted = Math.min(30, target.getMaxFuel() - target.getFuel());
                     int moved = this.takeServiceFuel(wanted);
                     if(moved > 0) {
                        target.setFuel(target.getFuel() + moved);
                        target.fuelSuppliedCount = 40;
                     }
                  }
               }
               return;
            }

            for(int i = 0; i < list.size(); ++i) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)list.get(i);
               if(!W_Entity.isEqual(this, ac)) {
                  if((!super.onGround || ac.canSupply()) && ac.getFuel() < ac.getMaxFuel()) {
                     int fc = ac.getMaxFuel() - ac.getFuel();
                     if(fc > 30) {
                        fc = 30;
                     }

                     ac.setFuel(ac.getFuel() + fc);
                  }

                  ac.fuelSuppliedCount = 40;
               }
            }
         }

      }
   }

   private boolean isFiniteServiceTarget(MCH_EntityBaseVehicle target) {
      return target != null && !W_Entity.isEqual(this, target) && !target.isDestroyed()
              && (!this.getAcInfo().forceY || MathHelper.floor_double(super.posY) == MathHelper.floor_double(target.posY));
   }

   private void sortServiceTargets(List targets) {
      Collections.sort(targets, new Comparator() {
         public int compare(Object left, Object right) {
            Entity a = (Entity)left;
            Entity b = (Entity)right;
            int distance = Double.compare(MCH_EntityBaseVehicle.this.getDistanceSqToEntity(a), MCH_EntityBaseVehicle.this.getDistanceSqToEntity(b));
            return distance != 0 ? distance : Integer.compare(W_Entity.getEntityId(a), W_Entity.getEntityId(b));
         }
      });
   }

   private void convertSolidServiceFuel() {
      for(int slot = 0; slot < this.getSizeInventory(); ++slot) {
         ItemStack stack = this.getStackInSlot(slot);
         long value = 0L;
         if(stack != null && stack.stackSize > 0) {
            if(stack.getItem() == Items.coal && stack.getItemDamage() == 0) value = 100L;
            else if(stack.getItem() == Items.coal && stack.getItemDamage() == 1) value = 75L;
            else if(stack.getItem() == Item.getItemFromBlock(Blocks.coal_block)) value = 900L;
         }
         if(value > 0L) {
            int count = 0;
            while(count < stack.stackSize && this.serviceFuel <= Long.MAX_VALUE - value) {
               this.addServiceFuel(value);
               ++count;
            }
            if(count > 0) this.decrStackSize(slot, count);
            if(this.getStackInSlot(slot) != null && this.getStackInSlot(slot).stackSize <= 0) this.setInventorySlotContents(slot, (ItemStack)null);
         }
      }
   }

   private int takeServiceFuel(int wanted) {
      int moved = (int)this.removeServiceFuel(wanted);
      for(int slot = 0; moved < wanted && slot < this.getSizeInventory(); ++slot) {
         ItemStack stack = this.getStackInSlot(slot);
         if(stack != null && stack.getItem() instanceof MCH_ItemFuel) {
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getItemDamage());
            int amount = Math.min(wanted - moved, remaining);
            if(amount > 0) {
               stack.setItemDamage(stack.getItemDamage() + amount);
               this.setInventorySlotContents(slot, stack);
               moved += amount;
            }
         }
      }
      return moved;
   }

   public void updateFuel() {
      if(this.getMaxFuel() != 0) {
         if(this.fuelSuppliedCount > 0) {
            --this.fuelSuppliedCount;
         }

         if(!this.isDestroyed() && !super.worldObj.isRemote) {
            if(this.getCountOnUpdate() % 20 == 0 && this.getFuel() > 1 && this.getThrottle() > 0.0D && this.fuelSuppliedCount <= 0) {
               double curFuel = this.getThrottle() * 1.4D;
               if(curFuel > 1.0D) {
                  curFuel = 1.0D;
               }

               this.fuelConsumption += curFuel * (double)this.getAcInfo().fuelConsumption * (double)this.getFuelConsumptionFactor();
               if(this.fuelConsumption > 1.0D) {
                  int fuel = (int)this.fuelConsumption;
                  this.fuelConsumption -= (double)fuel;
                  this.setFuel(this.getFuel() - fuel);
               }
            }

            int var5 = this.getFuel();
            if(this.canSupply() && this.getCountOnUpdate() % 10 == 0 && var5 < this.getMaxFuel()) {
               for(int i = 0; i < 3; ++i) {
                  if(var5 < this.getMaxFuel()) {
                     ItemStack var6 = this.getGuiInventory().getFuelSlotItemStack(i);
                     if(var6 != null && var6.getItem() instanceof MCH_ItemFuel && var6.getItemDamage() < var6.getMaxDamage()) {
                        int fc = this.getMaxFuel() - var5;
                        if(fc > 100) {
                           fc = 100;
                        }

                        if(var6.getItemDamage() > var6.getMaxDamage() - fc) {
                           fc = var6.getMaxDamage() - var6.getItemDamage();
                        }

                        var6.setItemDamage(var6.getItemDamage() + fc);
                        var5 += fc;
                     }
                  }
               }

               if(this.getFuel() != var5) {
                  MCH_Achievement.addStat(super.riddenByEntity, MCH_Achievement.supplyFuel, 1);
               }

               this.setFuel(var5);
            }
         }

      }
   }

   public float getFuelConsumptionFactor() {
      return 1.0F;
   }

   public void updateSupplyAmmo() {
      if(!super.worldObj.isRemote) {
         boolean isReloading = false;
         if(this.getRiddenByEntity() instanceof EntityPlayer && !this.getRiddenByEntity().isDead && ((EntityPlayer)this.getRiddenByEntity()).openContainer instanceof MCH_BaseVehicleGuiContainer) {
            isReloading = true;
         }

         this.setCommonStatus(2, isReloading);
         if(!this.isDestroyed() && this.beforeSupplyAmmo && !isReloading) {
            this.reloadAllWeapon();
            MCH_PacketNotifyAmmoNum.sendAllAmmoNum(this, (EntityPlayer)null);
         }

         this.beforeSupplyAmmo = isReloading;
      }

      if(this.getCommonStatus(2)) {
         this.supplyAmmoWait = 20;
      }

      if(this.supplyAmmoWait > 0) {
         --this.supplyAmmoWait;
      }

   }


   public boolean canAcceptAmmo(ItemStack stack) {
      return this.getAmmoSpaceRemaining(stack) > 0;
   }

   public int getAmmoSpaceRemaining(ItemStack stack) {
      if(stack == null || stack.stackSize <= 0 || this.getWeaponNum() <= 0) {
         return 0;
      }
      int space = 0;
      for(int wid = 0; wid < this.getWeaponNum(); ++wid) {
         MCH_WeaponSet ws = this.getWeapon(wid);
         if(ws != null && isRoundItemForWeapon(ws, stack)) {
            int weaponSpace = ws.getAllAmmoNum() - (ws.getRestAllAmmoNum() + ws.getAmmoNum());
            if(weaponSpace > 0) {
               space += weaponSpace;
            }
         }
      }
      return space;
   }

   public int trySupplyAmmoFromStack(ItemStack stack, EntityPlayer player) {
      if(super.worldObj.isRemote || stack == null || stack.stackSize <= 0 || this.isDestroyed()) {
         return 0;
      }

      int consumed = 0;
      for(int wid = 0; wid < this.getWeaponNum() && stack.stackSize > 0; ++wid) {
         MCH_WeaponSet ws = this.getWeapon(wid);
         int used = trySupplyAmmoToWeapon(ws, stack);
         if(used > 0) {
            consumed += used;
            if(ws.getAmmoNum() <= 0) {
               ws.reloadMag();
            }
            MCH_PacketNotifyAmmoNum.sendAmmoNum(this, player, wid);
         }
      }

      if(consumed > 0) {
         MCH_PacketNotifyAmmoNum.sendAllAmmoNum(this, player);
      }
      return consumed;
   }

   private int trySupplyAmmoToWeapon(MCH_WeaponSet ws, ItemStack stack) {
      if(ws == null || stack == null || ws.getInfo() == null || ws.getInfo().roundItems == null || ws.getInfo().roundItems.size() != 1) {
         return 0;
      }
      int space = ws.getAllAmmoNum() - (ws.getRestAllAmmoNum() + ws.getAmmoNum());
      if(space <= 0) {
         return 0;
      }

      Iterator i$ = ws.getInfo().roundItems.iterator();
      while(i$.hasNext()) {
         MCH_WeaponInfo.RoundItem ri = (MCH_WeaponInfo.RoundItem)i$.next();
         if(ri != null && ri.itemStack != null && stack.isItemEqual(ri.itemStack)) {
            int itemCost = ri.num <= 0 ? 1 : ri.num;
            int supplied = ws.getInfo().suppliedNum <= 0 ? 1 : ws.getInfo().suppliedNum;
            int packages = stack.stackSize / itemCost;
            if(packages <= 0) {
               return 0;
            }
            int packagesNeeded = (space + supplied - 1) / supplied;
            if(packages > packagesNeeded) {
               packages = packagesNeeded;
            }
            int ammoToAdd = packages * supplied;
            if(ammoToAdd > space) {
               ammoToAdd = space;
            }
            int before = ws.getRestAllAmmoNum() + ws.getAmmoNum();
            ws.setRestAllAmmoNum(ws.getRestAllAmmoNum() + ammoToAdd);
            int after = ws.getRestAllAmmoNum() + ws.getAmmoNum();
            if(after > before) {
               int consumed = packages * itemCost;
               stack.stackSize -= consumed;
               return consumed;
            }
            return 0;
         }
      }
      return 0;
   }

   private boolean isRoundItemForWeapon(MCH_WeaponSet ws, ItemStack stack) {
      if(ws == null || stack == null || ws.getInfo() == null || ws.getInfo().roundItems == null || ws.getInfo().roundItems.size() != 1) {
         return false;
      }
      Iterator i$ = ws.getInfo().roundItems.iterator();
      while(i$.hasNext()) {
         MCH_WeaponInfo.RoundItem ri = (MCH_WeaponInfo.RoundItem)i$.next();
         if(ri != null && ri.itemStack != null && stack.isItemEqual(ri.itemStack)) {
            return true;
         }
      }
      return false;
   }

   public void supplyAmmo(int weaponID) {
      if(super.worldObj.isRemote) {
         MCH_WeaponSet player = this.getWeapon(weaponID);
         player.supplyRestAllAmmo();
      } else {
         MCH_Achievement.addStat(super.riddenByEntity, MCH_Achievement.supplyAmmo, 1);
         if(this.getRiddenByEntity() instanceof EntityPlayer) {
            EntityPlayer var9 = (EntityPlayer)this.getRiddenByEntity();
            if(this.canPlayerSupplyAmmo(var9, weaponID)) {
               MCH_WeaponSet ws = this.getWeapon(weaponID);
               Iterator i$ = ws.getInfo().roundItems.iterator();

               while(i$.hasNext()) {
                  MCH_WeaponInfo.RoundItem ri = (MCH_WeaponInfo.RoundItem)i$.next();
                  int num = ri.num;

                  for(int i = 0; i < var9.inventory.mainInventory.length; ++i) {
                     ItemStack itemStack = var9.inventory.mainInventory[i];
                     if(itemStack != null && itemStack.isItemEqual(ri.itemStack)) {
                        if(itemStack.getItem() != W_Item.getItemByName("water_bucket") && itemStack.getItem() != W_Item.getItemByName("lava_bucket")) {
                           if(itemStack.stackSize > num) {
                              itemStack.stackSize -= num;
                              num = 0;
                           } else {
                              num -= itemStack.stackSize;
                              itemStack.stackSize = 0;
                              var9.inventory.mainInventory[i] = null;
                           }
                        } else if(itemStack.stackSize == 1) {
                           var9.inventory.setInventorySlotContents(i, new ItemStack(W_Item.getItemByName("bucket"), 1));
                           --num;
                        }
                     }

                     if(num <= 0) {
                        break;
                     }
                  }
               }

               ws.supplyRestAllAmmo();
            }
         }
      }

   }

   public void supplyAmmoToOtherAircraft() {
      float range = this.getAcInfo() != null?this.getAcInfo().ammoSupplyRange:0.0F;
      if(range > 0.0F) {
         if(!super.worldObj.isRemote && this.getCountOnUpdate() % 40 == 0) {
            List list = super.worldObj.getEntitiesWithinAABB(MCH_EntityBaseVehicle.class, this.getBoundingBox().expand((double)range, (double)range, (double)range));

            if(this.getAcInfo().ammoLoader) {
               if(this.isDestroyed() || this.getSizeInventory() <= 0) {
                  return;
               }
               this.sortServiceTargets(list);
               for(int i = 0; i < list.size(); ++i) {
                  MCH_EntityBaseVehicle target = (MCH_EntityBaseVehicle)list.get(i);
                  if(!this.isFiniteServiceTarget(target) || !target.canSupply()) continue;
                  for(int wid = 0; wid < target.getWeaponNum(); ++wid) {
                     if(this.supplyCargoAmmoPackage(target, wid)) {
                        MCH_WeaponSet ws = target.getWeapon(wid);
                        if(ws.getAmmoNum() <= 0) ws.reloadMag();
                        MCH_PacketNotifyAmmoNum.sendAmmoNum(target, target.getEntityByWeaponId(wid) instanceof EntityPlayer
                                ? (EntityPlayer)target.getEntityByWeaponId(wid) : null, wid);
                        MCH_PacketNotifyAmmoNum.sendAllAmmoNum(target, (EntityPlayer)null);
                     }
                  }
               }
               return;
            }

            for(int i = 0; i < list.size(); ++i) {
               MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)list.get(i);
               if(!W_Entity.isEqual(this, ac) && ac.canSupply()) {
                  for(int wid = 0; wid < ac.getWeaponNum(); ++wid) {
                     MCH_WeaponSet ws = ac.getWeapon(wid);
                     int num = ws.getRestAllAmmoNum() + ws.getAmmoNum();
                     if(num < ws.getAllAmmoNum()) {
                        int ammo = ws.getAllAmmoNum() / 10;
                        if(ammo < 1) {
                           ammo = 1;
                        }

                        ws.setRestAllAmmoNum(num + ammo);
                        EntityPlayer player = ac.getEntityByWeaponId(wid);
                        if(num != ws.getRestAllAmmoNum() + ws.getAmmoNum()) {
                           if(ws.getAmmoNum() <= 0) {
                              ws.reloadMag();
                           }

                           MCH_PacketNotifyAmmoNum.sendAmmoNum(ac, player, wid);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private boolean supplyCargoAmmoPackage(MCH_EntityBaseVehicle target, int weaponId) {
      MCH_WeaponSet ws = target.getWeapon(weaponId);
      if(ws == null || ws.getInfo() == null || ws.getInfo().roundItems == null || ws.getInfo().roundItems.isEmpty()) return false;
      int space = ws.getAllAmmoNum() - ws.getRestAllAmmoNum() - ws.getAmmoNum();
      if(space <= 0) return false;
      int supplied = Math.max(1, ws.getInfo().suppliedNum);
      int cycleLimit = Math.max(1, ws.getAllAmmoNum() / 10);
      int packages = Math.max(1, cycleLimit / supplied);
      packages = Math.min(packages, Math.max(1, (space + supplied - 1) / supplied));
      int availablePackages = this.countCompleteAmmoPackages(ws);
      packages = Math.min(packages, availablePackages);
      if(packages <= 0) return false;

      // Consume only after the complete transaction has been proved available.
      Iterator requirements = ws.getInfo().roundItems.iterator();
      while(requirements.hasNext()) {
         MCH_WeaponInfo.RoundItem requirement = (MCH_WeaponInfo.RoundItem)requirements.next();
         int remaining = Math.max(1, requirement.num) * packages;
         for(int slot = 0; slot < this.getSizeInventory() && remaining > 0; ++slot) {
            ItemStack stack = this.getStackInSlot(slot);
            if(stack != null && requirement.itemStack != null && stack.isItemEqual(requirement.itemStack)) {
               int amount = Math.min(remaining, stack.stackSize);
               this.decrStackSize(slot, amount);
               remaining -= amount;
               ItemStack left = this.getStackInSlot(slot);
               if(left != null && left.stackSize <= 0) this.setInventorySlotContents(slot, (ItemStack)null);
            }
         }
      }
      int add = Math.min(space, packages * supplied);
      int before = ws.getRestAllAmmoNum() + ws.getAmmoNum();
      ws.setRestAllAmmoNum(ws.getRestAllAmmoNum() + add);
      return ws.getRestAllAmmoNum() + ws.getAmmoNum() > before;
   }

   private int countCompleteAmmoPackages(MCH_WeaponSet ws) {
      int packages = Integer.MAX_VALUE;
      for(int index = 0; index < ws.getInfo().roundItems.size(); ++index) {
         MCH_WeaponInfo.RoundItem requirement = (MCH_WeaponInfo.RoundItem)ws.getInfo().roundItems.get(index);
         if(requirement == null || requirement.itemStack == null) return 0;
         boolean alreadyCounted = false;
         int cost = 0;
         for(int other = 0; other < ws.getInfo().roundItems.size(); ++other) {
            MCH_WeaponInfo.RoundItem candidate = (MCH_WeaponInfo.RoundItem)ws.getInfo().roundItems.get(other);
            if(candidate != null && candidate.itemStack != null && requirement.itemStack.isItemEqual(candidate.itemStack)) {
               if(other < index) alreadyCounted = true;
               cost += Math.max(1, candidate.num);
            }
         }
         if(alreadyCounted) continue;
         int found = 0;
         for(int slot = 0; slot < this.getSizeInventory(); ++slot) {
            ItemStack stack = this.getStackInSlot(slot);
            if(stack != null && stack.isItemEqual(requirement.itemStack)) found += stack.stackSize;
         }
         packages = Math.min(packages, found / cost);
      }
      return packages == Integer.MAX_VALUE ? 0 : packages;
   }

   public boolean canPlayerSupplyAmmo(EntityPlayer player, int weaponId) {
      //if on rack (eg: aircraft carrier
      if(MCH_Lib.getBlockIdY(this, 1, -3) == 0) {
         return false;
      } else if(!this.canSupply()) {
         return false;
      } else {
         MCH_WeaponSet ws = this.getWeapon(weaponId);
         if(ws.getRestAllAmmoNum() + ws.getAmmoNum() >= ws.getAllAmmoNum()) {
            return false;
         } else {
            Iterator i$ = ws.getInfo().roundItems.iterator();

            while(i$.hasNext()) {
               MCH_WeaponInfo.RoundItem ri = (MCH_WeaponInfo.RoundItem)i$.next();
               int num = ri.num;
               ItemStack[] arr$ = player.inventory.mainInventory;
               int len$ = arr$.length;
               int i$1 = 0;

               while(true) {
                  if(i$1 < len$) {
                     ItemStack itemStack = arr$[i$1];
                     if(itemStack != null && itemStack.isItemEqual(ri.itemStack)) {
                        num -= itemStack.stackSize;
                     }

                     if(num > 0) {
                        ++i$1;
                        continue;
                     }
                  }

                  if(num > 0) {
                     return false;
                  }
                  break;
               }
            }

            return true;
         }
      }
   }

   public static String getSkinOverlayTextureName(String baseName, String overlayName) {
      String overlay = overlayName != null && overlayName.startsWith("skinoverlays/") ? overlayName : "skinoverlays/" + overlayName;
      String base = baseName;
      int overlaySeparator = base != null ? base.indexOf("|skinoverlays/") : -1;
      if(overlaySeparator >= 0) {
         base = base.substring(0, overlaySeparator);
      }
      return (base != null && !base.isEmpty() && !base.startsWith("skinoverlays/") ? base : "") + "|" + overlay;
   }

   public static String getTexturePath(String directory, String textureName) {
      if(textureName != null && textureName.startsWith("skinoverlays/")) {
         return "textures/" + textureName + ".png";
      }
      return "textures/" + directory + "/" + textureName + ".png";
   }

   public MCH_EntityBaseVehicle setTextureName(String name) {
      if(name != null && !name.isEmpty()) {
         this.getDataWatcher().updateObject(21, String.valueOf(name));
      }

      return this;
   }

   public String getTextureName() {
      return this.getDataWatcher().getWatchableObjectString(21);
   }

   public void switchNextTextureName() {
      if(this.getAcInfo() != null) {
         String currentTexture = this.getTextureName();
         int overlaySeparator = currentTexture != null ? currentTexture.indexOf("|skinoverlays/") : -1;
         if(overlaySeparator >= 0) {
            currentTexture = currentTexture.substring(0, overlaySeparator);
         }
         this.setTextureName(this.getAcInfo().getNextTextureName(currentTexture));
      }

   }

   public void zoomCamera() {
      if(this.canZoom()) {
         float z = this.camera.getCameraZoom();
         if((double)z >= (double)this.getZoomMax() - 0.01D) {
            z = 1.0F;
         } else {
            z *= 2.0F;
            if(z >= (float)this.getZoomMax()) {
               z = (float)this.getZoomMax();
            }
         }

         this.camera.setCameraZoom((double)z <= (double)this.getZoomMax() + 0.01D?z:1.0F);
      }

   }

   public int getZoomMax() {
      return this.getAcInfo() != null?this.getAcInfo().cameraZoom:1;
   }

   public boolean canZoom() {
      return this.getZoomMax() > 1;
   }

   public boolean canSwitchCameraMode() {
      return this.isDestroyed()?false:this.getAcInfo() != null && this.getAcInfo().isEnableNightVision;
   }

   public boolean canSwitchCameraMode(int seatID) {
      return this.isDestroyed()?false:this.canSwitchCameraMode() && this.camera.isValidUid(seatID);
   }

   public int getCameraMode(EntityPlayer player) {
      return this.camera.getMode(this.getSeatIdByEntity(player));
   }

   public String getCameraModeName(EntityPlayer player) {
      return this.camera.getModeName(this.getSeatIdByEntity(player));
   }

   public void switchCameraMode(EntityPlayer player) {
      this.switchCameraMode(player, this.camera.getMode(this.getSeatIdByEntity(player)) + 1);
   }

   public void switchCameraMode(EntityPlayer player, int mode) {
      this.camera.setMode(this.getSeatIdByEntity(player), mode);
   }

   public void updateCameraViewers() {
      for(int i = 0; i < this.getSeatNum() + 1; ++i) {
         this.camera.updateViewer(i, this.getEntityBySeatId(i));
      }

   }

   public void updateRadar(int radarSpeed) {
      if(this.entityRadar != null && this.isRadarActive()) {
         this.radarRotate += radarSpeed;
         if(this.radarRotate >= 360) {
            this.radarRotate = 0;
         }

         if(this.radarRotate == 0) {
            this.entityRadar.updateXZ(this, 64);
         }
      }

   }

   public int getRadarRotate() {
      return this.radarRotate;
   }

   public void initRadar() {
      if(this.entityRadar != null) {
         this.entityRadar.clear();
      }
      this.radarRotate = 0;
   }

   public ArrayList getRadarEntityList() {
      return this.entityRadar != null ? this.entityRadar.getEntityList() : new ArrayList();
   }

   public ArrayList getRadarEnemyList() {
      return this.entityRadar != null ? this.entityRadar.getEnemyList() : new ArrayList();
   }

  // @Override
  // public void moveEntity(double parX, double parY, double parZ) {
  //    // Check the block under the tank
  //    Block blockUnder = MCH_Lib.getBlockY(this, 3, -2, false);
//
  //    // If the block is soul sand, reduce movement speed
  //    if (blockUnder == Blocks.soul_sand) {
  //       parX *= 0.8; // Reduce X movement by 20%
  //       parZ *= 0.8; // Reduce Z movement by 20%
  //    }
//
  //    // Proceed with original movement logic
  //    super.moveEntity(parX, parY, parZ);
  // }

   @Override
   public void moveEntity(double par1, double par3, double par5) {
      if(this.getAcInfo() != null) {
         super.worldObj.theProfiler.startSection("move");
         super.ySize *= 0.4F;
         double d3 = super.posX;
         double d4 = super.posY;
         double d5 = super.posZ;
         double d6 = par1;
         double d7 = par3;
         double d8 = par5;
         AxisAlignedBB axisalignedbb = super.boundingBox.copy();
         List list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(par1, par3, par5));

         for(int flag1 = 0; flag1 < list.size(); ++flag1) {
            par3 = ((AxisAlignedBB)list.get(flag1)).calculateYOffset(super.boundingBox, par3);
         }

         super.boundingBox.offset(0.0D, par3, 0.0D);
         if(!super.field_70135_K && d7 != par3) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         boolean var34 = super.onGround || d7 != par3 && d7 < 0.0D;

         int j;
         for(j = 0; j < list.size(); ++j) {
            par1 = ((AxisAlignedBB)list.get(j)).calculateXOffset(super.boundingBox, par1);
         }

         super.boundingBox.offset(par1, 0.0D, 0.0D);
         if(!super.field_70135_K && d6 != par1) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         for(j = 0; j < list.size(); ++j) {
            par5 = ((AxisAlignedBB)list.get(j)).calculateZOffset(super.boundingBox, par5);
         }

         super.boundingBox.offset(0.0D, 0.0D, par5);
         if(!super.field_70135_K && d8 != par5) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         if(super.stepHeight > 0.0F && var34 && super.ySize < 0.05F && (d6 != par1 || d8 != par5)) {
            double d12 = par1;
            double d10 = par3;
            double d11 = par5;
            par1 = d6;
            par3 = (double)super.stepHeight;
            par5 = d8;
            AxisAlignedBB throwable = super.boundingBox.copy();
            super.boundingBox.setBB(axisalignedbb);
            list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(d6, par3, d8));

            int k;
            for(k = 0; k < list.size(); ++k) {
               par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(super.boundingBox, par3);
            }

            super.boundingBox.offset(0.0D, par3, 0.0D);
            if(!super.field_70135_K && d7 != par3) {
               par5 = 0.0D;
               par3 = 0.0D;
               par1 = 0.0D;
            }

            for(k = 0; k < list.size(); ++k) {
               par1 = ((AxisAlignedBB)list.get(k)).calculateXOffset(super.boundingBox, par1);
            }

            super.boundingBox.offset(par1, 0.0D, 0.0D);
            if(!super.field_70135_K && d6 != par1) {
               par5 = 0.0D;
               par3 = 0.0D;
               par1 = 0.0D;
            }

            for(k = 0; k < list.size(); ++k) {
               par5 = ((AxisAlignedBB)list.get(k)).calculateZOffset(super.boundingBox, par5);
            }

            super.boundingBox.offset(0.0D, 0.0D, par5);
            if(!super.field_70135_K && d8 != par5) {
               par5 = 0.0D;
               par3 = 0.0D;
               par1 = 0.0D;
            }

            if(!super.field_70135_K && d7 != par3) {
               par5 = 0.0D;
               par3 = 0.0D;
               par1 = 0.0D;
            } else {
               par3 = (double)(-super.stepHeight);

               for(k = 0; k < list.size(); ++k) {
                  par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(super.boundingBox, par3);
               }

               super.boundingBox.offset(0.0D, par3, 0.0D);
            }

            if(d12 * d12 + d11 * d11 >= par1 * par1 + par5 * par5) {
               par1 = d12;
               par3 = d10;
               par5 = d11;
               super.boundingBox.setBB(throwable);
            }
         }

         super.worldObj.theProfiler.endSection();
         super.worldObj.theProfiler.startSection("rest");
         super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
         super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
         super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
         super.isCollidedHorizontally = d6 != par1 || d8 != par5;
         super.isCollidedVertically = d7 != par3;
         super.onGround = d7 != par3 && d7 < 0.0D;
         super.isCollided = super.isCollidedHorizontally || super.isCollidedVertically;
         this.updateFallState(par3, super.onGround);
         if(d6 != par1) {
            super.motionX = 0.0D;
         }

         if(d7 != par3) {
            super.motionY = 0.0D;
         }

         if(d8 != par5) {
            super.motionZ = 0.0D;
         }

         double var10000 = super.posX - d3;
         var10000 = super.posY - d4;
         var10000 = super.posZ - d5;

         try {
            this.doBlockCollisions();
         } catch (Throwable var33) {
            CrashReport crashreport = CrashReport.makeCrashReport(var33, "Checking entity tile collision");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
         }

         super.worldObj.theProfiler.endSection();
      }
   }

   private void handleStepHeightMovement(double par1, double par3, double par5, double deltaX, double deltaY, double deltaZ, AxisAlignedBB initialBoundingBox, List<AxisAlignedBB> collisionBoxes) {
      // Store initial deltas
      double initialDeltaX = par1;
      double initialDeltaY = par3;
      double initialDeltaZ = par5;

      // Attempt step height movement
      par1 = deltaX;
      par3 = (double) super.stepHeight;
      par5 = deltaZ;

      // Create a copy of the bounding box
      AxisAlignedBB stepBoundingBox = super.boundingBox.copy();
      super.boundingBox.setBB(initialBoundingBox);

      // Get colliding bounding boxes
      List<AxisAlignedBB> stepCollisionBoxes = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(deltaX, par3, deltaZ));

      // Calculate Y offset based on collisions
      for (AxisAlignedBB box : stepCollisionBoxes) {
         par3 = box.calculateYOffset(super.boundingBox, par3);
      }

      // Offset bounding box by calculated Y offset
      super.boundingBox.offset(0.0D, par3, 0.0D);

      // Check if movement is obstructed
      if (!super.field_70135_K && deltaY != par3) {
         deltaX = deltaY = deltaZ = 0.0D;
      }

      // Calculate X offset based on collisions
      for (AxisAlignedBB box : stepCollisionBoxes) {
         par1 = box.calculateXOffset(super.boundingBox, par1);
      }

      // Offset bounding box by calculated X offset
      super.boundingBox.offset(par1, 0.0D, 0.0D);

      // Check if movement is obstructed
      if (!super.field_70135_K && deltaX != par1) {
         deltaX = deltaY = deltaZ = 0.0D;
      }

      // Calculate Z offset based on collisions
      for (AxisAlignedBB box : stepCollisionBoxes) {
         par5 = box.calculateZOffset(super.boundingBox, par5);
      }

      // Offset bounding box by calculated Z offset
      super.boundingBox.offset(0.0D, 0.0D, par5);

      // Check if movement is obstructed
      if (!super.field_70135_K && deltaZ != par5) {
         deltaX = deltaY = deltaZ = 0.0D;
      }

      // Revert to initial deltas if step movement was less efficient
      if (initialDeltaX * initialDeltaX + initialDeltaZ * initialDeltaZ >= par1 * par1 + par5 * par5) {
         par1 = initialDeltaX;
         par3 = initialDeltaY;
         par5 = initialDeltaZ;
         super.boundingBox.setBB(stepBoundingBox);
      }
   }

   private void updateEntityPosition() {
      super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
      super.posY = super.boundingBox.minY + (double) super.yOffset - (double) super.ySize;
      super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
   }

   private void handleBlockCollisions() {
      try {
         this.doBlockCollisions();
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Checking entity tile collision");
         CrashReportCategory crashReportCategory = crashReport.makeCategory("Entity being checked for collision");
         this.addEntityCrashInfo(crashReportCategory);
         throw new ReportedException(crashReport);
      }
   }

   private void addExtraBoundingBoxBlockCollisions(AxisAlignedBB baseSweepBox, List collidingBoundingBoxes) {
      if(this.extraBoundingBox == null || this.extraBoundingBox.length <= 0) {
         return;
      }

      MCH_BoundingBox[] boxes = this.getCalculatedExtraBoundingBoxes();
      if(boxes == null || boxes.length <= 0) {
         return;
      }

      double motionX = this.getSweptMotionX(baseSweepBox);
      double motionY = this.getSweptMotionY(baseSweepBox);
      double motionZ = this.getSweptMotionZ(baseSweepBox);
      double baseCenterX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
      double baseCenterY = (super.boundingBox.minY + super.boundingBox.maxY) / 2.0D;
      double baseCenterZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;

      for(int boxIndex = 0; boxIndex < boxes.length; ++boxIndex) {
         AxisAlignedBB extraBox = boxes[boxIndex].boundingBox;
         AxisAlignedBB extraSweepBox = extraBox.addCoord(motionX, motionY, motionZ);
         ArrayList extraCollisions = new ArrayList();
         this.addBlockCollisionsToList(extraSweepBox, extraCollisions);
         double extraCenterX = (extraBox.minX + extraBox.maxX) / 2.0D;
         double extraCenterY = (extraBox.minY + extraBox.maxY) / 2.0D;
         double extraCenterZ = (extraBox.minZ + extraBox.maxZ) / 2.0D;
         double offsetX = baseCenterX - extraCenterX;
         double offsetY = baseCenterY - extraCenterY;
         double offsetZ = baseCenterZ - extraCenterZ;

         for(int collisionIndex = 0; collisionIndex < extraCollisions.size(); ++collisionIndex) {
            AxisAlignedBB collisionBox = (AxisAlignedBB)extraCollisions.get(collisionIndex);
            collidingBoundingBoxes.add(collisionBox.getOffsetBoundingBox(offsetX, offsetY, offsetZ));
         }
      }
   }

   private double getSweptMotionX(AxisAlignedBB sweepBox) {
      if(sweepBox.minX < super.boundingBox.minX) {
         return sweepBox.minX - super.boundingBox.minX;
      }
      if(sweepBox.maxX > super.boundingBox.maxX) {
         return sweepBox.maxX - super.boundingBox.maxX;
      }
      return 0.0D;
   }

   private double getSweptMotionY(AxisAlignedBB sweepBox) {
      if(sweepBox.minY < super.boundingBox.minY) {
         return sweepBox.minY - super.boundingBox.minY;
      }
      if(sweepBox.maxY > super.boundingBox.maxY) {
         return sweepBox.maxY - super.boundingBox.maxY;
      }
      return 0.0D;
   }

   private double getSweptMotionZ(AxisAlignedBB sweepBox) {
      if(sweepBox.minZ < super.boundingBox.minZ) {
         return sweepBox.minZ - super.boundingBox.minZ;
      }
      if(sweepBox.maxZ > super.boundingBox.maxZ) {
         return sweepBox.maxZ - super.boundingBox.maxZ;
      }
      return 0.0D;
   }

   private void addBlockCollisionsToList(AxisAlignedBB targetBox, List collidingBoundingBoxes) {
      int minX = MathHelper.floor_double(targetBox.minX);
      int maxX = MathHelper.floor_double(targetBox.maxX + 1.0D);
      int minY = MathHelper.floor_double(targetBox.minY);
      int maxY = MathHelper.floor_double(targetBox.maxY + 1.0D);
      int minZ = MathHelper.floor_double(targetBox.minZ);
      int maxZ = MathHelper.floor_double(targetBox.maxZ + 1.0D);

      for(int x = minX; x < maxX; ++x) {
         for(int z = minZ; z < maxZ; ++z) {
            if(super.worldObj.blockExists(x, 64, z)) {
               for(int y = minY - 1; y < maxY; ++y) {
                  Block block = W_WorldFunc.getBlock(super.worldObj, x, y, z);
                  if(block != null) {
                     block.addCollisionBoxesToList(super.worldObj, x, y, z, targetBox, collidingBoundingBoxes, this);
                  }
               }
            }
         }
      }
   }

   public static List getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
      //todo: make creative players/non survival mode players not collide with aircraft collisions
      ArrayList collidingBoundingBoxes = new ArrayList();
      collidingBoundingBoxes.clear();
      int i = MathHelper.floor_double(par2AxisAlignedBB.minX);
      int j = MathHelper.floor_double(par2AxisAlignedBB.maxX + 1.0D);
      int k = MathHelper.floor_double(par2AxisAlignedBB.minY);
      int l = MathHelper.floor_double(par2AxisAlignedBB.maxY + 1.0D);
      int i1 = MathHelper.floor_double(par2AxisAlignedBB.minZ);
      int j1 = MathHelper.floor_double(par2AxisAlignedBB.maxZ + 1.0D);

      for(int d0 = i; d0 < j; ++d0) {
         for(int l1 = i1; l1 < j1; ++l1) {
            if(par1Entity.worldObj.blockExists(d0, 64, l1)) {
               for(int list = k - 1; list < l; ++list) {
                  Block j2 = W_WorldFunc.getBlock(par1Entity.worldObj, d0, list, l1);
                  if(j2 != null) {
                     j2.addCollisionBoxesToList(par1Entity.worldObj, d0, list, l1, par2AxisAlignedBB, collidingBoundingBoxes, par1Entity);
                  }
               }
            }
         }
      }

      if(par1Entity instanceof MCH_EntityBaseVehicle) {
         MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)par1Entity;
         vehicle.addExtraBoundingBoxBlockCollisions(par2AxisAlignedBB, collidingBoundingBoxes);
      }

      double var15 = 0.25D;
      List var16 = par1Entity.worldObj.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB.expand(var15, var15, var15));

      for(int var17 = 0; var17 < var16.size(); ++var17) {
         Entity entity = (Entity)var16.get(var17);
         if(par1Entity instanceof MCH_EntityBaseVehicle
                 && ((MCH_EntityBaseVehicle)par1Entity).noCollisionEntities.containsKey(entity)) {
            continue;
         }
         if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) && !(entity instanceof MCH_EntityHitBox)) {
            AxisAlignedBB axisalignedbb1 = entity.getBoundingBox();
            if(axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB)) {
               collidingBoundingBoxes.add(axisalignedbb1);
            }

            axisalignedbb1 = par1Entity.getCollisionBox(entity);
            if(axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB)) {
               collidingBoundingBoxes.add(axisalignedbb1);
            }
         }
      }

      return collidingBoundingBoxes;
   }


   private boolean isSubmergedForDamage() {
      MCH_BaseVehicleInfo info = this.getAcInfo();
      return info != null
         && !info.isFloat
         && MCH_Lib.isBlockInWater(super.worldObj, (int)(super.posX + 0.5D), (int)(super.posY + 1.5D + (double)info.submergedDamageHeight), (int)(super.posZ + 0.5D));
   }

   protected boolean isEngineWaterboarded() {
      return this.isSubmergedForDamage()
         && (this instanceof MCH_EntityTank || this instanceof MCP_EntityPlane || this instanceof MCH_EntityHeli);
   }

   protected boolean applyEngineWaterboardingThrottleCut() {
      if(this.isEngineWaterboarded()) {
         this.setCurrentThrottle(0.0D);
         this.setThrottle(0.0D);
         this.throttleUp = false;
         this.throttleDown = false;
         this.throttleBack = 0.0F;
         return true;
      }

      return false;
   }

   private void attackEntityFromWaterboarding(int damage) {
      MCH_BaseVehicleInfo info = this.getAcInfo();
      int shutdownHp = 0;
      if(info != null && info.engineShutdownThreshold > 0) {
         shutdownHp = (int)Math.ceil((double)this.getMaxHP() * (double)info.engineShutdownThreshold / 100.0D);
      }

      int cappedDamage = Math.min(Math.max(damage, 0), this.getHP() - shutdownHp);
      if(cappedDamage > 0) {
         this.attackEntityFrom(DamageSource.inWall, cappedDamage);
      }
   }

   protected void onUpdate_updateBlock() {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.Collision_DestroyBlock.prmBool) {
         for(int l = 0; l < 4; ++l) {
            int i1 = MathHelper.floor_double(super.posX + ((double)(l % 2) - 0.5D) * 0.8D);
            int j1 = MathHelper.floor_double(super.posZ + ((double)(l / 2) - 0.5D) * 0.8D);

            for(int k1 = 0; k1 < 2; ++k1) {
               int l1 = MathHelper.floor_double(super.posY) + k1;
               Block block = W_WorldFunc.getBlock(super.worldObj, i1, l1, j1);
               if(!W_Block.isNull(block)) {
                  if(block == W_Block.getSnowLayer()) {
                     super.worldObj.setBlockToAir(i1, l1, j1);
                  }

                  if(block == Blocks.waterlily || block == Blocks.cake) {
                     W_WorldFunc.destroyBlock(super.worldObj, i1, l1, j1, false);
                  }
               }
            }
         }

      }
   }

   public void onUpdate_ParticleSmoke() {
      if(super.worldObj.isRemote) {
         if(this.getCurrentThrottle() > 0.10000000149011612D) {
            float yaw = this.getRotYaw();
            float pitch = this.getRotPitch();
            float roll = this.getRotRoll();
            MCH_WeaponSet ws = this.getCurrentWeapon(this.getRiddenByEntity());
            if(ws.getFirstWeapon() instanceof MCH_WeaponSmoke) {
               for(int i = 0; i < ws.getWeaponNum(); ++i) {
                  MCH_WeaponBase wb = ws.getWeapon(i);
                  if(wb != null) {
                     MCH_WeaponInfo wi = wb.getInfo();
                     if(wi != null) {
                        Vec3 rot = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -yaw - 180.0F + wb.fixRotationYaw, pitch - wb.fixRotationPitch, roll);
                        if((double)super.rand.nextFloat() <= this.getCurrentThrottle() * 1.5D) {
                           Vec3 pos = MCH_Lib.RotVec3(wb.position, -yaw, -pitch, -roll);
                           double x = super.posX + pos.xCoord + rot.xCoord;
                           double y = super.posY + pos.yCoord + rot.yCoord;
                           double z = super.posZ + pos.zCoord + rot.zCoord;

                           for(int smk = 0; smk < wi.smokeNum; ++smk) {
                              float c = super.rand.nextFloat() * 0.05F;
                              int maxAge = (int)(super.rand.nextDouble() * (double)wi.smokeMaxAge);
                              MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", x, y, z);
                              prm.setMotion(rot.xCoord * (double)wi.acceleration + (super.rand.nextDouble() - 0.5D) * 0.2D, rot.yCoord * (double)wi.acceleration + (super.rand.nextDouble() - 0.5D) * 0.2D, rot.zCoord * (double)wi.acceleration + (super.rand.nextDouble() - 0.5D) * 0.2D);
                              prm.size = ((float)super.rand.nextInt(5) + 5.0F) * wi.smokeSize;
                              prm.setColor(wi.color.a + super.rand.nextFloat() * 0.05F, wi.color.r + c, wi.color.g + c, wi.color.b + c);
                              prm.age = maxAge;
                              prm.toWhite = true;
                              prm.diffusible = true;
                              prm.visibleInThermal = true;
                              MCH_ParticlesUtil.spawnParticle(prm);
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   protected void onUpdate_ParticleSandCloud(boolean seaOnly) {
      if(!seaOnly || this.getAcInfo().enableSeaSurfaceParticle) {
         double particlePosY = (double)((int)super.posY);
         boolean b = false;
         float scale = this.getAcInfo().particlesScale * 3.0F;
         if(seaOnly) {
            scale *= 2.0F;
         }

         double throttle = this.getCurrentThrottle();
         throttle *= 2.0D;
         if(throttle > 1.0D) {
            throttle = 1.0D;
         }

         int count = seaOnly?(int)(scale * 7.0F):0;
         int rangeY = (int)(scale * 10.0F) + 1;

         int y;
         for(y = 0; y < rangeY && !b; ++y) {
            int pn = -1;

            while(pn <= 1) {
               int z = -1;

               while(true) {
                  if(z <= 1) {
                     label99: {
                        Block k = W_WorldFunc.getBlock(super.worldObj, (int)(super.posX + 0.5D) + pn, (int)(super.posY + 0.5D) - y, (int)(super.posZ + 0.5D) + z);
                        if(!b && k != null && !Block.isEqualTo(k, Blocks.air)) {
                           if(seaOnly && W_Block.isEqual(k, W_Block.getWater())) {
                              --count;
                           }

                           if(count <= 0) {
                              particlePosY = super.posY + 1.0D + (double)(scale / 5.0F) - (double)y;
                              b = true;
                              pn += 100;
                              break label99;
                           }
                        }

                        ++z;
                        continue;
                     }
                  }

                  ++pn;
                  break;
               }
            }
         }

         double var20 = (double)(rangeY - y + 1) / (5.0D * (double)scale) / 2.0D;
         if(b && this.getAcInfo().particlesScale > 0.01F) {
            for(int var21 = 0; var21 < (int)(throttle * 6.0D * var20); ++var21) {
               float r = (float)(super.rand.nextDouble() * 3.141592653589793D * 2.0D);
               double dx = (double)MathHelper.cos(r);
               double dz = (double)MathHelper.sin(r);
               MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.posX + dx * (double)scale * 3.0D, particlePosY + (super.rand.nextDouble() - 0.5D) * (double)scale, super.posZ + dz * (double)scale * 3.0D, (double)scale * dx * 0.3D, (double)scale * -0.4D * 0.05D, (double)scale * dz * 0.3D, scale * 5.0F);
               prm.setColor(prm.a * 0.6F, prm.r, prm.g, prm.b);
               prm.age = (int)(10.0F * scale);
               prm.motionYUpAge = seaOnly?0.2F:0.1F;
               MCH_ParticlesUtil.spawnParticle(prm);
            }
         }

      }
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      return par1Entity.boundingBox;
   }

   public AxisAlignedBB getBoundingBox() {
      return super.boundingBox;
   }

   public boolean canBePushed() {
      return false;
   }

   public double getMountedYOffset() {
      return 0.0D;
   }

   public float getShadowSize() {
      return 2.0F;
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   public boolean useFlare(int type) {
      if(this.getAcInfo() != null && this.getAcInfo().haveFlare()) {
         int[] arr$ = this.getAcInfo().flare.types;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            int i = arr$[i$];
            if(i == type) {
               this.setCommonStatus(0, true);
               if(this.flareDv.use(type)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean useChaff() {
      if(this.getAcInfo() != null && this.getAcInfo().haveChaff()) {
         if(this.chaff.onUse()) {
            return true;
         }
         return false;
      } else {
         return false;
      }
   }

   public boolean useMaintenance() {
      if(this.getAcInfo() != null && this.getAcInfo().haveMaintenance()) {
         if(this.maintenance.onUse()) {
            return true;
         }
         return false;
      } else {
         return false;
      }
   }

   public boolean useAPS(Entity e) {
      if(this.getAcInfo() != null && this.getAcInfo().haveAPS()) {
         if(this.aps.requestToggle(e)) {
            return true;
         }
         return false;
      } else {
         return false;
      }
   }

   public int getCurrentFlareType() {
      return !this.haveFlare()?0:this.getAcInfo().flare.types[this.currentFlareIndex];
   }

   public void nextFlareType() {
      if(this.haveFlare()) {
         this.currentFlareIndex = (this.currentFlareIndex + 1) % this.getAcInfo().flare.types.length;
      }

   }

   public boolean canUseFlare() {
      return this.getAcInfo() != null && this.getAcInfo().haveFlare() && (!this.getCommonStatus(0) && this.flareDv.tick == 0);
   }

   public boolean isFlarePreparation() {
      return this.flareDv.isInPreparation();
   }

   public boolean isFlareUsing() {
      return this.flareDv.isUsing();
   }

   public int getFlareTick() {
      return this.flareDv.tick;
   }

   public boolean haveFlare() {
      return this.getAcInfo() != null && this.getAcInfo().haveFlare();
   }

   public boolean haveFlare(int seatID) {
      return this.haveFlare() && seatID >= 0 && seatID <= 1;
   }

   public boolean canUseChaff() {
      return this.getAcInfo() != null && this.getAcInfo().haveChaff() && this.chaff.tick == 0;
   }

   public boolean canUseMaintenance() {
      return this.getAcInfo() != null && this.getAcInfo().haveMaintenance() && this.maintenance.tick == 0;
   }

   public boolean canUseAPS() {
      return this.getAcInfo() != null && this.getAcInfo().haveAPS() && !this.isDead && !this.isDestroyed()
              && this.aps != null;
   }

   public boolean haveChaff() {
      return this.getAcInfo() != null && this.getAcInfo().haveChaff();
   }

   public boolean haveMaintenance() {
      return this.getAcInfo() != null && this.getAcInfo().haveMaintenance();
   }

   public boolean haveAPS() {
      return this.getAcInfo() != null && this.getAcInfo().haveAPS();
   }

   public MCH_EntitySeat[] getSeats() {
      return this.seats != null?this.seats:seatsDummy;
   }

   public int getSeatIdByEntity(Entity entity) {
      if(entity == null) {
         return -1;
      } else if(isEqual(this.getRiddenByEntity(), entity)) {
         return 0;
      } else {
         for(int i = 0; i < this.getSeats().length; ++i) {
            MCH_EntitySeat seat = this.getSeats()[i];
            if(seat != null && isEqual(seat.riddenByEntity, entity)) {
               return i + 1;
            }
         }

         return -1;
      }
   }

   public MCH_EntitySeat getSeatByEntity(Entity entity) {
      int idx = this.getSeatIdByEntity(entity);
      return idx > 0?this.getSeat(idx - 1):null;
   }

   public Entity getEntityBySeatId(int id) {
      if(id == 0) {
         return this.getRiddenByEntity();
      } else {
         --id;
         return id >= 0 && id < this.getSeats().length?(this.seats[id] != null?this.seats[id].riddenByEntity:null):null;
      }
   }

   public EntityPlayer getEntityByWeaponId(int id) {
      if(id >= 0 && id < this.getWeaponNum()) {
         for(int i = 0; i < this.currentWeaponID.length; ++i) {
            if(this.currentWeaponID[i] == id) {
               Entity e = this.getEntityBySeatId(i);
               if(e instanceof EntityPlayer) {
                  return (EntityPlayer)e;
               }
            }
         }
      }

      return null;
   }

   public Entity getWeaponUserByWeaponName(String name) {
      if(this.getAcInfo() == null) {
         return null;
      } else {
         MCH_BaseVehicleInfo.Weapon weapon = this.getAcInfo().getWeaponByName(name);
         Entity entity = null;
         if(weapon != null) {
            entity = this.getEntityBySeatId(this.getWeaponSeatID((MCH_WeaponInfo)null, weapon));
            if(entity == null && weapon.canUsePilot) {
               entity = this.getRiddenByEntity();
            }
         }

         return entity;
      }
   }

   protected void newSeats(int seatsNum) {
      if(seatsNum >= 2) {
         if(this.seats != null) {
            for(int i = 0; i < this.seats.length; ++i) {
               if(this.seats[i] != null) {
                  this.seats[i].setDead();
                  this.seats[i] = null;
               }
            }
         }

         this.seats = new MCH_EntitySeat[seatsNum - 1];
      }

   }

   public MCH_EntitySeat getSeat(int idx) {
      return idx < this.seats.length?this.seats[idx]:null;
   }

   public void setSeat(int idx, MCH_EntitySeat seat) {
      if(idx < this.seats.length) {
         MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.setSeat SeatID=" + idx + " / seat[]" + (this.seats[idx] != null) + " / " + (seat != null && seat.riddenByEntity != null), new Object[0]);
         if(this.seats[idx] != null && this.seats[idx].riddenByEntity != null) {
            ;
         }

         this.seats[idx] = seat;
      }

   }

   public boolean isValidSeatID(int seatID) {
      return seatID >= 0 && seatID < this.getSeatNum() + 1;
   }

   public void updateHitBoxPosition() {}

   public void updateSeatsPosition(double px, double py, double pz, boolean setPrevPos) {
      MCH_SeatInfo[] info = this.getSeatsInfo();
      if(this.pilotSeat != null && !this.pilotSeat.isDead) {
         this.pilotSeat.prevPosX = this.pilotSeat.posX;
         this.pilotSeat.prevPosY = this.pilotSeat.posY;
         this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
         this.pilotSeat.setPosition(px, py, pz);
         if(info != null && info.length > 0 && info[0] != null) {
            Vec3 i = this.getTransformedPosition(info[0].pos.xCoord, info[0].pos.yCoord, info[0].pos.zCoord, px, py, pz, info[0].rotSeat);
            this.pilotSeat.setPosition(i.xCoord, i.yCoord, i.zCoord);
         }

         this.pilotSeat.rotationPitch = this.getRotPitch();
         this.pilotSeat.rotationYaw = this.getRotYaw();
         if(setPrevPos) {
            this.pilotSeat.prevPosX = this.pilotSeat.posX;
            this.pilotSeat.prevPosY = this.pilotSeat.posY;
            this.pilotSeat.prevPosZ = this.pilotSeat.posZ;
         }
      }

      int var17 = 0;
      MCH_EntitySeat[] arr$ = this.seats;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_EntitySeat seat = arr$[i$];
         ++var17;
         if(seat != null && !seat.isDead) {
            float offsetY = 0.0F;
            if(seat.riddenByEntity != null) {
               if(W_Lib.isClientPlayer(seat.riddenByEntity)) {
                  offsetY = 1.0F;
               } else if(seat.riddenByEntity.height >= 1.0F) {
                  offsetY = -seat.riddenByEntity.height + 1.0F;
               }
            }

            seat.prevPosX = seat.posX;
            seat.prevPosY = seat.posY;
            seat.prevPosZ = seat.posZ;
            MCH_SeatInfo si = var17 < info.length?info[var17]:info[0];
            Vec3 v = this.getTransformedPosition(si.pos.xCoord, si.pos.yCoord + (double)offsetY, si.pos.zCoord, px, py, pz, si.rotSeat);
            seat.setPosition(v.xCoord, v.yCoord, v.zCoord);
            seat.rotationPitch = this.getRotPitch();
            seat.rotationYaw = this.getRotYaw();
            if(setPrevPos) {
               seat.prevPosX = seat.posX;
               seat.prevPosY = seat.posY;
               seat.prevPosZ = seat.posZ;
            }

            if(si instanceof MCH_SeatRackInfo) {
               seat.updateRotation(((MCH_SeatRackInfo)si).fixYaw + this.getRotYaw(), ((MCH_SeatRackInfo)si).fixPitch);
            }

            seat.updatePosition();
         }
      }

   }

   public int getClientPositionDelayCorrection() {
      return 0;
   }

   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
      this.aircraftPosRotInc = Math.max(1, par9 + this.getClientPositionDelayCorrection());
      this.aircraftX = par1;
      this.aircraftY = par3;
      this.aircraftZ = par5;
      this.aircraftYaw = (double)this.getRotYaw() + MathHelper.wrapAngleTo180_double((double)par7 - (double)this.getRotYaw());
      this.aircraftPitch = (double) par8;

      // Apply current velocities
      super.motionX = this.velocityX;
      super.motionY = this.velocityY;
      super.motionZ = this.velocityZ;

      // Log debug information for monitoring

      //this was the last debug stuff I had not commented out
      //System.out.println("setPositionAndRotation2 called:");
      //System.out.println("  Position set to: X=" + par1 + ", Y=" + par3 + ", Z=" + par5);
      //System.out.println("  Rotation set to: Yaw=" + par7 + ", Pitch=" + par8);
      //System.out.println("  Increment: " + par9);
      //System.out.println("  Motion set to: motionX=" + super.motionX + ", motionY=" + super.motionY + ", motionZ=" + super.motionZ);
   }

   public void updateRiderPosition(double px, double py, double pz) {
      MCH_SeatInfo[] info = this.getSeatsInfo();
      if(super.riddenByEntity != null && !super.riddenByEntity.isDead) {
         float riddenEntityYOffset = super.riddenByEntity.yOffset;
         float offset = 0.0F;
         if(super.riddenByEntity instanceof EntityPlayer && !W_Lib.isClientPlayer(super.riddenByEntity)) {
            --offset;
         }

         Vec3 v;
         if(info != null && info.length > 0) {
            v = this.getTransformedPosition(info[0].pos.xCoord, info[0].pos.yCoord + (double)riddenEntityYOffset - 0.5D, info[0].pos.zCoord, px, py, pz, info[0].rotSeat);
         } else {
            v = this.getTransformedPosition(0.0D, (double)(riddenEntityYOffset - 1.0F), 0.0D);
         }

         super.riddenByEntity.yOffset = 0.0F;
         super.riddenByEntity.setPosition(v.xCoord, v.yCoord, v.zCoord);
         super.riddenByEntity.yOffset = riddenEntityYOffset;
      }

   }

   public void updateRiderPosition() {
      this.updateRiderPosition(super.posX, super.posY, super.posZ);
   }

   public Vec3 calcOnTurretPos(Vec3 pos) {
      float ry = this.getLastRiderYaw();
      if(this.getRiddenByEntity() != null) {
         ry = this.getRiddenByEntity().rotationYaw;
      }

      Vec3 tpos = this.getAcInfo().turretPosition.addVector(0.0D, pos.yCoord, 0.0D);
      Vec3 v = pos.addVector(-tpos.xCoord, -tpos.yCoord, -tpos.zCoord);
      v = MCH_Lib.RotVec3(v, -ry, 0.0F, 0.0F);
      Vec3 vv = MCH_Lib.RotVec3(tpos, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
      v.xCoord += vv.xCoord;
      v.yCoord += vv.yCoord;
      v.zCoord += vv.zCoord;
      return v;
   }

   public float getLastRiderYaw() {
      return this.lastRiderYaw;
   }

   public float getLastRiderPitch() {
      return this.lastRiderPitch;
   }

   private static int packLastRiderAngles(float yaw, float pitch) {
      int packedYaw = (short)((int)(MathHelper.wrapAngleTo180_float(yaw) * 10.0F));
      int packedPitch = (short)((int)(MathHelper.wrapAngleTo180_float(pitch) * 10.0F));
      return packedYaw << 16 | packedPitch & 65535;
   }

   private static float unpackLastRiderYaw(int packedAngles) {
      return (float)((short)(packedAngles >> 16)) * 0.1F;
   }

   private static float unpackLastRiderPitch(int packedAngles) {
      return (float)((short)(packedAngles & 65535)) * 0.1F;
   }

   protected void syncLastRiderAngles() {
      if(super.worldObj.isRemote) {
         if(!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            int packedAngles = this.getDataWatcher().getWatchableObjectInt(28);
            this.prevLastRiderYaw = this.lastRiderYaw;
            this.prevLastRiderPitch = this.lastRiderPitch;
            this.lastRiderYaw = unpackLastRiderYaw(packedAngles);
            this.lastRiderPitch = unpackLastRiderPitch(packedAngles);
         }
      } else {
         int packedAngles = packLastRiderAngles(this.lastRiderYaw, this.lastRiderPitch);
         if(this.getDataWatcher().getWatchableObjectInt(28) != packedAngles) {
            this.getDataWatcher().updateObject(28, Integer.valueOf(packedAngles));
         }
      }

   }

   @SideOnly(Side.CLIENT)
   public void setupAllRiderRenderPosition(float tick, EntityPlayer player) {
      double x = super.lastTickPosX + (super.posX - super.lastTickPosX) * (double)tick;
      double y = super.lastTickPosY + (super.posY - super.lastTickPosY) * (double)tick;
      double z = super.lastTickPosZ + (super.posZ - super.lastTickPosZ) * (double)tick;
      this.updateRiderPosition(x, y, z);
      this.updateSeatsPosition(x, y, z, true);

      for(int cpi = 0; cpi < this.getSeatNum() + 1; ++cpi) {
         Entity seatInfo = this.getEntityBySeatId(cpi);
         if(seatInfo != null) {
            seatInfo.lastTickPosX = seatInfo.posX;
            seatInfo.lastTickPosY = seatInfo.posY;
            seatInfo.lastTickPosZ = seatInfo.posZ;
         }
      }

      if(this instanceof MCP_EntityPlane && MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)this, player)) {
         return;
      }

      if(this.getTVMissile() != null && W_Lib.isClientPlayer(this.getTVMissile().shootingEntity)) {
         MCH_EntityTvMissile var14 = this.getTVMissile();
         x = var14.prevPosX + (var14.posX - var14.prevPosX) * (double)tick;
         y = var14.prevPosY + (var14.posY - var14.prevPosY) * (double)tick;
         z = var14.prevPosZ + (var14.posZ - var14.prevPosZ) * (double)tick;
         MCH_ViewEntityDummy.setCameraPosition(x, y, z);
      } else {
         MCH_BaseVehicleInfo.CameraPosition var13 = this.getCameraPosInfo();
         if(var13 != null && var13.pos != null) {
            MCH_SeatInfo var12 = this.getSeatInfo(player);
            Vec3 v;
            if(var12 != null && var12.rotSeat) {
               v = this.calcOnTurretPos(var13.pos);
            } else {
               v = MCH_Lib.RotVec3(var13.pos, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
            }

            MCH_ViewEntityDummy.setCameraPosition(x + v.xCoord, y + v.yCoord, z + v.zCoord);
            if(var13.fixRot) {
               ;
            }
         }
      }

   }

   public Vec3 getTurretPos(Vec3 pos, boolean turret) {
      if(turret) {
         float ry = this.getLastRiderYaw();
         if(this.getRiddenByEntity() != null) {
            ry = this.getRiddenByEntity().rotationYaw;
         }

         Vec3 tpos = this.getAcInfo().turretPosition.addVector(0.0D, pos.yCoord, 0.0D);
         Vec3 v = pos.addVector(-tpos.xCoord, -tpos.yCoord, -tpos.zCoord);
         v = MCH_Lib.RotVec3(v, -ry, 0.0F, 0.0F);
         Vec3 vv = MCH_Lib.RotVec3(tpos, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
         v.xCoord += vv.xCoord;
         v.yCoord += vv.yCoord;
         v.zCoord += vv.zCoord;
         return v;
      } else {
         return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
      }
   }

   public Vec3 getTransformedPosition(Vec3 v) {
      return this.getTransformedPosition(v.xCoord, v.yCoord, v.zCoord);
   }

   public Vec3 getTransformedPosition(double x, double y, double z) {
      return this.getTransformedPosition(x, y, z, super.posX, super.posY, super.posZ);
   }

   public Vec3 getTransformedPosition(Vec3 v, Vec3 pos) {
      return this.getTransformedPosition(v.xCoord, v.yCoord, v.zCoord, pos.xCoord, pos.yCoord, pos.zCoord);
   }

   public Vec3 getTransformedPosition(Vec3 v, double px, double py, double pz) {
      return this.getTransformedPosition(v.xCoord, v.yCoord, v.zCoord, super.posX, super.posY, super.posZ);
   }

   public Vec3 getTransformedPosition(double x, double y, double z, double px, double py, double pz) {
      Vec3 v = MCH_Lib.RotVec3(x, y, z, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
      return v.addVector(px, py, pz);
   }

   public Vec3 getTransformedPosition(double x, double y, double z, double px, double py, double pz, boolean rotSeat) {
      if(rotSeat && this.getAcInfo() != null) {
         MCH_BaseVehicleInfo v = this.getAcInfo();
         Vec3 tv = MCH_Lib.RotVec3(x - v.turretPosition.xCoord, y - v.turretPosition.yCoord, z - v.turretPosition.zCoord, -this.getLastRiderYaw() + this.getRotYaw(), 0.0F, 0.0F);
         x = tv.xCoord + v.turretPosition.xCoord;
         y = tv.yCoord + v.turretPosition.xCoord;
         z = tv.zCoord + v.turretPosition.xCoord;
      }

      Vec3 v1 = MCH_Lib.RotVec3(x, y, z, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
      return v1.addVector(px, py, pz);
   }

   protected MCH_SeatInfo[] getSeatsInfo() {
      if(this.seatsInfo != null) {
         return this.seatsInfo;
      } else {
         this.newSeatsPos();
         return this.seatsInfo;
      }
   }

   public MCH_SeatInfo getSeatInfo(int index) {
      MCH_SeatInfo[] seats = this.getSeatsInfo();
      return index >= 0 && seats != null && index < seats.length?seats[index]:null;
   }

   public MCH_SeatInfo getSeatInfo(Entity entity) {
      return this.getSeatInfo(this.getSeatIdByEntity(entity));
   }

   protected void setSeatsInfo(MCH_SeatInfo[] v) {
      this.seatsInfo = v;
   }

   public int getSeatNum() {
      if(this.getAcInfo() == null) {
         return 0;
      } else {
         int s = this.getAcInfo().getNumSeatAndRack();
         return s >= 1?s - 1:1;
      }
   }

   protected void newSeatsPos() {
      if(this.getAcInfo() != null) {
         MCH_SeatInfo[] v = new MCH_SeatInfo[this.getAcInfo().getNumSeatAndRack()];

         for(int i = 0; i < v.length; ++i) {
            v[i] = (MCH_SeatInfo)this.getAcInfo().seatList.get(i);
         }

         this.setSeatsInfo(v);
      }

   }

   public void createSeats(String uuid) {
      if(!super.worldObj.isRemote) {
         if(!uuid.isEmpty()) {
            this.setCommonUniqueId(uuid);
            this.seats = new MCH_EntitySeat[this.getSeatNum()];

            for(int i = 0; i < this.seats.length; ++i) {
               this.seats[i] = new MCH_EntitySeat(super.worldObj, super.posX, super.posY, super.posZ);
               this.seats[i].parentUniqueID = this.getCommonUniqueId();
               this.seats[i].seatID = i;
               this.seats[i].setParent(this);
               super.worldObj.spawnEntityInWorld(this.seats[i]);
            }

         }
      }
   }

   public boolean interactFirstSeat(EntityPlayer player) {
      if(!super.worldObj.isRemote && !this.switchSeat && !this.canPlayerEnterVehicle(player)) {
         this.notifyVehicleAccessDenied(player);
         return false;
      }
      if(!super.worldObj.isRemote) {
         this.searchSeat();
      }
      MCH_EntitySeat[] seatArray = this.getSeats();
      if(seatArray == null || seatArray.length == 0) {
         MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SEARCH-REJECT] reason=no_seat_array vehicle=%s player=%s",
                 new Object[]{this.debugEntity(this), this.debugEntity(player)});
         return false;
      }
      for(int i = 0; i < seatArray.length; ++i) {
         int seatId = i + 1;
         MCH_EntitySeat seat = this.resolveSeatReferenceForInteraction(i, "vehicle_interact");
         if(seat == null) {
            MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SKIP] reason=seat_reference_null vehicle=%s seat=%d",
                    new Object[]{this.debugEntity(this), Integer.valueOf(i)});
         } else {
            if(this.clearInvalidSeatOccupant(seat, i, "vehicle_interact_search")) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-REPAIRED] vehicle=%s seat=%d",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i)});
            }
            if(seat.riddenByEntity != null) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SKIP] reason=occupied vehicle=%s seat=%d occupant=%s",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i), this.debugEntity(seat.riddenByEntity)});
            } else if(this.isMountedEntity(player)) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SKIP] reason=player_already_mounted vehicle=%s seat=%d",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i)});
            } else if(!this.canRideSeatOrRack(seatId, player)) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SKIP] reason=seat_exclusion vehicle=%s seat=%d",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i)});
            } else {
               if(!super.worldObj.isRemote) {
                  this.clearPlacementMotionLock();
                  player.mountEntity(seat);
               }
               MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-ACCEPT] vehicle=%s seat=%d player=%s",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i), this.debugEntity(player)});
               return true;
            }
         }
      }
      MCH_Lib.DbgLog(super.worldObj, "[MCH-INTERACT][SEAT-SEARCH-REJECT] reason=no_available_seat vehicle=%s player=%s",
              new Object[]{this.debugEntity(this), this.debugEntity(player)});
      return false;
   }

   public void onMountPlayerSeat(MCH_EntitySeat seat, Entity entity) {
      if (seat == null || !((entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) ? true : false))
         return;
      if(this.worldObj.isRemote && MCH_Lib.getClientPlayer() == entity) {
         this.switchGunnerFreeLookMode(false);
      }

      this.clearPlacementMotionLock();
      this.initCurrentWeapon(entity);
      MCH_Lib.DbgLog(super.worldObj, "onMountEntitySeat:%d", new Object[]{Integer.valueOf(W_Entity.getEntityId(entity))});
      Entity pilot = this.getRiddenByEntity();
      int sid = this.getSeatIdByEntity(entity);
      if(sid == 1 && (this.getAcInfo() == null || !this.getAcInfo().isEnableConcurrentGunnerMode)) {
         this.switchGunnerMode(false);
      }

      if(sid > 0) {
         this.isGunnerModeOtherSeat = true;
      }

      if(pilot != null && this.getAcInfo() != null) {
         int cwid = this.getCurrentWeaponID(pilot);
         MCH_BaseVehicleInfo.Weapon w = this.getAcInfo().getWeaponById(cwid);
         if(w != null && this.getWeaponSeatID(this.getWeaponInfoById(cwid), w) == sid) {
            int next = this.getNextWeaponID(pilot, 1);
            MCH_Lib.DbgLog(super.worldObj, "onMountEntitySeat:%d:->%d", new Object[]{Integer.valueOf(W_Entity.getEntityId(pilot)), Integer.valueOf(next)});
            if(next >= 0) {
               this.switchWeapon(pilot, next);
            }
         }
      }

      if(super.worldObj.isRemote) {
         this.updateClientSettings(sid);
      }

   }

   public MCH_WeaponInfo getWeaponInfoById(int id) {
      if(id >= 0) {
         MCH_WeaponSet ws = this.getWeapon(id);
         if(ws != null) {
            return ws.getInfo();
         }
      }

      return null;
   }

   public abstract boolean canMountWithNearEmptyMinecart();

   protected void mountWithNearEmptyMinecart() {
      if(this.getRidingEntity() == null) {
         byte d = 2;
         if(this.dismountedUserCtrl) {
            d = 6;
         }

         List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand((double)d, (double)d, (double)d));
         if(list != null && !list.isEmpty()) {
            for(int i = 0; i < list.size(); ++i) {
               Entity entity = (Entity)list.get(i);
               if(entity instanceof EntityMinecartEmpty) {
                  if(this.dismountedUserCtrl) {
                     return;
                  }

                  if(entity.riddenByEntity == null && entity.canBePushed()) {
                     this.waitMountEntity = 20;
                     MCH_Lib.DbgLog(super.worldObj.isRemote, "MCH_EntityBaseVehicle.mountWithNearEmptyMinecart:" + entity, new Object[0]);
                     this.mountEntity(entity);
                     return;
                  }
               }
            }
         }

         this.dismountedUserCtrl = false;
      }
   }

   public boolean isRidePlayer() {
      if(this.getRiddenByEntity() instanceof EntityPlayer) {
         return true;
      } else {
         MCH_EntitySeat[] arr$ = this.getSeats();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntitySeat seat = arr$[i$];
            if(seat != null && seat.riddenByEntity instanceof EntityPlayer) {
               return true;
            }
         }

         return false;
      }
   }

   public void onUnmountPlayerSeat(MCH_EntitySeat seat, Entity entity) {

      if(this.isNewUAV() && !super.worldObj.isRemote) {
         this.returnNewUavPilotToStation(entity, "uav_seat_exit");
      }

      MCH_Lib.DbgLog(super.worldObj, "onUnmountPlayerSeat:%d", new Object[]{Integer.valueOf(W_Entity.getEntityId(entity))});
      if(!super.worldObj.isRemote && !this.isUAV() && !this.isNewUAV() && entity instanceof EntityPlayer) {
         MCH_PacketNotifyOnMountEntity.sendDismount(this, (EntityPlayer)entity);
      }
      int sid = this.getSeatIdByEntity(entity);
      this.camera.initCamera(sid, entity);
      MCH_SeatInfo seatInfo = this.getSeatInfo(seat.seatID + 1);
      if(seatInfo instanceof MCH_SeatRackInfo) {
         Vec3 rackUnmountPosition = this.getRackUnmountPosition((MCH_SeatRackInfo)seatInfo);
         if(rackUnmountPosition != null) {
            entity.setLocationAndAngles(rackUnmountPosition.xCoord, rackUnmountPosition.yCoord, rackUnmountPosition.zCoord,
                  this.getRotYaw() + seatInfo.fixYaw, seatInfo.fixPitch);
            this.listUnmountReserve.add(new MCH_EntityBaseVehicle.UnmountReserve(entity, rackUnmountPosition.xCoord,
                  rackUnmountPosition.yCoord, rackUnmountPosition.zCoord));
         } else {
            this.setUnmountPosition(entity, Vec3.createVectorHelper(seatInfo.pos.xCoord, 0.0D, seatInfo.pos.zCoord));
         }
      } else if(seatInfo != null) {
         this.setUnmountPosition(entity, Vec3.createVectorHelper(seatInfo.pos.xCoord, 0.0D, seatInfo.pos.zCoord));
      }

      if(!this.isRidePlayer()) {
         this.switchGunnerMode(false);
         this.switchHoveringMode(false);
      }

   }

   protected Vec3 getRackUnmountPosition(MCH_SeatRackInfo rackInfo) {
      return null;
   }

   public boolean isCreatedSeats() {
      return !this.getCommonUniqueId().isEmpty();
   }

   public void onUpdate_Seats() {
      boolean missingSeat = this.repairSeatReferences();

      if(missingSeat) {
         if(this.seatSearchCount == 0 || this.seatSearchCount > 40) {
            this.debugVehicleState("SEAT-RESYNC-NEEDED", null);
            if(super.worldObj.isRemote) {
               MCH_PacketSeatListRequest.requestSeatList(this);
            } else {
               boolean waitedForEntityLoad = this.seatSearchCount > 40;
               this.searchSeat();
               if(waitedForEntityLoad) {
                  this.recreateMissingSeatsInLoadedChunks();
               }
            }
            this.seatSearchCount = 0;
         }
         ++this.seatSearchCount;
      } else {
         this.seatSearchCount = 0;
      }
   }

   private boolean repairSeatReferences() {
      boolean missingSeat = false;
      for(int i = 0; i < this.seats.length; ++i) {
         MCH_EntitySeat seat = this.seats[i];
         if(seat == null) {
            missingSeat = true;
         } else if(seat.isDead || seat.worldObj != super.worldObj || seat.seatID != i) {
            missingSeat = true;
            if(this.seatSearchCount == 0 || this.seatSearchCount > 40) {
               MCH_Lib.DbgLog(super.worldObj,
                       "[MCH-SYNC][SEAT-INVALID] aircraft=%s index=%d seat=%s seatId=%d parent=%s sameWorld=%s",
                       new Object[]{this.debugEntity(this), Integer.valueOf(i), this.debugEntity(seat), Integer.valueOf(seat.seatID),
                               this.debugEntity(seat.getParent()), Boolean.valueOf(seat.worldObj == super.worldObj)});
            }
            this.seats[i] = null;
         } else if(seat.getParent() != this) {
            if(this.getCommonUniqueId().equals(seat.parentUniqueID)) {
               seat.setParent(this);
            } else {
               missingSeat = true;
               this.seats[i] = null;
            }
         } else {
            seat.fallDistance = 0.0F;
         }
      }
      return missingSeat;
   }

   public void repairSeatStateAfterLoad() {
      if(super.worldObj.isRemote) {
         return;
      }
      this.repairInvalidOccupantsForInteraction("tracking_or_chunk_load");
      this.searchSeat();
   }

   public void syncCompleteAircraftState(EntityPlayerMP player) {
      if(player == null || super.worldObj.isRemote || player.worldObj != super.worldObj) {
         return;
      }
      this.repairSeatStateAfterLoad();
      player.playerNetServerHandler.sendPacket(new S1CPacketEntityMetadata(this.getEntityId(), this.getDataWatcher(), true));
      MCH_PacketStatusResponse.sendStatus(this, player);
      MCH_PacketSeatListResponse.sendSeatList(this, player);

      if(super.ridingEntity != null) {
         player.playerNetServerHandler.sendPacket(new S1BPacketEntityAttach(0, this, super.ridingEntity));
      }
      if(super.riddenByEntity != null && super.riddenByEntity.ridingEntity == this) {
         player.playerNetServerHandler.sendPacket(new S1BPacketEntityAttach(0, super.riddenByEntity, this));
      }
      for(int i = 0; i < this.seats.length; ++i) {
         MCH_EntitySeat seat = this.seats[i];
         if(seat != null && seat.riddenByEntity != null && seat.riddenByEntity.ridingEntity == seat) {
            player.playerNetServerHandler.sendPacket(new S1BPacketEntityAttach(0, seat.riddenByEntity, seat));
         }
      }
      // Vanilla attach packets are discarded when their target has not spawned on
      // the client yet.  Give the affected normal-vehicle rider a bounded, ID-only
      // correction which can wait for aircraft/seat spawn ordering to settle.
      if(!this.isUAV() && !this.isNewUAV()) {
         int riderSeat = this.getSeatIdByEntity(player);
         if(riderSeat >= 0) {
            MCH_PacketNotifyOnMountEntity.sendToRider(this, player, riderSeat);
         }
      }
   }

   private void recreateMissingSeatsInLoadedChunks() {
      if(super.worldObj.isRemote || this.getAcInfo() == null || this.getCommonUniqueId().isEmpty()) {
         return;
      }

      for(int i = 0; i < this.seats.length; ++i) {
         if(this.seats[i] != null) {
            continue;
         }

         MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
         if(seatInfo == null) {
            continue;
         }
         Vec3 position = this.getTransformedPosition(seatInfo.pos);
         if(!super.worldObj.blockExists(MathHelper.floor_double(position.xCoord),
                 MathHelper.floor_double(position.yCoord), MathHelper.floor_double(position.zCoord))) {
            continue;
         }

         MCH_EntitySeat seat = new MCH_EntitySeat(super.worldObj);
         seat.parentUniqueID = this.getCommonUniqueId();
         seat.seatID = i;
         seat.setParent(this);
         seat.setPosition(position.xCoord, position.yCoord, position.zCoord);
         seat.prevPosX = position.xCoord;
         seat.prevPosY = position.yCoord;
         seat.prevPosZ = position.zCoord;
         if(super.worldObj.spawnEntityInWorld(seat)) {
            this.seats[i] = seat;
         }
      }
   }

   public void searchSeat() {
      this.repairSeatReferences();
      if(this.seats == null || this.getCommonUniqueId().isEmpty()) {
         return;
      }

      // Do not use an AABB around the aircraft here. Rack seats on large ships
      // can be more than 60 blocks from the carrier entity's origin.
      List list = super.worldObj.loadedEntityList;
      for(int i = 0; i < list.size(); ++i) {
         Object entity = list.get(i);
         if(entity instanceof MCH_EntitySeat) {
            MCH_EntitySeat seat = (MCH_EntitySeat)entity;
            if(!seat.isDead && this.getCommonUniqueId().equals(seat.parentUniqueID)
                    && seat.seatID >= 0 && seat.seatID < this.seats.length
                    && (this.seats[seat.seatID] == null || this.seats[seat.seatID] == seat)) {
               this.seats[seat.seatID] = seat;
               seat.setParent(this);
            }
         }
      }

   }

   public String getCommonUniqueId() {
      return this.commonUniqueId;
   }

   public void setCommonUniqueId(String uniqId) {
      if(!super.worldObj.isRemote && this.commonUniqueId != null && !this.commonUniqueId.equals(uniqId)) {
         MCH_UavRegistry.unregister(this);
      }
      this.commonUniqueId = uniqId;
      if(!super.worldObj.isRemote && (this.isUAV() || this.isNewUAV())) {
         MCH_UavRegistry.register(this);
         if(this.uavStation != null && !this.uavStation.isDead) {
            this.uavStation.linkUav(this);
         }
      }
   }


   private static UUID parseUUID(String value) {
      if(value == null || value.isEmpty()) {
         return null;
      }
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private void restoreStoredPilot(String reason) {
      Entity rider = super.riddenByEntity;
      if(!(rider instanceof EntityPlayerMP) && this.lastRiddenByEntity instanceof EntityPlayerMP) {
         rider = this.lastRiddenByEntity;
      }
      if(rider instanceof EntityPlayerMP) {
         MCH_UavInventory.restorePilotInventory((EntityPlayerMP)rider, reason);
      }
   }

   public void setDead() {
      this.setDead(false);
      for (ChunkCoordinates coord : activeLights) {
         int x = coord.posX, y = coord.posY, z = coord.posZ;
         if (worldObj.getBlock(x, y, z) == MCH_MOD.lightBlock) {
            worldObj.setBlockToAir(x, y, z);
            worldObj.markBlockForUpdate(x, y, z);
            worldObj.updateLightByType(EnumSkyBlock.Block, x, y, z);
         }
      }
      activeLights.clear();
   }

   public void setDead(boolean dropItems) {
      if(this.aps != null) {
         this.aps.reset();
      }
      releaseNewUavStationChunk("vehicle-dead");
      if(!super.worldObj.isRemote && this.isNewUAV() && this.isDestroyed() && !this.newUavShiftExitInProgress) {
         notifyLinkedStationNewUavRemoved();
         Entity pilot = super.riddenByEntity != null ? super.riddenByEntity : this.lastRiddenByEntity;
         if(pilot != null) {
            this.returnNewUavPilotToStation(pilot, "uav_destroyed");
         } else {
            restoreStoredPilot("uav_destroyed");
         }
      }
      MCH_UavRegistry.unregister(this);
      super.dropContentsWhenDead = dropItems;
      super.setDead();
      if(this.getRiddenByEntity() != null) {
         this.getRiddenByEntity().mountEntity((Entity)null);
      }

      this.getGuiInventory().setDead();
      MCH_EntitySeat[] arr$ = this.seats;
      int len$ = arr$.length;

      int i$;
      for(i$ = 0; i$ < len$; ++i$) {
         MCH_EntitySeat e = arr$[i$];
         if(e != null) {
            e.setDead();
         }
      }

      if(this.soundUpdater != null) {
         this.soundUpdater.update();
      }

      if(this.getTowChainEntity() != null) {
         this.getTowChainEntity().setDead();
         this.setTowChainEntity((MCH_EntityChain)null);
      }

      Entity[] var6 = this.getParts();
      len$ = var6.length;

      for(i$ = 0; i$ < len$; ++i$) {
         Entity var7 = var6[i$];
         if(var7 != null) {
            var7.setDead();
         }
      }

      MCH_Lib.DbgLog(super.worldObj, "setDead:" + (this.getAcInfo() != null?this.getAcInfo().name:"null"), new Object[0]);
   }

   private void notifyLinkedStationNewUavRemoved() {
      if(this.linkedUavStationUUID != null) {
         MCH_UavJsonStore.signalDestroyed(super.worldObj, this.linkedUavStationDimension, this.linkedUavStationX, this.linkedUavStationY, this.linkedUavStationZ);
      }
      MCH_EntityUavStation station = resolveLinkedUavStation();
      if(station != null) {
         station.markLinkedNewUavDestroyed(this);
      } else if(this.linkedUavStationUUID != null) {
         MCH_Lib.Log((Entity)this, "Removed New UAV could not resolve station %s at %.2f, %.2f, %.2f; queued station destruction state", new Object[] { this.linkedUavStationUUID.toString(), Double.valueOf(this.linkedUavStationX), Double.valueOf(this.linkedUavStationY), Double.valueOf(this.linkedUavStationZ) });
      }
   }

   public void discardDuplicateUav() {
      if(super.worldObj.isRemote || this.isDead) {
         return;
      }
      this.newUavShiftExitInProgress = true;
      try {
         this.setDead(false);
      } finally {
         this.newUavShiftExitInProgress = false;
      }
   }


   private MCH_EntityUavStation resolveLinkedUavStation() {
      if(this.uavStation != null && !this.uavStation.isDead) {
         return this.uavStation;
      }
      if(this.linkedUavStationUUID == null || this.linkedUavStationDimension != this.dimension) {
         return null;
      }

      // Integrated runClient usually retains the direct object reference. A dedicated server
      // may unload that station reference while preserving its UUID and coordinates in NBT.
      int stationX = MathHelper.floor_double(this.linkedUavStationX);
      int stationY = MathHelper.floor_double(this.linkedUavStationY);
      int stationZ = MathHelper.floor_double(this.linkedUavStationZ);
      super.worldObj.getChunkFromBlockCoords(stationX, stationZ);
      MCH_EntityUavStation coordinateMatch = null;
      for(Object obj : super.worldObj.loadedEntityList) {
         if(obj instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation station = (MCH_EntityUavStation)obj;
            if(!station.isDead && this.linkedUavStationUUID.equals(station.getUniqueID())) {
               this.setUavStation(station);
               return station;
            }
            if(!station.isDead && MathHelper.floor_double(station.posX) == stationX &&
               MathHelper.floor_double(station.posY) == stationY && MathHelper.floor_double(station.posZ) == stationZ) {
               coordinateMatch = station;
            }
         }
      }
      if(coordinateMatch != null) {
         MCH_Lib.Log((Entity)this, "Recovered New UAV station by stored coordinates after UUID lookup failed", new Object[0]);
         this.setUavStation(coordinateMatch);
         return coordinateMatch;
      }
      return null;
   }

   private boolean hasNewUavReturnPosition() {
      return this.linkedUavStationUUID != null && this.hasLinkedUavStationPosition
            && this.linkedUavStationDimension == this.dimension
            && !Double.isNaN(this.linkedUavStationX) && !Double.isInfinite(this.linkedUavStationX)
            && !Double.isNaN(this.linkedUavStationY) && !Double.isInfinite(this.linkedUavStationY)
            && !Double.isNaN(this.linkedUavStationZ) && !Double.isInfinite(this.linkedUavStationZ);
   }

   private void updateNewUavReturnPositionFromStation() {
      MCH_EntityUavStation station = resolveLinkedUavStation();
      if(station != null) {
         this.setUavStation(station);
      }
   }

   /**
    * Detaches a NewUAV pilot before moving them. Vanilla's unmount placement runs during
    * mountEntity(null), so teleporting first can be overwritten with the aircraft position.
    */
   public boolean returnNewUavPilotToStation(Entity pilot, String inventoryReason) {
      if(pilot == null || pilot instanceof MCH_EntitySeat || super.worldObj.isRemote || !this.isNewUAV()) {
         return false;
      }

      updateNewUavReturnPositionFromStation();
      if(!hasNewUavReturnPosition()) {
         MCH_Lib.Log((Entity)this, "Unable to return New UAV pilot: station link is missing or still restoring; preserving UAV control", new Object[0]);
         return false;
      }

      if(pilot.ridingEntity != null) {
         pilot.mountEntity((Entity)null);
      }
      this.moveLeft = false;
      this.moveRight = false;
      this.throttleDown = false;
      this.throttleUp = false;
      this.switchGunnerMode(false);
      this.setCommonStatus(CMN_ID_FREE_LOOK, false);
      this.setCameraId(0);
      this.camera.initCamera(0, pilot);
      super.riddenByEntity = null;
      this.lastRiddenByEntity = null;
      this.lastRidingEntity = null;
      pilot.motionX = 0.0D;
      pilot.motionY = 0.0D;
      pilot.motionZ = 0.0D;
      pilot.fallDistance = 0.0F;
      double safeY = this.linkedUavStationY + 2.0D;
      if(pilot instanceof EntityPlayerMP) {
         EntityPlayerMP player = (EntityPlayerMP)pilot;
         player.setPositionAndUpdate(this.linkedUavStationX, safeY, this.linkedUavStationZ);
         NEW_UAV_SAFE_RETURNS.put(player.getUniqueID(), new NewUavSafeReturn(
               this.linkedUavStationDimension, this.linkedUavStationX, safeY, this.linkedUavStationZ));
         MCH_EntityUavStation station = resolveLinkedUavStation();
         if(station != null) {
            station.clearNewUavReturnState(player);
         }
         MCH_UavInventory.restorePilotInventory(player, inventoryReason);
      } else {
         pilot.setPosition(this.linkedUavStationX, safeY, this.linkedUavStationZ);
      }
      return true;
   }

   public static void updateNewUavSafeReturn(EntityPlayerMP player) {
      if(player == null) {
         return;
      }
      NewUavSafeReturn pending = NEW_UAV_SAFE_RETURNS.get(player.getUniqueID());
      if(pending == null) {
         return;
      }
      if(player.dimension != pending.dimension || player.isDead) {
         NEW_UAV_SAFE_RETURNS.remove(player.getUniqueID());
         return;
      }

      int blockX = MathHelper.floor_double(pending.x);
      int blockY = MathHelper.floor_double(pending.y);
      int blockZ = MathHelper.floor_double(pending.z);
      boolean stationChunkLoaded = player.worldObj.blockExists(blockX, blockY, blockZ);
      if(pending.ticks++ < NEW_UAV_SAFE_RETURN_MIN_TICKS || !stationChunkLoaded) {
         player.motionX = 0.0D;
         player.motionY = 0.0D;
         player.motionZ = 0.0D;
         player.fallDistance = 0.0F;
         player.setPositionAndUpdate(pending.x, pending.y, pending.z);
      } else {
         NEW_UAV_SAFE_RETURNS.remove(player.getUniqueID());
      }
   }

   private static final class NewUavSafeReturn {
      private final int dimension;
      private final double x;
      private final double y;
      private final double z;
      private int ticks;

      private NewUavSafeReturn(int dimension, double x, double y, double z) {
         this.dimension = dimension;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }

   /** Called by every concrete vehicle subclass when its pilot entity dies. */
   protected void handleDeadPilot() {
      Entity pilot = this.getRiddenByEntity();
      if(pilot == null || !pilot.isDead) {
         return;
      }
      if(this.isNewUAV() && !super.worldObj.isRemote) {
         this.returnNewUavPilotToStation(pilot, "uav_pilot_death");
      } else {
         this.unmountEntity();
      }
      super.riddenByEntity = null;
   }

   public void unmountEntity() {
      if(!this.isRidePlayer()) {
         this.switchHoveringMode(false);
      }

      this.moveLeft = this.moveRight = this.throttleDown = this.throttleUp = false;
      Entity rByEntity = null;
      if(super.riddenByEntity != null) {
         rByEntity = super.riddenByEntity;
         this.camera.initCamera(0, rByEntity);
         if(!super.worldObj.isRemote && this.isNewUAV()) {
            if(!this.returnNewUavPilotToStation(rByEntity, "uav_exit")) {
               return;
            }
         } else if(!super.worldObj.isRemote) {
            super.riddenByEntity.mountEntity((Entity)null);
         }
      } else if(this.lastRiddenByEntity != null) {
         rByEntity = this.lastRiddenByEntity;
         if(rByEntity instanceof EntityPlayer) {
            this.camera.initCamera(0, rByEntity);
         }
         if(!super.worldObj.isRemote && this.isNewUAV()
               && !this.returnNewUavPilotToStation(rByEntity, "uav_exit")) {
            return;
         }
      }

      MCH_Lib.DbgLog(super.worldObj, "unmountEntity:" + rByEntity, new Object[0]);
      if(!this.isRidePlayer()) {
         this.switchGunnerMode(false);
      }

      setCommonStatus(1, false);
      if(rByEntity != null) {
         // NewUAV takes precedence over the legacy UAV flag used by some content packs.
         if(this.isNewUAV()) {
            // The server already detached and returned the pilot above. Keep the live UAV
            // linked so Continue/relog can deterministically reacquire the same entity.
         } else if(this.isUAV()) {
            if(rByEntity.ridingEntity instanceof MCH_EntityUavStation) {
               rByEntity.mountEntity((Entity)null);
            }
         } else {
            setUnmountPosition(rByEntity, this.getSeatsInfo()[0].pos);
         }
      }

      super.riddenByEntity = null;
      this.lastRiddenByEntity = null;
      if(this.cs_dismountAll) {
         this.unmountCrew(false);
      }

   }

   public Entity getRidingEntity() {
      return super.ridingEntity;
   }

   public void startUnmountCrew() {
      this.isParachuting = true;
      if(this.haveHatch()) {
         this.foldHatch(true, true);
      }

   }

   public void stopUnmountCrew() {
      this.isParachuting = false;
   }

   public void unmountCrew() {
      if(this.getAcInfo() != null) {
         if(this.getAcInfo().haveRepellingHook()) {
            if(!this.isRepelling()) {
               if(MCH_Lib.getBlockIdY(this, 3, -4) > 0) {
                  this.unmountCrew(false);
               } else if(this.canStartRepelling()) {
                  this.startRepelling();
               }
            } else {
               this.stopRepelling();
            }
         } else if(this.isParachuting) {
            this.stopUnmountCrew();
         } else if(this.getAcInfo().isEnableParachuting && MCH_Lib.getBlockIdY(this, 3, -10) == 0) {
            this.startUnmountCrew();
         } else {
            this.unmountCrew(false);
         }

      }
   }

   public boolean isRepelling() {
      return this.getCommonStatus(5);
   }

   public void setRepellingStat(boolean b) {
      this.setCommonStatus(5, b);
   }

   public Vec3 getRopePos(int ropeIndex) {
      return this.getAcInfo() != null && this.getAcInfo().haveRepellingHook() && ropeIndex < this.getAcInfo().repellingHooks.size()?this.getTransformedPosition(((MCH_BaseVehicleInfo.RepellingHook)this.getAcInfo().repellingHooks.get(ropeIndex)).pos):Vec3.createVectorHelper(super.posX, super.posY, super.posZ);
   }

   private void startRepelling() {
      MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.startRepelling()", new Object[0]);
      this.setRepellingStat(true);
      this.throttleUp = false;
      this.throttleDown = false;
      this.moveLeft = false;
      this.moveRight = false;
      this.tickRepelling = 0;
   }

   private void stopRepelling() {
      MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.stopRepelling()", new Object[0]);
      this.setRepellingStat(false);
   }

   public static float abs(float p_76135_0_) {
      return p_76135_0_ >= 0.0F?p_76135_0_:-p_76135_0_;
   }

   public static double abs(double p_76135_0_) {
      return p_76135_0_ >= 0.0D?p_76135_0_:-p_76135_0_;
   }

   public boolean canStartRepelling() {
      if(this.getAcInfo().haveRepellingHook() && this.isHovering() && abs(this.getRotPitch()) < 3.0F && abs(this.getRotRoll()) < 3.0F) {
         Vec3 v = ((Vec3)this.prevPosition.oldest()).addVector(-super.posX, -super.posY, -super.posZ);
         if(v.lengthVector() < 0.3D) {
            return true;
         }
      }

      return false;
   }

   public boolean unmountCrew(boolean unmountParachute) {
      boolean ret = false;
      MCH_SeatInfo[] pos = this.getSeatsInfo();

      for(int i = 0; i < this.seats.length; ++i) {
         if(this.seats[i] != null && this.seats[i].riddenByEntity != null) {
            Entity entity = this.seats[i].riddenByEntity;
            if(!(entity instanceof EntityPlayer) && !(pos[i + 1] instanceof MCH_SeatRackInfo)) {
               Vec3 dropPos;
               if(unmountParachute) {
                  if(this.getSeatIdByEntity(entity) > 1) {
                     ret = true;
                     dropPos = this.getTransformedPosition(this.getAcInfo().mobDropOption.pos, (Vec3)this.prevPosition.oldest());
                     this.seats[i].posX = dropPos.xCoord;
                     this.seats[i].posY = dropPos.yCoord;
                     this.seats[i].posZ = dropPos.zCoord;
                     entity.mountEntity((Entity)null);
                     entity.posX = dropPos.xCoord;
                     entity.posY = dropPos.yCoord;
                     entity.posZ = dropPos.zCoord;
                     this.dropEntityParachute(entity);
                     break;
                  }
               } else {
                  ret = true;
                  dropPos = pos[i + 1].pos;
                  this.setUnmountPosition(this.seats[i], pos[i + 1].pos);
                  entity.mountEntity((Entity)null);
                  this.setUnmountPosition(entity, pos[i + 1].pos);
               }
            }
         }
      }

      return ret;
   }

   public void setUnmountPosition(Entity rByEntity, Vec3 pos) {

      if (!this.isNewUAV()) {

         if (rByEntity != null) {
            MCH_BaseVehicleInfo info = this.getAcInfo();
            Vec3 v;
            if (info != null && info.unmountPosition != null) {
               v = this.getTransformedPosition(info.unmountPosition);
            } else {
               double x = pos.xCoord;
               x = x >= 0.0D ? x + 3.0D : x - 3.0D;
               v = this.getTransformedPosition(x, 2.0D, pos.zCoord);
            }

            rByEntity.setPosition(v.xCoord, v.yCoord, v.zCoord);
            this.listUnmountReserve.add(new MCH_EntityBaseVehicle.UnmountReserve(rByEntity, v.xCoord, v.yCoord, v.zCoord));
         }

      } else if(rByEntity != null && !super.worldObj.isRemote) {
         this.returnNewUavPilotToStation(rByEntity, "uav_unmount_position");
      }

   }

   public boolean unmountEntityFromSeat(Entity entity) {
      if(entity == null || this.seats == null) {
         return false;
      }

      if(this.isNewUAV() && !super.worldObj.isRemote) {
         return this.returnNewUavPilotToStation(entity, "uav_seat_exit");
      }

      for(MCH_EntitySeat seat : this.seats) {
         if(seat != null && W_Entity.isEqual(seat.riddenByEntity, entity)) {
            entity.mountEntity((Entity)null);
            break;
         }
      }
      return false;
   }

   public void ejectSeat(Entity entity) {
      int sid = this.getSeatIdByEntity(entity);
      if(sid >= 0 && sid <= 1) {
         if(this.getGuiInventory().haveParachute()) {
            if(sid == 0) {
               this.getGuiInventory().consumeParachute();
               this.unmountEntity();
               this.ejectSeatSub(entity, 0);
               //idk how or why this decided to neck itself but hopefully this works

               if(player.ridingEntity instanceof MCH_EntityHeli) {
                  //System.out.println("player is riding heli");
                  this.attackEntityFrom(DamageSource.inWall, this.getMaxHP());
               }

               entity = this.getEntityBySeatId(1);
               if(entity instanceof EntityPlayer) {
                  entity = null;
               }
            }

            if(this.getGuiInventory().haveParachute() && entity != null) {
               this.getGuiInventory().consumeParachute();
               this.unmountEntityFromSeat(entity);
               this.ejectSeatSub(entity, 1);
            }
         }

      }
   }

   public void ejectSeatSub(Entity entity, int sid) {
      Vec3 pos = this.getSeatInfo(sid) != null?this.getSeatInfo(sid).pos:null;
      Vec3 v;
      if(pos != null) {
         v = this.getTransformedPosition(pos.xCoord, pos.yCoord + 2.0D, pos.zCoord);
         entity.setPosition(v.xCoord, v.yCoord, v.zCoord);
      }

      v = MCH_Lib.RotVec3(0.0D, 2.0D, 0.0D, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
      entity.motionX = super.motionX + v.xCoord + ((double)super.rand.nextFloat() - 0.5D) * 0.1D;
      entity.motionY = super.motionY + v.yCoord;
      entity.motionZ = super.motionZ + v.zCoord + ((double)super.rand.nextFloat() - 0.5D) * 0.1D;
      MCH_EntityParachute parachute = new MCH_EntityParachute(super.worldObj, entity.posX, entity.posY, entity.posZ);
      parachute.rotationYaw = entity.rotationYaw;
      parachute.motionX = entity.motionX;
      parachute.motionY = entity.motionY;
      parachute.motionZ = entity.motionZ;
      parachute.fallDistance = entity.fallDistance;
      parachute.user = entity;
      parachute.setType(2);
      super.worldObj.spawnEntityInWorld(parachute);
      if(this.getAcInfo().haveCanopy() && this.isCanopyClose()) {
         this.openCanopy_EjectSeat();
      }

      W_WorldFunc.MOD_playSoundAtEntity(entity, "eject_seat", 5.0F, 1.0F);
   }

   public boolean canEjectSeat(Entity entity) {
      int sid = this.getSeatIdByEntity(entity);
      return sid == 0 && this.isUAV()?false:sid >= 0 && sid < 2 && this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat;
   }

   public int getNumEjectionSeat() {
      return 0;
   }

   public int getMountedEntityNum() {
      int num = 0;
      if(super.riddenByEntity != null && !super.riddenByEntity.isDead) {
         ++num;
      }

      if(this.seats != null && this.seats.length > 0) {
         MCH_EntitySeat[] arr$ = this.seats;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntitySeat seat = arr$[i$];
            if(seat != null && seat.riddenByEntity != null && !seat.riddenByEntity.isDead) {
               ++num;
            }
         }
      }

      return num;
   }

   public void mountMobToSeats() {
      List list = super.worldObj.getEntitiesWithinAABB(W_Lib.getEntityLivingBaseClass(), super.boundingBox.expand(3.0D, 2.0D, 3.0D));

      for(int i = 0; i < list.size(); ++i) {
         Entity entity = (Entity)list.get(i);
         if(!(entity instanceof EntityPlayer) && entity.ridingEntity == null) {
            int sid = 1;
            MCH_EntitySeat[] arr$ = this.getSeats();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MCH_EntitySeat seat = arr$[i$];
               if(seat != null && seat.riddenByEntity == null && !this.isMountedEntity(entity) && this.canRideSeatOrRack(sid, entity)) {
                  if(this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                     break;
                  }

                  entity.mountEntity(seat);
               }

               ++sid;
            }
         }
      }

   }

   public void mountEntityToRack() {
      this.debugRackState("ADD-RACK-BEGIN");
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.EnablePutRackInFlying.prmBool) {
         if(this.getCurrentThrottle() > 0.3D) {
            MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][ADD-RACK-REJECT] reason=throttle_too_high throttle=%.3f carrier=%s",
                    new Object[]{Double.valueOf(this.getCurrentThrottle()), this.debugEntity(this)});
            return;
         }

         Block countRideEntity = MCH_Lib.getBlockY(this, 1, -3, true);
         if(countRideEntity == null || W_Block.isEqual(countRideEntity, Blocks.air)) {
            MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][ADD-RACK-REJECT] reason=carrier_not_grounded carrier=%s block=%s",
                    new Object[]{this.debugEntity(this), countRideEntity});
            return;
         }
      }

      int var12 = 0;

      for(int sid = 0; sid < this.getSeatNum(); ++sid) {
         MCH_EntitySeat seat = this.getSeat(sid);
         if(this.getSeatInfo(1 + sid) instanceof MCH_SeatRackInfo && seat != null && seat.riddenByEntity == null) {
            MCH_SeatRackInfo info = (MCH_SeatRackInfo)this.getSeatInfo(1 + sid);
            Vec3 v = MCH_Lib.RotVec3(info.getEntryPos().xCoord, info.getEntryPos().yCoord, info.getEntryPos().zCoord, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
            v.xCoord += super.posX;
            v.yCoord += super.posY;
            v.zCoord += super.posZ;
            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(v.xCoord, v.yCoord, v.zCoord, v.xCoord, v.yCoord, v.zCoord);
            float range = info.range;
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, bb.expand((double)range, (double)range, (double)range));

            for(int i = 0; i < list.size(); ++i) {
               Entity entity = (Entity)list.get(i);
               if(this.canRideSeatOrRack(1 + sid, entity)) {
                  if(entity instanceof MCH_IEntityCanRideBaseVehicle) {
                     if(((MCH_IEntityCanRideBaseVehicle)entity).canRideAircraft(this, sid, info)) {
                        MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.mountEntityToRack:%d:%s", new Object[]{Integer.valueOf(sid), entity});
                        entity.mountEntity(seat);
                        ++var12;
                        break;
                     } else if(entity instanceof MCH_EntityBaseVehicle && ((MCH_EntityBaseVehicle)entity).exceedsRackPayloadCapacity(this)) {
                        ((MCH_EntityBaseVehicle)entity).notifyRackPayloadExceeded(this);
                     }
                  } else if(entity.ridingEntity == null) {
                     NBTTagCompound nbt = entity.getEntityData();
                     if(nbt.hasKey("CanMountEntity") && nbt.getBoolean("CanMountEntity")) {
                        MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.mountEntityToRack:%d:%s:%s", new Object[]{Integer.valueOf(sid), entity, entity.getClass()});
                        entity.mountEntity(seat);
                        ++var12;
                        break;
                     }
                  }
               }
            }
         }
      }

      if(var12 > 0) {
         W_WorldFunc.DEF_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "random.click", 1.0F, 1.0F);
         MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][ADD-RACK-ACCEPT] mounted=%d carrier=%s",
                 new Object[]{Integer.valueOf(var12), this.debugEntity(this)});
      } else {
         MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][ADD-RACK-REJECT] reason=no_eligible_entity_or_rack carrier=%s",
                 new Object[]{this.debugEntity(this)});
         this.debugRackState("ADD-RACK-REJECT");
      }

   }

   public void unmountEntityFromRack() {
      for(int sid = this.getSeatNum() - 1; sid >= 0; --sid) {
         MCH_EntitySeat seat = this.getSeat(sid);
         if(this.getSeatInfo(sid + 1) instanceof MCH_SeatRackInfo && seat != null && seat.riddenByEntity != null) {
            MCH_SeatRackInfo info = (MCH_SeatRackInfo)this.getSeatInfo(sid + 1);
            Entity entity = seat.riddenByEntity;
            Vec3 rackUnmountPosition = this.getRackUnmountPosition(info);
            if(rackUnmountPosition != null) {
               seat.posX = entity.posX = rackUnmountPosition.xCoord;
               seat.posY = entity.posY = rackUnmountPosition.yCoord;
               seat.posZ = entity.posZ = rackUnmountPosition.zCoord;
               entity.rotationYaw = this.getRotYaw() + info.fixYaw;
               entity.rotationPitch = info.fixPitch;
            } else {
               Vec3 pos = info.getEntryPos();
               if(entity instanceof MCH_EntityBaseVehicle) {
                  if(pos.zCoord >= (double)this.getAcInfo().bbZ) {
                     pos = pos.addVector(0.0D, 0.0D, 12.0D);
                  } else {
                     pos = pos.addVector(0.0D, 0.0D, -12.0D);
                  }
               }

               Vec3 v = MCH_Lib.RotVec3(pos.xCoord, pos.yCoord, pos.zCoord, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
               seat.posX = entity.posX = super.posX + v.xCoord;
               seat.posY = entity.posY = super.posY + v.yCoord;
               seat.posZ = entity.posZ = super.posZ + v.zCoord;
            }
            if(!(entity instanceof MCH_EntityBaseVehicle)) {
               MCH_EntityBaseVehicle.UnmountReserve ur = new MCH_EntityBaseVehicle.UnmountReserve(entity, entity.posX, entity.posY, entity.posZ);
               ur.cnt = 8;
               this.listUnmountReserve.add(ur);
            }
            entity.mountEntity((Entity)null);
            boolean launchedAircraft = entity instanceof MCH_EntityBaseVehicle && this.isLaunchRack(this, info);
            if(entity instanceof MCH_EntityBaseVehicle) {
               ((MCH_EntityBaseVehicle)entity).applyRackLaunch(this, info);
            }
            if(launchedAircraft || MCH_Lib.getBlockIdY(this, 3, -20) > 0) {
               MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.unmountEntityFromRack:%d:%s", new Object[]{Integer.valueOf(sid), entity});
            } else {
               MCH_Lib.DbgLog(super.worldObj, "MCH_EntityBaseVehicle.unmountEntityFromRack:%d Parachute:%s", new Object[]{Integer.valueOf(sid), entity});
               this.dropEntityParachute(entity);
            }
            break;
         }
      }

   }

   public void dropEntityParachute(Entity entity) {
      if(entity instanceof MCH_EntityBaseVehicle) {
         MCH_BaseVehicleInfo info = ((MCH_EntityBaseVehicle)entity).getAcInfo();
         double weight = info != null?Math.max(0.0D, info.weight):0.0D;
         if(weight > MCH_EntityParachute.MAX_CARGO_AIRDROP_WEIGHT_LB) {
            MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][PARADROP-REJECT] reason=overweight vehicle=%s weight=%.1f max=%.1f",
                    new Object[]{this.debugEntity(entity), Double.valueOf(weight), Double.valueOf(MCH_EntityParachute.MAX_CARGO_AIRDROP_WEIGHT_LB)});
            EntityPlayer player = ((MCH_EntityBaseVehicle)entity).getFirstMountPlayer();
            if(player == null) {
               player = this.getFirstMountPlayer();
            }
            if(player != null) {
               player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Cannot paradrop " + this.getVehicleDisplayName((MCH_EntityBaseVehicle)entity) + ": vehicle weighs " + this.formatRackPounds(weight) + " lb, exceeding the " + this.formatRackPounds(MCH_EntityParachute.MAX_CARGO_AIRDROP_WEIGHT_LB) + " lb cargo airdrop limit."));
            }
            return;
         }
      }
      entity.motionX = super.motionX;
      entity.motionY = super.motionY;
      entity.motionZ = super.motionZ;
      MCH_EntityParachute parachute = new MCH_EntityParachute(super.worldObj, entity.posX, entity.posY, entity.posZ);
      parachute.rotationYaw = entity.rotationYaw;
      parachute.motionX = entity.motionX;
      parachute.motionY = entity.motionY;
      parachute.motionZ = entity.motionZ;
      parachute.fallDistance = entity.fallDistance;
      parachute.user = entity;
      parachute.setType(3);
      super.worldObj.spawnEntityInWorld(parachute);
   }

   public void rideRack() {
      MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RIDE-BEGIN] child=%s type=%s riding=%s",
              new Object[]{this.debugEntity(this), this.getTypeName(), this.debugEntity(super.ridingEntity)});
      if(super.ridingEntity != null) {
         MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RIDE-REJECT] reason=child_already_riding child=%s parent=%s",
                 new Object[]{this.debugEntity(this), this.debugEntity(super.ridingEntity)});
         return;
      }

      AxisAlignedBB bb = this.getBoundingBox();
      List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, bb.expand(60.0D, 60.0D, 60.0D));
      int carrierCount = 0;
      int rackCount = 0;
      for(int i = 0; i < list.size(); ++i) {
         Entity entity = (Entity)list.get(i);
         if(!(entity instanceof MCH_EntityBaseVehicle)) {
            continue;
         }
         ++carrierCount;
         MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)entity;
         ac.repairInvalidOccupantsForInteraction("ride_rack_candidate");
         ac.debugRackState("RIDE-CANDIDATE");
         if(ac.getAcInfo() == null) {
            MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][CARRIER-REJECT] reason=aircraft_info_null carrier=%s", new Object[]{this.debugEntity(ac)});
            continue;
         }
         for(int sid = 0; sid < ac.getSeatNum(); ++sid) {
            MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);
            if(!(seatInfo instanceof MCH_SeatRackInfo)) {
               continue;
            }
            ++rackCount;
            MCH_SeatRackInfo info = (MCH_SeatRackInfo)seatInfo;
            MCH_EntitySeat seat = ac.resolveSeatReferenceForInteraction(sid, "ride_rack");
            if(seat == null) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RACK-REJECT] reason=seat_reference_null carrier=%s rack=%d",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid)});
               continue;
            }
            if(seat.isDead) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RACK-REJECT] reason=seat_dead carrier=%s rack=%d seat=%s",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid), this.debugEntity(seat)});
               continue;
            }
            if(seat.riddenByEntity != null) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RACK-REJECT] reason=occupied carrier=%s rack=%d child=%s",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid), this.debugEntity(seat.riddenByEntity)});
               continue;
            }
            if(!ac.canRideSeatOrRack(1 + sid, this)) {
               MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RACK-REJECT] reason=carrier_seat_exclusion carrier=%s rack=%d",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid)});
               continue;
            }
            Vec3 v = ac.getTransformedPosition(info.getEntryPos());
            float r = info.range;
            boolean inRange = super.posX >= v.xCoord - (double)r && super.posX <= v.xCoord + (double)r
                    && super.posY >= v.yCoord - (double)r && super.posY <= v.yCoord + (double)r
                    && super.posZ >= v.zCoord - (double)r && super.posZ <= v.zCoord + (double)r;
            if(!inRange) {
               MCH_Lib.DbgLog(super.worldObj,
                       "[MCH-RACK][RACK-REJECT] reason=outside_entry_range carrier=%s rack=%d childPos=%.2f,%.2f,%.2f entry=%.2f,%.2f,%.2f range=%.2f",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid), Double.valueOf(super.posX), Double.valueOf(super.posY),
                               Double.valueOf(super.posZ), Double.valueOf(v.xCoord), Double.valueOf(v.yCoord), Double.valueOf(v.zCoord), Float.valueOf(r)});
               continue;
            }
            if(!this.canRideAircraft(ac, sid, info)) {
               if(this.exceedsRackPayloadCapacity(ac)) {
                  this.notifyRackPayloadExceeded(ac);
               }
               MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RACK-REJECT] reason=child_type_or_nested_rack_or_payload_check carrier=%s rack=%d child=%s",
                       new Object[]{this.debugEntity(ac), Integer.valueOf(sid), this.debugEntity(this)});
               continue;
            }
            W_WorldFunc.DEF_playSoundEffect(super.worldObj, super.posX, super.posY, super.posZ, "random.click", 1.0F, 1.0F);
            this.mountEntity(seat);
            MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RIDE-ACCEPT] carrier=%s rack=%d seat=%s child=%s",
                    new Object[]{this.debugEntity(ac), Integer.valueOf(sid), this.debugEntity(seat), this.debugEntity(this)});
            return;
         }
      }
      MCH_Lib.DbgLog(super.worldObj, "[MCH-RACK][RIDE-REJECT] reason=no_eligible_rack nearbyEntities=%d carriers=%d racks=%d child=%s",
              new Object[]{Integer.valueOf(list.size()), Integer.valueOf(carrierCount), Integer.valueOf(rackCount), this.debugEntity(this)});
   }


   public boolean canPutToRack() {
      for(int i = 0; i < this.getSeatNum(); ++i) {
         MCH_EntitySeat seat = this.getSeat(i);
         MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
         if(seat != null && seat.riddenByEntity == null && seatInfo instanceof MCH_SeatRackInfo) {
            return true;
         }
      }

      return false;
   }

   public boolean canDownFromRack() {
      for(int i = 0; i < this.getSeatNum(); ++i) {
         MCH_EntitySeat seat = this.getSeat(i);
         MCH_SeatInfo seatInfo = this.getSeatInfo(i + 1);
         if(seat != null && seat.riddenByEntity != null && seatInfo instanceof MCH_SeatRackInfo) {
            return true;
         }
      }

      return false;
   }



   public void checkRideRack() {
      //todo carrier stuff here
      if(this.getCountOnUpdate() % 10 == 0) {
         this.canRideRackStatus = false;
         if(super.ridingEntity == null) {
            AxisAlignedBB bb = this.getBoundingBox();
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, bb.expand(60.0D, 60.0D, 60.0D));

            for(int i = 0; i < list.size(); ++i) {
               Entity entity = (Entity)list.get(i);
               if(entity instanceof MCH_EntityBaseVehicle) {
                  MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)entity;
                  if(ac.getAcInfo() != null) {
                     for(int sid = 0; sid < ac.getSeatNum(); ++sid) {
                        MCH_SeatInfo seatInfo = ac.getSeatInfo(1 + sid);
                        if(seatInfo instanceof MCH_SeatRackInfo) {
                           MCH_SeatRackInfo info = (MCH_SeatRackInfo)seatInfo;
                           MCH_EntitySeat seat = ac.getSeat(sid);
                           if(seat != null && seat.riddenByEntity == null) {
                              Vec3 v = ac.getTransformedPosition(info.getEntryPos());
                              float r = info.range;
                              boolean var10000;
                              if(super.posX >= v.xCoord - (double)r && super.posX <= v.xCoord + (double)r) {
                                 var10000 = true;
                              } else {
                                 var10000 = false;
                              }

                              if(super.posY >= v.yCoord - (double)r && super.posY <= v.yCoord + (double)r) {
                                 var10000 = true;
                              } else {
                                 var10000 = false;
                              }

                              if(super.posZ >= v.zCoord - (double)r && super.posZ <= v.zCoord + (double)r) {
                                 var10000 = true;
                              } else {
                                 var10000 = false;
                              }

                              if(super.posX >= v.xCoord - (double)r && super.posX <= v.xCoord + (double)r && super.posY >= v.yCoord - (double)r && super.posY <= v.yCoord + (double)r && super.posZ >= v.zCoord - (double)r && super.posZ <= v.zCoord + (double)r && this.canRideAircraft(ac, sid, info)) {
                                 this.canRideRackStatus = true;
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public boolean canRideRack() {
      return super.ridingEntity == null && this.canRideRackStatus;
   }

   private boolean exceedsRackPayloadCapacity(MCH_EntityBaseVehicle carrier) {
      return this.getRackPayloadAfterMount(carrier) < 0.0D;
   }

   private double getRackPayloadAfterMount(MCH_EntityBaseVehicle carrier) {
      if(carrier == null || carrier.getAcInfo() == null || this.getAcInfo() == null) {
         return 0.0D;
      }

      double payloadCapacity = Math.max(0.0D, carrier.getAcInfo().maximumExternalPayloadCapacity);
      double mountedWeight = carrier.getMountedRackPayloadWeight();
      double carriedWeight = Math.max(0.0D, this.getAcInfo().weight);
      return payloadCapacity - mountedWeight - carriedWeight;
   }

   private double getMountedRackPayloadWeight() {
      double mountedWeight = 0.0D;

      for(int sid = 0; sid < this.getSeatNum(); ++sid) {
         MCH_SeatInfo seatInfo = this.getSeatInfo(sid + 1);
         MCH_EntitySeat seat = this.getSeat(sid);
         if(seatInfo instanceof MCH_SeatRackInfo && seat != null && seat.riddenByEntity instanceof MCH_EntityBaseVehicle) {
            MCH_BaseVehicleInfo mountedInfo = ((MCH_EntityBaseVehicle)seat.riddenByEntity).getAcInfo();
            if(mountedInfo != null) {
               mountedWeight += Math.max(0.0D, mountedInfo.weight);
            }
         }
      }

      return mountedWeight;
   }

   private void notifyRackPayloadExceeded(MCH_EntityBaseVehicle carrier) {
      EntityPlayer player = this.getFirstMountPlayer();
      if(player == null && carrier != null) {
         player = carrier.getFirstMountPlayer();
      }
      if(player != null) {
         double payloadCapacity = carrier != null && carrier.getAcInfo() != null?Math.max(0.0D, carrier.getAcInfo().maximumExternalPayloadCapacity):0.0D;
         double mountedWeight = carrier != null?carrier.getMountedRackPayloadWeight():0.0D;
         double carriedWeight = this.getAcInfo() != null?Math.max(0.0D, this.getAcInfo().weight):0.0D;
         double remainingCapacity = Math.max(0.0D, payloadCapacity - mountedWeight);
         player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Cannot mount rack: " + this.getVehicleDisplayName(this) + " weighs " + this.formatRackPounds(carriedWeight) + " lb, but " + this.getVehicleDisplayName(carrier) + " only has " + this.formatRackPounds(remainingCapacity) + " lb of external payload capacity remaining (" + this.formatRackPounds(payloadCapacity) + " lb max)."));
      }
   }

   private String getVehicleDisplayName(MCH_EntityBaseVehicle vehicle) {
      if(vehicle != null && vehicle.getAcInfo() != null && vehicle.getAcInfo().displayName != null && !vehicle.getAcInfo().displayName.isEmpty()) {
         return vehicle.getAcInfo().displayName;
      }

      return vehicle != null?vehicle.getCommandSenderName():"vehicle";
   }

   private String formatRackPounds(double pounds) {
      if(Math.abs(pounds - (double)((long)pounds)) < 0.001D) {
         return String.valueOf((long)pounds);
      }

      return String.format(java.util.Locale.ROOT, "%.1f", pounds);
   }

   public boolean canRideAircraft(MCH_EntityBaseVehicle ac, int seatID, MCH_SeatRackInfo info) {
      if(this.getAcInfo() == null) {
         return false;
      } else if(ac.ridingEntity != null) {
         return false;
      } else if(super.ridingEntity != null) {
         return false;
      } else if(ac instanceof MCH_EntityShip && !this.getAcInfo().canMountShip) {
         return false;
      } else {
         boolean canRide = false;
         String[] arr$ = info.names;
         int len$ = arr$.length;

         int i$;
         for(i$ = 0; i$ < len$; ++i$) {
            String seat = arr$[i$];
            if(seat.equalsIgnoreCase(this.getAcInfo().name) || seat.equalsIgnoreCase(this.getAcInfo().getKindName())) {
               canRide = true;
               break;
            }
         }

         MCH_EntitySeat var12;
         if(!canRide) {
            Iterator var9 = this.getAcInfo().rideRacks.iterator();

            while(var9.hasNext()) {
               MCH_BaseVehicleInfo.RideRack var11 = (MCH_BaseVehicleInfo.RideRack)var9.next();
               i$ = ac.getAcInfo().getNumSeat() - 1 + (var11.rackID - 1);
               if(i$ == seatID && var11.name.equalsIgnoreCase(ac.getAcInfo().name)) {
                  var12 = ac.getSeat(ac.getAcInfo().getNumSeat() - 1 + var11.rackID - 1);
                  if(var12 != null && var12.riddenByEntity == null) {
                     canRide = true;
                     break;
                  }
               }
            }

            if(!canRide) {
               return false;
            }
         }

         if(this.exceedsRackPayloadCapacity(ac)) {
            return false;
         }

         MCH_EntitySeat[] var10 = this.getSeats();
         len$ = var10.length;

         for(i$ = 0; i$ < len$; ++i$) {
            var12 = var10[i$];
            if(var12 != null && var12.riddenByEntity instanceof MCH_IEntityCanRideBaseVehicle) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isMountedEntity(Entity entity) {
      return entity == null?false:this.isMountedEntity(W_Entity.getEntityId(entity));
   }

   public EntityPlayer getFirstMountPlayer() {
      if(this.getRiddenByEntity() instanceof EntityPlayer) {
         return (EntityPlayer)this.getRiddenByEntity();
      } else {
         MCH_EntitySeat[] arr$ = this.getSeats();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntitySeat seat = arr$[i$];
            if(seat != null && seat.riddenByEntity instanceof EntityPlayer) {
               return (EntityPlayer)seat.riddenByEntity;
            }
         }

         return null;
      }
   }

   public boolean isMountedSameTeamEntity(EntityLivingBase player) {
      if(player != null && player.getTeam() != null) {
         if(super.riddenByEntity instanceof EntityLivingBase && player.isOnSameTeam((EntityLivingBase)super.riddenByEntity)) {
            return true;
         } else {
            MCH_EntitySeat[] arr$ = this.getSeats();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MCH_EntitySeat seat = arr$[i$];
               if(seat != null && seat.riddenByEntity instanceof EntityLivingBase && player.isOnSameTeam((EntityLivingBase)seat.riddenByEntity)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isMountedOtherTeamEntity(EntityLivingBase player) {
      if(player == null) {
         return false;
      } else {
         EntityLivingBase target = null;
         if(super.riddenByEntity instanceof EntityLivingBase) {
            target = (EntityLivingBase)super.riddenByEntity;
            if(player.getTeam() != null && target.getTeam() != null && !player.isOnSameTeam(target)) {
               return true;
            }
         }

         MCH_EntitySeat[] arr$ = this.getSeats();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntitySeat seat = arr$[i$];
            if(seat != null && seat.riddenByEntity instanceof EntityLivingBase) {
               target = (EntityLivingBase)seat.riddenByEntity;
               if(player.getTeam() != null && target.getTeam() != null && !player.isOnSameTeam(target)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public boolean isMountedEntity(int entityId) {
      if(W_Entity.getEntityId(super.riddenByEntity) == entityId) {
         return true;
      } else {
         MCH_EntitySeat[] arr$ = this.getSeats();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_EntitySeat seat = arr$[i$];
            if(seat != null && seat.riddenByEntity != null && W_Entity.getEntityId(seat.riddenByEntity) == entityId) {
               return true;
            }
         }

         return false;
      }
   }

   public void onInteractFirst(EntityPlayer player) {}

   public boolean checkTeam(EntityPlayer player) {
      for(int i = 0; i < 1 + this.getSeatNum(); ++i) {
         Entity entity = this.getEntityBySeatId(i);
         if(entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
            EntityLivingBase riddenPlayer = (EntityLivingBase)entity;
            if(riddenPlayer.getTeam() != null && !riddenPlayer.isOnSameTeam((EntityLivingBase)player)) {
               return false;
            }
         }
      }

      return true;
   }

   private String debugEntity(Entity entity) {
      if(entity == null) {
         return "null";
      }
      Entity resolved = entity.worldObj == null?null:entity.worldObj.getEntityByID(entity.getEntityId());
      return String.format("%s{id=%d,uuid=%s,dead=%s,side=%s,loaded=%s,riding=%d,rider=%d}",
              entity.getClass().getSimpleName(), Integer.valueOf(entity.getEntityId()), entity.getUniqueID(),
              Boolean.valueOf(entity.isDead), entity.worldObj != null && entity.worldObj.isRemote?"CLIENT":"SERVER",
              Boolean.valueOf(resolved == entity), Integer.valueOf(W_Entity.getEntityId(entity.ridingEntity)),
              Integer.valueOf(W_Entity.getEntityId(entity.riddenByEntity)));
   }

   public void debugVehicleState(String context, EntityPlayer player) {
      if(!MCH_Config.EnableMCHLibDebugLog.prmBool) {
         return;
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-STATE][%s] vehicle=%s type=%s commonId=%s player=%s pilot=%s seatCount=%d",
              new Object[]{context, this.debugEntity(this), this.getTypeName(), this.getCommonUniqueId(),
                      this.debugEntity(player), this.debugEntity(super.riddenByEntity), Integer.valueOf(this.getSeats().length)});
      for(int i = 0; i < this.getSeats().length; ++i) {
         MCH_EntitySeat seat = this.getSeats()[i];
         MCH_Lib.DbgLog(super.worldObj,
                 "[MCH-STATE][%s][SEAT] index=%d seat=%s seatId=%d parent=%s parentCommonId=%s occupant=%s",
                 new Object[]{context, Integer.valueOf(i), this.debugEntity(seat), Integer.valueOf(seat == null?-1:seat.seatID),
                         seat == null?"null":this.debugEntity(seat.getParent()), seat == null?"":seat.parentUniqueID,
                         seat == null?"null":this.debugEntity(seat.riddenByEntity)});
      }
   }

   public void debugRackState(String context) {
      if(!MCH_Config.EnableMCHLibDebugLog.prmBool) {
         return;
      }
      int rackCount = 0;
      for(int i = 0; i < this.getSeatNum(); ++i) {
         if(this.getSeatInfo(i + 1) instanceof MCH_SeatRackInfo) {
            ++rackCount;
         }
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-RACK][%s] carrier=%s type=%s racks=%d pendingParent=%s pendingSeat=%d riding=%s",
              new Object[]{context, this.debugEntity(this), this.getTypeName(), Integer.valueOf(rackCount),
                      this.pendingRackParentUniqueId, Integer.valueOf(this.pendingRackSeatId), this.debugEntity(super.ridingEntity)});
      for(int i = 0; i < this.getSeatNum(); ++i) {
         MCH_SeatInfo info = this.getSeatInfo(i + 1);
         if(info instanceof MCH_SeatRackInfo) {
            MCH_EntitySeat seat = this.getSeat(i);
            MCH_Lib.DbgLog(super.worldObj,
                    "[MCH-RACK][%s][RACK] index=%d seat=%s occupied=%s child=%s",
                    new Object[]{context, Integer.valueOf(i), this.debugEntity(seat),
                            Boolean.valueOf(seat != null && seat.riddenByEntity != null),
                            seat == null?"null":this.debugEntity(seat.riddenByEntity)});
         }
      }
   }

   private MCH_EntitySeat resolveSeatReferenceForInteraction(int seatIndex, String context) {
      MCH_EntitySeat current = this.getSeat(seatIndex);
      if(current != null && !current.isDead && current.worldObj == super.worldObj
              && current.seatID == seatIndex && current.getParent() == this) {
         return current;
      }
      if(super.worldObj.isRemote || this.getCommonUniqueId().isEmpty()) {
         return current;
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-STATE][SEAT-RESOLVE] context=%s vehicle=%s index=%d stale=%s commonId=%s",
              new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex), this.debugEntity(current), this.getCommonUniqueId()});
      for(Object object : super.worldObj.loadedEntityList) {
         if(object instanceof MCH_EntitySeat) {
            MCH_EntitySeat candidate = (MCH_EntitySeat)object;
            if(!candidate.isDead && candidate.seatID == seatIndex
                    && this.getCommonUniqueId().equals(candidate.parentUniqueID)) {
               candidate.setParent(this);
               this.setSeat(seatIndex, candidate);
               MCH_Lib.DbgLog(super.worldObj,
                       "[MCH-STATE][SEAT-RESOLVE-ACCEPT] context=%s vehicle=%s index=%d seat=%s",
                       new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex), this.debugEntity(candidate)});
               return candidate;
            }
         }
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-STATE][SEAT-RESOLVE-FAIL] context=%s vehicle=%s index=%d reason=no_loaded_seat_with_parent_id",
              new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex)});
      MCH_EntitySeat recreated = this.recreateSeatInLoadedChunk(seatIndex, context);
      if(recreated != null) {
         return recreated;
      }
      return current != null && !current.isDead && current.worldObj == super.worldObj
              && current.seatID == seatIndex && current.getParent() == this?current:null;
   }

   private MCH_EntitySeat recreateSeatInLoadedChunk(int seatIndex, String context) {
      if(super.worldObj.isRemote || this.getAcInfo() == null || this.getCommonUniqueId().isEmpty()
              || seatIndex < 0 || seatIndex >= this.seats.length) {
         return null;
      }
      MCH_SeatInfo seatInfo = this.getSeatInfo(seatIndex + 1);
      if(seatInfo == null) {
         return null;
      }
      Vec3 position = this.getTransformedPosition(seatInfo.pos);
      if(!super.worldObj.blockExists(MathHelper.floor_double(position.xCoord),
              MathHelper.floor_double(position.yCoord), MathHelper.floor_double(position.zCoord))) {
         MCH_Lib.DbgLog(super.worldObj,
                 "[MCH-STATE][SEAT-RECREATE-SKIP] context=%s vehicle=%s index=%d reason=seat_chunk_not_loaded",
                 new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex)});
         return null;
      }
      MCH_EntitySeat seat = new MCH_EntitySeat(super.worldObj);
      seat.parentUniqueID = this.getCommonUniqueId();
      seat.seatID = seatIndex;
      seat.setParent(this);
      seat.setPosition(position.xCoord, position.yCoord, position.zCoord);
      seat.prevPosX = position.xCoord;
      seat.prevPosY = position.yCoord;
      seat.prevPosZ = position.zCoord;
      if(super.worldObj.spawnEntityInWorld(seat)) {
         this.setSeat(seatIndex, seat);
         MCH_Lib.DbgLog(super.worldObj,
                 "[MCH-STATE][SEAT-RECREATE] context=%s vehicle=%s index=%d seat=%s",
                 new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex), this.debugEntity(seat)});
         return seat;
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-STATE][SEAT-RECREATE-FAIL] context=%s vehicle=%s index=%d reason=spawn_rejected",
              new Object[]{context, this.debugEntity(this), Integer.valueOf(seatIndex)});
      return null;
   }

   private void repairInvalidOccupantsForInteraction(String context) {
      if(super.worldObj.isRemote) {
         return;
      }
      if(super.riddenByEntity != null && (super.riddenByEntity.isDead || super.riddenByEntity.ridingEntity != this
              || !super.worldObj.loadedEntityList.contains(super.riddenByEntity))) {
         MCH_Lib.DbgLog(super.worldObj,
                 "[MCH-STATE][REPAIR] context=%s reason=invalid_pilot_backreference vehicle=%s stalePilot=%s",
                 new Object[]{context, this.debugEntity(this), this.debugEntity(super.riddenByEntity)});
         super.riddenByEntity = null;
      }
      for(int i = 0; i < this.getSeats().length; ++i) {
         this.clearInvalidSeatOccupant(this.getSeats()[i], i, context);
      }
   }

   /**
    * Breaks only invalid direct-riding relationships owned by a dying player.
    * Remote UAV control is deliberately excluded because it is not a normal
    * Entity riding relationship and has its own death/inventory lifecycle.
    */
   public void clearDeadNormalVehicleRider(EntityPlayerMP player) {
      if(player == null || super.worldObj.isRemote || this.isUAV() || this.isNewUAV()) {
         return;
      }
      if(super.riddenByEntity == player) {
         super.riddenByEntity = null;
         MCH_PacketNotifyOnMountEntity.sendDismount(this, player);
      }
      for(int i = 0; i < this.seats.length; ++i) {
         MCH_EntitySeat seat = this.seats[i];
         if(seat != null && seat.riddenByEntity == player) {
            seat.riddenByEntity = null;
            MCH_PacketNotifyOnMountEntity.sendDismount(this, player);
         }
      }
      if(player.ridingEntity == this || player.ridingEntity instanceof MCH_EntitySeat
            && ((MCH_EntitySeat)player.ridingEntity).getParent() == this) {
         player.mountEntity((Entity)null);
      }
   }

   private boolean clearInvalidSeatOccupant(MCH_EntitySeat seat, int seatIndex, String context) {
      if(seat == null || seat.riddenByEntity == null) {
         return false;
      }
      Entity occupant = seat.riddenByEntity;
      if(!occupant.isDead && occupant.ridingEntity == seat
              && (super.worldObj.isRemote || super.worldObj.loadedEntityList.contains(occupant))) {
         return false;
      }
      MCH_Lib.DbgLog(super.worldObj,
              "[MCH-STATE][REPAIR] context=%s reason=invalid_seat_occupant_backreference side=%s vehicle=%s seat=%d staleOccupant=%s",
              new Object[]{context, super.worldObj.isRemote?"CLIENT":"SERVER", this.debugEntity(this), Integer.valueOf(seatIndex), this.debugEntity(occupant)});
      seat.riddenByEntity = null;
      return true;
   }

   private boolean rejectInteraction(EntityPlayer player, String reason) {
      return false;
   }

   private void acceptInteraction(EntityPlayer player, String result) {}

   public boolean interactFirst(EntityPlayer player, boolean ss) {
      this.switchSeat = ss;
      boolean ret = this.interactFirst(player);
      this.switchSeat = false;
      return ret;
   }

   public boolean interactFirst(EntityPlayer player) {
      this.repairInvalidOccupantsForInteraction("vehicle_interact");
      if(isDestroyed()) {
         return this.rejectInteraction(player, "destroyed");
      }
      if(getAcInfo() == null) {
         return this.rejectInteraction(player, "aircraft_info_null");
      }
      if(!checkTeam(player)) {
         return this.rejectInteraction(player, "team_check_failed");
      }
      ItemStack itemStack = player.getCurrentEquippedItem();
      if(itemStack != null && itemStack.getItem() instanceof mcheli.tool.MCH_ItemWrench) {
         if(!this.worldObj.isRemote && player.isSneaking()) {
            switchNextTextureName();
         }
         return this.rejectInteraction(player, "wrench_action");
      }
      if(itemStack != null) {
         MCH_ItemInfo itemInfo = MCH_ItemInfoManager.get(itemStack.getItem());
         if(itemInfo != null && itemInfo.textureOverlay) {
            if(!this.worldObj.isRemote) {
               this.setTextureName(getSkinOverlayTextureName(this.getTextureName(), itemInfo.name));
               if(!player.capabilities.isCreativeMode) {
                  --itemStack.stackSize;
                  if(itemStack.stackSize <= 0) {
                     player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
                  }
               }
            }
            return true;
         }
      }
      if(itemStack != null && itemStack.getItem() instanceof mcheli.mob.MCH_ItemSpawnGunner) {
         return this.rejectInteraction(player, "gunner_item");
      }
      if(player.isSneaking()) {
         openInventory(player);
         return this.rejectInteraction(player, "inventory_opened");
      }
      if(!getAcInfo().canRide) {
         return this.rejectInteraction(player, "config_canRide_false");
      }
      if(this.riddenByEntity == null && !isUAV() && !isNewUAV()) {
         if(player.ridingEntity instanceof MCH_EntitySeat) {
            return this.rejectInteraction(player, "player_already_riding_seat");
         }
         if(!canRideSeatOrRack(0, player)) {
            return this.rejectInteraction(player, "pilot_seat_exclusion");
         }
         if(!super.worldObj.isRemote && !this.switchSeat && !this.canPlayerEnterVehicle(player)) {
            this.notifyVehicleAccessDenied(player);
            return false;
         }
         if(!this.switchSeat) {
            if(getAcInfo().haveCanopy() && isCanopyClose()) {
               openCanopy();
               return this.rejectInteraction(player, "canopy_opened_retry_required");
            }
            if(getModeSwitchCooldown() > 0) {
               return this.rejectInteraction(player, "mode_switch_cooldown_" + getModeSwitchCooldown());
            }
         }
         closeCanopy();
         this.lastRiddenByEntity = null;
         initRadar();
         if(!this.worldObj.isRemote) {
            this.clearPlacementMotionLock();
            player.mountEntity(this);
            if(player.ridingEntity == this) {
               MCH_PacketNotifyOnMountEntity.sendToRider(this, player, 0);
            }
            if(player.ridingEntity == this && this.vehicleOwnerUUID == null) {
               this.vehicleOwnerUUID = player.getUniqueID();
            }
            if(!this.keepOnRideRotation) {
               mountMobToSeats();
            }
         } else {
            updateClientSettings(0);
         }
         setCameraId(0);
         initPilotWeapon();
         this.lowPassPartialTicks.clear();
         onInteractFirst(player);
         this.acceptInteraction(player, "pilot_mount");
         return true;
      }

      boolean seatResult = interactFirstSeat(player);
      if(seatResult) {
         this.acceptInteraction(player, "seat_search_requested pilotOccupied=" + this.debugEntity(this.riddenByEntity));
      } else {
         this.rejectInteraction(player, "no_valid_seat");
      }
      return seatResult;
   }



   public boolean canRideSeatOrRack(int seatId, Entity entity) {
      if(this.getAcInfo() == null) {
         return false;
      } else {
         Iterator i$ = this.getAcInfo().exclusionSeatList.iterator();

         while(i$.hasNext()) {
            Integer[] a = (Integer[])i$.next();
            if(Arrays.asList(a).contains(Integer.valueOf(seatId))) {
               Integer[] arr$ = a;
               int len$ = a.length;

               for(int i$1 = 0; i$1 < len$; ++i$1) {
                  int id = arr$[i$1].intValue();
                  if(this.getEntityBySeatId(id) != null) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }

   public void updateClientSettings(int seatId) {
      MCH_Config var10001 = MCH_MOD.config;
      this.cs_dismountAll = MCH_Config.DismountAll.prmBool;
      var10001 = MCH_MOD.config;
      this.cs_heliAutoThrottleDown = MCH_Config.AutoThrottleDownHeli.prmBool;
      var10001 = MCH_MOD.config;
      this.cs_planeAutoThrottleDown = MCH_Config.AutoThrottleDownPlane.prmBool;
      var10001 = MCH_MOD.config;
      this.cs_tankAutoThrottleDown = MCH_Config.AutoThrottleDownTank.prmBool;
      var10001 = MCH_MOD.config;
      this.cs_shipAutoThrottleDown = MCH_Config.AutoThrottleDownShip.prmBool;

      this.camera.setShaderSupport(seatId, Boolean.valueOf(W_EntityRenderer.isShaderSupport()));
      MCH_PacketNotifyClientSetting.send();
   }

   public boolean canLockEntity(Entity entity) {
      return !this.isMountedEntity(entity);
   }

   public void switchNextSeat(Entity entity) {
      if(entity != null) {
         if(this.seats != null && this.seats.length > 0) {
            if(this.isMountedEntity(entity)) {
               boolean isFound = false;
               int sid = 1;
               MCH_EntitySeat[] arr$ = this.seats;
               int len$ = arr$.length;

               int i$;
               MCH_EntitySeat seat;
               for(i$ = 0; i$ < len$; ++i$) {
                  seat = arr$[i$];
                  if(seat != null) {
                     if(this.getSeatInfo(sid) instanceof MCH_SeatRackInfo) {
                        break;
                     }

                     if(W_Entity.isEqual(seat.riddenByEntity, entity)) {
                        isFound = true;
                     } else if(isFound && seat.riddenByEntity == null) {
                        entity.mountEntity(seat);
                        return;
                     }

                     ++sid;
                  }
               }

               sid = 1;
               arr$ = this.seats;
               len$ = arr$.length;

               for(i$ = 0; i$ < len$; ++i$) {
                  seat = arr$[i$];
                  if(seat != null && seat.riddenByEntity == null) {
                     if(!(this.getSeatInfo(sid) instanceof MCH_SeatRackInfo)) {
                        entity.mountEntity(seat);
                        this.onMountPlayerSeat(seat, entity);
                        return;
                     }
                     break;
                  }

                  ++sid;
               }

            }
         }
      }
   }

   public void switchPrevSeat(Entity entity) {
      if(entity != null) {
         if(this.seats != null && this.seats.length > 0) {
            if(this.isMountedEntity(entity)) {
               boolean isFound = false;

               int i;
               MCH_EntitySeat seat;
               for(i = this.seats.length - 1; i >= 0; --i) {
                  seat = this.seats[i];
                  if(seat != null) {
                     if(W_Entity.isEqual(seat.riddenByEntity, entity)) {
                        isFound = true;
                     } else if(isFound && seat.riddenByEntity == null) {
                        entity.mountEntity(seat);
                        return;
                     }
                  }
               }

               for(i = this.seats.length - 1; i >= 0; --i) {
                  seat = this.seats[i];
                  if(!(this.getSeatInfo(i + 1) instanceof MCH_SeatRackInfo) && seat != null && seat.riddenByEntity == null) {
                     entity.mountEntity(seat);
                     return;
                  }
               }

            }
         }
      }
   }

   public Entity[] getParts() {
      return this.partEntities;
   }

   public float getSoundVolume() {
      return 1.0F;
   }

   public float getSoundPitch() {
      return 1.0F;
   }

   public abstract String getDefaultSoundName();

   public String getSoundName() {
      return this.getAcInfo() == null?"":(!this.getAcInfo().soundMove.isEmpty()?this.getAcInfo().soundMove:this.getDefaultSoundName());
   }

   public boolean isSkipNormalRender() {
      return super.ridingEntity instanceof MCH_EntitySeat;
   }

   public boolean isRenderBullet(Entity entity, Entity rider) {
      return !this.isCameraView(rider) || !W_Entity.isEqual(this.getTVMissile(), entity) || !W_Entity.isEqual(this.getTVMissile().shootingEntity, rider);
   }

   public boolean isCameraView(Entity entity) {
      return this.getIsGunnerMode(entity) || this.isUAV();
   }

   public void updateCamera(double x, double y, double z) {
      if(super.worldObj.isRemote) {
         if(this.getTVMissile() != null) {
            this.camera.setPosition(this.TVmissile.posX, this.TVmissile.posY, this.TVmissile.posZ);
            this.camera.setCameraZoom(1.0F);
            this.TVmissile.isSpawnParticle = !this.isMissileCameraMode(this.TVmissile.shootingEntity);
         } else {
            this.setTVMissile((MCH_EntityTvMissile)null);
            MCH_BaseVehicleInfo.CameraPosition cpi = this.getCameraPosInfo();
            Vec3 cp = cpi != null?cpi.pos:Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
            Vec3 v = MCH_Lib.RotVec3(cp, -this.getRotYaw(), -this.getRotPitch(), -this.getRotRoll());
            this.camera.setPosition(x + v.xCoord, y + v.yCoord, z + v.zCoord);
         }

      }
   }

   public void updateCameraRotate(float yaw, float pitch) {
      this.camera.prevRotationYaw = this.camera.rotationYaw;
      this.camera.prevRotationPitch = this.camera.rotationPitch;
      this.camera.rotationYaw = yaw;
      this.camera.rotationPitch = pitch;
   }

   public void updatePartCameraRotate() {
      if(super.worldObj.isRemote) {
         Entity e = this.getEntityBySeatId(1);
         if(e == null) {
            e = this.getRiddenByEntity();
         }

         if(e != null) {
            this.camera.partRotationYaw = e.rotationYaw;
            float pitch = e.rotationPitch;
            this.camera.prevPartRotationYaw = this.camera.partRotationYaw;
            this.camera.prevPartRotationPitch = this.camera.partRotationPitch;
            this.camera.partRotationPitch = pitch;
         }
      }

   }

   public void setTVMissile(MCH_EntityTvMissile entity) {
      this.TVmissile = entity;
   }

   public MCH_EntityTvMissile getTVMissile() {
      return this.TVmissile != null && !this.TVmissile.isDead?this.TVmissile:null;
   }

   public MCH_WeaponSet[] createWeapon(int seat_num) {
      this.currentWeaponID = new int[seat_num];

      for(int weaponSetArray = 0; weaponSetArray < this.currentWeaponID.length; ++weaponSetArray) {
         this.currentWeaponID[weaponSetArray] = -1;
      }

      if(this.getAcInfo() != null && this.getAcInfo().weaponSetList.size() > 0 && seat_num > 0) {
         MCH_WeaponSet[] var7 = new MCH_WeaponSet[this.getAcInfo().weaponSetList.size()];

         for(int i = 0; i < this.getAcInfo().weaponSetList.size(); ++i) {
            MCH_BaseVehicleInfo.WeaponSet ws = (MCH_BaseVehicleInfo.WeaponSet)this.getAcInfo().weaponSetList.get(i);
            MCH_WeaponBase[] wb = new MCH_WeaponBase[ws.weapons.size()];

            for(int defYaw = 0; defYaw < ws.weapons.size(); ++defYaw) {
               wb[defYaw] = MCH_WeaponCreator.createWeapon(super.worldObj, ws.type, ((MCH_BaseVehicleInfo.Weapon)ws.weapons.get(defYaw)).pos, ((MCH_BaseVehicleInfo.Weapon)ws.weapons.get(defYaw)).yaw, ((MCH_BaseVehicleInfo.Weapon)ws.weapons.get(defYaw)).pitch, this, ((MCH_BaseVehicleInfo.Weapon)ws.weapons.get(defYaw)).turret);
               wb[defYaw].aircraft = this;
            }

            if(wb.length > 0 && wb[0] != null) {
               float var8 = ((MCH_BaseVehicleInfo.Weapon)ws.weapons.get(0)).defaultYaw;
               var7[i] = new MCH_WeaponSet(wb);
               var7[i].prevRotationYaw = var8;
               var7[i].rotationYaw = var8;
               var7[i].defaultRotationYaw = var8;
            }
         }

         return var7;
      } else {
         return new MCH_WeaponSet[]{this.dummyWeapon};
      }
   }

   public void switchWeapon(Entity entity, int id) {
      int sid = getSeatIdByEntity(entity);
      //int sid = this.getSeatIdByEntity(entity);
      if (!isValidSeatID(sid))
         return;
      int beforeWeaponID = this.currentWeaponID[sid];
      if (getWeaponNum() <= 0 || this.currentWeaponID.length <= 0)
         return;
      if (id < 0)
         this.currentWeaponID[sid] = -1;
      if (id >= getWeaponNum())
         id = getWeaponNum() - 1;
      MCH_Lib.DbgLog(this.worldObj, "switchWeapon:" + W_Entity.getEntityId(entity) + " -> " + id, new Object[0]);
      getCurrentWeapon(entity).reload();
      this.currentWeaponID[sid] = id;
      MCH_WeaponSet ws = getCurrentWeapon(entity);
      ws.onSwitchWeapon(this.worldObj.isRemote, isInfinityAmmo(entity));
      //if(ws.getCurrentWeapon().worldObj.isRemote) {
      //   W_McClient.MOD_playSoundFX(ws.getInfo().weaponSwitchSound, 3F, 1.0F);
      //}
      //we dont do that here
      if (!this.worldObj.isRemote)
         MCH_PacketNotifyWeaponID.send((Entity)this, sid, id, ws.getAmmoNum(), ws.getRestAllAmmoNum());
   }

   public void updateWeaponID(int sid, int id) {
      if(sid >= 0 && sid < this.currentWeaponID.length) {
         if(this.getWeaponNum() > 0 && this.currentWeaponID.length > 0) {
            if(id < 0) {
               this.currentWeaponID[sid] = -1;
            }

            if(id >= this.getWeaponNum()) {
               id = this.getWeaponNum() - 1;
            }

            MCH_Lib.DbgLog(super.worldObj, "switchWeapon:seatID=" + sid + ", WeaponID=" + id, new Object[0]);
            this.currentWeaponID[sid] = id;
         }
      }
   }

   public void updateWeaponRestAmmo(int id, int num) {
      if(id < this.getWeaponNum()) {
         this.getWeapon(id).setRestAllAmmoNum(num);
      }

   }

   public MCH_WeaponSet getWeaponByName(String name) {
      MCH_WeaponSet[] arr$ = this.weapons;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_WeaponSet ws = arr$[i$];
         if(ws.isEqual(name)) {
            return ws;
         }
      }

      return null;
   }

   public int getWeaponIdByName(String name) {
      int id = 0;
      MCH_WeaponSet[] arr$ = this.weapons;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_WeaponSet ws = arr$[i$];
         if(ws.isEqual(name)) {
            return id;
         }

         ++id;
      }

      return -1;
   }

   public void reloadAllWeapon() {
      if(this.aps != null) this.aps.refillAmmo();
      for(int i = 0; i < this.getWeaponNum(); ++i) {
         this.getWeapon(i).reloadMag();
      }

   }

   public MCH_WeaponSet getFirstSeatWeapon() {
      return this.currentWeaponID != null && this.currentWeaponID.length > 0 && this.currentWeaponID[0] >= 0?this.getWeapon(this.currentWeaponID[0]):this.getWeapon(0);
   }

   public void initCurrentWeapon(Entity entity) {
      int sid = this.getSeatIdByEntity(entity);
      MCH_Lib.DbgLog(super.worldObj, "initCurrentWeapon:" + W_Entity.getEntityId(entity) + ":%d", new Object[]{Integer.valueOf(sid)});
      if(sid >= 0 && sid < this.currentWeaponID.length) {
         this.currentWeaponID[sid] = -1;
         if(entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
            this.currentWeaponID[sid] = this.getNextWeaponID(entity, 1);
            this.switchWeapon(entity, this.getCurrentWeaponID(entity));
            if(super.worldObj.isRemote) {
               MCH_PacketIndNotifyAmmoNum.send(this, -1);
            }
         }

      }
   }

   public void initPilotWeapon() {
      this.currentWeaponID[0] = -1;
   }

   public MCH_WeaponSet getCurrentWeapon(Entity entity) {
      return getWeapon(getCurrentWeaponID(entity));
   }

   protected MCH_WeaponSet getWeapon(int id) {
      if (id < 0 || this.weapons.length <= 0 || id >= this.weapons.length)
         return this.dummyWeapon;
      return this.weapons[id];
   }

   public int getWeaponIDBySeatID(int sid) {
      //todo: check this as well
      if (sid < 0 || sid >= this.currentWeaponID.length)
         return -1;
      return this.currentWeaponID[sid];
   }

   public double getLandInDistance(Entity user) {
      int currentTick = this.getCountOnUpdate();

      if (this.lastCalcLandInDistanceCount != currentTick && currentTick % 5 == 0) {
         this.lastCalcLandInDistanceCount = currentTick;

         MCH_WeaponParam prm = new MCH_WeaponParam();
         prm.setPosition(super.posX, super.posY, super.posZ);
         prm.entity = this;
         prm.user = user;
         prm.isInfinity = this.isInfinityAmmo(user);

         if (user != null) {
            MCH_WeaponSet currentWs = this.getCurrentWeapon(user);
            if (currentWs != null) {
               int sid = this.getSeatIdByEntity(user);
               MCH_BaseVehicleInfo.WeaponSet weaponSet = this.getAcInfo().getWeaponSetById(sid);

               // Check if weaponSet and weapons list exist and contain at least one Weapon
               if (weaponSet != null && weaponSet.weapons != null && !weaponSet.weapons.isEmpty()) {
                  Object w0 = weaponSet.weapons.get(0);
                  if (w0 instanceof MCH_BaseVehicleInfo.Weapon) {
                     prm.isTurret = ((MCH_BaseVehicleInfo.Weapon) w0).turret;
                  } else {
                     // fallback or log: first element not a Weapon instance
                     prm.isTurret = false; // or your default
                  }
               }

               this.lastLandInDistance = currentWs.getLandInDistance(prm);
            }
         }
      }

      return this.lastLandInDistance;
   }

   public boolean useCurrentWeapon(Entity user) {
      MCH_WeaponParam prm = new MCH_WeaponParam();
      prm.setPosition(super.posX, super.posY, super.posZ);
      prm.entity = this;
      prm.user = user;
      return this.useCurrentWeapon(prm);
   }

   public void currentWeaponLock(Entity user) {
      if(user == null ) { //|| this.aircraft.isFreeLookMode()
         return;
      }
      MCH_WeaponSet currentWs = this.getCurrentWeapon(user);
      if(currentWs != null || !this.aircraft.isFreeLookMode()) {
         MCH_WeaponParam prm = new MCH_WeaponParam();
         prm.setPosition(super.posX, super.posY, super.posZ);
         prm.entity = this;
         prm.user = user;
         currentWs.lock(prm);
      }
   }

   public void currentWeaponUnlock(Entity user) {
      if(user == null ) { //|| this.aircraft.isFreeLookMode()
         return;
      }
      MCH_WeaponSet currentWs = this.getCurrentWeapon(user);
      if(currentWs != null || !this.aircraft.isFreeLookMode()) {
         MCH_WeaponParam prm = new MCH_WeaponParam();
         prm.setPosition(super.posX, super.posY, super.posZ);
         prm.entity = this;
         prm.user = user;
         currentWs.onUnlock(prm);
      }
   }

   public boolean useCurrentWeapon(MCH_WeaponParam prm) {
      prm.isInfinity = this.isInfinityAmmo(prm.user);
      if(prm.user != null) {
         MCH_WeaponSet currentWs = this.getCurrentWeapon(prm.user);
         if(currentWs != null && currentWs.canUse()) {
            int sid = this.getSeatIdByEntity(prm.user);
            if(this.getAcInfo().getWeaponSetById(sid) != null) {
               prm.isTurret = ((MCH_BaseVehicleInfo.Weapon)this.getAcInfo().getWeaponSetById(sid).weapons.get(0)).turret;
            }

            int lastUsedIndex = currentWs.getCurrentWeaponIndex();
            if(currentWs.use(prm)) {
               MCH_WeaponSet[] shift = this.weapons;
               int arr$ = shift.length;

               int len$;
               for(len$ = 0; len$ < arr$; ++len$) {
                  MCH_WeaponSet i$ = shift[len$];
                  if(i$ != currentWs && !i$.getInfo().group.isEmpty() && i$.getInfo().group.equals(currentWs.getInfo().group)) {
                     i$.waitAndReloadByOther(prm.reload);
                  }
               }

               if(!super.worldObj.isRemote) {
                  int var10 = 0;
                  MCH_WeaponSet[] var11 = this.weapons;
                  len$ = var11.length;

                  for(int var12 = 0; var12 < len$; ++var12) {
                     MCH_WeaponSet ws = var11[var12];
                     if(ws == currentWs) {
                        break;
                     }

                     var10 += ws.getWeaponNum();
                  }

                  var10 += lastUsedIndex;
                  this.useWeaponStat |= var10 < 32?1 << var10:0;
               }

               return true;
            }
         }
      }

      return false;
   }

   public void switchCurrentWeaponMode(Entity entity) {
      this.getCurrentWeapon(entity).switchMode();
   }

   public int getWeaponNum() {
      return this.weapons.length;
   }

   public int getCurrentWeaponID(Entity entity) {
           if (!(entity instanceof EntityPlayer ) && !(entity instanceof MCH_EntityGunner) ) {
                return -1;
              }
          int id = getSeatIdByEntity(entity);
          return (id >= 0 && id < this.currentWeaponID.length) ? this.currentWeaponID[id] : -1;
        }

   public int getNextWeaponID(Entity entity, int step) {
           if (getAcInfo() == null) {
                return -1;
              }
           int sid = getSeatIdByEntity(entity);
           if (sid < 0) {
                return -1;
              }
           int id = getCurrentWeaponID(entity);

           int i;
           for (i = 0; i < getWeaponNum(); i++) {
                if (step >= 0) {
                     id = (id + 1) % getWeaponNum();
                   } else {
                     id = (id > 0) ? (id - 1) : (getWeaponNum() - 1);
                   }

                MCH_BaseVehicleInfo.Weapon w = getAcInfo().getWeaponById(id);
                if (w != null) {
                     MCH_WeaponInfo wi = getWeaponInfoById(id);
                     int wpsid = getWeaponSeatID(wi, w);
                     if (wpsid < getSeatNum() + 1 + 1 && (wpsid == sid || (sid == 0 && w.canUsePilot && !(getEntityBySeatId(wpsid) instanceof EntityPlayer) && !(getEntityBySeatId(wpsid) instanceof MCH_EntityGunner)))) { //
                          break;
                        }
                   }
              }

           if (i >= getWeaponNum()) {
                return -1;
              }
           MCH_Lib.DbgLog(this.worldObj, "getNextWeaponID:%d:->%d", new Object[] { Integer.valueOf(W_Entity.getEntityId(entity)), Integer.valueOf(id) });
           return id;
         }

   public int getWeaponSeatID(MCH_WeaponInfo wi, MCH_BaseVehicleInfo.Weapon w) {
      return wi != null && (wi.target & 195) == 0 && wi.type.isEmpty() && (MCH_MOD.proxy.isSinglePlayer() || MCH_Config.TestMode.prmBool)?1000:w.seatID;
   }

   public boolean isMissileCameraMode(Entity entity) {
      return this.getTVMissile() != null && this.isCameraView(entity);
   }

   public boolean isPilotReloading() {
      return this.getCommonStatus(2) || this.supplyAmmoWait > 0;
   }

   public int getUsedWeaponStat() {
      if(this.getAcInfo() == null) {
         return 0;
      } else if(this.getAcInfo().getWeaponNum() <= 0) {
         return 0;
      } else {
         int stat = 0;
         int i = 0;
         MCH_WeaponSet[] arr$ = this.weapons;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_WeaponSet w = arr$[i$];
            if(i >= 32) {
               break;
            }

            for(int wi = 0; wi < w.getWeaponNum() && i < 32; ++wi) {
               stat |= w.isUsed(wi)?1 << i:0;
               ++i;
            }
         }

         return stat;
      }
   }

   public boolean isWeaponNotCooldown(MCH_WeaponSet checkWs, int index) {
      if(this.getAcInfo() == null) {
         return false;
      } else if(this.getAcInfo().getWeaponNum() <= 0) {
         return false;
      } else {
         int shift = 0;
         MCH_WeaponSet[] arr$ = this.weapons;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_WeaponSet ws = arr$[i$];
            if(ws == checkWs) {
               break;
            }

            shift += ws.getWeaponNum();
         }

         shift += index;
         return shift < 32?(this.useWeaponStat & 1 << shift) != 0:false;
      }
   }

   public void updateWeapons() {
      if(this.getAcInfo() != null) {
         if(this.getAcInfo().getWeaponNum() > 0) {
            int prevUseWeaponStat = this.useWeaponStat;
            if(!super.worldObj.isRemote) {
               this.useWeaponStat |= this.getUsedWeaponStat();
               this.getDataWatcher().updateObject(24, new Integer(this.useWeaponStat));
               this.useWeaponStat = 0;
            } else {
               this.useWeaponStat = this.getDataWatcher().getWatchableObjectInt(24);
            }

            float yaw = MathHelper.wrapAngleTo180_float(this.getRotYaw());
            float pitch = MathHelper.wrapAngleTo180_float(this.getRotPitch());
            int id = 0;
            int wid = 0;

            while(wid < this.weapons.length) {
               MCH_WeaponSet w = this.weapons[wid];
               boolean isLongDelay = false;
               if(w.getFirstWeapon() != null) {
                  isLongDelay = w.isLongDelayWeapon();
               }

               boolean isSelected = false;
               int[] isWpnUsed = this.currentWeaponID;
               int wi = isWpnUsed.length;
               int entity = 0;

               while(true) {
                  if(entity < wi) {
                     int ep = isWpnUsed[entity];
                     if(ep != wid) {
                        ++entity;
                        continue;
                     }

                     isSelected = true;
                  }

                  boolean var16 = false;

                  float ey;
                  for(wi = 0; wi < w.getWeaponNum(); ++wi) {
                     boolean var18 = id < 32 && (prevUseWeaponStat & 1 << id) != 0;
                     boolean var20 = id < 32 && (this.useWeaponStat & 1 << id) != 0;
                     if(isLongDelay && var18 && var20) {
                        var20 = false;
                     }

                     var16 |= var20;
                     if(!var18 && var20) {
                        ey = w.getInfo().recoil;
                        if(ey > 0.0F) {
                           this.recoilCount = 30;
                           this.recoilValue = ey;
                           this.recoilYaw = w.rotationYaw;
                        }
                     }

                     if(super.worldObj.isRemote && var20) {
                        Vec3 var21 = MCH_Lib.RotVec3(0.0D, 0.0D, -1.0D, -w.rotationYaw - yaw, -w.rotationPitch);
                        Vec3 targetYaw = w.getCurrentWeapon().getShotPos(this);
                        this.spawnParticleMuzzleFlash(super.worldObj, w.getInfo(), super.posX + targetYaw.xCoord, super.posY + targetYaw.yCoord, super.posZ + targetYaw.zCoord, var21);
                     }

                     w.updateWeapon(this, var20, wi);
                     ++id;
                  }

                  w.update(this, isSelected, var16);
                  MCH_BaseVehicleInfo.Weapon var17 = this.getAcInfo().getWeaponById(wid);
                  if(var17 != null && !this.isDestroyed()) {
                     Entity var19 = this.getEntityBySeatId(this.getWeaponSeatID(this.getWeaponInfoById(wid), var17));
                     if(var17.canUsePilot && !(var19 instanceof EntityPlayer) && !(var19 instanceof MCH_EntityGunner) ) { //
                        var19 = this.getEntityBySeatId(0);
                     }




                     if(var19 instanceof EntityPlayer || var19 instanceof MCH_EntityGunner) {
                        float var22;
                        if((int)var17.minYaw != 0 || (int)var17.maxYaw != 0) {
                           var22 = var17.turret?MathHelper.wrapAngleTo180_float(this.getLastRiderYaw()) - yaw:0.0F;
                           ey = MathHelper.wrapAngleTo180_float(var19.rotationYaw - yaw - var17.defaultYaw - var22);
                           if(Math.abs((int)var17.minYaw) < 360 && Math.abs((int)var17.maxYaw) < 360) {
                              float var23 = MCH_Lib.RNG(ey, var17.minYaw, var17.maxYaw);
                              float wy = w.rotationYaw - var17.defaultYaw - var22;
                              if(var23 < wy) {
                                 if(wy - var23 > 15.0F) {
                                    wy -= 15.0F;
                                 } else {
                                    wy = var23;
                                 }
                              } else if(var23 > wy) {
                                 if(var23 - wy > 15.0F) {
                                    wy += 15.0F;
                                 } else {
                                    wy = var23;
                                 }
                              }

                              w.rotationYaw = wy + var17.defaultYaw + var22;
                           } else {
                              w.rotationYaw = ey + var22;
                           }
                        }

                        var22 = MathHelper.wrapAngleTo180_float(var19.rotationPitch - pitch);
                        w.rotationPitch = MCH_Lib.RNG(var22, var17.minPitch, var17.maxPitch);
                        w.rotationTurretYaw = 0.0F;
                     } else {
                        w.rotationTurretYaw = this.getLastRiderYaw() - this.getRotYaw();
                        if(this.ridingEntity != null) {
                           w.rotationYaw = 0.0F;
                        }
                     }
                  }

                  //if (!(entity instanceof EntityPlayer) && !(entity instanceof mcheli.mob.MCH_EntityGunner)) {
                  ///* 5064 */             w.rotationTurretYaw = getLastRiderYaw() - getRotYaw();
                  ///* 5065 */             if (this.ridingEntity != null) {
                  ///* 5066 */               w.rotationYaw = 0.0F;
                  ///*      */             }
                  ///*      */           } else {
                  //this is the worst fucking code i have ever dealt with

                  ++wid;
                  break;
               }
            }

            this.updateWeaponBay();
            if(this.hitStatus > 0) {
               --this.hitStatus;
            }

         }
      }
   }

   public void updateWeaponsRotation() {
      if(this.getAcInfo() != null) {
         if(this.getAcInfo().getWeaponNum() > 0) {
            if(!this.isDestroyed()) {
               float yaw = MathHelper.wrapAngleTo180_float(this.getRotYaw());
               float pitch = MathHelper.wrapAngleTo180_float(this.getRotPitch());

               for(int wid = 0; wid < this.weapons.length; ++wid) {
                  MCH_WeaponSet w = this.weapons[wid];
                  MCH_BaseVehicleInfo.Weapon wi = this.getAcInfo().getWeaponById(wid);
                  if(wi != null) {
                     Entity entity = this.getEntityBySeatId(this.getWeaponSeatID(this.getWeaponInfoById(wid), wi));
                     if (wi.canUsePilot && !(entity instanceof EntityPlayer) && !(entity instanceof mcheli.mob.MCH_EntityGunner)) { //
                                     entity = getEntityBySeatId(0);
                                   }

                     if(entity instanceof EntityPlayer || entity instanceof MCH_EntityGunner) {
                        float ep;
                        if((int)wi.minYaw != 0 || (int)wi.maxYaw != 0) {
                           ep = wi.turret?MathHelper.wrapAngleTo180_float(this.getLastRiderYaw()) - yaw:0.0F;
                           float ey = MathHelper.wrapAngleTo180_float(entity.rotationYaw - yaw - wi.defaultYaw - ep);
                           if(Math.abs((int)wi.minYaw) < 360 && Math.abs((int)wi.maxYaw) < 360) {
                              float targetYaw = MCH_Lib.RNG(ey, wi.minYaw, wi.maxYaw);
                              float wy = w.rotationYaw - wi.defaultYaw - ep;
                              if(targetYaw < wy) {
                                 if(wy - targetYaw > 15.0F) {
                                    wy -= 15.0F;
                                 } else {
                                    wy = targetYaw;
                                 }
                              } else if(targetYaw > wy) {
                                 if(targetYaw - wy > 15.0F) {
                                    wy += 15.0F;
                                 } else {
                                    wy = targetYaw;
                                 }
                              }

                              w.rotationYaw = wy + wi.defaultYaw + ep;
                           } else {
                              w.rotationYaw = ey + ep;
                           }
                        }

                        ep = MathHelper.wrapAngleTo180_float(entity.rotationPitch - pitch);
                        w.rotationPitch = MCH_Lib.RNG(ep, wi.minPitch, wi.maxPitch);
                        w.rotationTurretYaw = 0.0F;
                     } else {
                        w.rotationTurretYaw = this.getLastRiderYaw() - this.getRotYaw();
                     }

                     //if (!(entity instanceof EntityPlayer) && !(entity instanceof mcheli.mob.MCH_EntityGunner)) {
                     ///* 5129 */             w.rotationTurretYaw = getLastRiderYaw() - getRotYaw();
                  }

                  w.prevRotationYaw = w.rotationYaw;
               }

            }
         }
      }
   }

   private void spawnParticleMuzzleFlash(World w, MCH_WeaponInfo wi, double px, double py, double pz, Vec3 wrv) {
      Iterator i$;
      MCH_WeaponInfo.MuzzleFlash mf;
      if(wi.listMuzzleFlashSmoke != null) {
         i$ = wi.listMuzzleFlashSmoke.iterator();

         while(i$.hasNext()) {
            mf = (MCH_WeaponInfo.MuzzleFlash)i$.next();
            double color = px + -wrv.xCoord * (double)mf.dist;
            double y = py + -wrv.yCoord * (double)mf.dist;
            double z = pz + -wrv.zCoord * (double)mf.dist;
            MCH_ParticleParam p = new MCH_ParticleParam(w, "smoke", px, py, pz);
            p.size = mf.size;

            for(int i = 0; i < mf.num; ++i) {
               p.a = mf.a * 0.9F + w.rand.nextFloat() * 0.1F;
               float color1 = w.rand.nextFloat() * 0.1F;
               p.r = color1 + mf.r * 0.9F;
               p.g = color1 + mf.g * 0.9F;
               p.b = color1 + mf.b * 0.9F;
               p.age = (int)((double)mf.age + 0.1D * (double)mf.age * (double)w.rand.nextFloat());
               p.posX = color + (w.rand.nextDouble() - 0.5D) * (double)mf.range;
               p.posY = y + (w.rand.nextDouble() - 0.5D) * (double)mf.range;
               p.posZ = z + (w.rand.nextDouble() - 0.5D) * (double)mf.range;
               p.motionX = w.rand.nextDouble() * (p.posX < color?-0.2D:0.2D);
               p.motionY = w.rand.nextDouble() * (p.posY < y?-0.03D:0.03D);
               p.motionZ = w.rand.nextDouble() * (p.posZ < z?-0.2D:0.2D);
               MCH_ParticlesUtil.spawnParticle(p);
            }
         }
      }

      if(wi.listMuzzleFlash != null) {
         i$ = wi.listMuzzleFlash.iterator();

         while(i$.hasNext()) {
            mf = (MCH_WeaponInfo.MuzzleFlash)i$.next();
            float var21 = super.rand.nextFloat() * 0.1F + 0.9F;
            MCH_ParticlesUtil.spawnParticleExplode(super.worldObj, px + -wrv.xCoord * (double)mf.dist, py + -wrv.yCoord * (double)mf.dist, pz + -wrv.zCoord * (double)mf.dist, mf.size, var21 * mf.r, var21 * mf.g, var21 * mf.b, mf.a, mf.age + w.rand.nextInt(3));
         }
      }

   }

   private void updateWeaponBay() {
      for(int i = 0; i < this.weaponBays.length; ++i) {
         MCH_EntityBaseVehicle.WeaponBay wb = this.weaponBays[i];
         MCH_BaseVehicleInfo.WeaponBay info = (MCH_BaseVehicleInfo.WeaponBay)this.getAcInfo().partWeaponBay.get(i);
         boolean isSelected = false;
         Integer[] arr$ = info.weaponIds;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            int wid = arr$[i$].intValue();

            for(int sid = 0; sid < this.currentWeaponID.length; ++sid) {
               if(wid == this.currentWeaponID[sid] && this.getEntityBySeatId(sid) != null) {
                  isSelected = true;
               }
            }
         }

         wb.prevRot = wb.rot;
         if(isSelected) {
            if(wb.rot < 90.0F) {
               wb.rot += 3.0F;
            }

            if(wb.rot >= 90.0F) {
               wb.rot = 90.0F;
            }
         } else {
            if(wb.rot > 0.0F) {
               wb.rot -= 3.0F;
            }

            if(wb.rot <= 0.0F) {
               wb.rot = 0.0F;
            }
         }
      }

   }

   public int getHitStatus() {
      return this.hitStatus;
   }

   public int getMaxHitStatus() {
      return 15;
   }

   public void hitBullet() {
      this.hitStatus = this.getMaxHitStatus();
   }

   public void initRotationYaw(float yaw) {
      // Set the yaw for the current object and ensure consistency
      super.rotationYaw = yaw;
      super.prevRotationYaw = yaw;
      this.lastRiderYaw = yaw;
      this.lastSearchLightYaw = yaw;

      // Log the initial yaw value for debugging
      //System.out.println("Initializing rotation yaw to: " + yaw);

      // Ensure the weapons array is not null before processing
      if (this.weapons != null) {
         // Use an enhanced for-loop for better readability
         for (MCH_WeaponSet weapon : this.weapons) {
            // Ensure the weapon is not null before accessing its properties
            if (weapon != null) {
               weapon.rotationYaw = weapon.defaultRotationYaw;
               weapon.rotationPitch = 0.0F;
            } else {
               // Log a warning if a weapon in the array is null
               System.out.println("Warning: Null weapon encountered in weapons array.");
            }
         }
      } else {
         // Log a warning if the weapons array is null
         System.out.println("Warning: Weapons array is null.");
      }
   }

   public MCH_BaseVehicleInfo getAcInfo() {
      return this.acInfo;
   }

   public abstract Item getItem();

   /** UAVs retain their legacy cross-chunk control policy; normal vehicles do not. */
   public void updateForceSpawnPolicy() {
      super.forceSpawn = this.acInfo != null && (this.isUAV() || this.isNewUAV());
   }

   public void setAcInfo(MCH_BaseVehicleInfo info) {
      MCH_BaseVehicleInfo previousInfo = this.acInfo;
      boolean hadRadar = previousInfo != null && previousInfo.hasRadar();
      this.acInfo = info;
      if(previousInfo == null && info != null) {
         boolean active = this.pendingRadarActive != null ? this.pendingRadarActive.booleanValue() : info.hasRadar();
         this.pendingRadarActive = null;
         this.setRadarActive(active);
      } else if(previousInfo != null && (info == null || !info.hasRadar())) {
         boolean wasActive = this.getCommonStatus(CMN_ID_ACTIVE_RADAR);
         this.setRadarActive(false);
         if(!wasActive && (info == null || hadRadar)) {
            this.initRadar();
         }
      } else if(previousInfo != null && !hadRadar && info.hasRadar()) {
         this.setRadarActive(true);
      }
      this.updateForceSpawnPolicy();
      if(info != null) {
         this.partHatch = this.createHatch();
         this.partCanopy = this.createCanopy();
         this.partLandingGear = this.createLandingGear();
         this.weaponBays = this.createWeaponBays();
         this.rotPartRotation = new float[info.partRotPart.size()];
         this.prevRotPartRotation = new float[info.partRotPart.size()];
         this.extraBoundingBox = this.createExtraBoundingBox();
         this.markVehicleBoxCacheDirty("vehicle config changed");
         //this.extrawheelboundingbox = this.createannoyingboundingbox();
         this.partEntities = this.createParts();
         super.stepHeight = info.stepHeight;
      }
   }

   /** Applies a targeted definition snapshot without recreating seats, weapons, or moving-part state. */
   public boolean applyTargetedInfo(MCH_BaseVehicleInfo info) {
      if(info == null || this.acInfo == null) return false;
      if(info.getNumSeatAndRack() != this.acInfo.getNumSeatAndRack()) return false;
      boolean hadRadar = this.hasRadar();
      boolean wasRadarActive = this.getCommonStatus(CMN_ID_ACTIVE_RADAR);
      this.acInfo = info;
      if(!info.hasRadar()) {
         this.setRadarActive(false);
         if(hadRadar && !wasRadarActive) this.initRadar();
      }
      else if(!hadRadar) this.setRadarActive(true);
      this.updateForceSpawnPolicy();
      this.cameraId = Math.max(0, Math.min(this.cameraId, Math.max(0, info.cameraPosition.size() - 1)));
      this.extraBoundingBox = this.createExtraBoundingBox();
      this.markVehicleBoxCacheDirty("targeted vehicle config reload");
      super.stepHeight = info.stepHeight;
      this.setSize(info.bodyWidth, info.bodyHeight);
      this.aps.configure(info.apsUseTime, info.apsWaitTime, info.apsRange, info.apsAmmo);
      return true;
   }

   public MCH_BoundingBox[] createExtraBoundingBox() {
      // Get the list of extra bounding boxes
      MCH_BaseVehicleInfo acInfo = this.getAcInfo();
      if (acInfo == null || acInfo.extraBoundingBox == null) {
         return new MCH_BoundingBox[0];
      }

      List<MCH_BoundingBox> boundingBoxes = acInfo.extraBoundingBox;

      // Initialize the array with the size of the list
      MCH_BoundingBox[] ar = new MCH_BoundingBox[boundingBoxes.size()];

      // Iterate over the list and copy each bounding box to the array
      int i = 0;
      for (MCH_BoundingBox bb : boundingBoxes) {
         ar[i++] = bb.copy();
      }

      return ar;
   }


   public wheelBoundingBox[] createannoyingboundingbox() {
      // Get the list of extra bounding boxes
      MCH_BaseVehicleInfo acInfo = this.getAcInfo();
      if (acInfo == null || acInfo.wheelboundingbox == null) {
         return new wheelBoundingBox[0];
      }

      List<wheelBoundingBox> stupidboundingBoxes = acInfo.wheelboundingbox;

      // Initialize the array with the size of the list
      wheelBoundingBox[] ar2 = new wheelBoundingBox[stupidboundingBoxes.size()];

      // Iterate over the list and copy each bounding box to the array
      int i = 0;
      for (wheelBoundingBox bb2 : stupidboundingBoxes) {
         ar2[i++] = bb2.copy2();
      }

      return ar2;
   }

   public Entity[] createParts() {
      Entity[] list = new Entity[]{this.partEntities[0]};
      return list;
   }

   private void forceChunkLoading() {
      if (!super.worldObj.isRemote) {
         // Request a chunk loading ticket
         this.chunkTicket = ForgeChunkManager.requestTicket(MCH_MOD.instance, super.worldObj, ForgeChunkManager.Type.NORMAL);
         if (this.chunkTicket != null) {
            // Mark the chunk for loading
            int chunkX = MathHelper.floor_double(super.posX) >> 4;
            int chunkZ = MathHelper.floor_double(super.posZ) >> 4;
            ForgeChunkManager.forceChunk(this.chunkTicket, new ChunkCoordIntPair(chunkX, chunkZ));
         }
      }
   }


 //  private void fakeplayermaker() {

  // }

   public void updateUAV() {
      if(this.isUAV() || this.isNewUAV()) {
         if(super.worldObj.isRemote) {
            int udx = this.getDataWatcher().getWatchableObjectInt(22);
            if(udx > 0) {
               if(this.uavStation == null) {
                  //this is the bugged state when we first place newUAV, probably want to fetch xyz of station here maybe?
                  Entity uavEntity = super.worldObj.getEntityByID(udx);
                  if(uavEntity instanceof MCH_EntityUavStation) {
                     this.uavStation = (MCH_EntityUavStation)uavEntity;
                     this.uavStation.setControlAircract(this);
                  }
               }
            } else if(this.uavStation != null) {
               //this.uavStation.setControlAircract((MCH_EntityBaseVehicle)null);
               //this.uavStation = null;
               //System.out.println("null");
            }


            //System.out.println("everything is WORKING");
         } else if(this.uavStation != null) {
            updateNewUavStationChunkLoading();
            double udx1 = super.posX - this.uavStation.posX;
            double udz = super.posZ - this.uavStation.posZ;

            //haha gotcha
            //TODO: better uav handling
            if(udx1 * udx1 + udz * udz > 15625000.0D) {
               //System.out.println("test 4");
               this.uavStation.setControlAircract((MCH_EntityBaseVehicle)null);
               this.setUavStation((MCH_EntityUavStation)null);
               //System.out.println("null 2");

               //this will stop this shit from working

               //this.attackEntityFrom(DamageSource.outOfWorld, this.getMaxHP() + 10);
               //TODOne: teleport player as invulnerable entity
               //EntityPlayerMP


            }
            //this.forceChunkLoading();
            //System.out.println("everything is working, now chunk loading");
         } else {
            releaseNewUavStationChunk("station-cleared");
         }
         //System.out.println("everything is working 2");

         if(this.uavStation != null && this.uavStation.isDead) {
            //System.out.println("setting to null, uav station is dead");
            this.uavStation = null;
         }

      } else {
         releaseNewUavStationChunk("not-uav");
      }
      //System.out.println("working 3");
   }

   private void updateNewUavStationChunkLoading() {
      if(super.worldObj.isRemote || !this.isNewUAV() || this.uavStation == null || this.uavStation.isDead
            || this.getRiddenByEntity() == null) {
         releaseNewUavStationChunk("inactive-new-uav");
         return;
      }

      ChunkCoordIntPair stationChunk = new ChunkCoordIntPair(
            MathHelper.floor_double(this.uavStation.posX) >> 4,
            MathHelper.floor_double(this.uavStation.posZ) >> 4);

      if(this.newUavStationChunkTicket != null && stationChunk.equals(this.newUavForcedStationChunk)) {
         return;
      }

      releaseNewUavStationChunk("station-chunk-changed");
      this.newUavStationChunkTicket = ForgeChunkManager.requestTicket(MCH_MOD.instance, super.worldObj,
            ForgeChunkManager.Type.NORMAL);
      if(this.newUavStationChunkTicket == null) {
         MCH_Lib.Log((Entity)this, "Unable to request New UAV station chunk ticket", new Object[0]);
         return;
      }

      this.newUavForcedStationChunk = stationChunk;
      ForgeChunkManager.forceChunk(this.newUavStationChunkTicket, this.newUavForcedStationChunk);
      super.worldObj.getChunkFromChunkCoords(stationChunk.chunkXPos, stationChunk.chunkZPos);
   }

   private void releaseNewUavStationChunk(String reason) {
      if(this.newUavStationChunkTicket == null) {
         this.newUavForcedStationChunk = null;
         return;
      }
      if(this.newUavForcedStationChunk != null) {
         ForgeChunkManager.unforceChunk(this.newUavStationChunkTicket, this.newUavForcedStationChunk);
      }
      ForgeChunkManager.releaseTicket(this.newUavStationChunkTicket);
      this.newUavStationChunkTicket = null;
      this.newUavForcedStationChunk = null;
      MCH_Lib.Log((Entity)this, "Released New UAV station chunk ticket: %s", new Object[] { reason });
   }

   public void switchGunnerMode(boolean mode) {
      boolean debug_bk_mode = this.isGunnerMode;
      double debugThrottleBefore = this.getCurrentThrottle();
      float debugPitchBefore = this.getRotPitch();
      float debugYawBefore = this.getRotYaw();
      float debugRollBefore = this.getRotRoll();
      Entity pilot = this.getEntityBySeatId(0);
      if(!mode || this.canSwitchGunnerMode()) {
         if(this.isGunnerMode && !mode) {
            this.isGunnerMode = false;
            this.camera.setCameraZoom(1.0F);
            this.getCurrentWeapon(pilot).onSwitchWeapon(super.worldObj.isRemote, this.isInfinityAmmo(pilot));
         } else if(!this.isGunnerMode && mode) {
            this.isGunnerMode = true;
            this.camera.setCameraZoom(1.0F);
            this.getCurrentWeapon(pilot).onSwitchWeapon(super.worldObj.isRemote, this.isInfinityAmmo(pilot));
         }
      }

      MCH_Lib.DbgLog(super.worldObj, "switchGunnerMode %s->%s throttle %.4f->%.4f rot %.2f/%.2f/%.2f->%.2f/%.2f/%.2f", new Object[]{debug_bk_mode?"ON":"OFF", this.isGunnerMode?"ON":"OFF", Double.valueOf(debugThrottleBefore), Double.valueOf(this.getCurrentThrottle()), Float.valueOf(debugPitchBefore), Float.valueOf(debugYawBefore), Float.valueOf(debugRollBefore), Float.valueOf(this.getRotPitch()), Float.valueOf(this.getRotYaw()), Float.valueOf(this.getRotRoll())});
   }

   public boolean canSwitchGunnerMode() {
      return this.getAcInfo() != null && this.getAcInfo().isEnableGunnerMode?(!this.isCanopyClose()?false:(!this.getAcInfo().isEnableConcurrentGunnerMode && this.getEntityBySeatId(1) instanceof EntityPlayer?false:!this.isHoveringMode())):false;
   }

   public boolean canSwitchGunnerModeOtherSeat(EntityPlayer player) {
      int sid = this.getSeatIdByEntity(player);
      if(sid > 0) {
         MCH_SeatInfo info = this.getSeatInfo(sid);
         if(info != null) {
            return info.gunner && info.switchgunner;
         }
      }

      return false;
   }

   public void switchGunnerModeOtherSeat(EntityPlayer player) {
      this.isGunnerModeOtherSeat = !this.isGunnerModeOtherSeat;
   }

   public boolean isHoveringMode() {
      return this.isHoveringMode;
   }

   public void switchHoveringMode(boolean mode) {
      boolean debug_bk_mode = this.isHoveringMode;
      double debugThrottleBefore = this.getCurrentThrottle();
      float debugPitchBefore = this.getRotPitch();
      float debugYawBefore = this.getRotYaw();
      float debugRollBefore = this.getRotRoll();
      this.stopRepelling();
      if(this.canSwitchHoveringMode() && this.isHoveringMode() != mode) {
         this.isHoveringMode = mode;
      }

      MCH_Lib.DbgLog(super.worldObj, "switchHoveringMode %s->%s throttle %.4f->%.4f rot %.2f/%.2f/%.2f->%.2f/%.2f/%.2f", new Object[]{debug_bk_mode?"ON":"OFF", this.isHoveringMode?"ON":"OFF", Double.valueOf(debugThrottleBefore), Double.valueOf(this.getCurrentThrottle()), Float.valueOf(debugPitchBefore), Float.valueOf(debugYawBefore), Float.valueOf(debugRollBefore), Float.valueOf(this.getRotPitch()), Float.valueOf(this.getRotYaw()), Float.valueOf(this.getRotRoll())});

   }

   public boolean canSwitchHoveringMode() {
      return this.getAcInfo() == null?false:!this.isGunnerMode;
   }

   public boolean isHovering() {
      return this.isGunnerMode || this.isHoveringMode();
   }

   public boolean getIsGunnerMode(Entity entity) {
      if(this.getAcInfo() == null) {
         return false;
      } else {
         int id = this.getSeatIdByEntity(entity);
         if(id < 0) {
            return false;
         } else if(id == 0 && this.getAcInfo().isEnableGunnerMode) {
            return this.isGunnerMode;
         } else {
            MCH_SeatInfo[] st = this.getSeatsInfo();
            return id < st.length && st[id].gunner?(super.worldObj.isRemote && st[id].switchgunner?this.isGunnerModeOtherSeat:true):false;
         }
      }
   }

   public boolean isPilot(Entity player) {
      return W_Entity.isEqual(this.getRiddenByEntity(), player);
   }

   public boolean canSwitchFreeLook() {
      return true;
   }

   public boolean isFreeLookMode() {
      return this.getCommonStatus(1) || this.isRepelling();
   }

   public void switchFreeLookMode(boolean b) {
      this.setCommonStatus(1, b);
   }

   public void switchFreeLookModeClient(boolean b) {
      this.setCommonStatus(1, b, true);
   }

   public boolean canSwitchGunnerFreeLook(EntityPlayer player) {
      MCH_SeatInfo seatInfo = this.getSeatInfo(player);
      return seatInfo != null && seatInfo.fixRot && this.getIsGunnerMode(player);
   }

   public boolean isGunnerLookMode(EntityPlayer player) {
      return this.isPilot(player)?false:this.isGunnerFreeLookMode;
   }

   public void switchGunnerFreeLookMode(boolean b) {
      this.isGunnerFreeLookMode = b;
   }

   public void switchGunnerFreeLookMode() {
      this.switchGunnerFreeLookMode(!this.isGunnerFreeLookMode);
   }

   public void updateParts(int stat) {
      if(!this.isDestroyed()) {
         MCH_Parts[] parts = new MCH_Parts[]{this.partHatch, this.partCanopy, this.partLandingGear};
         MCH_Parts[] blockId = parts;
         int unfold = parts.length;

         for(int i$ = 0; i$ < unfold; ++i$) {
            MCH_Parts p = blockId[i$];
            if(p != null) {
               p.updateStatusClient(stat);
               p.update();
            }
         }

         if(!this.isDestroyed() && !super.worldObj.isRemote && this.partLandingGear != null) {
            boolean var7 = false;
            int var8;
            if(!this.isLandingGearFolded() && this.partLandingGear.getFactor() <= 0.1F) {
               var8 = MCH_Lib.getBlockIdY(this, 3, -20);
               if((this.getCurrentThrottle() <= 0.800000011920929D || super.onGround || var8 != 0) && this.getAcInfo().isFloat && (this.isInWater() || MCH_Lib.getBlockY(this, 3, -20, true) == W_Block.getWater())) {
                  this.partLandingGear.setStatusServer(true);
               }
            } else if(this.isLandingGearFolded() && this.partLandingGear.getFactor() >= 0.9F) {
               var8 = MCH_Lib.getBlockIdY(this, 3, -10);
               if(this.getCurrentThrottle() < (double)this.getUnfoldLandingGearThrottle() && var8 != 0) {
                  boolean var9 = true;
                  if(this.getAcInfo().isFloat) {
                     var8 = MCH_Lib.getBlockIdY(super.worldObj, super.posX, super.posY + 1.0D + (double)this.getAcInfo().floatOffset, super.posZ, 1, -150, true);
                     if(W_Block.isEqual(var8, W_Block.getWater())) {
                        var9 = false;
                     }
                  }

                  if(var9) {
                     this.partLandingGear.setStatusServer(false);
                  }
               } else if(this.getVtolMode() == 2 && var8 != 0) {
                  this.partLandingGear.setStatusServer(false);
               }
            }
         }

      }
   }

   public float getUnfoldLandingGearThrottle() {
      return 0.8F;
   }

   private int getPartStatus() {
      return this.getDataWatcher().getWatchableObjectInt(31);
   }

   private void setPartStatus(int n) {
      this.getDataWatcher().updateObject(31, Integer.valueOf(n));
   }

   protected void initPartRotation(float yaw, float pitch) {
      this.lastRiderYaw = yaw;
      this.prevLastRiderYaw = yaw;
      this.camera.partRotationYaw = yaw;
      this.camera.prevPartRotationYaw = yaw;
      this.lastSearchLightYaw = yaw;
   }

   public int getLastPartStatusMask() {
      return 24;
   }

   public int getModeSwitchCooldown() {
      return this.modeSwitchCooldown;
   }

   public void setModeSwitchCooldown(int n) {
      this.modeSwitchCooldown = n;
   }

   protected MCH_EntityBaseVehicle.WeaponBay[] createWeaponBays() {
      MCH_EntityBaseVehicle.WeaponBay[] wbs = new MCH_EntityBaseVehicle.WeaponBay[this.getAcInfo().partWeaponBay.size()];

      for(int i = 0; i < wbs.length; ++i) {
         wbs[i] = new MCH_EntityBaseVehicle.WeaponBay();
      }

      return wbs;
   }

   protected MCH_Parts createHatch() {
      MCH_Parts hatch = null;
      if(this.getAcInfo().haveHatch()) {
         hatch = new MCH_Parts(this, 4, 31, "Hatch");
         hatch.rotationMax = 90.0F;
         hatch.rotationInv = 1.5F;
         hatch.soundEndSwichOn.setPrm("plane_cc", 1.0F, 1.0F);
         hatch.soundEndSwichOff.setPrm("plane_cc", 1.0F, 1.0F);
         hatch.soundSwitching.setPrm("plane_cv", 1.0F, 0.5F);
      }

      return hatch;
   }

   public boolean haveHatch() {
      return this.partHatch != null;
   }

   public boolean canFoldHatch() {
      return this.partHatch != null && this.modeSwitchCooldown <= 0?this.partHatch.isOFF():false;
   }

   public boolean canUnfoldHatch() {
      return this.partHatch != null && this.modeSwitchCooldown <= 0?this.partHatch.isON():false;
   }

   public void foldHatch(boolean fold) {
      this.foldHatch(fold, false);
   }

   public void foldHatch(boolean fold, boolean force) {
      if(this.partHatch != null) {
         if(force || this.modeSwitchCooldown <= 0) {
            this.partHatch.setStatusServer(fold);
            this.modeSwitchCooldown = 20;
            if(!fold) {
               this.stopUnmountCrew();
            }

         }
      }
   }

   public float getHatchRotation() {
      return this.partHatch != null?this.partHatch.rotation:0.0F;
   }

   public float getPrevHatchRotation() {
      return this.partHatch != null?this.partHatch.prevRotation:0.0F;
   }

   public void foldLandingGear() {
      if(this.partLandingGear != null && this.getModeSwitchCooldown() <= 0) {
         this.partLandingGear.setStatusServer(true);
         this.setModeSwitchCooldown(20);
      }
   }

   public void unfoldLandingGear() {
      if(this.partLandingGear != null && this.getModeSwitchCooldown() <= 0) {
         if(this.isLandingGearFolded()) {
            this.partLandingGear.setStatusServer(false);
            this.setModeSwitchCooldown(20);
         }

      }
   }

   public boolean canFoldLandingGear() {
      if(this.getLandingGearRotation() >= 1.0F) {
         return false;
      } else {
         Block block = MCH_Lib.getBlockY(this, 3, -10, true);
         return !this.isLandingGearFolded() && block == Blocks.air;
      }
   }

   public boolean canUnfoldLandingGear() {
      return this.getLandingGearRotation() < 89.0F?false:this.isLandingGearFolded();
   }

   public boolean isLandingGearFolded() {
      return this.partLandingGear != null?this.partLandingGear.getStatus():false;
   }

   protected MCH_Parts createLandingGear() {
      MCH_Parts lg = null;
      if(this.getAcInfo().haveLandingGear()) {
         lg = new MCH_Parts(this, 2, 31, "LandingGear");
         lg.rotationMax = 90.0F;
         lg.rotationInv = 2.5F;
         lg.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         lg.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         lg.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
         lg.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
         lg.soundSwitching.setPrm("plane_cv", 1.0F, 0.75F);
      }

      return lg;
   }

   public float getLandingGearRotation() {
      return this.partLandingGear != null?this.partLandingGear.rotation:0.0F;
   }

   public float getPrevLandingGearRotation() {
      return this.partLandingGear != null?this.partLandingGear.prevRotation:0.0F;
   }

   public int getVtolMode() {
      return 0;
   }

   public void openCanopy() {
      if(this.partCanopy != null && this.getModeSwitchCooldown() <= 0) {
         this.partCanopy.setStatusServer(true);
         this.setModeSwitchCooldown(20);
      }
   }

   public void openCanopy_EjectSeat() {
      if(this.partCanopy != null) {
         this.partCanopy.setStatusServer(true, false);
         this.setModeSwitchCooldown(40);
      }
   }

   public void closeCanopy() {
      if(this.partCanopy != null && this.getModeSwitchCooldown() <= 0) {
         if(this.getCanopyStat()) {
            this.partCanopy.setStatusServer(false);
            this.setModeSwitchCooldown(20);
         }

      }
   }

   public boolean getCanopyStat() {
      return this.partCanopy != null?this.partCanopy.getStatus():false;
   }

   public boolean isCanopyClose() {
      return this.partCanopy == null?true:!this.getCanopyStat() && this.getCanopyRotation() <= 0.01F;
   }

   public float getCanopyRotation() {
      return this.partCanopy != null?this.partCanopy.rotation:0.0F;
   }

   public float getPrevCanopyRotation() {
      return this.partCanopy != null?this.partCanopy.prevRotation:0.0F;
   }

   protected MCH_Parts createCanopy() {
      MCH_Parts canopy = null;
      if(this.getAcInfo().haveCanopy()) {
         canopy = new MCH_Parts(this, 0, 31, "Canopy");
         canopy.rotationMax = 90.0F;
         canopy.rotationInv = 3.5F;
         canopy.soundEndSwichOn.setPrm("plane_cc", 1.0F, 1.0F);
         canopy.soundEndSwichOff.setPrm("plane_cc", 1.0F, 1.0F);
      }

      return canopy;
   }

   public boolean hasBrake() {
      return false;
   }

   public void setBrake(boolean b) {
      if(!super.worldObj.isRemote) {
         this.setCommonStatus(11, b);
      }

   }

   public boolean getBrake() {
      return this.getCommonStatus(11);
   }

   public int getSizeInventory() {
      return this.getAcInfo() != null?this.getAcInfo().inventorySize:0;
   }

   public String getInvName() {
      if(this.getAcInfo() == null) {
         return super.getInvName();
      } else {
         //TODO usually unlocalized dogshit, needs to be fixed
         String s = this.getAcInfo().displayName;
         return s.length() <= 32?s:s.substring(0, 31);
      }
   }

   public boolean isInvNameLocalized() {
      return this.getAcInfo() != null;
   }

   public boolean getGunnerStatus() {
      return getCommonStatus(12);
   }

   public void setGunnerStatus(boolean b) {
      if (!this.worldObj.isRemote)
         setCommonStatus(12, b);
   }

   public MCH_EntityChain getTowChainEntity() {
      return this.towChainEntity;
   }

   public void setTowChainEntity(MCH_EntityChain chainEntity) {
      this.towChainEntity = chainEntity;
   }

   public MCH_EntityChain getTowedChainEntity() {
      return this.towedChainEntity;
   }

   public void setTowedChainEntity(MCH_EntityChain towedChainEntity) {
      this.towedChainEntity = towedChainEntity;
   }

    public String getNameOnOtherRadar(MCH_EntityBaseVehicle other) {
      switch (other.getAcInfo().radarType) {
         case MODERN_AA: return getAcInfo().nameOnModernAARadar;
         case EARLY_AA: return getAcInfo().nameOnEarlyAARadar;
         case MODERN_AS: return getAcInfo().nameOnModernASRadar;
         case EARLY_AS: return getAcInfo().nameOnEarlyASRadar;
      }
      return "?";
    }

    //public String getNameOnMyRadar(MCH_EntityBaseVehicle other) {
    //   switch (getAcInfo().radarType) {
    //      case MODERN_AA: return other.getAcInfo().nameOnModernAARadar;
    //      case EARLY_AA: return other.getAcInfo().nameOnEarlyAARadar;
    //      case MODERN_AS: return other.getAcInfo().nameOnModernASRadar;
    //      case EARLY_AS: return other.getAcInfo().nameOnEarlyASRadar;
    //   }
    //   return "?";
    //}

   public String getNameOnMyRadar(MCH_EntityInfo other) {
      MCH_BaseVehicleInfo info = MCH_BaseVehicleInfo.allBaseVehicleInfo.getOrDefault(other.entityName, null);

      try {
         switch (getAcInfo().radarType) {
            case MODERN_AA:
               return info.nameOnModernAARadar;
            case EARLY_AA:
               return info.nameOnEarlyAARadar;
            case MODERN_AS:
               return info.nameOnModernASRadar;
            case EARLY_AS:
               return info.nameOnEarlyASRadar;
         }
      } catch (Exception ex) {
         return null;
      }

      return "?";
   }

   /** Returns whether this vehicle has an enabled laser warning receiver. */
   public boolean hasLWR() {
      return this.getAcInfo() != null && this.getAcInfo().LWR;
   }

   /** Keeps legacy warning audio for non-tanks while making tanks opt in with LWR. */
   public boolean canPlayAlertSound() {
      return !(this instanceof MCH_EntityTank) || this.hasLWR();
   }

   /** Lock packets historically required flares; tank LWR is independent of them. */
   public boolean canNotifyLock() {
      return this instanceof MCH_EntityTank ? this.hasLWR() : this.haveFlare();
   }

   /** Tank warning detection may run without flares only when LWR is enabled. */
   public boolean canDetectWarning() {
      return this.haveFlare() || this instanceof MCH_EntityTank && this.hasLWR();
   }

   public class WeaponBay {

      public float rot = 0.0F;
      public float prevRot = 0.0F;


   }

   protected class UnmountReserve {

      final Entity entity;
      final double posX;
      final double posY;
      final double posZ;
      int cnt = 5;


      public UnmountReserve(Entity e, double x, double y, double z) {
         this.entity = e;
         this.posX = x;
         this.posY = y;
         this.posZ = z;
      }
   }
}
