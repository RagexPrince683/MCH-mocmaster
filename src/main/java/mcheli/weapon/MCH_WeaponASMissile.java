package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.weapon.MCH_EntityASMissile;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponASMissile extends MCH_WeaponBase {

   public MCH_WeaponASMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 3.0F;
      super.explosionPower = 9;
      super.power = 40;
      super.interval = -350;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   public boolean isCooldownCountReloadTime() {
      return true;
   }

   public void update(int countWait) {
      super.update(countWait);
   }

   public boolean shot(MCH_WeaponParam prm) {
      float yaw = prm.user.rotationYaw;
      float pitch = prm.user.rotationPitch;
      double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      if(super.worldObj.isRemote) {
         tX = tX * 200.0D / dist;
         tY = tY * 200.0D / dist;
         tZ = tZ * 200.0D / dist;
      } else {
         tX = tX * 250.0D / dist;
         tY = tY * 250.0D / dist;
         tZ = tZ * 250.0D / dist;
      }

      Vec3 src = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX, prm.entity.posY + 1.62D, prm.entity.posZ);
      Vec3 dst = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX + tX, prm.entity.posY + 1.62D + tY, prm.entity.posZ + tZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, src, dst);
      if(m != null && W_MovingObjectPosition.isHitTypeTile(m) && !MCH_Lib.isBlockInWater(super.worldObj, m.blockX, m.blockY, m.blockZ)) {
         if(!super.worldObj.isRemote) {
            MCH_EntityASMissile e = new MCH_EntityASMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double)super.acceleration);
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.targetPosX = m.hitVec.xCoord;
            e.targetPosY = m.hitVec.yCoord;
            e.targetPosZ = m.hitVec.zCoord;
            super.worldObj.spawnEntityInWorld(e);
            this.playSound(prm.entity);
         }

         return true;
      } else {
         return false;
      }
   }
}
