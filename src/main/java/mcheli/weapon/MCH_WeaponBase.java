package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Random;

public abstract class MCH_WeaponBase {

   protected static final Random rand = new Random();
   public final World worldObj;
   public final Vec3 position;
   public final float fixRotationYaw;
   public final float fixRotationPitch;
   public final String name;
   public final MCH_WeaponInfo weaponInfo;
   public int chemYield;
   public String displayName;
   public int power;
   public float acceleration;
   public int explosionPower;
   public int explosionPowerInWater;
   public int nukeYield;
   public int interval;
   public int numMode;
   public int lockTime;
   public int piercing;
   public int heatCount;
   public MCH_Cartridge cartridge;
   public boolean onTurret;
   public MCH_EntityAircraft aircraft;
   public int tick;
   public int optionParameter1;
   public int optionParameter2;
   private int currentMode;
   public boolean canPlaySound;


   public MCH_WeaponBase(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      this.worldObj = w;
      this.position = v;
      this.fixRotationYaw = yaw;
      this.fixRotationPitch = pitch;
      this.name = nm;
      this.weaponInfo = wi;
      this.displayName = wi != null?wi.displayName:"";
      this.power = 0;
      this.acceleration = 0.0F;
      this.explosionPower = 0;
      this.explosionPowerInWater = 0;
      this.nukeYield = wi.nukeYield;
      this.chemYield = wi.chemYield;
      this.interval = 1;
      this.numMode = 0;
      this.lockTime = 0;
      this.heatCount = 0;
      this.cartridge = null;
      this.tick = 0;
      this.optionParameter1 = 0;
      this.optionParameter2 = 0;
      this.setCurrentMode(0);
      this.canPlaySound = true;
   }

   public MCH_WeaponInfo getInfo() {
      return this.weaponInfo;
   }

   public String getName() {
      return this.displayName;
   }
   
   public MCH_WeaponParam setAccuracy(MCH_WeaponParam prm) {
	   if(weaponInfo.accuracy > 0.0F) {
       	double accuracy = weaponInfo.accuracy;
          if(prm.entity.motionX > 0 || prm.entity.motionZ > 0) {
       	   if(weaponInfo.stabilizer != 0) {
       		   accuracy *= (100 / weaponInfo.stabilizer);
       	   }else {
       		   accuracy *= 100;
       	   }
          }
          prm.rotYaw += (rand.nextFloat() - 0.5F) * accuracy;
          prm.rotPitch += (rand.nextFloat() - 0.5F) * accuracy;
       }
	   return prm;
   }

   public abstract boolean shot(MCH_WeaponParam var1);

   public void setLockChecker(MCH_IEntityLockChecker checker) {}

   public void setLockCountMax(int n) {}

   public int getLockCount() {
      return 0;
   }

   public int getLockCountMax() {
      return 0;
   }

   public final int getNumAmmoMax() {
      return this.getInfo().round;
   }

   public int getCurrentMode() {
      return this.getInfo() != null && this.getInfo().fixMode > 0?this.getInfo().fixMode:this.currentMode;
   }

   public void setCurrentMode(int currentMode) {
      this.currentMode = currentMode;
   }

   public final int getAllAmmoNum() {
      return this.getInfo().maxAmmo;
   }

   public final int getReloadCount() {
      return this.getInfo().reloadTime;
   }

   public final MCH_SightType getSightType() {
      return this.getInfo().sight;
   }

   public MCH_GuidanceSystem getGuidanceSystem() {
      return null;
   }

   public void update(int countWait) {
      if(countWait != 0) {
         ++this.tick;
      }

   }

   public boolean isCooldownCountReloadTime() {
      return false;
   }

   public void modifyCommonParameters() {
      this.modifyParameters();
      
   }

   public void modifyParameters() {}

   public boolean switchMode() {
      if(this.getInfo() != null && this.getInfo().fixMode > 0) {
         return false;
      } else {
         int beforeMode = this.getCurrentMode();
         if(this.numMode > 0) {
            this.setCurrentMode((this.getCurrentMode() + 1) % this.numMode);
         } else {
            this.setCurrentMode(0);
         }

         if(beforeMode != this.getCurrentMode()) {
            this.onSwitchMode();
         }

         return beforeMode != this.getCurrentMode();
      }
   }

   public void onSwitchMode() {}

   public boolean use(MCH_WeaponParam prm) {
      Vec3 v = this.getShotPos(prm.entity);
      prm.posX += v.xCoord;
      prm.posY += v.yCoord;
      prm.posZ += v.zCoord;
      
      if(this.shot(prm)) {
         this.tick = 0;
         return true;
      } else {
         return false;
      }
   }

   public Vec3 getShotPos(Entity entity) {
      if(entity instanceof MCH_EntityAircraft && this.onTurret) {
    	 // System.out.println("On turret");
         return ((MCH_EntityAircraft)entity).calcOnTurretPos(this.position);
      } else {
    	 // System.out.println("NOT on turret");
         Vec3 v = Vec3.createVectorHelper(this.position.xCoord, this.position.yCoord, this.position.zCoord);
         float roll = entity instanceof MCH_EntityAircraft?((MCH_EntityAircraft)entity).getRotRoll():0.0F;
         return MCH_Lib.RotVec3(v, -entity.rotationYaw, -entity.rotationPitch, -roll);
      }
   }

   public void playSound(Entity e) {
      this.playSound(e, this.getInfo().soundFileName);
   }

   public void playSound(Entity e, String snd) {
      if(!e.worldObj.isRemote && this.canPlaySound && this.getInfo() != null) {
         float prnd = this.getInfo().soundPitchRandom;
         W_WorldFunc.MOD_playSoundEffect(this.worldObj, e.posX, e.posY, e.posZ, snd, this.getInfo().soundVolume, this.getInfo().soundPitch * (1.0F - prnd) + rand.nextFloat() * prnd);
      }

   }

   public void playSoundClient(Entity e, float volume, float pitch) {
      if(e.worldObj.isRemote && this.getInfo() != null) {
         W_McClient.MOD_playSoundFX(this.getInfo().soundFileName, volume, pitch);
      }

   }

   public double getLandInDistance(MCH_WeaponParam prm) {
      if(this.weaponInfo == null) {
         return -1.0D;
      } else if(this.weaponInfo.gravity >= 0.0F) {
         return -1.0D;
      } else {
         Vec3 v = MCH_Lib.RotVec3(0.0D, 0.0D, 1.0D, -prm.rotYaw, -prm.rotPitch, -prm.rotRoll);
         double s = Math.sqrt(v.xCoord * v.xCoord + v.yCoord * v.yCoord + v.zCoord * v.zCoord);
         double acc = this.acceleration < 4.0F?(double)this.acceleration:4.0D;
         double accFac = (double)this.acceleration / acc;
         double my = v.yCoord * (double)this.acceleration / s;
         if(my <= 0.0D) {
            return -1.0D;
         } else {
            double mx = v.xCoord * (double)this.acceleration / s;
            double mz = v.zCoord * (double)this.acceleration / s;
            double ls = my / (double)this.weaponInfo.gravity;
            double gravity = (double)this.weaponInfo.gravity * accFac;
            double spx;
            if(ls < -12.0D) {
               spx = ls / -12.0D;
               mx *= spx;
               my *= spx;
               mz *= spx;
               gravity *= spx * spx * 0.95D;
            }

            spx = prm.posX;
            double spy = prm.posY + 3.0D;
            double spz = prm.posZ;
            Vec3 vs = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
            Vec3 ve = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);

            for(int i = 0; i < 50; ++i) {
               vs.xCoord = spx;
               vs.yCoord = spy;
               vs.zCoord = spz;
               ve.xCoord = spx + mx;
               ve.yCoord = spy + my;
               ve.zCoord = spz + mz;
               MovingObjectPosition mop = this.worldObj.rayTraceBlocks(vs, ve);
               double dx;
               double dz;
               if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
                  dx = (double)mop.blockX - prm.posX;
                  dz = (double)mop.blockZ - prm.posZ;
                  return Math.sqrt(dx * dx + dz * dz);
               }

               my += gravity;
               spx += mx;
               spy += my;
               spz += mz;
               if(spy < prm.posY) {
                  dx = spx - prm.posX;
                  dz = spz - prm.posZ;
                  return Math.sqrt(dx * dx + dz * dz);
               }
            }

            return -1.0D;
         }
      }
   }

}
