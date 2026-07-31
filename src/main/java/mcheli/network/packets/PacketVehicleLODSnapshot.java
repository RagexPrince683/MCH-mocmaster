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
    public static final int MAX_WEAPON_POSES = 64;
    public static final int MAX_TURRET_PARTS = 128;

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
            writeString(data, entry.commonUniqueId);
            data.writeInt(entry.dimension);
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
            for (int side = 0; side < 2; ++side) data.writeFloat(entry.trackRollerRotation[side]);
            for (int side = 0; side < 2; ++side) data.writeFloat(entry.previousTrackRollerRotation[side]);
            for (int side = 0; side < 2; ++side) data.writeFloat(entry.crawlerTrackPhase[side]);
            for (int side = 0; side < 2; ++side) data.writeFloat(entry.previousCrawlerTrackPhase[side]);
            data.writeFloat(entry.wheelRotation);
            data.writeFloat(entry.previousWheelRotation);
            data.writeFloat(entry.wheelYaw);
            data.writeFloat(entry.previousWheelYaw);
            int weaponCount = entry.weaponPoses == null ? 0 : Math.min(entry.weaponPoses.length, MAX_WEAPON_POSES);
            data.writeByte(weaponCount);
            for (int weaponIndex = 0; weaponIndex < weaponCount; ++weaponIndex) {
                WeaponPose pose = entry.weaponPoses[weaponIndex];
                data.writeFloat(pose.yaw);
                data.writeFloat(pose.prevYaw);
                data.writeFloat(pose.pitch);
                data.writeFloat(pose.prevPitch);
                data.writeFloat(pose.turretYaw);
                data.writeFloat(pose.prevTurretYaw);
                data.writeFloat(pose.rotationTurretYaw);
                data.writeFloat(pose.defaultRotationYaw);
                data.writeFloat(pose.barrelRotation);
                data.writeFloat(pose.prevBarrelRotation);
                data.writeFloat(pose.recoil);
                data.writeFloat(pose.prevRecoil);
                data.writeBoolean(pose.visible);
            }
            data.writeFloat(entry.rotorRotation);
            data.writeFloat(entry.prevRotorRotation);
            data.writeFloat(entry.rotorAngularChange);
            data.writeBoolean(entry.rotorFolded);
            data.writeFloat(entry.landingGearRotation);
            data.writeFloat(entry.prevLandingGearRotation);
            data.writeFloat(entry.nozzleRotation);
            data.writeFloat(entry.prevNozzleRotation);
            data.writeFloat(entry.wingRotation);
            data.writeFloat(entry.prevWingRotation);
            data.writeFloat(entry.aimYaw);
            data.writeFloat(entry.prevAimYaw);
            data.writeFloat(entry.aimPitch);
            data.writeFloat(entry.prevAimPitch);
            data.writeFloat(entry.turretBarrelRotation);
            data.writeFloat(entry.prevTurretBarrelRotation);
            int turretCount = entry.turretParts == null ? 0 : Math.min(entry.turretParts.length, MAX_TURRET_PARTS);
            data.writeByte(turretCount);
            for (int partIndex = 0; partIndex < turretCount; ++partIndex) {
                data.writeFloat(entry.turretParts[partIndex].recoil);
                data.writeFloat(entry.turretParts[partIndex].prevRecoil);
                data.writeBoolean(entry.turretParts[partIndex].visible);
            }
            data.writeInt(entry.packedLight);
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.dimension = data.readInt();
        int count = data.readUnsignedShort();
        if (count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Vehicle LOD snapshot entry count exceeds " + MAX_ENTRIES);
        }
        List<Entry> decoded = new ArrayList<Entry>(count);
        for (int i = 0; i < count; ++i) {
            Entry entry = new Entry();
            entry.uuid = new UUID(data.readLong(), data.readLong());
            entry.entityId = data.readInt();
            entry.commonUniqueId = readString(data);
            entry.dimension = data.readInt();
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
            for (int side = 0; side < 2; ++side) entry.trackRollerRotation[side] = data.readFloat();
            for (int side = 0; side < 2; ++side) entry.previousTrackRollerRotation[side] = data.readFloat();
            for (int side = 0; side < 2; ++side) entry.crawlerTrackPhase[side] = data.readFloat();
            for (int side = 0; side < 2; ++side) entry.previousCrawlerTrackPhase[side] = data.readFloat();
            entry.wheelRotation = data.readFloat();
            entry.previousWheelRotation = data.readFloat();
            entry.wheelYaw = data.readFloat();
            entry.previousWheelYaw = data.readFloat();
            int weaponCount = data.readUnsignedByte();
            if (weaponCount > MAX_WEAPON_POSES) {
                throw new IllegalArgumentException("Vehicle LOD weapon pose count exceeds " + MAX_WEAPON_POSES);
            }
            entry.weaponPoses = new WeaponPose[weaponCount];
            for (int weaponIndex = 0; weaponIndex < weaponCount; ++weaponIndex) {
                WeaponPose pose = new WeaponPose();
                pose.yaw = data.readFloat();
                pose.prevYaw = data.readFloat();
                pose.pitch = data.readFloat();
                pose.prevPitch = data.readFloat();
                pose.turretYaw = data.readFloat();
                pose.prevTurretYaw = data.readFloat();
                pose.rotationTurretYaw = data.readFloat();
                pose.defaultRotationYaw = data.readFloat();
                pose.barrelRotation = data.readFloat();
                pose.prevBarrelRotation = data.readFloat();
                pose.recoil = data.readFloat();
                pose.prevRecoil = data.readFloat();
                pose.visible = data.readBoolean();
                entry.weaponPoses[weaponIndex] = pose;
            }
            entry.rotorRotation = data.readFloat();
            entry.prevRotorRotation = data.readFloat();
            entry.rotorAngularChange = data.readFloat();
            entry.rotorFolded = data.readBoolean();
            entry.landingGearRotation = data.readFloat();
            entry.prevLandingGearRotation = data.readFloat();
            entry.nozzleRotation = data.readFloat();
            entry.prevNozzleRotation = data.readFloat();
            entry.wingRotation = data.readFloat();
            entry.prevWingRotation = data.readFloat();
            entry.aimYaw = data.readFloat();
            entry.prevAimYaw = data.readFloat();
            entry.aimPitch = data.readFloat();
            entry.prevAimPitch = data.readFloat();
            entry.turretBarrelRotation = data.readFloat();
            entry.prevTurretBarrelRotation = data.readFloat();
            int turretCount = data.readUnsignedByte();
            if (turretCount > MAX_TURRET_PARTS) throw new IllegalArgumentException("Vehicle LOD turret part count exceeds " + MAX_TURRET_PARTS);
            entry.turretParts = new TurretPartPose[turretCount];
            for (int partIndex = 0; partIndex < turretCount; ++partIndex) {
                TurretPartPose pose = new TurretPartPose();
                pose.recoil = data.readFloat();
                pose.prevRecoil = data.readFloat();
                pose.visible = data.readBoolean();
                entry.turretParts[partIndex] = pose;
            }
            entry.packedLight = data.readInt();
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
        public String commonUniqueId = "";
        public int dimension;
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
        public final float[] trackRollerRotation = new float[2];
        public final float[] previousTrackRollerRotation = new float[2];
        public final float[] crawlerTrackPhase = new float[2];
        public final float[] previousCrawlerTrackPhase = new float[2];
        public float wheelRotation;
        public float previousWheelRotation;
        public float wheelYaw;
        public float previousWheelYaw;
        public WeaponPose[] weaponPoses = new WeaponPose[0];
        public float rotorRotation;
        public float prevRotorRotation;
        public float rotorAngularChange;
        public boolean rotorFolded;
        public float landingGearRotation, prevLandingGearRotation;
        public float nozzleRotation, prevNozzleRotation;
        public float wingRotation, prevWingRotation;
        public float aimYaw, prevAimYaw, aimPitch, prevAimPitch;
        public float turretBarrelRotation, prevTurretBarrelRotation;
        public TurretPartPose[] turretParts = new TurretPartPose[0];
        public int packedLight;
    }

    /** Render-only pose at the matching index in MCH_BaseVehicleInfo.partWeapon. */
    public static class WeaponPose {
        public float yaw;
        public float prevYaw;
        public float pitch;
        public float prevPitch;
        public float turretYaw;
        public float prevTurretYaw;
        public float rotationTurretYaw;
        public float defaultRotationYaw;
        public float barrelRotation;
        public float prevBarrelRotation;
        public float recoil;
        public float prevRecoil;
        public boolean visible = true;
    }

    public static class TurretPartPose {
        public float recoil;
        public float prevRecoil;
        public boolean visible = true;
    }
}
