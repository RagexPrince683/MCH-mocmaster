package mcheli.gui;

import mcheli.MCH_ClientCommonTickHandler;
import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import mcheli.aircraft.MCH_EntityAircraft;
import mcheli.aircraft.MCH_PacketNotifyInfoReloaded;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.weapon.MCH_WeaponInfoManager;
import mcheli.wrapper.W_GuiButton;
import mcheli.wrapper.W_GuiContainer;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MCH_ConfigGui extends W_GuiContainer {

   private final EntityPlayer thePlayer;
   private int scaleFactor;
   private MCH_GuiOnOffButton buttonMouseInv;
   private MCH_GuiOnOffButton buttonStickModeHeli;
   private MCH_GuiOnOffButton buttonStickModePlane;
   private MCH_GuiOnOffButton buttonHideKeyBind;
   private MCH_GuiOnOffButton buttonShowHUDTP;
   private MCH_GuiOnOffButton buttonSmoothShading;
   private MCH_GuiOnOffButton buttonShowEntityMarker;
   private MCH_GuiOnOffButton buttonMarkThroughWall;
   private MCH_GuiOnOffButton buttonReplaceCamera;
   private MCH_GuiOnOffButton buttonNewExplosion;
   private MCH_GuiSlider sliderEntityMarkerSize;
   private MCH_GuiSlider sliderBlockMarkerSize;
   private MCH_GuiSlider sliderSensitivity;
   private MCH_GuiSlider[] sliderHitMark;
   private MCH_GuiOnOffButton buttonTestMode;
   private MCH_GuiOnOffButton buttonThrottleHeli;
   private MCH_GuiOnOffButton buttonThrottlePlane;
   private MCH_GuiOnOffButton buttonThrottleTank;
   private MCH_GuiOnOffButton buttonFlightSimMode;
   private MCH_GuiOnOffButton buttonSwitchWeaponWheel;
   private W_GuiButton buttonReloadAircraftInfo;
   private W_GuiButton buttonReloadWeaponInfo;
   private W_GuiButton buttonReloadAllHUD;
   public List listControlButtons;
   public List listRenderButtons;
   public List listKeyBindingButtons;
   public List listDevelopButtons;
   public MCH_GuiList keyBindingList;
   public int waitKeyButtonId;
   public int waitKeyAcceptCount;
   public static final int BUTTON_RENDER = 50;
   public static final int BUTTON_KEY_BINDING = 51;
   public static final int BUTTON_PREV_CONTROL = 52;
   public static final int BUTTON_DEVELOP = 55;
   public static final int BUTTON_KEY_LIST = 53;
   public static final int BUTTON_KEY_RESET_ALL = 54;
   public static final int BUTTON_KEY_LIST_BASE = 200;
   public static final int BUTTON_KEY_RESET_BASE = 300;
   public static final int BUTTON_DEV_RELOAD_AC = 400;
   public static final int BUTTON_DEV_RELOAD_WEAPON = 401;
   public static final int BUTTON_DEV_RELOAD_HUD = 402;
   public static final int BUTTON_SAVE_CLOSE = 100;
   public static final int BUTTON_CANCEL = 101;
   public int currentScreenId = 0;
   public static final int SCREEN_CONTROLS = 0;
   public static final int SCREEN_RENDER = 1;
   public static final int SCREEN_KEY_BIND = 2;
   public static final int SCREEN_DEVELOP = 3;
   private int ignoreButtonCounter = 0;


   public MCH_ConfigGui(EntityPlayer player) {
      super(new MCH_ConfigGuiContainer(player));
      this.thePlayer = player;
      super.xSize = 330;
      super.ySize = 200;
   }

   public void initGui() {
      super.initGui();
      super.buttonList.clear();
      int x1 = super.guiLeft + 10;
      int x2 = super.guiLeft + 10 + 150 + 10;
      int y = super.guiTop;
      boolean DY = true;
      this.listControlButtons = new ArrayList();
      this.buttonMouseInv = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, "Invert Mouse : ");
      this.sliderSensitivity = new MCH_GuiSlider(0, x1, y + 50, 150, 20, "Sensitivity : %.1f", 0.0F, 0.0F, 30.0F, 0.1F);
      this.buttonFlightSimMode = new MCH_GuiOnOffButton(0, x1, y + 75, 150, 20, "Mouse Flight Sim Mode : ");
      this.buttonSwitchWeaponWheel = new MCH_GuiOnOffButton(0, x1, y + 100, 150, 20, "Switch Weapon Wheel : ");
      this.listControlButtons.add(new W_GuiButton(50, x1, y + 125, 150, 20, "Render Settings >>"));
      this.listControlButtons.add(new W_GuiButton(51, x1, y + 150, 150, 20, "Key Binding >>"));
      this.listControlButtons.add(new W_GuiButton(55, x2, y + 150, 150, 20, "Development >>"));
      this.buttonTestMode = new MCH_GuiOnOffButton(0, x1, y + 175, 150, 20, "Test Mode : ");
      this.buttonStickModeHeli = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, "Stick Mode Heli : ");
      this.buttonStickModePlane = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, "Stick Mode Plane : ");
      this.buttonThrottleHeli = new MCH_GuiOnOffButton(0, x2, y + 75, 150, 20, "Throttle Down Heli : ");
      this.buttonThrottlePlane = new MCH_GuiOnOffButton(0, x2, y + 100, 150, 20, "Throttle Down Plane : ");
      this.buttonThrottleTank = new MCH_GuiOnOffButton(0, x2, y + 125, 150, 20, "Throttle Down Tank : ");
      this.listControlButtons.add(this.buttonMouseInv);
      this.listControlButtons.add(this.buttonStickModeHeli);
      this.listControlButtons.add(this.buttonStickModePlane);
      this.listControlButtons.add(this.sliderSensitivity);
      this.listControlButtons.add(this.buttonThrottleHeli);
      this.listControlButtons.add(this.buttonThrottlePlane);
      this.listControlButtons.add(this.buttonThrottleTank);
      this.listControlButtons.add(this.buttonTestMode);
      this.listControlButtons.add(this.buttonFlightSimMode);
      this.listControlButtons.add(this.buttonSwitchWeaponWheel);
      Iterator id = this.listControlButtons.iterator();

      W_GuiButton idr;
      while(id.hasNext()) {
         idr = (W_GuiButton)id.next();
         super.buttonList.add(idr);
      }

      this.listRenderButtons = new ArrayList();
      this.buttonShowHUDTP = new MCH_GuiOnOffButton(0, x1, y + 25, 150, 20, "Show HUD Third Person : ");
      this.buttonHideKeyBind = new MCH_GuiOnOffButton(0, x1, y + 50, 150, 20, "Hide Key Binding : ");
      this.sliderHitMark = new MCH_GuiSlider[]{new MCH_GuiSlider(0, x1 + 0, y + 125, 75, 20, "Alpha:%.0f", 0.0F, 0.0F, 255.0F, 16.0F), new MCH_GuiSlider(0, x1 + 75, y + 75, 75, 20, "Red:%.0f", 0.0F, 0.0F, 255.0F, 16.0F), new MCH_GuiSlider(0, x1 + 75, y + 100, 75, 20, "Green:%.0f", 0.0F, 0.0F, 255.0F, 16.0F), new MCH_GuiSlider(0, x1 + 75, y + 125, 75, 20, "Blue:%.0f", 0.0F, 0.0F, 255.0F, 16.0F)};
      this.buttonReplaceCamera = new MCH_GuiOnOffButton(0, x1, y + 150, 150, 20, "Change Camera Pos : ");
      this.listRenderButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));
      this.buttonSmoothShading = new MCH_GuiOnOffButton(0, x2, y + 25, 150, 20, "Smooth Shading : ");
      this.buttonShowEntityMarker = new MCH_GuiOnOffButton(0, x2, y + 50, 150, 20, "Show Entity Maker : ");
      this.sliderEntityMarkerSize = new MCH_GuiSlider(0, x2 + 30, y + 75, 120, 20, "Entity Marker Size:%.0f", 10.0F, 0.0F, 30.0F, 1.0F);
      this.sliderBlockMarkerSize = new MCH_GuiSlider(0, x2 + 60, y + 100, 90, 20, "Block Marker Size:%.0f", 10.0F, 0.0F, 20.0F, 1.0F);
      this.buttonMarkThroughWall = new MCH_GuiOnOffButton(0, x2 + 30, y + 100, 120, 20, "Mark Through Wall : ");
      this.buttonNewExplosion = new MCH_GuiOnOffButton(0, x2, y + 150, 150, 20, "Default Explosion : ");
      this.listRenderButtons.add(this.buttonShowHUDTP);

      for(int var12 = 0; var12 < this.sliderHitMark.length; ++var12) {
         this.listRenderButtons.add(this.sliderHitMark[var12]);
      }

      this.listRenderButtons.add(this.buttonSmoothShading);
      this.listRenderButtons.add(this.buttonHideKeyBind);
      this.listRenderButtons.add(this.buttonShowEntityMarker);
      this.listRenderButtons.add(this.buttonReplaceCamera);
      this.listRenderButtons.add(this.buttonNewExplosion);
      this.listRenderButtons.add(this.sliderEntityMarkerSize);
      this.listRenderButtons.add(this.sliderBlockMarkerSize);
      id = this.listRenderButtons.iterator();

      while(id.hasNext()) {
         idr = (W_GuiButton)id.next();
         super.buttonList.add(idr);
      }

      this.listKeyBindingButtons = new ArrayList();
      this.waitKeyButtonId = 0;
      this.waitKeyAcceptCount = 0;
      this.keyBindingList = new MCH_GuiList(53, 7, x1, y + 25 - 2, 310, 150, "");
      this.listKeyBindingButtons.add(this.keyBindingList);
      this.listKeyBindingButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));
      this.listKeyBindingButtons.add(new W_GuiButton(54, x1 + 90, y + 175, 60, 20, "Reset All"));
      boolean var13 = true;
      boolean var14 = true;
      MCH_GuiListItemKeyBind[] var10000 = new MCH_GuiListItemKeyBind[23];
      MCH_GuiListItemKeyBind var10003 = new MCH_GuiListItemKeyBind(200, 300, x1, "Up", MCH_Config.KeyUp);
      MCH_Config var10009 = MCH_MOD.config;
      var10000[0] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(201, 301, x1, "Down", MCH_Config.KeyDown);
      var10009 = MCH_MOD.config;
      var10000[1] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(202, 302, x1, "Right", MCH_Config.KeyRight);
      var10009 = MCH_MOD.config;
      var10000[2] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(203, 303, x1, "Left", MCH_Config.KeyLeft);
      var10009 = MCH_MOD.config;
      var10000[3] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(204, 304, x1, "Switch Gunner", MCH_Config.KeySwitchMode);
      var10009 = MCH_MOD.config;
      var10000[4] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(205, 305, x1, "Switch Hovering", MCH_Config.KeySwitchHovering);
      var10009 = MCH_MOD.config;
      var10000[5] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(206, 306, x1, "Switch Weapon1", MCH_Config.KeySwitchWeapon1);
      var10009 = MCH_MOD.config;
      var10000[6] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(207, 307, x1, "Switch Weapon2", MCH_Config.KeySwitchWeapon2);
      var10009 = MCH_MOD.config;
      var10000[7] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(208, 308, x1, "Switch Weapon Mode", MCH_Config.KeySwWeaponMode);
      var10009 = MCH_MOD.config;
      var10000[8] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(209, 309, x1, "Zoom / Fold Wing", MCH_Config.KeyZoom);
      var10009 = MCH_MOD.config;
      var10000[9] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(210, 310, x1, "Camera Mode", MCH_Config.KeyCameraMode);
      var10009 = MCH_MOD.config;
      var10000[10] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(211, 311, x1, "Unmount Mobs", MCH_Config.KeyUnmount);
      var10009 = MCH_MOD.config;
      var10000[11] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(212, 312, x1, "Flare", MCH_Config.KeyFlare);
      var10009 = MCH_MOD.config;
      var10000[12] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(213, 313, x1, "Vtol / Drop / Fold Blade", MCH_Config.KeyExtra);
      var10009 = MCH_MOD.config;
      var10000[13] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(214, 314, x1, "Third Person Distance Up", MCH_Config.KeyCameraDistUp);
      var10009 = MCH_MOD.config;
      var10000[14] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(215, 315, x1, "Third Person Distance Down", MCH_Config.KeyCameraDistDown);
      var10009 = MCH_MOD.config;
      var10000[15] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(216, 316, x1, "Switch Free Look", MCH_Config.KeyFreeLook);
      var10009 = MCH_MOD.config;
      var10000[16] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(217, 317, x1, "Open GUI", MCH_Config.KeyGUI);
      var10009 = MCH_MOD.config;
      var10000[17] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(218, 318, x1, "Gear Up Down", MCH_Config.KeyGearUpDown);
      var10009 = MCH_MOD.config;
      var10000[18] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(219, 319, x1, "Put entity in the rack", MCH_Config.KeyPutToRack);
      var10009 = MCH_MOD.config;
      var10000[19] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(220, 320, x1, "Drop entity from the rack", MCH_Config.KeyDownFromRack);
      var10009 = MCH_MOD.config;
      var10000[20] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(221, 321, x1, "[MP]Score board", MCH_Config.KeyScoreboard);
      var10009 = MCH_MOD.config;
      var10000[21] = var10003;
      var10003 = new MCH_GuiListItemKeyBind(222, 322, x1, "[MP][OP]Multiplay manager", MCH_Config.KeyMultiplayManager);
      var10009 = MCH_MOD.config;
      var10000[22] = var10003;
      MCH_GuiListItemKeyBind[] listKeyBindItems = var10000;
      MCH_GuiListItemKeyBind[] i$ = listKeyBindItems;
      int b = listKeyBindItems.length;

      for(int i$1 = 0; i$1 < b; ++i$1) {
         MCH_GuiListItemKeyBind item = i$[i$1];
         this.keyBindingList.addItem(item);
      }

      Iterator var15 = this.listKeyBindingButtons.iterator();

      W_GuiButton var16;
      while(var15.hasNext()) {
         var16 = (W_GuiButton)var15.next();
         super.buttonList.add(var16);
      }

      this.listDevelopButtons = new ArrayList();
      if(Minecraft.getMinecraft().isSingleplayer()) {
         this.buttonReloadAircraftInfo = new W_GuiButton(400, x1, y + 50, 150, 20, "Reload aircraft setting");
         this.buttonReloadWeaponInfo = new W_GuiButton(401, x1, y + 75, 150, 20, "Reload All Weapons");
         this.buttonReloadAllHUD = new W_GuiButton(402, x1, y + 100, 150, 20, "Reload All HUD");
         this.listDevelopButtons.add(this.buttonReloadAircraftInfo);
         this.listDevelopButtons.add(this.buttonReloadWeaponInfo);
         this.listDevelopButtons.add(this.buttonReloadAllHUD);
      }

      this.listDevelopButtons.add(new W_GuiButton(52, x1, y + 175, 90, 20, "Controls <<"));
      var15 = this.listDevelopButtons.iterator();

      while(var15.hasNext()) {
         var16 = (W_GuiButton)var15.next();
         super.buttonList.add(var16);
      }

      super.buttonList.add(new GuiButton(100, x2, y + 175, 80, 20, "Save & Close"));
      super.buttonList.add(new GuiButton(101, x2 + 90, y + 175, 60, 20, "Cancel"));
      this.switchScreen(0);
      this.applySwitchScreen();
      this.getAllStatusFromConfig();
   }

   public boolean canButtonClick() {
      return this.ignoreButtonCounter <= 0;
   }

   public void getAllStatusFromConfig() {
      MCH_Config var10001 = MCH_MOD.config;
      this.buttonMouseInv.setOnOff(MCH_Config.InvertMouse.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonStickModeHeli.setOnOff(MCH_Config.MouseControlStickModeHeli.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonStickModePlane.setOnOff(MCH_Config.MouseControlStickModePlane.prmBool);
      var10001 = MCH_MOD.config;
      this.sliderSensitivity.setSliderValue((float)MCH_Config.MouseSensitivity.prmDouble);
      var10001 = MCH_MOD.config;
      this.buttonShowHUDTP.setOnOff(MCH_Config.DisplayHUDThirdPerson.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonSmoothShading.setOnOff(MCH_Config.SmoothShading.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonHideKeyBind.setOnOff(MCH_Config.HideKeybind.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonShowEntityMarker.setOnOff(MCH_Config.DisplayEntityMarker.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonMarkThroughWall.setOnOff(MCH_Config.DisplayMarkThroughWall.prmBool);
      var10001 = MCH_MOD.config;
      this.sliderEntityMarkerSize.setSliderValue((float)MCH_Config.EntityMarkerSize.prmDouble);
      var10001 = MCH_MOD.config;
      this.sliderBlockMarkerSize.setSliderValue((float)MCH_Config.BlockMarkerSize.prmDouble);
      var10001 = MCH_MOD.config;
      this.buttonReplaceCamera.setOnOff(MCH_Config.ReplaceRenderViewEntity.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonNewExplosion.setOnOff(MCH_Config.DefaultExplosionParticle.prmBool);
      MCH_GuiSlider var10000 = this.sliderHitMark[0];
      var10001 = MCH_MOD.config;
      var10000.setSliderValue(MCH_Config.hitMarkColorAlpha * 255.0F);
      var10000 = this.sliderHitMark[1];
      var10001 = MCH_MOD.config;
      var10000.setSliderValue((float)(MCH_Config.hitMarkColorRGB >> 16 & 255));
      var10000 = this.sliderHitMark[2];
      var10001 = MCH_MOD.config;
      var10000.setSliderValue((float)(MCH_Config.hitMarkColorRGB >> 8 & 255));
      var10000 = this.sliderHitMark[3];
      var10001 = MCH_MOD.config;
      var10000.setSliderValue((float)(MCH_Config.hitMarkColorRGB >> 0 & 255));
      var10001 = MCH_MOD.config;
      this.buttonThrottleHeli.setOnOff(MCH_Config.AutoThrottleDownHeli.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonThrottlePlane.setOnOff(MCH_Config.AutoThrottleDownPlane.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonThrottleTank.setOnOff(MCH_Config.AutoThrottleDownTank.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonTestMode.setOnOff(MCH_Config.TestMode.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonFlightSimMode.setOnOff(MCH_Config.MouseControlFlightSimMode.prmBool);
      var10001 = MCH_MOD.config;
      this.buttonSwitchWeaponWheel.setOnOff(MCH_Config.SwitchWeaponWithMouseWheel.prmBool);
   }

   public void saveAndApplyConfig() {
      MCH_Config var10000;
      label20: {
         boolean n = false;
         var10000 = MCH_MOD.config;
         MCH_Config.InvertMouse.setPrm(this.buttonMouseInv.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.MouseControlStickModeHeli.setPrm(this.buttonStickModeHeli.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.MouseControlStickModePlane.setPrm(this.buttonStickModePlane.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.MouseControlFlightSimMode.setPrm(this.buttonFlightSimMode.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.SwitchWeaponWithMouseWheel.setPrm(this.buttonSwitchWeaponWheel.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.MouseSensitivity.setPrm((double)this.sliderSensitivity.getSliderValueInt(1));
         var10000 = MCH_MOD.config;
         MCH_Config.DisplayHUDThirdPerson.setPrm(this.buttonShowHUDTP.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.SmoothShading.setPrm(this.buttonSmoothShading.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.HideKeybind.setPrm(this.buttonHideKeyBind.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.DisplayEntityMarker.setPrm(this.buttonShowEntityMarker.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.DisplayMarkThroughWall.setPrm(this.buttonMarkThroughWall.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.EntityMarkerSize.setPrm((double)this.sliderEntityMarkerSize.getSliderValueInt(1));
         var10000 = MCH_MOD.config;
         MCH_Config.BlockMarkerSize.setPrm((double)this.sliderBlockMarkerSize.getSliderValueInt(1));
         var10000 = MCH_MOD.config;
         MCH_Config.ReplaceRenderViewEntity.setPrm(this.buttonReplaceCamera.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.DefaultExplosionParticle.setPrm(this.buttonNewExplosion.getOnOff());
         float a = this.sliderHitMark[0].getSliderValue();
         int r = (int)this.sliderHitMark[1].getSliderValue();
         int g = (int)this.sliderHitMark[2].getSliderValue();
         int b = (int)this.sliderHitMark[3].getSliderValue();
         var10000 = MCH_MOD.config;
         MCH_Config.hitMarkColorAlpha = a / 255.0F;
         var10000 = MCH_MOD.config;
         MCH_Config.hitMarkColorRGB = r << 16 | g << 8 | b;
         var10000 = MCH_MOD.config;
         MCH_Config.HitMarkColor.setPrm(String.format("%d, %d, %d, %d", new Object[]{Integer.valueOf((int)a), Integer.valueOf(r), Integer.valueOf(g), Integer.valueOf(b)}));
         var10000 = MCH_MOD.config;
         boolean b1 = MCH_Config.AutoThrottleDownHeli.prmBool;
         var10000 = MCH_MOD.config;
         boolean b2 = MCH_Config.AutoThrottleDownPlane.prmBool;
         var10000 = MCH_MOD.config;
         MCH_Config.AutoThrottleDownHeli.setPrm(this.buttonThrottleHeli.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.AutoThrottleDownPlane.setPrm(this.buttonThrottlePlane.getOnOff());
         var10000 = MCH_MOD.config;
         MCH_Config.AutoThrottleDownTank.setPrm(this.buttonThrottleTank.getOnOff());
         MCH_Config var10001 = MCH_MOD.config;
         if(b1 == MCH_Config.AutoThrottleDownHeli.prmBool) {
            var10001 = MCH_MOD.config;
            if(b2 == MCH_Config.AutoThrottleDownPlane.prmBool) {
               break label20;
            }
         }

         this.sendClientSettings();
      }

      for(int i = 0; i < this.keyBindingList.getItemNum(); ++i) {
         ((MCH_GuiListItemKeyBind)this.keyBindingList.getItem(i)).applyKeycode();
      }

      MCH_ClientCommonTickHandler.instance.updatekeybind(MCH_MOD.config);
      var10000 = MCH_MOD.config;
      MCH_Config.TestMode.setPrm(this.buttonTestMode.getOnOff());
      MCH_MOD.config.write();
   }

   public void switchScreen(int screenID) {
      this.waitKeyButtonId = 0;
      this.currentScreenId = screenID;
      Iterator i$ = this.listControlButtons.iterator();

      W_GuiButton b;
      while(i$.hasNext()) {
         b = (W_GuiButton)i$.next();
         b.setVisible(false);
      }

      i$ = this.listRenderButtons.iterator();

      while(i$.hasNext()) {
         b = (W_GuiButton)i$.next();
         b.setVisible(false);
      }

      i$ = this.listKeyBindingButtons.iterator();

      while(i$.hasNext()) {
         b = (W_GuiButton)i$.next();
         b.setVisible(false);
      }

      i$ = this.listDevelopButtons.iterator();

      while(i$.hasNext()) {
         b = (W_GuiButton)i$.next();
         b.setVisible(false);
      }

      this.ignoreButtonCounter = 3;
   }

   public void applySwitchScreen() {
      Iterator i$;
      W_GuiButton b;
      switch(this.currentScreenId) {
      case 0:
      default:
         i$ = this.listControlButtons.iterator();

         while(i$.hasNext()) {
            b = (W_GuiButton)i$.next();
            b.setVisible(true);
         }

         return;
      case 1:
         i$ = this.listRenderButtons.iterator();

         while(i$.hasNext()) {
            b = (W_GuiButton)i$.next();
            b.setVisible(true);
         }

         return;
      case 2:
         i$ = this.listKeyBindingButtons.iterator();

         while(i$.hasNext()) {
            b = (W_GuiButton)i$.next();
            b.setVisible(true);
         }

         return;
      case 3:
         i$ = this.listDevelopButtons.iterator();

         while(i$.hasNext()) {
            b = (W_GuiButton)i$.next();
            b.setVisible(true);
         }

      }
   }

   public void sendClientSettings() {
      if(super.mc.thePlayer != null) {
         MCH_EntityAircraft ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(super.mc.thePlayer);
         if(ac != null) {
            int seatId = ac.getSeatIdByEntity(super.mc.thePlayer);
            if(seatId == 0) {
               ac.updateClientSettings(seatId);
            }
         }
      }

   }

   public void keyTyped(char a, int code) {
      if(this.waitKeyButtonId != 0) {
         if(code != 1) {
            super.keyTyped(a, code);
         }

         this.acceptKeycode(code);
         this.waitKeyButtonId = 0;
      } else {
         super.keyTyped(a, code);
      }

   }

   protected void mouseClicked(int par1, int par2, int par3) {
      super.mouseClicked(par1, par2, par3);
      if(this.waitKeyButtonId != 0 && this.waitKeyAcceptCount == 0) {
         this.acceptKeycode(par3 - 100);
         this.waitKeyButtonId = 0;
      }

   }

   public void acceptKeycode(int code) {
      if(code != 1 && super.mc.currentScreen instanceof MCH_ConfigGui) {
         MCH_GuiListItemKeyBind kb = (MCH_GuiListItemKeyBind)this.keyBindingList.getItem(this.waitKeyButtonId - 200);
         if(kb != null) {
            kb.setKeycode(code);
         }
      }

   }

   public void handleMouseInput() {
      super.handleMouseInput();
      if(this.waitKeyButtonId == 0) {
         int var16 = Mouse.getEventDWheel();
         if(var16 != 0) {
            if(var16 > 0) {
               this.keyBindingList.scrollDown(2.0F);
            } else if(var16 < 0) {
               this.keyBindingList.scrollUp(2.0F);
            }
         }

      }
   }

   public void updateScreen() {
      super.updateScreen();
      if(this.waitKeyAcceptCount > 0) {
         --this.waitKeyAcceptCount;
      }

      if(this.ignoreButtonCounter > 0) {
         --this.ignoreButtonCounter;
         if(this.ignoreButtonCounter == 0) {
            this.applySwitchScreen();
         }
      }

   }

   public void onGuiClosed() {
      super.onGuiClosed();
   }

   protected void actionPerformed(GuiButton button) {
      try {
         super.actionPerformed(button);
         if(!button.enabled) {
            return;
         }

         if(this.waitKeyButtonId != 0) {
            return;
         }

         if(!this.canButtonClick()) {
            return;
         }

         MCH_EntityAircraft ac;
         switch(button.id) {
         case 50:
            this.switchScreen(1);
            break;
         case 51:
            this.switchScreen(2);
            break;
         case 52:
            this.switchScreen(0);
            break;
         case 53:
            MCH_GuiListItem e = this.keyBindingList.lastPushItem;
            if(e != null) {
               MCH_GuiListItemKeyBind var10 = (MCH_GuiListItemKeyBind)e;
               if(var10.lastPushButton != null) {
                  int var11 = this.keyBindingList.getItemNum();
                  if(var10.lastPushButton.id >= 200 && var10.lastPushButton.id < 200 + var11) {
                     this.waitKeyButtonId = var10.lastPushButton.id;
                     this.waitKeyAcceptCount = 5;
                  } else if(var10.lastPushButton.id >= 300 && var10.lastPushButton.id < 300 + var11) {
                     var10.resetKeycode();
                  }

                  var10.lastPushButton = null;
               }
            }
            break;
         case 54:
            for(int var8 = 0; var8 < this.keyBindingList.getItemNum(); ++var8) {
               ((MCH_GuiListItemKeyBind)this.keyBindingList.getItem(var8)).resetKeycode();
            }

            return;
         case 55:
            this.switchScreen(3);
            break;
         case 100:
            this.saveAndApplyConfig();
            super.mc.thePlayer.closeScreen();
            break;
         case 101:
            super.mc.thePlayer.closeScreen();
            break;
         case 401:
            MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload all weapon info.", new Object[0]);
            MCH_PacketNotifyInfoReloaded.sendRealodAllWeapon();
            MCH_WeaponInfoManager.reload();
            List list = super.mc.theWorld.loadedEntityList;

            for(int i = 0; i < list.size(); ++i) {
               if(list.get(i) instanceof MCH_EntityAircraft) {
                  ac = (MCH_EntityAircraft)list.get(i);
                  if(ac.getAcInfo() != null) {
                     ac.getAcInfo().reload();
                     ac.changeType(ac.getAcInfo().name);
                     ac.onAcInfoReloaded();
                  }
               }
            }

            super.mc.thePlayer.closeScreen();
            break;
         case 402:
            MCH_MOD.proxy.reloadHUD();
         case 400:
            ac = MCH_EntityAircraft.getAircraft_RiddenOrControl(this.thePlayer);
            if(ac != null && ac.getAcInfo() != null) {
               String var9 = ac.getAcInfo().name;
               MCH_Lib.DbgLog(true, "MCH_BaseInfo.reload : " + var9, new Object[0]);
               List var12 = super.mc.theWorld.loadedEntityList;

               for(int i1 = 0; i1 < var12.size(); ++i1) {
                  if(var12.get(i1) instanceof MCH_EntityAircraft) {
                     ac = (MCH_EntityAircraft)var12.get(i1);
                     if(ac.getAcInfo() != null && ac.getAcInfo().name.equals(var9)) {
                        ac.getAcInfo().reload();
                        ac.changeType(var9);
                        ac.onAcInfoReloaded();
                     }
                  }
               }

               MCH_PacketNotifyInfoReloaded.sendRealodAc();
            }

            super.mc.thePlayer.closeScreen();
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

   }

   public boolean doesGuiPauseGame() {
      return true;
   }

   protected void drawGuiContainerForegroundLayer(int par1, int par2) {
      super.drawGuiContainerForegroundLayer(par1, par2);
      this.drawString("MC Helicopter MOD Options", 10, 10, 16777215);
      if(this.currentScreenId == 0) {
         this.drawString("< Controls >", 170, 10, 16777215);
      } else if(this.currentScreenId == 1) {
         this.drawString("< Render >", 170, 10, 16777215);
         this.drawString("Hit Mark", 10, 75, 16777215);
         byte ignoreItems = 0;
         int var11 = ignoreItems | (int)this.sliderHitMark[0].getSliderValue() << 24;
         var11 |= (int)this.sliderHitMark[1].getSliderValue() << 16;
         var11 |= (int)this.sliderHitMark[2].getSliderValue() << 8;
         var11 |= (int)this.sliderHitMark[3].getSliderValue() << 0;
         this.drawSampleHitMark(40, 105, var11);
         double y = (double)this.sliderEntityMarkerSize.getSliderValue();
         double len$ = 170.0D + (30.0D - y) / 2.0D;
         double s = (double)(this.sliderEntityMarkerSize.yPosition - this.sliderEntityMarkerSize.getHeight());
         double[] ls = new double[]{len$ + y, s, len$, s, len$ + y / 2.0D, s + y};
         this.drawLine(ls, -65536, 4);
         y = (double)this.sliderBlockMarkerSize.getSliderValue();
         len$ = 185.0D;
         s = (double)this.sliderBlockMarkerSize.yPosition;
         var11 = -65536;
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glDisable(3553);
         GL11.glBlendFunc(770, 771);
         GL11.glColor4ub((byte)(var11 >> 16 & 255), (byte)(var11 >> 8 & 255), (byte)(var11 >> 0 & 255), (byte)(var11 >> 24 & 255));
         Tessellator.instance.startDrawing(1);
         MCH_GuiTargetMarker.drawRhombus(Tessellator.instance, 15, len$, s, (double)super.zLevel, y, var11);
         Tessellator.instance.draw();
         GL11.glEnable(3553);
         GL11.glDisable(3042);
         GL11.glColor4b((byte)-1, (byte)-1, (byte)-1, (byte)-1);
         GL11.glPopMatrix();
      } else {
         int var12;
         if(this.currentScreenId == 2) {
            this.drawString("< Key Binding >", 170, 10, 16777215);
            if(this.waitKeyButtonId != 0) {
               drawRect(30, 30, super.xSize - 30, super.ySize - 30, -533712848);
               String var13 = "Please ant key or mouse button.";
               var12 = this.getStringWidth(var13);
               this.drawString(var13, (super.xSize - var12) / 2, super.ySize / 2 - 4, 16777215);
            }
         } else if(this.currentScreenId == 3) {
            this.drawString("< Development >", 170, 10, 16777215);
            this.drawString("Single player only!", 10, 30, 16711680);
            if(this.buttonReloadAircraftInfo != null && this.buttonReloadAircraftInfo.isOnMouseOver()) {
               this.drawString("The following items are not reload.", 170, 30, 16777215);
               String[] var14 = MCH_AircraftInfo.getCannotReloadItem();
               var12 = 10;
               String[] arr$ = var14;
               int var15 = var14.length;

               for(int i$ = 0; i$ < var15; ++i$) {
                  String var16 = arr$[i$];
                  this.drawString("  " + var16, 170, 30 + var12, 16777215);
                  var12 += 10;
               }
            }
         }
      }

   }

   protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
      W_ScaledResolution scaledresolution = new W_ScaledResolution(super.mc, super.mc.displayWidth, super.mc.displayHeight);
      this.scaleFactor = scaledresolution.getScaleFactor();
      W_McClient.MOD_bindTexture("textures/gui/config.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int x = (super.width - super.xSize) / 2;
      int y = (super.height - super.ySize) / 2;
      this.drawTexturedModalRectRotate((double)x, (double)y, (double)super.xSize, (double)super.ySize, 0.0D, 0.0D, (double)super.xSize, (double)super.ySize, 0.0F, 512.0D, 256.0D);
   }

   public void drawSampleHitMark(int x, int y, int color) {
      byte IVX = 10;
      byte IVY = 10;
      byte SZX = 5;
      byte SZY = 5;
      double[] ls = new double[]{(double)(x - IVX), (double)(y - IVY), (double)(x - SZX), (double)(y - SZY), (double)(x - IVX), (double)(y + IVY), (double)(x - SZX), (double)(y + SZY), (double)(x + IVX), (double)(y - IVY), (double)(x + SZX), (double)(y - SZY), (double)(x + IVX), (double)(y + IVY), (double)(x + SZX), (double)(y + SZY)};
      this.drawLine(ls, color, 1);
   }

   public void drawLine(double[] line, int color, int mode) {
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4ub((byte)(color >> 16 & 255), (byte)(color >> 8 & 255), (byte)(color >> 0 & 255), (byte)(color >> 24 & 255));
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawing(mode);

      for(int i = 0; i < line.length; i += 2) {
         tessellator.addVertex(line[i + 0], line[i + 1], (double)super.zLevel);
      }

      tessellator.draw();
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glColor4b((byte)-1, (byte)-1, (byte)-1, (byte)-1);
      GL11.glPopMatrix();
   }

   public void drawTexturedModalRectRotate(double left, double top, double width, double height, double uLeft, double vTop, double uWidth, double vHeight, float rot, double texWidth, double texHeight) {
      GL11.glPushMatrix();
      GL11.glTranslated(left + width / 2.0D, top + height / 2.0D, 0.0D);
      GL11.glRotatef(rot, 0.0F, 0.0F, 1.0F);
      float fw = (float)(1.0D / texWidth);
      float fh = (float)(1.0D / texHeight);
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV(-width / 2.0D, height / 2.0D, (double)super.zLevel, uLeft * (double)fw, (vTop + vHeight) * (double)fh);
      tessellator.addVertexWithUV(width / 2.0D, height / 2.0D, (double)super.zLevel, (uLeft + uWidth) * (double)fw, (vTop + vHeight) * (double)fh);
      tessellator.addVertexWithUV(width / 2.0D, -height / 2.0D, (double)super.zLevel, (uLeft + uWidth) * (double)fw, vTop * (double)fh);
      tessellator.addVertexWithUV(-width / 2.0D, -height / 2.0D, (double)super.zLevel, uLeft * (double)fw, vTop * (double)fh);
      tessellator.draw();
      GL11.glPopMatrix();
   }
}
