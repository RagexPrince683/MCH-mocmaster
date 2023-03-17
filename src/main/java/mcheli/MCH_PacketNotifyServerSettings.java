package mcheli;

import com.google.common.io.ByteArrayDataInput;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.DataOutputStream;
import java.io.IOException;

public class MCH_PacketNotifyServerSettings extends MCH_Packet {

   public boolean enableCamDistChange = true;
   public boolean enableEntityMarker = true;
   public boolean enablePVP = true;
   public double stingerLockRange = 120.0D;
   public boolean enableDebugBoundingBox = true;


   public int getMessageID() {
      return 268437568;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         byte e = data.readByte();
         this.enableCamDistChange = this.getBit(e, 0);
         this.enableEntityMarker = this.getBit(e, 1);
         this.enablePVP = this.getBit(e, 2);
         this.stingerLockRange = (double)data.readFloat();
         this.enableDebugBoundingBox = this.getBit(e, 3);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         byte e = 0;
         byte e1 = this.setBit(e, 0, this.enableCamDistChange);
         e1 = this.setBit(e1, 1, this.enableEntityMarker);
         e1 = this.setBit(e1, 2, this.enablePVP);
         e1 = this.setBit(e1, 3, this.enableDebugBoundingBox);
         dos.writeByte(e1);
         dos.writeFloat((float)this.stingerLockRange);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void send(EntityPlayerMP player) {
      MCH_PacketNotifyServerSettings s = new MCH_PacketNotifyServerSettings();
      MCH_Config var10001 = MCH_MOD.config;
      s.enableCamDistChange = !MCH_Config.DisableCameraDistChange.prmBool;
      var10001 = MCH_MOD.config;
      s.enableEntityMarker = MCH_Config.DisplayEntityMarker.prmBool;
      s.enablePVP = MinecraftServer.getServer().isPVPEnabled();
      var10001 = MCH_MOD.config;
      s.stingerLockRange = MCH_Config.StingerLockRange.prmDouble;
      var10001 = MCH_MOD.config;
      s.enableDebugBoundingBox = MCH_Config.EnableDebugBoundingBox.prmBool;
      if(player != null) {
         W_Network.sendToPlayer(s, player);
      } else {
         W_Network.sendToAllPlayers(s);
      }

   }

   public static void sendAll() {
      send((EntityPlayerMP)null);
   }
}
