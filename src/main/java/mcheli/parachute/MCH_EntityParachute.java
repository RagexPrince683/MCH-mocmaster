package mcheli.parachute;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.particles.MCH_ParticleParam;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.wrapper.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityParachute extends W_Entity {

   private double speedMultiplier;
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
   public Entity user;
   public int onGroundCount;


   public MCH_EntityParachute(World par1World) {
      super(par1World);
      this.speedMultiplier = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(1.5F, 0.6F);
      super.yOffset = super.height / 2.0F;
      this.user = null;
      this.onGroundCount = 0;
   }

   public MCH_EntityParachute(World par1World, double par2, double par4, double par6) {
      this(par1World);
      this.setPosition(par2, par4 + (double)super.yOffset, par6);
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      super.prevPosX = par2;
      super.prevPosY = par4;
      super.prevPosZ = par6;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   protected void entityInit() {
      this.getDataWatcher().addObject(31, Byte.valueOf((byte)0));
   }

   public void setType(int n) {
      this.getDataWatcher().updateObject(31, Byte.valueOf((byte)n));
   }

   public int getType() {
      return this.getDataWatcher().getWatchableObjectByte(31);
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
      return (double)super.height * 0.0D - 0.30000001192092896D;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return false;
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
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
      if(!super.worldObj.isRemote && super.ticksExisted % 10 == 0) {
         MCH_Lib.DbgLog(super.worldObj, "MCH_EntityParachute.onUpdate %d, %.3f", new Object[]{Integer.valueOf(super.ticksExisted), Double.valueOf(super.motionY)});
      }

      if(this.isOpenParachute() && super.motionY > -0.3D && super.ticksExisted > 20) {
         super.fallDistance = (float)((double)super.fallDistance * 0.85D);
      }

      if(!super.worldObj.isRemote && this.user != null && this.user.ridingEntity == null) {
         this.user.mountEntity(this);
         super.rotationYaw = super.prevRotationYaw = this.user.rotationYaw;
         this.user = null;
      }

      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      double d1 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * 0.0D / 5.0D - 0.125D;
      double d2 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * 1.0D / 5.0D - 0.125D;
      AxisAlignedBB axisalignedbb = W_AxisAlignedBB.getAABB(super.boundingBox.minX, d1, super.boundingBox.minZ, super.boundingBox.maxX, d2, super.boundingBox.maxZ);
      if(super.worldObj.isAABBInMaterial(axisalignedbb, Material.water)) {
         this.onWaterSetBoat();
         this.setDead();
      }

      if(super.worldObj.isRemote) {
         this.onUpdateClient();
      } else {
         this.onUpdateServer();
      }

   }

   public void onUpdateClient() {
      if(this.paraPosRotInc > 0) {
         double color = super.posX + (this.paraX - super.posX) / (double)this.paraPosRotInc;
         double y = super.posY + (this.paraY - super.posY) / (double)this.paraPosRotInc;
         double z = super.posZ + (this.paraZ - super.posZ) / (double)this.paraPosRotInc;
         double yaw = MathHelper.wrapAngleTo180_double(this.paraYaw - (double)super.rotationYaw);
         super.rotationYaw = (float)((double)super.rotationYaw + yaw / (double)this.paraPosRotInc);
         super.rotationPitch = (float)((double)super.rotationPitch + (this.paraPitch - (double)super.rotationPitch) / (double)this.paraPosRotInc);
         --this.paraPosRotInc;
         this.setPosition(color, y, z);
         this.setRotation(super.rotationYaw, super.rotationPitch);
         if(super.riddenByEntity != null) {
            this.setRotation(super.riddenByEntity.prevRotationYaw, super.rotationPitch);
         }
      } else {
         this.setPosition(super.posX + super.motionX, super.posY + super.motionY, super.posZ + super.motionZ);
         if(super.onGround) {
            ;
         }

         super.motionX *= 0.99D;
         super.motionY *= 0.95D;
         super.motionZ *= 0.99D;
      }

      if(!this.isOpenParachute() && super.motionY > 0.01D) {
         float var12 = 0.6F + super.rand.nextFloat() * 0.2F;
         double dx = super.prevPosX - super.posX;
         double dy = super.prevPosY - super.posY;
         double dz = super.prevPosZ - super.posZ;
         int num = 1 + (int)((double)MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) * 2.0D);

         for(double i = 0.0D; i < (double)num; ++i) {
            MCH_ParticleParam prm = new MCH_ParticleParam(super.worldObj, "smoke", super.prevPosX + (super.posX - super.prevPosX) * (i / (double)num) * 0.8D, super.prevPosY + (super.posY - super.prevPosY) * (i / (double)num) * 0.8D, super.prevPosZ + (super.posZ - super.prevPosZ) * (i / (double)num) * 0.8D);
            prm.motionX = super.motionX * 0.5D + (super.rand.nextDouble() - 0.5D) * 0.5D;
            prm.motionX = super.motionY * -0.5D + (super.rand.nextDouble() - 0.5D) * 0.5D;
            prm.motionX = super.motionZ * 0.5D + (super.rand.nextDouble() - 0.5D) * 0.5D;
            prm.size = 5.0F;
            prm.setColor(0.8F + super.rand.nextFloat(), var12, var12, var12);
            MCH_ParticlesUtil.spawnParticle(prm);
         }
      }

   }

   public void onUpdateServer() {
      double prevSpeed = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      double gravity = super.onGround?0.01D:0.03D;
      if(this.getType() == 2 && super.ticksExisted < 20) {
         gravity = 0.01D;
      }

      super.motionY -= gravity;
      double yaw;
      double dx;
      double dz;
      if(this.isOpenParachute()) {
         if(W_Lib.isEntityLivingBase(super.riddenByEntity)) {
            yaw = W_Lib.getEntityMoveDist(super.riddenByEntity);
            if(!this.isOpenParachute()) {
               yaw = 0.0D;
            }

            if(yaw > 0.0D) {
               dx = -Math.sin((double)(super.riddenByEntity.rotationYaw * 3.1415927F / 180.0F));
               dz = Math.cos((double)(super.riddenByEntity.rotationYaw * 3.1415927F / 180.0F));
               super.motionX += dx * this.speedMultiplier * 0.05D;
               super.motionZ += dz * this.speedMultiplier * 0.05D;
            }
         }

         yaw = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         if(yaw > 0.35D) {
            super.motionX *= 0.35D / yaw;
            super.motionZ *= 0.35D / yaw;
            yaw = 0.35D;
         }

         if(yaw > prevSpeed && this.speedMultiplier < 0.35D) {
            this.speedMultiplier += (0.35D - this.speedMultiplier) / 35.0D;
            if(this.speedMultiplier > 0.35D) {
               this.speedMultiplier = 0.35D;
            }
         } else {
            this.speedMultiplier -= (this.speedMultiplier - 0.07D) / 35.0D;
            if(this.speedMultiplier < 0.07D) {
               this.speedMultiplier = 0.07D;
            }
         }
      }

      if(super.onGround) {
         ++this.onGroundCount;
         if(this.onGroundCount > 5) {
            this.onGroundAndDead();
            return;
         }
      }

      this.moveEntity(super.motionX, super.motionY, super.motionZ);
      if(this.getType() == 2 && super.ticksExisted < 20) {
         super.motionY *= 0.95D;
      } else {
         super.motionY *= 0.9D;
      }

      if(this.isOpenParachute()) {
         super.motionX *= 0.95D;
         super.motionZ *= 0.95D;
      } else {
         super.motionX *= 0.99D;
         super.motionZ *= 0.99D;
      }

      super.rotationPitch = 0.0F;
      yaw = (double)super.rotationYaw;
      dx = super.prevPosX - super.posX;
      dz = super.prevPosZ - super.posZ;
      if(dx * dx + dz * dz > 0.001D) {
         yaw = (double)((float)(Math.atan2(dx, dz) * 180.0D / 3.141592653589793D));
      }

      double yawDiff = MathHelper.wrapAngleTo180_double(yaw - (double)super.rotationYaw);
      if(yawDiff > 20.0D) {
         yawDiff = 20.0D;
      }

      if(yawDiff < -20.0D) {
         yawDiff = -20.0D;
      }

      if(super.riddenByEntity != null) {
         this.setRotation(super.riddenByEntity.rotationYaw, super.rotationPitch);
      } else {
         super.rotationYaw = (float)((double)super.rotationYaw + yawDiff);
         this.setRotation(super.rotationYaw, super.rotationPitch);
      }

      List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand(0.2D, 0.0D, 0.2D));
      if(list != null && !list.isEmpty()) {
         for(int l = 0; l < list.size(); ++l) {
            Entity entity = (Entity)list.get(l);
            if(entity != super.riddenByEntity && entity.canBePushed() && entity instanceof MCH_EntityParachute) {
               entity.applyEntityCollision(this);
            }
         }
      }

      if(super.riddenByEntity != null && super.riddenByEntity.isDead) {
         super.riddenByEntity = null;
         this.setDead();
      }

   }

   public void onGroundAndDead() {
      ++super.posY;
      this.updateRiderPosition();
      this.setDead();
   }

   public void onWaterSetBoat() {
      if(!super.worldObj.isRemote) {
         if(this.getType() == 2) {
            if(super.riddenByEntity != null) {
               int px = (int)(super.posX + 0.5D);
               int py = (int)(super.posY + 0.5D);
               int pz = (int)(super.posZ + 0.5D);
               boolean foundBlock = false;

               int countWater;
               for(countWater = 0; countWater < 5 && py + countWater >= 0 && py + countWater <= 255; ++countWater) {
                  Block size = W_WorldFunc.getBlock(super.worldObj, px, py - countWater, pz);
                  if(size == W_Block.getWater()) {
                     py -= countWater;
                     foundBlock = true;
                     break;
                  }
               }

               if(foundBlock) {
                  countWater = 0;
                  boolean var11 = true;

                  for(int entityboat = 0; entityboat < 3 && py + entityboat >= 0 && py + entityboat <= 255; ++entityboat) {
                     int x = -2;

                     while(x <= 2) {
                        int z = -2;

                        while(true) {
                           if(z <= 2) {
                              label61: {
                                 Block block = W_WorldFunc.getBlock(super.worldObj, px + x, py - entityboat, pz + z);
                                 if(block == W_Block.getWater()) {
                                    ++countWater;
                                    if(countWater > 37) {
                                       break label61;
                                    }
                                 }

                                 ++z;
                                 continue;
                              }
                           }

                           ++x;
                           break;
                        }
                     }
                  }

                  if(countWater > 37) {
                     EntityBoat var12 = new EntityBoat(super.worldObj, (double)px, (double)((float)py + 1.0F), (double)pz);
                     var12.rotationYaw = super.rotationYaw - 90.0F;
                     super.worldObj.spawnEntityInWorld(var12);
                     super.riddenByEntity.mountEntity(var12);
                  }

               }
            }
         }
      }
   }

   public boolean isOpenParachute() {
      return this.getType() != 2 || super.motionY < -0.1D;
   }

   public void updateRiderPosition() {
      if(super.riddenByEntity != null) {
         double x = -Math.sin((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.1D;
         double z = Math.cos((double)super.rotationYaw * 3.141592653589793D / 180.0D) * 0.1D;
         super.riddenByEntity.setPosition(super.posX + x, super.posY + this.getMountedYOffset() + super.riddenByEntity.getYOffset(), super.posZ + z);
      }

   }

   protected void writeEntityToNBT(NBTTagCompound nbt) {
      nbt.setByte("ParachuteModelType", (byte)this.getType());
   }

   protected void readEntityFromNBT(NBTTagCompound nbt) {
      this.setType(nbt.getByte("ParachuteModelType"));
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 4.0F;
   }

   public boolean interactFirst(EntityPlayer par1EntityPlayer) {
      return false;
   }
}
