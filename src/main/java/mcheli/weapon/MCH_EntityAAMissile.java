package mcheli.weapon;

import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityAAMissile extends MCH_EntityBaseBullet {

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
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 7.0F * this.getInfo().smokeSize * 0.5F);
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
               if(this.getInfo().proximityFuseDist >= 0.1F && d < (double)this.getInfo().proximityFuseDist) {
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
            this.setDead();
         }
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.AAMissile;
   }
}
