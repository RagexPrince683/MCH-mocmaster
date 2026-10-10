package mcheli.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import mcheli.compat.MCH_MaterialCompat;
import mcheli.metallurgy.MCH_Metallurgy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Optional NEI entry, sharing the machine's authoritative whitelist, costs and provider gate. */
@SideOnly(Side.CLIENT)
public class MCH_MetallurgyRecipeHandler extends TemplateRecipeHandler {
    private final boolean casting;
    public MCH_MetallurgyRecipeHandler(boolean casting) { this.casting = casting; }
    @Override public TemplateRecipeHandler newInstance() { return new MCH_MetallurgyRecipeHandler(casting); }
    @Override public String getRecipeName() {
        return I18n.format(casting ? "container.mcheli.steel_foundry" : "container.mcheli.blast_furnace");
    }
    @Override public String getOverlayIdentifier() { return casting ? "mcheli.steel_casting" : "mcheli.blast_furnace"; }
    @Override public String getGuiTexture() { return "textures/gui/container/inventory.png"; }
    @Override public void drawBackground(int recipe) { Gui.drawRect(0, 0, 166, 110, 0xFF303030); }
    @Override public int recipiesPerPage() { return 1; }

    @Override public void loadCraftingRecipes(String id, Object... results) {
        if (getOverlayIdentifier().equals(id)) load(null, null);
        else super.loadCraftingRecipes(id, results);
    }
    @Override public void loadCraftingRecipes(ItemStack output) { load(output, null); }
    @Override public void loadUsageRecipes(ItemStack input) { load(null, input); }

    private void load(ItemStack wanted, ItemStack input) {
        if (casting) {
            if (input != null && !MCH_Metallurgy.isSteel(input) && input.getItem() != Items.coal) return;
            for (MCH_Metallurgy.Cast cast : MCH_Metallurgy.casts()) {
                if (wanted == null || wanted.isItemEqual(cast.stack())) {
                    arecipes.add(new MetalRecipe(steelInputs(cast.steel), cast.stack(), cast.steel));
                }
            }
        } else if (MCH_Metallurgy.canProduceSteel()) {
            ItemStack steel = new ItemStack(MCH_Metallurgy.steelItem());
            if (wanted != null && !wanted.isItemEqual(steel)) return;
            if (input != null && !MCH_Metallurgy.isIron(input) && MCH_Metallurgy.fuelValue(input) == 0) return;
            List<ItemStack> iron = new ArrayList<ItemStack>();
            iron.add(new ItemStack(Items.iron_ingot));
            iron.add(new ItemStack(Blocks.iron_ore));
            iron.addAll(OreDictionary.getOres("ingotIron"));
            iron.addAll(OreDictionary.getOres("oreIron"));
            arecipes.add(new MetalRecipe(iron, steel, 1));
        }
    }

    private List<ItemStack> steelInputs(int count) {
        List<ItemStack> result = new ArrayList<ItemStack>();
        Object input = MCH_MaterialCompat.recipeInput(new ItemStack(MCH_Metallurgy.steelItem()));
        if (input instanceof String) {
            for (ItemStack stack : OreDictionary.getOres((String) input)) {
                ItemStack copy = stack.copy();
                copy.stackSize = count;
                result.add(copy);
            }
        } else result.add(new ItemStack(MCH_Metallurgy.steelItem(), count));
        return result;
    }

    @Override public void drawExtras(int recipe) {
        MetalRecipe entry = (MetalRecipe) arecipes.get(recipe);
        String[] lines = casting ? new String[] {
            I18n.format("mcheli.metallurgy.nei.cast", entry.cost, MCH_Metallurgy.CAST_TICKS),
            I18n.format("mcheli.metallurgy.nei.melt", MCH_Metallurgy.MELT_TICKS, MCH_Metallurgy.MAX_HEAT),
            I18n.format("mcheli.metallurgy.nei.molds"),
            I18n.format("mcheli.metallurgy.nei.capacity", MCH_Metallurgy.MAX_STEEL)
        } : new String[] {
            I18n.format("mcheli.metallurgy.nei.blast", MCH_Metallurgy.BLAST_TICKS),
            I18n.format("mcheli.metallurgy.nei.fuel"),
            I18n.format("mcheli.metallurgy.nei.block")
        };
        for (int i = 0; i < lines.length; i++) {
            Minecraft.getMinecraft().fontRenderer.drawString(lines[i], 4, 48 + i * 12, 0xFFFFFF);
        }
    }

    private final class MetalRecipe extends CachedRecipe {
        private final PositionedStack input, fuel, result;
        private final int cost;
        MetalRecipe(List<ItemStack> inputs, ItemStack output, int cost) {
            this.input = new PositionedStack(inputs, 26, 22);
            this.fuel = new PositionedStack(new ItemStack(Items.coal), 64, 22);
            this.result = new PositionedStack(output, 120, 22);
            this.cost = cost;
        }
        @Override public List<PositionedStack> getIngredients() {
            List<PositionedStack> inputs = new ArrayList<PositionedStack>();
            inputs.add(input);
            inputs.add(fuel);
            return getCycledIngredients(cycleticks / 20, inputs);
        }
        @Override public PositionedStack getResult() { return result; }
    }
}
