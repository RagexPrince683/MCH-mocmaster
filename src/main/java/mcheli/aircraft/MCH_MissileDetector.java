package mcheli.aircraft;

import mcheli.MCH_PacketNotifyLock;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;

public class MCH_MissileDetector {

   private MCH_EntityAircraft ac;
   private World world;
   private int alertCount;
   public static final int SEARCH_RANGE = 60;


   public MCH_MissileDetector(MCH_EntityAircraft aircraft, World w) {
      this.world = w;
      this.ac = aircraft;
      this.alertCount = 0;
   }

   public void update() {
      if(this.ac.haveFlare()) {
         if(this.alertCount > 0) {
            --this.alertCount;
         }

         boolean isLocked = this.ac.getEntityData().getBoolean("Tracking");
         if(isLocked) {
            this.ac.getEntityData().setBoolean("Tracking", false);
         }

         if(this.ac.getEntityData().getBoolean("LockOn")) {
            if(this.alertCount == 0) {
               this.alertCount = 10;
               if(this.ac != null && this.ac.haveFlare() && !this.ac.isDestroyed()) {
                  for(int rider = 0; rider < 2; ++rider) {
                     Entity entity = this.ac.getEntityBySeatId(rider);
                     if(entity instanceof EntityPlayerMP) {
                        MCH_PacketNotifyLock.sendToPlayer((EntityPlayerMP)entity);
                     }
                  }
               }
            }

            this.ac.getEntityData().setBoolean("LockOn", false);
         }

         if(!this.ac.isDestroyed()) {
            Entity var4 = this.ac.getRiddenByEntity();
            if(var4 == null) {
               var4 = this.ac.getEntityBySeatId(1);
            }

            if(var4 != null) {
               if(this.ac.isFlareUsing()) {
                 // this.destroyMissile();
               } else if(!this.ac.isUAV() && !this.world.isRemote) {
                  if(this.alertCount == 0 && (isLocked || this.isLockedByMissile())) {
                     this.alertCount = 20;
                     W_WorldFunc.MOD_playSoundAtEntity(this.ac, "alert", 1.0F, 1.0F);
                  }
               } else if(this.ac.isUAV() && this.world.isRemote && this.alertCount == 0 && (isLocked || this.isLockedByMissile())) {
                  this.alertCount = 20;
                  if(W_Lib.isClientPlayer(var4)) {
                     W_McClient.MOD_playSoundFX("alert", 1.0F, 1.0F);
                  }
               }
            }

         }
      }
   }

   public boolean destroyMissile() {
      List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(60.0D, 60.0D, 60.0D));
      if(list != null) {
         for(int i = 0; i < list.size(); ++i) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet)list.get(i);
            if(msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
               msl.targetEntity = null;
               msl.setDead();
            }
         }
      }

      return false;
   }

   public boolean isLockedByMissile() {
      List list = this.world.getEntitiesWithinAABB(MCH_EntityBaseBullet.class, this.ac.boundingBox.expand(60.0D, 60.0D, 60.0D));
      if(list != null) {
         for(int i = 0; i < list.size(); ++i) {
            MCH_EntityBaseBullet msl = (MCH_EntityBaseBullet)list.get(i);
            if(msl.targetEntity != null && (this.ac.isMountedEntity(msl.targetEntity) || msl.targetEntity.equals(this.ac))) {
               return true;
            }
         }
      }

      return false;
   }
}
