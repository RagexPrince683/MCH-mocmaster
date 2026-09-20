package mcheli.command;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;

public class MCH_PacketCommandSave extends MCH_Packet {

   public String str = "";


   public int getMessageID() {
      return 536873729;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.str = data.readUTF();
      } catch (Exception exception) {
         exception.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeUTF(this.str);
      } catch (IOException oException) {
         oException.printStackTrace();
      }

   }

   public static void send(String cmd) {
      MCH_PacketCommandSave s = new MCH_PacketCommandSave();
      s.str = cmd;
      W_Network.sendToServer(s);
   }
}
