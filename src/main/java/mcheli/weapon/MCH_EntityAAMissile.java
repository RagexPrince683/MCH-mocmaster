package mcheli.weapon;

import mcheli.MCH_Config;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker {

   private static final double PLANE_SPEED_CONFIG_BASELINE = 1000.0D;
   private static final double MPH_TO_PLANE_SPEED_FACTOR = 1.74D;

   public MCH_EntityAAMissile(World par1World) {
      super(par1World);
      super.targetEntity = null;
   }

   public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
      this.acceleration = acceleration * getPlaneSpeedScale();
      this.setMotion(targetX, targetY, targetZ);
   }

   private static double getPlaneSpeedScale() {
      return MCH_Config.AllPlaneSpeed.prmDouble / PLANE_SPEED_CONFIG_BASELINE * MPH_TO_PLANE_SPEED_FACTOR;
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
      }

      if(!super.worldObj.isRemote && this.getInfo() != null) {
         if(super.shootingEntity != null && isValidExistingTarget(super.targetEntity)) {
            double x = super.posX - super.targetEntity.posX;
            double y = super.posY - super.targetEntity.posY;
            double z = super.posZ - super.targetEntity.posZ;
            double distanceSq = x * x + y * y + z * z;
            if(distanceSq > 3422500.0D) {
               this.setDead();
            } else if(this.getCountOnUpdate() > this.getInfo().rigidityTime) {
               double fuseDistanceSq = (double)this.getInfo().proximityFuseDist * (double)this.getInfo().proximityFuseDist;
               if(this.getInfo().proximityFuseDist >= 0.1F && distanceSq < fuseDistanceSq) {
                  MovingObjectPosition mop = new MovingObjectPosition(super.targetEntity);
                  super.posX = (super.targetEntity.posX + super.posX) / 2.0D;
                  super.posY = (super.targetEntity.posY + super.posY) / 2.0D;
                  super.posZ = (super.targetEntity.posZ + super.posZ) / 2.0D;
                  this.onImpact(mop, 1.0F);
               } else {
                  this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
               }
            }
         } else {
            if(super.targetEntity instanceof MCH_EntityFlare || super.targetEntity instanceof MCH_EntityChaff) {
               super.targetEntity = null;
            }
            if(getInfo().activeRadar && ticksExisted % getInfo().scanInterval == 0) {
               scanForTargets();
            }
         }
      }

   }

   private void scanForTargets() {
      Vector3f missileDirection = new Vector3f((float) super.motionX, (float) super.motionY, (float) super.motionZ);
      double range = getInfo().maxLockOnRange;
      List<Entity> list = worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(
              posX - range, posY - range, posZ - range,
              posX + range, posY + range, posZ + range
      ));

      if (list != null && !list.isEmpty()) {
         double closestAngle = Double.MAX_VALUE;
         Entity closestTarget = null;

         for (Entity entity : list) {
            if (MCH_WeaponGuidanceSystem.isEligibleMissileTarget(entity, shootingAircraft,
                    getInfo(), false, true, false)) {

               if (W_Entity.isEqual(entity, shootingAircraft)) {
                  continue;
               }

               double dx = entity.posX - super.posX;
               double dy = entity.posY - super.posY;
               double dz = entity.posZ - super.posZ;
               Vector3f targetDirection = new Vector3f((float) dx, (float) dy, (float) dz);

               double angle = Math.abs(Vector3f.angle(missileDirection, targetDirection));

               if(angle > Math.toRadians(getInfo().maxLockOnAngle)) {
                  continue;
               }

               if (angle < closestAngle) {
                  closestAngle = angle;
                  closestTarget = entity;
               }
            }
         }

         if (closestTarget != null) {
            super.targetEntity = closestTarget;
         }
      }
   }

   private boolean isValidExistingTarget(Entity entity) {
      return MCH_WeaponGuidanceSystem.isEligibleMissileTarget(entity, shootingAircraft,
              getInfo(), false, true, false);
   }


   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.AAMissile;
   }

   @Override
   protected boolean isHomingExpired() {
      return false;
   }

   @Override
   public boolean canLockEntity(Entity var1) {
      return false;
   }
}
