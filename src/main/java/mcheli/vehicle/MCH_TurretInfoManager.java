package mcheli.vehicle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import mcheli.vehicle.MCH_TurretInfo;
import net.minecraft.item.Item;

public class MCH_TurretInfoManager extends MCH_BaseVehicleInfoManager {

   private static MCH_TurretInfoManager instance = new MCH_TurretInfoManager();
   public static HashMap map = new LinkedHashMap();


   public static MCH_TurretInfo get(String name) {
      return (MCH_TurretInfo)map.get(name);
   }

   public static MCH_TurretInfoManager getInstance() {
      return instance;
   }

   public MCH_BaseInfo newInfo(String name) {
      return new MCH_TurretInfo(name);
   }

   public Map getMap() {
      return map;
   }

   public static MCH_TurretInfo getFromItem(Item item) {
      return getInstance().getAcInfoFromItem(item);
   }

   public MCH_TurretInfo getAcInfoFromItem(Item item) {
      if(item == null) {
         return null;
      } else {
         Iterator i$ = map.values().iterator();

         MCH_TurretInfo info;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            info = (MCH_TurretInfo)i$.next();
         } while(info.item != item);

         return info;
      }
   }

}
