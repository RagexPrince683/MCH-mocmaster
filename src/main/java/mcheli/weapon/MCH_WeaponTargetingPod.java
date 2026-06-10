package mcheli.weapon;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class MCH_WeaponTargetingPod extends MCH_WeaponBase {

   public MCH_WeaponTargetingPod(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   private boolean shotSonar(MCH_WeaponParam prm, MCH_WeaponInfo info) {
      if(!(prm.user instanceof EntityLivingBase)) {
         return false;
      }

      EntityLivingBase user = (EntityLivingBase)prm.user;

      // Sonar radius:
      // Length = base sonar range
      // Power = strength multiplier
      // Example: Length 16, Power 10 => 160 block radius
      float sonarRange = info.length * Math.max(1.0F, super.power);

      // Anti-lag clamp.
      if(sonarRange > 256.0F) {
         sonarRange = 256.0F;
      }

      if(sonarRange < 8.0F) {
         sonarRange = 8.0F;
      }

      return MCH_Multiplay.spotEntityRadius(
              user,
              prm.posX,
              prm.posY,
              prm.posZ,
              info.target,
              sonarRange,
              info.markTime,
              true
      );
   }







   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         MCH_WeaponInfo info = this.getInfo();

         // Active sonar mode:
         // If this TargetingPod has Power > 0, treat it as a radial sonar ping
         // instead of normal optical/radar spotting.
         if(super.power > 0 && (info.target & 64) != 0) {
            boolean found = this.shotSonar(prm, info);

            if(found) {
               this.playSound(prm.entity);
            } else {
               this.playSound(prm.entity, "ng");
            }

            return true;
         }

         if((info.target & 64) != 0) {
            if(MCH_Multiplay.markPoint((EntityPlayer)prm.user, prm.posX, prm.posY, prm.posZ)) {
               this.playSound(prm.user);
            } else {
               this.playSound(prm.user, "ng");
            }
         } else if(MCH_Multiplay.spotEntity((EntityLivingBase)prm.user, (MCH_EntityAircraft)prm.entity, prm.posX, prm.posY, prm.posZ, info.target, info.length, info.markTime, info.angle)) {
            this.playSound(prm.entity);
         } else {
            this.playSound(prm.entity, "ng");
         }
      }

      return true;
   }
}
