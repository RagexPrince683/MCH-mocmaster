package mcheli;

import java.util.List;
import java.util.Map;
import mcheli.MCH_BaseInfo;
import mcheli.MCH_InputFile;
import mcheli.MCH_Lib;

//Inherited by all vehicle classes.
public abstract class MCH_InfoManagerBase {

   private String lastPath;
   private String lastType;

   public abstract MCH_BaseInfo newInfo(String var1);

   public abstract Map getMap();

   public boolean load(String path, String type) {
      lastPath = path;
      lastType = type;
      path = path.replace('\\', '/');
      String dirPrefix = path + type;
      List<String> entries = MCH_ResourceHelper.listResources(dirPrefix, ".txt");
      if(entries != null && entries.size() > 0) {
         for(int i = 0; i < entries.size(); ++i) {
            String resourcePath = entries.get(i);
            MCH_InputFile inFile = new MCH_InputFile();
            int line = 0;

            try {
               String e = MCH_ResourceHelper.getEntryName(resourcePath);
               if(!this.getMap().containsKey(e) && inFile.openClasspath("/" + resourcePath)) {
                  MCH_BaseInfo info = this.newInfo(e);
                  info.filePath = resourcePath;

                  String str;
                  while((str = inFile.readLine()) != null) {
                     ++line;
                     str = str.trim();
                     int eqIdx = str.indexOf(61);
                     if(eqIdx >= 0 && str.length() > eqIdx + 1) {
                        info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                     }
                  }

                  if(info.isValidData()) {
                     this.getMap().put(e, info);
                  }
               }
            } catch (Exception var19) {
               if(line > 0) {
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{resourcePath, Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{resourcePath});
               }

               var19.printStackTrace();
            } finally {
               inFile.close();
            }
         }

         MCH_Lib.Log("Read %d %s", new Object[]{Integer.valueOf(this.getMap().size()), type});
         return this.getMap().size() > 0;
      } else {
         return false;
      }
   }

   public boolean reload() {
      if(lastPath == null || lastType == null) {
         MCH_Lib.Log("### Cannot reload: never loaded");
         return false;
      }
      try {
         this.getMap().clear();
         return this.load(lastPath, lastType);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }
}
