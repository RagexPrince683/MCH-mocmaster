package mcheli.weapon;

import mcheli.aircraft.MCH_EntityBaseVehicle;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityTorpedo extends MCH_EntityTvMissile {

   public double targetPosX;
   public double targetPosY;
   public double targetPosZ;
   public double accelerationInWater = 2.0D;


   public MCH_EntityTorpedo(World par1World) {
      super(par1World);
      this.targetPosX = 0.0D;
      this.targetPosY = 0.0D;
      this.targetPosZ = 0.0D;
      this.isSpawnParticle = false;
   }

   public void onUpdateMotion() {
      // Guided torpedoes intentionally apply the TV-missile steering from
      // onUpdateGuided() only while submerged, so the inherited TV missile
      // update cannot steer them through air or out of the water.
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getInfo() != null && this.getInfo().isGuidedTorpedo) {
         this.onUpdateGuided();
      } else {
         this.onUpdateNoGuided();
      }

      if(this.isInWater() && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
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
      if(!super.worldObj.isRemote && this.isInWater()) {
         this.applyTvGuidanceInWater();

         if(super.acceleration < this.accelerationInWater) {
            super.acceleration += 0.1D;
         } else if(super.acceleration > this.accelerationInWater + 0.2D) {
            super.acceleration -= 0.1D;
         }

         double d = MathHelper.sqrt_double(super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ);
         if(d > 0.001D) {
            super.motionX = super.motionX * super.acceleration / d;
            super.motionY = super.motionY * super.acceleration / d;
            super.motionZ = super.motionZ * super.acceleration / d;
         }

         this.keepGuidedTorpedoInWater();
      }

      if(this.isInWater()) {
         double yaw = (double)((float)Math.atan2(super.motionZ, super.motionX));
         super.rotationYaw = (float)(yaw * 180.0D / Math.PI) - 90.0F;

         double h = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         super.rotationPitch = -((float)(Math.atan2(super.motionY, h) * 180.0D / Math.PI));
      }
   }

   private void applyTvGuidanceInWater() {
      if(this.getInfo() == null || this.getInfo().laserGuidance) {
         return;
      }

      if(super.shootingEntity != null && !super.shootingEntity.isDead) {
         MCH_EntityBaseVehicle ac = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(super.shootingEntity);
         if(ac != null && ac.getTVMissile() == this) {
            float yaw = super.shootingEntity.rotationYaw;
            float pitch = super.shootingEntity.rotationPitch;
            double tX = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
            double tZ = MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI);
            double tY = -MathHelper.sin(pitch / 180.0F * (float)Math.PI);
            this.setMotion(tX, tY, tZ);
            this.setRotation(yaw, pitch);
         }
      }
   }

   private void keepGuidedTorpedoInWater() {
      if(!this.isWaterAt(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ)) {
         super.motionY = Math.min(super.motionY, -0.05D);

         if(!this.isWaterAt(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ)) {
            super.motionX *= 0.25D;
            super.motionZ *= 0.25D;
         }
      }
   }

   private boolean isWaterAt(double x, double y, double z) {
      return super.worldObj.getBlock(
              MathHelper.floor_double(x),
              MathHelper.floor_double(y),
              MathHelper.floor_double(z)
      ).getMaterial() == Material.water;
   }

   public MCH_EntityTorpedo(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
      this.isSpawnParticle = false;
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Torpedo;
   }
}
