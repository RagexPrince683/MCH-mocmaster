package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.plane.MCP_EntityPlane;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponGuidanceSystem extends MCH_EntityGuidanceSystem {

   public World worldObj;
   protected Entity user;
   public Entity lastLockEntity;
   private Entity targetEntity;

   private MCH_EntityAircraft aircraft;



   public MCH_WeaponGuidanceSystem() {
      this((World)null);
   }

   public MCH_WeaponGuidanceSystem(World w) {
      this.worldObj = w;
      this.targetEntity = null;
      this.lastLockEntity = null;
      this.lockCount = 0;
      this.continueLockCount = 0;
      this.lockCountMax = 1;
      this.prevLockCount = 0;
      this.canLockInWater = false;
      this.canLockOnGround = false;
      this.canLockInAir = false;
      this.ridableOnly = false;
      this.lockRange = 300.0D;
      this.lockAngle = 10;
      this.checker = null;
   }

   public void setWorld(World w) {
      this.worldObj = w;
   }

   public void setLockCountMax(int i) {
      this.lockCountMax = i > 0 ? i : 1;
   }

   @Override
   public int getLockCountMax() {
      float stealth = getEntityStealth(this.targetEntity);
      return (int)((float)this.lockCountMax + (float)this.lockCountMax * stealth);
   }
   @Override
   public int getLockCount() {
      return this.lockCount;
   }
   @Override
   public boolean isLockingEntity(Entity entity) {
      return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead && W_Entity.isEqual(entity, this.targetEntity);
   }

   public Entity getLockingEntity() {
      return this.getLockCount() > 0 && this.targetEntity != null && !this.targetEntity.isDead?this.targetEntity:null;
   }

   public Entity getTargetEntity() {
      return this.targetEntity;
   }

   public boolean isLockComplete() {
      return this.getLockCount() == this.getLockCountMax() && this.lastLockEntity != null;
   }

   @Override
   public void update() {
      if(this.worldObj != null && this.worldObj.isRemote) {
         if(this.lockCount != this.prevLockCount) {
            this.prevLockCount = this.lockCount;
         } else {
            this.lockCount = this.prevLockCount = 0;
         }
      }

   }

   public static boolean isEntityOnGround(Entity entity, int height) {
      if(entity != null && !entity.isDead) {
         if(entity.onGround) {
            return true;
         }

         for(int i = 0; i < height; ++i) {
            int x = (int)(entity.posX + 0.5D);
            int y = (int)(entity.posY + 0.5D) - i;
            int z = (int)(entity.posZ + 0.5D);
            int blockId = W_WorldFunc.getBlockId(entity.worldObj, x, y, z);
            if(blockId != 0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public boolean lock(Entity user) {
      this.user = user;
      //if (!this.aircraft.isFreeLookMode()) {
         return this.lock(user, true);
      //}
      //return this.lock(user, false);
   }

   public boolean lock(Entity user, boolean isLockContinue) {

      // 如果是服务器端，则直接返回
      if(!this.worldObj.isRemote) {
         return false;
      } else {

         boolean result = false;  // 锁定结果
         double dz;  // 目标实体在Z轴的距离

         if(this.lockCount == 0) {  // 如果还没有锁定实体
            // 获取范围内的所有实体
            List canLock = this.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.expand(this.lockRange, this.lockRange, this.lockRange));
            Entity potentialTarget = null;  // 潜在的锁定目标
            double dist = this.lockRange * this.lockRange * 2.0D;  // 最大锁定距离

            // 遍历所有实体
            for(int i = 0; i < canLock.size(); ++i) {
               Entity currentEntity = (Entity)canLock.get(i);
               // 检查实体是否可以锁定
               if(this.canLockEntity(currentEntity) ) { //please fucking work
                  //&& !this.aircraft.isFreeLookMode()
                  //do not fucking do this here god hates this
                  //&& !this.aircraft.isFreeLookMode()
                  //todo here I CANT FUCKING ACCESS THIS SHIT FROM HERE AAAAA //this.aircraft.isFreeLookMode()
                  dz = currentEntity.posX - user.posX;
                  double dy = currentEntity.posY - user.posY;
                  double dz1 = currentEntity.posZ - user.posZ;
                  double distance = dz * dz + dy * dy + dz1 * dz1;
                  Entity entityLocker1 = this.getLockEntity(user);
                  float stealth1 = 1.0F - getEntityStealth(currentEntity);
                  double range1 = this.lockRange;
                  // 计算锁定角度
                  float angle = (float)this.lockAngle * (stealth1 / 2.0F + 0.5F);
                  // 判断实体是否在锁定范围内
                  if(distance < range1 * range1 && distance < dist && inLockAngle(entityLocker1, user.rotationYaw, user.rotationPitch, currentEntity, angle)) {
                     // 检测目标是否可见
                     Vec3 v1 = W_WorldFunc.getWorldVec3(this.worldObj, entityLocker1.posX, entityLocker1.posY, entityLocker1.posZ);
                     Vec3 v2 = W_WorldFunc.getWorldVec3(this.worldObj, currentEntity.posX, currentEntity.posY + (double)(currentEntity.height / 2.0F), currentEntity.posZ);
                     MovingObjectPosition m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
                     if(m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
                        potentialTarget = currentEntity;  // 设置锁定目标
                     }
                  }
               }
            }


            this.targetEntity = potentialTarget;  // 将潜在目标设置为当前目标
            if(potentialTarget != null) {
               ++this.lockCount;  // 如果锁定了目标，增加锁定计数
            }
         } else if(this.targetEntity != null && !this.targetEntity.isDead) {  // 如果已经有目标并且目标未死亡
            boolean canLockTarget = true;  // 是否可以继续锁定目标

            if(targetEntity instanceof MCH_EntityAircraft) {
               if(isRadarMissile && targetEntity.getEntityData().getBoolean("ChaffUsing")) {
                  canLockTarget = false;
               }
            }

            // 检查目标是否在水中，如果不能锁定水中目标，则设为false
            if(!this.canLockInWater && this.targetEntity.isInWater()) {
               canLockTarget = false;
            }

            boolean isTargetOnGround = isEntityOnGround(this.targetEntity, lockMinHeight);  // 判断目标是否在地面上
            // 检查目标是否可以锁定在地面
            if(!this.canLockOnGround && isTargetOnGround) {
               canLockTarget = false;
            }

            // 检查目标是否可以锁定在空中
            if(!this.canLockInAir && !isTargetOnGround) {
               canLockTarget = false;
            }

            //todo here
            //if (this.aircraft.isFreeLookMode()) {
            //   canLockTarget = false;
            //}
            //should work please I pray actually I should probably use the bullshit below this for that because
            // I assigned aircraft earlier and java just fucking hates actual working logic because fuck you also
            // fucking goddamn it has no constructor or some bullshit idk fuck this goddamn mod

            //no god hates ts

            MCH_EntityAircraft ac = null; //玩家乘坐的实体
            if(user.ridingEntity instanceof MCH_EntityAircraft) {
               ac = (MCH_EntityAircraft)user.ridingEntity;

               if (ac.isFreeLookMode() && this.canLockInAir) {
                  canLockTarget = false;
                  //NO MORE BITCH SHIT
               }

            } else if(user.ridingEntity instanceof MCH_EntitySeat) {
               ac = ((MCH_EntitySeat)user.ridingEntity).getParent();
            } else if(user.ridingEntity instanceof MCH_EntityUavStation) {
               ac = ((MCH_EntityUavStation)user.ridingEntity).getControlAircract();
            }
            if(ac instanceof MCP_EntityPlane && targetEntity instanceof MCP_EntityPlane) {
               Vector3f playerVelocity = new Vector3f(ac.motionX, ac.motionY, ac.motionZ);  // 玩家机体的速度向量
               Vector3f targetVelocity = new Vector3f(targetEntity.motionX, targetEntity.motionY, targetEntity.motionZ);  // 目标机体的速度向量
               float angleInDegrees = 0;
               if (playerVelocity.length() > 0.001 && targetVelocity.length() > 0.001) {
                  // 计算两个向量的点积
                  float dotProduct = Vector3f.dot(playerVelocity, targetVelocity);
                  // 计算两个向量的长度
                  float playerSpeed = playerVelocity.length();
                  float targetSpeed = targetVelocity.length();
                  // 计算夹角的余弦值
                  float cosAngle = dotProduct / (playerSpeed * targetSpeed);
                  // 确保夹角余弦值在合法范围内 [-1, 1]，避免浮动导致的异常值
                  cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
                  // 计算夹角（弧度）
                  float angle = (float) Math.acos(cosAngle);
                  // 如果夹角大于90度，将其转换为锐角（90度以内）
                  if (angle > Math.PI / 2) {
                     angle = (float) (Math.PI - angle);  // 转换为锐角
                  }
                  // 将角度转化为度数（可选）
                  angleInDegrees = (float) Math.toDegrees(angle);
               }
               if (angleInDegrees > ac.getCurrentWeapon(user).getCurrentWeapon().getInfo().pdHDNMaxDegree) {
                  canLockTarget = false;
               }
            }

            // 如果可以继续锁定
            if(canLockTarget) {
               double dx = this.targetEntity.posX - user.posX;
               double dy = this.targetEntity.posY - user.posY;
               dz = this.targetEntity.posZ - user.posZ;
               float stealth = 1.0F - getEntityStealth(this.targetEntity);
               double lockRange = this.lockRange * (double)stealth;
               // 判断目标是否在锁定范围内
               if(dx * dx + dy * dy + dz * dz < lockRange * lockRange) {
                  if(this.worldObj.isRemote && this.lockSoundCount == 1) {
                     //MCH_PacketNotifyLock.send(this.getTargetEntity());
                  }

                  this.lockSoundCount = (this.lockSoundCount + 1) % 15;
                  Entity entityLocker = this.getLockEntity(user);
                  // 判断目标是否处于锁定范围
                  if(inLockAngle(entityLocker, user.rotationYaw, user.rotationPitch, this.targetEntity, (float)this.lockAngle)) {
                     if(this.lockCount < this.getLockCountMax()) {
                        ++this.lockCount;  // 增加锁定计数
                     }
                  } else if(this.continueLockCount > 0) {
                     --this.continueLockCount;
                     if(this.continueLockCount <= 0 && this.lockCount > 0) {
                        --this.lockCount;
                     }
                  } else {
                     this.continueLockCount = 0;
                     --this.lockCount;
                  }

                  // 如果达到最大锁定计数，则锁定成功
                  if(this.lockCount >= this.getLockCountMax()) {
                     if(this.continueLockCount <= 0) {
                        this.continueLockCount = this.getLockCountMax() / 3;
                        if(this.continueLockCount > 20) {
                           this.continueLockCount = 20;
                        }
                     }

                     result = true;  // 锁定成功
                     this.lastLockEntity = this.targetEntity;
                     if(isLockContinue) {
                        this.prevLockCount = this.lockCount - 1;
                     } else {
                        this.clearLock();
                     }
                  }
               } else {
                  this.clearLock();  // 如果不在锁定范围内，清除锁定
               }
            } else {
               this.clearLock();  // 如果不能继续锁定，清除锁定
            }
         } else {
            this.clearLock();  // 如果目标为空或已死亡，清除锁定
         }

         result = this.lockCount >= this.getLockCountMax();  // 判断是否锁定成功

         if(result) {
            this.lastLockEntity = targetEntity;
            // 播放锁定成功音效
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_basic_tone", 1.0f, 1.0f);
         } else {
            // 播放锁定失败音效
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_lock_tone", 1.0f, 1.0f);
         }

         return result;  // 返回锁定结果
      }
   }


   public static float getEntityStealth(Entity entity) {
      return entity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity).getStealth():(entity != null && entity.ridingEntity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity.ridingEntity).getStealth():0.0F);
   }

   public void clearLock() {
      this.targetEntity = null;
      this.lockCount = 0;
      this.continueLockCount = 0;
      this.lockSoundCount = 0;
   }

   public Entity getLockEntity(Entity entity) {
      if(entity.ridingEntity instanceof MCH_EntityUavStation) {
         MCH_EntityUavStation us = (MCH_EntityUavStation)entity.ridingEntity;
         if(us.getControlAircract() != null) {
            return us.getControlAircract();
         }
      }

      return entity;
   }

   public boolean canLockEntity(Entity entity) {
      // 如果不允许锁定玩家，且实体为玩家，则返回false
      if(this.ridableOnly && entity instanceof EntityPlayer && entity.ridingEntity == null) {
         return false;
      } else {
         // 获取实体的类名
         String className = entity.getClass().getName();

         // 如果实体是 EntityCamera 类型的，返回false
         if(className.indexOf("EntityCamera") >= 0) {
            return false;
         }
         // 红外弹可以锁定热焰弹
         if(this.isHeatSeekerMissile && entity instanceof MCH_EntityFlare) {
            return true;
         }
         // 雷达弹可以锁定箔条
         if(this.isRadarMissile && entity instanceof MCH_EntityChaff) {
            return true;
         }
         // 锁定导弹
         if(this.canLockMissile &&
                 (entity instanceof MCH_EntityAAMissile || entity instanceof MCH_EntityATMissile
                         || entity instanceof MCH_EntityASMissile || entity instanceof MCH_EntityTvMissile)) {
            if(!W_Entity.isEqual(user, ((MCH_EntityBaseBullet) entity).shootingEntity)) {
               return true;
            }
         }
         // 如果实体既不是生物实体，也不是飞机、车辆等特定类型，返回false
         if(!W_Lib.isEntityLivingBase(entity)
                 && !(entity instanceof MCH_EntityAircraft)
                 && className.indexOf("EntityVehicle") < 0
                 && className.indexOf("EntityPlane") < 0
                 && className.indexOf("EntityMecha") < 0
                 && className.indexOf("EntityAAGun") < 0) {
            return false;
         }
         // 如果实体在水中，而不能锁定水中的实体，则返回false
         else if(!this.canLockInWater && entity.isInWater()) {
            return false;
         }
         // 如果有自定义的实体锁定检查器，并且检查器返回false，则返回false
         else if(this.checker != null && !this.checker.canLockEntity(entity)) {
            return false;
         }

         else {
            // 判断实体是否在地面上
            boolean ong = isEntityOnGround(entity, lockMinHeight);
            // 如果可以锁定地面上的实体或实体不在地面上，且可以锁定空中的实体，则返回true
            return (this.canLockOnGround || !ong) && (this.canLockInAir || ong);
         }
      }
   }


   public static boolean inLockAngle(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
      double dx = target.posX - entity.posX;
      double dy = target.posY + (double)(target.height / 2.0F) - entity.posY;
      double dz = target.posZ - entity.posZ;
      float entityYaw = (float)MCH_Lib.getRotate360((double)rotationYaw);
      float targetYaw = (float)MCH_Lib.getRotate360(Math.atan2(dz, dx) * 180.0D / 3.141592653589793D);
      float diffYaw = (float)MCH_Lib.getRotate360((double)(targetYaw - entityYaw - 90.0F));
      double dxz = Math.sqrt(dx * dx + dz * dz);
      float targetPitch = -((float)(Math.atan2(dy, dxz) * 180.0D / 3.141592653589793D));
      float diffPitch = targetPitch - rotationPitch;
      return (diffYaw < lockAng || diffYaw > 360.0F - lockAng) && Math.abs(diffPitch) < lockAng;
   }

   @Override
   protected Entity getLastLockEntity() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public double getLockPosX() {
      return targetEntity.posX;
   }

   @Override
   public double getLockPosY() {
      return targetEntity.posY;
   }

   @Override
   public double getLockPosZ() {
      return targetEntity.posZ;
   }
}
