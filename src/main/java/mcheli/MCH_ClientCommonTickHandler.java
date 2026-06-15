package mcheli;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;

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
import mcheli.mob.MCH_GuiSpawnGunner;
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
import mcheli.vehicle.MCH_ClientTurretTickHandler;
import mcheli.vehicle.MCH_EntityTurret;
import mcheli.vehicle.MCH_GuiTurret;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_Reflection;
import mcheli.wrapper.W_TickHandler;
import mcheli.wrapper.W_Vec3;
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
import mcheli.ship.MCH_ClientShipTickHandler;
import mcheli.ship.MCH_EntityShip;
import mcheli.ship.MCH_GuiShip;

//Eventhooks, clientproxy, tickhandler, guis and config just to name a few inheritors
@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {

   public static MCH_ClientCommonTickHandler instance;
   public MCH_GuiCommon gui_Common;
   public MCH_Gui gui_Heli;
   public MCH_Gui gui_Plane;
   public MCH_Gui gui_Ship;
   public MCH_Gui gui_Tank;
   public MCH_Gui gui_GLTD;
   public MCH_Gui gui_Vehicle;
   public MCH_Gui gui_LWeapon;
   public MCH_Gui gui_Wrench;
   public MCH_Gui gui_SwnGnr;
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
   public static MCH_EntityBaseVehicle ridingAircraft = null;
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
      this.gui_Ship = new MCH_GuiShip(minecraft);
      this.gui_Tank = new MCH_GuiTank(minecraft);
      this.gui_GLTD = new MCH_GuiGLTD(minecraft);
      this.gui_Vehicle = new MCH_GuiTurret(minecraft);
      this.gui_LWeapon = new MCH_GuiLightWeapon(minecraft);
      this.gui_Wrench = new MCH_GuiWrench(minecraft);
      this.gui_SwnGnr = new MCH_GuiSpawnGunner(minecraft);
      this.gui_RngFndr = new MCH_GuiRangeFinder(minecraft);
      this.gui_EMarker = new MCH_GuiTargetMarker(minecraft);
      this.gui_Title = new MCH_GuiTitle(minecraft);
      this.guis = new MCH_Gui[]{this.gui_RngFndr, this.gui_LWeapon, this.gui_Heli, this.gui_Plane, this.gui_Ship, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle};
      this.guiTicks = new MCH_Gui[]{this.gui_Common, this.gui_Heli, this.gui_Plane, this.gui_Tank, this.gui_GLTD, this.gui_Vehicle, this.gui_LWeapon, this.gui_Wrench, this.gui_SwnGnr, this.gui_RngFndr, this.gui_EMarker, this.gui_Title};      this.ticks = new MCH_ClientTickHandlerBase[]{new MCH_ClientHeliTickHandler(minecraft, config), new MCP_ClientPlaneTickHandler(minecraft, config), new MCH_ClientShipTickHandler(minecraft, config), new MCH_ClientTankTickHandler(minecraft, config), new MCH_ClientGLTDTickHandler(minecraft, config), new MCH_ClientTurretTickHandler(minecraft, config), new MCH_ClientLightWeaponTickHandler(minecraft, config), new MCH_ClientSeatTickHandler(minecraft, config), new MCH_ClientToolTickHandler(minecraft, config)};
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

      MCH_EntityBaseVehicle var11 = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(var7);
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
      MCH_PlayerViewHandler.onUpdate();
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
               mouseRollDeltaX = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaX, 0.85F, partialTicks);
            }

            if(Math.abs(mouseRollDeltaY) < getMaxStickLength() * 0.2D) {
               mouseRollDeltaY = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaY, 0.85F, partialTicks);
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

   /** Returns elapsed render time in Minecraft ticks; visual interpolation still uses raw partialTicks. */
   private static float getRenderSimulationDelta(float partialTicks) {
      float base = prevTick;
      for(int i = 0; i < 10 && base > partialTicks; ++i) {
         --base;
      }

      return mcheli.aircraft.MCH_FlightModel.getBoundedTickDelta(partialTicks - base);
   }

   private static void debugFlightControl(MCH_EntityBaseVehicle ac, float simDelta, float mouseX, float mouseY, float stickX, float stickY) {
      if(!MCH_Config.DebugFlightControl.prmBool || ac == null || ac.ticksExisted % 20 != 0) {
         return;
      }

      MCP_EntityPlane plane = ac instanceof MCP_EntityPlane ? (MCP_EntityPlane)ac : null;

      double forwardSpeed = plane != null
            ? ac.motionX * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).xCoord
                  + ac.motionY * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).yCoord
                  + ac.motionZ * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).zCoord
            : 0.0D;

      System.out.println(String.format(
              "[MCHeli] flight-control dt=%.3f inputMouse=(%.3f,%.3f) inputStick=(%.3f,%.3f) angularVelocity=(pitch=%.4f,yaw=%.4f,roll=%.4f) rot=(pitch=%.2f,yaw=%.2f,roll=%.2f) aero=(throttle=%.0f%%,flaps=%s,airspeed=%.3f,forwardSpeed=%.3f,verticalSpeed=%.4f,pitch=%.2f,mass=%.2f,weightForce=%.4f,engineThrust=%.4f,liftForce=%.4f,liftBeforeStall=%.4f,liftAfterStall=%.4f,liftToWeight=%.2f,thrustToWeight=%.2f,netForward=%.4f,takeoffMult=%.2f,takeoffBase=%.3f,takeoffEffective=%.3f,takeoffActive=%s,validTakeoff=%s,validClimb=%s,stallSuppressedHeadroom=%s,gravity=%.4f,resolvedGravity=%.4f,gravityOverride=%s,liftAccel=%.4f,netY=%.4f,airborne=%s,placementLock=%s,motion=(%.4f,%.4f,%.4f),cachedVelocity=(%.4f,%.4f,%.4f),aoaFromVelocity=%.2f,criticalAoA=%.2f,stallDemand=%.2f,speedSeverity=%.2f,aoaSeverity=%.2f,stallSeverity=%.2f,stallState=%s,overspeed=%s,g=%.2f,drag=%.3f,liftLoss=%.2f,authority=%.2f,pitchBreak=%s,pitchBreakAngularVelocity=%.4f)",
              simDelta,
              mouseX,
              mouseY,
              stickX,
              stickY,
              ac.getPitchAngularVelocity(),
              ac.getYawAngularVelocity(),
              ac.getRollAngularVelocity(),
              ac.getRotPitch(),
              ac.getRotYaw(),
              ac.getRotRoll(),
              Double.valueOf(ac.getNormalizedThrottle() * 100.0D),
              Boolean.valueOf(ac.isCombatFlapsDeployed()),
              Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ),
              forwardSpeed,
              ac.motionY,
              ac.getRotPitch(),
              plane != null ? plane.getPhysicalMass() : 1.0D,
              plane != null ? plane.getLastWeightForce() : 0.0D,
              plane != null ? plane.getLastEngineThrustForce() : 0.0D,
              plane != null ? plane.getLastLiftForce() : 0.0D,
              plane != null ? plane.getLastLiftForceBeforeStallLoss() : 0.0D,
              plane != null ? plane.getLastLiftForceAfterStallLoss() : 0.0D,
              plane != null ? plane.getLiftToWeightRatio() : 0.0D,
              plane != null ? plane.getThrustToWeightRatio() : 0.0D,
              plane != null ? plane.getLastNetForwardAcceleration() : 0.0D,
              plane != null ? plane.getTakeoffDistanceMultiplier() : 1.0D,
              plane != null ? plane.getLastBaseTakeoffSpeed() : 0.0D,
              plane != null ? plane.getLastEffectiveTakeoffSpeed() : 0.0D,
              Boolean.valueOf(plane != null && plane.isLastTakeoffMultiplierActive()),
              Boolean.valueOf(plane != null && plane.isLastValidTakeoff()),
              Boolean.valueOf(plane != null && plane.isLastValidClimb()),
              Boolean.valueOf(plane != null && plane.isLastStallSuppressedLiftHeadroom()),
              ac.getLastGravityAcceleration(),
              ac.getResolvedNewFlightGravity(),
              Boolean.valueOf(ac.isUsingNewFlightGravityOverride()),
              ac.getLastLiftAcceleration(),
              ac.getLastNetVerticalAcceleration(),
              Boolean.valueOf(ac.isLastAirborne()),
              Boolean.valueOf(ac.isPlacementMotionLocked()),
              ac.motionX,
              ac.motionY,
              ac.motionZ,
              ac.getCachedVelocityX(),
              ac.getCachedVelocityY(),
              ac.getCachedVelocityZ(),
              ac.getAngleOfAttackDegrees(),
              ac.getCriticalAoA(),
              ac.getStallDemand(),
              ac.getSpeedStallSeverity(),
              ac.getAoAStallSeverity(),
              ac.getStallSeverity(),
              Boolean.valueOf(ac.getStallSeverity() > 0.0D),
              Boolean.valueOf(ac.isOverspeeding()),
              ac.getCurrentGForce(),
              ac.getLastAerodynamicDrag(),
              ac.getLastLiftLoss(),
              ac.getDebugControlAuthority(),
              Boolean.valueOf(ac.isPitchBreakActive()),
              plane != null ? plane.getLastPitchBreakAngularVelocity() : 0.0D
      ));
   }

   public void onRenderTickPre(float partialTicks) {
      MCH_GuiTargetMarker.clearMarkEntityPos();
      if(!MCH_ServerSettings.enableDebugBoundingBox) {
         RenderManager.debugBoundingBox = false;
      }

      MCH_ClientEventHook.haveSearchLightAircraft.clear();
      if(super.mc != null && super.mc.theWorld != null) {
         Iterator player = Minecraft.getMinecraft().theWorld.loadedEntityList.iterator();

         while(player.hasNext()) {
            Object currentItemstack = player.next();
            if(currentItemstack instanceof MCH_EntityBaseVehicle && ((MCH_EntityBaseVehicle)currentItemstack).haveSearchLight()) {
               MCH_ClientEventHook.haveSearchLightAircraft.add((MCH_EntityBaseVehicle)currentItemstack);
            }
         }
      }

      if(!W_McClient.isGamePaused()) {
         EntityClientPlayerMP var17 = super.mc.thePlayer;
         if(var17 != null) {
            float simDelta = getRenderSimulationDelta(partialTicks);
            ItemStack var18 = var17.getCurrentEquippedItem();
            if(var18 != null && var18.getItem() instanceof MCH_ItemWrench && var17.getItemInUseCount() > 0) {
               W_Reflection.setItemRendererProgress(1.0F);
            }

            ridingAircraft = MCH_EntityBaseVehicle.getAircraft_RiddenOrControl(var17);
            if(ridingAircraft != null) {
               cameraMode = ridingAircraft.getCameraMode(var17);
            } else if(var17.ridingEntity instanceof MCH_EntityGLTD) {
               MCH_EntityGLTD ac = (MCH_EntityGLTD)var17.ridingEntity;
               cameraMode = ac.camera.getMode(0);
            } else {
               cameraMode = 0;
            }

            MCH_EntityBaseVehicle var19 = null;
            if(!(var17.ridingEntity instanceof MCH_EntityHeli) && !(var17.ridingEntity instanceof MCP_EntityPlane) && !(var17.ridingEntity instanceof MCH_EntityShip) && !(var17.ridingEntity instanceof MCH_EntityTank)) {
               if(var17.ridingEntity instanceof MCH_EntityUavStation) {
                  var19 = ((MCH_EntityUavStation)var17.ridingEntity).getControlAircract();
               } else if(var17.ridingEntity instanceof MCH_EntityTurret) {
                  MCH_EntityBaseVehicle stickMode = (MCH_EntityBaseVehicle)var17.ridingEntity;
                  stickMode.setupAllRiderRenderPosition(partialTicks, var17);
               }
            } else {
               var19 = (MCH_EntityBaseVehicle)var17.ridingEntity;
            }

            boolean var20 = false;
            MCH_Config var10000;
            if(var19 instanceof MCH_EntityHeli) {
               var10000 = MCH_MOD.config;
               var20 = MCH_Config.MouseControlStickModeHeli.prmBool;
            }

            if(var19 instanceof MCP_EntityPlane || var19 instanceof MCH_EntityShip) {
               var10000 = MCH_MOD.config;
               var20 = MCH_Config.MouseControlStickModePlane.prmBool;
            }

            float p;
            float r;
            if(var19 != null && var19.canMouseRot()) {
               if(!isRideAircraft) {
                  var19.onInteractFirst(var17);
               }

               isRideAircraft = true;
               this.updateMouseDelta(var20, simDelta);
               boolean var22 = false;
               float var23 = 0.0F;
               float var25 = 0.0F;
               MCH_SeatInfo var26 = var19.getSeatInfo(var17);
               if(var26 != null && var26.fixRot && var19.getIsGunnerMode(var17) && !var19.isGunnerLookMode(var17)) {
                  var22 = true;
                  var23 = var26.fixYaw;
                  //System.out.println("yaw1");
                  var25 = var26.fixPitch;
                  mouseRollDeltaX *= 0.0D;
                  mouseRollDeltaY *= 0.0D;
                  mouseDeltaX *= 0.0D;
                  mouseDeltaY *= 0.0D;
               } else if(var19.isPilot(var17)) {
                  MCH_BaseVehicleInfo.CameraPosition var28 = var19.getCameraPosInfo();
                  if(var28 != null) {
                     var23 = var28.yaw;
                     //System.out.println("yaw2");
                     var25 = var28.pitch;
                  }
               }

               if(var19.getAcInfo() == null) {
                  var17.setAngles((float)mouseDeltaX, (float)mouseDeltaY);
               } else {
                  var19.setAngles(var17, var22, var23, var25, (float)(mouseDeltaX + prevMouseDeltaX) / 2.0F, (float)(mouseDeltaY + prevMouseDeltaY) / 2.0F, (float)mouseRollDeltaX, (float)mouseRollDeltaY, simDelta);
                  debugFlightControl(var19, simDelta, (float)mouseDeltaX, (float)mouseDeltaY, (float)mouseRollDeltaX, (float)mouseRollDeltaY);
               }

               var19.setupAllRiderRenderPosition(partialTicks, var17);
               double var29 = (double)MathHelper.sqrt_double(mouseRollDeltaX * mouseRollDeltaX + mouseRollDeltaY * mouseRollDeltaY);
               if(!var20 || var29 < getMaxStickLength() * 0.1D) {
                  mouseRollDeltaX = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaX, 0.95F, simDelta);
                  mouseRollDeltaY = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaY, 0.95F, simDelta);
               }

               p = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
               r = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
               //System.out.println("yaw3");
               p *= MathHelper.cos((float)((double)r * 3.141592653589793D / 180.0D));
               if(var19.getTVMissile() != null && W_Lib.isClientPlayer(var19.getTVMissile().shootingEntity) && var19.getIsGunnerMode(var17)) {
                  p = 0.0F;
               }

               W_Reflection.setCameraRoll(p);
               this.correctViewEntityDummy(var17);
            } else {
               MCH_EntitySeat var21 = var17.ridingEntity instanceof MCH_EntitySeat?(MCH_EntitySeat)var17.ridingEntity:null;
               if(var21 != null && var21.getParent() != null) {
                  this.updateMouseDelta(var20, simDelta);
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
                  //System.out.println("yaw4");
                  p = var19.getRotPitch();
                  r = var19.getRotRoll();
                  var19.setRotYaw(var19.calcRotYaw(partialTicks));
                  //System.out.println("yaw5");
                  var19.setRotPitch(var19.calcRotPitch(partialTicks));
                  var19.setRotRoll(var19.calcRotRoll(partialTicks));
                  float revRoll = 0.0F;
                  if(wi) {
                     var17.rotationYaw = var19.getRotYaw() + seatInfo.fixYaw;
                     //System.out.println("yaw6");
                     var17.rotationPitch = var19.getRotPitch() + seatInfo.fixPitch;
                     if(var17.rotationPitch > 90.0F) {
                        var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.prevRotationYaw += 180.0F;
                        var17.rotationYaw += 180.0F;
                        //System.out.println("yaw7");
                        revRoll = 180.0F;
                     } else if(var17.rotationPitch < -90.0F) {
                        var17.prevRotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.rotationPitch -= (var17.rotationPitch - 90.0F) * 2.0F;
                        var17.prevRotationYaw += 180.0F;
                        var17.rotationYaw += 180.0F;
                        //System.out.println("yaw8");
                        revRoll = 180.0F;
                     }
                  }

                  var19.setupAllRiderRenderPosition(partialTicks, var17);
                  var19.setRotYaw(y);
                  //System.out.println("yaw9");
                  var19.setRotPitch(p);
                  var19.setRotRoll(r);
                  mouseRollDeltaX = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaX, 0.9F, simDelta);
                  mouseRollDeltaY = (double)mcheli.aircraft.MCH_FlightModel.decayPerTick((float)mouseRollDeltaY, 0.9F, simDelta);
                  float roll = MathHelper.wrapAngleTo180_float(var19.getRotRoll());
                  float yaw = MathHelper.wrapAngleTo180_float(var19.getRotYaw() - var17.rotationYaw);
                  //System.out.println("yaw10");
                  roll *= MathHelper.cos((float)((double)yaw * 3.141592653589793D / 180.0D));
                  //System.out.println("yaw11");
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
                  //System.out.println("yaw12");
                  var19.prevLastRiderYaw = var17.prevRotationYaw;
                  //System.out.println("yaw13");
                  var19.lastRiderPitch = var17.rotationPitch;
                  var19.prevLastRiderPitch = var17.prevRotationPitch;
               }

               var19.updateWeaponsRotation();
            }

            MCH_ViewEntityDummy var24 = MCH_ViewEntityDummy.getInstance(var17.worldObj);
            if(var24 != null) {
               var24.rotationYaw = var17.rotationYaw;
               //System.out.println("yaw14");
               var24.prevRotationYaw = var17.prevRotationYaw;
               //System.out.println("yaw15");
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
            //System.out.println("yaw16");
            de.prevRotationYaw += 360.0F;
         } else if(de.rotationYaw - de.prevRotationYaw < -180.0F) {
            de.prevRotationYaw -= 360.0F;
            //System.out.println("yaw17");
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
      if (this.mc.thePlayer != null) {
         MCH_ClientTickHandlerBase.applyRotLimit((Entity)this.mc.thePlayer);
         MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.thePlayer.worldObj);
         if (mCH_ViewEntityDummy != null) {
            ((Entity)mCH_ViewEntityDummy).rotationPitch = this.mc.thePlayer.rotationPitch;
            ((Entity)mCH_ViewEntityDummy).rotationYaw = this.mc.thePlayer.rotationYaw;
            ((Entity)mCH_ViewEntityDummy).prevRotationPitch = this.mc.thePlayer.prevRotationPitch;
            ((Entity)mCH_ViewEntityDummy).prevRotationYaw = this.mc.thePlayer.prevRotationYaw;
         }
      }
      if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat || this.mc.currentScreen.getClass().toString().indexOf("GuiDriveableController") >= 0) {
         for (MCH_Gui gui : this.guis) {
            if (drawGui(gui, partialTicks))
               break;
         }
         drawGui((MCH_Gui)this.gui_Common, partialTicks);
         drawGui(this.gui_Wrench, partialTicks);
         drawGui(this.gui_SwnGnr, partialTicks);
         drawGui(this.gui_EMarker, partialTicks);
         if (isDrawScoreboard)
            MCH_GuiScoreboard.drawList(this.mc, this.mc.fontRenderer, false);
         drawGui(this.gui_Title, partialTicks);
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
