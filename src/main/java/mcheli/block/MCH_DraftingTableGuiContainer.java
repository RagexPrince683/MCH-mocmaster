package mcheli.block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import mcheli.MCH_IRecipeList;
import mcheli.MCH_ItemRecipe;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.vehicle.MCH_TurretInfoManager;
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
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

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

   private IRecipe findRecipeByOutput(ItemStack output) {
      MCH_IRecipeList[] recipeLists = new MCH_IRecipeList[] {
              MCH_ItemRecipe.getInstance(),
              MCH_HeliInfoManager.getInstance(),
              MCP_PlaneInfoManager.getInstance(),
              MCH_TurretInfoManager.getInstance(),
              MCH_TankInfoManager.getInstance(),
              MCH_ShipInfoManager.getInstance()
      };

      for(int i = 0; i < recipeLists.length; ++i) {
         MCH_IRecipeList list = recipeLists[i];

         for(int j = 0; j < list.getRecipeListSize(); ++j) {
            IRecipe recipe = list.getRecipe(j);

            if(recipe != null &&
                    recipe.getRecipeOutput() != null &&
                    recipe.getRecipeOutput().isItemEqual(output)) {
               return recipe;
            }
         }
      }

      return null;
   }

   private boolean canConsumeRecipeIngredients(IRecipe recipe) {
      ArrayList ingredients = this.getRecipeIngredientObjects(recipe);

      for(int i = 0; i < ingredients.size(); ++i) {
         Object ingredient = ingredients.get(i);

         if(ingredient == null) {
            continue;
         }

         int invSlot = this.findInventorySlotForIngredient(ingredient, null);

         if(invSlot < 0) {
            return false;
         }
      }

      return true;
   }

   private void consumeRecipeIngredients(IRecipe recipe) {
      ArrayList ingredients = this.getRecipeIngredientObjects(recipe);
      ArrayList usedSlots = new ArrayList();

      for(int i = 0; i < ingredients.size(); ++i) {
         Object ingredient = ingredients.get(i);

         if(ingredient == null) {
            continue;
         }

         int invSlot = this.findInventorySlotForIngredient(ingredient, usedSlots);

         if(invSlot >= 0) {
            ItemStack stack = this.player.inventory.getStackInSlot(invSlot);

            if(stack != null) {
               --stack.stackSize;

               if(stack.stackSize <= 0) {
                  this.player.inventory.setInventorySlotContents(invSlot, null);
               }

               usedSlots.add(Integer.valueOf(invSlot));
            }
         }
      }

      this.player.inventory.markDirty();
   }

   private ArrayList getRecipeIngredientObjects(IRecipe recipe) {
      ArrayList list = new ArrayList();

      if(recipe instanceof ShapedRecipes) {
         ShapedRecipes rcp = (ShapedRecipes)recipe;

         for(int i = 0; i < rcp.recipeItems.length; ++i) {
            if(rcp.recipeItems[i] != null) {
               list.add(rcp.recipeItems[i]);
            }
         }
      } else if(recipe instanceof ShapelessRecipes) {
         ShapelessRecipes rcp = (ShapelessRecipes)recipe;

         for(int i = 0; i < rcp.recipeItems.size(); ++i) {
            Object obj = rcp.recipeItems.get(i);

            if(obj != null) {
               list.add(obj);
            }
         }
      } else if(recipe instanceof ShapedOreRecipe) {
         ShapedOreRecipe rcp = (ShapedOreRecipe)recipe;
         Object[] input = rcp.getInput();

         for(int i = 0; i < input.length; ++i) {
            if(input[i] != null) {
               list.add(input[i]);
            }
         }
      } else if(recipe instanceof ShapelessOreRecipe) {
         ShapelessOreRecipe rcp = (ShapelessOreRecipe)recipe;
         ArrayList input = rcp.getInput();

         for(int i = 0; i < input.size(); ++i) {
            Object obj = input.get(i);

            if(obj != null) {
               list.add(obj);
            }
         }
      } else {
         // Fallback for old exact-item recipe logic.
         Map mapRecipe = MCH_Lib.getItemMapFromRecipe(recipe);

         if(mapRecipe != null) {
            Iterator it = mapRecipe.keySet().iterator();

            while(it.hasNext()) {
               Item item = (Item)it.next();
               int count = ((Integer)mapRecipe.get(item)).intValue();

               for(int i = 0; i < count; ++i) {
                  list.add(new ItemStack(item));
               }
            }
         }
      }

      return list;
   }

   private int findInventorySlotForIngredient(Object ingredient, ArrayList usedSlots) {
      for(int i = 0; i < this.player.inventory.mainInventory.length; ++i) {
         if(usedSlots != null && usedSlots.contains(Integer.valueOf(i))) {
            continue;
         }

         ItemStack stack = this.player.inventory.mainInventory[i];

         if(stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            continue;
         }

         if(this.doesStackMatchIngredient(stack, ingredient)) {
            return i;
         }
      }

      return -1;
   }

   private boolean doesStackMatchIngredient(ItemStack stack, Object ingredient) {
      if(stack == null || stack.getItem() == null || ingredient == null) {
         return false;
      }

      if(ingredient instanceof ItemStack) {
         return this.doesStackMatchStack(stack, (ItemStack)ingredient);
      }

      if(ingredient instanceof List) {
         List list = (List)ingredient;

         for(int i = 0; i < list.size(); ++i) {
            Object entry = list.get(i);

            if(this.doesStackMatchIngredient(stack, entry)) {
               return true;
            }
         }

         return false;
      }

      if(ingredient instanceof String) {
         ArrayList ores = OreDictionary.getOres((String)ingredient);

         for(int i = 0; i < ores.size(); ++i) {
            Object entry = ores.get(i);

            if(entry instanceof ItemStack && this.doesStackMatchStack(stack, (ItemStack)entry)) {
               return true;
            }
         }

         return false;
      }

      return false;
   }

   private boolean doesStackMatchStack(ItemStack stack, ItemStack target) {
      if(stack == null || target == null) {
         return false;
      }

      if(stack.getItem() == null || target.getItem() == null) {
         return false;
      }

      if(stack.getItem() != target.getItem()) {
         return false;
      }

      int targetDamage = target.getItemDamage();

      return targetDamage == OreDictionary.WILDCARD_VALUE || stack.getItemDamage() == targetDamage;
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
         MCH_Lib.DbgLog(this.player.worldObj,
                        "MCH_DraftingTableGuiContainer.createRecipeItem:OutputSlot is not empty",
                        new Object[0]);
         return;
      }

      if(outputItem == null) {
         MCH_Lib.DbgLog(this.player.worldObj,
                        "Error:MCH_DraftingTableGuiContainer.createRecipeItem:outputItem = null",
                        new Object[0]);
         return;
      }

      ItemStack outputStack = new ItemStack(outputItem);
      IRecipe recipe = this.findRecipeByOutput(outputStack);

      if(recipe == null) {
         MCH_Lib.DbgLog(this.player.worldObj,
                        "Error:MCH_DraftingTableGuiContainer.createRecipeItem:recipe not found for " + outputItem.getUnlocalizedName(),
                        new Object[0]);
         return;
      }

      MCH_Lib.DbgLog(this.player.worldObj,
                     "Drafting create: recipeClass=" + recipe.getClass().getName() +
                             " output=" + outputItem.getUnlocalizedName(),
                     new Object[0]);

      if(!isCreativeMode) {
         if(!this.canConsumeRecipeIngredients(recipe)) {
            MCH_Lib.DbgLog(this.player.worldObj,
                           "Error:MCH_DraftingTableGuiContainer.createRecipeItem:not enough ingredients for " + outputItem.getUnlocalizedName(),
                           new Object[0]);
            return;
         }

         this.consumeRecipeIngredients(recipe);
      }

      this.getSlot(this.outputSlotIndex).putStack(recipe.getRecipeOutput().copy());
      this.getSlot(this.outputSlotIndex).onSlotChanged();

      MCH_Lib.DbgLog(this.player.worldObj,
                     "MCH_DraftingTableGuiContainer.createRecipeItem:SUCCESS output=" + outputItem.getUnlocalizedName(),
                     new Object[0]);
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
