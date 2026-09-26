package mcheli.tank;

import java.util.ArrayList;
import java.util.List;

import mcheli.aircraft.MCH_BoundingBox;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntityHitBox;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class MCH_EntityWheel extends W_Entity {

   private MCH_EntityBaseVehicle parents;
   public Vec3 pos;
   boolean isPlus;


   public MCH_EntityWheel(World w) {
      super(w);
      this.setSize(1.0F, 1.0F);
      super.stepHeight = 1.5F;
      super.isImmuneToFire = true;
      this.isPlus = false;
   }

   public void setWheelPos(Vec3 pos, Vec3 weightedCenter) {
      this.pos = pos;
      this.isPlus = pos.zCoord >= weightedCenter.zCoord;
   }

   //public MCH_BoundingBox copy() {
   //   return new MCH_BoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor);
   //}

   public void travelToDimension(int p_71027_1_) {}

   public MCH_EntityBaseVehicle getParents() {
      return this.parents;
   }

   public void setParents(MCH_EntityBaseVehicle parents) {
      this.parents = parents;
   }

   protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
      this.setDead();
   }

   protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {}

   /** Actual support at the current wheel position, independent of paired onGround flags. */
   public boolean hasGroundContact() {
      if(this.isDead || this.worldObj == null || this.boundingBox == null) return false;
      double probe = -0.05D;
      List boxes = this.getCollidingBoundingBoxes(this, this.boundingBox.addCoord(0.0D, probe, 0.0D));
      return hasGroundSupport(this.boundingBox, boxes);
   }

   /** Grip-only support sweep. The invisible suspension box is not the rendered tire patch. */
   public boolean hasCarGroundContact() {
      if(this.isDead || this.worldObj == null || this.boundingBox == null || this.parents == null || this.pos == null) return false;
      double restGap = Math.max(0.0D, this.parents.yOffset + this.pos.yCoord - this.yOffset);
      AxisAlignedBB box = this.boundingBox.copy();
      // A suspension wheel can lag below the body during takeoff. Never use that stale support.
      double targetBottom = this.parents.getTransformedPosition(this.pos).yCoord - this.yOffset;
      box.offset(0.0D, Math.max(0.0D, targetBottom - box.minY), 0.0D);
      double reach = 0.05D + restGap;
      return hasGroundSupport(box, this.getCollidingBoundingBoxes(this, box.addCoord(0.0D, -reach, 0.0D)), reach);
   }

   static boolean hasGroundSupport(AxisAlignedBB wheelBox, List boxes) {
      return hasGroundSupport(wheelBox, boxes, 0.05D);
   }

   static boolean hasGroundSupport(AxisAlignedBB wheelBox, List boxes, double reach) {
      double probe = -reach;
      for(int i = 0; i < boxes.size(); ++i) {
         if(((AxisAlignedBB)boxes.get(i)).calculateYOffset(wheelBox, probe) > probe) return true;
      }
      return false;
   }

   public void moveEntity(double parX, double parY, double parZ) {
      super.worldObj.theProfiler.startSection("move");
      super.ySize *= 0.4F;
      double nowPosX = super.posX;
      double nowPosY = super.posY;
      double nowPosZ = super.posZ;
      double mx = parX;
      double my = parY;
      double mz = parZ;
      AxisAlignedBB axisalignedbb = super.boundingBox.copy();
      List list = this.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(parX, parY, parZ));

      for(int flag1 = 0; flag1 < list.size(); ++flag1) {
         parY = ((AxisAlignedBB)list.get(flag1)).calculateYOffset(super.boundingBox, parY);
      }

      super.boundingBox.offset(0.0D, parY, 0.0D);
      boolean isValid = super.onGround || my != parY && my < 0.0D;

      int bkParY;
      for(bkParY = 0; bkParY < list.size(); ++bkParY) {
         parX = ((AxisAlignedBB)list.get(bkParY)).calculateXOffset(super.boundingBox, parX);
      }

      super.boundingBox.offset(parX, 0.0D, 0.0D);

      for(bkParY = 0; bkParY < list.size(); ++bkParY) {
         parZ = ((AxisAlignedBB)list.get(bkParY)).calculateZOffset(super.boundingBox, parZ);
      }

      super.boundingBox.offset(0.0D, 0.0D, parZ);
      if(super.stepHeight > 0.0F && isValid && super.ySize < 0.05F && (mx != parX || mz != parZ)) {
         double bkParX = parX;
         double result = parY;
         double bkParZ = parZ;
         parX = mx;
         parY = (double)super.stepHeight;
         parZ = mz;
         AxisAlignedBB throwable = super.boundingBox.copy();
         super.boundingBox.setBB(axisalignedbb);
         list = this.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(mx, parY, mz));

         int crashreport;
         for(crashreport = 0; crashreport < list.size(); ++crashreport) {
            parY = ((AxisAlignedBB)list.get(crashreport)).calculateYOffset(super.boundingBox, parY);
         }

         super.boundingBox.offset(0.0D, parY, 0.0D);

         for(crashreport = 0; crashreport < list.size(); ++crashreport) {
            parX = ((AxisAlignedBB)list.get(crashreport)).calculateXOffset(super.boundingBox, parX);
         }

         super.boundingBox.offset(parX, 0.0D, 0.0D);

         for(crashreport = 0; crashreport < list.size(); ++crashreport) {
            parZ = ((AxisAlignedBB)list.get(crashreport)).calculateZOffset(super.boundingBox, parZ);
         }

         super.boundingBox.offset(0.0D, 0.0D, parZ);
         parY = (double)(-super.stepHeight);

         for(crashreport = 0; crashreport < list.size(); ++crashreport) {
            parY = ((AxisAlignedBB)list.get(crashreport)).calculateYOffset(super.boundingBox, parY);
         }

         super.boundingBox.offset(0.0D, parY, 0.0D);
         if(bkParX * bkParX + bkParZ * bkParZ >= parX * parX + parZ * parZ) {
            parX = bkParX;
            parY = result;
            parZ = bkParZ;
            super.boundingBox.setBB(throwable);
         }
      }

      super.worldObj.theProfiler.endSection();
      super.worldObj.theProfiler.startSection("rest");
      super.posX = (super.boundingBox.minX + super.boundingBox.maxX) / 2.0D;
      super.posY = super.boundingBox.minY + (double)super.yOffset - (double)super.ySize;
      super.posZ = (super.boundingBox.minZ + super.boundingBox.maxZ) / 2.0D;
      super.isCollidedHorizontally = mx != parX || mz != parZ;
      super.isCollidedVertically = my != parY;
      super.onGround = my != parY && my < 0.0D;
      super.isCollided = super.isCollidedHorizontally || super.isCollidedVertically;
      this.updateFallState(parY, super.onGround);
      if(mx != parX) {
         super.motionX = 0.0D;
      }

      if(my != parY) {
         super.motionY = 0.0D;
      }

      if(mz != parZ) {
         super.motionZ = 0.0D;
      }

      try {
         this.doBlockCollisions();
      } catch (Throwable throwable2) {
         CrashReport crashReport = CrashReport.makeCrashReport(throwable2, "Checking entity tile collision");
         CrashReportCategory crashreportcategory = crashReport.makeCategory("Entity being checked for collision");
         this.addEntityCrashInfo(crashreportcategory);
      }

      super.worldObj.theProfiler.endSection();
   }

   public List getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
      ArrayList collidingBoundingBoxes = new ArrayList();
      collidingBoundingBoxes.clear();
      int i = MathHelper.floor_double(par2AxisAlignedBB.minX);
      int j = MathHelper.floor_double(par2AxisAlignedBB.maxX + 1.0D);
      int k = MathHelper.floor_double(par2AxisAlignedBB.minY);
      int l = MathHelper.floor_double(par2AxisAlignedBB.maxY + 1.0D);
      int i1 = MathHelper.floor_double(par2AxisAlignedBB.minZ);
      int j1 = MathHelper.floor_double(par2AxisAlignedBB.maxZ + 1.0D);

      for(int d0 = i; d0 < j; ++d0) {
         for(int l1 = i1; l1 < j1; ++l1) {
            if(par1Entity.worldObj.blockExists(d0, 64, l1)) {
               for(int list = k - 1; list < l; ++list) {
                  Block j2 = W_WorldFunc.getBlock(par1Entity.worldObj, d0, list, l1);
                  if(j2 != null) {
                     j2.addCollisionBoxesToList(par1Entity.worldObj, d0, list, l1, par2AxisAlignedBB, collidingBoundingBoxes, par1Entity);
                  }
               }
            }
         }
      }

      double result = 0.25D;
      List entities = par1Entity.worldObj.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB.expand(result, result, result));

      for(int index = 0; index < entities.size(); ++index) {
         Entity entity = (Entity)entities.get(index);
         if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) && !(entity instanceof MCH_EntityHitBox) && entity != this.parents) {
            AxisAlignedBB axisalignedbb1 = entity.getBoundingBox();
            if(axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB)) {
               collidingBoundingBoxes.add(axisalignedbb1);
            }

            axisalignedbb1 = par1Entity.getCollisionBox(entity);
            if(axisalignedbb1 != null && axisalignedbb1.intersectsWith(par2AxisAlignedBB)) {
               collidingBoundingBoxes.add(axisalignedbb1);
            }
         }
      }

      return collidingBoundingBoxes;
   }
}
