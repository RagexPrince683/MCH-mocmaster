package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_ItemAircraft;
import mcheli.wrapper.W_Item;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MCH_CreativeTabs extends CreativeTabs {

   private List iconItems = new ArrayList();
   private Item lastItem;
   private int currentIconIndex = 0;
   private int switchItemWait = 0;
   private Item fixedItem = null;


   public MCH_CreativeTabs(String label) {
      super(label);
   }

   public void setFixedIconItem(String itemName) {
      if(itemName.indexOf(58) >= 0) {
         this.fixedItem = W_Item.getItemByName(itemName);
         if(this.fixedItem != null) {
            this.fixedItem.setTextureName(itemName);
         }
      } else {
         this.fixedItem = W_Item.getItemByName("mcheli:" + itemName);
         if(this.fixedItem != null) {
            this.fixedItem.setTextureName("mcheli:" + itemName);
         }
      }

   }

   public Item getTabIconItem() {
      if(this.iconItems.size() <= 0) {
         return null;
      } else {
         this.currentIconIndex = (this.currentIconIndex + 1) % this.iconItems.size();
         return (Item)this.iconItems.get(this.currentIconIndex);
      }
   }

   public ItemStack getIconItemStack() {
      if(this.fixedItem != null) {
         return new ItemStack(this.fixedItem, 1, 0);
      } else {
         if(this.switchItemWait > 0) {
            --this.switchItemWait;
         } else {
            this.lastItem = this.getTabIconItem();
            this.switchItemWait = 60;
         }

         if(this.lastItem == null) {
            this.lastItem = W_Item.getItemByName("iron_block");
         }

         return new ItemStack(this.lastItem, 1, 0);
      }
   }

   @SideOnly(Side.CLIENT)
   public void displayAllReleventItems(List list) {
      super.displayAllReleventItems(list);
      Comparator<ItemStack> cmp = new Comparator<ItemStack>(){
         public int compare(ItemStack i1, ItemStack i2) {
            if(i1.getItem() instanceof MCH_ItemAircraft && i2.getItem() instanceof MCH_ItemAircraft) {
               MCH_AircraftInfo info1 = ((MCH_ItemAircraft)i1.getItem()).getAircraftInfo();
               MCH_AircraftInfo info2 = ((MCH_ItemAircraft)i2.getItem()).getAircraftInfo();
               if(info1 != null && info2 != null) {
                  String s1 = info1.category + "." + info1.name;
                  String s2 = info2.category + "." + info2.name;
                  return s1.compareTo(s2);
               }
            }

            return i1.getItem().getUnlocalizedName().compareTo(i2.getItem().getUnlocalizedName());
         }
      };
      Collections.sort(list, cmp);
   }

   public void addIconItem(Item i) {
      if(i != null) {
         this.iconItems.add(i);
      }

   }

   public String getTranslatedTabLabel() {
      return "MC Heli";
   }
}
