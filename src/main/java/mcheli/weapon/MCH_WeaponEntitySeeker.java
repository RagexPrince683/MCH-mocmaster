package mcheli.weapon;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class MCH_WeaponEntitySeeker extends MCH_WeaponBase {

   public MCH_IEntityLockChecker entityLockChecker;
   public MCH_GuidanceSystem guidanceSystem;


   public MCH_WeaponEntitySeeker(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      this.guidanceSystem = new MCH_RadarGuidanceSystem(w);
      this.guidanceSystem.lockRange = guidanceSystem.lockRange = wi.radius;
      this.guidanceSystem.lockAngle = 5;
      this.guidanceSystem.setLockCountMax(25);
   }

   
   public MCH_GuidanceSystem getGuidanceSystem() {
      return this.guidanceSystem;
   }

   public int getLockCount() {
      return this.guidanceSystem.getLockCount();
   }

   public void setLockCountMax(int n) {
      this.guidanceSystem.setLockCountMax(n);
   }

   public int getLockCountMax() {
      return this.guidanceSystem.getLockCountMax();
   }

   public void setLockChecker(MCH_IEntityLockChecker checker) {
      this.guidanceSystem.checker = checker;
   }

   public void update(int countWait) {
      super.update(countWait);
      this.guidanceSystem.update();
   }
}
