package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.MCH_PacketNotifyLock;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.sensors.MCH_RadarContact;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_RadarGuidanceSystem extends MCH_GuidanceSystem{

   public MCH_RadarContact lastLockEntity;
   private MCH_RadarContact targetEntity;

   public MCH_RadarGuidanceSystem() {
      this((World)null);
   }

   public MCH_RadarGuidanceSystem(World w) {
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
      this.lockRange = 5000.0D;
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
      //float stealth = getEntityStealth(this.targetEntity);
      return (int)((float)this.lockCountMax + (float)this.lockCountMax);
   }
   @Override
   public int getLockCount() {
      return this.lockCount;
   }

   public boolean isLockingEntity(Entity entity) {
	   
      return this.getLockCount() > 0 && this.targetEntity != null; //&& !this.targetEntity.isDead && W_Entity.isEqual(entity, this.targetEntity);
   }

   public MCH_RadarContact getLockingEntity() {
      return this.getLockCount() > 0 && this.targetEntity != null ?this.targetEntity:null;
   }

   public MCH_RadarContact getTargetEntity() {
      return this.targetEntity;
   }

   public boolean isLockComplete() {
      return this.getLockCount() >= this.getLockCountMax();
   }

   @Override
   public void update() {
      if(this.worldObj != null && this.worldObj.isRemote) {
         if(this.lockCount != this.prevLockCount) {
            this.prevLockCount = this.lockCount;
         } else {
            this.lockCount = this.prevLockCount = 0;
         }
         //if(this.lockCount >= this.lockCountMax) {
        	// this.lockCount = this.lockCountMax;
         //}
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
   
   public void lock2(Entity user, boolean isLockContinue) {
	   if(!this.worldObj.isRemote) {
	         return ;
	      } else {
	    	  boolean result = false;
	    	  if(user.ridingEntity instanceof MCH_EntityAircraft) { //If user is riding an aircraft
	    		  MCH_EntityAircraft ac = (MCH_EntityAircraft)user.ridingEntity; //Get their AC
	 	         if(this.lockCount == 0) { 
	 	        	MCH_RadarContact target = ac.radarTarget;
	 	        	if(true) {
	 	        		this.targetEntity = target;
	 	        		result = true;
	 	        	}
	 	         }
	    	  }
	      }
   }
   
   public boolean lock(Entity user, boolean isLockContinue) {
      if(!this.worldObj.isRemote) {
         return false;
      } else {
         boolean result = false;
         MCH_RadarContact target = null;
         if(user.ridingEntity instanceof MCH_EntityAircraft) {
        	 MCH_EntityAircraft ac = (MCH_EntityAircraft)user.ridingEntity;
        	 
        	 target = ac.radarTarget;
        	 if(target == null) {return false;}
	         if(this.lockCount == 0) {
	        	 //List canLock = ac.contacts;
	            //List canLock = this.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.expand(this.lockRange, this.lockRange, this.lockRange));
	        	//MCH_WeaponSet ws = ac.getCurrentWeapon(user);
	        	//if(ws.getCurrentWeapon() instanceof MCH_WeaponAAMissile) {
	        		
	        		//this.lockRange = ws.getCurrentWeapon().weaponInfo.radius;
	        		//ac.print("Changing lock range to " + lockRange);
	        	//}
	            double dist = this.lockRange * this.lockRange * 2.0D;
	            //System.out.println("yeo");
	           // for(int i = 0; i < canLock.size(); ++i) {
	            	//System.out.println("fock");
	              // MCH_RadarContact contact = (MCH_RadarContact)canLock.get(i);
	               if(true) {//if(this.canLockEntity(ong)) {
	                  double dx = target.x - user.posX;
	                  double dy = target.y - user.posY;
	                  double dz = target.z - user.posZ;
	                  double d = dx * dx + dy * dy + dz * dz;
	                  Entity entityLocker1 = this.getLockEntity(user);
	                  //float stealth1 = 1.0F - getEntityStealth(ong);
	                  double range1 = this.lockRange;// * //(double)stealth1;
	                  float angle = (float)this.lockAngle;//	 * (dy / 2.0F + 0.5F);
	                //  if(d < range1 * range1 && d < dist && inLockRange(entityLocker1, user.rotationYaw, user.rotationPitch, target, angle)) {
	                    if(d <= dist) {
	                    	
	                  	Vec3 v1 = W_WorldFunc.getWorldVec3(this.worldObj, entityLocker1.posX, entityLocker1.posY, entityLocker1.posZ);
	                     Vec3 v2 = W_WorldFunc.getWorldVec3(this.worldObj, target.x, target.y, target.z);
	                     MovingObjectPosition m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
	                     if(m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
	                       // target = contact;
	                        result=true;
	                     }else {
	                    	 //System.out.println("fucc");
	                     }
	                  }else {
	                	  return false;
	                  }
	               }
	            }
	
	            this.targetEntity = target;
	            if(target != null) {
	               ++this.lockCount;
	            }
	         } else if(this.targetEntity != null) {
	            boolean var26 = true;
	            //if(!this.canLockInWater && this.targetEntity.isInWater()) {
	              // var26 = false;
	           // }
	
	         //   boolean var27 = isEntityOnGround(this.targetEntity);
	           // if(!this.canLockOnGround && var27) {
	             //  var26 = false;
	           // }
	
	            //if(!this.canLockInAir && !var27) {
	             //  var26 = false;
	            //}
	
	            if(var26) {
	               double var28 = this.targetEntity.x - user.posX;
	               double dy = this.targetEntity.y - user.posY;
	               double dz = this.targetEntity.z - user.posZ;
	               float var29 = 1.0F;// - getEntityStealth(this.targetEntity);
	               double range = this.lockRange * (double)var29;
	              if(true) {// if(var28 * var28 + dy * dy + dz * dz < range * range) {
	                  if(this.worldObj.isRemote && this.lockSoundCount == 1) {
	                     MCH_PacketNotifyLock.send(this.targetEntity.entityID);
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
	                	 this.lockCount = this.getLockCountMax();
	                	 //System.out.println("Yeet!");
	                	 result = true;
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
         }
         
         //System.out.println("LOCK RESULT " + result + " tgt " + this.lastLockEntity);
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
   
   public static boolean inLockRange(Entity entity, float rotationYaw, float rotationPitch, MCH_RadarContact target, float lockAng) {
	      if(target == null) {
	    	  System.out.println("TARGET IS NULL");
	    	  return false;
	      }
	      if(entity == null) {
	    	  System.out.println("ENTITY IS NULL");
	    	  return false;
	      }
	   	  double dx = target.x - entity.posX;
	      double dy = target.y + (double)(target.height / 2.0F) - entity.posY;
	      double dz = target.z - entity.posZ;
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
	
	return null;
}
}
