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
   private int interactionDebugLoadTicks = -1;

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

      if(this.interactionDebugLoadTicks >= 0 && this.interactionDebugLoadTicks < 40) {
         MCH_Lib.DbgLog(this.worldObj, "[MCHeliSeatLoadTick] tick=%d %s",
                 new Object[]{Integer.valueOf(this.interactionDebugLoadTicks), this.getInteractionDebugSnapshot(null)});
         ++this.interactionDebugLoadTicks;
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
      MCH_Lib.DbgLog(this.worldObj, "[MCHeliSeatNBTWrite] %s", new Object[]{this.getInteractionDebugSnapshot(null)});
      nbt.setInteger("SeatID", this.seatID);
      nbt.setString("ParentUniqueID", this.parentUniqueID);
   }

   protected void readEntityFromNBT(NBTTagCompound nbt) {
      this.seatID = nbt.getInteger("SeatID");
      this.parentUniqueID = nbt.getString("ParentUniqueID");
      this.interactionDebugLoadTicks = 0;
      MCH_Lib.DbgLog(this.worldObj, "[MCHeliSeatNBTRead] %s", new Object[]{this.getInteractionDebugSnapshot(null)});
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

   public String getInteractionDebugSnapshot(EntityPlayer player) {
      String side = this.worldObj != null && this.worldObj.isRemote?"CLIENT":"SERVER";
      int dimension = this.worldObj != null && this.worldObj.provider != null?this.worldObj.provider.dimensionId:Integer.MIN_VALUE;
      MCH_EntityAircraft aircraft = this.getParent();
      return String.format(java.util.Locale.ROOT,
              "side=%s seatId=%d id=%d uuid=%s dimension=%d pos=%.3f,%.3f,%.3f player=%s playerUuid=%s parent=%s parentCommonId=%s linkedToExpectedParent=%s occupant=%s occupantDead=%s occupantBackref=%s ridingEntity=%s isDead=%s rack=%s",
              side, Integer.valueOf(this.seatID), Integer.valueOf(this.getEntityId()), this.getUniqueID(), Integer.valueOf(dimension),
              Double.valueOf(this.posX), Double.valueOf(this.posY), Double.valueOf(this.posZ),
              player == null?"null":player.getCommandSenderName(), player == null?"null":player.getUniqueID(),
              aircraft == null?"null":aircraft.getClass().getSimpleName() + "#" + aircraft.getEntityId(), this.parentUniqueID,
              Boolean.valueOf(aircraft != null && this.parentUniqueID != null && this.parentUniqueID.equals(aircraft.getCommonUniqueId())),
              this.riddenByEntity, Boolean.valueOf(this.riddenByEntity != null && this.riddenByEntity.isDead),
              Boolean.valueOf(this.riddenByEntity != null && this.riddenByEntity.ridingEntity == this), this.ridingEntity,
              Boolean.valueOf(this.isDead), Boolean.valueOf(aircraft != null && aircraft.getSeatInfo(this.seatID + 1) instanceof MCH_SeatRackInfo));
   }

   private boolean rejectInteraction(EntityPlayer player, String reason) {
      MCH_Lib.DbgLog(this.worldObj, "[MCHeliInteractReject] %s reason=%s",
              new Object[]{this.getInteractionDebugSnapshot(player), reason});
      return false;
   }

   public boolean interactFirst(EntityPlayer player) {
      MCH_Lib.DbgLog(this.worldObj, "[MCHeliInteractDebug] %s reason=SEAT_INTERACT_FIRST_ENTERED", new Object[]{this.getInteractionDebugSnapshot(player)});
      MCH_Lib.DbgLog(this.worldObj,
              "[MCH-INTERACT][SEAT-BEGIN] side=%s seatId=%d seatEntity=%d seatUuid=%s parent=%s player=%s playerUuid=%s occupant=%s playerRiding=%s",
              new Object[]{this.worldObj.isRemote?"CLIENT":"SERVER", Integer.valueOf(this.seatID), Integer.valueOf(this.getEntityId()),
                      this.getUniqueID(), this.parent == null?"null":this.parent.getEntityId() + "/" + this.parent.getUniqueID(),
                      player.getCommandSenderName(), player.getUniqueID(), this.riddenByEntity, player.ridingEntity});
      if(getParent() == null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=parent_null seatId=%d parentCommonId=%s",
                 new Object[]{Integer.valueOf(this.seatID), this.parentUniqueID});
         return this.rejectInteraction(player, "SEAT_PARENT_NULL");
      }
      if(getParent().isDestroyed()) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=parent_destroyed seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return this.rejectInteraction(player, "PARENT_DESTROYED");
      }
      ItemStack itemStack = player.getCurrentEquippedItem();
      if(itemStack != null && itemStack.getItem() instanceof mcheli.mob.MCH_ItemSpawnGunner) {
         return getParent().interactFirst(player);
      }
      if(!getParent().checkTeam(player)) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=team_check_failed seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return this.rejectInteraction(player, "TEAM_CHECK_FAILED");
      }
      if(!this.worldObj.isRemote && this.riddenByEntity != null
              && (this.riddenByEntity.isDead || this.riddenByEntity.ridingEntity != this
              || !this.worldObj.loadedEntityList.contains(this.riddenByEntity))) {
         MCH_Lib.DbgLog(this.worldObj,
                 "[MCH-STATE][REPAIR] context=seat_interact reason=invalid_seat_occupant_backreference seatId=%d staleOccupantId=%d staleOccupantUuid=%s dead=%s",
                 new Object[]{Integer.valueOf(this.seatID), Integer.valueOf(this.riddenByEntity.getEntityId()),
                         this.riddenByEntity.getUniqueID(), Boolean.valueOf(this.riddenByEntity.isDead)});
         this.riddenByEntity = null;
      }
      if(this.riddenByEntity != null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=occupied seatId=%d occupantId=%d occupantUuid=%s dead=%s ridingBackref=%s",
                 new Object[]{Integer.valueOf(this.seatID), Integer.valueOf(this.riddenByEntity.getEntityId()), this.riddenByEntity.getUniqueID(),
                         Boolean.valueOf(this.riddenByEntity.isDead), Boolean.valueOf(this.riddenByEntity.ridingEntity == this)});
         return this.rejectInteraction(player, this.riddenByEntity.isDead?"SEAT_OCCUPIED_BY_DEAD_ENTITY":"SEAT_OCCUPIED");
      }
      if(player.ridingEntity != null) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=player_already_riding seatId=%d riding=%s",
                 new Object[]{Integer.valueOf(this.seatID), player.ridingEntity});
         return this.rejectInteraction(player, "PLAYER_ALREADY_RIDING");
      }
      if(!canRideMob(player)) {
         MCH_Lib.DbgLog(this.worldObj, "[MCH-INTERACT][SEAT-REJECT] reason=seat_is_rack_or_invalid seatId=%d", new Object[]{Integer.valueOf(this.seatID)});
         return this.rejectInteraction(player, "SEAT_IS_RACK_OR_INVALID");
      }
      player.mountEntity(this);
      MCH_Lib.DbgLog(this.worldObj, "[MCHeliInteractDebug] %s reason=ALLOW_INTERACT result=SEAT_MOUNT", new Object[]{this.getInteractionDebugSnapshot(player)});
      return true;
   }

   public MCH_EntityAircraft getParent() {
      return this.parent;
   }

   public void setParent(MCH_EntityAircraft parent) {
      this.parent = parent;
   }
}
