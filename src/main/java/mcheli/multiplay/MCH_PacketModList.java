package mcheli.multiplay;

import com.google.common.io.ByteArrayDataInput;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_PacketModList extends MCH_Packet {

   public boolean firstData = false;
   public int id = 0;
   public int num = 0;
   public List list = new ArrayList();


   public int getMessageID() {
      return 536873473;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.firstData = data.readByte() == 1;
         this.id = data.readInt();
         this.num = data.readInt();

         for(int e = 0; e < this.num; ++e) {
            this.list.add(data.readUTF());
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeByte(this.firstData?1:0);
         dos.writeInt(this.id);
         dos.writeInt(this.num);
         Iterator e = this.list.iterator();

         while(e.hasNext()) {
            String s = (String)e.next();
            dos.writeUTF(s);
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   public static void send(EntityPlayer player, MCH_PacketModList p) {
      W_Network.sendToPlayer(p, player);
   }

   public static void send(List list, int id) {
      MCH_PacketModList p = null;
      int size = 0;
      boolean isFirst = true;
      Iterator i$ = list.iterator();

      while(i$.hasNext()) {
         String s = (String)i$.next();
         if(p == null) {
            p = new MCH_PacketModList();
            p.id = id;
            p.firstData = isFirst;
            isFirst = false;
         }

         p.list.add(s);
         size += s.length() + 2;
         if(size > 1024) {
            p.num = p.list.size();
            W_Network.sendToServer(p);
            p = null;
            size = 0;
         }
      }

      if(p != null) {
         p.num = p.list.size();
         W_Network.sendToServer(p);
      }

   }
}
