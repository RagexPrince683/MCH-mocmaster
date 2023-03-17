package mcheli.block;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Lib;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_DraftingTablePacketHandler {

   public static void onPacketCreate(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_DraftingTableCreatePacket packet = new MCH_DraftingTableCreatePacket();
         packet.readData(data);
         boolean openScreen = player.openContainer instanceof MCH_DraftingTableGuiContainer;
         MCH_Lib.DbgLog(false, "MCH_DraftingTablePacketHandler.onPacketCreate : " + openScreen, new Object[0]);
         if(openScreen) {
            ((MCH_DraftingTableGuiContainer)player.openContainer).createRecipeItem(packet.outputItem, packet.map);
         }
      }

   }
}
