package mcheli.weapon;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.MCH_ResourceHelper;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.weapon.MCH_WeaponInfo;
import mcheli.wrapper.W_Item;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MCH_WeaponInfoManager {

   private static MCH_WeaponInfoManager instance = new MCH_WeaponInfoManager();
   private static volatile Map map;
   private static String lastPath;


   private MCH_WeaponInfoManager() {
      map = new LinkedHashMap();
   }

   public static boolean reload() {
      return lastPath != null && loadAndPublish(lastPath, true);
   }

   public static boolean load(String path) {
      lastPath = path;
      return loadAndPublish(path, false);
   }

   private static boolean loadAndPublish(String path, boolean reload) {
      LinkedHashMap newMap = new LinkedHashMap();
      path = path.replace('\\', '/');
      List<String> entries = MCH_ResourceHelper.listResources(path + "weapons", ".txt");
      if(entries == null || entries.isEmpty()) return false;
      for(int i = 0; i < entries.size(); ++i) {
         String resourcePath = entries.get(i);
         BufferedReader br = null;
         int line = 0;
         try {
            String name = MCH_ResourceHelper.getEntryName(resourcePath);
            if(!newMap.containsKey(name)) {
               br = MCH_ResourceHelper.openResource("/" + resourcePath);
               if(br == null) continue;
               MCH_WeaponInfo info = new MCH_WeaponInfo(name);
               String str;
               while((str = br.readLine()) != null) {
                  ++line; str = str.trim(); int eqIdx = str.indexOf(61);
                  if(eqIdx >= 0 && str.length() > eqIdx + 1) info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
               }
               info.checkData();
               newMap.put(name, info);
            }
         } catch(Exception e) {
            MCH_Lib.Log("### Reload failed %s : line=%d; keeping previous weapon data", new Object[]{resourcePath, Integer.valueOf(line)});
            e.printStackTrace(); return false;
         } finally { try { if(br != null) br.close(); } catch(IOException ignored) {} }
      }
      if(newMap.isEmpty()) return false;
      resolveAllExternalItems(newMap, reload);
      map = newMap;
      MCH_Lib.Log("[mcheli] Read %d weapons", new Object[]{Integer.valueOf(newMap.size())});
      return true;
   }

   public static void setRoundItems() {
      setRoundItems(map);
   }

   /** Resolves references which may belong to mods that register after MCHeli pre-init. */
   public static void resolveAllExternalItems() {
      resolveAllExternalItems(map, true);
   }

   public static Item resolveDispenseItem(MCH_WeaponInfo weapon) {
      Item item = weapon.resolveDispenseItem();
      if(item == null && weapon.dispenseItemName != null && !weapon.dispenseItemName.isEmpty()
            && !weapon.dispenseItemWarningLogged) {
         weapon.dispenseItemWarningLogged = true;
         logUnresolvedDispenseItem(weapon);
      }
      return item;
   }

   private static void resolveAllExternalItems(Map snapshot, boolean warn) {
      setRoundItems(snapshot);
      Iterator iterator = snapshot.values().iterator();
      while(iterator.hasNext()) {
         MCH_WeaponInfo weapon = (MCH_WeaponInfo)iterator.next();
         if(weapon.resolveDispenseItem() == null && warn && weapon.dispenseItemName != null
               && !weapon.dispenseItemName.isEmpty() && !weapon.dispenseItemWarningLogged) {
            weapon.dispenseItemWarningLogged = true;
            logUnresolvedDispenseItem(weapon);
         }
      }
   }

   private static void logUnresolvedDispenseItem(MCH_WeaponInfo weapon) {
      MCH_Lib.Log("Unable to resolve DispenseItem for weapon '%s': registry name '%s' on %s side",
            new Object[]{weapon.name, weapon.dispenseItemName,
                  FMLCommonHandler.instance().getEffectiveSide().toString().toLowerCase()});
   }

   private static void setRoundItems(Map snapshot) {
      Iterator i$ = snapshot.values().iterator();

      while(i$.hasNext()) {
         MCH_WeaponInfo w = (MCH_WeaponInfo)i$.next();

         MCH_WeaponInfo.RoundItem r;
         Item item;
         for(Iterator i$1 = w.roundItems.iterator(); i$1.hasNext(); r.itemStack = new ItemStack(item, 1, r.damage)) {
            r = (MCH_WeaponInfo.RoundItem)i$1.next();
            item = W_Item.getItemByName(r.itemName);
         }
      }

   }

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
