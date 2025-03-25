package mcheli.plane;

import java.util.Iterator;
import java.util.List;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.*;
import mcheli.chain.MCH_EntityChain;
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
      // 判断是否不处于炮手模式
      if (!super.isGunnerMode) {
         // 获取油门上下状态
         float throttleUpDown = this.getAcInfo().throttleUpDown;

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
            if (this.getRidingEntity() != null) {
               double mx = this.getRidingEntity().motionX;
               double mz = this.getRidingEntity().motionZ;
               // 基于骑乘实体的速度调整油门
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
                  this.addCurrentThrottle(0.01D * (double) f);
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
               this.addCurrentThrottle(-0.01D * (double) throttleUpDown);
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
            this.addCurrentThrottle(-0.005D * (double) throttleUpDown);
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
      this.updateCollisionBox();
      if(this.canFloatWater()) {
         dp = this.getWaterDepth();
      }

      boolean levelOff = super.isGunnerMode;
      if(dp == 0.0D) {
         // 如果是目标无人机，并且有足够的燃料且没有被摧毁，则执行以下代码
         if (this.isTargetDrone() && this.canUseFuel() && !this.isDestroyed()) {

            // 获取无人机当前位置3个单位向下、40个单位向前的方块
            Block throttle = MCH_Lib.getBlockY(this, 3, -100, true);

            // 如果方块不为空且不是空气方块（即存在某个物体）
            if (throttle != null && !W_Block.isEqual(throttle, Blocks.air)) {

               // 如果没有找到目标方块，或者目标方块是空气方块，则执行下面的代码
               throttle = MCH_Lib.getBlockY(this, 3, -5, true);

               // 如果目标方块为空或是空气方块，进行自动驾驶的旋转和俯仰调整
               if (throttle == null || W_Block.isEqual(throttle, Blocks.air)) {

                  // 根据自动驾驶旋转量调整航向（Yaw）
                  this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 2.0F);

                  // 如果俯仰角度大于-20度，则逐渐减小俯仰角度
                  if (this.getRotPitch() > -20.0F) {
                     this.setRotPitch(this.getRotPitch() - 0.5F);
                  }
               }
            } else {
               // 如果没有遇到障碍物，则按照自动驾驶的旋转量调整航向（Yaw）
               this.setRotYaw(this.getRotYaw() + this.getAcInfo().autoPilotRot * 1.0F);

               // 自动调整俯仰角度，使其逐渐减小
               this.setRotPitch(this.getRotPitch() * 0.95F);

               // 如果可以收起起落架，则执行收起起落架的操作
               if (this.canFoldLandingGear()) {
                  this.foldLandingGear();
               }

               // 标记为平稳飞行状态
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

      // 计算油门1的值，当前油门除以10
      float throttle1 = (float)(this.getCurrentThrottle() / 10.0D);
      Vec3 v;

      // 如果喷嘴的旋转角度大于0.001F
      if(this.getNozzleRotation() > 0.001F) {
         // 根据喷嘴旋转角度调整飞机俯仰角度
         this.setRotPitch(this.getRotPitch() * 0.95F);
         // 根据航向角和俯仰角计算方向向量
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - this.getNozzleRotation());
         // 如果喷嘴旋转角度大于等于90度，缩小x和z方向的速度
         if(this.getNozzleRotation() >= 90.0F) {
            v.xCoord *= 0.800000011920929D;
            v.zCoord *= 0.800000011920929D;
         }
      } else {
         // 否则，计算默认的方向向量，俯仰角度减去10度
         v = MCH_Lib.Rot2Vec3(this.getRotYaw(), this.getRotPitch() - 10.0F);
      }

      // 如果没有达到平稳飞行状态
      if(!levelOff) {
         // 如果喷嘴旋转角度小于等于0.01F，根据油门调整垂直方向上的速度
         if(this.getNozzleRotation() <= 0.01F) {
            super.motionY += v.yCoord * (double)throttle1 / 2.0D;
         } else {
            super.motionY += v.yCoord * (double)throttle1 / 8.0D;
         }
      }

      // 判断是否可以在地面移动
      boolean canMove = true;
      if(!this.getAcInfo().canMoveOnGround) {
         // 获取地面方块信息，判断是否可以移动
         Block motion = MCH_Lib.getBlockY(this, 3, -2, false);
         // 如果方块不是水或者空气方块，设置canMove为false，表示不能移动
         if(!W_Block.isEqual(motion, W_Block.getWater()) && !W_Block.isEqual(motion, Blocks.air) && !W_Block.isEqual(motion, Blocks.flowing_water)) {
            canMove = false;
         }
      }

      // 如果可以移动，则更新水平速度
      if(canMove) {
         // 如果启用了倒车功能，并且油门向后，则根据油门倒退
         if (this.getAcInfo().enableBack && super.throttleBack > 0.0F) {
            super.motionX -= v.xCoord * (double) super.throttleBack;
            super.motionZ -= v.zCoord * (double) super.throttleBack;
         } else {
            // 否则，根据油门前进
            super.motionX += v.xCoord * (double) throttle1;
            super.motionZ += v.zCoord * (double) throttle1;
         }
      }

      // 对垂直速度进行衰减
      super.motionY *= 0.95D;
      // 根据飞行器的运动系数衰减水平速度
      super.motionX *= this.getAcInfo().motionFactor;
      super.motionZ *= this.getAcInfo().motionFactor;

      // 计算当前水平速度的大小
      double motion1 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      // 获取最大速度限制
      float speedLimit = this.getMaxSpeed();
      // 如果当前速度超过最大速度限制，按最大速度比例缩小水平速度
      if(motion1 > (double)speedLimit) {
         super.motionX *= (double)speedLimit / motion1;
         super.motionZ *= (double)speedLimit / motion1;
         motion1 = speedLimit;
      }

      // 如果当前速度大于上一帧的速度，并且当前速度小于最大速度限制，逐步增加速度
      if(motion1 > prevMotion && super.currentSpeed < (double)speedLimit) {
         super.currentSpeed += ((double)speedLimit - super.currentSpeed) / 35.0D;
         if(super.currentSpeed > (double)speedLimit) {
            super.currentSpeed = (double)speedLimit;
         }
      } else {
         // 否则逐步减少速度，保持最低速度0.07
         super.currentSpeed -= (super.currentSpeed - 0.07D) / 35.0D;
         if(super.currentSpeed < 0.07D) {
            super.currentSpeed = 0.07D;
         }
      }

      // 如果飞行器在地面或距离地面较近，则缩减水平速度，应用地面俯仰角度
      if(super.onGround || MCH_Lib.getBlockIdY(this, 1, -2) > 0) {
         super.motionX *= this.getAcInfo().motionFactor;
         super.motionZ *= this.getAcInfo().motionFactor;
         // 如果俯仰角度小于40度，则根据地面状态调整俯仰角度
         if(MathHelper.abs(this.getRotPitch()) < 40.0F) {
            this.applyOnGroundPitch(0.8F);
         }
      }

      // 更新飞行器位置
      this.moveEntity(super.motionX, super.motionY, super.motionZ);

      // 更新旋转角度
      this.setRotation(this.getRotYaw(), this.getRotPitch());
      // 更新方块信息
      this.onUpdate_updateBlock();

      // 如果骑乘的实体存在并且已经死亡，则解除骑乘
      if(this.getRiddenByEntity() != null && this.getRiddenByEntity().isDead) {
         this.unmountEntity();
         super.riddenByEntity = null;
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
            final MCH_EntityAircraft rideAc = super.ridingEntity instanceof MCH_EntityAircraft
                    ? (MCH_EntityAircraft) super.ridingEntity
                    : (super.ridingEntity instanceof MCH_EntitySeat
                    ? ((MCH_EntitySeat) super.ridingEntity).getParent()
                    : null);

            // Get a list of entities within the bounding box
            List<Entity> list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, bb.expand(0.3D, 0.3D, 0.3D), new IEntitySelector() {
               @Override
               public boolean isEntityApplicable(Entity e) {
                  // Exclude certain entity types from being affected by collision
                  if (e != rideAc && !(e instanceof EntityItem) && !(e instanceof EntityXPOrb)
                          && !(e instanceof MCH_EntityBaseBullet) && !(e instanceof MCH_EntityChain)
                          && !(e instanceof MCH_EntitySeat)) {

                     // Special handling for tanks
                     if (e instanceof MCP_EntityPlane) {
                        MCP_EntityPlane plane = (MCP_EntityPlane) e;
                        //todo
                        //if (plane.getPlaneInfo() != null && plane.getPlaneInfo().weightType == 2) {
                        //   return MCH_Config.Collision_EntityTankDamage.prmBool;
                        //}
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

                  if (e instanceof MCH_EntityAircraft) {
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
      if(this.getSeatIdByEntity(e) >= 0) {
         return false;
      } else if(super.noCollisionEntities.containsKey(e)) {
         return false;
      } else {
         if(e instanceof MCH_EntityHitBox && ((MCH_EntityHitBox)e).parent != null) {
            MCH_EntityAircraft ac = ((MCH_EntityHitBox)e).parent;
            if(super.noCollisionEntities.containsKey(ac)) {
               return false;
            }
         }

         return e.ridingEntity instanceof MCH_EntityAircraft && super.noCollisionEntities.containsKey(e.ridingEntity)?false:!(e.ridingEntity instanceof MCH_EntitySeat) || ((MCH_EntitySeat)e.ridingEntity).getParent() == null || !super.noCollisionEntities.containsKey(((MCH_EntitySeat)e.ridingEntity).getParent());
      }
   }

   public void updateCollisionBox() {
      if(this.getAcInfo() != null) {
         //this.WheelMng.updateBlock();
         MCH_BoundingBox[] arr$ = super.extraBoundingBox;
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