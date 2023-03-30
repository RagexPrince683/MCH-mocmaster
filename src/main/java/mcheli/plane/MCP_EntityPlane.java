package mcheli.plane;

import java.util.Iterator;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketStatusRequest;
import mcheli.aircraft.MCH_Parts;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCP_EntityPlane extends MCH_EntityAircraft {

   private MCP_PlaneInfo planeInfo = null;
   public float soundVolume;
   public MCH_Parts partNozzle;
   public MCH_Parts partWing;
   public float rotationRotor;
   public float prevRotationRotor;
   public float addkeyRotValue;


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
      return yaw * 0.8F;
   }

   public float getPitchFactor() {
      float pitch = this.getVtolMode() > 0?this.getPlaneInfo().vtolPitch:super.getPitchFactor();
      return pitch * 0.8F;
   }

   public float getRollFactor() {
      float roll = this.getVtolMode() > 0?this.getPlaneInfo().vtolYaw:super.getRollFactor();
      return roll * 0.8F;
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

   public void onUpdateAngles(float partialTicks) {
      if(!this.isDestroyed()) {
         if(super.isGunnerMode) {
            this.setRotPitch(this.getRotPitch() * 0.95F);
            this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 0.2F);
            if(MathHelper.abs(this.getRotRoll()) > 20.0F) {
               this.setRotRoll(this.getRotRoll() * 0.95F);
            }
         }

         boolean isFly = MCH_Lib.getBlockIdY(this, 3, -3) == 0;
         float rot;
         if(isFly && !this.isFreeLookMode() && !super.isGunnerMode && (!this.getAcInfo().isFloat || this.getWaterDepth() <= 0.0D)) {
            if(isFly) {
               MCH_Config var10000 = MCH_MOD.config;
               if(!MCH_Config.MouseControlFlightSimMode.prmBool) {
                  this.rotationByKey(partialTicks);
                  this.setRotRoll(this.getRotRoll() + this.addkeyRotValue * 0.5F * this.getAcInfo().mobilityRoll);
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

         this.addkeyRotValue = (float)((double)this.addkeyRotValue * (1.0D - (double)(0.1F * partialTicks)));
         if(!isFly && MathHelper.abs(this.getRotPitch()) < 40.0F) {
            this.applyOnGroundPitch(0.97F);
         }

         if(this.getNozzleRotation() > 0.001F) {
            rot = 1.0F - 0.03F * partialTicks;
            this.setRotPitch(this.getRotPitch() * rot);
            rot = 1.0F - 0.1F * partialTicks;
            this.setRotRoll(this.getRotRoll() * rot);
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

   }

   protected void onUpdate_ControlNotHovering() {
      if(!super.isGunnerMode) {
         float throttleUpDown = this.getAcInfo().throttleUpDown;
         boolean turn = super.moveLeft && !super.moveRight || !super.moveLeft && super.moveRight;
         float pivotTurnThrottle = this.getAcInfo().pivotTurnThrottle;
         boolean localThrottleUp = super.throttleUp;
         if(turn && this.getCurrentThrottle() < (double)this.getAcInfo().pivotTurnThrottle && !localThrottleUp && !super.throttleDown) {
            localThrottleUp = true;
            throttleUpDown *= 2.0F;
         }

         if(localThrottleUp) {
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
               } else {
                  this.setCurrentThrottle(1.0D);
               }
            }
         } else if(super.throttleDown) {
            if(this.getCurrentThrottle() > 0.0D) {
               this.addCurrentThrottle(-0.01D * (double)throttleUpDown);
            } else {
               this.setCurrentThrottle(0.0D);
               if(this.getAcInfo().enableBack) {
                  super.throttleBack = (float)((double)super.throttleBack + 0.0025D * (double)throttleUpDown);
                  if(super.throttleBack > 0.6F) {
                     super.throttleBack = 0.6F;
                  }
               }
            }
         } else if(super.cs_planeAutoThrottleDown && this.getCurrentThrottle() > 0.0D) {
            this.addCurrentThrottle(-0.005D * (double)throttleUpDown);
            if(this.getCurrentThrottle() <= 0.0D) {
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
               MCH_AircraftInfo.ParticleSplash p = (MCH_AircraftInfo.ParticleSplash)i$.next();

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
               MCH_AircraftInfo.DrawnPart nozzle = (MCH_AircraftInfo.DrawnPart)i$.next();
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
      if(this.canFloatWater()) {
         dp = this.getWaterDepth();
      }

      boolean levelOff = super.isGunnerMode;
      if(dp == 0.0D) {
         if(this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {
            Block throttle = MCH_Lib.getBlockY(this, 3, -40, true);
            if(throttle != null && !W_Block.isEqual(throttle, Blocks.air)) {
               throttle = MCH_Lib.getBlockY(this, 3, -5, true);
               if(throttle == null || W_Block.isEqual(throttle, Blocks.air)) {
                  this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 2.0F);
                  if(this.getRotPitch() > -20.0F) {
                     this.setRotPitch(this.getRotPitch() - 0.5F);
                  }
               }
            } else {
               this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 1.0F);
               this.setRotPitch(this.getRotPitch() * 0.95F);
               if(this.canFoldLandingGear()) {
                  this.foldLandingGear();
               }

               levelOff = true;
            }
         }

         if(!levelOff) {
            super.motionY += 0.04D + (double)(!this.isInWater()?this.getAcInfo().gravity:this.getAcInfo().gravityInWater);
            super.motionY += -0.047D * (1.0D - this.getCurrentThrottle());
         } else {
            super.motionY *= 0.8D;
         }
      } else {
         this.setRotPitch(this.getRotPitch() * 0.8F, "getWaterDepth != 0");
         if(MathHelper.abs(this.getRotRoll()) < 40.0F) {
            this.setRotRoll(this.getRotRoll() * 0.9F);
         }

         if(dp < 1.0D) {
            super.motionY -= 1.0E-4D;
            super.motionY += 0.007D * this.getCurrentThrottle();
         } else {
            if(super.motionY < 0.0D) {
               super.motionY /= 2.0D;
            }

            super.motionY += 0.007D;
         }
      }

      float throttle1 = (float)(this.getCurrentThrottle() / 10.0D);
      Vec3 v;
      if(this.getNozzleRotation() > 0.001F) {
         this.setRotPitch(this.getRotPitch() * 0.95F);
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - this.getNozzleRotation());
         if(this.getNozzleRotation() >= 90.0F) {
            v.xCoord *= 0.800000011920929D;
            v.zCoord *= 0.800000011920929D;
         }
      } else {
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);
      }

      if(!levelOff) {
         if(this.getNozzleRotation() <= 0.01F) {
            super.motionY += v.yCoord * (double)throttle1 / 2.0D;
         } else {
            super.motionY += v.yCoord * (double)throttle1 / 8.0D;
         }
      }

      boolean canMove = true;
      if(!this.getAcInfo().canMoveOnGround) {
         Block motion = MCH_Lib.getBlockY(this, 3, -2, false);
         if(!W_Block.isEqual(motion, W_Block.getWater()) && !W_Block.isEqual(motion, Blocks.air) && !W_Block.isEqual(motion, Blocks.flowing_water)) {
            canMove = false;
         }
      }

      if(canMove) {
         if(this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
            super.motionX -= v.xCoord * (double)super.throttleBack;
            super.motionZ -= v.zCoord * (double)super.throttleBack;
         } else {
            super.motionX += v.xCoord * (double)throttle1;
            super.motionZ += v.zCoord * (double)throttle1;
         }
      }

      double motion1 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      float speedLimit = this.getMaxSpeed();
      if(motion1 > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion1;
         super.motionZ *= (double)speedLimit / motion1;
         motion1 = (double)speedLimit;
      }

      if(motion1 > prevMotion && super.currentSpeed < (double)speedLimit) {
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

      if(super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
         super.motionX *= (double)this.getAcInfo().motionFactor;
         super.motionZ *= (double)this.getAcInfo().motionFactor;
         if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
            this.applyOnGroundPitch(0.8F);
         }
      }

      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      super.motionY *= 0.95D;
      super.motionX *= (double)this.getAcInfo().motionFactor;
      super.motionZ *= (double)this.getAcInfo().motionFactor;
      this.setRotation(this.getRotYaw(), this.getRotPitch());
      this.onUpdate_updateBlock();
      if(this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
         this.unmountEntity();
         super.riddenByEntity = null;
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
