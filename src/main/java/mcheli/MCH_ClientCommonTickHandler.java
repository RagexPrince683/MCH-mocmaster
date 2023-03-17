package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.aircraft.*;
import mcheli.command.MCH_GuiTitle;
import mcheli.gltd.MCH_ClientGLTDTickHandler;
import mcheli.gltd.MCH_EntityGLTD;
import mcheli.gltd.MCH_GuiGLTD;
import mcheli.gui.MCH_Gui;
import mcheli.helicopter.MCH_ClientHeliTickHandler;
import mcheli.helicopter.MCH_EntityHeli;
import mcheli.helicopter.MCH_GuiHeli;
import mcheli.lweapon.MCH_ClientLightWeaponTickHandler;
import mcheli.lweapon.MCH_GuiLightWeapon;
import mcheli.multiplay.MCH_GuiScoreboard;
import mcheli.multiplay.MCH_GuiTargetMarker;
import mcheli.multiplay.MCH_MultiplayClient;
import mcheli.plane.MCP_ClientPlaneTickHandler;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_GuiPlane;
import mcheli.tank.MCH_ClientTankTickHandler;
import mcheli.tank.MCH_EntityTank;
import mcheli.tank.MCH_GuiTank;
import mcheli.tool.MCH_ClientToolTickHandler;
import mcheli.tool.MCH_GuiWrench;
import mcheli.tool.MCH_ItemWrench;
import mcheli.tool.rangefinder.MCH_GuiRangeFinder;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.vehicle.MCH_ClientVehicleTickHandler;
import mcheli.vehicle.MCH_EntityVehicle;
import mcheli.vehicle.MCH_GuiVehicle;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;

import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {

   public static MCH_ClientCommonTickHandler instance;
   public MCH_GuiCommon gui_Common;
   public MCH_Gui gui_Heli;
   public MCH_Gui gui_Plane;
   public MCH_Gui gui_Tank;
   public MCH_Gui gui_GLTD;
   public MCH_Gui gui_Vehicle;
   public MCH_Gui gui_LWeapon;
   public MCH_Gui gui_Wrench;
   public MCH_Gui gui_EMarker;
   public MCH_Gui gui_RngFndr;
   public MCH_Gui gui_Title;
   public MCH_Gui[] guis;
   public MCH_Gui[] guiTicks;
   public MCH_ClientTickHandlerBase[] ticks;
   public MCH_Key[] Keys;
   public MCH_Key KeyCamDistUp;
   public MCH_Key KeyCamDistDown;
   public MCH_Key KeyScoreboard;
   public MCH_Key KeyMultiplayManager;
   public static int cameraMode = 0;
   public static MCH_EntityAircraft ridingAircraft = null;
   public static boolean isDrawScoreboard = false;
   public static int sendLDCount = 0;
   public static boolean isLocked = false;
   public static int lockedSoundCount = 0;
   int debugcnt;
   private static double prevMouseDeltaX;
   private static double prevMouseDeltaY;
   private static double mouseDeltaX = 0.0D;
   private static double mouseDeltaY = 0.0D;
   private static double mouseRollDeltaX = 0.0D;
   private static double mouseRollDeltaY = 0.0D;
   private static boolean isRideAircraft = false;
   private static float prevTick = 0.0F;


   public MCH_ClientCommonTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft);
      this.gui_Common = new MCH_GuiCommon(minecraft);
      this.gui_Heli = new MCH_GuiHeli(minecraft);
      this.gui_Plane = new MCP_GuiPlane(minecraft);
      this.gui_Tank = new MCH_GuiTank(minecraft);
      this.gui_GLTD = new MCH_GuiGLTD(minecraft);
      this.gui_Vehicle = new MCH_GuiVehicle(minecraft);
      this.gui_LWeapon = new MCH_GuiLightWeapon(minecraft);
      this.gui_Wrench = new MCH_GuiWrench(minecraft);
      this.gui_RngFndr = new MCH_GuiRangeFinder(minecraft);
      this.gui_EMarker = new MCH_GuiTargetMarker(minecraft);
      this.gui_Title = new MCH_GuiTitle(minecraft);
      this.guis = new MCH_Gui[]{this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle};
      this.guiTicks = new MCH_Gui[]{this.gui_Common, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle, this.gui_LWeapon, this.gui_Wrench, this.gui_RngFndr, this.gui_EMarker, this.gui_Title};
      this.ticks = new MCH_ClientTickHandlerBase[]{new MCH_ClientHeliTickHandler(minecraft, config), new MCP_ClientPlaneTickHandler(minecraft, config), new MCH_ClientTankTickHandler(minecraft, config), new MCH_ClientGLTDTickHandler(minecraft, config), new MCH_ClientVehicleTickHandler(minecraft, config), new MCH_ClientLightWeaponTickHandler(minecraft, config), new MCH_ClientSeatTickHandler(minecraft, config), new MCH_ClientToolTickHandler(minecraft, config)};
      this.updatekeybind(config);
   }

   public void updatekeybind(MCH_Config config) {
      this.KeyCamDistUp = new MCH_Key(MCH_Config.KeyCameraDistUp.prmInt);
      this.KeyCamDistDown = new MCH_Key(MCH_Config.KeyCameraDistDown.prmInt);
      this.KeyScoreboard = new MCH_Key(MCH_Config.KeyScoreboard.prmInt);
      this.KeyMultiplayManager = new MCH_Key(MCH_Config.KeyMultiplayManager.prmInt);
      this.Keys = new MCH_Key[]{this.KeyCamDistUp, this.KeyCamDistDown, this.KeyScoreboard, this.KeyMultiplayManager};
      MCH_ClientTickHandlerBase[] arr$ = this.ticks;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MCH_ClientTickHandlerBase t = arr$[i$];
         t.updateKeybind(config);
      }

   }

   public String getLabel() {
      return null;
   }

   public void onTick() {
      MCH_ClientTickHandlerBase.initRotLimit();
      MCH_Key[] player = this.Keys;
      int inOtherGui = player.length;

      for(int ac = 0; ac < inOtherGui; ++ac) {
         MCH_Key len$ = player[ac];
         len$.update();
      }

      EntityClientPlayerMP var7 = super.mc.thePlayer;
      if(var7 != null && super.mc.currentScreen == null) {
         if(MCH_ServerSettings.enableCamDistChange && (this.KeyCamDistUp.isKeyDown() || this.KeyCamDistDown.isKeyDown())) {
            inOtherGui = (int)W_Reflection.getThirdPersonDistance();
            if(this.KeyCamDistUp.isKeyDown() && inOtherGui < 60) {
               inOtherGui += 4;
               if(inOtherGui > 60) {
                  inOtherGui = 60;
               }

               W_Reflection.setThirdPersonDistance((float)inOtherGui);
            } else if(this.KeyCamDistDown.isKeyDown()) {
               inOtherGui -= 4;
               if(inOtherGui < 4) {
                  inOtherGui = 4;
               }

               W_Reflection.setThirdPersonDistance((float)inOtherGui);
            }
         }

         if(super.mc.currentScreen == null) {
            label85: {
               if(super.mc.isSingleplayer()) {
                  MCH_Config var10000 = MCH_MOD.config;
                  if(!MCH_Config.DebugLog) {
                     break label85;
                  }
               }

               isDrawScoreboard = this.KeyScoreboard.isKeyPress();
               if(!isDrawScoreboard && this.KeyMultiplayManager.isKeyDown()) {
                  MCH_PacketIndOpenScreen.send(5);
               }
            }
         }
      }

      if(sendLDCount < 10) {
         ++sendLDCount;
      } else {
         MCH_MultiplayClient.sendImageData();
         sendLDCount = 0;
      }

      boolean var12 = super.mc.currentScreen != null;
      MCH_ClientTickHandlerBase[] var8 = this.ticks;
      int var10 = var8.length;

      int i$;
      for(i$ = 0; i$ < var10; ++i$) {
         MCH_ClientTickHandlerBase g = var8[i$];
         g.onTick(var12);
      }

      MCH_Gui[] var9 = this.guiTicks;
      var10 = var9.length;

      for(i$ = 0; i$ < var10; ++i$) {
         MCH_Gui var13 = var9[i$];
         var13.onTick();
      }

      MCH_EntityAircraft var11 = MCH_EntityAircraft.getAircraft_RiddenOrControl(var7);
      if(var7 != null && var11 != null && !var11.isDestroyed()) {
         if(isLocked && lockedSoundCount == 0) {
            isLocked = false;
            lockedSoundCount = 20;
            MCH_ClientTickHandlerBase.playSound("locked");
         }
      } else {
         lockedSoundCount = 0;
         isLocked = false;
      }

      if(lockedSoundCount > 0) {
         --lockedSoundCount;
      }

   }

   public void onTickPre() {
      if(super.mc.thePlayer != null && super.mc.theWorld != null) {
         this.onTick();
      }

   }

   public void onTickPost() {
      if(super.mc.thePlayer != null && super.mc.theWorld != null) {
         MCH_GuiTargetMarker.onClientTick();
      }

   }

   public static double getCurrentStickX() {
      return mouseRollDeltaX;
   }

   public static double getCurrentStickY() {
      double inv = 1.0D;
      if(Minecraft.getMinecraft().gameSettings.invertMouse) {
         inv = -inv;
      }

      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.InvertMouse.prmBool) {
         inv = -inv;
      }

      return mouseRollDeltaY * inv;
   }

   public static double getMaxStickLength() {
      return 40.0D;
   }

   public void updateMouseDelta(boolean stickMode, float partialTicks) {
      prevMouseDeltaX = mouseDeltaX;
      prevMouseDeltaY = mouseDeltaY;
      mouseDeltaX = 0.0D;
      mouseDeltaY = 0.0D;
      if(super.mc.inGameHasFocus && Display.isActive() && super.mc.currentScreen == null) {
         if(stickMode) {
            if(Math.abs(mouseRollDeltaX) < getMaxStickLength() * 0.2D) {
               mouseRollDeltaX *= (double)(1.0F - 0.15F * partialTicks);
            }

            if(Math.abs(mouseRollDeltaY) < getMaxStickLength() * 0.2D) {
               mouseRollDeltaY *= (double)(1.0F - 0.15F * partialTicks);
            }
         }

         super.mc.mouseHelper.mouseXYChange();
         float f1 = super.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float f2 = f1 * f1 * f1 * 8.0F;
         MCH_Config var10000 = MCH_MOD.config;
         double ms = MCH_Config.MouseSensitivity.prmDouble * 0.1D;
         mouseDeltaX = ms * (double)super.mc.mouseHelper.deltaX * (double)f2;
         mouseDeltaY = ms * (double)super.mc.mouseHelper.deltaY * (double)f2;
         byte inv = 1;
         if(super.mc.gameSettings.invertMouse) {
            inv = -1;
         }

         var10000 = MCH_MOD.config;
         if(MCH_Config.InvertMouse.prmBool) {
            inv *= -1;
         }

         mouseRollDeltaX += mouseDeltaX;
         mouseRollDeltaY += mouseDeltaY * (double)inv;
         double dist = mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY;
         if(dist > 1.0D) {
            dist = (double)MathHelper.sqrt_double(dist);
            double d = dist;
            if(dist > getMaxStickLength()) {
               d = getMaxStickLength();
            }

            mouseRollDeltaX /= dist;
            mouseRollDeltaY /= dist;
            mouseRollDeltaX *= d;
            mouseRollDeltaY *= d;
         }
      }

   }

   public void renderCompass(){
      boolean hasCompass = true;

      if(hasCompass){
        // MCH_HudItemGraduation yaw = new MCH_HudItemGraduation(0, 0, "plyr_yaw", "0", "0", "-100");
         //yaw.drawCommonGraduationYaw(Minecraft.getMinecraft().thePlayer.rotationYaw, 0,  0, -100);
      }
   }

   public void onRenderTickPre(float partialTicks) {
      renderCompass();

      MCH_GuiTargetMarker.clearMarkEntityPos();
      if(!MCH_ServerSettings.enableDebugBoundingBox) {
         RenderManager.debugBoundingBox = false;
      }

      MCH_ClientEventHook.haveSearchLightAircraft.clear();
      if(super.mc != null && super.mc.theWorld != null) {
         Iterator player = Minecraft.getMinecraft().theWorld.loadedEntityList.iterator();

         while(player.hasNext()) {
            Object currentItemstack = player.next();
            if(currentItemstack instanceof MCH_EntityAircraft && ((MCH_EntityAircraft)currentItemstack).haveSearchLight()) {
               MCH_ClientEventHook.haveSearchLightAircraft.add((MCH_EntityAircraft)currentItemstack);
            }
         }
      }

      if(!W_McClient.isGamePaused()) {
         EntityClientPlayerMP var17 = super.mc.thePlayer;
         if(var17 != null) {
            ItemStack var18 = var17.getCurrentEquippedItem();
            if(var18 != null && var18.getItem() instanceof MCH_ItemWrench && var17.getItemInUseCount() > 0) {
               W_Reflection.setItemRendererProgress(1.0F);
            }

            ridingAircraft = MCH_EntityAircraft.getAircraft_RiddenOrControl(var17);
            if(ridingAircraft != null) {
               cameraMode = ridingAircraft.getCameraMode(var17);
            } else if(var17.ridingEntity instanceof MCH_EntityGLTD) {
               MCH_EntityGLTD ac = (MCH_EntityGLTD)var17.ridingEntity;
               cameraMode = ac.camera.getMode(0);
            } else {
               cameraMode = 0;
            }

            MCH_EntityAircraft var19 = null;
            if(!(var17.ridingEntity instanceof MCH_EntityHeli) && !(var17.ridingEntity instanceof MCP_EntityPlane) && !(var17.ridingEntity instanceof MCH_EntityTank)) {
               if(var17.ridingEntity instanceof MCH_EntityUavStation) {
                  var19 = ((MCH_EntityUavStation)var17.ridingEntity).getControlAircract();
               } else if(var17.ridingEntity instanceof MCH_EntityVehicle) {
                  MCH_EntityAircraft stickMode = (MCH_EntityAircraft)var17.ridingEntity;
                  stickMode.setupAllRiderRenderPosition(partialTicks, var17);
               }
            } else {
               var19 = (MCH_EntityAircraft)var17.ridingEntity;
            }

            boolean var20 = false;
            MCH_Config var10000;
            if(var19 instanceof MCH_EntityHeli) {
               var10000 = MCH_MOD.config;
               var20 = MCH_Config.MouseControlStickModeHeli.prmBool;
            }

            if(var19 instanceof MCP_EntityPlane) {
               var10000 = MCH_MOD.config;
               var20 = MCH_Config.MouseControlStickModePlane.prmBool;
            }

            for(int de = 0; de < 10 && prevTick > partialTicks; ++de) {
               --prevTick;
            }

            float p;
            float r;
            if(var19 != null && var19.canMouseRot()) {
               if(!isRideAircraft) {
                  var19.onInteractFirst(var17);
               }

               isRideAircraft = true;
               this.updateMouseDelta(var20, partialTicks);
               boolean var22 = false;
               float var23 = 0.0F;
               float var25 = 0.0F;
               MCH_SeatInfo var26 = var19.getSeatInfo(var17);
               if(var26 != null && var26.fixRot && var19.getIsGunnerMode(var17) && !var19.isGunnerLookMode(var17)) {
                  var22 = true;
                  var23 = var26.fixYaw;
                  var25 = var26.fixPitch;
                  mouseRollDeltaX *= 0.0D;
                  mouseRollDeltaY *= 0.0D;
                  mouseDeltaX *= 0.0D;
                  mouseDeltaY *= 0.0D;
               } else if(var19.isPilot(var17)) {
                  MCH_AircraftInfo.CameraPosition var28 = var19.getCameraPosInfo();
                  if(var28 != null) {
                     var23 = var28.yaw;
                     var25 = var28.pitch;
                  }
               }

               if(var19.getAcInfo() == null) {
                  var17.setAngles((float)mouseDeltaX, (float)mouseDeltaY);
               } else {
                  var19.setAngles(var17, var22, var23, var25, (float)(mouseDeltaX + prevMouseDeltaX) / 2.0F, (float)(mouseDeltaY + prevMouseDeltaY) / 2.0F, (float)mouseRollDeltaX, (float)mouseRollDeltaY, partialTicks - prevTick);
               }

               var19.setupAllRiderRenderPosition(partialTicks, var17);
               double var29 = (double)MathHelper.sqrt_double(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
               if(!var20 || var29 < getMaxStickLength() * 0.1D) {
                  mouseRollDeltaX *= 0.95D;
                  mouseRollDeltaY *= 0.95D;
               }

               p = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
               r = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
               p *= MathHelper.cos((float)((double)r * 3.141592653589793D / 180.0D));
               if(var19.getTVMissile() != null && W_Lib.isClientPlayer(var19.getTVMissile().shootingEntity) && var19.getIsGunnerMode(var17)) {
                  p = 0.0F;
               }

               W_Reflection.setCameraRoll(p);
               this.correctViewEntityDummy(var17);
            } else {
               MCH_EntitySeat var21 = var17.ridingEntity instanceof MCH_EntitySeat?(MCH_EntitySeat)var17.ridingEntity:null;
               if(var21 != null && var21.getParent() != null) {
                  this.updateMouseDelta(var20, partialTicks);
                  var19 = var21.getParent();
                  boolean wi = false;
                  MCH_SeatInfo seatInfo = var19.getSeatInfo(var17);
                  if(seatInfo != null && seatInfo.fixRot && var19.getIsGunnerMode(var17) && !var19.isGunnerLookMode(var17)) {
                     wi = true;
                     mouseRollDeltaX *= 0.0D;
                     mouseRollDeltaY *= 0.0D;
                     mouseDeltaX *= 0.0D;
                     mouseDeltaY *= 0.0D;
                  }

                  Vec3 v = Vec3.createVectorHelper(mouseDeltaX, mouseRollDeltaY, 0.0D);
                  W_Vec3.rotateAroundZ((float)((double)(var19.calcRotRoll(partialTicks) / 180.0F) * 3.141592653589793D), v);
                  MCH_WeaponSet ws = var19.getCurrentWeapon(var17);
                  mouseDeltaY *= ws != null && ws.getInfo() != null?(double)ws.getInfo().cameraRotationSpeedPitch:1.0D;
                  var17.setAngles((float)mouseDeltaX, (float)mouseDeltaY);
                  float y = var19.getRotYaw();
                  p = var19.getRotPitch();
                  r = var19.getRotRoll();
                  var19.setRotYaw(var19.calcRotYaw(partialTicks));
                  var19.setRotPitch(var19.calcRotPitch(partialTicks));
                  var19.setRotRoll(var19.calcRotRoll(partialTicks));
                  float revRoll = 0.0F;
                  if(wi) {
                     var17.rotationYaw = var19.getRotYaw() + seatInfo.fixYaw;
                     var17.rotationPitch = var19.getRotPitch() + seatInfo.fixPitch;
                     if(var17.rotationPitch > 90.0F) {
                        var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.prevRotationYaw += 180.0F;
                        var17.rotationYaw += 180.0F;
                        revRoll = 180.0F;
                     } else if(var17.rotationPitch < -90.0F) {
                        var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.prevRotationYaw += 180.0F;
                        var17.rotationYaw += 180.0F;
                        revRoll = 180.0F;
                     }
                  }

                  var19.setupAllRiderRenderPosition(partialTicks, var17);
                  var19.setRotYaw(y);
                  var19.setRotPitch(p);
                  var19.setRotRoll(r);
                  mouseRollDeltaX *= 0.9D;
                  mouseRollDeltaY *= 0.9D;
                  float roll = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
                  float yaw = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
                  roll *= MathHelper.cos((float)((double)yaw * 3.141592653589793D / 180.0D));
                  if(var19.getTVMissile() != null && W_Lib.isClientPlayer(var19.getTVMissile().shootingEntity) && var19.getIsGunnerMode(var17)) {
                     roll = 0.0F;
                  }

                  W_Reflection.setCameraRoll(roll + revRoll);
                  this.correctViewEntityDummy(var17);
               } else {
                  if(isRideAircraft) {
                     W_Reflection.setCameraRoll(0.0F);
                     isRideAircraft = false;
                  }

                  mouseRollDeltaX = 0.0D;
                  mouseRollDeltaY = 0.0D;
               }
            }

            if(var19 != null) {
               if(var19.getSeatIdByEntity(var17) == 0 && !var19.isDestroyed()) {
                  var19.lastRiderYaw = var17.rotationYaw;
                  var19.prevLastRiderYaw = var17.prevRotationYaw;
                  var19.lastRiderPitch = var17.rotationPitch;
                  var19.prevLastRiderPitch = var17.prevRotationPitch;
               }

               var19.updateWeaponsRotation();
            }

            MCH_ViewEntityDummy var24 = MCH_ViewEntityDummy.getInstance(var17.worldObj);
            if(var24 != null) {
               var24.rotationYaw = var17.rotationYaw;
               var24.prevRotationYaw = var17.prevRotationYaw;
               if(var19 != null) {
                  MCH_WeaponSet var27 = var19.getCurrentWeapon(var17);
                  if(var27 != null && var27.getInfo() != null && var27.getInfo().fixCameraPitch) {
                     var24.rotationPitch = var24.prevRotationPitch = 0.0F;
                  }
               }
            }

            prevTick = partialTicks;
         }
      }
   }

   public void correctViewEntityDummy(Entity entity) {
      MCH_ViewEntityDummy de = MCH_ViewEntityDummy.getInstance(entity.worldObj);
      if(de != null) {
         if(de.rotationYaw - de.prevRotationYaw > 180.0F) {
            de.prevRotationYaw += 360.0F;
         } else if(de.rotationYaw - de.prevRotationYaw < -180.0F) {
            de.prevRotationYaw -= 360.0F;
         }
      }

   }

   public void onPlayerTickPre(EntityPlayer player) {
      if(player.worldObj.isRemote) {
         ItemStack currentItemstack = player.getCurrentEquippedItem();
         if(currentItemstack != null && currentItemstack.getItem() instanceof MCH_ItemWrench && player.getItemInUseCount() > 0 && player.getItemInUse() != currentItemstack) {
            int maxdm = currentItemstack.getMaxDamage();
            int dm = currentItemstack.getItemDamage();
            if(dm <= maxdm && dm > 0) {
               player.setItemInUse(currentItemstack, player.getItemInUseCount());
            }
         }
      }

   }

   public void onPlayerTickPost(EntityPlayer player) {}

   public void onRenderTickPost(float partialTicks) {
      if(super.mc.thePlayer != null) {
         MCH_ClientTickHandlerBase.applyRotLimit(super.mc.thePlayer);
         MCH_ViewEntityDummy arr$ = MCH_ViewEntityDummy.getInstance(super.mc.thePlayer.worldObj);
         if(arr$ != null) {
            arr$.rotationPitch = super.mc.thePlayer.rotationPitch;
            arr$.rotationYaw = super.mc.thePlayer.rotationYaw;
            arr$.prevRotationPitch = super.mc.thePlayer.prevRotationPitch;
            arr$.prevRotationYaw = super.mc.thePlayer.prevRotationYaw;
         }
      }

      if(super.mc.currentScreen == null || super.mc.currentScreen instanceof GuiChat || super.mc.currentScreen.getClass().toString().indexOf("GuiDriveableController") >= 0) {
         MCH_Gui[] var6 = this.guis;
         int len$ = var6.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            MCH_Gui gui = var6[i$];
            if(this.drawGui(gui, partialTicks)) {
               break;
            }
         }

         this.drawGui(this.gui_Common, partialTicks);
         this.drawGui(this.gui_Wrench, partialTicks);
         this.drawGui(this.gui_EMarker, partialTicks);
         if(isDrawScoreboard) {
            MCH_GuiScoreboard.drawList(super.mc, super.mc.fontRenderer, false);
         }

         this.drawGui(this.gui_Title, partialTicks);
      }

   }

   public boolean drawGui(MCH_Gui gui, float partialTicks) {
      if(gui.isDrawGui(super.mc.thePlayer)) {
         gui.drawScreen(0, 0, partialTicks);
         return true;
      } else {
         return false;
      }
   }

}
