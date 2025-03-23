package mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public abstract class MCH_WeaponEntitySeeker extends MCH_WeaponBase {

   public MCH_IEntityLockChecker entityLockChecker;
   public MCH_WeaponGuidanceSystem guidanceSystem;


   public MCH_WeaponEntitySeeker(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      this.guidanceSystem = new MCH_WeaponGuidanceSystem(w);
      this.guidanceSystem.lockRange = wi.maxLockOnRange;
      this.guidanceSystem.lockAngle = wi.maxLockOnAngle;
      this.guidanceSystem.antiFlareCount = wi.antiFlareCount;
      this.guidanceSystem.pdHDNMaxDegree = wi.pdHDNMaxDegree;
      this.guidanceSystem.pdHDNMaxDegreeLockOutCount = wi.pdHDNMaxDegreeLockOutCount;
      this.guidanceSystem.lockMinHeight = wi.lockMinHeight;
      this.guidanceSystem.setLockCountMax(25);
      this.guidanceSystem.isHeatSeekerMissile = wi.isHeatSeekerMissile;
      this.guidanceSystem.isRadarMissile = wi.isRadarMissile;
      this.guidanceSystem.passiveRadar = wi.passiveRadar;
      this.guidanceSystem.passiveRadarLockOutCount = wi.passiveRadarLockOutCount;
      this.guidanceSystem.canLockMissile = wi.canLockMissile;
   }

   @Override
   public MCH_WeaponGuidanceSystem getGuidanceSystem() {
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

   public static List<MCH_EntityBaseBullet> getShootBullets(World worldObj, Entity user, int range) {
      List list = worldObj.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, AxisAlignedBB.getBoundingBox(
              user.posX - range, user.posY - range, user.posZ - range,
              user.posX + range, user.posY + range, user.posZ + range
      ));
      List<MCH_EntityBaseBullet> result = new ArrayList<>();
      if (list != null) {
         for (Object o : list) {
            if(o != null) {
               MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet) o;
               if(!msl.isDead && msl.getInfo() != null && msl.getInfo().passiveRadar) {
                  //System.out.println("检测到雷达弹"); //TODO shootingEntity always null in client side
                  result.add(msl);
               }
            }
         }
      }
      return result;
   }
}
