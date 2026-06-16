package mcheli.plane;

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
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
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

public class MCP_EntityPlane extends MCH_EntityBaseVehicle {

   private static final float PLANE_MANEUVERABILITY_FACTOR = 1.5F;

   private MCP_PlaneInfo planeInfo = null;
   public float soundVolume;
   public MCH_Parts partNozzle;
   public MCH_Parts partWing;
   public float rotationRotor;
   public float prevRotationRotor;
   public float addkeyRotValue;
   /** Smoothed engine output; commanded throttle remains unchanged for controls and networking. */
   private double engineThrottle;
   /** Last total drag fraction applied by the fixed-wing energy model, exposed for debug output. */
   private double lastAerodynamicDrag;
   /** Last stall lift-loss fraction applied to vertical motion, exposed for debug output. */
   private double lastLiftLoss;
   /** Last downward gravity acceleration applied by the new fixed-wing model. */
   private double lastGravityAcceleration;
   /** Last upward lift acceleration applied by the new fixed-wing model. */
   private double lastLiftAcceleration;
   /** Last net fixed-wing vertical acceleration before velocity damping. */
   private double lastNetVerticalAcceleration;
   /** Last calculated physical weight force for new-flight debug output. */
   private double lastWeightForce;
   /** Last calculated lift force for new-flight debug output. */
   private double lastLiftForce;
   /** Last calculated lift force before stall lift loss, exposed for stall tuning. */
   private double lastLiftForceBeforeStallLoss;
   /** Last calculated lift force after stall lift loss, exposed for stall tuning. */
   private double lastLiftForceAfterStallLoss;
   /** Last calculated engine thrust force for new-flight debug output. */
   private double lastEngineThrustForce;
   /** Last net forward acceleration after thrust and energy effects. */
   private double lastNetForwardAcceleration;
   /** Base stall/takeoff speed before runway-distance scaling. */
   private double lastBaseTakeoffSpeed;
   /** Effective takeoff speed after TakeoffDistanceMultiplier. */
   private double lastEffectiveTakeoffSpeed;
   /** True when takeoff threshold scaling is actively gating/assisting rotation. */
   private boolean lastTakeoffMultiplierActive;
   /** True when stall state suppressed takeoff/climb lift headroom this tick. */
   private boolean lastStallSuppressedLiftHeadroom;
   /** Debug validity flags for takeoff and climb lift headroom. */
   private boolean lastValidTakeoff;
   private boolean lastValidClimb;
   /** Last stall pitch-break angular velocity applied through the normal rotation path. */
   private double lastPitchBreakAngularVelocity;
   /** Last total nose-down stall pitch moment applied for debug output. */
   private double lastStallPitchMoment;
   /** Last throttle-deficit nose-down pitch moment applied for debug output. */
   private double lastThrustPitchDownMoment;
   /** Last lift coefficient multiplier after AoA and stall lift loss. */
   private double lastLiftCoefficient;
   /** True when current stall state is blending back toward normal flight. */
   private boolean stallRecovering;
   /** Fraction of pilot nose-up pitch input currently suppressed by energy/stall state. */
   private double lastNoseUpPitchSuppression;
   /** Last calculated unsupported-climb severity used by energy and pitch protection. */
   private double lastUnsupportedClimbSeverity;
   /** True when the idle-throttle sanity guard detected a low-energy nose-high climb. */
   private boolean lastIdleUnsupportedClimb;
   /** Last idle-throttle warning reason emitted through debug output. */
   private String lastIdleThrottleWarning;
   /** Last combined horizontal speed used by low-speed stall diagnostics. */
   private double lastHorizontalSpeed;
   /** Last low-horizontal-speed warning reason emitted through debug output. */
   private String lastLowHorizontalSpeedWarning;
   /** Last compressibility-limited pitch authority multiplier. */
   private double lastPitchAuthority;
   /** Last full control authority multiplier applied before pitch-axis modifiers. */
   private double lastControlAuthority;
   /** Last airborne state used by the new fixed-wing force model. */
   private boolean lastAirborne;
   /** Local-axis body rates used by fixed-wing damped control response. */
   private float pitchAngularVelocity;
   private float rollAngularVelocity;
   private float yawAngularVelocity;
   /** Latest approximate fixed-wing load factor. */
   private double currentGForce = 1.0D;
   /** Fractional overspeed damage retained between ticks. */
   private double overspeedDamageAccumulator;
   /** Current unsigned angle between the nose and velocity vectors, in degrees. */
   private double angleOfAttack;
   /** Latest speed-derived stall demand before smoothing. */
   private double speedStallSeverity;
   /** Latest AoA-derived stall demand before smoothing. */
   private double aoaStallSeverity;
   /** Latest max(speed, AoA) stall demand before smoothing. */
   private double stallDemand;
   /** True when deterministic stall pitch break was applied this tick. */
   private boolean pitchBreakActive;
   /** Smoothed stall state used by lift loss, controls, and instability. */
   private double stallSeverity;
   private boolean stalling;
   private boolean combatFlapsDeployed;

   private static class AeroState {
      boolean airborne;
      boolean vtolMode;
      double horizontalSpeed;
      double airspeed;
      double forwardSpeed;
      double verticalSpeed;

      double pitchDeg;
      double noseUpAttitude;
      double aoaDeg;
      double signedAoADeg;

      double stallSpeed;
      double stallRecoverySpeed;
      double speedSeverity;
      double aoaSeverity;
      double stallDemand;
      double stallSeverity;
      double instantStallSeverity;

      double gravityForce;
      double weightForce;
      double liftForce;
      double liftToWeight;

      double thrustForce;
      double thrustToWeight;

      double climbSustainSpeed;
      double climbEnergyDeficit;
      double unsupportedNoseUpSeverity;

      double controlAuthority;
      double pitchAuthority;
      double noseUpPitchSuppression;

      boolean validTakeoff;
      boolean validClimb;
   }

   private AeroState aeroState = new AeroState();


   public MCP_EntityPlane(World world) {
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
      this.partNozzle = null;
      this.partWing = null;
      super.stepHeight = 0.6F;
      this.rotationRotor = 0.0F;
      this.prevRotationRotor = 0.0F;
      this.engineThrottle = 0.0D;
      this.lastAerodynamicDrag = 0.0D;
      this.lastLiftLoss = 0.0D;
      this.lastGravityAcceleration = 0.0D;
      this.lastLiftAcceleration = 0.0D;
      this.lastNetVerticalAcceleration = 0.0D;
      this.lastWeightForce = 0.0D;
      this.lastLiftForce = 0.0D;
      this.lastLiftForceBeforeStallLoss = 0.0D;
      this.lastLiftForceAfterStallLoss = 0.0D;
      this.lastEngineThrustForce = 0.0D;
      this.speedStallSeverity = 0.0D;
      this.aoaStallSeverity = 0.0D;
      this.stallDemand = 0.0D;
      this.pitchBreakActive = false;
      this.lastNetForwardAcceleration = 0.0D;
      this.lastBaseTakeoffSpeed = 0.0D;
      this.lastEffectiveTakeoffSpeed = 0.0D;
      this.lastTakeoffMultiplierActive = false;
      this.lastStallSuppressedLiftHeadroom = false;
      this.lastValidTakeoff = false;
      this.lastValidClimb = false;
      this.lastIdleUnsupportedClimb = false;
      this.lastIdleThrottleWarning = "";
      this.lastHorizontalSpeed = 0.0D;
      this.lastLowHorizontalSpeedWarning = "";
      this.lastPitchBreakAngularVelocity = 0.0D;
      this.lastStallPitchMoment = 0.0D;
      this.lastThrustPitchDownMoment = 0.0D;
      this.lastLiftCoefficient = 0.0D;
      this.stallRecovering = false;
      this.lastNoseUpPitchSuppression = 0.0D;
      this.lastUnsupportedClimbSeverity = 0.0D;
      this.lastPitchAuthority = 1.0D;
      this.lastControlAuthority = 1.0D;
      this.lastAirborne = false;
      this.angleOfAttack = 0.0D;
      this.stallSeverity = 0.0D;
      this.stalling = false;
      this.combatFlapsDeployed = false;
      this.aeroState = new AeroState();
   }

   public String getKindName() {
      return "planes";
   }

   public String getEntityType() {
      return "Plane";
   }

   public MCP_PlaneInfo getPlaneInfo() {
      return this.planeInfo;
   }

   public void changeType(String type) {
      if(!type.isEmpty()) {
         this.planeInfo = MCP_PlaneInfoManager.get(type);
      }

      if(this.planeInfo == null) {
         MCH_Lib.Log((Entity)this, "##### MCP_EntityPlane changePlaneType() Plane info null %d, %s, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), type, this.getEntityName()});
         this.setDead();
      } else {
         this.setAcInfo(this.planeInfo);
         this.newSeats(this.getAcInfo().getNumSeatAndRack());
         this.partNozzle = this.createNozzle(this.planeInfo);
         this.partWing = this.createWing(this.planeInfo);
         super.weapons = this.createWeapon(1 + this.getSeatNum());
         this.initPartRotation(this.getRotYaw(), this.getRotPitch());
      }

   }

   public Item getItem() {
      return this.getPlaneInfo() != null?this.getPlaneInfo().item:null;
   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MountMinecartPlane.prmBool;
   }

   protected void entityInit() {
      super.entityInit();
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      if(this.planeInfo == null) {
         this.planeInfo = MCP_PlaneInfoManager.get(this.getTypeName());
         if(this.planeInfo == null) {
            MCH_Lib.Log((Entity)this, "##### MCP_EntityPlane readEntityFromNBT() Plane info null %d, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName()});
            this.setDead();
         } else {
            this.setAcInfo(this.planeInfo);
         }
      }

   }

   public void setDead() {
      super.setDead();
   }

   public int getNumEjectionSeat() {
      if(this.getAcInfo() != null && this.getAcInfo().isEnableEjectionSeat) {
         int n = this.getSeatNum() + 1;
         return n <= 2?n:0;
      } else {
         return 0;
      }
   }

   public void onInteractFirst(EntityPlayer player) {
      this.addkeyRotValue = 0.0F;
   }

   public boolean canSwitchGunnerMode() {
      if(!super.canSwitchGunnerMode()) {
         return false;
      } else {
         float roll = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotRoll()));
         float pitch = MathHelper.abs(MathHelper.wrapAngleTo180_float(this.getRotPitch()));
         return roll <= 40.0F && pitch <= 40.0F?this.getCurrentThrottle() > 0.6000000238418579D && MCH_Lib.getBlockIdY(this, 3, -5) == 0:false;
      }
   }

   public void onUpdateAircraft() {
      if(this.planeInfo == null) {
         this.changeType(this.getTypeName());
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
      } else {
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
         this.onUpdate_Control();
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

         if(super.onGround && this.getVtolMode() == 0 && this.planeInfo.isDefaultVtol) {
            this.swithVtolMode(true);
         }

         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ;
         if(!this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < 70.0F) {
            this.setRotPitch(this.getRotPitch() * 0.95F, "isHovering()");
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

   public boolean canUpdateYaw(Entity player) {
      return super.canUpdateYaw(player) && !this.isHovering();
   }

   public boolean canUpdatePitch(Entity player) {
      return super.canUpdatePitch(player) && !this.isHovering();
   }

   public boolean canUpdateRoll(Entity player) {
      return super.canUpdateRoll(player) && !this.isHovering();
   }

   public float getYawFactor() {
      float yaw = this.getVtolMode() > 0?this.getPlaneInfo().vtolYaw:super.getYawFactor();
      return yaw * 0.8F * (this.useNewMobilitySystem() ? PLANE_MANEUVERABILITY_FACTOR : 1.0F);
   }

   public float getPitchFactor() {
      float pitch = this.getVtolMode() > 0?this.getPlaneInfo().vtolPitch:super.getPitchFactor();
      return pitch * 0.8F * (this.useNewMobilitySystem() ? PLANE_MANEUVERABILITY_FACTOR : 1.0F);
   }

   public float getRollFactor() {
      float roll = this.getVtolMode() > 0?this.getPlaneInfo().vtolYaw:super.getRollFactor();
      return roll * 0.8F * (this.useNewMobilitySystem() ? PLANE_MANEUVERABILITY_FACTOR : 1.0F);
   }

   public boolean isOverridePlayerPitch() {
      return super.isOverridePlayerPitch() && !this.isHovering();
   }

   public boolean isOverridePlayerYaw() {
      return super.isOverridePlayerYaw() && !this.isHovering();
   }

   public float getControlRotYaw(float mouseX, float mouseY, float tick) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.MouseControlFlightSimMode.prmBool) {
         this.rotationByKey(tick);
         return this.addkeyRotValue * 20.0F;
      } else {
         return mouseX;
      }
   }

   public float getControlRotPitch(float mouseX, float mouseY, float tick) {
      return mouseY;
   }

   public float getControlRotRoll(float mouseX, float mouseY, float tick) {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MouseControlFlightSimMode.prmBool?mouseX * 2.0F:(this.getVtolMode() == 0?mouseX * 0.5F:mouseX);
   }

   private void rotationByKey(float partialTicks) {
      float rot = 0.2F;
      MCH_Config var10000 = MCH_MOD.config;
      if(!MCH_Config.MouseControlFlightSimMode.prmBool && this.getVtolMode() != 0) {
         rot *= 0.0F;
      }

      if(super.moveLeft && !super.moveRight) {
         this.addkeyRotValue -= rot * partialTicks;
      }

      if(super.moveRight && !super.moveLeft) {
         this.addkeyRotValue += rot * partialTicks;
      }

   }

   protected float getControlAuthorityFactor() {
      if(!this.useNewMobilitySystem()) {
         return 1.0F;
      }

      if(this.getPlaneInfo() == null || this.getNozzleRotation() > 0.01F || this.onGround) {
         return 1.0F;
      }

      MCP_PlaneInfo info = this.getPlaneInfo();
      double highGAuthority = MCH_FlightModel.getHighGControlAuthority(this.currentGForce,
            info.maxComfortableG, info.maxStructuralG, info.gControlPenalty);
      double instantSeverity = this.getInstantStallSeverity();
      double severity = Math.max(this.aeroState != null ? this.aeroState.stallSeverity : this.stallSeverity, instantSeverity);
      double flapAuthority = this.isCombatFlapsDeployed() ? 1.0D + (double)info.newFlightCombatFlapControl : 1.0D;
      float authority = (float)MCH_FlightModel.clamp(highGAuthority * MCH_FlightModel.getControlAuthority(severity)
            * flapAuthority, 0.05D, 1.35D);
      if(this.aeroState != null) {
         this.aeroState.instantStallSeverity = instantSeverity;
         this.aeroState.controlAuthority = authority;
      }
      return authority;
   }

   private double getUnsupportedClimbSeverity() {
      return this.aeroState != null ? this.aeroState.unsupportedNoseUpSeverity : 0.0D;
   }



   private boolean isIdleUnsupportedClimb(double noseUpAttitude, double stallSpeed) {
      AeroState state = this.aeroState;
      return state != null && state.airborne && !state.vtolMode && this.getPropulsiveEngineThrottle() <= 0.01D
            && noseUpAttitude > 0.05D && state.airspeed < state.stallRecoverySpeed;
   }


   private double getNoseUpPitchSuppression() {
      AeroState state = this.aeroState;
      return state != null ? MCH_FlightModel.clamp(state.unsupportedNoseUpSeverity, 0.0D, 1.0D) : 0.0D;
   }


   public double getCurrentGForce() {
      return this.currentGForce;
   }

   public float getPitchAngularVelocity() {
      return this.pitchAngularVelocity;
   }

   public float getRollAngularVelocity() {
      return this.rollAngularVelocity;
   }

   public float getYawAngularVelocity() {
      return this.yawAngularVelocity;
   }

   public double getAngleOfAttackDegrees() {
      return this.angleOfAttack;
   }

   public double getStallSeverity() {
      return this.stallSeverity;
   }

   public double getSpeedStallSeverity() {
      return this.speedStallSeverity;
   }

   public double getAoAStallSeverity() {
      return this.aoaStallSeverity;
   }

   public double getStallDemand() {
      return this.stallDemand;
   }

   public double getCriticalAoA() {
      return this.getPlaneInfo() != null ? (double)this.getPlaneInfo().criticalAoA : 0.0D;
   }

   public boolean isPitchBreakActive() {
      return this.pitchBreakActive;
   }

   public double getLastAerodynamicDrag() {
      return this.lastAerodynamicDrag;
   }

   public double getLastLiftLoss() {
      return this.lastLiftLoss;
   }

   public double getLastGravityAcceleration() {
      return this.lastGravityAcceleration;
   }

   public double getLastLiftAcceleration() {
      return this.lastLiftAcceleration;
   }

   public double getLastNetVerticalAcceleration() {
      return this.lastNetVerticalAcceleration;
   }

   public double getPhysicalMass() {
      MCP_PlaneInfo info = this.getPlaneInfo();
      return info != null ? Math.max(0.05D, (double)info.physicalMass) : 1.0D;
   }

   public double getLastWeightForce() {
      return this.lastWeightForce;
   }

   public double getLastLiftForce() {
      return this.lastLiftForce;
   }

   public double getLastLiftForceBeforeStallLoss() {
      return this.lastLiftForceBeforeStallLoss;
   }

   public double getLastLiftForceAfterStallLoss() {
      return this.lastLiftForceAfterStallLoss;
   }

   public double getLastEngineThrustForce() {
      return this.lastEngineThrustForce;
   }

   public double getLiftToWeightRatio() {
      return this.lastWeightForce > 1.0E-6D ? this.lastLiftForce / this.lastWeightForce : 0.0D;
   }

   public double getThrustToWeightRatio() {
      return this.lastEngineThrustForce / Math.max(this.lastWeightForce, 1.0E-6D);
   }

   public double getLastNetForwardAcceleration() {
      return this.lastNetForwardAcceleration;
   }

   public double getTakeoffDistanceMultiplier() {
      MCP_PlaneInfo info = this.getPlaneInfo();
      return info != null ? MCH_FlightModel.clamp((double)info.takeoffDistanceMultiplier, 0.25D, 4.0D) : 1.0D;
   }

   public double getLastBaseTakeoffSpeed() {
      return this.lastBaseTakeoffSpeed;
   }

   public double getLastEffectiveTakeoffSpeed() {
      return this.lastEffectiveTakeoffSpeed;
   }

   public boolean isLastTakeoffMultiplierActive() {
      return this.lastTakeoffMultiplierActive;
   }

   public boolean isLastStallSuppressedLiftHeadroom() {
      return this.lastStallSuppressedLiftHeadroom;
   }

   public boolean isLastValidTakeoff() {
      return this.lastValidTakeoff;
   }

   public boolean isLastValidClimb() {
      return this.lastValidClimb;
   }

   public double getLastPitchBreakAngularVelocity() {
      return this.lastPitchBreakAngularVelocity;
   }

   public double getLastStallPitchMoment() {
      return this.lastStallPitchMoment;
   }

   public double getLastThrustPitchDownMoment() {
      return this.lastThrustPitchDownMoment;
   }

   public double getLastLiftCoefficient() {
      return this.lastLiftCoefficient;
   }

   public boolean isStallRecovering() {
      return this.stallRecovering;
   }

   public double getLastNoseUpPitchSuppression() {
      return this.lastNoseUpPitchSuppression;
   }

   public double getLastUnsupportedClimbSeverity() {
      return this.lastUnsupportedClimbSeverity;
   }

   public boolean isLastIdleUnsupportedClimb() {
      return this.lastIdleUnsupportedClimb;
   }

   public String getLastIdleThrottleWarning() {
      return this.lastIdleThrottleWarning;
   }

   public double getLastHorizontalSpeed() {
      return this.lastHorizontalSpeed;
   }

   public String getLastLowHorizontalSpeedWarning() {
      return this.lastLowHorizontalSpeedWarning;
   }

   public boolean isUnsupportedClimb() {
      return this.lastUnsupportedClimbSeverity > 1.0E-3D;
   }

   public double getLastPitchAuthority() {
      return this.lastPitchAuthority;
   }

   public double getLastControlAuthority() {
      return this.lastControlAuthority;
   }


   public boolean isLastAirborne() {
      return this.lastAirborne;
   }

   public double getResolvedNewFlightGravity() {
      return this.resolveNewFlightGravity();
   }

   public boolean isUsingNewFlightGravityOverride() {
      return this.getPlaneInfo() != null && !Float.isNaN(this.getPlaneInfo().newFlightGravity);
   }

   public float getDebugControlAuthority() {
      return this.getControlAuthorityFactor();
   }

   public double getAirspeed() {
      return Math.sqrt(super.motionX * super.motionX + super.motionY * super.motionY
            + super.motionZ * super.motionZ);
   }

   public boolean isNewFlightModelEnabled() {
      return this.useNewMobilitySystem();
   }

   public double getNormalizedThrottle() {
      return MCH_FlightModel.clamp(this.getCurrentThrottle(), 0.0D, 1.0D);
   }

   public int getThrottlePercent() {
      return (int)Math.round(this.getNormalizedThrottle() * 100.0D);
   }

   public boolean canUseCombatFlaps() {
      return this.useNewMobilitySystem() && this.getPlaneInfo() != null && this.getPlaneInfo().newFlightCombatFlaps
            && this.getNozzleRotation() <= 0.01F;
   }

   public boolean isCombatFlapsDeployed() {
      return this.canUseCombatFlaps() && this.combatFlapsDeployed;
   }

   public void setCombatFlapsDeployed(boolean deployed) {
      this.combatFlapsDeployed = deployed && this.canUseCombatFlaps();
   }

   public void toggleCombatFlaps() {
      this.setCombatFlapsDeployed(!this.combatFlapsDeployed);
   }

   public boolean isOverspeeding() {
      return this.useNewMobilitySystem() && MCH_FlightModel.getOverspeedSeverity(this.getAirspeed(), this.getMaxSafeSpeed()) > 0.0D;
   }

   protected double getCompressibilitySpeed() {
      MCP_PlaneInfo info = this.getPlaneInfo();
      if(info == null) {
         return 0.0D;
      }

      float levelSpeed = info.maxLevelSpeed > 0.0F ? info.maxLevelSpeed : this.getMaxSpeed();
      return info.compressibilitySpeed > 0.0F ? (double)info.compressibilitySpeed : (double)levelSpeed * 0.9D;
   }

   protected double getMaxSafeSpeed() {
      MCP_PlaneInfo info = this.getPlaneInfo();
      if(info == null) {
         return 0.0D;
      }

      float levelSpeed = info.maxLevelSpeed > 0.0F ? info.maxLevelSpeed : this.getMaxSpeed();
      double safeSpeed = info.maxSafeSpeed > 0.0F ? (double)info.maxSafeSpeed : (double)levelSpeed * 1.1D;
      if(this.isCombatFlapsDeployed()) {
         safeSpeed *= (double)info.newFlightCombatFlapOverspeed;
      }
      return safeSpeed;
   }

   private double getInstantStallSeverity() {
      if(this.getPlaneInfo() == null) {
         return 0.0D;
      }

      Vec3 forward = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch());
      double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      double speed = Math.sqrt(horizontalSpeed * horizontalSpeed + super.motionY * super.motionY);
      double aoa = MCH_FlightModel.getAngleOfAttackDegrees(forward.xCoord, forward.yCoord, forward.zCoord,
            super.motionX, super.motionY, super.motionZ);
      double stallSpeed = MCH_FlightModel.getStallSpeed(this.getPlaneInfo().stallSpeed, this.getMaxSpeed(),
            this.getPlaneInfo().stallSpeedFactor);
      double speedSeverity = Math.max(MCH_FlightModel.getSpeedStallSeverity(speed, stallSpeed),
            MCH_FlightModel.getSpeedStallSeverity(horizontalSpeed, stallSpeed));
      return Math.max(speedSeverity, MCH_FlightModel.getAoAStallSeverity(aoa, this.getPlaneInfo().criticalAoA));
   }


   public void setAngles(Entity player, boolean fixRot, float fixYaw, float fixPitch, float deltaX, float deltaY, float x, float y, float partialTicks) {
      if(!this.useNewMobilitySystem()) {
         super.setAngles(player, fixRot, fixYaw, fixPitch, deltaX, deltaY, x, y, partialTicks);
         return;
      }

      MCP_PlaneInfo planeInfo = this.getPlaneInfo();
      if(planeInfo == null) {
         super.setAngles(player, fixRot, fixYaw, fixPitch, deltaX, deltaY, x, y, partialTicks);
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
      double pitchAuthority = MCH_FlightModel.getCompressibilityPitchAuthority(this.getAirspeed(),
            this.getCompressibilitySpeed(), this.getMaxSafeSpeed(), this.getPlaneInfo().compressibilityPitchPenalty);
      this.lastControlAuthority = controlAuthority;
      this.lastPitchAuthority = pitchAuthority;
      if(this.aeroState != null) {
         this.aeroState.controlAuthority = controlAuthority;
         this.aeroState.pitchAuthority = pitchAuthority;
      }
      pitch *= controlAuthority * (float)pitchAuthority;
      this.lastNoseUpPitchSuppression = this.getNoseUpPitchSuppression();
      if(this.aeroState != null) {
         this.aeroState.noseUpPitchSuppression = this.lastNoseUpPitchSuppression;
      }
      if(pitch < 0.0F && this.lastNoseUpPitchSuppression > 0.0D) {
         pitch *= (float)(1.0D - this.lastNoseUpPitchSuppression);
      }
      roll *= controlAuthority;
      yaw *= controlAuthority;

      // The legacy controls above still define the requested angular rate.
      // Integrating that request as a damped body rate retains existing mobility
      // tuning while preventing the airframe from snapping to every mouse movement.
      MCP_PlaneInfo info = this.getPlaneInfo();
      this.pitchAngularVelocity = MCH_FlightModel.updateAngularVelocity(this.pitchAngularVelocity, pitch,
            info.pitchTorque, info.pitchDamping, info.inertiaMultiplier, partialTicks);
      this.rollAngularVelocity = MCH_FlightModel.updateAngularVelocity(this.rollAngularVelocity, roll,
            info.rollTorque, info.rollDamping, info.inertiaMultiplier, partialTicks);
      this.yawAngularVelocity = MCH_FlightModel.updateAngularVelocity(this.yawAngularVelocity, yaw,
            info.yawTorque, info.yawDamping, info.inertiaMultiplier, partialTicks);
      pitch = this.pitchAngularVelocity * partialTicks;
      roll = this.rollAngularVelocity * partialTicks;
      yaw = this.yawAngularVelocity * partialTicks;

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

   private void updateAerodynamicState() {
      AeroState state = new AeroState();
      MCP_PlaneInfo info = this.getPlaneInfo();
      state.airborne = !super.onGround && MCH_Lib.getBlockIdY(this, 1, -2) == 0;
      state.vtolMode = this.getNozzleRotation() > 0.01F;
      state.horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      state.verticalSpeed = super.motionY;
      state.airspeed = Math.sqrt(state.horizontalSpeed * state.horizontalSpeed + state.verticalSpeed * state.verticalSpeed);
      state.pitchDeg = this.getRotPitch();
      state.noseUpAttitude = MCH_FlightModel.clamp((double)(-this.getRotPitch()) / 45.0D, 0.0D, 1.0D);
      Vec3 forward = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch());
      state.forwardSpeed = super.motionX * forward.xCoord + super.motionY * forward.yCoord + super.motionZ * forward.zCoord;
      state.aoaDeg = MCH_FlightModel.getAngleOfAttackDegrees(forward.xCoord, forward.yCoord, forward.zCoord,
            super.motionX, super.motionY, super.motionZ);
      state.signedAoADeg = MCH_FlightModel.getSignedPitchAoADegrees(forward.xCoord, forward.yCoord, forward.zCoord,
            super.motionX, super.motionY, super.motionZ);

      if(info != null) {
         state.stallSpeed = MCH_FlightModel.getStallSpeed(info.stallSpeed, this.getMaxSpeed(), info.stallSpeedFactor);
         state.stallRecoverySpeed = info.stallRecoverySpeed > 0.0F ? (double)info.stallRecoverySpeed : state.stallSpeed * 1.2D;
         state.speedSeverity = MCH_FlightModel.getSpeedStallSeverity(state.airspeed, state.stallSpeed);
         state.aoaSeverity = MCH_FlightModel.getAoAStallSeverity(state.aoaDeg, info.criticalAoA);
         state.stallDemand = Math.max(state.speedSeverity, state.aoaSeverity);

         boolean safeRecoverySpeed = state.airspeed >= state.stallRecoverySpeed;
         boolean safeRecoveryAoA = Math.abs(state.aoaDeg) <= (double)info.criticalAoA * 0.75D;
         if(this.stalling) {
            if(safeRecoverySpeed && safeRecoveryAoA) {
               this.stalling = false;
            }
         } else if(state.stallDemand > 0.03D) {
            this.stalling = true;
         }

         double targetSeverity = this.stalling ? Math.max(0.12D, state.stallDemand) : 0.0D;
         double recoveryRate = MCH_FlightModel.clamp((double)info.stallRecoveryRate, 0.01D, 1.0D);
         this.stallSeverity += (targetSeverity - this.stallSeverity)
               * (targetSeverity > this.stallSeverity ? 0.35D : recoveryRate);
         this.stallRecovering = !this.stalling && this.stallSeverity > 0.0D;
         if(!this.stalling && this.stallSeverity < 1.0E-3D) {
            this.stallSeverity = 0.0D;
            this.stallRecovering = false;
         }
         state.stallSeverity = this.stallSeverity;
         state.instantStallSeverity = state.stallDemand;

         double gravityAccel = this.resolveNewFlightGravity();
         if(this.isInWater()) {
            gravityAccel = Math.max(0.0D, Math.min(gravityAccel, -(double)this.getAcInfo().gravityInWater));
         }
         double mass = this.getPhysicalMass();
         state.gravityForce = gravityAccel;
         state.weightForce = gravityAccel * mass;

         double liftPower = (double)info.newFlightLowThrottleLiftRetention
               + (1.0D - (double)info.newFlightLowThrottleLiftRetention) * this.getEffectiveEngineThrottle();
         if(this.isCombatFlapsDeployed()) {
            liftPower += (double)info.newFlightCombatFlapLift;
         }
         boolean nearGround = super.onGround || MCH_Lib.getBlockIdY(this, 3, -5) > 0;
         double takeoffMultiplier = this.getTakeoffDistanceMultiplier();
         this.lastBaseTakeoffSpeed = state.stallSpeed;
         this.lastEffectiveTakeoffSpeed = state.stallSpeed * takeoffMultiplier;
         this.lastTakeoffMultiplierActive = nearGround && Math.abs(takeoffMultiplier - 1.0D) > 1.0E-4D
               && state.forwardSpeed >= 0.0D && state.horizontalSpeed < state.stallSpeed * 1.15D;
         state.validTakeoff = nearGround && !state.vtolMode && state.forwardSpeed >= this.lastEffectiveTakeoffSpeed
               && state.stallSeverity < 0.20D && state.aoaSeverity < 0.30D;
         if(state.validTakeoff) {
            double runwayReadiness = MCH_FlightModel.clamp((state.horizontalSpeed - this.lastEffectiveTakeoffSpeed)
                  / Math.max(0.05D, state.stallSpeed * 0.35D), 0.0D, 1.0D);
            liftPower += 0.35D * runwayReadiness;
         }

         // Fixed-wing lift is a product of dynamic pressure and wing AoA. Pitch
         // alone no longer creates upward momentum: low airspeed, negative AoA, or
         // separated flow cannot support the aircraft even if the nose is high.
         double dynamicPressureLift = MCH_FlightModel.clamp((state.airspeed * state.airspeed)
               / Math.max(0.0025D, state.stallRecoverySpeed * state.stallRecoverySpeed), 0.0D, 1.45D);
         double aoaLift = MCH_FlightModel.getLiftCurveCoefficient(state.signedAoADeg, info.criticalAoA, state.stallSeverity);
         double liftLoss = MCH_FlightModel.clamp(state.stallSeverity * (double)info.stallLiftLoss, 0.0D, 1.0D);
         double liftBeforeStallLoss = state.weightForce * MCH_FlightModel.clamp(liftPower, 0.0D, 2.5D)
               * dynamicPressureLift * aoaLift;
         state.liftForce = liftBeforeStallLoss * (1.0D - liftLoss);
         state.liftToWeight = state.liftForce / Math.max(state.weightForce, 1.0E-6D);
         state.thrustForce = Math.max(0.0D, (double)info.engineThrust * MCH_FlightModel.clamp(this.getPropulsiveEngineThrottle(), 0.0D, 1.0D));
         state.thrustToWeight = state.thrustForce / Math.max(state.weightForce, 1.0E-6D);
         state.climbSustainSpeed = state.stallRecoverySpeed;
         double liftDeficit = MCH_FlightModel.clamp(1.0D - state.liftToWeight, 0.0D, 1.0D);
         double thrustDeficit = MCH_FlightModel.clamp(1.0D - state.thrustToWeight, 0.0D, 1.0D);
         double climbOrLowEnergyFactor = Math.max(state.verticalSpeed > 0.0D ? MCH_FlightModel.clamp(state.verticalSpeed / 0.18D, 0.0D, 1.0D) : 0.35D,
               MCH_FlightModel.clamp((state.climbSustainSpeed - state.airspeed) / Math.max(0.05D, state.climbSustainSpeed), 0.0D, 1.0D));
         state.climbEnergyDeficit = Math.max(Math.max(state.speedSeverity, state.aoaSeverity),
               Math.max(state.stallSeverity, Math.max(liftDeficit, thrustDeficit)));
         state.unsupportedNoseUpSeverity = state.airborne && !state.vtolMode
               ? MCH_FlightModel.clamp(state.noseUpAttitude * state.climbEnergyDeficit * climbOrLowEnergyFactor, 0.0D, 1.0D) : 0.0D;
         state.validClimb = state.airborne && !state.vtolMode && state.stallSeverity < 0.20D && state.aoaSeverity < 0.30D
               && state.speedSeverity < 0.30D
               && (state.liftToWeight >= 1.0D || (state.thrustToWeight >= 0.75D && state.airspeed >= state.climbSustainSpeed));

         this.lastLiftLoss = liftLoss;
         this.lastLiftForceBeforeStallLoss = liftBeforeStallLoss;
         this.lastLiftForceAfterStallLoss = state.liftForce;
         this.lastLiftCoefficient = aoaLift * (1.0D - liftLoss);
      }

      this.aeroState = state;
      this.angleOfAttack = state.aoaDeg;
      this.speedStallSeverity = state.speedSeverity;
      this.aoaStallSeverity = state.aoaSeverity;
      this.stallDemand = state.stallDemand;
      this.lastAirborne = state.airborne;
      this.lastHorizontalSpeed = state.horizontalSpeed;
      this.lastEngineThrustForce = state.thrustForce;
      this.lastWeightForce = state.weightForce;
      this.lastLiftForce = state.liftForce;
      this.lastValidTakeoff = state.validTakeoff;
      this.lastValidClimb = state.validClimb;
      this.lastUnsupportedClimbSeverity = state.unsupportedNoseUpSeverity;
   }



   private void applyOverspeedDamage(double severity) {
      MCP_PlaneInfo info = this.getPlaneInfo();
      if(info == null || info.overspeedDamageRate <= 0.0F || severity <= 0.0D || this.isDestroyed()) {
         return;
      }

      this.overspeedDamageAccumulator += severity * (double)info.overspeedDamageRate;
      int damage = (int)this.overspeedDamageAccumulator;
      if(damage > 0) {
         this.overspeedDamageAccumulator -= (double)damage;
         this.setDamageTaken(this.getDamageTaken() + damage);
      }
   }

   private double resolveNewFlightGravity() {
      MCP_PlaneInfo info = this.getPlaneInfo();
      if(info != null && !Float.isNaN(info.newFlightGravity)) {
         return Math.max(0.0D, (double)info.newFlightGravity);
      }
      if(MCH_Config.NewFlightGravity != null && MCH_Config.NewFlightGravity.prmDouble > 0.0D) {
         return MCH_Config.NewFlightGravity.prmDouble;
      }
      return 0.008D;
   }


   private void updateNewFlightThrustForce() {
      if(!this.useNewMobilitySystem() || this.getPlaneInfo() == null) {
         this.lastEngineThrustForce = 0.0D;
         return;
      }
      double propulsiveThrottle = MCH_FlightModel.clamp(this.getPropulsiveEngineThrottle(), 0.0D, 1.0D);
      this.lastEngineThrustForce = Math.max(0.0D, (double)this.getPlaneInfo().engineThrust * propulsiveThrottle);
   }

   private void applyNewFlightVerticalForces() {
      AeroState state = this.aeroState;
      if(state == null || !state.airborne) {
         this.lastGravityAcceleration = 0.0D;
         this.lastLiftAcceleration = 0.0D;
         this.lastNetVerticalAcceleration = 0.0D;
         this.lastWeightForce = 0.0D;
         this.lastLiftForce = 0.0D;
         this.lastLiftCoefficient = 0.0D;
         this.lastValidClimb = false;
         return;
      }

      double mass = this.getPhysicalMass();
      double netAccel = (state.liftForce - state.weightForce) / mass;
      super.motionY += netAccel;
      this.lastGravityAcceleration = state.gravityForce;
      this.lastLiftAcceleration = state.liftForce / mass;
      this.lastWeightForce = state.weightForce;
      this.lastLiftForce = state.liftForce;
      this.lastNetVerticalAcceleration = netAccel;
      this.lastValidClimb = state.validClimb;
   }


   private void applyNewFlightTakeoffAssist(boolean levelOff, double waterDepth) {
      // Takeoff assist is folded into AeroState.liftForce so it uses the same airspeed,
      // AoA, and stall filters as all other fixed-wing lift. This method only applies
      // runway gating when rotation is attempted below the effective takeoff speed.
      AeroState state = this.aeroState;
      if(state == null || levelOff || waterDepth != 0.0D || state.vtolMode || !this.useNewMobilitySystem()) {
         this.lastValidTakeoff = false;
         return;
      }
      this.lastValidTakeoff = state.validTakeoff;
      if(!state.validTakeoff && this.lastTakeoffMultiplierActive && super.motionY > 0.0D) {
         super.motionY *= MCH_FlightModel.clamp(state.horizontalSpeed / Math.max(0.05D, this.lastEffectiveTakeoffSpeed), 0.15D, 1.0D);
      }
   }



   private void applyThrottleDeficitPitchDown(boolean nearGround, double waterDepth, boolean levelOff) {
      this.lastThrustPitchDownMoment = 0.0D;
      AeroState state = this.aeroState;
      if(state == null || !this.useNewMobilitySystem() || this.getPlaneInfo() == null || nearGround || waterDepth != 0.0D
            || state.vtolMode || levelOff) {
         return;
      }

      double pitchMoment = state.unsupportedNoseUpSeverity
            * (double)this.getPlaneInfo().stallPitchRecoveryStrength
            * MCH_FlightModel.clamp((double)this.getPlaneInfo().stallStrength / 0.6D, 0.0D, 4.0D) * 0.45D;
      if(pitchMoment <= 1.0E-4D) {
         return;
      }

      double appliedPitchDownVelocity = MCH_FlightModel.clamp(pitchMoment, 0.0D, 0.65D);
      this.pitchAngularVelocity += (float)appliedPitchDownVelocity;
      if(this.pitchAngularVelocity < 0.0F) {
         this.pitchAngularVelocity *= (float)(1.0D - MCH_FlightModel.clamp(state.unsupportedNoseUpSeverity * 0.45D, 0.0D, 0.45D));
      }
      this.lastThrustPitchDownMoment = appliedPitchDownVelocity;
   }


   public void resetNewFlightPlacementMotion() {
      if(this.getPlaneInfo() == null || !this.getPlaneInfo().useNewMobilitySystem) {
         return;
      }
      super.motionX = super.motionY = super.motionZ = 0.0D;
      this.velocityX = this.velocityY = this.velocityZ = 0.0D;
      this.engineThrottle = 0.0D;
      this.lastAerodynamicDrag = 0.0D;
      this.lastLiftLoss = 0.0D;
      this.lastGravityAcceleration = 0.0D;
      this.lastLiftAcceleration = 0.0D;
      this.lastNetVerticalAcceleration = 0.0D;
      this.lastWeightForce = 0.0D;
      this.lastLiftForce = 0.0D;
      this.lastLiftForceBeforeStallLoss = 0.0D;
      this.lastLiftForceAfterStallLoss = 0.0D;
      this.lastEngineThrustForce = 0.0D;
      this.speedStallSeverity = 0.0D;
      this.aoaStallSeverity = 0.0D;
      this.stallDemand = 0.0D;
      this.pitchBreakActive = false;
      this.lastNetForwardAcceleration = 0.0D;
      this.lastBaseTakeoffSpeed = 0.0D;
      this.lastEffectiveTakeoffSpeed = 0.0D;
      this.lastTakeoffMultiplierActive = false;
      this.lastStallSuppressedLiftHeadroom = false;
      this.lastValidTakeoff = false;
      this.lastValidClimb = false;
      this.lastIdleUnsupportedClimb = false;
      this.lastIdleThrottleWarning = "";
      this.lastPitchBreakAngularVelocity = 0.0D;
      this.lastStallPitchMoment = 0.0D;
      this.lastThrustPitchDownMoment = 0.0D;
      this.lastLiftCoefficient = 0.0D;
      this.stallRecovering = false;
      this.lastNoseUpPitchSuppression = 0.0D;
      this.lastUnsupportedClimbSeverity = 0.0D;
      this.lastIdleUnsupportedClimb = false;
      this.lastIdleThrottleWarning = "";
      this.lastPitchAuthority = 1.0D;
      this.lastControlAuthority = 1.0D;
      this.lastAirborne = false;
      this.pitchAngularVelocity = this.rollAngularVelocity = this.yawAngularVelocity = 0.0F;
      this.currentGForce = 1.0D;
      this.overspeedDamageAccumulator = 0.0D;
      this.angleOfAttack = 0.0D;
      this.stallSeverity = 0.0D;
      this.stalling = false;
      this.aircraftPosRotInc = 0;
      this.clearPlacementMotionState();
   }

   protected void updateVehicleStress() {
      if(!this.useNewMobilitySystem()) {
         this.currentGForce = 1.0D;
         this.overspeedDamageAccumulator = 0.0D;
         return;
      }

      MCP_PlaneInfo info = this.getPlaneInfo();
      if(info == null) {
         this.currentGForce = 1.0D;
         return;
      }

      double pitchRate = Math.max(Math.abs((double)this.pitchAngularVelocity),
            Math.abs((double)MathHelper.wrapAngleTo180_float(this.getRotPitch() - this.prevRotationPitch)));
      double yawRate = Math.max(Math.abs((double)this.yawAngularVelocity),
            Math.abs((double)MathHelper.wrapAngleTo180_float(this.getRotYaw() - this.prevRotationYaw)));
      double turnRate = Math.sqrt(pitchRate * pitchRate + yawRate * yawRate);
      double speed = this.getAirspeed();
      this.currentGForce = MCH_FlightModel.getApproximateGForce(speed, turnRate);

      double structuralOverload = Math.max(0.0D, this.currentGForce / Math.max(1.0D,
            (double)info.maxStructuralG) - 1.0D);
      double overspeed = MCH_FlightModel.getOverspeedSeverity(speed, this.getMaxSafeSpeed());
      if(!super.worldObj.isRemote) {
         if(structuralOverload > 0.0D) {
            // Reserved for plane-specific structural failure hooks.
         }
         this.applyOverspeedDamage(overspeed);
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

         boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
         float rot;
         if(isFly && !this.isFreeLookMode() && !super.isGunnerMode && (!this.getAcInfo().isFloat || this.getWaterDepth() <= 0.0D)) {
            if(isFly) {
               MCH_Config var10000 = MCH_MOD.config;
               if(!MCH_Config.MouseControlFlightSimMode.prmBool) {
                  this.rotationByKey(partialTicks);
                  float maneuverabilityFactor = this.useNewMobilitySystem()
                        ? PLANE_MANEUVERABILITY_FACTOR * this.getControlAuthorityFactor() : 1.0F;
                  this.setRotRoll(this.getRotRoll() + this.addkeyRotValue * 0.5F * this.getAcInfo().mobilityRoll
                        * maneuverabilityFactor);
               }
            }
         } else {
            rot = 1.0F;
            if(!isFly) {
               rot = this.getAcInfo().mobilityYawOnGround;
               if(!this.getAcInfo().canRotOnGround) {
                  Block block = MCH_Lib.getBlockY(this, 3, -2, false);
                  if(!W_Block.isEqual(block, W_Block.getWater()) && !W_Block.isEqual(block, Blocks.air) && !W_Block.isEqual(block, Blocks.flowing_water)) {
                     rot = 0.0F;
                  }
               }
            }

            if(super.moveLeft && !super.moveRight) {
               this.setRotYaw(this.getRotYaw() - 0.6F * rot * partialTicks);
            }

            if(super.moveRight && !super.moveLeft) {
               this.setRotYaw(this.getRotYaw() + 0.6F * rot * partialTicks);
            }
         }

         this.addkeyRotValue = this.decayMobilityValue(this.addkeyRotValue, 0.9F, partialTicks);
         if(!isFly && MathHelper.abs(this.getRotPitch()) < 40.0F) {
            this.applyOnGroundPitch(0.97F);
         }

         if(this.getNozzleRotation() > 0.001F) {
            this.setRotPitch(this.decayMobilityValue(this.getRotPitch(), 0.97F, partialTicks));
            this.setRotRoll(this.decayMobilityValue(this.getRotRoll(), 0.9F, partialTicks));
         }

      }
   }

   protected void onUpdate_Control() {
      if(super.isGunnerMode && !this.canUseFuel()) {
         this.switchGunnerMode(false);
      }

      super.throttleBack = (float)((double)super.throttleBack * 0.8D);
      if(this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead && this.isCanopyClose() && this.canUseWing() && this.canUseFuel() && !this.isDestroyed()) {
         this.onUpdate_ControlNotHovering();
      } else if(this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
         super.throttleUp = true;
         this.onUpdate_ControlNotHovering();
      } else if(this.getCurrentThrottle() > 0.0D) {
         this.addCurrentThrottle(-0.0025D * (double)this.getAcInfo().throttleUpDown);
      } else {
         this.setCurrentThrottle(0.0D);
      }

      if(this.getCurrentThrottle() < 0.0D) {
         this.setCurrentThrottle(0.0D);
      }

      if(this.useNewMobilitySystem() && this.getCurrentThrottle() > 1.0D) {
         this.setCurrentThrottle(1.0D);
      }

      if(super.worldObj.isRemote) {
         if(!W_Lib.isClientPlayer(this.getRiddenByEntity())) {
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

      if(this.useNewMobilitySystem()) {
         this.engineThrottle = MCH_FlightModel.clamp(MCH_FlightModel.approachEngineOutput(this.engineThrottle,
               MCH_FlightModel.clamp(this.getCurrentThrottle(), 0.0D, 1.0D),
               this.getPlaneInfo().throttleAcceleration, this.getPlaneInfo().engineDrag), 0.0D, 1.0D);
      } else {
         this.engineThrottle = MCH_FlightModel.clamp(this.getCurrentThrottle(), 0.0D, 1.0D);
      }

      if(!this.canUseCombatFlaps()) {
         this.combatFlapsDeployed = false;
      }
   }

   protected double getEngineThrottle() {
      return MCH_FlightModel.clamp(this.useNewMobilitySystem() ? this.engineThrottle : this.getCurrentThrottle(), 0.0D, 1.0D);
   }

   public double getDebugEngineThrottle() {
      return this.getEngineThrottle();
   }

   public double getDebugEffectiveEngineThrottle() {
      return this.getEffectiveEngineThrottle();
   }

   public double getDebugPropulsiveEngineThrottle() {
      return this.getPropulsiveEngineThrottle();
   }

   protected double getEffectiveEngineThrottle() {
      if(!this.useNewMobilitySystem() || this.getPlaneInfo() == null) {
         return MCH_FlightModel.clamp(this.getEngineThrottle(), 0.0D, 1.0D);
      }

      MCP_PlaneInfo info = this.getPlaneInfo();
      double smoothed = MCH_FlightModel.clamp(this.getEngineThrottle(), 0.0D, 1.0D);
      double response = Math.max(0.1D, (double)info.newFlightThrottleResponse);
      double curved = Math.pow(smoothed, response);
      return MCH_FlightModel.clamp((double)info.newFlightIdleThrottle
            + (1.0D - (double)info.newFlightIdleThrottle) * curved, 0.0D, 1.0D);
   }

   protected double getPropulsiveEngineThrottle() {
      // Zero commanded throttle means zero propulsive thrust. Keep aerodynamic
      // evaluation active; do not turn idle into a powered-climb fallback.
      if(this.getCurrentThrottle() <= 0.01D || this.isGroundedForPropulsion() && this.getCurrentThrottle() <= 0.01D) {
         return 0.0D;
      }
      return MCH_FlightModel.clamp(this.getEffectiveEngineThrottle(), 0.0D, 1.0D);
   }

   private boolean isGroundedForPropulsion() {
      return super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0;
   }

   protected void onUpdate_ControlNotHovering() {
      // 判断是否不处于炮手模式
      if (!super.isGunnerMode) {
         // 获取油门上下状态
         float throttleUpDown = this.getAcInfo().throttleUpDown;
         double throttleRateUp = 0.01D * (double)throttleUpDown;
         double throttleRateDown = 0.01D * (double)throttleUpDown;
         if(this.useNewMobilitySystem() && this.getPlaneInfo() != null) {
            throttleRateUp = (double)this.getPlaneInfo().newFlightThrottleChangeRateUp;
            throttleRateDown = (double)this.getPlaneInfo().newFlightThrottleChangeRateDown;
         }

         // 判断是否是转向状态（只左转或只右转）
         boolean turn = super.moveLeft && !super.moveRight || !super.moveLeft && super.moveRight;

         // 获取旋转转向油门
         float pivotTurnThrottle = this.getAcInfo().pivotTurnThrottle;

         // 本地油门上升状态
         boolean localThrottleUp = super.throttleUp;

         // 如果是转向且当前油门小于旋转油门阈值，并且没有加速和减速
         if (turn && this.getCurrentThrottle() < (double) this.getAcInfo().pivotTurnThrottle && !localThrottleUp && !super.throttleDown) {
            // 设置本地油门上升状态为true
            localThrottleUp = true;
            // 加速倍增
            throttleUpDown *= 2.0F;
         }

         // 如果本地油门上升
         if (localThrottleUp) {
            // 设置油门为当前油门
            float f = throttleUpDown;

            // 如果骑乘的实体不为空，调整油门
            if (this.getRidingEntity() != null && !this.isMountedOnRack()) {
               double mx = this.getRidingEntity().motionX;
               double mz = this.getRidingEntity().motionZ;
               // Non-rack carriers retain their speed-scaled throttle behavior.
               f = throttleUpDown * MathHelper.sqrt_double(mx * mx + mz * mz) * this.getAcInfo().throttleUpDownOnEntity;
            }

            // 如果允许倒车并且油门向后，则递减后退油门
            if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
               super.throttleBack = (float) ((double) super.throttleBack - 0.01D * (double) f);
            } else {
               // 否则，设置后退油门为0
               super.throttleBack = 0.0F;
               // 如果当前油门小于1，则增加油门
               if (this.getCurrentThrottle() < 1.0D) {
                  this.addCurrentThrottle(this.useNewMobilitySystem() && this.getPlaneInfo() != null ? throttleRateUp : 0.01D * (double) f);
               } else {
                  // 否则，设置油门为最大值1
                  this.setCurrentThrottle(1.0D);
               }
            }
         }
         // 如果本地油门下降
         else if (super.throttleDown) {
            // 如果当前油门大于0，则递减油门
            if (this.getCurrentThrottle() > 0.0D) {
               this.addCurrentThrottle(-throttleRateDown);
            } else {
               // 否则，设置油门为0
               this.setCurrentThrottle(0.0D);
               // 如果允许倒车，则增加后退油门
               if (this.getAcInfo().enableBack) {
                  super.throttleBack = (float) ((double) super.throttleBack + 0.0025D * (double) throttleUpDown);
                  // 限制后退油门不超过0.6
                  if (super.throttleBack > 0.6F) {
                     super.throttleBack = 0.6F;
                  }
               }
            }
         }
         // 如果启用了自动油门降低，并且当前油门大于0，则逐步降低油门
         else if (super.cs_planeAutoThrottleDown && this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-(this.useNewMobilitySystem() && this.getPlaneInfo() != null ? throttleRateDown * 0.5D : 0.005D * (double) throttleUpDown));
            // 如果油门低于0，则设置为0
            if (this.getCurrentThrottle() <= 0.0D) {
               this.setCurrentThrottle(0.0D);
            }
         }
      }
   }


   protected void onUpdate_Particle() {
      if(super.worldObj.isRemote) {
         this.onUpdate_ParticleLandingGear();
         this.onUpdate_ParticleNozzle();
      }

   }

   protected void onUpdate_Particle2() {
      if(super.worldObj.isRemote) {
         if((double)this.getHP() < (double)this.getMaxHP() * 0.5D) {
            if(this.getPlaneInfo() != null) {
               int rotorNum = this.getPlaneInfo().rotorList.size();
               if(rotorNum < 0) {
                  rotorNum = 0;
               }

               if(super.isFirstDamageSmoke) {
                  super.prevDamageSmokePos = new Vec3[rotorNum + 1];
               }

               float yaw = this.getRotYaw();
               float pitch = this.getRotPitch();
               float roll = this.getRotRoll();
               boolean spawnSmoke = true;

               int px;
               for(px = 0; px < rotorNum; ++px) {
                  if((double)this.getHP() >= (double)this.getMaxHP() * 0.2D && this.getMaxHP() > 0) {
                     int rotor_pos = (int)(((double)this.getHP() / (double)this.getMaxHP() - 0.2D) / 0.3D * 15.0D);
                     if(rotor_pos > 0 && super.rand.nextInt(rotor_pos) > 0) {
                        spawnSmoke = false;
                     }
                  }

                  Vec3 var16 = ((MCP_PlaneInfo.Rotor)this.getPlaneInfo().rotorList.get(px)).pos;
                  Vec3 py = MCH_Lib.RotVec3(var16, -yaw, -pitch, -roll);
                  double x = super.posX + py.xCoord;
                  double y = super.posY + py.yCoord;
                  double z = super.posZ + py.zCoord;
                  this.onUpdate_Particle2SpawnSmoke(px, x, y, z, 1.0F, spawnSmoke);
               }

               spawnSmoke = true;
               if((double)this.getHP() >= (double)this.getMaxHP() * 0.2D && this.getMaxHP() > 0) {
                  px = (int)(((double)this.getHP() / (double)this.getMaxHP() - 0.2D) / 0.3D * 15.0D);
                  if(px > 0 && super.rand.nextInt(px) > 0) {
                     spawnSmoke = false;
                  }
               }

               double var15 = super.posX;
               double var17 = super.posY;
               double pz = super.posZ;
               if(this.getSeatInfo(0) != null && this.getSeatInfo(0).pos != null) {
                  Vec3 pos = MCH_Lib.RotVec3(0.0D, this.getSeatInfo(0).pos.yCoord, -2.0D, -yaw, -pitch, -roll);
                  var15 += pos.xCoord;
                  var17 += pos.yCoord;
                  pz += pos.zCoord;
               }

               this.onUpdate_Particle2SpawnSmoke(rotorNum, var15, var17, pz, rotorNum == 0?2.0F:1.0F, spawnSmoke);
               super.isFirstDamageSmoke = false;
            }
         }
      }
   }

   public void onUpdate_Particle2SpawnSmoke(int ri, double x, double y, double z, float size, boolean spawnSmoke) {
      if(super.isFirstDamageSmoke || super.prevDamageSmokePos[ri] == null) {
         super.prevDamageSmokePos[ri] = Vec3.createVectorHelper(x, y, z);
      }

      Vec3 prev = super.prevDamageSmokePos[ri];
      double dx = x - prev.xCoord;
      double dy = y - prev.yCoord;
      double dz = z - prev.zCoord;
      int num = (int)((double)MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) / 0.3D) + 1;

      for(int i = 0; i < num; ++i) {
         float c = 0.2F + super.rand.nextFloat() * 0.3F;
         MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", prev.xCoord + (x - prev.xCoord) * (double)i / 3.0D, prev.yCoord + (y - prev.yCoord) * (double)i / 3.0D, prev.zCoord + (z - prev.zCoord) * (double)i / 3.0D);
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
      double d = super.motionX * super.motionX + super.motionZ * super.motionZ;
      if(d > 0.01D) {
         int x = MathHelper.floor_double(super.posX + 0.5D);
         int y = MathHelper.floor_double(super.posY - 0.5D);
         int z = MathHelper.floor_double(super.posZ + 0.5D);
         MCH_ParticlesUtil.spawnParticleTileCrack(super.worldObj, x, y, z, super.posX + ((double)super.rand.nextFloat() - 0.5D) * (double)super.width, super.boundingBox.minY + 0.1D, super.posZ + ((double)super.rand.nextFloat() - 0.5D) * (double)super.width, -super.motionX * 4.0D, 1.5D, -super.motionZ * 4.0D);
      }

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

   public void onUpdate_ParticleNozzle() {
      if(this.planeInfo != null && this.planeInfo.haveNozzle()) {
         if(this.getCurrentThrottle() > 0.10000000149011612D) {
            float yaw = this.getRotYaw();
            float pitch = this.getRotPitch();
            float roll = this.getRotRoll();
            Vec3 nozzleRot = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -yaw - 180.0F, pitch - this.getNozzleRotation(), roll);
            Iterator i$ = this.planeInfo.nozzles.iterator();

            while(i$.hasNext()) {
               MCH_BaseVehicleInfo.DrawnPart nozzle = (MCH_BaseVehicleInfo.DrawnPart)i$.next();
               if((double)super.rand.nextFloat() <= this.getCurrentThrottle() * 1.5D) {
                  Vec3 nozzlePos = MCH_Lib.RotVec3(nozzle.pos, -yaw, -pitch, -roll);
                  double x = super.posX + nozzlePos.xCoord + nozzleRot.xCoord;
                  double y = super.posY + nozzlePos.yCoord + nozzleRot.yCoord;
                  double z = super.posZ + nozzlePos.zCoord + nozzleRot.zCoord;
                  float a = 0.7F;
                  if(W_WorldFunc.getBlockId(super.worldObj, (int)(x + nozzleRot.xCoord * 3.0D), (int)(y + nozzleRot.yCoord * 3.0D), (int)(z + nozzleRot.zCoord * 3.0D)) != 0) {
                     a = 2.0F;
                  }

                  MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", x, y, z, nozzleRot.xCoord + (double)((super.rand.nextFloat() - 0.5F) * a), nozzleRot.yCoord, nozzleRot.zCoord + (double)((super.rand.nextFloat() - 0.5F) * a), 5.0F * this.getAcInfo().particlesScale);
                  MCH_ParticlesUtil.spawnParticle(prm);
               }
            }

         }
      }
   }

   public void destroyAircraft() {
      super.destroyAircraft();
      byte inv = 1;
      if(this.getRotRoll() >= 0.0F) {
         if(this.getRotRoll() > 90.0F) {
            inv = -1;
         }
      } else if(this.getRotRoll() > -90.0F) {
         inv = -1;
      }

      super.rotDestroyedRoll = (0.5F + super.rand.nextFloat()) * (float)inv;
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
         if(MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
            if(MathHelper.abs(this.getRotPitch()) < 10.0F) {
               this.setRotPitch(this.getRotPitch() + super.rotDestroyedPitch);
            }

            float roll = MathHelper.abs(this.getRotRoll());
            if(roll < 45.0F || roll > 135.0F) {
               this.setRotRoll(this.getRotRoll() + super.rotDestroyedRoll);
            }
         } else if(MathHelper.abs(this.getRotPitch()) > 20.0F) {
            this.setRotPitch(this.getRotPitch() * 0.99F);
         }
      }

      if(this.getRiddenByEntity() != null) {
         ;
      }

      this.updateSound();
      this.onUpdate_Particle();
      this.onUpdate_Particle2();
      this.onUpdate_ParticleSplash();
      this.onUpdate_ParticleSandCloud(true);
      this.updateCamera(super.posX, super.posY, super.posZ);
   }

   private void onUpdate_Server() {
      Entity rdnEnt = this.getRiddenByEntity();
      double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      double dp = 0.0D;
      this.updateCollisionBox();
      if(this.canFloatWater()) {
         dp = this.getWaterDepth();
      }
      if(this.useNewMobilitySystem()) {
         this.updateAerodynamicState();
         this.updateNewFlightThrustForce();
      } else {
         this.angleOfAttack = 0.0D;
         this.stallSeverity = 0.0D;
         this.lastAerodynamicDrag = 0.0D;
         this.lastLiftLoss = 0.0D;
         this.lastGravityAcceleration = 0.0D;
         this.lastLiftAcceleration = 0.0D;
         this.lastNetVerticalAcceleration = 0.0D;
         this.lastAirborne = false;
         this.speedStallSeverity = 0.0D;
         this.aoaStallSeverity = 0.0D;
         this.stallDemand = 0.0D;
         this.pitchBreakActive = false;
         this.stalling = false;
         this.lastEngineThrustForce = 0.0D;
         this.lastValidClimb = false;
         this.lastUnsupportedClimbSeverity = 0.0D;
         this.lastIdleUnsupportedClimb = false;
         this.lastIdleThrottleWarning = "";
         this.lastHorizontalSpeed = 0.0D;
         this.lastLowHorizontalSpeedWarning = "";
         this.aeroState = new AeroState();
      }

      boolean levelOff = super.isGunnerMode;
      if(dp == 0.0D) {
         // Target drones with fuel and no fatal damage run simple terrain-following autopilot.
         if (this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {

            // Check for terrain well ahead and below the drone.
            Block throttle = MCH_Lib.getBlockY(this, 3, -100, true);

            // If terrain is detected ahead, look closer before pitching up.
            if (throttle != null && !W_Block.isEqual(throttle, Blocks.air)) {

               // Re-check closer terrain after the long-range hit.
               throttle = MCH_Lib.getBlockY(this, 3, -5, true);

               // If the closer path is clear, turn and climb gently.
               if (throttle == null || W_Block.isEqual(throttle, Blocks.air)) {

                  // Apply autopilot yaw.
                  this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 2.0F);

                  // Negative pitch is nose-up in this codebase; climb toward -20 degrees.
                  if (this.getRotPitch() > -20.0F) {
                     this.setRotPitch(this.getRotPitch() - 0.5F);
                  }
               }
            } else {
               // With no terrain ahead, continue the normal autopilot turn.
               this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 1.0F);

               // Ease pitch back toward level flight.
               this.setRotPitch(this.decayMobilityValue(this.getRotPitch(), 0.95F, 1.0F));

               // Retract landing gear when available.
               if (this.canFoldLandingGear()) {
                  this.foldLandingGear();
               }

               // Treat this as level-off/autopilot flight for the vertical model.
               levelOff = true;
            }
         }


         if(!levelOff) {
            if(this.useNewMobilitySystem() && this.getPlaneInfo() != null) {
               this.applyNewFlightVerticalForces();
            } else {
               super.motionY += 0.04D + (double)(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
               super.motionY += -0.047D * (1.0D - this.getEngineThrottle());
            }
         } else {
            super.motionY *= 0.8D;
            this.lastGravityAcceleration = 0.0D;
            this.lastLiftAcceleration = 0.0D;
            this.lastNetVerticalAcceleration = 0.0D;
            this.lastAirborne = false;
         }
      } else {
         this.setRotPitch(this.getRotPitch() * 0.8F, "getWaterDepth != 0");
         if(MathHelper.abs(this.getRotRoll()) < 40.0F) {
            this.setRotRoll(this.getRotRoll() * 0.9F);
         }

         if(dp < 1.0D) {
            super.motionY -= 1.0E-4D;
            super.motionY += 0.007D * this.getEngineThrottle();
         } else {
            if(super.motionY < 0.0D) {
               super.motionY /= 2.0D;
            }

            super.motionY += 0.007D;
         }
      }

      // Convert throttle into per-tick acceleration.
      double propulsiveThrottle = this.useNewMobilitySystem()
            ? this.getPropulsiveEngineThrottle() : this.getEngineThrottle();
      propulsiveThrottle = MCH_FlightModel.clamp(propulsiveThrottle, 0.0D, 1.0D);
      float throttle1 = (float)(propulsiveThrottle / 10.0D);
      if(this.useNewMobilitySystem() && this.getPlaneInfo() != null) {
         double mass = this.getPhysicalMass();
         double thrustForce = Math.max(0.0D, (double)this.getPlaneInfo().engineThrust * propulsiveThrottle);
         throttle1 = (float)(thrustForce / mass / 10.0D);
         this.lastEngineThrustForce = thrustForce;
      } else {
         this.lastEngineThrustForce = 0.0D;
      }
      Vec3 v;
      this.lastStallSuppressedLiftHeadroom = false;
      this.lastPitchBreakAngularVelocity = 0.0D;
      this.lastStallPitchMoment = 0.0D;
      this.lastThrustPitchDownMoment = 0.0D;

      // If the nozzle is rotated, use VTOL-style thrust vectoring.
      if(this.getNozzleRotation() > 0.001F) {
         // Ease aircraft pitch while the nozzles are rotating.
         this.setRotPitch(this.decayMobilityValue(this.getRotPitch(), 0.95F, 1.0F));
         // Build a thrust direction from yaw and pitch plus nozzle deflection.
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - this.getNozzleRotation());
         // A fully downward nozzle keeps some horizontal authority but less than normal flight.
         if(this.getNozzleRotation() >= 90.0F) {
            v.xCoord *= 0.800000011920929D;
            v.zCoord *= 0.800000011920929D;
         }
      } else {
         // Conventional flight uses pitch for attitude and AoA, not direct vertical thrust.
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);
      }

      // Apply thrust. In the new fixed-wing model, conventional forward thrust
      // should not convert pitch directly into balloon-like vertical velocity;
      // climb comes from airspeed/AoA lift and is limited by stall/drag.
      if(!levelOff) {
         if(this.getNozzleRotation() <= 0.01F) {
            double verticalThrust = v.yCoord * (double)throttle1 / 2.0D;
            if(this.useNewMobilitySystem() && this.getPlaneInfo() != null) {
               verticalThrust = Math.min(0.0D, verticalThrust * 0.15D);
            }
            super.motionY += verticalThrust;
         } else {
            super.motionY += v.yCoord * (double)throttle1 / 8.0D;
         }
      }

      // Check whether this vehicle is allowed to move on the ground.
      boolean canMove = true;
      if(!this.getAcInfo().canMoveOnGround) {
         // Inspect the block below before ground movement.
         Block motion = MCH_Lib.getBlockY(this, 3, -2, false);
         // Solid non-water blocks prevent ground movement for aircraft that disallow it.
         if(!W_Block.isEqual(motion, W_Block.getWater()) && !W_Block.isEqual(motion, Blocks.air) && !W_Block.isEqual(motion, Blocks.flowing_water)) {
            canMove = false;
         }
      }

      double forwardSpeedBefore = super.motionX * v.xCoord + super.motionZ * v.zCoord;

      // Apply horizontal propulsion when movement is allowed.
      if(canMove) {
         // Reverse when the vehicle supports backing up and reverse throttle is active.
         if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
            super.motionX -= v.xCoord * (double) super.throttleBack;
            super.motionZ -= v.zCoord * (double) super.throttleBack;
         } else {
            // Otherwise accelerate forward.
            super.motionX += v.xCoord * (double) throttle1;
            super.motionZ += v.zCoord * (double) throttle1;
         }
      }

      // Dampen vertical velocity after forces are applied.
      super.motionY *= 0.95D;
      // Dampen horizontal velocity by the aircraft motion factor.
      super.motionX *= this.getAcInfo().motionFactor;
      super.motionZ *= this.getAcInfo().motionFactor;

      float baseSpeedLimit = this.getMaxSpeed();
      float levelSpeed = this.useNewMobilitySystem() && this.getPlaneInfo().maxLevelSpeed > 0.0F ? this.getPlaneInfo().maxLevelSpeed : baseSpeedLimit;

      // Apply a deliberately simple energy model only to conventional airborne flight.
      // Velocity direction carries the gained/lost energy, while bank and body rates
      // cheaply approximate induced and control-surface drag during hard manoeuvres.
      this.lastAerodynamicDrag = 0.0D;
      if(this.useNewMobilitySystem() && dp == 0.0D && !super.onGround && this.getNozzleRotation() <= 0.01F && !levelOff) {
         double horizontalSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         double bankLoad = MCH_FlightModel.clamp(MathHelper.abs(this.getRotRoll()) / 75.0D, 0.0D, 1.0D);
         double bodyRate = (MathHelper.abs(this.pitchAngularVelocity) + MathHelper.abs(this.rollAngularVelocity)
               + MathHelper.abs(this.yawAngularVelocity)) / 6.0D;
         double controlLoad = MCH_FlightModel.clamp(bodyRate, 0.0D, 1.0D);
         double turnLoad = Math.max(bankLoad, controlLoad);
         double engineBrakeDrag = this.getPlaneInfo().newFlightEngineBrakeDrag;
         if(this.isCombatFlapsDeployed()) {
            engineBrakeDrag += this.getPlaneInfo().newFlightCombatFlapDrag;
            turnLoad = MCH_FlightModel.clamp(turnLoad + (double)this.getPlaneInfo().newFlightCombatFlapLift, 0.0D, 1.0D);
         }
         double mass = this.getPhysicalMass();
         double drag = MCH_FlightModel.getEnergyDrag(horizontalSpeed, (double)levelSpeed, this.getEffectiveEngineThrottle(),
               turnLoad, controlLoad, this.getPlaneInfo().baseDrag, this.getPlaneInfo().inducedDrag,
               this.getPlaneInfo().controlSurfaceDrag, (float)engineBrakeDrag) / mass;
         drag += MCH_FlightModel.getAngleOfAttackDrag(this.angleOfAttack, this.getPlaneInfo().criticalAoA,
               this.getPlaneInfo().baseDrag, this.getPlaneInfo().aoaDragMultiplier) / mass;
         drag += this.stallSeverity * (0.035D + 0.12D * MCH_FlightModel.clamp(Math.abs(this.angleOfAttack)
               / Math.max(1.0D, (double)this.getPlaneInfo().criticalAoA), 0.0D, 2.0D));
         double stallSpeedForIdleDrag = MCH_FlightModel.getStallSpeed(this.getPlaneInfo().stallSpeed, this.getMaxSpeed(),
               this.getPlaneInfo().stallSpeedFactor);
         this.lastIdleUnsupportedClimb = this.isIdleUnsupportedClimb(
               MCH_FlightModel.clamp((double)(-this.getRotPitch() - 8.0F) / 42.0D, 0.0D, 1.0D), stallSpeedForIdleDrag);
         this.lastUnsupportedClimbSeverity = this.getUnsupportedClimbSeverity();
         if(this.lastUnsupportedClimbSeverity > 0.0D) {
            double idleDragBoost = this.lastIdleUnsupportedClimb ? 0.12D : 0.0D;
            drag += this.lastUnsupportedClimbSeverity * (0.08D + idleDragBoost + 0.22D * Math.max(this.stallSeverity, this.aoaStallSeverity));
            this.lastStallSuppressedLiftHeadroom = true;
         }
         drag = MCH_FlightModel.clamp(drag, 0.0D, 0.5D);
         this.lastAerodynamicDrag = drag;
         double energyChange = MCH_FlightModel.getVerticalEnergyChange(super.motionY,
               this.getPlaneInfo().climbEnergyLoss, this.getPlaneInfo().diveEnergyGain) / mass;
         double targetSpeed = Math.max(0.0D, horizontalSpeed * (1.0D - drag) + energyChange);

         if(horizontalSpeed > 1.0E-4D) {
            double energyScale = targetSpeed / horizontalSpeed;
            super.motionX *= energyScale;
            super.motionZ *= energyScale;
         } else if(targetSpeed > 0.0D) {
            double yaw = Math.toRadians((double)this.getRotYaw());
            super.motionX += -Math.sin(yaw) * targetSpeed;
            super.motionZ += Math.cos(yaw) * targetSpeed;
         }
      }

      this.lastNetForwardAcceleration = super.motionX * v.xCoord + super.motionZ * v.zCoord - forwardSpeedBefore;
      this.applyNewFlightTakeoffAssist(levelOff, dp);

      // Calculate current horizontal speed.
      double motion1 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      if(this.isGroundedForPropulsion() && this.getCurrentThrottle() <= 0.0D
            && super.throttleBack <= 0.0F && motion1 > prevMotion) {
         if(prevMotion > 1.0E-6D) {
            double groundSpeedScale = prevMotion / motion1;
            super.motionX *= groundSpeedScale;
            super.motionZ *= groundSpeedScale;
         } else {
            super.motionX = 0.0D;
            super.motionZ = 0.0D;
         }
         motion1 = prevMotion;
         this.lastNetForwardAcceleration = Math.min(0.0D, this.lastNetForwardAcceleration);
      }
      // Diving permits an overspeed only for vehicles explicitly using the new mobility system.
      float speedLimit = this.useNewMobilitySystem()
            ? (float)MCH_FlightModel.getDiveSpeedLimit(levelSpeed, this.getRotPitch(), super.motionY, this.getPlaneInfo().diveSpeedMultiplier)
            : baseSpeedLimit;
      // Clamp horizontal speed to the current speed limit.
      if(motion1 > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion1;
         super.motionZ *= (double)speedLimit / motion1;
         motion1 = speedLimit;
      }

      // Increase displayed/current speed gradually while accelerating below the limit.
      if(motion1 > prevMotion && super.currentSpeed < (double)speedLimit) {
         super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
         if(super.currentSpeed > (double)speedLimit) {
            super.currentSpeed = (double)speedLimit;
         }
      } else {
         // Otherwise decay displayed/current speed toward the minimum idle value.
         super.currentSpeed -= (super.currentSpeed - 0.07D) / 35.0D;
         if(super.currentSpeed < 0.07D) {
            super.currentSpeed = 0.07D;
         }
      }

      // Keep ground effect for several blocks so takeoff remains forgiving. Away from
      // the runway, low speed or excessive AoA removes lift and introduces a repeatable
      // buffet/wing drop. Lowering the nose reduces AoA and lets speed build to recovery.
      boolean nearGround = super.onGround || MCH_Lib.getBlockIdY(this, 3, -5) > 0;
      if(!this.useNewMobilitySystem()) {
         this.lastLiftLoss = 0.0D;
      }
      this.pitchBreakActive = false;
      this.lastUnsupportedClimbSeverity = this.getUnsupportedClimbSeverity();
      double stallSpeedForIdle = this.getPlaneInfo() != null
            ? MCH_FlightModel.getStallSpeed(this.getPlaneInfo().stallSpeed, this.getMaxSpeed(), this.getPlaneInfo().stallSpeedFactor) : 0.0D;
      this.lastIdleUnsupportedClimb = this.isIdleUnsupportedClimb(
            MCH_FlightModel.clamp((double)(-this.getRotPitch() - 8.0F) / 42.0D, 0.0D, 1.0D), stallSpeedForIdle);
      this.updateIdleThrottleWarning();
      this.updateLowHorizontalSpeedWarning();
      this.applyThrottleDeficitPitchDown(nearGround, dp, levelOff);
      if(this.useNewMobilitySystem() && !nearGround && dp == 0.0D && this.getNozzleRotation() <= 0.01F && !levelOff && this.stallSeverity > 0.0D) {
         double liftLoss = MCH_FlightModel.clamp(this.stallSeverity * (double)this.getPlaneInfo().stallLiftLoss, 0.0D, 1.0D);
         this.lastLiftLoss = liftLoss;

         double phase = (double)(super.ticksExisted + this.getEntityId() * 13) * 0.37D;
         double buffet = Math.sin(phase) * (double)this.getPlaneInfo().stallInstability * this.stallSeverity;
         double wingDrop = ((this.getEntityId() & 1) == 0 ? 1.0D : -1.0D)
               * (double)this.getPlaneInfo().stallInstability * this.stallSeverity;
         this.setRotRoll(this.getRotRoll() + (float)(wingDrop * 0.08D + buffet * 0.04D));
         this.setRotYaw(this.getRotYaw() + (float)(buffet * 0.02D));

         double aerodynamicDemand = Math.max(this.stallDemand, Math.max(this.speedStallSeverity, this.aoaStallSeverity));
         double noseUpAttitude = MCH_FlightModel.clamp((double)(-this.getRotPitch()) / 45.0D, 0.0D, 1.0D);
         double liftDeficit = MCH_FlightModel.clamp(1.0D - this.getLiftToWeightRatio(), 0.0D, 1.0D);
         double authorityLoss = MCH_FlightModel.clamp(1.0D - (double)this.getControlAuthorityFactor(), 0.0D, 1.0D);
         double legacyStrengthScale = MCH_FlightModel.clamp((double)this.getPlaneInfo().stallStrength / 0.6D, 0.0D, 4.0D);
         double pitchRecovery = this.stallSeverity * (double)this.getPlaneInfo().stallPitchRecoveryStrength
               * legacyStrengthScale
               * (0.25D + 0.75D * aerodynamicDemand)
               * (0.55D + 0.45D * Math.max(noseUpAttitude, liftDeficit));
         double deepStallBreak = this.stallSeverity * this.stallSeverity * (double)this.getPlaneInfo().stallBreakStrength
               * legacyStrengthScale
               * (0.35D + 0.65D * Math.max(this.aoaStallSeverity, authorityLoss));
         double stallPitchMoment = pitchRecovery + deepStallBreak;
         if(stallPitchMoment > 1.0E-4D) {
            this.pitchBreakActive = true;
            // MCHeli/Minecraft pitch is inverted from aerodynamic sign: nose-up attitude is
            // negative rotation pitch, and nose-down attitude is positive rotation pitch.
            double appliedPitchBreakVelocity = MCH_FlightModel.clamp(stallPitchMoment * 0.45D, 0.0D, 1.8D);
            this.pitchAngularVelocity += (float)appliedPitchBreakVelocity;
            this.lastPitchBreakAngularVelocity = appliedPitchBreakVelocity;
            this.lastStallPitchMoment = stallPitchMoment;
            if(this.pitchAngularVelocity < 0.0F) {
               this.pitchAngularVelocity *= (float)(1.0D - MCH_FlightModel.clamp(this.stallSeverity * 0.35D, 0.0D, 0.35D));
            }
         }
      }

      // Lift fades through a band below the configured ceiling instead of hitting an invisible wall.
      if(this.useNewMobilitySystem()) {
         double ceilingLift = MCH_FlightModel.getCeilingLiftFactor(super.posY, this.getAcInfo().flightCeiling, this.getAcInfo().flightCeilingRange);
         if(!nearGround && ceilingLift < 1.0D) {
            if(super.motionY > 0.0D) {
               super.motionY *= 0.9D + ceilingLift * 0.1D;
            }
            super.motionY -= (1.0D - ceilingLift) * 0.012D;
         }
      }

      if(this.useNewMobilitySystem() && MCH_Config.DebugLog && super.ticksExisted % 20 == 0) {
         MCH_Lib.DbgLog(super.worldObj,
               "PlaneAero[%s] pitch=%.2f speed=%.3f hSpeed=%.3f vSpeed=%.3f aoa=%.2f lift=%.4f drag=%.4f stall=%.3f ctrl=%.3f pitchAuth=%.3f",
               new Object[]{this.getEntityName(), Float.valueOf(this.getRotPitch()), Double.valueOf(this.getAirspeed()),
                     Double.valueOf(this.lastHorizontalSpeed), Double.valueOf(super.motionY), Double.valueOf(this.angleOfAttack),
                     Double.valueOf(this.lastLiftForce), Double.valueOf(this.lastAerodynamicDrag), Double.valueOf(this.stallSeverity),
                     Double.valueOf(this.lastControlAuthority), Double.valueOf(this.lastPitchAuthority)});
      }

      // If the aircraft is on or near the ground, damp horizontal speed and apply ground pitch.
      if(nearGround) {
         super.motionX *= this.getAcInfo().motionFactor;
         super.motionZ *= this.getAcInfo().motionFactor;
         // Blend the resting pitch only when the aircraft is not steeply rotated.
         if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
            this.applyOnGroundPitch(0.8F);
         }
      }

      // Update aircraft position.
      this.moveEntity(super.motionX, super.motionY, super.motionZ);

      // Update rotation and block interactions.
      this.setRotation(this.getRotYaw(), this.getRotPitch());
      this.onUpdate_updateBlock();

      this.handleDeadPilot();


   }



   private void updateLowHorizontalSpeedWarning() {
      this.lastLowHorizontalSpeedWarning = "";
      AeroState state = this.aeroState;
      if(state == null || !this.useNewMobilitySystem() || this.getPlaneInfo() == null || !state.airborne || state.vtolMode) {
         return;
      }

      double noseUpDegrees = Math.max(0.0D, (double)-this.getRotPitch());
      if(state.horizontalSpeed < 0.12D && state.stallDemand < 0.8D) {
         this.lastLowHorizontalSpeedWarning = "lowSpeedNotStalled";
      } else if(state.horizontalSpeed < 0.12D && state.validClimb) {
         this.lastLowHorizontalSpeedWarning = "lowSpeedValidClimb";
      } else if(state.liftToWeight < 1.0D && this.lastNetVerticalAcceleration >= 0.0D) {
         this.lastLowHorizontalSpeedWarning = "liftDeficitNetClimb";
      } else if(noseUpDegrees > 10.0D && state.airspeed < state.stallRecoverySpeed && state.unsupportedNoseUpSeverity < 0.2D) {
         this.lastLowHorizontalSpeedWarning = "noseUpLowEnergyUnsupportedTooLow";
      }
   }


   private void updateIdleThrottleWarning() {
      this.lastIdleThrottleWarning = "";
      AeroState state = this.aeroState;
      if(state == null || !this.useNewMobilitySystem() || this.getPlaneInfo() == null || !state.airborne || state.vtolMode) {
         return;
      }

      double propulsiveThrottle = this.getPropulsiveEngineThrottle();
      boolean nonFinite = Double.isNaN(propulsiveThrottle) || Double.isInfinite(propulsiveThrottle)
            || Double.isNaN(state.thrustToWeight) || Double.isInfinite(state.thrustToWeight)
            || Double.isNaN(state.liftToWeight) || Double.isInfinite(state.liftToWeight)
            || Double.isNaN(state.unsupportedNoseUpSeverity) || Double.isInfinite(state.unsupportedNoseUpSeverity)
            || Double.isNaN(state.stallSeverity) || Double.isInfinite(state.stallSeverity);
      if(nonFinite) {
         this.lastIdleThrottleWarning = "nonFinite";
      } else if(propulsiveThrottle < 0.01D && state.thrustToWeight > 0.05D) {
         this.lastIdleThrottleWarning = "zeroThrottleHasThrustToWeight";
      } else if(propulsiveThrottle < 0.01D && state.validClimb) {
         this.lastIdleThrottleWarning = "zeroThrottleValidClimb";
      } else if(propulsiveThrottle < 0.01D && state.liftToWeight < 1.0D && this.lastNetVerticalAcceleration >= 0.0D) {
         this.lastIdleThrottleWarning = "zeroThrottleLiftDeficitNetClimb";
      }
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


                  //if () {  // Ensure it ignores self e == MCP_EntityPlane.this || e == rideAc
                  //   return false;
                  //}

                  // Exclude certain entity types from being affected by collision
                  if (e != rideAc && !(e instanceof EntityItem) && !(e instanceof EntityXPOrb && !(e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff )
                          && !(e instanceof MCH_EntityBaseBullet) && !(e instanceof MCH_EntityChain)
                          && !(e instanceof MCH_EntitySeat)) && !(e == MCP_EntityPlane.this || e == rideAc) ) {

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
                  MCH_Lib.DbgLog(super.worldObj, "MCP_EntityPlane.collisionEntity damage=%.1f %s", damage, e.toString());
               }
            }
         }
      }
   }

   private boolean shouldCollisionDamage(Entity e) {
      // Prevent self-collision
      if (e == this || (e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox) e).parent == this)) {
         return false;
      }

      if (e == this || e == super.ridingEntity) {
         return false;
      }
      //i hate this fucking mod



      if (e instanceof MCH_EntityFlare || e instanceof MCH_EntityChaff) {
         return false;
      }

      if (this.getSeatIdByEntity(e) >= 0) {
         return false;
      } else if (super.noCollisionEntities.containsKey(e)) {
         return false;
      } else {
         if (e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox) e).parent != null) {
            MCH_EntityBaseVehicle ac = ((MCH_EntityHitBox) e).parent;
            if (super.noCollisionEntities.containsKey(ac)) {
               return false;
            }
         }

         return e.ridingEntity instanceof MCH_EntityBaseVehicle && super.noCollisionEntities.containsKey(e.ridingEntity)
                 ? false
                 : !(e.ridingEntity instanceof MCH_EntitySeat)
                 || ((MCH_EntitySeat)e.ridingEntity).getParent() == null
                 || !super.noCollisionEntities.containsKey(((MCH_EntitySeat)e.ridingEntity).getParent());
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

   public float getMaxSpeed() {
      float f = 0.0F;
      if(this.partWing != null && this.getPlaneInfo().isVariableSweepWing) {
         f = (this.getPlaneInfo().sweepWingSpeed - this.getPlaneInfo().speed) * this.partWing.getFactor();
      } else if(super.partHatch != null && this.getPlaneInfo().isVariableSweepWing) {
         f = (this.getPlaneInfo().sweepWingSpeed - this.getPlaneInfo().speed) * super.partHatch.getFactor();
      }

      return this.getPlaneInfo().speed + f;
   }

   public float getSoundVolume() {
      return this.getAcInfo() != null && this.getAcInfo().throttleUpDown <= 0.0F?0.0F:this.soundVolume * 0.7F;
   }

   public void updateSound() {
      float target = (float)this.getCurrentThrottle();
      if(this.getRiddenByEntity() != null && (super.partCanopy == null || this.getCanopyRotation() < 1.0F)) {
         target += 0.1F;
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
      return (float)(0.6D + this.getCurrentThrottle() * 0.4D);
   }

   public String getDefaultSoundName() {
      return "plane";
   }

   public void updateParts(int stat) {
      super.updateParts(stat);
      if(!this.isDestroyed()) {
         MCH_Parts[] parts = new MCH_Parts[]{this.partNozzle, this.partWing};
         MCH_Parts[] arr$ = parts;
         int len$ = parts.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Parts p = arr$[i$];
            if(p != null) {
               p.updateStatusClient(stat);
               p.update();
            }
         }

         if(!super.worldObj.isRemote && this.partWing != null && this.getPlaneInfo().isVariableSweepWing && this.partWing.isON() && this.getCurrentThrottle() >= 0.20000000298023224D && (this.getCurrentThrottle() < 0.5D || MCH_Lib.getBlockIdY(this, 1, -10) != 0)) {
            this.partWing.setStatusServer(false);
         }

      }
   }

   public float getUnfoldLandingGearThrottle() {
      return 0.7F;
   }

   public boolean canSwitchVtol() {
      if(this.planeInfo != null && this.planeInfo.isEnableVtol) {
         if(this.getModeSwitchCooldown() > 0) {
            return false;
         } else if(this.getVtolMode() == 1) {
            return false;
         } else if(MathHelper.abs(this.getRotRoll()) > 30.0F) {
            return false;
         } else if(super.onGround && this.planeInfo.isDefaultVtol) {
            return false;
         } else {
            this.setModeSwitchCooldown(20);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean getNozzleStat() {
      return this.partNozzle != null?this.partNozzle.getStatus():false;
   }

   public int getVtolMode() {
      return !this.getNozzleStat()?(this.getNozzleRotation() <= 0.005F?0:1):(this.getNozzleRotation() >= 89.995F?2:1);
   }

   public float getFuleConsumptionFactor() {
      return super.getFuelConsumptionFactor() * (float)(this.getVtolMode() == 2?1:1);
   }

   public float getNozzleRotation() {
      return this.partNozzle != null?this.partNozzle.rotation:0.0F;
   }

   public float getPrevNozzleRotation() {
      return this.partNozzle != null?this.partNozzle.prevRotation:0.0F;
   }

   public void swithVtolMode(boolean mode) {
      if(this.partNozzle != null) {
         if(this.planeInfo.isDefaultVtol && super.onGround && !mode) {
            return;
         }

         if(!super.worldObj.isRemote) {
            this.partNozzle.setStatusServer(mode);
         }

         if(this.getRiddenByEntity() != null && !this.getRiddenByEntity().isDead) {
            this.getRiddenByEntity().rotationPitch = this.getRiddenByEntity().prevRotationPitch = 0.0F;
         }
      }

   }

   protected MCH_Parts createNozzle(MCP_PlaneInfo info) {
      MCH_Parts nozzle = null;
      if(info.haveNozzle() || info.haveRotor() || info.isEnableVtol) {
         nozzle = new MCH_Parts(this, 1, 31, "Nozzle");
         nozzle.rotationMax = 90.0F;
         nozzle.rotationInv = 1.5F;
         nozzle.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         nozzle.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         nozzle.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
         nozzle.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
         nozzle.soundSwitching.setPrm("plane_cv", 1.0F, 0.5F);
         if(info.isDefaultVtol) {
            nozzle.forceSwitch(true);
         }
      }

      return nozzle;
   }

   protected MCH_Parts createWing(MCP_PlaneInfo info) {
      MCH_Parts wing = null;
      if(this.planeInfo.haveWing()) {
         wing = new MCH_Parts(this, 3, 31, "Wing");
         wing.rotationMax = 90.0F;
         wing.rotationInv = 2.5F;
         wing.soundStartSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         wing.soundEndSwichOn.setPrm("plane_cc", 1.0F, 0.5F);
         wing.soundStartSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
         wing.soundEndSwichOff.setPrm("plane_cc", 1.0F, 0.5F);
      }

      return wing;
   }

   public boolean canUseWing() {
      return this.partWing == null?true:(this.getPlaneInfo().isVariableSweepWing?(this.getCurrentThrottle() < 0.2D?this.partWing.isOFF():true):this.partWing.isOFF());
   }

   public boolean canFoldWing() {
      if(this.partWing != null && this.getModeSwitchCooldown() <= 0) {
         if(this.getPlaneInfo().isVariableSweepWing) {
            if(!super.onGround && MCH_Lib.getBlockIdY(this, 3, -20) == 0) {
               if(this.getCurrentThrottle() < 0.699999988079071D) {
                  return false;
               }
            } else if(this.getCurrentThrottle() > 0.10000000149011612D) {
               return false;
            }
         } else {
            if(!super.onGround && MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
               return false;
            }

            if(this.getCurrentThrottle() > 0.009999999776482582D) {
               return false;
            }
         }

         return this.partWing.isOFF();
      } else {
         return false;
      }
   }

   public boolean canUnfoldWing() {
      return this.partWing != null && this.getModeSwitchCooldown() <= 0?this.partWing.isON():false;
   }

   public void foldWing(boolean fold) {
      if(this.partWing != null && this.getModeSwitchCooldown() <= 0) {
         this.partWing.setStatusServer(fold);
         this.setModeSwitchCooldown(20);
      }
   }

   public float getWingRotation() {
      return this.partWing != null?this.partWing.rotation:0.0F;
   }

   public float getPrevWingRotation() {
      return this.partWing != null?this.partWing.prevRotation:0.0F;
   }
}
