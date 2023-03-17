/*
 * Decompiled with CFR 0_123.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.Item
 *  net.minecraft.stats.Achievement
 */
package mcheli.wrapper;

import mcheli.MCH_Lib;
import mcheli.MCH_OutputFile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;

import java.util.ArrayList;
import java.util.HashMap;

public class W_LanguageRegistry {
    private static HashMap<String, ArrayList<String>> map = new HashMap();

    public static void addName(Object objectToName, String name) {
        W_LanguageRegistry.addNameForObject(objectToName, "en_US", name);
    }

    public static void addNameForObject(Object o, String lang, String name) {
        W_LanguageRegistry.addNameForObject(o, lang, name, "", "");
    }

    public static void addNameForObject(Object o, String lang, String name, String key, String desc) {
        if (o == null) {
            return;
        }
        if (!map.containsKey(lang)) {
            map.put(lang, new ArrayList());
        }
        if (o instanceof Item) {
            map.get(lang).add(((Item)o).getUnlocalizedName() + ".name=" + name);
        }
        if (o instanceof Block) {
            map.get(lang).add(((Block)o).getUnlocalizedName() + ".name=" + name);
        } else if (o instanceof Achievement) {
            map.get(lang).add("achievement." + key + "=" + name);
            map.get(lang).add("achievement." + key + ".desc=" + desc);
        }
    }

    public static void updateLang(String filePath) {
        for (String key : map.keySet()) {
            ArrayList<String> list = map.get(key);
            MCH_OutputFile file = new MCH_OutputFile();
            if (!file.openUTF8(filePath + key + ".lang")) continue;
            for (String s : list) {
                file.writeLine(s);
            }
            MCH_Lib.Log("[mcheli] Update lang:" + file.file.getAbsolutePath(), new Object[0]);
            file.close();
        }
        map = null;
    }
}

