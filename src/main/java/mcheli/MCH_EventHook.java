package mcheli;

import java.util.ArrayList;
import java.util.HashSet;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_PacketNotifyServerSettings;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.aircraft.MCH_PacketAircraftLocation;
import mcheli.uav.MCH_UavInventory;
import mcheli.uav.MCH_UavRegistry;
import mcheli.chain.MCH_ItemChain;
import mcheli.command.MCH_Command;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.plane.MCP_EntityPlane;
//import mcheli.sensors.MCH_VisualContact;
//import mcheli.sensors.Mk1Eyeball;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_EventHook;
import mcheli.wrapper.W_Lib;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

public class MCH_EventHook extends W_EventHook {

   int acloaded = 0;
   private final Map<EntityPlayerMP, Set<UUID>> aircraftPendingRetrack = new WeakHashMap<EntityPlayerMP, Set<UUID>>();
   private Field trackedEntityIdsField;
   private boolean searchedTrackedEntityIdsField;


   @SubscribeEvent
   public void worldLoad(WorldEvent.Load event) {
      if(event.world instanceof WorldServer && !event.world.isRemote) {
         this.expandAircraftLODTrackingRange((WorldServer)event.world);
      }

   }

   private void expandAircraftLODTrackingRange(WorldServer world) {
      double farDistance = MCH_Config.AircraftLODFarDistance != null?MCH_Config.AircraftLODFarDistance.prmDouble:0.0D;
      if(farDistance <= 0.0D) {
         return;
      }

      int range = (int)Math.min(2147483647.0D, Math.max(600.0D, Math.ceil(farDistance)));
      EntityTracker tracker = world.getEntityTracker();
      Field[] fields = EntityTracker.class.getDeclaredFields();

      for(int i = 0; i < fields.length; ++i) {
         Field field = fields[i];
         if(field.getType() == Integer.TYPE) {
            try {
               field.setAccessible(true);
               int currentRange = field.getInt(tracker);
               if(currentRange < range) {
                  field.setInt(tracker, range);
               }
            } catch(Exception ignored) {
            }

            return;
         }
      }

   }


   // Aircraft use forceSpawn and a long LOD tracking range, so the server tracker can
   // retain a player after that player stops watching the aircraft's actual chunk.
   @SubscribeEvent
   public void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
      EntityPlayerMP player = event.player;
      if(player == null || !(player.worldObj instanceof WorldServer)) {
         return;
      }

      WorldServer world = (WorldServer)player.worldObj;
      EntityTracker tracker = world.getEntityTracker();
      Set<UUID> pending = null;
      for(Object object : world.loadedEntityList) {
         if(object instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft)object;
            if(!aircraft.isDead && aircraft.chunkCoordX == event.chunk.chunkXPos
                    && aircraft.chunkCoordZ == event.chunk.chunkZPos) {
               EntityTrackerEntry entry = this.getTrackerEntry(tracker, aircraft.getEntityId());
               if(entry != null && entry.trackingPlayers.contains(player)) {
                  if(pending == null) {
                     pending = this.aircraftPendingRetrack.get(player);
                     if(pending == null) {
                        pending = new HashSet<UUID>();
                        this.aircraftPendingRetrack.put(player, pending);
                     }
                  }
                  pending.add(aircraft.getUniqueID());
               }
            }
         }
      }
   }

   // Forge posts Watch after vanilla has attempted to send entities in the chunk.
   // Recreate only entries that survived the matching UnWatch; otherwise vanilla sees
   // the player in trackingPlayers and suppresses the parent aircraft spawn packet.
   @SubscribeEvent
   public void onChunkWatch(ChunkWatchEvent.Watch event) {
      EntityPlayerMP player = event.player;
      if(player == null || !(player.worldObj instanceof WorldServer)) {
         return;
      }
      Set<UUID> pending = this.aircraftPendingRetrack.get(player);
      if(pending == null || pending.isEmpty()) {
         return;
      }

      WorldServer world = (WorldServer)player.worldObj;
      EntityTracker tracker = world.getEntityTracker();
      for(Object object : world.loadedEntityList) {
         if(object instanceof MCH_EntityAircraft) {
            MCH_EntityAircraft aircraft = (MCH_EntityAircraft)object;
            if(!aircraft.isDead && aircraft.chunkCoordX == event.chunk.chunkXPos
                    && aircraft.chunkCoordZ == event.chunk.chunkZPos && pending.remove(aircraft.getUniqueID())) {
               this.retrackEntityForPlayer(tracker, aircraft, player);
               for(MCH_EntitySeat seat : aircraft.getSeats()) {
                  if(seat != null && !seat.isDead) {
                     this.retrackEntityForPlayer(tracker, seat, player);
                  }
               }
            }
         }
      }

      if(pending.isEmpty()) {
         this.aircraftPendingRetrack.remove(player);
      }
   }

   private void retrackEntityForPlayer(EntityTracker tracker, Entity entity, EntityPlayerMP player) {
      EntityTrackerEntry entry = this.getTrackerEntry(tracker, entity.getEntityId());
      if(entry != null) {
         entry.removePlayerFromTracker(player);
         entry.tryStartWachingThis(player);
      }
   }

   private EntityTrackerEntry getTrackerEntry(EntityTracker tracker, int entityId) {
      if(!this.searchedTrackedEntityIdsField) {
         this.searchedTrackedEntityIdsField = true;
         for(Field field : EntityTracker.class.getDeclaredFields()) {
            if(field.getType() == IntHashMap.class) {
               field.setAccessible(true);
               this.trackedEntityIdsField = field;
               break;
            }
         }
      }

      if(this.trackedEntityIdsField != null) {
         try {
            IntHashMap trackedEntityIds = (IntHashMap)this.trackedEntityIdsField.get(tracker);
            Object entry = trackedEntityIds.lookup(entityId);
            return entry instanceof EntityTrackerEntry?(EntityTrackerEntry)entry:null;
         } catch(Exception ignored) {
            this.trackedEntityIdsField = null;
         }
      }
      return null;
   }

   public void commandEvent(CommandEvent event) {
      MCH_Command.onCommandEvent(event);
   }

   //@SubscribeEvent
   //public void onTick(TickEvent.PlayerTickEvent event) {
   //   if (event.phase != TickEvent.Phase.END) return;
//
   //   EntityPlayer player = event.player;
//
   //   // Debug print on both sides
   //   System.out.println("[TICK] " + (player.worldObj.isRemote ? "CLIENT" : "SERVER") + " | Player: " + player.getCommandSenderName());
//
   //   int lightWeaponCount = countLightWeapons(player);
   //   System.out.println("Light weapon count: " + lightWeaponCount);
//
   //   if (lightWeaponCount > 1) {
   //      int amplifier = lightWeaponCount - 1;
//
   //      // Only apply potion on server, but could sync to client if needed
   //      player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 200, amplifier, true));
   //      System.out.println("Applied Slowness (amplifier: " + amplifier + ")");
   //   }
   //}
//
   //private int countLightWeapons(EntityPlayer player) {
   //   int count = 0;
   //   for (ItemStack itemStack : player.inventory.mainInventory) {
   //      if (itemStack != null) {
   //         System.out.println("Found item: " + itemStack.getItem().getUnlocalizedName());
   //         if (itemStack.getItem() instanceof MCH_ItemLightWeaponBase) {
   //            count++;
   //            System.out.println("Counted light weapon");
   //         }
   //      }
   //   }
   //   return count;
   //}

   //will never run for some fucking reason; why is it every time I try to mess with the event hooks nothing fucking works?



   //private void drawContacts(float partialTick) {
   //   EntityPlayer player = Minecraft.getMinecraft().thePlayer;
   //   for (MCH_VisualContact contact : Mk1Eyeball.getInstance().contacts) {
   //      if (player.getDistance(contact.x, contact.y, contact.z) >= 64) {
   //         Mk1Eyeball.renderContact(contact, partialTick);
   //         System.out.println("eventhook drawcontacts");
   //      }
   //   }
   //}

  //@SubscribeEvent
  //public void onRenderWorldEvent(RenderWorldLastEvent event) {
  //   Mk1Eyeball.getInstance().update();
  //   System.out.println("onrenderworldevent drawcontacts time");
  //   System.out.println(Mk1Eyeball.getInstance().contacts + " is mk1 eyeball contacts");
  //   drawContacts(event.partialTicks);
  //}




//  @SubscribeEvent
//  public void onRenderWorldLastEvent(RenderWorldLastEvent evt) {
//     //System.out.println("THIS WORKS");
//     //it indeed works
//     World worldObj = Minecraft.getMinecraft().theWorld;
//     for(Object O : worldObj.playerEntities){
//        EntityPlayer player = (EntityPlayer)O;
//        AxisAlignedBB aabb = player.boundingBox.expand(350,350,350);
//        List<MCH_EntityAircraft> list = new ArrayList<>();
//        for(Object e : worldObj.getEntitiesWithinAABBExcludingEntity(player,aabb)) {
//           if (e instanceof MCH_EntityAircraft) { //&& is ridden
//              list.add((MCH_EntityAircraft)e);
//              MCH_PacketAircraftLocation.send((MCH_EntityAircraft)e, player);
//              //System.out.println("idk testing I think this won't fire");
//           }
//        }
//     }
//  }

 //  private void drawContacts(float partialTick) {
 //     EntityPlayer player = Minecraft.getMinecraft().thePlayer;
 //     if (player == null) return;
//
 //     for(MCH_VisualContact contact : Mk1Eyeball.getInstance().contacts){
 //        if(player.getDistance(contact.x, contact.y, contact.z) >= 64) {
 //           Mk1Eyeball.renderContact(contact, partialTick);
 //           System.out.println("mk1 eyeball get instance contacts and mk1 eyeball render contact");
 //        }
 //     }
 //  }
 //  @SubscribeEvent
 //  public void onRenderWorldEvent(RenderWorldLastEvent event){
 //     //System.out.println("onrenderworldevent");
 //     //is firing
 //     Mk1Eyeball.getInstance().update();
 //     drawContacts(event.partialTicks);
 //  }

 //     @SideOnly(Side.CLIENT)
 //     @SubscribeEvent
 //     public void onRenderWorldEvent(RenderWorldLastEvent event) {
 //        System.out.println("onrenderworldevent");
 //        //firing
 //        Mk1Eyeball.getInstance().update();
 //        drawContacts(event.partialTicks);
 //     }
//
 //  @SideOnly(Side.CLIENT)
 //  private void drawContacts(float partialTicks) {
 //     EntityPlayer player = Minecraft.getMinecraft().thePlayer;
 //     if (player == null) {
 //        System.out.println("Player is null");
 //        return;
 //     }
//
 //     List<MCH_VisualContact> contacts = Mk1Eyeball.getInstance().contacts;
 //     if (contacts == null || contacts.isEmpty()) {
 //        System.out.println("No contacts found");
 //        return;
 //     }
//
 //     for (MCH_VisualContact contact : contacts) {
 //        System.out.println("Contact found at " + contact.x + ", " + contact.y + ", " + contact.z);
 //        if (player.getDistance(contact.x, contact.y, contact.z) <= 64) {
 //           System.out.println("Rendering contact within range");
 //           Mk1Eyeball.renderContact(contact, partialTicks);
 //        } else {
 //           System.out.println("Contact out of range");
 //        }
 //     }
 //  }

   @SubscribeEvent
   public void onLivingDeathEvent(LivingDeathEvent event) {
      if(event.entity instanceof EntityPlayerMP) {
         MCH_UavInventory.restorePilotInventory((EntityPlayerMP)event.entity, "player_death");
      }
   }

   @SubscribeEvent
   public void onStartTracking(PlayerEvent.StartTracking event) {
      if(!(event.entityPlayer instanceof EntityPlayerMP) || event.entityPlayer.worldObj.isRemote) {
         return;
      }

      MCH_EntityAircraft aircraft = null;
      if(event.target instanceof MCH_EntityAircraft) {
         aircraft = (MCH_EntityAircraft)event.target;
      } else if(event.target instanceof MCH_EntitySeat) {
         MCH_EntitySeat seat = (MCH_EntitySeat)event.target;
         aircraft = seat.getParent();
         if(aircraft == null && seat.parentUniqueID != null && !seat.parentUniqueID.isEmpty()) {
            for(Object object : event.entityPlayer.worldObj.loadedEntityList) {
               if(object instanceof MCH_EntityAircraft
                       && seat.parentUniqueID.equals(((MCH_EntityAircraft)object).getCommonUniqueId())) {
                  aircraft = (MCH_EntityAircraft)object;
                  seat.setParent(aircraft);
                  break;
               }
            }
         }
      }

      if(aircraft != null) {
         aircraft.syncCompleteAircraftState((EntityPlayerMP)event.entityPlayer);
      }
   }

   @SubscribeEvent
   public void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if(event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP && !event.player.worldObj.isRemote) {
         if(MCH_UavInventory.hasStoredPilotInventory(event.player) && !(event.player.ridingEntity instanceof MCH_EntityAircraft)) {
            MCH_UavInventory.restorePilotInventory((EntityPlayerMP)event.player, "not_piloting");
         }
      }
   }




   public void entitySpawn(EntityJoinWorldEvent event) {



      if(event.entity instanceof MCH_EntitySeat) {
         MCH_EntitySeat joinedSeat = (MCH_EntitySeat)event.entity;
         MCH_Lib.DbgLog(event.world,
                 "[MCH-TRACK][SEAT-JOIN] side=%s entityId=%d uuid=%s seatId=%d parentCommonId=%s parent=%s occupant=%s",
                 new Object[]{event.world.isRemote?"CLIENT":"SERVER", Integer.valueOf(joinedSeat.getEntityId()), joinedSeat.getUniqueID(),
                         Integer.valueOf(joinedSeat.seatID), joinedSeat.parentUniqueID, joinedSeat.getParent(), joinedSeat.riddenByEntity});
         if(!event.world.isRemote && joinedSeat.parentUniqueID != null && !joinedSeat.parentUniqueID.isEmpty()) {
            for(Object object : event.world.loadedEntityList) {
               if(object instanceof MCH_EntityAircraft
                       && joinedSeat.parentUniqueID.equals(((MCH_EntityAircraft)object).getCommonUniqueId())) {
                  MCH_EntityAircraft parent = (MCH_EntityAircraft)object;
                  joinedSeat.setParent(parent);
                  if(joinedSeat.seatID >= 0 && joinedSeat.seatID < parent.getSeats().length) {
                     parent.setSeat(joinedSeat.seatID, joinedSeat);
                  }
                  parent.repairSeatStateAfterLoad();
                  break;
               }
            }
         }
      } else if(W_Lib.isEntityLivingBase(event.entity) && !W_EntityPlayer.isPlayer(event.entity)) {
         MCH_Config var10002 = MCH_MOD.config;
         event.entity.renderDistanceWeight *= MCH_Config.MobRenderDistanceWeight.prmDouble;
      } else if(event.entity instanceof MCH_EntityAircraft) {
         MCH_EntityAircraft joinedAircraft = (MCH_EntityAircraft)event.entity;
         MCH_Lib.DbgLog(event.world,
                 "[MCH-TRACK][AIRCRAFT-JOIN] side=%s entityId=%d uuid=%s class=%s type=%s commonId=%s",
                 new Object[]{event.world.isRemote?"CLIENT":"SERVER", Integer.valueOf(joinedAircraft.getEntityId()),
                         joinedAircraft.getUniqueID(), joinedAircraft.getClass().getName(), joinedAircraft.getTypeName(), joinedAircraft.getCommonUniqueId()});
         joinedAircraft.debugVehicleState("ENTITY-JOIN", null);
         joinedAircraft.debugRackState("ENTITY-JOIN");
         //reload aircraft render setting here
         //if (event.world.isRemote) {
//
         //   if (event.entity instanceof MCH_EntityAircraft && acloaded == 0) {
         //      MCH_EntityAircraft ac = (MCH_EntityAircraft) event.entity;
//
         //      // Safely call getAcInfo() on the instance
         //      if (ac.getAcInfo() != null) {
         //         ac.getAcInfo().reload();
         //         acloaded++;
         //      }
         //   }
         //}
         MCH_EntityAircraft b = (MCH_EntityAircraft)event.entity;
         if(!event.world.isRemote && (b.isUAV() || b.isNewUAV())) {
            MCH_UavRegistry.register(b);
         }
         if(!b.worldObj.isRemote && !b.isCreatedSeats()) {
            b.createSeats(UUID.randomUUID().toString());
         }
         if(!b.worldObj.isRemote) {
            b.repairSeatStateAfterLoad();
         }
      } else if(W_EntityPlayer.isPlayer(event.entity)) {
         Entity e = event.entity;
         boolean b1 = Float.isNaN(e.rotationPitch);
         b1 |= Float.isNaN(e.prevRotationPitch);
         b1 |= Float.isInfinite(e.rotationPitch);
         b1 |= Float.isInfinite(e.prevRotationPitch);
         if(b1) {
            MCH_Lib.Log(event.entity, "### EntityJoinWorldEvent Error:Player invalid rotation pitch(" + e.rotationPitch + ")", new Object[0]);
            e.rotationPitch = 0.0F;
            e.prevRotationPitch = 0.0F;
         }

         b1 = Float.isInfinite(e.rotationYaw);
         b1 |= Float.isInfinite(e.prevRotationYaw);
         b1 |= Float.isNaN(e.rotationYaw);
         b1 |= Float.isNaN(e.prevRotationYaw);
         if(b1) {
            MCH_Lib.Log(event.entity, "### EntityJoinWorldEvent Error:Player invalid rotation yaw(" + e.rotationYaw + ")", new Object[0]);
            e.rotationYaw = 0.0F;
            e.prevRotationYaw = 0.0F;
         }

         if(!e.worldObj.isRemote && event.entity instanceof EntityPlayerMP) {
            MCH_Lib.DbgLog(false, "EntityJoinWorldEvent:" + event.entity, new Object[0]);
            MCH_PacketNotifyServerSettings.send((EntityPlayerMP)event.entity);
            MCH_UavRegistry.rebuildUavRegistry(event.entity.worldObj);
            if(MCH_UavInventory.hasStoredPilotInventory((EntityPlayer)event.entity)) {
               MCH_UavInventory.restorePilotInventory((EntityPlayerMP)event.entity, "player_join");
            }
         }
      }

   }

   public void livingAttackEvent(LivingAttackEvent event) {
      MCH_EntityAircraft ac = this.getRiddenAircraft(event.entity);
      if(ac != null) {
         if(ac.getAcInfo() != null) {
            if(!ac.isDestroyed()) {
               if(ac.getAcInfo().damageFactor <= 0.0F) {
                  Entity attackEntity = event.source.getEntity();
                  if(attackEntity == null) {
                     event.setCanceled(true);
                  } else if(W_Entity.isEqual(attackEntity, event.entity)) {
                     event.setCanceled(true);
                  } else if(ac.isMountedEntity(attackEntity)) {
                     event.setCanceled(true);
                  } else {
                     MCH_EntityAircraft atkac = this.getRiddenAircraft(attackEntity);
                     if(W_Entity.isEqual(atkac, ac)) {
                        event.setCanceled(true);
                     }
                  }

               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onLivingAttack(LivingAttackEvent event) {
      if (event.source != null && event.source.getEntity() == event.entityLiving) {
         // Player is attacking themselves — block it.
         event.setCanceled(true);
      }
   }

   public void livingHurtEvent(LivingHurtEvent event) {
           MCH_EntityAircraft ac = getRiddenAircraft(event.entity);
           if (ac != null &&
                     ac.getAcInfo() != null) {
               if (ac.isNewUAV()) {
                     event.setCanceled(true);
                     return;
                   }
                if (!ac.isDestroyed()) {
                     Entity attackEntity = event.source.getEntity();
                     if (attackEntity == null) {
                          ac.attackEntityFrom(event.source, event.ammount * 2.0F);
                          event.ammount *= (ac.getAcInfo()).damageFactor;
                        } else if (W_Entity.isEqual(attackEntity, event.entity)) {
                          ac.attackEntityFrom(event.source, event.ammount * 2.0F);
                          event.ammount *= (ac.getAcInfo()).damageFactor;
                        } else if (ac.isMountedEntity(attackEntity)) {
                          event.ammount = 0.0F;
                          event.setCanceled(true);
                        } else {
                          MCH_EntityAircraft atkac = getRiddenAircraft(attackEntity);
                          if (W_Entity.isEqual((Entity)atkac, (Entity)ac)) {
                               event.ammount = 0.0F;
                               event.setCanceled(true);
                             } else {
                               ac.attackEntityFrom(event.source, event.ammount * 2.0F);
                               event.ammount *= (ac.getAcInfo()).damageFactor;
                             }
                        }
                   }
              }
         }

   public MCH_EntityAircraft getRiddenAircraft(Entity entity) {
      MCH_EntityAircraft ac = null;
      Entity ridden = entity.ridingEntity;
      if(ridden instanceof MCH_EntityAircraft) {
         ac = (MCH_EntityAircraft)ridden;
      } else if(ridden instanceof MCH_EntitySeat) {
         ac = ((MCH_EntitySeat)ridden).getParent();
      }

      if(ac == null) {
         //50x50x50 area to test for the parent aircraft
         //nice, but it could be better.
         List list = entity.worldObj.getEntitiesWithinAABB(MCH_EntityAircraft.class, entity.boundingBox.expand(50.0D, 50.0D, 50.0D));
         if(list != null) {
            for(int i = 0; i < list.size(); ++i) {
               MCH_EntityAircraft tmp = (MCH_EntityAircraft)list.get(i);
               if(tmp.isMountedEntity(entity)) {
                  return tmp;
               }
            }
         }
      }

      return ac;
   }

   public void entityInteractEvent(EntityInteractEvent event) {
      if(event.target instanceof MCH_EntityAircraft || event.target instanceof MCH_EntitySeat) {
         MCH_Lib.DbgLog(event.entityPlayer.worldObj,
                 "[MCH-INTERACT][FORGE-EVENT] side=%s target=%s targetId=%d targetUuid=%s player=%s playerUuid=%s canceled=%s",
                 new Object[]{event.entityPlayer.worldObj.isRemote?"CLIENT":"SERVER", event.target.getClass().getName(),
                         Integer.valueOf(event.target.getEntityId()), event.target.getUniqueID(), event.entityPlayer.getCommandSenderName(),
                         event.entityPlayer.getUniqueID(), Boolean.valueOf(event.isCanceled())});
         if(event.target instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft)event.target).debugVehicleState("FORGE-INTERACT-EVENT", event.entityPlayer);
            ((MCH_EntityAircraft)event.target).debugRackState("FORGE-INTERACT-EVENT");
         }
      }
      ItemStack item = event.entityPlayer.getHeldItem();
      if(item != null) {
         if(item.getItem() instanceof MCH_ItemChain) {
            MCH_ItemChain.interactEntity(item, event.target, event.entityPlayer, event.entityPlayer.worldObj);
            event.setCanceled(true);
         } else if(item.getItem() instanceof MCH_ItemAircraft) {
            ((MCH_ItemAircraft)item.getItem()).rideEntity(item, event.target, event.entityPlayer);
         }

      }
   }

   //@SubscribeEvent
   //public void onWorldTick(TickEvent.WorldTickEvent event) {
   //   if (event.phase != TickEvent.Phase.END) return;
//
   //   World world = event.world;
   //   List<Entity> loaded = world.loadedEntityList;
   //   int bulletCount = 0;
   //   List<MCH_EntityBaseBullet> excessBullets = new ArrayList<>();
//
   //   for (Object obj : loaded) {
//
   //      if (obj instanceof MCH_EntityBaseBullet) {
//
   //         MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) obj;
   //         bulletCount++;
//
   //         // Only mark for removal if it's NOT a chunk-loading bullet and has been idle
   //         if (!bullet.shouldLoadChunks() && bullet.idleStartTime > 0) {
   //            System.out.println("bullet count" + bulletCount);
   //            excessBullets.add(bullet);
   //         }
   //      }
   //   }
//
   //   if (bulletCount > 1000) {
   //      int bulletsToKill = Math.min(200, excessBullets.size());
   //      for (int i = 0; i < bulletsToKill; i++) {
   //         excessBullets.get(i).setDead();
   //      }
//
   //      System.out.println("Bullet cleanup triggered: removed " + bulletsToKill + " non-chunkloading idle bullets.");
   //   }
   //}
   //this fucking class makes me schizophrenic

   public void entityCanUpdate(CanUpdate event) {
      //ooh I have a new idea here
      //todo: let's say this is cache right? ok, well guess what cache,
      // what if I want you to render every fucking vehicle ever placed?
      // the benefit of this is we don't have to make some new stupid fucking eventhook since this retarded mod seems
      // to absolutely fucking hate when I do that for some reason because it bounces between two completely fucking schizophrenic
      // classes for some reason
      // Force all aircraft to always tick
      //if (event.entity instanceof MCH_EntityAircraft) {
      //   //if this works how I hope it will we should probably add
      //   //ac.getRiddenByEntity() != null so we ensure we only tick PLAYER vehicles
      //   event.canUpdate = true;
      //} //I wonder if this will work
      //it did not work.

      if(event.entity instanceof MCH_EntityBaseBullet) {
         MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)event.entity;
         // todo: maybe meddle with this to see if it can maybe preserve the bullet if unloaded and having a gravity going down

         //bullet.setDead();
         //WHY ARE YOU SETTING DEAD HERE YOU DONT HAVE A REASON TO
         //todo add a new chunk loader here under the strict criteria that the bullet is still alive and has a gravity going down
         if (bullet.shouldLoadChunks()) {
            System.out.println("should load chunks1");
            bullet.idleStartTime = -1;
            //17140 ticks = ~14 minutes
            //which is how far an apfsds bullet should be allowed to go according to my genericified math

            long timePassed = bullet.worldObj.getTotalWorldTime() - bullet.idleStartTime;

            if (bullet.bomblet) {
               if (timePassed > 300) { // 600 ticks = 30 seconds
                  bullet.setDead();
                  System.out.println("'Chunk loading' Bomblet set dead after being idle for 15 seconds.");
               }
            }

            //System.out.println("bullet checking and loading chunks");
            if (bullet.ticksExisted > 2) {
               bullet.checkAndLoadChunks();
            }

            //fail safe 5hrs
            if (bullet.ticksExisted > 36000) {
               System.out.println("Bullet exceeded 5 hours of existence, setting dead. Report to Developer:" + bullet.getName());
               bullet.setDead();
            }

         } else {
            if (bullet.idleStartTime < 0) {
               // Start the idle timer
               bullet.idleStartTime = bullet.worldObj.getTotalWorldTime();
            } else {
               long timePassed = bullet.worldObj.getTotalWorldTime() - bullet.idleStartTime;
               if (!bullet.bomblet) { // 2400 ticks = 2 minutes
                  if (timePassed > 2400) {
                     bullet.setDead();
                     System.out.println("Non chunk loading Bullet set dead after being idle for 2 minutes.");
                  }
               } else {
                  if (timePassed > 300) { // 600 ticks = 30 seconds
                     bullet.setDead();
                     System.out.println("Bomblet set dead after being idle for 15 seconds.");
                  }
                  //despawn bomblets way faster

               }
               //todo or >25 bullets loaded start despawning them
            }
         }
         //this was so retarded to implement I hope it works
         //REDFLAG: bullet.setDead();
      }

   }
}
