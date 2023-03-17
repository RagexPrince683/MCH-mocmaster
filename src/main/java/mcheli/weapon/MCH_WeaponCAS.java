package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_MovingObjectPosition;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_WeaponCAS extends MCH_WeaponBase {

   private double targetPosX;
   private double targetPosY;
   private double targetPosZ;
   public int direction;
   private int startTick;
   private int cntAtk;
   private Entity shooter;
   public Entity user;


   public MCH_WeaponCAS(World w, Vec3 v, float yaw, float pitch, String nm, MCH_WeaponInfo wi) {
      super(w, v, yaw, pitch, nm, wi);
      super.acceleration = 4.0F;
      super.explosionPower = 2;
      super.power = 32;
      super.interval = -300;
      if(w.isRemote) {
         super.interval -= 10;
      }

      this.targetPosX = 0.0D;
      this.targetPosY = 0.0D;
      this.targetPosZ = 0.0D;
      this.direction = 0;
      this.startTick = 0;
      this.cntAtk = 3;
      this.shooter = null;
      this.user = null;
   }

   public void update(int countWait) {
      super.update(countWait);
      if(!super.worldObj.isRemote && this.cntAtk < 3 && countWait != 0 && super.tick == this.startTick) {
         double x = 0.0D;
         double z = 0.0D;
         if(this.cntAtk >= 1) {
            double sign = this.cntAtk == 1?1.0D:-1.0D;
            if(this.direction == 0 || this.direction == 2) {
               x = MCH_WeaponBase.rand.nextDouble() * 10.0D * sign;
               z = (MCH_WeaponBase.rand.nextDouble() - 0.5D) * 10.0D;
            }

            if(this.direction == 1 || this.direction == 3) {
               z = MCH_WeaponBase.rand.nextDouble() * 10.0D * sign;
               x = (MCH_WeaponBase.rand.nextDouble() - 0.5D) * 10.0D;
            }
         }

         this.spawnA10(this.targetPosX + x, this.targetPosY + 20.0D, this.targetPosZ + z);
         this.startTick = super.tick + 45;
         ++this.cntAtk;
      }

   }

   public void modifyParameters() {
      if(super.interval > -250) {
         super.interval = -250;
      }

   }

   public void setTargetPosition(double x, double y, double z) {
      this.targetPosX = x;
      this.targetPosY = y;
      this.targetPosZ = z;
   }

   public void spawnA10(double x, double y, double z) {
      double mX = 0.0D;
      double mY = 0.0D;
      double mZ = 0.0D;
      boolean SPEED = true;
      if(this.direction == 0) {
         mZ += 3.0D;
         z -= 90.0D;
      }

      if(this.direction == 1) {
         mX -= 3.0D;
         x += 90.0D;
      }

      if(this.direction == 2) {
         mZ -= 3.0D;
         z += 90.0D;
      }

      if(this.direction == 3) {
         mX += 3.0D;
         x -= 90.0D;
      }

      MCH_EntityA10 a10 = new MCH_EntityA10(super.worldObj, x, y, z);
      a10.setWeaponName(super.name);
      a10.prevRotationYaw = a10.rotationYaw = (float)(90 * this.direction);
      a10.motionX = mX;
      a10.motionY = mY;
      a10.motionZ = mZ;
      a10.direction = this.direction;
      a10.shootingEntity = this.user;
      a10.shootingAircraft = this.shooter;
      a10.explosionPower = super.explosionPower;
      a10.power = super.power;
      
      a10.acceleration = super.acceleration;
      super.worldObj.spawnEntityInWorld(a10);
      W_WorldFunc.MOD_playSoundEffect(super.worldObj, x, y, z, "a-10_snd", 150.0F, 1.0F);
   }

   public boolean shot(MCH_WeaponParam prm) {
      float yaw = prm.user.rotationYaw;
      float pitch = prm.user.rotationPitch;
      double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      if(super.worldObj.isRemote) {
         tX = tX * 80.0D / dist;
         tY = tY * 80.0D / dist;
         tZ = tZ * 80.0D / dist;
      } else {
         tX = tX * 150.0D / dist;
         tY = tY * 150.0D / dist;
         tZ = tZ * 150.0D / dist;
      }

      Vec3 src = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX, prm.entity.posY + 2.0D, prm.entity.posZ);
      Vec3 dst = W_WorldFunc.getWorldVec3(super.worldObj, prm.entity.posX + tX, prm.entity.posY + tY + 2.0D, prm.entity.posZ + tZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, src, dst);
      if(m != null && W_MovingObjectPosition.isHitTypeTile(m)) {
         this.targetPosX = m.hitVec.xCoord;
         this.targetPosY = m.hitVec.yCoord;
         this.targetPosZ = m.hitVec.zCoord;
         this.direction = (int)MCH_Lib.getRotate360((double)(yaw + 45.0F)) / 90;
         this.direction += MCH_WeaponBase.rand.nextBoolean()?-1:1;
         this.direction %= 4;
         if(this.direction < 0) {
            this.direction += 4;
         }

         this.user = prm.user;
         this.shooter = prm.entity;
         if(prm.entity != null) {
            this.playSoundClient(prm.entity, 1.0F, 1.0F);
         }

         this.startTick = 50;
         this.cntAtk = 0;
         return true;
      } else {
         return false;
      }
   }

   public boolean shot(Entity user, double px, double py, double pz, int option1, int option2) {
      float yaw = user.rotationYaw;
      float pitch = user.rotationPitch;
      double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
      double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
      double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
      if(super.worldObj.isRemote) {
         tX = tX * 80.0D / dist;
         tY = tY * 80.0D / dist;
         tZ = tZ * 80.0D / dist;
      } else {
         tX = tX * 120.0D / dist;
         tY = tY * 120.0D / dist;
         tZ = tZ * 120.0D / dist;
      }

      Vec3 src = W_WorldFunc.getWorldVec3(super.worldObj, px, py, pz);
      Vec3 dst = W_WorldFunc.getWorldVec3(super.worldObj, px + tX, py + tY, pz + tZ);
      MovingObjectPosition m = W_WorldFunc.clip(super.worldObj, src, dst);
      if(W_MovingObjectPosition.isHitTypeTile(m)) {
         if(super.worldObj.isRemote) {
            double dx = m.hitVec.xCoord - px;
            double var10000 = m.hitVec.yCoord - py;
            double dz = m.hitVec.zCoord - pz;
            if(Math.sqrt(dx * dx + dz * dz) < 20.0D) {
               return false;
            }
         }

         this.targetPosX = m.hitVec.xCoord;
         this.targetPosY = m.hitVec.yCoord;
         this.targetPosZ = m.hitVec.zCoord;
         this.direction = (int)MCH_Lib.getRotate360((double)(yaw + 45.0F)) / 90;
         this.direction += MCH_WeaponBase.rand.nextBoolean()?-1:1;
         this.direction %= 4;
         if(this.direction < 0) {
            this.direction += 4;
         }

         this.user = user;
         this.shooter = null;
         super.tick = 0;
         this.startTick = 50;
         this.cntAtk = 0;
         return true;
      } else {
         return false;
      }
   }
}
