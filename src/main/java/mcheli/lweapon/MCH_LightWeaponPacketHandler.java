package mcheli.lweapon;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Lib;
import mcheli.lweapon.MCH_ItemLightWeaponBase;
import mcheli.lweapon.MCH_PacketLightWeaponPlayerControl;
import mcheli.weapon.MCH_IEntityLockChecker;
import mcheli.weapon.MCH_WeaponBase;
import mcheli.weapon.MCH_WeaponCreator;
import mcheli.weapon.MCH_WeaponParam;
import mcheli.wrapper.W_EntityPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;

public class MCH_LightWeaponPacketHandler {

   public static void onPacket_PlayerControl(EntityPlayer player, ByteArrayDataInput data) {

      if (player.worldObj.isRemote) // Check if the world is remote (client-side)
         return;

      MCH_PacketLightWeaponPlayerControl pc = new MCH_PacketLightWeaponPlayerControl();
      pc.readData(data);

      if (pc.camMode == 1) {
         System.out.println(pc.camMode + "is cam mode");
         player.removePotionEffect(Potion.nightVision.id); // Remove night vision if camMode is 1
      }

      ItemStack is = player.getHeldItem();
      if (is == null || !(is.getItem() instanceof MCH_ItemLightWeaponBase))
         return;

      MCH_ItemLightWeaponBase lweapon = (MCH_ItemLightWeaponBase) is.getItem();

      if (pc.camMode == 2 && MCH_ItemLightWeaponBase.isHeld(player)) {
         System.out.println(pc.camMode + "is cam mode");
         player.addPotionEffect(new PotionEffect(Potion.nightVision.id, 255, 0, false)); // Add night vision if camMode is 2
      }

      if (pc.camMode > 0) {
         System.out.println(pc.camMode + "is cam mode");
         MCH_Lib.DbgLog(false, "MCH_LightWeaponPacketHandler NV=%s", (pc.camMode == 2) ? "ON" : "OFF");
      }

      if (pc.useWeapon && is.getItemDamage() < is.getMaxDamage()) {
         String name = MCH_ItemLightWeaponBase.getName(player.getHeldItem());
         MCH_WeaponBase w = MCH_WeaponCreator.createWeapon(player.worldObj, name, Vec3.createVectorHelper(0.0D, 0.0D, 0.0D), 0.0F, 0.0F, null, false);
         MCH_WeaponParam prm = new MCH_WeaponParam();
         prm.entity = player;
         prm.user = player;
         prm.setPosAndRot(pc.useWeaponPosX, pc.useWeaponPosY, pc.useWeaponPosZ, player.rotationYaw, player.rotationPitch);
         prm.option1 = pc.useWeaponOption1;
         prm.option2 = pc.useWeaponOption2;
         w.shot(prm);

         if (!player.capabilities.isCreativeMode) {
            if (is.getMaxDamage() == 1) {
               is.stackSize--;
            } else {
               is.setItemDamage(is.getMaxDamage());
            }
         }
      } else if (pc.cmpReload > 0) {
         if (is.getItemDamage() > 1 && W_EntityPlayer.hasItem(player, lweapon.bullet)) {
            if (!player.capabilities.isCreativeMode) {
               W_EntityPlayer.consumeInventoryItem(player, lweapon.bullet);
            }
            is.setItemDamage(0);
         }
      }
   }
}
