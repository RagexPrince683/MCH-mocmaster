package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_DefaultBulletModels;
import mcheli.weapon.MCH_EntityBaseBullet;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class MCH_EntityTvMissile extends MCH_EntityBaseBullet {

   public boolean isSpawnParticle = true;

   //public static boolean isTVMissile = true;


   public MCH_EntityTvMissile(World par1World) {
      super(par1World);
   }

   public MCH_EntityTvMissile(World par1World, double posX, double posY, double posZ, double targetX, double targetY, double targetZ, float yaw, float pitch, double acceleration) {
      super(par1World, posX, posY, posZ, targetX, targetY, targetZ, yaw, pitch, acceleration);
   }

   public void onUpdate() {
      super.onUpdate();
      this.onUpdateBomblet();
      if(this.isSpawnParticle && this.getInfo() != null && !this.getInfo().disableSmoke) {
         this.spawnParticle(this.getInfo().trajectoryParticleName, 3, 5.0F * this.getInfo().smokeSize * 0.5F);
      }

      if(super.shootingEntity != null) {
         double x = super.posX - super.shootingEntity.posX;
         double y = super.posY - super.shootingEntity.posY;
         double z = super.posZ - super.shootingEntity.posZ;
         if(x * x + y * y + z * z > 1440000.0D) {
            this.setDead();
         }

         if(!super.worldObj.isRemote && !super.isDead) {
            this.onUpdateMotion();
         }
      } else if(!super.worldObj.isRemote) {
         this.setDead();
      }

   }

   public void onUpdateMotion() {
      Entity e = super.shootingEntity;
      if(e != null && !e.isDead) {
         MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(e);
         if(ac != null && ac.getTVMissile() == this) {
            float yaw = e.rotationYaw;
            float pitch = e.rotationPitch;
            double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
            this.setMotion(tX, tY, tZ);
            this.setRotation(yaw, pitch);
         }
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
      return MCH_DefaultBulletModels.ATMissile;
   }
}
