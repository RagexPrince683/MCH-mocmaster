package mcheli.compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mcheli.MCH_Lib;
import mcheli.item.MCH_ItemInfo;
import mcheli.item.MCH_ItemInfoManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** One-way inputs for the audited synthetic materials; other addon items keep their contracts. */
public final class MCH_MaterialCompat {
    private static final Map<String, Material> MATERIALS = new LinkedHashMap<String, Material>();
    static {
        define("item.ingot_aluminum", "ingotAluminum,ingotAluminium", "ingot_aluminium");
        define("item.ingot_steel", "ingotSteel", "ingot_steel");
        define("plastic", "ingotAnyPlastic,ingotPolycarbonate", "ingot_polymer,ingot_bakelite,ingot_pc");
        define("rubber", "ingotAnyRubber,ingotRubber,itemRubber", "ingot_rubber");
        // Armor categories may accept these alloys locally; they are not the same material globally.
        define("lap", "plateSteel,plateStainlessSteel", "plate_steel,plate_stainless");
        define("map", "plateTripleSteel,plateWeaponSteel,plateStainlessSteel", "plate_weaponsteel,plate_stainless");
        define("hap", "plateSextupleSteel,plateSaturnite,plateDuraSteel", "plate_saturnite,plate_dura_steel");
        // An armor ingredient is a whole depleted-uranium ingot, not waste or a tiny/nugget quantum.
        define("du", "ingotUranium238,ingotU238", "ingot_u238");
    }

    private MCH_MaterialCompat() {}

    private static void define(String name, String ores, String hbmItems) {
        MATERIALS.put(name, new Material(name, ores.split(","), hbmItems.split(",")));
    }

    public static boolean isPrivateMaterial(String name) {
        return MATERIALS.containsKey(name);
    }

    /** Called after external init/post-init registrations, before any MCHeli recipe is parsed. */
    public static void initialize() {
        for (Material material : MATERIALS.values()) {
            MCH_ItemInfo info = MCH_ItemInfoManager.get(material.name);
            if (info == null || info.item == null) continue;
            material.item = info.item;
            // RTM and HBM share this mod ID. Keep the items for existing saves and recipe inputs.
            material.recipesDisabled = Loader.isModLoaded("hbm")
                    && (material.name.equals("item.ingot_aluminum")
                        || material.name.equals("item.ingot_steel") || material.name.equals("du"));
            if (material.recipesDisabled) {
                MCH_Lib.Log("Material %s: output recipes disabled; hbm owns production", material.name);
            }
            List<ItemStack> inputs = new ArrayList<ItemStack>();
            // Explicit provider IDs establish ownership without depending on OreDictionary order.
            for (String name : material.hbmItems) {
                Item item = GameRegistry.findItem("hbm", "item." + name);
                if (item != null) add(inputs, new ItemStack(item));
            }
            for (String ore : material.ores) {
                for (ItemStack stack : OreDictionary.getOres(ore)) {
                    if (isRealInput(stack)) add(inputs, stack);
                }
            }
            Collections.sort(inputs, new Comparator<ItemStack>() {
                public int compare(ItemStack a, ItemStack b) {
                    String left = String.valueOf(Item.itemRegistry.getNameForObject(a.getItem()));
                    String right = String.valueOf(Item.itemRegistry.getNameForObject(b.getItem()));
                    boolean lh = left.startsWith("hbm:"), rh = right.startsWith("hbm:");
                    if (lh != rh) return lh ? -1 : 1;
                    int result = left.compareTo(right);
                    return result != 0 ? result : Integer.compare(a.getItemDamage(), b.getItemDamage());
                }
            });
            material.external = !inputs.isEmpty();
            if (material.external) {
                add(inputs, new ItemStack(info.item));
                for (ItemStack stack : inputs) OreDictionary.registerOre(material.inputKey, stack);
                MCH_Lib.Log("Material %s: one-way external inputs %s; output recipes %s", material.name, inputs,
                        material.recipesDisabled ? "disabled" : "retained");
            } else {
                MCH_Lib.Log("Material %s: private input (no generic OreDictionary export); output recipes %s",
                        material.name, material.recipesDisabled ? "disabled" : "retained");
            }
        }
    }

    private static boolean isRealInput(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        String id = String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
        if (id.startsWith("mcheli:")) return false;
        // HMG's steel/polymer are synthetic fallbacks; its ore-smelted aluminum is a real input.
        return !id.equals("HandmadeGuns:steelIngot") && !id.equals("HandmadeGuns:polymer");
    }

    private static void add(List<ItemStack> inputs, ItemStack stack) {
        for (ItemStack old : inputs) {
            if (old.getItem() == stack.getItem() && old.getItemDamage() == stack.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(old, stack)) return;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        inputs.add(copy);
    }

    public static Object recipeInput(ItemStack stack) {
        Material material = find(stack.getItem());
        return material != null && material.external ? material.inputKey : stack;
    }

    public static boolean isPrivateMaterial(Item item) {
        return find(item) != null;
    }

    /** Suppress every configured output recipe for externally owned materials, including conversions. */
    public static boolean allowOutput(Item item) {
        Material material = find(item);
        return material == null || !material.recipesDisabled;
    }

    private static Material find(Item item) {
        for (Material material : MATERIALS.values()) if (material.item == item && item != null) return material;
        return null;
    }

    private static final class Material {
        final String name, inputKey;
        final String[] ores, hbmItems;
        Item item;
        boolean external, recipesDisabled;
        Material(String name, String[] ores, String[] hbmItems) {
            this.name = name;
            this.inputKey = "mcheliInput_" + name;
            this.ores = ores;
            this.hbmItems = hbmItems;
        }
    }
}
