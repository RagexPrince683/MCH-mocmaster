package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketSeatListRequest extends MCH_Packet {

   public int entityID_AC = -1;


   public int getMessageID() {
      return 536875024;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.entityID_AC = data.readInt();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeInt(this.entityID_AC);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void requestSeatList(MCH_EntityAircraft ac) {
      MCH_PacketSeatListRequest s = new MCH_PacketSeatListRequest();
      s.entityID_AC = W_Entity.getEntityId(ac);
      W_Network.sendToServer(s);
   }
}
