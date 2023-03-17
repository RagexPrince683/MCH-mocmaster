package mcheli.weapon;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponDummy extends MCH_WeaponBase {

   static final MCH_WeaponInfo dummy = new MCH_WeaponInfo("none");


   public int getUseInterval() {
      return 0;
   }

   public MCH_WeaponDummy(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, !nm.isEmpty()?nm:"none", wi != null?wi:dummy);
   }

   public boolean shot(MCH_WeaponParam prm) {
      return false;
   }

}
