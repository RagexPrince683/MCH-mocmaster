package mcheli.uav;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import mcheli.MCH_Config;
import mcheli.MCH_Explosion;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_ItemHeli;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_ItemPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class MCH_EntityUavStation
           extends W_EntityContainer
         {
      protected static final int DATAWT_ID_KIND = 27;
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

      public static double storedStationX;
      public static double storedStationY;
      public static double storedStationZ;

      private boolean continuePressed = false;
      //stored player never used
      //private EntityPlayer storedPlayer;
      public boolean isridingnewuav = false;
      public String newUavPlayerUUID;


             public void setContinuePressed(boolean flag) {
                 this.continuePressed = flag;
             }

             public boolean isContinuePressed() {
                 return this.continuePressed;
             }

             public void storeStationPosition() {
                 storedStationX = this.posX;
                 storedStationY = this.posY;
                 storedStationZ = this.posZ;
             }

             public double getStoredStationX() {
                 return storedStationX;
             }

             public double getStoredStationY() {
                 return storedStationY;
             }

             public double getStoredStationZ() {
                 return storedStationZ;
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
           setControlAircract((MCH_EntityAircraft)null);
           setLastControlAircraft((MCH_EntityAircraft)null);
           this.loadedLastControlAircraftGuid = "";
         }
      protected double aircraftY; protected double aircraftZ; protected double aircraftYaw; protected double aircraftPitch; private MCH_EntityAircraft controlAircraft; private MCH_EntityAircraft lastControlAircraft; private String loadedLastControlAircraftGuid; public int posUavX; public int posUavY; public int posUavZ; public float rotCover; public float prevRotCover;
      protected void entityInit() {
           super.entityInit();
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

      public MCH_EntityAircraft getControlAircract() {
           return this.controlAircraft;
         }

      public void setControlAircract(MCH_EntityAircraft ac) {
           this.controlAircraft = ac;
           if (ac != null && !ac.isDead ) { //&& MCH_EntityUavStation != null MAKE A NEW FUCKING VARIABLE TO TEST IF THE UAV STATION IS ALIVE OR NOT
                setLastControlAircraft(ac);
              }
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
             public void setDead() {
                 System.out.println("setDead fired in UAV Station");

                 if (this.controlAircraft != null) {
                     System.out.println("Retrieving stored player UUID from controlled UAV.");
                     this.newUavPlayerUUID = this.controlAircraft.newUavPlayerUUID;
                 }

                 if (this.newUavPlayerUUID != null) {
                     System.out.println("Stored player UUID: " + this.newUavPlayerUUID);
                     for (Object obj : worldObj.playerEntities) {
                         EntityPlayer player = (EntityPlayer) obj;
                         if (player.getUniqueID().toString().equals(this.newUavPlayerUUID)) {
                             System.out.println("Found matching player by UUID. Dismounting and teleporting...");

                             // Ensure dismount
                             if (player.ridingEntity instanceof MCH_EntityAircraft) {
                                 MCH_EntityAircraft aircraft = (MCH_EntityAircraft) player.ridingEntity;
                                 System.out.println("Player is currently in UAV. Calling unmountAircraft...");
                                 aircraft.unmountAircraft();
                             } else {
                                 System.out.println("Player is NOT riding a UAV, forcing dismount.");
                                 player.mountEntity(null);
                             }

                             // Teleport player back safely
                             if (!player.worldObj.isRemote) {
                                 player.setPositionAndUpdate(
                                         MCH_EntityUavStation.storedStationX,
                                         MCH_EntityUavStation.storedStationY,
                                         MCH_EntityUavStation.storedStationZ
                                 );
                             }

                             player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Station destroyed! Teleporting back to station."));
                             break;
                         }
                     }
                 } else {
                     System.out.println("No stored player UUID in UAV station.");
                 }

                 super.setDead();
                 System.out.println("UAV Station setDead completed.");
             }


             public boolean attackEntityFrom(DamageSource damageSource, float damage) {
           if (isEntityInvulnerable())
                return false;
           if (this.isDead)
               return true;
          if (this.worldObj.isRemote) {
                return true;
              }
           String dmt = damageSource.getDamageType();
           MCH_Config var10000 = MCH_MOD.config;
           damage = MCH_Config.applyDamageByExternal((Entity)this, damageSource, damage);
           if (!MCH_Multiplay.canAttackEntity(damageSource, (Entity)this)) {
                return false;
              }
           boolean isCreative = false;
           Entity entity = damageSource.getEntity();
           boolean isDamegeSourcePlayer = false;
           if (entity instanceof EntityPlayer) {
                isCreative = ((EntityPlayer)entity).capabilities.isCreativeMode;
                if (dmt.compareTo("player") == 0) {
                     isDamegeSourcePlayer = true;
                   }

                W_WorldFunc.MOD_playSoundAtEntity((Entity)this, "hit", 1.0F, 1.0F);
              } else {
                W_WorldFunc.MOD_playSoundAtEntity((Entity)this, "helidmg", 1.0F, 0.9F + this.rand.nextFloat() * 0.1F);
              }

           setBeenAttacked();
           if (damage > 0.0F) {
               //i might want to store these at the top of the class idk tho
               //nvm this works perfectly fine NOT THIS DOES NOTHING WHAT
               //double gotox = MCH_EntityUavStation.storedStationX;
               //double gotoy = MCH_EntityUavStation.storedStationY;
               //double gotoz = MCH_EntityUavStation.storedStationZ;
               //EntityPlayer player = (EntityPlayer) this.riddenByEntity;
               //if (this.riddenByEntity != null) {
               //    this.riddenByEntity.mountEntity((Entity) this);
               //}

               EntityPlayer player = null;
               // If there's a rider, capture the UUID immediately
               if (this.riddenByEntity instanceof EntityPlayer) {
                   player = (EntityPlayer)this.riddenByEntity;
                   this.newUavPlayerUUID = player.getUniqueID().toString();
               } else if (this.newUavPlayerUUID != null) {
                   // Search for the player using the stored UUID
                   for (Object obj : worldObj.playerEntities) {
                       if (obj instanceof EntityPlayer) {
                           EntityPlayer p = (EntityPlayer) obj;
                           if (p.getUniqueID().toString().equals(this.newUavPlayerUUID)) {
                               player = p;
                               break;
                           }
                       }
                   }

                   if (player != null) {
                       // Force dismount
                       player.mountEntity(null);
                       // Optional: add potion effects or chat messages as needed
                       player.setPositionAndUpdate(storedStationX, storedStationY, storedStationZ);
                       System.out.println("Teleporting player in damage logic.");
                   } else {
                       System.out.println("No player found for teleportation in damage logic.");
                   }
               }

               this.dropContentsWhenDead = true;
              // if (this.riddenByEntity != null){ //!isDamegeSourcePlayer && WE WANT TO CHECK IF
               // THE MCH_ENTITY AIRCRAFT IS RIDDEN NOT THE PLAYER,
               // THE PLAYER IS MOUNTED TO THE FUCKING UAV
               //NOT THAT IT FUCKING MATTERS THIS SHIT JUST CRASHES ANYWAYS YAY
               // I FUCKING LOVE MINECRAFT MODDING
               System.out.println(MCH_EntityUavStation.storedStationX + " " +
                       MCH_EntityUavStation.storedStationY + " " +
                       MCH_EntityUavStation.storedStationZ + " " + "station pos");

               // Teleport player
               //player.setPositionAndUpdate(
               //        MCH_EntityUavStation.storedStationX,
               //        MCH_EntityUavStation.storedStationY,
               //        MCH_EntityUavStation.storedStationZ
               //);
               //teleport the player? I think you meant CRASH THE FUCKING GAME
         //  }
                setDead();
                if (!isDamegeSourcePlayer) {
                     MCH_Explosion.newExplosion(this.worldObj, (Entity)null, this.riddenByEntity, this.posX, this.posY, this.posZ, 1.0F, 0.0F, true, true, false, false, 0);
                   }

                if (!isCreative) {
                     int kind = getKind();
                     if (kind > 0) {
                          dropItemWithOffset((Item)MCH_MOD.itemUavStation[kind - 1], 1, 0.0F);
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
           if (getKind() == 2 && this.riddenByEntity != null) {
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

           //99% sure does not work lool... I would FUCKING KNOW IF THERE WERE PRINT STATEMENTS
          //if (this.riddenByEntity instanceof EntityPlayer && this.controlAircraft != null && this.controlAircraft.getAcInfo().isNewUAV) {
          //    //((EntityPlayer) this.riddenByEntity).inventory.pickupSlot = -1; // Prevents item pickup for new UAVs only
          //    ((EntityPlayer) this.riddenByEntity).inventoryContainer.detectAndSendChanges();
          //}

          if (this.riddenByEntity instanceof EntityPlayer && this.controlAircraft != null && this.controlAircraft.getAcInfo().isNewUAV) {
              isridingnewuav = true;
              System.out.println("isridingnewuav is true");
              player.addPotionEffect(new PotionEffect(11, 20, 4)); // Resistance IV
              player.addPotionEffect(new PotionEffect(12, 20, 0)); // Fire Resistance
              // Prevent picking up items by removing them from collision
              //except that this only fires when the station is in range because this is in the station class so like this will only fire when the station is loaded
              //oh also it just doesn't even fucking work idk why I even tried this
              //List<EntityItem> items = this.worldObj.getEntitiesWithinAABB(EntityItem.class, player.boundingBox.expand(1.5, 1.5, 1.5));
              //for (EntityItem item : items) {
              //    item.delayBeforeCanPickup = 999999; // Stops item from being picked up
              //}
          }




           if (this.riddenByEntity == null &&
                     this.lastRiddenByEntity != null) {
                unmountEntity(true);
               //if (controlAircraft.isNewUAV()) {
               //    System.out.println(MCH_EntityUavStation.storedStationX + " " +
               //            MCH_EntityUavStation.storedStationY + " " +
               //            MCH_EntityUavStation.storedStationZ + " " + "station pos");

               //    // Teleport player
               //    player.setPositionAndUpdate(
               //            MCH_EntityUavStation.storedStationX,
               //            MCH_EntityUavStation.storedStationY,
               //            MCH_EntityUavStation.storedStationZ
               //    );
               //}
              }




           int uavStationKind = getKind();
           if (this.ticksExisted >= 30 || uavStationKind <= 0 || uavStationKind == 1 || uavStationKind == 2);



           if (this.worldObj.isRemote && !this.isRequestedSyncStatus) {
                this.isRequestedSyncStatus = true;
              }

           this.prevPosX = this.posX;
           this.prevPosY = this.posY;
           this.prevPosZ = this.posZ;
           if (getControlAircract() != null && ((getControlAircract()).isDead || getControlAircract().isDestroyed())) {
                setControlAircract((MCH_EntityAircraft)null);
              }

           if (getLastControlAircraft() != null && ((getLastControlAircraft()).isDead || getLastControlAircraft().isDestroyed())) {
                setLastControlAircraft((MCH_EntityAircraft)null);
              }

           if (this.worldObj.isRemote) {
                onUpdate_Client();
              } else {
                onUpdate_Server();
              }

           this.lastRiddenByEntity = this.riddenByEntity;
         }

      public MCH_EntityAircraft getLastControlAircraft() {
           return this.lastControlAircraft;
         }

      public MCH_EntityAircraft getAndSearchLastControlAircraft() {
           if (getLastControlAircraft() == null) {
                int id = getLastControlAircraftEntityId().intValue();
                if (id > 0) {
                     Entity entity = this.worldObj.getEntityByID(id);
                     if (entity instanceof MCH_EntityAircraft) {
                          MCH_EntityAircraft ac = (MCH_EntityAircraft)entity;
                          if (ac.isUAV()) {
                               setLastControlAircraft(ac);
                             }
                        }
                   }
              }

           return getLastControlAircraft();
         }

      public void setLastControlAircraft(MCH_EntityAircraft ac) {
           MCH_Lib.DbgLog(this.worldObj, "MCH_EntityUavStation.setLastControlAircraft:" + ac, new Object[0]);
           this.lastControlAircraft = ac;
         }

      public Integer getLastControlAircraftEntityId() {
           return Integer.valueOf(getDataWatcher().getWatchableObjectInt(28));
         }

      public void setLastControlAircraftEntityId(int s) {
           if (!this.worldObj.isRemote) {
                getDataWatcher().updateObject(28, Integer.valueOf(s));
              }
         }


      public void searchLastControlAircraft() {
          //makes a box around the station to search for the last controlled aircraft, for regular non-new UAVs
           if (!this.loadedLastControlAircraftGuid.isEmpty()) {
                List<MCH_EntityAircraft> list = this.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, getBoundingBox().expand(120.0D, 120.0D, 120.0D));
                if (list != null) {
                     for (int i = 0; i < list.size(); i++) {
                          MCH_EntityAircraft ac = list.get(i);
                          if (ac.getCommonUniqueId().equals(this.loadedLastControlAircraftGuid)) {
                               String n = (ac.getAcInfo() != null) ? (ac.getAcInfo()).displayName : ("no info : " + ac);
                               MCH_Lib.DbgLog(this.worldObj, "MCH_EntityUavStation.searchLastControlAircraft:found" + n, new Object[0]);
                               setLastControlAircraft(ac);
                               setLastControlAircraftEntityId(W_Entity.getEntityId((Entity)ac));
                               this.loadedLastControlAircraftGuid = "";
                               return;
                             }
                        }
                   }
              }
         }


             protected void onUpdate_Client() {
                 /* 430 */     if (this.aircraftPosRotInc > 0) {
                     /* 431 */       double rpinc = this.aircraftPosRotInc;
                     /* 432 */       double yaw = MathHelper.wrapAngleTo180_double(this.aircraftYaw - this.rotationYaw);
                     /* 433 */       this.rotationYaw = (float)(this.rotationYaw + yaw / rpinc);
                     /* 434 */       this.rotationPitch = (float)(this.rotationPitch + (this.aircraftPitch - this.rotationPitch) / rpinc);
                     /* 435 */       setPosition(this.posX + (this.aircraftX - this.posX) / rpinc, this.posY + (this.aircraftY - this.posY) / rpinc, this.posZ + (this.aircraftZ - this.posZ) / rpinc);
                     /* 436 */       setRotation(this.rotationYaw, this.rotationPitch);
                     /* 437 */       this.aircraftPosRotInc--;
                     /*     */     } else {
                     /* 439 */       setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
                     /* 440 */       this.motionY *= 0.96D;
                     /* 441 */       this.motionX = 0.0D;
                     /* 442 */       this.motionZ = 0.0D;
                     /*     */     }
                 /*     */
                 /* 445 */     updateUavPosition();
                 /*     */   }

             private void onUpdate_Server() {
                      this.motionY -= 0.03D;
                      moveEntity(0.0D, this.motionY, 0.0D);
                      this.motionY *= 0.96D;
                      this.motionX = 0.0D;
                      this.motionZ = 0.0D;
                      setRotation(this.rotationYaw, this.rotationPitch);
                      if (this.riddenByEntity != null) {
                            if (this.riddenByEntity.isDead) {
                                  unmountEntity(true);
                                  this.riddenByEntity = null;
                                } else {
                                  ItemStack item = getStackInSlot(0);
                                  if (item != null && item.stackSize > 0) {
                                        handleItem(this.riddenByEntity, item);
                                        if (item.stackSize == 0) {
                                              setInventorySlotContents(0, (ItemStack)null);
                                            }
                                      }
                                }
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
                 /* 489 */     if (this.riddenByEntity != null) {
                     /* 490 */       double x = -Math.sin(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                     /* 491 */       double z = Math.cos(this.rotationYaw * Math.PI / 180.0D) * 0.9D;
                     /* 492 */       this.riddenByEntity.setPosition(this.posX + x, this.posY + getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ + z);
                     /*     */     }
                 /*     */   }


             public void controlLastAircraft(Entity user) {
                 MCH_EntityAircraft lastAc = getLastControlAircraft();
                 if (lastAc != null && !lastAc.isDead) {
                     lastAc.setUavStation(this);
                     setControlAircract(lastAc);

                     if(this.riddenByEntity instanceof EntityPlayer) {
                         lastAc.storedRider = (EntityPlayer)this.riddenByEntity;
                     }

                     // Store the current station position
                     System.out.println("stationposition" + storedStationX + " " + storedStationY + " " + storedStationZ);
                     storeStationPosition();

                     // If it's a new UAV and it's been alive long enough, mount the player to the UAV.
                     if (this.controlAircraft != null &&
                             this.controlAircraft.getAcInfo() != null &&
                             this.controlAircraft.getAcInfo().isNewUAV &&
                             this.controlAircraft.ticksExisted >= 10) {
                         this.riddenByEntity.mountEntity((Entity)this.controlAircraft);
                     }
                     W_EntityPlayer.closeScreen(user);
                 }
                 //this will never fire but its good to have redundants I guess? The part of this that works
                 // is in the MCH_EntityAircraft class.
                 if (this.controlAircraft.getAcInfo().isNewUAV && this.isDead) {
                     System.out.println("UAV is dead, teleporting to stored station position");
                        this.setPosition(storedStationX, storedStationY, storedStationZ);
                        //todo probably also set the fuckin mount thing to null before setting position so minecraft doesnt tardmax
                 }
             }


     public void handleItem(Entity user, ItemStack itemStack) {
           if (user != null && !user.isDead && itemStack != null && itemStack.stackSize == 1 &&
                     !this.worldObj.isRemote) {
                Object ac = null;
                double x = this.posX + this.posUavX;
                double y = this.posY + this.posUavY;
                double z = this.posZ + this.posUavZ;
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

                if (item instanceof MCH_ItemHeli) {
                     MCH_HeliInfo hi1 = MCH_HeliInfoManager.getFromItem(item);
                     if (hi1 != null && hi1.isUAV) {
                         if (!hi1.isSmallUAV && getKind() == 2) {
                               ac = null;
                             } else {
                               ac = ((MCH_ItemHeli)item).createAircraft(this.worldObj, x, y, z, itemStack);
                             }
                        }
                   }

                if (item instanceof MCH_ItemTank) {
                     MCH_TankInfo hi2 = MCH_TankInfoManager.getFromItem(item);
                     if (hi2 != null && hi2.isUAV) {
                          if (!hi2.isSmallUAV && getKind() == 2) {
                               ac = null;
                             } else {
                               ac = ((MCH_ItemTank)item).createAircraft(this.worldObj, x, y, z, itemStack);
                             }
                        }
                   }

                if (ac != null) {
                     ((Entity)ac).rotationYaw = this.rotationYaw - 180.0F;
                     ((Entity)ac).prevRotationYaw = ((Entity)ac).rotationYaw;
                     user.rotationYaw = this.rotationYaw - 180.0F;
                     if (this.worldObj.getCollidingBoundingBoxes((Entity)ac, ((Entity)ac).boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                          itemStack.stackSize--;
                          MCH_Lib.DbgLog(this.worldObj, "Create UAV: %s : %s", new Object[] { item.getUnlocalizedName(), item });
                          user.rotationYaw = this.rotationYaw - 180.0F;
                          if (!((MCH_EntityAircraft)ac).isTargetDrone()) {
                               ((MCH_EntityAircraft)ac).setUavStation(this);
                               setControlAircract((MCH_EntityAircraft)ac);
                             }

                          this.worldObj.spawnEntityInWorld((Entity)ac);
                          if (!((MCH_EntityAircraft)ac).isTargetDrone()) {
                               ((MCH_EntityAircraft)ac).setFuel((int)(((MCH_EntityAircraft)ac).getMaxFuel() * 0.05F));
                               W_EntityPlayer.closeScreen(user);
                             } else {
                               ((MCH_EntityAircraft)ac).setFuel(((MCH_EntityAircraft)ac).getMaxFuel());
                             }
                        } else {
                          ((MCH_EntityAircraft)ac).setDead();
                        }
                   }
              }
         }



      public void _setInventorySlotContents(int par1, ItemStack itemStack) {
           setInventorySlotContents(par1, itemStack);
         }

      public boolean interactFirst(EntityPlayer player) {

          if(this.riddenByEntity instanceof EntityPlayer){
              //EntityPlayer player = (EntityPlayer)this.riddenByEntity;
              //SIR THIS IS ALREADY DEFINED SIR
              this.newUavPlayerUUID = player.getUniqueID().toString();
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
                     getControlAircract().setUavStation((MCH_EntityUavStation) null);
                 }

                 setControlAircract((MCH_EntityAircraft) null);
                 if (this.worldObj.isRemote) {
                     W_EntityPlayer.closeScreen(rByEntity);
                 }

                 EntityPlayer player = (EntityPlayer) this.riddenByEntity;

                 // Ensure player teleports back to the UAV station when dismounting
                 //I don't know that this will ever fire considering rByEntity is null but I will trust the plan gpt
                 if (rByEntity instanceof EntityPlayer && this.controlAircraft != null && this.controlAircraft.getAcInfo().isNewUAV) {
                     System.out.println("Teleporting player back to stored station position.");
                     player.setPositionAndUpdate(storedStationX, storedStationY, storedStationZ);
                 }
                 //this infact does not work

                 if (player != null && this.controlAircraft != null && this.controlAircraft.getAcInfo().isNewUAV) {
                     System.out.println("Teleporting player back to stored station position (New UAV).");
                     player.setPositionAndUpdate(storedStationX, storedStationY, storedStationZ);
                 }
                 //this either, this also doesn't work

                 this.riddenByEntity = null;
                 this.lastRiddenByEntity = null;
             }
    }