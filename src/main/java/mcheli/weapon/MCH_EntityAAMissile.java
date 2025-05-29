package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.flare.MCH_EntityChaff;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vector.Vector3f;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet implements MCH_IEntityLockChecker {

   public MCH_EntityAAMissile(World par1World) {
      super(par1World);
      super.targetEntity = null;
   }

   public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
      }

      if(!super.worldObj.isRemote && this.getInfo() != null) {
         if(super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
            double x = super.posX - super.targetEntity.posX;
            double y = super.posY - super.targetEntity.posY;
            double z = super.posZ - super.targetEntity.posZ;
            double d = x * x + y * y + z * z;
            if(d > 3422500.0D) {
               this.setDead();
            } else if(this.getCountOnUpdate() > this.getInfo().rigidityTime) {
               if(this.getInfo().proximityFuseDist >= 0.1F && d * d < (double)this.getInfo().proximityFuseDist) {
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
            if (entity instanceof MCH_EntityAircraft || entity instanceof MCH_EntityChaff) {

               if (W_Entity.isEqual(entity, shootingAircraft)) {
                  continue;
               }

               boolean isTargetOnGround = MCH_WeaponGuidanceSystem.isEntityOnGround(entity, getInfo().lockMinHeight);
               if (isTargetOnGround) {
                  continue;
               }

               //todo here?

               //boolean isShooterFreeLook = (shootingEntity instanceof MCH_EntitySeat) && ((MCH_EntitySeat) shootingEntity).isFreeLook();

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


   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.AAMissile;
   }

   @Override
   public boolean canLockEntity(Entity var1) {
      return false;
   }
}