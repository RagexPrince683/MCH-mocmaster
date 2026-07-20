package mcheli.weapon;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_ResourceHelper;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.wrapper.W_Item;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MCH_WeaponInfoManager {

   private static MCH_WeaponInfoManager instance = new MCH_WeaponInfoManager();
   private static HashMap map;
   private static String lastPath;


   private MCH_WeaponInfoManager() {
      map = new HashMap();
   }

   public static boolean reload() {
      boolean ret = false;

      try {
         map.clear();
         ret = load(lastPath);
         setRoundItems();
         MCH_MOD.proxy.registerModels();
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      return ret;
   }

   public static boolean load(String path) {
      lastPath = path;
      path = path.replace('\\', '/');
      String dirPrefix = path + "weapons";
      List<String> entries = MCH_ResourceHelper.listResources(dirPrefix, ".txt");
      if(entries != null && entries.size() > 0) {
         for(int i = 0; i < entries.size(); ++i) {
            String resourcePath = entries.get(i);
            BufferedReader br = null;
            int line = 0;

            try {
               String e = MCH_ResourceHelper.getEntryName(resourcePath);
               if(!map.containsKey(e)) {
                  br = MCH_ResourceHelper.openResource("/" + resourcePath);
                  if (br == null) continue;
                  MCH_WeaponInfo info = new MCH_WeaponInfo(e);

                  String str;
                  while((str = br.readLine()) != null) {
                     ++line;
                     str = str.trim();
                     int eqIdx = str.indexOf(61);
                     if(eqIdx >= 0 && str.length() > eqIdx + 1) {
                        info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                     }
                  }

                  info.checkData();
                  map.put(e, info);
               }
            } catch (IOException var22) {
               if(line > 0) {
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{resourcePath, Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{resourcePath});
               }

               var22.printStackTrace();
            } finally {
               try {
                  if(br != null) {
                     br.close();
                  }
               } catch (Exception var21) {
                  ;
               }

            }
         }

         MCH_Lib.Log("[mcheli] Read %d weapons", new Object[]{Integer.valueOf(map.size())});
         return map.size() > 0;
      } else {
         return false;
      }
   }

   public static void setRoundItems() {
      Iterator i$ = map.values().iterator();

      while(i$.hasNext()) {
         MCH_WeaponInfo w = (MCH_WeaponInfo)i$.next();

         MCH_WeaponInfo.RoundItem r;
         Item item;
         for(Iterator i$1 = w.roundItems.iterator(); i$1.hasNext(); r.itemStack = new ItemStack(item, 1, r.damage)) {
            r = (MCH_WeaponInfo.RoundItem)i$1.next();
            item = W_Item.getItemByName(r.itemName);
         }
      }

   }

   public static MCH_WeaponInfo get(String name) {
      return (MCH_WeaponInfo)map.get(name);
   }

   public static boolean contains(String name) {
      return map.containsKey(name);
   }

   public static Set getKeySet() {
      return map.keySet();
   }

   public static Collection getValues() {
      return map.values();
   }

}
