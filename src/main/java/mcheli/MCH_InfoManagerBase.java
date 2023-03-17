package mcheli;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;

public abstract class MCH_InfoManagerBase {

   public abstract MCH_BaseInfo newInfo(String var1);

   public abstract Map getMap();

   public boolean load(String path, String type) {
      path = path.replace('\\', '/');
      File dir = new File(path + type);
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
            int line = 0;
            MCH_InputFile inFile = new MCH_InputFile();
            Object br = null;

            try {
               String e = f.getName().toLowerCase();
               e = e.substring(0, e.length() - 4);
               if(!this.getMap().containsKey(e) && inFile.openUTF8(f)) {
                  MCH_BaseInfo info = this.newInfo(e);
                  info.filePath = f.getCanonicalPath();

                  String str;
                  while((str = inFile.br.readLine()) != null) {
                     ++line;
                     str = str.trim();
                     int eqIdx = str.indexOf(61);
                     if(eqIdx >= 0 && str.length() > eqIdx + 1) {
                        info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                     }
                  }

                  boolean var21 = false;
                  if(info.isValidData()) {
                     this.getMap().put(e, info);
                  }
               }
            } catch (Exception var19) {
               if(line > 0) {
                  MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{f.getName(), Integer.valueOf(line)});
               } else {
                  MCH_Lib.Log("### Load failed %s", new Object[]{f.getName()});
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
}
