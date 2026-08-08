package mcheli;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import java.util.Iterator;
import java.util.UUID;

import mcheli.aircraft.MCH_BaseVehicleInfo;
import mcheli.aircraft.MCH_EntityBaseVehicle;
import mcheli.aircraft.MCH_EntityHide;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_RenderBaseVehicle;
import mcheli.aircraft.MCH_SoundUpdater;
import mcheli.aircraft.MCH_VehicleItemModelRender;
import mcheli.block.MCH_DraftingTableItemRender;
import mcheli.block.MCH_DraftingTableRenderer;
import mcheli.block.MCH_DraftingTableTileEntity;
import mcheli.chain.MCH_EntityChain;
import mcheli.chain.MCH_RenderChain;
import mcheli.command.MCH_GuiTitle;
import mcheli.container.MCH_EntityContainer;
import mcheli.container.MCH_RenderContainer;
import mcheli.debug.MCH_RenderTest;
import mcheli.flare.MCH_EntityChaff;
import mcheli.flare.MCH_EntityFlare;
import mcheli.flare.MCH_RenderChaff;
import mcheli.flare.MCH_RenderFlare;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gltd.MCH_ItemGLTDRender;
import mcheli.gltd.MCH_RenderGLTD;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_HeliInfo;
import mcheli.helicopter.MCH_HeliInfoManager;
import mcheli.helicopter.MCH_RenderHeli;
import mcheli.hud.MCH_HudManager;
import mcheli.lweapon.MCH_ItemLightWeaponRender;
import mcheli.item.MCH_ItemInfoManager;
import mcheli.lod.MCH_VehicleLODManager;
import mcheli.network.packets.PacketVehicleLODSnapshot;
import mcheli.multiplay.MCH_MultiplayClient;
import mcheli.parachute.MCH_EntityParachute;
import mcheli.parachute.MCH_RenderParachute;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.particles.MCH_ParticleTexture;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.plane.MCP_RenderPlane;
import mcheli.ship.MCH_EntityShip;
import mcheli.ship.MCH_RenderShip;
import mcheli.ship.MCH_ShipInfo;
import mcheli.ship.MCH_ShipInfoManager;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_RenderTank;
import mcheli.tank.MCH_TankInfo;
import mcheli.tank.MCH_TankInfoManager;
import mcheli.throwable.MCH_EntityThrowable;
import mcheli.throwable.MCH_RenderThrowable;
import mcheli.throwable.MCH_ThrowableInfo;
import mcheli.throwable.MCH_ThrowableInfoManager;
import mcheli.tool.MCH_ItemRenderWrench;
import mcheli.tool.rangefinder.MCH_ItemRenderRangeFinder;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.uav.MCH_RenderUavStation;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_RenderTurret;
import mcheli.vehicle.MCH_TurretInfo;
import mcheli.vehicle.MCH_TurretInfoManager;
import mcheli.weapon.*;
import mcheli.wrapper.W_Item;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_MinecraftForgeClient;
import mcheli.wrapper.W_Reflection;
import mcheli.wrapper.W_TickRegistry;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.MinecraftForge;
import mcheli.multithread.MultiThreadModelManager;

import mcheli.mob.MCH_EntityGunner;
import mcheli.mob.MCH_RenderGunner;

public class MCH_ClientProxy extends MCH_CommonProxy {

   private final MCH_RenderRWR rwrRenderer = new MCH_RenderRWR();
   public String lastLoadHUDPath = "";
   private long nextTargetedReloadId;
   private long pendingTargetedReloadId;
   private int pendingTargetedEntityId = -1;
   private UUID pendingTargetedEntityUuid;
   private String pendingTargetedDefinition;
   private Object pendingTargetedWorld;
   private long pendingTargetedDeadline;


   public String getDataDir() {
      return Minecraft.getMinecraft().mcDataDir.getPath();
   }

   @Override
   public void registerAddonResourcePack() {
      try {
         MCH_AddonResourcePack addonPack = new MCH_AddonResourcePack();
         // Register as a default pack so refreshResources() retains the live addon source.
         java.lang.reflect.Field field = Minecraft.class.getDeclaredField("defaultResourcePacks");
         field.setAccessible(true);
         @SuppressWarnings("unchecked")
         java.util.List<net.minecraft.client.resources.IResourcePack> defaultPacks =
             (java.util.List<net.minecraft.client.resources.IResourcePack>) field.get(Minecraft.getMinecraft());
         defaultPacks.add(addonPack);
         MCH_Lib.Log("Registered live MCHeli resource pack");
      } catch (Exception e) {
         MCH_Lib.Log("Failed to register addon resource pack: %s", e.getMessage());
      }
   }

   public void registerRenderer() {
      MinecraftForge.EVENT_BUS.register(MCH_VehicleLODManager.INSTANCE);
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntitySeat.class, new MCH_RenderTest(0.0F, 0.0F, 0.0F, "seat"));
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityHeli.class, new MCH_RenderHeli());
      RenderingRegistry.registerEntityRenderingHandler(MCP_EntityPlane.class, new MCP_RenderPlane());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityShip.class, new MCH_RenderShip());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTank.class, new MCH_RenderTank());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityGLTD.class, new MCH_RenderGLTD());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityChain.class, new MCH_RenderChain());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityParachute.class, new MCH_RenderParachute());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityContainer.class, new MCH_RenderContainer());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTurret.class, new MCH_RenderTurret());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityUavStation.class, new MCH_RenderUavStation());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityCartridge.class, new MCH_RenderCartridge());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityHide.class, new MCH_RenderNull());
      RenderingRegistry.registerEntityRenderingHandler(MCH_ViewEntityDummy.class, new MCH_RenderNull());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityRocket.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTvMissile.class, new MCH_RenderTvMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityBullet.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityA10.class, new MCH_RenderA10());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityAAMissile.class, new MCH_RenderAAMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityASMissile.class, new MCH_RenderASMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityATMissile.class, new MCH_RenderATMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTorpedo.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityBomb.class, new MCH_RenderBomb());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityMarkerRocket.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityDispensedItem.class, new MCH_RenderNone());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityFlare.class, new MCH_RenderFlare());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityThrowable.class, new MCH_RenderThrowable());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityGunner.class, new MCH_RenderGunner());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityLockBox.class, new MCH_RenderLockBox());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityChaff.class, new MCH_RenderChaff());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemJavelin, new MCH_ItemLightWeaponRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemStinger, new MCH_ItemLightWeaponRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemRpg, new MCH_ItemLightWeaponRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.invisibleItem, new MCH_InvisibleItemRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemGLTD, new MCH_ItemGLTDRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemWrench, new MCH_ItemRenderWrench());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemRangeFinder, new MCH_ItemRenderRangeFinder());
      this.registerVehicleItemRenderers();
   }

   private void registerVehicleItemRenderers() {
      MCH_VehicleItemModelRender renderer = new MCH_VehicleItemModelRender();
      registerVehicleItemRenderers(MCH_HeliInfoManager.map.values().iterator(), renderer);
      registerVehicleItemRenderers(MCP_PlaneInfoManager.map.values().iterator(), renderer);
      registerVehicleItemRenderers(MCH_ShipInfoManager.map.values().iterator(), renderer);
      registerVehicleItemRenderers(MCH_TankInfoManager.map.values().iterator(), renderer);
      registerVehicleItemRenderers(MCH_TurretInfoManager.map.values().iterator(), renderer);
   }

   private void registerVehicleItemRenderers(Iterator iterator, MCH_VehicleItemModelRender renderer) {
      while(iterator.hasNext()) {
         MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)iterator.next();
         if(info != null && info.getItem() != null) {
            W_MinecraftForgeClient.registerItemRenderer(info.getItem(), renderer);
         }
      }
   }

   public void registerBlockRenderer() {
      ClientRegistry.bindTileEntitySpecialRenderer(MCH_DraftingTableTileEntity.class, new MCH_DraftingTableRenderer());
      W_MinecraftForgeClient.registerItemRenderer(W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable), new MCH_DraftingTableItemRender());
   }


   public void scheduleClientInfoReload() {
      Minecraft.getMinecraft().func_152344_a(new Runnable() {
         public void run() {
            MCH_ResourceHelper.refreshResourceSources();
            // 1.7.10 rebuilds textures and the sound registry on this client-thread call.
            Minecraft.getMinecraft().refreshResources();
            int succeeded = 0;
            if(MCH_HeliInfoManager.getInstance().reload()) ++succeeded;
            if(MCP_PlaneInfoManager.getInstance().reload()) ++succeeded;
            if(MCH_ShipInfoManager.getInstance().reload()) ++succeeded;
            if(MCH_TankInfoManager.getInstance().reload()) ++succeeded;
            if(MCH_TurretInfoManager.getInstance().reload()) ++succeeded;
            if(MCH_WeaponInfoManager.reload()) ++succeeded;
            if(MCH_ItemInfoManager.reload()) ++succeeded;
            if(MCH_ThrowableInfoManager.reload()) ++succeeded;
            MCH_VehicleItemModelRender.resetForReload();
            MCH_ModelManager.clearForReload();
            registerModels();
            boolean hud = false;
            try { reloadHUD(); hud = true; } catch(RuntimeException e) { MCH_Lib.Log("HUD reload failed: %s", e.getMessage()); }
            MCH_Lib.Log("Client live reload: info=%d/8, models=complete, HUD=%s, textures/sounds=refreshed",
                    Integer.valueOf(succeeded), hud ? "complete" : "failed");
         }
      });
   }

   public boolean requestTargetedVehicleReload(MCH_EntityBaseVehicle vehicle) {
      Minecraft mc = Minecraft.getMinecraft();
      if(this.pendingTargetedReloadId != 0L) {
         chatTargetedReload(false, "A vehicle reload request is already pending");
         return false;
      }
      if(vehicle == null || vehicle.getAcInfo() == null) return false;
      long id = ++this.nextTargetedReloadId;
      if(id == 0L) id = ++this.nextTargetedReloadId;
      this.pendingTargetedReloadId = id;
      this.pendingTargetedEntityId = vehicle.getEntityId();
      this.pendingTargetedEntityUuid = vehicle.getUniqueID();
      this.pendingTargetedDefinition = vehicle.getAcInfo().name;
      this.pendingTargetedWorld = mc.theWorld;
      this.pendingTargetedDeadline = System.currentTimeMillis() + 10000L;
      mcheli.aircraft.MCH_PacketNotifyInfoReloaded.sendTargetedRequest(id, vehicle.getEntityId());
      return true;
   }

   public boolean isTargetedVehicleReloadPending() { return this.pendingTargetedReloadId != 0L; }

   public void tickTargetedVehicleReload() {
      if(this.pendingTargetedReloadId == 0L) return;
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.theWorld == null || mc.theWorld != this.pendingTargetedWorld) {
         clearTargetedReload();
      } else if(System.currentTimeMillis() >= this.pendingTargetedDeadline) {
         clearTargetedReload();
         chatTargetedReload(false, "Request timed out");
      }
   }

   private void clearTargetedReload() {
      this.pendingTargetedReloadId = 0L;
      this.pendingTargetedEntityId = -1;
      this.pendingTargetedEntityUuid = null;
      this.pendingTargetedDefinition = null;
      this.pendingTargetedWorld = null;
      this.pendingTargetedDeadline = 0L;
   }

   private void chatTargetedReload(boolean success, String text) {
      if(Minecraft.getMinecraft().thePlayer != null)
         Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
               success ? "Vehicle configuration reloaded: " + text : "Vehicle reload failed: " + text));
   }

   public void scheduleTargetedVehicleReload(final long requestId, final int entityId,
         final String definition, final boolean serverSuccess, final String serverReason) {
      Minecraft.getMinecraft().func_152344_a(new Runnable() {
         public void run() {
            if(requestId == 0L || requestId != pendingTargetedReloadId) return;
            boolean success = serverSuccess;
            String reason = serverReason;
            Entity entity = Minecraft.getMinecraft().theWorld == null ? null
                  : Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
            MCH_EntityBaseVehicle vehicle = entity instanceof MCH_EntityBaseVehicle
                  ? (MCH_EntityBaseVehicle)entity : null;
            if(success && vehicle == null) {
               success = false;
               reason = "Client entity unloaded";
            } else if(success && (entityId != pendingTargetedEntityId
                  || !vehicle.getUniqueID().equals(pendingTargetedEntityUuid)
                  || !definition.equals(pendingTargetedDefinition))) {
               success = false;
               reason = "Client entity changed before the reload result arrived";
            }
            if(success) {
               mcheli.MCH_InfoManagerBase manager = mcheli.aircraft.MCH_VehicleInfoReload.managerFor(vehicle);
               MCH_BaseVehicleInfo oldInfo = vehicle.getAcInfo();
               success = manager != null && manager.reloadEntry(definition);
               if(success) {
                  MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)manager.getMap().get(definition);
                  success = vehicle.applyTargetedInfo(info);
                  if(success) {
                     try {
                        MCH_VehicleItemModelRender.invalidate(oldInfo);
                        mcheli.tank.MCH_TurretPopModelCache.invalidate(oldInfo);
                        if(vehicle instanceof MCH_EntityHeli) registerModelsHeli(definition, true);
                        else if(vehicle instanceof MCP_EntityPlane) registerModelsPlane(definition, true);
                        else if(vehicle instanceof MCH_EntityShip) registerModelsShip(definition, true);
                        else if(vehicle instanceof MCH_EntityTank) registerModelsTank(definition, true);
                        else if(vehicle instanceof MCH_EntityTurret) registerModelsVehicle(definition, true);
                        else throw new IllegalStateException("Unsupported vehicle type");
                        MCH_VehicleLODManager.INSTANCE.invalidate(vehicle.getUniqueID());
                     } catch(RuntimeException e) {
                        success = false;
                        reason = "Client model reload failed: " + e.getMessage();
                     }
                  } else reason = "Seat count changed; restart is required";
               } else reason = manager == null ? "Unsupported vehicle type"
                     : "Client definition reload failed: " + manager.getLastReloadError();
            }
            clearTargetedReload();
            chatTargetedReload(success, success ? definition : reason);
         }
      });
   }

   public void registerModels() {
      MCH_ModelManager.setForceReloadMode(true);
      MCH_RenderBaseVehicle.debugModel = MCH_ModelManager.load("box");
      MCH_ModelManager.load("a-10");
      MCH_RenderGLTD.model = MCH_ModelManager.load("gltd");
      MCH_ModelManager.load("chain");
      MCH_ModelManager.load("container");
      MCH_ModelManager.load("parachute1");
      MCH_ModelManager.load("parachute2");
      MCH_ModelManager.load("lweapons", "fim92");
      MCH_ModelManager.load("lweapons", "fgm148");
      MCH_ModelManager.load("lweapons", "rpg7");
      String[] i$ = MCH_RenderUavStation.MODEL_NAME;
      int wi = i$.length;

      for(int i$1 = 0; i$1 < wi; ++i$1) {
         String s = i$[i$1];
         MCH_ModelManager.load(s);
      }

      MCH_ModelManager.load("wrench");
      MCH_ModelManager.load("rangefinder");
      MCH_HeliInfoManager.getInstance();

      if (MCH_Config.MultiThreadedModelLoading.prmBool) {
         System.out.println("Starting multithreaded model loading");
         MultiThreadModelManager.start(this);
         return;
      }

      Iterator var5 = MCH_HeliInfoManager.map.keySet().iterator();

      String var6;
      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsHeli(var6, false);
      }

      var5 = MCP_PlaneInfoManager.map.keySet().iterator();

      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsPlane(var6, false);
      }

      var5 = MCH_ShipInfoManager.map.keySet().iterator();

      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsShip(var6, false);
      }

      MCH_TankInfoManager.getInstance();
      var5 = MCH_TankInfoManager.map.keySet().iterator();

      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsTank(var6, false);
      }

      var5 = MCH_TurretInfoManager.map.keySet().iterator();

      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsVehicle(var6, false);
      }

      registerModels_Bullet();
      MCH_DefaultBulletModels.Bullet = this.loadBulletModel("bullet");
      MCH_DefaultBulletModels.AAMissile = this.loadBulletModel("aamissile");
      MCH_DefaultBulletModels.ATMissile = this.loadBulletModel("asmissile");
      MCH_DefaultBulletModels.ASMissile = this.loadBulletModel("asmissile");
      MCH_DefaultBulletModels.Bomb = this.loadBulletModel("bomb");
      MCH_DefaultBulletModels.Rocket = this.loadBulletModel("rocket");
      MCH_DefaultBulletModels.Torpedo = this.loadBulletModel("torpedo");

      MCH_ThrowableInfo var7;
      for(var5 = MCH_ThrowableInfoManager.getValues().iterator(); var5.hasNext(); var7.model = MCH_ModelManager.load("throwable", var7.name)) {
         var7 = (MCH_ThrowableInfo)var5.next();
      }

      MCH_ModelManager.load("blocks", "drafting_table");
   }

   public static void registerModels_Throwable(){
      System.out.println("Loading throwable");

      for (Object obj : MCH_ThrowableInfoManager.getValues()) {
         if (obj instanceof MCH_ThrowableInfo) { // Ensure the object is of type MCH_ThrowableInfo
            MCH_ThrowableInfo throwableInfo = (MCH_ThrowableInfo) obj;
            IModelCustom modelCustom = MCH_ModelManager.load("throwable", throwableInfo.name);
            if (modelCustom != null) {
               System.out.println("Adding model for " + throwableInfo.name);
               throwableInfo.model = modelCustom;
            } else {
               System.out.println("ERROR: No model found for throwable " + throwableInfo.name);
            }
         } else {
            System.out.println("ERROR: Invalid object type in throwable info manager");
         }
      }
   }

   public static void registerModels_Bullet() {
      Iterator i$ = MCH_WeaponInfoManager.getValues().iterator();

      while(i$.hasNext()) {
         MCH_WeaponInfo wi = (MCH_WeaponInfo)i$.next();
         IModelCustom m = null;
         if(!wi.bulletModelName.isEmpty()) {
            m = MCH_ModelManager.load("bullets", wi.bulletModelName);
            if(m != null) {
               wi.bulletModel = new MCH_BulletModel(wi.bulletModelName, m);
            }
         }

         if(!wi.bombletModelName.isEmpty()) {
            m = MCH_ModelManager.load("bullets", wi.bombletModelName);
            if(m != null) {
               wi.bombletModel = new MCH_BulletModel(wi.bombletModelName, m);
            }
         }

         if(wi.cartridge != null && !wi.cartridge.name.isEmpty()) {
            wi.cartridge.model = MCH_ModelManager.load("bullets", wi.cartridge.name);
            if(wi.cartridge.model == null) {
               wi.cartridge = null;
            }
         }
      }

   }

   public void registerModelsHeli(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_HeliInfo info = (MCH_HeliInfo)MCH_HeliInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("helicopters", info.name);

      MCH_HeliInfo.Rotor rotor;
      for(Iterator i$ = info.rotorList.iterator(); i$.hasNext(); rotor.model = this.loadPartModel("helicopters", info.name, info.model, rotor.modelName)) {
         rotor = (MCH_HeliInfo.Rotor)i$.next();
      }

      this.registerCommonPart("helicopters", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public void registerModelsPlane(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCP_PlaneInfo info = (MCP_PlaneInfo)MCP_PlaneInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("planes", info.name);

      Iterator i$;
      MCH_BaseVehicleInfo.DrawnPart w;
      for(i$ = info.nozzles.iterator(); i$.hasNext(); w.model = this.loadPartModel("planes", info.name, info.model, w.modelName)) {
         w = (MCH_BaseVehicleInfo.DrawnPart)i$.next();
      }

      i$ = info.rotorList.iterator();

      Iterator i$1;
      while(i$.hasNext()) {
         MCP_PlaneInfo.Rotor w1 = (MCP_PlaneInfo.Rotor)i$.next();
         w1.model = this.loadPartModel("planes", info.name, info.model, w1.modelName);

         MCP_PlaneInfo.Blade p;
         for(i$1 = w1.blades.iterator(); i$1.hasNext(); p.model = this.loadPartModel("planes", info.name, info.model, p.modelName)) {
            p = (MCP_PlaneInfo.Blade)i$1.next();
         }
      }

      i$ = info.wingList.iterator();

      while(i$.hasNext()) {
         MCP_PlaneInfo.Wing w2 = (MCP_PlaneInfo.Wing)i$.next();
         w2.model = this.loadPartModel("planes", info.name, info.model, w2.modelName);
         MCP_PlaneInfo.Pylon p1;
         if(w2.pylonList != null) {
            for(i$1 = w2.pylonList.iterator(); i$1.hasNext(); p1.model = this.loadPartModel("planes", info.name, info.model, p1.modelName)) {
               p1 = (MCP_PlaneInfo.Pylon)i$1.next();
            }
         }
      }

      this.registerCommonPart("planes", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public void registerModelsShip(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_ShipInfo info = (MCH_ShipInfo)MCH_ShipInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("ships", info.name);

      Iterator i$;
      MCH_BaseVehicleInfo.DrawnPart w;
      for(i$ = info.nozzles.iterator(); i$.hasNext(); w.model = this.loadPartModel("ships", info.name, info.model, w.modelName)) {
         w = (MCH_BaseVehicleInfo.DrawnPart)i$.next();
      }

      i$ = info.rotorList.iterator();

      Iterator i$1;
      while(i$.hasNext()) {
         MCH_ShipInfo.Rotor w1 = (MCH_ShipInfo.Rotor)i$.next();
         w1.model = this.loadPartModel("ships", info.name, info.model, w1.modelName);

         MCH_ShipInfo.Blade p;
         for(i$1 = w1.blades.iterator(); i$1.hasNext(); p.model = this.loadPartModel("ships", info.name, info.model, p.modelName)) {
            p = (MCH_ShipInfo.Blade)i$1.next();
         }
      }

      i$ = info.wingList.iterator();

      while(i$.hasNext()) {
         MCH_ShipInfo.Wing w2 = (MCH_ShipInfo.Wing)i$.next();
         w2.model = this.loadPartModel("ships", info.name, info.model, w2.modelName);
         MCH_ShipInfo.Pylon p1;
         if(w2.pylonList != null) {
            for(i$1 = w2.pylonList.iterator(); i$1.hasNext(); p1.model = this.loadPartModel("ships", info.name, info.model, p1.modelName)) {
               p1 = (MCH_ShipInfo.Pylon)i$1.next();
            }
         }
      }

      this.registerCommonPart("ships", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public void registerModelsVehicle(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_TurretInfo info = (MCH_TurretInfo)MCH_TurretInfoManager.map.get(name);
      String turretDirectory = info.getDirectoryName();
      info.model = MCH_ModelManager.load(turretDirectory, info.name);
      Iterator i$ = info.partList.iterator();

      while(i$.hasNext()) {
         MCH_TurretInfo.VPart vp = (MCH_TurretInfo.VPart)i$.next();
         vp.model = this.loadPartModel(turretDirectory, info.name, info.model, vp.modelName);
         if(vp.child != null) {
            this.registerVCPModels(info, vp);
         }
      }

      this.registerCommonPart(turretDirectory, info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public void registerModelsTank(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_TankInfo info = (MCH_TankInfo)MCH_TankInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("tanks", info.name);
      this.registerCommonPart("tanks", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public MCH_BulletModel loadBulletModel(String name) {
      IModelCustom m = MCH_ModelManager.load("bullets", name);
      return m != null?new MCH_BulletModel(name, m):null;
   }

   private IModelCustom loadPartModel(String path, String name, IModelCustom body, String part) {
      return body instanceof W_ModelCustom && ((W_ModelCustom)body).containsPart("$" + part)?null:MCH_ModelManager.load(path, name + "_" + part);
   }

   private void registerCommonPart(String path, MCH_BaseVehicleInfo info) {
      Iterator i$;
      MCH_BaseVehicleInfo.Hatch c;
      for(i$ = info.hatchList.iterator(); i$.hasNext(); c.model = this.loadPartModel(path, info.name, info.model, c.modelName)) {
         c = (MCH_BaseVehicleInfo.Hatch)i$.next();
      }

      MCH_BaseVehicleInfo.Camera c1;
      for(i$ = info.cameraList.iterator(); i$.hasNext(); c1.model = this.loadPartModel(path, info.name, info.model, c1.modelName)) {
         c1 = (MCH_BaseVehicleInfo.Camera)i$.next();
      }

      MCH_BaseVehicleInfo.Throttle c2;
      for(i$ = info.partThrottle.iterator(); i$.hasNext(); c2.model = this.loadPartModel(path, info.name, info.model, c2.modelName)) {
         c2 = (MCH_BaseVehicleInfo.Throttle)i$.next();
      }

      MCH_BaseVehicleInfo.RotPart c3;
      for(i$ = info.partRotPart.iterator(); i$.hasNext(); c3.model = this.loadPartModel(path, info.name, info.model, c3.modelName)) {
         c3 = (MCH_BaseVehicleInfo.RotPart)i$.next();
      }

      i$ = info.partWeapon.iterator();

      while(i$.hasNext()) {
         MCH_BaseVehicleInfo.PartWeapon c4 = (MCH_BaseVehicleInfo.PartWeapon)i$.next();
         c4.model = this.loadPartModel(path, info.name, info.model, c4.modelName);

         MCH_BaseVehicleInfo.PartWeaponChild wc;
         for(Iterator i$1 = c4.child.iterator(); i$1.hasNext(); wc.model = this.loadPartModel(path, info.name, info.model, wc.modelName)) {
            wc = (MCH_BaseVehicleInfo.PartWeaponChild)i$1.next();
         }
      }

      MCH_BaseVehicleInfo.Canopy c5;
      for(i$ = info.canopyList.iterator(); i$.hasNext(); c5.model = this.loadPartModel(path, info.name, info.model, c5.modelName)) {
         c5 = (MCH_BaseVehicleInfo.Canopy)i$.next();
      }

      MCH_BaseVehicleInfo.LandingGear c6;
      for(i$ = info.landingGear.iterator(); i$.hasNext(); c6.model = this.loadPartModel(path, info.name, info.model, c6.modelName)) {
         c6 = (MCH_BaseVehicleInfo.LandingGear)i$.next();
      }

      MCH_BaseVehicleInfo.WeaponBay c7;
      for(i$ = info.partWeaponBay.iterator(); i$.hasNext(); c7.model = this.loadPartModel(path, info.name, info.model, c7.modelName)) {
         c7 = (MCH_BaseVehicleInfo.WeaponBay)i$.next();
      }

      MCH_BaseVehicleInfo.CrawlerTrack c8;
      for(i$ = info.partCrawlerTrack.iterator(); i$.hasNext(); c8.model = this.loadPartModel(path, info.name, info.model, c8.modelName)) {
         c8 = (MCH_BaseVehicleInfo.CrawlerTrack)i$.next();
      }

      MCH_BaseVehicleInfo.TrackRoller c9;
      for(i$ = info.partTrackRoller.iterator(); i$.hasNext(); c9.model = this.loadPartModel(path, info.name, info.model, c9.modelName)) {
         c9 = (MCH_BaseVehicleInfo.TrackRoller)i$.next();
      }

      MCH_BaseVehicleInfo.PartWheel c10;
      for(i$ = info.partWheel.iterator(); i$.hasNext(); c10.model = this.loadPartModel(path, info.name, info.model, c10.modelName)) {
         c10 = (MCH_BaseVehicleInfo.PartWheel)i$.next();
      }

      for(i$ = info.partSteeringWheel.iterator(); i$.hasNext(); c10.model = this.loadPartModel(path, info.name, info.model, c10.modelName)) {
         c10 = (MCH_BaseVehicleInfo.PartWheel)i$.next();
      }

   }

   private void registerVCPModels(MCH_TurretInfo info, MCH_TurretInfo.VPart vp) {
      Iterator i$ = vp.child.iterator();

      while(i$.hasNext()) {
         MCH_TurretInfo.VPart vcp = (MCH_TurretInfo.VPart)i$.next();
         vcp.model = this.loadPartModel(info.getDirectoryName(), info.name, info.model, vcp.modelName);
         if(vcp.child != null) {
            this.registerVCPModels(info, vcp);
         }
      }

   }

   public void registerClientTick() {
      Minecraft mc = Minecraft.getMinecraft();
      MCH_ClientCommonTickHandler.instance = new MCH_ClientCommonTickHandler(mc, MCH_MOD.config);
      W_TickRegistry.registerTickHandler(MCH_ClientCommonTickHandler.instance, Side.CLIENT);
   }

   public void updateVehicleLODSnapshots(int dimension, java.util.List<PacketVehicleLODSnapshot.Entry> entries) {
      MCH_VehicleLODManager.INSTANCE.update(dimension, entries);
   }

   public void clearVehicleLODSnapshots() {
      MCH_VehicleLODManager.INSTANCE.clear();
   }

   public boolean isRemote() {
      return true;
   }

   public String side() {
      return "Client";
   }

   public MCH_SoundUpdater CreateSoundUpdater(MCH_EntityBaseVehicle aircraft) {
      return aircraft != null && aircraft.worldObj.isRemote?new MCH_SoundUpdater(Minecraft.getMinecraft(), aircraft, Minecraft.getMinecraft().thePlayer):null;
   }

   public void registerSounds() {
      W_McClient.addSound("alert.ogg");
      W_McClient.addSound("locked.ogg");
      W_McClient.addSound("gltd.ogg");
      W_McClient.addSound("zoom.ogg");
      W_McClient.addSound("ng.ogg");
      W_McClient.addSound("a-10_snd.ogg");
      W_McClient.addSound("gau-8_snd.ogg");
      W_McClient.addSound("hit.ogg");
      W_McClient.addSound("helidmg.ogg");
      W_McClient.addSound("heli.ogg");
      W_McClient.addSound("plane.ogg");
      W_McClient.addSound("plane_cc.ogg");
      W_McClient.addSound("plane_cv.ogg");
      W_McClient.addSound("chain.ogg");
      W_McClient.addSound("chain_ct.ogg");
      W_McClient.addSound("eject_seat.ogg");
      W_McClient.addSound("fim92_snd.ogg");
      W_McClient.addSound("fim92_reload.ogg");
      W_McClient.addSound("lockon.ogg");
      Iterator i$ = MCH_WeaponInfoManager.getValues().iterator();

      while(i$.hasNext()) {
         MCH_WeaponInfo info = (MCH_WeaponInfo)i$.next();
         W_McClient.addSound(info.soundFileName + ".ogg");
      }

      while(i$.hasNext()) {
         MCH_WeaponInfo info = (MCH_WeaponInfo)i$.next();
         W_McClient.addSound(info.weaponSwitchSound + ".ogg");
      }

      i$ = MCP_PlaneInfoManager.map.values().iterator();

      while(i$.hasNext()) {
         MCP_PlaneInfo info1 = (MCP_PlaneInfo)i$.next();
         if(!info1.soundMove.isEmpty()) {
            W_McClient.addSound(info1.soundMove + ".ogg");
         }
      }

      i$ = MCH_HeliInfoManager.map.values().iterator();

      while(i$.hasNext()) {
         MCH_HeliInfo info2 = (MCH_HeliInfo)i$.next();
         if(!info2.soundMove.isEmpty()) {
            W_McClient.addSound(info2.soundMove + ".ogg");
         }
      }

      i$ = MCH_TankInfoManager.map.values().iterator();

      while(i$.hasNext()) {
         MCH_TankInfo info3 = (MCH_TankInfo)i$.next();
         if(!info3.soundMove.isEmpty()) {
            W_McClient.addSound(info3.soundMove + ".ogg");
         }
      }

      i$ = MCH_TurretInfoManager.map.values().iterator();

      while(i$.hasNext()) {
         MCH_TurretInfo info4 = (MCH_TurretInfo)i$.next();
         if(!info4.soundMove.isEmpty()) {
            W_McClient.addSound(info4.soundMove + ".ogg");
         }
      }

   }

   public MCH_Config loadConfig(String fileName) {
      super.lastConfigFileName = fileName;
      MCH_Config config = new MCH_Config(Minecraft.getMinecraft().mcDataDir.getPath(), "/" + fileName);
      config.load();
      config.write();
      return config;
   }

   public MCH_Config reconfig() {
      MCH_Lib.DbgLog(false, "MCH_ClientProxy.reconfig()", new Object[0]);
      MCH_Config config = this.loadConfig(super.lastConfigFileName);
      MCH_ClientCommonTickHandler.instance.updatekeybind(config);
      return config;
   }

   public void loadHUD(String path) {
      this.lastLoadHUDPath = path;
      MCH_HudManager.load(path);
   }

   public void reloadHUD() {
      this.loadHUD(this.lastLoadHUDPath);
   }

   public Entity getClientPlayer() {
      return Minecraft.getMinecraft().thePlayer;
   }

   @Override
   public void sendPacketToServer(Packet packet) {
      if (packet != null && Minecraft.getMinecraft().thePlayer != null) {
         Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(packet);
      }
   }

   public void init() {
      MCH_ParticleTexture.register();
      mcheli.texture.MCH_ModelTextureRepairManager.register();
      MinecraftForge.EVENT_BUS.register(new MCH_ParticlesUtil());
      MinecraftForge.EVENT_BUS.register(new MCH_ClientEventHook());
      MinecraftForge.EVENT_BUS.register(new MCH_RenderBVRLockBox());
      MinecraftForge.EVENT_BUS.register(this.rwrRenderer);
   }

   public MCH_RenderRWR getRwrRenderer() {
      return this.rwrRenderer;
   }

   public void setCreativeDigDelay(int n) {
      W_Reflection.setCreativeDigSpeed(n);
   }

   public boolean isFirstPerson() {
      return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
   }

   public int getNewRenderType() {
      return RenderingRegistry.getNextAvailableRenderId();
   }

   public boolean isSinglePlayer() {
      return Minecraft.getMinecraft().isSingleplayer();
   }

   public void readClientModList() {
      try {
         Minecraft e = Minecraft.getMinecraft();
         MCH_MultiplayClient.readModList(e.getSession().getPlayerID());
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   public void printChatMessage(IChatComponent chat, int showTime, int pos) {
      ((MCH_GuiTitle)MCH_ClientCommonTickHandler.instance.gui_Title).setupTitle(chat, showTime, pos);
   }

   public void hitBullet() {
      MCH_ClientCommonTickHandler.instance.gui_Common.hitBullet();
   }

   public void clientLocked() {
      MCH_ClientCommonTickHandler.isLocked = true;
   }
}
