package mcheli.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.network.PacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;

/** A bounded, server-authored inventory of real vehicles expected after respawn. */
public class PacketVehicleRespawnProbe extends PacketBase {
    public static final int MAX_VEHICLES = 128;
    public int generation, playerId, dimension, serverWorldIdentity;
    public UUID playerUuid = new UUID(0L, 0L);
    public List<Entry> vehicles = new ArrayList<Entry>();
    private static int clientGeneration;
    private static final Set<UUID> previouslyResolved = new HashSet<UUID>();
    public static int getClientGeneration() { return clientGeneration; }

    public static class Entry {
        public int entityId, chunkX, chunkZ;
        public UUID uuid;
        public String typeName;
        public double x, y, z;
    }

    @Override
    public void encodeInto(ChannelHandlerContext context, ByteBuf data) {
        data.writeInt(generation); data.writeInt(playerId); writeUuid(data, playerUuid);
        data.writeInt(dimension); data.writeInt(serverWorldIdentity);
        int count = Math.min(vehicles.size(), MAX_VEHICLES);
        data.writeShort(count);
        for(int i = 0; i < count; ++i) {
            Entry entry = vehicles.get(i);
            data.writeInt(entry.entityId); writeUuid(data, entry.uuid); writeUTF(data, entry.typeName);
            data.writeDouble(entry.x); data.writeDouble(entry.y); data.writeDouble(entry.z);
            data.writeInt(entry.chunkX); data.writeInt(entry.chunkZ);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext context, ByteBuf data) {
        generation = data.readInt(); playerId = data.readInt(); playerUuid = readUuid(data);
        dimension = data.readInt(); serverWorldIdentity = data.readInt();
        int count = Math.min(data.readUnsignedShort(), MAX_VEHICLES);
        vehicles = new ArrayList<Entry>(count);
        for(int i = 0; i < count; ++i) {
            Entry entry = new Entry();
            entry.entityId = data.readInt(); entry.uuid = readUuid(data); entry.typeName = readUTF(data);
            entry.x = data.readDouble(); entry.y = data.readDouble(); entry.z = data.readDouble();
            entry.chunkX = data.readInt(); entry.chunkZ = data.readInt(); vehicles.add(entry);
        }
    }

    @Override public void handleServerSide(EntityPlayerMP player) {}

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer ignored) {
        final PacketVehicleRespawnProbe packet = this;
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override public void run() { packet.inspectClientWorld(); }
        });
    }

    @SideOnly(Side.CLIENT)
    private void inspectClientWorld() {
        clientGeneration = generation;
        Minecraft mc = Minecraft.getMinecraft();
        PacketVehicleRespawnProbeResult result = new PacketVehicleRespawnProbeResult();
        result.generation = generation;
        result.clientPlayerId = mc.thePlayer == null ? -1 : mc.thePlayer.getEntityId();
        result.clientPlayerIdentity = System.identityHashCode(mc.thePlayer);
        result.clientWorldIdentity = System.identityHashCode(mc.theWorld);
        result.renderViewIdentity = System.identityHashCode(mc.renderViewEntity);
        result.respawnAge = mcheli.MCH_ClientCommonTickHandler.getRespawnAuditAge();
        result.replacementProcessed = mc.thePlayer != null && mc.theWorld != null
            && mc.thePlayer.getEntityId() == playerId && playerUuid.equals(mc.thePlayer.getUniqueID())
            && mc.theWorld.provider.dimensionId == dimension;
        for(Entry expected : vehicles) result.vehicles.add(inspect(expected, mc));
        MCH_Lib.RespawnAuditLog("client-probe generation=%d ready=%s playerId=%d world=%x vehicles=%d",
            Integer.valueOf(generation), Boolean.valueOf(result.replacementProcessed), Integer.valueOf(result.clientPlayerId),
            Integer.valueOf(result.clientWorldIdentity), Integer.valueOf(result.vehicles.size()));
        MCH_MOD.getPacketHandler().sendToServer(result);
    }

    @SideOnly(Side.CLIENT)
    private static PacketVehicleRespawnProbeResult.Entry inspect(Entry expected, Minecraft mc) {
        PacketVehicleRespawnProbeResult.Entry out = new PacketVehicleRespawnProbeResult.Entry();
        out.expectedId = expected.entityId; out.expectedUuid = expected.uuid;
        Entity byId = mc.theWorld == null ? null : mc.theWorld.getEntityByID(expected.entityId);
        Entity byUuid = null;
        if(mc.theWorld != null) for(Object value : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity)value;
            if(expected.uuid.equals(entity.getUniqueID())) { byUuid = entity; break; }
        }
        out.foundById = byId != null; out.foundByUuid = byUuid != null;
        out.previouslyFound = previouslyResolved.contains(expected.uuid);
        if(byUuid != null) previouslyResolved.add(expected.uuid);
        Entity found = byUuid != null ? byUuid : byId;
        if(found != null) {
            out.foundClass = found.getClass().getName(); out.foundId = found.getEntityId();
            out.foundUuid = found.getUniqueID(); out.objectIdentity = System.identityHashCode(found);
            out.worldIdentity = System.identityHashCode(found.worldObj); out.dead = found.isDead;
            out.addedToChunk = found.addedToChunk; out.collidable = found.canBeCollidedWith();
            AxisAlignedBB box = found.getBoundingBox();
            out.boundingBox = box == null ? "null" : String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f..%.2f,%.2f,%.2f",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
        if(found instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)found;
            out.acInfo = vehicle.getAcInfo() == null ? "null" : vehicle.getAcInfo().name;
            out.renderingLod = vehicle.isRenderingLOD; out.skipNormalRender = vehicle.isSkipNormalRender();
            out.seatCount = vehicle.getSeats().length;
            for(mcheli.aircraft.MCH_EntitySeat seat : vehicle.getSeats()) if(seat != null && seat.getParent() == vehicle) ++out.validSeatParents;
            for(Object entity : mc.theWorld.loadedEntityList) {
                if(entity instanceof mcheli.aircraft.MCH_EntityHitBox && ((mcheli.aircraft.MCH_EntityHitBox)entity).parent == vehicle) {
                    ++out.hitboxCount; ++out.validHitboxParents;
                }
            }
        }
        return out;
    }

    static void writeUuid(ByteBuf data, UUID uuid) { data.writeLong(uuid.getMostSignificantBits()); data.writeLong(uuid.getLeastSignificantBits()); }
    static UUID readUuid(ByteBuf data) { return new UUID(data.readLong(), data.readLong()); }
}
