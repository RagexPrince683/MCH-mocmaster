package mcheli;

import mcheli.aircraft.MCH_ItemFuel;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemCoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

public class MCH_RecipeFuel implements IRecipe {

   public boolean matches(InventoryCrafting inv, World var2) {
      int jcnt = 0;
      int ccnt = 0;

      for(int i = 0; i < inv.getSizeInventory(); ++i) {
         ItemStack is = inv.getStackInSlot(i);
         if(is != null) {
            if(is.getItem() instanceof MCH_ItemFuel) {
               if(is.getItemDamage() == 0) {
                  return false;
               }

               ++jcnt;
               if(jcnt > 1) {
                  return false;
               }
            } else {
               if(!(is.getItem() instanceof ItemCoal) || is.stackSize <= 0) {
                  return false;
               }

               ++ccnt;
            }
         }
      }

      return jcnt == 1 && ccnt > 0;
   }

   public ItemStack getCraftingResult(InventoryCrafting inv) {
      ItemStack output = new ItemStack(MCH_MOD.itemFuel);

      int i;
      ItemStack is;
      for(i = 0; i < inv.getSizeInventory(); ++i) {
         is = inv.getStackInSlot(i);
         if(is != null && is.getItem() instanceof MCH_ItemFuel) {
            output.setItemDamage(is.getItemDamage());
            break;
         }
      }

      for(i = 0; i < inv.getSizeInventory(); ++i) {
         is = inv.getStackInSlot(i);
         if(is != null && is.getItem() instanceof ItemCoal) {
            byte sp = 100;
            if(is.getItemDamage() == 1) {
               sp = 75;
            }

            if(output.getItemDamage() > sp) {
               output.setItemDamage(output.getItemDamage() - sp);
            } else {
               output.setItemDamage(0);
            }
         }
      }

      return output;
   }

   public int getRecipeSize() {
      return 9;
   }

   public ItemStack getRecipeOutput() {
      return null;
   }
}
