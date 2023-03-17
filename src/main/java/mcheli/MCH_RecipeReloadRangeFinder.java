package mcheli;

import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemRedstone;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class MCH_RecipeReloadRangeFinder implements IRecipe {

   public boolean matches(InventoryCrafting inv, World var2) {
      int jcnt = 0;
      int ccnt = 0;

      for(int i = 0; i < inv.getSizeInventory(); ++i) {
         ItemStack is = inv.getStackInSlot(i);
         if(is != null) {
            if(is.getItem() instanceof MCH_ItemRangeFinder) {
               if(is.getItemDamage() == 0) {
                  return false;
               }

               ++jcnt;
               if(jcnt > 1) {
                  return false;
               }
            } else {
               if(!(is.getItem() instanceof ItemRedstone) || is.stackSize <= 0) {
                  return false;
               }

               ++ccnt;
               if(ccnt > 1) {
                  return false;
               }
            }
         }
      }

      return jcnt == 1 && ccnt > 0;
   }

   public ItemStack getCraftingResult(InventoryCrafting inv) {
      ItemStack output = new ItemStack(MCH_MOD.itemRangeFinder);
      return output;
   }

   public int getRecipeSize() {
      return 9;
   }

   public ItemStack getRecipeOutput() {
      return null;
   }
}
