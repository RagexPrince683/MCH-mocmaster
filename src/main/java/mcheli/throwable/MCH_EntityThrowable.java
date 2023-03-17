package mcheli.throwable;

import mcheli.MCH_Explosion;
import mcheli.MCH_Lib;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class MCH_EntityThrowable extends EntityThrowable {

   private static final int DATAID_NAME = 31;
   private int countOnUpdate;
   private MCH_ThrowableInfo throwableInfo;
   public double boundPosX;
   public double boundPosY;
   public double boundPosZ;
   public MovingObjectPosition lastOnImpact;
   public int noInfoCount;


   public MCH_EntityThrowable(World par1World) {
      super(par1World);
      this.init();
   }

   public MCH_EntityThrowable(World par1World, EntityLivingBase par2EntityLivingBase, float acceleration) {
      super(par1World, par2EntityLivingBase);
      super.motionX *= (double)acceleration;
      super.motionY *= (double)acceleration;
      super.motionZ *= (double)acceleration;
      this.init();
   }

   public MCH_EntityThrowable(World par1World, double par2, double par4, double par6) {
      super(par1World, par2, par4, par6);
      this.init();
   }

   public MCH_EntityThrowable(World p_i1777_1_, double x, double y, double z, float yaw, float pitch) {
      this(p_i1777_1_);
      this.setSize(0.25F, 0.25F);
      this.setLocationAndAngles(x, y, z, yaw, pitch);
      super.posX -= (double)(MathHelper.cos(super.rotationYaw / 180.0F * 3.1415927F) * 0.16F);
      super.posY -= 0.10000000149011612D;
      super.posZ -= (double)(MathHelper.sin(super.rotationYaw / 180.0F * 3.1415927F) * 0.16F);
      this.setPosition(super.posX, super.posY, super.posZ);
      super.yOffset = 0.0F;
      float f = 0.4F;
      super.motionX = (double)(-MathHelper.sin(super.rotationYaw / 180.0F * 3.1415927F) * MathHelper.cos(super.rotationPitch / 180.0F * 3.1415927F) * f);
      super.motionZ = (double)(MathHelper.cos(super.rotationYaw / 180.0F * 3.1415927F) * MathHelper.cos(super.rotationPitch / 180.0F * 3.1415927F) * f);
      super.motionY = (double)(-MathHelper.sin((super.rotationPitch + this.func_70183_g()) / 180.0F * 3.1415927F) * f);
      this.setThrowableHeading(super.motionX, super.motionY, super.motionZ, this.func_70182_d(), 1.0F);
   }

   public void init() {
      this.lastOnImpact = null;
      this.countOnUpdate = 0;
      this.setInfo((MCH_ThrowableInfo)null);
      this.noInfoCount = 0;
      this.getDataWatcher().addObject(31, new String(""));
   }

   public void setDead() {
      String s = this.getInfo() != null?this.getInfo().name:"null";
      MCH_Lib.DbgLog(super.worldObj, "MCH_EntityThrowable.setDead(%s)", new Object[]{s});
      super.setDead();
   }

   public void onUpdate() {
      this.boundPosX = super.posX;
      this.boundPosY = super.posY;
      this.boundPosZ = super.posZ;
      if(this.getInfo() != null) {
         Block i = W_WorldFunc.getBlock(super.worldObj, (int)(super.posX + 0.5D), (int)super.posY, (int)(super.posZ + 0.5D));
         Material y = W_WorldFunc.getBlockMaterial(super.worldObj, (int)(super.posX + 0.5D), (int)super.posY, (int)(super.posZ + 0.5D));
         if(i != null && y == Material.water) {
            super.motionY += (double)this.getInfo().gravityInWater;
         } else {
            super.motionY += (double)this.getInfo().gravity;
         }
      }

      super.onUpdate();
      if(this.lastOnImpact != null) {
         this.boundBullet(this.lastOnImpact);
         this.setPosition(this.boundPosX + super.motionX, this.boundPosY + super.motionY, this.boundPosZ + super.motionZ);
         this.lastOnImpact = null;
      }

      ++this.countOnUpdate;
      if(this.countOnUpdate >= 2147483632) {
         this.setDead();
      } else {
         if(this.getInfo() == null) {
            String var6 = this.getDataWatcher().getWatchableObjectString(31);
            if(!var6.isEmpty()) {
               this.setInfo(MCH_ThrowableInfoManager.get(var6));
            }

            if(this.getInfo() == null) {
               ++this.noInfoCount;
               if(this.noInfoCount > 10) {
                  this.setDead();
               }

               return;
            }
         }

         if(!super.isDead) {
            if(!super.worldObj.isRemote) {
               if(this.countOnUpdate == this.getInfo().timeFuse && this.getInfo().explosion > 0) {
                  MCH_Explosion.newExplosion(super.worldObj, (Entity)null, (Entity)null, super.posX, super.posY, super.posZ, (float)this.getInfo().explosion, (float)this.getInfo().explosion, true, true, false, true, 0);
                  this.setDead();
                  return;
               }

               if(this.countOnUpdate >= this.getInfo().aliveTime) {
                  this.setDead();
                  return;
               }
            } else if(this.countOnUpdate >= this.getInfo().timeFuse && this.getInfo().explosion <= 0) {
               for(int var7 = 0; var7 < this.getInfo().smokeNum; ++var7) {
                  float var8 = this.getInfo().smokeVelocityVertical >= 0.0F?0.2F:-0.2F;
                  float r = this.getInfo().smokeColor.r * 0.9F + super.rand.nextFloat() * 0.1F;
                  float g = this.getInfo().smokeColor.g * 0.9F + super.rand.nextFloat() * 0.1F;
                  float b = this.getInfo().smokeColor.b * 0.9F + super.rand.nextFloat() * 0.1F;
                  if(this.getInfo().smokeColor.r == this.getInfo().smokeColor.g) {
                     g = r;
                  }

                  if(this.getInfo().smokeColor.r == this.getInfo().smokeColor.b) {
                     b = r;
                  }

                  if(this.getInfo().smokeColor.g == this.getInfo().smokeColor.b) {
                     b = g;
                  }

                  this.spawnParticle("explode", 4, this.getInfo().smokeSize + super.rand.nextFloat() * this.getInfo().smokeSize / 3.0F, r, g, b, this.getInfo().smokeVelocityHorizontal * (super.rand.nextFloat() - 0.5F), this.getInfo().smokeVelocityVertical * super.rand.nextFloat(), this.getInfo().smokeVelocityHorizontal * (super.rand.nextFloat() - 0.5F));
               }
            }

         }
      }
   }

   public void spawnParticle(String name, int num, float size, float r, float g, float b, float mx, float my, float mz) {
      if(super.worldObj.isRemote) {
         if(name.isEmpty() || num < 1) {
            return;
         }

         double x = (super.posX - super.prevPosX) / (double)num;
         double y = (super.posY - super.prevPosY) / (double)num;
         double z = (super.posZ - super.prevPosZ) / (double)num;

         for(int i = 0; i < num; ++i) {
            MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.prevPosX + x * (double)i, 1.0D + super.prevPosY + y * (double)i, super.prevPosZ + z * (double)i);
            prm.setMotion((double)mx, (double)my, (double)mz);
            prm.size = size;
            prm.setColor(1.0F, r, g, b);
            prm.isEffectWind = true;
            prm.toWhite = true;
            MCH_ParticlesUtil.spawnParticle(prm);
         }
      }

   }

   protected float getGravityVelocity() {
      return 0.0F;
   }

   public void boundBullet(MovingObjectPosition m) {
      float bound = this.getInfo().bound;
      switch(m.sideHit) {
      case 0:
      case 1:
         super.motionX *= 0.8999999761581421D;
         super.motionZ *= 0.8999999761581421D;
         this.boundPosY = m.hitVec.yCoord;
         if((m.sideHit != 0 || super.motionY <= 0.0D) && (m.sideHit != 1 || super.motionY >= 0.0D)) {
            super.motionY = 0.0D;
         } else {
            super.motionY = -super.motionY * (double)bound;
         }
         break;
      case 2:
         if(super.motionZ > 0.0D) {
            super.motionZ = -super.motionZ * (double)bound;
         }
         break;
      case 3:
         if(super.motionZ < 0.0D) {
            super.motionZ = -super.motionZ * (double)bound;
         }
         break;
      case 4:
         if(super.motionX > 0.0D) {
            super.motionX = -super.motionX * (double)bound;
         }
         break;
      case 5:
         if(super.motionX < 0.0D) {
            super.motionX = -super.motionX * (double)bound;
         }
      }

   }

   protected void onImpact(MovingObjectPosition m) {
      if(this.getInfo() != null) {
         this.lastOnImpact = m;
      }

   }

   public MCH_ThrowableInfo getInfo() {
      return this.throwableInfo;
   }

   public void setInfo(MCH_ThrowableInfo info) {
      this.throwableInfo = info;
      if(info != null && !super.worldObj.isRemote) {
         this.getDataWatcher().updateObject(31, new String(info.name));
      }

   }
}
