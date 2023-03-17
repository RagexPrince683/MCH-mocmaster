package mcheli;

import net.minecraft.util.Vec3;

import java.io.File;

public class MCH_BaseInfo {

   public String filePath;


   public boolean toBool(String s) {
      return s.equalsIgnoreCase("true");
   }

   public boolean toBool(String s, boolean defaultValue) {
      return s.equalsIgnoreCase("true")?true:(s.equalsIgnoreCase("false")?false:defaultValue);
   }

   public float toFloat(String s) {
      return Float.parseFloat(s);
   }

   public float toFloat(String s, float min, float max) {
      float f = Float.parseFloat(s);
      return f < min?min:(f > max?max:f);
   }

   public double toDouble(String s) {
      return Double.parseDouble(s);
   }

   public Vec3 toVec3(String x, String y, String z) {
      return Vec3.createVectorHelper(this.toDouble(x), this.toDouble(y), this.toDouble(z));
   }

   public int toInt(String s) {
      return Integer.parseInt(s);
   }

   public int toInt(String s, int min, int max) {
      int f = Integer.parseInt(s);
      return f < min?min:(f > max?max:f);
   }

   public int hex2dec(String s) {
      return !s.startsWith("0x") && !s.startsWith("0X") && s.indexOf(0) != 35?(int)(Long.decode("0x" + s).longValue() & -1L):(int)(Long.decode(s).longValue() & -1L);
   }

   public String[] splitParam(String data) {
      return data.split("\\s*,\\s*");
   }

   public String[] splitParamSlash(String data) {
      return data.split("\\s*/\\s*");
   }

   public boolean isValidData() throws Exception {
      return true;
   }

   public void loadItemData(String item, String data) {}

   public void loadItemData(int fileLine, String item, String data) {}

   public void preReload() {}

   public void postReload() {}

   public boolean canReloadItem(String item) {
      return false;
   }

   public boolean reload() {
      return this.reload(this);
   }

   private boolean reload(MCH_BaseInfo info) {
      int line = 0;
      MCH_InputFile inFile = new MCH_InputFile();
      Object br = null;
      File f = new File(info.filePath);

      try {
         if(inFile.openUTF8(f)) {
            info.preReload();

            String e;
            while((e = inFile.br.readLine()) != null) {
               ++line;
               e = e.trim();
               int eqIdx = e.indexOf(61);
               if(eqIdx >= 0 && e.length() > eqIdx + 1) {
                  String item = e.substring(0, eqIdx).trim().toLowerCase();
                  if(info.canReloadItem(item)) {
                     info.loadItemData(item, e.substring(eqIdx + 1).trim());
                  }
               }
            }

            boolean var14 = false;
            info.isValidData();
            info.postReload();
         }
      } catch (Exception var12) {
         if(line > 0) {
            MCH_Lib.Log("### Load failed %s : line=%d", new Object[]{f.getName(), Integer.valueOf(line)});
         } else {
            MCH_Lib.Log("### Load failed %s", new Object[]{f.getName()});
         }

         var12.printStackTrace();
      } finally {
         inFile.close();
      }

      return true;
   }
}
