package mcheli.weapon;

import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponSmoke extends MCH_WeaponBase {

   public MCH_WeaponSmoke(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 0;
   }

   public boolean shot(MCH_WeaponParam prm) {
      return false;
   }
}
