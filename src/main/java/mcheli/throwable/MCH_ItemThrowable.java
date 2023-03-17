package mcheli.throwable;

import mcheli.MCH_Lib;
import mcheli.wrapper.W_Item;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemThrowable extends W_Item {

   public MCH_ItemThrowable(int par1) {
      super(par1);
      this.setMaxStackSize(1);
   }

   public static void registerDispenseBehavior(Item item) {
      BlockDispenser.dispenseBehaviorRegistry.putObject(item, new MCH_ItemThrowableDispenseBehavior());
   }

   public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
      player.setItemInUse(itemStack, this.getMaxItemUseDuration(itemStack));
      return itemStack;
   }

   public void onPlayerStoppedUsing(ItemStack itemStack, World world, EntityPlayer player, int par4) {
      if(itemStack != null && itemStack.stackSize > 0) {
         MCH_ThrowableInfo info = MCH_ThrowableInfoManager.get(itemStack.getItem());
         if(info != null) {
            if(!player.capabilities.isCreativeMode) {
               --itemStack.stackSize;
               if(itemStack.stackSize <= 0) {
                  player.inventory.mainInventory[player.inventory.currentItem] = null;
               }
            }

            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (Item.itemRand.nextFloat() * 0.4F + 0.8F));
            if(!world.isRemote) {
               float acceleration = 1.0F;
               par4 = itemStack.getMaxItemUseDuration() - par4;
               if(par4 <= 35) {
                  if(par4 < 5) {
                     par4 = 5;
                  }

                  acceleration = (float)par4 / 25.0F;
               }

               MCH_Lib.DbgLog(world, "MCH_ItemThrowable.onPlayerStoppedUsing(%d)", new Object[]{Integer.valueOf(par4)});
               MCH_EntityThrowable entity = new MCH_EntityThrowable(world, player, acceleration);
               entity.setInfo(info);
               world.spawnEntityInWorld(entity);
            }
         }
      }

   }

   public int getMaxItemUseDuration(ItemStack par1ItemStack) {
      return 72000;
   }

   public EnumAction getItemUseAction(ItemStack par1ItemStack) {
      return EnumAction.bow;
   }
}
