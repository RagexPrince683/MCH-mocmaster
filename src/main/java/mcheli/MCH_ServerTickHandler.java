package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.lod.MCH_VehicleLODVisibility;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.network.packets.PacketVehicleMountGraph;
import mcheli.aircraft.MCH_EntitySeat;
import net.minecraft.entity.player.EntityPlayer;
import mcheli.plane.MCP_EntityPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.weapon.MCH_WeaponSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/** Sends render-only vehicle snapshots without changing Forge entity tracking. */
public class MCH_ServerTickHandler {
   private static final int UPDATE_INTERVAL_TICKS = 20;
   private static final int MAX_ENTRIES = 512;
   /** Must match the normal vehicle/seat registration range in MCH_MOD. */
   private static final double NORMAL_TRACKING_RANGE_SQ = MCH_VehicleLODVisibility.NORMAL_TRACKING_RANGE_SQ;
   private int tick;
   private static long nextLodDiagnosticMs;
   private static final Map<String, PendingMountGraph> PENDING_MOUNT_GRAPHS = new HashMap<String, PendingMountGraph>();
   private static final AtomicInteger NEXT_MOUNT_SEQUENCE = new AtomicInteger();

   private static final class PendingMountGraph {
      final EntityPlayerMP observer;
      final MCH_EntityBaseVehicle vehicle;
      final int dimension;
      final String commonUniqueId;
      final int sequence;
      int age;
      int sends;
      PendingMountGraph(EntityPlayerMP observer, MCH_EntityBaseVehicle vehicle) {
         this.observer = observer;
         this.vehicle = vehicle;
         this.dimension = observer.dimension;
         this.commonUniqueId = vehicle.getCommonUniqueId();
         this.sequence = NEXT_MOUNT_SEQUENCE.incrementAndGet();
      }
   }

   public static synchronized void scheduleMountGraph(MCH_EntityBaseVehicle vehicle, EntityPlayerMP observer) {
      if(vehicle == null || observer == null || vehicle.isDead || vehicle.isUAV() || vehicle.isNewUAV()
         || vehicle.getCommonUniqueId() == null || vehicle.getCommonUniqueId().length() == 0) return;
      String key = System.identityHashCode(observer) + ":" + observer.dimension + ":" + vehicle.getEntityId()
         + ":" + vehicle.getCommonUniqueId();
      if(!PENDING_MOUNT_GRAPHS.containsKey(key)) PENDING_MOUNT_GRAPHS.put(key, new PendingMountGraph(observer, vehicle));
   }

   public static synchronized void cancelMountGraph(MCH_EntityBaseVehicle vehicle, EntityPlayerMP observer) {
      Iterator<PendingMountGraph> iterator = PENDING_MOUNT_GRAPHS.values().iterator();
      while(iterator.hasNext()) {
         PendingMountGraph pending = iterator.next();
         if(pending.observer == observer && pending.vehicle == vehicle) iterator.remove();
      }
   }

   @SubscribeEvent
   public void onServerTickEvent(ServerTickEvent event) {
      if(event.phase != Phase.END) return;
      tickMountGraphs();
      if(++this.tick < UPDATE_INTERVAL_TICKS) return;
      this.tick = 0;

      MinecraftServer server = MinecraftServer.getServer();
      if(server == null || server.worldServers == null) {
         return;
      }

      double farDistance = MCH_Config.AircraftLODFarDistance != null
         ? MCH_Config.AircraftLODFarDistance.prmDouble : MCH_VehicleLODVisibility.MAX_LOD_DISTANCE;
      farDistance = MCH_VehicleLODVisibility.hardDistance(farDistance);
      double farDistanceSq = farDistance * farDistance;

      for(WorldServer world : server.worldServers) {
         for(Object playerObject : world.playerEntities) {
            if(!(playerObject instanceof EntityPlayerMP)) {
               continue;
            }
            EntityPlayerMP player = (EntityPlayerMP)playerObject;
            List<PacketVehicleLODSnapshot.Entry> entries = collectSnapshots(world, player, farDistanceSq);
            MCH_MOD.getPacketHandler().sendTo(new PacketVehicleLODSnapshot(world.provider.dimensionId, entries), player);
         }
      }
   }

   private static List<PacketVehicleLODSnapshot.Entry> collectSnapshots(WorldServer world, final EntityPlayerMP player, double farDistanceSq) {
      List<MCH_EntityBaseVehicle> aircraft = new ArrayList<MCH_EntityBaseVehicle>();
      for(Object object : world.loadedEntityList) {
         if(object instanceof MCH_EntityBaseVehicle) {
            MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)object;
            double dx = vehicle.posX - player.posX;
            double dy = vehicle.posY - player.posY;
            double dz = vehicle.posZ - player.posZ;
            double distanceSq = MCH_VehicleLODVisibility.distanceSq(dx, dy, dz);
            boolean watched = world.getPlayerManager().isPlayerWatchingChunk(player,
               vehicle.chunkCoordX, vehicle.chunkCoordZ);
            boolean qualifies = !vehicle.isDead && vehicle.getAcInfo() != null && categoryOf(vehicle) >= 0
               && !vehicle.isUAV() && !vehicle.isNewUAV()
               && MCH_VehicleLODVisibility.shouldSendSnapshot(watched, distanceSq, Math.sqrt(farDistanceSq));
            diagnoseSnapshot(world, player, vehicle, dx, dy, dz, farDistanceSq, qualifies);
            if(qualifies) {
               aircraft.add(vehicle);
            }
         }
      }

      Collections.sort(aircraft, new Comparator<MCH_EntityBaseVehicle>() {
         @Override
         public int compare(MCH_EntityBaseVehicle left, MCH_EntityBaseVehicle right) {
            return Double.compare(left.getDistanceSqToEntity(player), right.getDistanceSqToEntity(player));
         }
      });

      int count = Math.min(aircraft.size(), MAX_ENTRIES);
      List<PacketVehicleLODSnapshot.Entry> entries = new ArrayList<PacketVehicleLODSnapshot.Entry>(count);
      for(int i = 0; i < count; ++i) {
         MCH_EntityBaseVehicle vehicle = aircraft.get(i);
         PacketVehicleLODSnapshot.Entry entry = new PacketVehicleLODSnapshot.Entry();
         entry.uuid = vehicle.getUniqueID();
         entry.entityId = vehicle.getEntityId();
         entry.commonUniqueId = vehicle.getCommonUniqueId();
         entry.dimension = world.provider.dimensionId;
         entry.category = categoryOf(vehicle);
         entry.typeName = vehicle.getAcInfo().name;
         entry.textureName = vehicle.getTextureName();
         entry.x = vehicle.posX;
         entry.y = vehicle.posY;
         entry.z = vehicle.posZ;
         entry.yaw = vehicle.getRotYaw();
         entry.pitch = vehicle.getRotPitch();
         entry.roll = vehicle.getRotRoll();
         entry.scale = 1.0F;
         entry.landingGearRotation = vehicle.getLandingGearRotation();
         entry.prevLandingGearRotation = vehicle.getPrevLandingGearRotation();
         if(entry.category == 3) {
            for(int side = 0; side < 2; ++side) {
               entry.trackRollerRotation[side] = vehicle.rotTrackRoller[side];
               entry.previousTrackRollerRotation[side] = vehicle.prevRotTrackRoller[side];
               entry.crawlerTrackPhase[side] = vehicle.rotCrawlerTrack[side];
               entry.previousCrawlerTrackPhase[side] = vehicle.prevRotCrawlerTrack[side];
            }
            entry.wheelRotation = vehicle.rotWheel;
            entry.previousWheelRotation = vehicle.prevRotWheel;
            entry.wheelYaw = vehicle.rotYawWheel;
            entry.previousWheelYaw = vehicle.prevRotYawWheel;
         }
         entry.weaponPoses = collectWeaponPoses(vehicle);
         if(vehicle instanceof MCH_EntityHeli) {
            MCH_EntityHeli heli = (MCH_EntityHeli)vehicle;
            entry.rotorRotation = (float)heli.rotationRotor;
            entry.prevRotorRotation = (float)heli.prevRotationRotor;
            entry.rotorAngularChange = entry.rotorRotation - entry.prevRotorRotation;
            if(entry.rotorAngularChange < -180.0F) entry.rotorAngularChange += 360.0F;
            if(entry.rotorAngularChange > 180.0F) entry.rotorAngularChange -= 360.0F;
            entry.rotorFolded = heli.isFoldBlades();
         }
         if(vehicle instanceof MCP_EntityPlane) {
            MCP_EntityPlane plane = (MCP_EntityPlane)vehicle;
            capturePlaneLikeState(entry, plane.getNozzleRotation(), plane.getPrevNozzleRotation(),
               plane.getWingRotation(), plane.getPrevWingRotation(), plane.rotationRotor, plane.prevRotationRotor);
         } else if(vehicle instanceof MCH_EntityShip) {
            MCH_EntityShip ship = (MCH_EntityShip)vehicle;
            capturePlaneLikeState(entry, ship.getNozzleRotation(), ship.getPrevNozzleRotation(),
               ship.getWingRotation(), ship.getPrevWingRotation(), ship.rotationRotor, ship.prevRotationRotor);
         } else if(vehicle instanceof MCH_EntityTurret) {
            captureTurretState(entry, (MCH_EntityTurret)vehicle);
         }
         entry.packedLight = getPackedLight(world, vehicle);
         entries.add(entry);
      }
      return entries;
   }

   private static void diagnoseSnapshot(WorldServer world, EntityPlayerMP player, MCH_EntityBaseVehicle vehicle,
      double dx, double dy, double dz, double farDistanceSq, boolean qualifies) {
      long now = System.currentTimeMillis();
      if(MCH_Config.DebugVehicleLODVisibility == null || !MCH_Config.DebugVehicleLODVisibility.prmBool
         || now < nextLodDiagnosticMs) return;
      nextLodDiagnosticMs = now + 1000L;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      double vertical = Math.abs(dy);
      double distance = Math.sqrt(MCH_VehicleLODVisibility.distanceSq(dx, dy, dz));
      double hardRange = Math.sqrt(farDistanceSq);
      boolean watched = world.getPlayerManager().isPlayerWatchingChunk(player, vehicle.chunkCoordX, vehicle.chunkCoordZ);
      String reason = qualifies ? "snapshot" : distance <= MCH_VehicleLODVisibility.NORMAL_TRACKING_RANGE
         ? "normal_tracking" : distance >= hardRange ? "hard_range" : "excluded_vehicle";
      MCH_Lib.DbgLog(true,
         "VehicleLODSnapshot horizontal=%.1f vertical=%.1f distance3d=%.1f normalRange=%.1f hardRange=%.1f watchedChunk=%s qualifies=%s result=%s",
         new Object[]{Double.valueOf(horizontal), Double.valueOf(vertical), Double.valueOf(distance),
            Double.valueOf(MCH_VehicleLODVisibility.NORMAL_TRACKING_RANGE), Double.valueOf(hardRange),
            Boolean.valueOf(watched), Boolean.valueOf(qualifies), reason});
   }

   private static synchronized void tickMountGraphs() {
      Iterator<PendingMountGraph> iterator = PENDING_MOUNT_GRAPHS.values().iterator();
      while(iterator.hasNext()) {
         PendingMountGraph pending = iterator.next();
         ++pending.age;
         EntityPlayerMP observer = pending.observer;
         MCH_EntityBaseVehicle vehicle = pending.vehicle;
         if(observer.isDead || observer.playerNetServerHandler == null || observer.dimension != pending.dimension
            || observer.worldObj != vehicle.worldObj || vehicle.isDead
            || !pending.commonUniqueId.equals(vehicle.getCommonUniqueId())
            || !(observer.worldObj instanceof WorldServer)
            || !((WorldServer)observer.worldObj).getPlayerManager().isPlayerWatchingChunk(observer, vehicle.chunkCoordX, vehicle.chunkCoordZ)
            || vehicle.getDistanceSqToEntity(observer) > NORMAL_TRACKING_RANGE_SQ) {
            iterator.remove();
            continue;
         }
         if(pending.age == 1 || pending.age == 5 || pending.age == 10) {
            MCH_MOD.getPacketHandler().sendTo(createMountGraph(pending), observer);
            ++pending.sends;
         }
         if(pending.age >= 10 || pending.sends >= 3) iterator.remove();
      }
   }

   private static PacketVehicleMountGraph createMountGraph(PendingMountGraph pending) {
      MCH_EntityBaseVehicle vehicle = pending.vehicle;
      PacketVehicleMountGraph packet = new PacketVehicleMountGraph();
      packet.observerId = pending.observer.getEntityId();
      packet.dimension = pending.dimension;
      packet.vehicleId = vehicle.getEntityId();
      packet.commonUniqueId = pending.commonUniqueId;
      packet.aircraftType = vehicle.getAcInfo() == null ? "" : vehicle.getAcInfo().name;
      packet.sequence = pending.sequence;
      List<PacketVehicleMountGraph.RiderEntry> riders = new ArrayList<PacketVehicleMountGraph.RiderEntry>();
      addRider(riders, vehicle.getRiddenByEntity(), vehicle, 0);
      int count = Math.min(vehicle.getSeatNum(), 63);
      for(int i = 0; i < count; ++i) {
         MCH_EntitySeat seat = vehicle.getSeat(i);
         if(seat != null) addRider(riders, seat.riddenByEntity, seat, i + 1);
      }
      packet.riders = riders;
      return packet;
   }

   private static void addRider(List<PacketVehicleMountGraph.RiderEntry> riders, Entity rider, Entity mount, int seatIndex) {
      if(rider == null || rider.isDead) return;
      PacketVehicleMountGraph.RiderEntry entry = new PacketVehicleMountGraph.RiderEntry();
      entry.riderId = rider.getEntityId();
      entry.seatEntityId = seatIndex == 0 ? -1 : mount.getEntityId();
      entry.seatIndex = seatIndex;
      entry.riderClass = rider.getClass().getName();
      if(rider instanceof EntityPlayer) entry.playerProfileId = ((EntityPlayer)rider).getGameProfile().getId();
      if(mount instanceof MCH_EntitySeat) entry.parentCommonUniqueId = ((MCH_EntitySeat)mount).parentUniqueID;
      riders.add(entry);
   }

   private static void capturePlaneLikeState(PacketVehicleLODSnapshot.Entry entry, float nozzle, float prevNozzle,
      float wing, float prevWing, float rotor, float prevRotor) {
      entry.nozzleRotation = nozzle;
      entry.prevNozzleRotation = prevNozzle;
      entry.wingRotation = wing;
      entry.prevWingRotation = prevWing;
      entry.rotorRotation = rotor;
      entry.prevRotorRotation = prevRotor;
      entry.rotorAngularChange = rotor - prevRotor;
      if(entry.rotorAngularChange < -180.0F) entry.rotorAngularChange += 360.0F;
      if(entry.rotorAngularChange > 180.0F) entry.rotorAngularChange -= 360.0F;
   }

   private static void captureTurretState(PacketVehicleLODSnapshot.Entry entry, MCH_EntityTurret turret) {
      MCH_TurretInfo info = turret.getTurretInfo();
      MCH_WeaponSet ws = turret.getFirstSeatWeapon();
      entry.aimYaw = turret.getLastRiderYaw();
      entry.prevAimYaw = turret.prevLastRiderYaw;
      entry.aimPitch = turret.getLastRiderPitch();
      entry.prevAimPitch = turret.prevLastRiderPitch;
      if(info == null || ws == null) return;
      entry.turretBarrelRotation = ws.rotBarrel;
      entry.prevTurretBarrelRotation = ws.prevRotBarrel;
      List<PacketVehicleLODSnapshot.TurretPartPose> poses = new ArrayList<PacketVehicleLODSnapshot.TurretPartPose>();
      int index = 0;
      for(Object object : info.partList) index = captureTurretPart((MCH_TurretInfo.VPart)object, turret, ws, index, poses);
      entry.turretParts = poses.toArray(new PacketVehicleLODSnapshot.TurretPartPose[poses.size()]);
   }

   private static int captureTurretPart(MCH_TurretInfo.VPart part, MCH_EntityTurret turret, MCH_WeaponSet ws,
      int index, List<PacketVehicleLODSnapshot.TurretPartPose> poses) {
      if(poses.size() >= PacketVehicleLODSnapshot.MAX_TURRET_PARTS) return index;
      PacketVehicleLODSnapshot.TurretPartPose pose = new PacketVehicleLODSnapshot.TurretPartPose();
      poses.add(pose);
      if(index < ws.getWeaponNum()) {
         pose.recoil = ws.recoilBuf[index].recoilBuf;
         pose.prevRecoil = ws.recoilBuf[index].prevRecoilBuf;
      }
      if(part.type == 2 || part.type == 3) ++index;
      if(part.child != null) for(Object child : part.child) index = captureTurretPart((MCH_TurretInfo.VPart)child, turret, ws, index, poses);
      pose.visible = part.type != 3 || !turret.isWeaponNotCooldown(ws, index);
      return index;
   }

   private static PacketVehicleLODSnapshot.WeaponPose[] collectWeaponPoses(MCH_EntityBaseVehicle vehicle) {
      MCH_BaseVehicleInfo info = vehicle.getAcInfo();
      int count = Math.min(info.partWeapon.size(), PacketVehicleLODSnapshot.MAX_WEAPON_POSES);
      PacketVehicleLODSnapshot.WeaponPose[] poses = new PacketVehicleLODSnapshot.WeaponPose[count];
      Entity rider = vehicle.getRiddenByEntity();
      MCH_WeaponSet before = null;
      int weaponIndex = 0;
      for(int i = 0; i < count; ++i) {
         MCH_BaseVehicleInfo.PartWeapon part = (MCH_BaseVehicleInfo.PartWeapon)info.partWeapon.get(i);
         MCH_WeaponSet ws = vehicle.getWeaponByName(part.name[0]);
         if(ws != before) {
            weaponIndex = 0;
            before = ws;
         }
         PacketVehicleLODSnapshot.WeaponPose pose = new PacketVehicleLODSnapshot.WeaponPose();
         pose.turretYaw = vehicle.getLastRiderYaw() - vehicle.getRotYaw();
         pose.prevTurretYaw = vehicle.prevLastRiderYaw - vehicle.prevRotationYaw;
         if(ws != null) {
            pose.yaw = ws.rotationYaw - ws.defaultRotationYaw;
            pose.prevYaw = ws.prevRotationYaw - ws.defaultRotationYaw;
            pose.pitch = ws.rotationPitch;
            pose.prevPitch = ws.prevRotationPitch;
            pose.rotationTurretYaw = ws.rotationTurretYaw;
            pose.defaultRotationYaw = ws.defaultRotationYaw;
            pose.barrelRotation = ws.rotBarrel;
            pose.prevBarrelRotation = ws.prevRotBarrel;
            MCH_WeaponSet.Recoil recoil = ws.recoilBuf[0];
            for(int n = 0; n < part.name.length; ++n) {
               MCH_WeaponSet candidate = vehicle.getWeaponByName(part.name[n]);
               if(candidate != null && candidate.recoilBuf[0].recoilBuf > recoil.recoilBuf) recoil = candidate.recoilBuf[0];
            }
            pose.recoil = recoil.recoilBuf;
            pose.prevRecoil = recoil.prevRecoilBuf;
         } else if(rider != null) {
            pose.yaw = rider.rotationYaw - vehicle.getRotYaw();
            pose.prevYaw = rider.prevRotationYaw - vehicle.prevRotationYaw;
            pose.pitch = rider.rotationPitch;
            pose.prevPitch = rider.prevRotationPitch;
         } else {
            pose.yaw = pose.turretYaw;
            pose.prevYaw = pose.prevTurretYaw;
            pose.pitch = vehicle.getLastRiderPitch();
            pose.prevPitch = vehicle.prevLastRiderPitch;
         }
         pose.visible = !part.isMissile || !vehicle.isWeaponNotCooldown(ws, weaponIndex);
         poses[i] = pose;
         ++weaponIndex;
      }
      return poses;
   }

   /**
    * Samples the vehicle's actual world light without client-only combined
    * brightness helpers. Dedicated servers strip those helpers, and the
    * aircraft override's gradual sky-light smoothing is inappropriate for the
    * once-per-second LOD snapshots.
    */
   private static int getPackedLight(WorldServer world, MCH_EntityBaseVehicle vehicle) {
      if(vehicle.haveSearchLight() && vehicle.isSearchLightON()) {
         return 15728880;
      }

      int x = MathHelper.floor_double(vehicle.posX);
      int z = MathHelper.floor_double(vehicle.posZ);
      if(!world.blockExists(x, 0, z)) {
         return 0;
      }

      double sampleOffset = (vehicle.boundingBox.maxY - vehicle.boundingBox.minY) * 0.66D;
      float flotationOffset = vehicle.getAcInfo() != null
         ? vehicle.getAcInfo().submergedDamageHeight : 0.0F;
      if(vehicle.canFloatWater()) {
         flotationOffset = Math.abs(vehicle.getAcInfo().floatOffset) + 1.0F;
      }

      int y = MathHelper.clamp_int(MathHelper.floor_double(vehicle.posY + (double)flotationOffset
         - (double)vehicle.yOffset + sampleOffset), 0, 255);
      Chunk chunk = world.getChunkFromBlockCoords(x, z);
      int skyLight = chunk.getSavedLightValue(EnumSkyBlock.Sky, x & 15, y, z & 15);
      int blockLight = chunk.getSavedLightValue(EnumSkyBlock.Block, x & 15, y, z & 15);
      return skyLight << 20 | blockLight << 4;
   }

   private static byte categoryOf(MCH_EntityBaseVehicle vehicle) {
      if(vehicle instanceof MCH_EntityHeli) return 0;
      if(vehicle instanceof MCP_EntityPlane) return 1;
      if(vehicle instanceof MCH_EntityShip) return 2;
      if(vehicle instanceof MCH_EntityTank) return 3;
      if(vehicle instanceof MCH_EntityTurret) return 4;
      return -1;
   }
}
