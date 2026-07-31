package mcheli.aircraft;

import com.google.common.io.ByteArrayDataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mcheli.MCH_Packet;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.wrapper.W_Entity;
import mcheli.wrapper.W_Network;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class MCH_PacketNotifyOnMountEntity extends MCH_Packet {

   public int entityID_Ac = -1;
   public int entityID_rider = -1;
   public int seatID = -1;
   public UUID aircraftUUID;
   public UUID riderUUID;
   public int sequence;
   private static final AtomicInteger NEXT_SEQUENCE = new AtomicInteger();


   public int getMessageID() {
      return 268439632;
   }

   public void readData(ByteArrayDataInput data) {
      try {
         this.entityID_Ac = data.readInt();
         this.entityID_rider = data.readInt();
         this.seatID = data.readShort();
         this.aircraftUUID = new UUID(data.readLong(), data.readLong());
         this.riderUUID = new UUID(data.readLong(), data.readLong());
         this.sequence = data.readInt();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   public void writeData(DataOutputStream dos) {
      try {
         dos.writeInt(this.entityID_Ac);
         dos.writeInt(this.entityID_rider);
         dos.writeShort(this.seatID);
         dos.writeLong(this.aircraftUUID.getMostSignificantBits());
         dos.writeLong(this.aircraftUUID.getLeastSignificantBits());
         dos.writeLong(this.riderUUID.getMostSignificantBits());
         dos.writeLong(this.riderUUID.getLeastSignificantBits());
         dos.writeInt(this.sequence);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

   }

   public static void send(MCH_EntityBaseVehicle ac, Entity rider, int seatId) {
      if(ac != null && rider != null) {
         Entity pilot = ac.getRiddenByEntity();
         if(pilot instanceof EntityPlayer && !pilot.isDead) {
            MCH_PacketNotifyOnMountEntity s = new MCH_PacketNotifyOnMountEntity();
            s.entityID_Ac = W_Entity.getEntityId(ac);
            s.entityID_rider = W_Entity.getEntityId(rider);
            s.seatID = seatId;
            populateIdentity(s, ac, rider);
            W_Network.sendToPlayer(s, (EntityPlayer)pilot);
         }
      }
   }

   public static void sendToRider(MCH_EntityBaseVehicle ac, EntityPlayer rider, int seatId) {
      if(ac == null || rider == null || ac.isUAV() || ac.isNewUAV()) return;
      MCH_PacketNotifyOnMountEntity packet = new MCH_PacketNotifyOnMountEntity();
      packet.entityID_Ac = W_Entity.getEntityId(ac);
      packet.entityID_rider = W_Entity.getEntityId(rider);
      packet.seatID = seatId;
      populateIdentity(packet, ac, rider);
      W_Network.sendToPlayer(packet, rider);
   }

   public static void sendDismount(MCH_EntityBaseVehicle ac, EntityPlayer rider) {
      sendToRider(ac, rider, -1);
   }

   private static void populateIdentity(MCH_PacketNotifyOnMountEntity packet, MCH_EntityBaseVehicle ac, Entity rider) {
      packet.aircraftUUID = ac.getUniqueID();
      packet.riderUUID = rider.getUniqueID();
      packet.sequence = NEXT_SEQUENCE.incrementAndGet();
   }
}
