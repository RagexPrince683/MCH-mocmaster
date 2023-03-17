package mcheli.helicopter;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_PacketStatusRequest;
import mcheli.aircraft.MCH_Rotor;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Iterator;

public class MCH_EntityHeli extends MCH_EntityAircraft {

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
   public float goalAngle = 70.0F;
   public boolean yawLeft = false;
   public boolean yawRight = false;
   
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
         this.newSeats(this.getAcInfo().getNumSeatAndRack());
         this.createRotors();
         super.weapons = this.createWeapon(1 + this.getSeatNum());
         this.initPartRotation(this.getRotYaw(), this.getRotPitch());
      }

   }

   public Item getItem() {
      return this.getHeliInfo() != null?this.getHeliInfo().item:null;
   }

   public boolean canMountWithNearEmptyMinecart() {
      MCH_Config var10000 = MCH_MOD.config;
      return MCH_Config.MountMinecartHeli.prmBool;
   }
   
   public float getMaxMove() {
	   return 1.0F;
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
      this.setFoldBladeStat((byte)(par1NBTTagCompound.getBoolean("FoldBlade")?0:2));
      if(this.heliInfo == null) {
         this.heliInfo = MCH_HeliInfoManager.get(this.getTypeName());
         if(this.heliInfo == null) {
            MCH_Lib.Log((Entity)this, "##### MCH_EntityHeli readEntityFromNBT() Heli info null %d, %s", new Object[]{Integer.valueOf(W_Entity.getEntityId(this)), this.getEntityName()});
            this.setDead(true);
         } else {
            this.setAcInfo(this.heliInfo);
         }
      }

      if(!beforeFoldBlade && this.getFoldBladeStat() == 0) {
         this.forceFoldBlade();
      }

      this.prevRotationRotor = this.rotationRotor;
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

         this.updateWeapons();
         this.onUpdate_Seats();
         this.onUpdate_Control();
         this.onUpdate_Rotor();
         super.prevPosX = super.posX;
         super.prevPosY = super.posY;
         super.prevPosZ = super.posZ; 
         if(!this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < this.goalAngle) { //if(!this.isDestroyed() && this.isHovering() && MathHelper.abs(this.getRotPitch()) < 70.0F) {
            this.setRotPitch(this.getRotPitch() * 0.95F);
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
      return super.canMouseRot();
   }

   public boolean canUpdatePitch(Entity player) {
      return super.canUpdatePitch(player) && !this.isHovering();
   }

   public boolean canUpdateRoll(Entity player) {
      return super.canUpdateRoll(player) && !this.isHovering();
   }

   public boolean isOverridePlayerPitch() {
      return super.isOverridePlayerPitch() && !this.isHovering();
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
      return mouseX;
   }

   public float getControlRotPitch(float mouseX, float mouseY, float tick) {
      return mouseY;
   }

   public float getControlRotRoll(float mouseX, float mouseY, float tick) {
      return mouseX;
   }

   public void onUpdateAngles(float partialTicks) {
      if(!this.isDestroyed()) {
         float rotRoll = !this.isHovering()?0.04F:0.07F;
         rotRoll = 1.0F - rotRoll * partialTicks;
         if((double)this.getRotRoll() > 0.1D && this.getRotRoll() < 65.0F) {
            this.setRotRoll(this.getRotRoll() * rotRoll);
         }

         if((double)this.getRotRoll() < -0.1D && this.getRotRoll() > -65.0F) {
            this.setRotRoll(this.getRotRoll() * rotRoll);
         }

         if(MCH_Lib.getBlockIdY(this, 3, -3) == 0) {
            if(super.moveLeft && !super.moveRight) {
               this.setRotRoll(this.getRotRoll() - 1.2F * partialTicks);
            }

            if(super.moveRight && !super.moveLeft) {
               this.setRotRoll(this.getRotRoll() + 1.2F * partialTicks);
            }
            if(super.moveUp && ! super.moveDown) {
            	 this.setRotPitch(this.getRotPitch() + 1.2F * partialTicks);
            }else if(super.moveDown && ! super.moveUp) {
            	this.setRotPitch(this.getRotPitch() - 1.2F * partialTicks);
            }
         } else {
            if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
               this.applyOnGroundPitch(0.97F);
            }

            if(this.heliInfo.isEnableFoldBlade && this.rotors.length > 0 && this.getFoldBladeStat() == 0 && !this.isDestroyed()) {
               if(super.moveLeft && !super.moveRight) {
                  this.setRotYaw(this.getRotYaw() - 0.5F * partialTicks);
               }

               if(super.moveRight && !super.moveLeft) {
                  this.setRotYaw(this.getRotYaw() + 0.5F * partialTicks);
               }
            }
         }
         if(this.yawRight && ! this.yawLeft) {
        	 this.setRotYaw(this.getRotYaw() + 1.0F * partialTicks);
         }else if(this.yawLeft && !this.yawRight) {
        	 this.setRotYaw(this.getRotYaw() - 1.0F * partialTicks);
         }
      }
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
      if(this.isHoveringMode() && !this.canUseFuel(true)) {
         this.switchHoveringMode(false);
      }

      if(super.isGunnerMode && !this.canUseFuel()) {
         this.switchGunnerMode(false);
      }

      if(!this.isDestroyed() && (this.getRiddenByEntity() != null || this.isHoveringMode()) && this.canUseBlades() && this.isCanopyClose() && this.canUseFuel(true)) {
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
      float rp1 = (float)(1.0D - this.getCurrentThrottle());
      this.rotationRotor += (double)((1.0F - rp1 * rp1 * rp1) * this.getAcInfo().rotorSpeed);
      this.rotationRotor %= 360.0D;
      
   }

   protected void onUpdate_ControlNotHovering() {
      float throttleUpDown = this.getAcInfo().throttleUpDown;
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
      } else if((!super.worldObj.isRemote || W_Lib.isClientPlayer(this.getRiddenByEntity())) && super.cs_heliAutoThrottleDown) {
         if(this.getCurrentThrottle() > 0.52D) {
            this.addCurrentThrottle(-0.01D * (double)throttleUpDown);
         } else if(this.getCurrentThrottle() < 0.48D) {
            this.addCurrentThrottle(0.01D * (double)throttleUpDown);
         }
      }

      if(!super.worldObj.isRemote) {
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

   protected void onUpdate_ControlHovering() {
      if(this.getCurrentThrottle() < 1.0D) {
         this.addCurrentThrottle(0.03333333333333333D);
      } else {
         this.setCurrentThrottle(1.0D);
      }

      if(!super.worldObj.isRemote) {
         boolean move = false;
         float yaw = this.getRotYaw();
         double x = 0.0D;
         double z = 0.0D;
         if(super.throttleUp) {
        	this.goalAngle -= 1;
            yaw = this.getRotYaw();
            x += Math.sin((double)yaw * 3.141592653589793D / 180.0D);
            z += Math.cos((double)yaw * 3.141592653589793D / 180.0D);
            move = true;
         }

         if(super.throttleDown) {
        	this.goalAngle += 1;
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
        	this.setRotPitch(this.goalAngle);
            double d = Math.sqrt(x * x + z * z);
            super.motionX -= x / d * 0.009999999776482582D * (double)this.getAcInfo().speed;
            super.motionZ += z / d * 0.009999999776482582D * (double)this.getAcInfo().speed;
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
                           int ebi = super.rand.nextInt(1 + super.extraBoundingBox.length);
                           if(p < 0.3D && ebi > 0) {
                              AxisAlignedBB bb = super.extraBoundingBox[ebi - 1].boundingBox;
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
      double prevMotion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      float ogp = this.getAcInfo().onGroundPitch;
      double motion;
      float speedLimit;
      float pitch;
      if(!this.isHovering()) {
         motion = 0.0D;
         if(this.canFloatWater()) {
            motion = this.getWaterDepth();
         }

         if(motion == 0.0D) {
            super.motionY += !this.isInWater()?(double)this.getAcInfo().gravity:(double)this.getAcInfo().gravityInWater;
            speedLimit = this.getRotYaw() / 180.0F * 3.1415927F;
            pitch = this.getRotPitch();
            if(MCH_Lib.getBlockIdY(this, 3, -3) > 0) {
               pitch -= ogp;
            }

            super.motionX += 0.1D * (double)MathHelper.sin(speedLimit) * super.currentSpeed * (double)(-(pitch * pitch * pitch / 30000.0F)) * this.getCurrentThrottle();
            super.motionZ += 0.1D * (double)MathHelper.cos(speedLimit) * super.currentSpeed * (double)(pitch * pitch * pitch / 30000.0F) * this.getCurrentThrottle();
            double y = (double)(MathHelper.abs(this.getRotPitch()) + MathHelper.abs(this.getRotRoll()));
            y *= 0.6000000238418579D;
            if(y <= 50.0D) {
               y = 1.0D - y / 50.0D;
            } else {
               y = 0.0D;
            }

            double throttle = this.getCurrentThrottle();
            if(this.isDestroyed()) {
               throttle *= 0.65D;
            }

            super.motionY += (y * 0.025D+0.023D) * throttle;
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
         if(super.rand.nextInt(50) == 0) {
            super.motionX += (super.rand.nextDouble() - 0.5D) / 30.0D;
         }

         if(super.rand.nextInt(50) == 0) {
            super.motionY += (super.rand.nextDouble() - 0.5D) / 50.0D;
         }

         if(super.rand.nextInt(50) == 0) {
            super.motionZ += (super.rand.nextDouble() - 0.5D) / 30.0D;
         }
      }

      motion = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      speedLimit = this.getAcInfo().speed;
      if(motion > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion;
         super.motionZ *= (double)speedLimit / motion;
         motion = (double)speedLimit;
      }

      if(motion > prevMotion && super.currentSpeed < (double)speedLimit) {
         super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
         if(super.currentSpeed > (double)speedLimit) {
            super.currentSpeed = (double)speedLimit;
         }
      } else {
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

      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      super.motionY *= 0.95D;
      super.motionX *= 0.99D;
      super.motionZ *= 0.99D;
      this.setRotation(this.getRotYaw(), this.getRotPitch());
      this.onUpdate_updateBlock();
      if(this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
         this.unmountEntity();
         super.riddenByEntity = null;
      }

   }
}
