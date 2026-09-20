package mcheli.tool;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.multiplay.MCH_Multiplay;
import mcheli.multiplay.MCH_PacketIndSpotEntity;
import mcheli.tool.rangefinder.MCH_ItemRangeFinder;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class MCH_ToolPacketHandler {

   public static void onPacket_IndSpotEntity(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_PacketIndSpotEntity pc = new MCH_PacketIndSpotEntity();
         pc.readData(data);
         ItemStack itemStack = player.getHeldItem();
         if(itemStack != null && itemStack.getItem() instanceof MCH_ItemRangeFinder) {
            if(pc.targetFilter == 0) {
               if(MCH_Multiplay.markPoint(player, player.posX, player.posY + (double)player.getEyeHeight(), player.posZ)) {
                  W_WorldFunc.MOD_playSoundAtEntity(player, "pi", 1.0F, 1.0F);
               } else {
                  W_WorldFunc.MOD_playSoundAtEntity(player, "ng", 1.0F, 1.0F);
               }
            } else if(itemStack.getItemDamage() < itemStack.getMaxDamage()) {
               MCH_Config configuration = MCH_MOD.config;
               if(MCH_Config.RangeFinderConsume.prmBool) {
                  itemStack.damageItem(1, player);
               }

               int result;
               if((pc.targetFilter & 252) == 0) {
                  result = 60;
               } else {
                  configuration = MCH_MOD.config;
                  result = MCH_Config.RangeFinderSpotTime.prmInt;
               }

               int time = result;
               double positionX = player.posX;
               double positionX2 = player.posY + (double)player.getEyeHeight();
               MCH_Config configuration2 = MCH_MOD.config;
               if(MCH_Multiplay.spotEntity(player, (MCH_EntityBaseVehicle)null, positionX, positionX2, player.posZ, pc.targetFilter, (float)MCH_Config.RangeFinderSpotDist.prmInt, time, 20.0F)) {
                  W_WorldFunc.MOD_playSoundAtEntity(player, "pi", 1.0F, 1.0F);
               } else {
                  W_WorldFunc.MOD_playSoundAtEntity(player, "ng", 1.0F, 1.0F);
               }
            }
         }

      }
   }
}
