package mcheli.uav;

import com.google.common.io.ByteArrayDataInput;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_UavPacketStatus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_UavPacketHandler
        /*    */ {
    /*    */   public static void onPacketUavStatus(EntityPlayer player, ByteArrayDataInput data) {
        System.out.println("onPacketUavStatus fired for player: " + player.getCommandSenderName());
        /* 11 */     if (!player.worldObj.isRemote) {
            /* 12 */       MCH_UavPacketStatus status = new MCH_UavPacketStatus();
            /* 13 */       status.readData(data);
            /* 14 */       if (player.ridingEntity instanceof MCH_EntityUavStation) {
                /* 15 */         ((MCH_EntityUavStation)player.ridingEntity).setUavPosition(status.posUavX, status.posUavY, status.posUavZ);
                /* 16 */         if (status.continueControl)
                    /* 17 */           ((MCH_EntityUavStation)player.ridingEntity).controlLastAircraft((Entity)player);
                /*    */       }
            /*    */     }
        /*    */   }
    /*    */ }
