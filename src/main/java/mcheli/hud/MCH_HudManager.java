package mcheli.hud;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;
import mcheli.MCH_ResourceHelper;
import mcheli.hud.MCH_Hud;
import mcheli.hud.MCH_HudItem;
import net.minecraft.client.Minecraft;
import mcheli.hud.layout.MCH_HudLayoutManager;

public class MCH_HudManager {

   private static MCH_HudManager instance = new MCH_HudManager();
   private static HashMap map;


   private MCH_HudManager() {
      map = new HashMap();
   }

   public static boolean load(String path) {
      MCH_HudLayoutManager.reload();
      MCH_HudItem.mc = Minecraft.getMinecraft();
      map.clear();
      path = path.replace('\\', '/');
      String dirPrefix = path + "hud";
      List<String> entries = MCH_ResourceHelper.listResources(dirPrefix, ".txt");
      if(entries != null && entries.size() > 0) {
         for(int i = 0; i < entries.size(); ++i) {
            String resourcePath = entries.get(i);
            MCH_InputFile inFile = new MCH_InputFile();
            int line = 0;

            try {
               String e = MCH_ResourceHelper.getEntryName(resourcePath);
               if(!map.containsKey(e) && inFile.openClasspath("/" + resourcePath)) {
                  MCH_Hud info = new MCH_Hud(e, resourcePath);

                  String str;
                  while((str = inFile.readLine()) != null) {
                     ++line;
                     str = str.trim();
                     if(str.equalsIgnoreCase("endif")) {
                        str = "endif=0";
                     }

                     if(str.equalsIgnoreCase("exit")) {
                        str = "exit=0";
                     }
                     if(str.equalsIgnoreCase("endlayoutgroup")) {
                        str = "endlayoutgroup=0";
                     }

                     int eqIdx = str.indexOf(61);
                     if(eqIdx >= 0 && str.length() > eqIdx + 1) {
                        info.loadItemData(line, str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                     }
                  }

                  info.checkData();
                  map.put(e, info);
               }
            } catch (Exception var18) {
               MCH_Lib.Log("### HUD file error! %s Line=%d", new Object[]{resourcePath, Integer.valueOf(line)});
               var18.printStackTrace();
               throw new RuntimeException(var18);
            } finally {
               inFile.close();
            }
         }

         MCH_Lib.Log("Read %d HUD", new Object[]{Integer.valueOf(map.size())});
         return map.size() > 0;
      } else {
         return false;
      }
   }

   public static MCH_Hud get(String name) {
      return (MCH_Hud)map.get(name.toLowerCase());
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
