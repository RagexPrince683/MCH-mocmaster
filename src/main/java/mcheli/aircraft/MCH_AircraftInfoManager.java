package mcheli.aircraft;

import mcheli.MCH_IRecipeList;
import mcheli.MCH_InfoManagerBase;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MCH_AircraftInfoManager extends MCH_InfoManagerBase implements MCH_IRecipeList {

   private List listItemRecipe = new ArrayList();


   public int getRecipeListSize() {
      return this.listItemRecipe.size();
   }

   public IRecipe getRecipe(int index) {
      return (IRecipe)this.listItemRecipe.get(index);
   }

   public void addRecipe(IRecipe recipe, int count, String name, String recipeString) {
      if(recipe != null && recipe.getRecipeOutput() != null && recipe.getRecipeOutput().getItem() != null) {
         this.listItemRecipe.add(recipe);
      } else {
         throw new RuntimeException("[mcheli]Recipe Parameter Error! recipe" + count + " : " + name + ".txt : " + recipe + " : " + recipeString);
      }
   }

   public abstract MCH_AircraftInfo getAcInfoFromItem(Item var1);

   public MCH_AircraftInfo getAcInfoFromItem(IRecipe recipe) {
      Map map = this.getMap();
      return recipe != null?this.getAcInfoFromItem(recipe.getRecipeOutput().getItem()):null;
   }
}
