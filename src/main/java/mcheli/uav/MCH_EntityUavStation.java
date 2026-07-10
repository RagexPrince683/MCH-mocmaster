package mcheli.uav;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.UUID;
import mcheli.MCH_Config;
import mcheli.MCH_Explosion;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_ItemPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ItemShip;
import mcheli.ship.MCH_ShipInfo;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_ItemTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityContainer;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

public class MCH_EntityUavStation
           extends W_EntityContainer
         {
      protected static final int DATAWT_ID_NEW_UAV_PILOT = 25;
      protected static final int DATAWT_ID_CONTINUE_STATE = 26;
      protected static final int DATAWT_ID_KIND = 27;
      private static final byte CONTINUE_NONE = 0;
      private static final byte CONTINUE_AVAILABLE = 1;
      private static final byte CONTINUE_DESTROYED = 2;
      protected static final int DATAWT_ID_LAST_AC = 28;
      protected static final int DATAWT_ID_UAV_X = 29;
      protected static final int DATAWT_ID_UAV_Y = 30;
      protected static final int DATAWT_ID_UAV_Z = 31;
      protected Entity lastRiddenByEntity;
      public boolean isRequestedSyncStatus;
      @SideOnly(Side.CLIENT)
      protected double velocityX;
      @SideOnly(Side.CLIENT)
      protected double velocityY;
      @SideOnly(Side.CLIENT)
      protected double velocityZ;
      protected int aircraftPosRotInc;
      protected double aircraftX;

      private boolean continuePressed = false;
      public MCH_EntityBaseVehicle assignedUav = null;

      public int assignedUavId = -1; // fallback tracking
      public String assignedUavUUID = "";
      private UUID ownerUUID;
      private UUID linkedUavEntityUUID;
      private String linkedUavCommonId = "";
      private int linkedUavDimension;
      private double linkedUavX;
      private double linkedUavY;
      private double linkedUavZ;
      private boolean hasStoredUavLink;
      private ItemStack lastUavItemStack;
      private boolean hasStoredUavRespawnPosition;
      private double storedUavRespawnX;
      private double storedUavRespawnY;
      private double storedUavRespawnZ;
      private boolean respawnStoredUavAtSavedPosition;
      private boolean awaitingLoadedUav;
      private int pendingContinueTicks;
      private ForgeChunkManager.Ticket reconnectChunkTicket;
      private ChunkCoordIntPair reconnectStationChunk;
      private ChunkCoordIntPair reconnectUavChunk;

      private boolean storedUavWasDestroyed;



             public void setContinuePressed(boolean flag) {
                 this.continuePressed = flag;
             }

             public boolean isContinuePressed() {
                 return this.continuePressed;
             }

      public MCH_EntityUavStation(World world) {
           super(world);
           this.dropContentsWhenDead = false;
           this.preventEntitySpawning = true;
           setSize(2.0F, 0.7F);
           this.yOffset = this.height / 2.0F;
           this.motionX = 0.0D;
           this.motionY = 0.0D;
           this.motionZ = 0.0D;
           this.ignoreFrustumCheck = true;
           this.lastRiddenByEntity = null;
           this.aircraftPosRotInc = 0;
           this.aircraftX = 0.0D;
           this.aircraftY = 0.0D;
           this.aircraftZ = 0.0D;
           this.aircraftYaw = 0.0D;
           this.aircraftPitch = 0.0D;
           this.posUavX = 0;
           this.posUavY = 0;
           this.posUavZ = 0;
           this.rotCover = 0.0F;
           this.prevRotCover = 0.0F;
           setControlAircract((MCH_EntityBaseVehicle)null);
           setLastControlAircraft((MCH_EntityBaseVehicle)null);
           this.loadedLastControlAircraftGuid = "";
           this.linkedUavCommonId = "";
           this.hasStoredUavLink = false;
           this.lastUavItemStack = null;
           this.hasStoredUavRespawnPosition = false;
           this.respawnStoredUavAtSavedPosition = false;
           this.awaitingLoadedUav = false;
           this.pendingContinueTicks = 0;
           this.storedUavWasDestroyed = false;
         }
      protected double aircraftY;
      protected double aircraftZ;
      protected double aircraftYaw;
      protected double aircraftPitch;
      private MCH_EntityBaseVehicle controlAircraft;
      private MCH_EntityBaseVehicle lastControlAircraft;
      private String loadedLastControlAircraftGuid;
      public int posUavX;
      public int posUavY;
      public int posUavZ;
      public float rotCover;
      public float prevRotCover;
      protected void entityInit() {
           super.entityInit();
           getDataWatcher().addObject(DATAWT_ID_NEW_UAV_PILOT, "");
           getDataWatcher().addObject(DATAWT_ID_CONTINUE_STATE, Byte.valueOf(CONTINUE_NONE));
           getDataWatcher().addObject(27, Byte.valueOf((byte)0));
           getDataWatcher().addObject(28, Integer.valueOf(0));
           getDataWatcher().addObject(29, Integer.valueOf(0));
           getDataWatcher().addObject(30, Integer.valueOf(0));
           getDataWatcher().addObject(31, Integer.valueOf(0));
           setOpen(true);
         }

      public int getStatus() {
           return getDataWatcher().getWatchableObjectByte(27);
         }

      public void setStatus(int n) {
           if (!this.worldObj.isRemote) {
                MCH_Lib.DbgLog(this.worldObj, "MCH_EntityUavStation.setStatus(%d)", new Object[] { Integer.valueOf(n) });
                getDataWatcher().updateObject(27, Byte.valueOf((byte)n));
              }
         }


      public int getKind() {
           return 0x7F & getStatus();
         }

      public void setKind(int n) {
           setStatus(getStatus() & 0x80 | n);
         }

      public boolean isOpen() {
           return ((getStatus() & 0x80) != 0);
         }

      public void setOpen(boolean b) {
           setStatus((b ? 128 : 0) | getStatus() & 0x7F);
         }

      public MCH_EntityBaseVehicle getControlAircract() {
           return this.controlAircraft;
         }

      public void setControlAircract(MCH_EntityBaseVehicle ac) {
           if (ac == null) {
                this.controlAircraft = null;
           } else if (!ac.isDead && linkUav(ac)) {
                this.controlAircraft = ac;
                setLastControlAircraft(ac);
           }
         }

      public UUID getOwnerUUID() {
           return this.ownerUUID;
         }

      public void setOwnerUUID(UUID uuid) {
           this.ownerUUID = uuid;
         }



      public boolean linkUav(MCH_EntityBaseVehicle ac) {
           if(ac == null) {
                return false;
           }
           UUID persistentId = ac.getUavPersistentUUID();
           if(this.hasStoredUavLink && this.linkedUavEntityUUID != null
                 && !this.linkedUavEntityUUID.equals(persistentId)
                 && !this.linkedUavEntityUUID.equals(ac.getUniqueID())) {
                MCH_Lib.Log((Entity)this, "Rejected UAV %d because station %d is locked to persistent UUID %s", new Object[] {
                      Integer.valueOf(W_Entity.getEntityId((Entity)ac)), Integer.valueOf(W_Entity.getEntityId((Entity)this)),
                      this.linkedUavEntityUUID.toString() });
                return false;
           }
           if(!isUavLinkedToThisStation(ac)) {
                MCH_Lib.Log((Entity)this, "Rejected UAV %d because it belongs to another UAV station", new Object[] {
                      Integer.valueOf(W_Entity.getEntityId((Entity)ac)) });
                return false;
           }
           MCH_UavRegistry.register(ac);
           if(ac.isDead) {
                return false;
           }
           this.assignedUav = ac;
           this.assignedUavId = ac.getEntityId();
           this.assignedUavUUID = persistentId == null ? ac.getUniqueID().toString() : persistentId.toString();
           this.linkedUavEntityUUID = persistentId == null ? ac.getUniqueID() : persistentId;
           this.linkedUavCommonId = ac.getCommonUniqueId() == null ? "" : ac.getCommonUniqueId();
           updateLinkedUavPosition(ac);
           ac.setUavStation(this);
           if(this.ownerUUID != null) {
                ac.setOwnerUUID(this.ownerUUID);
           }
           this.hasStoredUavLink = true;
           this.awaitingLoadedUav = false;
           setContinuationState(CONTINUE_AVAILABLE);
           setLastControlAircraftEntityId(W_Entity.getEntityId((Entity)ac));
           return true;
         }

      public void updateLinkedUavPosition(MCH_EntityBaseVehicle ac) {
           if(ac != null) {
                this.linkedUavDimension = ac.dimension;
                this.linkedUavX = ac.posX;
                this.linkedUavY = ac.posY;
                this.linkedUavZ = ac.posZ;
           }
         }

      public boolean hasContinuableUavLink() {
           byte state = getContinuationState();
           if(this.worldObj.isRemote) {
                return state == CONTINUE_AVAILABLE;
           }
           if(this.storedUavWasDestroyed || state == CONTINUE_DESTROYED) {
                return false;
           }
           boolean available = getLastControlAircraftEntityId().intValue() != 0 ||
                  (this.assignedUav != null && !this.assignedUav.isDead) ||
                  this.assignedUavId > 0 ||
                  (this.assignedUavUUID != null && !this.assignedUavUUID.isEmpty()) ||
                  this.linkedUavEntityUUID != null ||
                  (this.linkedUavCommonId != null && !this.linkedUavCommonId.isEmpty()) ||
                  (this.loadedLastControlAircraftGuid != null && !this.loadedLastControlAircraftGuid.isEmpty()) ||
                  this.lastUavItemStack != null ||
                  MCH_UavJsonStore.load(this.worldObj, this) != null;
           setContinuationState(available ? CONTINUE_AVAILABLE : CONTINUE_NONE);
           return available;
         }

      public boolean wasLinkedUavDestroyed() {
           return this.storedUavWasDestroyed || getContinuationState() == CONTINUE_DESTROYED;
         }

      private byte getContinuationState() {
           return getDataWatcher().getWatchableObjectByte(DATAWT_ID_CONTINUE_STATE);
         }

      private void setContinuationState(byte state) {
           if(!this.worldObj.isRemote) {
                getDataWatcher().updateObject(DATAWT_ID_CONTINUE_STATE, Byte.valueOf(state));
           }
         }

      public void unlinkInvalidUav() {
           this.assignedUav = null;
           this.assignedUavId = -1;
           this.assignedUavUUID = "";
           this.linkedUavEntityUUID = null;
           this.linkedUavCommonId = "";
           this.hasStoredUavLink = false;
           this.lastUavItemStack = null;
           this.hasStoredUavRespawnPosition = false;
           this.respawnStoredUavAtSavedPosition = false;
           this.awaitingLoadedUav = false;
           this.pendingContinueTicks = 0;
           this.controlAircraft = null;
           setLastControlAircraft((MCH_EntityBaseVehicle)null);
           setLastControlAircraftEntityId(0);
           setContinuationState(CONTINUE_NONE);
         }


      public void setUavPosition(int x, int y, int z) {
           if (!this.worldObj.isRemote) {
                this.posUavX = x;
                this.posUavY = y;
                this.posUavZ = z;
                getDataWatcher().updateObject(29, Integer.valueOf(x));
                getDataWatcher().updateObject(30, Integer.valueOf(y));
                getDataWatcher().updateObject(31, Integer.valueOf(z));
              }
         }


      public void updateUavPosition() {
          this.posUavX = getDataWatcher().getWatchableObjectInt(29);
          this.posUavY = getDataWatcher().getWatchableObjectInt(30);
          this.posUavZ = getDataWatcher().getWatchableObjectInt(31);
        }

      protected void writeEntityToNBT(NBTTagCompound nbt) {
           super.writeEntityToNBT(nbt);
           nbt.setInteger("UavStatus", getStatus());
           nbt.setInteger("PosUavX", this.posUavX);
           nbt.setInteger("PosUavY", this.posUavY);
           nbt.setInteger("PosUavZ", this.posUavZ);
           String s = "";
           if (getLastControlAircraft() != null && !(getLastControlAircraft()).isDead) {
                s = getLastControlAircraft().getCommonUniqueId();
              }

           if (s.isEmpty()) {
                s = this.loadedLastControlAircraftGuid;
              }

           nbt.setString("LastCtrlAc", s);
           nbt.setString("OwnerUUID", this.ownerUUID == null ? "" : this.ownerUUID.toString());
           nbt.setString("LinkedUavPersistentUUID", this.linkedUavEntityUUID == null ? "" : this.linkedUavEntityUUID.toString());
           nbt.setString("LinkedUavEntityUUID", this.linkedUavEntityUUID == null ? "" : this.linkedUavEntityUUID.toString());
           nbt.setString("LinkedUavCommonId", this.linkedUavCommonId == null ? "" : this.linkedUavCommonId);
           nbt.setInteger("LinkedUavDimension", this.linkedUavDimension);
           nbt.setBoolean("HasStoredUavLink", this.hasStoredUavLink);
           nbt.setDouble("LinkedUavX", this.linkedUavX);
           nbt.setDouble("LinkedUavY", this.linkedUavY);
           nbt.setDouble("LinkedUavZ", this.linkedUavZ);
           nbt.setBoolean("StoredUavWasDestroyed", this.storedUavWasDestroyed);

          if (this.assignedUav != null && !this.assignedUav.isDead) {
              nbt.setInteger("AssignedUavId", this.assignedUav.getEntityId());
              UUID persistentId = this.assignedUav.getUavPersistentUUID();
              nbt.setString("AssignedUavUUID", persistentId == null ? this.assignedUav.getUniqueID().toString() : persistentId.toString());
          }
         }

      protected void readEntityFromNBT(NBTTagCompound nbt) {
           super.readEntityFromNBT(nbt);
           setUavPosition(nbt.getInteger("PosUavX"), nbt.getInteger("PosUavY"), nbt.getInteger("PosUavZ"));
           if (nbt.hasKey("UavStatus")) {
                setStatus(nbt.getInteger("UavStatus"));
              } else {
                setKind(1);
              }

           this.loadedLastControlAircraftGuid = nbt.getString("LastCtrlAc");
          if (nbt.hasKey("AssignedUavId")) {
              this.assignedUavId = nbt.getInteger("AssignedUavId");
          }
          if (nbt.hasKey("AssignedUavUUID")) {
              this.assignedUavUUID = nbt.getString("AssignedUavUUID");
          }
          this.ownerUUID = parseUavUUID(nbt.getString("OwnerUUID"));
          this.linkedUavEntityUUID = parseUavUUID(nbt.getString("LinkedUavPersistentUUID"));
          if(this.linkedUavEntityUUID == null) {
              this.linkedUavEntityUUID = parseUavUUID(nbt.getString("LinkedUavEntityUUID"));
          }
          this.linkedUavCommonId = nbt.getString("LinkedUavCommonId");
          this.linkedUavDimension = nbt.getInteger("LinkedUavDimension");
          this.hasStoredUavLink = nbt.getBoolean("HasStoredUavLink");
          this.hasStoredUavRespawnPosition = false;
          this.linkedUavX = nbt.getDouble("LinkedUavX");
          this.linkedUavY = nbt.getDouble("LinkedUavY");
          this.linkedUavZ = nbt.getDouble("LinkedUavZ");
          this.lastUavItemStack = null;
          this.storedUavWasDestroyed = nbt.getBoolean("StoredUavWasDestroyed");

          if(this.storedUavWasDestroyed) {
              this.lastUavItemStack = null;
              this.hasStoredUavRespawnPosition = false;
              this.respawnStoredUavAtSavedPosition = false;
          }
          if(this.linkedUavEntityUUID == null) {
              this.linkedUavEntityUUID = parseUavUUID(this.assignedUavUUID);
          }
          boolean hasLinkedUavIdentity = !this.storedUavWasDestroyed &&
                  (this.linkedUavEntityUUID != null ||
                  (this.linkedUavCommonId != null && !this.linkedUavCommonId.isEmpty()) ||
                  this.assignedUavId > 0 ||
                  (this.assignedUavUUID != null && !this.assignedUavUUID.isEmpty()) ||
                  (this.loadedLastControlAircraftGuid != null && !this.loadedLastControlAircraftGuid.isEmpty()) ||
                  this.lastUavItemStack != null ||
                  (!this.worldObj.isRemote && MCH_UavJsonStore.load(this.worldObj, this) != null));
          if(this.lastUavItemStack != null && !this.hasStoredUavRespawnPosition) {
              this.hasStoredUavRespawnPosition = this.linkedUavY != 0.0D || this.linkedUavX != 0.0D || this.linkedUavZ != 0.0D;
          }
          this.awaitingLoadedUav = hasLinkedUavIdentity;
          this.hasStoredUavLink = hasLinkedUavIdentity;
          setContinuationState(this.storedUavWasDestroyed ? CONTINUE_DESTROYED : (hasLinkedUavIdentity ? CONTINUE_AVAILABLE : CONTINUE_NONE));
          if(this.awaitingLoadedUav) {
              setLastControlAircraftEntityId(-1);
          }
         }




      private void detachLinkedUavForStationDestruction(MCH_EntityBaseVehicle linkedUav) {
           if(this.worldObj.isRemote || linkedUav == null) {
                return;
           }

           Entity rider = linkedUav.getRiddenByEntity();
           if(rider != null) {
                rider.mountEntity((Entity)null);
                double x = this.posX - Math.sin(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                double z = this.posZ + Math.cos(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                double y = this.posY + getMountedYOffset() + rider.getYOffset();
                if(rider instanceof EntityPlayerMP) {
                     EntityPlayerMP player = (EntityPlayerMP)rider;
                     W_EntityPlayer.closeScreen(player);
                     player.setPositionAndUpdate(x, y, z);
                     if(linkedUav.isNewUAV()) {
                          MCH_UavInventory.restorePilotInventory(player, "station_destroyed");
                     }
                } else {
                     rider.setPosition(x, y, z);
                }
                rider.fallDistance = 0.0F;
           }

           linkedUav.unlinkUavStation();
           MCH_UavJsonStore.remove(this.worldObj, this);
           releaseReconnectChunks("station-destroyed");
           this.riddenByEntity = null;
           this.lastRiddenByEntity = null;
           this.assignedUav = null;
           this.assignedUavId = -1;
           this.assignedUavUUID = "";
           this.linkedUavEntityUUID = null;
           this.linkedUavCommonId = "";
           this.hasStoredUavLink = false;
           this.awaitingLoadedUav = false;
           this.pendingContinueTicks = 0;
           this.controlAircraft = null;
           setLastControlAircraft((MCH_EntityBaseVehicle)null);
           setLastControlAircraftEntityId(0);
           setNewUavPilotProfile((EntityPlayer)null);
           setContinuationState(CONTINUE_NONE);
      }

      public void initUavPostion() {
           int rt = (int)(MCH_Lib.getRotate360((this.rotationYaw + 45.0F)) / 90.0D);
           boolean D = true;
           this.posUavX = (rt != 0 && rt != 3) ? -12 : 12;
           this.posUavZ = (rt != 0 && rt != 1) ? -12 : 12;
           this.posUavY = 2;
           setUavPosition(this.posUavX, this.posUavY, this.posUavZ);
         }

             @Override
             public boolean attackEntityFrom(DamageSource damageSource, float damage) {
                 if (isEntityInvulnerable()) return false;
                 if (this.isDead) return true;
                 if (this.worldObj.isRemote) return true;



                 // Apply external damage modifications
                 String damageType = damageSource.getDamageType();
                 damage = MCH_Config.applyDamageByExternal(this, damageSource, damage);

                 // Disallow if not attackable
                 if (!MCH_Multiplay.canAttackEntity(damageSource, this)) return false;

                 // Attacker info
                 Entity attacker = damageSource.getEntity();
                 boolean isCreative = false;
                 boolean isPlayerSource = false;

                 if (attacker instanceof EntityPlayer) {
                     EntityPlayer player = (EntityPlayer) attacker;
                     isCreative = player.capabilities.isCreativeMode;
                     if ("player".equals(damageType)) isPlayerSource = true;

                     W_WorldFunc.MOD_playSoundAtEntity(this, "hit", 1.0F, 1.0F);
                 } else {
                     W_WorldFunc.MOD_playSoundAtEntity(this, "helidmg", 1.0F, 0.9F + this.rand.nextFloat() * 0.1F);
                 }

                 setBeenAttacked();

                 if (damage > 0.0F) {
                     MCH_EntityBaseVehicle linkedUav = this.assignedUav;
                     if(linkedUav == null || linkedUav.isDead) {
                         linkedUav = this.controlAircraft;
                     }
                     if(linkedUav == null || linkedUav.isDead) {
                         linkedUav = findLinkedUavEntity(this.worldObj);
                     }
                     if(linkedUav != null && !linkedUav.isDead) {
                         detachLinkedUavForStationDestruction(linkedUav);
                     }

                     // Handle station death
                     this.dropContentsWhenDead = true;

                     setDead();

                     // Explosion if not caused by player
                     if (!isPlayerSource) {
                         System.out.println("explosion created");
                         MCH_Explosion.newExplosion(
                                 this.worldObj, null, this.riddenByEntity,
                                 this.posX, this.posY, this.posZ,
                                 1.0F, 0.0F,
                                 true, true, false, false, 0
                         );
                     }

                     // Drop station item if not creative
                     if (!isCreative) {
                         int kind = getKind();
                         if (kind > 0) {
                             dropItemWithOffset(MCH_MOD.itemUavStation[kind - 1], 1, 0.0F);
                         }
                     }
                 }

                 return true;
             }





             protected boolean canTriggerWalking() {
           return false;
         }

      public AxisAlignedBB getCollisionBox(Entity par1Entity) {
           return par1Entity.boundingBox;
         }

      public AxisAlignedBB getBoundingBox() {
           return this.boundingBox;
         }

      public boolean canBePushed() {
           return false;
         }

      public double getMountedYOffset() {
           return getMountedYOffset(this.riddenByEntity != null);
         }

      @SideOnly(Side.CLIENT)
      public double getMountedYOffsetForVisualPilot() {
           return getMountedYOffset(true);
         }

      private double getMountedYOffset(boolean occupied) {
           if (getKind() == 2 && occupied) {
                double px = -Math.sin(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                double pz = Math.cos(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                int x = (int)(this.posX + px);
                int y = (int)(this.posY - 0.5D);
                int z = (int)(this.posZ + pz);
                Block block = this.worldObj.getBlock(x, y, z);
                return block.isOpaqueCube() ? -0.4D : -0.9D;
              }
           return 0.35D;
         }


      @SideOnly(Side.CLIENT)
      public float getShadowSize() {
           return 2.0F;
         }

      public boolean canBeCollidedWith() {
           return !this.isDead;
         }


      public void applyEntityCollision(Entity par1Entity) {}


      public void addVelocity(double par1, double par3, double par5) {}

      @SideOnly(Side.CLIENT)
      public void setVelocity(double par1, double par3, double par5) {
           this.velocityX = this.motionX = par1;
           this.velocityY = this.motionY = par3;
           this.velocityZ = this.motionZ = par5;
         }

      public void onUpdate() {

          //I don't know if this should go in the EntityAircraft class or this class's onupdate method
          // but fuck you here you go!
          if (!this.worldObj.isRemote) {
              relinkStoredUav(false);
          }

          if (this.assignedUav == null && this.assignedUavId > 0 && !this.worldObj.isRemote) {
              Entity e = this.worldObj.getEntityByID(this.assignedUavId);
              if (e instanceof MCH_EntityBaseVehicle) {
                  linkUav((MCH_EntityBaseVehicle)e);
              }
              else if (!this.assignedUavUUID.isEmpty()) {
                  for (Object obj : this.worldObj.loadedEntityList) {
                      if (obj instanceof MCH_EntityBaseVehicle) {
                          MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)obj;
                          UUID persistentId = ac.getUavPersistentUUID();
                          if (ac.getUniqueID().toString().equals(this.assignedUavUUID) || (persistentId != null && persistentId.toString().equals(this.assignedUavUUID))) {
                              linkUav(ac);
                              break;
                          }
                      }
                  }
              }
          }

          EntityPlayer player = (EntityPlayer)this.riddenByEntity;
           super.onUpdate();
           this.prevRotCover = this.rotCover;
           if (isOpen()) {
                if (this.rotCover < 1.0F) {
                     this.rotCover += 0.1F;
                   } else {
                     this.rotCover = 1.0F;
                   }
              } else if (this.rotCover > 0.0F) {
                this.rotCover -= 0.1F;
              } else {
                this.rotCover = 0.0F;
              }

          if(!this.worldObj.isRemote && this.riddenByEntity instanceof EntityPlayer
                  && this.controlAircraft != null && this.controlAircraft.isNewUAV()) {
              this.controlAircraft.setUavStation(this);
          }
          if(!this.worldObj.isRemote) {
              updateNewUavPilotProfile();
          }



           if (this.riddenByEntity == null &&
                     this.lastRiddenByEntity != null) {
                unmountEntity(true);

              }




           int uavStationKind = getKind();
           if (this.ticksExisted >= 30 || uavStationKind <= 0 || uavStationKind == 1 || uavStationKind == 2);



           if (this.worldObj.isRemote && !this.isRequestedSyncStatus) {
                this.isRequestedSyncStatus = true;
              }

           this.prevPosX = this.posX;
           this.prevPosY = this.posY;
           this.prevPosZ = this.posZ;
           if(!this.worldObj.isRemote && this.ticksExisted % 10 == 0 && !this.storedUavWasDestroyed &&
              (this.hasStoredUavLink || getContinuationState() == CONTINUE_AVAILABLE) &&
              MCH_UavJsonStore.consumeDestroyed(this.worldObj, this)) {
                MCH_Lib.Log((Entity)this, "Consumed out-of-range New UAV destruction signal; disabling Continue", new Object[0]);
                markLinkedNewUavDestroyed((MCH_EntityBaseVehicle)null);
           }
           if (getControlAircract() != null && getControlAircract().isDestroyed()) {
                MCH_EntityBaseVehicle destroyed = getControlAircract();
                MCH_Lib.Log((Entity)this, "Linked UAV %d is destroyed; marking station Continue state destroyed", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)destroyed)) });
                if(destroyed.isNewUAV()) {
                     markLinkedNewUavDestroyed(destroyed);
                } else {
                     unlinkInvalidUav();
                }
              } else if (getControlAircract() != null && getControlAircract().isDead) {
                markLinkedUavUnloaded();
                setControlAircract((MCH_EntityBaseVehicle)null);
              }

           if (getLastControlAircraft() != null && getLastControlAircraft().isDestroyed()) {
                MCH_EntityBaseVehicle destroyed = getLastControlAircraft();
                MCH_Lib.Log((Entity)this, "Last linked UAV %d is destroyed; marking station Continue state destroyed", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)destroyed)) });
                if(destroyed.isNewUAV()) {
                     markLinkedNewUavDestroyed(destroyed);
                } else {
                     unlinkInvalidUav();
                }
              } else if (getLastControlAircraft() != null && getLastControlAircraft().isDead) {
                markLinkedUavUnloaded();
                setLastControlAircraft((MCH_EntityBaseVehicle)null);
              }

           if (this.worldObj.isRemote) {
                onUpdate_Client();
              } else {
                onUpdate_Server();
              }

           this.lastRiddenByEntity = this.riddenByEntity;
         }

      public MCH_EntityBaseVehicle getLastControlAircraft() {
           return this.lastControlAircraft;
         }

      public MCH_EntityBaseVehicle getAndSearchLastControlAircraft() {
           if (getLastControlAircraft() == null && !this.worldObj.isRemote) {
                relinkStoredUav(false);
              }
           if (getLastControlAircraft() == null) {
                int id = getLastControlAircraftEntityId().intValue();
                if (id > 0) {
                     Entity entity = this.worldObj.getEntityByID(id);
                     if (entity instanceof MCH_EntityBaseVehicle) {
                          MCH_EntityBaseVehicle ac = (MCH_EntityBaseVehicle)entity;
                          if (ac.isUAV() || ac.isNewUAV()) {
                               setLastControlAircraft(ac);
                             }
                        }
                   }
              }

           return getLastControlAircraft();
         }

      public void setLastControlAircraft(MCH_EntityBaseVehicle ac) {
           MCH_Lib.DbgLog(this.worldObj, "MCH_EntityUavStation.setLastControlAircraft:" + ac, new Object[0]);
           this.lastControlAircraft = ac;
           if(ac != null && !this.worldObj.isRemote) {
                setLastControlAircraftEntityId(W_Entity.getEntityId((Entity)ac));
           }
         }

      public Integer getLastControlAircraftEntityId() {
           return Integer.valueOf(getDataWatcher().getWatchableObjectInt(28));
         }

      public void setLastControlAircraftEntityId(int s) {
           if (!this.worldObj.isRemote) {
                getDataWatcher().updateObject(28, Integer.valueOf(s));
              }
         }

      private void markLinkedUavUnloaded() {
           if(!this.worldObj.isRemote && hasContinuableUavLink()) {
                setLastControlAircraftEntityId(-1);
           }
         }



      private UUID parseUavUUID(String value) {
           if(value == null || value.isEmpty()) {
                return null;
           }
           try {
                return UUID.fromString(value);
           } catch (IllegalArgumentException e) {
                return null;
           }
         }

      public MCH_EntityBaseVehicle findLinkedUavEntity(World world) {
           if(this.assignedUav != null && !this.assignedUav.isDead
                 && MCH_UavRegistry.isPresentInLoadedEntityList(world, this.assignedUav)
                 && isUavLinkedToThisStation(this.assignedUav)) {
                return this.assignedUav;
           }
           this.assignedUav = null;
           if(!hasStableLinkedUavIdentity()) {
                return null;
           }
           UUID lookupUuid = this.linkedUavEntityUUID != null ? this.linkedUavEntityUUID : parseUavUUID(this.assignedUavUUID);
           if(lookupUuid == null && (this.linkedUavCommonId == null || this.linkedUavCommonId.isEmpty())) {
                return null;
           }
           MCH_EntityBaseVehicle ac = MCH_UavRegistry.findLinkedUav(world, lookupUuid, this.linkedUavCommonId, null);
           if(ac == null) {
                loadLinkedUavChunk(world);
                ac = MCH_UavRegistry.findLinkedUav(world, lookupUuid, this.linkedUavCommonId, null);
           }
           return isUavLinkedToThisStation(ac) ? ac : null;
         }

      private boolean hasStableLinkedUavIdentity() {
           return this.linkedUavEntityUUID != null ||
                 (this.linkedUavCommonId != null && !this.linkedUavCommonId.isEmpty()) ||
                 (this.assignedUavUUID != null && !this.assignedUavUUID.isEmpty());
      }

      private boolean isUavLinkedToThisStation(MCH_EntityBaseVehicle ac) {
           if(ac == null) {
                return false;
           }
           MCH_EntityUavStation station = ac.getUavStation();
           if(station != null && station != this) {
                return false;
           }
           UUID linkedStationUuid = ac.getLinkedUavStationUUID();
           return linkedStationUuid == null || linkedStationUuid.equals(this.getUniqueID());
         }

      private boolean pinReconnectChunks(EntityPlayerMP player) {
           if(this.worldObj == null || this.worldObj.isRemote || this.linkedUavDimension != this.dimension
                 || !isUsableUavPosition(this.linkedUavX, this.linkedUavY, this.linkedUavZ)) {
                logReconnect("pin-rejected", player, null);
                return false;
           }

           if(this.reconnectChunkTicket != null && this.reconnectUavChunk != null) {
                this.worldObj.getChunkFromChunkCoords(this.reconnectUavChunk.chunkXPos, this.reconnectUavChunk.chunkZPos);
                logReconnect("chunks-already-pinned", player, null);
                return true;
           }
           this.reconnectChunkTicket = ForgeChunkManager.requestTicket(MCH_MOD.instance, this.worldObj, ForgeChunkManager.Type.NORMAL);
           if(this.reconnectChunkTicket == null) {
                logReconnect("ticket-unavailable", player, null);
                return false;
           }

           this.reconnectStationChunk = new ChunkCoordIntPair(MathHelper.floor_double(this.posX) >> 4,
                 MathHelper.floor_double(this.posZ) >> 4);
           this.reconnectUavChunk = new ChunkCoordIntPair(MathHelper.floor_double(this.linkedUavX) >> 4,
                 MathHelper.floor_double(this.linkedUavZ) >> 4);
           ForgeChunkManager.forceChunk(this.reconnectChunkTicket, this.reconnectStationChunk);
           ForgeChunkManager.forceChunk(this.reconnectChunkTicket, this.reconnectUavChunk);
           this.worldObj.getChunkFromChunkCoords(this.reconnectStationChunk.chunkXPos, this.reconnectStationChunk.chunkZPos);
           this.worldObj.getChunkFromChunkCoords(this.reconnectUavChunk.chunkXPos, this.reconnectUavChunk.chunkZPos);
           logReconnect("chunks-pinned", player, null);
           return true;
      }

      private void releaseReconnectChunks(String reason) {
           if(this.reconnectChunkTicket != null) {
                if(this.reconnectStationChunk != null) {
                     ForgeChunkManager.unforceChunk(this.reconnectChunkTicket, this.reconnectStationChunk);
                }
                if(this.reconnectUavChunk != null && !this.reconnectUavChunk.equals(this.reconnectStationChunk)) {
                     ForgeChunkManager.unforceChunk(this.reconnectChunkTicket, this.reconnectUavChunk);
                }
                ForgeChunkManager.releaseTicket(this.reconnectChunkTicket);
                MCH_Lib.Log((Entity)this, "[UAV-RECONNECT] step=chunks-released reason=%s", new Object[] { reason });
           }
           this.reconnectChunkTicket = null;
           this.reconnectStationChunk = null;
           this.reconnectUavChunk = null;
      }

      private boolean isChunkLoaded(ChunkCoordIntPair chunk) {
           return chunk != null && this.worldObj.getChunkProvider().chunkExists(chunk.chunkXPos, chunk.chunkZPos);
      }

      private void logReconnect(String step, EntityPlayerMP player, MCH_EntityBaseVehicle resolved) {
           Entity riding = player == null ? null : player.ridingEntity;
           MCH_Lib.Log((Entity)this,
                 "[UAV-RECONNECT] step=%s station=(%.2f,%.2f,%.2f) uavUuid=%s uavDim=%d uavChunk=(%d,%d) stationLoaded=%s uavLoaded=%s loadedEntityList=%s riding=%s resolved=%s playerPos=%s finalMount=%s stationControl=%s",
                 new Object[] {
                       step, Double.valueOf(this.posX), Double.valueOf(this.posY), Double.valueOf(this.posZ),
                       this.linkedUavEntityUUID == null ? this.assignedUavUUID : this.linkedUavEntityUUID.toString(),
                       Integer.valueOf(this.linkedUavDimension),
                       Integer.valueOf(MathHelper.floor_double(this.linkedUavX) >> 4),
                       Integer.valueOf(MathHelper.floor_double(this.linkedUavZ) >> 4),
                       Boolean.valueOf(isChunkLoaded(this.reconnectStationChunk)),
                       Boolean.valueOf(isChunkLoaded(this.reconnectUavChunk)),
                       Boolean.valueOf(MCH_UavRegistry.isPresentInLoadedEntityList(this.worldObj, resolved)),
                       riding == null ? "null" : riding.getClass().getSimpleName() + "#" + riding.getEntityId(),
                       resolved == null ? "null" : resolved.getClass().getSimpleName() + "#" + resolved.getEntityId() + "/dead=" + resolved.isDead,
                       player == null ? "null" : String.format("(%.2f,%.2f,%.2f)", player.posX, player.posY, player.posZ),
                       Boolean.valueOf(player != null && resolved != null && player.ridingEntity == resolved
                             && resolved.getRiddenByEntity() == player),
                       this.controlAircraft == null ? "null" : this.controlAircraft.getClass().getSimpleName() + "#" + this.controlAircraft.getEntityId()
                 });
      }

      private void restorePlayerAtStation(EntityPlayerMP player, MCH_EntityBaseVehicle ac, String reason) {
           if(player == null) {
                return;
           }
           if(player.ridingEntity != null && player.ridingEntity != this) {
                player.mountEntity((Entity)null);
           }
           this.riddenByEntity = null;
           this.lastRiddenByEntity = null;
           double x = this.posX - Math.sin(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
           double z = this.posZ + Math.cos(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
           player.setPositionAndUpdate(x, this.posY + getMountedYOffset() + player.getYOffset(), z);
           player.mountEntity(this);
           if(player.ridingEntity == this) {
                this.riddenByEntity = player;
                this.lastRiddenByEntity = player;
                setControlAircract(ac);
           }
           player.fallDistance = 0.0F;
           logReconnect("rollback-" + reason, player, ac);
      }

      private void loadLinkedUavChunk(World world) {
           if(world == null || world.isRemote || (this.linkedUavX == 0.0D && this.linkedUavY == 0.0D && this.linkedUavZ == 0.0D)) {
                return;
           }
           int x = MathHelper.floor_double(this.linkedUavX);
           int z = MathHelper.floor_double(this.linkedUavZ);
           world.getChunkFromBlockCoords(x, z);
         }

      private boolean relinkStoredUav(boolean requireLoaded) {
           MCH_EntityBaseVehicle ac = findLinkedUavEntity(this.worldObj);
           if(ac != null && !ac.isDead && linkUav(ac)) {
                setLastControlAircraft(ac);
                setLastControlAircraftEntityId(W_Entity.getEntityId((Entity)ac));
                return true;
           }
           this.awaitingLoadedUav = !requireLoaded;
           markLinkedUavUnloaded();
           return false;
         }

      private boolean isValidLinkedUav(MCH_EntityBaseVehicle ac, EntityPlayer player) {
           if(ac == null || ac.isDead || ac.isDestroyed()) {
                return false;
           }
           if(player != null) {
                if(this.ownerUUID != null && !this.ownerUUID.equals(player.getUniqueID())) {
                     return false;
                }
                if(ac.getOwnerUUID() != null && !ac.getOwnerUUID().equals(player.getUniqueID())) {
                     return false;
                }
                if(this.ownerUUID == null) {
                     setOwnerUUID(player.getUniqueID());
                }
           }
           if(this.linkedUavDimension != 0 && ac.dimension != this.linkedUavDimension) {
                return false;
           }
           if(!isUavLinkedToThisStation(ac)) {
                return false;
           }
           return ac.isUAV() || ac.isNewUAV();
         }


      private boolean isUsableUavPosition(double x, double y, double z) {
           return !Double.isNaN(x) && !Double.isInfinite(x) &&
                  !Double.isNaN(y) && !Double.isInfinite(y) &&
                  !Double.isNaN(z) && !Double.isInfinite(z) &&
                  (x != 0.0D || y != 0.0D || z != 0.0D);
         }

      public boolean prepareNewUavShiftExit(MCH_EntityBaseVehicle ac) {
           if(this.worldObj.isRemote || ac == null) {
                return false;
           }
           if(ac.isDestroyed()) {
                markLinkedNewUavDestroyed(ac);
                return true;
           }
           this.storedUavWasDestroyed = false;
           double previousLinkedX = this.linkedUavX;
           double previousLinkedY = this.linkedUavY;
           double previousLinkedZ = this.linkedUavZ;
           double shiftX = ac.posX;
           double shiftY = ac.posY;
           double shiftZ = ac.posZ;

           // On a dedicated server the dismount can be processed after the entity has briefly
           // received an uninitialized zero position. Keep the last authoritative server-tick
           // position instead of replacing a valid snapshot with 0,0,0.
           if(!isUsableUavPosition(shiftX, shiftY, shiftZ)) {
                if(isUsableUavPosition(ac.lastTickPosX, ac.lastTickPosY, ac.lastTickPosZ)) {
                     shiftX = ac.lastTickPosX;
                     shiftY = ac.lastTickPosY;
                     shiftZ = ac.lastTickPosZ;
                } else if(isUsableUavPosition(ac.prevPosX, ac.prevPosY, ac.prevPosZ)) {
                     shiftX = ac.prevPosX;
                     shiftY = ac.prevPosY;
                     shiftZ = ac.prevPosZ;
                } else if(isUsableUavPosition(previousLinkedX, previousLinkedY, previousLinkedZ)) {
                     shiftX = previousLinkedX;
                     shiftY = previousLinkedY;
                     shiftZ = previousLinkedZ;
                }
           }

           if(isUsableUavPosition(shiftX, shiftY, shiftZ)) {
                if(this.lastUavItemStack == null || !MCH_UavJsonStore.save(this.worldObj, this, ac, this.lastUavItemStack, shiftX, shiftY, shiftZ)) {
                     MCH_Lib.Log((Entity)this, "New UAV %d shift-exit JSON save failed; preserving the aircraft", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)ac)) });
                     return false;
                }
                this.linkedUavDimension = ac.dimension;
                this.linkedUavX = shiftX;
                this.linkedUavY = shiftY;
                this.linkedUavZ = shiftZ;
                this.storedUavRespawnX = shiftX;
                this.storedUavRespawnY = shiftY;
                this.storedUavRespawnZ = shiftZ;
                this.hasStoredUavRespawnPosition = true;
                setContinuationState(CONTINUE_AVAILABLE);
                MCH_Lib.Log((Entity)this, "New UAV %d shifted out at %.2f, %.2f, %.2f; replacing the saved Continue position", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)ac)), Double.valueOf(shiftX), Double.valueOf(shiftY), Double.valueOf(shiftZ) });
           } else {
                MCH_Lib.Log((Entity)this, "New UAV %d shifted out with an invalid server position; cancelling deletion so Continue cannot use stale coordinates", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)ac)) });
                return false;
           }
           this.assignedUav = null;
           this.assignedUavId = -1;
           this.assignedUavUUID = "";
           this.linkedUavEntityUUID = null;
           this.linkedUavCommonId = "";
           this.hasStoredUavLink = false;
           this.awaitingLoadedUav = false;
           this.pendingContinueTicks = 0;
           this.controlAircraft = null;
           setLastControlAircraft((MCH_EntityBaseVehicle)null);
           setLastControlAircraftEntityId(0);
           return true;
         }


         public void markLinkedNewUavDestroyed(MCH_EntityBaseVehicle ac) {
                 if(this.worldObj.isRemote) {
                     return;
                 }

                 if(ac != null) {
                     MCH_Lib.Log(
                             (Entity)this,
                             "New UAV %d was destroyed; clearing stored Continue item/state.",
                             new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)ac)) }
                     );
                 }

                 this.storedUavWasDestroyed = true;
                 setContinuationState(CONTINUE_DESTROYED);
                 MCH_UavJsonStore.consumeDestroyed(this.worldObj, this);
                 MCH_UavJsonStore.remove(this.worldObj, this);

                 // This is the important part: kill the fake respawn token.
                 this.lastUavItemStack = null;
                 this.hasStoredUavRespawnPosition = false;
                 this.respawnStoredUavAtSavedPosition = false;

                 this.assignedUav = null;
                 this.assignedUavId = -1;
                 this.assignedUavUUID = "";
                 this.linkedUavEntityUUID = null;
                 this.linkedUavCommonId = "";
                 this.loadedLastControlAircraftGuid = "";
                 this.hasStoredUavLink = false;
                 this.awaitingLoadedUav = false;
                 this.pendingContinueTicks = 0;

                 this.controlAircraft = null;
                 setLastControlAircraft((MCH_EntityBaseVehicle)null);
                 setLastControlAircraftEntityId(0);
          }

      private boolean continueWithStoredUavItem(Entity user) {
           //if(user == null || this.lastUavItemStack == null || this.worldObj.isRemote) {
           //     return false;
           //}
          if(user == null || this.worldObj.isRemote) {
              return false;
          }

          if(this.storedUavWasDestroyed) {
              if(user instanceof EntityPlayer) {
                  W_EntityPlayer.addChatMessage(
                          (EntityPlayer)user,
                          EnumChatFormatting.RED + "The linked drone was destroyed. Insert a new UAV item to launch again."
                  );
              }
              return false;
          }
          if(this.linkedUavEntityUUID != null
                || (this.linkedUavCommonId != null && !this.linkedUavCommonId.isEmpty())
                || (this.assignedUavUUID != null && !this.assignedUavUUID.isEmpty())) {
              // A persisted identity means a real entity is authoritative even if its chunk
              // has not finished loading. Spawning from the legacy JSON token here creates a
              // second live UAV and lets registration order decide which one the station uses.
              this.awaitingLoadedUav = true;
              markLinkedUavUnloaded();
              return false;
          }

          MCH_UavJsonStore.StoredUav stored = MCH_UavJsonStore.load(this.worldObj, this);
          if(stored == null) {
              return false;
          }
          ItemStack storedStack = stored.createItemStack();
          if(storedStack == null || stored.uavDimension != this.dimension || !isUsableUavPosition(stored.exitX, stored.exitY, stored.exitZ)) {
              MCH_Lib.Log((Entity)this, "Stored New UAV JSON entry is invalid for station %d", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)this)) });
              return false;
          }
          this.lastUavItemStack = storedStack;
          this.storedUavRespawnX = stored.exitX;
          this.storedUavRespawnY = stored.exitY;
          this.storedUavRespawnZ = stored.exitZ;
          this.hasStoredUavRespawnPosition = true;

           ItemStack stack = this.lastUavItemStack.copy();
           stack.stackSize = 1;
           MCH_Lib.Log((Entity)this, "Continue requested after shifted-out new UAV; relaunching stored UAV item %s", new Object[] { stack.getItem() == null ? "null" : stack.getItem().getUnlocalizedName() });
           this.respawnStoredUavAtSavedPosition = true;
           try {
                handleItem(user, stack);
           } finally {
                this.respawnStoredUavAtSavedPosition = false;
           }
           MCH_EntityBaseVehicle ac = getControlAircract();
           if(ac != null && !ac.isDead) {
                ac.setLocationAndAngles(stored.exitX, stored.exitY, stored.exitZ, stored.exitYaw, stored.exitPitch);
                if(this.riddenByEntity != null && ac.isNewUAV()) {
                     if(!startNewUavControl(this.riddenByEntity, ac)) {
                          this.pendingContinueTicks = 60;
                          return false;
                     }
                }
                return true;
           }
           return false;
         }

      private boolean startNewUavControl(Entity user, MCH_EntityBaseVehicle ac) {
           if(!(user instanceof EntityPlayerMP) || ac == null || !ac.isNewUAV()
                 || this.riddenByEntity != user || user.ridingEntity != this) {
                return false;
           }
           EntityPlayerMP player = (EntityPlayerMP)user;
           logReconnect("begin", player, null);
           if(!pinReconnectChunks(player)) {
                return false;
           }
           boolean attached = false;
           boolean releaseTicket = false;
           try {
                // Resolve only after both temporary tickets are active and both chunks have
                // been synchronously requested from the server chunk provider.
                MCH_EntityBaseVehicle resolved = MCH_UavRegistry.findLoadedByUuid(this.worldObj,
                      this.linkedUavEntityUUID);
                logReconnect("resolved", player, resolved);
                if(resolved == null || resolved.isDead || resolved.isDestroyed()
                      || !MCH_UavRegistry.isPresentInLoadedEntityList(this.worldObj, resolved)) {
                     logReconnect("resolve-pending", player, resolved);
                     return false;
                }
                if(!isValidLinkedUav(resolved, player) || !linkUav(resolved)) {
                     releaseTicket = true;
                     restorePlayerAtStation(player, ac, "invalid-uav");
                     return false;
                }

                // Clear every station-side control/rider cache before direct NewUAV control
                // is applied. The station link itself remains persisted for future returns.
                this.controlAircraft = null;
                setLastControlAircraft((MCH_EntityBaseVehicle)null);
                setLastControlAircraftEntityId(-1);
                setNewUavPilotProfile((EntityPlayer)null);
                logReconnect("station-state-cleared", player, resolved);

                if(!resolved.mountNewUavPilot(player, this)) {
                     restorePlayerAtStation(player, resolved, "mount-failed");
                     return false;
                }
                attached = player.ridingEntity == resolved && resolved.getRiddenByEntity() == player
                      && this.riddenByEntity == null;
                logReconnect("mount-applied", player, resolved);
                if(!attached) {
                     restorePlayerAtStation(player, resolved, "mount-not-confirmed");
                     return false;
                }

                this.pendingContinueTicks = 0;
                this.lastRiddenByEntity = null;
                setNewUavPilotProfile(player);
                W_EntityPlayer.closeScreen(player);
                logReconnect("complete", player, resolved);
                releaseTicket = true;
                return true;
           } finally {
                if(releaseTicket || attached) {
                     releaseReconnectChunks(attached ? "transfer-complete" : "transfer-rejected");
                }
           }
      }

      public boolean detachRiderForNewUavControl(EntityPlayerMP player) {
           if(this.worldObj.isRemote || player == null || this.riddenByEntity != player
                 || player.ridingEntity != this) {
                return false;
           }

           W_EntityPlayer.closeScreen(player);
           player.mountEntity((Entity)null);
           if(player.ridingEntity != null) {
                return false;
           }

           // Do not leave the station's remote-control rider cache alive while the same
           // player is attached directly to a New UAV. A stale station rider makes client
           // camera/chunk tracking continue from the station until the player reconnects.
           this.riddenByEntity = null;
           this.lastRiddenByEntity = null;
           player.fallDistance = 0.0F;
           return true;
         }

      public void clearNewUavReturnState(EntityPlayerMP player) {
           if(this.worldObj.isRemote) {
                return;
           }
           if(this.riddenByEntity == player) {
                this.riddenByEntity = null;
           }
           if(this.lastRiddenByEntity == player) {
                this.lastRiddenByEntity = null;
           }
           this.controlAircraft = null;
           setLastControlAircraft((MCH_EntityBaseVehicle)null);
           setLastControlAircraftEntityId(0);
           setNewUavPilotProfile((EntityPlayer)null);
           this.pendingContinueTicks = 0;
           releaseReconnectChunks("new-uav-return");
      }

      private void updateNewUavPilotProfile() {
           MCH_EntityBaseVehicle activeNewUav = this.controlAircraft != null && this.controlAircraft.isNewUAV()
                 ? this.controlAircraft : MCH_UavRegistry.findLoadedByUuid(this.worldObj, this.linkedUavEntityUUID);
           Entity pilot = activeNewUav != null && activeNewUav.isNewUAV()
                 && activeNewUav.isLinkedToStation(this) ? activeNewUav.getRiddenByEntity() : null;
           setNewUavPilotProfile(pilot instanceof EntityPlayer ? (EntityPlayer)pilot : null);
         }

      private void setNewUavPilotProfile(EntityPlayer player) {
           String value = "";
           if(player != null && !player.isDead) {
                GameProfile profile = player.getGameProfile();
                String uuid = profile.getId() == null ? "" : profile.getId().toString();
                value = uuid + "|" + profile.getName();
              }
           if(!value.equals(getDataWatcher().getWatchableObjectString(DATAWT_ID_NEW_UAV_PILOT))) {
                getDataWatcher().updateObject(DATAWT_ID_NEW_UAV_PILOT, value);
              }
         }

      @SideOnly(Side.CLIENT)
      public GameProfile getNewUavPilotProfile() {
           String value = getDataWatcher().getWatchableObjectString(DATAWT_ID_NEW_UAV_PILOT);
           if(value == null || value.isEmpty()) {
                return null;
              }
           int separator = value.indexOf('|');
           String uuid = separator >= 0 ? value.substring(0, separator) : "";
           String name = separator >= 0 ? value.substring(separator + 1) : value;
           if(name.isEmpty()) {
                return null;
              }
           return new GameProfile(parseUavUUID(uuid), name);
         }

      public boolean transferAmmoToLinkedUav(EntityPlayerMP player) {
           if(this.worldObj.isRemote) {
                return false;
           }
           ItemStack stack = getStackInSlot(0);
           if(stack == null || stack.stackSize <= 0) {
                return false;
           }
           MCH_EntityBaseVehicle ac = findLinkedUavEntity(this.worldObj);
           if(!isValidLinkedUav(ac, player)) {
                return false;
           }
           int before = stack.stackSize;
           int consumed = ac.trySupplyAmmoFromStack(stack, player);
           if(consumed <= 0) {
                return false;
           }
           if(stack.stackSize <= 0) {
                setInventorySlotContents(0, (ItemStack)null);
           } else if(stack.stackSize != before) {
                setInventorySlotContents(0, stack);
           }
           if(player != null) {
                player.inventoryContainer.detectAndSendChanges();
           }
           MCH_Lib.DbgLog(this.worldObj, "Transferred %d UAV ammo item(s) from station %d to UAV %d", new Object[] { Integer.valueOf(consumed), Integer.valueOf(W_Entity.getEntityId((Entity)this)), Integer.valueOf(W_Entity.getEntityId((Entity)ac)) });
           return true;
         }

      public boolean canStationAmmoFeedLinkedUav(ItemStack stack) {
           MCH_EntityBaseVehicle ac = findLinkedUavEntity(this.worldObj);
           return ac != null && ac.canAcceptAmmo(stack);
         }

      public void searchLastControlAircraft() {
          //makes a box around the station to search for the last controlled aircraft, for regular non-new UAVs
           if (!this.loadedLastControlAircraftGuid.isEmpty()) {
                List<MCH_EntityBaseVehicle> list = this.worldObj.getEntitiesWithinAABB(MCH_EntityBaseVehicle.class, getBoundingBox().expand(120.0D, 120.0D, 120.0D));
                if (list != null) {
                     for (int i = 0; i < list.size(); i++) {
                          MCH_EntityBaseVehicle ac = list.get(i);
                          if (ac.getCommonUniqueId().equals(this.loadedLastControlAircraftGuid)) {
                               String n = (ac.getAcInfo() != null) ? (ac.getAcInfo()).displayName : ("no info : " + ac);
                               MCH_Lib.DbgLog(this.worldObj, "MCH_EntityUavStation.searchLastControlAircraft:found" + n, new Object[0]);
                               linkUav(ac);
                               setLastControlAircraft(ac);
                               setLastControlAircraftEntityId(W_Entity.getEntityId((Entity)ac));
                               this.loadedLastControlAircraftGuid = "";
                               this.awaitingLoadedUav = false;
                               return;
                             }
                        }
                   }
              }
         }


             protected void onUpdate_Client() {
                      if (this.aircraftPosRotInc > 0) {
                            double rpinc = this.aircraftPosRotInc;
                            double yaw = MathHelper.wrapAngleTo180_double(this.aircraftYaw - this.rotationYaw);
                            this.rotationYaw = (float)(this.rotationYaw + yaw / rpinc);
                            this.rotationPitch = (float)(this.rotationPitch + (this.aircraftPitch - this.rotationPitch) / rpinc);
                            setPosition(this.posX + (this.aircraftX - this.posX) / rpinc, this.posY + (this.aircraftY - this.posY) / rpinc, this.posZ + (this.aircraftZ - this.posZ) / rpinc);
                            setRotation(this.rotationYaw, this.rotationPitch);
                            this.aircraftPosRotInc--;
                          } else {
                            setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
                            this.motionY *= 0.96D;
                            this.motionX = 0.0D;
                            this.motionZ = 0.0D;
                          }

                      updateUavPosition();
                    }

             private void onUpdate_Server() {
                      this.motionY -= 0.03D;
                      moveEntity(0.0D, this.motionY, 0.0D);
                      this.motionY *= 0.96D;
                      this.motionX = 0.0D;
                      this.motionZ = 0.0D;
                      setRotation(this.rotationYaw, this.rotationPitch);
                      if (this.riddenByEntity != null) {
                            Entity stationRider = this.riddenByEntity;
                            if (this.pendingContinueTicks > 0) {
                                  --this.pendingContinueTicks;
                                  if(this.pendingContinueTicks % 10 == 0) {
                                        controlLastAircraft(stationRider, false);
                                      }
                                }
                            // Continue can transfer stationRider to the UAV and clear
                            // riddenByEntity inside controlLastAircraft. Do not process the
                            // transferred player as though they were still on the station.
                            if(this.riddenByEntity != stationRider) {
                                  return;
                                }
                            if (stationRider.isDead) {
                                  releaseReconnectChunks("rider-dead");
                                  unmountEntity(true);
                                  this.riddenByEntity = null;
                                } else {
                                  ItemStack item = getStackInSlot(0);
                                  if (item != null && item.stackSize > 0 && !hasContinuableUavLink()) {
                                        handleItem(stationRider, item);
                                        if (item.stackSize == 0) {
                                              setInventorySlotContents(0, (ItemStack)null);
                                            }
                                      }
                                }
                          }
                      else if(this.reconnectChunkTicket != null) {
                            this.pendingContinueTicks = 0;
                            releaseReconnectChunks("rider-left-station");
                          }

                      if (getLastControlAircraft() == null && this.ticksExisted % 40 == 0) {
                            searchLastControlAircraft();
                          }
                    }


      public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
           this.aircraftPosRotInc = par9 + 8;
           this.aircraftX = par1;
           this.aircraftY = par3;
           this.aircraftZ = par5;
           this.aircraftYaw = par7;
           this.aircraftPitch = par8;
           this.motionX = this.velocityX;
           this.motionY = this.velocityY;
           this.motionZ = this.velocityZ;
         }

             public void updateRiderPosition() {
                      if (this.riddenByEntity != null) {
                            double x = -Math.sin(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                            double z = Math.cos(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                            this.riddenByEntity.setPosition(this.posX + x, this.posY + getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ + z);
                          }
                    }


             public void controlLastAircraft(Entity user) {
                 controlLastAircraft(user, true);
             }

             private void controlLastAircraft(Entity user, boolean notify) {

                 if(!this.worldObj.isRemote && !this.storedUavWasDestroyed && MCH_UavJsonStore.consumeDestroyed(this.worldObj, this)) {
                     markLinkedNewUavDestroyed((MCH_EntityBaseVehicle)null);
                 }
                 if(wasLinkedUavDestroyed()) {
                     this.pendingContinueTicks = 0;
                     if(notify && user instanceof EntityPlayer) {
                         W_EntityPlayer.addChatMessage((EntityPlayer)user, EnumChatFormatting.RED + "The linked drone was destroyed. Insert a new UAV item to launch again.");
                     }
                     return;
                 }
                 if(!hasContinuableUavLink()) {
                     if(notify && user instanceof EntityPlayer) {
                         W_EntityPlayer.addChatMessage((EntityPlayer)user, "No linked UAV is stored in this station.");
                     }
                     return;
                 }
                 MCH_EntityBaseVehicle lastAc = getAndSearchLastControlAircraft();
                 if(lastAc == null && user instanceof EntityPlayerMP && this.linkedUavEntityUUID != null) {
                     EntityPlayerMP player = (EntityPlayerMP)user;
                     logReconnect("continue-resolve-begin", player, null);
                     if(pinReconnectChunks(player)) {
                         lastAc = MCH_UavRegistry.findLoadedByUuid(this.worldObj, this.linkedUavEntityUUID);
                         logReconnect("continue-resolve-result", player, lastAc);
                         if(lastAc != null && linkUav(lastAc)) {
                             setLastControlAircraft(lastAc);
                         }
                     }
                 }
                 if (lastAc == null && relinkStoredUav(false)) {
                     lastAc = getLastControlAircraft();
                 }
                 if (lastAc != null && !lastAc.isDead) {

                     if(!isValidLinkedUav(lastAc, user instanceof EntityPlayer ? (EntityPlayer)user : null)) { return; }
                     if(!linkUav(lastAc)) {
                         this.pendingContinueTicks = 60;
                         return;
                     }
                     setControlAircract(lastAc);
                     if(this.controlAircraft != lastAc) {
                         this.pendingContinueTicks = 60;
                         return;
                     }
                     //this.assignedUav = uav;

                     if(this.riddenByEntity instanceof EntityPlayer) {
                         setOwnerUUID(((EntityPlayer)this.riddenByEntity).getUniqueID());
                         if(this.riddenByEntity instanceof EntityPlayerMP) {
                             EntityPlayerMP player = (EntityPlayerMP)this.riddenByEntity;
                             transferAmmoToLinkedUav(player);
                         }
                     }

                     if (this.controlAircraft != null &&
                             this.controlAircraft.getAcInfo() != null &&
                             this.controlAircraft.getAcInfo().isNewUAV) {
                         if(!startNewUavControl(user, this.controlAircraft)) {
                             this.pendingContinueTicks = 60;
                             return;
                         }
                     }
                     this.pendingContinueTicks = 0;
                     W_EntityPlayer.closeScreen(user);
                 } else if (continueWithStoredUavItem(user)) {
                     this.pendingContinueTicks = 0;
                 } else if (this.storedUavWasDestroyed) {
                     this.pendingContinueTicks = 0;
                     if(notify && user instanceof EntityPlayer) {
                         W_EntityPlayer.addChatMessage(
                                 (EntityPlayer)user,
                                 EnumChatFormatting.RED + "The linked drone was destroyed. Insert a new UAV item to launch again."
                         );
                     }
                 } else {
                     this.pendingContinueTicks = 60;
                     markLinkedUavUnloaded();
                 }

             }


     public void handleItem(Entity user, ItemStack itemStack) {
           if (user != null && !user.isDead && itemStack != null && itemStack.stackSize == 1 &&
                     !this.worldObj.isRemote) {
                Object ac = null;
                double x = this.posX + this.posUavX;
                double y = this.posY + this.posUavY;
                double z = this.posZ + this.posUavZ;
                if(this.respawnStoredUavAtSavedPosition && this.hasStoredUavRespawnPosition &&
                   isUsableUavPosition(this.storedUavRespawnX, this.storedUavRespawnY, this.storedUavRespawnZ)) {
                     x = this.storedUavRespawnX;
                     y = this.storedUavRespawnY;
                     z = this.storedUavRespawnZ;
                     MCH_Lib.Log((Entity)this, "Respawning shifted-out UAV at saved delete position %.2f, %.2f, %.2f", new Object[] { Double.valueOf(x), Double.valueOf(y), Double.valueOf(z) });
                   }
                if (y <= 1.0D) {
                     y = 2.0D;
                   }

                Item item = itemStack.getItem();
                if (item instanceof MCP_ItemPlane) {
                     MCP_PlaneInfo hi = MCP_PlaneInfoManager.getFromItem(item);
                     if (hi != null && (hi.isUAV || hi.isNewUAV)) {
                          if (!hi.isSmallUAV && getKind() == 2) {
                               ac = null;
                             } else {
                               ac = ((MCP_ItemPlane)item).createAircraft(this.worldObj, x, y, z, itemStack);
                             }
                       }
                  }

               if (item instanceof MCH_ItemShip) {
                   MCH_ShipInfo hi = MCH_ShipInfoManager.getFromItem(item);
                   if (hi != null && (hi.isUAV || hi.isNewUAV)) {
                       if (!hi.isSmallUAV && getKind() == 2) {
                           ac = null;
                       } else {
                           ac = ((MCH_ItemShip)item).createAircraft(this.worldObj, x, y, z, itemStack);
                       }
                   }
               }

                if (item instanceof MCH_ItemHeli) {
                     MCH_HeliInfo hi1 = MCH_HeliInfoManager.getFromItem(item);
                     if (hi1 != null && (hi1.isUAV || hi1.isNewUAV)) {
                         if (!hi1.isSmallUAV && getKind() == 2) {
                               ac = null;
                             } else {
                               ac = ((MCH_ItemHeli)item).createAircraft(this.worldObj, x, y, z, itemStack);
                             }
                        }
                   }

                if (item instanceof MCH_ItemTank) {
                     MCH_TankInfo hi2 = MCH_TankInfoManager.getFromItem(item);
                     if (hi2 != null && (hi2.isUAV || hi2.isNewUAV)) {
                          if (!hi2.isSmallUAV && getKind() == 2) {
                               ac = null;
                             } else {
                               ac = ((MCH_ItemTank)item).createAircraft(this.worldObj, x, y, z, itemStack);
                             }
                        }
                   }

                if (ac != null) {
                    MCH_EntityBaseVehicle linked = findLinkedUavEntity(this.worldObj);
                    if(linked != null && !linked.isDead && isValidLinkedUav(linked, user instanceof EntityPlayer ? (EntityPlayer)user : null)) {
                        MCH_Lib.Log((Entity)this, "Avoided spawning duplicate UAV from station %d; reusing linked UAV %d (%s)", new Object[] { Integer.valueOf(W_Entity.getEntityId((Entity)this)), Integer.valueOf(W_Entity.getEntityId((Entity)linked)), linked.getUavPersistentUUID() == null ? "" : linked.getUavPersistentUUID().toString() });
                        ((MCH_EntityBaseVehicle)ac).setDead(false);
                        linkUav(linked);
                        setControlAircract(linked);
                        if(linked.isNewUAV()) {
                             if(!startNewUavControl(user, linked)) {
                                  this.pendingContinueTicks = 60;
                                }
                          } else {
                             W_EntityPlayer.closeScreen(user);
                          }
                        return;
                    }
                    if(MCH_Config.ItemDamage.prmBool) {

                        ((MCH_EntityBaseVehicle)ac).getAcDataFromItem(itemStack);
                    } //why wasn't this here already?
                     ((Entity)ac).rotationYaw = this.rotationYaw - 180.0F;
                     ((Entity)ac).prevRotationYaw = ((Entity)ac).rotationYaw;
                     user.rotationYaw = this.rotationYaw - 180.0F;
                     if (this.worldObj.getCollidingBoundingBoxes((Entity)ac, ((Entity)ac).boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                         this.storedUavWasDestroyed = false;
                         this.lastUavItemStack = itemStack.copy();
                         this.lastUavItemStack.stackSize = 1;
                         itemStack.stackSize--;
                          MCH_Lib.DbgLog(this.worldObj, "Create UAV: %s : %s", new Object[] { item.getUnlocalizedName(), item });
                          user.rotationYaw = this.rotationYaw - 180.0F;
                          if (!((MCH_EntityBaseVehicle)ac).isTargetDrone()) {
                               this.setOwnerUUID(user instanceof EntityPlayer ? ((EntityPlayer)user).getUniqueID() : this.ownerUUID);
                               ((MCH_EntityBaseVehicle)ac).setOwnerUUID(this.ownerUUID);
                               linkUav((MCH_EntityBaseVehicle)ac);
                               setControlAircract((MCH_EntityBaseVehicle)ac);
                             }

                          this.worldObj.spawnEntityInWorld((Entity)ac);
                          linkUav((MCH_EntityBaseVehicle) ac);
                          if (!((MCH_EntityBaseVehicle)ac).isTargetDrone()) {
                               ((MCH_EntityBaseVehicle)ac).setFuel((int)(((MCH_EntityBaseVehicle)ac).getMaxFuel() * 0.05F));
                               if(((MCH_EntityBaseVehicle)ac).isNewUAV()) {
                                    if(!startNewUavControl(user, (MCH_EntityBaseVehicle)ac)) {
                                         this.pendingContinueTicks = 60;
                                       }
                                 } else {
                                    W_EntityPlayer.closeScreen(user);
                                 }
                             } else {
                               ((MCH_EntityBaseVehicle)ac).setFuel(((MCH_EntityBaseVehicle)ac).getMaxFuel());
                             }
                        } else {
                          ((MCH_EntityBaseVehicle)ac).setDead();
                        }
                   }
              }
         }



      public void _setInventorySlotContents(int par1, ItemStack itemStack) {
           setInventorySlotContents(par1, itemStack);
         }

      public boolean interactFirst(EntityPlayer player) {

          if(player != null && this.ownerUUID == null && !hasContinuableUavLink()) {
              this.setOwnerUUID(player.getUniqueID());
          }

           int kind = getKind();
           if (kind <= 0)
                return false;
           if (this.riddenByEntity != null) {
                return false;
              }
           if (kind == 2) {
                if (player.isSneaking()) {
                     setOpen(!isOpen());
                     return false;
                   }

                if (!isOpen()) {
                     return false;
                   }
              }

           this.riddenByEntity = null;
           this.lastRiddenByEntity = null;
           if (!this.worldObj.isRemote) {
                player.mountEntity((Entity)this);
                player.openGui(MCH_MOD.instance, 0, player.worldObj, (int)this.posX, (int)this.posY, (int)this.posZ);
              }

           return true;
         }


      public int getSizeInventory() {
           return 1;
         }

      public int getInventoryStackLimit() {
           return 1;
         }

             public void unmountEntity(boolean unmountAllEntity) {
                 Entity rByEntity = null;
                 if (this.riddenByEntity != null) {
                     if (!this.worldObj.isRemote) {

                         rByEntity = this.riddenByEntity;
                         this.riddenByEntity.mountEntity((Entity) null);
                     }
                 } else if (this.lastRiddenByEntity != null) {
                     rByEntity = this.lastRiddenByEntity;
                 }

                 if (getControlAircract() != null) {
                     if(!getControlAircract().isNewUAV()) {
                         getControlAircract().setUavStation((MCH_EntityUavStation) null);
                     }
                 }

                 if(getControlAircract() == null || !getControlAircract().isNewUAV()) {
                     setControlAircract((MCH_EntityBaseVehicle) null);
                 }
                 if (this.worldObj.isRemote) {
                     W_EntityPlayer.closeScreen(rByEntity);
                 }

                 this.riddenByEntity = null;
                 this.lastRiddenByEntity = null;
             }
    }
