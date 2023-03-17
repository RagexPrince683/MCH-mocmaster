package mcheli.plane;

import mcheli.MCH_BaseInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MCP_PlaneInfoManager extends MCH_AircraftInfoManager {

   private static MCP_PlaneInfoManager instance = new MCP_PlaneInfoManager();
   public static HashMap map = new LinkedHashMap();


   public static MCP_PlaneInfo get(String name) {
      return (MCP_PlaneInfo)map.get(name);
   }

   public static MCP_PlaneInfoManager getInstance() {
      return instance;
   }

   public MCH_BaseInfo newInfo(String name) {
      return new MCP_PlaneInfo(name);
   }

   public Map getMap() {
      return map;
   }

   public static MCP_PlaneInfo getFromItem(Item item) {
      return getInstance().getAcInfoFromItem(item);
   }

   public MCP_PlaneInfo getAcInfoFromItem(Item item) {
      if(item == null) {
         return null;
      } else {
         Iterator i$ = map.values().iterator();

         MCP_PlaneInfo info;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            info = (MCP_PlaneInfo)i$.next();
         } while(info.item != item);

         return info;
      }
   }

}
