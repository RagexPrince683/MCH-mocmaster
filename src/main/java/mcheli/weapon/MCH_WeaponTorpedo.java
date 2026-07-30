package mcheli.weapon;

import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_PacketNotifyTVMissileEntity;
import mcheli.wrapper.W_Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponTorpedo extends MCH_WeaponBase {

   protected MCH_EntityTorpedo lastShotGuidedTorpedo = null;
   protected net.minecraft.entity.Entity lastShotEntity = null;

   public MCH_WeaponTorpedo(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 0.5F;
      super.explosionPower = 8;
      super.power = 35;
      super.interval = -100;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }


   public void update(int countWait) {
      super.update(countWait);

      if(!super.worldObj.isRemote) {
         if(super.tick <= 9 && this.lastShotGuidedTorpedo != null && this.lastShotEntity != null) {
            if(super.tick % 3 == 0 && !this.lastShotGuidedTorpedo.isDead && !this.lastShotEntity.isDead) {
               MCH_PacketNotifyTVMissileEntity.send(W_Entity.getEntityId(this.lastShotEntity), W_Entity.getEntityId(this.lastShotGuidedTorpedo));
            }

            if(super.tick == 9) {
               this.lastShotEntity = null;
               this.lastShotGuidedTorpedo = null;
            }
         }

         if(super.tick <= 2 && this.lastShotEntity instanceof MCH_EntityBaseVehicle) {
            ((MCH_EntityBaseVehicle)this.lastShotEntity).setTVMissile(this.lastShotGuidedTorpedo);
         }
      }
   }

   public boolean shot(MCH_WeaponParam prm) {
      return this.getInfo() != null?(this.getInfo().isGuidedTorpedo?this.shotGuided(prm):this.shotNoGuided(prm)):false;
   }

   protected boolean shotNoGuided(MCH_WeaponParam prm) {
      if(super.worldObj.isRemote) {
         return true;
      } else {
         float yaw = prm.rotYaw;
         float pitch = prm.rotPitch;
         double mx = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double mz = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double my = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
         double launchAcceleration = MCH_WeaponBase.getEffectiveLaunchAcceleration(this.getInfo(), (double)this.getInfo().acceleration);
         mx = mx * launchAcceleration + prm.entity.motionX;
         my = my * launchAcceleration + prm.entity.motionY;
         mz = mz * launchAcceleration + prm.entity.motionZ;
         double acceleration = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
         MCH_EntityTorpedo e = new MCH_EntityTorpedo(super.worldObj, prm.posX, prm.posY, prm.posZ, mx, my, mz, yaw, 0.0F, acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.motionX = mx;
         e.motionY = my;
         e.motionZ = mz;
         e.accelerationInWater = this.getInfo() != null?(double)this.getInfo().accelerationInWater:1.0D;
         super.worldObj.spawnEntityInWorld(e);
         this.playSound(prm.entity);
         return true;
      }
   }

   protected boolean shotGuided(MCH_WeaponParam prm) {
      if(super.worldObj.isRemote) {
         return true;
      }

      float yaw = prm.user.rotationYaw;
      float pitch = prm.user.rotationPitch;
      double mx = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double mz = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double my = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      double launchAcceleration = MCH_WeaponBase.getEffectiveLaunchAcceleration(this.getInfo(), (double)this.getInfo().acceleration);
      mx = mx * launchAcceleration + prm.entity.motionX;
      my = my * launchAcceleration + prm.entity.motionY;
      mz = mz * launchAcceleration + prm.entity.motionZ;
      double acceleration = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);

      MCH_EntityTorpedo e = new MCH_EntityTorpedo(super.worldObj, prm.posX, prm.posY, prm.posZ, mx, my, mz, yaw, pitch, acceleration);
      e.setName(super.name);
      e.setParameterFromWeapon(this, prm.entity, prm.user);
      e.motionX = mx;
      e.motionY = my;
      e.motionZ = mz;
      e.accelerationInWater = this.getInfo() != null?(double)this.getInfo().accelerationInWater:1.0D;
      this.lastShotEntity = prm.entity;
      this.lastShotGuidedTorpedo = e;
      super.worldObj.spawnEntityInWorld(e);
      this.playSound(prm.entity);
      return true;
   }
}
