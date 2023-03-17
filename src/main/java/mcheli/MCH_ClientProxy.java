package mcheli;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import mcheli.aircraft.*;
import mcheli.block.*;
import mcheli.chain.MCH_EntityChain;
import mcheli.chain.MCH_RenderChain;
import mcheli.command.MCH_GuiTitle;
import mcheli.container.MCH_EntityContainer;
import mcheli.container.MCH_RenderContainer;
import mcheli.debug.MCH_RenderTest;
import mcheli.flare.MCH_EntityFlare;
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
import mcheli.multiplay.MCH_MultiplayClient;
import mcheli.parachute.MCH_EntityParachute;
import mcheli.parachute.MCH_RenderParachute;
import mcheli.particles.MCH_ParticlesUtil;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlaneInfo;
import mcheli.plane.MCP_PlaneInfoManager;
import mcheli.plane.MCP_RenderPlane;
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
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_RenderVehicle;
import mcheli.vehicle.MCH_VehicleInfo;
import mcheli.vehicle.MCH_VehicleInfoManager;
import mcheli.weapon.*;
import mcheli.wrapper.*;
import mcheli.wrapper.modelloader.W_ModelCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.MinecraftForge;

import java.util.Iterator;

public class MCH_ClientProxy extends MCH_CommonProxy {

   public String lastLoadHUDPath = "";


   public String getDataDir() {
      return Minecraft.getMinecraft().mcDataDir.getPath();
   }

   public void registerRenderer() {
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntitySeat.class, new MCH_RenderTest(0.0F, 0.0F, 0.0F, "seat"));
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityHeli.class, new MCH_RenderHeli());
      RenderingRegistry.registerEntityRenderingHandler(MCP_EntityPlane.class, new MCP_RenderPlane());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTank.class, new MCH_RenderTank());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityGLTD.class, new MCH_RenderGLTD());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityChain.class, new MCH_RenderChain());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityParachute.class, new MCH_RenderParachute());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityContainer.class, new MCH_RenderContainer());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityVehicle.class, new MCH_RenderVehicle());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityUavStation.class, new MCH_RenderUavStation());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityCartridge.class, new MCH_RenderCartridge());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityHide.class, new MCH_RenderNull());
      RenderingRegistry.registerEntityRenderingHandler(MCH_ViewEntityDummy.class, new MCH_RenderNull());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityRocket.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTvMissile.class, new MCH_RenderTvMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityBullet.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityA10.class, new MCH_RenderA10());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityAAMissile.class, new MCH_RenderAAMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntitySARHMissile.class, new MCH_RenderSARHMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityASMissile.class, new MCH_RenderASMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityAShM.class, new MCH_RenderASMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityARM.class, new MCH_RenderASMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityATMissile.class, new MCH_RenderTvMissile());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityTorpedo.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityBomb.class, new MCH_RenderBomb());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityMarkerRocket.class, new MCH_RenderBullet());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityDispensedItem.class, new MCH_RenderNone());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityFlare.class, new MCH_RenderFlare());
      RenderingRegistry.registerEntityRenderingHandler(MCH_EntityThrowable.class, new MCH_RenderThrowable());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemJavelin, new MCH_ItemLightWeaponRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemStinger, new MCH_ItemLightWeaponRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.invisibleItem, new MCH_InvisibleItemRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemGLTD, new MCH_ItemGLTDRender());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemWrench, new MCH_ItemRenderWrench());
      W_MinecraftForgeClient.registerItemRenderer(MCH_MOD.itemRangeFinder, new MCH_ItemRenderRangeFinder());
   }

   public void registerBlockRenderer() {
      ClientRegistry.bindTileEntitySpecialRenderer(MCH_DraftingTableTileEntity.class, new MCH_DraftingTableRenderer());
      W_MinecraftForgeClient.registerItemRenderer(W_Item.getItemFromBlock(MCH_MOD.blockDraftingTable), new MCH_DraftingTableItemRender());


      ClientRegistry.bindTileEntitySpecialRenderer(SewingTileEntity.class, new BlockRenderer());
      W_MinecraftForgeClient.registerItemRenderer(W_Item.getItemFromBlock(MCH_MOD.blockSewingMachine), new MCH_DraftingTableItemRender());
   }

   public void registerModels() {
      MCH_ModelManager.setForceReloadMode(true);
      MCH_RenderAircraft.debugModel = MCH_ModelManager.load("box");
      MCH_ModelManager.load("a-10");
      MCH_RenderGLTD.model = MCH_ModelManager.load("gltd");
      MCH_ModelManager.load("chain");
      MCH_ModelManager.load("container");
      MCH_ModelManager.load("parachute1");
      MCH_ModelManager.load("parachute2");
      MCH_ModelManager.load("lweapons", "fim92");
      MCH_ModelManager.load("lweapons", "fgm148");
      String[] i$ = MCH_RenderUavStation.MODEL_NAME;
      int wi = i$.length;

      for(int i$1 = 0; i$1 < wi; ++i$1) {
         String s = i$[i$1];
         MCH_ModelManager.load(s);
      }

      MCH_ModelManager.load("wrench");
      MCH_ModelManager.load("rangefinder");
      MCH_HeliInfoManager.getInstance();
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

      MCH_TankInfoManager.getInstance();
      var5 = MCH_TankInfoManager.map.keySet().iterator();

      while(var5.hasNext()) {
         var6 = (String)var5.next();
         this.registerModelsTank(var6, false);
      }

      var5 = MCH_VehicleInfoManager.map.keySet().iterator();

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
      MCH_ModelManager.load("blocks", "sewing_machine");
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
      MCH_AircraftInfo.DrawnPart w;
      for(i$ = info.nozzles.iterator(); i$.hasNext(); w.model = this.loadPartModel("planes", info.name, info.model, w.modelName)) {
         w = (MCH_AircraftInfo.DrawnPart)i$.next();
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

   public void registerModelsVehicle(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_VehicleInfo info = (MCH_VehicleInfo)MCH_VehicleInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("vehicles", info.name);
      Iterator i$ = info.partList.iterator();

      while(i$.hasNext()) {
         MCH_VehicleInfo.VPart vp = (MCH_VehicleInfo.VPart)i$.next();
         vp.model = this.loadPartModel("vehicles", info.name, info.model, vp.modelName);
         if(vp.child != null) {
            this.registerVCPModels(info, vp);
         }
      }

      this.registerCommonPart("vehicles", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   public void registerModelsTank(String name, boolean reload) {
      MCH_ModelManager.setForceReloadMode(reload);
      MCH_TankInfo info = (MCH_TankInfo)MCH_TankInfoManager.map.get(name);
      info.model = MCH_ModelManager.load("tanks", info.name);
      this.registerCommonPart("tanks", info);
      MCH_ModelManager.setForceReloadMode(false);
   }

   private MCH_BulletModel loadBulletModel(String name) {
      IModelCustom m = MCH_ModelManager.load("bullets", name);
      return m != null?new MCH_BulletModel(name, m):null;
   }

   private IModelCustom loadPartModel(String path, String name, IModelCustom body, String part) {
      return body instanceof W_ModelCustom && ((W_ModelCustom)body).containsPart("$" + part)?null:MCH_ModelManager.load(path, name + "_" + part);
   }

   private void registerCommonPart(String path, MCH_AircraftInfo info) {
      Iterator i$;
      MCH_AircraftInfo.Hatch c;
      for(i$ = info.hatchList.iterator(); i$.hasNext(); c.model = this.loadPartModel(path, info.name, info.model, c.modelName)) {
         c = (MCH_AircraftInfo.Hatch)i$.next();
      }

      MCH_AircraftInfo.Camera c1;
      for(i$ = info.cameraList.iterator(); i$.hasNext(); c1.model = this.loadPartModel(path, info.name, info.model, c1.modelName)) {
         c1 = (MCH_AircraftInfo.Camera)i$.next();
      }

      MCH_AircraftInfo.Throttle c2;
      for(i$ = info.partThrottle.iterator(); i$.hasNext(); c2.model = this.loadPartModel(path, info.name, info.model, c2.modelName)) {
         c2 = (MCH_AircraftInfo.Throttle)i$.next();
      }

      MCH_AircraftInfo.RotPart c3;
      for(i$ = info.partRotPart.iterator(); i$.hasNext(); c3.model = this.loadPartModel(path, info.name, info.model, c3.modelName)) {
         c3 = (MCH_AircraftInfo.RotPart)i$.next();
      }

      i$ = info.partWeapon.iterator();

      while(i$.hasNext()) {
         MCH_AircraftInfo.PartWeapon c4 = (MCH_AircraftInfo.PartWeapon)i$.next();
         c4.model = this.loadPartModel(path, info.name, info.model, c4.modelName);

         MCH_AircraftInfo.PartWeaponChild wc;
         for(Iterator i$1 = c4.child.iterator(); i$1.hasNext(); wc.model = this.loadPartModel(path, info.name, info.model, wc.modelName)) {
            wc = (MCH_AircraftInfo.PartWeaponChild)i$1.next();
         }
      }

      MCH_AircraftInfo.Canopy c5;
      for(i$ = info.canopyList.iterator(); i$.hasNext(); c5.model = this.loadPartModel(path, info.name, info.model, c5.modelName)) {
         c5 = (MCH_AircraftInfo.Canopy)i$.next();
      }

      MCH_AircraftInfo.LandingGear c6;
      for(i$ = info.landingGear.iterator(); i$.hasNext(); c6.model = this.loadPartModel(path, info.name, info.model, c6.modelName)) {
         c6 = (MCH_AircraftInfo.LandingGear)i$.next();
      }

      MCH_AircraftInfo.WeaponBay c7;
      for(i$ = info.partWeaponBay.iterator(); i$.hasNext(); c7.model = this.loadPartModel(path, info.name, info.model, c7.modelName)) {
         c7 = (MCH_AircraftInfo.WeaponBay)i$.next();
      }

      MCH_AircraftInfo.CrawlerTrack c8;
      for(i$ = info.partCrawlerTrack.iterator(); i$.hasNext(); c8.model = this.loadPartModel(path, info.name, info.model, c8.modelName)) {
         c8 = (MCH_AircraftInfo.CrawlerTrack)i$.next();
      }

      MCH_AircraftInfo.TrackRoller c9;
      for(i$ = info.partTrackRoller.iterator(); i$.hasNext(); c9.model = this.loadPartModel(path, info.name, info.model, c9.modelName)) {
         c9 = (MCH_AircraftInfo.TrackRoller)i$.next();
      }

      MCH_AircraftInfo.PartWheel c10;
      for(i$ = info.partWheel.iterator(); i$.hasNext(); c10.model = this.loadPartModel(path, info.name, info.model, c10.modelName)) {
         c10 = (MCH_AircraftInfo.PartWheel)i$.next();
      }

      for(i$ = info.partSteeringWheel.iterator(); i$.hasNext(); c10.model = this.loadPartModel(path, info.name, info.model, c10.modelName)) {
         c10 = (MCH_AircraftInfo.PartWheel)i$.next();
      }

   }

   private void registerVCPModels(MCH_VehicleInfo info, MCH_VehicleInfo.VPart vp) {
      Iterator i$ = vp.child.iterator();

      while(i$.hasNext()) {
         MCH_VehicleInfo.VPart vcp = (MCH_VehicleInfo.VPart)i$.next();
         vcp.model = this.loadPartModel("vehicles", info.name, info.model, vcp.modelName);
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

   public boolean isRemote() {
      return true;
   }

   public String side() {
      return "Client";
   }

   public MCH_SoundUpdater CreateSoundUpdater(MCH_EntityAircraft aircraft) {
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
      W_McClient.addSound("ir_basic_tone.ogg");
      W_McClient.addSound("ir_lock_tone.ogg");
      Iterator i$ = MCH_WeaponInfoManager.getValues().iterator();

      while(i$.hasNext()) {
         MCH_WeaponInfo info = (MCH_WeaponInfo)i$.next();
         W_McClient.addSound(info.soundFileName + ".ogg");
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

      i$ = MCH_VehicleInfoManager.map.values().iterator();

      while(i$.hasNext()) {
         MCH_VehicleInfo info4 = (MCH_VehicleInfo)i$.next();
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

   public void init() {
      MinecraftForge.EVENT_BUS.register(new MCH_ParticlesUtil());
      MinecraftForge.EVENT_BUS.register(new MCH_ClientEventHook());
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
