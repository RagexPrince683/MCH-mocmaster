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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.network.packets.PacketVehicleRespawnProbe;
import mcheli.network.packets.PacketVehicleRespawnProbeResult;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_EntityHitBox;
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
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.util.IntHashMap;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

/** Sends render-only vehicle snapshots without changing Forge entity tracking. */
public class MCH_ServerTickHandler {
   private static final int UPDATE_INTERVAL_TICKS = 20;
   private static final int MAX_ENTRIES = 512;
   /** Must match the normal vehicle/seat registration range in MCH_MOD. */
   private static final double NORMAL_TRACKING_RANGE_SQ = 200.0D * 200.0D;
   private static final int TRACKING_REFRESH_TIMEOUT_TICKS = 80;
   private static int nextRespawnGeneration;
   private static final Queue<PendingProbeResult> PENDING_PROBE_RESULTS =
      new ConcurrentLinkedQueue<PendingProbeResult>();
   private final Map<String, TrackingRefresh> pendingTrackingRefreshes = new HashMap<String, TrackingRefresh>();
   private int tick;
   private long respawnAuditTick;

   private static final class PendingProbeResult {
      final EntityPlayerMP player;
      final PacketVehicleRespawnProbeResult result;

      PendingProbeResult(EntityPlayerMP player, PacketVehicleRespawnProbeResult result) {
         this.player = player;
         this.result = result;
      }
   }

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
      String key = player.getUniqueID().toString() + ":" + System.identityHashCode(player);
      this.pendingTrackingRefreshes.put(key, new TrackingRefresh(player, reason, ++nextRespawnGeneration));
      MCH_Lib.RespawnAuditLog("queue reason=%s player=%s id=%d uuid=%s object=%x dim=%d world=%x chunk=%d,%d",
         new Object[]{reason, player.getCommandSenderName(), Integer.valueOf(player.getEntityId()), player.getUniqueID(),
            Integer.valueOf(System.identityHashCode(player)), Integer.valueOf(player.dimension),
            Integer.valueOf(System.identityHashCode(player.worldObj)), Integer.valueOf(player.chunkCoordX), Integer.valueOf(player.chunkCoordZ)});
   }

   private void refreshPendingPlayerTracking() {
      Iterator<Map.Entry<String, TrackingRefresh>> iterator = this.pendingTrackingRefreshes.entrySet().iterator();
      while(iterator.hasNext()) {
         TrackingRefresh refresh = iterator.next().getValue();
         EntityPlayerMP player = refresh.player;
         ++refresh.age;
         if(player.isDead || !(player.worldObj instanceof WorldServer)
            || !containsPlayerInstance(player.worldObj.playerEntities, player)) {
            iterator.remove();
            MCH_Lib.RespawnAuditLog("timeout-stale player=%s id=%d object=%x dead=%s age=%d",
               new Object[]{player.getCommandSenderName(), Integer.valueOf(player.getEntityId()),
                  Integer.valueOf(System.identityHashCode(player)), Boolean.valueOf(player.isDead), Integer.valueOf(refresh.age)});
            continue;
         }

         WorldServer world = (WorldServer)player.worldObj;
         int nearbyVehicles = 0, trackedVehicles = 0, readyChunks = 0;
         for(Object object : world.loadedEntityList) {
            if(object instanceof MCH_EntityBaseVehicle && !((MCH_EntityBaseVehicle)object).isDead
               && !((MCH_EntityBaseVehicle)object).isUAV() && !((MCH_EntityBaseVehicle)object).isNewUAV()
               && ((MCH_EntityBaseVehicle)object).getDistanceSqToEntity(player) <= NORMAL_TRACKING_RANGE_SQ) {
               ++nearbyVehicles;
               MCH_EntityBaseVehicle vehicle = (MCH_EntityBaseVehicle)object;
               boolean chunkReady = world.getPlayerManager().isPlayerWatchingChunk(player, vehicle.chunkCoordX, vehicle.chunkCoordZ);
               if(chunkReady) ++readyChunks;
               Set<?> watchers = world.getEntityTracker().getTrackingPlayers(vehicle);
               if(containsPlayerInstance(new ArrayList<Object>(watchers), player)) {
                  ++trackedVehicles;
               }
               if(!refresh.expected.containsKey(Integer.valueOf(vehicle.getEntityId())))
                  refresh.expected.put(Integer.valueOf(vehicle.getEntityId()), new ExpectedVehicle(vehicle, !chunkReady));
            }
         }
         if(refresh.age == 1 || refresh.age == 5 || refresh.age == 20 || refresh.age == 40)
            MCH_Lib.RespawnAuditLog("readiness player=%s age=%d nearby=%d chunkReady=%d tracked=%d",
               player.getCommandSenderName(), Integer.valueOf(refresh.age), Integer.valueOf(nearbyVehicles), Integer.valueOf(readyChunks), Integer.valueOf(trackedVehicles));
         this.executePendingRepairs(refresh, world);
         if(shouldSendProbe(refresh.age) || refresh.probeRequested) {
            refresh.probeRequested = false;
            this.sendProbe(refresh);
         }
         if(refresh.confirmedAll) {
            iterator.remove();
            MCH_Lib.RespawnAuditLog("success-confirmed generation=%d player=%s age=%d vehicles=%d targetedAttempts=%d",
               Integer.valueOf(refresh.generation), player.getCommandSenderName(), Integer.valueOf(refresh.age),
               Integer.valueOf(refresh.expected.size()), Integer.valueOf(refresh.attempts));
         } else if(refresh.age >= TRACKING_REFRESH_TIMEOUT_TICKS) {
            iterator.remove();
            MCH_Lib.RespawnAuditLog("timeout generation=%d player=%s vehicles=%d tracked=%d ready=%d clientConfirmed=%s classification=%s",
               Integer.valueOf(refresh.generation), player.getCommandSenderName(), Integer.valueOf(nearbyVehicles),
               Integer.valueOf(trackedVehicles), Integer.valueOf(readyChunks), Boolean.valueOf(refresh.confirmedAll),
               refresh.resultReceived ? "unconfirmed-after-probes" : "1-client-never-returned-probe");
         }
      }
   }

   private static final class TrackingRefresh {
      final EntityPlayerMP player;
      final String reason;
      int age;
      int attempts;
      final int generation;
      final Map<Integer, ExpectedVehicle> expected = new HashMap<Integer, ExpectedVehicle>();
      boolean confirmedAll, probeRequested, resultReceived, replacementReady;
      TrackingRefresh(EntityPlayerMP player, String reason, int generation) { this.player = player; this.reason = reason; this.generation = generation; }
   }

   private static final class ExpectedVehicle {
      final MCH_EntityBaseVehicle vehicle;
      final boolean premature;
      int resendAttempts;
      boolean clientReportedMissing, clientReportedCollision, clientConfirmed, repairPending;
      long repairNotBeforeTick;
      String lastClassification = "not-probed";
      ExpectedVehicle(MCH_EntityBaseVehicle vehicle, boolean premature) { this.vehicle = vehicle; this.premature = premature; }
   }

   private static boolean shouldSendProbe(int age) { return age == 1 || age == 5 || age == 10 || age == 20 || age == 40 || age == 60; }

   private void sendProbe(TrackingRefresh refresh) {
      PacketVehicleRespawnProbe packet = new PacketVehicleRespawnProbe();
      packet.generation = refresh.generation; packet.playerId = refresh.player.getEntityId();
      packet.playerUuid = refresh.player.getUniqueID(); packet.dimension = refresh.player.dimension;
      packet.serverWorldIdentity = System.identityHashCode(refresh.player.worldObj);
      for(ExpectedVehicle expected : refresh.expected.values()) {
         if(packet.vehicles.size() >= PacketVehicleRespawnProbe.MAX_VEHICLES) break;
         MCH_EntityBaseVehicle vehicle = expected.vehicle;
         if(vehicle.isDead || vehicle.worldObj != refresh.player.worldObj) continue;
         PacketVehicleRespawnProbe.Entry entry = new PacketVehicleRespawnProbe.Entry();
         entry.entityId=vehicle.getEntityId(); entry.uuid=vehicle.getUniqueID();
         entry.typeName=vehicle.getAcInfo()==null?vehicle.getClass().getSimpleName():vehicle.getAcInfo().name;
         entry.commonUniqueId=vehicle.getCommonUniqueId()==null?"":vehicle.getCommonUniqueId();
         entry.x=vehicle.posX; entry.y=vehicle.posY; entry.z=vehicle.posZ; entry.chunkX=vehicle.chunkCoordX; entry.chunkZ=vehicle.chunkCoordZ;
         packet.vehicles.add(entry);
      }
      MCH_MOD.getPacketHandler().sendTo(packet, refresh.player);
      MCH_Lib.RespawnAuditLog("probe-send generation=%d age=%d playerId=%d vehicles=%d", Integer.valueOf(refresh.generation),
         Integer.valueOf(refresh.age), Integer.valueOf(refresh.player.getEntityId()), Integer.valueOf(packet.vehicles.size()));
   }

   public static void enqueueProbeResult(EntityPlayerMP player, PacketVehicleRespawnProbeResult result) {
      if(player == null || result == null) {
         return;
      }

      PENDING_PROBE_RESULTS.offer(new PendingProbeResult(player, result));
   }

   private void processPendingProbeResults() {
      PendingProbeResult pending;

      while((pending = PENDING_PROBE_RESULTS.poll()) != null) {
         EntityPlayerMP player = pending.player;

         if(player == null
            || player.isDead
            || player.playerNetServerHandler == null
            || !(player.worldObj instanceof WorldServer)
            || !containsPlayerInstance(player.worldObj.playerEntities, player)) {
            MCH_Lib.RespawnAuditLog(
               "probe-result-discarded player=%s generation=%d reason=stale-player",
               player == null ? "null" : player.getCommandSenderName(),
               Integer.valueOf(pending.result.generation));
            continue;
         }

         this.handleProbeResult(player, pending.result);
      }
   }

   private void handleProbeResult(EntityPlayerMP player, PacketVehicleRespawnProbeResult result) {
      TrackingRefresh refresh = null;
      for(TrackingRefresh candidate : this.pendingTrackingRefreshes.values()) {
         if(candidate.player == player && candidate.generation == result.generation) {
            refresh = candidate;
            break;
         }
      }
      if(refresh == null) {
         MCH_Lib.RespawnAuditLog("probe-result-stale generation=%d playerId=%d", Integer.valueOf(result.generation), Integer.valueOf(player.getEntityId()));
         return;
      }
      refresh.resultReceived = true;
      refresh.replacementReady = result.replacementProcessed;
      boolean all = result.replacementProcessed;
      MCH_Lib.RespawnAuditLog("probe-result generation=%d ready=%s playerPresent=%s worldPresent=%s playerIdMatches=%s playerUuidDiagnostic=%s dimensionMatches=%s auditPlayerMatches=%s respawnAgeReady=%s age=%d vehicles=%d",
         Integer.valueOf(result.generation), Boolean.valueOf(result.replacementProcessed), Boolean.valueOf(result.playerPresent), Boolean.valueOf(result.worldPresent),
         Boolean.valueOf(result.playerIdMatches), Boolean.valueOf(result.playerUuidMatchesDiagnostic), Boolean.valueOf(result.dimensionMatches),
         Boolean.valueOf(result.auditPlayerMatches), Boolean.valueOf(result.respawnAgeReady), Integer.valueOf(result.respawnAge), Integer.valueOf(result.vehicles.size()));
      if(!result.playerPresent || !result.worldPresent) {
         MCH_Lib.RespawnAuditLog("probe-divergence generation=%d classification=2-probe-before-replacement-world", Integer.valueOf(result.generation));
      }
      for(PacketVehicleRespawnProbeResult.Entry entry : result.vehicles) {
         ExpectedVehicle expected = refresh.expected.get(Integer.valueOf(entry.expectedId));
         String classification = classify(entry, result.respawnAge);
         boolean correct = "confirmed".equals(classification);
         all &= correct;
         MCH_Lib.RespawnAuditLog("probe-vehicle generation=%d expectedId=%d expectedType=%s serverUuid=%s class=%s foundId=%d foundType=%s clientUuidDiagnostic=%s uuidMatchesDiagnostic=%s positionDelta=%.2f previouslyConfirmedById=%s chunk=%s repairPending=%s classification=%s",
            Integer.valueOf(result.generation), Integer.valueOf(entry.expectedId), entry.expectedType, entry.expectedUuid, entry.foundClass,
            Integer.valueOf(entry.foundId), entry.foundType, entry.foundUuid, Boolean.valueOf(entry.uuidMatchesDiagnostic),
            Double.valueOf(Math.sqrt(entry.positionDeltaSq)), Boolean.valueOf(entry.previouslyFound), Boolean.valueOf(entry.addedToChunk),
            Boolean.valueOf(expected != null && expected.repairPending), classification);
         if(expected != null) {
            expected.lastClassification = classification;
            expected.clientConfirmed = correct;
            if(correct) {
               expected.repairPending = false;
            } else if(result.replacementProcessed && ("3-missing-entity".equals(classification) || "4-id-collision".equals(classification) || "5-removed-after-create".equals(classification))) {
               expected.clientReportedMissing |= !entry.foundById;
               expected.clientReportedCollision |= "4-id-collision".equals(classification);
               expected.repairPending = expected.resendAttempts < 2;
               expected.repairNotBeforeTick = this.respawnAuditTick + 1L;
               MCH_Lib.RespawnAuditLog("repair-pending generation=%d vehicleId=%d classification=%s pending=%s",
                  Integer.valueOf(refresh.generation), Integer.valueOf(entry.expectedId), classification, Boolean.valueOf(expected.repairPending));
            }
         }
      }
      refresh.confirmedAll = all && result.vehicles.size() == refresh.expected.size();
   }

   private static String classify(PacketVehicleRespawnProbeResult.Entry e, int respawnAge) {
      if(!e.foundById && e.previouslyFound) return "5-removed-after-create";
      if(!e.foundById) return "3-missing-entity";
      if(!e.baseVehicle || e.foundId != e.expectedId || !e.typeMatches || !e.dimensionMatches || !e.positionMatches || !e.currentWorld || !e.commonIdMatches) return "4-id-collision";
      if(e.dead) return "6-dead";
      if(!e.addedToChunk && respawnAge >= 10) return "7-not-in-chunk";
      if("null".equals(e.foundType)) return "8-no-ac-info";
      if(e.skipNormalRender) return "9-render-skipped";
      if(!e.collidable || "null".equals(e.boundingBox)) return "10-invalid-collision";
      if(e.validSeatParents != e.seatCount || e.validHitboxParents < e.hitboxCount) return "11-invalid-dependent-parent";
      return "confirmed";
   }

   private void executePendingRepairs(TrackingRefresh refresh, WorldServer world) {
      for(ExpectedVehicle expected : refresh.expected.values()) {
         if(!expected.repairPending || expected.clientConfirmed || expected.resendAttempts >= 2) continue;
         if(this.respawnAuditTick < expected.repairNotBeforeTick) continue;
         if(!refresh.replacementReady || refresh.player.isDead || refresh.player.worldObj != world || !containsPlayerInstance(world.playerEntities, refresh.player)) continue;
         if(expected.vehicle.isDead || expected.vehicle.worldObj != world) {
            expected.repairPending = false;
            continue;
         }
         if(!world.getPlayerManager().isPlayerWatchingChunk(refresh.player, expected.vehicle.chunkCoordX, expected.vehicle.chunkCoordZ)) {
            MCH_Lib.RespawnAuditLog("repair-wait generation=%d vehicleId=%d reason=chunk-not-watched", Integer.valueOf(refresh.generation), Integer.valueOf(expected.vehicle.getEntityId()));
            continue;
         }
         this.targetedResend(refresh, expected, world);
      }
   }

   private void targetedResend(TrackingRefresh refresh, ExpectedVehicle expected, WorldServer world) {
      EntityTrackerEntry parentEntry = trackerEntry(world, expected.vehicle.getEntityId());
      if(parentEntry == null) {
         MCH_Lib.RespawnAuditLog("resend-no-entry generation=%d vehicleId=%d", Integer.valueOf(refresh.generation), Integer.valueOf(expected.vehicle.getEntityId()));
         return;
      }
      ++expected.resendAttempts; ++refresh.attempts; expected.repairPending = false;
      removeAndReaddWatcher(parentEntry, refresh.player, refresh.generation, expected.vehicle.getEntityId());
      MCH_Lib.RespawnAuditLog("targeted-resend generation=%d vehicleId=%d attempt=%d", Integer.valueOf(refresh.generation), Integer.valueOf(expected.vehicle.getEntityId()), Integer.valueOf(expected.resendAttempts));
      for(Object object : world.loadedEntityList) {
         boolean dependent = object instanceof MCH_EntitySeat && ((MCH_EntitySeat)object).getParent() == expected.vehicle
            || object instanceof MCH_EntityHitBox && ((MCH_EntityHitBox)object).parent == expected.vehicle;
         if(dependent) {
            Entity entity = (Entity)object;
            EntityTrackerEntry entry = trackerEntry(world, entity.getEntityId());
            if(entry != null) {
               removeAndReaddWatcher(entry, refresh.player, refresh.generation, entity.getEntityId());
               MCH_Lib.RespawnAuditLog("dependent-resend generation=%d parentId=%d dependentId=%d", Integer.valueOf(refresh.generation), Integer.valueOf(expected.vehicle.getEntityId()), Integer.valueOf(entity.getEntityId()));
            }
         }
      }
      refresh.probeRequested = true;
      MCH_Lib.RespawnAuditLog("post-resend-probe generation=%d vehicleId=%d", Integer.valueOf(refresh.generation), Integer.valueOf(expected.vehicle.getEntityId()));
   }

   private static void removeAndReaddWatcher(EntityTrackerEntry entry, EntityPlayerMP player, int generation, int vehicleId) {
      Set watchers = ObfuscationReflectionHelper.getPrivateValue(EntityTrackerEntry.class, entry, "trackingPlayers", "field_73134_o");
      int before = watchers == null ? -1 : watchers.size();
      boolean exactRemoved = false;
      if(watchers != null) {
         Iterator iterator = watchers.iterator();
         while(iterator.hasNext()) {
            if(iterator.next() == player) {
               iterator.remove(); exactRemoved = true; break;
            }
         }
      } else {
         MCH_Lib.RespawnAuditLog("watcher-reflection-failure generation=%d vehicleId=%d", Integer.valueOf(generation), Integer.valueOf(vehicleId));
      }
      entry.removeFromWatchingList(player);
      if(exactRemoved) player.func_152339_d(entry.myEntity);
      int after = watchers == null ? -1 : watchers.size();
      MCH_Lib.RespawnAuditLog("watcher-remove generation=%d vehicleId=%d exactRemoved=%s watchersBefore=%d watchersAfter=%d", Integer.valueOf(generation), Integer.valueOf(vehicleId), Boolean.valueOf(exactRemoved), Integer.valueOf(before), Integer.valueOf(after));
      entry.tryStartWachingThis(player);
      boolean exactPresent = false;
      if(watchers != null) for(Object watcher : watchers) if(watcher == player) { exactPresent = true; break; }
      MCH_Lib.RespawnAuditLog("watcher-readd generation=%d vehicleId=%d exactPresent=%s", Integer.valueOf(generation), Integer.valueOf(vehicleId), Boolean.valueOf(exactPresent));
   }

   private static EntityTrackerEntry trackerEntry(WorldServer world, int entityId) {
      IntHashMap map = ObfuscationReflectionHelper.getPrivateValue(EntityTracker.class, world.getEntityTracker(), "trackedEntityHashTable", "field_72794_c");
      return map == null ? null : (EntityTrackerEntry)map.lookup(entityId);
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
      ++this.respawnAuditTick;
      this.processPendingProbeResults();
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
