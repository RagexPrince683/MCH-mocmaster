package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.flare.MCH_APS;
import mcheli.network.PacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketAPSState extends PacketBase {
    private int vehicleId, state, ammo, arming, reload;
    private boolean armed;
    public PacketAPSState() {}
    public PacketAPSState(MCH_EntityBaseVehicle vehicle, MCH_APS aps) {
        vehicleId = vehicle.getEntityId(); state = aps.getState().ordinal(); armed = aps.isArmed();
        ammo = aps.getAmmoRemaining(); arming = aps.getArmingTimer(); reload = aps.getReloadTimer();
    }
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(vehicleId); data.writeByte(state); data.writeBoolean(armed);
        data.writeInt(ammo); data.writeInt(arming); data.writeInt(reload);
    }
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        vehicleId = data.readInt(); state = data.readByte(); armed = data.readBoolean();
        ammo = data.readInt(); arming = data.readInt(); reload = data.readInt();
    }
    public void handleServerSide(EntityPlayerMP player) {}
    public void handleClientSide(EntityPlayer player) {
        Entity entity = player.worldObj.getEntityByID(vehicleId);
        if (entity instanceof MCH_EntityBaseVehicle) ((MCH_EntityBaseVehicle)entity).aps.applyClientState(state, armed, ammo, arming, reload);
    }
}
