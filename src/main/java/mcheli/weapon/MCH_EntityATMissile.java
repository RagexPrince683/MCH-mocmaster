package mcheli.weapon;

import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityATMissile extends MCH_EntityBaseBullet {

   public int guidanceType = 0;


   public MCH_EntityATMissile(World par1World) {
      super(par1World);
   }

   public MCH_EntityATMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getInfo() != null && !this.getInfo().disableSmoke && super.ticksExisted >= this.getInfo().trajectoryParticleStartTick) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
      }

      if(!super.worldObj.isRemote) {
         if(super.shootingEntity != null && super.targetEntity != null && !super.targetEntity.isDead) {
            this.onUpdateMotion();
         } else {
            this.setDead();
         }
      }

      double a = (double)((float)Math.atan2(super.motionZ, super.motionX));
      super.rotationYaw = (float)(a * 180.0D / 3.141592653589793D) - 90.0F;
      double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
   }

   public void onUpdateMotion() {
      double x = super.targetEntity.posX - super.posX;
      double y = super.targetEntity.posY - super.posY;
      double z = super.targetEntity.posZ - super.posZ;
      double d = x * x + y * y + z * z;
      if(d <= 2250000.0D && !super.targetEntity.isDead) {
         if(this.getInfo().proximityFuseDist >= 0.1F && d < (double)this.getInfo().proximityFuseDist) {
            MovingObjectPosition var11 = new MovingObjectPosition(super.targetEntity);
            var11.entityHit = null;
            this.onImpact(var11, 1.0F);
         } else {
            int rigidityTime = this.getInfo().rigidityTime;
            float af = this.getCountOnUpdate() < rigidityTime + this.getInfo().trajectoryParticleStartTick?0.5F:1.0F;
            if(this.getCountOnUpdate() > rigidityTime) {
               if(this.guidanceType == 1) {
                  if(this.getCountOnUpdate() <= rigidityTime + 20) {
                     this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY + 150.0D, super.targetEntity.posZ, af);
                  } else if(this.getCountOnUpdate() <= rigidityTime + 30) {
                     this.guidanceToTarget(super.targetEntity.posX, super.shootingEntity.posY, super.targetEntity.posZ, af);
                  } else {
                     if(this.getCountOnUpdate() == rigidityTime + 35) {
                        this.setPower((int)((float)this.getPower() * 1.2F));
                        if(super.explosionPower > 0) {
                           ++super.explosionPower;
                        }
                     }

                     this.guidanceToTarget(super.targetEntity.posX, super.targetEntity.posY, super.targetEntity.posZ, af);
                  }
               } else {
                  d = (double)MathHelper.sqrt_double(d);
                  super.motionX = x * super.acceleration / d * (double)af;
                  super.motionY = y * super.acceleration / d * (double)af;
                  super.motionZ = z * super.acceleration / d * (double)af;
               }
            }
         }
      } else {
         this.setDead();
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.ATMissile;
   }
}
