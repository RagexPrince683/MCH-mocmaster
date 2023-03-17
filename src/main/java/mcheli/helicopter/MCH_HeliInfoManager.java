package mcheli.helicopter;

import mcheli.MCH_BaseInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_HeliInfoManager extends MCH_AircraftInfoManager {

   private static final MCH_HeliInfoManager instance = new MCH_HeliInfoManager();
   public static final HashMap map = new LinkedHashMap();


   public static MCH_HeliInfoManager getInstance() {
      return instance;
   }

   public static MCH_HeliInfo get(String name) {
      return (MCH_HeliInfo)map.get(name);
   }

   public MCH_BaseInfo newInfo(String name) {
      return new MCH_HeliInfo(name);
   }

   public Map getMap() {
      return map;
   }

   public static MCH_HeliInfo getFromItem(Item item) {
      return getInstance().getAcInfoFromItem(item);
   }

   public MCH_HeliInfo getAcInfoFromItem(Item item) {
      if(item == null) {
         return null;
      } else {
         Iterator i$ = map.values().iterator();

         MCH_HeliInfo info;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            info = (MCH_HeliInfo)i$.next();
         } while(info.item != item);

         return info;
      }
   }

}
