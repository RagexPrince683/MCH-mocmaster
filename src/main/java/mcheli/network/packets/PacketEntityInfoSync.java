package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.MCH_EntityInfoClientTracker;
import mcheli.MCH_EntityInfo;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

public class PacketEntityInfoSync extends PacketBase {

    public static final byte OPERATION_UPDATE = 0;
    public static final byte OPERATION_REMOVE = 1;

    private List<MCH_EntityInfo> entities;
    private byte operation;

    public PacketEntityInfoSync() {}

    public PacketEntityInfoSync(List<MCH_EntityInfo> entities, byte operation) {
        this.entities = entities;
        this.operation = operation;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        buf.writeByte(operation);
        buf.writeInt(entities.size());
        for (MCH_EntityInfo info : entities) {
            // 编码实体信息
            buf.writeInt(info.entityId);
            writeUTF(buf, info.worldName);
            writeUTF(buf, info.entityName);
            writeUTF(buf, info.entityClassName);
            buf.writeDouble(info.posX);
            buf.writeDouble(info.posY);
            buf.writeDouble(info.posZ);
            buf.writeDouble(info.lastTickPosX);
            buf.writeDouble(info.lastTickPosY);
            buf.writeDouble(info.lastTickPosZ);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        operation = buf.readByte();
        int count = buf.readInt();
        entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entities.add(new MCH_EntityInfo(
                buf.readInt(),
                readUTF(buf),
                readUTF(buf),
                readUTF(buf),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
            ));
        }
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    // 客户端处理
    @Override
    public void handleClientSide(EntityPlayer player) {
        switch (operation) {
            case OPERATION_UPDATE:
                MCH_EntityInfoClientTracker.updateEntities(entities);
                break;
            case OPERATION_REMOVE:
                MCH_EntityInfoClientTracker.removeEntities(entities);
                break;
        }
    }

}