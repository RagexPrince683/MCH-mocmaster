package mcheli.vehicle;

import mcheli.MCH_BaseInfo;
import mcheli.aircraft.MCH_AircraftInfoManager;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MCH_VehicleInfoManager extends MCH_AircraftInfoManager {

   private static MCH_VehicleInfoManager instance = new MCH_VehicleInfoManager();
   public static HashMap map = new LinkedHashMap();


   public static MCH_VehicleInfo get(String name) {
      return (MCH_VehicleInfo)map.get(name);
   }

   public static MCH_VehicleInfoManager getInstance() {
      return instance;
   }

   public MCH_BaseInfo newInfo(String name) {
      return new MCH_VehicleInfo(name);
   }

   public Map getMap() {
      return map;
   }

   public static MCH_VehicleInfo getFromItem(Item item) {
      return getInstance().getAcInfoFromItem(item);
   }

   public MCH_VehicleInfo getAcInfoFromItem(Item item) {
      if(item == null) {
         return null;
      } else {
         Iterator i$ = map.values().iterator();

         MCH_VehicleInfo info;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            info = (MCH_VehicleInfo)i$.next();
         } while(info.item != item);

         return info;
      }
   }

}
