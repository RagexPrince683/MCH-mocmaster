package mcheli.weapon;

import mcheli.MCH_Lib;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponDispenser extends MCH_WeaponBase {

   public MCH_WeaponDispenser(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 0.5F;
      super.explosionPower = 0;
      super.power = 0;
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
         MCH_EntityDispensedItem e = new MCH_EntityDispensedItem(super.worldObj, prm.posX, prm.posY, prm.posZ, v.xCoord, v.yCoord, v.zCoord, prm.rotYaw, prm.rotPitch, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.motionX = prm.entity.motionX + e.motionX * 0.5D;
         e.motionY = prm.entity.motionY + e.motionY * 0.5D;
         e.motionZ = prm.entity.motionZ + e.motionZ * 0.5D;
         e.posX += e.motionX * 0.5D;
         e.posY += e.motionY * 0.5D;
         e.posZ += e.motionZ * 0.5D;
         super.worldObj.spawnEntityInWorld(e);
      }

      return true;
   }
}
