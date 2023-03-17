package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketSeatPlayerControl extends MCH_Packet {

   public boolean isUnmount = false;
   public byte switchSeat = 0;
   public boolean parachuting;


   public int getMessageID() {
      return 536875040;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         byte e = data.readByte();
         this.isUnmount = (e >> 3 & 1) != 0;
         this.switchSeat = (byte)(e >> 1 & 3);
         this.parachuting = (e >> 0 & 1) != 0;
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         byte e = (byte)((this.isUnmount?8:0) | this.switchSeat << 1 | (this.parachuting?1:0));
         dos.writeByte(e);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }
}
