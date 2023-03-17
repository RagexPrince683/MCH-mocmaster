package mcheli.particles;

import mcheli.wrapper.W_EntityFX;
import net.minecraft.world.World;

public abstract class MCH_EntityParticleBase extends W_EntityFX {

   public boolean isEffectedWind;
   public boolean diffusible;
   public boolean toWhite;
   public float particleMaxScale;
   public float gravity;
   public float moutionYUpAge;


   public MCH_EntityParticleBase(World par1World, double x, double y, double z, double mx, double my, double mz) {
      super(par1World, x, y, z, mx, my, mz);
      super.motionX = mx;
      super.motionY = my;
      super.motionZ = mz;
      this.isEffectedWind = false;
      this.particleMaxScale = super.particleScale;
   }

   public MCH_EntityParticleBase setParticleScale(float scale) {
      super.particleScale = scale;
      return this;
   }

   public void setParticleMaxAge(int age) {
      super.particleMaxAge = age;
   }

   public void setParticleTextureIndex(int par1) {
      super.particleTextureIndexX = par1 % 8;
      super.particleTextureIndexY = par1 / 8;
   }

   public int getFXLayer() {
      return 2;
   }
   /*
   public void moveEntity(double par1, double par3, double par5) {
      if(super.noClip) {
         super.boundingBox.offset(par1, par3, par5);
         super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
         super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
         super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
      } else {
         super.worldObj.theProfiler.startSection("move");
         super.ySize *= 0.4F;
         double d6 = par1;
         double d7 = par3;
         double d8 = par5;
         List list = super.worldObj.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(par1, par3, par5));

         for(int flag1 = 0; flag1 < list.size(); ++flag1) {
            if (list.get(flag1) != null){
               par3 = ((AxisAlignedBB)list.get(flag1)).calculateYOffset(super.boundingBox, par3);
            }
         }

         super.boundingBox.offset(0.0D, par3, 0.0D);
         if(!super.field_70135_K && d7 != par3) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         int j;
         for(j = 0; j < list.size(); ++j) {
            if (list.get(j) != null){
               par1 = ((AxisAlignedBB)list.get(j)).calculateXOffset(super.boundingBox, par1);
            }

         }

         super.boundingBox.offset(par1, 0.0D, 0.0D);
         if(!super.field_70135_K && d6 != par1) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         for(j = 0; j < list.size(); ++j) {
            if (list.get(j) != null){
               par5 = ((AxisAlignedBB)list.get(j)).calculateZOffset(super.boundingBox, par5);
            }

         }

         super.boundingBox.offset(0.0D, 0.0D, par5);
         if(!super.field_70135_K && d8 != par5) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         super.worldObj.theProfiler.endSection();
         super.worldObj.theProfiler.startSection("rest");
         super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
         super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
         super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
         super.isCollidedHorizontally = d6 != par1 || d8 != par5;
         super.isCollidedVertically = d7 != par3;
         super.onGround = d7 != par3 && d7 < 0.0D;
         super.isCollided = super.isCollidedHorizontally || super.isCollidedVertically;
         this.updateFallState(par3, super.onGround);
         if(d6 != par1) {
            super.motionX = 0.0D;
         }

         if(d7 != par3) {
            super.motionY = 0.0D;
         }

         if(d8 != par5) {
            super.motionZ = 0.0D;
         }
         try {
            this.doBlockCollisions();
         } catch (Throwable var34) {
            CrashReport crashreport = CrashReport.makeCrashReport(var34, "Checking entity block collision");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
         }

         super.worldObj.theProfiler.endSection();
      }

   }
   */


}
