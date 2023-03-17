package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.tool.MCH_ItemWrench;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class MCH_EntitySeat extends W_Entity {

   public String parentUniqueID;
   private MCH_EntityAircraft parent;
   public int seatID;
   public int parentSearchCount;
   protected Entity lastRiddenByEntity;
   public static final float BB_SIZE = 1.0F;


   public MCH_EntitySeat(World world) {
      super(world);
      this.setSize(1.0F, 1.0F);
      super.yOffset = 0.0F;
      super.motionX = 0.0D;
      super.motionY = 0.0D;
      super.motionZ = 0.0D;
      this.seatID = -1;
      this.setParent((MCH_EntityAircraft)null);
      this.parentSearchCount = 0;
      this.lastRiddenByEntity = null;
      super.ignoreFrustumCheck = true;
      super.isImmuneToFire = true;
   }

   public MCH_EntitySeat(World world, double x, double y, double z) {
      this(world);
      this.setPosition(x, y + 1.0D, z);
      super.prevPosX = x;
      super.prevPosY = y + 1.0D;
      super.prevPosZ = z;
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
      return false;
   }

   public double getMountedYOffset() {
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
      return this.getParent() != null?this.getParent().attackEntityFrom(par1DamageSource, par2):false;
   }

   public boolean canBeCollidedWith() {
      return !super.isDead;
   }

   @SideOnly(Side.CLIENT)
   public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {}

   public void setDead() {
      super.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      super.fallDistance = 0.0F;
      if(super.riddenByEntity != null) {
         super.riddenByEntity.fallDistance = 0.0F;
      }

      if(this.lastRiddenByEntity == null && super.riddenByEntity != null) {
         if(this.getParent() != null) {
            MCH_Lib.DbgLog(super.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", new Object[]{Integer.valueOf(this.seatID), super.riddenByEntity.toString()});
            this.getParent().onMountPlayerSeat(this, super.riddenByEntity);
         }
      } else if(this.lastRiddenByEntity != null && super.riddenByEntity == null && this.getParent() != null) {
         MCH_Lib.DbgLog(super.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", new Object[]{Integer.valueOf(this.seatID), this.lastRiddenByEntity.toString()});
         this.getParent().onUnmountPlayerSeat(this, this.lastRiddenByEntity);
      }

      if(super.worldObj.isRemote) {
         this.onUpdate_Client();
      } else {
         this.onUpdate_Server();
      }

      this.lastRiddenByEntity = super.riddenByEntity;
   }

   private void onUpdate_Client() {
      this.checkDetachmentAndDelete();
   }

   private void onUpdate_Server() {
      this.checkDetachmentAndDelete();
      if(super.riddenByEntity != null && super.riddenByEntity.isDead) {
         super.riddenByEntity = null;
      }

   }

   public void updateRiderPosition() {
      this.updatePosition();
   }

   public void updatePosition() {
      Entity ridEnt = super.riddenByEntity;
      if(ridEnt != null) {
         ridEnt.setPosition(super.posX, super.posY, super.posZ);
         ridEnt.motionX = ridEnt.motionY = ridEnt.motionZ = 0.0D;
      }

   }

   public void updateRotation(float yaw, float pitch) {
      Entity ridEnt = super.riddenByEntity;
      if(ridEnt != null) {
         ridEnt.rotationYaw = yaw;
         ridEnt.rotationPitch = pitch;
      }

   }

   protected void checkDetachmentAndDelete() {
      if(!super.isDead && (this.seatID < 0 || this.getParent() == null || this.getParent().isDead)) {
         if(this.getParent() != null && this.getParent().isDead) {
            this.parentSearchCount = 100000000;
         }

         if(this.parentSearchCount >= 1200) {
            this.setDead();
            if(!super.worldObj.isRemote && super.riddenByEntity != null) {
               super.riddenByEntity.mountEntity((Entity)null);
            }

            this.setParent((MCH_EntityAircraft)null);
            MCH_Lib.DbgLog(super.worldObj, "[Error]seat=%d, parentUniqueID=%s", new Object[]{Integer.valueOf(this.seatID), this.parentUniqueID});
         } else {
            ++this.parentSearchCount;
         }
      } else {
         this.parentSearchCount = 0;
      }

   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      par1NBTTagCompound.setInteger("SeatID", this.seatID);
      par1NBTTagCompound.setString("ParentUniqueID", this.parentUniqueID);
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      this.seatID = par1NBTTagCompound.getInteger("SeatID");
      this.parentUniqueID = par1NBTTagCompound.getString("ParentUniqueID");
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean canRideMob(Entity entity) {
      return this.getParent() != null && this.seatID >= 0?!(this.getParent().getSeatInfo(this.seatID + 1) instanceof MCH_SeatRackInfo):false;
   }

   public boolean isGunnerMode() {
      return super.riddenByEntity != null && this.getParent() != null?this.getParent().getIsGunnerMode(super.riddenByEntity):false;
   }

   public boolean interactFirst(EntityPlayer player) {
      if(this.getParent() != null && !this.getParent().isDestroyed()) {
         if(!this.getParent().checkTeam(player)) {
            return false;
         } else {
            ItemStack itemStack = player.getCurrentEquippedItem();
            if(itemStack != null && itemStack.getItem() instanceof MCH_ItemWrench) {
               return this.getParent().interactFirst(player);
            } else if(super.riddenByEntity != null) {
               return false;
            } else if(player.ridingEntity != null) {
               return false;
            } else if(!this.canRideMob(player)) {
               return false;
            } else {
               player.mountEntity(this);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public MCH_EntityAircraft getParent() {
      return this.parent;
   }

   public void setParent(MCH_EntityAircraft parent) {
      this.parent = parent;
   }
}
