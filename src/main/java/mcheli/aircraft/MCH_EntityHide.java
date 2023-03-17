package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MCH_EntityHide extends W_Entity {

   private MCH_EntityAircraft ac;
   private Entity user;
   private int paraPosRotInc;
   private double paraX;
   private double paraY;
   private double paraZ;
   private double paraYaw;
   private double paraPitch;
   @SideOnly(Side.CLIENT)
   private double velocityX;
   @SideOnly(Side.CLIENT)
   private double velocityY;
   @SideOnly(Side.CLIENT)
   private double velocityZ;


   public MCH_EntityHide(World par1World) {
      super(par1World);
      this.setSize(1.0F, 1.0F);
      super.preventEntitySpawning = true;
      super.yOffset = super.height / 2.0F;
      this.user = null;
      super.motionX = super.motionY = super.motionZ = 0.0D;
   }

   public MCH_EntityHide(World par1World, double x, double y, double z) {
      this(par1World);
      super.posX = x;
      super.posY = y;
      super.posZ = z;
   }

   protected void entityInit() {
      super.entityInit();
      this.createRopeIndex(-1);
      this.getDataWatcher().addObject(31, new Integer(0));
   }

   public void setParent(MCH_EntityAircraft ac, Entity user, int ropeIdx) {
      this.ac = ac;
      this.setRopeIndex(ropeIdx);
      this.user = user;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public AxisAlignedBB getCollisionBox(Entity par1Entity) {
      return par1Entity.boundingBox;
   }

   public AxisAlignedBB getBoundingBox() {
      return super.boundingBox;
   }

   public boolean canBePushed() {
      return true;
   }

   public double getMountedYOffset() {
      return (double)super.height * 0.0D - 0.3D;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return false;
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   protected void writeEntityToNBT(NBTTagCompound nbt) {}

   protected void readEntityFromNBT(NBTTagCompound nbt) {}

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean interactFirst(EntityPlayer par1EntityPlayer) {
      return false;
   }

   public void createRopeIndex(int defaultValue) {
      this.getDataWatcher().addObject(30, new Integer(defaultValue));
   }

   public int getRopeIndex() {
      return this.getDataWatcher().getWatchableObjectInt(30);
   }

   public void setRopeIndex(int value) {
      this.getDataWatcher().updateObject(30, new Integer(value));
   }

   @SideOnly(Side.CLIENT)
   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
      this.paraPosRotInc = par9 + 10;
      this.paraX = par1;
      this.paraY = par3;
      this.paraZ = par5;
      this.paraYaw = (double)par7;
      this.paraPitch = (double)par8;
      super.motionX = this.velocityX;
      super.motionY = this.velocityY;
      super.motionZ = this.velocityZ;
   }

   @SideOnly(Side.CLIENT)
   public void setVelocity(double par1, double par3, double par5) {
      this.velocityX = super.motionX = par1;
      this.velocityY = super.motionY = par3;
      this.velocityZ = super.motionZ = par5;
   }

   public void setDead() {
      super.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      if(this.user != null && !super.worldObj.isRemote) {
         if(this.ac != null) {
            this.getDataWatcher().updateObject(31, new Integer(this.ac.getEntityId()));
         }

         this.user.mountEntity(this);
         this.user = null;
      }

      int id;
      if(this.ac == null && super.worldObj.isRemote) {
         id = this.getDataWatcher().getWatchableObjectInt(31);
         if(id > 0) {
            Entity v = super.worldObj.getEntityByID(id);
            if(v instanceof MCH_EntityAircraft) {
               this.ac = (MCH_EntityAircraft)v;
            }
         }
      }

      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      super.fallDistance = 0.0F;
      if(super.riddenByEntity != null) {
         super.riddenByEntity.fallDistance = 0.0F;
      }

      if(this.ac != null) {
         if(!this.ac.isRepelling()) {
            this.setDead();
         }

         id = this.getRopeIndex();
         if(id >= 0) {
            Vec3 v1 = this.ac.getRopePos(id);
            super.posX = v1.xCoord;
            super.posZ = v1.zCoord;
         }
      }

      this.setPosition(super.posX, super.posY, super.posZ);
      if(super.worldObj.isRemote) {
         this.onUpdateClient();
      } else {
         this.onUpdateServer();
      }

   }

   public void onUpdateClient() {
      if(this.paraPosRotInc > 0) {
         double x = super.posX + (this.paraX - super.posX) / (double)this.paraPosRotInc;
         double y = super.posY + (this.paraY - super.posY) / (double)this.paraPosRotInc;
         double z = super.posZ + (this.paraZ - super.posZ) / (double)this.paraPosRotInc;
         double yaw = MathHelper.wrapAngleTo180_double(this.paraYaw - (double)super.rotationYaw);
         super.rotationYaw = (float)((double)super.rotationYaw + yaw / (double)this.paraPosRotInc);
         super.rotationPitch = (float)((double)super.rotationPitch + (this.paraPitch - (double)super.rotationPitch) / (double)this.paraPosRotInc);
         --this.paraPosRotInc;
         this.setPosition(x, y, z);
         this.setRotation(super.rotationYaw, super.rotationPitch);
         if(super.riddenByEntity != null) {
            this.setRotation(super.riddenByEntity.prevRotationYaw, super.rotationPitch);
         }
      } else {
         this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
         super.motionX *= 0.99D;
         super.motionY *= 0.95D;
         super.motionZ *= 0.99D;
      }

   }

   public void onUpdateServer() {
      super.motionY -= super.onGround?0.01D:0.03D;
      if(super.onGround) {
         this.onGroundAndDead();
      } else {
         this.moveEntity(super.motionX, super.motionY, super.motionZ);
         super.motionY *= 0.9D;
         super.motionX *= 0.95D;
         super.motionZ *= 0.95D;
         int id = this.getRopeIndex();
         if(this.ac != null && id >= 0) {
            Vec3 v = this.ac.getRopePos(id);
            if(Math.abs(super.posY - v.yCoord) > (double)(Math.abs(this.ac.ropesLength) + 5.0F)) {
               this.onGroundAndDead();
            }
         }

         if(super.riddenByEntity != null && super.riddenByEntity.isDead) {
            super.riddenByEntity = null;
            this.setDead();
         }

      }
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
            if(super.worldObj.blockExists(d0, 64, l1)) {
               for(int list = k - 1; list < l; ++list) {
                  Block j2 = W_WorldFunc.getBlock(super.worldObj, d0, list, l1);
                  if(j2 != null) {
                     j2.addCollisionBoxesToList(super.worldObj, d0, list, l1, par2AxisAlignedBB, collidingBoundingBoxes, par1Entity);
                  }
               }
            }
         }
      }

      double var16 = 0.25D;
      List var17 = super.worldObj.getEntitiesWithinAABBExcludingEntity(par1Entity, par2AxisAlignedBB.expand(var16, var16, var16));

      for(int var18 = 0; var18 < var17.size(); ++var18) {
         Entity entity = (Entity)var17.get(var18);
         if(!W_Lib.isEntityLivingBase(entity) && !(entity instanceof MCH_EntitySeat) && !(entity instanceof MCH_EntityHitBox)) {
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

   public void moveEntity(double par1, double par3, double par5) {
      super.worldObj.theProfiler.startSection("move");
      super.ySize *= 0.4F;
      double d3 = super.posX;
      double d4 = super.posY;
      double d5 = super.posZ;
      double d6 = par1;
      double d7 = par3;
      double d8 = par5;
      AxisAlignedBB axisalignedbb = super.boundingBox.copy();
      List list = this.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(par1, par3, par5));

      for(int flag1 = 0; flag1 < list.size(); ++flag1) {
         par3 = ((AxisAlignedBB)list.get(flag1)).calculateYOffset(super.boundingBox, par3);
      }

      super.boundingBox.offset(0.0D, par3, 0.0D);
      if(!super.field_70135_K && d7 != par3) {
         par5 = 0.0D;
         par3 = 0.0D;
         par1 = 0.0D;
      }

      boolean var34 = super.onGround || d7 != par3 && d7 < 0.0D;

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

      if(super.stepHeight > 0.0F && var34 && super.ySize < 0.05F && (d6 != par1 || d8 != par5)) {
         double d12 = par1;
         double d10 = par3;
         double d11 = par5;
         par1 = d6;
         par3 = (double)super.stepHeight;
         par5 = d8;
         AxisAlignedBB throwable = super.boundingBox.copy();
         super.boundingBox.setBB(axisalignedbb);
         list = this.getCollidingBoundingBoxes(this, super.boundingBox.addCoord(d6, par3, d8));

         int k;
         for(k = 0; k < list.size(); ++k) {
            par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(super.boundingBox, par3);
         }

         super.boundingBox.offset(0.0D, par3, 0.0D);
         if(!super.field_70135_K && d7 != par3) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         for(k = 0; k < list.size(); ++k) {
            par1 = ((AxisAlignedBB)list.get(k)).calculateXOffset(super.boundingBox, par1);
         }

         super.boundingBox.offset(par1, 0.0D, 0.0D);
         if(!super.field_70135_K && d6 != par1) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         for(k = 0; k < list.size(); ++k) {
            par5 = ((AxisAlignedBB)list.get(k)).calculateZOffset(super.boundingBox, par5);
         }

         super.boundingBox.offset(0.0D, 0.0D, par5);
         if(!super.field_70135_K && d8 != par5) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         }

         if(!super.field_70135_K && d7 != par3) {
            par5 = 0.0D;
            par3 = 0.0D;
            par1 = 0.0D;
         } else {
            par3 = (double)(-super.stepHeight);

            for(k = 0; k < list.size(); ++k) {
               par3 = ((AxisAlignedBB)list.get(k)).calculateYOffset(super.boundingBox, par3);
            }

            super.boundingBox.offset(0.0D, par3, 0.0D);
         }

         if(d12 * d12 + d11 * d11 >= par1 * par1 + par5 * par5) {
            par1 = d12;
            par3 = d10;
            par5 = d11;
            super.boundingBox.setBB(throwable);
         }
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

      double var10000 = super.posX - d3;
      var10000 = super.posY - d4;
      var10000 = super.posZ - d5;

      try {
         this.doBlockCollisions();
      } catch (Throwable var33) {
         CrashReport crashreport = CrashReport.makeCrashReport(var33, "Checking entity tile collision");
         CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
         this.addEntityCrashInfo(crashreportcategory);
         throw new ReportedException(crashreport);
      }

      super.worldObj.theProfiler.endSection();
   }

   public void onGroundAndDead() {
      super.posY += 0.5D;
      this.updateRiderPosition();
      this.setDead();
   }

   public void _updateRiderPosition() {
      if(super.riddenByEntity != null) {
         double x = -Math.sin((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.1D;
         double z = Math.cos((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.1D;
         super.riddenByEntity.setPosition(super.posX + x, super.posY + this.getMountedYOffset() + super.riddenByEntity.getYOffset(), super.posZ + z);
      }

   }
}
