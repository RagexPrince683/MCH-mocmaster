package mcheli.metallurgy;

import cpw.mods.fml.common.registry.GameRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mcheli.compat.MCH_MaterialCompat;
import mcheli.item.MCH_ItemInfo;
import mcheli.item.MCH_ItemInfoManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

/** Vehicle-only extraction. Existing addon items remain the canonical material/component IDs. */
public final class MCH_Metallurgy {
    public static final int BLAST_TICKS = 300;
    public static final int MELT_TICKS = 21;
    public static final int CAST_TICKS = 100;
    public static final int MAX_FUEL = 256;
    public static final int MAX_HEAT = 4;
    public static final int MAX_STEEL = 32;
    public static MCH_MetallurgyBlock blastFurnace;
    public static MCH_MetallurgyBlock foundry;
    public static MCH_MetallurgyPartBlock part;

    // XF's vehicle cast costs, without its firearm parts or unused sawblade.
    private static final Cast[] CASTS = {
        new Cast("item.ingot_steel", 1),
        new Cast("grate", 3),
        new Cast("largespring", 3),
        new Cast("lap", 5),
        new Cast("shiphull1", 15),
        new Cast("basicmechanicparts", 4),
        new Cast("rims", 3),
        new Cast("tanktrackpiece", 2)
    };
    private static List<Cast> activeCasts = Collections.emptyList();
    private static Item steelItem;
    private static Object steelInput;

    private MCH_Metallurgy() {}

    public static void register() {
        cpw.mods.fml.common.registry.LanguageRegistry.instance().loadLocalization(
            "/assets/mcheli/metallurgy/en_US.lang", "en_US", false);
        blastFurnace = new MCH_MetallurgyBlock(false);
        foundry = new MCH_MetallurgyBlock(true);
        part = new MCH_MetallurgyPartBlock();
        GameRegistry.registerBlock(blastFurnace, MCH_MetallurgyItemBlock.class, "blast_furnace");
        GameRegistry.registerBlock(foundry, MCH_MetallurgyItemBlock.class, "steel_foundry");
        GameRegistry.registerBlock(part, (Class<? extends net.minecraft.item.ItemBlock>) null, "metallurgy_part");
        GameRegistry.registerTileEntity(MCH_BlastFurnaceTile.class, "mcheli:blast_furnace");
        GameRegistry.registerTileEntity(MCH_FoundryTile.class, "mcheli:steel_foundry");
        GameRegistry.registerTileEntity(MCH_MetallurgyPartTile.class, "mcheli:metallurgy_part");
    }

    /** Called after material compatibility resolves the installed providers. */
    public static void registerRecipes() {
        steelItem = item("item.ingot_steel");
        if (steelItem != null) steelInput = MCH_MaterialCompat.recipeInput(new ItemStack(steelItem));
        List<Cast> active = new ArrayList<Cast>();
        for (Cast cast : CASTS) {
            cast.item = item(cast.name);
            if (cast.item != null && (!cast.name.equals("item.ingot_steel") || canProduceSteel())) active.add(cast);
        }
        activeCasts = Collections.unmodifiableList(active);
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blastFurnace),
            "BIB", "BIB", "BFB", 'B', Blocks.stonebrick, 'I', "ingotIron", 'F', Blocks.furnace));
        Item steel = steelItem();
        if (steel != null) {
            GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(foundry),
                "IBI", "SFS", "SDS", 'I', "ingotIron", 'B', Items.bucket,
                'S', MCH_MaterialCompat.recipeInput(new ItemStack(steel)), 'F', Blocks.furnace,
                'D', new ItemStack(Blocks.stone_slab, 1, 0)));
        }
    }

    public static Item item(String name) {
        MCH_ItemInfo info = MCH_ItemInfoManager.get(name);
        return info == null ? null : info.item;
    }

    public static Item steelItem() { return steelItem; }

    public static boolean canProduceSteel() {
        Item steel = steelItem();
        return steel != null && MCH_MaterialCompat.allowOutput(steel);
    }

    /** Cast-only outputs must never receive addon crafting/conversion recipes. */
    public static boolean isCastOnly(Item item) {
        if (item == null) return false;
        for (Cast cast : CASTS) if (cast.output() == item) return true;
        return false;
    }

    public static List<Cast> casts() {
        return activeCasts;
    }

    public static Cast cast(String name) {
        for (Cast cast : activeCasts) if (cast.name.equals(name)) return cast;
        return null;
    }

    public static boolean isIron(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == Items.iron_ingot || stack.getItem() == Item.getItemFromBlock(Blocks.iron_ore)) return true;
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if (name.equals("ingotIron") || name.equals("oreIron")) return true;
        }
        return false;
    }

    public static int fuelValue(ItemStack stack) {
        if (stack == null) return 0;
        if (stack.getItem() == Items.coal) return 4;
        // A block contains nine coal. Do not reproduce XF's ten-coal compression bonus.
        return stack.getItem() == Item.getItemFromBlock(Blocks.coal_block) ? 36 : 0;
    }

    public static boolean isSteel(ItemStack stack) {
        Item steel = steelItem();
        if (steel == null || stack == null) return false;
        Object input = steelInput;
        if (input instanceof String) {
            for (ItemStack ore : OreDictionary.getOres((String) input)) {
                if (OreDictionary.itemMatches(ore, stack, false)) return true;
            }
            return false;
        }
        return stack.getItem() == steel && stack.getItemDamage() == 0;
    }

    public static final class Cast {
        public final String name;
        public final int steel;
        private Item item;
        private Cast(String name, int steel) { this.name = name; this.steel = steel; }
        public Item output() { return item; }
        public ItemStack stack() { return new ItemStack(output()); }
    }
}
