package mcheli.tool.rangefinder;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.multiplay.MCH_PacketIndSpotEntity;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Reflection;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MCH_ItemRangeFinder extends W_Item {

   public static int rangeFinderUseCooldown = 0;
   public static boolean continueUsingItem = false;
   public static float zoom = 2.0F;
   public static int mode = 0;


   public MCH_ItemRangeFinder(int itemId) {
      super(itemId);
      super.maxStackSize = 1;
      this.setMaxDamage(10);
   }

   public static boolean canUse(EntityPlayer player) {
      if(player == null) {
         return false;
      } else if(player.worldObj == null) {
         return false;
      } else if(player.getCurrentEquippedItem() == null) {
         return false;
      } else if(!(player.getCurrentEquippedItem().getItem() instanceof MCH_ItemRangeFinder)) {
         return false;
      } else if(player.ridingEntity instanceof MCH_EntityAircraft) {
         return false;
      } else {
         if(player.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntityAircraft ac = ((MCH_EntitySeat)player.ridingEntity).getParent();
            if(ac != null && (ac.getIsGunnerMode(player) || ac.getWeaponIDBySeatID(ac.getSeatIdByEntity(player)) >= 0)) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean isUsingScope(EntityPlayer player) {
      return player.getItemInUseDuration() > 8 || continueUsingItem;
   }

   public static void onStartUseItem() {
      zoom = 2.0F;
      W_Reflection.setCameraZoom(2.0F);
      continueUsingItem = true;
   }

   public static void onStopUseItem() {
      W_Reflection.restoreCameraZoom();
      continueUsingItem = false;
   }

   @SideOnly(Side.CLIENT)
   public void spotEntity(EntityPlayer player, ItemStack itemStack) {
      if(player != null && player.worldObj.isRemote && rangeFinderUseCooldown == 0 && player.getItemInUseDuration() > 8) {
         if(mode == 2) {
            rangeFinderUseCooldown = 60;
            MCH_PacketIndSpotEntity.send(player, 0);
         } else if(itemStack.getItemDamage() < itemStack.getMaxDamage()) {
            rangeFinderUseCooldown = 60;
            MCH_PacketIndSpotEntity.send(player, mode == 0?60:3);
         } else {
            W_McClient.MOD_playSoundFX("ng", 1.0F, 1.0F);
         }
      }

   }

   public void onPlayerStoppedUsing(ItemStack p_77615_1_, World p_77615_2_, EntityPlayer p_77615_3_, int p_77615_4_) {
      if(p_77615_2_.isRemote) {
         onStopUseItem();
      }

   }

   public ItemStack onEaten(ItemStack p_77654_1_, World p_77654_2_, EntityPlayer p_77654_3_) {
      return p_77654_1_;
   }

   @SideOnly(Side.CLIENT)
   public boolean isFull3D() {
      return true;
   }

   public EnumAction getItemUseAction(ItemStack itemStack) {
      return EnumAction.bow;
   }

   public int getMaxItemUseDuration(ItemStack itemStack) {
      return 72000;
   }

   public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
      if(canUse(player)) {
         player.setItemInUse(itemStack, this.getMaxItemUseDuration(itemStack));
      }

      return itemStack;
   }

}
