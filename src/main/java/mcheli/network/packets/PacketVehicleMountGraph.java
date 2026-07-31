package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import mcheli.MCH_Lib;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/** A bounded, observer-directed description of a normal vehicle's mount graph. */
public class PacketVehicleMountGraph extends PacketBase {
    public static final int PROTOCOL_VERSION = 1;
    private static final int MAX_RIDERS = 64;
    private static final int MAX_STRING_BYTES = 128;
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final List<Pending> PENDING = new ArrayList<Pending>();
    private static int clientWorldIdentity;
    private static int clientPlayerIdentity;

    public int version = PROTOCOL_VERSION;
    public int observerId;
    public int dimension;
    public int vehicleId;
    public String commonUniqueId = "";
    public String aircraftType = "";
    public int sequence;
    public List<RiderEntry> riders = Collections.emptyList();

    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeByte(version);
        data.writeInt(observerId);
        data.writeInt(dimension);
        data.writeInt(vehicleId);
        writeString(data, commonUniqueId);
        writeString(data, aircraftType);
        data.writeInt(sequence);
        int count = Math.min(riders.size(), MAX_RIDERS);
        data.writeByte(count);
        for(int i = 0; i < count; ++i) {
            RiderEntry entry = riders.get(i);
            data.writeInt(entry.riderId);
            data.writeInt(entry.seatEntityId);
            data.writeShort(entry.seatIndex);
            writeString(data, entry.parentCommonUniqueId);
            writeString(data, entry.riderClass);
            data.writeBoolean(entry.playerProfileId != null);
            if(entry.playerProfileId != null) {
                data.writeLong(entry.playerProfileId.getMostSignificantBits());
                data.writeLong(entry.playerProfileId.getLeastSignificantBits());
            }
        }
    }

    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        version = data.readUnsignedByte();
        observerId = data.readInt();
        dimension = data.readInt();
        vehicleId = data.readInt();
        commonUniqueId = readString(data);
        aircraftType = readString(data);
        sequence = data.readInt();
        int count = data.readUnsignedByte();
        if(count > MAX_RIDERS) throw new IllegalArgumentException("Mount graph exceeds " + MAX_RIDERS + " riders");
        List<RiderEntry> decoded = new ArrayList<RiderEntry>(count);
        for(int i = 0; i < count; ++i) {
            RiderEntry entry = new RiderEntry();
            entry.riderId = data.readInt();
            entry.seatEntityId = data.readInt();
            entry.seatIndex = data.readShort();
            entry.parentCommonUniqueId = readString(data);
            entry.riderClass = readString(data);
            if(data.readBoolean()) entry.playerProfileId = new UUID(data.readLong(), data.readLong());
            decoded.add(entry);
        }
        riders = decoded;
    }

    public void handleClientSide(EntityPlayer player) {
        if(player == null || version != PROTOCOL_VERSION || observerId != player.getEntityId()
            || dimension != player.dimension) return;
        synchronized(PENDING) {
            resetForWorld(player);
            for(int i = PENDING.size() - 1; i >= 0; --i) {
                Pending old = PENDING.get(i);
                if(old.packet.vehicleId == vehicleId && old.packet.commonUniqueId.equals(commonUniqueId)
                    && old.packet.sequence <= sequence) PENDING.remove(i);
            }
            if(PENDING.size() >= 64) PENDING.remove(0);
            PENDING.add(new Pending(this));
        }
        MCH_Lib.DbgLog(true, "[MCH-MOUNT-SYNC] mount-graph-received vehicle=%d sequence=%d riders=%d", vehicleId, sequence, riders.size());
    }

    public void handleServerSide(EntityPlayerMP player) { }

    public static void tickClient(EntityPlayer player) {
        if(player == null || !player.worldObj.isRemote) return;
        synchronized(PENDING) {
            resetForWorld(player);
            for(int i = PENDING.size() - 1; i >= 0; --i) {
                Pending pending = PENDING.get(i);
                if(apply(player, pending.packet)) {
                    PENDING.remove(i);
                } else if(--pending.ticksLeft <= 0) {
                    MCH_Lib.DbgLog(true, "[MCH-MOUNT-SYNC] mount-graph-timeout vehicle=%d sequence=%d", pending.packet.vehicleId, pending.packet.sequence);
                    PENDING.remove(i);
                } else if(!pending.waitLogged) {
                    pending.waitLogged = true;
                    MCH_Lib.DbgLog(true, "[MCH-MOUNT-SYNC] mount-graph-wait vehicle=%d sequence=%d", pending.packet.vehicleId, pending.packet.sequence);
                }
            }
        }
    }

    public static void clearClientQueue() {
        synchronized(PENDING) {
            PENDING.clear();
            clientWorldIdentity = 0;
            clientPlayerIdentity = 0;
        }
    }

    private static boolean apply(EntityPlayer observer, PacketVehicleMountGraph packet) {
        if(observer.dimension != packet.dimension || packet.commonUniqueId.length() == 0) return false;
        Entity found = observer.worldObj.getEntityByID(packet.vehicleId);
        if(!(found instanceof MCH_EntityBaseVehicle) || found.isDead || found.worldObj != observer.worldObj) return false;
        MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)found;
        if(!packet.commonUniqueId.equals(vehicle.getCommonUniqueId()) || vehicle.getAcInfo() == null
            || !packet.aircraftType.equals(vehicle.getAcInfo().name)) return false;
        for(RiderEntry entry : packet.riders) {
            Entity rider = observer.worldObj.getEntityByID(entry.riderId);
            Entity mount = entry.seatIndex == 0 ? vehicle : observer.worldObj.getEntityByID(entry.seatEntityId);
            if(rider == null || rider.isDead || rider.worldObj != observer.worldObj || mount == null || mount.isDead
                || mount.worldObj != observer.worldObj) return false;
            if(rider instanceof EntityPlayer && entry.playerProfileId != null
                && !entry.playerProfileId.equals(((EntityPlayer)rider).getGameProfile().getId())) return false;
            if(!(rider instanceof EntityPlayer) && !rider.getClass().getName().equals(entry.riderClass)) return false;
            if(entry.seatIndex > 0) {
                if(!(mount instanceof MCH_EntitySeat)) return false;
                MCH_EntitySeat seat = (MCH_EntitySeat)mount;
                if(seat.seatID + 1 != entry.seatIndex || !packet.commonUniqueId.equals(seat.parentUniqueID)
                    || seat.getParent() != vehicle) return false;
            }
        }
        for(RiderEntry entry : packet.riders) {
            Entity rider = observer.worldObj.getEntityByID(entry.riderId);
            Entity mount = entry.seatIndex == 0 ? vehicle : observer.worldObj.getEntityByID(entry.seatEntityId);
            if(rider.ridingEntity != mount) rider.mountEntity(mount);
            if(rider.ridingEntity != mount || mount.riddenByEntity != rider) return false;
        }
        MCH_Lib.DbgLog(true, "[MCH-MOUNT-SYNC] mount-graph-applied vehicle=%d sequence=%d riders=%d", packet.vehicleId, packet.sequence, packet.riders.size());
        return true;
    }

    private static void resetForWorld(EntityPlayer player) {
        int world = System.identityHashCode(player.worldObj);
        int localPlayer = System.identityHashCode(player);
        if(world != clientWorldIdentity || localPlayer != clientPlayerIdentity) {
            PENDING.clear();
            clientWorldIdentity = world;
            clientPlayerIdentity = localPlayer;
        }
    }

    private static void writeString(ByteBuf data, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(UTF_8);
        int length = Math.min(bytes.length, MAX_STRING_BYTES);
        data.writeByte(length);
        data.writeBytes(bytes, 0, length);
    }

    private static String readString(ByteBuf data) {
        int length = data.readUnsignedByte();
        byte[] bytes = new byte[length];
        data.readBytes(bytes);
        return new String(bytes, UTF_8);
    }

    public static class RiderEntry {
        public int riderId;
        public int seatEntityId = -1;
        public int seatIndex;
        public String parentCommonUniqueId = "";
        public String riderClass = "";
        public UUID playerProfileId;
    }

    private static final class Pending {
        final PacketVehicleMountGraph packet;
        int ticksLeft = 100;
        boolean waitLogged;
        Pending(PacketVehicleMountGraph packet) { this.packet = packet; }
    }
}
