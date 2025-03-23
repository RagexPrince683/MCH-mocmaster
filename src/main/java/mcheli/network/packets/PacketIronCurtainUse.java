package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketIronCurtainUse extends PacketBase {

    public int acId;
    public int time;


    public PacketIronCurtainUse() {
    }

    public PacketIronCurtainUse(int acId, int time) {
        this.acId = acId;
        this.time = time;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(acId);
        data.writeInt(time);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        acId = data.readInt();
        time = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        Entity e = clientPlayer.worldObj.getEntityByID(acId);
        if(e instanceof MCH_EntityAircraft) {
            ((MCH_EntityAircraft) e).ironCurtainRunningTick = time;
        }
    }
}
