package mcheli.weapon;

import mcheli.wrapper.W_Lib;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityBomb extends MCH_EntityBaseBullet {

   public MCH_EntityBomb(World par1World) {
      super(par1World);
   }

   public MCH_EntityBomb(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   
   
  
   
   public void onUpdate() {
      super.onUpdate();
      if(!super.worldObj.isRemote && this.getInfo() != null) {
         super.motionX *= 0.999D;
         super.motionZ *= 0.999D;
         if(this.isInWater()) {
            super.motionX *= (double)this.getInfo().velocityInWater;
            super.motionY *= (double)this.getInfo().velocityInWater;
            super.motionZ *= (double)this.getInfo().velocityInWater;
         }

         float dist = this.getInfo().proximityFuseDist;
         if(dist > 0.1F && this.getCountOnUpdate() % 10 == 0) {
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand((double)dist, (double)dist, (double)dist));
            if(list != null) {
               for(int i = 0; i < list.size(); ++i) {
                  Entity entity = (Entity)list.get(i);
                  if(W_Lib.isEntityLivingBase(entity) && this.canBeCollidedEntity(entity)) {
                     MovingObjectPosition m = new MovingObjectPosition((int)(super.posX + 0.5D), (int)(super.posY + 0.5D), (int)(super.posZ + 0.5D), 0, Vec3.createVectorHelper(super.posX, super.posY, super.posZ));
                     this.onImpact(m, 1.0F);
                     break;
                  }
               }
            }
         }
      }

      this.onUpdateBomblet();
   }

   public void sprinkleBomblet() {
      if(!super.worldObj.isRemote) {
         MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, (float)super.rand.nextInt(360), 0.0F, super.acceleration);
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         e.setName(this.getName());
         float MOTION = 1.0F;
         float RANDOM = this.getInfo().bombletDiff;
         e.motionX = super.motionX * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.motionY = super.motionY * 1.0D / 2.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM / 2.0F);
         e.motionZ = super.motionZ * 1.0D + (double)((super.rand.nextFloat() - 0.5F) * RANDOM);
         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Bomb;
   }
}
