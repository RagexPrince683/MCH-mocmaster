package mcheli.weapon;

import mcheli.MCH_Lib;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponMarkerRocket extends MCH_WeaponBase {

   public MCH_WeaponMarkerRocket(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 3.0F;
      super.explosionPower = 0;
      super.power = 0;
      super.interval = 60;
      if(w.isRemote) {
         super.interval += 10;
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
         MCH_EntityMarkerRocket e = new MCH_EntityMarkerRocket(super.worldObj, prm.posX, prm.posY, prm.posZ, v.xCoord, v.yCoord, v.zCoord, prm.rotYaw, prm.rotPitch, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.setMarkerStatus(1);
         super.worldObj.spawnEntityInWorld(e);
      } else {
         super.optionParameter1 = this.getCurrentMode();
      }

      return true;
   }
}
