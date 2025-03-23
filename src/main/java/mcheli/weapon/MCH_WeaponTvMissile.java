package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketNotifyTVMissileEntity;
import mcheli.network.packets.PacketLaserGuidanceTargeting;
import mcheli.wrapper.W_Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponTvMissile extends MCH_WeaponBase {

   protected MCH_EntityTvMissile lastShotTvMissile = null;
   protected Entity lastShotEntity = null;
   protected boolean isTVGuided = false;
   public MCH_LaserGuidanceSystem guidanceSystem;

   //todo: add guided cluster munition type/bomblet handling


   public MCH_WeaponTvMissile(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.power = 32;
      super.acceleration = 2.0F;
      super.explosionPower = 4;
      super.interval = -100;
      if(w.isRemote) {
         super.interval -= 10;
      }

      super.numMode = 2;
      this.lastShotEntity = null;
      this.lastShotTvMissile = null;
      this.isTVGuided = false;

      if (getInfo().laserGuidance) {
         this.guidanceSystem = new MCH_LaserGuidanceSystem();
         guidanceSystem.worldObj = w;
         guidanceSystem.hasLaserGuidancePod = wi.hasLaserGuidancePod;
         if (w.isRemote) {
            initGuidanceSystemClient();
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void initGuidanceSystemClient() {
      guidanceSystem.user = Minecraft.getMinecraft().thePlayer;
   }

   @Override
   public MCH_LaserGuidanceSystem getGuidanceSystem() {
      return this.guidanceSystem;
   }

   public String getName() {
      String opt = "";
      if(this.getCurrentMode() == 0) {
         opt = " [TV]";
      }

      if(this.getCurrentMode() == 2) {
         opt = " [TA]";
      }

      return super.getName() + opt;
   }

   public void update(int countWait) {
      super.update(countWait);

      if(!super.worldObj.isRemote) {
         if(this.isTVGuided && super.tick <= 9) {
            if(super.tick % 3 == 0 && this.lastShotTvMissile != null && !this.lastShotTvMissile.isDead && this.lastShotEntity != null && !this.lastShotEntity.isDead) {
               MCH_PacketNotifyTVMissileEntity.send(W_Entity.getEntityId(this.lastShotEntity), W_Entity.getEntityId(this.lastShotTvMissile));
            }

            if(super.tick == 9) {
               this.lastShotEntity = null;
               this.lastShotTvMissile = null;
            }
         }

         if(super.tick <= 2 && this.lastShotEntity instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft)this.lastShotEntity).setTVMissile(this.lastShotTvMissile);
         }
      }

   }

   public boolean shot(MCH_WeaponParam prm) {
      return super.worldObj.isRemote?this.shotClient(prm.entity, prm.user):this.shotServer(prm);
   }

   protected boolean shotClient(Entity entity, Entity user) {
      super.optionParameter2 = 0;
      super.optionParameter1 = this.getCurrentMode();
      return true;
   }

   protected boolean shotServer(MCH_WeaponParam prm) {

      float yaw, pitch;
      if(getInfo().enableOffAxis) {
         yaw = prm.user.rotationYaw + super.fixRotationYaw;
         pitch = prm.user.rotationPitch + super.fixRotationPitch;
      } else {
         yaw = prm.entity.rotationYaw + super.fixRotationYaw;
         pitch = prm.entity.rotationPitch + super.fixRotationPitch;
      }

      double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      this.isTVGuided = prm.option1 == 0;
      float acr = super.acceleration;
      if(!this.isTVGuided) {
         acr = (float)((double)acr * 1.5D);
      }

      MCH_EntityTvMissile e = new MCH_EntityTvMissile(super.worldObj, prm.posX, prm.posY, prm.posZ, tX, tY, tZ, yaw, pitch, (double)acr);
      e.setName(super.name);
      e.setParameterFromWeapon(this, prm.entity, prm.user);
      this.lastShotEntity = prm.entity;
      this.lastShotTvMissile = e;
      super.worldObj.spawnEntityInWorld(e);
      this.playSound(prm.entity);
      return true;
   }

   @Override
   public boolean lock(MCH_WeaponParam prm) {
      if(super.worldObj.isRemote) {
         if(guidanceSystem != null) {
            this.guidanceSystem.targeting = true;
            if(super.tick % 3 == 0) {
               MCH_MOD.getPacketHandler().sendToServer(new PacketLaserGuidanceTargeting(true));
            }
            this.guidanceSystem.update();
         }
      }
      return false;
   }

   @Override
   public void onUnlock(MCH_WeaponParam prm) {
      if(super.worldObj.isRemote) {
         if(guidanceSystem != null) {
            this.guidanceSystem.targeting = false;
            if(super.tick % 3 == 0) {
               MCH_MOD.getPacketHandler().sendToServer(new PacketLaserGuidanceTargeting(false));
            }
         }
      }
   }
}
