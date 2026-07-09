package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Lib;
import mcheli.MCH_Packet;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_PacketSeatListResponse extends MCH_Packet {

   public int entityID_AC = -1;
   public int seatNum = -1;
   public int[] seatEntityID = new int[]{-1};
   public int[] riderEntityID = new int[]{-1};


   public int getMessageID() {
      return 268439569;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.entityID_AC = data.readInt();
         this.seatNum = data.readShort();
         if(this.seatNum > 0) {
            this.seatEntityID = new int[this.seatNum];
            this.riderEntityID = new int[this.seatNum];

            for(int e = 0; e < this.seatNum; ++e) {
               this.seatEntityID[e] = data.readInt();
               this.riderEntityID[e] = data.readInt();
            }
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeInt(this.entityID_AC);
         if(this.seatNum > 0 && this.seatEntityID != null && this.seatEntityID.length == this.seatNum
                 && this.riderEntityID != null && this.riderEntityID.length == this.seatNum) {
            dos.writeShort(this.seatNum);

            for(int e = 0; e < this.seatNum; ++e) {
               dos.writeInt(this.seatEntityID[e]);
               dos.writeInt(this.riderEntityID[e]);
            }
         } else {
            dos.writeShort(-1);
         }
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void sendSeatList(MCH_EntityBaseVehicle ac, EntityPlayer player) {
      MCH_Lib.DbgLog(ac.worldObj, "[MCH-SYNC][SEAT-RESPONSE-SEND] aircraftId=%d aircraftUuid=%s player=%s playerUuid=%s seats=%d",
              new Object[]{Integer.valueOf(W_Entity.getEntityId(ac)), ac.getUniqueID(), player.getCommandSenderName(),
                      player.getUniqueID(), Integer.valueOf(ac.getSeats().length)});
      ac.debugVehicleState("SEAT-RESPONSE-SEND", player);
      MCH_PacketSeatListResponse s = new MCH_PacketSeatListResponse();
      s.setParameter(ac);
      W_Network.sendToPlayer(s, player);
   }

   protected void setParameter(MCH_EntityBaseVehicle ac) {
      if(ac != null) {
         this.entityID_AC = W_Entity.getEntityId(ac);
         this.seatNum = ac.getSeats().length;
         if(this.seatNum > 0) {
            this.seatEntityID = new int[this.seatNum];
            this.riderEntityID = new int[this.seatNum];

            for(int i = 0; i < this.seatNum; ++i) {
               MCH_EntitySeat seat = ac.getSeat(i);
               this.seatEntityID[i] = W_Entity.getEntityId(seat);
               this.riderEntityID[i] = seat != null && seat.riddenByEntity != null
                       && seat.riddenByEntity.ridingEntity == seat?W_Entity.getEntityId(seat.riddenByEntity):-1;
            }
         } else {
            this.seatEntityID = new int[]{-1};
            this.riderEntityID = new int[]{-1};
         }

      }
   }
}
