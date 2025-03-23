package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_WeaponTvMissile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLaserGuidanceTargeting extends PacketBase {

    boolean targeting;

    public PacketLaserGuidanceTargeting(boolean targeting) {
        this.targeting = targeting;
    }

    public PacketLaserGuidanceTargeting() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(targeting);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targeting = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(playerEntity);
        if(ac != null && ac.getCurrentWeapon(playerEntity).getCurrentWeapon() instanceof MCH_WeaponTvMissile) {
            MCH_WeaponTvMissile weaponTvMissile = (MCH_WeaponTvMissile) ac.getCurrentWeapon(playerEntity).getCurrentWeapon();
            if(weaponTvMissile.guidanceSystem != null) {
                weaponTvMissile.guidanceSystem.targeting = targeting;
            }
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
