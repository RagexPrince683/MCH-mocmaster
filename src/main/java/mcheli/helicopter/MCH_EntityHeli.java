package mcheli.helicopter;

import java.util.Iterator;
import java.util.List;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.*;
import mcheli.chain.MCH_EntityChain;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityHeli extends MCH_EntityBaseVehicle {

   public static final byte FOLD_STAT_FOLDED = 0;
   public static final byte FOLD_STAT_FOLDING = 1;
   public static final byte FOLD_STAT_UNFOLDED = 2;
   public static final byte FOLD_STAT_UNFOLDING = 3;
   private MCH_HeliInfo heliInfo = null;
   public double prevRotationRotor = 0.0D;
   public double rotationRotor = 0.0D;
   public MCH_Rotor[] rotors;
   public byte lastFoldBladeStat;
   public int foldBladesCooldown;
   public float prevRollFactor = 0.0F;
   private boolean newHeliFlightModelEnabled;
   private float physicalMass = 1.0F;
   private float normalizedRotorRPM;
   private float targetRotorRPM;
   private float rotorEnergy;
   private float enginePowerOutput;
   private float lastRotorRPM;
   private float rotorSpoolDelta;
   private boolean rotorReadyForLift;
   private float collectiveInput;
   private float rotorThrust;
   private float rotorEfficiency;
   private float rotorVerticalThrust;
   private float weightForce;
   private float netVerticalForce;
   private float verticalAcceleration;
   private float verticalDragApplied;
   private float finalMotionY;
   private float verticalForce;
   private float cyclicPitchInput;
   private float cyclicRollInput;
   private float rotorTiltForward;
   private float rotorTiltRight;
   private float rotorHorizontalThrustX;
   private float rotorHorizontalThrustZ;
   private float horizontalAccelerationX;
   private float horizontalAccelerationZ;
   private float parasiteDragAppliedX;
   private float parasiteDragAppliedZ;
   private float forwardVelocityComponent;
   private float lateralVelocityComponent;
   private float appliedLateralThrustMultiplier;
   private boolean lateralSpeedCapped;
   private boolean backwardThrustScaled;
   private boolean backwardSpeedCapped;
   private float finalMotionX;
   private float finalMotionZ;
   private float hoverAssistStrength;
   private float hoverCollectiveCorrection;
   private float hoverThrottleBias;
   private float hoverVerticalSpeedAverage;
   private int hoverVerticalNextAdjustmentTick;
   private float hoverCyclicPitchCorrection;
   private float hoverCyclicRollCorrection;
   private float manualInputOverrideFactor;
   private float targetVerticalSpeed;
   private float targetHorizontalSpeed;
   private float localDriftForward;
   private float localDriftRight;
   private float tailRotorInput;
   /** Positive main-rotor torque reaction yaws the fuselage right unless countered by tail rotor thrust. */
   private float heliYawAngularVelocity;
   private float heliYawTorque;
   private float tailRotorTorque;
   private float mainRotorTorqueReaction;
   private float yawDampingApplied;
   private float yawAngularAcceleration;
   private float finalRotYaw;
   private boolean hoverAssistActive;
   private boolean mouseFlightInputLocked;
   private float debugYawInput;
   private float debugYawAuthority;
   private float debugFinalYawRate;
   private static final float NEW_HELI_MIN_MASS = 0.01F;
   private static final float NEW_HELI_MIN_INERTIA = 0.01F;
   private static final float NEW_HELI_GROUNDED_YAW_INPUT_DEADZONE = 0.04F;
   private static final float NEW_HELI_GROUNDED_YAW_DAMPING = 0.35F;
   private static final double LEGACY_HELI_REGULAR_FORWARD_ACCEL = 0.50D;
   private static final double LEGACY_HELI_HOVER_TRANSLATION_ACCEL = 0.0015D;
   private static final double NEW_HELI_REGULAR_HORIZONTAL_SPEED_SCALE = 4.0D;
   private static final double NEW_HELI_HOVER_HORIZONTAL_SPEED_SCALE = 0.35D;


   public MCH_EntityHeli(World world) {
      super(world);
      super.currentSpeed = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(2.0F, 0.7F);
      super.yOffset = super.height / 2.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      super.weapons = this.createWeapon(0);
      this.rotors = new MCH_Rotor[0];
      this.lastFoldBladeStat = -1;
      if(super.worldObj.isRemote) {
         this.foldBladesCooldown = 40;
      }

   }

   public String getKindName() {
      return "helicopters";
   }

   public String getEntityType() {
      return "Plane";
   }

   public MCH_HeliInfo getHeliInfo() {
      return this.heliInfo;
   }

   public void changeType(String type) {
      if(!type.isEmpty()) {
         this.heliInfo = MCH_HeliInfoManager.get(type);
      }

      if(this.heliInfo == null) {
         MCH_Lib.Log((Entity)this, "##### MCH_EntityHeli changeHeliType() Heli info null %d, %s, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), type, this.getEntityName()});
         this.setDead(true);
      } else {
         this.setAcInfo(this.heliInfo);
         this.updateNewHelicopterFlightTelemetryConfig();
         this.newSeats(this.getAcInfo().getNumSeatAndRack());
         this.createRotors();
         super.weapons = this.createWeapon(1 + this.getSeatNum());
         this.initPartRotation(this.getRotYaw(), this.getRotPitch());
      }

   }

   private void updateNewHelicopterFlightTelemetryConfig() {
      this.newHeliFlightModelEnabled = this.heliInfo != null && this.heliInfo.useNewHelicopterFlightModel;
      this.physicalMass = this.newHeliFlightModelEnabled?this.sanitizePositive(this.heliInfo.physicalMass, 1.0F, NEW_HELI_MIN_MASS):1.0F;
      if(!this.newHeliFlightModelEnabled) {
         this.resetNewHelicopterFlightTelemetry();
      }
   }

   private boolean isFinite(double value) {
      return !Double.isNaN(value) && !Double.isInfinite(value);
   }

   private boolean isFinite(float value) {
      return !Float.isNaN(value) && !Float.isInfinite(value);
   }

   private float sanitizePositive(float value, float fallback, float minValue) {
      return this.isFinite(value) && value >= minValue?value:Math.max(fallback, minValue);
   }

   private float sanitizeClamped(float value, float minValue, float maxValue, float fallback) {
      return this.isFinite(value)?MathHelper.clamp_float(value, minValue, maxValue):MathHelper.clamp_float(fallback, minValue, maxValue);
   }

   private float getSafeTickDelta(float tickDelta) {
      return this.sanitizeClamped(tickDelta, 0.0F, 1.0F, 1.0F);
   }

   private void resetNewHelicopterFlightTelemetry() {
      this.normalizedRotorRPM = 0.0F;
      this.targetRotorRPM = 0.0F;
      this.rotorEnergy = 0.0F;
      this.enginePowerOutput = 0.0F;
      this.lastRotorRPM = 0.0F;
      this.rotorSpoolDelta = 0.0F;
      this.rotorReadyForLift = false;
      this.collectiveInput = 0.0F;
      this.rotorThrust = 0.0F;
      this.rotorEfficiency = 0.0F;
      this.rotorVerticalThrust = 0.0F;
      this.weightForce = 0.0F;
      this.netVerticalForce = 0.0F;
      this.verticalAcceleration = 0.0F;
      this.verticalDragApplied = 0.0F;
      this.finalMotionY = 0.0F;
      this.verticalForce = 0.0F;
      this.cyclicPitchInput = 0.0F;
      this.cyclicRollInput = 0.0F;
      this.rotorTiltForward = 0.0F;
      this.rotorTiltRight = 0.0F;
      this.rotorHorizontalThrustX = 0.0F;
      this.rotorHorizontalThrustZ = 0.0F;
      this.horizontalAccelerationX = 0.0F;
      this.horizontalAccelerationZ = 0.0F;
      this.parasiteDragAppliedX = 0.0F;
      this.parasiteDragAppliedZ = 0.0F;
      this.forwardVelocityComponent = 0.0F;
      this.lateralVelocityComponent = 0.0F;
      this.appliedLateralThrustMultiplier = 0.0F;
      this.lateralSpeedCapped = false;
      this.backwardThrustScaled = false;
      this.backwardSpeedCapped = false;
      this.finalMotionX = 0.0F;
      this.finalMotionZ = 0.0F;
      this.hoverAssistStrength = 0.0F;
      this.hoverCollectiveCorrection = 0.0F;
      this.hoverThrottleBias = 0.0F;
      this.hoverVerticalSpeedAverage = 0.0F;
      this.hoverVerticalNextAdjustmentTick = 0;
      this.hoverCyclicPitchCorrection = 0.0F;
      this.hoverCyclicRollCorrection = 0.0F;
      this.manualInputOverrideFactor = 0.0F;
      this.targetVerticalSpeed = 0.0F;
      this.targetHorizontalSpeed = 0.0F;
      this.localDriftForward = 0.0F;
      this.localDriftRight = 0.0F;
      this.tailRotorInput = 0.0F;
      this.heliYawAngularVelocity = 0.0F;
      this.heliYawTorque = 0.0F;
      this.tailRotorTorque = 0.0F;
      this.mainRotorTorqueReaction = 0.0F;
      this.yawDampingApplied = 0.0F;
      this.yawAngularAcceleration = 0.0F;
      this.finalRotYaw = this.getRotYaw();
      this.hoverAssistActive = false;
      this.mouseFlightInputLocked = false;
      this.debugYawInput = 0.0F;
      this.debugYawAuthority = 0.0F;
      this.debugFinalYawRate = 0.0F;
   }

   public boolean isNewHeliFlightModelEnabled() {
      return this.newHeliFlightModelEnabled;
   }

   public float getPhysicalMass() {
      return this.physicalMass;
   }

   public float getNormalizedRotorRPM() {
      return this.normalizedRotorRPM;
   }

   public float getRotorRPM() {
      return this.normalizedRotorRPM;
   }

   public float getTargetRotorRPM() {
      return this.targetRotorRPM;
   }

   public float getRotorEnergy() {
      return this.rotorEnergy;
   }

   public float getEnginePowerOutput() {
      return this.enginePowerOutput;
   }

   public float getLastRotorRPM() {
      return this.lastRotorRPM;
   }

   public float getRotorSpoolDelta() {
      return this.rotorSpoolDelta;
   }

   public boolean isRotorReadyForLift() {
      return this.rotorReadyForLift;
   }

   public float getCollectiveInput() {
      return this.collectiveInput;
   }

   public float getRotorThrust() {
      return this.rotorThrust;
   }

   public float getRotorEfficiency() {
      return this.rotorEfficiency;
   }

   public float getRotorVerticalThrust() {
      return this.rotorVerticalThrust;
   }

   public float getWeightForce() {
      return this.weightForce;
   }

   public float getNetVerticalForce() {
      return this.netVerticalForce;
   }

   public float getVerticalAcceleration() {
      return this.verticalAcceleration;
   }

   public float getVerticalDragApplied() {
      return this.verticalDragApplied;
   }

   public float getFinalMotionY() {
      return this.finalMotionY;
   }

   public float getVerticalForce() {
      return this.verticalForce;
   }

   public float getCyclicPitchInput() {
      return this.cyclicPitchInput;
   }

   public float getCyclicRollInput() {
      return this.cyclicRollInput;
   }

   public float getRotorTiltForward() {
      return this.rotorTiltForward;
   }

   public float getRotorTiltRight() {
      return this.rotorTiltRight;
   }

   public float getRotorHorizontalThrustX() {
      return this.rotorHorizontalThrustX;
   }

   public float getRotorHorizontalThrustZ() {
      return this.rotorHorizontalThrustZ;
   }

   public float getHorizontalAccelerationX() {
      return this.horizontalAccelerationX;
   }

   public float getHorizontalAccelerationZ() {
      return this.horizontalAccelerationZ;
   }

   public float getParasiteDragAppliedX() {
      return this.parasiteDragAppliedX;
   }

   public float getParasiteDragAppliedZ() {
      return this.parasiteDragAppliedZ;
   }

   public float getForwardVelocityComponent() {
      return this.forwardVelocityComponent;
   }

   public float getLateralVelocityComponent() {
      return this.lateralVelocityComponent;
   }

   public float getAppliedLateralThrustMultiplier() {
      return this.appliedLateralThrustMultiplier;
   }

   public boolean isLateralSpeedCapped() {
      return this.lateralSpeedCapped;
   }

   public boolean isBackwardThrustScaled() {
      return this.backwardThrustScaled;
   }

   public boolean isBackwardSpeedCapped() {
      return this.backwardSpeedCapped;
   }

   public float getFinalMotionX() {
      return this.finalMotionX;
   }

   public float getFinalMotionZ() {
      return this.finalMotionZ;
   }

   public float getHoverAssistStrength() {
      return this.hoverAssistStrength;
   }

   public float getHoverCollectiveCorrection() {
      return this.hoverCollectiveCorrection;
   }

   public float getHoverCyclicPitchCorrection() {
      return this.hoverCyclicPitchCorrection;
   }

   public float getHoverCyclicRollCorrection() {
      return this.hoverCyclicRollCorrection;
   }

   public float getManualInputOverrideFactor() {
      return this.manualInputOverrideFactor;
   }

   public float getTargetVerticalSpeed() {
      return this.targetVerticalSpeed;
   }

   public float getTargetHorizontalSpeed() {
      return this.targetHorizontalSpeed;
   }

   public float getLocalDriftForward() {
      return this.localDriftForward;
   }

   public float getLocalDriftRight() {
      return this.localDriftRight;
   }

   public float getTailRotorInput() {
      return this.tailRotorInput;
   }

   @Override
   public float getYawAngularVelocity() {
      return this.newHeliFlightModelEnabled?this.heliYawAngularVelocity:super.getYawAngularVelocity();
   }

   public float getHeliYawTorque() {
      return this.heliYawTorque;
   }

   public float getTailRotorTorque() {
      return this.tailRotorTorque;
   }

   public float getMainRotorTorqueReaction() {
      return this.mainRotorTorqueReaction;
   }

   public float getYawDampingApplied() {
      return this.yawDampingApplied;
   }

   public float getYawAngularAcceleration() {
      return this.yawAngularAcceleration;
   }

   public float getFinalRotYaw() {
      return this.finalRotYaw;
   }

   public boolean isHoverAssistActive() {
      return this.hoverAssistActive;
   }

   public boolean isMouseFlightInputLocked() {
      return this.mouseFlightInputLocked;
   }

   public float getDebugYawInput() {
      return this.debugYawInput;
   }

   public float getDebugYawAuthority() {
      return this.debugYawAuthority;
   }

   public float getDebugFinalYawRate() {
      return this.debugFinalYawRate;
   }

   public Item getItem() {
      return this.getHeliInfo() != null?this.getHeliInfo().item:null;
   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MountMinecartHeli.prmBool;
   }

   protected void entityInit() {
      super.entityInit();
      super.dataWatcher.addObject(30, Byte.valueOf((byte)2));
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setDouble("RotorSpeed", this.getCurrentThrottle());
      par1NBTTagCompound.setDouble("rotetionRotor", this.rotationRotor);
      par1NBTTagCompound.setBoolean("FoldBlade", this.getFoldBladeStat() == 0);
      par1NBTTagCompound.setFloat("NewHeliRotorRPM", this.normalizedRotorRPM);
      par1NBTTagCompound.setFloat("NewHeliTargetRotorRPM", this.targetRotorRPM);
      par1NBTTagCompound.setFloat("NewHeliRotorEnergy", this.rotorEnergy);
      par1NBTTagCompound.setFloat("NewHeliEnginePower", this.enginePowerOutput);
      par1NBTTagCompound.setFloat("NewHeliYawAngularVelocity", this.heliYawAngularVelocity);
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      boolean beforeFoldBlade = this.getFoldBladeStat() == 0;
      if(this.getCommonUniqueId().isEmpty()) {
         this.setCommonUniqueId(par1NBTTagCompound.getString("HeliUniqueId"));
         MCH_Lib.Log((Entity)this, "# MCH_EntityHeli readEntityFromNBT() " + W_Entity.getEntityId(this) + ", " + this.getEntityName() + ", AircraftUniqueId=null, HeliUniqueId=" + this.getCommonUniqueId(), new Object[0]);
      }

      if(this.getTypeName().isEmpty()) {
         this.setTypeName(par1NBTTagCompound.getString("HeliType"));
         MCH_Lib.Log((Entity)this, "# MCH_EntityHeli readEntityFromNBT() " + W_Entity.getEntityId(this) + ", " + this.getEntityName() + ", TypeName=null, HeliType=" + this.getTypeName(), new Object[0]);
      }

      this.setCurrentThrottle(par1NBTTagCompound.getDouble("RotorSpeed"));
      this.rotationRotor = par1NBTTagCompound.getDouble("rotetionRotor");
      if(par1NBTTagCompound.hasKey("NewHeliRotorRPM")) {
         this.normalizedRotorRPM = this.sanitizeClamped(par1NBTTagCompound.getFloat("NewHeliRotorRPM"), 0.0F, 1.0F, 0.0F);
      }
      if(par1NBTTagCompound.hasKey("NewHeliTargetRotorRPM")) {
         this.targetRotorRPM = this.sanitizeClamped(par1NBTTagCompound.getFloat("NewHeliTargetRotorRPM"), 0.0F, 1.0F, 0.0F);
      }
      if(par1NBTTagCompound.hasKey("NewHeliRotorEnergy")) {
         this.rotorEnergy = Math.max(this.sanitizeClamped(par1NBTTagCompound.getFloat("NewHeliRotorEnergy"), 0.0F, 100000.0F, 0.0F), 0.0F);
      }
      if(par1NBTTagCompound.hasKey("NewHeliEnginePower")) {
         this.enginePowerOutput = this.sanitizeClamped(par1NBTTagCompound.getFloat("NewHeliEnginePower"), 0.0F, 1.0F, 0.0F);
      }
      if(par1NBTTagCompound.hasKey("NewHeliYawAngularVelocity")) {
         this.heliYawAngularVelocity = this.sanitizeClamped(par1NBTTagCompound.getFloat("NewHeliYawAngularVelocity"), -8.0F, 8.0F, 0.0F);
      }
      this.setFoldBladeStat((byte)(par1NBTTagCompound.getBoolean("FoldBlade")?0:2));
      if(this.heliInfo == null) {
         this.heliInfo = MCH_HeliInfoManager.get(this.getTypeName());
         if(this.heliInfo == null) {
            MCH_Lib.Log((Entity)this, "##### MCH_EntityHeli readEntityFromNBT() Heli info null %d, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName()});
            this.setDead(true);
         } else {
            this.setAcInfo(this.heliInfo);
            this.updateNewHelicopterFlightTelemetryConfig();
         }
      }
      if(!this.newHeliFlightModelEnabled) {
         this.resetNewHelicopterFlightTelemetry();
      }

      if(!beforeFoldBlade && this.getFoldBladeStat() == 0) {
         this.forceFoldBlade();
      }

      this.prevRotationRotor = this.rotationRotor;
   }

   private void updateUnpilotedThrottleDecay() {
      this.hoverThrottleBias = 0.0F;
      this.hoverCollectiveCorrection = 0.0F;
      this.hoverVerticalSpeedAverage = 0.0F;
      this.hoverVerticalNextAdjustmentTick = 0;
      this.targetVerticalSpeed = 0.0F;
      if(this.getCurrentThrottle() > 0.0D) {
         float throttleUpDown = this.getAcInfo() != null?this.getAcInfo().throttleUpDown:1.0F;
         double decay = Math.max(0.02D * (double)Math.max(throttleUpDown, 0.0F), 0.005D);
         this.addCurrentThrottle(-decay);
      } else {
         this.setCurrentThrottle(0.0D);
      }
   }

   public int getNumEjectionSeat() {
      if(this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat) {
         int n = this.getSeatNum() + 1;
         return n <= 2?n:0;
      } else {
         return 0;
      }
   }

   public float getSoundVolume() {
      return this.getAcInfo() != null && this.getAcInfo().throttleUpDown <= 0.0F?0.0F:(float)this.getCurrentThrottle() * 2.0F;
   }

   public float getSoundPitch() {
      return (float)(0.2D + this.getCurrentThrottle() * 0.2D);
   }

   public String getDefaultSoundName() {
      return "heli";
   }

   public float getUnfoldLandingGearThrottle() {
      double x = super.posX - super.prevPosX;
      double y = super.posY - super.prevPosY;
      double z = super.posZ - super.prevPosZ;
      float s = this.getAcInfo().speed / 3.5F;
      return x * x + y * y + z * z <= (double)s?0.8F:0.3F;
   }

   protected void createRotors() {
      if(this.heliInfo != null) {
         this.rotors = new MCH_Rotor[this.heliInfo.rotorList.size()];
         int i = 0;

         for(Iterator i$ = this.heliInfo.rotorList.iterator(); i$.hasNext(); ++i) {
            MCH_HeliInfo.Rotor r = (MCH_HeliInfo.Rotor)i$.next();
            this.rotors[i] = new MCH_Rotor(r.bladeNum, r.bladeRot, super.worldObj.isRemote?2:2, (float)r.pos.xCoord, (float)r.pos.yCoord, (float)r.pos.zCoord, (float)r.rot.xCoord, (float)r.rot.yCoord, (float)r.rot.zCoord, r.haveFoldFunc);
         }

      }
   }

   protected void forceFoldBlade() {
      if(this.heliInfo != null && this.rotors.length > 0 && this.heliInfo.isEnableFoldBlade) {
         MCH_Rotor[] arr$ = this.rotors;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Rotor r = arr$[i$];
            r.update((float)this.rotationRotor);
            this.foldBlades();
            r.forceFold();
         }
      }

   }

   public boolean isFoldBlades() {
      return this.heliInfo != null && this.rotors.length > 0?this.getFoldBladeStat() == 0:false;
   }

   protected boolean canSwitchFoldBlades() {
      return this.heliInfo != null && this.rotors.length > 0?this.heliInfo.isEnableFoldBlade && this.getCurrentThrottle() <= 0.01D && this.foldBladesCooldown == 0 && (this.getFoldBladeStat() == 2 || this.getFoldBladeStat() == 0):false;
   }

   protected boolean canUseBlades() {
      if(this.heliInfo == null) {
         return false;
      } else if(this.rotors.length <= 0) {
         return true;
      } else if(this.getFoldBladeStat() == 2) {
         MCH_Rotor[] arr$ = this.rotors;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Rotor r = arr$[i$];
            if(r.isFoldingOrUnfolding()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isNewHelicopterBladesUsable() {
      return this.canUseBlades();
   }

   protected void foldBlades() {
      if(this.heliInfo != null && this.rotors.length > 0) {
         this.setCurrentThrottle(0.0D);
         MCH_Rotor[] arr$ = this.rotors;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Rotor r = arr$[i$];
            r.startFold();
         }

      }
   }

   public void unfoldBlades() {
      if(this.heliInfo != null && this.rotors.length > 0) {
         MCH_Rotor[] arr$ = this.rotors;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Rotor r = arr$[i$];
            r.startUnfold();
         }

      }
   }

   public void onRideEntity(Entity ridingEntity) {
      if(ridingEntity instanceof MCH_EntitySeat) {
         if(this.heliInfo == null || this.rotors.length <= 0) {
            return;
         }

         if(this.heliInfo.isEnableFoldBlade) {
            this.forceFoldBlade();
            this.setFoldBladeStat((byte)0);
         }
      }

   }

   protected byte getFoldBladeStat() {
      return super.dataWatcher.getWatchableObjectByte(30);
   }

   public void setFoldBladeStat(byte b) {
      if(!super.worldObj.isRemote && b >= 0 && b <= 3) {
         super.dataWatcher.updateObject(30, Byte.valueOf(b));
      }

   }

   public boolean canSwitchGunnerMode() {
      if(super.canSwitchGunnerMode() && this.canUseBlades()) {
         float roll = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotRoll()));
         float pitch = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotPitch()));
         if(roll < 40.0F && pitch < 40.0F) {
            return true;
         }
      }

      return false;
   }

   public boolean canSwitchHoveringMode() {
      if(super.canSwitchHoveringMode() && this.canUseBlades()) {
         float roll = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotRoll()));
         float pitch = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotPitch()));
         if(roll < 40.0F && pitch < 40.0F) {
            return true;
         }
      }

      return false;
   }

   public void onUpdateAircraft() {
      if(this.heliInfo == null) {
         this.changeType(this.getTypeName());
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
      } else {
         if(!super.isRequestedSyncStatus) {
            super.isRequestedSyncStatus = true;
            if(super.worldObj.isRemote) {
               byte stat = this.getFoldBladeStat();
               if(stat == 1 || stat == 0) {
                  this.forceFoldBlade();
               }

               MCH_PacketStatusRequest.requestStatus(this);
            }
         }

         if(super.lastRiddenByEntity == null && this.getRiddenByEntity() != null) {
            this.initCurrentWeapon(this.getRiddenByEntity());
         }

         this.updateNewHelicopterFlightTelemetryConfig();
         if(this.newHeliFlightModelEnabled && (!this.canUseBlades() || this.isFoldBlades())) {
            this.resetNewHelicopterFlightTelemetry();
         }
         this.updateWeapons();
         this.onUpdate_Seats();
         this.onUpdate_Control();
         this.onUpdate_Rotor();
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
         if(!this.newHeliFlightModelEnabled && !this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < 70.0F) {
            this.setRotPitch(this.decayMobilityValue(this.getRotPitch(), 0.95F, 1.0F));
         }

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

   public boolean canMouseRot() {
      return super.canMouseRot() && !this.isMouseFlightInputLocked();
   }

   public boolean canSwitchFreeLook() {
      return !super.isGunnerMode && super.canSwitchFreeLook();
   }

   public boolean canUpdatePitch(Entity player) {
      return super.canUpdatePitch(player) && (this.newHeliFlightModelEnabled || !this.isHovering());
   }

   public boolean canUpdateRoll(Entity player) {
      return super.canUpdateRoll(player) && (this.newHeliFlightModelEnabled || !this.isHovering());
   }

   public boolean isOverridePlayerPitch() {
      return super.isOverridePlayerPitch() && (this.newHeliFlightModelEnabled || !this.isHovering());
   }

   public float getRollFactor() {
      float roll = super.getRollFactor();
      double d = this.getDistanceSq(super.prevPosX, super.posY, super.prevPosZ);
      double s = (double)this.getAcInfo().speed;
      double var10000;
      if(s > 0.1D) {
         var10000 = d / s;
      } else {
         var10000 = 0.0D;
      }

      float f = this.prevRollFactor;
      this.prevRollFactor = roll;
      return (roll + f) / 2.0F;
   }

   public float getControlRotYaw(float mouseX, float mouseY, float tick) {
      this.mouseFlightInputLocked = this.newHeliFlightModelEnabled && super.isGunnerMode;
      if(this.newHeliFlightModelEnabled) {
         if(this.mouseFlightInputLocked) {
            this.tailRotorInput = 0.0F;
            this.debugYawInput = 0.0F;
            return 0.0F;
         }
         float yawLimit = (float)Math.max(this.getAddRotationYawLimit(), 1.0D);
         this.tailRotorInput = MathHelper.clamp_float(mouseX / yawLimit, -1.0F, 1.0F);
         this.debugYawInput = this.tailRotorInput;
         return 0.0F;
      }

      return mouseX;
   }

   public float getControlRotPitch(float mouseX, float mouseY, float tick) {
      return mouseY;
   }

   public float getControlRotRoll(float mouseX, float mouseY, float tick) {
      return mouseX;
   }

   public void onUpdateAngles(float partialTicks) {
      if(this.useNewMobilitySystem()) {
         partialTicks = MCH_FlightModel.getBoundedTickDelta(partialTicks);
      }
      if(!this.isDestroyed()) {
         float rotRoll = !this.isHovering()?0.96F:0.93F;
         if((double)this.getRotRoll() > 0.1D && this.getRotRoll() < 65.0F) {
            this.setRotRoll(this.decayMobilityValue(this.getRotRoll(), rotRoll, partialTicks));
         }

         if((double)this.getRotRoll() < -0.1D && this.getRotRoll() > -65.0F) {
            this.setRotRoll(this.decayMobilityValue(this.getRotRoll(), rotRoll, partialTicks));
         }

         if(MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
            float controlAuthority = this.getControlAuthorityFactor();
            if(super.moveLeft && !super.moveRight) {
               this.setRotRoll(this.getRotRoll() - 1.2F * partialTicks * controlAuthority);
            }

            if(super.moveRight && !super.moveLeft) {
               this.setRotRoll(this.getRotRoll() + 1.2F * partialTicks * controlAuthority);
            }
         } else {
            if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
               this.applyOnGroundPitch(0.97F);
            }

            if(!this.newHeliFlightModelEnabled && this.heliInfo.isEnableFoldBlade && this.rotors.length > 0 && this.getFoldBladeStat() == 0 && !this.isDestroyed()) {
               if(super.moveLeft && !super.moveRight) {
                  this.setRotYaw(this.getRotYaw() - 0.5F * partialTicks);
               }

               if(super.moveRight && !super.moveLeft) {
                  this.setRotYaw(this.getRotYaw() + 0.5F * partialTicks);
               }
            }
         }

         if(this.newHeliFlightModelEnabled) {
            this.applyNewHelicopterYawModel(partialTicks);
         }
      }
   }

   private void applyNewHelicopterYawModel(float tickDelta) {
      if(this.heliInfo == null) {
         return;
      }
      tickDelta = this.getSafeTickDelta(tickDelta);

      boolean bladesUsable = this.canUseBlades() && !this.isFoldBlades();
      boolean engineUsable = !this.isDestroyed() && bladesUsable && this.isCanopyClose() && this.canUseFuel(true);
      this.normalizedRotorRPM = this.sanitizeClamped(this.normalizedRotorRPM, 0.0F, 1.0F, 0.0F);
      float rpmAuthority = engineUsable?this.normalizedRotorRPM:0.0F;
      float damageAuthority = MathHelper.clamp_float((float)this.getHP() / Math.max(1.0F, (float)this.getMaxHP()), 0.25F, 1.0F);
      if(this.isDestroyed()) {
         damageAuthority = 0.0F;
      }

      float collective = this.sanitizeClamped(this.collectiveInput, 0.0F, 1.0F, 0.0F);
      float maxThrust = Math.max(this.sanitizePositive(this.heliInfo.mainRotorMaxThrust, 0.0F, 0.0F), 0.0F);
      float rotorLoad = Math.max(Math.max(this.rotorThrust, 0.0F), maxThrust * collective * this.sanitizeClamped(this.enginePowerOutput, 0.0F, 1.0F, 0.0F));
      // Positive reaction is the fuselage yawing right from main-rotor drag; tail-rotor torque subtracts from it.
      this.mainRotorTorqueReaction = engineUsable?rotorLoad * rpmAuthority:0.0F;

      if(this.isNewHelicopterGroundedForYaw()) {
         float groundedDamping = MathHelper.clamp_float(NEW_HELI_GROUNDED_YAW_DAMPING * tickDelta, 0.0F, 1.0F);
         this.heliYawAngularVelocity *= 1.0F - groundedDamping;
         if(MathHelper.abs(this.tailRotorInput) <= NEW_HELI_GROUNDED_YAW_INPUT_DEADZONE || MathHelper.abs(this.heliYawAngularVelocity) < 0.01F) {
            this.heliYawAngularVelocity = 0.0F;
         }

         this.mainRotorTorqueReaction = 0.0F;
         this.tailRotorTorque = 0.0F;
         this.heliYawTorque = 0.0F;
         this.yawAngularAcceleration = 0.0F;
         this.yawDampingApplied = 0.0F;
         this.debugFinalYawRate = this.heliYawAngularVelocity;
         this.finalRotYaw = this.getRotYaw() + this.heliYawAngularVelocity * tickDelta;
         this.setRotYaw(this.finalRotYaw);
         this.logNewHelicopterControlDebug();
         return;
      }

      float tailAuthority = Math.max(this.heliInfo.tailRotorAuthority, 0.0F) * this.getNewHelicopterYawAuthorityBoost() * rpmAuthority * damageAuthority;
      this.debugYawAuthority = tailAuthority;
      this.tailRotorTorque = this.tailRotorInput * tailAuthority * Math.max(maxThrust, 0.01F);
      this.heliYawTorque = this.tailRotorTorque - this.mainRotorTorqueReaction;

      float inertia = this.sanitizePositive(this.heliInfo.angularInertia, 1.0F, NEW_HELI_MIN_INERTIA);
      this.yawAngularAcceleration = this.heliYawTorque / inertia;
      this.heliYawAngularVelocity += this.yawAngularAcceleration * tickDelta;

      this.yawDampingApplied = -this.heliYawAngularVelocity * Math.max(this.heliInfo.yawDamping, 0.0F) * tickDelta;
      this.heliYawAngularVelocity += this.yawDampingApplied;
      this.heliYawAngularVelocity = MathHelper.clamp_float(this.heliYawAngularVelocity, -8.0F, 8.0F);
      this.debugFinalYawRate = this.heliYawAngularVelocity;
      this.logNewHelicopterControlDebug();

      this.finalRotYaw = this.getRotYaw() + this.heliYawAngularVelocity * tickDelta;
      this.setRotYaw(this.finalRotYaw);
   }

   private boolean isNewHelicopterGroundedForYaw() {
      if(this.isDestroyed()) {
         return false;
      }

      return super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0;
   }

   private float getNewHelicopterYawAuthorityBoost() {
      // Yaw authority is tuned per helicopter asset via TailRotorAuthority/YawAuthority.
      // Keep this runtime multiplier neutral so config buffs stay explicit and bounded.
      return 1.0F;
   }

   private boolean isNewHelicopterHoverAssistMode() {
      return this.isHoveringMode() || super.isGunnerMode;
   }

   private void logNewHelicopterControlDebug() {
      if(MCH_Config.DebugFlightControl == null || !MCH_Config.DebugFlightControl.prmBool || this.ticksExisted % 20 != 0) {
         return;
      }
      MCH_Lib.Log((Entity)this, "[MCHeli][NewHeliControl] gunnerModeActive=%s hoverAssistActive=%s mouseFlightInputLocked=%s yawInput=%.3f yawAuthority=%.3f finalYawRate=%.3f",
            new Object[]{Boolean.valueOf(super.isGunnerMode), Boolean.valueOf(this.hoverAssistActive), Boolean.valueOf(this.mouseFlightInputLocked), Float.valueOf(this.debugYawInput), Float.valueOf(this.debugYawAuthority), Float.valueOf(this.debugFinalYawRate)});
   }

   protected void onUpdate_Rotor() {
      byte stat = this.getFoldBladeStat();
      boolean isEndSwitch = true;
      if(stat != this.lastFoldBladeStat) {
         if(stat == 1) {
            this.foldBlades();
         } else if(stat == 3) {
            this.unfoldBlades();
         }

         if(super.worldObj.isRemote) {
            this.foldBladesCooldown = 40;
         }

         this.lastFoldBladeStat = stat;
      } else if(this.foldBladesCooldown > 0) {
         --this.foldBladesCooldown;
      }

      MCH_Rotor[] arr$ = this.rotors;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_Rotor r = arr$[i$];
         r.update((float)this.rotationRotor);
         if(r.isFoldingOrUnfolding()) {
            isEndSwitch = false;
         }
      }

      if(isEndSwitch) {
         if(stat == 1) {
            this.setFoldBladeStat((byte)0);
         } else if(stat == 3) {
            this.setFoldBladeStat((byte)2);
         }
      }

   }

   protected void onUpdate_Control() {
      this.mouseFlightInputLocked = super.isGunnerMode;
      if(super.isGunnerMode && this.isFreeLookMode()) {
         this.switchFreeLookMode(false);
      }

      //if(getHP() * 100 / getMaxHP() < getAcInfo().engineShutdownThreshold) {
      //   setCurrentThrottle(0);
      //   throttleUp = false;
      //   throttleBack = 0;
      //   return;
      //}
      //hovering and death animation had issues with this. Not good!

      if(this.applyEngineWaterboardingThrottleCut()) {
         this.normalizedRotorRPM = 0.0F;
         this.targetRotorRPM = 0.0F;
         this.enginePowerOutput = 0.0F;
         return;
      }

      if(this.isHoveringMode() && !this.canUseFuel(true)) {
         this.switchHoveringMode(false);
      }

      if(super.isGunnerMode && !this.canUseFuel()) {
         this.switchGunnerMode(false);
      }

      if(!this.isDestroyed() && this.getRiddenByEntity() == null && this.canUseBlades() && this.isCanopyClose() && this.canUseFuel(true)) {
         this.updateUnpilotedThrottleDecay();
      } else if(!this.isDestroyed() && (this.getRiddenByEntity() != null || this.isHoveringMode()) && this.canUseBlades() && this.isCanopyClose() && this.canUseFuel(true)) {
         if(!this.isHovering()) {
            this.onUpdate_ControlNotHovering();
         } else {
            this.onUpdate_ControlHovering();
         }
      } else {
         if(this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-0.00125D);
         } else {
            this.setCurrentThrottle(0.0D);
         }

         if(this.heliInfo.isEnableFoldBlade && this.rotors.length > 0 && this.getFoldBladeStat() == 0 && super.onGround && !this.isDestroyed()) {
            this.onUpdate_ControlFoldBladeAndOnGround();
         }
      }

      if(super.worldObj.isRemote) {
         if(!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
            double rp = this.getThrottle();
            if(this.getCurrentThrottle() >= rp - 0.02D) {
               this.addCurrentThrottle(-0.01D);
            } else if(this.getCurrentThrottle() < rp) {
               this.addCurrentThrottle(0.01D);
            }
         }
      } else {
         this.setThrottle(this.getCurrentThrottle());
      }

      if(this.getCurrentThrottle() < 0.0D) {
         this.setCurrentThrottle(0.0D);
      }

      this.prevRotationRotor = this.rotationRotor;
      if(this.newHeliFlightModelEnabled) {
         this.updateNewHelicopterRotorSpool();
         this.rotationRotor += (double)(this.normalizedRotorRPM * this.getAcInfo().rotorSpeed);
      } else {
         float rp1 = (float)(1.0D - this.getCurrentThrottle());
         this.rotationRotor += (double)((1.0F - rp1 * rp1 * rp1) * this.getAcInfo().rotorSpeed);
      }

      this.rotationRotor %= 360.0D;
   }

   private void updateNewHelicopterRotorSpool() {
      boolean bladesUsable = this.canUseBlades() && !this.isFoldBlades();
      boolean enginePowered = !this.isDestroyed() && bladesUsable && this.isCanopyClose() && this.canUseFuel(true);
      this.normalizedRotorRPM = this.sanitizeClamped(this.normalizedRotorRPM, 0.0F, 1.0F, 0.0F);
      this.lastRotorRPM = this.normalizedRotorRPM;

      // Keep visual rotor RPM coupled to the aircraft throttle while the blades are
      // still usable.  Player dismount intentionally leaves currentThrottle to be
      // reduced by the existing auto-throttle-down path; forcing engine output to
      // zero just because there is no active pilot makes the new flight model stop
      // the rendered blades immediately while sound and particles continue to idle.
      float throttleRotorCommand = MathHelper.clamp_float((float)this.getCurrentThrottle(), 0.0F, 1.0F);
      this.enginePowerOutput = enginePowered?throttleRotorCommand:0.0F;
      this.targetRotorRPM = (!this.isDestroyed() && bladesUsable && this.isCanopyClose())?throttleRotorCommand:this.enginePowerOutput;

      float delta = this.targetRotorRPM - this.normalizedRotorRPM;
      float configuredRate = Math.max(delta >= 0.0F?this.heliInfo.rotorSpoolUpRate:this.heliInfo.rotorSpoolDownRate, 0.0F);
      float inertia = this.sanitizePositive(this.heliInfo.rotorInertia, 1.0F, NEW_HELI_MIN_INERTIA);
      float maxStep = MathHelper.clamp_float(configuredRate / inertia, 0.0F, 1.0F);
      if(MathHelper.abs(delta) <= maxStep) {
         this.normalizedRotorRPM = this.targetRotorRPM;
      } else if(delta > 0.0F) {
         this.normalizedRotorRPM += maxStep;
      } else {
         this.normalizedRotorRPM -= maxStep;
      }

      this.normalizedRotorRPM = MathHelper.clamp_float(this.normalizedRotorRPM, 0.0F, 1.0F);
      this.rotorSpoolDelta = this.normalizedRotorRPM - this.lastRotorRPM;
      this.rotorEnergy = this.normalizedRotorRPM * this.normalizedRotorRPM * inertia;
      this.rotorReadyForLift = bladesUsable && this.normalizedRotorRPM >= 0.85F;
   }

   protected void onUpdate_ControlNotHovering() {
      float throttleUpDown = this.getAcInfo().throttleUpDown;
      if(this.getRiddenByEntity() == null) {
         this.updateUnpilotedThrottleDecay();
         return;
      }

      if(super.throttleUp) {
         if(this.getCurrentThrottle() < 1.0D) {
            this.addCurrentThrottle(0.02D * (double)throttleUpDown);
         } else {
            this.setCurrentThrottle(1.0D);
         }
      } else if(super.throttleDown) {
         if(this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-0.014285714285714285D * (double)throttleUpDown);
         } else {
            this.setCurrentThrottle(0.0D);
         }
      } else if((!super.worldObj.isRemote || W_Lib.isClientPlayer(this.getRiddenByEntity())) && super.cs_heliAutoThrottleDown && (!this.newHeliFlightModelEnabled || !this.isHoveringMode())) {
         if(this.getCurrentThrottle() > 0.52D) {
            this.addCurrentThrottle(-0.01D * (double)throttleUpDown);
         } else if(this.getCurrentThrottle() < 0.48D) {
            this.addCurrentThrottle(0.01D * (double)throttleUpDown);
         }
      }

      this.updateNewHelicopterHoverThrottleController(throttleUpDown);

      if(!super.worldObj.isRemote && !this.newHeliFlightModelEnabled) {
         boolean move = false;
         float yaw = this.getRotYaw();
         double x = 0.0D;
         double z = 0.0D;
         if(super.moveLeft && !super.moveRight) {
            yaw = this.getRotYaw() - 90.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.moveRight && !super.moveLeft) {
            yaw = this.getRotYaw() + 90.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(move) {
            double f = 1.0D;
            double d = Math.sqrt(x * x + z * z);
            super.motionX -= x / d * 0.019999999552965164D * f * (double)this.getAcInfo().speed;
            super.motionZ += z / d * 0.019999999552965164D * f * (double)this.getAcInfo().speed;
         }
      }

   }

   private float getNewHelicopterCalculatedHoverThrottle() {
      if(this.heliInfo == null) {
         return 0.0F;
      }

      float mass = this.sanitizePositive(this.physicalMass, 1.0F, NEW_HELI_MIN_MASS);
      float gravity = MathHelper.abs(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
      float maxThrust = Math.max(this.heliInfo.mainRotorMaxThrust, 0.0F);
      float pitchComponent = MathHelper.cos(this.getRotPitch() / 180.0F * 3.1415927F);
      float rollComponent = MathHelper.cos(this.getRotRoll() / 180.0F * 3.1415927F);
      float cyclicTiltMagnitudeSq = this.rotorTiltForward * this.rotorTiltForward + this.rotorTiltRight * this.rotorTiltRight;
      float diskVerticalComponent = MathHelper.sqrt_float(Math.max(0.0F, 1.0F - cyclicTiltMagnitudeSq));
      float verticalComponent = MathHelper.clamp_float(pitchComponent * rollComponent * diskVerticalComponent, 0.05F, 1.0F);
      float ceilingEfficiency = (float)MCH_FlightModel.getCeilingLiftFactor(super.posY, this.getAcInfo().flightCeiling, this.getAcInfo().flightCeilingRange);
      double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      float translationalLift = MathHelper.clamp_float((float)(horizontalSpeed / Math.max(0.1D, (double)this.getAcInfo().speed)), 0.0F, 1.0F) * Math.max(this.heliInfo.translationalLiftCoefficient, 0.0F);
      float efficiency = MathHelper.clamp_float(ceilingEfficiency * verticalComponent + translationalLift, 0.05F, 2.0F);
      if(super.motionY < -0.12D && horizontalSpeed < 0.15D && this.getCurrentThrottle() > 0.45D) {
         efficiency *= 0.55F;
      }
      if(this.isDestroyed() || !this.canUseFuel(true) || !this.canUseBlades() || this.isFoldBlades()) {
         efficiency = 0.0F;
      }
      float liftSpool = MathHelper.clamp_float((this.normalizedRotorRPM - 0.35F) / 0.65F, 0.0F, 1.0F);
      liftSpool *= liftSpool;
      float availableVerticalThrust = maxThrust * liftSpool * efficiency * verticalComponent;
      if(availableVerticalThrust > 0.0F) {
         float verticalDrag = MathHelper.clamp_float(this.heliInfo.verticalDrag, 0.0F, 1000.0F);
         float dragAssist = (float)super.motionY * verticalDrag * mass;
         return MathHelper.clamp_float((mass * gravity + dragAssist) / availableVerticalThrust, 0.0F, 1.0F);
      }

      // If RPM is still spooling, fall back to the steady-state throttle/RPM
      // relationship, but keep the same attitude-compensated vertical component.
      float requiredLiftRatio = maxThrust > 0.0F?(mass * gravity) / (maxThrust * efficiency * verticalComponent):1.0F;
      float low = 0.35F;
      float high = 1.0F;
      for(int i = 0; i < 16; ++i) {
         float mid = (low + high) * 0.5F;
         float midSpool = MathHelper.clamp_float((mid - 0.35F) / 0.65F, 0.0F, 1.0F);
         float producedLiftRatio = mid * midSpool * midSpool;
         if(producedLiftRatio < requiredLiftRatio) {
            low = mid;
         } else {
            high = mid;
         }
      }

      return MathHelper.clamp_float((low + high) * 0.5F, 0.0F, 1.0F);
   }

   private void enforceNewHelicopterHoverMinimumThrottle() {
      if(this.newHeliFlightModelEnabled && this.isNewHelicopterHoverAssistMode() && this.heliInfo != null) {
         float minimumThrottle = Math.max(MathHelper.clamp_float(this.heliInfo.hoverMinimumThrottle, 0.0F, 1.0F), this.getNewHelicopterCalculatedHoverThrottle());
         if(this.getCurrentThrottle() < (double)minimumThrottle) {
            this.setCurrentThrottle((double)minimumThrottle);
         }
      }
   }

   private void updateNewHelicopterHoverThrottleController(float throttleUpDown) {
      if(!this.newHeliFlightModelEnabled || !this.isNewHelicopterHoverAssistMode() || this.heliInfo == null) {
         this.hoverThrottleBias = 0.0F;
         this.hoverVerticalSpeedAverage = 0.0F;
         this.hoverVerticalNextAdjustmentTick = 0;
         this.hoverCollectiveCorrection = 0.0F;
         return;
      }

      this.enforceNewHelicopterHoverMinimumThrottle();

      float configuredStrength = MathHelper.clamp_float(this.heliInfo.hoverAssistStrength, 0.0F, 1.0F);
      if(configuredStrength <= 0.0F || super.throttleUp || super.throttleDown) {
         this.hoverThrottleBias *= 0.90F;
         this.hoverVerticalNextAdjustmentTick = 0;
         this.hoverCollectiveCorrection = this.hoverThrottleBias;
         return;
      }

      this.targetVerticalSpeed = 0.0F;
      float calculatedHoverThrottle = this.getNewHelicopterCalculatedHoverThrottle();
      float throttleError = calculatedHoverThrottle - (float)this.getCurrentThrottle();
      float feedForwardLimit = MathHelper.clamp_float(this.heliInfo.hoverVerticalCorrectionLimit, 0.0F, 1.0F) * Math.max(throttleUpDown, 0.0F) * 2.0F;
      if(feedForwardLimit > 0.0F && MathHelper.abs(throttleError) > 0.001F) {
         this.addCurrentThrottle((double)MathHelper.clamp_float(throttleError, -feedForwardLimit, feedForwardLimit));
         this.setCurrentThrottle(MathHelper.clamp_double(this.getCurrentThrottle(), 0.0D, 1.0D));
      }
      float verticalSpeed = (float)MCH_HudShared.getVerticalSpeedMotionY(this);
      this.hoverVerticalSpeedAverage += (verticalSpeed - this.hoverVerticalSpeedAverage) * 0.20F;
      float deadzone = MathHelper.clamp_float(this.heliInfo.hoverVerticalSpeedDeadzone, 0.0F, 1.0F);
      float correctionLimit = MathHelper.clamp_float(this.heliInfo.hoverVerticalCorrectionLimit, 0.0F, 1.0F);
      float biasLimit = MathHelper.clamp_float(this.heliInfo.hoverThrottleBiasLimit, 0.0F, 1.0F);
      float verticalError = this.targetVerticalSpeed - this.hoverVerticalSpeedAverage;
      boolean correctingVerticalSpeed = MathHelper.abs(verticalError) > deadzone;
      if(correctingVerticalSpeed) {
         if(this.ticksExisted >= this.hoverVerticalNextAdjustmentTick) {
            float strength = Math.max(this.heliInfo.hoverVerticalStabilizerStrength, 0.0F);
            float normalizedError = MathHelper.clamp_float(MathHelper.abs(verticalError) / Math.max(deadzone, 0.01F), 0.0F, 6.0F);
            float responseCurve = 1.0F + normalizedError * normalizedError * 0.35F;
            float dynamicLimit = MathHelper.clamp_float(correctionLimit * responseCurve, correctionLimit, Math.min(biasLimit, correctionLimit * 4.0F));
            float correctionStep = MathHelper.clamp_float(verticalError * strength * configuredStrength * responseCurve, -dynamicLimit, dynamicLimit);
            this.hoverThrottleBias = MathHelper.clamp_float(this.hoverThrottleBias + correctionStep, -biasLimit, biasLimit);
            float throttleStepLimit = MathHelper.clamp_float(correctionLimit * (1.0F + normalizedError * 0.75F), correctionLimit, Math.min(biasLimit, correctionLimit * 5.0F));
            float throttleStep = MathHelper.clamp_float(this.hoverThrottleBias * Math.max(throttleUpDown, 0.0F), -throttleStepLimit, throttleStepLimit);
            this.addCurrentThrottle((double)throttleStep);
            this.setCurrentThrottle(MathHelper.clamp_double(this.getCurrentThrottle(), 0.0D, 1.0D));
            this.enforceNewHelicopterHoverMinimumThrottle();
            int adjustmentInterval = this.heliInfo != null?MathHelper.clamp_int(this.heliInfo.hoverVerticalAdjustmentInterval, 0, 200):0;
            this.hoverVerticalNextAdjustmentTick = this.ticksExisted + adjustmentInterval;
         }
      } else {
         this.hoverVerticalNextAdjustmentTick = 0;
         if(this.hoverThrottleBias > correctionLimit) {
            this.hoverThrottleBias -= correctionLimit;
         } else if(this.hoverThrottleBias < -correctionLimit) {
            this.hoverThrottleBias += correctionLimit;
         } else {
            this.hoverThrottleBias = 0.0F;
         }
      }
      this.hoverCollectiveCorrection = this.hoverThrottleBias;
   }

   protected void onUpdate_ControlHovering() {
      if(this.newHeliFlightModelEnabled) {
         this.onUpdate_ControlNotHovering();
         return;
      }

      if(this.getCurrentThrottle() < 1.0D) {
         this.addCurrentThrottle(0.03333333333333333D);
      } else {
         this.setCurrentThrottle(1.0D);
      }

      if(!super.worldObj.isRemote && !this.newHeliFlightModelEnabled) {
         boolean move = false;
         float yaw = this.getRotYaw();
         double x = 0.0D;
         double z = 0.0D;
         if(super.throttleUp) {
            yaw = this.getRotYaw();
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.throttleDown) {
            yaw = this.getRotYaw() - 180.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.moveLeft && !super.moveRight) {
            yaw = this.getRotYaw() - 90.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.moveRight && !super.moveLeft) {
            yaw = this.getRotYaw() + 90.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(move) {
            double d = Math.sqrt(x * x + z * z);
            super.motionX -= x / d * LEGACY_HELI_HOVER_TRANSLATION_ACCEL * (double)this.getAcInfo().speed;
            super.motionZ += z / d * LEGACY_HELI_HOVER_TRANSLATION_ACCEL * (double)this.getAcInfo().speed;
         }
      }

   }

   protected void onUpdate_ControlFoldBladeAndOnGround() {
      if(!super.worldObj.isRemote) {
         boolean move = false;
         float yaw = this.getRotYaw();
         double x = 0.0D;
         double z = 0.0D;
         if(super.throttleUp) {
            yaw = this.getRotYaw();
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.throttleDown) {
            yaw = this.getRotYaw() - 180.0F;
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(move) {
            double d = Math.sqrt(x * x + z * z);
            super.motionX -= x / d * 0.029999999329447746D;
            super.motionZ += z / d * 0.029999999329447746D;
         }
      }

   }

   protected void onUpdate_Particle2() {
      if(super.worldObj.isRemote) {
         if((double)this.getHP() <= (double)this.getMaxHP() * 0.5D) {
            if(this.getHeliInfo() != null) {
               int rotorNum = this.getHeliInfo().rotorList.size();
               if(rotorNum > 0) {
                  if(super.isFirstDamageSmoke) {
                     super.prevDamageSmokePos = new Vec3[rotorNum];
                  }

                  for(int ri = 0; ri < rotorNum; ++ri) {
                     Vec3 rotor_pos = ((MCH_HeliInfo.Rotor)this.getHeliInfo().rotorList.get(ri)).pos;
                     float yaw = this.getRotYaw();
                     float pitch = this.getRotPitch();
                     Vec3 pos = MCH_Lib.RotVec3(rotor_pos, -yaw, -pitch, -this.getRotRoll());
                     double x = super.posX + pos.xCoord;
                     double y = super.posY + pos.yCoord;
                     double z = super.posZ + pos.zCoord;
                     if(super.isFirstDamageSmoke) {
                        super.prevDamageSmokePos[ri] = Vec3.createVectorHelper(x, y, z);
                     }

                     Vec3 prev = super.prevDamageSmokePos[ri];
                     double dx = x - prev.xCoord;
                     double dy = y - prev.yCoord;
                     double dz = z - prev.zCoord;
                     int num = (int)(MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) * 2.0F) + 1;

                     for(double i = 0.0D; i < (double)num; ++i) {
                        double p = (double)this.getHP() / (double)this.getMaxHP();
                        if(p < (double)(super.rand.nextFloat() / 2.0F)) {
                           float c = 0.2F + super.rand.nextFloat() * 0.3F;
                           MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", prev.xCoord + (x - prev.xCoord) * (i / (double)num), prev.yCoord + (y - prev.yCoord) * (i / (double)num), prev.zCoord + (z - prev.zCoord) * (i / (double)num));
                           prm.motionX = (super.rand.nextDouble() - 0.5D) * 0.3D;
                           prm.motionY = super.rand.nextDouble() * 0.1D;
                           prm.motionZ = (super.rand.nextDouble() - 0.5D) * 0.3D;
                           prm.size = ((float)super.rand.nextInt(5) + 5.0F) * 1.0F;
                           prm.setColor(0.7F + super.rand.nextFloat() * 0.1F, c, c, c);
                           MCH_ParticlesUtil.spawnParticle(prm);
                           MCH_BoundingBox[] boxes = this.getCalculatedExtraBoundingBoxes();
                           int ebi = super.rand.nextInt(1 + boxes.length);
                           if(p < 0.3D && ebi > 0) {
                              AxisAlignedBB bb = boxes[ebi - 1].boundingBox;
                              double bx = (bb.maxX + bb.minX) / 2.0D;
                              double by = (bb.maxY + bb.minY) / 2.0D;
                              double bz = (bb.maxZ + bb.minZ) / 2.0D;
                              prm.posX = bx;
                              prm.posY = by;
                              prm.posZ = bz;
                              MCH_ParticlesUtil.spawnParticle(prm);
                           }
                        }
                     }

                     super.prevDamageSmokePos[ri].xCoord = x;
                     super.prevDamageSmokePos[ri].yCoord = y;
                     super.prevDamageSmokePos[ri].zCoord = z;
                  }

                  super.isFirstDamageSmoke = false;
               }
            }
         }
      }
   }

   private void applyNewHelicopterCollectiveLift(double attitudeEfficiency, double throttle, double horizontalSpeed, float tickDelta) {
      tickDelta = this.getSafeTickDelta(tickDelta);
      float mass = this.sanitizePositive(this.physicalMass, 1.0F, NEW_HELI_MIN_MASS);
      this.physicalMass = mass;
      float gravity = MathHelper.abs(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
      boolean bladesUsable = this.canUseBlades() && !this.isFoldBlades();
      this.normalizedRotorRPM = this.sanitizeClamped(this.normalizedRotorRPM, 0.0F, 1.0F, 0.0F);
      this.collectiveInput = this.sanitizeClamped((float)throttle, 0.0F, 1.0F, 0.0F);

      double efficiency = MCH_FlightModel.getCeilingLiftFactor(super.posY, this.getAcInfo().flightCeiling, this.getAcInfo().flightCeilingRange);
      efficiency *= MCH_FlightModel.clamp(attitudeEfficiency, 0.0D, 1.0D);
      efficiency += MCH_FlightModel.clamp(horizontalSpeed / Math.max(0.1D, (double)this.getAcInfo().speed), 0.0D, 1.0D) * (double)this.heliInfo.translationalLiftCoefficient;

      boolean vortexRing = super.motionY < -0.12D && horizontalSpeed < 0.15D && this.collectiveInput > 0.45F;
      if(vortexRing) {
         efficiency *= 0.55D;
      }

      if(this.isDestroyed()) {
         efficiency *= 0.65D;
      }

      if(!this.canUseFuel(true) || !bladesUsable) {
         efficiency = 0.0D;
      }

      this.rotorEfficiency = this.sanitizeClamped((float)efficiency, 0.0F, 2.0F, 0.0F);
      float liftSpool = MathHelper.clamp_float((this.normalizedRotorRPM - 0.35F) / 0.65F, 0.0F, 1.0F);
      liftSpool *= liftSpool;
      this.rotorThrust = Math.max(this.heliInfo.mainRotorMaxThrust, 0.0F) * liftSpool * this.collectiveInput * this.rotorEfficiency;

      float pitchComponent = MathHelper.cos(this.getRotPitch() / 180.0F * 3.1415927F);
      float rollComponent = MathHelper.cos(this.getRotRoll() / 180.0F * 3.1415927F);
      float cyclicTiltMagnitudeSq = this.rotorTiltForward * this.rotorTiltForward + this.rotorTiltRight * this.rotorTiltRight;
      float diskVerticalComponent = MathHelper.sqrt_float(Math.max(0.0F, 1.0F - cyclicTiltMagnitudeSq));
      float verticalComponent = MathHelper.clamp_float(pitchComponent * rollComponent * diskVerticalComponent, 0.0F, 1.0F);
      this.rotorVerticalThrust = this.rotorThrust * verticalComponent;
      this.weightForce = mass * gravity;
      this.netVerticalForce = this.rotorVerticalThrust - this.weightForce;
      this.verticalForce = this.netVerticalForce;
      this.verticalAcceleration = this.netVerticalForce / mass;
      super.motionY += (double)(this.verticalAcceleration * tickDelta);

      float verticalDrag = MathHelper.clamp_float(this.heliInfo.verticalDrag, 0.0F, 1.0F / Math.max(tickDelta, 0.001F));
      this.verticalDragApplied = (float)(-super.motionY * (double)verticalDrag * (double)tickDelta);
      super.motionY += (double)this.verticalDragApplied;
      float maxClimbRate = this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.maxClimbRate, 0.0F, 1000.0F):0.16F;
      if(maxClimbRate > 0.0F && super.motionY > (double)maxClimbRate) {
         super.motionY = (double)maxClimbRate;
      }
      this.finalMotionY = (float)super.motionY;
   }

   private void updateNewHelicopterCyclicInput() {
      float pitchInput = 0.0F;
      float rollInput = 0.0F;

      // In the new helicopter model W/S are collective controls only.
      // Do not feed them into cyclic pitch, otherwise S creates raw
      // backward acceleration and gunner/hover paths can manufacture
      // forward speed from view/control-mode changes.
      if(super.moveRight && !super.moveLeft) {
         rollInput = 1.0F;
      } else if(super.moveLeft && !super.moveRight) {
         rollInput = -1.0F;
      }

      this.updateNewHelicopterHoverAssist(pitchInput, rollInput);
      pitchInput += this.hoverCyclicPitchCorrection;
      rollInput += this.hoverCyclicRollCorrection;

      this.cyclicPitchInput = this.sanitizeClamped(pitchInput, -1.0F, 1.0F, 0.0F);
      this.cyclicRollInput = this.sanitizeClamped(rollInput, -1.0F, 1.0F, 0.0F);
      float authority = this.heliInfo != null?this.sanitizeClamped(this.heliInfo.cyclicAuthority, 0.0F, 1.0F, 0.0F):0.0F;
      this.rotorTiltForward = MathHelper.clamp_float(this.cyclicPitchInput * authority, -1.0F, 1.0F);
      this.rotorTiltRight = MathHelper.clamp_float(this.cyclicRollInput * authority, -1.0F, 1.0F);

      float tiltMagnitudeSq = this.rotorTiltForward * this.rotorTiltForward + this.rotorTiltRight * this.rotorTiltRight;
      if(tiltMagnitudeSq > 1.0F) {
         float invMagnitude = 1.0F / MathHelper.sqrt_float(tiltMagnitudeSq);
         this.rotorTiltForward *= invMagnitude;
         this.rotorTiltRight *= invMagnitude;
      }
   }

   private void updateNewHelicopterHoverAssist(float manualPitchInput, float manualRollInput) {
      this.hoverAssistStrength = 0.0F;
      this.hoverCollectiveCorrection = 0.0F;
      this.hoverCyclicPitchCorrection = 0.0F;
      this.hoverCyclicRollCorrection = 0.0F;
      this.manualInputOverrideFactor = 0.0F;
      this.targetVerticalSpeed = 0.0F;
      this.targetHorizontalSpeed = 0.0F;
      this.localDriftForward = 0.0F;
      this.localDriftRight = 0.0F;
      this.hoverAssistActive = false;

      if(!this.newHeliFlightModelEnabled || !this.isNewHelicopterHoverAssistMode() || this.heliInfo == null) {
         this.hoverThrottleBias = 0.0F;
         return;
      }

      float configuredStrength = MathHelper.clamp_float(this.heliInfo.hoverAssistStrength, 0.0F, 1.0F);
      this.hoverAssistStrength = configuredStrength;
      if(configuredStrength <= 0.0F) {
         this.hoverThrottleBias = 0.0F;
         return;
      }

      float manualCollectiveInput = super.throttleDown?1.0F:(super.throttleUp?0.55F:0.0F);
      float manualCyclicInput = Math.max(MathHelper.abs(manualPitchInput), MathHelper.abs(manualRollInput));
      this.manualInputOverrideFactor = MathHelper.clamp_float(Math.max(manualCollectiveInput, manualCyclicInput), 0.0F, 1.0F);
      float assistBlend = configuredStrength * (1.0F - this.manualInputOverrideFactor);
      this.hoverAssistActive = assistBlend > 0.001F;
      if(!this.hoverAssistActive) {
         this.hoverThrottleBias *= 0.90F;
         this.hoverCollectiveCorrection = this.hoverThrottleBias;
         return;
      }

      this.targetVerticalSpeed = 0.0F;
      this.targetHorizontalSpeed = 0.0F;
      this.hoverCollectiveCorrection = this.hoverThrottleBias;

      float yawRadians = this.getRotYaw() / 180.0F * 3.1415927F;
      float forwardX = -MathHelper.sin(yawRadians);
      float forwardZ = MathHelper.cos(yawRadians);
      float rightX = -MathHelper.cos(yawRadians);
      float rightZ = -MathHelper.sin(yawRadians);
      this.localDriftForward = (float)(super.motionX * (double)forwardX + super.motionZ * (double)forwardZ);
      this.localDriftRight = (float)(super.motionX * (double)rightX + super.motionZ * (double)rightZ);
      float attitudeStrength = this.heliInfo != null?Math.max(this.heliInfo.hoverPitchStabilizerStrength, 0.0F):0.0F;
      float levelPitchCorrection = MathHelper.clamp_float(-this.getRotPitch() * attitudeStrength, -0.45F, 0.45F);
      float levelRollCorrection = MathHelper.clamp_float(-this.getRotRoll() * attitudeStrength, -0.45F, 0.45F);
      this.hoverCyclicPitchCorrection = MathHelper.clamp_float(-this.localDriftForward * 3.0F + levelPitchCorrection, -0.75F, 0.75F) * assistBlend;
      this.hoverCyclicRollCorrection = MathHelper.clamp_float(-this.localDriftRight * 3.0F + levelRollCorrection, -0.75F, 0.75F) * assistBlend;
   }

   private void applyNewHelicopterCyclicThrust(float tickDelta) {
      tickDelta = this.getSafeTickDelta(tickDelta);
      float mass = this.sanitizePositive(this.physicalMass, 1.0F, NEW_HELI_MIN_MASS);
      this.physicalMass = mass;
      float yawRadians = this.getRotYaw() / 180.0F * 3.1415927F;
      float forwardX = -MathHelper.sin(yawRadians);
      float forwardZ = MathHelper.cos(yawRadians);
      float rightX = -MathHelper.cos(yawRadians);
      float rightZ = -MathHelper.sin(yawRadians);

      float horizontalThrustScale = this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.horizontalRotorThrustScale, 0.0F, 1000.0F):0.35F;
      float lateralThrustScale = this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.helicopterLateralThrustScale, 0.0F, 1000.0F):0.45F;
      float backwardThrustScale = this.heliInfo != null && this.isFinite(this.heliInfo.helicopterBackwardThrustScale)?MathHelper.clamp_float(this.heliInfo.helicopterBackwardThrustScale, 0.0F, 1000.0F):1.0F;
      float attitudeTiltForward = MathHelper.clamp_float(MathHelper.sin(this.getRotPitch() / 180.0F * 3.1415927F), -0.75F, 0.75F);
      float attitudeTiltRight = MathHelper.clamp_float(-MathHelper.sin(this.getRotRoll() / 180.0F * 3.1415927F), -0.75F, 0.75F);
      float effectiveTiltForward = MathHelper.clamp_float(this.rotorTiltForward + attitudeTiltForward, -1.0F, 1.0F);
      float effectiveTiltRight = MathHelper.clamp_float(this.rotorTiltRight + attitudeTiltRight, -1.0F, 1.0F);
      float forwardThrustScale = effectiveTiltForward < 0.0F?backwardThrustScale:1.0F;
      this.backwardThrustScaled = effectiveTiltForward < 0.0F && MathHelper.abs(backwardThrustScale - 1.0F) > 0.0001F;
      this.appliedLateralThrustMultiplier = lateralThrustScale;

      float forwardThrust = this.rotorThrust * horizontalThrustScale * effectiveTiltForward * forwardThrustScale;
      float lateralThrust = this.rotorThrust * horizontalThrustScale * effectiveTiltRight * lateralThrustScale;
      this.rotorHorizontalThrustX = forwardThrust * forwardX + lateralThrust * rightX;
      this.rotorHorizontalThrustZ = forwardThrust * forwardZ + lateralThrust * rightZ;
      this.horizontalAccelerationX = this.rotorHorizontalThrustX / mass;
      this.horizontalAccelerationZ = this.rotorHorizontalThrustZ / mass;
      super.motionX += (double)(this.horizontalAccelerationX * tickDelta);
      super.motionZ += (double)(this.horizontalAccelerationZ * tickDelta);

      float forwardDrag = this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.parasiteDrag, 0.0F, 1.0F / Math.max(tickDelta, 0.001F)):0.0F;
      float lateralDrag = this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.helicopterLateralDrag, 0.0F, 1.0F / Math.max(tickDelta, 0.001F)):forwardDrag;
      float forwardVelocity = (float)(super.motionX * (double)forwardX + super.motionZ * (double)forwardZ);
      float lateralVelocity = (float)(super.motionX * (double)rightX + super.motionZ * (double)rightZ);
      float dampedForwardVelocity = forwardVelocity * (1.0F - forwardDrag * tickDelta);
      float dampedLateralVelocity = lateralVelocity * (1.0F - lateralDrag * tickDelta);
      this.parasiteDragAppliedX = (dampedForwardVelocity - forwardVelocity) * forwardX + (dampedLateralVelocity - lateralVelocity) * rightX;
      this.parasiteDragAppliedZ = (dampedForwardVelocity - forwardVelocity) * forwardZ + (dampedLateralVelocity - lateralVelocity) * rightZ;
      forwardVelocity = dampedForwardVelocity;
      lateralVelocity = dampedLateralVelocity;

      double configuredSpeed = (double)this.getAcInfo().speed;
      double speedScale = this.isHoveringMode()?NEW_HELI_HOVER_HORIZONTAL_SPEED_SCALE:NEW_HELI_REGULAR_HORIZONTAL_SPEED_SCALE;
      double minimumSafetyLimit = this.isHoveringMode()?0.35D:4.0D;
      double safetyLimit = Math.max(configuredSpeed * speedScale, minimumSafetyLimit);
      float lateralLimit = (float)(safetyLimit * (double)(this.heliInfo != null?MathHelper.clamp_float(this.heliInfo.helicopterMaxLateralSpeedScale, 0.0F, 1000.0F):0.45F));
      this.lateralSpeedCapped = lateralLimit > 0.0F && MathHelper.abs(lateralVelocity) > lateralLimit;
      if(this.lateralSpeedCapped) {
         lateralVelocity = MathHelper.clamp_float(lateralVelocity, -lateralLimit, lateralLimit);
      }
      float backwardLimitScale = this.heliInfo != null && this.isFinite(this.heliInfo.helicopterMaxBackwardSpeedScale)?MathHelper.clamp_float(this.heliInfo.helicopterMaxBackwardSpeedScale, 0.0F, 1000.0F):1.0F;
      float backwardLimit = (float)(safetyLimit * (double)backwardLimitScale);
      this.backwardSpeedCapped = forwardVelocity < -backwardLimit && MathHelper.abs(backwardLimitScale - 1.0F) > 0.0001F;
      if(this.backwardSpeedCapped) {
         forwardVelocity = -backwardLimit;
      }

      super.motionX = (double)(forwardVelocity * forwardX + lateralVelocity * rightX);
      super.motionZ = (double)(forwardVelocity * forwardZ + lateralVelocity * rightZ);
      double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if(horizontalSpeed > safetyLimit) {
         super.motionX *= safetyLimit / horizontalSpeed;
         super.motionZ *= safetyLimit / horizontalSpeed;
      }

      this.forwardVelocityComponent = (float)(super.motionX * (double)forwardX + super.motionZ * (double)forwardZ);
      this.lateralVelocityComponent = (float)(super.motionX * (double)rightX + super.motionZ * (double)rightZ);
      this.finalMotionX = (float)super.motionX;
      this.finalMotionZ = (float)super.motionZ;
   }

   private void guardNewHelicopterPhysicsState() {
      if(!this.newHeliFlightModelEnabled) {
         return;
      }

      boolean invalid = false;
      invalid |= !this.isFinite(super.motionX);
      invalid |= !this.isFinite(super.motionY);
      invalid |= !this.isFinite(super.motionZ);
      invalid |= !this.isFinite(this.getRotYaw());
      invalid |= !this.isFinite(this.getRotPitch());
      invalid |= !this.isFinite(this.getRotRoll());
      invalid |= !this.isFinite(this.heliYawAngularVelocity);
      invalid |= !this.isFinite(this.getPitchAngularVelocity());
      invalid |= !this.isFinite(this.getRollAngularVelocity());
      if(!invalid) {
         return;
      }

      String typeName = this.getTypeName();
      MCH_Lib.Log((Entity)this, "[MCHeli][NewHeliFlightModel][WARN] Invalid physics state reset for type=%s config=%s motion=(%s,%s,%s) rot=(%s,%s,%s) angular=(pitch=%s,yaw=%s,roll=%s)",
            new Object[]{typeName, this.heliInfo != null?this.heliInfo.name:"null", Double.valueOf(super.motionX), Double.valueOf(super.motionY), Double.valueOf(super.motionZ), Float.valueOf(this.getRotPitch()), Float.valueOf(this.getRotYaw()), Float.valueOf(this.getRotRoll()), Float.valueOf(this.getPitchAngularVelocity()), Float.valueOf(this.heliYawAngularVelocity), Float.valueOf(this.getRollAngularVelocity())});
      if(!this.isFinite(super.motionX)) {
         super.motionX = 0.0D;
      }
      if(!this.isFinite(super.motionY)) {
         super.motionY = 0.0D;
      }
      if(!this.isFinite(super.motionZ)) {
         super.motionZ = 0.0D;
      }
      if(!this.isFinite(this.getRotYaw())) {
         this.setRotYaw(0.0F);
      }
      if(!this.isFinite(this.getRotPitch())) {
         this.setRotPitch(0.0F);
      }
      if(!this.isFinite(this.getRotRoll())) {
         this.setRotRoll(0.0F);
      }
      if(!this.isFinite(this.heliYawAngularVelocity)) {
         this.heliYawAngularVelocity = 0.0F;
      }
      this.finalMotionX = (float)super.motionX;
      this.finalMotionY = (float)super.motionY;
      this.finalMotionZ = (float)super.motionZ;
      this.finalRotYaw = this.getRotYaw();
   }

   protected void onUpdate_Client() {
      if(this.getRiddenByEntity() != null && W_Lib.isClientPlayer(this.getRiddenByEntity())) {
         this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch;
      }

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

      if(this.isDestroyed()) {
         if(super.rotDestroyedYaw < 15.0F) {
            super.rotDestroyedYaw += 0.3F;
         }

         this.setRotYaw(this.getRotYaw() + super.rotDestroyedYaw * (float)this.getCurrentThrottle());
         if(MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
            if(MathHelper.abs(this.getRotPitch()) < 10.0F) {
               this.setRotPitch(this.getRotPitch() + super.rotDestroyedPitch);
            }

            this.setRotRoll(this.getRotRoll() + super.rotDestroyedRoll);
         }
      }

      if(this.getRiddenByEntity() != null) {
         ;
      }

      this.onUpdate_ParticleSandCloud(false);
      this.onUpdate_Particle2();
      this.updateCamera(super.posX, super.posY, super.posZ);
   }

   private void onUpdate_Server() {
      this.updateCollisionBox();
      double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      float ogp = this.getAcInfo().onGroundPitch;
      double motion;
      float speedLimit;
      float pitch;
      boolean applyNewHeliFreeFlight = this.newHeliFlightModelEnabled && super.isGunnerMode && !this.isHoveringMode();
      if(!this.isHovering() || applyNewHeliFreeFlight) {
         motion = 0.0D;
         if(this.canFloatWater()) {
            motion = this.getWaterDepth();
         }

         if(motion == 0.0D) {
            if(!this.newHeliFlightModelEnabled || this.isDestroyed()) {
               super.motionY += !this.isInWater()?(double)this.getAcInfo().gravity:(double)this.getAcInfo().gravityInWater;
            }

            speedLimit = this.getRotYaw() / 180.0F * 3.1415927F;
            pitch = this.getRotPitch();
            if(MCH_Lib.getBlockIdY(this, 3, -3) > 0) {
               pitch -= ogp;
            }

            if(!this.newHeliFlightModelEnabled) {
               super.motionX += LEGACY_HELI_REGULAR_FORWARD_ACCEL * (double)MathHelper.sin(speedLimit) * super.currentSpeed * (double)(-(pitch * pitch * pitch / 30000.0F)) * this.getCurrentThrottle();
               super.motionZ += LEGACY_HELI_REGULAR_FORWARD_ACCEL * (double)MathHelper.cos(speedLimit) * super.currentSpeed * (double)(pitch * pitch * pitch / 30000.0F) * this.getCurrentThrottle();
            }
            double y = (double)(MathHelper.abs(this.getRotPitch()) + MathHelper.abs(this.getRotRoll()));
            y *= 0.6000000238418579D;
            if(y <= 50.0D) {
               y = 1.0D - y / 50.0D;
            } else {
               y = 0.0D;
            }

            double throttle = this.getCurrentThrottle();
            if(this.isDestroyed()) {
               // A destroyed helicopter should autorotate/fall instead of feeding residual
               // throttle into lift. Keeping visual rotor throttle separate prevents the
               // death spin from turning into an upward spiral while the engine winds down.
               throttle = 0.0D;
            }

            double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
            if(this.newHeliFlightModelEnabled && !this.isDestroyed() && this.canUseBlades() && !this.isFoldBlades()) {
               this.updateNewHelicopterCyclicInput();
               this.applyNewHelicopterCollectiveLift(y, throttle, horizontalSpeed, 1.0F);
               this.applyNewHelicopterCyclicThrust(1.0F);
            } else {
               double rotorEfficiency = 1.0D;
               double translationalLift = 0.0D;
               if(this.useNewMobilitySystem()) {
                  rotorEfficiency = MCH_FlightModel.getCeilingLiftFactor(super.posY, this.getAcInfo().flightCeiling, this.getAcInfo().flightCeilingRange);

                  // Fast forward flight gives the rotor cleaner airflow (translational lift).
                  translationalLift = MCH_FlightModel.clamp(horizontalSpeed / Math.max(0.1D, (double)this.getAcInfo().speed), 0.0D, 1.0D) * 0.004D;

                  // A powered, near-vertical descent can enter a vortex-ring state. Forward
                  // motion or lowering collective lets the helicopter recover naturally.
                  boolean vortexRing = super.motionY < -0.12D && horizontalSpeed < 0.15D && throttle > 0.45D;
                  if(vortexRing) {
                     rotorEfficiency *= 0.55D;
                     super.motionY -= 0.006D;
                  }
               }

               super.motionY += ((y * 0.025D + 0.03D) * throttle + translationalLift * throttle) * rotorEfficiency;
            }
         } else {
            if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
               speedLimit = this.getRotPitch();
               speedLimit -= ogp;
               speedLimit *= 0.9F;
               speedLimit += ogp;
               this.setRotPitch(speedLimit);
            }

            if(MathHelper.abs(this.getRotRoll()) < 40.0F) {
               this.setRotRoll(this.getRotRoll() * 0.9F);
            }

            if(motion < 1.0D) {
               super.motionY -= 1.0E-4D;
               super.motionY += 0.007D * this.getCurrentThrottle();
            } else {
               if(super.motionY < 0.0D) {
                  super.motionY *= 0.7D;
               }

               super.motionY += 0.007D;
            }
         }
      } else {
         if(this.newHeliFlightModelEnabled && !this.isDestroyed() && this.canUseBlades() && !this.isFoldBlades()) {
            double throttle = this.isDestroyed()?this.getCurrentThrottle() * 0.65D:this.getCurrentThrottle();
            double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
            this.updateNewHelicopterCyclicInput();
            double hoverAttitudeEfficiency = (double)(MathHelper.cos(this.getRotPitch() / 180.0F * 3.1415927F) * MathHelper.cos(this.getRotRoll() / 180.0F * 3.1415927F));
            this.applyNewHelicopterCollectiveLift(hoverAttitudeEfficiency, throttle, horizontalSpeed, 1.0F);
            this.applyNewHelicopterCyclicThrust(1.0F);
         } else if(this.newHeliFlightModelEnabled && this.isDestroyed()) {
            // Destroyed new-flight helicopters can still be in hover mode. Do not reset
            // rotor telemetry here; let rotor RPM spool down naturally while applying
            // gravity so the aircraft falls instead of hanging or climbing.
            super.motionY += !this.isInWater()?(double)this.getAcInfo().gravity:(double)this.getAcInfo().gravityInWater;
         } else if(this.newHeliFlightModelEnabled) {
            this.resetNewHelicopterFlightTelemetry();
         }

         if(!this.newHeliFlightModelEnabled && super.rand.nextInt(50) == 0) {
            super.motionX += (super.rand.nextDouble() - 0.5D) / 30.0D;
         }

         if(!this.newHeliFlightModelEnabled && super.rand.nextInt(50) == 0) {
            super.motionY += (super.rand.nextDouble() - 0.5D) / 50.0D;
         }

         if(!this.newHeliFlightModelEnabled && super.rand.nextInt(50) == 0) {
            super.motionZ += (super.rand.nextDouble() - 0.5D) / 30.0D;
         }
      }

      if(this.isDestroyed() && super.motionY > 0.0D) {
         super.motionY *= 0.25D;
      }

      if(this.useNewMobilitySystem()) {
         double ceilingLift = MCH_FlightModel.getCeilingLiftFactor(super.posY, this.getAcInfo().flightCeiling, this.getAcInfo().flightCeilingRange);
         if(!super.onGround && ceilingLift < 1.0D) {
            if(super.motionY > 0.0D) {
               super.motionY *= 0.88D + ceilingLift * 0.12D;
            }
            super.motionY -= (1.0D - ceilingLift) * 0.014D;
         }
      }

      motion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      speedLimit = this.getAcInfo().speed;
      if(!this.newHeliFlightModelEnabled && motion > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion;
         super.motionZ *= (double)speedLimit / motion;
         motion = (double)speedLimit;
      }

      if(!this.newHeliFlightModelEnabled && motion > prevMotion && super.currentSpeed < (double)speedLimit) {
         super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
         if(super.currentSpeed > (double)speedLimit) {
            super.currentSpeed = (double)speedLimit;
         }
      } else if(!this.newHeliFlightModelEnabled) {
         super.currentSpeed -= (super.currentSpeed - 0.07D) / 35.0D;
         if(super.currentSpeed < 0.07D) {
            super.currentSpeed = 0.07D;
         }
      }

      if(super.onGround) {
         super.motionX *= 0.5D;
         super.motionZ *= 0.5D;
         if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
            pitch = this.getRotPitch();
            pitch -= ogp;
            pitch *= 0.9F;
            pitch += ogp;
            this.setRotPitch(pitch);
         }

         if(MathHelper.abs(this.getRotRoll()) < 40.0F) {
            this.setRotRoll(this.getRotRoll() * 0.9F);
         }
      }

      this.guardNewHelicopterPhysicsState();
      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      super.motionY *= 0.95D;
      super.motionX *= 0.99D;
      super.motionZ *= 0.99D;
      this.guardNewHelicopterPhysicsState();
      this.setRotation(this.getRotYaw(), this.getRotPitch());
      this.onUpdate_updateBlock();
      this.handleDeadPilot();

   }

   private void collisionEntity(AxisAlignedBB bb) {
      if (bb != null) {
         // Calculate speed
         double speed = Math.sqrt(super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ);

         if (speed > 0.05D) {
            Entity rider = this.getRiddenByEntity();
            float damage = (float)(speed * 15.0D);

            // Get the aircraft entity the plane is riding on, if applicable
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
                  if (e != rideAc && !(e instanceof EntityItem) && !(e instanceof EntityXPOrb && !(e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff)
                          && !(e instanceof MCH_EntityBaseBullet) && !(e instanceof MCH_EntityChain)
                          && !(e instanceof MCH_EntitySeat)) ) {

                     // Special handling for planes
                     //if (e instanceof MCP_EntityPlane) {
                     //   MCP_EntityPlane plane = (MCP_EntityPlane) e;
                     //todo
                     //if (plane.getPlaneInfo() != null && plane.getPlaneInfo().weightType == 2) {
                     //   return MCH_Config.Collision_EntityTankDamage.prmBool;
                     //}
                     //todo: fix up how this works as in collision because this is not fair to xradar perms/block protection
                     // }

                     // Default collision entity damage
                     if (e instanceof MCH_EntityBaseVehicle) {
                        return MCH_Config.Collision_EntityDamage.prmBool;
                     }
                     //how does this singular if statement fix everything I can't with this fucking mod
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
                     e.motionX += super.motionX * 1.5D;
                     e.motionZ += super.motionZ * 1.5D;
                  }

                  // Damage self based on collision with large entities
                  if ( (e.width >= 1.0F || e.height >= 1.5D)) { //this.getPlaneInfo().weightType != 2 &&
                     ds = (e instanceof EntityLivingBase)
                             ? DamageSource.causeMobDamage((EntityLivingBase) e)
                             : DamageSource.generic;

                     this.attackEntityFrom(ds, damage / 3.0F);
                  }

                  // Log the collision
                  MCH_Lib.DbgLog(super.worldObj, "MCH_EntityHeli.collisionEntity damage=%.1f %s", damage, e.toString());
               }
            }
         }
      }
   }

   private boolean shouldCollisionDamage(Entity e) {

      if(e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff) {
         return false;
      }
      //please stop fucking colliding with flares and chaffs you fucking retarded ass mod i swear to fuck

      if(this.getSeatIdByEntity(e) >= 0) {
         return false;
      } else if(super.noCollisionEntities.containsKey(e)) {
         return false;
      } else {
         if(e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox)e).parent != null ) { //|| e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff
            //cannot cast these to aircraft because fuck you lollll!!!!
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
         //this.WheelMng.updateBlock();
         MCH_BoundingBox[] arr$ = this.getCalculatedExtraBoundingBoxes();
         int len$ = arr$.length;

         MCH_Config var10000;
         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_BoundingBox bb = arr$[i$];
            if(super.rand.nextInt(3) == 0) {
               var10000 = MCH_MOD.config;
               //todo config
               //if(MCH_Config.Collision_DestroyBlock.prmBool) {
               //   Vec3 v = this.getTransformedPosition(bb.offsetX, bb.offsetY, bb.offsetZ);
               //this.destoryBlockRange(v, (double)bb.width, (double)bb.height);
               //}

               this.collisionEntity(bb.boundingBox);
            }
         }

         var10000 = MCH_MOD.config;
         //todo config
         //if(MCH_Config.Collision_DestroyBlock.prmBool) {
         //this.destoryBlockRange(this.getTransformedPosition(0.0D, 0.0D, 0.0D), (double)super.width * 1.5D, (double)(super.height * 2.0F));
         //}

         this.collisionEntity(this.getBoundingBox());
      }
   }


}
