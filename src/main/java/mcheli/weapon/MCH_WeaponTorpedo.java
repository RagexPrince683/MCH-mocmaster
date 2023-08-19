package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
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
      float yaw = prm.user.rotationYaw;
      float pitch = prm.user.rotationPitch;
      Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -yaw, -pitch, -prm.rotRoll);
      double tX = v.xCoord;
      double tZ = v.zCoord;
      double tY = v.yCoord;
      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      if(super.worldObj.isRemote) {
         tX = tX * 100.0D / dist;
         tY = tY * 100.0D / dist;
         tZ = tZ * 100.0D / dist;
      } else {
         tX = tX * 150.0D / dist;
         tY = tY * 150.0D / dist;
         tZ = tZ * 150.0D / dist;
      }

      Vec3 src = W_WorldFunc.getWorldVec3(super.worldObj, prm.user.posX, prm.user.posY, prm.user.posZ);
      Vec3 dst = W_WorldFunc.getWorldVec3(super.worldObj, prm.user.posX + tX, prm.user.posY + tY, prm.user.posZ + tZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, src, dst);
      if(m != null && W_MovingObjectPosition.isHitTypeTile(m) && MCH_Lib.isBlockInWater(super.worldObj, m.blockX, m.blockY, m.blockZ)) {
         if(!super.worldObj.isRemote) {
            double mx = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double mz = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double my = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
            mx = mx * (double)this.getInfo().acceleration + prm.entity.motionX;
            my = my * (double)this.getInfo().acceleration + prm.entity.motionY;
            mz = mz * (double)this.getInfo().acceleration + prm.entity.motionZ;
            super.acceleration = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
            MCH_EntityTorpedo e = new MCH_EntityTorpedo(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, yaw, 0.0F, (double)super.acceleration);
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.targetPosX = m.hitVec.xCoord;
            e.targetPosY = m.hitVec.yCoord;
            e.targetPosZ = m.hitVec.zCoord;
            e.motionX = mx;
            e.motionY = my;
            e.motionZ = mz;
            e.accelerationInWater = this.getInfo() != null?(double)this.getInfo().accelerationInWater:1.0D;
            super.worldObj.spawnEntityInWorld(e);
            this.playSound(prm.entity);
         }

         return true;
      } else {
         return false;
      }
   }
}
