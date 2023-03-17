package mcheli.tank;

import mcheli.MCH_BaseInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_TankInfoManager extends MCH_AircraftInfoManager {

   private static MCH_TankInfoManager instance = new MCH_TankInfoManager();
   public static HashMap map = new LinkedHashMap();


   public static MCH_TankInfo get(String name) {
      return (MCH_TankInfo)map.get(name);
   }

   public static MCH_TankInfoManager getInstance() {
      return instance;
   }

   public MCH_BaseInfo newInfo(String name) {
      return new MCH_TankInfo(name);
   }

   public Map getMap() {
      return map;
   }

   public static MCH_TankInfo getFromItem(Item item) {
      return getInstance().getAcInfoFromItem(item);
   }

   public MCH_TankInfo getAcInfoFromItem(Item item) {
      if(item == null) {
         return null;
      } else {
         Iterator i$ = map.values().iterator();

         MCH_TankInfo info;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            info = (MCH_TankInfo)i$.next();
         } while(info.item != item);

         return info;
      }
   }

}
