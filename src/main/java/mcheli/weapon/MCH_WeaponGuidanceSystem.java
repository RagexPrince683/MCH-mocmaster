package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.uav.MCH_EntityUavStation;
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

public class MCH_WeaponGuidanceSystem extends MCH_GuidanceSystem{

	public Entity lastLockEntity;
	private Entity targetEntity;


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
	      this.lockRange = 100.0D;
	      this.lockAngle = 10;
	      this.checker = null;
	   }
	   
	   public void setWorld(World w) {
	      this.worldObj = w;
	   }

	   public void setLockCountMax(int i) {
	      this.lockCountMax = i > 0?i:1;
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

	   public static boolean isEntityOnGround(Entity entity) {
	      if(entity != null && !entity.isDead) {
	         if(entity.onGround) {
	            return true;
	         }

	         for(int i = 0; i < 12; ++i) {
	            int x = (int)(entity.posX + 0.5D);
	            int y = (int)(entity.posY + 0.5D) - i;
	            int z = (int)(entity.posZ + 0.5D);
	            int blockId = W_WorldFunc.getBlockId(entity.worldObj, x, y, z);
	            if(blockId != 0 && !W_WorldFunc.isBlockWater(entity.worldObj, x, y, z)) {
	               return true;
	            }
	         }
	      }

	      return false;
	   }
	   
	   @Override
	   public boolean lock(Entity user) {
	      return this.lock(user, true);
	   }

	   public boolean lock(Entity user, boolean isLockContinue) {
		   
	      if(!this.worldObj.isRemote) {
	         return false;
	      } else {
	         boolean result = false;
	         double dz;
	         if(this.lockCount == 0) {
	            List canLock = this.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.expand(this.lockRange, this.lockRange, this.lockRange));
	            Entity dx = null;
	            double dist = this.lockRange * this.lockRange * 2.0D;

	            for(int i = 0; i < canLock.size(); ++i) {
	               Entity ong = (Entity)canLock.get(i);
	               if(this.canLockEntity(ong)) {
	                  dz = ong.posX - user.posX;
	                  double stealth = ong.posY - user.posY;
	                  double dz1 = ong.posZ - user.posZ;
	                  double d = dz * dz + stealth * stealth + dz1 * dz1;
	                  Entity entityLocker1 = this.getLockEntity(user);
	                  float stealth1 = 1.0F - getEntityStealth(ong);
	                  double range1 = this.lockRange;
	                  float angle = (float)this.lockAngle * (stealth1 / 2.0F + 0.5F);
	                  if(d < range1 * range1 && d < dist && inLockRange(entityLocker1, user.rotationYaw, user.rotationPitch, ong, angle)) {
	                     Vec3 v1 = W_WorldFunc.getWorldVec3(this.worldObj, entityLocker1.posX, entityLocker1.posY, entityLocker1.posZ);
	                     Vec3 v2 = W_WorldFunc.getWorldVec3(this.worldObj, ong.posX, ong.posY + (double)(ong.height / 2.0F), ong.posZ);
	                     MovingObjectPosition m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
	                     if(m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
	                        dx = ong;
	                     }
	                  }
	               }
	            }

	            this.targetEntity = dx;
	            if(dx != null) {
	               ++this.lockCount;
	            }
	         } else if(this.targetEntity != null && !this.targetEntity.isDead) {
	            boolean var26 = true;
	            if(!this.canLockInWater && this.targetEntity.isInWater()) {
	               var26 = false;
	            }

	            boolean var27 = isEntityOnGround(this.targetEntity);
	            if(!this.canLockOnGround && var27) {
	               var26 = false;
	            }

	            if(!this.canLockInAir && !var27) {
	               var26 = false;
	            }

	            if(var26) {
	               double var28 = this.targetEntity.posX - user.posX;
	               double dy = this.targetEntity.posY - user.posY;
	               dz = this.targetEntity.posZ - user.posZ;
	               float var29 = 1.0F - getEntityStealth(this.targetEntity);
	               double range = this.lockRange * (double)var29;
	               if(var28 * var28 + dy * dy + dz * dz < range * range) {
	                  if(this.worldObj.isRemote && this.lockSoundCount == 1) {
	                     //MCH_PacketNotifyLock.send(this.getTargetEntity());
	                	  
	                  }

	                  this.lockSoundCount = (this.lockSoundCount + 1) % 15;
	                  Entity entityLocker = this.getLockEntity(user);
	                  if(inLockRange(entityLocker, user.rotationYaw, user.rotationPitch, this.targetEntity, (float)this.lockAngle)) {
	                     if(this.lockCount < this.getLockCountMax()) {
	                        ++this.lockCount;
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

	                  if(this.lockCount >= this.getLockCountMax()) {
	                     if(this.continueLockCount <= 0) {
	                        this.continueLockCount = this.getLockCountMax() / 3;
	                        if(this.continueLockCount > 20) {
	                           this.continueLockCount = 20;
	                        }
	                     }

	                     result = true;
	                     this.lastLockEntity = this.targetEntity;
	                     if(isLockContinue) {
	                        this.prevLockCount = this.lockCount - 1;
	                     } else {
	                        this.clearLock();
	                     }
	                  }
	               } else {
	                  this.clearLock();
	               }
	            } else {
	               this.clearLock();
	            }
	         } else {
	            this.clearLock();
	         }
	         result = this.lockCount >= this.getLockCountMax();
	         
	         if(result) {
	        	 this.lastLockEntity= targetEntity;
	        	 this.worldObj.playSoundAtEntity(user, "mcheli:ir_basic_tone", 1.0f, 1.0f);
	        	 //playSound(user, );
	        	 //System.out.println("Lock successful. " + lastLockEntity.getCommandSenderName());
	         }else {
	    		 // playSound(user, "ir_lock_tone");
	    		  this.worldObj.playSoundAtEntity(user, "mcheli:ir_lock_tone", 1.0f, 1.0f);
	    	  }
	    	 // System.out.println("Locking! Result: " + result + " LockCount " + lockCount + " countMax " + getLockCountMax());

	         
	         return result;
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
	      if(this.ridableOnly && entity instanceof EntityPlayer && entity.ridingEntity == null) {
	         return false;
	      } else {
	         String className = entity.getClass().getName();
	         if(className.indexOf("EntityCamera") >= 0) {
	            return false;
	         } else if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntityAircraft) && className.indexOf("EntityVehicle") < 0 && className.indexOf("EntityPlane") < 0 && className.indexOf("EntityMecha") < 0 && className.indexOf("EntityAAGun") < 0) {
	            return false;
	         } else if(!this.canLockInWater && entity.isInWater()) {
	            return false;
	         } else if(this.checker != null && !this.checker.canLockEntity(entity)) {
	            return false;
	         } else {
	            boolean ong = isEntityOnGround(entity);
	            return !this.canLockOnGround && ong?false:this.canLockInAir || ong;
	         }
	      }
	   }

	   public static boolean inLockRange(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
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
}
