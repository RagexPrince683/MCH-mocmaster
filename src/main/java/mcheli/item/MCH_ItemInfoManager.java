package mcheli.item;

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
import net.minecraft.item.Item;

public class MCH_ItemInfoManager {

    private static HashMap map = new LinkedHashMap();
    private static String lastPath;

    public static boolean load(String path) {
        lastPath = path;
        path = path.replace('\\', '/');
        String dirPrefix = path + "item";
        List<String> entries = MCH_ResourceHelper.listResources(dirPrefix, ".txt");
        if(entries != null && entries.size() > 0) {
            for(int i = 0; i < entries.size(); ++i) {
                String resourcePath = entries.get(i);
                MCH_InputFile inFile = new MCH_InputFile();
                int line = 0;

                try {
                    String e = MCH_ResourceHelper.getEntryName(resourcePath);
                    if(!map.containsKey(e) && inFile.openClasspath("/" + resourcePath)) {
                        MCH_ItemInfo info = new MCH_ItemInfo(e);

                        String str;
                        while((str = inFile.readLine()) != null) {
                            ++line;
                            str = str.trim();
                            int eqIdx = str.indexOf(61);
                            if(eqIdx >= 0 && str.length() > eqIdx + 1) {
                                info.loadItemData(str.substring(0, eqIdx).trim().toLowerCase(), str.substring(eqIdx + 1).trim());
                            }
                        }

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

            MCH_Lib.Log("Read %d item", new Object[]{Integer.valueOf(map.size())});
            return map.size() > 0;
        } else {
            return false;
        }
    }

    public static boolean reload() {
        if(lastPath == null) return false;
        try {
            map.clear();
            return load(lastPath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static MCH_ItemInfo get(String name) {
        return (MCH_ItemInfo)map.get(name);
    }

    public static MCH_ItemInfo get(Item item) {
        Iterator i$ = map.values().iterator();

        MCH_ItemInfo info;
        do {
            if(!i$.hasNext()) {
                return null;
            }

            info = (MCH_ItemInfo)i$.next();
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
