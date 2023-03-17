package mcheli.uav;

import com.google.common.io.ByteArrayDataInput;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_UavPacketHandler {

   public static void onPacketUavStatus(EntityPlayer player, ByteArrayDataInput data) {
      if(!player.worldObj.isRemote) {
         MCH_UavPacketStatus status = new MCH_UavPacketStatus();
         status.readData(data);
         if(player.ridingEntity instanceof MCH_EntityUavStation) {
            ((MCH_EntityUavStation)player.ridingEntity).setUavPosition(status.posUavX, status.posUavY, status.posUavZ);
            if(status.continueControl) {
               ((MCH_EntityUavStation)player.ridingEntity).controlLastAircraft(player);
            }
         }
      }

   }
}
