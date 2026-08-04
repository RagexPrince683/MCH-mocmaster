package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_Key;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BaseVehicleClientTickHandler;
import mcheli.aircraft.MCH_EntitySeat;
import mcheli.aircraft.MCH_SeatInfo;
import mcheli.plane.MCP_EntityPlane;
import mcheli.plane.MCP_PlanePacketPlayerControl;
import mcheli.uav.MCH_EntityUavStation;
import mcheli.wrapper.W_Network;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import java.util.HashMap;
import java.util.Map;

public class MCP_ClientPlaneTickHandler extends MCH_BaseVehicleClientTickHandler {

   public MCH_Key KeySwitchMode;
   public MCH_Key KeyEjectSeat;
   public MCH_Key KeyZoom;
   public MCH_Key KeyMouseAim;
   public MCH_Key KeyBombReticleMode;
   public MCH_Key[] Keys;
   private static int bombReticlePlaneEntityId = -1;
   private static boolean bombReticleMode = false;
   public static final int BOMB_RETICLE_IMPACT_GRACE_TICKS = 20;
   private static final double BOMB_RETICLE_FALLBACK_MIN_DISTANCE = 32.0D;
   private static final double BOMB_RETICLE_FALLBACK_MAX_DISTANCE = 512.0D;
   private static final double BOMB_RETICLE_NO_TERRAIN_TARGET_Y = 0.0D;
   private static final Map bombReticleFallbacks = new HashMap();
   private final MCP_PlaneChaseCamera chaseCamera = new MCP_PlaneChaseCamera();
   private boolean wasUsingChaseCamera = false;


   public MCP_ClientPlaneTickHandler(Minecraft minecraft, MCH_Config config) {
      super(minecraft, config);
      this.updateKeybind(config);
   }

   public void updateKeybind(MCH_Config config) {
      super.updateKeybind(config);
      this.KeySwitchMode = new MCH_Key(MCH_Config.KeySwitchMode.prmInt);
      this.KeyEjectSeat = new MCH_Key(MCH_Config.KeySwitchHovering.prmInt);
      this.KeyZoom = new MCH_Key(MCH_Config.KeyZoom.prmInt);
      this.KeyMouseAim = new MCH_Key(MCH_Config.KeyPlaneMouseAim.prmInt);
      this.KeyBombReticleMode = new MCH_Key(MCH_Config.KeyBombReticleMode.prmInt);
      this.Keys = new MCH_Key[]{super.KeyUp, super.KeyDown, super.KeyRight, super.KeyLeft, this.KeySwitchMode, this.KeyEjectSeat, super.KeyUseWeapon,super.KeyCurrentWeaponLock, super.KeyVehicleLock, super.KeyRadar, super.KeySwWeaponMode, super.KeySwitchWeapon1, super.KeySwitchWeapon2, this.KeyZoom, this.KeyMouseAim, this.KeyBombReticleMode, super.KeyCameraMode, super.KeyUnmount, super.KeyUnmountForce, super.KeyFlare, super.KeyChaff,super.KeyAPS, super.KeyMaintenance, super.KeyExtra, super.KeyFreeLook, super.KeyGUI, super.KeyGearUpDown, super.KeyPutToRack, super.KeyDownFromRack};
   }

   protected void update(EntityPlayer player, MCP_EntityPlane plane) {
      if(plane.getIsGunnerMode(player)) {
         MCH_SeatInfo seatInfo = plane.getSeatInfo(player);
         if(seatInfo != null) {
            setRotLimitPitch(seatInfo.minPitch, seatInfo.maxPitch, player);
         }
      }

      plane.updateRadar(10);
      plane.updateCameraRotate(player.rotationYaw, player.rotationPitch);
   }

   protected void onTick(boolean inGUI) {
      MCH_Key[] player = this.Keys;
      int plane = player.length;

      for(int isPilot = 0; isPilot < plane; ++isPilot) {
         MCH_Key viewEntityDummy = player[isPilot];
         viewEntityDummy.update();
      }

      super.isBeforeRiding = super.isRiding;
      EntityClientPlayerMP var7 = super.mc.thePlayer;
      MCP_EntityPlane var8 = null;
      boolean var9 = true;
      if(var7 != null) {
         if(var7.ridingEntity instanceof MCP_EntityPlane) {
            var8 = (MCP_EntityPlane)var7.ridingEntity;
         } else if(var7.ridingEntity instanceof MCH_EntitySeat) {
            MCH_EntitySeat var10 = (MCH_EntitySeat)var7.ridingEntity;
            if(var10.getParent() instanceof MCP_EntityPlane) {
               var9 = false;
               var8 = (MCP_EntityPlane)var10.getParent();
            }
         } else if(var7.ridingEntity instanceof MCH_EntityUavStation) {
            MCH_EntityUavStation var11 = (MCH_EntityUavStation)var7.ridingEntity;
            if(var11.getControlAircract() instanceof MCP_EntityPlane) {
               var8 = (MCP_EntityPlane)var11.getControlAircract();
            }
         }
      }

      if(var8 != null && var8.getAcInfo() != null) {
         this.update(var7, var8);
         boolean useChaseCamera = this.chaseCamera.shouldUse(super.mc, var7, var8, var9);
         if(!useChaseCamera && this.wasUsingChaseCamera) {
            this.chaseCamera.reset();
            W_Reflection.setThirdPersonDistance(var8.thirdPersonDist);
         }
         MCH_ViewEntityDummy var12 = MCH_ViewEntityDummy.getInstance(super.mc.theWorld);
         if(!useChaseCamera) {
            var12.update(var8.camera);
         }
         if(!inGUI) {
            if(!var8.isDestroyed()) {
               this.playerControl(var7, var8, var9);
            }
         } else {
            this.playerControlInGUI(var7, var8, var9);
         }

         this.updateBombReticleMode(var7, var8, var9);
         this.forceBombReticleCamera(var7, var8, var9);

         boolean hideHand = true;
         if(useChaseCamera) {
            this.chaseCamera.update(super.mc, var7, var8);
         } else if((!var9 || !var8.isAlwaysCameraView()) && !var8.getIsGunnerMode(var7) && var8.getCameraId() <= 0) {
            MCH_Lib.setRenderViewEntity(var7);
            if(!var9 && var8.getCurrentWeaponID(var7) < 0) {
               hideHand = false;
            }
         } else {
            MCH_Lib.setRenderViewEntity(var12);
         }

         if(hideHand) {
            MCH_Lib.disableFirstPersonItemRender(var7.getCurrentEquippedItem());
         }

         this.wasUsingChaseCamera = useChaseCamera;
         super.isRiding = true;
      } else {
         if(this.wasUsingChaseCamera) {
            this.chaseCamera.reset();
            this.wasUsingChaseCamera = false;
         }
         super.isRiding = false;
         resetBombReticleMode();
      }

      if(!super.isBeforeRiding && super.isRiding && var8 != null && !this.wasUsingChaseCamera) {
         W_Reflection.setThirdPersonDistance(var8.thirdPersonDist);
         MCH_ViewEntityDummy.getInstance(super.mc.theWorld).setPosition(var8.posX, var8.posY + 0.5D, var8.posZ);
      } else if(super.isBeforeRiding && !super.isRiding) {
         W_Reflection.restoreDefaultThirdPersonDistance();
         MCH_Lib.enableFirstPersonItemRender();
         MCH_Lib.setRenderViewEntity(var7);
         W_Reflection.setCameraRoll(0.0F);
         this.chaseCamera.reset();
         this.wasUsingChaseCamera = false;
      }

   }

   private void updateBombReticleMode(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      if(player == null || plane == null || !isPilot || !plane.getIsGunnerMode(player)
            || plane.getPlaneInfo() == null || !plane.getPlaneInfo().hasBombSight
            || super.mc == null || super.mc.gameSettings == null || super.mc.gameSettings.thirdPersonView != 0) {
         resetBombReticleMode();
         return;
      }
      if(this.KeyBombReticleMode.isKeyDown()) {
         bombReticleMode = !bombReticleMode;
         bombReticlePlaneEntityId = bombReticleMode ? plane.getEntityId() : -1;
         playSoundOK();
      }
   }


   private void forceBombReticleCamera(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      if(mcheli.compat.MCH_ReplayModCompat.isReplayPlaybackActive()) {
         resetBombReticleMode();
         return;
      }
      if(player == null || plane == null || !isPilot || !isBombReticleMode(plane)) {
         clearBombReticleFallback(plane);
         return;
      }
      MCP_PlaneCCIPHelper.Result result = MCP_PlaneCCIPHelper.predictCurrentWeapon(plane, player);
      Vec3 target = null;
      int planeEntityId = plane.getEntityId();
      if(result != null && result.valid && result.impact != null) {
         target = result.hitRealTerrain ? result.impact : getNoTerrainBombReticleTarget(result);
         updateBombReticleFallback(planeEntityId, target, plane.ticksExisted);
      } else {
         target = getBombReticleFallbackTarget(plane, player);
      }
      if(target == null) {
         return;
      }
      Vec3 cameraPos = Vec3.createVectorHelper(plane.camera.posX, plane.camera.posY, plane.camera.posZ);
      double dx = target.xCoord - cameraPos.xCoord;
      double dy = target.yCoord - cameraPos.yCoord;
      double dz = target.zCoord - cameraPos.zCoord;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      if(horizontal < 1.0E-6D) {
         return;
      }
      float yaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(dy, horizontal) * 180.0D / Math.PI));
      pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
      applyBombReticleCameraRotation(player, plane, yaw, pitch);
   }

   private static void applyBombReticleCameraRotation(EntityPlayer player, MCP_EntityPlane plane, float yaw, float pitch) {
      player.prevRotationYaw = player.rotationYaw;
      player.prevRotationPitch = pitch;
      player.rotationYaw = yaw;
      player.rotationPitch = pitch;
      setRotLimitPitch(pitch, pitch, player);
      plane.updateCameraRotate(yaw, pitch);
   }

   private static void updateBombReticleFallback(int planeEntityId, Vec3 impact, int tick) {
      if(impact != null) {
         bombReticleFallbacks.put(Integer.valueOf(planeEntityId), new BombReticleFallback(impact, tick));
      }
   }

   private static Vec3 getNoTerrainBombReticleTarget(MCP_PlaneCCIPHelper.Result result) {
      if(result == null || result.releasePos == null || result.impact == null) {
         return result != null ? result.impact : null;
      }
      Vec3 start = result.releasePos;
      Vec3 end = result.impact;
      if(result.firstUnloadedPosition != null) {
         start = result.firstUnloadedPosition;
      }
      Vec3 target = interpolateBombReticleTargetAtY(start, end, BOMB_RETICLE_NO_TERRAIN_TARGET_Y);
      if(target != null) {
         return target;
      }
      return result.impact;
   }

   private static Vec3 interpolateBombReticleTargetAtY(Vec3 start, Vec3 end, double targetY) {
      if(start == null || end == null) {
         return null;
      }
      double dy = end.yCoord - start.yCoord;
      if(Math.abs(dy) < 1.0E-7D) {
         return Vec3.createVectorHelper(end.xCoord, targetY, end.zCoord);
      }
      double t = (targetY - start.yCoord) / dy;
      if(t < 0.0D) {
         t = 0.0D;
      }
      return Vec3.createVectorHelper(
            start.xCoord + (end.xCoord - start.xCoord) * t,
            targetY,
            start.zCoord + (end.zCoord - start.zCoord) * t);
   }

   private static Vec3 getBombReticleFallbackTarget(MCP_EntityPlane plane, EntityPlayer player) {
      int planeEntityId = plane.getEntityId();
      BombReticleFallback fallback = (BombReticleFallback)bombReticleFallbacks.get(Integer.valueOf(planeEntityId));
      if(fallback != null && plane.ticksExisted - fallback.tick <= BOMB_RETICLE_IMPACT_GRACE_TICKS) {
         return fallback.copyImpact();
      }
      Vec3 derived = deriveBombReticleCameraTarget(plane, player);
      updateBombReticleFallback(planeEntityId, derived, plane.ticksExisted);
      return derived;
   }

   private static Vec3 deriveBombReticleCameraTarget(MCP_EntityPlane plane, EntityPlayer player) {
      if(plane == null || player == null || plane.camera == null) {
         return null;
      }
      Vec3 cameraPos = Vec3.createVectorHelper(plane.camera.posX, plane.camera.posY, plane.camera.posZ);
      Vec3 motion = Vec3.createVectorHelper(plane.motionX, plane.motionY, plane.motionZ);
      double motionSpeed = motion.lengthVector();
      Vec3 forward = motionSpeed > 1.0E-4D ? motion : MCH_Lib.Rot2Vec3(plane.getRotYaw(), plane.getRotPitch());
      double forwardLength = forward.lengthVector();
      if(forwardLength < 1.0E-7D) {
         return null;
      }
      forward = Vec3.createVectorHelper(forward.xCoord / forwardLength, forward.yCoord / forwardLength, forward.zCoord / forwardLength);
      double horizontalSpeed = Math.sqrt(plane.motionX * plane.motionX + plane.motionZ * plane.motionZ);
      double distance = MathHelper.clamp_double(cameraPos.yCoord * 2.5D + horizontalSpeed * 80.0D,
            BOMB_RETICLE_FALLBACK_MIN_DISTANCE, BOMB_RETICLE_FALLBACK_MAX_DISTANCE);
      double gravityDrop = Math.max(0.0D, distance * 0.15D);
      Vec3 fallback = Vec3.createVectorHelper(
            cameraPos.xCoord + forward.xCoord * distance,
            cameraPos.yCoord + forward.yCoord * distance - gravityDrop,
            cameraPos.zCoord + forward.zCoord * distance);
      if(fallback.yCoord > BOMB_RETICLE_NO_TERRAIN_TARGET_Y) {
         fallback.yCoord = BOMB_RETICLE_NO_TERRAIN_TARGET_Y;
      }
      return fallback;
   }

   private static void clearBombReticleFallback(MCP_EntityPlane plane) {
      if(plane != null) {
         bombReticleFallbacks.remove(Integer.valueOf(plane.getEntityId()));
      }
   }

   private static class BombReticleFallback {
      private final Vec3 impact;
      private final int tick;

      private BombReticleFallback(Vec3 impact, int tick) {
         this.impact = Vec3.createVectorHelper(impact.xCoord, impact.yCoord, impact.zCoord);
         this.tick = tick;
      }

      private Vec3 copyImpact() {
         return Vec3.createVectorHelper(this.impact.xCoord, this.impact.yCoord, this.impact.zCoord);
      }
   }

   public static boolean isBombReticleMode(MCP_EntityPlane plane) {
      return plane != null && plane.getPlaneInfo() != null && plane.getPlaneInfo().hasBombSight
            && bombReticleMode && bombReticlePlaneEntityId == plane.getEntityId();
   }

   public static void resetBombReticleMode() {
      bombReticleMode = false;
      bombReticlePlaneEntityId = -1;
      bombReticleFallbacks.clear();
   }

   protected void playerControlInGUI(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      this.commonPlayerControlInGUI(player, plane, isPilot, new MCP_PlanePacketPlayerControl());
   }

   protected void playerControl(EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      MCP_PlanePacketPlayerControl pc = new MCP_PlanePacketPlayerControl();
      boolean send = false;
      send = this.commonPlayerControl(player, plane, isPilot, pc);
      boolean isUav;
      if(isPilot) {
         if(this.KeySwitchMode.isKeyDown()) {
            if(plane.getIsGunnerMode(player) && plane.canSwitchCameraPos()) {
               pc.switchMode = 0;
               plane.switchGunnerMode(false);
               send = true;
               plane.setCameraId(1);
            } else if(plane.getCameraId() > 0) {
               plane.setCameraId(plane.getCameraId() + 1);
               if(plane.getCameraId() >= plane.getCameraPosNum()) {
                  plane.setCameraId(0);
               }
            } else if(plane.canSwitchGunnerMode()) {
               pc.switchMode = (byte)(plane.getIsGunnerMode(player)?0:1);
               plane.switchGunnerMode(!plane.getIsGunnerMode(player));
               send = true;
               plane.setCameraId(0);
            } else if(plane.canSwitchCameraPos()) {
               plane.setCameraId(1);
            } else {
               playSoundNG();
            }
         }

         if(super.KeyExtra.isKeyDown()) {
            if(plane.canSwitchVtol()) {
               isUav = plane.getNozzleStat();
               if(!isUav) {
                  pc.switchVtol = 1;
               } else {
                  pc.switchVtol = 0;
               }

               plane.swithVtolMode(!isUav);
               send = true;
            } else if(plane.canUseCombatFlaps()) {
               plane.toggleCombatFlaps();
               pc.switchCombatFlaps = (byte)(plane.isCombatFlapsDeployed()?1:0);
               send = true;
            } else {
               playSoundNG();
            }
         }
      } else if(this.KeySwitchMode.isKeyDown()) {
         if(plane.canSwitchGunnerModeOtherSeat(player)) {
            plane.switchGunnerModeOtherSeat(player);
            send = true;
         } else {
            playSoundNG();
         }
      }

      if(this.KeyZoom.isKeyDown()) {
         isUav = plane.isUAV() && !plane.getAcInfo().haveHatch() && !plane.getPlaneInfo().haveWing();
         if(!plane.getIsGunnerMode(player) && !isUav) {
            if(isPilot) {
               if(plane.getAcInfo().haveHatch()) {
                  if(plane.canFoldHatch()) {
                     pc.switchHatch = 2;
                     send = true;
                  } else if(plane.canUnfoldHatch()) {
                     pc.switchHatch = 1;
                     send = true;
                  }
               } else if(plane.canFoldWing()) {
                  pc.switchHatch = 2;
                  send = true;
               } else if(plane.canUnfoldWing()) {
                  pc.switchHatch = 1;
                  send = true;
               }
            }
         } else {
            plane.zoomCamera();
            playSound("zoom", 0.5F, 1.0F);
         }
      }

      if(isPilot && this.KeyMouseAim.isKeyDown()) {
         if(plane.isNewFlightModelEnabled() && MCH_Config.EnableMouseAimControls.prmBool) {
            plane.toggleMouseAimControls();
            send = true;
            playSoundOK();
         } else {
            playSoundNG();
         }
      }

      if(this.KeyEjectSeat.isKeyDown() && plane.canEjectSeat(player)) {
         pc.ejectSeat = true;
         send = true;
      }

      if(send) {
         W_Network.sendToServer(pc);
      }

   }
}
