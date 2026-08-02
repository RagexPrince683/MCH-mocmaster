package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_Math;
import mcheli.aircraft.*;
import mcheli.chain.MCH_EntityChain;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.command.IEntitySelector;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import io.netty.buffer.ByteBuf;

//the cursed extension
//refactoring would be a fucking nightmare
public class MCH_EntityTank extends MCH_EntityBaseVehicle {

   private MCH_TankInfo tankInfo = null;
   public float soundVolume;
   public float soundVolumeTarget;
   public float rotationRotor;
   public float prevRotationRotor;
   public float addkeyRotValue;
   public final MCH_WheelManager WheelMng;
   public float partialTicks;
   private int trackDamageTaken;
   public boolean turretPopStarted;
   public boolean turretPopLanded;
   public double turretPopX, turretPopY, turretPopZ;
   public double prevTurretPopX, prevTurretPopY, prevTurretPopZ;
   public double turretPopMotionX, turretPopMotionY, turretPopMotionZ;
   public float turretPopYaw, turretPopPitch, turretPopRoll;
   public float prevTurretPopYaw, prevTurretPopPitch, prevTurretPopRoll;
   public float turretPopAngularYaw, turretPopAngularPitch, turretPopAngularRoll;
   /** Frozen local pose of the canonical $turret assembly at destruction. */
   public float turretPopFrozenYaw, turretPopFrozenPitch;
   public int turretPopAge;
   private boolean turretPopMissingPartWarned;
   /** Persisted destruction edge latch; prevents old wrecks from starting after reload. */
   private boolean turretPopDestructionObserved;

   //TODO
   private int currentGear = 1;  // Starting gear
   private final int maxGear = 5;  // Number of gears
   private double[] gearSpeedLimits = {5.0D, 10.0D, 20.0D, 30.0D, 40.0D};  // Speed limits for each gear
   private double[] gearAccelerationMultipliers = {1.0D, 0.8D, 0.6D, 0.4D, 0.2D};  // Acceleration dampening for higher gears


   public MCH_EntityTank(World world) {
      super(world);
      super.currentSpeed = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(2.0F, 0.7F);
      super.yOffset = super.height / 2.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      super.weapons = this.createWeapon(0);
      this.soundVolume = 0.0F;
      super.stepHeight = 0.6F;
      this.rotationRotor = 0.0F;
      this.prevRotationRotor = 0.0F;
      this.WheelMng = new MCH_WheelManager(this);
      this.trackDamageTaken = 0;
      this.turretPopStarted = false;
      this.turretPopLanded = false;
      this.turretPopDestructionObserved = false;
   }

   //tracks are very broken
   public int getTrackMaxHP() {
      return this.tankInfo != null?Math.max(1, this.tankInfo.trackMaxHP):1;
   }

   public int getTrackHP() {
      return Math.max(0, this.getTrackMaxHP() - this.trackDamageTaken);
   }

   public boolean isTrackDestroyed() {
      return this.getTrackHP() <= 0;
   }

   public String getKindName() {
      return "tanks";
   }

   public String getEntityType() {
      return "Vehicle"; // Legacy display type for tanks.
   }

   public MCH_TankInfo getTankInfo() {
      return this.tankInfo;
   }

   public void changeType(String type) {
      if(!type.isEmpty()) {
         this.tankInfo = MCH_TankInfoManager.get(type);
      }

      if(this.tankInfo == null) {
         MCH_Lib.Log((Entity)this, "##### MCH_EntityTank changeTankType() Tank info null %d, %s, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), type, this.getEntityName()});
         this.setDead();
      } else {
         this.setAcInfo(this.tankInfo);
         this.newSeats(this.getAcInfo().getNumSeatAndRack());
         this.switchFreeLookModeClient(this.getAcInfo().defaultFreelook);
         super.weapons = this.createWeapon(1 + this.getSeatNum());
         this.initPartRotation(this.getRotYaw(), this.getRotPitch());
         this.WheelMng.createWheels(super.worldObj, this.getAcInfo().wheels, Vec3.createVectorHelper(0.0D, -0.35D, (double)this.getTankInfo().weightedCenterZ));
      }

   }

   public Item getItem() {
      return this.getTankInfo() != null?this.getTankInfo().item:null;
   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MountMinecartTank.prmBool;
   }

   protected void entityInit() {
      super.entityInit();
   }

   public float getGiveDamageRot() {
      return 91.0F;
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setInteger("TrackDamage", this.trackDamageTaken);
      par1NBTTagCompound.setBoolean("TurretPopStarted", this.turretPopStarted);
      par1NBTTagCompound.setBoolean("TurretPopDestructionObserved", this.turretPopDestructionObserved || this.isDestroyed());
      if(this.turretPopStarted) {
         par1NBTTagCompound.setBoolean("TurretPopLanded", this.turretPopLanded);
         par1NBTTagCompound.setDouble("TurretPopX", this.turretPopX); par1NBTTagCompound.setDouble("TurretPopY", this.turretPopY); par1NBTTagCompound.setDouble("TurretPopZ", this.turretPopZ);
         par1NBTTagCompound.setDouble("TurretPopMX", this.turretPopMotionX); par1NBTTagCompound.setDouble("TurretPopMY", this.turretPopMotionY); par1NBTTagCompound.setDouble("TurretPopMZ", this.turretPopMotionZ);
         par1NBTTagCompound.setFloat("TurretPopYaw", this.turretPopYaw); par1NBTTagCompound.setFloat("TurretPopPitch", this.turretPopPitch); par1NBTTagCompound.setFloat("TurretPopRoll", this.turretPopRoll);
         par1NBTTagCompound.setFloat("TurretPopAY", this.turretPopAngularYaw); par1NBTTagCompound.setFloat("TurretPopAP", this.turretPopAngularPitch); par1NBTTagCompound.setFloat("TurretPopAR", this.turretPopAngularRoll);
         par1NBTTagCompound.setFloat("TurretPopFrozenYaw", this.turretPopFrozenYaw); par1NBTTagCompound.setFloat("TurretPopFrozenPitch", this.turretPopFrozenPitch);
         par1NBTTagCompound.setInteger("TurretPopAge", this.turretPopAge);
      }
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      this.trackDamageTaken = Math.max(0, par1NBTTagCompound.getInteger("TrackDamage"));
      this.turretPopStarted = par1NBTTagCompound.getBoolean("TurretPopStarted");
      // Old saves have no latch: an already destroyed entity must be treated as observed.
      this.turretPopDestructionObserved = par1NBTTagCompound.hasKey("TurretPopDestructionObserved")
            ? par1NBTTagCompound.getBoolean("TurretPopDestructionObserved") : this.isDestroyed();
      if(this.turretPopStarted) {
         this.turretPopLanded = par1NBTTagCompound.getBoolean("TurretPopLanded");
         this.turretPopX = this.prevTurretPopX = par1NBTTagCompound.getDouble("TurretPopX"); this.turretPopY = this.prevTurretPopY = par1NBTTagCompound.getDouble("TurretPopY"); this.turretPopZ = this.prevTurretPopZ = par1NBTTagCompound.getDouble("TurretPopZ");
         this.turretPopMotionX = par1NBTTagCompound.getDouble("TurretPopMX"); this.turretPopMotionY = par1NBTTagCompound.getDouble("TurretPopMY"); this.turretPopMotionZ = par1NBTTagCompound.getDouble("TurretPopMZ");
         this.turretPopYaw = this.prevTurretPopYaw = par1NBTTagCompound.getFloat("TurretPopYaw"); this.turretPopPitch = this.prevTurretPopPitch = par1NBTTagCompound.getFloat("TurretPopPitch"); this.turretPopRoll = this.prevTurretPopRoll = par1NBTTagCompound.getFloat("TurretPopRoll");
         this.turretPopAngularYaw = par1NBTTagCompound.getFloat("TurretPopAY"); this.turretPopAngularPitch = par1NBTTagCompound.getFloat("TurretPopAP"); this.turretPopAngularRoll = par1NBTTagCompound.getFloat("TurretPopAR"); this.turretPopAge = Math.max(0, par1NBTTagCompound.getInteger("TurretPopAge"));
         this.turretPopFrozenYaw = par1NBTTagCompound.getFloat("TurretPopFrozenYaw"); this.turretPopFrozenPitch = par1NBTTagCompound.getFloat("TurretPopFrozenPitch");
      }
      if(this.tankInfo == null) {
         this.tankInfo = MCH_TankInfoManager.get(this.getTypeName());
         if(this.tankInfo == null) {
            MCH_Lib.Log((Entity)this, "##### MCH_EntityTank readEntityFromNBT() Tank info null %d, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName()});
            this.setDead();
         } else {
            this.setAcInfo(this.tankInfo);
         }
      }

   }

   public void setDead() {
      super.setDead();
   }

   public void onInteractFirst(EntityPlayer player) {
      this.addkeyRotValue = 0.0F;
      player.rotationYawHead = player.prevRotationYawHead = this.getLastRiderYaw();
      player.prevRotationYaw = player.rotationYaw = this.getLastRiderYaw();
      player.rotationPitch = this.getLastRiderPitch();
   }

   public boolean canSwitchGunnerMode() {
      return !super.canSwitchGunnerMode()?false:false;
   }

   public void onUpdateAircraft() {

      //add partial ticks here???


      if(this.tankInfo == null) {
         this.changeType(this.getTypeName());
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
      } else {
         if(!super.worldObj.isRemote) {
            this.updateTurretPopDestructionTransition();
            this.updateTurretPop();
         }
         else {
            this.predictTurretPopClient();
            this.updateTurretPopSmoke();
         }
         if(!super.isRequestedSyncStatus) {
            super.isRequestedSyncStatus = true;
            if(super.worldObj.isRemote) {
               MCH_PacketStatusRequest.requestStatus(this);
            }
         }

         if(super.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
            this.initCurrentWeapon(this.getRiddenByEntity());
         }

         this.updateWeapons();
         this.onUpdate_Seats();
         this.onUpdate_Control(partialTicks);
         this.prevRotationRotor = this.rotationRotor;
         this.rotationRotor = (float)((double)this.rotationRotor + this.getCurrentThrottle() * (double)this.getAcInfo().rotorSpeed);
         if(this.rotationRotor > 360.0F) {
            this.rotationRotor -= 360.0F;
            this.prevRotationRotor -= 360.0F;
         }

         if(this.rotationRotor < 0.0F) {
            this.rotationRotor += 360.0F;
            this.prevRotationRotor += 360.0F;
         }

         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
         if(this.isDestroyed() && this.getCurrentThrottle() > 0.0D) {
            if(MCH_Lib.getBlockIdY(this, 3, -2) > 0) {
               this.setCurrentThrottle(this.getCurrentThrottle() * 0.8D);
            }

            if(this.isExploded()) {
               this.setCurrentThrottle(this.getCurrentThrottle() * 0.98D);
            }
         }

         this.updateCameraViewers();
         if(super.worldObj.isRemote) {
            this.onUpdate_Client();
         } else {
            this.onUpdate_Server();
         }

      }
   }

   @SideOnly(Side.CLIENT)
   public boolean canRenderOnFire() {
      return this.isDestroyed() || super.canRenderOnFire();
   }

   public void updateExtraBoundingBox() {
      if(super.worldObj.isRemote) {
         super.updateExtraBoundingBox();
      } else if(this.getCountOnUpdate() <= 1) {
         super.updateExtraBoundingBox();
         super.updateExtraBoundingBox();
      }

   }

   public double calculateXOffset(List list, AxisAlignedBB bb, double parX) {
      for(int i = 0; i < list.size(); ++i) {
         parX = ((AxisAlignedBB)list.get(i)).calculateXOffset(bb, parX);
      }

      bb.offset(parX, 0.0D, 0.0D);
      return parX;
   }

   public double calculateYOffset(List list, AxisAlignedBB bb, double parY) {
      for(int i = 0; i < list.size(); ++i) {
         parY = ((AxisAlignedBB)list.get(i)).calculateYOffset(bb, parY);
      }

      bb.offset(0.0D, parY, 0.0D);
      return parY;
   }

   public double calculateZOffset(List list, AxisAlignedBB bb, double parZ) {
      for(int i = 0; i < list.size(); ++i) {
         parZ = ((AxisAlignedBB)list.get(i)).calculateZOffset(bb, parZ);
      }

      bb.offset(0.0D, 0.0D, parZ);
      return parZ;
   }

   public void moveEntity(double parX, double parY, double parZ) {

      // Check for slowing blocks under the tank, and slow the tank
      Block blockUnder = MCH_Lib.getBlockY(this, 3, -2, false);
      if (BlockUtils.isSlowingBlock(blockUnder, super.worldObj, (int)super.posX, (int)super.posY, (int)super.posZ, this)) {
         // Apply 20% speed reduction for slowing blocks
         parX *= 0.8; // Reduce X movement by 20%
         parZ *= 0.8; // Reduce Z movement by 20%
      }

      super.worldObj.theProfiler.startSection("move");
      super.ySize *= 0.4F;
      double nowPosX = super.posX;
      double nowPosY = super.posY;
      double nowPosZ = super.posZ;
      double mx = parX;
      double my = parY;
      double mz = parZ;
      AxisAlignedBB backUpAxisalignedBB = super.boundingBox.copy();
      List list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(parX, parY, parZ));
      parY = this.calculateYOffset(list, super.boundingBox, parY);
      boolean flag1 = super.onGround || my != parY && my < 0.0D;
      MCH_BoundingBox[] prevPX = super.extraBoundingBox;
      int len$ = prevPX.length;

      for(int prevPZ = 0; prevPZ < len$; ++prevPZ) {
         MCH_BoundingBox ebb = prevPX[prevPZ];
         ebb.updatePosition(super.posX, super.posY, super.posZ, this.getRotYaw(), this.getRotPitch(), this.getRotRoll());
      }

      parX = this.calculateXOffset(list, super.boundingBox, parX);
      parZ = this.calculateZOffset(list, super.boundingBox, parZ);
      double minX;
      double var38;
      double var39;
      if(super.stepHeight > 0.0F && flag1 && super.ySize < 0.05F && (mx != parX || mz != parZ)) {
         var38 = parX;
         var39 = parY;
         minX = parZ;
         parY = (double)super.stepHeight;
         AxisAlignedBB minZ = super.boundingBox.copy();
         super.boundingBox.setBB(backUpAxisalignedBB);
         list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(mx, parY, mz));
         this.calculateYOffset(list, super.boundingBox, parY);
         parX = this.calculateXOffset(list, super.boundingBox, mx);
         parZ = this.calculateZOffset(list, super.boundingBox, mz);
         parY = this.calculateYOffset(list, super.boundingBox, (double)(-super.stepHeight));
         if(var38 * var38 + minX * minX >= parX * parX + parZ * parZ) {
            parX = var38;
            parY = var39;
            parZ = minX;
            super.boundingBox.setBB(minZ);
         }
      }

      var38 = super.posX;
      var39 = super.posZ;
      super.worldObj.theProfiler.endSection();
      super.worldObj.theProfiler.startSection("rest");
      minX = super.boundingBox.minX;
      double var40 = super.boundingBox.minZ;
      double maxX = super.boundingBox.maxX;
      double maxZ = super.boundingBox.maxZ;
      super.posX = (minX + maxX) / 2.0D;
      super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
      super.posZ = (var40 + maxZ) / 2.0D;
      super.isCollidedHorizontally = mx != parX || mz != parZ;
      super.isCollidedVertically = my != parY;
      super.onGround = my != parY && my < 0.0D;
      super.isCollided = super.isCollidedHorizontally || super.isCollidedVertically;
      this.updateFallState(parY, super.onGround);
      if(mx != parX) {
         super.motionX = 0.0D;
      }

      if(my != parY) {
         super.motionY = 0.0D;
      }

      if(mz != parZ) {
         super.motionZ = 0.0D;
      }

      try {
         this.doBlockCollisions();
      } catch (Throwable var37) {
         CrashReport crashreport = CrashReport.makeCrashReport(var37, "Checking entity tile collision");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
         this.addEntityCrashInfo(crashreportcategory);
      }

      super.worldObj.theProfiler.endSection();
   }
//help
   private void rotationByKey(float partialTicks) {
      float rot = 0.2F;
      if(super.moveLeft && !super.moveRight) {
         this.addkeyRotValue -= rot * partialTicks;
      }

      if(super.moveRight && !super.moveLeft) {
         this.addkeyRotValue += rot * partialTicks;
      }

   }

   public void onUpdateAngles(float partialTicks) {
      if(this.useNewMobilitySystem()) {
         partialTicks = MCH_FlightModel.getBoundedTickDelta(partialTicks);
      }
      if(!this.isDestroyed()) {
         if(super.isGunnerMode) {
            this.setRotPitch(this.decayMobilityValue(this.getRotPitch(), 0.95F, partialTicks));
            this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 0.2F * partialTicks);
            if(MathHelper.abs(this.getRotRoll()) > 20.0F) {
               this.setRotRoll(this.decayMobilityValue(this.getRotRoll(), 0.95F, partialTicks));
            }
         }

         this.updateRecoil(partialTicks);
         this.setRotPitch(this.getRotPitch() + (this.WheelMng.targetPitch - this.getRotPitch()) * partialTicks);
         this.setRotRoll(this.getRotRoll() + (this.WheelMng.targetRoll - this.getRotRoll()) * partialTicks);
         boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
         //System.out.println("isfly" + isFly);

         //logic for like rotation
         if(!isFly || this.getAcInfo().isFloat && this.getWaterDepth() > 0.0D) {
            float rotonground = 1.0F;
            if(!isFly) {
               rotonground = this.getAcInfo().mobilityYawOnGround;
               if(!this.getAcInfo().canRotOnGround) {
                  Block pivotTurnThrottle = MCH_Lib.getBlockY(this, 3, -2, false);
                  if(!W_Block.isEqual(pivotTurnThrottle, W_Block.getWater()) && !W_Block.isEqual(pivotTurnThrottle, Blocks.air)) {
                     rotonground = 0.0F;
                  }
               }
            }

            float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
            double dx = super.posX - super.prevPosX;
            double dz = super.posZ - super.prevPosZ;
            double dist = dx * dx + dz * dz;

            if(pivotTurnThrottle1 <= 0.0F || this.getCurrentThrottle() >= (double)pivotTurnThrottle1 || super.throttleBack >= pivotTurnThrottle1 / 10.0F || dist > (double)super.throttleBack * 0.01D) {
               float sf = (float)Math.sqrt(dist <= 1.0D?dist:1.0D);
               if(pivotTurnThrottle1 <= 0.0F) {
                  sf = 1.0F;
               }

               float flag = !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 + 0.05D?-1.0F:1.0F;
               if(super.moveLeft && !super.moveRight) {
                  this.setRotYaw(this.getRotYaw() - 0.6F * rotonground * partialTicks * flag * sf);
               }

               if(super.moveRight && !super.moveLeft) {
                  this.setRotYaw(this.getRotYaw() + 0.6F * rotonground * partialTicks * flag * sf);
               }

            }
         }
         else if(!super.onGround || this.WheelMng.hasWheelContact()) {
            this.applyMovingAirborneSteering(partialTicks);
         }

         this.addkeyRotValue = this.decayMobilityValue(this.addkeyRotValue, 0.9F, partialTicks);
      }
   }

   /**
    * Retains directional yaw while a moving tank is briefly unsupported or has
    * only partial track contact. Grounded and floating steering remain handled
    * by the existing path above.
    */
   private void applyMovingAirborneSteering(float partialTicks) {
      float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
      if(pivotTurnThrottle1 > 0.0F && this.getAcInfo().enableBack && super.throttleDown && this.getCurrentThrottle() <= 0.0D && super.throttleBack > 0.0F) {
         // onUpdate_ControlSub already applies the established reverse yaw here.
         return;
      }

      double dx = super.posX - super.prevPosX;
      double dz = super.posZ - super.prevPosZ;
      double dist = dx * dx + dz * dz;
      if(dist <= 1.0E-6D) {
         dist = super.motionX * super.motionX + super.motionZ * super.motionZ;
      }

      if(dist <= 1.0E-6D) {
         return;
      }

      if(pivotTurnThrottle1 <= 0.0F || this.getCurrentThrottle() >= (double)pivotTurnThrottle1 || super.throttleBack >= pivotTurnThrottle1 / 10.0F || dist > (double)super.throttleBack * 0.01D) {
         float sf = (float)Math.sqrt(dist <= 1.0D?dist:1.0D);
         if(pivotTurnThrottle1 <= 0.0F) {
            sf = 1.0F;
         }

         float flag = !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 + 0.05D?-1.0F:1.0F;
         if(super.moveLeft && !super.moveRight) {
            this.setRotYaw(this.getRotYaw() - 0.6F * partialTicks * flag * sf);
         }

         if(super.moveRight && !super.moveLeft) {
            this.setRotYaw(this.getRotYaw() + 0.6F * partialTicks * flag * sf);
         }
      }
   }

   protected void onUpdate_Control(float partialTicks) {

      if(this.isTrackDestroyed()) {
         this.setCurrentThrottle(0.0D);
         this.setThrottle(0.0D);
         super.throttleUp = false;
         super.throttleDown = false;
         super.throttleBack = 0.0F;
         return;
      }

      if(this.applyEngineWaterboardingThrottleCut()) {
         return;
      }

      if(getHP() * 100 / getMaxHP() < getAcInfo().engineShutdownThreshold) {
         setCurrentThrottle(0);
         throttleUp = false;
         throttleBack = 0;
         return;
      }

      if(super.isGunnerMode && !this.canUseFuel()) {
         this.switchGunnerMode(false);
      }

      super.throttleBack = (float)((double)super.throttleBack * 0.8D);
      if(this.getBrake()) {
         super.throttleBack = (float)((double)super.throttleBack * 0.5D); //todo: add braking force variable here
         if(this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-0.02D * (double)this.getAcInfo().throttleUpDown);
         } else {
            this.setCurrentThrottle(0.0D);
         }
      }

      if(this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead && this.isCanopyClose() && this.canUseFuel() && !this.isDestroyed()) {
         this.onUpdate_ControlSub(partialTicks);
      } else if(this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
         super.throttleUp = true;
         this.onUpdate_ControlSub(partialTicks);
      } else if(this.getCurrentThrottle() > 0.0D) {
         this.addCurrentThrottle(-0.0025D * (double)this.getAcInfo().throttleUpDown);
      } else {
         this.setCurrentThrottle(0.0D);
      }

      if(this.getCurrentThrottle() < 0.0D) {
         this.setCurrentThrottle(0.0D);
      }

      if(super.worldObj.isRemote) {
         if(!W_Lib.isClientPlayer(this.getRiddenByEntity()) || this.getCountOnUpdate() % 200 == 0) {
            double ct = this.getThrottle();
            if(this.getCurrentThrottle() > ct) {
               this.addCurrentThrottle(-0.005D);
            }

            if(this.getCurrentThrottle() < ct) {
               this.addCurrentThrottle(0.005D);
            }
         }
      } else {
         this.setThrottle(this.getCurrentThrottle());
      }

   }

   protected void fall(float distance) {
      if(!super.worldObj.isRemote && distance > 3.0F && !this.isDestroyed()) {
         float damage = (distance - 3.0F) * 2.0F;
         this.attackEntityFrom(DamageSource.fall, damage);
      }

      if(this.getRiddenByEntity() != null) {
         this.getRiddenByEntity().fallDistance = 0.0F;
      }
   }

   public boolean attackEntityFrom(DamageSource damageSource, float damage) {
      EnumBoundingBoxType hitType = this.lastHitBoundingBoxType;
      if(!super.worldObj.isRemote && hitType == EnumBoundingBoxType.TRACK && !this.isDestroyed()) {
         this.lastBBDamageFactor = 1.0F;
         if(this.trackDamageTaken > this.getTrackMaxHP()) {
            this.trackDamageTaken = this.getTrackMaxHP();
         }
         this.lastHitBoundingBoxType = EnumBoundingBoxType.DEFAULT;
         this.trackDamageTaken += Math.max(1, (int)damage);


         this.setBeenAttacked();
         this.timeSinceHit = 1;
         return true;
      }

      boolean attacked = super.attackEntityFrom(damageSource, damage);
      return attacked;
   }

   protected void onUpdate_ControlSub(float partialTicks) {

      if(!super.isGunnerMode) {
         float throttleUpDown = this.getAcInfo().throttleUpDown;
         if(super.throttleUp) {
            float f = throttleUpDown;
            if(this.getRidingEntity() != null) {
               double mx = this.getRidingEntity().motionX;
               double mz = this.getRidingEntity().motionZ;
               f = throttleUpDown * MathHelper.sqrt_double(mx * mx + mz * mz) * this.getAcInfo().throttleUpDownOnEntity;
            }

            if(this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
               super.throttleBack = (float)((double)super.throttleBack - 0.01D * (double)f);
            } else {
               super.throttleBack = 0.0F;
               if(this.getCurrentThrottle() < 1.0D) {
                  this.addCurrentThrottle(0.01D * (double)f);
                  //here as well?
               } else {
                  this.setCurrentThrottle(1.0D);
                  //implement a new variable here to add throttle control for specific vehicles specifically 1.8D
               }
            }



         } else if(super.throttleDown) {



            if(this.getCurrentThrottle() > 0.0D) {
               this.addCurrentThrottle(-0.01D * (double)throttleUpDown);
            } else {
               this.setCurrentThrottle(0.0D);
               if(this.getAcInfo().enableBack) {
                 // super.throttleBack = (float)((double)super.throttleBack + 0.0025D * (double)throttleUpDown);
                  super.throttleBack = (float)((double)super.throttleBack + 0.0025D * (double)throttleUpDown * getAcInfo().throttleDownFactor);
//                  if(super.throttleBack > 0.6F) { //todno: add a new variable here for reversespeed
//                     super.throttleBack = 0.6F;
//                  }
                  float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
                  if (pivotTurnThrottle1 > 0) {
                     if (super.throttleBack > 0) {
                        double dx = super.posX - super.prevPosX;
                        double dz = super.posZ - super.prevPosZ;
                        double dist = dx * dx + dz * dz;
                        float sf = (float)Math.sqrt(dist <= 1.0D ? dist : 1.0D);
                        if (pivotTurnThrottle1 <= 0.0F) {
                           sf = 1.0F;
                        }

                        float rotonground = 1.0F;
                        boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;

                        if (!isFly) {
                           rotonground = this.getAcInfo().mobilityYawOnGround;
                           if (!this.getAcInfo().canRotOnGround) {
                              Block pivotTurnThrottle = MCH_Lib.getBlockY(this, 3, -2, false);
                              if (!W_Block.isEqual(pivotTurnThrottle, W_Block.getWater()) && !W_Block.isEqual(pivotTurnThrottle, Blocks.air)) {
                                 rotonground = 0.0F;
                              }
                           }
                        }

                        float flag = !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 + 0.05D ? -1.0F : 1.0F;
                        if(super.moveLeft && !super.moveRight) {
                           this.setRotYaw(this.getRotYaw() + 0.6F * rotonground * partialTicks * flag * sf);
                        }

                        if(super.moveRight && !super.moveLeft) {
                           this.setRotYaw(this.getRotYaw() - 0.6F * rotonground * partialTicks * flag * sf);
                        }
                     }
                  }
               }
            }
         } else if(super.cs_tankAutoThrottleDown && this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-0.005D * (double)throttleUpDown);
            if(this.getCurrentThrottle() <= 0.0D) {
               this.setCurrentThrottle(0.0D);
            }
         }
      }

   }

   protected void onUpdate_Particle2() {
      if(super.worldObj.isRemote) {
         if((double)this.getHP() < (double)this.getMaxHP() * 0.5D) {
            if(this.getTankInfo() != null) {
               int bbNum = this.getTankInfo().extraBoundingBox.size();
               if(bbNum < 0) {
                  bbNum = 0;
               }

               if(super.isFirstDamageSmoke || super.prevDamageSmokePos.length != bbNum + 1) {
                  super.prevDamageSmokePos = new Vec3[bbNum + 1];
               }

               float yaw = this.getRotYaw();
               float pitch = this.getRotPitch();
               float roll = this.getRotRoll();

               int px;
               double py;
               double pz;
               for(int b = 0; b < bbNum; ++b) {
                  if((double)this.getHP() >= (double)this.getMaxHP() * 0.2D && this.getMaxHP() > 0) {
                     px = (int)(((double)this.getHP() / (double)this.getMaxHP() - 0.2D) / 0.3D * 15.0D);
                     if(px > 0 && super.rand.nextInt(px) > 0) {
                        continue;
                     }
                  }

                  MCH_BoundingBox var15 = (MCH_BoundingBox)this.getTankInfo().extraBoundingBox.get(b);
                  Vec3 pos = this.getTransformedPosition(var15.offsetX, var15.offsetY, var15.offsetZ);
                  py = pos.xCoord;
                  pz = pos.yCoord;
                  double pos1 = pos.zCoord;
                  this.onUpdate_Particle2SpawnSmoke(b, py, pz, pos1, 1.0F);
               }

               boolean var14 = true;
               if((double)this.getHP() >= (double)this.getMaxHP() * 0.2D && this.getMaxHP() > 0) {
                  px = (int)(((double)this.getHP() / (double)this.getMaxHP() - 0.2D) / 0.3D * 15.0D);
                  if(px > 0 && super.rand.nextInt(px) > 0) {
                     var14 = false;
                  }
               }

               if(var14) {
                  double var16 = super.posX;
                  py = super.posY;
                  pz = super.posZ;
                  if(this.getSeatInfo(0) != null && this.getSeatInfo(0).pos != null) {
                     Vec3 var17 = MCH_Lib.RotVec3(0.0D, this.getSeatInfo(0).pos.yCoord, -2.0D, -yaw, -pitch, -roll);
                     var16 += var17.xCoord;
                     py += var17.yCoord;
                     pz += var17.zCoord;
                  }

                  this.onUpdate_Particle2SpawnSmoke(bbNum, var16, py, pz, bbNum == 0?2.0F:1.0F);
               }

               super.isFirstDamageSmoke = false;
            }
         }





      }
   }

   public void onUpdate_Particle2SpawnSmoke(int ri, double x, double y, double z, float size) {
      if(super.isFirstDamageSmoke || super.prevDamageSmokePos[ri] == null) {
         super.prevDamageSmokePos[ri] = Vec3.createVectorHelper(x, y, z);
      }

      Vec3 prev = super.prevDamageSmokePos[ri];
      double var10000 = x - prev.xCoord;
      var10000 = y - prev.yCoord;
      var10000 = z - prev.zCoord;
      byte num = 1;

      for(int i = 0; i < num; ++i) {
         float c = 0.2F + super.rand.nextFloat() * 0.3F;
         MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", x, y, z);
         prm.motionX = (double)size * (super.rand.nextDouble() - 0.5D) * 0.3D;
         prm.motionY = (double)size * super.rand.nextDouble() * 0.1D;
         prm.motionZ = (double)size * (super.rand.nextDouble() - 0.5D) * 0.3D;
         prm.size = size * ((float)super.rand.nextInt(5) + 5.0F) * 1.0F;
         prm.setColor(0.7F + super.rand.nextFloat() * 0.1F, c, c, c);
         MCH_ParticlesUtil.spawnParticle(prm);
      }

      super.prevDamageSmokePos[ri].xCoord = x;
      super.prevDamageSmokePos[ri].yCoord = y;
      super.prevDamageSmokePos[ri].zCoord = z;
   }

   public void onUpdate_Particle2SpawnSmode(int ri, double x, double y, double z, float size) {
      if(super.isFirstDamageSmoke) {
         super.prevDamageSmokePos[ri] = Vec3.createVectorHelper(x, y, z);
      }

      Vec3 prev = super.prevDamageSmokePos[ri];
      double dx = x - prev.xCoord;
      double dy = y - prev.yCoord;
      double dz = z - prev.zCoord;
      int num = (int)((double)MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) / 0.3D) + 1;

      for(int i = 0; i < num; ++i) {
         float c = 0.2F + super.rand.nextFloat() * 0.3F;
         MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", x, y, z);
         prm.motionX = (double)size * (super.rand.nextDouble() - 0.5D) * 0.3D;
         prm.motionY = (double)size * super.rand.nextDouble() * 0.1D;
         prm.motionZ = (double)size * (super.rand.nextDouble() - 0.5D) * 0.3D;
         prm.size = size * ((float)super.rand.nextInt(5) + 5.0F) * 1.0F;
         prm.setColor(0.7F + super.rand.nextFloat() * 0.1F, c, c, c);
         MCH_ParticlesUtil.spawnParticle(prm);
      }

      super.prevDamageSmokePos[ri].xCoord = x;
      super.prevDamageSmokePos[ri].yCoord = y;
      super.prevDamageSmokePos[ri].zCoord = z;
   }

   public void onUpdate_ParticleLandingGear() {
      this.WheelMng.particleLandingGear();
   }

   private void onUpdate_ParticleSplash() {
      if(this.getAcInfo() != null) {
         if(super.worldObj.isRemote) {
            double mx = super.posX - super.prevPosX;
            double mz = super.posZ - super.prevPosZ;
            double dist = mx * mx + mz * mz;
            if(dist > 1.0D) {
               dist = 1.0D;
            }

            Iterator i$ = this.getAcInfo().particleSplashs.iterator();

            while(i$.hasNext()) {
               MCH_BaseVehicleInfo.ParticleSplash p = (MCH_BaseVehicleInfo.ParticleSplash)i$.next();

               for(int i = 0; i < p.num; ++i) {
                  if(dist > 0.03D + (double)super.rand.nextFloat() * 0.1D) {
                     this.setParticleSplash(p.pos, -mx * (double)p.acceleration, (double)p.motionY, -mz * (double)p.acceleration, p.gravity, (double)p.size * (0.5D + dist * 0.5D), p.age);
                  }
               }
            }

         }
      }
   }

   private void setParticleSplash(Vec3 pos, double mx, double my, double mz, float gravity, double size, int age) {
      Vec3 v = this.getTransformedPosition(pos);
      v = v.addVector(super.rand.nextDouble() - 0.5D, (super.rand.nextDouble() - 0.5D) * 0.5D, super.rand.nextDouble() - 0.5D);
      int x = (int)(v.xCoord + 0.5D);
      int y = (int)(v.yCoord + 0.0D);
      int z = (int)(v.zCoord + 0.5D);
      if(W_WorldFunc.isBlockWater(super.worldObj, x, y, z)) {
         float c = super.rand.nextFloat() * 0.3F + 0.7F;
         MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", v.xCoord, v.yCoord, v.zCoord);
         prm.motionX = mx + ((double)super.rand.nextFloat() - 0.5D) * 0.7D;
         prm.motionY = my;
         prm.motionZ = mz + ((double)super.rand.nextFloat() - 0.5D) * 0.7D;
         prm.size = (float)size * (super.rand.nextFloat() * 0.2F + 0.8F);
         prm.setColor(0.9F, c, c, c);
         prm.age = age + (int)((double)super.rand.nextFloat() * 0.5D * (double)age);
         prm.gravity = gravity;
         MCH_ParticlesUtil.spawnParticle(prm);
      }

   }

   public void destroyAircraft() {
      boolean wasDestroyed = this.isDestroyed();
      super.destroyAircraft();
      super.rotDestroyedPitch = 0.0F;
      super.rotDestroyedRoll = 0.0F;
      super.rotDestroyedYaw = 0.0F;
      if(!super.worldObj.isRemote && !wasDestroyed && this.isDestroyed()) {
         this.onTurretPopDestructionTransition();
      }
   }

   /** Detects every server-side alive-to-destroyed edge, including max-HP setters. */
   private void updateTurretPopDestructionTransition() {
      boolean destroyed = this.isDestroyed();
      if(destroyed && !this.turretPopDestructionObserved) this.onTurretPopDestructionTransition();
      else if(!destroyed) this.turretPopDestructionObserved = false;
   }

   private void onTurretPopDestructionTransition() {
      this.turretPopDestructionObserved = true;
      if(this.turretPopStarted || this.tankInfo == null || !this.tankInfo.enableTurretPop) return;
      // Starting is configuration-driven and server authoritative. Model-section
      // validation is client-only and may safely fall back to the intact wreck.
      this.startTurretPop();
   }

   /**
    * Resolves the canonical configured main cannon, never an arbitrary loaded part.
    */
   public MCH_BaseVehicleInfo.PartWeapon getTurretPopRoot() {
      if(this.tankInfo == null || !this.tankInfo.enableTurretPop) return null;
      for(Object object : this.tankInfo.partWeapon) {
         MCH_BaseVehicleInfo.PartWeapon part = (MCH_BaseVehicleInfo.PartWeapon)object;
         if("weapon0".equalsIgnoreCase(part.modelName)) return part;
      }
      return null;
   }

   private void startTurretPop() {
      return;
      //aye bro none of that shit worked
      //Vec3 p = this.getTransformedPosition(this.tankInfo.turretPosition);
      //this.turretPopStarted = true; this.turretPopLanded = false; this.turretPopAge = 0;
      //this.turretPopX = this.prevTurretPopX = p.xCoord; this.turretPopY = this.prevTurretPopY = p.yCoord; this.turretPopZ = this.prevTurretPopZ = p.zCoord;
      //double a = super.rand.nextDouble() * Math.PI * 2.0D;
      //double speed = 0.18D + super.rand.nextDouble() * 0.16D;
      //this.turretPopMotionX = Math.cos(a) * speed; this.turretPopMotionY = 1.05D + super.rand.nextDouble() * 0.35D; this.turretPopMotionZ = Math.sin(a) * speed;
      //this.turretPopYaw = this.prevTurretPopYaw = this.getRotYaw(); this.turretPopPitch = this.prevTurretPopPitch = 0.0F; this.turretPopRoll = this.prevTurretPopRoll = 0.0F;
      //this.turretPopFrozenYaw = MathHelper.wrapAngleTo180_float(this.getLastRiderYaw() - this.getRotYaw());
      //MCH_BaseVehicleInfo.PartWeapon root = this.getTurretPopRoot();
      //MCH_WeaponSet weapon = root != null ? this.getWeaponByName(root.name[0]) : null;
      //this.turretPopFrozenPitch = weapon != null ? weapon.rotationPitch : this.getLastRiderPitch();
      //this.turretPopAngularYaw = 8.0F + super.rand.nextFloat() * 12.0F; this.turretPopAngularPitch = (super.rand.nextFloat() - 0.5F) * 24.0F; this.turretPopAngularRoll = (super.rand.nextFloat() - 0.5F) * 30.0F;
      //MCH_PacketTurretPop.send(this);
   }

   private void updateTurretPop() {
      if(!this.turretPopStarted || this.turretPopLanded) return;
      this.prevTurretPopX = this.turretPopX; this.prevTurretPopY = this.turretPopY; this.prevTurretPopZ = this.turretPopZ;
      this.prevTurretPopYaw = this.turretPopYaw; this.prevTurretPopPitch = this.turretPopPitch; this.prevTurretPopRoll = this.turretPopRoll;
      this.turretPopMotionY = Math.max(-1.5D, this.turretPopMotionY - 0.055D);
      this.turretPopX += this.turretPopMotionX; this.turretPopY += this.turretPopMotionY; this.turretPopZ += this.turretPopMotionZ;
      this.turretPopYaw += this.turretPopAngularYaw; this.turretPopPitch += this.turretPopAngularPitch; this.turretPopRoll += this.turretPopAngularRoll;
      ++this.turretPopAge;
      int bx = MathHelper.floor_double(this.turretPopX), by = MathHelper.floor_double(this.turretPopY), bz = MathHelper.floor_double(this.turretPopZ);
      double groundTop = Double.NEGATIVE_INFINITY;
      if(!super.worldObj.blockExists(bx, 0, bz)) {
         this.turretPopX = this.prevTurretPopX; this.turretPopY = this.prevTurretPopY; this.turretPopZ = this.prevTurretPopZ;
         this.turretPopMotionY = 0.0D;
         return;
      }
      if(this.turretPopMotionY <= 0.0D) for(int testY = MathHelper.floor_double(this.prevTurretPopY); testY >= by - 1; --testY) {
         Block block = super.worldObj.getBlock(bx, testY, bz);
         AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(super.worldObj, bx, testY, bz);
         if(box != null && block.getMaterial().blocksMovement() && box.maxY <= this.prevTurretPopY && box.maxY >= this.turretPopY - 0.35D) { groundTop = box.maxY; break; }
      }
      boolean timedOut = this.turretPopAge >= 600;
      if(groundTop != Double.NEGATIVE_INFINITY || timedOut) {
         // Models expose no group bounds, so 0.35 is a conservative pivot-to-bottom clearance.
         this.turretPopY = (timedOut ? super.worldObj.getHeightValue(bx, bz) : groundTop) + 0.35D;
         this.turretPopMotionY = 0.0D; this.turretPopMotionX *= 0.15D; this.turretPopMotionZ *= 0.15D;
         this.turretPopAngularYaw *= 0.08F; this.turretPopAngularPitch *= 0.08F; this.turretPopAngularRoll *= 0.08F; this.turretPopLanded = true;
      }
      if((this.turretPopAge % 3) == 0 || this.turretPopLanded) MCH_PacketTurretPop.send(this);
   }

   public void applyTurretPopState(MCH_PacketTurretPop p) {
      this.prevTurretPopX = this.turretPopStarted?this.turretPopX:p.x; this.prevTurretPopY = this.turretPopStarted?this.turretPopY:p.y; this.prevTurretPopZ = this.turretPopStarted?this.turretPopZ:p.z;
      this.prevTurretPopYaw = this.turretPopStarted?this.turretPopYaw:p.yaw; this.prevTurretPopPitch = this.turretPopStarted?this.turretPopPitch:p.pitch; this.prevTurretPopRoll = this.turretPopStarted?this.turretPopRoll:p.roll;
      this.turretPopStarted = true; this.turretPopLanded = p.landed; this.turretPopX=p.x; this.turretPopY=p.y; this.turretPopZ=p.z; this.turretPopYaw=p.yaw; this.turretPopPitch=p.pitch; this.turretPopRoll=p.roll; this.turretPopAge=p.age;
      this.turretPopMotionX=p.mx; this.turretPopMotionY=p.my; this.turretPopMotionZ=p.mz; this.turretPopAngularYaw=p.ay; this.turretPopAngularPitch=p.ap; this.turretPopAngularRoll=p.ar;
      this.turretPopFrozenYaw=p.frozenYaw; this.turretPopFrozenPitch=p.frozenPitch;
   }

   /** Predicts between three-tick server snapshots; each snapshot remains corrective. */
   @SideOnly(Side.CLIENT)
   private void predictTurretPopClient() {
      if(!this.turretPopStarted || this.turretPopLanded) return;
      this.prevTurretPopX = this.turretPopX; this.prevTurretPopY = this.turretPopY; this.prevTurretPopZ = this.turretPopZ;
      this.prevTurretPopYaw = this.turretPopYaw; this.prevTurretPopPitch = this.turretPopPitch; this.prevTurretPopRoll = this.turretPopRoll;
      this.turretPopMotionY = Math.max(-1.5D, this.turretPopMotionY - 0.055D);
      this.turretPopX += this.turretPopMotionX; this.turretPopY += this.turretPopMotionY; this.turretPopZ += this.turretPopMotionZ;
      this.turretPopYaw += this.turretPopAngularYaw; this.turretPopPitch += this.turretPopAngularPitch; this.turretPopRoll += this.turretPopAngularRoll;
      ++this.turretPopAge;
   }

   private void updateTurretPopSmoke() {
      if(!this.turretPopStarted || (this.turretPopLanded && this.turretPopAge > 620)) return;
      if(!this.turretPopLanded || this.turretPopAge % 5 == 0) {
         MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", this.turretPopX, this.turretPopY, this.turretPopZ);
         prm.motionX = (super.rand.nextDouble() - 0.5D) * 0.06D;
         prm.motionY = 0.08D + super.rand.nextDouble() * 0.04D;
         prm.motionZ = (super.rand.nextDouble() - 0.5D) * 0.06D;
         prm.size = 3.0F + super.rand.nextFloat(); prm.age = 35; prm.setColor(0.85F, 0.12F, 0.12F, 0.12F);
         MCH_ParticlesUtil.spawnParticle(prm);
      }
   }

   public void writeSpawnData(ByteBuf buffer) { super.writeSpawnData(buffer); MCH_PacketTurretPop.writeState(buffer, this); }
   public void readSpawnData(ByteBuf buffer) { super.readSpawnData(buffer); if(buffer.readableBytes() > 0) MCH_PacketTurretPop.readState(buffer, this); }



   protected void onUpdate_Client() {
//      if(this.getRiddenByEntity() != null && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
//         this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch;
//      }

      if(super.aircraftPosRotInc > 0) {
         this.applyServerPositionAndRotation();
      } else {
         this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
         if(!this.isDestroyed() && (super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0)) {
            super.motionX *= 0.95D;
            super.motionZ *= 0.95D;
            this.applyOnGroundPitch(0.95F);
         }

         if(this.isInWater()) {
            super.motionX *= 0.99D;
            super.motionZ *= 0.99D;
         }
      }

      this.updateWheels();
      this.onUpdate_Particle2();
      this.updateSound();
      if(super.worldObj.isRemote) {
         this.onUpdate_ParticleLandingGear();
         this.onUpdate_ParticleSplash();
         this.onUpdate_ParticleSandCloud(true);
      }

      this.updateCamera(super.posX, super.posY, super.posZ);
   }

   public void applyOnGroundPitch(float factor) {}


   private void onUpdate_Server() {

      final boolean DEBUG = false;
      //gpt was right, drag coeff is nerfing my grabbed MPH logic.

      // --------------------------------------------------
      // A: previous horizontal speed
      // --------------------------------------------------
      double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if (DEBUG) {
         System.out.println(String.format(
                 "DBG_A tick=%d prevMotion=%.5f motionX=%.5f motionZ=%.5f",
                 this.ticksExisted, prevMotion, super.motionX, super.motionZ
         ));
      }

      // --------------------------------------------------
      // ORIGINAL VERTICAL LOGIC (UNCHANGED)
      // --------------------------------------------------
      double dp = this.canFloatWater() ? this.getWaterDepth() : 0.0D;
      boolean levelOff = super.isGunnerMode;

      boolean wasOnGroundBeforeMove = super.onGround;
      double motionYBeforeGravity = super.motionY;

      if (dp == 0.0D) {
         if (!levelOff) {
            super.motionY += 0.04D + (!this.isInWater()
                    ? this.getAcInfo().gravity
                    : this.getAcInfo().gravityInWater);
            super.motionY += -0.047D * (1.0D - this.getCurrentThrottle());
         } else {
            super.motionY *= 0.8D;
         }
      } else {
         if (dp < 1.0D) {
            super.motionY -= 1.0E-4D;
            super.motionY += 0.007D * this.getCurrentThrottle();
         } else {
            if (super.motionY < 0.0D) super.motionY *= 0.5D;
            super.motionY += 0.007D;
         }
      }

      // --------------------------------------------------
      // THRUST (ACCEL ONLY, NOT SPEED)
      // --------------------------------------------------
      float throttle = (float)(this.getCurrentThrottle() / 10.0D);
      Vec3 v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);

      if (!levelOff) {
         super.motionY += v.yCoord * throttle / 8.0D;
      }

      boolean canMove = true;
      if (!this.getAcInfo().canMoveOnGround) {
         Block b = MCH_Lib.getBlockY(this, 3, -2, false);
         if (!W_Block.isEqual(b, W_Block.getWater()) && !W_Block.isEqual(b, Blocks.air)) {
            canMove = false;
         }
      }

      if (canMove) {
         if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
            super.motionX -= v.xCoord * super.throttleBack;
            super.motionZ -= v.zCoord * super.throttleBack;
         } else {
            super.motionX += v.xCoord * throttle;
            super.motionZ += v.zCoord * throttle;
         }
      }

      // --------------------------------------------------
      // B: after acceleration
      // --------------------------------------------------
      double afterAccel = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if (DEBUG) {
         System.out.println(String.format(
                 "DBG_B tick=%d afterAccel=%.5f motionX=%.5f motionZ=%.5f throttle=%.3f",
                 this.ticksExisted, afterAccel, super.motionX, super.motionZ, throttle
         ));
      }

      // --------------------------------------------------
      // HARD SPEED CLAMP (AUTHORITATIVE, NO CACHE)
      // --------------------------------------------------
      float maxSpeed = this.getTankInfo().speed;

      if (afterAccel > maxSpeed) {
         double scale = maxSpeed / afterAccel;
         super.motionX *= scale;
         super.motionZ *= scale;
      }

      // --------------------------------------------------
      // FRICTION / DRAG
      // --------------------------------------------------
      if (super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
         super.motionX *= this.getAcInfo().motionFactor;
         super.motionZ *= this.getAcInfo().motionFactor;
      } else {
         super.motionX *= 0.9995D;
         super.motionZ *= 0.9995D;
      }

      // --------------------------------------------------
      // MOVE
      // --------------------------------------------------
      this.updateWheels();
      double motionYBeforeMove = super.motionY;
      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      this.updateGroundVehicleFallDamage(wasOnGroundBeforeMove, motionYBeforeGravity, motionYBeforeMove);

      // --------------------------------------------------
      // C: after move
      // --------------------------------------------------
      double afterMove = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if (DEBUG) {
         System.out.println(String.format(
                 "DBG_C tick=%d afterMove=%.5f motionX=%.5f motionZ=%.5f speedLimit=%.5f",
                 this.ticksExisted, afterMove, super.motionX, super.motionZ, maxSpeed
         ));
      }

      super.motionY *= 0.95D;

      this.setRotation(this.getRotYaw(), this.getRotPitch());
      this.onUpdate_updateBlock();
      this.updateCollisionBox();

      this.handleDeadPilot();
   }




   private void collisionEntity(AxisAlignedBB bb) {
      //this is collision for this entity, not other entities
      if (bb != null) {
         // Calculate speed
         double speed = Math.sqrt(super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ);

         if (speed > 0.18D) { //18mph = lethal
            Entity rider = this.getRiddenByEntity();
            float damage = (float)(speed * 15.0D);

            // Get the aircraft entity the tank is riding on, if applicable
            final MCH_EntityBaseVehicle rideAc = super.ridingEntity instanceof MCH_EntityBaseVehicle
                    ? (MCH_EntityBaseVehicle) super.ridingEntity
                    : (super.ridingEntity instanceof MCH_EntitySeat
                    ? ((MCH_EntitySeat) super.ridingEntity).getParent()
                    : null);

            // Get a list of entities within the bounding box
            List<Entity> list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, bb.expand(0.3D, 0.3D, 0.3D), new IEntitySelector() {
               @Override
               public boolean isEntityApplicable(Entity e) {
                  // Exclude certain entity types from being affected by collision
                  if (e != rideAc && !(e instanceof EntityItem) && !(e instanceof EntityXPOrb) && !(e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff)
                          && !(e instanceof MCH_EntityBaseBullet) && !(e instanceof MCH_EntityChain)
                          && !(e instanceof MCH_EntitySeat)) {

                     // Special handling for tanks
                     if (e instanceof MCH_EntityTank) {
                        MCH_EntityTank tank = (MCH_EntityTank) e;
                        if (tank.getTankInfo() != null && tank.getTankInfo().weightType == 2) {
                           return MCH_Config.Collision_EntityTankDamage.prmBool;
                        }
                        //todo: fix up how this works as in collision because this is not fair to xradar perms/block protection
                     }

                     // Default collision entity damage
                     return MCH_Config.Collision_EntityDamage.prmBool;
                  }
                  return false;
               }
            });

            // Process each entity within the bounding box
            for (Entity e : list) {
               if (this.shouldCollisionDamage(e)) {
                  double dx = e.posX - super.posX;
                  double dz = e.posZ - super.posZ;
                  double dist = Math.sqrt(dx * dx + dz * dz);

                  if (dist > 5.0D) {
                     dist = 5.0D;
                  }

                  // Adjust damage based on distance
                  damage += (5.0D - dist);

                  // Determine the damage source
                  DamageSource ds = (rider instanceof EntityLivingBase)
                          ? DamageSource.causeMobDamage((EntityLivingBase) rider)
                          : DamageSource.generic;

                  // Apply damage and collision effects
                  MCH_Lib.applyEntityHurtResistantTimeConfig(e);
                  e.attackEntityFrom(ds, damage);

                  if (e instanceof MCH_EntityBaseVehicle) {
                     // Slight pushback for aircrafts
                     e.motionX += super.motionX * 0.05D;
                     e.motionZ += super.motionZ * 0.05D;
                  } else if (e instanceof EntityArrow) {
                     // Destroy arrows on impact
                     e.setDead();
                  } else {
                     // Apply strong pushback for other entities
                     e.motionX += super.motionX * 3.5D;
                     e.motionZ += super.motionZ * 3.5D;
                  }

                  // Damage self based on collision with large entities
                  if (this.getTankInfo().weightType != 2 && (e.width >= 1.0F || e.height >= 1.5D)) {
                     ds = (e instanceof EntityLivingBase)
                             ? DamageSource.causeMobDamage((EntityLivingBase) e)
                             : DamageSource.generic;

                     this.attackEntityFrom(ds, damage / 10.0F);
                  }

                  // Log the collision
                  MCH_Lib.DbgLog(super.worldObj, "MCH_EntityTank.collisionEntity damage=%.1f %s", damage, e.toString());
               }
            }
         }
      }
   }

   private boolean shouldCollisionDamage(Entity e) {
      if(this.getSeatIdByEntity(e) >= 0) {
         return false;
      } else if(super.noCollisionEntities.containsKey(e)) {
         return false;
      } else {
         if(e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox)e).parent != null) {
            MCH_EntityBaseVehicle ac = ((MCH_EntityHitBox)e).parent;
            if(super.noCollisionEntities.containsKey(ac)) {
               return false;
            }
         }

         return e.ridingEntity instanceof MCH_EntityBaseVehicle && super.noCollisionEntities.containsKey(e.ridingEntity)?false:!(e.ridingEntity instanceof MCH_EntitySeat) || ((MCH_EntitySeat)e.ridingEntity).getParent() == null || !super.noCollisionEntities.containsKey(((MCH_EntitySeat)e.ridingEntity).getParent());
      }
   }

   public void updateCollisionBox() {
      if(this.getAcInfo() != null) {
         this.WheelMng.updateBlock();
         MCH_BoundingBox[] arr$ = this.getCalculatedExtraBoundingBoxes();
         int len$ = arr$.length;

         MCH_Config var10000;
         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_BoundingBox bb = arr$[i$];
            if(super.rand.nextInt(3) == 0) {
               var10000 = MCH_MOD.config;
               if(MCH_Config.Collision_DestroyBlock.prmBool) {
                  Vec3 v = this.getTransformedPosition(bb.offsetX, bb.offsetY, bb.offsetZ);
                  this.destoryBlockRange(v, (double)bb.width, (double)bb.height);
               }

               this.collisionEntity(bb.boundingBox);
            }
         }

         var10000 = MCH_MOD.config;
         if(MCH_Config.Collision_DestroyBlock.prmBool) {
            this.destoryBlockRange(this.getTransformedPosition(0.0D, 0.0D, 0.0D), (double)super.width * 1.5D, (double)(super.height * 2.0F));
         }

         this.collisionEntity(this.getBoundingBox());
      }
   }

   public void destoryBlockRange(Vec3 v, double w, double h) {
      if(this.getAcInfo() != null) {
         MCH_Config var10000 = MCH_MOD.config;
         List destroyBlocks = MCH_Config.getBreakableBlockListFromType(this.getTankInfo().weightType);
         var10000 = MCH_MOD.config;
         List noDestroyBlocks = MCH_Config.getNoBreakableBlockListFromType(this.getTankInfo().weightType);
         var10000 = MCH_MOD.config;
         List destroyMaterials = MCH_Config.getBreakableMaterialListFromType(this.getTankInfo().weightType);
         int ws = (int)(w + 2.0D) / 2;
         int hs = (int)(h + 2.0D) / 2;

         for(int x = -ws; x <= ws; ++x) {
            int z = -ws;

            while(z <= ws) {
               int y = -hs;

               while(true) {
                  if(y <= hs + 1) {
                     label102: {
                        int bx = (int)(v.xCoord + (double)x - 0.5D);
                        int by = (int)(v.yCoord + (double)y - 1.0D);
                        int bz = (int)(v.zCoord + (double)z - 0.5D);
                        Block block = by >= 0 && by < 256?super.worldObj.getBlock(bx, by, bz):Blocks.air;
                        Material mat = block.getMaterial();
                        if(!Block.isEqualTo(block, Blocks.air)) {
                           Iterator i$ = noDestroyBlocks.iterator();

                           Block m;
                           while(i$.hasNext()) {
                              m = (Block)i$.next();
                              if(Block.isEqualTo(block, m)) {
                                 block = null;
                                 break;
                              }
                           }

                           if(block == null) {
                              break label102;
                           }

                           i$ = destroyBlocks.iterator();

                           while(i$.hasNext()) {
                              m = (Block)i$.next();
                              if(Block.isEqualTo(block, m)) {
                                 this.destroyBlock(bx, by, bz);
                                 mat = null;
                                 break;
                              }
                           }

                           if(mat == null) {
                              break label102;
                           }

                           i$ = destroyMaterials.iterator();

                           while(i$.hasNext()) {
                              Material var21 = (Material)i$.next();
                              if(block.getMaterial() == var21) {
                                 this.destroyBlock(bx, by, bz);
                                 break;
                              }
                           }
                        }

                        ++y;
                        continue;
                     }
                  }

                  ++z;
                  break;
               }
            }
         }

      }
   }

   public void destroyBlock(int bx, int by, int bz) {
      if(super.rand.nextInt(8) == 0) {
         W_WorldFunc.destroyBlock(super.worldObj, bx, by, bz, true);
      } else {
         super.worldObj.setBlockToAir(bx, by, bz);
      }

   }

   private void updateWheels() {
      this.WheelMng.move(super.motionX, super.motionY, super.motionZ);
   }

   public float getMaxSpeed() {
      return this.getTankInfo().speed + 0.0F;
   }

   //set angles is le turret (1.12.2)
   public void setAngles(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY, float x, float y, float partialTicks) {
      // Keep turret/camera limits tied to elapsed tick time instead of render FPS only for the opt-in mobility path.
      if(this.useNewMobilitySystem()) {
         partialTicks = MCH_FlightModel.getBoundedTickDelta(partialTicks);
      } else {
         if(partialTicks < 0.03F) {
            partialTicks = 0.4F;
         }

         if(partialTicks > 0.9F) {
            partialTicks = 0.6F;
         }

         this.lowPassPartialTicks.put(partialTicks);
         partialTicks = this.lowPassPartialTicks.getAvg();
      }
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
      MCH_Math.FMatrix m_add = MCH_Math.newMatrix();
      MCH_Math.MatTurnZ(m_add, roll / 180.0F * 3.1415927F);
      MCH_Math.MatTurnX(m_add, pitch / 180.0F * 3.1415927F);
      MCH_Math.MatTurnY(m_add, yaw / 180.0F * 3.1415927F);
      MCH_Math.MatTurnZ(m_add, (float)((double)(this.getRotRoll() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnX(m_add, (float)((double)(this.getRotPitch() / 180.0F) * 3.141592653589793D));
      MCH_Math.MatTurnY(m_add, (float)((double)(this.getRotYaw() / 180.0F) * 3.141592653589793D));
      MCH_Math.FVector3D v = MCH_Math.MatrixToEuler(m_add);
      v.x = MCH_Lib.RNG(v.x, -90.0F, 90.0F);
      v.z = MCH_Lib.RNG(v.z, -90.0F, 90.0F);
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
         v.x = MCH_Lib.RNG(this.getRotPitch(), -90.0F, 90.0F);
         v.z = MCH_Lib.RNG(this.getRotRoll(), -90.0F, 90.0F);
         this.setRotPitch(v.x);
         this.setRotRoll(v.z);
      }

      float RV = 180.0F;
      if(MathHelper.abs(this.getRotPitch()) > 90.0F) {
         MCH_Lib.DbgLog(true, "MCH_EntityBaseVehicle.setAngles Error:Pitch=%.1f", new Object[]{Float.valueOf(this.getRotPitch())});
         this.setRotPitch(0.0F);
      }

      if(this.getRotRoll() > 180.0F) {
         this.setRotRoll(this.getRotRoll() - 360.0F);
      }

      if(this.getRotRoll() < -180.0F) {
         this.setRotRoll(this.getRotRoll() + 360.0F);
      }

      super.prevRotationRoll = this.getRotRoll();
      super.prevRotationPitch = this.getRotPitch();
      if(this.getRidingEntity() == null) {
         super.prevRotationYaw = this.getRotYaw();
      }

      float deltaLimit = this.getAcInfo().cameraRotationSpeed * partialTicks;
      MCH_WeaponSet ws = this.getCurrentWeapon(player);
      deltaLimit *= ws != null && ws.getInfo() != null?ws.getInfo().cameraRotationSpeedPitch:1.0F;
      if(deltaX > deltaLimit) {
         deltaX = deltaLimit;
      }

      if(deltaX < -deltaLimit) {
         deltaX = -deltaLimit;
      }

      if(deltaY > deltaLimit) {
         deltaY = deltaLimit;
      }

      if(deltaY < -deltaLimit) {
         deltaY = -deltaLimit;
      }

      if(!this.isOverridePlayerYaw() && !fixRot) {
         player.setAngles(deltaX, 0.0F);
      } else {
         if(this.getRidingEntity() == null) {
            player.prevRotationYaw = this.getRotYaw() + fixYaw;
         } else {
            if(this.getRotYaw() - player.rotationYaw > 180.0F) {
               player.prevRotationYaw += 360.0F;
            }

            if(this.getRotYaw() - player.rotationYaw < -180.0F) {
               player.prevRotationYaw -= 360.0F;
            }
         }

         player.rotationYaw = this.getRotYaw() + fixYaw;
      }

      if(!this.isOverridePlayerPitch() && !fixRot) {
         player.setAngles(0.0F, deltaY);
      } else {
         player.prevRotationPitch = this.getRotPitch() + fixPitch;
         player.rotationPitch = this.getRotPitch() + fixPitch;
      }

      float playerYaw = MathHelper.wrapAngleTo180_float(this.getRotYaw() - player.rotationYaw);
      float playerPitch = this.getRotPitch() * MathHelper.cos((float)((double)playerYaw * 3.141592653589793D / 180.0D)) + -this.getRotRoll() * MathHelper.sin((float)((double)playerYaw * 3.141592653589793D / 180.0D));
      if(MCH_MOD.proxy.isFirstPerson()) {
         player.rotationPitch = MCH_Lib.RNG(player.rotationPitch, playerPitch + this.getAcInfo().minRotationPitch, playerPitch + this.getAcInfo().maxRotationPitch);
         player.rotationPitch = MCH_Lib.RNG(player.rotationPitch, -90.0F, 90.0F);
      }

      player.prevRotationPitch = player.rotationPitch;
      if(this.getRidingEntity() == null && ac_yaw != this.getRotYaw() || ac_pitch != this.getRotPitch() || ac_roll != this.getRotRoll()) {
         super.aircraftRotChanged = true;
      }

   }

   public float getSoundVolume() {
      return this.getAcInfo() != null && this.getAcInfo().throttleUpDown <= 0.0F?0.0F:this.soundVolume * 0.7F;
   }

   public void updateSound() {
      float target = (float)this.getCurrentThrottle();
      if(this.getRiddenByEntity() != null && (super.partCanopy == null || this.getCanopyRotation() < 1.0F)) {
         target += 0.1F;
      }

      if(!super.moveLeft && !super.moveRight && !super.throttleDown) {
         this.soundVolumeTarget *= 0.8F;
      } else {
         this.soundVolumeTarget += 0.1F;
         if(this.soundVolumeTarget > 0.75F) {
            this.soundVolumeTarget = 0.75F;
         }
      }

      if(target < this.soundVolumeTarget) {
         target = this.soundVolumeTarget;
      }

      if(this.soundVolume < target) {
         this.soundVolume += 0.02F;
         if(this.soundVolume >= target) {
            this.soundVolume = target;
         }
      } else if(this.soundVolume > target) {
         this.soundVolume -= 0.02F;
         if(this.soundVolume <= target) {
            this.soundVolume = target;
         }
      }

   }

   public float getSoundPitch() {
      float target1 = (float)(0.5D + this.getCurrentThrottle() * 0.5D);
      float target2 = (float)(0.5D + (double)this.soundVolumeTarget * 0.5D);
      return target1 > target2?target1:target2;
   }

   public String getDefaultSoundName() {
      return "prop";
   }

   public boolean hasBrake() {
      return true;
   }

   public void updateParts(int stat) {
      super.updateParts(stat);
      if(!this.isDestroyed()) {
         MCH_Parts[] parts = new MCH_Parts[0];
         MCH_Parts[] arr$ = parts;
         int len$ = parts.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Parts p = arr$[i$];
            if(p != null) {
               p.updateStatusClient(stat);
               p.update();
            }
         }

      }
   }

   public float getUnfoldLandingGearThrottle() {
      return 0.7F;
   }
}
