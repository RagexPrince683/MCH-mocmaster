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
   public float suspensionCompression;
   public float prevSuspensionCompression;
   public float suspensionCompressionRate;
   public boolean suspensionSupported;
   /** Contact at the present footprint; predicted contact must not balance a stationary car. */
   public boolean suspensionCurrentSupported;
   public boolean suspensionCompressionInitialized;
   public double suspensionSupportY = Double.NaN;
   public boolean suspensionPredictedSupported;
   public double suspensionRequiredLift;


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

   /**
    * Measures present support separately from the pending footprint. Prediction
    * cannot supply current compression, spring force, grip or stationary balance.
    */
   public double measureSuspensionCompression(Vec3 currentAnchor, Vec3 predictedAnchor, double travel) {
      this.suspensionSupportY = Double.NaN;
      this.suspensionRequiredLift = 0;
      double current = this.measureCompressionAt(currentAnchor, travel);
      this.suspensionCurrentSupported = current >= 0.0D;
      double supportY = this.suspensionSupportY;
      double requiredLift = this.suspensionRequiredLift;
      double predicted = currentAnchor == predictedAnchor ? current : this.measureCompressionAt(predictedAnchor, travel);
      this.suspensionPredictedSupported = predicted >= 0.0D;
      this.suspensionSupportY = supportY;
      this.suspensionRequiredLift = requiredLift;
      this.suspensionSupported = this.suspensionCurrentSupported;
      // Saturation is distinct from droop: an obstructed wheel cannot be placed
      // below the raised surface as though it had simply lost its contact.
      return this.suspensionSupported ? current : requiredLift > 0 ? travel : 0.0D;
   }

   /** Correct low collision anchors without changing authored model positions or terrain pose. */
   double getSuspensionAnchorOffset() {
      double levelBottom = this.parents.posY + this.pos.yCoord + this.boundingBox.minY - this.posY;
      return Math.max(0.0D, Math.min(this.parents.getUnrotatedBodyFloor() - levelBottom,
            this.boundingBox.maxY - this.boundingBox.minY));
   }

   private double measureCompressionAt(Vec3 anchor, double travel) {
      AxisAlignedBB probe = this.boundingBox.copy();
      double centerX = (probe.minX + probe.maxX) * 0.5D;
      double centerZ = (probe.minZ + probe.maxZ) * 0.5D;
      double anchorOffset = this.getSuspensionAnchorOffset();
      // Keep the level reference independent of pitch/roll so unequal wheel heights
      // still describe terrain. Bound normalization and the tracking skin by the
      // original wheel height; a box wholly below the body floor gains no reach.
      double lift = Math.max(0.0D, Math.min(0.05D, probe.maxY - probe.minY - anchorOffset));
      probe.offset(anchor.xCoord - centerX, anchor.yCoord - this.posY + anchorOffset + lift,
            anchor.zCoord - centerZ);

      // Select real top faces rather than calculateYOffset alone: an overlapping
      // start box cannot produce a downward hit with that API. Neither the skin
      // nor overlap recovery extends the lower suspension endpoint.
      double sweep = -(travel + 0.08D + lift);
      List boxes = this.getBlockCollisionBoxes(this, probe.addCoord(0.0D, sweep, 0.0D));
      double anchorBottom = probe.minY - lift;
      double highest = Double.NaN;
      double requiredLift = 0;
      for(int i = 0; i < boxes.size(); ++i) {
         AxisAlignedBB box = (AxisAlignedBB)boxes.get(i);
         if(box.maxX <= probe.minX || box.minX >= probe.maxX
               || box.maxZ <= probe.minZ || box.minZ >= probe.maxZ) continue;
         if(box.maxY < anchorBottom - travel - 0.08D - 1.0E-5D) continue;
         if(box.maxY > anchorBottom + lift + 1.0E-5D) {
            // This is over-compression, not an unsupported downward sweep. Only
            // the body solver may lift the chassis, within its existing budget.
            if(box.minY <= probe.minY + 1.0E-5D)
               requiredLift = Math.max(requiredLift, box.maxY - anchorBottom);
            continue;
         }
         if(Double.isNaN(highest) || box.maxY > highest) highest = box.maxY;
      }
      this.suspensionRequiredLift = requiredLift;
      this.suspensionSupportY = requiredLift > 0 ? Double.NaN : highest;
      if(Double.isNaN(this.suspensionSupportY)) return -1.0D;
      return MathHelper.clamp_double(travel + highest - anchorBottom, 0.0D, travel);
   }

   /** Grip-only support sweep. The invisible suspension box is not the rendered tire patch. */
   public boolean hasCarGroundContact() {
      if(this.isDead || this.worldObj == null || this.boundingBox == null || this.parents == null || this.pos == null) return false;
      AxisAlignedBB box = this.boundingBox.copy();
      // Use the actual box offset/height, including ySize, rather than assuming
      // every authored wheel anchor is above the body's collision floor.
      Vec3 anchor = this.parents.getTransformedPosition(this.pos);
      double targetBottom = anchor.yCoord + box.minY - this.posY;
      MCH_TankInfo info = this.parents instanceof MCH_EntityTank ? ((MCH_EntityTank)this.parents).getTankInfo() : null;
      if(info != null && info.civilianCarGrip) {
         // Read the current accepted anchor, never a stale/predicted wheel box.
         // Use suspension's block-shape and travel bounds for all civilian contacts.
         double savedY = this.suspensionSupportY, savedLift = this.suspensionRequiredLift;
         boolean supported = this.measureCompressionAt(anchor, info.suspensionTravel) >= 0;
         this.suspensionSupportY = savedY; this.suspensionRequiredLift = savedLift;
         return supported;
      }
      double bodyBottom = this.parents.getUnrotatedBodyFloor();
      double restGap = Math.max(0.0D, targetBottom - bodyBottom);
      // Low anchors (e.g. Chiron Y=-0.74) put the invisible box through the road.
      // calculateYOffset cannot find downward support from an overlapping box.
      // Move only the query bottom to the body floor within the anchored wheel's
      // real height; never promote a box wholly below that floor into contact.
      double height = box.maxY - box.minY;
      double supportBottom = Math.max(targetBottom, Math.min(bodyBottom, targetBottom + height));
      // A suspension wheel can lag below the body during takeoff. Never use that stale support.
      double queryBottom = Math.max(box.minY, supportBottom);
      // Entity tracking floors client positions to 1/32 block. A grounded body's
      // interpolated floor can therefore sit just inside the road, where a downward
      // calculateYOffset finds no support. Lift through the existing collision skin,
      // within the anchored box height, and extend the sweep by exactly that lift.
      // Its lower endpoint is unchanged: unsupported/airborne wheels gain no reach.
      double lift = Math.max(0.0D, Math.min(0.05D, targetBottom + height - queryBottom));
      box.offset(0.0D, queryBottom + lift - box.minY, 0.0D);
      double reach = 0.05D + restGap + lift;
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

   private List getBlockCollisionBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
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

      return collidingBoundingBoxes;
   }

   public List getCollidingBoundingBoxes(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB) {
      List collidingBoundingBoxes = this.getBlockCollisionBoxes(par1Entity, par2AxisAlignedBB);
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
