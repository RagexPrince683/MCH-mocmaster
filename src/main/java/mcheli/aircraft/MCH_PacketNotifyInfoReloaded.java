package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import mcheli.MCH_Packet;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_PacketNotifyInfoReloaded extends MCH_Packet {

   public int type = -1;
   public int entityId = -1;
   public String entityUuid = "";
   public String definition = "";
   public boolean success;
   public String reason = "";


   public int getMessageID() {
      return 536875063;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.type = data.readInt();
         if(this.type == 3 || this.type == 4) {
            this.entityId = data.readInt();
            this.entityUuid = data.readUTF();
            if(this.type == 4) {
               this.definition = data.readUTF();
               this.success = data.readBoolean();
               this.reason = data.readUTF();
            }
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeInt(this.type);
         if(this.type == 3 || this.type == 4) {
            dos.writeInt(this.entityId);
            dos.writeUTF(this.entityUuid);
            if(this.type == 4) {
               dos.writeUTF(this.definition);
               dos.writeBoolean(this.success);
               dos.writeUTF(this.reason);
            }
         }
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void sendRealodAc() {
      MCH_PacketNotifyInfoReloaded s = new MCH_PacketNotifyInfoReloaded();
      s.type = 0;
      W_Network.sendToServer(s);
   }

   public static void sendTargetedRequest(MCH_EntityBaseVehicle vehicle) {
      MCH_PacketNotifyInfoReloaded p = new MCH_PacketNotifyInfoReloaded();
      p.type = 3; p.entityId = vehicle.getEntityId(); p.entityUuid = vehicle.getUniqueID().toString();
      W_Network.sendToServer(p);
   }

   public static void sendTargetedResult(EntityPlayer player, MCH_EntityBaseVehicle vehicle,
         String definition, boolean success, String reason) {
      MCH_PacketNotifyInfoReloaded p = new MCH_PacketNotifyInfoReloaded();
      p.type = 4; p.entityId = vehicle == null ? -1 : vehicle.getEntityId();
      p.entityUuid = vehicle == null ? "" : vehicle.getUniqueID().toString();
      p.definition = definition == null ? "" : definition; p.success = success;
      p.reason = reason == null ? "" : reason;
      W_Network.sendToPlayer(p, player);
   }

   public static void sendRealodAllWeapon() {
      MCH_PacketNotifyInfoReloaded s = new MCH_PacketNotifyInfoReloaded();
      s.type = 1;
      W_Network.sendToServer(s);
   }

   public static void sendToAllClients(int type) {
      MCH_PacketNotifyInfoReloaded s = new MCH_PacketNotifyInfoReloaded();
      s.type = type;
      W_Network.sendToAllPlayers(s);
   }
}
