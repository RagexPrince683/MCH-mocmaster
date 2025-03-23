package mcheli.weapon;

import mcheli.weapon.MCH_EntityATMissile;
import mcheli.weapon.MCH_WeaponEntitySeeker;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponATMissile extends MCH_WeaponEntitySeeker {

   public MCH_WeaponATMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 32;
      super.acceleration = 2.0F;
      super.explosionPower = 4;
      super.interval = 5;
      if(w.isRemote) {
         super.interval += 5;
      }

      super.numMode = 2;
      super.guidanceSystem.canLockOnGround = true;
      super.guidanceSystem.ridableOnly = wi.ridableOnly;
   }

   public boolean isCooldownCountReloadTime() {
      return true;
   }

   public String getName() {
      String opt = "";
      if(this.getCurrentMode() == 1) {
         opt = " [TA]";
      }

      return super.getName() + opt;
   }

   public void update(int countWait) {
      super.update(countWait);
   }

   @Override
   public boolean shot(MCH_WeaponParam prm) {
      boolean result = false;
      if(!super.worldObj.isRemote) {
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
            MCH_EntityATMissile e = new MCH_EntityATMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
            if (yaw > 180.0F) {//so we are just basically defining yaw to like not go 360 mlg mode right hopefully pray to god it works okay
               yaw -= 360.0F;
            } else if (yaw < -180.0F) {
               yaw += 360.0F;
            }
            e.setName(super.name);
            e.setParameterFromWeapon(this, prm.entity, prm.user);
            e.guidanceType = prm.option2;
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
               MCH_EntityATMissile e = new MCH_EntityATMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double) super.acceleration);
               if (yaw > 180.0F) {//so we are just basically defining yaw to like not go 360 mlg mode right hopefully pray to god it works okay
                  yaw -= 360.0F;
               } else if (yaw < -180.0F) {
                  yaw += 360.0F;
               }
               e.setName(super.name);
               e.setParameterFromWeapon(this, prm.entity, prm.user);
               e.setTargetEntity(tgtEnt);
               e.guidanceType = prm.option2;
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
            super.optionParameter2 = this.getCurrentMode();
         }
      }

      return result;
   }

   @Override
   public boolean lock(MCH_WeaponParam prm) {
      if(!super.worldObj.isRemote) {
         // do nothing
      } else {
         if(getInfo().passiveRadar) {
            super.guidanceSystem.lock(prm.user);
            if(guidanceSystem.isLockComplete()) {
               Entity target = guidanceSystem.lastLockEntity;
               //获取玩家射击的AT弹
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
