package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.uav.MCH_EntityUavStation;
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
      setSize(1.0F, 1.0F);
      this.yOffset = 0.0F;
      this.motionX = this.motionY = this.motionZ = 0.0D;
      this.seatID = -1;
      setParent(null);
      this.parentSearchCount = 0;
      this.lastRiddenByEntity = null;
      this.ignoreFrustumCheck = true;
      this.isImmuneToFire = true;
   }

   public MCH_EntitySeat(World world, double x, double y, double z) {
      this(world);
      setPosition(x, y + 1.0D, z);
      this.prevPosX = x;
      this.prevPosY = y + 1.0D;
      this.prevPosZ = z;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   public AxisAlignedBB getCollisionBox(Entity entity) {
      return entity.boundingBox;
   }

   public AxisAlignedBB getBoundingBox() {
      return this.boundingBox;
   }

   public boolean canBePushed() {
      return false;
   }

   public double getMountedYOffset() {
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource damageSource, float amount) {
      return getParent() != null && getParent().attackEntityFrom(damageSource, amount);
   }

   public boolean canBeCollidedWith() {
      return !this.isDead;
   }

   @SideOnly(Side.CLIENT)
   public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int p_70056_9_) {}

   public void setDead() {
      super.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      this.fallDistance = 0.0F;
      if (this.riddenByEntity != null) {
         this.riddenByEntity.fallDistance = 0.0F;
      }


      // If this seat belongs to a new UAV and the player dismounts, teleport them except it does not work
      if (this.lastRiddenByEntity instanceof EntityPlayer && this.riddenByEntity == null) {
         EntityPlayer player = (EntityPlayer) this.lastRiddenByEntity;

         if (this.parent != null && this.parent.getAcInfo().isNewUAV) {
            System.out.println("[NEW UAV] Player dismounted! Teleporting to UAV Station.");
            player.setPositionAndUpdate(
                    MCH_EntityUavStation.storedStationX,
                    MCH_EntityUavStation.storedStationY,
                    MCH_EntityUavStation.storedStationZ
            );
         }

         this.lastRiddenByEntity = null; // Prevent repeat teleport
      }

      if (this.lastRiddenByEntity == null && this.riddenByEntity != null) {
         if (getParent() != null) {
            MCH_Lib.DbgLog(this.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", this.seatID, this.riddenByEntity.toString());
            getParent().onMountPlayerSeat(this, this.riddenByEntity);
         }
      } else if (this.lastRiddenByEntity != null && this.riddenByEntity == null && getParent() != null) {
         MCH_Lib.DbgLog(this.worldObj, "MCH_EntitySeat.onUpdate:SeatID=%d", this.seatID, this.lastRiddenByEntity.toString());
         getParent().onUnmountPlayerSeat(this, this.lastRiddenByEntity);
      }

      if (this.worldObj.isRemote) {
         onUpdate_Client();
      } else {
         onUpdate_Server();
      }

      this.lastRiddenByEntity = this.riddenByEntity;
   }

   private void onUpdate_Client() {
      checkDetachmentAndDelete();
   }

   private void onUpdate_Server() {
      checkDetachmentAndDelete();
      if (this.riddenByEntity != null && this.riddenByEntity.isDead) {
         this.riddenByEntity = null;
      }
   }

   public void updateRiderPosition() {
      updatePosition();
   }

   public void updatePosition() {
      if (this.riddenByEntity != null) {
         this.riddenByEntity.setPosition(this.posX, this.posY, this.posZ);
         this.riddenByEntity.motionX = this.riddenByEntity.motionY = this.riddenByEntity.motionZ = 0.0D;
      }
   }

   public void updateRotation(float yaw, float pitch) {
      if (this.riddenByEntity != null) {
         this.riddenByEntity.rotationYaw = yaw;
         this.riddenByEntity.rotationPitch = pitch;
      }
   }

   protected void checkDetachmentAndDelete() {
      if (!this.isDead && (this.seatID < 0 || getParent() == null || getParent().isDead)) {
         if (getParent() != null && getParent().isDead) {
            this.parentSearchCount = Integer.MAX_VALUE;
         }

         if (this.parentSearchCount >= 1200) {
            setDead();
            if (!this.worldObj.isRemote && this.riddenByEntity != null) {
               this.riddenByEntity.mountEntity(null);
            }
            setParent(null);
            MCH_Lib.DbgLog(this.worldObj, "[Error] Seat entity deleted: seat=%d, parentUniqueID=%s", this.seatID, this.parentUniqueID);
         } else {
            this.parentSearchCount++;
         }
      } else {
         this.parentSearchCount = 0;
      }
   }

   protected void writeEntityToNBT(NBTTagCompound nbt) {
      nbt.setInteger("SeatID", this.seatID);
      nbt.setString("ParentUniqueID", this.parentUniqueID);
   }

   protected void readEntityFromNBT(NBTTagCompound nbt) {
      this.seatID = nbt.getInteger("SeatID");
      this.parentUniqueID = nbt.getString("ParentUniqueID");
   }

   @SideOnly(Side.CLIENT)
   public float getShadowSize() {
      return 0.0F;
   }

   public boolean canRideMob(Entity entity) {
      return getParent() != null && this.seatID >= 0 && !(getParent().getSeatInfo(this.seatID + 1) instanceof MCH_SeatRackInfo);
   }

   public boolean isGunnerMode() {
      return this.riddenByEntity != null && getParent() != null && getParent().getIsGunnerMode(this.riddenByEntity);
   }

   public boolean interactFirst(EntityPlayer player) {
      if (getParent() != null && !getParent().isDestroyed()) {
         if (!getParent().checkTeam(player)) return false;
         if (this.riddenByEntity != null || player.ridingEntity != null) return false;
         if (!canRideMob(player)) return false;
         player.mountEntity(this);
         return true;
      }
      return false;
   }

   public MCH_EntityAircraft getParent() {
      return this.parent;
   }

   public void setParent(MCH_EntityAircraft parent) {
      this.parent = parent;
   }
}
