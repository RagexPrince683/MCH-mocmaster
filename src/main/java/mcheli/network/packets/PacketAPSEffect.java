package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketAPSEffect extends PacketBase {
    private double x, y, z;
    public PacketAPSEffect() {}
    public PacketAPSEffect(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) { data.writeDouble(x); data.writeDouble(y); data.writeDouble(z); }
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) { x = data.readDouble(); y = data.readDouble(); z = data.readDouble(); }
    public void handleServerSide(EntityPlayerMP player) {}
    public void handleClientSide(EntityPlayer player) {
        for (int i = 0; i < 10; ++i) player.worldObj.spawnParticle("smoke", x, y, z,
                (player.worldObj.rand.nextDouble() - 0.5D) * 0.15D,
                (player.worldObj.rand.nextDouble() - 0.5D) * 0.15D,
                (player.worldObj.rand.nextDouble() - 0.5D) * 0.15D);
    }
}
