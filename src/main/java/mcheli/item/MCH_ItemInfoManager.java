package mcheli.item;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import mcheli.MCH_ResourceHelper;
import net.minecraft.item.Item;

public class MCH_ItemInfoManager {
   private static volatile Map map = new LinkedHashMap();
   private static String lastPath;

   public static boolean load(String path) { lastPath = path; return loadAndPublish(path, false); }
   public static boolean reload() { return lastPath != null && loadAndPublish(lastPath, true); }

   private static boolean loadAndPublish(String path, boolean reload) {
      Map oldMap = map;
      LinkedHashMap newMap = new LinkedHashMap();
      List<String> entries = MCH_ResourceHelper.listResources(path.replace('\\', '/') + "item", ".txt");
      if(entries == null || entries.isEmpty()) return false;
      for(int i = 0; i < entries.size(); ++i) {
         String resourcePath = entries.get(i);
         MCH_InputFile inFile = new MCH_InputFile();
         int line = 0;
         try {
            String name = MCH_ResourceHelper.getEntryName(resourcePath);
            if(!newMap.containsKey(name) && inFile.openClasspath("/" + resourcePath)) {
               MCH_ItemInfo info = new MCH_ItemInfo(name);
               String str;
               while((str = inFile.readLine()) != null) {
                  ++line; str = str.trim(); int eqIdx = str.indexOf(61);
                  if(eqIdx >= 0 && str.length() > eqIdx + 1) info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
               }

               newMap.put(name, info);
            }
         } catch(Exception e) {
            MCH_Lib.Log("### Reload failed %s : line=%d; keeping previous item data", new Object[]{resourcePath, Integer.valueOf(line)});
            e.printStackTrace(); return false;
         } finally { inFile.close(); }
      }
      if(newMap.isEmpty()) return false;
      if(reload) for(Object key : newMap.keySet()) {
         MCH_ItemInfo oldInfo = (MCH_ItemInfo)oldMap.get(key);
         MCH_ItemInfo newInfo = (MCH_ItemInfo)newMap.get(key);
         if(oldInfo != null) { newInfo.item = oldInfo.item; newInfo.itemID = oldInfo.itemID;  }
         else MCH_Lib.Log("### New item definition %s requires item registration; restart the game", new Object[]{key});
      }
      map = newMap;
      MCH_Lib.Log("Read %d item", new Object[]{Integer.valueOf(newMap.size())});
      return true;
   }

   public static MCH_ItemInfo get(String name) { return (MCH_ItemInfo)map.get(name); }
   public static MCH_ItemInfo get(Item item) { Map snapshot = map; for(Object value : snapshot.values()) { MCH_ItemInfo info = (MCH_ItemInfo)value; if(info.item == item) return info; } return null; }
   public static boolean contains(String name) { return map.containsKey(name); }
   public static Set getKeySet() { return map.keySet(); }
   public static Collection getValues() { return map.values(); }
}
