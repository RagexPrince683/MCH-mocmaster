package mcheli.weapon;

import mcheli.weapon.MCH_EntityAAMissile;
import mcheli.weapon.MCH_WeaponEntitySeeker;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponAAMissile extends MCH_WeaponEntitySeeker {

   public MCH_WeaponAAMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 12;
      super.acceleration = 2.5F;
      super.explosionPower = 4;
      super.interval = 5;
      if(w.isRemote) {
         super.interval += 5;
      }

      super.guidanceSystem.canLockInAir = true;
      super.guidanceSystem.ridableOnly = wi.ridableOnly;
   }

   public boolean isCooldownCountReloadTime() {
      return true;
   }

   public void update(int countWait) {
      super.update(countWait);
   }

   public boolean shot(MCH_WeaponParam prm) {
      boolean result = false;
      if(!super.worldObj.isRemote) {
         Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
         if(tgtEnt != null && !tgtEnt.isDead) {
            this.playSound(prm.entity);
            float yaw = prm.entity.rotationYaw + super.fixRotationYaw;
            float pitch = prm.entity.rotationPitch + super.fixRotationPitch;
            double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
            double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
            MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double)super.acceleration);
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.setTargetEntity(tgtEnt);
            super.worldObj.spawnEntityInWorld(e);
            result = true;
         }
      } else if(super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
         result = true;
         super.optionParameter1 = W_Entity.getEntityId(super.guidanceSystem.lastLockEntity);
      }

      return result;
   }
}
