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
import mcheli.particles.MCH_ThermalParticleFilter;
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
import mcheli.vehicle.MCH_ClientTurretTickHandler;
import mcheli.vehicle.MCH_GuiTurret;
import mcheli.weapon.MCH_WeaponSet;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_McClient;
import mcheli.plane.MCP_PlaneChaseCamera;
import mcheli.wrapper.W_Reflection;
import mcheli.wrapper.W_TickHandler;
import mcheli.wrapper.W_Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.Display;
import mcheli.ship.MCH_ClientShipTickHandler;
import mcheli.ship.MCH_EntityShip;
import mcheli.ship.MCH_GuiShip;
import mcheli.compat.MCH_ReplayModCompat;

//Eventhooks, clientproxy, tickhandler, guis and config just to name a few inheritors
@SideOnly(Side.CLIENT)
public class MCH_ClientCommonTickHandler extends W_TickHandler {

   /** Three seconds measured against a monotonic clock. */
   public static final long DISMOUNT_HOLD_NANOS = 3000000000L;

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
   private long dismountHoldStartNanos = -1L;
   private boolean dismountRequestPending;
   private boolean dismountHoldTriggered;
   private boolean suppressedDismountKey;
   private Entity dismountMount;
   private EntityClientPlayerMP dismountPlayer;
   private World dismountWorld;
   private Object dismountConnection;
   private boolean restoreMouseFocusAfterRender;
   private boolean replayPlaybackActive;



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
                  if(!MCH_Config.EnableMCHLibDebugLog.prmBool) {
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
            if(var11.canPlayAlertSound()) {
               lockedSoundCount = 20;
               MCH_ClientTickHandlerBase.playSound("locked");
            }
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
      MCH_MOD.proxy.tickTargetedVehicleReload();
      boolean replayActive = MCH_ReplayModCompat.updatePlaybackState();
      if(replayActive != this.replayPlaybackActive) {
         this.replayPlaybackActive = replayActive;
         if(replayActive) {
            this.releaseCameraAndControlForReplay();
         } else {
            MCH_ReplayModCompat.logCameraHandlingRestored();
         }
      }
      if(replayActive) {
         return;
      }
      if(super.mc.thePlayer != null && super.mc.theWorld != null) {
         this.updateDismountHoldState();
         this.onTick();
      } else {
         this.resetDismountHoldState();
      }

   }

   public void onTickPost() {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_PlayerViewHandler.clearRecoil();
         return;
      }
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

   public static double getZoomSensitivityMultiplier(Entity player) {
      return player instanceof EntityPlayer?MCH_ZoomContext.resolve(Minecraft.getMinecraft(), (EntityPlayer)player).getSensitivityMultiplier():1.0D;
   }

   public void updateMouseDelta(boolean stickMode, float partialTicks) {
      MCH_ZoomContext context = MCH_ZoomContext.resolve(super.mc, super.mc.thePlayer);
      this.updateMouseDelta(stickMode, partialTicks, context.getSensitivityMultiplier());
   }

   private void updateMouseDelta(boolean stickMode, float partialTicks, double zoomSensitivityMultiplier) {
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
         mouseDeltaX *= zoomSensitivityMultiplier;
         mouseDeltaY *= zoomSensitivityMultiplier;
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

   /** Consumes the render-frame mouse sample before EntityRenderer can apply it a second time. */
   private void updateVanillaLook(MCH_ZoomContext context) {
      double multiplier = context.getSensitivityMultiplier();
      if(context.inputPath != MCH_ZoomContext.InputPath.VANILLA_LOOK || multiplier >= 1.0D ||
            !super.mc.inGameHasFocus || !Display.isActive() || super.mc.currentScreen != null) {
         return;
      }

      super.mc.mouseHelper.mouseXYChange();
      float sensitivity = super.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
      float curve = sensitivity * sensitivity * sensitivity * 8.0F;
      float yaw = (float)((double)super.mc.mouseHelper.deltaX * (double)curve * multiplier);
      float pitch = (float)((double)super.mc.mouseHelper.deltaY * (double)curve * multiplier);
      if(super.mc.gameSettings.invertMouse) {
         pitch = -pitch;
      }
      super.mc.thePlayer.setAngles(yaw, pitch);
      // EntityRenderer runs after RenderTickEvent.START. Suppress only its mouse branch so
      // the raw sample consumed above cannot be applied again during this render frame.
      super.mc.inGameHasFocus = false;
      this.restoreMouseFocusAfterRender = true;
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
      if((!MCH_Config.DebugFlightControl.prmBool && !MCH_Config.MouseAimDebug.prmBool) || ac == null || ac.ticksExisted % 20 != 0) {
         return;
      }

      MCP_EntityPlane plane = ac instanceof MCP_EntityPlane ? (MCP_EntityPlane)ac : null;
      MCH_EntityHeli heli = ac instanceof MCH_EntityHeli ? (MCH_EntityHeli)ac : null;
      if(heli != null && heli.isNewHeliFlightModelEnabled()) {
         System.out.println(String.format("[MCHeli][NewHeli][opt-in/config] dt=%.3f type=%s enabled=%s mass=%.3f destroyed=%s bladesUsable=%s folded=%s",
               simDelta, heli.getTypeName(), Boolean.valueOf(heli.isNewHeliFlightModelEnabled()), Float.valueOf(heli.getPhysicalMass()), Boolean.valueOf(heli.isDestroyed()), Boolean.valueOf(heli.isNewHelicopterBladesUsable()), Boolean.valueOf(heli.isFoldBlades())));
         System.out.println(String.format("[MCHeli][NewHeli][rotor RPM] current=%.4f target=%.4f last=%.4f delta=%.4f energy=%.4f engineOutput=%.4f ready=%s",
               Float.valueOf(heli.getNormalizedRotorRPM()), Float.valueOf(heli.getTargetRotorRPM()), Float.valueOf(heli.getLastRotorRPM()), Float.valueOf(heli.getRotorSpoolDelta()), Float.valueOf(heli.getRotorEnergy()), Float.valueOf(heli.getEnginePowerOutput()), Boolean.valueOf(heli.isRotorReadyForLift())));
         System.out.println(String.format("[MCHeli][NewHeli][collective/lift] collective=%.4f thrust=%.4f efficiency=%.4f verticalThrust=%.4f weight=%.4f netY=%.4f accelY=%.4f dragY=%.4f finalY=%.4f",
               Float.valueOf(heli.getCollectiveInput()), Float.valueOf(heli.getRotorThrust()), Float.valueOf(heli.getRotorEfficiency()), Float.valueOf(heli.getRotorVerticalThrust()), Float.valueOf(heli.getWeightForce()), Float.valueOf(heli.getNetVerticalForce()), Float.valueOf(heli.getVerticalAcceleration()), Float.valueOf(heli.getVerticalDragApplied()), Float.valueOf(heli.getFinalMotionY())));
         System.out.println(String.format("[MCHeli][NewHeli][cyclic/horizontal] input=(pitch=%.4f,roll=%.4f) tilt=(forward=%.4f,right=%.4f) thrust=(x=%.4f,z=%.4f) accel=(x=%.4f,z=%.4f) drag=(x=%.4f,z=%.4f) localVel=(forward=%.4f,lateral=%.4f) lateralScale=%.4f lateralCap=%s backwardScaleActive=%s backwardCap=%s",
               Float.valueOf(heli.getCyclicPitchInput()), Float.valueOf(heli.getCyclicRollInput()), Float.valueOf(heli.getRotorTiltForward()), Float.valueOf(heli.getRotorTiltRight()), Float.valueOf(heli.getRotorHorizontalThrustX()), Float.valueOf(heli.getRotorHorizontalThrustZ()), Float.valueOf(heli.getHorizontalAccelerationX()), Float.valueOf(heli.getHorizontalAccelerationZ()), Float.valueOf(heli.getParasiteDragAppliedX()), Float.valueOf(heli.getParasiteDragAppliedZ()), Float.valueOf(heli.getForwardVelocityComponent()), Float.valueOf(heli.getLateralVelocityComponent()), Float.valueOf(heli.getAppliedLateralThrustMultiplier()), Boolean.valueOf(heli.isLateralSpeedCapped()), Boolean.valueOf(heli.isBackwardThrustScaled()), Boolean.valueOf(heli.isBackwardSpeedCapped())));
         System.out.println(String.format("[MCHeli][NewHeli][hover assist] active=%s strength=%.4f manualOverride=%.4f collectiveCorrection=%.4f cyclicCorrection=(pitch=%.4f,roll=%.4f) targetSpeed=(vertical=%.4f,horizontal=%.4f) drift=(forward=%.4f,right=%.4f)",
               Boolean.valueOf(heli.isHoverAssistActive()), Float.valueOf(heli.getHoverAssistStrength()), Float.valueOf(heli.getManualInputOverrideFactor()), Float.valueOf(heli.getHoverCollectiveCorrection()), Float.valueOf(heli.getHoverCyclicPitchCorrection()), Float.valueOf(heli.getHoverCyclicRollCorrection()), Float.valueOf(heli.getTargetVerticalSpeed()), Float.valueOf(heli.getTargetHorizontalSpeed()), Float.valueOf(heli.getLocalDriftForward()), Float.valueOf(heli.getLocalDriftRight())));
         System.out.println(String.format("[MCHeli][NewHeli][yaw/tail rotor] yawInput=%.4f mainReaction=%.4f tailTorque=%.4f netTorque=%.4f yawAccel=%.4f yawVelocity=%.4f damping=%.4f finalYaw=%.2f",
               Float.valueOf(heli.getTailRotorInput()), Float.valueOf(heli.getMainRotorTorqueReaction()), Float.valueOf(heli.getTailRotorTorque()), Float.valueOf(heli.getHeliYawTorque()), Float.valueOf(heli.getYawAngularAcceleration()), Float.valueOf(heli.getYawAngularVelocity()), Float.valueOf(heli.getYawDampingApplied()), Float.valueOf(heli.getFinalRotYaw())));
         System.out.println(String.format("[MCHeli][NewHeli][final motion] motion=(%.4f,%.4f,%.4f) cachedFinal=(%.4f,%.4f,%.4f) rot=(pitch=%.2f,yaw=%.2f,roll=%.2f)",
               Double.valueOf(ac.motionX), Double.valueOf(ac.motionY), Double.valueOf(ac.motionZ), Float.valueOf(heli.getFinalMotionX()), Float.valueOf(heli.getFinalMotionY()), Float.valueOf(heli.getFinalMotionZ()), Float.valueOf(heli.getRotPitch()), Float.valueOf(heli.getRotYaw()), Float.valueOf(heli.getRotRoll())));
         return;
      }

      double forwardSpeed = plane != null
            ? ac.motionX * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).xCoord
                  + ac.motionY * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).yCoord
                  + ac.motionZ * MCH_Lib.Rot2Vec3(ac.getRotYaw(), ac.getRotPitch()).zCoord
            : 0.0D;

      System.out.println(String.format(
              "[MCHeli] flight-control dt=%.3f inputMouse=(%.3f,%.3f) inputStick=(%.3f,%.3f) angularVelocity=(pitch=%.4f,yaw=%.4f,roll=%.4f) finalPitchAngularVelocity=%.4f rot=(pitch=%.2f,yaw=%.2f,roll=%.2f) aero=(throttle=%.0f%%,engineOutput=%.0f%%,effectiveThrottle=%.0f%%,propulsiveThrottle=%.0f%%,flaps=%s,airspeed=%.3f,forwardAirspeed=%.3f,bodyForwardAirspeed=%.3f,horizontalSpeed=%.3f,trueAirspeed=%.3f,totalSpeed=%.3f,forwardSpeed=%.3f,verticalSpeed=%.4f,pitch=%.2f,mass=%.2f,weightForce=%.4f,engineThrust=%.4f,liftForce=%.4f,liftBeforeStall=%.4f,liftAfterStall=%.4f,liftCoeff=%.2f,dragCoeff=%.4f,liftVector=(%.4f,%.4f,%.4f),dragVector=(%.4f,%.4f,%.4f),liftToWeight=%.2f,thrustToWeight=%.2f,netForward=%.4f,takeoffMult=%.2f,takeoffBase=%.3f,takeoffEffective=%.3f,takeoffActive=%s,validTakeoff=%s,validClimb=%s,stallSuppressedHeadroom=%s,gravity=%.4f,resolvedGravity=%.4f,gravityOverride=%s,liftAccel=%.4f,netY=%.4f,airborne=%s,placementLock=%s,motion=(%.4f,%.4f,%.4f),cachedVelocity=(%.4f,%.4f,%.4f),pitchPlaneAoA=%.2f,sideslipAngle=%.2f,oldAoA=%.2f,selectedEffectiveAoA=%.2f,criticalAoA=%.2f,stallSpeed=%.3f,energyRatio=%.2f,pitchEnvelopeReference=%.2f,pitchExcess=%.2f,commandPitchExcess=%.2f,physicalPitchExcess=%.2f,commandLimiterActive=%s,physicalRecoveryActive=%s,noseUpAuthority=%.2f,noseDownRecoveryTorque=%.4f,finalElevatorInput=%.4f,envelopeRecoveryActive=%s,recoveryDueToLowEnergy=%s,recoveryDueToAoA=%s,recoveryDueToLiftDeficit=%s,recoveryDueToUnsupportedClimb=%s,stallDemand=%.2f,speedSeverity=%.2f,aoaSeverity=%.2f,stallSeverity=%.2f,stallPitchMoment=%.4f,throttlePitchDown=%.4f,pitchMoment=%.4f,aoaMoment=%.4f,stabilityMoment=%.4f,airflowScale=%.4f,pitchMomentAngularVelocity=%.4f,stallState=%s,stallReason=%s,recovery=%s,overspeed=%s,g=%.2f,drag=%.3f,climbEnergyDrag=%.4f,pitchClimbDragFactor=%.3f,aoaDragFactor=%.4f,horizontalSpeedBefore=%.3f,horizontalSpeedAfter=%.3f,liftLoss=%.2f,controlAuthority=%.2f,speedRatio=%.2f,deepStallSeverity=%.2f,pilotControlAuthority=%.2f,softStallAuthority=%.2f,pitchAuthority=%.2f,pitchUpLimiter=%.2f,pitchDownAuthority=%.2f,rollAuthority=%.2f,yawAuthority=%.2f,finalPitchAuthority=%.2f,pitchInputRequested=%.4f,pitchInputAfterAuthority=%.4f,pitchAuthorityAfterSuppression=%.2f,pilotPitchAngularVelocity=%.4f,noseUpSuppress=%.2f,unsupportedClimb=%s,unsupportedSeverity=%.2f,idleUnsupported=%s,idleWarning=%s,lowHorizontalWarning=%s,pitchBreak=%s,noseDownRecoverySeverity=%.2f,forcedPitchDelta=%.4f,pitchBreakAngularVelocity=%.4f,kineticEnergy=%.4f,potentialEnergy=%.4f,totalEnergy=%.4f,specificEnergy=%.4f,energyDelta=%.4f,excessPower=%.4f,energyDeficitSeverity=%.2f,climbEnergyDemand=%.4f,pitchEnergyDemand=%.4f,energyUnsupportedClimb=%s,energyForcedRecovery=%s,diveThrottle=%.0f%%,noseDownDegrees=%.2f,falling01=%.2f,noseDown01=%.2f,diveGain=%.5f,horizontalSpeedBeforeDiveAssist=%.3f,horizontalSpeedAfterDiveAssist=%.3f,maxDiveHorizontalSpeed=%.3f,diveAssistSuppressedBySpeedCap=%s,diveAssistIgnoredThrottle=%s,diveAssistActive=%s) %s",
              simDelta,
              mouseX,
              mouseY,
              stickX,
              stickY,
              ac.getPitchAngularVelocity(),
              ac.getYawAngularVelocity(),
              ac.getRollAngularVelocity(),
              plane != null ? plane.getLastFinalPitchAngularVelocity() : ac.getPitchAngularVelocity(),
              ac.getRotPitch(),
              ac.getRotYaw(),
              ac.getRotRoll(),
              Double.valueOf(ac.getNormalizedThrottle() * 100.0D),
              Double.valueOf(plane != null ? plane.getDebugEngineThrottle() * 100.0D : ac.getNormalizedThrottle() * 100.0D),
              Double.valueOf(plane != null ? plane.getDebugEffectiveEngineThrottle() * 100.0D : ac.getNormalizedThrottle() * 100.0D),
              Double.valueOf(plane != null ? plane.getDebugPropulsiveEngineThrottle() * 100.0D : ac.getNormalizedThrottle() * 100.0D),
              Boolean.valueOf(ac.isCombatFlapsDeployed()),
              Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ),
              plane != null ? plane.getLastForwardAirspeed() : forwardSpeed,
              plane != null ? plane.getLastBodyForwardAirspeed() : forwardSpeed,
              ac.getLastHorizontalSpeed(),
              plane != null ? plane.getLastTrueAirspeed() : Math.sqrt(ac.motionX * ac.motionX + ac.motionY * ac.motionY + ac.motionZ * ac.motionZ),
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
              ac.getLastLiftCoefficient(),
              plane != null ? plane.getLastDragCoefficient() : 0.0D,
              plane != null ? plane.getLastLiftVectorX() : 0.0D,
              plane != null ? plane.getLastLiftVectorY() : 0.0D,
              plane != null ? plane.getLastLiftVectorZ() : 0.0D,
              plane != null ? plane.getLastDragVectorX() : 0.0D,
              plane != null ? plane.getLastDragVectorY() : 0.0D,
              plane != null ? plane.getLastDragVectorZ() : 0.0D,
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
              plane != null ? plane.getPitchPlaneAngleOfAttackDegrees() : ac.getAngleOfAttackDegrees(),
              plane != null ? plane.getSideslipAngleDegrees() : 0.0D,
              plane != null ? plane.getOldAngleOfAttackDegrees() : ac.getAngleOfAttackDegrees(),
              ac.getAngleOfAttackDegrees(),
              ac.getCriticalAoA(),
              plane != null && plane.getPlaneInfo() != null ? MCH_FlightModel.getStallSpeed(plane.getPlaneInfo().stallSpeed, plane.getMaxSpeed(), plane.getPlaneInfo().stallSpeedFactor) : 0.0D,
              plane != null ? plane.getLastPitchEnvelopeEnergyRatio() : 1.0D,
              plane != null ? plane.getLastPitchEnvelopeReference() : 90.0D,
              plane != null ? plane.getLastPitchEnvelopeExcess() : 0.0D,
              plane != null ? plane.getLastCommandPitchExcess() : 0.0D,
              plane != null ? plane.getLastPhysicalPitchExcess() : 0.0D,
              Boolean.valueOf(plane != null && plane.isLastCommandLimiterActive()),
              Boolean.valueOf(plane != null && plane.isLastPhysicalRecoveryActive()),
              plane != null ? plane.getLastPitchUpAuthority() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastNoseDownRecoveryTorque() : 0.0D,
              plane != null ? plane.getLastFinalElevatorInput() : 0.0D,
              Boolean.valueOf(plane != null && plane.isLastEnvelopeRecoveryActive()),
              Boolean.valueOf(plane != null && plane.isLastRecoveryDueToLowEnergy()),
              Boolean.valueOf(plane != null && plane.isLastRecoveryDueToAoA()),
              Boolean.valueOf(plane != null && plane.isLastRecoveryDueToLiftDeficit()),
              Boolean.valueOf(plane != null && plane.isLastRecoveryDueToUnsupportedClimb()),
              ac.getStallDemand(),
              ac.getSpeedStallSeverity(),
              ac.getAoAStallSeverity(),
              ac.getStallSeverity(),
              ac.getLastStallPitchMoment(),
              plane != null ? plane.getLastThrustPitchDownMoment() : 0.0D,
              plane != null ? plane.getLastPitchMoment() : 0.0D,
              plane != null ? plane.getLastAoAPitchMoment() : 0.0D,
              plane != null ? plane.getLastStabilityPitchMoment() : 0.0D,
              plane != null ? plane.getLastPitchMomentAirflowScale() : 0.0D,
              plane != null ? plane.getLastPitchMomentAngularVelocity() : 0.0D,
              Boolean.valueOf(ac.getStallSeverity() > 0.0D),
              plane != null ? plane.getLastStallReason() : "",
              Boolean.valueOf(ac.isStallRecovering()),
              Boolean.valueOf(ac.isOverspeeding()),
              ac.getCurrentGForce(),
              ac.getLastAerodynamicDrag(),
              plane != null ? plane.getLastClimbEnergyDrag() : 0.0D,
              plane != null ? plane.getLastPitchClimbDragFactor() : 0.0D,
              plane != null ? plane.getLastAoADragFactor() : 0.0D,
              plane != null ? plane.getLastHorizontalSpeedBeforeEnergyDrag() : ac.getLastHorizontalSpeed(),
              plane != null ? plane.getLastHorizontalSpeedAfterEnergyDrag() : ac.getLastHorizontalSpeed(),
              ac.getLastLiftLoss(),
              ac.getLastControlAuthority(),
              plane != null ? plane.getLastAirflowAuthorityRaw() : 1.0D,
              plane != null ? plane.getDeepStallSeverity() : 0.0D,
              plane != null ? plane.getLastAirflowAuthority() : 1.0D,
              ac.getLastControlAuthority(),
              plane != null ? plane.getLastPitchAuthority() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastPitchUpAuthority() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastPitchDownAuthority() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastRollAuthority() : ac.getLastControlAuthority(),
              plane != null ? plane.getLastYawAuthority() : ac.getLastControlAuthority(),
              plane != null ? plane.getLastFinalPitchAuthority() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastRequestedPitchInput() : 0.0D,
              plane != null ? plane.getLastPitchInputAfterAuthority() : 0.0D,
              plane != null ? plane.getLastPitchAuthorityAfterSuppression() : ac.getLastPitchAuthority(),
              plane != null ? plane.getLastPilotPitchAngularVelocity() : ac.getPitchAngularVelocity(),
              ac.getLastNoseUpPitchSuppression(),
              Boolean.valueOf(ac.isUnsupportedClimb()),
              ac.getLastUnsupportedClimbSeverity(),
              Boolean.valueOf(ac.isLastIdleUnsupportedClimb()),
              ac.getLastIdleThrottleWarning(),
              ac.getLastLowHorizontalSpeedWarning(),
              Boolean.valueOf(ac.isPitchBreakActive()),
              plane != null ? plane.getLastNoseDownRecoverySeverity() : 0.0D,
              plane != null ? plane.getLastForcedNoseDownPitchDelta() : 0.0D,
              plane != null ? plane.getLastPitchBreakAngularVelocity() : 0.0D,
              plane != null ? plane.getLastKineticEnergy() : 0.0D,
              plane != null ? plane.getLastPotentialEnergy() : 0.0D,
              plane != null ? plane.getLastTotalEnergy() : 0.0D,
              plane != null ? plane.getLastSpecificEnergy() : 0.0D,
              plane != null ? plane.getLastEnergyDelta() : 0.0D,
              plane != null ? plane.getLastExcessPower() : 0.0D,
              plane != null ? plane.getLastEnergyDeficitSeverity() : 0.0D,
              plane != null ? plane.getLastClimbEnergyDemand() : 0.0D,
              plane != null ? plane.getLastPitchEnergyDemand() : 0.0D,
              Boolean.valueOf(plane != null && plane.isLastEnergyUnsupportedClimb()),
              Boolean.valueOf(plane != null && plane.isLastEnergyForcedRecovery()),
              Double.valueOf((plane != null ? plane.getLastDiveAssistThrottle() : ac.getNormalizedThrottle()) * 100.0D),
              plane != null ? plane.getLastDiveAssistNoseDownDegrees() : 0.0D,
              plane != null ? plane.getLastDiveAssistFalling01() : 0.0D,
              plane != null ? plane.getLastDiveAssistNoseDown01() : 0.0D,
              plane != null ? plane.getLastDiveAssistGain() : 0.0D,
              plane != null ? plane.getLastDiveAssistHorizontalSpeedBefore() : 0.0D,
              plane != null ? plane.getLastDiveAssistHorizontalSpeedAfter() : 0.0D,
              plane != null ? plane.getLastDiveAssistMaxHorizontalSpeed() : 0.0D,
              Boolean.valueOf(plane != null && plane.isLastDiveAssistSuppressedBySpeedCap()),
              Boolean.valueOf(plane != null && plane.isLastDiveAssistIgnoredThrottle()),
              Boolean.valueOf(plane != null && plane.isLastDiveAssistActive()),
              plane != null ? plane.getMouseAimDebugString() : "mouseAim=(not-plane)"
      ));

      if(plane != null && plane.getLastLowHorizontalSpeedWarning().length() > 0) {
         System.out.println(String.format(
               "[MCHeli][WARN] low-horizontal-speed flight sanity: reason=%s motionX=%.4f motionZ=%.4f forwardAirspeed=%.3f horizontalSpeed=%.3f totalSpeed=%.3f verticalSpeed=%.4f airspeed=%.3f pitchPlaneAoA=%.2f sideslipAngle=%.2f oldAoA=%.2f selectedEffectiveAoA=%.2f stallSpeed=%.3f speedSeverity=%.2f aoaSeverity=%.2f stallDemand=%.2f stallSeverity=%.2f liftToWeight=%.3f thrustToWeight=%.3f validClimb=%s validTakeoff=%s controlAuthority=%.2f speedRatio=%.2f deepStallSeverity=%.2f pilotControlAuthority=%.2f softStallAuthority=%.2f pitchAuthority=%.2f pitchUpLimiter=%.2f pitchDownAuthority=%.2f rollAuthority=%.2f yawAuthority=%.2f finalPitchAuthority=%.2f pitchInputRequested=%.4f pitchInputAfterAuthority=%.4f pitchAuthorityAfterSuppression=%.2f pilotPitchAngularVelocity=%.4f noseDownRecoverySeverity=%.2f forcedPitchDelta=%.4f finalPitchAngularVelocity=%.4f pitchEnvelopeReference=%.2f pitchExcess=%.2f commandPitchExcess=%.2f physicalPitchExcess=%.2f commandLimiterActive=%s physicalRecoveryActive=%s energyRatio=%.2f noseDownRecoveryTorque=%.4f finalElevatorInput=%.4f envelopeRecoveryActive=%s recoveryDueToLowEnergy=%s recoveryDueToAoA=%s recoveryDueToLiftDeficit=%s recoveryDueToUnsupportedClimb=%s unsupportedClimb=%.3f netY=%.4f",
               plane.getLastLowHorizontalSpeedWarning(),
               plane.motionX,
               plane.motionZ,
               plane.getLastForwardAirspeed(),
               plane.getLastHorizontalSpeed(),
               plane.getAirspeed(),
               plane.motionY,
               plane.getAirspeed(),
               plane.getPitchPlaneAngleOfAttackDegrees(),
               plane.getSideslipAngleDegrees(),
               plane.getOldAngleOfAttackDegrees(),
               plane.getAngleOfAttackDegrees(),
               plane.getPlaneInfo() != null ? MCH_FlightModel.getStallSpeed(plane.getPlaneInfo().stallSpeed, plane.getMaxSpeed(), plane.getPlaneInfo().stallSpeedFactor) : 0.0D,
               plane.getSpeedStallSeverity(),
               plane.getAoAStallSeverity(),
               plane.getStallDemand(),
               plane.getStallSeverity(),
               plane.getLiftToWeightRatio(),
               plane.getThrustToWeightRatio(),
               Boolean.valueOf(plane.isLastValidClimb()),
               Boolean.valueOf(plane.isLastValidTakeoff()),
               plane.getLastControlAuthority(),
               plane.getLastAirflowAuthorityRaw(),
               plane.getDeepStallSeverity(),
               plane.getLastAirflowAuthority(),
               plane.getLastStallAuthority(),
               plane.getLastPitchAuthority(),
               plane.getLastPitchUpAuthority(),
               plane.getLastPitchDownAuthority(),
               plane.getLastRollAuthority(),
               plane.getLastYawAuthority(),
               plane.getLastFinalPitchAuthority(),
               plane.getLastRequestedPitchInput(),
               plane.getLastPitchInputAfterAuthority(),
               plane.getLastPitchAuthorityAfterSuppression(),
               plane.getLastPilotPitchAngularVelocity(),
               plane.getLastNoseDownRecoverySeverity(),
               plane.getLastForcedNoseDownPitchDelta(),
               plane.getLastFinalPitchAngularVelocity(),
               plane.getLastPitchEnvelopeReference(),
               plane.getLastPitchEnvelopeExcess(),
               plane.getLastCommandPitchExcess(),
               plane.getLastPhysicalPitchExcess(),
               Boolean.valueOf(plane.isLastCommandLimiterActive()),
               Boolean.valueOf(plane.isLastPhysicalRecoveryActive()),
               plane.getLastPitchEnvelopeEnergyRatio(),
               plane.getLastNoseDownRecoveryTorque(),
               plane.getLastFinalElevatorInput(),
               Boolean.valueOf(plane.isLastEnvelopeRecoveryActive()),
               Boolean.valueOf(plane.isLastRecoveryDueToLowEnergy()),
               Boolean.valueOf(plane.isLastRecoveryDueToAoA()),
               Boolean.valueOf(plane.isLastRecoveryDueToLiftDeficit()),
               Boolean.valueOf(plane.isLastRecoveryDueToUnsupportedClimb()),
               plane.getLastUnsupportedClimbSeverity(),
               plane.getLastNetVerticalAcceleration()));
      }

      if(plane != null && plane.getLastIdleThrottleWarning().length() > 0) {
         System.out.println(String.format(
               "[MCHeli][WARN] idle-throttle flight sanity: reason=%s throttle=%.3f propulsiveThrottle=%.3f thrustToWeight=%.3f liftToWeight=%.3f validClimb=%s unsupportedClimb=%.3f airspeed=%.3f netY=%.4f pitch=%.2f",
               plane.getLastIdleThrottleWarning(),
               plane.getNormalizedThrottle(),
               plane.getDebugPropulsiveEngineThrottle(),
               plane.getThrustToWeightRatio(),
               plane.getLiftToWeightRatio(),
               Boolean.valueOf(plane.isLastValidClimb()),
               plane.getLastUnsupportedClimbSeverity(),
               plane.getAirspeed(),
               plane.getLastNetVerticalAcceleration(),
               plane.getRotPitch()));
      }
   }

   public void onRenderTickPre(float partialTicks) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         ridingAircraft = null;
         cameraMode = 0;
         return;
      }
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

            MCH_ZoomContext zoomContext = MCH_ZoomContext.resolve(super.mc, var17);
            this.updateVanillaLook(zoomContext);
            ridingAircraft = zoomContext.vehicle;
            if(ridingAircraft != null) {
               cameraMode = ridingAircraft.getCameraMode(var17);
            } else if(var17.ridingEntity instanceof MCH_EntityGLTD) {
               MCH_EntityGLTD ac = (MCH_EntityGLTD)var17.ridingEntity;
               cameraMode = ac.camera.getMode(0);
            } else {
               cameraMode = 0;
            }

            // Hide smoke only for drawing; particles keep updating so existing smoke returns
            // immediately when the local camera leaves thermal vision.
            MCH_ThermalParticleFilter.beginRender();

            MCH_EntityBaseVehicle var19 = zoomContext.vehicle;

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
            if(!(var17.ridingEntity instanceof MCH_EntitySeat) && var19 != null && var19.canMouseRot()) {
               if(!isRideAircraft) {
                  var19.onInteractFirst(var17);
               }

               isRideAircraft = true;
               this.updateMouseDelta(var20, simDelta, zoomContext.getSensitivityMultiplier());
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

               if(var19 instanceof MCP_EntityPlane && MCP_PlaneChaseCamera.shouldConsumeFreelookMouse((MCP_EntityPlane)var19, var17)) {
                  MCP_PlaneChaseCamera.addFreelookMouseDelta((mouseDeltaX + prevMouseDeltaX) / 2.0D, (mouseDeltaY + prevMouseDeltaY) / 2.0D);
                  mouseDeltaX = 0.0D;
                  mouseDeltaY = 0.0D;
                  prevMouseDeltaX = 0.0D;
                  prevMouseDeltaY = 0.0D;
               }

               if(var19.getAcInfo() == null) {
                  var17.setAngles((float)mouseDeltaX, (float)mouseDeltaY);
               } else {
                  var19.setAngles(var17, var22, var23, var25, (float)(mouseDeltaX + prevMouseDeltaX) / 2.0F, (float)(mouseDeltaY + prevMouseDeltaY) / 2.0F, (float)mouseRollDeltaX, (float)mouseRollDeltaY, simDelta);
                  debugFlightControl(var19, simDelta, (float)mouseDeltaX, (float)mouseDeltaY, (float)mouseRollDeltaX, (float)mouseRollDeltaY);
               }

               if(!(var19 instanceof MCP_EntityPlane) || !MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)var19, var17)) {
                  var19.setupAllRiderRenderPosition(partialTicks, var17);
               }
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
               if(!(var19 instanceof MCP_EntityPlane) || !MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)var19, var17)) {
                  this.correctViewEntityDummy(var17);
               }
            } else {
               MCH_EntitySeat var21 = var17.ridingEntity instanceof MCH_EntitySeat?(MCH_EntitySeat)var17.ridingEntity:null;
               if(var21 != null && var21.getParent() != null) {
                  this.updateMouseDelta(var20, simDelta, zoomContext.getSensitivityMultiplier());
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

                  if(!(var19 instanceof MCP_EntityPlane) || !MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)var19, var17)) {
                     var19.setupAllRiderRenderPosition(partialTicks, var17);
                  }
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
                  if(!(var19 instanceof MCP_EntityPlane) || !MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)var19, var17)) {
                     this.correctViewEntityDummy(var17);
                  }
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
            if(var24 != null && (!(var19 instanceof MCP_EntityPlane) || !MCP_PlaneChaseCamera.isRenderCameraActiveFor((MCP_EntityPlane)var19, var17))) {
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
      if(player == super.mc.thePlayer) {
         MCH_BaseVehiclePacketHandler.tickPendingMounts(player);
         mcheli.network.packets.PacketVehicleMountGraph.tickClient(player);
         if(player.isDead) {
            MCH_MOD.proxy.clearVehicleLODSnapshots();
            MCH_BaseVehiclePacketHandler.clearPendingMounts();
            mcheli.network.packets.PacketVehicleMountGraph.clearClientQueue();
         }
      }
      if(player == super.mc.thePlayer && this.isHoldingMCHeliDismount()) {
         KeyBinding.setKeyBindState(super.mc.gameSettings.keyBindSneak.getKeyCode(), false);
         ((EntityClientPlayerMP)player).movementInput.sneak = false;
         this.suppressedDismountKey = true;
      }

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

   public void onPlayerTickPost(EntityPlayer player) {
      if(player == super.mc.thePlayer && this.suppressedDismountKey) {
         KeyBinding.setKeyBindState(super.mc.gameSettings.keyBindSneak.getKeyCode(),
               MCH_Key.isKeyDown(super.mc.gameSettings.keyBindSneak));
         this.suppressedDismountKey = false;
      }
   }

   private void updateDismountHoldState() {
      EntityClientPlayerMP player = super.mc.thePlayer;
      Entity mount = player.ridingEntity;
      boolean mcheliMount = mount instanceof MCH_EntityBaseVehicle || mount instanceof MCH_EntitySeat;
      boolean contextChanged = player != this.dismountPlayer || player.worldObj != this.dismountWorld
            || player.sendQueue != this.dismountConnection || mount != this.dismountMount;

      if(contextChanged) {
         this.resetDismountHoldState();
         this.dismountPlayer = player;
         this.dismountWorld = player.worldObj;
         this.dismountConnection = player.sendQueue;
         this.dismountMount = mount;
      }

      boolean pressed = MCH_Key.isKeyDown(super.mc.gameSettings.keyBindSneak);
      if(!mcheliMount || super.mc.currentScreen != null || !pressed) {
         this.dismountHoldStartNanos = -1L;
         this.dismountRequestPending = false;
         this.dismountHoldTriggered = false;
      } else if(this.dismountHoldStartNanos < 0L) {
         this.dismountHoldStartNanos = System.nanoTime();
      } else if(!this.dismountHoldTriggered
            && System.nanoTime() - this.dismountHoldStartNanos >= DISMOUNT_HOLD_NANOS) {
         this.dismountRequestPending = true;
         this.dismountHoldTriggered = true;
      }

      if(this.isHoldingMCHeliDismount()) {
         KeyBinding.setKeyBindState(super.mc.gameSettings.keyBindSneak.getKeyCode(), false);
         player.movementInput.sneak = false;
         this.suppressedDismountKey = true;
      }
   }

   private boolean isHoldingMCHeliDismount() {
      return this.dismountMount != null && this.dismountHoldStartNanos >= 0L
            && !this.dismountHoldTriggered;
   }

   private void resetDismountHoldState() {
      if(this.suppressedDismountKey) {
         KeyBinding.setKeyBindState(super.mc.gameSettings.keyBindSneak.getKeyCode(),
               MCH_Key.isKeyDown(super.mc.gameSettings.keyBindSneak));
      }
      this.dismountHoldStartNanos = -1L;
      this.dismountRequestPending = false;
      this.dismountHoldTriggered = false;
      this.suppressedDismountKey = false;
      this.dismountMount = null;
      this.dismountPlayer = null;
      this.dismountWorld = null;
      this.dismountConnection = null;
   }

   public boolean consumeDismountRequest(EntityPlayer player) {
      if(player == this.dismountPlayer && player.ridingEntity == this.dismountMount
            && this.dismountRequestPending) {
         this.dismountRequestPending = false;
         return true;
      }
      return false;
   }

   public int getDismountHoldRemainingSeconds(EntityPlayer player) {
      if(player == null || player != this.dismountPlayer || player != super.mc.thePlayer
            || player.ridingEntity != this.dismountMount || this.dismountHoldStartNanos < 0L
            || this.dismountHoldTriggered || super.mc.currentScreen != null
            || !MCH_Key.isKeyDown(super.mc.gameSettings.keyBindSneak)) {
         return 3;
      }

      long remainingNanos = DISMOUNT_HOLD_NANOS - (System.nanoTime() - this.dismountHoldStartNanos);
      int seconds = (int)Math.ceil((double)remainingNanos / 1000000000.0D);
      return Math.max(1, Math.min(3, seconds));
   }

   public void onRenderTickPost(float partialTicks) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ThermalParticleFilter.endRender();
         if(this.restoreMouseFocusAfterRender) {
            this.mc.inGameHasFocus = true;
            this.restoreMouseFocusAfterRender = false;
         }
         return;
      }
      MCH_ThermalParticleFilter.endRender();
      if(this.restoreMouseFocusAfterRender) {
         this.mc.inGameHasFocus = true;
         this.restoreMouseFocusAfterRender = false;
      }
      if (this.mc.thePlayer != null) {
         MCH_ClientTickHandlerBase.applyRotLimit((Entity)this.mc.thePlayer);
         MCH_ViewEntityDummy mCH_ViewEntityDummy = MCH_ViewEntityDummy.getInstance(this.mc.thePlayer.worldObj);
         MCP_EntityPlane activePlane = this.mc.thePlayer.ridingEntity instanceof MCP_EntityPlane?(MCP_EntityPlane)this.mc.thePlayer.ridingEntity:null;
         if (mCH_ViewEntityDummy != null && (activePlane == null || !MCP_PlaneChaseCamera.isRenderCameraActiveFor(activePlane, this.mc.thePlayer))) {
            ((Entity)mCH_ViewEntityDummy).rotationPitch = this.mc.thePlayer.rotationPitch;
            ((Entity)mCH_ViewEntityDummy).rotationYaw = this.mc.thePlayer.rotationYaw;
            ((Entity)mCH_ViewEntityDummy).prevRotationPitch = this.mc.thePlayer.prevRotationPitch;
            ((Entity)mCH_ViewEntityDummy).prevRotationYaw = this.mc.thePlayer.prevRotationYaw;
         }
         MCP_PlaneChaseCamera.applyActiveRenderCamera(this.mc);
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
      MCP_PlaneChaseCamera.enforceActiveRenderCameraOwnership(this.mc, "MCH_ClientCommonTickHandler.onRenderTickPost.final");
   }

   public boolean drawGui(MCH_Gui gui, float partialTicks) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         return false;
      }
      if(gui.isDrawGui(super.mc.thePlayer)) {
         gui.drawScreen(0, 0, partialTicks);
         return true;
      } else {
         return false;
      }
   }

   /** Draws the same first applicable vehicle GUI used by the gameplay overlay. */
   public boolean drawHudLayoutEditorPreview(float partialTicks) {
      if(this.mc.thePlayer == null) return false;
      boolean rendered = false;
      for(MCH_Gui gui : this.guis) {
         if(gui.isDrawGui(this.mc.thePlayer)) {
            gui.drawScreen(0, 0, partialTicks);
            rendered = true;
            break;
         }
      }
      if(MCH_MOD.proxy instanceof MCH_ClientProxy) {
         rendered |= ((MCH_ClientProxy)MCH_MOD.proxy).getRwrRenderer().renderHudLayoutEditorPreview(partialTicks);
      }
      return rendered;
   }

   private void releaseCameraAndControlForReplay() {
      MCP_PlaneChaseCamera.releaseForReplayPlayback(super.mc);
      MCP_ClientPlaneTickHandler.resetBombReticleMode();
      MCH_PlayerViewHandler.clearRecoil();
      W_Reflection.clearMCHeliCameraRollForReplayPlayback();
      MCH_Lib.enableFirstPersonItemRender();
      ridingAircraft = null;
      cameraMode = 0;
      isRideAircraft = false;
      isDrawScoreboard = false;
      mouseDeltaX = mouseDeltaY = 0.0D;
      prevMouseDeltaX = prevMouseDeltaY = 0.0D;
      mouseRollDeltaX = mouseRollDeltaY = 0.0D;
      this.resetDismountHoldState();
      if(this.Keys != null) {
         for(MCH_Key key : this.Keys) {
            key.reset();
         }
      }
      MCH_ReplayModCompat.logCameraOwnershipReleased();
   }

}
