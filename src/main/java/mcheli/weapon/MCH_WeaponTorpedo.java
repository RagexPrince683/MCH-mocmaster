package mcheli.weapon;

import mcheli.weapon.MCH_EntityTorpedo;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponTorpedo extends MCH_WeaponBase {

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
         mx = mx * (double)this.getInfo().acceleration + prm.entity.motionX;
         my = my * (double)this.getInfo().acceleration + prm.entity.motionY;
         mz = mz * (double)this.getInfo().acceleration + prm.entity.motionZ;
         super.acceleration = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
         MCH_EntityTorpedo e = new MCH_EntityTorpedo(super.worldObj, prm.posX, prm.posY, prm.posZ, mx, my, mz, yaw, 0.0F, (double)super.acceleration);
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
      } else {
         float yaw = prm.rotYaw;
         float pitch = prm.rotPitch;
         double mx = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double mz = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double my = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
         mx = mx * (double)this.getInfo().acceleration + prm.entity.motionX;
         my = my * (double)this.getInfo().acceleration + prm.entity.motionY;
         mz = mz * (double)this.getInfo().acceleration + prm.entity.motionZ;
         super.acceleration = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
         MCH_EntityTorpedo e = new MCH_EntityTorpedo(super.worldObj, prm.posX, prm.posY, prm.posZ, mx, my, mz, yaw, 0.0F, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         Entity tgtEnt = prm.user != null ? prm.user.worldObj.getEntityByID(prm.option1) : null;
         if(tgtEnt != null && !tgtEnt.isDead && tgtEnt != prm.entity && tgtEnt != prm.user && tgtEnt.isInWater()) {
            e.setTargetEntity(tgtEnt);
         }
         e.motionX = mx;
         e.motionY = my;
         e.motionZ = mz;
         e.accelerationInWater = this.getInfo() != null?(double)this.getInfo().accelerationInWater:1.0D;
         super.worldObj.spawnEntityInWorld(e);
         this.playSound(prm.entity);
         return true;
      }
   }
}
