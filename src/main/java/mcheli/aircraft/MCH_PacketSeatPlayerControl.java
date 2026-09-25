package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Packet;

public class MCH_PacketSeatPlayerControl extends MCH_Packet {

   public boolean isUnmount = false;
   public byte switchSeat = 0;
   public boolean parachuting;
   public int dismountMountEntityId = -1;
   public int dismountParentEntityId = -1;
   public int dismountSeatId = -1;
   /** 1 starts a physical hold, 2 cancels it. Normal completion still uses isUnmount. */
   public byte dismountHoldAction;


   public int getMessageID() {
      return 536875040;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         byte e = data.readByte();
         this.isUnmount = (e >> 3 & 1) != 0;
         this.switchSeat = (byte)(e >> 1 & 3);
         this.parachuting = (e >> 0 & 1) != 0;
         this.dismountMountEntityId = data.readInt();
         this.dismountParentEntityId = data.readInt();
         this.dismountSeatId = data.readInt();
         this.dismountHoldAction = data.readByte();
      } catch (Exception exception) {
         exception.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         byte e = (byte)((this.isUnmount?8:0) | this.switchSeat << 1 | (this.parachuting?1:0));
         dos.writeByte(e);
         dos.writeInt(this.dismountMountEntityId);
         dos.writeInt(this.dismountParentEntityId);
         dos.writeInt(this.dismountSeatId);
         dos.writeByte(this.dismountHoldAction);
      } catch (IOException oException) {
         oException.printStackTrace();
      }

   }
}
