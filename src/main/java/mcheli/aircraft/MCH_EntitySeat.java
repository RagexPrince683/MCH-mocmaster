package mcheli.aircraft;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import mcheli.MCH_Lib;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class MCH_EntitySeat extends W_Entity implements IEntityAdditionalSpawnData {
   public String parentUniqueID;
   private MCH_EntityBaseVehicle parent;
   private int parentEntityID;
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
      this.parentEntityID = -1;
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
      return null;
   }

   public AxisAlignedBB getBoundingBox() {
      return null;
   }

   public boolean canBePushed() {
      return false;
   }

   public double getMountedYOffset() {
      return -0.3D;
   }

   public boolean attackEntityFrom(DamageSource damageSource, float amount) {
      Entity sourceEntity = damageSource.getEntity();

      // hopefully* Prevent rider from damaging their self (and fatally erroring)
      //**does not work
      //if (sourceEntity != null && sourceEntity == this.riddenByEntity) {
      //   return false;
      //}

      // Pass damage to the aircraft if valid
      return getParent() != null && getParent().attackEntityFrom(damageSource, amount);
   }

   @Override
   public boolean hitByEntity(Entity entity) {
      // hopefully* Prevent player from hitting the seat they are riding
      //**also does not work
      //if (entity == this.riddenByEntity) {
      //   return false;
      //}
      return super.hitByEntity(entity);
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
      //if (this.lastRiddenByEntity instanceof EntityPlayer && this.riddenByEntity == null) {
      //   System.out.println("this.lastRiddenByEntity instanceof EntityPlayer && this.riddenByEntity == null");
      //   EntityPlayer player = (EntityPlayer) this.lastRiddenByEntity;
//
      //   if (this.parent != null && this.parent.getAcInfo().isNewUAV) {
      //      System.out.println("[NEW UAV] Player dismounted! Teleporting to UAV Station.");
      //      player.setPositionAndUpdate(
      //              MCH_EntityUavStation.storedStationX,
      //              MCH_EntityUavStation.storedStationY,
      //              MCH_EntityUavStation.storedStationZ
      //      );
      //   }
//
      //   this.lastRiddenByEntity = null; // Prevent repeat teleport
      //}
      //well I mean it doesn't work so might as well comment it out.

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
      resolveParentEntity();
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


   private void resolveParentEntity() {
      if(this.parent != null && !this.parent.isDead) {
         return;
      }
      this.parent = null;

      if(this.parentEntityID > 0) {
         Entity entity = this.worldObj.getEntityByID(this.parentEntityID);
         if(entity instanceof MCH_EntityBaseVehicle && !entity.isDead) {
            setParent((MCH_EntityBaseVehicle)entity);
            return;
         }
      }

      if(this.parentUniqueID != null && !this.parentUniqueID.isEmpty()) {
         for(Object object : this.worldObj.loadedEntityList) {
            if(object instanceof MCH_EntityBaseVehicle
                    && this.parentUniqueID.equals(((MCH_EntityBaseVehicle)object).getCommonUniqueId())) {
               setParent((MCH_EntityBaseVehicle)object);
               return;
            }
         }
      }
   }

   public void writeSpawnData(ByteBuf buffer) {
      buffer.writeInt(this.parent != null ? this.parent.getEntityId() : this.parentEntityID);
      buffer.writeInt(this.seatID);
      byte[] commonId = this.parentUniqueID == null
              ? new byte[0] : this.parentUniqueID.getBytes(StandardCharsets.UTF_8);
      buffer.writeShort(commonId.length);
      buffer.writeBytes(commonId);
   }

   public void readSpawnData(ByteBuf buffer) {
      this.parentEntityID = buffer.readInt();
      this.seatID = buffer.readInt();
      int commonIdLength = buffer.readUnsignedShort();
      if(commonIdLength > 0) {
         byte[] commonId = new byte[commonIdLength];
         buffer.readBytes(commonId);
         this.parentUniqueID = new String(commonId, StandardCharsets.UTF_8);
      } else {
         this.parentUniqueID = "";
      }
      resolveParentEntity();
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
      if(getParent() == null && this.parentUniqueID != null && !this.parentUniqueID.isEmpty()) {
         for(Object object : this.worldObj.loadedEntityList) {
            if(object instanceof MCH_EntityBaseVehicle
                    && this.parentUniqueID.equals(((MCH_EntityBaseVehicle)object).getCommonUniqueId())) {
               setParent((MCH_EntityBaseVehicle)object);
               break;
            }
         }
      }
      MCH_Lib.DbgLog(this.worldObj,
              "[MCH-INTERACT][SEAT-BEGIN] side=%s seatId=%d seatEntity=%d seatUuid=%s parent=%s player=%s playerUuid=%s occupant=%s playerRiding=%s",
              new Object[]{this.worldObj.isRemote?"CLIENT":"SERVER", Integer.valueOf(this.seatID), Integer.valueOf(this.getEntityId()),
                      this.getUniqueID(), this.parent == null?"null":this.parent.getEntityId() + "/" + this.parent.getUniqueID(),
                      player.getCommandSenderName(), player.getUniqueID(), this.riddenByEntity, player.ridingEntity});
      if(getParent() == null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=parent_null seatId=%d parentCommonId=%s",
                 new Object[]{Integer.valueOf(this.seatID), this.parentUniqueID});
         return false;
      }
      if(getParent().isDestroyed()) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=parent_destroyed seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return false;
      }
      ItemStack itemStack = player.getCurrentEquippedItem();
      if(itemStack != null && itemStack.getItem() instanceof mcheli.mob.MCH_ItemSpawnGunner) {
         return getParent().interactFirst(player);
      }
      if(!getParent().checkTeam(player)) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=team_check_failed seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return false;
      }
      if(this.riddenByEntity != null && isInvalidSeatOccupant(this.riddenByEntity)) {
         MCH_Lib.DbgLog(this.worldObj,
                 "[MCH-STATE][REPAIR] context=seat_interact reason=invalid_seat_occupant_backreference side=%s seatId=%d staleOccupantId=%d staleOccupantUuid=%s dead=%s",
                 new Object[]{this.worldObj.isRemote?"CLIENT":"SERVER", Integer.valueOf(this.seatID), Integer.valueOf(this.riddenByEntity.getEntityId()),
                         this.riddenByEntity.getUniqueID(), Boolean.valueOf(this.riddenByEntity.isDead)});
         this.riddenByEntity = null;
      }
      if(this.riddenByEntity != null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=occupied seatId=%d occupantId=%d occupantUuid=%s dead=%s ridingBackref=%s",
                 new Object[]{Integer.valueOf(this.seatID), Integer.valueOf(this.riddenByEntity.getEntityId()), this.riddenByEntity.getUniqueID(),
                         Boolean.valueOf(this.riddenByEntity.isDead), Boolean.valueOf(this.riddenByEntity.ridingEntity == this)});
         return false;
      }
      if(player.ridingEntity != null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=player_already_riding seatId=%d riding=%s",
                 new Object[]{Integer.valueOf(this.seatID), player.ridingEntity});
         return false;
      }
      if(!this.worldObj.isRemote && !getParent().canPlayerEnterVehicle(player)) {
         player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "This vehicle is locked."));
         return false;
      }
      if(!canRideMob(player)) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=seat_is_rack_or_invalid seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return false;
      }
      if(!this.worldObj.isRemote) {
         getParent().clearPlacementMotionLock();
      }
      player.mountEntity(this);
      if(!this.worldObj.isRemote && player.ridingEntity == this && getParent() != null
              && !getParent().isUAV() && !getParent().isNewUAV()) {
         MCH_PacketNotifyOnMountEntity.sendToRider(getParent(), player, this.seatID + 1);
      }
      return true;
   }

   private boolean isInvalidSeatOccupant(Entity occupant) {
      return occupant == null || occupant.isDead || occupant.ridingEntity != this
              || (!this.worldObj.isRemote && !this.worldObj.loadedEntityList.contains(occupant));
   }

   public MCH_EntityBaseVehicle getParent() {
      return this.parent;
   }

   public void setParent(MCH_EntityBaseVehicle parent) {
      this.parent = parent;
      if(parent != null) {
         this.parentEntityID = parent.getEntityId();
         this.parentUniqueID = parent.getCommonUniqueId();
      }
   }
}
