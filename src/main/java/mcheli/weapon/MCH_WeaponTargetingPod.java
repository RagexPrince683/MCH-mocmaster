package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.multiplay.MCH_Multiplay;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponTargetingPod extends MCH_WeaponBase {

   public MCH_WeaponTargetingPod(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         MCH_WeaponInfo info = this.getInfo();
         if((info.target & 64) != 0) {
            if(MCH_Multiplay.markPoint((EntityPlayer)prm.user, prm.posX, prm.posY, prm.posZ)) {
               this.playSound(prm.user);
            } else {
               this.playSound(prm.user, "ng");
            }
         } else if(MCH_Multiplay.spotEntity((EntityPlayer)prm.user, (MCH_EntityAircraft)prm.entity, prm.posX, prm.posY, prm.posZ, info.target, info.length, info.markTime, info.angle)) {
            this.playSound(prm.entity);
         } else {
            this.playSound(prm.entity, "ng");
         }
      }

      return true;
   }
}
