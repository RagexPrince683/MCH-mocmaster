package mcheli.weapon;

import net.minecraft.world.World;

public class MCH_EntityRocket extends MCH_EntityBaseBullet {

   public MCH_EntityRocket(World par1World) {
      super(par1World);
   }

   public MCH_EntityRocket(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      this.onUpdateBomblet();
      if(super.isBomblet <= 0 && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnExplosionParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
      }

   }

   public void sprinkleBomblet() {
      if(!super.worldObj.isRemote) {
         MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, super.posX, super.posY, super.posZ, super.motionX, super.motionY, super.motionZ, super.rotationYaw, super.rotationPitch, super.acceleration);
         e.setName(this.getName());
         e.setParameterFromWeapon(this, super.shootingAircraft, super.shootingEntity);
         float MOTION = this.getInfo().bombletDiff;
         float RANDOM = 1.2F;
         e.motionX += ((double)super.rand.nextFloat() - 0.5D) * (double)MOTION;
         e.motionY += ((double)super.rand.nextFloat() - 0.5D) * (double)MOTION;
         e.motionZ += ((double)super.rand.nextFloat() - 0.5D) * (double)MOTION;
         e.setBomblet();
         super.worldObj.spawnEntityInWorld(e);
      }

   }

   public MCH_BulletModel getDefaultBulletModel() {
      return MCH_DefaultBulletModels.Rocket;
   }
}
