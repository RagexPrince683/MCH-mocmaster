package mcheli.tank;

import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import net.minecraft.item.Item;

public class MCH_TankInfoManager extends MCH_BaseVehicleInfoManager {
   private static final MCH_TankInfoManager instance = new MCH_TankInfoManager();
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) { MCH_Lib.Log("### New vehicle definition %s requires item registration; restart the game", new Object[]{name}); }

   public static volatile Map map = new LinkedHashMap();

   public static MCH_TankInfoManager getInstance() { return instance; }
   public static MCH_TankInfo get(String name) { return (MCH_TankInfo)map.get(name); }
   public MCH_BaseInfo newInfo(String name) { return new MCH_TankInfo(name); }
   public Map getMap() { return map; }
   protected void setMap(Map newMap) { map = newMap; MCH_BaseVehicleInfo.rebuildGlobalRegistry(); }

   protected void preserveRuntimeState(MCH_BaseInfo oldValue, MCH_BaseInfo newValue) {
      MCH_TankInfo oldInfo = (MCH_TankInfo)oldValue;
      MCH_TankInfo newInfo = (MCH_TankInfo)newValue;
      newInfo.item = oldInfo.item;
      newInfo.itemID = oldInfo.itemID;
      newInfo.model = oldInfo.model;
   }

   public static MCH_TankInfo getFromItem(Item item) { return getInstance().getAcInfoFromItem(item); }
   public MCH_TankInfo getAcInfoFromItem(Item item) {
      if(item == null) return null;
      Map snapshot = map;
      for(Object value : snapshot.values()) {
         MCH_TankInfo info = (MCH_TankInfo)value;
         if(info.item == item) return info;
      }
      return null;
   }
}
