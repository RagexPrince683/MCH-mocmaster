package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import mcheli.MCH_MOD;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * A lightweight, read-only description of distant vehicles.  These snapshots are
 * deliberately independent of Forge's entity tracker and never spawn an entity.
 */
public class PacketVehicleLODSnapshot extends PacketBase {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int MAX_ENTRIES = 512;
    private static final int MAX_STRING_BYTES = 128;

    public int dimension;
    public List<Entry> entries = Collections.emptyList();

    public PacketVehicleLODSnapshot() {
    }

    public PacketVehicleLODSnapshot(int dimension, List<Entry> entries) {
        this.dimension = dimension;
        this.entries = entries;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.dimension);
        int count = Math.min(this.entries.size(), MAX_ENTRIES);
        data.writeShort(count);
        for (int i = 0; i < count; ++i) {
            Entry entry = this.entries.get(i);
            data.writeLong(entry.uuid.getMostSignificantBits());
            data.writeLong(entry.uuid.getLeastSignificantBits());
            data.writeInt(entry.entityId);
            data.writeByte(entry.category);
            writeString(data, entry.typeName);
            writeString(data, entry.textureName);
            data.writeDouble(entry.x);
            data.writeDouble(entry.y);
            data.writeDouble(entry.z);
            data.writeFloat(entry.yaw);
            data.writeFloat(entry.pitch);
            data.writeFloat(entry.roll);
            data.writeFloat(entry.scale);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.dimension = data.readInt();
        int count = Math.min(data.readUnsignedShort(), MAX_ENTRIES);
        List<Entry> decoded = new ArrayList<Entry>(count);
        for (int i = 0; i < count; ++i) {
            Entry entry = new Entry();
            entry.uuid = new UUID(data.readLong(), data.readLong());
            entry.entityId = data.readInt();
            entry.category = data.readByte();
            entry.typeName = readString(data);
            entry.textureName = readString(data);
            entry.x = data.readDouble();
            entry.y = data.readDouble();
            entry.z = data.readDouble();
            entry.yaw = data.readFloat();
            entry.pitch = data.readFloat();
            entry.roll = data.readFloat();
            entry.scale = data.readFloat();
            decoded.add(entry);
        }
        this.entries = decoded;
    }

    @Override
    public void handleClientSide(EntityPlayer player) {
        MCH_MOD.proxy.updateVehicleLODSnapshots(this.dimension, this.entries);
    }

    @Override
    public void handleServerSide(EntityPlayerMP player) {
        // Server-to-client only.
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

    public static class Entry {
        public UUID uuid;
        public int entityId;
        public byte category;
        public String typeName;
        public String textureName;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float roll;
        public float scale = 1.0F;
    }
}
