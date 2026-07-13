package mcheli;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.plane.MCP_EntityPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.tank.MCH_EntityTank;
import mcheli.vehicle.MCH_EntityTurret;
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
   private int tick;

   @SubscribeEvent
   public void onServerTickEvent(ServerTickEvent event) {
      if(event.phase != Phase.END || ++this.tick < UPDATE_INTERVAL_TICKS) {
         return;
      }
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
               && vehicle.getDistanceSqToEntity(player) <= farDistanceSq) {
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
         entry.packedLight = getPackedLight(world, vehicle);
         entries.add(entry);
      }
      return entries;
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
