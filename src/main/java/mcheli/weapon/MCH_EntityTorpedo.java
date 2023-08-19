package mcheli.weapon;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityTorpedo extends MCH_EntityBaseBullet {

   public double targetPosX;
   public double targetPosY;
   public double targetPosZ;
   public double accelerationInWater = 2.0D;


   public MCH_EntityTorpedo(World par1World) {
      super(par1World);
      this.targetPosX = 0.0D;
      this.targetPosY = 0.0D;
      this.targetPosZ = 0.0D;
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getInfo() != null && this.getInfo().isGuidedTorpedo) {
         this.onUpdateGuided();
      } else {
         this.onUpdateNoGuided();
      }

      if(this.isInWater() && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
      }

   }

   private void onUpdateNoGuided() {
      double a;
      if(!super.worldObj.isRemote && this.isInWater()) {
         super.motionY *= 0.800000011920929D;
         if(super.acceleration < this.accelerationInWater) {
            super.acceleration += 0.1D;
         } else if(super.acceleration > this.accelerationInWater + 0.20000000298023224D) {
            super.acceleration -= 0.1D;
         }

         a = super.motionX;
         double y = super.motionY;
         double z = super.motionZ;
         double d = (double)MathHelper.sqrt_double(a * a + y * y + z * z);
         super.motionX = a * super.acceleration / d;
         super.motionY = y * super.acceleration / d;
         super.motionZ = z * super.acceleration / d;
      }

      if(this.isInWater()) {
         a = (double)((float)Math.atan2(super.motionZ, super.motionX));
         super.rotationYaw = (float)(a * 180.0D / 3.141592653589793D) - 90.0F;
      }

   }

   private void onUpdateGuided() {
      double a;
      double r;
      if(!super.worldObj.isRemote && this.isInWater()) {
         if(super.acceleration < this.accelerationInWater) {
            super.acceleration += 0.1D;
         } else if(super.acceleration > this.accelerationInWater + 0.20000000298023224D) {
            super.acceleration -= 0.1D;
         }

         a = this.targetPosX - super.posX;
         r = this.targetPosY - super.posY;
         double z = this.targetPosZ - super.posZ;
         double d = (double)MathHelper.sqrt_double(a * a + r * r + z * z);
         super.motionX = a * super.acceleration / d;
         super.motionY = r * super.acceleration / d;
         super.motionZ = z * super.acceleration / d;
      }

      if(this.isInWater()) {
         a = (double)((float)Math.atan2(super.motionZ, super.motionX));
         super.rotationYaw = (float)(a * 180.0D / 3.141592653589793D) - 90.0F;
         r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
      }

   }

   public MCH_EntityTorpedo(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Torpedo;
   }
}
