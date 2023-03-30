package mcheli.particles;

import java.util.ArrayList;
import java.util.List;
import mcheli.wrapper.W_EntityFX;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
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

   public void moveEntity(double par1, double par3, double par5) {
      if(super.noClip) {
         super.boundingBox.offset(par1, par3, par5);
         super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
         super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
         super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
      } else {
         super.worldObj.theProfiler.startSection("move");
         super.ySize *= 0.4F;
         double d3 = super.posX;
         double d4 = super.posY;
         double d5 = super.posZ;
         double d6 = par1;
         double d7 = par3;
         double d8 = par5;
         AxisAlignedBB axisalignedbb = super.boundingBox.copy();
         boolean flag = false;
         List list = super.worldObj.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(par1, par3, par5));

         for(int flag1 = 0; flag1 < list.size(); ++flag1) {
            par3 = ((AxisAlignedBB)list.get(flag1)).calculateYOffset(super.boundingBox, par3);
         }

         super.boundingBox.offset(0.0D, par3, 0.0D);
         if(!super.field_70135_K && d7 != par3) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         boolean var10000;
         if(!super.onGround && (d7 == par3 || d7 >= 0.0D)) {
            var10000 = false;
         } else {
            var10000 = true;
         }

         int j;
         for(j = 0; j < list.size(); ++j) {
            par1 = ((AxisAlignedBB)list.get(j)).calculateXOffset(super.boundingBox, par1);
         }

         super.boundingBox.offset(par1, 0.0D, 0.0D);
         if(!super.field_70135_K && d6 != par1) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         for(j = 0; j < list.size(); ++j) {
            par5 = ((AxisAlignedBB)list.get(j)).calculateZOffset(super.boundingBox, par5);
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

         double var35 = super.posX - d3;
         var35 = super.posY - d4;
         var35 = super.posZ - d5;

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

   public List getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
      ArrayList collidingBoundingBoxes = new ArrayList();
      int i = MathHelper.floor_double(par2AxisAlignedBB.minX);
      int j = MathHelper.floor_double(par2AxisAlignedBB.maxX + 1.0D);
      int k = MathHelper.floor_double(par2AxisAlignedBB.minY);
      int l = MathHelper.floor_double(par2AxisAlignedBB.maxY + 1.0D);
      int i1 = MathHelper.floor_double(par2AxisAlignedBB.minZ);
      int j1 = MathHelper.floor_double(par2AxisAlignedBB.maxZ + 1.0D);

      for(int k1 = i; k1 < j; ++k1) {
         for(int l1 = i1; l1 < j1; ++l1) {
            if(super.worldObj.blockExists(k1, 64, l1)) {
               for(int i2 = k - 1; i2 < l; ++i2) {
                  Block block;
                  if(k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000) {
                     block = W_WorldFunc.getBlock(super.worldObj, k1, i2, l1);
                  } else {
                     block = Blocks.stone;
                  }

                  block.addCollisionBoxesToList(super.worldObj, k1, i2, l1, par2AxisAlignedBB, collidingBoundingBoxes, par1Entity);
               }
            }
         }
      }

      return collidingBoundingBoxes;
   }
}
