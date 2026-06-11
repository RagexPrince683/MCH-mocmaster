package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Lib;
import mcheli.MCH_Packet;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;

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

   public static void requestSeatList(MCH_EntityBaseVehicle ac) {
      MCH_Lib.DbgLog(ac.worldObj, "[MCH-SYNC][SEAT-REQUEST-SEND] aircraftId=%d aircraftUuid=%s type=%s seats=%d",
              new Object[]{Integer.valueOf(W_Entity.getEntityId(ac)), ac.getUniqueID(), ac.getTypeName(), Integer.valueOf(ac.getSeats().length)});
      ac.debugVehicleState("SEAT-REQUEST-SEND", null);
      MCH_PacketSeatListRequest s = new MCH_PacketSeatListRequest();
      s.entityID_AC = W_Entity.getEntityId(ac);
      W_Network.sendToServer(s);
   }
}
