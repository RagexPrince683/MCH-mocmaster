package mcheli;

import cpw.mods.fml.common.FMLCommonHandler;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_ServerTickHandler;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_SoundUpdater;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;

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
      MCH_Lib.RespawnAuditLog("registered FML server handlers: PlayerRespawnEvent, PlayerChangedDimensionEvent, ServerTickEvent");
   }

   public boolean isRemote() {
      return false;
   }

   public String side() {
      return "Server";
   }

   public MCH_SoundUpdater CreateSoundUpdater(MCH_EntityBaseVehicle aircraft) {
      return null;
   }

   public void registerSounds() {}

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

   public Entity getClientPlayer() {
      return null;
   }

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
