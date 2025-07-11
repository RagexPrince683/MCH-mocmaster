package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

import java.util.*;

import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketAircraftLocation;
import mcheli.plane.MCP_EntityPlane;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Reflection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class MCH_ServerTickHandler {

   HashMap rcvMap = new HashMap();
   HashMap sndMap = new HashMap();
   int sndPacketNum = 0;
   int rcvPacketNum = 0;
   int tick;


   @SubscribeEvent
   public void onServerTickEvent(ServerTickEvent event) {
      if (event.phase != Phase.END) return;
      //MCH_ESMHandler.getInstance().onTick();
      MinecraftServer minecraftServer = MinecraftServer.getServer();

      for (WorldServer server : MinecraftServer.getServer().worldServers) {
         for (Object playerObj : server.playerEntities) {
            EntityPlayer player = (EntityPlayer) playerObj;
            AxisAlignedBB aabb = player.boundingBox.expand(350, 350, 350);
            List<MCP_EntityPlane> list = new ArrayList<>();
            List<Entity> entities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);
            for (Entity e : entities) {
               if (e instanceof MCP_EntityPlane && !e.onGround) {
                  MCP_EntityPlane plane = (MCP_EntityPlane) e;
                  list.add(plane);
                  //System.out.println("server tick handler");
                  MCH_PacketAircraftLocation.send(plane, player);
               }
            }
         }
      }
   }

   double visualDistance = 2500;
   @SubscribeEvent
   void onWorldTick(TickEvent.WorldTickEvent evt) {
      System.out.println("onworldtick works");
      //inb4 this shit never fires and its more goddamn schizophrenic bullshit from this fuckass mod
      World worldObj = evt.world;

      // --- Existing MCH plane syncing logic ---
      for (Object obj : worldObj.playerEntities) {
         if (obj instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) obj;
            AxisAlignedBB aabb = player.boundingBox.expand(visualDistance, visualDistance, visualDistance);
            List<MCP_EntityPlane> list = new ArrayList<>();
            for (Object entityObj : worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb)) {
               if (entityObj instanceof MCP_EntityPlane) {
                  MCP_EntityPlane plane = (MCP_EntityPlane) entityObj;
                  if (!plane.onGround) {
                     list.add(plane);
                     MCH_PacketAircraftLocation.send(plane, player);
                  }
               }
            }
         }
      }

      // --- NEW: Bullet despawn logic ---
      //List<Entity> loaded = worldObj.loadedEntityList;
      //int bulletCount = 0;
      //List<MCH_EntityBaseBullet> excessBullets = new ArrayList<>();
//
      //for (Object obj : loaded) {
      //   if (obj instanceof MCH_EntityBaseBullet) {
      //      MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet) obj;
      //      bulletCount++;
//
      //      if (!bullet.shouldLoadChunks() && bullet.idleStartTime > 0) {
      //         System.out.println("NOT should load chunks");
      //         excessBullets.add(bullet);
      //      }
      //   }
      //}
//
      //if (bulletCount > 1000) {
      //   int bulletsToKill = Math.min(200, excessBullets.size());
      //   for (int i = 0; i < bulletsToKill; i++) {
      //      excessBullets.get(i).setDead();
      //   }
      //   System.out.println("Bullet cleanup triggered: removed " + bulletsToKill + " bullets.");
      //}
      //this shit does NOT work. I FUCKING LOVE THIS MOD'S SCHIZOPHRENIA
   }

   private void onServerTickPre() {
      ++this.tick;
      List list = W_Reflection.getNetworkManagers();
      if(list != null) {
         for(int i = 0; i < list.size(); ++i) {
            Queue queue = W_Reflection.getReceivedPacketsQueue((NetworkManager)list.get(i));
            if(queue != null) {
               this.putMap(this.rcvMap, queue.iterator());
               this.rcvPacketNum += queue.size();
            }

            queue = W_Reflection.getSendPacketsQueue((NetworkManager)list.get(i));
            if(queue != null) {
               this.putMap(this.sndMap, queue.iterator());
               this.sndPacketNum += queue.size();
            }
         }
      }

      if(this.tick >= 20) {
         this.tick = 0;
         this.rcvPacketNum = this.sndPacketNum = 0;
         this.rcvMap.clear();
         this.sndMap.clear();
      }

   }

   public void putMap(HashMap map, Iterator iterator) {
      while(iterator.hasNext()) {
         Object o = iterator.next();
         String key = o.getClass().getName().toString();
         if(key.startsWith("net.minecraft.")) {
            key = "Minecraft";
         } else if(o instanceof FMLProxyPacket) {
            FMLProxyPacket p = (FMLProxyPacket)o;
            key = p.channel();
         } else {
            key = "Unknown!";
         }

         if(map.containsKey(key)) {
            map.put(key, Integer.valueOf(1 + ((Integer)map.get(key)).intValue()));
         } else {
            map.put(key, Integer.valueOf(1));
         }
      }

   }

   private void onServerTickPost() {}
}
