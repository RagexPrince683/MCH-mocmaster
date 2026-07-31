package mcheli.network.packets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mcheli.MCH_ClientCommonTickHandler;
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
    private static final double POSITION_TOLERANCE_SQ = 32.0D * 32.0D;
    public int generation, playerId, dimension, serverWorldIdentity;
    public UUID playerUuid = new UUID(0L, 0L);
    public List<Entry> vehicles = new ArrayList<Entry>();
    private static int clientGeneration;
    private static final Set<Integer> previouslyResolvedIds = new HashSet<Integer>();

    public static int getClientGeneration() { return clientGeneration; }
    public static void clearClientResolutionState() { clientGeneration = 0; previouslyResolvedIds.clear(); }

    public static class Entry {
        public int entityId, chunkX, chunkZ;
        public UUID uuid;
        public String typeName, commonUniqueId = "";
        public double x, y, z;
    }

    @Override
    public void encodeInto(ChannelHandlerContext context, ByteBuf data) {
        data.writeInt(generation); data.writeInt(playerId); writeUuid(data, playerUuid);
        data.writeInt(dimension); data.writeInt(serverWorldIdentity);
        int count = Math.min(vehicles.size(), MAX_VEHICLES); data.writeShort(count);
        for(int i = 0; i < count; ++i) {
            Entry entry = vehicles.get(i);
            data.writeInt(entry.entityId); writeUuid(data, entry.uuid); writeUTF(data, entry.typeName); writeUTF(data, entry.commonUniqueId);
            data.writeDouble(entry.x); data.writeDouble(entry.y); data.writeDouble(entry.z);
            data.writeInt(entry.chunkX); data.writeInt(entry.chunkZ);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext context, ByteBuf data) {
        generation = data.readInt(); playerId = data.readInt(); playerUuid = readUuid(data);
        dimension = data.readInt(); serverWorldIdentity = data.readInt();
        int count = Math.min(data.readUnsignedShort(), MAX_VEHICLES); vehicles = new ArrayList<Entry>(count);
        for(int i = 0; i < count; ++i) {
            Entry entry = new Entry();
            entry.entityId = data.readInt(); entry.uuid = readUuid(data); entry.typeName = readUTF(data); entry.commonUniqueId = readUTF(data);
            entry.x = data.readDouble(); entry.y = data.readDouble(); entry.z = data.readDouble();
            entry.chunkX = data.readInt(); entry.chunkZ = data.readInt(); vehicles.add(entry);
        }
    }

    @Override public void handleServerSide(EntityPlayerMP player) {}

    @Override @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer ignored) {
        final PacketVehicleRespawnProbe packet = this;
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override public void run() { packet.inspectClientWorld(); }
        });
    }

    @SideOnly(Side.CLIENT)
    private void inspectClientWorld() {
        if(generation != clientGeneration) {
            previouslyResolvedIds.clear();
            clientGeneration = generation;
        }
        Minecraft mc = Minecraft.getMinecraft();
        PacketVehicleRespawnProbeResult result = new PacketVehicleRespawnProbeResult();
        result.generation = generation;
        result.playerPresent = mc.thePlayer != null;
        result.worldPresent = mc.theWorld != null;
        result.playerIdMatches = result.playerPresent && mc.thePlayer.getEntityId() == playerId;
        result.playerUuidMatchesDiagnostic = result.playerPresent && playerUuid.equals(mc.thePlayer.getUniqueID());
        result.dimensionMatches = result.worldPresent && mc.theWorld.provider.dimensionId == dimension;
        result.auditPlayerMatches = result.playerPresent && MCH_ClientCommonTickHandler.isRespawnAuditPlayer(mc.thePlayer);
        result.respawnAge = MCH_ClientCommonTickHandler.getRespawnAuditAge();
        result.respawnAgeReady = result.respawnAge >= 1;
        result.replacementProcessed = result.playerPresent && result.worldPresent && result.playerIdMatches
            && result.dimensionMatches && result.auditPlayerMatches && result.respawnAgeReady;
        result.clientPlayerId = result.playerPresent ? mc.thePlayer.getEntityId() : -1;
        result.clientPlayerIdentity = System.identityHashCode(mc.thePlayer);
        result.clientWorldIdentity = System.identityHashCode(mc.theWorld);
        result.renderViewIdentity = System.identityHashCode(mc.renderViewEntity);
        for(Entry expected : vehicles) result.vehicles.add(inspect(expected, mc, dimension));
        MCH_Lib.RespawnAuditLog("client-readiness generation=%d playerPresent=%s worldPresent=%s playerIdMatches=%s playerUuidDiagnostic=%s dimensionMatches=%s auditPlayerMatches=%s respawnAgeReady=%s ready=%s",
            Integer.valueOf(generation), Boolean.valueOf(result.playerPresent), Boolean.valueOf(result.worldPresent), Boolean.valueOf(result.playerIdMatches),
            Boolean.valueOf(result.playerUuidMatchesDiagnostic), Boolean.valueOf(result.dimensionMatches), Boolean.valueOf(result.auditPlayerMatches),
            Boolean.valueOf(result.respawnAgeReady), Boolean.valueOf(result.replacementProcessed));
        MCH_MOD.getPacketHandler().sendToServer(result);
    }

    @SideOnly(Side.CLIENT)
    private static PacketVehicleRespawnProbeResult.Entry inspect(Entry expected, Minecraft mc, int probeDimension) {
        PacketVehicleRespawnProbeResult.Entry out = new PacketVehicleRespawnProbeResult.Entry();
        out.expectedId = expected.entityId; out.expectedUuid = expected.uuid; out.expectedType = expected.typeName;
        Entity found = mc.theWorld == null ? null : mc.theWorld.getEntityByID(expected.entityId);
        out.foundById = found != null; out.previouslyFound = previouslyResolvedIds.contains(Integer.valueOf(expected.entityId));
        if(found != null) {
            out.foundClass = found.getClass().getName(); out.foundId = found.getEntityId(); out.foundUuid = found.getUniqueID();
            out.uuidMatchesDiagnostic = expected.uuid.equals(out.foundUuid); out.objectIdentity = System.identityHashCode(found);
            out.worldIdentity = System.identityHashCode(found.worldObj); out.currentWorld = found.worldObj == mc.theWorld;
            out.dimensionMatches = found.dimension == probeDimension; out.dead = found.isDead; out.addedToChunk = found.addedToChunk;
            out.collidable = found.canBeCollidedWith();
            double dx = found.posX - expected.x, dy = found.posY - expected.y, dz = found.posZ - expected.z;
            out.positionDeltaSq = dx * dx + dy * dy + dz * dz; out.positionMatches = out.positionDeltaSq <= POSITION_TOLERANCE_SQ;
            AxisAlignedBB box = found.getBoundingBox();
            out.boundingBox = box == null ? "null" : String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f..%.2f,%.2f,%.2f", box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
        if(found instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)found;
            out.baseVehicle = true; out.foundType = vehicle.getAcInfo() == null ? "null" : vehicle.getAcInfo().name;
            out.typeMatches = expected.typeName.equals(out.foundType); out.commonUniqueId = vehicle.getCommonUniqueId() == null ? "" : vehicle.getCommonUniqueId();
            out.commonIdMatches = expected.commonUniqueId.length() == 0 || expected.commonUniqueId.equals(out.commonUniqueId);
            out.renderingLod = vehicle.isRenderingLOD; out.skipNormalRender = vehicle.isSkipNormalRender(); out.seatCount = vehicle.getSeats().length;
            for(mcheli.aircraft.MCH_EntitySeat seat : vehicle.getSeats()) if(seat != null && seat.getParent() == vehicle) ++out.validSeatParents;
            for(Object entity : mc.theWorld.loadedEntityList) if(entity instanceof mcheli.aircraft.MCH_EntityHitBox && ((mcheli.aircraft.MCH_EntityHitBox)entity).parent == vehicle) { ++out.hitboxCount; ++out.validHitboxParents; }
            if(out.foundId == expected.entityId && out.typeMatches && out.dimensionMatches && out.positionMatches && out.currentWorld && out.commonIdMatches) previouslyResolvedIds.add(Integer.valueOf(expected.entityId));
        }
        return out;
    }

    static void writeUuid(ByteBuf data, UUID uuid) { data.writeLong(uuid.getMostSignificantBits()); data.writeLong(uuid.getLeastSignificantBits()); }
    static UUID readUuid(ByteBuf data) { return new UUID(data.readLong(), data.readLong()); }
}
