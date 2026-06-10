package mcheli;

import net.minecraft.item.crafting.IRecipe;

//Recipe list for drafting table/all vehicle items.
public interface MCH_IRecipeList {

   int getRecipeListSize();

   IRecipe getRecipe(int var1);
}
