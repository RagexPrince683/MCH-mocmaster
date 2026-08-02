package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.plane.MCP_EntityPlane;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.ship.MCH_EntityShip;
import mcheli.tank.MCH_EntityTank;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponGuidanceSystem extends MCH_EntityGuidanceSystem {

   public enum TargetDomain {
      AIR,
      GROUND,
      SURFACE,
      UNDERWATER,
      UNKNOWN
   }

   public enum TargetCategory {
      AIRCRAFT,
      GROUND_VEHICLE,
      SHIP,
      LIVING,
      MISSILE,
      COUNTERMEASURE,
      UNKNOWN
   }

   public World worldObj;
   protected Entity user;
   public Entity lastLockEntity;
   private Entity targetEntity;

   private MCH_EntityBaseVehicle aircraft;



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

         // LockMinHeight remains the optional look-down distance.  A one-block contact
         // probe is always retained for compatibility when it is zero.
         int probeHeight = Math.max(1, height);
         for(int i = 0; i < probeHeight; ++i) {
            int x = (int)Math.floor(entity.posX);
            int y = (int)Math.floor(entity.boundingBox.minY - 0.01D) - i;
            int z = (int)Math.floor(entity.posZ);
            int blockId = W_WorldFunc.getBlockId(entity.worldObj, x, y, z);
            if(blockId != 0) {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Uses the physics flag plus a narrow, full-footprint block probe.  Unlike
    * LockMinHeight, this is contact detection and cannot classify low flight as ground.
    */
   public static boolean hasAircraftGroundContact(Entity entity) {
      if(entity == null || entity.isDead || entity.worldObj == null || entity.boundingBox == null) {
         return false;
      }
      if(entity.onGround) {
         return true;
      }
      AxisAlignedBB contactBox = entity.boundingBox.copy();
      contactBox.minY -= 0.0625D;
      contactBox.maxY = entity.boundingBox.minY + 0.001D;
      return entity.worldObj.checkBlockCollision(contactBox);
   }

   /** Classifies stable vehicle roles before using transient physical contact. */
   public static TargetDomain getTargetDomain(Entity entity, int groundProbeHeight) {
      if(entity == null || entity.isDead) {
         return TargetDomain.UNKNOWN;
      }
      if(entity instanceof MCP_EntityPlane || entity instanceof MCH_EntityHeli) {
         return hasAircraftGroundContact(entity) ? TargetDomain.GROUND : TargetDomain.AIR;
      }
      if(entity instanceof MCH_EntityShip) {
         MCH_EntityShip ship = (MCH_EntityShip)entity;
         return ship.isDiving && ship.isInWater() ? TargetDomain.UNDERWATER : TargetDomain.SURFACE;
      }
      if(entity instanceof MCH_EntityTank || entity instanceof MCH_EntityTurret
              || entity instanceof MCH_EntityUavStation) {
         return TargetDomain.GROUND;
      }

      String className = entity.getClass().getName();
      // Preserve the legacy external-mod compatibility contract, but give its known
      // roles stable domains instead of treating all vehicles as airborne off blocks.
      if(className.indexOf("EntityPlane") >= 0) {
         return hasAircraftGroundContact(entity) ? TargetDomain.GROUND : TargetDomain.AIR;
      }
      if(className.indexOf("EntityVehicle") >= 0 || className.indexOf("EntityMecha") >= 0
              || className.indexOf("EntityAAGun") >= 0) {
         return TargetDomain.GROUND;
      }
      if(entity instanceof MCH_EntityBaseVehicle) {
         return TargetDomain.UNKNOWN;
      }
      // A transient movement state does not change an ordinary entity's physical role.
      // In particular, jumping players and falling mobs are not aircraft.
      if(W_Lib.isEntityLivingBase(entity)) {
         return TargetDomain.GROUND;
      }
      return TargetDomain.UNKNOWN;
   }

   /** Returns the stable role independently of the target's current physical domain. */
   public static TargetCategory getTargetCategory(Entity entity) {
      if(entity == null || entity.isDead) {
         return TargetCategory.UNKNOWN;
      }
      if(entity instanceof MCH_EntityFlare || entity instanceof MCH_EntityChaff) {
         return TargetCategory.COUNTERMEASURE;
      }
      if(entity instanceof MCH_EntityBaseBullet) {
         return TargetCategory.MISSILE;
      }
      if(entity instanceof MCP_EntityPlane || entity instanceof MCH_EntityHeli) {
         return TargetCategory.AIRCRAFT;
      }
      if(entity instanceof MCH_EntityShip) {
         return TargetCategory.SHIP;
      }
      if(entity instanceof MCH_EntityTank || entity instanceof MCH_EntityTurret
              || entity instanceof MCH_EntityUavStation) {
         return TargetCategory.GROUND_VEHICLE;
      }
      String className = entity.getClass().getName();
      if(className.indexOf("EntityPlane") >= 0) {
         return TargetCategory.AIRCRAFT;
      }
      if(className.indexOf("EntityVehicle") >= 0 || className.indexOf("EntityMecha") >= 0
              || className.indexOf("EntityAAGun") >= 0) {
         return TargetCategory.GROUND_VEHICLE;
      }
      if(W_Lib.isEntityLivingBase(entity)) {
         return TargetCategory.LIVING;
      }
      return TargetCategory.UNKNOWN;
   }

   private boolean isDomainAllowed(Entity entity) {
      TargetDomain domain = getTargetDomain(entity, this.lockMinHeight);
      if(domain == TargetDomain.GROUND) {
         return this.canLockOnGround;
      }
      if(domain == TargetDomain.AIR) {
         return this.canLockInAir;
      }
      if(domain == TargetDomain.SURFACE) {
         return this.canLockInWater;
      }
      if(domain == TargetDomain.UNDERWATER) {
         return this.canLockInWater;
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
      if(user == null || user.isDead || this.worldObj == null || user.worldObj != this.worldObj) {
         this.clearLock();
         return false;
      }
      // If server side, returns immediately
      if(!this.worldObj.isRemote) {
         return false;
      } else {

         boolean result = false;  // Lock result
         double dz;  // Distance to target entity on the Z axis

         if(this.lockCount == 0) {  // If no entity has been locked yet
            // Gets all entities within range
            List canLock = this.worldObj.getEntitiesWithinAABBExcludingEntity(user, user.boundingBox.expand(this.lockRange, this.lockRange, this.lockRange));
            Entity potentialTarget = null;  // Potential lock target
            double dist = this.lockRange * this.lockRange * 2.0D;  // Maximum lock distance

            // Iterates over all entities
            for(int i = 0; i < canLock.size(); ++i) {
               Entity currentEntity = (Entity)canLock.get(i);
               // Checks whether entity can be locked
               if(this.canLockEntity(currentEntity)) {
                  dz = currentEntity.posX - user.posX;
                  double dy = currentEntity.posY - user.posY;
                  double dz1 = currentEntity.posZ - user.posZ;
                  double distance = dz * dz + dy * dy + dz1 * dz1;
                  Entity entityLocker1 = this.getLockEntity(user);
                  float stealth1 = 1.0F - getEntityStealth(currentEntity);
                  double range1 = this.lockRange;
                  // Calculates lock angle
                  float angle = (float)this.lockAngle * (stealth1 / 2.0F + 0.5F);
                  // Determines whether entity is within lock range
                  if(distance < range1 * range1 && distance < dist && inLockAngle(entityLocker1, user.rotationYaw, user.rotationPitch, currentEntity, angle)) {
                     // Checks whether target is visible
                     Vec3 v1 = W_WorldFunc.getWorldVec3(this.worldObj, entityLocker1.posX, entityLocker1.posY, entityLocker1.posZ);
                     Vec3 v2 = W_WorldFunc.getWorldVec3(this.worldObj, currentEntity.posX, currentEntity.posY + (double)(currentEntity.height / 2.0F), currentEntity.posZ);
                     MovingObjectPosition m = W_WorldFunc.clip(this.worldObj, v1, v2, false, true, false);
                     if(m == null || W_MovingObjectPosition.isHitTypeEntity(m)) {
                        potentialTarget = currentEntity;  // Sets lock target
                        dist = distance;
                     }
                  }
               }
            }


            this.targetEntity = potentialTarget;  // Sets potential target as current target
            if(potentialTarget != null) {
               ++this.lockCount;  // If target is locked, increments lock count
            }
         } else if(this.targetEntity != null && !this.targetEntity.isDead) {  // If a target already exists and is not dead
            // Apply the same classification used to acquire a target; visual flare emitters and
            // expired countermeasures may outlive the effect that made them appear target-like.
            boolean canLockTarget = this.canLockEntity(this.targetEntity);  // Whether target can continue to be locked

            if(targetEntity instanceof MCH_EntityBaseVehicle) {
               if(isRadarMissile && targetEntity.getEntityData().getBoolean("ChaffUsing")) {
                  canLockTarget = false;
               }
            }

            MCH_EntityBaseVehicle ac = null; //Entity ridden by the player
            if(user.ridingEntity instanceof MCH_EntityBaseVehicle) {
               ac = (MCH_EntityBaseVehicle)user.ridingEntity;

               if (ac.isFreeLookMode() && this.canLockInAir && (ac instanceof MCP_EntityPlane)) {
                  canLockTarget = false;
               }

            } else if(user.ridingEntity instanceof MCH_EntitySeat) {
               ac = ((MCH_EntitySeat)user.ridingEntity).getParent();
            } else if(user.ridingEntity instanceof MCH_EntityUavStation) {
               ac = ((MCH_EntityUavStation)user.ridingEntity).getControlAircract();
            }
            if(ac instanceof MCP_EntityPlane && targetEntity instanceof MCP_EntityPlane) {
               Vector3f playerVelocity = new Vector3f(ac.motionX, ac.motionY, ac.motionZ);  // Velocity vector of the player aircraft
               Vector3f targetVelocity = new Vector3f(targetEntity.motionX, targetEntity.motionY, targetEntity.motionZ);  // Velocity vector of the target aircraft
               float angleInDegrees = 0;
               if (playerVelocity.length() > 0.001 && targetVelocity.length() > 0.001) {
                  // Calculates the dot product of the two vectors
                  float dotProduct = Vector3f.dot(playerVelocity, targetVelocity);
                  // Calculates lengths of the two vectors
                  float playerSpeed = playerVelocity.length();
                  float targetSpeed = targetVelocity.length();
                  // Calculates the cosine of the angle
                  float cosAngle = dotProduct / (playerSpeed * targetSpeed);
                  // Ensures the angle cosine is within the valid range [-1, 1],avoids abnormal values caused by floating-point error
                  cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));
                  // Calculates the angle (radians)
                  float angle = (float) Math.acos(cosAngle);
                  // If the angle is greater than 90 degrees, converts it to an acute angle (within 90 degrees)
                  if (angle > Math.PI / 2) {
                     angle = (float) (Math.PI - angle);  // Converts to an acute angle
                  }
                  // Converts angle to degrees (optional)
                  angleInDegrees = (float) Math.toDegrees(angle);
               }
               if (angleInDegrees > ac.getCurrentWeapon(user).getCurrentWeapon().getInfo().pdHDNMaxDegree) {
                  canLockTarget = false;
               }
            }

            // If lock can continue
            if(canLockTarget) {
               double dx = this.targetEntity.posX - user.posX;
               double dy = this.targetEntity.posY - user.posY;
               dz = this.targetEntity.posZ - user.posZ;
               float stealth = 1.0F - getEntityStealth(this.targetEntity);
               double lockRange = this.lockRange * (double)stealth;
               // Determines whether target is within lock range
               if(dx * dx + dy * dy + dz * dz < lockRange * lockRange) {
                  if(this.worldObj.isRemote && this.lockSoundCount == 1) {
                     //MCH_PacketNotifyLock.send(this.getTargetEntity());
                  }

                  this.lockSoundCount = (this.lockSoundCount + 1) % 15;
                  Entity entityLocker = this.getLockEntity(user);
                  // Determines whether target is in lock range
                  if(inLockAngle(entityLocker, user.rotationYaw, user.rotationPitch, this.targetEntity, (float)this.lockAngle)) {
                     if(this.lockCount < this.getLockCountMax()) {
                        ++this.lockCount;  // Increments lock count
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

                  // If max lock count is reached, lock succeeds
                  if(this.lockCount >= this.getLockCountMax()) {
                     if(this.continueLockCount <= 0) {
                        this.continueLockCount = this.getLockCountMax() / 3;
                        if(this.continueLockCount > 20) {
                           this.continueLockCount = 20;
                        }
                     }

                     result = true;  // Lock succeeded
                     this.lastLockEntity = this.targetEntity;
                     if(isLockContinue) {
                        this.prevLockCount = this.lockCount - 1;
                     } else {
                        this.clearLock();
                     }
                  }
               } else {
                  this.clearLock();  // If not in lock range, clears lock
               }
            } else {
               this.clearLock();  // If lock cannot continue, clears lock
            }
         } else {
            this.clearLock();  // If target is null or dead, clears lock
         }

         result = this.lockCount >= this.getLockCountMax();  // Determines whether lock succeeded

         if(result) {
            this.lastLockEntity = targetEntity;
            // Plays lock success sound
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_basic_tone", 1.0f, 1.0f);
         } else {
            // Plays lock failure sound
            this.worldObj.playSoundAtEntity(user, "mcheli:ir_lock_tone", 1.0f, 1.0f);
         }

         return result;  // Returns lock result
      }
   }


   public static float getEntityStealth(Entity entity) {
      return entity instanceof MCH_EntityBaseVehicle?((MCH_EntityBaseVehicle)entity).getStealth():(entity != null && entity.ridingEntity instanceof MCH_EntityBaseVehicle?((MCH_EntityBaseVehicle)entity.ridingEntity).getStealth():0.0F);
   }

   public void clearLock() {
      this.targetEntity = null;
      this.lockCount = 0;
      this.continueLockCount = 0;
      this.lockSoundCount = 0;
      if(this.lastLockEntity != null && (this.lastLockEntity.isDead
              || this.lastLockEntity.worldObj == null
              || this.lastLockEntity.worldObj.getEntityByID(this.lastLockEntity.getEntityId()) != this.lastLockEntity)) {
         this.lastLockEntity = null;
      }
   }

   public Entity getLockEntity(Entity entity) {
      if(entity == null) {
         return null;
      }
      if(entity.ridingEntity instanceof MCH_EntityUavStation) {
         MCH_EntityUavStation us = (MCH_EntityUavStation)entity.ridingEntity;
         if(us.getControlAircract() != null) {
            return us.getControlAircract();
         }
      }

      return entity;
   }

   public boolean canLockEntity(Entity entity) {
      if(!isBasicTargetValid(this.user, entity)) {
         return false;
      }
      if(isVehicleOccupant(entity)) {
         return false;
      }
      // If locking players is not allowed and entity is a player, returns false
      if(this.ridableOnly && entity instanceof EntityPlayer && entity.ridingEntity == null) {
         return false;
      } else {
         // Gets entity class name
         String className = entity.getClass().getName();

         // If entity is EntityCamera type, returns false
         if(className.indexOf("EntityCamera") >= 0) {
            return false;
         }
         // Countermeasure entities are classified before normal living/vehicle targets. Smoke
         // particles are client-only EntityFX objects and never participate in this entity list.
         if(entity instanceof MCH_EntityFlare || entity instanceof MCH_EntityChaff) {
            return isValidCountermeasureTarget(entity, this.isHeatSeekerMissile, this.isRadarMissile);
         }
         // Locks missiles
         if(this.canLockMissile &&
                 (entity instanceof MCH_EntityAAMissile || entity instanceof MCH_EntityATMissile
                         || entity instanceof MCH_EntityASMissile || entity instanceof MCH_EntityTvMissile)) {
            if(!W_Entity.isEqual(user, ((MCH_EntityBaseBullet) entity).shootingEntity)) {
               return true;
            }
         }
         // If entity is neither a living entity nor a specific type such as aircraft or vehicle, returns false
         if(!W_Lib.isEntityLivingBase(entity)
                 && !(entity instanceof MCH_EntityBaseVehicle)
                 && !(entity instanceof MCH_EntityUavStation)
                 && className.indexOf("EntityVehicle") < 0
                 && className.indexOf("EntityPlane") < 0
                 && className.indexOf("EntityMecha") < 0
                 && className.indexOf("EntityAAGun") < 0) {
            return false;
         }
         // Water immersion remains an independent permission for non-ship targets.
         else if(!this.canLockInWater && entity.isInWater()) {
            return false;
         }
         else if(!this.isDomainAllowed(entity)) {
            return false;
         }
         // Custom restrictions are intentionally applied after shared domain permissions.
         else if(this.checker != null && !this.checker.canLockEntity(entity)) {
            return false;
         }
         return true;
      }
   }

   public boolean canLockEntity(Entity shooter, Entity entity) {
      if(!isBasicTargetValid(shooter, entity)) {
         return false;
      }
      this.user = shooter;
      return canLockEntity(entity);
   }

   private static boolean isBasicTargetValid(Entity shooter, Entity target) {
      return shooter != null && target != null && !shooter.isDead && !target.isDead
              && !W_Entity.isEqual(shooter, target) && shooter.worldObj != null
              && shooter.worldObj == target.worldObj
              && target.worldObj.getEntityByID(target.getEntityId()) == target;
   }

   private static boolean isVehicleOccupant(Entity entity) {
      if(entity == null || entity.ridingEntity == null) {
         return false;
      }
      Entity mount = entity.ridingEntity;
      if(mount instanceof MCH_EntitySeat) {
         return ((MCH_EntitySeat)mount).getParent() != null;
      }
      return mount instanceof MCH_EntityBaseVehicle || mount instanceof MCH_EntityUavStation;
   }

   /** Server-side validation for the entity id supplied by the client. */
   public boolean canLaunchAt(Entity shooter, Entity entity) {
      if(!isBasicTargetValid(shooter, entity) || this.worldObj == null
              || shooter.worldObj != this.worldObj || !canLockEntity(shooter, entity)) {
         return false;
      }
      Entity locker = getLockEntity(shooter);
      if(locker == null || locker.worldObj != this.worldObj) {
         return false;
      }
      double dx = entity.posX - shooter.posX;
      double dy = entity.posY - shooter.posY;
      double dz = entity.posZ - shooter.posZ;
      double range = this.lockRange * (double)(1.0F - getEntityStealth(entity));
      if(dx * dx + dy * dy + dz * dz >= range * range
              || !inLockAngle(locker, shooter.rotationYaw, shooter.rotationPitch, entity, (float)this.lockAngle)) {
         return false;
      }
      Vec3 from = W_WorldFunc.getWorldVec3(this.worldObj, locker.posX, locker.posY, locker.posZ);
      Vec3 to = W_WorldFunc.getWorldVec3(this.worldObj, entity.posX,
              entity.posY + (double)(entity.height / 2.0F), entity.posZ);
      MovingObjectPosition hit = W_WorldFunc.clip(this.worldObj, from, to, false, true, false);
      return hit == null || W_MovingObjectPosition.isHitTypeEntity(hit);
   }

   public static boolean isEligibleMissileTarget(Entity target, Entity shooter, MCH_WeaponInfo info,
           boolean allowGround, boolean allowAir, boolean allowWater) {
      if(info == null || !isBasicTargetValid(shooter, target) || isVehicleOccupant(target)) {
         return false;
      }
      if(target instanceof MCH_EntityFlare || target instanceof MCH_EntityChaff) {
         return isValidCountermeasureTarget(target, info.isHeatSeekerMissile, info.isRadarMissile);
      }
      if(info.ridableOnly && target instanceof EntityPlayer && target.ridingEntity == null) {
         return false;
      }
      if(target instanceof MCH_EntityBaseBullet) {
         return info.canLockMissile
                 && !W_Entity.isEqual(shooter, ((MCH_EntityBaseBullet)target).shootingEntity);
      }
      String className = target.getClass().getName();
      if(className.indexOf("EntityCamera") >= 0
              || !W_Lib.isEntityLivingBase(target)
              && !(target instanceof MCH_EntityBaseVehicle)
              && !(target instanceof MCH_EntityUavStation)
              && className.indexOf("EntityVehicle") < 0
              && className.indexOf("EntityPlane") < 0
              && className.indexOf("EntityMecha") < 0
              && className.indexOf("EntityAAGun") < 0) {
         return false;
      }
      if(!allowWater && target.isInWater()) {
         return false;
      }
      TargetDomain domain = getTargetDomain(target, info.lockMinHeight);
      return domain == TargetDomain.GROUND ? allowGround
              : domain == TargetDomain.AIR ? allowAir
              : (domain == TargetDomain.SURFACE || domain == TargetDomain.UNDERWATER) && allowWater;
   }

   public static boolean isValidCountermeasureTarget(Entity entity, boolean heatSeeking, boolean radarGuided) {
      if(entity instanceof MCH_EntityFlare) {
         return !radarGuided && heatSeeking && ((MCH_EntityFlare)entity).isActiveCountermeasure();
      }
      if(entity instanceof MCH_EntityChaff) {
         return radarGuided && ((MCH_EntityChaff)entity).isActiveCountermeasure();
      }
      return false;
   }


   public static boolean inLockAngle(Entity entity, float rotationYaw, float rotationPitch, Entity target, float lockAng) {
      if(entity == null || target == null || entity.isDead || target.isDead
              || entity.worldObj == null || entity.worldObj != target.worldObj) {
         return false;
      }
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
      return this.lastLockEntity;
   }

   @Override
   public double getLockPosX() {
      return targetEntity != null ? targetEntity.posX : 0.0D;
   }

   @Override
   public double getLockPosY() {
      return targetEntity != null ? targetEntity.posY : 0.0D;
   }

   @Override
   public double getLockPosZ() {
      return targetEntity != null ? targetEntity.posZ : 0.0D;
   }
}
