package mcheli.weapon;

import mcheli.MCH_Lib;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponRocket extends MCH_WeaponBase {

   public MCH_WeaponRocket(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 4.0F;
      super.explosionPower = 3;
      super.power = 22;
      super.interval = 5;
      if(w.isRemote) {
         super.interval += 2;
      }

   }

   public String getName() {
      return super.getName() + (this.getCurrentMode() == 0?"":" [AIRBURST]");
   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
         MCH_EntityRocket e = new MCH_EntityRocket(super.worldObj, prm.posX, prm.posY, prm.posZ, v.xCoord, v.yCoord, v.zCoord, prm.rotYaw, prm.rotPitch, (double)super.acceleration);
         e.setName(super.name);
         if(prm.option1 == 1) {
            e.isAirburst = true;
         }else{
            this.weaponInfo.explosionAltitude = 0;
         }
         //this.aircraft.print("Airburst: "  + prm.option1);
         e.setParameterFromWeapon(this, prm.entity, prm.user);

         super.worldObj.spawnEntityInWorld(e);
      } else {
         super.optionParameter1 = this.getCurrentMode();
      }

      return true;
   }
}
