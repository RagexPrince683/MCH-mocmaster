package mcheli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Base for information managers whose data is published as immutable snapshots. */
public abstract class MCH_InfoManagerBase {

   private String lastPath;
   private String lastType;
   private String lastReloadError = "";

   public abstract MCH_BaseInfo newInfo(String name);
   public abstract Map getMap();
   protected abstract void setMap(Map map);

   /** Copies runtime-only state from an already published entry. */
   protected void preserveRuntimeState(MCH_BaseInfo oldInfo, MCH_BaseInfo newInfo) {}
   protected void onNewReloadEntry(String name, MCH_BaseInfo info) {}

   public boolean load(String path, String type) {
      lastPath = path;
      lastType = type;
      return loadAndPublish(path, type, false);
   }

   private boolean loadAndPublish(String path, String type, boolean reload) {
      Map oldMap = getMap();
      LinkedHashMap newMap = new LinkedHashMap();
      path = path.replace('\\', '/');
      List<String> entries = MCH_ResourceHelper.listResources(path + type, ".txt");
      if(entries == null || entries.isEmpty()) {
         if(reload) {
            setMap(newMap);
            MCH_Lib.Log("Read 0 %s (all definitions removed)", new Object[]{type});
            return true;
         }
         return false;
      }

      for(int i = 0; i < entries.size(); ++i) {
         String resourcePath = entries.get(i);
         MCH_InputFile inFile = new MCH_InputFile();
         int line = 0;
         try {
            String name = MCH_ResourceHelper.getEntryName(resourcePath);
            if(!newMap.containsKey(name) && inFile.openClasspath("/" + resourcePath)) {
               MCH_BaseInfo info = newInfo(name);
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
               if(info.isValidData()) newMap.put(name, info);
            }
         } catch(Exception e) {
            MCH_Lib.Log("### Reload failed %s%s; keeping previous %s data", new Object[]{resourcePath, line > 0 ? " : line=" + line : "", type});
            e.printStackTrace();
            return false;
         } finally {
            inFile.close();
         }
      }

      if(newMap.isEmpty()) return false;
      if(reload) {
         for(Object key : newMap.keySet()) {
            MCH_BaseInfo oldInfo = (MCH_BaseInfo)oldMap.get(key);
            if(oldInfo != null) preserveRuntimeState(oldInfo, (MCH_BaseInfo)newMap.get(key));
            else onNewReloadEntry((String)key, (MCH_BaseInfo)newMap.get(key));
         }
      }
      setMap(newMap); // Single volatile publication; published maps are never mutated.
      MCH_Lib.Log("Read %d %s", new Object[]{Integer.valueOf(newMap.size()), type});
      return true;
   }

   public boolean reload() {
      if(lastPath == null || lastType == null) {
         MCH_Lib.Log("### Cannot reload: never loaded");
         return false;
      }
      return loadAndPublish(lastPath, lastType, true);
   }

   /** Reloads one already-published definition without exposing partial data. */
   public synchronized boolean reloadEntry(String name) {
      lastReloadError = "";
      Map oldMap = getMap();
      MCH_BaseInfo oldInfo = (MCH_BaseInfo)oldMap.get(name);
      if(lastPath == null || lastType == null || oldInfo == null) {
         lastReloadError = "Vehicle definition is not loaded: " + name;
         return false;
      }
      MCH_ResourceHelper.refreshResourceSources();
      String resourcePath = MCH_ResourceHelper.normalizeAssetPath(oldInfo.filePath);
      MCH_InputFile inFile = new MCH_InputFile();
      int line = 0;
      try {
         if(!MCH_ResourceHelper.resourceExists(resourcePath) || !inFile.openClasspath(resourcePath)) {
            lastReloadError = "Vehicle definition resource is missing: " + resourcePath;
            MCH_Lib.Log("### %s", new Object[]{lastReloadError});
            return false;
         }
         MCH_BaseInfo newInfo = newInfo(name);
         newInfo.filePath = resourcePath;
         String str;
         while((str = inFile.readLine()) != null) {
            ++line;
            str = str.trim();
            int eqIdx = str.indexOf('=');
            if(eqIdx >= 0 && str.length() > eqIdx + 1)
               newInfo.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
         }
         if(!newInfo.isValidData()) {
            lastReloadError = "Vehicle definition is invalid: " + resourcePath + " : line=" + line;
            MCH_Lib.Log("### %s", new Object[]{lastReloadError});
            return false;
         }
         if(oldInfo instanceof mcheli.aircraft.MCH_BaseVehicleInfo
               && newInfo instanceof mcheli.aircraft.MCH_BaseVehicleInfo
               && ((mcheli.aircraft.MCH_BaseVehicleInfo)oldInfo).getNumSeatAndRack()
               != ((mcheli.aircraft.MCH_BaseVehicleInfo)newInfo).getNumSeatAndRack()) {
            lastReloadError = "Seat count changed; restart is required";
            return false;
         }
         preserveRuntimeState(oldInfo, newInfo);
         LinkedHashMap newMap = new LinkedHashMap(oldMap);
         newMap.put(name, newInfo);
         setMap(newMap);
         MCH_Lib.Log("Reloaded one %s: %s", new Object[]{lastType, resourcePath});
         return true;
      } catch(Exception e) {
         lastReloadError = "Parse failed: " + resourcePath + " : line=" + line + " (" + e.getMessage() + ")";
         MCH_Lib.Log("### %s", new Object[]{lastReloadError});
         e.printStackTrace();
         return false;
      } finally {
         inFile.close();
      }
   }

   public String getLastReloadError() { return lastReloadError; }
}
