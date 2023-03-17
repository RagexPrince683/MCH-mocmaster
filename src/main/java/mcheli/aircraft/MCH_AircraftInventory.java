package mcheli.aircraft;

import mcheli.parachute.MCH_ItemParachute;
import mcheli.wrapper.W_NBTTag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Random;

public class MCH_AircraftInventory implements IInventory {

   public final int SLOT_FUEL0 = 0;
   public final int SLOT_FUEL1 = 1;
   public final int SLOT_FUEL2 = 2;
   public final int SLOT_PARACHUTE0 = 3;
   public final int SLOT_PARACHUTE1 = 4;
   private ItemStack[] containerItems = new ItemStack[this.getSizeInventory()];
   final MCH_EntityAircraft aircraft;


   public MCH_AircraftInventory(MCH_EntityAircraft ac) {
      this.aircraft = ac;
   }

   public ItemStack getFuelSlotItemStack(int i) {
      return this.getStackInSlot(0 + i);
   }

   public ItemStack getParachuteSlotItemStack(int i) {
      return this.getStackInSlot(3 + i);
   }

   public boolean haveParachute() {
      for(int i = 0; i < 2; ++i) {
         ItemStack item = this.getParachuteSlotItemStack(i);
         if(item != null && item.getItem() instanceof MCH_ItemParachute) {
            return true;
         }
      }

      return false;
   }

   public void consumeParachute() {
      for(int i = 0; i < 2; ++i) {
         ItemStack item = this.getParachuteSlotItemStack(i);
         if(item != null && item.getItem() instanceof MCH_ItemParachute) {
            this.setInventorySlotContents(3 + i, (ItemStack)null);
            break;
         }
      }

   }

   public int getSizeInventory() {
      return 10;
   }

   public ItemStack getStackInSlot(int var1) {
      return this.containerItems[var1];
   }

   public void setDead() {
      Random rand = new Random();
      if(this.aircraft.dropContentsWhenDead && !this.aircraft.worldObj.isRemote) {
         for(int i = 0; i < this.getSizeInventory(); ++i) {
            ItemStack itemstack = this.getStackInSlot(i);
            if(itemstack != null) {
               float x = rand.nextFloat() * 0.8F + 0.1F;
               float y = rand.nextFloat() * 0.8F + 0.1F;
               float z = rand.nextFloat() * 0.8F + 0.1F;

               while(itemstack.stackSize > 0) {
                  int j = rand.nextInt(21) + 10;
                  if(j > itemstack.stackSize) {
                     j = itemstack.stackSize;
                  }

                  itemstack.stackSize -= j;
                  EntityItem entityitem = new EntityItem(this.aircraft.worldObj, this.aircraft.posX + (double)x, this.aircraft.posY + (double)y, this.aircraft.posZ + (double)z, new ItemStack(itemstack.getItem(), j, itemstack.getItemDamage()));
                  if(itemstack.hasTagCompound()) {
                     entityitem.getEntityItem().setTagCompound((NBTTagCompound)itemstack.getTagCompound().copy());
                  }

                  float f3 = 0.05F;
                  entityitem.motionX = (double)((float)rand.nextGaussian() * f3);
                  entityitem.motionY = (double)((float)rand.nextGaussian() * f3 + 0.2F);
                  entityitem.motionZ = (double)((float)rand.nextGaussian() * f3);
                  this.aircraft.worldObj.spawnEntityInWorld(entityitem);
               }
            }
         }
      }

   }

   public ItemStack decrStackSize(int par1, int par2) {
      if(this.containerItems[par1] != null) {
         ItemStack itemstack;
         if(this.containerItems[par1].stackSize <= par2) {
            itemstack = this.containerItems[par1];
            this.containerItems[par1] = null;
            return itemstack;
         } else {
            itemstack = this.containerItems[par1].splitStack(par2);
            if(this.containerItems[par1].stackSize == 0) {
               this.containerItems[par1] = null;
            }

            return itemstack;
         }
      } else {
         return null;
      }
   }

   public ItemStack getStackInSlotOnClosing(int par1) {
      if(this.containerItems[par1] != null) {
         ItemStack itemstack = this.containerItems[par1];
         this.containerItems[par1] = null;
         return itemstack;
      } else {
         return null;
      }
   }

   public void setInventorySlotContents(int par1, ItemStack par2ItemStack) {
      this.containerItems[par1] = par2ItemStack;
      if(par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit()) {
         par2ItemStack.stackSize = this.getInventoryStackLimit();
      }

   }

   public String getInventoryName() {
      return this.getInvName();
   }

   public String getInvName() {
      if(this.aircraft.getAcInfo() == null) {
         return "";
      } else {
         String s = this.aircraft.getAcInfo().displayName;
         return s.length() <= 32?s:s.substring(0, 31);
      }
   }

   public boolean isInvNameLocalized() {
      return this.aircraft.getAcInfo() != null;
   }

   public boolean hasCustomInventoryName() {
      return this.isInvNameLocalized();
   }

   public int getInventoryStackLimit() {
      return 64;
   }

   public void markDirty() {}

   public boolean isUseableByPlayer(EntityPlayer player) {
      return player.getDistanceSqToEntity(this.aircraft) <= 144.0D;
   }

   public boolean isItemValidForSlot(int par1, ItemStack par2ItemStack) {
      return true;
   }

   public boolean isStackValidForSlot(int par1, ItemStack par2ItemStack) {
      return true;
   }

   public void openInventory() {}

   public void closeInventory() {}

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      NBTTagList nbttaglist = new NBTTagList();

      for(int i = 0; i < this.containerItems.length; ++i) {
         if(this.containerItems[i] != null) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.setByte("SlotAC", (byte)i);
            this.containerItems[i].writeToNBT(nbttagcompound1);
            nbttaglist.appendTag(nbttagcompound1);
         }
      }

      par1NBTTagCompound.setTag("ItemsAC", nbttaglist);
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      NBTTagList nbttaglist = W_NBTTag.getTagList(par1NBTTagCompound, "ItemsAC", 10);
      this.containerItems = new ItemStack[this.getSizeInventory()];

      for(int i = 0; i < nbttaglist.tagCount(); ++i) {
         NBTTagCompound nbttagcompound1 = W_NBTTag.tagAt(nbttaglist, i);
         int j = nbttagcompound1.getByte("SlotAC") & 255;
         if(j >= 0 && j < this.containerItems.length) {
            this.containerItems[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
         }
      }

   }

   public void onInventoryChanged() {}

   public void openChest() {}

   public void closeChest() {}
}
