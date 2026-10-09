package mcheli.aircraft;

import mcheli.wrapper.W_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemCoal;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.List;
import mcheli.compat.MCH_FuelCompat;
import mcheli.compat.MCH_FuelCompat.FuelContainer;

public class MCH_ItemFuel extends W_Item {

   public MCH_ItemFuel(int itemID) {
      super(itemID);
      this.setMaxDamage(600);
      this.setMaxStackSize(16);
      this.setHasSubtypes(true); // Fuel level is part of stack identity.
      this.setNoRepair();
      this.setFull3D();
   }

   public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
      if(!world.isRemote && stack.getItemDamage() > 0 && !player.capabilities.isCreativeMode) {
         ItemStack can = stack.copy();
         can.stackSize = 1;
         this.refuelPetroleum(can, player);
         this.refuel(can, player, 1);
         this.refuel(can, player, 0);
         if (!ItemStack.areItemStacksEqual(can, single(stack))) {
            if (stack.stackSize == 1) return can;
            --stack.stackSize;
            give(player, can);
         }
      }

      return stack;
   }

   private void refuel(ItemStack stack, EntityPlayer player, int coalType) {
      ItemStack[] list = player.inventory.mainInventory;

      for(int i = 0; i < list.length; ++i) {
         ItemStack is = list[i];
         if(is != null && is.getItem() instanceof ItemCoal && is.getItemDamage() == coalType) {
            for(int j = 0; is.stackSize > 0 && stack.getItemDamage() > 0 && j < 64; ++j) {
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

   private static ItemStack single(ItemStack stack) {
      ItemStack result = stack.copy();
      result.stackSize = 1;
      return result;
   }

   private void refuelPetroleum(ItemStack can, EntityPlayer player) {
      if (MCH_FuelCompat.getFuel(can) >= can.getMaxDamage()) return;
      for (int slot = 0; slot < player.inventory.mainInventory.length; ++slot) {
         ItemStack source = player.inventory.mainInventory[slot];
         FuelContainer input = MCH_FuelCompat.getInput(source);
         if (input == null) continue;
         MCH_FuelCompat.setFuel(can, MCH_FuelCompat.getFuel(can) + input.units);
         if (--source.stackSize <= 0) player.inventory.mainInventory[slot] = null;
         give(player, input.getEmpty());
         return;
      }
   }

   private static void give(EntityPlayer player, ItemStack stack) {
      if (!player.inventory.addItemStackToInventory(stack)) player.dropPlayerItemWithRandomChoice(stack, false);
   }

   // Cans with identical fuel level and reserve may stack; retain the fuel bar without vanilla damage semantics.
   @Override public boolean isDamaged(ItemStack stack) { return false; }
   @Override public boolean showDurabilityBar(ItemStack stack) { return stack.getItemDamage() > 0; }
   @Override public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
      lines.add("Fuel per can: " + MCH_FuelCompat.getFuel(stack) + " units");
      lines.add("Right-click to refill from petroleum containers or coal.");
   }
}
