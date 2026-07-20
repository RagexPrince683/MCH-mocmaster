package mcheli.throwable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import mcheli.MCH_ResourceHelper;
import mcheli.throwable.MCH_ThrowableInfo;
import net.minecraft.item.Item;

public class MCH_ThrowableInfoManager {

   private static MCH_ThrowableInfoManager instance = new MCH_ThrowableInfoManager();
   private static HashMap map = new LinkedHashMap();


   public static boolean load(String path) {
      path = path.replace('\\', '/');
      String dirPrefix = path + "throwable";
      List<String> entries = MCH_ResourceHelper.listResources(dirPrefix, ".txt");
      if(entries != null && entries.size() > 0) {
         for(int i = 0; i < entries.size(); ++i) {
            String resourcePath = entries.get(i);
            MCH_InputFile inFile = new MCH_InputFile();
            int line = 0;

            try {
               String e = MCH_ResourceHelper.getEntryName(resourcePath);
               if(!map.containsKey(e) && inFile.openClasspath("/" + resourcePath)) {
                  MCH_ThrowableInfo info = new MCH_ThrowableInfo(e);

                  String str;
                  while((str = inFile.readLine()) != null) {
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
            } catch (Exception var16) {
               if(line > 0) {
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{resourcePath, Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{resourcePath});
               }

               var16.printStackTrace();
            } finally {
               inFile.close();
            }
         }

         MCH_Lib.Log("Read %d throwable", new Object[]{Integer.valueOf(map.size())});
         return map.size() > 0;
      } else {
         return false;
      }
   }

   public static MCH_ThrowableInfo get(String name) {
      return (MCH_ThrowableInfo)map.get(name);
   }

   public static MCH_ThrowableInfo get(Item item) {
      Iterator i$ = map.values().iterator();

      MCH_ThrowableInfo info;
      do {
         if(!i$.hasNext()) {
            return null;
         }

         info = (MCH_ThrowableInfo)i$.next();
      } while(info.item != item);

      return info;
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
