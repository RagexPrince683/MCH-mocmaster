package mcheli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class MCH_SoundsJson {

   public static boolean update(String path) {
      boolean result = true;
      path = path.replace('\\', '/');
      File dir = new File(path + "sounds");
      File[] files = dir.listFiles(new FileFilter() {
         public boolean accept(File pathname) {
            String s = pathname.getName().toLowerCase();
            return pathname.isFile() && s.length() >= 5 && s.substring(s.length() - 4).compareTo(".ogg") == 0;
         }
      });
      int cnt = 0;
      PrintWriter pw = null;

      try {
         File e = new File(path + "sounds.json");
         pw = new PrintWriter(e);
         pw.println("{");
         if(files != null) {
            LinkedHashMap map = new LinkedHashMap();
            File[] i$ = files;
            int key = files.length;

            for(int list = 0; list < key; ++list) {
               File line = i$[list];
               String fi = line.getName().toLowerCase();
               int ei = fi.lastIndexOf(".");
               fi = fi.substring(0, ei);
               String key1 = fi;
               char c = fi.charAt(fi.length() - 1);
               if(c >= 48 && c <= 57) {
                  key1 = fi.substring(0, fi.length() - 1);
               }

               if(!map.containsKey(key1)) {
                  map.put(key1, new ArrayList());
               }

               ((ArrayList)map.get(key1)).add(fi);
            }

            String var24;
            for(Iterator var21 = map.keySet().iterator(); var21.hasNext(); pw.println(var24)) {
               String var22 = (String)var21.next();
               ++cnt;
               ArrayList var23 = (ArrayList)map.get(var22);
               var24 = "";
               var24 = "\"" + var22 + "\": {\"category\": \"master\",\"sounds\": [";

               for(int var25 = 0; var25 < var23.size(); ++var25) {
                  var24 = var24 + (var25 > 0?",":"") + "\"" + (String)var23.get(var25) + "\"";
               }

               var24 = var24 + "]}";
               if(cnt < map.size()) {
                  var24 = var24 + ",";
               }
            }
         }

         pw.println("}");
         pw.println("");
         result = true;
      } catch (IOException var19) {
         result = false;
         var19.printStackTrace();
      } finally {
         if(pw != null) {
            pw.close();
         }

      }

      if(result) {
         MCH_Lib.Log("Update sounds.json. %d sounds", new Object[]{Integer.valueOf(cnt)});
      } else {
         MCH_Lib.Log("Failed sounds.json update! %d sounds", new Object[]{Integer.valueOf(cnt)});
      }

      return result;
   }
}
