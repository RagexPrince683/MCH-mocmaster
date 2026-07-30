package mcheli.plane;

import java.util.LinkedHashMap;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_BaseVehicleInfoManager;
import net.minecraft.item.Item;

public class MCP_PlaneInfoManager extends MCH_BaseVehicleInfoManager {
   private static final MCP_PlaneInfoManager instance = new MCP_PlaneInfoManager();
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) { MCH_Lib.Log("### New vehicle definition %s requires item registration; restart the game", new Object[]{name}); }

   public static volatile Map map = new LinkedHashMap();

   public static MCP_PlaneInfoManager getInstance() { return instance; }
   public static MCP_PlaneInfo get(String name) { return (MCP_PlaneInfo)map.get(name); }
   public MCH_BaseInfo newInfo(String name) { return new MCP_PlaneInfo(name); }
   public Map getMap() { return map; }
   protected void setMap(Map newMap) { map = newMap; MCH_BaseVehicleInfo.rebuildGlobalRegistry(); }

   protected void preserveRuntimeState(MCH_BaseInfo oldValue, MCH_BaseInfo newValue) {
      MCP_PlaneInfo oldInfo = (MCP_PlaneInfo)oldValue;
      MCP_PlaneInfo newInfo = (MCP_PlaneInfo)newValue;
      newInfo.item = oldInfo.item;
      newInfo.itemID = oldInfo.itemID;
      newInfo.model = oldInfo.model;
   }

   public static MCP_PlaneInfo getFromItem(Item item) { return getInstance().getAcInfoFromItem(item); }
   public MCP_PlaneInfo getAcInfoFromItem(Item item) {
      if(item == null) return null;
      Map snapshot = map;
      for(Object value : snapshot.values()) {
         MCP_PlaneInfo info = (MCP_PlaneInfo)value;
         if(info.item == item) return info;
      }
      return null;
   }
}
