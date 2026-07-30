package mcheli.helicopter;

import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import net.minecraft.item.Item;

public class MCH_HeliInfoManager extends MCH_BaseVehicleInfoManager {
   private static final MCH_HeliInfoManager instance = new MCH_HeliInfoManager();
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) { MCH_Lib.Log("### New vehicle definition %s requires item registration; restart the game", new Object[]{name}); }

   public static volatile Map map = new LinkedHashMap();

   public static MCH_HeliInfoManager getInstance() { return instance; }
   public static MCH_HeliInfo get(String name) { return (MCH_HeliInfo)map.get(name); }
   public MCH_BaseInfo newInfo(String name) { return new MCH_HeliInfo(name); }
   public Map getMap() { return map; }
   protected void setMap(Map newMap) { map = newMap; MCH_BaseVehicleInfo.rebuildGlobalRegistry(); }

   protected void preserveRuntimeState(MCH_BaseInfo oldValue, MCH_BaseInfo newValue) {
      MCH_HeliInfo oldInfo = (MCH_HeliInfo)oldValue;
      MCH_HeliInfo newInfo = (MCH_HeliInfo)newValue;
      newInfo.item = oldInfo.item;
      newInfo.itemID = oldInfo.itemID;
      newInfo.model = oldInfo.model;
   }

   public static MCH_HeliInfo getFromItem(Item item) { return getInstance().getAcInfoFromItem(item); }
   public MCH_HeliInfo getAcInfoFromItem(Item item) {
      if(item == null) return null;
      Map snapshot = map;
      for(Object value : snapshot.values()) {
         MCH_HeliInfo info = (MCH_HeliInfo)value;
         if(info.item == item) return info;
      }
      return null;
   }
}
