package mcheli;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import mcheli.chain.MCH_ItemChain;
import mcheli.command.MCH_Command;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

public class MCH_EventHook extends W_EventHook {

   int acloaded = 0;

   public void commandEvent(CommandEvent event) {
      MCH_Command.onCommandEvent(event);
   }

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



   public void entitySpawn(EntityJoinWorldEvent event) {



      if(W_Lib.isEntityLivingBase(event.entity) && !W_EntityPlayer.isPlayer(event.entity)) {
         MCH_Config var10002 = MCH_MOD.config;
         event.entity.renderDistanceWeight *= MCH_Config.MobRenderDistanceWeight.prmDouble;
      } else if(event.entity instanceof MCH_EntityAircraft) {
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
         if(!b.worldObj.isRemote && !b.isCreatedSeats()) {
            b.createSeats(UUID.randomUUID().toString());
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

   public void entityCanUpdate(CanUpdate event) {
      if(event.entity instanceof MCH_EntityBaseBullet) {
         MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)event.entity;
         // todo: maybe meddle with this to see if it can maybe preserve the bullet if unloaded and having a gravity going down
         System.out.println("set dead in eventhook entitycanupdate");
         bullet.setDead();
         //REDFLAG: bullet.setDead();
      }

   }
}
