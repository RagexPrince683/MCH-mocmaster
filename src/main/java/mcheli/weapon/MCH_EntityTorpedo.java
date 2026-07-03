package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

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
         if(this.isInvalidTorpedoTarget(super.targetEntity)) {
            this.setTargetEntity(this.findTorpedoTarget());
         }

         double tx;
         double ty;
         double tz;

         if(super.targetEntity != null) {
            tx = super.targetEntity.posX;
            ty = this.getGuidanceDepth(super.targetEntity);
            tz = super.targetEntity.posZ;
         } else {
            tx = this.targetPosX;
            ty = Math.min(this.targetPosY, this.getGuidanceDepth((Entity)null));
            tz = this.targetPosZ;
         }

         if(super.acceleration < this.accelerationInWater) {
            super.acceleration += 0.1D;
         } else if(super.acceleration > this.accelerationInWater + 0.2D) {
            super.acceleration -= 0.1D;
         }

         double dx = tx - super.posX;
         double dy = ty - super.posY;
         double dz = tz - super.posZ;
         double d = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);

         if(d > 0.001D) {
            super.motionX = dx * super.acceleration / d;
            super.motionY = this.clamp(dy * super.acceleration / d, -0.25D, 0.08D);
            super.motionZ = dz * super.acceleration / d;
            if(!this.hasWaterAbove() && super.motionY > 0.0D) {
               super.motionY = Math.min(super.motionY, 0.0D);
            }
         }
      }

      if(this.isInWater()) {
         double yaw = (double)((float)Math.atan2(super.motionZ, super.motionX));
         super.rotationYaw = (float)(yaw * 180.0D / Math.PI) - 90.0F;

         double h = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         super.rotationPitch = -((float)(Math.atan2(super.motionY, h) * 180.0D / Math.PI));
      }
   }

   private Entity findTorpedoTarget() {
      double range = 96.0D;

      List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(
              this,
              super.boundingBox.expand(range, range * 0.5D, range)
      );

      Entity best = null;
      double bestScore = Double.MAX_VALUE;

      for(int i = 0; i < list.size(); ++i) {
         Entity e = (Entity)list.get(i);

         if(this.isInvalidTorpedoTarget(e)) {
            continue;
         }

         double dx = e.posX - super.posX;
         double dy = e.posY - super.posY;
         double dz = e.posZ - super.posZ;
         double distSq = dx * dx + dy * dy + dz * dz;

         // Front-cone check so torpedoes do not instantly 180-degree lock.
         double motionLen = Math.sqrt(super.motionX * super.motionX + super.motionY * super.motionY + super.motionZ * super.motionZ);
         double targetLen = Math.sqrt(dx * dx + dy * dy + dz * dz);

         if(motionLen > 0.001D && targetLen > 0.001D) {
            double dot = (super.motionX * dx + super.motionY * dy + super.motionZ * dz) / (motionLen * targetLen);

            if(dot < 0.25D) {
               continue;
            }
         }

         if(distSq < bestScore) {
            bestScore = distSq;
            best = e;
         }
      }

      return best;
   }

   private boolean isInvalidTorpedoTarget(Entity e) {
      if(e == null || e.isDead || e == super.shootingEntity || e == super.shootingAircraft) {
         return true;
      }

      if(super.shootingAircraft != null && (e.ridingEntity == super.shootingAircraft || e.riddenByEntity == super.shootingAircraft)) {
         return true;
      }

      if(!e.isInWater()) {
         return true;
      }

      return !(e instanceof mcheli.ship.MCH_EntityShip) && !(e instanceof mcheli.aircraft.MCH_EntityBaseVehicle);
   }

   private double clamp(double value, double min, double max) {
      return value < min ? min : (value > max ? max : value);
   }

   private double getGuidanceDepth(Entity target) {
      double desiredY = target != null ? target.posY + target.height * 0.25D : super.posY;
      double maxY = Math.floor(super.posY) + 0.20D;
      return Math.min(desiredY, maxY);
   }

   private boolean hasWaterAbove() {
      int x = MathHelper.floor_double(super.posX);
      int y = MathHelper.floor_double(super.posY + 0.6D);
      int z = MathHelper.floor_double(super.posZ);
      return super.worldObj.getBlock(x, y, z).getMaterial() == net.minecraft.block.material.Material.water;
   }

   public MCH_EntityTorpedo(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Torpedo;
   }
}
