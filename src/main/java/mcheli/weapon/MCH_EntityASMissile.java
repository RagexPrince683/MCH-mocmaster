package mcheli.weapon;

import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityASMissile extends MCH_EntityBaseBullet {

   public double targetPosX;
   public double targetPosY;
   public double targetPosZ;


   public MCH_EntityASMissile(World par1World) {
      super(par1World);
      this.targetPosX = 0.0D;
      this.targetPosY = 0.0D;
      this.targetPosZ = 0.0D;
   }

   public float getGravity() {
      return this.getBomblet() == 1?-0.03F:super.getGravity();
   }

   public float getGravityInWater() {
      return this.getBomblet() == 1?-0.03F:super.getGravityInWater();
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.getInfo() != null && !this.getInfo().disableSmoke && this.getBomblet() == 0) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 10.0F * this.getInfo().smokeSize * 0.5F);
      }

      if(this.getInfo() != null && !super.worldObj.isRemote && super.isBomblet != 1) {
         Block a = W_WorldFunc.getBlock(super.worldObj, (int)this.targetPosX, (int)this.targetPosY, (int)this.targetPosZ);
         if(a != null && a.isCollidable()) {
            double dist = this.getDistance(this.targetPosX, this.targetPosY, this.targetPosZ);
            if(dist < (double)this.getInfo().proximityFuseDist) {
               if(this.getInfo().bomblet > 0) {
                  for(int x = 0; x < this.getInfo().bomblet; ++x) {
                     this.sprinkleBomblet();
                  }
               } else {
                  MovingObjectPosition var15 = new MovingObjectPosition(this);
                  this.onImpact(var15, 1.0F);
               }

               this.setDead();
            } else {
               double var16;
               double y;
               double z;
               double d;
               if((double)this.getGravity() == 0.0D) {
                  var16 = 0.0D;
                  if(this.getCountOnUpdate() < 10) {
                     var16 = 20.0D;
                  }

                  y = this.targetPosX - super.posX;
                  z = this.targetPosY + var16 - super.posY;
                  d = this.targetPosZ - super.posZ;
                  double d1 = (double)MathHelper.sqrt_double(y * y + z * z + d * d);
                  super.motionX = y * super.acceleration / d1;
                  super.motionY = z * super.acceleration / d1;
                  super.motionZ = d * super.acceleration / d1;
               } else {
                  var16 = this.targetPosX - super.posX;
                  y = this.targetPosY - super.posY;
                  y *= 0.3D;
                  z = this.targetPosZ - super.posZ;
                  d = (double)MathHelper.sqrt_double(var16 * var16 + y * y + z * z);
                  super.motionX = var16 * super.acceleration / d;
                  super.motionZ = z * super.acceleration / d;
               }
            }
         }
      }

      double var14 = (double)((float)Math.atan2(super.motionZ, super.motionX));
      super.rotationYaw = (float)(var14 * 180.0D / 3.141592653589793D) - 90.0F;
      double r = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      super.rotationPitch = -((float)(Math.atan2(super.motionY, r) * 180.0D / 3.141592653589793D));
      this.onUpdateBomblet();
   }

   public void sprinkleBomblet() {
      if(!super.worldObj.isRemote) {
         MCH_EntityASMissile e = new MCH_EntityASMissile(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         e.setName(this.getName());
         float MOTION = 0.5F;
         float RANDOM = this.getInfo().bombletDiff;
         e.motionX = super.motionX * 0.5D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.motionY = super.motionY * 0.5D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
         e.motionZ = super.motionZ * 0.5D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
      }

   }

   public MCH_EntityASMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.ASMissile;
   }
}
