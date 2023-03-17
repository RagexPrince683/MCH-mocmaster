package mcheli.aircraft;

import mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemCoal;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemFuel extends W_Item {

   public MCH_ItemFuel(int itemID) {
      super(itemID);
      this.setMaxDamage(600);
      this.setMaxStackSize(1);
      this.setNoRepair();
      this.setFull3D();
   }

   public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
      int damage = stack.getItemDamage();
      if(!world.isRemote && stack.isItemDamaged() && !player.capabilities.isCreativeMode) {
         this.refuel(stack, player, 1);
         this.refuel(stack, player, 0);
      }

      return stack;
   }

   private void refuel(ItemStack stack, EntityPlayer player, int coalType) {
      ItemStack[] list = player.inventory.mainInventory;

      for(int i = 0; i < list.length; ++i) {
         ItemStack is = list[i];
         if(is != null && is.getItem() instanceof ItemCoal && is.getItemDamage() == coalType) {
            for(int j = 0; is.stackSize > 0 && stack.isItemDamaged() && j < 64; ++j) {
               int damage = stack.getItemDamage() - (coalType == 1?75:100);
               if(damage < 0) {
                  damage = 0;
               }

               stack.setItemDamage(damage);
               --is.stackSize;
            }

            if(is.stackSize <= 0) {
               list[i] = null;
            }
         }
      }

   }
}
