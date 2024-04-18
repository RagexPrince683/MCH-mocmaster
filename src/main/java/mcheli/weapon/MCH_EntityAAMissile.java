package mcheli.weapon;

import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet {
   double speed = Math.sqrt(super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ);
   private double maxTurningAngle = 180.0 + speed * 0.1; // Maximum turning angle in degrees


   public MCH_EntityAAMissile(World par1World) {
      super(par1World);
      super.targetEntity = null;
   }

   public MCH_EntityAAMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {

      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
      super.targetEntity = null;
   }

   public void setMaxTurningAngle(float angle) {
      maxTurningAngle = angle;
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getCountOnUpdate() > 4 && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
      }

      if (!super.worldObj.isRemote && this.getInfo() != null) {
         if (super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
            double x = super.posX - super.targetEntity.posX;
            double y = super.posY - super.targetEntity.posY;
            double z = super.posZ - super.targetEntity.posZ;
            //square of distance between coordinates
            double d = x * x + y * y + z * z;
            if(d > 3422500.0D) {
               this.setDead();
            } else if(this.getCountOnUpdate() > this.getInfo().rigidityTime) {
               if (this.getInfo().proximityFuseDist >= 0.1F && d < (double) this.getInfo().proximityFuseDist) {
                  MovingObjectPosition mop = new MovingObjectPosition(super.targetEntity);
                  super.posX = (super.targetEntity.posX + super.posX) / 2.0D;
                  super.posY = (super.targetEntity.posY + super.posY) / 2.0D;
                  super.posZ = (super.targetEntity.posZ + super.posZ) / 2.0D;
                  this.onImpact(mop, 1.0F);
               } else {
                  this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
               }
            }
            // Rest of the original onUpdate logic

            // Replace this line
            // this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);

            // With this line
            double missileHeading = Math.toDegrees(Math.atan2(super.motionZ, super.motionX));
            missileHeading = (missileHeading + 360) % 360;
            System.out.println(missileHeading);
            double angleToTarget = Math.abs(missileHeading - rotationYaw);
            if (angleToTarget > maxTurningAngle) {
               this.newExplosion(super.posX, super.posY, super.posZ, 1, 1, false);
               this.setDead();
            } else {
               this.guidanceToTargetLimited(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);
            }

            // Rest of the original onUpdate logic
         } else {
            this.newExplosion(super.posX, super.posY, super.posZ, 1, 1, false);
            this.setDead();
         }
      }
   }

   private void guidanceToTargetLimited(double targetX, double targetY, double targetZ) {
      if (super.getInfo() != null) {
         double deltaX = targetX - super.posX;
         double deltaY = targetY - super.posY;
         double deltaZ = targetZ - super.posZ;

         double distanceToTarget = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

         if (distanceToTarget > 0) {
            double maxDelta = maxTurningAngle * Math.PI / 180.0;
            double angleScale = Math.min(1.0, maxDelta / distanceToTarget);

            double adjustedDeltaX = deltaX * angleScale;
            double adjustedDeltaY = deltaY * angleScale;
            double adjustedDeltaZ = deltaZ * angleScale;

            double adjustedTargetX = super.posX + adjustedDeltaX;
            double adjustedTargetY = super.posY + adjustedDeltaY;
            double adjustedTargetZ = super.posZ + adjustedDeltaZ;

            super.rotationYaw = (float) Math.toDegrees(Math.atan2(adjustedDeltaZ, adjustedDeltaX));
            super.rotationPitch = (float) Math.toDegrees(Math.atan2(-adjustedDeltaY, Math.sqrt(adjustedDeltaX * adjustedDeltaX + adjustedDeltaZ * adjustedDeltaZ)));

            super.motionX = adjustedDeltaX;
            super.motionY = adjustedDeltaY;
            super.motionZ = adjustedDeltaZ;
         }
      }
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.AAMissile;
   }
}