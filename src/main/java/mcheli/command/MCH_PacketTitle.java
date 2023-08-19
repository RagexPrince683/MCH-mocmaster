package mcheli.command;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IChatComponent.Serializer;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketTitle extends MCH_Packet {

   public IChatComponent chatComponent = null;
   public int showTime = 1;
   public int position = 0;


   public int getMessageID() {
      return 268438272;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.chatComponent = Serializer.func_150699_a(data.readUTF());
         this.showTime = data.readShort();
         this.position = data.readShort();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeUTF(Serializer.func_150696_a(this.chatComponent));
         dos.writeShort(this.showTime);
         dos.writeShort(this.position);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void send(IChatComponent chat, int showTime, int pos) {
      MCH_PacketTitle s = new MCH_PacketTitle();
      s.chatComponent = chat;
      s.showTime = showTime;
      s.position = pos;
      W_Network.sendToAllPlayers(s);
   }
}
