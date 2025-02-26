package mcheli;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class MCH_PacketUpdateUavStationUUID extends W_PacketBase {
    private int stationEntityId;
    private String uuid;

    // Default constructor required for reflection
    public MCH_PacketUpdateUavStationUUID() {}

    public MCH_PacketUpdateUavStationUUID(int stationEntityId, String uuid) {
        this.stationEntityId = stationEntityId;
        this.uuid = uuid;
    }


    public void writeData(ByteBuf buffer) {
        buffer.writeInt(this.stationEntityId);
        ByteBufUtils.writeUTF8String(buffer, this.uuid);
    }


    public void readData(ByteBuf buffer) {
        this.stationEntityId = buffer.readInt();
        this.uuid = ByteBufUtils.readUTF8String(buffer);
    }


    public void execute() {
        // This packet should be handled on the server side.
        WorldServer world = (WorldServer) MinecraftServer.getServer().worldServerForDimension(0); // adjust dimension as needed
        Entity entity = world.getEntityByID(this.stationEntityId);
        if (entity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation station = (MCH_EntityUavStation) entity;
            station.newUavPlayerUUID = this.uuid;
            System.out.println("Server: Updated UAV Station newUavPlayerUUID to " + this.uuid);
        }
    }
}
