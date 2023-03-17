package mcheli.block;

import mcheli.MCH_IRecipeList;
import mcheli.MCH_ItemRecipe;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_EntityPlayer;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import java.util.Iterator;
import java.util.Map;

public class MCH_DraftingTableGuiContainer extends Container {

   public final EntityPlayer player;
   public final int posX;
   public final int posY;
   public final int posZ;
   public final int outputSlotIndex;
   private IInventory outputSlot = new InventoryCraftResult();


   public MCH_DraftingTableGuiContainer(EntityPlayer player, int posX, int posY, int posZ) {
      this.player = player;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;

      int a;
      for(a = 0; a < 3; ++a) {
         for(int x = 0; x < 9; ++x) {
            this.addSlotToContainer(new Slot(player.inventory, 9 + x + a * 9, 30 + x * 18, 140 + a * 18));
         }
      }

      for(a = 0; a < 9; ++a) {
         this.addSlotToContainer(new Slot(player.inventory, a, 30 + a * 18, 198));
      }

      this.outputSlotIndex = super.inventoryItemStacks.size();
      Slot var7 = new Slot(this.outputSlot, this.outputSlotIndex, 178, 90) {
         public boolean isItemValid(ItemStack par1ItemStack) {
            return false;
         }
      };
      this.addSlotToContainer(var7);
      MCH_Lib.DbgLog(player.worldObj, "MCH_DraftingTableGuiContainer.MCH_DraftingTableGuiContainer", new Object[0]);
   }

   public void detectAndSendChanges() {
      super.detectAndSendChanges();
   }

   public boolean canInteractWith(EntityPlayer player) {
      Block block = W_WorldFunc.getBlock(player.worldObj, this.posX, this.posY, this.posZ);
      return !W_Block.isEqual(block, MCH_MOD.blockDraftingTable) && !W_Block.isEqual(block, MCH_MOD.blockDraftingTableLit)?false:player.getDistanceSq((double)this.posX, (double)this.posY, (double)this.posZ) <= 144.0D;
   }

   public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
      ItemStack itemstack = null;
      Slot slot = (Slot)super.inventorySlots.get(slotIndex);
      if(slot != null && slot.getHasStack()) {
         ItemStack itemstack1 = slot.getStack();
         itemstack = itemstack1.copy();
         if(slotIndex != this.outputSlotIndex) {
            return null;
         }

         if(!this.mergeItemStack(itemstack1, 0, 36, true)) {
            return null;
         }

         slot.onSlotChange(itemstack1, itemstack);
         if(itemstack1.stackSize == 0) {
            slot.putStack((ItemStack)null);
         } else {
            slot.onSlotChanged();
         }

         if(itemstack1.stackSize == itemstack.stackSize) {
            return null;
         }

         slot.onPickupFromSlot(player, itemstack1);
      }

      return itemstack;
   }

   public void onContainerClosed(EntityPlayer player) {
      super.onContainerClosed(player);
      if(!player.worldObj.isRemote) {
         ItemStack itemstack = this.getSlot(this.outputSlotIndex).getStack();
         if(itemstack != null) {
            W_EntityPlayer.dropPlayerItemWithRandomChoice(player, itemstack, false, false);
         }
      }

      MCH_Lib.DbgLog(player.worldObj, "MCH_DraftingTableGuiContainer.onContainerClosed", new Object[0]);
   }

   public void createRecipeItem(Item outputItem, Map map) {
      boolean isCreativeMode = this.player.capabilities.isCreativeMode;
      if(this.getSlot(this.outputSlotIndex).getHasStack() && !isCreativeMode) {
         MCH_Lib.DbgLog(this.player.worldObj, "MCH_DraftingTableGuiContainer.createRecipeItem:OutputSlot is not empty", new Object[0]);
      } else if(outputItem == null) {
         MCH_Lib.DbgLog(this.player.worldObj, "Error:MCH_DraftingTableGuiContainer.createRecipeItem:outputItem = null", new Object[0]);
      } else if(map != null && map.size() > 0) {
         ItemStack itemStack = new ItemStack(outputItem);
         boolean result = false;
         IRecipe recipe = null;
         MCH_IRecipeList[] recipeLists = new MCH_IRecipeList[]{MCH_ItemRecipe.getInstance(), MCH_HeliInfoManager.getInstance(), MCP_PlaneInfoManager.getInstance(), MCH_VehicleInfoManager.getInstance(), MCH_TankInfoManager.getInstance()};
         MCH_IRecipeList[] i$ = recipeLists;
         int key = recipeLists.length;

         int i;
         for(i = 0; i < key; ++i) {
            MCH_IRecipeList rl = i$[i];
            int index = this.searchRecipeFromList(rl, itemStack);
            if(index >= 0) {
               recipe = this.isValidRecipe(rl, itemStack, index, map);
               break;
            }
         }

         if(recipe != null && (isCreativeMode || MCH_Lib.canPlayerCreateItem(recipe, this.player.inventory))) {
            Iterator var13 = map.keySet().iterator();

            while(var13.hasNext()) {
               Item var14 = (Item)var13.next();

               for(i = 0; i < ((Integer)map.get(var14)).intValue(); ++i) {
                  if(!isCreativeMode) {
                     W_EntityPlayer.consumeInventoryItem(this.player, var14);
                  }

                  this.getSlot(this.outputSlotIndex).putStack(recipe.getRecipeOutput().copy());
                  result = true;
               }
            }
         }

         MCH_Lib.DbgLog(this.player.worldObj, "MCH_DraftingTableGuiContainer:Result=" + result + ":Recipe=" + recipe + " :" + outputItem.getUnlocalizedName() + ": map=" + map, new Object[0]);
      } else {
         MCH_Lib.DbgLog(this.player.worldObj, "Error:MCH_DraftingTableGuiContainer.createRecipeItem:map is null : " + map, new Object[0]);
      }
   }

   public IRecipe isValidRecipe(MCH_IRecipeList list, ItemStack itemStack, int startIndex, Map map) {
      for(int index = startIndex; index >= 0 && index < list.getRecipeListSize(); ++index) {
         IRecipe recipe = list.getRecipe(index);
         if(!itemStack.isItemEqual(recipe.getRecipeOutput())) {
            return null;
         }

         Map mapRecipe = MCH_Lib.getItemMapFromRecipe(recipe);
         boolean isEqual = true;
         Iterator i$ = map.keySet().iterator();

         while(i$.hasNext()) {
            Item key = (Item)i$.next();
            if(!mapRecipe.containsKey(key) || mapRecipe.get(key) != map.get(key)) {
               isEqual = false;
               break;
            }
         }

         if(isEqual) {
            return recipe;
         }
      }

      return null;
   }

   public int searchRecipeFromList(MCH_IRecipeList list, ItemStack item) {
      for(int i = 0; i < list.getRecipeListSize(); ++i) {
         if(list.getRecipe(i).getRecipeOutput().isItemEqual(item)) {
            return i;
         }
      }

      return -1;
   }
}
