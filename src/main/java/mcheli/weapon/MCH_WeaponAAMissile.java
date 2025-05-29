package mcheli.weapon;

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

   //todo no more locking from rear/free look mode
   //if this in free look mode, then we should not be able to lock on to targets

   @Override
   public boolean shot(MCH_WeaponParam prm) {
      boolean result = false;
      if(!super.worldObj.isRemote) {

         //if (!this.aircraft.isFreeLookMode())
         //todo shouldnt matter but if it does uncomment

         if(getInfo().passiveRadar || getInfo().activeRadar) {
            this.playSound(prm.entity);

            float yaw, pitch;
            if(getInfo().enableOffAxis) {
               yaw = prm.user.rotationYaw + super.fixRotationYaw;
               pitch = prm.user.rotationPitch + super.fixRotationPitch;
            } else {
               yaw = prm.entity.rotationYaw + super.fixRotationYaw;
               pitch = prm.entity.rotationPitch + super.fixRotationPitch;
            }
            double tX = -MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
            double tZ = MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F);
            double tY = -MathHelper.sin(pitch / 180.0F * 3.1415927F);
            MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
            if (yaw > 180.0F) {//so we are just basically defining yaw to like not go 360 mlg mode right hopefully pray to god it works okay
               yaw -= 360.0F;
            } else if (yaw < -180.0F) {
               yaw += 360.0F;
            }
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            super.worldObj.spawnEntityInWorld(e);
            result = true;
         } else {
            Entity tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
            if (tgtEnt != null && !tgtEnt.isDead) {
               this.playSound(prm.entity);
               float yaw, pitch;
               if(getInfo().enableOffAxis) {
                  yaw = prm.user.rotationYaw + super.fixRotationYaw;
                  pitch = prm.user.rotationPitch + super.fixRotationPitch;
               } else {
                  yaw = prm.entity.rotationYaw + super.fixRotationYaw;
                  pitch = prm.entity.rotationPitch + super.fixRotationPitch;
               }
               double tX = (double) (-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
               double tZ = (double) (MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
               double tY = (double) (-MathHelper.sin(pitch / 180.0F * 3.1415927F));
               MCH_EntityAAMissile e = new MCH_EntityAAMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
               if (yaw > 180.0F) {//so we are just basically defining yaw to like not go 360 mlg mode right hopefully pray to god it works okay
                  yaw -= 360.0F;
               } else if (yaw < -180.0F) {
                  yaw += 360.0F;
               }
               e.setName(super.name);
               e.setParameterFromWeapon(this, prm.entity, prm.user);
               e.setTargetEntity(tgtEnt);
               super.worldObj.spawnEntityInWorld(e);
               result = true;
            }
         }
      } else {
         if(getInfo().passiveRadar || getInfo().activeRadar) {
            result = true;
         } else if (super.guidanceSystem.lock(prm.user) && super.guidanceSystem.lastLockEntity != null) {
            result = true;
            super.optionParameter1 = W_Entity.getEntityId(super.guidanceSystem.lastLockEntity);
         }
      }

      return result;
   }

   @Override
   public boolean lock(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote ) {
         //|| this.aircraft.isFreeLookMode() did not work
         //YOU WILL LOOK AT THE FUCKING TARGET LEGITIMATELY EDWARD
         // do nothing
      } else {

         //if (this.aircraft.isFreeLookMode())

         if(getInfo().passiveRadar) {
            super.guidanceSystem.lock(prm.user);
            if(guidanceSystem.isLockComplete()) {
               Entity target = guidanceSystem.lastLockEntity;
               //获取玩家射击的AA弹
               for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                  bullet.clientSetTargetEntity(target);
                  super.optionParameter1 = W_Entity.getEntityId(target);
               }
            }
            else {
               for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                  bullet.clientSetTargetEntity(null);
                  super.optionParameter1 = 0;
               }
            }
         }
      }
      return false;
   }

   @Override
   public void onUnlock(MCH_WeaponParam prm) {
      if(worldObj.isRemote) {
         if (guidanceSystem != null && prm.user != null) {
            if (!guidanceSystem.isLockComplete()) {
               for (MCH_EntityBaseBullet bullet : getShootBullets(worldObj, prm.user, getInfo().maxLockOnRange)) {
                  bullet.clientSetTargetEntity(null);
                  super.optionParameter1 = 0;
               }
            }
         }
      }
   }
}
