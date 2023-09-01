package mcheli.weapon;

import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet {

   private float maxTurningAngle = 45.0F; // Maximum turning angle in degrees

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
            // Rest of the original onUpdate logic

            // Replace this line
            // this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);

            // With this line
            this.guidanceToTargetLimited(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ);

            // Rest of the original onUpdate logic
         } else {
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