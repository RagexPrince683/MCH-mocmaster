package mcheli.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mcheli.MCH_ServerTickHandler;
import mcheli.network.PacketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/** Client resolution evidence returned to the authoritative server audit. */
public class PacketVehicleRespawnProbeResult extends PacketBase {
    public int generation, clientPlayerId, clientPlayerIdentity, clientWorldIdentity, renderViewIdentity, respawnAge;
    public boolean replacementProcessed;
    public List<Entry> vehicles = new ArrayList<Entry>();
    public static class Entry {
        public int expectedId, foundId = -1, objectIdentity, worldIdentity, seatCount, validSeatParents, hitboxCount, validHitboxParents;
        public UUID expectedUuid, foundUuid;
        public boolean foundById, foundByUuid, previouslyFound, dead, addedToChunk, collidable, renderingLod, skipNormalRender;
        public String foundClass = "null", acInfo = "null", boundingBox = "null";
    }
    @Override public void encodeInto(ChannelHandlerContext c, ByteBuf d) {
        d.writeInt(generation); d.writeInt(clientPlayerId); d.writeInt(clientPlayerIdentity); d.writeInt(clientWorldIdentity);
        d.writeInt(renderViewIdentity); d.writeInt(respawnAge); d.writeBoolean(replacementProcessed);
        int count = Math.min(vehicles.size(), PacketVehicleRespawnProbe.MAX_VEHICLES); d.writeShort(count);
        for(int i=0;i<count;++i) writeEntry(d, vehicles.get(i));
    }
    @Override public void decodeInto(ChannelHandlerContext c, ByteBuf d) {
        generation=d.readInt(); clientPlayerId=d.readInt(); clientPlayerIdentity=d.readInt(); clientWorldIdentity=d.readInt();
        renderViewIdentity=d.readInt(); respawnAge=d.readInt(); replacementProcessed=d.readBoolean();
        int count=Math.min(d.readUnsignedShort(), PacketVehicleRespawnProbe.MAX_VEHICLES); vehicles=new ArrayList<Entry>(count);
        for(int i=0;i<count;++i) vehicles.add(readEntry(d));
    }
    private void writeEntry(ByteBuf d, Entry e) {
        d.writeInt(e.expectedId); PacketVehicleRespawnProbe.writeUuid(d,e.expectedUuid); d.writeBoolean(e.foundById); d.writeBoolean(e.foundByUuid); d.writeBoolean(e.previouslyFound);
        writeUTF(d,e.foundClass); d.writeInt(e.foundId); d.writeBoolean(e.foundUuid!=null); if(e.foundUuid!=null) PacketVehicleRespawnProbe.writeUuid(d,e.foundUuid);
        d.writeInt(e.objectIdentity); d.writeInt(e.worldIdentity); d.writeBoolean(e.dead); d.writeBoolean(e.addedToChunk); writeUTF(d,e.acInfo);
        d.writeBoolean(e.renderingLod); d.writeBoolean(e.skipNormalRender); d.writeBoolean(e.collidable); writeUTF(d,e.boundingBox);
        d.writeInt(e.seatCount); d.writeInt(e.validSeatParents); d.writeInt(e.hitboxCount); d.writeInt(e.validHitboxParents);
    }
    private Entry readEntry(ByteBuf d) {
        Entry e=new Entry(); e.expectedId=d.readInt(); e.expectedUuid=PacketVehicleRespawnProbe.readUuid(d); e.foundById=d.readBoolean(); e.foundByUuid=d.readBoolean(); e.previouslyFound=d.readBoolean();
        e.foundClass=readUTF(d); e.foundId=d.readInt(); if(d.readBoolean()) e.foundUuid=PacketVehicleRespawnProbe.readUuid(d);
        e.objectIdentity=d.readInt(); e.worldIdentity=d.readInt(); e.dead=d.readBoolean(); e.addedToChunk=d.readBoolean(); e.acInfo=readUTF(d);
        e.renderingLod=d.readBoolean(); e.skipNormalRender=d.readBoolean(); e.collidable=d.readBoolean(); e.boundingBox=readUTF(d);
        e.seatCount=d.readInt(); e.validSeatParents=d.readInt(); e.hitboxCount=d.readInt(); e.validHitboxParents=d.readInt(); return e;
    }
    @Override public void handleServerSide(EntityPlayerMP player) {
        MCH_ServerTickHandler.enqueueProbeResult(player, this);
    }
    @Override @SideOnly(Side.CLIENT) public void handleClientSide(EntityPlayer player) {}
}
