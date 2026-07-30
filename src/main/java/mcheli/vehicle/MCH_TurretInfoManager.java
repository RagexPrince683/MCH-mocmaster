package mcheli.vehicle;

import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import net.minecraft.item.Item;

public class MCH_TurretInfoManager extends MCH_BaseVehicleInfoManager {
   private static final MCH_TurretInfoManager instance = new MCH_TurretInfoManager();
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) { MCH_Lib.Log("### New vehicle definition %s requires item registration; restart the game", new Object[]{name}); }

   public static volatile Map map = new LinkedHashMap();

   public static MCH_TurretInfoManager getInstance() { return instance; }
   public static MCH_TurretInfo get(String name) { return (MCH_TurretInfo)map.get(name); }
   public MCH_BaseInfo newInfo(String name) { return new MCH_TurretInfo(name); }
   public Map getMap() { return map; }
   protected void setMap(Map newMap) { map = newMap; MCH_BaseVehicleInfo.rebuildGlobalRegistry(); }

   protected void preserveRuntimeState(MCH_BaseInfo oldValue, MCH_BaseInfo newValue) {
      MCH_TurretInfo oldInfo = (MCH_TurretInfo)oldValue;
      MCH_TurretInfo newInfo = (MCH_TurretInfo)newValue;
      newInfo.item = oldInfo.item;
      newInfo.itemID = oldInfo.itemID;
      newInfo.model = oldInfo.model;
   }

   public static MCH_TurretInfo getFromItem(Item item) { return getInstance().getAcInfoFromItem(item); }
   public MCH_TurretInfo getAcInfoFromItem(Item item) {
      if(item == null) return null;
      Map snapshot = map;
      for(Object value : snapshot.values()) {
         MCH_TurretInfo info = (MCH_TurretInfo)value;
         if(info.item == item) return info;
      }
      return null;
   }
}
