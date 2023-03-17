package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.wrapper.W_McClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Random;

public class MCH_WeaponSet {

   private static Random rand = new Random();
   private final String name;
   protected ArrayList<MCH_WeaponBase> weapons;
   private int currentWeaponIndex;
   public float rotationYaw;
   public float rotationPitch;
   public float prevRotationYaw;
   public float prevRotationPitch;
   public float defaultRotationYaw;
   public float rotationTurretYaw;
   public float rotBay;
   public float prevRotBay;
   public MCH_WeaponSet.Recoil[] recoilBuf;
   protected int numAmmo;
   protected int numRestAllAmmo;
   public int currentHeat;
   public int cooldownSpeed;
   public int countWait;
   public int countReloadWait;
   protected int[] lastUsedCount;
   private static final int WAIT_CLEAR_USED_COUNT = 4;
   public int soundWait;
   private int lastUsedOptionParameter1;
   private int lastUsedOptionParameter2;
   public float rotBarrelSpd;
   public float rotBarrel;
   public float prevRotBarrel;


   public MCH_WeaponSet(MCH_WeaponBase[] weapon) {
      this.lastUsedOptionParameter1 = 0;
      this.lastUsedOptionParameter2 = 0;
      this.name = weapon[0].name;
      this.weapons = new ArrayList<MCH_WeaponBase>();
      for(MCH_WeaponBase w : weapon) {
    	  weapons.add(w);
      }
      this.currentWeaponIndex = 0;
      this.countWait = 0;
      this.countReloadWait = 0;
      this.lastUsedCount = new int[weapon.length];
      this.rotationYaw = 0.0F;
      this.prevRotationYaw = 0.0F;
      this.rotationPitch = 0.0F;
      this.prevRotationPitch = 0.0F;
      this.setAmmoNum(0);
      this.setRestAllAmmoNum(0);
      this.currentHeat = 0;
      this.soundWait = 0;
      this.cooldownSpeed = 1;
      this.rotBarrelSpd = 0.0F;
      this.rotBarrel = 0.0F;
      this.prevRotBarrel = 0.0F;
      this.recoilBuf = new MCH_WeaponSet.Recoil[weapon.length];

      for(int i = 0; i < this.recoilBuf.length; ++i) {
         this.recoilBuf[i] = new MCH_WeaponSet.Recoil(weapon[i].getInfo().recoilBufCount, weapon[i].getInfo().recoilBufCountSpeed);
      }

      this.defaultRotationYaw = 0.0F;
   }

   public MCH_WeaponSet(MCH_WeaponBase weapon) {
      this(new MCH_WeaponBase[]{weapon});
   }

   public void removeWeapon(MCH_WeaponBase weapon) {
	   weapons.remove(weapon);
   }
   
   public void addWeapon(MCH_WeaponBase weapon) {
	   weapons.add(weapon);
   }
   
   public boolean isEqual(String s) {
      return this.name.equalsIgnoreCase(s);
   }

   public int getAmmoNum() {
      return this.numAmmo;
   }

   public int getAmmoNumMax() {
      return this.getFirstWeapon().getNumAmmoMax();
   }

   public int getRestAllAmmoNum() {
      return this.numRestAllAmmo;
   }

   public int getAllAmmoNum() {
      return this.getFirstWeapon().getAllAmmoNum();
   }

   public void setAmmoNum(int n) {
      this.numAmmo = n;
   }

   public void setRestAllAmmoNum(int n) {
      int debugBefore = this.numRestAllAmmo;
      int m = this.getInfo().maxAmmo - this.getAmmoNum();
      this.numRestAllAmmo = n <= m?n:m;
      MCH_Lib.DbgLog(this.getFirstWeapon().worldObj, "MCH_WeaponSet.setRestAllAmmoNum:%s %d->%d (%d)", new Object[]{this.getName(), Integer.valueOf(debugBefore), Integer.valueOf(this.numRestAllAmmo), Integer.valueOf(n)});
   }

   public void supplyRestAllAmmo() {
      int m = this.getInfo().maxAmmo;
      if(this.getRestAllAmmoNum() + this.getAmmoNum() < m) {
         this.setRestAllAmmoNum(this.getRestAllAmmoNum() + this.getAmmoNum() + this.getInfo().suppliedNum);
      }

   }

   public boolean isInPreparation() {
      return this.countWait < 0 || this.countReloadWait > 0;
   }

   public String getName() {
      MCH_WeaponBase w = this.getCurrentWeapon();
      return w != null?w.getName():"";
   }

   public boolean canUse() {
      return this.countWait == 0;
   }

   public boolean isLongDelayWeapon() {
      return this.getInfo().delay > 4;
   }

   public void reload() {
      MCH_WeaponBase crtWpn = this.getCurrentWeapon();
      if(this.getAmmoNumMax() > 0 && this.getAmmoNum() < this.getAmmoNumMax() && crtWpn.getReloadCount() > 0) {
         this.countReloadWait = crtWpn.getReloadCount();
         if(crtWpn.worldObj.isRemote) {
            this.setAmmoNum(0);
         }

         if(!crtWpn.worldObj.isRemote) {
            this.countReloadWait -= 20;
            if(this.countReloadWait <= 0) {
               this.countReloadWait = 1;
            }
         }
      }

   }

   public void reloadMag() {
      int restAmmo = this.getRestAllAmmoNum();
      int nAmmo = this.getAmmoNumMax() - this.getAmmoNum();
      if(nAmmo > 0) {
         if(nAmmo > restAmmo) {
            nAmmo = restAmmo;
         }

         this.setAmmoNum(this.getAmmoNum() + nAmmo);
         this.setRestAllAmmoNum(this.getRestAllAmmoNum() - nAmmo);
      }

   }

   public void switchMode() {
      boolean isChanged = false;
      Object[] cntSwitch = (Object[]) this.weapons.toArray();
      int len$ = cntSwitch.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_WeaponBase w = (MCH_WeaponBase) cntSwitch[i$];
         if(w != null) {
            isChanged = w.switchMode() || isChanged;
         }
      }

      if(isChanged) {
         byte var6 = 15;
         if(this.countWait >= -var6) {
            if(this.countWait > var6) {
               this.countWait = -this.countWait;
            } else {
               this.countWait = -var6;
            }
         }

         if(this.getCurrentWeapon().worldObj.isRemote) {
            W_McClient.DEF_playSoundFX("random.click", 1.0F, 1.0F);
         }
      }

   }

   public void onSwitchWeapon(boolean isRemote, boolean isCreative) {
      int cntSwitch = 15;
      if(isRemote) {
         cntSwitch += 10;
      }

      if(this.countWait >= -cntSwitch) {
         if(this.countWait > cntSwitch) {
            this.countWait = -this.countWait;
         } else {
            this.countWait = -cntSwitch;
         }
      }

      this.currentWeaponIndex = 0;
      if(isCreative) {
         this.setAmmoNum(this.getAmmoNumMax());
      }

   }

   public boolean isUsed(int index) {
      MCH_WeaponBase w = this.getFirstWeapon();
      if(w != null && index < this.lastUsedCount.length) {
         int cnt = this.lastUsedCount[index];
         return w.interval >= 4 && cnt > w.interval / 2 || cnt >= 4;
      } else {
         return false;
      }
   }

   public void update(Entity shooter, boolean isSelected, boolean isUsed) {
      if(this.getCurrentWeapon().getInfo() != null) {
         if(this.countReloadWait > 0) {
            --this.countReloadWait;
            if(this.countReloadWait == 0) {
               this.reloadMag();
            }
         }

         for(int arr$ = 0; arr$ < this.lastUsedCount.length; ++arr$) {
            if(this.lastUsedCount[arr$] > 0) {
               if(this.lastUsedCount[arr$] == 4) {
                  if(0 == this.getCurrentWeaponIndex() && this.canUse() && (this.getAmmoNum() > 0 || this.getAllAmmoNum() <= 0)) {
                     --this.lastUsedCount[arr$];
                  }
               } else {
                  --this.lastUsedCount[arr$];
               }
            }
         }

         if(this.currentHeat > 0) {
            if(this.currentHeat < this.getCurrentWeapon().getInfo().maxHeatCount) {
               ++this.cooldownSpeed;
            }

            this.currentHeat -= this.cooldownSpeed / 20 + 1;
            if(this.currentHeat < 0) {
               this.currentHeat = 0;
            }
         }

         if(this.countWait > 0) {
            --this.countWait;
         }

         if(this.countWait < 0) {
            ++this.countWait;
         }

         this.prevRotationYaw = this.rotationYaw;
         this.prevRotationPitch = this.rotationPitch;
         if(this.weapons != null && this.weapons.size() > 0) {
           // MCH_WeaponBase[] var8 = (MCH_WeaponBase[]) this.weapons.toArray();
            //int len$ = var8.length;

            //for(int i$ = 0; i$ < len$; ++i$) {
               //MCH_WeaponBase w = var8[i$];
        	 for(MCH_WeaponBase w : this.weapons) {
               if(w != null) {
                  w.update(this.countWait);
               }
            }
         }

         if(this.soundWait > 0) {
            --this.soundWait;
         }

         if(isUsed && this.rotBarrelSpd < 75.0F) {
            this.rotBarrelSpd += (float)(25 + rand.nextInt(3));
            if(this.rotBarrelSpd > 74.0F) {
               this.rotBarrelSpd = 74.0F;
            }
         }

         this.prevRotBarrel = this.rotBarrel;
         this.rotBarrel += this.rotBarrelSpd;
         if(this.rotBarrel >= 360.0F) {
            this.rotBarrel -= 360.0F;
            this.prevRotBarrel -= 360.0F;
         }

         if(this.rotBarrelSpd > 0.0F) {
            --this.rotBarrelSpd;
            if(this.rotBarrelSpd < 0.0F) {
               this.rotBarrelSpd = 0.0F;
            }
         }

      }
   }

   public void updateWeapon(Entity shooter, boolean isUsed, int index) {
      MCH_WeaponBase crtWpn = this.getWeapon(index);
      float rb;
      if(isUsed && shooter.worldObj.isRemote && crtWpn != null && crtWpn.cartridge != null) {
         Vec3 r = crtWpn.getShotPos(shooter);
         rb = shooter.rotationYaw;
         float pitch = shooter.rotationPitch;
         if(shooter instanceof MCH_EntityVehicle && shooter.riddenByEntity != null) {
            ;
         }

         MCH_EntityCartridge.spawnCartridge(shooter.worldObj, crtWpn.cartridge, shooter.posX + r.xCoord, shooter.posY + r.yCoord, shooter.posZ + r.zCoord, shooter.motionX, shooter.motionY, shooter.motionZ, rb + this.rotationYaw, pitch + this.rotationPitch);
      }

      if(index < this.recoilBuf.length) {
         MCH_WeaponSet.Recoil var8 = this.recoilBuf[index];
         var8.prevRecoilBuf = var8.recoilBuf;
         if(isUsed && var8.recoilBufCount <= 0) {
            var8.recoilBufCount = var8.recoilBufCountMax;
         }

         if(var8.recoilBufCount > 0) {
            if(var8.recoilBufCountMax <= 1) {
               var8.recoilBuf = 1.0F;
            } else if(var8.recoilBufCountMax == 2) {
               var8.recoilBuf = var8.recoilBufCount == 2?1.0F:0.6F;
            } else {
               if(var8.recoilBufCount > var8.recoilBufCountMax / 2) {
                  var8.recoilBufCount -= var8.recoilBufCountSpeed;
               }

               rb = (float)var8.recoilBufCount / (float)var8.recoilBufCountMax;
               var8.recoilBuf = MathHelper.sin(rb * 3.1415927F);
            }

            --var8.recoilBufCount;
         } else {
            var8.recoilBuf = 0.0F;
         }
      }

   }

   

   
   public boolean use(MCH_WeaponParam prm) {
      MCH_WeaponBase crtWpn = this.getCurrentWeapon();
      if(crtWpn != null && crtWpn.getInfo() != null) {
         MCH_WeaponInfo info = crtWpn.getInfo();
         if((this.getAmmoNumMax() <= 0 || this.getAmmoNum() > 0) && (info.maxHeatCount <= 0 || this.currentHeat < info.maxHeatCount)) {
            crtWpn.canPlaySound = this.soundWait == 0;
            prm.rotYaw = prm.entity != null?prm.entity.rotationYaw:0.0F;
            prm.rotPitch = prm.entity != null?prm.entity.rotationPitch:0.0F;
            prm.rotYaw += this.rotationYaw + crtWpn.fixRotationYaw;
            prm.rotPitch += this.rotationPitch + crtWpn.fixRotationPitch;
            if(info.accuracy > 0.0F) {
            	double accuracy = info.accuracy;
               if(prm.entity.motionX > 0 || prm.entity.motionZ > 0) {
            	   if(info.stabilizer != 0) {
            		   accuracy *= (100 / info.stabilizer);
            	   }else {
            		   accuracy *= 100;
            	   }
               }
               prm.rotYaw += (rand.nextFloat() - 0.5F) * accuracy;
               prm.rotPitch += (rand.nextFloat() - 0.5F) * accuracy;
            }
            

            prm.rotYaw = MathHelper.wrapAngleTo180_float(prm.rotYaw);
            prm.rotPitch = MathHelper.wrapAngleTo180_float(prm.rotPitch);
            if(crtWpn.use(prm)) {
               if(info.maxHeatCount > 0) {
                  this.cooldownSpeed = 1;
                  this.currentHeat += crtWpn.heatCount;
                  if(this.currentHeat >= info.maxHeatCount) {
                     this.currentHeat += 30;
                  }
               }

               if(info.soundDelay > 0 && this.soundWait == 0) {
                  this.soundWait = info.soundDelay;
               }

               this.lastUsedOptionParameter1 = crtWpn.optionParameter1;
               this.lastUsedOptionParameter2 = crtWpn.optionParameter2;
               this.lastUsedCount[this.currentWeaponIndex] = crtWpn.interval > 0?crtWpn.interval:-crtWpn.interval;
               if(crtWpn.isCooldownCountReloadTime() && crtWpn.getReloadCount() - 10 > this.lastUsedCount[this.currentWeaponIndex]) {
                  this.lastUsedCount[this.currentWeaponIndex] = crtWpn.getReloadCount() - 10;
               }

               this.currentWeaponIndex = (this.currentWeaponIndex + 1) % this.weapons.size();
               this.countWait = crtWpn.interval;
               this.countReloadWait = 0;
               if(this.getAmmoNum() > 0) {
                  this.setAmmoNum(this.getAmmoNum() - 1);
               }

               if(this.getAmmoNum() <= 0) {
                  if(prm.isInfinity && this.getRestAllAmmoNum() < this.getAmmoNumMax()) {
                     this.setRestAllAmmoNum(this.getAmmoNumMax());
                  }

                  this.reload();
                  prm.reload = true;
               }

               prm.result = true;
            }
         }
      }

      return prm.result;
   }

   public void waitAndReloadByOther(boolean reload) {
      MCH_WeaponBase crtWpn = this.getCurrentWeapon();
      if(crtWpn != null && crtWpn.getInfo() != null) {
         this.countWait = crtWpn.interval;
         this.countReloadWait = 0;
         if(reload && this.getAmmoNumMax() > 0 && crtWpn.getReloadCount() > 0) {
            this.countReloadWait = crtWpn.getReloadCount();
            if(!crtWpn.worldObj.isRemote) {
               this.countReloadWait -= 20;
               if(this.countReloadWait <= 0) {
                  this.countReloadWait = 1;
               }
            }
         }
      }

   }

   public int getLastUsedOptionParameter1() {
      return this.lastUsedOptionParameter1;
   }

   public int getLastUsedOptionParameter2() {
      return this.lastUsedOptionParameter2;
   }

   public MCH_WeaponBase getFirstWeapon() {
      return this.getWeapon(0);
   }

   public int getCurrentWeaponIndex() {
      return this.currentWeaponIndex;
   }

   public MCH_WeaponBase getCurrentWeapon() {
      return this.getWeapon(this.currentWeaponIndex);
   }

   public MCH_WeaponBase getWeapon(int idx) {
      return this.weapons != null && this.weapons.size() > 0 && idx < this.weapons.size()?this.weapons.get(idx):null;
   }

   public int getWeaponNum() {
      return this.weapons != null?this.weapons.size():0;
   }

   public MCH_WeaponInfo getInfo() {
      return this.getFirstWeapon().getInfo();
   }

   public double getLandInDistance(MCH_WeaponParam prm) {
      double ret = -1.0D;
      MCH_WeaponBase crtWpn = this.getCurrentWeapon();
      if(crtWpn != null && crtWpn.getInfo() != null) {
         MCH_WeaponInfo info = crtWpn.getInfo();
         prm.rotYaw = prm.entity != null?prm.entity.rotationYaw:0.0F;
         prm.rotPitch = prm.entity != null?prm.entity.rotationPitch:0.0F;
         prm.rotYaw += this.rotationYaw + crtWpn.fixRotationYaw;
         prm.rotPitch += this.rotationPitch + crtWpn.fixRotationPitch;
         prm.rotYaw = MathHelper.wrapAngleTo180_float(prm.rotYaw);
         prm.rotPitch = MathHelper.wrapAngleTo180_float(prm.rotPitch);
         return crtWpn.getLandInDistance(prm);
      } else {
         return ret;
      }
   }


   public class Recoil {

      public int recoilBufCount;
      public final int recoilBufCountMax;
      public final int recoilBufCountSpeed;
      public float recoilBuf;
      public float prevRecoilBuf;


      public Recoil(int max, int spd) {
         this.recoilBufCountMax = max;
         this.recoilBufCountSpeed = spd;
      }
   }
}
