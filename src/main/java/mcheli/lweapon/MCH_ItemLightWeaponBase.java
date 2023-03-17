package mcheli.lweapon;

import mcheli.wrapper.W_Item;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class MCH_ItemLightWeaponBase extends W_Item {

   public final MCH_ItemLightWeaponBullet bullet;


   public MCH_ItemLightWeaponBase(int par1, MCH_ItemLightWeaponBullet bullet) {
      super(par1);
      this.setMaxDamage(10);
      this.setMaxStackSize(1);
      this.bullet = bullet;
   }

   public static String getName(ItemStack itemStack) {
      if(itemStack != null && itemStack.getItem() instanceof MCH_ItemLightWeaponBase) {
         String name = itemStack.getUnlocalizedName();
         int li = name.lastIndexOf(":");
         if(li >= 0) {
            name = name.substring(li + 1);
         }

         return name;
      } else {
         return "";
      }
   }

   public static boolean isHeld(EntityPlayer player) {
      ItemStack is = player != null?player.getHeldItem():null;
      return is != null && is.getItem() instanceof MCH_ItemLightWeaponBase?player.getItemInUseDuration() > 10:false;
   }

   public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
      PotionEffect pe = player.getActivePotionEffect(Potion.nightVision);
      if(pe != null && pe.getDuration() < 220) {
         player.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), 250, 0, false));
      }

   }

   public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
      return true;
   }

   public EnumAction getItemUseAction(ItemStack par1ItemStack) {
      return EnumAction.bow;
   }

   public int getMaxItemUseDuration(ItemStack par1ItemStack) {
      return 72000;
   }

   public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer) {
      if(par1ItemStack != null) {
         par3EntityPlayer.setItemInUse(par1ItemStack, this.getMaxItemUseDuration(par1ItemStack));
      }

      return par1ItemStack;
   }
}
