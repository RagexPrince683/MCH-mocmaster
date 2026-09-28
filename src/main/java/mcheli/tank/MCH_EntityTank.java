package mcheli.tank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
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
   private MCH_BoundingBox carPrimaryBox;
   private double carPrimaryX, carPrimaryY, carPrimaryZ;
   private float carPrimaryYaw, carPrimaryPitch, carPrimaryRoll;
   public float partialTicks;
   private boolean carPhysicsYawInitialized;
   private float carPhysicsYaw;
   private boolean carBodyPoseInitialized;
   private float carBodyYaw, carBodyPitch, carBodyRoll;
   private double carDiagnosticDriveForce;
   private MCH_WheelManager.CarContact carDiagnosticDriveContact;
   private MCH_CarGripDiagnostics.Snapshot carGripDiagnostic;
   public final MCH_CarDrivetrain carDrivetrain = new MCH_CarDrivetrain();
   private Entity carInputPilot;
   private boolean carHandbrakeInput;
   private float frontWheelRotation, prevFrontWheelRotation, rearWheelRotation, prevRearWheelRotation;
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

   @Override
   public MCH_BoundingBox getPrimaryBoundingBox() {
      if(this.tankInfo == null || !this.tankInfo.civilianCarGrip) return null;
      double centerY = (double)super.height * 0.5D - (double)super.yOffset + (double)super.ySize;
      boolean resized = this.carPrimaryBox == null || this.carPrimaryBox.width != super.width
            || this.carPrimaryBox.height != super.height || this.carPrimaryBox.offsetY != centerY;
      if(resized) {
         this.carPrimaryBox = new MCH_BoundingBox(0, centerY, 0, super.width, super.height, super.width, 1.0F);
      }
      float yaw = this.getRotYaw(), pitch = this.getRotPitch(), roll = this.getRotRoll();
      if(resized || this.carPrimaryX != super.posX || this.carPrimaryY != super.posY
            || this.carPrimaryZ != super.posZ || this.carPrimaryYaw != yaw
            || this.carPrimaryPitch != pitch || this.carPrimaryRoll != roll) {
         this.carPrimaryBox.updatePosition(super.posX, super.posY, super.posZ, yaw, pitch, roll);
         this.carPrimaryX = super.posX; this.carPrimaryY = super.posY; this.carPrimaryZ = super.posZ;
         this.carPrimaryYaw = yaw; this.carPrimaryPitch = pitch; this.carPrimaryRoll = roll;
      }
      return this.carPrimaryBox;
   }

   private void updateCarPrimaryBounds() {
      MCH_BoundingBox primary = this.getPrimaryBoundingBox();
      if(primary != null) {
         super.boundingBox.setBB(primary.boundingBox);
      } else if(this.carPrimaryBox != null) {
         // A definition reload can remove the opt-in while the vehicle is pitched.
         this.carPrimaryBox = null;
         super.setPosition(super.posX, super.posY, super.posZ);
      }
   }

   @Override
   public AxisAlignedBB getBoundingBox() {
      this.updateCarPrimaryBounds();
      return super.getBoundingBox();
   }

   @Override
   public void setPosition(double x, double y, double z) {
      super.setPosition(x, y, z);
      this.updateCarPrimaryBounds();
   }

   @Override
   public void setRotYaw(float yaw) {
      super.setRotYaw(yaw);
      this.updateCarPrimaryBounds();
   }

   @Override
   public void setRotPitch(float pitch) {
      super.setRotPitch(pitch);
      this.updateCarPrimaryBounds();
   }

   @Override
   public void setRotRoll(float roll) {
      super.setRotRoll(roll);
      this.updateCarPrimaryBounds();
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
         this.carBodyPoseInitialized = false;
         this.updateCarPrimaryBounds();
      }

   }

   public Item getItem() {
      return this.getTankInfo() != null?this.getTankInfo().item:null;
   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config configuration = MCH_MOD.config;
      return MCH_Config.MountMinecartTank.prmBool;
   }

   protected void entityInit() {
      super.entityInit();
      this.getDataWatcher().addObject(17, Integer.valueOf(2 << 22));
      this.getDataWatcher().addObject(18, Integer.valueOf(0));
      this.getDataWatcher().addObject(30, Integer.valueOf(0));
   }

   public boolean hasCarDrivetrain() {
      return this.tankInfo != null && this.tankInfo.civilianCarDrivetrain;
   }

   public void setCarControlInput(Entity pilot, boolean up, boolean down, boolean handbrake) {
      this.carInputPilot = pilot;
      this.throttleUp = up;
      this.throttleDown = down;
      this.carHandbrakeInput = handbrake;
   }

   private void clearCarControlInput() {
      this.carInputPilot = null;
      this.carHandbrakeInput = false;
      this.throttleUp = this.throttleDown = this.moveLeft = this.moveRight = false;
      this.throttleBack = 0;
      this.carDrivetrain.clearInputs();
      this.setCurrentThrottle(0);
      this.setThrottle(0);
      this.setBrake(false);
   }

   @Override
   public void unmountEntity() {
      super.unmountEntity();
      if(this.hasCarDrivetrain()) this.clearCarControlInput();
   }

   private void updateCarControl() {
      if(super.worldObj.isRemote) {
         this.carDrivetrain.readState(this.getDataWatcher().getWatchableObjectInt(17),
               this.getDataWatcher().getWatchableObjectInt(18), this.getDataWatcher().getWatchableObjectInt(30));
         this.setCurrentThrottle(this.carDrivetrain.throttle);
         this.throttleBack = this.carDrivetrain.gear < 0 && this.carDrivetrain.throttle > 0 ? 0.1F : 0;
         return;
      }
      Entity pilot = this.getRiddenByEntity();
      if(pilot != this.carInputPilot) {
         this.clearCarControlInput();
         this.carInputPilot = pilot;
      }
      boolean active = pilot != null && !pilot.isDead
            && !this.isDestroyed() && !this.isTrackDestroyed() && !this.isGunnerMode && !this.isRepelling()
            && this.isCanopyClose() && this.canUseFuel() && !this.isEngineWaterboarded()
            && this.getHP() * 100 / this.getMaxHP() >= this.getAcInfo().engineShutdownThreshold;
      if(!active) this.clearCarControlInput();
      double yaw = Math.toRadians(this.getRotYaw());
      double speed = -super.motionX * Math.sin(yaw) + super.motionZ * Math.cos(yaw);
      this.carDrivetrain.updateControls(this.tankInfo, this.throttleUp, this.throttleDown,
            this.carHandbrakeInput, active, speed, Math.hypot(super.motionX, super.motionZ));
      this.setCurrentThrottle(this.carDrivetrain.throttle);
      this.setThrottle(this.carDrivetrain.throttle);
      this.throttleBack = active && this.carDrivetrain.gear < 0 && this.throttleDown && !this.throttleUp
            ? 0.1F * this.carDrivetrain.throttle : 0;
      this.setBrake(active && (this.throttleDown || this.carHandbrakeInput));
   }

   private void syncCarDrivetrain() {
      this.getDataWatcher().updateObject(17, Integer.valueOf(this.carDrivetrain.engineState()));
      this.getDataWatcher().updateObject(18, Integer.valueOf(this.carDrivetrain.axleState(true)));
      this.getDataWatcher().updateObject(30, Integer.valueOf(this.carDrivetrain.axleState(false)));
   }

   public float getCarWheelRotation(double z, float tickTime) {
      return this.WheelMng.isFrontAxle(z)
            ? this.prevFrontWheelRotation + (this.frontWheelRotation - this.prevFrontWheelRotation) * tickTime
            : this.prevRearWheelRotation + (this.rearWheelRotation - this.prevRearWheelRotation) * tickTime;
   }

   private void updateCarWheelAnimation() {
      this.prevFrontWheelRotation = this.frontWheelRotation;
      this.prevRearWheelRotation = this.rearWheelRotation;
      this.frontWheelRotation += this.carDrivetrain.frontWheelSpeed * this.tankInfo.partWheelRot;
      this.rearWheelRotation += this.carDrivetrain.rearWheelSpeed * this.tankInfo.partWheelRot;
      float frontWrap = (float)Math.floor(this.frontWheelRotation / 360.0F) * 360.0F;
      float rearWrap = (float)Math.floor(this.rearWheelRotation / 360.0F) * 360.0F;
      this.frontWheelRotation -= frontWrap; this.prevFrontWheelRotation -= frontWrap;
      this.rearWheelRotation -= rearWrap; this.prevRearWheelRotation -= rearWrap;
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
      double minX;
      double result;
      double result2;
      MCH_CarBodyMovement.Result civilianMovement = null;
      double civilianPoseY = 0;
      MCH_CarBodyMovement.Trace civilianTrace = null;
      float poseYaw = this.getRotYaw(), posePitch = this.getRotPitch(), poseRoll = this.getRotRoll();
      if(this.getTankInfo() != null && this.getTankInfo().civilianCarGrip) {
         final MCH_CarBodyMovement.Collisions collisions = this::getBodyComponentCollisions;
         if(MCH_CarGripDiagnostics.enabled(this.getTankInfo()) && !super.worldObj.isRemote) {
            civilianTrace = new MCH_CarBodyMovement.Trace();
            civilianTrace.targetPitch = this.WheelMng.targetPitch;
            civilianTrace.targetRoll = this.WheelMng.targetRoll;
         }
         if(!super.worldObj.isRemote) civilianPoseY = this.resolveCivilianPose(collisions);
         // Keep the whole update within the existing two-degree pitch/roll limit,
         // including a pose change that the raised step can clear later.
         final float stepPitch = this.carBodyPitch
               + MathHelper.clamp_float(this.WheelMng.targetPitch - this.carBodyPitch, -2, 2);
         final float stepRoll = this.carBodyRoll
               + MathHelper.clamp_float(this.WheelMng.targetRoll - this.carBodyRoll, -2, 2);
         MCH_CarBodyMovement.StepPose stepPose = stepPitch == this.getRotPitch() && stepRoll == this.getRotRoll()
               ? null : lift -> this.civilianStepPose(collisions, lift, stepPitch, stepRoll);
         List<MCH_CarCollisionBox> body = this.carBodyAt(this.getRotYaw(), this.getRotPitch(), this.getRotRoll(), 0, 0);
         civilianMovement = MCH_CarBodyMovement.resolveBody(body, collisions, mx, my, mz,
               super.ySize < 0.05F ? Math.max(0, super.stepHeight - civilianPoseY) : 0,
               this.hasCurrentCivilianWheelSupport(collisions), civilianTrace, stepPose);
         if(civilianMovement.rotated) {
            this.setRotPitch(stepPitch); this.setRotRoll(stepRoll);
         }
         parX = civilianMovement.x;
         parY = civilianMovement.y;
         parZ = civilianMovement.z;
         super.boundingBox.offset(parX, parY, parZ);
      } else {
         AxisAlignedBB backUpAxisalignedBB = super.boundingBox.copy();
         List list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(parX, parY, parZ));
         parY = this.calculateYOffset(list, super.boundingBox, parY);
         boolean flag1 = super.onGround || my != parY && my < 0.0D;
         MCH_BoundingBox[] prevPX = super.extraBoundingBox;
         int iteratedValueCount = prevPX.length;

         for(int prevPZ = 0; prevPZ < iteratedValueCount; ++prevPZ) {
            MCH_BoundingBox ebb = prevPX[prevPZ];
            ebb.updatePosition(super.posX, super.posY, super.posZ, this.getRotYaw(), this.getRotPitch(), this.getRotRoll());
         }

         parX = this.calculateXOffset(list, super.boundingBox, parX);
         parZ = this.calculateZOffset(list, super.boundingBox, parZ);
         if(super.stepHeight > 0.0F && flag1 && super.ySize < 0.05F && (mx != parX || mz != parZ)) {
            result = parX;
            result2 = parY;
            minX = parZ;
            parY = (double)super.stepHeight;
            AxisAlignedBB minZ = super.boundingBox.copy();
            super.boundingBox.setBB(backUpAxisalignedBB);
            list = getCollidingBoundingBoxes(this, super.boundingBox.addCoord(mx, parY, mz));
            this.calculateYOffset(list, super.boundingBox, parY);
            parX = this.calculateXOffset(list, super.boundingBox, mx);
            parZ = this.calculateZOffset(list, super.boundingBox, mz);
            parY = this.calculateYOffset(list, super.boundingBox, (double)(-super.stepHeight));
            if(result * result + minX * minX >= parX * parX + parZ * parZ) {
               parX = result;
               parY = result2;
               parZ = minX;
               super.boundingBox.setBB(minZ);
            }
         }
      }

      result = super.posX;
      result2 = super.posZ;
      super.worldObj.theProfiler.endSection();
      super.worldObj.theProfiler.startSection("rest");
      minX = super.boundingBox.minX;
      double positionX = super.boundingBox.minZ;
      double maxX = super.boundingBox.maxX;
      double maxZ = super.boundingBox.maxZ;
      // An oriented primary's enclosing minimum/center is not the vehicle origin.
      super.posX = civilianMovement != null ? nowPosX + parX : (minX + maxX) / 2.0D;
      super.posY = civilianMovement != null ? nowPosY + civilianPoseY + parY
            : super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
      super.posZ = civilianMovement != null ? nowPosZ + parZ : (positionX + maxZ) / 2.0D;
      if(civilianMovement != null) {
         this.updateCarPrimaryBounds();
         this.acceptCivilianBodyPose();
      } else {
         this.carBodyPoseInitialized = false;
      }
      boolean blockedX = civilianMovement != null ? civilianMovement.blockedX : mx != parX;
      boolean blockedZ = civilianMovement != null ? civilianMovement.blockedZ : mz != parZ;
      super.isCollidedHorizontally = blockedX || blockedZ;
      super.isCollidedVertically = civilianMovement != null ? MCH_CarBodyMovement.changed(my, parY + civilianPoseY) : my != parY;
      super.onGround = civilianMovement != null ? civilianMovement.grounded : my != parY && my < 0.0D;
      super.isCollided = super.isCollidedHorizontally || super.isCollidedVertically;
      this.updateFallState(parY + civilianPoseY, super.onGround);
      if(blockedX) {
         super.motionX = 0.0D;
      }

      if(super.isCollidedVertically) {
         super.motionY = 0.0D;
      }

      if(blockedZ) {
         super.motionZ = 0.0D;
      }

      if(civilianTrace != null) {
         MCH_CarGripDiagnostics.recordMovement(this, civilianTrace, civilianMovement, carDiagnosticDriveContact,
               carDiagnosticDriveForce, nowPosX, nowPosY, nowPosZ, mx, my, mz, civilianPoseY, poseYaw, posePitch, poseRoll);
      }

      try {
         this.doBlockCollisions();
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity tile collision");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
         this.addEntityCrashInfo(crashreportcategory);
      }

      super.worldObj.theProfiler.endSection();
   }

   /** Build candidates without changing collider history or the vehicle origin. */
   private List<MCH_CarCollisionBox> carBodyAt(float yaw, float pitch, float roll, double lift,
         double pitchRollArc, double yawArc) {
      List<MCH_CarCollisionBox> body = new ArrayList<MCH_CarCollisionBox>(super.extraBoundingBox.length + 1);
      MCH_BoundingBox[] definitions = new MCH_BoundingBox[super.extraBoundingBox.length + 1];
      definitions[0] = this.getPrimaryBoundingBox();
      System.arraycopy(super.extraBoundingBox, 0, definitions, 1, super.extraBoundingBox.length);
      for(MCH_BoundingBox definition : definitions) {
         MCH_BoundingBox current = definition.copy();
         if(pitchRollArc != 0 || yawArc != 0) {
            double rx = Math.abs(definition.offsetX) + definition.width * 0.5D;
            double ry = Math.abs(definition.offsetY) + definition.height * 0.5D;
            double rz = Math.abs(definition.offsetZ) + definition.depth * 0.5D;
            double radius = Math.sqrt(rx * rx + ry * ry + rz * rz);
            double[] margins = new double[3];
            for(int axis = 0; axis < 3; ++axis) {
               Vec3 basis = MCH_Lib.RotVec3(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0,
                     -yaw, -pitch, -roll);
               // Any intermediate corner lies inside the midpoint box expanded by
               // its maximum angular displacement. Pure yaw has no vertical arc.
               margins[axis] = radius * (pitchRollArc + yawArc * Math.hypot(basis.xCoord, basis.zCoord));
               if(margins[axis] > 0) margins[axis] += 0.001D * radius;
            }
            current = new MCH_BoundingBox(definition.offsetX, definition.offsetY, definition.offsetZ,
                  Math.nextUp((float)(definition.width + 2 * margins[0])),
                  Math.nextUp((float)(definition.height + 2 * margins[1])),
                  Math.nextUp((float)(definition.depth + 2 * margins[2])), definition.damegeFactor);
         }
         current.updatePosition(super.posX, super.posY + lift, super.posZ, yaw, pitch, roll);
         body.add(new MCH_CarCollisionBox(current));
      }
      return body;
   }

   private List<MCH_CarCollisionBox> carBodyAt(float yaw, float pitch, float roll, double lift, double arc) {
      return this.carBodyAt(yaw, pitch, roll, lift, arc, 0);
   }

   /** Pose changes use a swept angular envelope and a bounded supported lift/settle. */
   private double resolveCivilianPose(MCH_CarBodyMovement.Collisions collisions) {
      float desiredYaw = this.getRotYaw();
      float desiredPitch = this.WheelMng.targetPitch, desiredRoll = this.WheelMng.targetRoll;
      if(!this.carBodyPoseInitialized) {
         this.carBodyYaw = desiredYaw; this.carBodyPitch = this.getRotPitch(); this.carBodyRoll = this.getRotRoll();
      }
      this.setRotYaw(this.carBodyYaw); this.setRotPitch(this.carBodyPitch); this.setRotRoll(this.carBodyRoll);
      List<MCH_CarCollisionBox> initial = this.carBodyAt(this.carBodyYaw, this.carBodyPitch, this.carBodyRoll, 0, 0);
      if(!MCH_CarBodyMovement.clear(initial, collisions)) {
         double up = MCH_CarBodyMovement.recoverUp(initial, collisions, 0.1D, super.stepHeight);
         super.posY += up;
         this.updateCarPrimaryBounds();
         return up;
      }

      float yawDelta = MathHelper.wrapAngleTo180_float(desiredYaw - this.carBodyYaw);
      float pitchDelta = MathHelper.clamp_float(desiredPitch - this.carBodyPitch, -2, 2);
      float rollDelta = MathHelper.clamp_float(desiredRoll - this.carBodyRoll, -2, 2);
      if(yawDelta == 0 && pitchDelta == 0 && rollDelta == 0) return 0;
      boolean supported = MCH_CarBodyMovement.supportedBody(initial, collisions)
            || this.hasCurrentCivilianWheelSupport(collisions);
      double rise = supported ? MCH_CarBodyMovement.move(initial, collisions, 1, Math.min(0.1D, super.stepHeight)) : 0;
      for(float fraction = 1; fraction >= 0.015625F; fraction *= 0.5F) {
         float yaw = this.carBodyYaw + yawDelta * fraction;
         float pitch = this.carBodyPitch + pitchDelta * fraction;
         float roll = this.carBodyRoll + rollDelta * fraction;
         double pitchRollArc = Math.toRadians((Math.abs(pitchDelta) + Math.abs(rollDelta)) * fraction) * 0.5D;
         double yawArc = Math.toRadians(Math.abs(yawDelta) * fraction) * 0.5D;
         float midYaw = this.carBodyYaw + yawDelta * fraction * 0.5F;
         float midPitch = this.carBodyPitch + pitchDelta * fraction * 0.5F;
         float midRoll = this.carBodyRoll + rollDelta * fraction * 0.5F;
         double lift = 0;
         if(!MCH_CarBodyMovement.clear(this.carBodyAt(midYaw, midPitch, midRoll, 0, pitchRollArc, yawArc), collisions)) {
            if(rise <= 0 || !MCH_CarBodyMovement.clear(
                  this.carBodyAt(midYaw, midPitch, midRoll, rise, pitchRollArc, yawArc), collisions)) continue;
            lift = rise;
         }
         List<MCH_CarCollisionBox> finalBody = this.carBodyAt(yaw, pitch, roll, lift, 0);
         if(!MCH_CarBodyMovement.clear(finalBody, collisions)) continue;
         double down = MCH_CarBodyMovement.move(finalBody, collisions, 1, -lift);
         this.setRotYaw(yaw); this.setRotPitch(pitch); this.setRotRoll(roll);
         super.posY += lift + down;
         this.updateCarPrimaryBounds();
         return lift + down;
      }
      return 0;
   }

   /** Raised rotation shares the step's lift, clearance and landing budget. */
   private List<MCH_CarCollisionBox> civilianStepPose(MCH_CarBodyMovement.Collisions collisions,
         double lift, float pitch, float roll) {
      float currentPitch = this.getRotPitch(), currentRoll = this.getRotRoll();
      double arc = Math.toRadians(Math.abs(pitch - currentPitch) + Math.abs(roll - currentRoll)) * 0.5D;
      List<MCH_CarCollisionBox> envelope = this.carBodyAt(this.getRotYaw(),
            (currentPitch + pitch) * 0.5F, (currentRoll + roll) * 0.5F, lift, arc);
      if(!MCH_CarBodyMovement.clear(envelope, collisions)) return null;
      List<MCH_CarCollisionBox> body = this.carBodyAt(this.getRotYaw(), pitch, roll, lift, 0);
      return MCH_CarBodyMovement.clear(body, collisions) ? body : null;
   }

   private void acceptCivilianBodyPose() {
      this.carBodyYaw = this.getRotYaw(); this.carBodyPitch = this.getRotPitch(); this.carBodyRoll = this.getRotRoll();
      this.carBodyPoseInitialized = true;
      this.carPhysicsYaw = this.carBodyYaw;
      this.WheelMng.settleCivilianWheels();
   }

   /** Read-only support at the present axle footprint, never at the pending destination. */
   private boolean hasCurrentCivilianWheelSupport(MCH_CarBodyMovement.Collisions collisions) {
      if(this.WheelMng == null || this.WheelMng.wheels == null) return false;
      double travel = this.getTankInfo().suspensionTravel;
      for(MCH_EntityWheel wheel : this.WheelMng.wheels) {
         if(wheel == null || wheel.isDead || wheel.pos == null || wheel.boundingBox == null) continue;
         Vec3 anchor = this.getTransformedPosition(wheel.pos);
         double anchorBottom = anchor.yCoord + wheel.getSuspensionAnchorOffset()
               + wheel.boundingBox.minY - wheel.posY;
         // Reject wheels left below the body during takeoff, even if their flags say grounded.
         if(wheel.boundingBox.minY < anchorBottom - travel - 1.0E-5D
               || wheel.boundingBox.minY > anchorBottom + 1.0E-5D) continue;
         AxisAlignedBB present = wheel.boundingBox.copy();
         present.offset(anchor.xCoord - (present.minX + present.maxX) * 0.5D, 0,
               anchor.zCoord - (present.minZ + present.maxZ) * 0.5D);
         if(MCH_CarBodyMovement.supported(Collections.singletonList(present), collisions)) return true;
      }
      return false;
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

   private float getCarSteeringDirection(double dx, double dz) {
      // Engine throttle also opens in reverse now; it cannot select steering direction.
      double yaw = Math.toRadians(this.getRotYaw());
      double signed = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
      if(Math.abs(signed) < 1.0E-6D) signed = -super.motionX * Math.sin(yaw) + super.motionZ * Math.cos(yaw);
      return Math.abs(signed) >= 1.0E-6D ? (signed < 0 ? -1.0F : 1.0F)
            : this.carDrivetrain.gear < 0 ? -1.0F : 1.0F;
   }

   public void onUpdateAngles(float partialTicks) {
      if(this.useNewMobilitySystem() || (this.getTankInfo() != null && this.getTankInfo().civilianCarGrip && this.getTankInfo().carLateralGrip > 0)) {
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
         if(this.getTankInfo() == null || !this.getTankInfo().civilianCarGrip) {
            this.setRotPitch(this.getRotPitch() + (this.WheelMng.targetPitch - this.getRotPitch()) * partialTicks);
            this.setRotRoll(this.getRotRoll() + (this.WheelMng.targetRoll - this.getRotRoll()) * partialTicks);
         }
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

               float flag = this.hasCarDrivetrain() ? this.getCarSteeringDirection(dx, dz)
                     : !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 + 0.05D?-1.0F:1.0F;
               if(super.moveLeft && !super.moveRight) {
                  this.steerGroundVehicle(-0.6F * rotonground * partialTicks * flag * sf, partialTicks);
               }

               if(super.moveRight && !super.moveLeft) {
                  this.steerGroundVehicle(0.6F * rotonground * partialTicks * flag * sf, partialTicks);
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
      if(!this.hasCarDrivetrain() && pivotTurnThrottle1 > 0.0F && this.getAcInfo().enableBack && super.throttleDown && this.getCurrentThrottle() <= 0.0D && super.throttleBack > 0.0F) {
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

         float flag = this.hasCarDrivetrain() ? this.getCarSteeringDirection(dx, dz)
               : !super.throttleUp && super.throttleDown && this.getCurrentThrottle() < (double)pivotTurnThrottle1 + 0.05D?-1.0F:1.0F;
         if(super.moveLeft && !super.moveRight) {
            this.steerGroundVehicle(-0.6F * partialTicks * flag * sf, partialTicks);
         }

         if(super.moveRight && !super.moveLeft) {
            this.steerGroundVehicle(0.6F * partialTicks * flag * sf, partialTicks);
         }
      }
   }

   private void steerGroundVehicle(float requested, float tickDelta) {
      MCH_TankInfo info = this.getTankInfo();
      if(info != null && info.civilianCarGrip && info.carLateralGrip > 0.0F) {
         double speed = Math.hypot(super.motionX, super.motionZ);
         double contact = this.WheelMng.getCarGroundContact(MCH_CarGripDiagnostics.enabled(info)).fraction();
         float rawRequested = requested;
         requested = MCH_CarTireGrip.steeringDelta(rawRequested, speed, info.carLateralGrip, contact,
                 info.carMinimumSteering, tickDelta);
         if(super.worldObj.isRemote && MCH_CarGripDiagnostics.enabled(info)) {
            MCH_CarGripDiagnostics.recordClient(info.name, this.getEntityId(), this.ticksExisted,
                    super.moveLeft, super.moveRight, rawRequested, requested, speed, contact, tickDelta);
         }
      }
      this.setRotYaw(this.getRotYaw() + requested);
   }

   protected void onUpdate_Control(float partialTicks) {

      if(this.hasCarDrivetrain()) {
         this.updateCarControl();
         return;
      }
      if(this.carInputPilot != null) this.clearCarControlInput();

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
         // S shares the brake-lamp status with Space. Once forward throttle is zero,
         // S requests reverse power; Space clears throttleDown in the input handler.
         // Preserve declared civilian controls, including cars without a reverse-speed opt-in.
         boolean legacyMilitaryReverse = this.getAcInfo().enableBack && super.throttleDown
                 && !super.throttleUp && this.getCurrentThrottle() <= 0.0D
                 && !this.getTankInfo().civilianCarGrip && !"C".equals(this.getAcInfo().category);
         if(!legacyMilitaryReverse && !MCH_CarReverseControl.isReverseInput(this.getTankInfo().civilianCarReverseSpeed,
                 super.throttleDown, this.getCurrentThrottle())) {
            super.throttleBack = (float)((double)super.throttleBack * 0.5D);
         }
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
            if(this.getTankInfo().civilianCarReverseSpeed > 0.0F) {
               // A bounded reverse demand must not delay the driver's forward input.
               super.throttleBack = 0.0F;
            }
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
                  super.throttleBack = (float)((double)super.throttleBack + 0.0025D * (double)throttleUpDown * getAcInfo().throttleDownFactor);
                  if(this.getTankInfo().civilianCarReverseSpeed > 0.0F) {
                     super.throttleBack = MCH_CarReverseControl.boundThrottle(super.throttleBack);
                  }
                  float pivotTurnThrottle1 = this.getAcInfo().pivotTurnThrottle;
                  // Civilian steering is handled once by onUpdateAngles and the server grip budget.
                  if (pivotTurnThrottle1 > 0 && !(this.getTankInfo().civilianCarGrip && this.getTankInfo().carLateralGrip > 0)) {
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
                           this.steerGroundVehicle(0.6F * rotonground * partialTicks * flag * sf, partialTicks);
                        }

                        if(super.moveRight && !super.moveLeft) {
                           this.steerGroundVehicle(-0.6F * rotonground * partialTicks * flag * sf, partialTicks);
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

                  MCH_BoundingBox result = (MCH_BoundingBox)this.getTankInfo().extraBoundingBox.get(b);
                  Vec3 pos = this.getTransformedPosition(result.offsetX, result.offsetY, result.offsetZ);
                  py = pos.xCoord;
                  pz = pos.yCoord;
                  double pos1 = pos.zCoord;
                  this.onUpdate_Particle2SpawnSmoke(b, py, pz, pos1, 1.0F);
               }

               boolean result2 = true;
               if((double)this.getHP() >= (double)this.getMaxHP() * 0.2D && this.getMaxHP() > 0) {
                  px = (int)(((double)this.getHP() / (double)this.getMaxHP() - 0.2D) / 0.3D * 15.0D);
                  if(px > 0 && super.rand.nextInt(px) > 0) {
                     result2 = false;
                  }
               }

               if(result2) {
                  double positionX = super.posX;
                  py = super.posY;
                  pz = super.posZ;
                  if(this.getSeatInfo(0) != null && this.getSeatInfo(0).pos != null) {
                     Vec3 position = MCH_Lib.RotVec3(0.0D, this.getSeatInfo(0).pos.yCoord, -2.0D, -yaw, -pitch, -roll);
                     positionX += position.xCoord;
                     py += position.yCoord;
                     pz += position.zCoord;
                  }

                  this.onUpdate_Particle2SpawnSmoke(bbNum, positionX, py, pz, bbNum == 0?2.0F:1.0F);
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
      double positionX = x - prev.xCoord;
      positionX = y - prev.yCoord;
      positionX = z - prev.zCoord;
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

            Iterator iteratedValueIndex = this.getAcInfo().particleSplashs.iterator();

            while(iteratedValueIndex.hasNext()) {
               MCH_BaseVehicleInfo.ParticleSplash p = (MCH_BaseVehicleInfo.ParticleSplash)iteratedValueIndex.next();

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

      MCH_TankInfo info = this.getTankInfo();
      boolean useCarBody = info != null && info.civilianCarGrip;
      if(useCarBody) {
         // The server advances the wheels before it advances the vehicle body. Keep car
         // prediction in the same order so both sides probe the same suspension state.
         this.updateWheels();
      }
      if(super.aircraftPosRotInc > 0) {
         if(useCarBody && !this.isDestroyed() && W_Lib.isClientPlayer(this.getRiddenByEntity())
               && this.getRidingEntity() == null) {
            // The common interpolation keeps a local pilot's angles. Civilian
            // pitch/roll instead follow the server's accepted collision pose.
            this.setRotPitch((float)(this.getRotPitch()
                  + (super.aircraftPitch - this.getRotPitch()) / super.aircraftPosRotInc));
            this.setRotRoll((float)(this.getRotRoll()
                  + MathHelper.wrapAngleTo180_double(this.getServerRoll() - this.getRotRoll()) / super.aircraftPosRotInc));
         }
         this.applyServerPositionAndRotation();
         if(useCarBody) this.acceptCivilianBodyPose();
      } else {
         // Use the same reverse clamp for client extrapolation. Server interpolation
         // above continues to follow authoritative positions without locally clipping them.
         this.applyCivilianCarReverseSpeedLimit(this.getAcInfo().enableBack && super.throttleBack > 0.0F);
         // Snapshot positions are interpolated and quantized, not accepted server
         // collision poses. Do not recover/step them or erase replicated velocity.
         this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
         if(useCarBody) this.acceptCivilianBodyPose();
         if(this.hasCarDrivetrain()) {
            double speed = Math.hypot(super.motionX, super.motionZ);
            if(speed > 0) {
               double drag = this.carDrivetrain.drag(info, speed,
                     this.carDrivetrain.frontContact || this.carDrivetrain.rearContact);
               double scale = Math.max(0, 1.0D - drag / speed);
               super.motionX *= scale;
               super.motionZ *= scale;
            }
         } else if(!this.isDestroyed() && (super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0)) {
            super.motionX *= 0.95D;
            super.motionZ *= 0.95D;
            this.applyOnGroundPitch(0.95F);
         }

         if(this.isInWater()) {
            super.motionX *= 0.99D;
            super.motionZ *= 0.99D;
         }
      }

      if(!useCarBody) {
         this.updateWheels();
      }
      this.onUpdate_Particle2();
      if(this.hasCarDrivetrain()) {
         this.updateCarWheelAnimation();
         this.WheelMng.particleCarWheelSlip(this.carDrivetrain);
      }
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
      this.carDiagnosticDriveForce = Double.NaN;
      this.carDiagnosticDriveContact = null;
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
      double driveScale = 1.0D;
      if(!this.hasCarDrivetrain() && this.tankInfo.driveType != null) {
         boolean reverse = this.getAcInfo().enableBack && super.throttleBack > 0.0F;
         double demand = reverse ? -super.throttleBack : throttle;
         double requested = demand * Math.hypot(v.xCoord, v.zCoord);
         if(requested != 0.0D) {
            // Sample current collision support before engine force, not the paired
            // suspension flags or body onGround. The later wheel update stays in place.
            MCH_WheelManager.CarContact contact = this.WheelMng.getCarGroundContact(false);
            double yaw = Math.toRadians(this.getRotYaw());
            double sideways = super.motionX * Math.cos(yaw) + super.motionZ * Math.sin(yaw);
            driveScale = MCH_CarTireGrip.driveAcceleration(requested, sideways, this.tankInfo, contact) / requested;
         }
      }

      if (!levelOff && !this.hasCarDrivetrain()) {
         super.motionY += v.yCoord * throttle / 8.0D * driveScale;
      }

      boolean canMove = true;
      if (!this.getAcInfo().canMoveOnGround) {
         Block b = MCH_Lib.getBlockY(this, 3, -2, false);
         if (!W_Block.isEqual(b, W_Block.getWater()) && !W_Block.isEqual(b, Blocks.air)) {
            canMove = false;
         }
      }

      if(this.hasCarDrivetrain()) {
         double yaw = Math.toRadians(this.getRotYaw());
         double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
         double speed = super.motionX * forwardX + super.motionZ * forwardZ;
         double sideways = super.motionX * Math.cos(yaw) + super.motionZ * Math.sin(yaw);
         MCH_WheelManager.CarContact driveContact = this.WheelMng.getCarGroundContact(false);
         double force = this.carDrivetrain.acceleration(this.tankInfo, driveContact, speed, sideways, canMove);
         if(MCH_CarGripDiagnostics.enabled(this.tankInfo)) {
            this.carDiagnosticDriveForce = force;
            this.carDiagnosticDriveContact = driveContact;
         }
         super.motionX += forwardX * force;
         super.motionZ += forwardZ * force;
         this.syncCarDrivetrain();
      } else if (canMove) {
         if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
            super.motionX -= v.xCoord * super.throttleBack * driveScale;
            super.motionZ -= v.zCoord * super.throttleBack * driveScale;
         } else {
            super.motionX += v.xCoord * throttle * driveScale;
            super.motionZ += v.zCoord * throttle * driveScale;
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

      if (!this.hasCarDrivetrain() && afterAccel > maxSpeed) {
         double scale = maxSpeed / afterAccel;
         super.motionX *= scale;
         super.motionZ *= scale;
      }

      // --------------------------------------------------
      // FRICTION / DRAG
      // --------------------------------------------------
      if(this.hasCarDrivetrain()) {
         if(afterAccel > 0) {
            double drag = this.carDrivetrain.drag(this.tankInfo, afterAccel,
                  this.carDrivetrain.frontContact || this.carDrivetrain.rearContact);
            double scale = Math.max(0, 1.0D - drag / afterAccel);
            super.motionX *= scale;
            super.motionZ *= scale;
         }
      } else if (super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
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
      this.applyCarLateralGrip();
      // For the force/drag car model, Speed is only the final safety boundary.
      if(this.hasCarDrivetrain()) {
         double speed = Math.hypot(super.motionX, super.motionZ);
         if(speed > maxSpeed) {
            double scale = maxSpeed / speed;
            super.motionX *= scale;
            super.motionZ *= scale;
         }
      }
      // Clamp after drag and the server's final steering/grip heading, before movement.
      this.applyCivilianCarReverseSpeedLimit(canMove && this.getAcInfo().enableBack && super.throttleBack > 0.0F);
      if(this.hasCarDrivetrain() && (this.carDrivetrain.serviceBrake || this.carDrivetrain.handbrake)
            && (this.carDrivetrain.frontContact || this.carDrivetrain.rearContact)
            && Math.hypot(super.motionX, super.motionZ) < 1.0E-5D) {
         super.motionX = super.motionZ = 0;
      }
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

   private void applyCivilianCarReverseSpeedLimit(boolean powered) {
      MCH_TankInfo info = this.getTankInfo();
      if(info == null || info.civilianCarReverseSpeed <= 0.0F) return;
      float limit = Math.min(info.speed, info.civilianCarReverseSpeed);
      double scale = MCH_CarReverseControl.speedScale(super.motionX, super.motionZ, this.getRotYaw(), limit, powered);
      super.motionX *= scale;
      super.motionZ *= scale;
   }

   void applyCarLateralGrip() {
      MCH_TankInfo info = this.getTankInfo();
      if(super.worldObj.isRemote || info == null) return;
      boolean active = info.civilianCarGrip && info.carLateralGrip > 0.0F;
      this.carGripDiagnostic = null;
      if(!active && !MCH_CarGripDiagnostics.enabled(info)) {
         this.carPhysicsYawInitialized = false;
         return;
      }
      MCH_WheelManager.CarContact contact = this.WheelMng.getCarGroundContact(MCH_CarGripDiagnostics.enabled(info));
      float requestedYaw = 0.0F, appliedYaw = 0.0F;
      if(active) {
         if(!this.carPhysicsYawInitialized) {
            this.carPhysicsYaw = this.getRotYaw();
            this.carPhysicsYawInitialized = true;
         }
         // Rotation packets may arrive several times per tick; share one server physics budget.
         requestedYaw = MathHelper.wrapAngleTo180_float(this.getRotYaw() - this.carPhysicsYaw);
         appliedYaw = MCH_CarTireGrip.steeringDelta(requestedYaw, Math.hypot(super.motionX, super.motionZ),
                 info.carLateralGrip, contact.fraction(), info.carMinimumSteering, 1.0F);
         this.setRotYaw(this.carPhysicsYaw + appliedYaw);
         this.carPhysicsYaw = this.getRotYaw();
      } else {
         this.carPhysicsYawInitialized = false;
      }
      double yaw = Math.toRadians(this.getRotYaw());
      double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
      // Orthogonal horizontal basis: forward=(-sin(yaw),cos(yaw)), side=(cos(yaw),sin(yaw)).
      double sideways = super.motionX * forwardZ - super.motionZ * forwardX;
      MCH_CarTireGrip.Result result = MCH_CarTireGrip.calculate(sideways, info.carLateralGrip,
              contact.fraction(), contact.response(info));
      String reason = !info.civilianCarGrip ? "not_opted_in" : info.carLateralGrip <= 0 ? "grip_disabled"
              : contact.total == 0 ? "no_wheels" : result.reason;
      double correction = active ? result.applied : 0.0D;
      if(correction != 0.0D) {
         super.motionX -= correction * forwardZ;
         super.motionZ += correction * forwardX;
      }
      if(MCH_CarGripDiagnostics.enabled(info)) {
         this.carGripDiagnostic = new MCH_CarGripDiagnostics.Snapshot(info.name, this.getEntityId(), this.ticksExisted,
                 contact, sideways, result.requested, correction, result.limit, reason, requestedYaw, appliedYaw);
         MCH_CarGripDiagnostics.record(this.carGripDiagnostic);
      }
   }

   public MCH_CarGripDiagnostics.Snapshot getCarGripDiagnostic() { return this.carGripDiagnostic; }




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
         MCH_BoundingBox[] iteratedValues = this.getCalculatedExtraBoundingBoxes();
         int iteratedValueCount = iteratedValues.length;

         MCH_Config configuration;
         for(int iteratedValueIndex = 0; iteratedValueIndex < iteratedValueCount; ++iteratedValueIndex) {
            MCH_BoundingBox bb = iteratedValues[iteratedValueIndex];
            if(super.rand.nextInt(3) == 0) {
               configuration = MCH_MOD.config;
               if(MCH_Config.Collision_DestroyBlock.prmBool) {
                  Vec3 v = this.getTransformedPosition(bb.offsetX, bb.offsetY, bb.offsetZ);
                  this.destoryBlockRange(v, (double)bb.width, (double)bb.height);
               }

               this.collisionEntity(bb.boundingBox);
            }
         }

         configuration = MCH_MOD.config;
         if(MCH_Config.Collision_DestroyBlock.prmBool) {
            this.destoryBlockRange(this.getTransformedPosition(0.0D, 0.0D, 0.0D), (double)super.width * 1.5D, (double)(super.height * 2.0F));
         }

         this.collisionEntity(this.getBoundingBox());
      }
   }

   public void destoryBlockRange(Vec3 v, double w, double h) {
      if(this.getAcInfo() != null) {
         MCH_Config configuration = MCH_MOD.config;
         List destroyBlocks = MCH_Config.getBreakableBlockListFromType(this.getTankInfo().weightType);
         configuration = MCH_MOD.config;
         List noDestroyBlocks = MCH_Config.getNoBreakableBlockListFromType(this.getTankInfo().weightType);
         configuration = MCH_MOD.config;
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
                           Iterator iteratedValueIndex = noDestroyBlocks.iterator();

                           Block m;
                           while(iteratedValueIndex.hasNext()) {
                              m = (Block)iteratedValueIndex.next();
                              if(Block.isEqualTo(block, m)) {
                                 block = null;
                                 break;
                              }
                           }

                           if(block == null) {
                              break label102;
                           }

                           iteratedValueIndex = destroyBlocks.iterator();

                           while(iteratedValueIndex.hasNext()) {
                              m = (Block)iteratedValueIndex.next();
                              if(Block.isEqualTo(block, m)) {
                                 this.destroyBlock(bx, by, bz);
                                 mat = null;
                                 break;
                              }
                           }

                           if(mat == null) {
                              break label102;
                           }

                           iteratedValueIndex = destroyMaterials.iterator();

                           while(iteratedValueIndex.hasNext()) {
                              Material material = (Material)iteratedValueIndex.next();
                              if(block.getMaterial() == material) {
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
      // Use elapsed tick time for opted-in mobility and civilian steering.
      if(this.useNewMobilitySystem() || (this.getTankInfo() != null && this.getTankInfo().civilianCarGrip && this.getTankInfo().carLateralGrip > 0)) {
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
      if(this.hasCarDrivetrain()) {
         target = this.carDrivetrain.running ? 0.15F + 0.85F * this.carDrivetrain.throttle : 0;
         this.soundVolume += (target - this.soundVolume) * 0.15F;
         return;
      }
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
      if(this.hasCarDrivetrain()) {
         float rev = Math.max(0, Math.min(1, (this.carDrivetrain.rpm - this.tankInfo.carIdleRpm)
               / (this.tankInfo.carRedlineRpm - this.tankInfo.carIdleRpm)));
         return 0.65F + rev * 0.75F;
      }
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
         MCH_Parts[] iteratedValues = parts;
         int iteratedValueCount = parts.length;

         for(int iteratedValueIndex = 0; iteratedValueIndex < iteratedValueCount; ++iteratedValueIndex) {
            MCH_Parts p = iteratedValues[iteratedValueIndex];
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
