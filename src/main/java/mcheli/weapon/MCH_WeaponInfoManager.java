package mcheli.weapon;

import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
//import mcheli.wrapper.W_Item;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
      File dir = new File(path);
      File[] files = dir.listFiles(new FileFilter() {
         public boolean accept(File pathname) {
            String s = pathname.getName().toLowerCase();
            return pathname.isFile() && s.length() >= 5 && s.substring(s.length() - 4).compareTo(".txt") == 0;
         }
      });
      if(files != null && files.length > 0) {
         File[] arr$ = files;
         int len$ = files.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            File f = arr$[i$];
            BufferedReader br = null;
            int line = 0;

            try {
               String e = f.getName().toLowerCase();
               e = e.substring(0, e.length() - 4);
               if(!map.containsKey(e)) {
                  br = new BufferedReader(new FileReader(f));
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
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{f.getName(), Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{f.getName()});
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
         //for(Iterator i$1 = w.roundItems.iterator(); i$1.hasNext(); r.itemStack = new ItemStack(item, 1, r.damage)) {
            //r = (MCH_WeaponInfo.RoundItem)i$1.next();
            //item = W_Item.getItemByName(r.itemName);
         }
      }

   //}

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
