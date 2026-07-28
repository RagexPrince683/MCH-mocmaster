package mcheli;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Generates sounds.json by scanning .ogg files from classpath resources.
 * At build time, a Gradle task pre-generates sounds.json into the resources.
 * At runtime, this class can also generate it as a fallback.
 */
public class MCH_SoundsJson {

   /**
    * Generates sounds.json content from a list of .ogg resource paths.
    * Each path should be like "assets/mcheli/sounds/heli_engine.ogg".
    */
   public static String generateSoundsJson(List<String> oggPaths) {
      LinkedHashMap<String, ArrayList<String>> map = new LinkedHashMap<>();

      for (String oggPath : oggPaths) {
         String name = oggPath;
         // Strip directory prefix
         int lastSlash = name.lastIndexOf('/');
         if (lastSlash >= 0) name = name.substring(lastSlash + 1);
         // Strip extension
         int dot = name.lastIndexOf('.');
         if (dot > 0) name = name.substring(0, dot);

         // Strip trailing digit (sound variants like "explosion1", "explosion2")
         String key = name;
         if (name.length() > 0) {
            char c = name.charAt(name.length() - 1);
            if (c >= '0' && c <= '9') {
               key = name.substring(0, name.length() - 1);
            }
         }

         if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
         }
         map.get(key).add(name);
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\n");
      int cnt = 0;
      for (String key : map.keySet()) {
         cnt++;
         ArrayList<String> sounds = map.get(key);
         sb.append("  \"").append(key).append("\": {\"category\": \"master\", \"sounds\": [");
         for (int i = 0; i < sounds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(sounds.get(i)).append("\"");
         }
         sb.append("]}");
         if (cnt < map.size()) sb.append(",");
         sb.append("\n");
      }
      sb.append("}\n");

      return sb.toString();
   }

    /**
     * Runtime: checks that sounds.json exists on the classpath.
     * Since sounds.json is now pre-generated and bundled in the JAR,
     * this just verifies it's present and logs the count.
     */
    public static boolean update(String path) {
       path = path.replace('\\', '/');

       // Check if bundled sounds.json is accessible via classpath or addon overlay
       try {
          java.io.BufferedReader br = MCH_ResourceHelper.openResource("/" + path + "sounds.json");
          if (br != null) {
             br.close();
             MCH_Lib.Log("sounds.json found on classpath/addon", new Object[0]);
             return true;
          }
       } catch (Exception e) {
          // fall through
       }

       // Fallback: generate from .ogg files if sounds.json is missing
       List<String> oggFiles = MCH_ResourceHelper.listResources(path + "sounds", ".ogg");
       int cnt = oggFiles != null ? oggFiles.size() : 0;
       if (cnt > 0) {
          String json = generateSoundsJson(oggFiles);
          try {
             File outFile = new File(path + "sounds.json");
             PrintWriter pw = new PrintWriter(outFile);
             pw.print(json);
             pw.close();
             MCH_Lib.Log("Generated sounds.json. %d sounds", new Object[]{Integer.valueOf(cnt)});
             return true;
          } catch (Exception e) {
             e.printStackTrace();
             MCH_Lib.Log("Failed sounds.json generation! %d sounds", new Object[]{Integer.valueOf(cnt)});
          }
       }

       MCH_Lib.Log("No sounds found and no bundled sounds.json", new Object[0]);
       return false;
    }
}
