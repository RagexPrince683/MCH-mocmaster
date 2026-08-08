package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_ServerTickHandler;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_IEntitySoundUpdater;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.network.Packet;

public class MCH_CommonProxy {

   public String lastConfigFileName;


   public String getDataDir() {
      return MinecraftServer.getServer().getFolderName();
   }

   public void registerRenderer() {}

   public void registerBlockRenderer() {}

   public void registerModels() {}

   public void registerModelsHeli(String name, boolean reload) {}

   public void registerModelsPlane(String name, boolean reload) {}
   public void registerModelsShip(String name, boolean reload) {}

   public void registerModelsVehicle(String name, boolean reload) {}

   public void registerModelsTank(String name, boolean reload) {}

   public void registerClientTick() {}

   public void updateVehicleLODSnapshots(int dimension, List<PacketVehicleLODSnapshot.Entry> entries) {}

   public void clearVehicleLODSnapshots() {}

   public void registerServerTick() {
      FMLCommonHandler.instance().bus().register(new MCH_ServerTickHandler());
   }

   public boolean isRemote() {
      return false;
   }

   public String side() {
      return "Server";
   }

   public MCH_IEntitySoundUpdater CreateSoundUpdater(MCH_EntityBaseVehicle aircraft) {
      return null;
   }

   public void registerSounds() {}

   /** Client-only hook; dedicated servers must not resolve resource-pack classes. */
   public void registerAddonResourcePack() {}

   public MCH_Config loadConfig(String fileName) {
      this.lastConfigFileName = fileName;
      MCH_Config config = new MCH_Config("./", fileName);
      config.load();
      config.write();
      return config;
   }

   public MCH_Config reconfig() {
      MCH_Lib.DbgLog(false, "MCH_CommonProxy.reconfig()", new Object[0]);
      return this.loadConfig(this.lastConfigFileName);
   }

   public void loadHUD(String path) {}

   public void reloadHUD() {}

   public void scheduleClientInfoReload() {}
   public boolean requestTargetedVehicleReload(mcheli.aircraft.MCH_EntityBaseVehicle vehicle) { return false; }
   public boolean isTargetedVehicleReloadPending() { return false; }
   public void tickTargetedVehicleReload() {}
   public void scheduleTargetedVehicleReload(long requestId, int entityId, String definition,
         boolean success, String reason) {}

   public Entity getClientPlayer() {
      return null;
   }

   /** Client-only packet queue hook. Calls on a dedicated server are ignored. */
   public void sendPacketToServer(Packet packet) {}

   public void setCreativeDigDelay(int n) {}

   public void init() {}

   public boolean isFirstPerson() {
      return false;
   }

   public int getNewRenderType() {
      return -1;
   }

   public boolean isSinglePlayer() {
      return MinecraftServer.getServer().isSinglePlayer();
   }

   public void readClientModList() {}

   public void printChatMessage(IChatComponent chat, int showTime, int pos) {}

   public void hitBullet() {}

   public void clientLocked() {}
}
