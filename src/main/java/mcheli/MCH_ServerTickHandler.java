package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import mcheli.sensors.MCH_ESMHandler;
import mcheli.wrapper.W_Reflection;
import net.minecraft.network.NetworkManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class MCH_ServerTickHandler {

   HashMap rcvMap = new HashMap();
   HashMap sndMap = new HashMap();
   int sndPacketNum = 0;
   int rcvPacketNum = 0;
   int tick;


   @SubscribeEvent
   public void onServerTickEvent(ServerTickEvent event) {
      Phase var10001 = event.phase;
      if(event.phase == Phase.START) {
    	  
         ;
      }

      var10001 = event.phase;
      if(event.phase == Phase.END) {
    	  MCH_ESMHandler.getInstance().onTick();
         ;
      }

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
