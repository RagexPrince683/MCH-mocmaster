package mcheli.weapon;

import mcheli.MCH_Lib;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponMachineGun2 extends MCH_WeaponBase {

   public MCH_WeaponMachineGun2(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 16;
      super.acceleration = 4.0F;
      super.explosionPower = 1;
      super.interval = 2;
      super.numMode = 2;
   }

   public void modifyParameters() {
      if(super.explosionPower == 0) {
         super.numMode = 0;
      }

   }

   public String getName() {
      return super.getName() + (this.getCurrentMode() == 0?"":" [HE]");
   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
    	  if(prm.isTurret) {
    		  prm.rotYaw = prm.user.rotationYaw;
    		  prm.rotPitch = prm.user.rotationPitch;
    		}
         Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
         MCH_EntityBullet e = new MCH_EntityBullet(super.worldObj, prm.posX, prm.posY, prm.posZ, v.xCoord, v.yCoord, v.zCoord, prm.rotYaw, prm.rotPitch, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         if(this.getInfo().modeNum < 2) {
            e.explosionPower = super.explosionPower;
         } else {
            e.explosionPower = prm.option1 == 0?-super.explosionPower:super.explosionPower;
         }

         e.posX += e.motionX * 0.5D;
         e.posY += e.motionY * 0.5D;
         e.posZ += e.motionZ * 0.5D;
         super.worldObj.spawnEntityInWorld(e);
         this.playSound(prm.entity);
      } else {
         super.optionParameter1 = this.getCurrentMode();
      }

      return true;
   }
}
