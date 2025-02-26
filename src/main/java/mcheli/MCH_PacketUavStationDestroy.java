package mcheli;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class MCH_PacketUavStationDestroy extends W_PacketBase implements IMessage {
    private int stationEntityId;

    // Default constructor (needed for packet system)
    public MCH_PacketUavStationDestroy() {}

    public MCH_PacketUavStationDestroy(int stationEntityId) {
        this.stationEntityId = stationEntityId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.stationEntityId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.stationEntityId = buf.readInt();
    }

    public byte[] createData() {
        byte[] data = new byte[4];
        data[0] = (byte) (stationEntityId & 0xFF);
        data[1] = (byte) ((stationEntityId >> 8) & 0xFF);
        data[2] = (byte) ((stationEntityId >> 16) & 0xFF);
        data[3] = (byte) ((stationEntityId >> 24) & 0xFF);
        return data;
    }

    // ❌ Removed incorrect @Override annotation
    public void fromBytes(ByteArrayDataInput data) {
        this.stationEntityId = data.readInt();
    }

    // ✅ SERVER-SIDE HANDLING LOGIC
    public static void handle(MCH_PacketUavStationDestroy pkt) {
        WorldServer world = (WorldServer) MinecraftServer.getServer().worldServerForDimension(0);
        Entity entity = world.getEntityByID(pkt.stationEntityId);

        if (entity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation station = (MCH_EntityUavStation) entity;
            System.out.println("Server: Received destroy request for UAV Station, calling setDead()...");
            station.setDead();
        } else {
            System.out.println("Server: Could not find UAV Station to destroy!");
        }
    }
}
