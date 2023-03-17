package mcheli.throwable;

import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import net.minecraft.item.Item;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class MCH_ThrowableInfoManager {

   private static MCH_ThrowableInfoManager instance = new MCH_ThrowableInfoManager();
   private static HashMap map = new LinkedHashMap();


   public static boolean load(String path) {
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
            MCH_InputFile inFile = new MCH_InputFile();
            int line = 0;

            try {
               String e = f.getName().toLowerCase();
               e = e.substring(0, e.length() - 4);
               if(!map.containsKey(e) && inFile.openUTF8(f)) {
                  MCH_ThrowableInfo info = new MCH_ThrowableInfo(e);

                  String str;
                  while((str = inFile.br.readLine()) != null) {
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
            } catch (IOException var16) {
               if(line > 0) {
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{f.getName(), Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{f.getName()});
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
