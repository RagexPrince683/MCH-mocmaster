package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketVehicleLODSnapshot;
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
   private static final double NORMAL_TRACKING_RANGE_SQ = 200.0D * 200.0D;
   private static final int TRACKING_REFRESH_DELAY_TICKS = 1;
   private final Map<EntityPlayerMP, Integer> pendingTrackingRefreshes = new HashMap<EntityPlayerMP, Integer>();
   private int tick;

   @SubscribeEvent
   public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      this.queueTrackingRefresh(event.player, "respawn");
   }

   @SubscribeEvent
   public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      this.queueTrackingRefresh(event.player, "dimension " + event.fromDim + "->" + event.toDim);
   }

   private void queueTrackingRefresh(Entity playerEntity, String reason) {
      if(!(playerEntity instanceof EntityPlayerMP) || playerEntity.worldObj.isRemote) {
         return;
      }
      EntityPlayerMP player = (EntityPlayerMP)playerEntity;
      this.pendingTrackingRefreshes.put(player, Integer.valueOf(TRACKING_REFRESH_DELAY_TICKS));
      MCH_Lib.DbgLog(player.worldObj, "[MCH-RESPAWN-TRACK] queued %s refresh player=%s id=%d uuid=%s object=%x dim=%d world=%x chunk=%d,%d",
         new Object[]{reason, player.getCommandSenderName(), Integer.valueOf(player.getEntityId()), player.getUniqueID(),
            Integer.valueOf(System.identityHashCode(player)), Integer.valueOf(player.dimension),
            Integer.valueOf(System.identityHashCode(player.worldObj)), Integer.valueOf(player.chunkCoordX), Integer.valueOf(player.chunkCoordZ)});
   }

   private void refreshPendingPlayerTracking() {
      Iterator<Map.Entry<EntityPlayerMP, Integer>> iterator = this.pendingTrackingRefreshes.entrySet().iterator();
      while(iterator.hasNext()) {
         Map.Entry<EntityPlayerMP, Integer> entry = iterator.next();
         EntityPlayerMP player = entry.getKey();
         int delay = entry.getValue().intValue() - 1;
         if(delay > 0) {
            entry.setValue(Integer.valueOf(delay));
            continue;
         }
         iterator.remove();
         if(player.isDead || !(player.worldObj instanceof WorldServer)
            || !containsPlayerInstance(player.worldObj.playerEntities, player)) {
            MCH_Lib.DbgLog(player.worldObj, "[MCH-RESPAWN-TRACK] skipped stale refresh player=%s id=%d object=%x dead=%s",
               new Object[]{player.getCommandSenderName(), Integer.valueOf(player.getEntityId()),
                  Integer.valueOf(System.identityHashCode(player)), Boolean.valueOf(player.isDead)});
            continue;
         }

         WorldServer world = (WorldServer)player.worldObj;
         int nearbyVehicles = 0;
         for(Object object : world.loadedEntityList) {
            if(object instanceof MCH_EntityBaseVehicle && !((MCH_EntityBaseVehicle)object).isDead
               && ((MCH_EntityBaseVehicle)object).getDistanceSqToEntity(player) <= NORMAL_TRACKING_RANGE_SQ) {
               ++nearbyVehicles;
            }
         }

         // The replacement player deliberately reuses the dead player's entity ID.
         // EntityTrackerEntry's watcher set can consequently regard a stale old
         // player object as equal to the replacement and suppress its spawn packet.
         // Remove that one player's watcher generation, then let vanilla rebuild it
         // after PlayerManager has installed the replacement's watched chunks.
         world.getEntityTracker().removePlayerFromTrackers(player);
         world.getEntityTracker().updateTrackedEntities();
         MCH_Lib.DbgLog(world, "[MCH-RESPAWN-TRACK] refreshed player=%s id=%d uuid=%s object=%x dim=%d world=%x chunk=%d,%d nearbyVehicles=%d",
            new Object[]{player.getCommandSenderName(), Integer.valueOf(player.getEntityId()), player.getUniqueID(),
               Integer.valueOf(System.identityHashCode(player)), Integer.valueOf(player.dimension),
               Integer.valueOf(System.identityHashCode(world)), Integer.valueOf(player.chunkCoordX),
               Integer.valueOf(player.chunkCoordZ), Integer.valueOf(nearbyVehicles)});
      }
   }

   private static boolean containsPlayerInstance(List<?> players, EntityPlayerMP player) {
      for(Object candidate : players) {
         if(candidate == player) return true;
      }
      return false;
   }

   @SubscribeEvent
   public void onServerTickEvent(ServerTickEvent event) {
      if(event.phase != Phase.END) {
         return;
      }
      this.refreshPendingPlayerTracking();
      if(++this.tick < UPDATE_INTERVAL_TICKS) return;
      this.tick = 0;

      MinecraftServer server = MinecraftServer.getServer();
      if(server == null || server.worldServers == null) {
         return;
      }

      double farDistance = MCH_Config.AircraftLODFarDistance != null
         ? MCH_Config.AircraftLODFarDistance.prmDouble : 4096.0D;
      double farDistanceSq = farDistance > 0.0D ? farDistance * farDistance : Double.MAX_VALUE;

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
            if(!vehicle.isDead && vehicle.getAcInfo() != null && categoryOf(vehicle) >= 0
               && vehicle.getDistanceSqToEntity(player) <= farDistanceSq
               && vehicle.getDistanceSqToEntity(player) > NORMAL_TRACKING_RANGE_SQ) {
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
