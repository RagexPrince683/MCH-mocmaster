package mcheli.weapon;

import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_WeaponAAMissile;
import mcheli.weapon.MCH_WeaponASMissile;
import mcheli.weapon.MCH_WeaponATMissile;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponBomb;
import mcheli.weapon.MCH_WeaponCAS;
import mcheli.weapon.MCH_WeaponDispenser;
import mcheli.weapon.MCH_WeaponDummy;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.weapon.MCH_WeaponMachineGun1;
import mcheli.weapon.MCH_WeaponMachineGun2;
import mcheli.weapon.MCH_WeaponMarkerRocket;
import mcheli.weapon.MCH_WeaponRocket;
import mcheli.weapon.MCH_WeaponSmoke;
import mcheli.weapon.MCH_WeaponTargetingPod;
import mcheli.weapon.MCH_WeaponTorpedo;
import mcheli.weapon.MCH_WeaponTvMissile;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponCreator {

   public static MCH_WeaponBase createWeapon(World w, String weaponName, Vec3 v, float yaw, float pitch, MCH_IEntityLockChecker lockChecker, boolean onTurret) {
      MCH_WeaponInfo info = MCH_WeaponInfoManager.get(weaponName);
      if(info != null && info.type != "") {
         Object weapon = null;
         if(info.type.compareTo("machinegun1") == 0) {
            weapon = new MCH_WeaponMachineGun1(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("machinegun2") == 0) {
            weapon = new MCH_WeaponMachineGun2(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("tvmissile") == 0) {
            weapon = new MCH_WeaponTvMissile(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("torpedo") == 0) {
            weapon = new MCH_WeaponTorpedo(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("cas") == 0) {
            weapon = new MCH_WeaponCAS(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("rocket") == 0) {
            weapon = new MCH_WeaponRocket(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("asmissile") == 0) {
            weapon = new MCH_WeaponASMissile(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("aamissile") == 0) {
            weapon = new MCH_WeaponAAMissile(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("atmissile") == 0) {
            weapon = new MCH_WeaponATMissile(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("bomb") == 0) {
            weapon = new MCH_WeaponBomb(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("mkrocket") == 0) {
            weapon = new MCH_WeaponMarkerRocket(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("dummy") == 0) {
            weapon = new MCH_WeaponDummy(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("smoke") == 0) {
            weapon = new MCH_WeaponSmoke(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("dispenser") == 0) {
            weapon = new MCH_WeaponDispenser(w, v, yaw, pitch, weaponName, info);
         }

         if(info.type.compareTo("targetingpod") == 0) {
            weapon = new MCH_WeaponTargetingPod(w, v, yaw, pitch, weaponName, info);
         }

         if(weapon != null) {
            ((MCH_WeaponBase)weapon).displayName = info.displayName;
            ((MCH_WeaponBase)weapon).power = info.power;
            ((MCH_WeaponBase)weapon).acceleration = info.acceleration;
            ((MCH_WeaponBase)weapon).explosionPower = info.explosion;
            ((MCH_WeaponBase)weapon).explosionPowerInWater = info.explosionInWater;
            ((MCH_WeaponBase)weapon).interval = info.delay;
            ((MCH_WeaponBase)weapon).setLockCountMax(info.lockTime);
            ((MCH_WeaponBase)weapon).setLockChecker(lockChecker);
            ((MCH_WeaponBase)weapon).numMode = info.modeNum;
            ((MCH_WeaponBase)weapon).piercing = info.piercing;
            ((MCH_WeaponBase)weapon).heatCount = info.heatCount;
            ((MCH_WeaponBase)weapon).onTurret = onTurret;
            if(info.maxHeatCount > 0 && ((MCH_WeaponBase)weapon).heatCount < 2) {
               ((MCH_WeaponBase)weapon).heatCount = 2;
            }

            if(w.isRemote) {
               if(((MCH_WeaponBase)weapon).interval < 4) {
                  ++((MCH_WeaponBase)weapon).interval;
               } else if(((MCH_WeaponBase)weapon).interval < 7) {
                  ((MCH_WeaponBase)weapon).interval += 2;
               } else if(((MCH_WeaponBase)weapon).interval < 10) {
                  ((MCH_WeaponBase)weapon).interval += 3;
               } else if(((MCH_WeaponBase)weapon).interval < 20) {
                  ((MCH_WeaponBase)weapon).interval += 6;
               } else {
                  ((MCH_WeaponBase)weapon).interval += 10;
                  if(((MCH_WeaponBase)weapon).interval >= 40) {
                     ((MCH_WeaponBase)weapon).interval = -((MCH_WeaponBase)weapon).interval;
                  }
               }

               ++((MCH_WeaponBase)weapon).heatCount;
               ((MCH_WeaponBase)weapon).cartridge = info.cartridge;
            }

            ((MCH_WeaponBase)weapon).modifyCommonParameters();
         }

         return (MCH_WeaponBase)weapon;
      } else {
         return null;
      }
   }
}
