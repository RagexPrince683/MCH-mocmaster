package mcheli.weapon;

import mcheli.MCH_Explosion;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.weapon.MCH_EntityBomb;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponBomb extends MCH_WeaponBase {

   public MCH_WeaponBomb(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 0.5F;
      super.explosionPower = 9;
      super.power = 35;
      super.interval = -90;
      if(w.isRemote) {
         super.interval -= 10;
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      if(this.getInfo() != null && this.getInfo().destruct) {
         if(prm.entity instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft e1 = (MCH_EntityAircraft)prm.entity;
            if(e1.isUAV() && e1.getSeatNum() == 0) {
               if(!super.worldObj.isRemote) {
                  MCH_Explosion.newExplosion(super.worldObj, (Entity)null, prm.user, e1.posX, e1.posY, e1.posZ, (float)this.getInfo().explosion, (float)this.getInfo().explosionBlock, true, true, this.getInfo().flaming, true, 0);
                  this.playSound(prm.entity);
               }

               e1.destruct();
            }
         }
      } else if(!super.worldObj.isRemote) {
         this.playSound(prm.entity);
         MCH_EntityBomb e = new MCH_EntityBomb(super.worldObj, prm.posX, prm.posY, prm.posZ, prm.entity.motionX, prm.entity.motionY, prm.entity.motionZ, prm.entity.rotationYaw, 0.0F, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.motionX = prm.entity.motionX;
         e.motionY = prm.entity.motionY;
         e.motionZ = prm.entity.motionZ;
         super.worldObj.spawnEntityInWorld(e);
      }

      return true;
   }
}
