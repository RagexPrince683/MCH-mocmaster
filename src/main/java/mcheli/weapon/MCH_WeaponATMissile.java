package mcheli.weapon;

import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponATMissile extends MCH_WeaponEntitySeeker {
   MCH_WeaponGuidanceSystem guidanceSystem= new MCH_WeaponGuidanceSystem(this.worldObj);
   public MCH_WeaponATMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 32;
      super.acceleration = 2.0F;
      super.explosionPower = 4;
      super.interval = -100;
      if(w.isRemote) {
         super.interval -= 10;
      }

      super.numMode = 2;
      //this.guidanceSystem = new MCH_WeaponGuidanceSystem(this.worldObj);
      super.guidanceSystem.canLockOnGround = true;
      super.guidanceSystem.ridableOnly = wi.ridableOnly;
      this.guidanceSystem.canLockOnGround = true;
      this.guidanceSystem.ridableOnly = wi.ridableOnly;
      this.guidanceSystem.setLockCountMax(wi.lockTime);
   }

   
   @Override
   public MCH_WeaponGuidanceSystem getGuidanceSystem() {
	   return this.guidanceSystem;
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

   public boolean shot(MCH_WeaponParam prm) {
      return super.worldObj.isRemote?this.shotClient(prm.entity, prm.user):this.shotServer(prm);
   }

   protected boolean shotClient(Entity entity, Entity user) {
      boolean result = false;
      if(this.guidanceSystem.lock(user) && this.guidanceSystem.lastLockEntity != null) {
         result = true;
         System.out.println("Firing!");
         super.optionParameter1 = W_Entity.getEntityId((Entity)this.guidanceSystem.lastLockEntity); //TODO FIX
      }

      super.optionParameter2 = this.getCurrentMode();
      return result;
   }

   protected boolean shotServer(MCH_WeaponParam prm) {
      Entity tgtEnt = null;
      tgtEnt = prm.user.worldObj.getEntityByID(prm.option1);
      if(tgtEnt != null && !tgtEnt.isDead) {
         float yaw = prm.user.rotationYaw + super.fixRotationYaw;
         float pitch = prm.entity.rotationPitch + super.fixRotationPitch;
         double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
         MCH_EntityATMissile e = new MCH_EntityATMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double)super.acceleration);
         e.setName(super.name);
         e.setParameterFromWeapon(this, prm.entity, prm.user);
         e.setTargetEntity(tgtEnt);
         e.guidanceType = prm.option2;
         super.worldObj.spawnEntityInWorld(e);
         this.playSound(prm.entity);
         return true;
      } else {
         return false;
      }
   }
}
