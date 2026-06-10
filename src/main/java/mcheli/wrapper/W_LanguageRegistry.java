package mcheli.wrapper;

import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.stats.Achievement;

import java.util.HashMap;
import java.util.Map;

public class W_LanguageRegistry {

    private static Map<String, Map<String, String>> map = new HashMap<>();

    public static void addName(Object objectToName, String name) {
        addNameForObject(objectToName, "en_US", name);
    }

    public static void addNameForObject(Object o, String lang, String name) {
        addNameForObject(o, lang, name, "", "");
    }

    public static void addNameForObject(Object o, String lang, String name, String key, String desc) {
        if (o == null || lang == null || name == null) {
            System.out.println("[MCH] Lang skipped null: " + o + ", " + lang + ", " + name);
            return;
        }

        String locKey = null;

        if (o instanceof Item) {
            locKey = ((Item)o).getUnlocalizedName() + ".name";
        } else if (o instanceof Block) {
            locKey = ((Block)o).getUnlocalizedName() + ".name";
        } else if (o instanceof Achievement) {
            locKey = "achievement." + key;
        }

        System.out.println("[MCH] Lang add: " + lang + " | " + locKey + " = " + name);

        map.putIfAbsent(lang, new HashMap<String, String>());
        Map<String, String> entries = map.get(lang);

        if (o instanceof Item) {
            entries.put(locKey, name);
        } else if (o instanceof Block) {
            entries.put(locKey, name);
        } else if (o instanceof Achievement) {
            entries.put("achievement." + key, name);
            entries.put("achievement." + key + ".desc", desc);
        }
    }

    // Registers all names with Forge at runtime
    public static void applyNames() {
        for (String lang : map.keySet()) {
            Map<String, String> entries = map.get(lang);
            for (String key : entries.keySet()) {
                String value = entries.get(key);
                LanguageRegistry.instance().addStringLocalization(key, lang, value);
            }
        }
        map.clear();
    }
}
