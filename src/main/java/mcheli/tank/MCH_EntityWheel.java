package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.aircraft.MCH_EntityAircraft;
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
import net.minecraft.util.*;
import net.minecraft.world.World;

public class MCH_EntityWheel extends W_Entity {

   private MCH_EntityAircraft parents;
   public Vec3 pos;
   boolean isPlus;

   public boolean immobile = false;
   private float health = 20.0F; // Set default health


   public MCH_EntityWheel(World w) {
      super(w);
      //this.health = getHealth();
      this.setSize(1.0F, 1.0F);
      super.stepHeight = 1.5F;
      super.isImmuneToFire = true;
      this.isPlus = false;
   }

  //@Override
  //public AxisAlignedBB getBoundingBox() {
  //   return this.boundingBox; // Use the entity's bounding box for interactions
  //}

  //@Override
  //public AxisAlignedBB getCollisionBox(Entity entity) {
  //   return this.boundingBox; // Use the same bounding box for collisions
  //}

  //@Override
  //public void setSize(float width, float height) {
  //   super.setSize(width, height);
  //   this.boundingBox.setBounds(
  //           this.posX - (width / 2), this.posY, this.posZ - (width / 2),
  //           this.posX + (width / 2), this.posY + height, this.posZ + (width / 2)
  //   );
  //}

   //@Override
   //public boolean attackEntityFrom(DamageSource source, float amount) {
   //   if (this.immobile) {
   //      return false; // Ignore damage if already immobile
   //   }
//
   //   this.health -= amount;
   //   if (this.health <= 0.0F) {
   //      this.immobile = true; // Mark wheel as immobile
   //      if (this.parents instanceof MCH_EntityTank) {
   //         ((MCH_EntityTank) this.parents).onWheelDestroyed(); // Notify tank
   //      }
   //   }
   //   return true;
   //}

   @Override
   public AxisAlignedBB getBoundingBox() {
      return AxisAlignedBB.getBoundingBox(
              this.posX - 0.5, this.posY - 0.5, this.posZ - 0.5,
              this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5
      );
   }

   @Override
   public AxisAlignedBB getCollisionBox(Entity entity) {
      return this.getBoundingBox(); // Use the same box for collisions
   }

   //@Override
   //public boolean attackEntityFrom(DamageSource source, float amount) {
   //   if (this.immobile) {
   //      return false; // Ignore damage if already immobile
   //   }
//
   //   // Determine the attack location
   //   Vec3 damagePos = null;
   //   if (source.getEntity() != null) {
   //      damagePos = Vec3.createVectorHelper(
   //              source.getEntity().posX,
   //              source.getEntity().posY,
   //              source.getEntity().posZ
   //      );
   //   } else if (source instanceof EntityDamageSourceIndirect) {
   //      Entity indirectEntity = ((EntityDamageSourceIndirect) source).getSourceOfDamage();
   //      if (indirectEntity != null) {
   //         damagePos = Vec3.createVectorHelper(
   //                 indirectEntity.posX,
   //                 indirectEntity.posY,
   //                 indirectEntity.posZ
   //         );
   //      }
   //   }
//
   //   // Check if the damage is within the wheel's hitbox
   //   if (damagePos != null && this.getBoundingBox().isVecInside(damagePos)) {
   //      this.health -= amount;
   //      if (this.health <= 0.0F) {
   //         this.immobile = true; // Mark wheel as immobile
   //         if (this.parents instanceof MCH_EntityTank) {
   //            ((MCH_EntityTank) this.parents).onWheelDestroyed(); // Notify tank
   //         }
   //      }
   //      return true;
   //   }
//
   //   return false; // Ignore if the damage is outside the hitbox
   //}

   @Override
   public boolean attackEntityFrom(DamageSource source, float amount) {
      this.health -= amount;
      if (this.health <= 0.0F) {
         this.immobile = true; // Mark wheel as immobile
         if (this.parents instanceof MCH_EntityTank) {
            ((MCH_EntityTank) this.parents).onWheelDestroyed(); // Notify tank
         }
      }
      return true;
   }

  //public float getHealth() {
  //   return this.health;
  //}

  //public void setHealth(float health) {
  //   this.health = health;
  //}

   public void setWheelPos(Vec3 pos, Vec3 weightedCenter) {
      this.pos = pos;
      this.isPlus = pos.zCoord >= weightedCenter.zCoord;
   }

   public void travelToDimension(int p_71027_1_) {}

   public MCH_EntityAircraft getParents() {
      return this.parents;
   }

   public void setParents(MCH_EntityAircraft parents) {
      this.parents = parents;
   }

   //DO NOT CHANGE p_70014_1_ p_70014_1_ KEEPS THIS FUNCTIONING DESPITE HAVING 0 USES, MCHELI CODE IS JUST MAGIC BRO JUST DON'T CHANGE IT OKAY
   protected void readEntityFromNBT(NBTTagCompound p_70014_1_) {
      this.setDead();
      //this.immobile = nbt.getBoolean("Immobile");
      //this.health = nbt.getFloat("Health");
   }

   protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
      //nbt.setBoolean("Immobile", this.immobile);
      //nbt.setFloat("Health", this.health);
   }

   public void moveEntity(double parX, double parY, double parZ) {
      //if (this.immobile) {
      //   return; // Prevent movement when immobile
      //}
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
      boolean var32 = super.onGround || my != parY && my < 0.0D;

      int bkParY;
      for(bkParY = 0; bkParY < list.size(); ++bkParY) {
         parX = ((AxisAlignedBB)list.get(bkParY)).calculateXOffset(super.boundingBox, parX);
      }

      super.boundingBox.offset(parX, 0.0D, 0.0D);

      for(bkParY = 0; bkParY < list.size(); ++bkParY) {
         parZ = ((AxisAlignedBB)list.get(bkParY)).calculateZOffset(super.boundingBox, parZ);
      }

      super.boundingBox.offset(0.0D, 0.0D, parZ);
      if(super.stepHeight > 0.0F && var32 && super.ySize < 0.05F && (mx != parX || mz != parZ)) {
         double bkParX = parX;
         double var33 = parY;
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
            parY = var33;
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
      } catch (Throwable var31) {
         CrashReport var34 = CrashReport.makeCrashReport(var31, "Checking entity tile collision");
         CrashReportCategory crashreportcategory = var34.makeCategory("Entity being checked for collision");
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

      double var16 = 0.25D;
      List var17 = par1Entity.worldObj.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB.expand(var16, var16, var16));

      for(int var18 = 0; var18 < var17.size(); ++var18) {
         Entity entity = (Entity)var17.get(var18);
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
