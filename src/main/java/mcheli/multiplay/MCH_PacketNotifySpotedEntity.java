package mcheli.multiplay;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataOutputStream;

public class MCH_PacketNotifySpotedEntity extends MCH_Packet {

   public int count = 0;
   public int num = 0;
   public int[] entityId = null;


   public int getMessageID() {
      return 268437761;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.count = data.readShort();
         this.num = data.readShort();
         if(this.num > 0) {
            this.entityId = new int[this.num];

            for(int e = 0; e < this.num; ++e) {
               this.entityId[e] = data.readInt();
            }
         } else {
            this.num = 0;
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeShort(this.count);
         dos.writeShort(this.num);

         for(int e = 0; e < this.num; ++e) {
            dos.writeInt(this.entityId[e]);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public static void send(EntityPlayer player, int count, int[] entityId) {
      if(player != null && entityId != null && entityId.length > 0 && count > 0) {
         if(count > 30000) {
            count = 30000;
         }

         MCH_PacketNotifySpotedEntity pkt = new MCH_PacketNotifySpotedEntity();
         pkt.count = count;
         pkt.num = entityId.length;
         if(pkt.num > 300) {
            pkt.num = 300;
         }

         if(pkt.num > entityId.length) {
            pkt.num = entityId.length;
         }

         pkt.entityId = entityId;
         W_Network.sendToPlayer(pkt, player);
      }
   }
}
