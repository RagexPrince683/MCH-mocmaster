package mcheli.ship;

import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import net.minecraft.item.Item;

public class MCH_ShipInfoManager extends MCH_BaseVehicleInfoManager {
   private static final MCH_ShipInfoManager instance = new MCH_ShipInfoManager();
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) { MCH_Lib.Log("### New vehicle definition %s requires item registration; restart the game", new Object[]{name}); }

   public static volatile Map map = new LinkedHashMap();

   public static MCH_ShipInfoManager getInstance() { return instance; }
   public static MCH_ShipInfo get(String name) { return (MCH_ShipInfo)map.get(name); }
   public MCH_BaseInfo newInfo(String name) { return new MCH_ShipInfo(name); }
   public Map getMap() { return map; }
   protected void setMap(Map newMap) { map = newMap; MCH_BaseVehicleInfo.rebuildGlobalRegistry(); }

   protected void preserveRuntimeState(MCH_BaseInfo oldValue, MCH_BaseInfo newValue) {
      MCH_ShipInfo oldInfo = (MCH_ShipInfo)oldValue;
      MCH_ShipInfo newInfo = (MCH_ShipInfo)newValue;
      newInfo.item = oldInfo.item;
      newInfo.itemID = oldInfo.itemID;
      newInfo.model = oldInfo.model;
   }

   public static MCH_ShipInfo getFromItem(Item item) { return getInstance().getAcInfoFromItem(item); }
   public MCH_ShipInfo getAcInfoFromItem(Item item) {
      if(item == null) return null;
      Map snapshot = map;
      for(Object value : snapshot.values()) {
         MCH_ShipInfo info = (MCH_ShipInfo)value;
         if(info.item == item) return info;
      }
      return null;
   }
}
