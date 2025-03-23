package mcheli.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mcheli.network.PacketBase;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Entity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketLockTarget extends PacketBase {
    public int targetID;
    public int entityID;

    public PacketLockTarget(int targetID, int entityID) {
        this.targetID = targetID;
        this.entityID = entityID;
    }

    public PacketLockTarget() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(targetID);
        data.writeInt(entityID);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        targetID = data.readInt();
        entityID = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        MCH_EntityBaseBullet bullet = null;
        Entity target = null;
        for(Object obj : playerEntity.worldObj.loadedEntityList) {
            if(obj instanceof MCH_EntityBaseBullet && ((Entity)obj).getEntityId() == entityID) {
                bullet = (MCH_EntityBaseBullet) obj;
            }
            if(((Entity)obj).getEntityId() == targetID) {
                target = (Entity) obj;
            }
        }
        if (bullet != null) {
            if(!W_Entity.isEqual(bullet.shootingEntity, playerEntity)) {
                return;
            }
            bullet.setTargetEntity(target);
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
