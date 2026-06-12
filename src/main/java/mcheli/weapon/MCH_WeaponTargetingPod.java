package mcheli.weapon;

import mcheli.aircraft.MCH_EntityBaseVehicle;
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

      // Sonar radius is Length.
      // Power is damage, NOT range.
      float sonarRange = info.length;

      if(sonarRange > 256.0F) {
         sonarRange = 256.0F;
      }

      if(sonarRange < 8.0F) {
         sonarRange = 8.0F;
      }

      boolean found = MCH_Multiplay.spotEntityRadius(
              user,
              prm.posX,
              prm.posY,
              prm.posZ,
              info.target,
              sonarRange,
              info.markTime,
              true
      );

      this.applySonarDamage(prm, info, sonarRange);

      return found;
   }

   private void applySonarDamage(MCH_WeaponParam prm, MCH_WeaponInfo info, float sonarRange) {
      if(super.power <= 0) {
         return;
      }

      AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
              prm.posX - (double)sonarRange,
              prm.posY - (double)sonarRange,
              prm.posZ - (double)sonarRange,
              prm.posX + (double)sonarRange,
              prm.posY + (double)sonarRange,
              prm.posZ + (double)sonarRange
      );

      List list = super.worldObj.getEntitiesWithinAABB(Entity.class, box);

      for(int i = 0; i < list.size(); ++i) {
         Entity e = (Entity)list.get(i);

         if(e == null || e.isDead) {
            continue;
         }

         if(e == prm.user || e == prm.entity) {
            continue;
         }

         // Do not damage vehicles/ships/submarines with sonar.
         if(e instanceof MCH_EntityBaseVehicle) {
            continue;
         }

         // Only living things: players, squid, mobs, animals.
         if(!(e instanceof EntityLivingBase)) {
            continue;
         }

         // Must match the sonar target filter.
         // Example: Target = ships/others/players lets players and squid be affected.
         if(!MCH_Multiplay.canSpotEntityWithFilter(info.target, e)) {
            continue;
         }

         // Must be in/on water.
         if(!this.isEntityInOrOnWater(e)) {
            continue;
         }

         double dx = e.posX - prm.posX;
         double dy = e.posY - prm.posY;
         double dz = e.posZ - prm.posZ;
         double distSq = dx * dx + dy * dy + dz * dz;

         if(distSq > (double)(sonarRange * sonarRange)) {
            continue;
         }

         // Optional: do not hurt teammates.
         //if(prm.user instanceof EntityLivingBase && !MCH_Multiplay.canAttackEntity((Entity)prm.user, e)) {
         //   continue;
         //}
         //no, wtf this isn't a conventional weapon

         double dist = Math.sqrt(distSq);
         double falloff = 1.0D - Math.min(1.0D, dist / (double)sonarRange);

         // Keep damage meaningful but not insane.
         // At center: full Power.
         // At edge: 25% Power minimum if still detected.
         float damage = (float)((double)super.power * (0.25D + falloff * 0.75D));

         if(damage < 1.0F) {
            damage = 1.0F;
         }

         e.attackEntityFrom(DamageSource.generic, damage);
      }
   }

   private boolean isEntityInOrOnWater(Entity e) {
      if(e == null || e.worldObj == null) {
         return false;
      }

      if(e.isInWater()) {
         return true;
      }

      int x = (int)Math.floor(e.posX);
      int z = (int)Math.floor(e.posZ);

      int minY = (int)Math.floor(e.boundingBox.minY) - 2;
      int maxY = (int)Math.floor(e.boundingBox.minY) + 2;

      for(int y = minY; y <= maxY; ++y) {
         if(e.worldObj.getBlock(x, y, z).getMaterial() == Material.water) {
            return true;
         }
      }

      return false;
   }







   public boolean shot(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         MCH_WeaponInfo info = this.getInfo();

         // Active sonar mode:
         // If this TargetingPod has Power > 0, treat it as a radial sonar ping
         // instead of normal optical/radar spotting.
         if(super.power > 0 && (info.target & 128) != 0) {
            boolean found = this.shotSonar(prm, info);

            // Active sonar always emits a ping.
            // No contacts is not a failure.
            this.playSound(prm.entity);

            // Optional debug while testing.
            // if(!found) {
            //     System.out.println("[MCH] Sonar ping: no contacts");
            // } else {
            //     System.out.println("[MCH] Sonar ping: contact found");
            // }

            return true;
         }

         if((info.target & 64) != 0) {
            if(MCH_Multiplay.markPoint((EntityPlayer)prm.user, prm.posX, prm.posY, prm.posZ)) {
               this.playSound(prm.user);
            } else {
               this.playSound(prm.user, "ng");
            }
         } else if(MCH_Multiplay.spotEntity((EntityLivingBase)prm.user, (MCH_EntityBaseVehicle)prm.entity, prm.posX, prm.posY, prm.posZ, info.target, info.length, info.markTime, info.angle)) {
            this.playSound(prm.entity);
         } else {
            this.playSound(prm.entity, "ng");
         }
      }

      return true;
   }
}
