package mcheli.container;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_IEntityCanRideAircraft;
import mcheli.aircraft.MCH_SeatRackInfo;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.wrapper.W_AxisAlignedBB;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_EntityContainer;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class MCH_EntityContainer extends W_EntityContainer implements MCH_IEntityCanRideAircraft {

   private boolean field_70279_a;
   private double speedMultiplier;
   private int boatPosRotationIncrements;
   private double boatX;
   private double boatY;
   private double boatZ;
   private double boatYaw;
   private double boatPitch;
   @SideOnly(Side.CLIENT)
   private double velocityX;
   @SideOnly(Side.CLIENT)
   private double velocityY;
   @SideOnly(Side.CLIENT)
   private double velocityZ;


   public MCH_EntityContainer(World par1World) {
      super(par1World);
      this.speedMultiplier = 0.07D;
      super.preventEntitySpawning = true;
      this.setSize(2.0F, 1.0F);
      super.yOffset = super.height / 2.0F;
      super.stepHeight = 0.6F;
      super.isImmuneToFire = true;
      super.renderDistanceWeight = 2.0D;
   }

   public MCH_EntityContainer(World par1World, double par2, double par4, double par6) {
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
      super.dataWatcher.addObject(17, new Integer(0));
      super.dataWatcher.addObject(18, new Integer(1));
      super.dataWatcher.addObject(19, new Integer(0));
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

   public int getSizeInventory() {
      return 54;
   }

   public String getInvName() {
      return "Container " + super.getInvName();
   }

   public double getMountedYOffset() {
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource ds, float damage) {
      if(this.isEntityInvulnerable()) {
         return false;
      } else if(!super.worldObj.isRemote && !super.isDead) {
         MCH_Config var10000 = MCH_MOD.config;
         damage = MCH_Config.applyDamageByExternal(this, ds, damage);
         if(!MCH_Multiplay.canAttackEntity(ds, this)) {
            return false;
         } else if(ds.getEntity() instanceof EntityPlayer && ds.getDamageType().equalsIgnoreCase("player")) {
            MCH_Lib.DbgLog(super.worldObj, "MCH_EntityContainer.attackEntityFrom:damage=%.1f:%s", new Object[]{Float.valueOf(damage), ds.getDamageType()});
            W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.3F);
            this.setDamageTaken(this.getDamageTaken() + (int)(damage * 20.0F));
            this.setForwardDirection(-this.getForwardDirection());
            this.setTimeSinceHit(10);
            this.setBeenAttacked();
            boolean flag = ds.getEntity() instanceof EntityPlayer && ((EntityPlayer)ds.getEntity()).capabilities.isCreativeMode;
            if(flag || (float)this.getDamageTaken() > 40.0F) {
               if(!flag) {
                  this.dropItemWithOffset(MCH_MOD.itemContainer, 1, 0.0F);
               }

               this.setDead();
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @SideOnly(Side.CLIENT)
   public void performHurtAnimation() {
      this.setForwardDirection(-this.getForwardDirection());
      this.setTimeSinceHit(10);
      this.setDamageTaken(this.getDamageTaken() * 11);
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   @SideOnly(Side.CLIENT)
   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
      this.boatPosRotationIncrements = par9 + 10;
      this.boatX = par1;
      this.boatY = par3;
      this.boatZ = par5;
      this.boatYaw = (double)par7;
      this.boatPitch = (double)par8;
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

   public void onUpdate() {
      super.onUpdate();
      if(this.getTimeSinceHit() > 0) {
         this.setTimeSinceHit(this.getTimeSinceHit() - 1);
      }

      if((float)this.getDamageTaken() > 0.0F) {
         this.setDamageTaken(this.getDamageTaken() - 1);
      }

      super.prevPosX = super.posX;
      super.prevPosY = super.posY;
      super.prevPosZ = super.posZ;
      byte b0 = 5;
      double d0 = 0.0D;

      for(int d3 = 0; d3 < b0; ++d3) {
         double d1 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * (double)(d3 + 0) / (double)b0 - 0.125D;
         double d2 = super.boundingBox.minY + (super.boundingBox.maxY - super.boundingBox.minY) * (double)(d3 + 1) / (double)b0 - 0.125D;
         AxisAlignedBB axisalignedbb = W_AxisAlignedBB.getAABB(super.boundingBox.minX, d1, super.boundingBox.minZ, super.boundingBox.maxX, d2, super.boundingBox.maxZ);
         if(super.worldObj.isAABBInMaterial(axisalignedbb, Material.water)) {
            d0 += 1.0D / (double)b0;
         } else if(super.worldObj.isAABBInMaterial(axisalignedbb, Material.lava)) {
            d0 += 1.0D / (double)b0;
         }
      }

      double var22 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
      double d4;
      double d5;
      if(var22 > 0.2625D) {
         d4 = Math.cos((double)super.rotationYaw * 3.141592653589793D / 180.0D);
         d5 = Math.sin((double)super.rotationYaw * 3.141592653589793D / 180.0D);
      }

      double d10;
      double d11;
      float d12;
      if(super.worldObj.isRemote) {
         if(this.boatPosRotationIncrements > 0) {
            d4 = super.posX + (this.boatX - super.posX) / (double)this.boatPosRotationIncrements;
            d5 = super.posY + (this.boatY - super.posY) / (double)this.boatPosRotationIncrements;
            d11 = super.posZ + (this.boatZ - super.posZ) / (double)this.boatPosRotationIncrements;
            d10 = MathHelper.wrapAngleTo180_double(this.boatYaw - (double)super.rotationYaw);
            super.rotationYaw = (float)((double)super.rotationYaw + d10 / (double)this.boatPosRotationIncrements);
            super.rotationPitch = (float)((double)super.rotationPitch + (this.boatPitch - (double)super.rotationPitch) / (double)this.boatPosRotationIncrements);
            --this.boatPosRotationIncrements;
            this.setPosition(d4, d5, d11);
            this.setRotation(super.rotationYaw, super.rotationPitch);
         } else {
            d4 = super.posX + super.motionX;
            d5 = super.posY + super.motionY;
            d11 = super.posZ + super.motionZ;
            this.setPosition(d4, d5, d11);
            if(super.onGround) {
               d12 = 0.9F;
               super.motionX *= 0.8999999761581421D;
               super.motionZ *= 0.8999999761581421D;
            }

            super.motionX *= 0.99D;
            super.motionY *= 0.95D;
            super.motionZ *= 0.99D;
         }
      } else {
         if(d0 < 1.0D) {
            d4 = d0 * 2.0D - 1.0D;
            super.motionY += 0.04D * d4;
         } else {
            if(super.motionY < 0.0D) {
               super.motionY /= 2.0D;
            }

            super.motionY += 0.007D;
         }

         d4 = Math.sqrt(super.motionX * super.motionX + super.motionZ * super.motionZ);
         if(d4 > 0.35D) {
            d5 = 0.35D / d4;
            super.motionX *= d5;
            super.motionZ *= d5;
            d4 = 0.35D;
         }

         if(d4 > var22 && this.speedMultiplier < 0.35D) {
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

         if(super.onGround) {
            d12 = 0.9F;
            super.motionX *= 0.8999999761581421D;
            super.motionZ *= 0.8999999761581421D;
         }

         this.moveEntity(super.motionX, super.motionY, super.motionZ);
         super.motionX *= 0.99D;
         super.motionY *= 0.95D;
         super.motionZ *= 0.99D;
         super.rotationPitch = 0.0F;
         d5 = (double)super.rotationYaw;
         d11 = super.prevPosX - super.posX;
         d10 = super.prevPosZ - super.posZ;
         if(d11 * d11 + d10 * d10 > 0.001D) {
            d5 = (double)((float)(Math.atan2(d10, d11) * 180.0D / 3.141592653589793D));
         }

         double var23 = MathHelper.wrapAngleTo180_double(d5 - (double)super.rotationYaw);
         if(var23 > 5.0D) {
            var23 = 5.0D;
         }

         if(var23 < -5.0D) {
            var23 = -5.0D;
         }

         super.rotationYaw = (float)((double)super.rotationYaw + var23);
         this.setRotation(super.rotationYaw, super.rotationPitch);
         if(!super.worldObj.isRemote) {
            List list = super.worldObj.getEntitiesWithinAABBExcludingEntity(this, super.boundingBox.expand(0.2D, 0.0D, 0.2D));
            int l;
            if(list != null && !list.isEmpty()) {
               for(l = 0; l < list.size(); ++l) {
                  Entity i1 = (Entity)list.get(l);
                  if(i1.canBePushed() && i1 instanceof MCH_EntityContainer) {
                     i1.applyEntityCollision(this);
                  }
               }
            }

            MCH_Config var10000 = MCH_MOD.config;
            if(MCH_Config.Collision_DestroyBlock.prmBool) {
               for(l = 0; l < 4; ++l) {
                  int var24 = MathHelper.floor_double(super.posX + ((double)(l % 2) - 0.5D) * 0.8D);
                  int j1 = MathHelper.floor_double(super.posZ + ((double)(l / 2) - 0.5D) * 0.8D);

                  for(int k1 = 0; k1 < 2; ++k1) {
                     int l1 = MathHelper.floor_double(super.posY) + k1;
                     if(W_WorldFunc.isEqualBlock(super.worldObj, var24, l1, j1, W_Block.getSnowLayer())) {
                        super.worldObj.setBlockToAir(var24, l1, j1);
                     } else if(W_WorldFunc.isEqualBlock(super.worldObj, var24, l1, j1, Blocks.waterlily)) {
                        W_WorldFunc.destroyBlock(super.worldObj, var24, l1, j1, true);
                     }
                  }
               }
            }
         }
      }

   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 2.0F;
   }

   public boolean interactFirst(EntityPlayer player) {
      if(player != null) {
         this.openInventory(player);
      }

      return true;
   }

   public void setDamageTaken(int par1) {
      super.dataWatcher.updateObject(19, Integer.valueOf(par1));
   }

   public int getDamageTaken() {
      return super.dataWatcher.getWatchableObjectInt(19);
   }

   public void setTimeSinceHit(int par1) {
      super.dataWatcher.updateObject(17, Integer.valueOf(par1));
   }

   public int getTimeSinceHit() {
      return super.dataWatcher.getWatchableObjectInt(17);
   }

   public void setForwardDirection(int par1) {
      super.dataWatcher.updateObject(18, Integer.valueOf(par1));
   }

   public int getForwardDirection() {
      return super.dataWatcher.getWatchableObjectInt(18);
   }

   public boolean canRideAircraft(MCH_EntityAircraft ac, int seatID, MCH_SeatRackInfo info) {
      String[] arr$ = info.names;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String s = arr$[i$];
         if(s.equalsIgnoreCase("container")) {
            return ac.ridingEntity == null && super.ridingEntity == null;
         }
      }

      return false;
   }

   public boolean isSkipNormalRender() {
      return super.ridingEntity instanceof MCH_EntitySeat;
   }
}
