package mcheli;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.io.UnsupportedEncodingException;

public class MCH_PacketUpdateUavStationUUID extends W_PacketBase implements IMessage {
    public int stationEntityId;
    public String uuid;

    // Public no-arg constructor required for deserialization.
    public MCH_PacketUpdateUavStationUUID() { }

    public MCH_PacketUpdateUavStationUUID(int stationEntityId, String uuid) {
        this.stationEntityId = stationEntityId;
        this.uuid = uuid;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.stationEntityId);
        writeString(buf, this.uuid);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.stationEntityId = buf.readInt();
        this.uuid = readString(buf);
    }

    private void writeString(ByteBuf buf, String s) {
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (Exception e) {
            bytes = new byte[0];
        }
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private String readString(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}