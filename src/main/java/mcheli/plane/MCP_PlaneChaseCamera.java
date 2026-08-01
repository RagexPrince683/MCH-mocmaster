package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_Key;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BoundingBox;
import mcheli.compat.MCH_ReplayModCompat;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MCP_PlaneChaseCamera {

   private static MCP_PlaneChaseCamera activeCamera;
   private static MCP_EntityPlane activeRenderPlane;
   private static boolean consumedByRenderHook;
   private static String currentOwner = "NONE";
   private static String lastWriterMethod = "NONE";
   private static long nextOrientDebugTime;
   private static int savedThirdPersonView;
   private static boolean renderTickBypassActive;
   private static int dummyTransformWrites;
   private static boolean allowNextChaseDummyWrite;
   private static double desiredX;
   private static double desiredY;
   private static double desiredZ;
   private static MCH_ViewEntityDummy activeDummy;
   private static long nextProofLogTime;
   private static long nextStageProofLogTime;
   private static long nextWriteLogTime;
   private static long nextPhaseLogTime;
   private static long lastClientTickComputed = -1L;
   private static boolean renderStartAppliedThisPhase;
   private static double pendingFreelookMouseX;
   private static double pendingFreelookMouseY;
   private long nextDebugTime;

   private MCP_EntityPlane activePlane;
   private int activeView = -1;
   private double posX;
   private double posY;
   private double posZ;
   private float yaw;
   private float pitch;
   private boolean initialized;
   private boolean lastCollisionAdjusted;
   private String lastCollisionHit = "none";
   private Vec3 smoothedFocus;
   private double smoothedDistance;
   private double lastDistanceBeforeCollision;
   private double lastDistanceAfterCollision;
   private float smoothedPitchInfluence;
   private double lookAheadBlend;
   private boolean freelookActive;
   private boolean wasFreelookActive;
   private float orbitYawOffset;
   private float orbitPitchOffset;
   private float smoothedOrbitYawOffset;
   private float smoothedOrbitPitchOffset;
   private double orbitDirX;
   private double orbitDirY;
   private double orbitDirZ;
   private double lastFinalChaseDistance;
   private double lastBaseChaseDistance;
   private double lastSizeDistanceBonus;
   private double lastSpeedDistanceBonus;
   private double lastDesiredDistance;
   private double lastCollisionAdjustedDistance;
   private float smoothedFov = -1.0F;
   private float lastFovTarget;
   private double lastDesiredX;
   private double lastDesiredY;
   private double lastDesiredZ;
   private double renderPosX;
   private double renderPosY;
   private double renderPosZ;
   private double renderPrevPosX;
   private double renderPrevPosY;
   private double renderPrevPosZ;
   private float renderYaw;
   private float renderPitch;
   private float renderRoll;
   private float renderZoom = 1.0F;
   private boolean hasRenderTransform;

   public boolean shouldUse(Minecraft mc, EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      boolean thirdPerson = mc != null && mc.gameSettings != null && mc.gameSettings.thirdPersonView > 0;
      boolean hasPlayer = player != null;
      boolean hasPlane = plane != null;
      boolean enabled = MCH_Config.EnableNewPlaneThirdPersonCamera.prmBool;
      boolean newFlight = hasPlane && plane.isNewFlightModelEnabled();
      boolean notGunner = hasPlane && hasPlayer && !plane.getIsGunnerMode(player);
      boolean cameraOk = hasPlane && plane.getCameraId() <= 0;
      boolean notDestroyed = hasPlane && !plane.isDestroyed();
      boolean result = thirdPerson && hasPlayer && hasPlane && isPilot && enabled && newFlight && notGunner && cameraOk && notDestroyed;
      logShouldUseProof(mc, plane, result, thirdPerson, hasPlayer, hasPlane, isPilot, enabled, newFlight, notGunner, cameraOk, notDestroyed);
      return result;
   }

   public void reset() {
      this.activePlane = null;
      this.activeView = -1;
      this.initialized = false;
      this.hasRenderTransform = false;
      this.lookAheadBlend = 0.0D;
      this.freelookActive = false;
      this.wasFreelookActive = false;
      this.orbitYawOffset = 0.0F;
      this.orbitPitchOffset = 0.0F;
      this.smoothedOrbitYawOffset = 0.0F;
      this.smoothedOrbitPitchOffset = 0.0F;
      W_Reflection.setCameraRoll(0.0F);
      if(activeCamera == this) {
         activeCamera = null;
         activeRenderPlane = null;
         activeDummy = null;
         consumedByRenderHook = false;
      }
   }

   public void update(Minecraft mc, EntityPlayer player, MCP_EntityPlane plane) {
      if(!this.initialized || this.activePlane != plane || this.activeView != mc.gameSettings.thirdPersonView) {
         this.initialize(plane);
      }

      long clientTick = plane.worldObj != null?plane.worldObj.getTotalWorldTime():-1L;
      if(clientTick == lastClientTickComputed && activeRenderPlane == plane) {
         logPhase("CLIENT_TICK", mc, activeDummy, false, false);
         return;
      }
      lastClientTickComputed = clientTick;

      this.wasFreelookActive = this.freelookActive;
      this.freelookActive = shouldUseHoldFreelookAsCameraOnly(plane, player);
      this.updateFreelookOrbit();
      Vec3 anchor = this.getCameraFocusPoint(plane);
      Vec3 focus = this.computeHeldLookAheadFocus(plane, anchor);
      this.smoothedFocus = this.smoothVec(this.smoothedFocus, focus, MCH_Config.NewPlaneCameraFocusSmoothing.prmDouble);
      if(this.smoothedFocus != null) {
         focus = this.smoothedFocus;
      }
      Vec3 aimFocus = this.getAimFocus(focus);
      Vec3 desired = this.computeDesiredCameraPosition(plane, focus);
      this.lastDistanceBeforeCollision = this.distance(focus, desired);
      if(MCH_Config.DebugFlightControl.prmBool && MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool) {
         desired = Vec3.createVectorHelper(plane.posX, plane.posY + 20.0D, plane.posZ);
         this.lastCollisionAdjusted = false;
         this.lastCollisionHit = "debugAbovePlane";
      } else {
         if(MCH_Config.EnableNewPlaneCameraCollision.prmBool && MCH_Config.NewPlaneCameraCollision.prmBool) {
            desired = this.adjustForCollision(plane, focus, desired);
         } else {
            this.lastCollisionAdjusted = false;
            this.lastCollisionHit = "disabled";
         }
         desired = this.escapeAircraftCollision(plane, focus, desired);
      }
      this.lastDistanceAfterCollision = this.distance(focus, desired);
      this.lastCollisionAdjustedDistance = this.lastDistanceAfterCollision;
      this.lastFinalChaseDistance = this.lastDistanceAfterCollision;
      this.lastDesiredX = desired.xCoord;
      this.lastDesiredY = desired.yCoord;
      this.lastDesiredZ = desired.zCoord;
      desired = this.smoothVec(Vec3.createVectorHelper(this.posX, this.posY, this.posZ), desired, MCH_Config.NewPlaneCameraPositionSmoothing.prmDouble);
      activeCamera = this;
      activeRenderPlane = plane;
      desiredX = desired.xCoord;
      desiredY = desired.yCoord;
      desiredZ = desired.zCoord;
      currentOwner = "CHASE";
      dummyTransformWrites = 0;
      consumedByRenderHook = false;

      float targetYaw = this.computeLookYaw(desired, aimFocus);
      float targetPitch = this.computeLookPitch(desired, aimFocus);
      double yawSmooth = this.wasFreelookActive && !this.freelookActive?MCH_Config.FreelookReturnSmoothing.prmDouble:MCH_Config.NewPlaneCameraYawSmoothing.prmDouble;
      double pitchSmooth = this.wasFreelookActive && !this.freelookActive?MCH_Config.FreelookReturnSmoothing.prmDouble:MCH_Config.NewPlaneCameraPitchSmoothing.prmDouble;
      this.yaw = this.smoothAngle(this.yaw, targetYaw, yawSmooth);
      this.pitch = this.smoothAngle(this.pitch, targetPitch, pitchSmooth);
      this.posX = desired.xCoord;
      this.posY = desired.yCoord;
      this.posZ = desired.zCoord;

      this.storeRenderTransform(plane);
      this.mirrorRenderTransformToPlaneCamera(plane);
      logPhase("CLIENT_TICK", mc, activeDummy, false, false);
      this.debugCamera(mc, plane, focus, desired, true);
      W_Reflection.setCameraRoll(this.getRollInfluence(plane));
   }

   private static void logShouldUseProof(Minecraft mc, MCP_EntityPlane plane, boolean result, boolean thirdPerson, boolean hasPlayer, boolean hasPlane, boolean isPilot, boolean enabled, boolean newFlight, boolean notGunner, boolean cameraOk, boolean notDestroyed) {
      if(!MCH_Config.DebugFlightControl.prmBool || System.currentTimeMillis() < nextProofLogTime) {
         return;
      }
      nextProofLogTime = System.currentTimeMillis() + 1000L;
      String renderView = mc != null && mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][PROOF] shouldUse=%s DebugFlightControl=%s NewPlaneCameraDebugAbovePlane=%s planeId=%d renderView=%s conditions thirdPersonView>0=%s player!=null=%s plane!=null=%s isPilot=%s EnableNewPlaneThirdPersonCamera=%s newFlight=%s notGunner=%s cameraId<=0=%s notDestroyed=%s",
            new Object[]{Boolean.valueOf(result), Boolean.valueOf(MCH_Config.DebugFlightControl.prmBool), Boolean.valueOf(MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool), Integer.valueOf(plane != null?plane.getEntityId():-1), renderView,
                  Boolean.valueOf(thirdPerson), Boolean.valueOf(hasPlayer), Boolean.valueOf(hasPlane), Boolean.valueOf(isPilot), Boolean.valueOf(enabled), Boolean.valueOf(newFlight), Boolean.valueOf(notGunner), Boolean.valueOf(cameraOk), Boolean.valueOf(notDestroyed)});
   }

   private static void logStageProof(Minecraft mc, String label, MCP_EntityPlane plane, boolean shouldUse) {
      if(!MCH_Config.DebugFlightControl.prmBool || System.currentTimeMillis() < nextStageProofLogTime) {
         return;
      }
      nextStageProofLogTime = System.currentTimeMillis() + 1000L;
      String renderView = mc != null && mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][PROOF] %s DebugFlightControl=%s NewPlaneCameraDebugAbovePlane=%s shouldUse=%s planeId=%d renderView=%s",
            new Object[]{label, Boolean.valueOf(MCH_Config.DebugFlightControl.prmBool), Boolean.valueOf(MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool), Boolean.valueOf(shouldUse), Integer.valueOf(plane != null?plane.getEntityId():-1), renderView});
   }

   public static void logCameraWrite(String methodName, String stage) {
      Minecraft mc = Minecraft.getMinecraft();
      if(MCH_Config.DebugFlightControl != null && MCH_Config.DebugFlightControl.prmBool && System.currentTimeMillis() >= nextWriteLogTime) {
         nextWriteLogTime = System.currentTimeMillis() + 1000L;
         String renderView = mc != null && mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
         MCH_Lib.Log("[MCHeli][PlaneChaseCamera][WRITE] method=%s stage=%s active=%s renderView=%s thirdPersonDistance=%.3f thirdPersonDistanceTemp=%.3f",
               new Object[]{methodName, stage, Boolean.valueOf(activeCamera != null), renderView, Float.valueOf(W_Reflection.getThirdPersonDistance()), Float.valueOf(W_Reflection.getThirdPersonDistanceTemp())});
      }
   }

   private void initialize(MCP_EntityPlane plane) {
      Vec3 focus = this.getCameraFocusPoint(plane);
      Vec3 desired = this.computeDesiredCameraPosition(plane, focus);
      this.posX = desired.xCoord;
      this.posY = desired.yCoord;
      this.posZ = desired.zCoord;
      this.yaw = plane.getRotYaw();
      this.pitch = this.computeLookPitch(desired, focus);
      this.smoothedFocus = focus;
      this.smoothedDistance = this.distance(focus, desired);
      this.smoothedPitchInfluence = plane.getRotPitch();
      this.activePlane = plane;
      this.activeView = Minecraft.getMinecraft().gameSettings.thirdPersonView;
      this.initialized = true;
   }

   private void storeRenderTransform(MCP_EntityPlane plane) {
      if(this.hasRenderTransform) {
         this.renderPrevPosX = this.renderPosX;
         this.renderPrevPosY = this.renderPosY;
         this.renderPrevPosZ = this.renderPosZ;
      } else {
         this.renderPrevPosX = this.posX;
         this.renderPrevPosY = this.posY;
         this.renderPrevPosZ = this.posZ;
      }
      this.renderPosX = this.posX;
      this.renderPosY = this.posY;
      this.renderPosZ = this.posZ;
      this.renderYaw = this.yaw;
      this.renderPitch = this.pitch;
      this.renderRoll = plane != null?this.getRollInfluence(plane):0.0F;
      this.renderZoom = plane != null && plane.camera != null?plane.camera.getCameraZoom():1.0F;
      this.hasRenderTransform = true;
   }

   private void mirrorRenderTransformToPlaneCamera(MCP_EntityPlane plane) {
      if(plane == null || plane.camera == null || !this.hasRenderTransform) {
         return;
      }
      plane.camera.prevRotationYaw = plane.camera.rotationYaw;
      plane.camera.prevRotationPitch = plane.camera.rotationPitch;
      plane.camera.rotationYaw = this.renderYaw;
      plane.camera.rotationPitch = this.renderPitch;
      plane.camera.setCameraZoom(this.renderZoom);
      plane.camera.setPosition(this.renderPosX, this.renderPosY, this.renderPosZ);
   }

   private Vec3 getCameraFocusPoint(MCP_EntityPlane plane) {
      MCP_PlaneInfo info = plane.getPlaneInfo();
      double ox = info != null?(double)info.newPlaneCameraFocusOffsetX:0.0D;
      double oy = info != null?(double)info.newPlaneCameraFocusOffsetY:1.0D;
      double oz = info != null?(double)info.newPlaneCameraFocusOffsetZ:0.0D;
      float yawRad = plane.getRotYaw() * 0.017453292F;
      double sin = (double)MathHelper.sin(yawRad);
      double cos = (double)MathHelper.cos(yawRad);
      double worldX = plane.posX + ox * cos - oz * sin;
      double worldY = plane.posY + oy + MCH_Config.PlaneChaseFocusVerticalOffset.prmDouble;
      double worldZ = plane.posZ + ox * sin + oz * cos;
      return Vec3.createVectorHelper(worldX, worldY, worldZ);
   }

   private double getCameraDistance(MCP_EntityPlane plane) {
      if(MCH_Config.DebugFlightControl.prmBool && MCH_Config.NewPlaneCameraDebugDistance.prmDouble > 0.0D) {
         return MCH_Config.NewPlaneCameraDebugDistance.prmDouble;
      }
      double dx = plane.posX - plane.prevPosX;
      double dy = plane.posY - plane.prevPosY;
      double dz = plane.posZ - plane.prevPosZ;
      double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
      this.lastBaseChaseDistance = MCH_Config.NewPlaneCameraDistance.prmDouble;
      this.lastSizeDistanceBonus = this.getAircraftSize(plane) * MCH_Config.NewPlaneCameraSizeDistanceScale.prmDouble;
      this.lastSpeedDistanceBonus = 0.0D;
      double target = this.lastBaseChaseDistance + this.lastSizeDistanceBonus;
      if(MCH_Config.EnableNewPlaneCameraSpeedDistance.prmBool) {
         this.lastSpeedDistanceBonus = Math.min(speed * MCH_Config.PlaneChaseSpeedDistanceScale.prmDouble, MCH_Config.PlaneChaseSpeedDistanceMaxBonus.prmDouble);
         target += this.lastSpeedDistanceBonus;
      }
      this.lastDesiredDistance = target;
      target = MCH_Lib.RNG(target, MCH_Config.NewPlaneCameraMinDistance.prmDouble, MCH_Config.NewPlaneCameraMaxDistance.prmDouble);
      if(this.smoothedDistance <= 0.0D) {
         this.smoothedDistance = target;
      } else {
         this.smoothedDistance += (target - this.smoothedDistance) * MCH_Config.NewPlaneCameraDistanceSmoothing.prmDouble;
      }
      return this.smoothedDistance;
   }

   private Vec3 computeDesiredCameraPosition(MCP_EntityPlane plane, Vec3 focus) {
      double distance = this.getCameraDistance(plane);
      double height = MCH_Config.NewPlaneCameraHeight.prmDouble;
      double side = MCH_Config.NewPlaneCameraSideOffset.prmDouble;
      this.smoothedPitchInfluence += (plane.getRotPitch() - this.smoothedPitchInfluence) * (float)MCH_Config.NewPlaneCameraPitchInfluenceSmoothing.prmDouble;
      Vec3 forward = this.getOrbitDirection(plane);
      Vec3 right = MCH_Lib.Rot2Vec3(plane.getRotYaw() + 90.0F, 0.0F);
      double x = focus.xCoord - forward.xCoord * distance + right.xCoord * side;
      double y = focus.yCoord - forward.yCoord * distance + height;
      double z = focus.zCoord - forward.zCoord * distance + right.zCoord * side;
      return Vec3.createVectorHelper(x, y, z);
   }

   private Vec3 getAimFocus(Vec3 focus) {
      return focus.addVector(0.0D, MCH_Config.PlaneChaseScreenVerticalBias.prmDouble, 0.0D);
   }

   private Vec3 getOrbitDirection(MCP_EntityPlane plane) {
      float yaw = plane.getRotYaw() + this.smoothedOrbitYawOffset;
      float pitchInfluence = this.freelookActive || Math.abs(this.smoothedOrbitYawOffset) > 0.01F || Math.abs(this.smoothedOrbitPitchOffset) > 0.01F?0.0F:this.smoothedPitchInfluence * (float)MCH_Config.PlaneChasePitchInfluence.prmDouble;
      float pitch = pitchInfluence + this.smoothedOrbitPitchOffset;
      Vec3 dir = MCH_Lib.Rot2Vec3(yaw, pitch);
      this.orbitDirX = dir.xCoord;
      this.orbitDirY = dir.yCoord;
      this.orbitDirZ = dir.zCoord;
      return dir;
   }

   private void updateFreelookOrbit() {
      if(this.freelookActive) {
         double sensitivity = MCH_Config.PlaneFreelookOrbitSensitivity.prmDouble;
         this.orbitYawOffset += (float)(pendingFreelookMouseX * sensitivity);
         this.orbitPitchOffset += (float)(pendingFreelookMouseY * sensitivity);
         this.orbitPitchOffset = MathHelper.clamp_float(this.orbitPitchOffset, -(float)MCH_Config.PlaneFreelookMaxPitchDown.prmDouble, (float)MCH_Config.PlaneFreelookMaxPitchUp.prmDouble);
         pendingFreelookMouseX = 0.0D;
         pendingFreelookMouseY = 0.0D;
      } else {
         this.orbitYawOffset = this.smoothAngle(this.orbitYawOffset, 0.0F, MCH_Config.PlaneFreelookReturnSmoothing.prmDouble);
         this.orbitPitchOffset = this.smoothAngle(this.orbitPitchOffset, 0.0F, MCH_Config.PlaneFreelookReturnSmoothing.prmDouble);
         if(Math.abs(this.orbitYawOffset) < 0.01F) {
            this.orbitYawOffset = 0.0F;
         }
         if(Math.abs(this.orbitPitchOffset) < 0.01F) {
            this.orbitPitchOffset = 0.0F;
         }
      }
      double yawSmoothing = this.freelookActive?MCH_Config.PlaneFreelookYawSmoothing.prmDouble:MCH_Config.PlaneFreelookReturnSmoothing.prmDouble;
      double pitchSmoothing = this.freelookActive?MCH_Config.PlaneFreelookPitchSmoothing.prmDouble:MCH_Config.PlaneFreelookReturnSmoothing.prmDouble;
      this.smoothedOrbitYawOffset = this.smoothAngle(this.smoothedOrbitYawOffset, this.orbitYawOffset, yawSmoothing);
      this.smoothedOrbitPitchOffset = this.smoothAngle(this.smoothedOrbitPitchOffset, this.orbitPitchOffset, pitchSmoothing);
   }

   private Vec3 computeHeldLookAheadFocus(MCP_EntityPlane plane, Vec3 anchor) {
      boolean held = MCH_Config.EnablePlaneLookAhead.prmBool && MCH_Key.isKeyDown(MCH_Config.KeyPlaneLookAhead.prmInt);
      double smoothing = held?MCH_Config.PlaneLookAheadSmoothing.prmDouble:MCH_Config.PlaneLookAheadReturnSmoothing.prmDouble;
      double target = held?1.0D:0.0D;
      this.lookAheadBlend += (target - this.lookAheadBlend) * MCH_Lib.RNG(smoothing, 0.01D, 1.0D);
      if(this.lookAheadBlend < 1.0E-3D) {
         this.lookAheadBlend = 0.0D;
         return anchor;
      }
      Vec3 forward = MCH_Lib.Rot2Vec3(plane.getRotYaw(), plane.getRotPitch() * 0.35F);
      double distance = MCH_Config.PlaneLookAheadDistance.prmDouble * this.lookAheadBlend;
      return anchor.addVector(forward.xCoord * distance, forward.yCoord * distance, forward.zCoord * distance);
   }

   private Vec3 adjustForCollision(MCP_EntityPlane plane, Vec3 focus, Vec3 desired) {
      Vec3 start = focus.addVector(0.0D, 0.5D, 0.0D);
      MovingObjectPosition hit = plane.worldObj.rayTraceBlocks(start, desired, false);
      this.lastCollisionAdjusted = false;
      this.lastCollisionHit = "none";
      if(hit != null && hit.hitVec != null) {
         double hitDistance = this.distance(start, hit.hitVec);
         if(hitDistance > 1.0D) {
            Vec3 away = Vec3.createVectorHelper(start.xCoord - hit.hitVec.xCoord, start.yCoord - hit.hitVec.yCoord, start.zCoord - hit.hitVec.zCoord).normalize();
            Vec3 adjusted = hit.hitVec.addVector(away.xCoord * 0.35D, away.yCoord * 0.35D, away.zCoord * 0.35D);
            this.lastCollisionAdjusted = true;
            this.lastCollisionHit = "block@" + hit.blockX + "," + hit.blockY + "," + hit.blockZ;
            return this.enforceMinimumDistance(focus, desired, adjusted);
         }
      }
      return this.enforceMinimumDistance(focus, desired, desired);
   }

   private Vec3 escapeAircraftCollision(MCP_EntityPlane plane, Vec3 focus, Vec3 desired) {
      Vec3 escaped = desired;
      String hit = this.getAircraftCollisionHit(plane, escaped);
      if(hit.equals("none")) {
         return escaped;
      }
      Vec3 chaseDirection = Vec3.createVectorHelper(desired.xCoord - focus.xCoord, desired.yCoord - focus.yCoord, desired.zCoord - focus.zCoord);
      if(chaseDirection.lengthVector() < 1.0E-4D) {
         chaseDirection = MCH_Lib.Rot2Vec3(plane.getRotYaw(), 0.0F);
      } else {
         chaseDirection = chaseDirection.normalize();
      }
      for(int i = 0; i < 64 && !hit.equals("none"); ++i) {
         escaped = escaped.addVector(chaseDirection.xCoord, chaseDirection.yCoord, chaseDirection.zCoord);
         hit = this.getAircraftCollisionHit(plane, escaped);
      }
      this.lastCollisionAdjusted = true;
      this.lastCollisionHit = hit.equals("none")?"aircraftEscape":hit;
      return this.enforceMinimumDistance(focus, desired, escaped);
   }

   private String getAircraftCollisionHit(MCP_EntityPlane plane, Vec3 point) {
      if(this.isPointInsideAabb(point, plane.boundingBox)) {
         return "planeBB";
      }
      MCH_BoundingBox[] boxes = plane.getCalculatedExtraBoundingBoxes();
      for(int i = 0; i < boxes.length; ++i) {
         MCH_BoundingBox box = boxes[i];
         if(box != null && box.contains(point)) {
            return "partBB#" + i;
         }
      }
      return "none";
   }

   private boolean isPointInsideAabb(Vec3 point, AxisAlignedBB bb) {
      return bb != null && point.xCoord >= bb.minX && point.xCoord <= bb.maxX && point.yCoord >= bb.minY && point.yCoord <= bb.maxY && point.zCoord >= bb.minZ && point.zCoord <= bb.maxZ;
   }

   private double distanceToAabb(Vec3 point, AxisAlignedBB bb) {
      if(bb == null) {
         return Double.MAX_VALUE;
      }
      double dx = point.xCoord < bb.minX?bb.minX - point.xCoord:(point.xCoord > bb.maxX?point.xCoord - bb.maxX:0.0D);
      double dy = point.yCoord < bb.minY?bb.minY - point.yCoord:(point.yCoord > bb.maxY?point.yCoord - bb.maxY:0.0D);
      double dz = point.zCoord < bb.minZ?bb.minZ - point.zCoord:(point.zCoord > bb.maxZ?point.zCoord - bb.maxZ:0.0D);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private double distanceToObb(Vec3 point, MCH_BoundingBox box) {
      if(box == null) {
         return Double.MAX_VALUE;
      }
      return box.distanceTo(point);
   }

   private double nearestAircraftBoxDistance(MCP_EntityPlane plane, Vec3 point) {
      double nearest = this.distanceToAabb(point, plane.boundingBox);
      MCH_BoundingBox[] boxes = plane.getCalculatedExtraBoundingBoxes();
      for(int i = 0; i < boxes.length; ++i) {
         MCH_BoundingBox box = boxes[i];
         if(box != null) {
            nearest = Math.min(nearest, this.distanceToObb(point, box));
         }
      }
      return nearest;
   }

   private double getAircraftSize(MCP_EntityPlane plane) {
      AxisAlignedBB bb = plane.boundingBox;
      if(bb == null) {
         return 0.0D;
      }
      double sx = bb.maxX - bb.minX;
      double sy = bb.maxY - bb.minY;
      double sz = bb.maxZ - bb.minZ;
      MCH_BoundingBox[] boxes = plane.getCalculatedExtraBoundingBoxes();
      for(int i = 0; i < boxes.length; ++i) {
         if(boxes[i] != null && boxes[i].boundingBox != null) {
            AxisAlignedBB b = boxes[i].boundingBox;
            sx = Math.max(sx, b.maxX - b.minX);
            sy = Math.max(sy, b.maxY - b.minY);
            sz = Math.max(sz, b.maxZ - b.minZ);
         }
      }
      return Math.max(sx, Math.max(sy, sz));
   }

   private float computeLookYaw(Vec3 camera, Vec3 focus) {
      double dx = focus.xCoord - camera.xCoord;
      double dz = focus.zCoord - camera.zCoord;
      return (float)(Math.atan2(dz, dx) * 57.29577951308232D) - 90.0F;
   }

   private float computeLookPitch(Vec3 camera, Vec3 focus) {
      double dx = focus.xCoord - camera.xCoord;
      double dy = focus.yCoord - camera.yCoord;
      double dz = focus.zCoord - camera.zCoord;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      return MathHelper.clamp_float((float)(-Math.atan2(dy, horizontal) * 57.29577951308232D), -35.0F, 35.0F);
   }

   private Vec3 enforceMinimumDistance(Vec3 focus, Vec3 desired, Vec3 camera) {
      double minDistance = MCH_Config.NewPlaneCameraMinDistance.prmDouble;
      if(this.distance(focus, camera) >= minDistance) {
         return camera;
      }
      Vec3 direction = Vec3.createVectorHelper(desired.xCoord - focus.xCoord, desired.yCoord - focus.yCoord, desired.zCoord - focus.zCoord).normalize();
      return focus.addVector(direction.xCoord * minDistance, direction.yCoord * minDistance, direction.zCoord * minDistance);
   }

   private Vec3 smoothVec(Vec3 from, Vec3 to, double amount) {
      if(from == null || to == null) {
         return to;
      }
      amount = MCH_Lib.RNG(amount, 0.01D, 1.0D);
      return Vec3.createVectorHelper(from.xCoord + (to.xCoord - from.xCoord) * amount, from.yCoord + (to.yCoord - from.yCoord) * amount, from.zCoord + (to.zCoord - from.zCoord) * amount);
   }

   private float smoothAngle(float from, float to, double amount) {
      amount = MCH_Lib.RNG(amount, 0.01D, 1.0D);
      return from + MathHelper.wrapAngleTo180_float(to - from) * (float)amount;
   }

   private float getRollInfluence(MCP_EntityPlane plane) {
      return MCH_Config.EnableNewPlaneCameraRollInfluence.prmBool?plane.getRotRoll() * (float)MCH_Config.NewPlaneCameraRollInfluence.prmDouble:0.0F;
   }

   private boolean isHoldFreelookActive() {
      return MCH_Config.EnableHoldFreelook.prmBool && MCH_Key.isKeyDown(MCH_Config.KeyFreeLook.prmInt);
   }

   private double distance(Vec3 a, Vec3 b) {
      double dx = a.xCoord - b.xCoord;
      double dy = a.yCoord - b.yCoord;
      double dz = a.zCoord - b.zCoord;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private void debugCamera(Minecraft mc, MCP_EntityPlane plane, Vec3 focus, Vec3 desired, boolean shouldUse) {
      if(MCH_Config.DebugFlightControl.prmBool && System.currentTimeMillis() >= this.nextDebugTime) {
         this.nextDebugTime = System.currentTimeMillis() + 1000L;
         MCH_ViewEntityDummy dummy = mc != null && mc.theWorld != null?MCH_ViewEntityDummy.getInstance(mc.theWorld):null;
         double dist = dummy != null?this.distance(Vec3.createVectorHelper(dummy.posX, dummy.posY, dummy.posZ), desired):0.0D;
         boolean dummyInsidePlaneBB = dummy != null && this.isPointInsideAabb(Vec3.createVectorHelper(dummy.posX, dummy.posY, dummy.posZ), plane.boundingBox);
         boolean dummyInsidePartBB = dummy != null && !this.getAircraftCollisionHit(plane, Vec3.createVectorHelper(dummy.posX, dummy.posY, dummy.posZ)).equals("none") && !dummyInsidePlaneBB;
         double nearestBoxDistance = this.nearestAircraftBoxDistance(plane, desired);
         MCH_Lib.Log("CHASE_CAM: phase=CLIENT_TICK fovOverride=%s fovTarget=%.2f smoothedFov=%.2f baseDistance=%.2f sizeBonus=%.2f speedBonus=%.2f desiredDistance=%.2f smoothedDistance=%.2f collisionAdjustedDistance=%.2f freelook=%s freelookRaw=(%.2f,%.2f) freelookSmoothed=(%.2f,%.2f) freelookReturning=%s rollInfluence=%.2f pitchInfluence=%.2f focus=(%.3f, %.3f, %.3f) desired=(%.3f, %.3f, %.3f) smoothed=(%.3f, %.3f, %.3f) distBeforeCollision=%.3f distAfterCollision=%.3f yaw=%.2f pitch=%.2f lookAheadBlend=%.2f focusYOffset=%.2f screenBias=%.2f orbitDir=(%.3f,%.3f,%.3f) finalDummy=(%.3f, %.3f, %.3f) renderView=(%.3f, %.3f, %.3f) planeBB=(%.3f, %.3f, %.3f -> %.3f, %.3f, %.3f) dummyBB=(%.3f, %.3f, %.3f -> %.3f, %.3f, %.3f) dummyInsidePlaneBB=%s dummyInsidePartBB=%s collisionAdjusted=%s collisionHit=%s nearestAircraftBoxDistance=%.3f thirdPersonViewMasked=%s distanceToFocus=%.3f owner=%s lastWriter=%s writes=%d thirdPerson=%d",
               new Object[]{Boolean.valueOf(isFovOverrideActive()), Float.valueOf(this.lastFovTarget), Float.valueOf(this.smoothedFov), Double.valueOf(this.lastBaseChaseDistance), Double.valueOf(this.lastSizeDistanceBonus), Double.valueOf(this.lastSpeedDistanceBonus), Double.valueOf(this.lastDesiredDistance), Double.valueOf(this.smoothedDistance), Double.valueOf(this.lastCollisionAdjustedDistance), Boolean.valueOf(this.freelookActive), Float.valueOf(this.orbitYawOffset), Float.valueOf(this.orbitPitchOffset), Float.valueOf(this.smoothedOrbitYawOffset), Float.valueOf(this.smoothedOrbitPitchOffset), Boolean.valueOf(!this.freelookActive && (Math.abs(this.orbitYawOffset) > 0.01F || Math.abs(this.orbitPitchOffset) > 0.01F)), Float.valueOf(this.getRollInfluence(plane)), Float.valueOf(this.smoothedPitchInfluence * (float)MCH_Config.PlaneChasePitchInfluence.prmDouble), Double.valueOf(focus.xCoord), Double.valueOf(focus.yCoord), Double.valueOf(focus.zCoord),
                     Double.valueOf(this.lastDesiredX), Double.valueOf(this.lastDesiredY), Double.valueOf(this.lastDesiredZ),
                     Double.valueOf(this.posX), Double.valueOf(this.posY), Double.valueOf(this.posZ), Double.valueOf(this.lastDistanceBeforeCollision), Double.valueOf(this.lastDistanceAfterCollision), Float.valueOf(this.yaw), Float.valueOf(this.pitch), Double.valueOf(this.lookAheadBlend), Double.valueOf(MCH_Config.PlaneChaseFocusVerticalOffset.prmDouble), Double.valueOf(MCH_Config.PlaneChaseScreenVerticalBias.prmDouble), Double.valueOf(this.orbitDirX), Double.valueOf(this.orbitDirY), Double.valueOf(this.orbitDirZ),
                     Double.valueOf(dummy != null?dummy.posX:0.0D), Double.valueOf(dummy != null?dummy.posY:0.0D), Double.valueOf(dummy != null?dummy.posZ:0.0D),
                     Double.valueOf(mc.renderViewEntity != null?mc.renderViewEntity.posX:0.0D), Double.valueOf(mc.renderViewEntity != null?mc.renderViewEntity.posY:0.0D), Double.valueOf(mc.renderViewEntity != null?mc.renderViewEntity.posZ:0.0D),
                     Double.valueOf(plane.boundingBox != null?plane.boundingBox.minX:0.0D), Double.valueOf(plane.boundingBox != null?plane.boundingBox.minY:0.0D), Double.valueOf(plane.boundingBox != null?plane.boundingBox.minZ:0.0D),
                     Double.valueOf(plane.boundingBox != null?plane.boundingBox.maxX:0.0D), Double.valueOf(plane.boundingBox != null?plane.boundingBox.maxY:0.0D), Double.valueOf(plane.boundingBox != null?plane.boundingBox.maxZ:0.0D),
                     Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.minX:0.0D), Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.minY:0.0D), Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.minZ:0.0D),
                     Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.maxX:0.0D), Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.maxY:0.0D), Double.valueOf(dummy != null && dummy.boundingBox != null?dummy.boundingBox.maxZ:0.0D),
                     Boolean.valueOf(dummyInsidePlaneBB), Boolean.valueOf(dummyInsidePartBB), Boolean.valueOf(this.lastCollisionAdjusted), this.lastCollisionHit, Double.valueOf(nearestBoxDistance), Boolean.valueOf(renderTickBypassActive), Double.valueOf(this.distance(focus, desired)),
                     currentOwner, lastWriterMethod, Integer.valueOf(dummyTransformWrites), Integer.valueOf(mc.gameSettings.thirdPersonView)});
      }
   }

   public static boolean isFovOverrideActive() {
      return !MCH_ReplayModCompat.isReplayPlaybackActive() && activeCamera != null && activeRenderPlane != null && MCH_Config.EnablePlaneChaseFOVOverride.prmBool;
   }

   public static float applyFovOverride(float currentFov) {
      if(!isFovOverrideActive()) {
         return currentFov;
      }
      return activeCamera.updateFov(currentFov);
   }

   private float updateFov(float currentFov) {
      float target = (float)(this.freelookActive?MCH_Config.PlaneChaseFreelookFOV.prmDouble:MCH_Config.PlaneChaseFOV.prmDouble);
      this.lastFovTarget = target;
      if(this.smoothedFov < 0.0F) {
         this.smoothedFov = currentFov;
      }
      this.smoothedFov += (target - this.smoothedFov) * (float)MCH_Config.PlaneChaseFOVSmoothing.prmDouble;
      return this.smoothedFov;
   }

   public static boolean shouldConsumeFreelookMouse(MCP_EntityPlane plane, EntityPlayer player) {
      return shouldUseHoldFreelookAsCameraOnly(plane, player);
   }

   public static boolean shouldUseHoldFreelookAsCameraOnly(MCP_EntityPlane plane, EntityPlayer player) {
      if(plane == null || player == null) {
         return false;
      }
      Minecraft mc = Minecraft.getMinecraft();
      return MCH_Config.EnableHoldFreelook.prmBool
            && MCH_Config.EnableNewPlaneThirdPersonCamera.prmBool
            && plane.isNewFlightModelEnabled()
            && plane.isPilot(player)
            && mc != null
            && mc.gameSettings != null
            && mc.gameSettings.thirdPersonView > 0
            && plane.getCameraId() <= 0
            && !plane.getIsGunnerMode(player)
            && MCH_Key.isKeyDown(MCH_Config.KeyFreeLook.prmInt);
   }

   public static void addFreelookMouseDelta(double deltaX, double deltaY) {
      if(Math.abs(deltaX) < 0.01D) {
         deltaX = 0.0D;
      }
      if(Math.abs(deltaY) < 0.01D) {
         deltaY = 0.0D;
      }
      pendingFreelookMouseX += deltaX;
      pendingFreelookMouseY += deltaY;
   }

   public static boolean applyRenderStartCamera(Minecraft mc) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ReplayModCompat.logBlockedCameraWrite("MCP_PlaneChaseCamera.applyRenderStartCamera");
         return false;
      }
      if(activeCamera == null || activeRenderPlane == null || mc == null || mc.theWorld == null || activeRenderPlane.isDead) {
         logPhase("RENDER_START", mc, activeDummy, false, true);
         return false;
      }
      if(renderStartAppliedThisPhase && mc.renderViewEntity == activeDummy) {
         logPhase("RENDER_START", mc, activeDummy, false, true);
         return true;
      }
      renderStartAppliedThisPhase = true;
      boolean applied = applyActiveRenderCamera(mc);
      logPhase("RENDER_START", mc, activeDummy, applied, true);
      return applied;
   }

   public static void logPhase(String phase, Minecraft mc, MCH_ViewEntityDummy dummy, boolean transformApplied, boolean beforeOrientCamera) {
      if(!MCH_Config.DebugFlightControl.prmBool || System.currentTimeMillis() < nextPhaseLogTime) {
         return;
      }
      nextPhaseLogTime = System.currentTimeMillis() + 1000L;
      Entity view = mc != null?mc.renderViewEntity:null;
      MCH_Lib.Log("CHASE_PHASE: phase=%s renderViewEntity=%s dummy=(%.3f, %.3f, %.3f) thirdPersonView=%d thirdPersonDistance=%.3f thirdPersonDistanceTemp=%.3f transformApplied=%s beforeOrientCamera=%s",
            new Object[]{phase, view != null?view.getClass().getName():"null",
                  Double.valueOf(dummy != null?dummy.posX:0.0D), Double.valueOf(dummy != null?dummy.posY:0.0D), Double.valueOf(dummy != null?dummy.posZ:0.0D),
                  Integer.valueOf(mc != null && mc.gameSettings != null?mc.gameSettings.thirdPersonView:-1), Float.valueOf(W_Reflection.getThirdPersonDistance()), Float.valueOf(W_Reflection.getThirdPersonDistanceTemp()), Boolean.valueOf(transformApplied), Boolean.valueOf(beforeOrientCamera)});
   }

   public static boolean applyActiveRenderCamera(Minecraft mc) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ReplayModCompat.logBlockedCameraWrite("MCP_PlaneChaseCamera.applyActiveRenderCamera");
         return false;
      }
      if(activeCamera == null || activeRenderPlane == null || mc == null || mc.theWorld == null || activeRenderPlane.isDead) {
         return false;
      }
      MCH_ViewEntityDummy dummy = MCH_ViewEntityDummy.getInstance(mc.theWorld);
      if(dummy == null) {
         return false;
      }
      activeDummy = dummy;
      allowNextChaseDummyWrite = true;
      activeCamera.applyTransformToDummy(dummy);
      W_Reflection.setThirdPersonDistance(0.0F);
      W_Reflection.setThirdPersonDistanceTemp(0.0F);
      mc.renderViewEntity = dummy;
      MCH_Lib.setRenderViewEntity(dummy);
      W_Reflection.setCameraRoll(activeCamera.renderRoll);
      logStageProof(mc, "CHASE APPLY HIT", activeRenderPlane, true);
      consumedByRenderHook = true;
      logRenderViewEntityState(mc, dummy, "applyActiveRenderCamera");
      checkRenderViewEntityOwnership(mc, dummy);
      return true;
   }

   public static boolean applyActiveRiderCamera(MCP_EntityPlane plane) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ReplayModCompat.logBlockedCameraWrite("MCP_PlaneChaseCamera.applyActiveRiderCamera");
         return false;
      }
      if(activeCamera == null || activeRenderPlane == null || activeRenderPlane != plane) {
         return false;
      }
      MCH_ViewEntityDummy dummy = MCH_ViewEntityDummy.getInstance(plane.worldObj);
      if(dummy == null) {
         return false;
      }
      activeDummy = dummy;
      allowNextChaseDummyWrite = true;
      activeCamera.applyTransformToDummy(dummy);
      consumedByRenderHook = true;
      return true;
   }

   public void applyTransformToDummy(MCH_ViewEntityDummy dummy) {
      if(dummy == null || !this.hasRenderTransform) {
         return;
      }
      dummy.prevPosX = this.renderPrevPosX;
      dummy.prevPosY = this.renderPrevPosY;
      dummy.prevPosZ = this.renderPrevPosZ;
      dummy.lastTickPosX = this.renderPrevPosX;
      dummy.lastTickPosY = this.renderPrevPosY;
      dummy.lastTickPosZ = this.renderPrevPosZ;
      dummy.posX = this.renderPosX;
      dummy.posY = this.renderPosY;
      dummy.posZ = this.renderPosZ;
      dummy.prevRotationYaw = this.renderYaw;
      dummy.prevRotationPitch = this.renderPitch;
      dummy.rotationYaw = this.renderYaw;
      dummy.rotationPitch = this.renderPitch;
      dummy.configureNoCollisionChaseDummy();
      recordDummyTransformWrite("MCP_PlaneChaseCamera.applyTransformToDummy", true);
      logCameraWrite("MCP_PlaneChaseCamera.applyTransformToDummy", String.format("chaseStored=(%.3f,%.3f,%.3f)", Double.valueOf(this.renderPosX), Double.valueOf(this.renderPosY), Double.valueOf(this.renderPosZ)));
   }

   public static boolean isAnyRenderCameraActive() {
      return activeCamera != null && activeRenderPlane != null && activeDummy != null;
   }

   public static boolean ownsRenderEntity(Entity entity) {
      return activeDummy != null && entity == activeDummy;
   }

   public static void warnSkippedRenderViewRestore(Entity entity, String methodName) {
      if(activeCamera != null && !ownsRenderEntity(entity)) {
         MCH_Lib.Log("[MCHeli] WARNING: blocked non-chase renderViewEntity restore in %s to %s", new Object[]{methodName, entity != null?entity.getClass().getName():"null"});
      }
   }

   public static boolean enforceActiveRenderCameraOwnership(Minecraft mc, String stage) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ReplayModCompat.logBlockedCameraWrite("MCP_PlaneChaseCamera.enforceActiveRenderCameraOwnership");
         return false;
      }
      if(activeCamera == null || activeRenderPlane == null || mc == null || mc.theWorld == null || activeRenderPlane.isDead) {
         return false;
      }
      MCH_ViewEntityDummy dummy = activeDummy != null?activeDummy:MCH_ViewEntityDummy.getInstance(mc.theWorld);
      if(dummy == null) {
         return false;
      }
      activeDummy = dummy;
      if(mc.renderViewEntity != dummy) {
         MCH_Lib.Log("[MCHeli] WARNING: chase camera lost renderViewEntity ownership to %s", new Object[]{mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null"});
      }
      W_Reflection.setThirdPersonDistance(0.0F);
      W_Reflection.setThirdPersonDistanceTemp(0.0F);
      mc.renderViewEntity = dummy;
      MCH_Lib.setRenderViewEntity(dummy);
      logStageProof(mc, "CHASE ENFORCE HIT", activeRenderPlane, true);
      logRenderViewEntityState(mc, dummy, stage);
      checkRenderViewEntityOwnership(mc, dummy);
      return true;
   }

   private static void checkRenderViewEntityOwnership(Minecraft mc, MCH_ViewEntityDummy dummy) {
      if(mc.renderViewEntity != dummy) {
         MCH_Lib.Log("[MCHeli] WARNING: chase camera lost renderViewEntity ownership to %s", new Object[]{mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null"});
      }
   }

   private static void logRenderViewEntityState(Minecraft mc, MCH_ViewEntityDummy dummy, String stage) {
      if(!MCH_Config.DebugFlightControl.prmBool || mc == null) {
         return;
      }
      Entity view = mc.renderViewEntity;
      Entity player = mc.thePlayer;
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][%s] renderView=%s id=%d isDummy=%s equalsChaseDummy=%s pos=(%.3f, %.3f, %.3f) prev=(%.3f, %.3f, %.3f) lastTick=(%.3f, %.3f, %.3f) yaw=%.2f pitch=%.2f chaseStored=(%.3f, %.3f, %.3f) dummy=(%.3f, %.3f, %.3f) player=(%.3f, %.3f, %.3f) plane.camera=(%.3f, %.3f, %.3f) desired=(%.3f, %.3f, %.3f)",
            new Object[]{stage, view != null?view.getClass().getName():"null", Integer.valueOf(view != null?view.getEntityId():-1), Boolean.valueOf(view instanceof MCH_ViewEntityDummy), Boolean.valueOf(view == dummy),
                  Double.valueOf(view != null?view.posX:0.0D), Double.valueOf(view != null?view.posY:0.0D), Double.valueOf(view != null?view.posZ:0.0D),
                  Double.valueOf(view != null?view.prevPosX:0.0D), Double.valueOf(view != null?view.prevPosY:0.0D), Double.valueOf(view != null?view.prevPosZ:0.0D),
                  Double.valueOf(view != null?view.lastTickPosX:0.0D), Double.valueOf(view != null?view.lastTickPosY:0.0D), Double.valueOf(view != null?view.lastTickPosZ:0.0D),
                  Float.valueOf(view != null?view.rotationYaw:0.0F), Float.valueOf(view != null?view.rotationPitch:0.0F),
                  Double.valueOf(activeCamera != null && activeCamera.hasRenderTransform?activeCamera.renderPosX:0.0D), Double.valueOf(activeCamera != null && activeCamera.hasRenderTransform?activeCamera.renderPosY:0.0D), Double.valueOf(activeCamera != null && activeCamera.hasRenderTransform?activeCamera.renderPosZ:0.0D),
                  Double.valueOf(dummy != null?dummy.posX:0.0D), Double.valueOf(dummy != null?dummy.posY:0.0D), Double.valueOf(dummy != null?dummy.posZ:0.0D),
                  Double.valueOf(player != null?player.posX:0.0D), Double.valueOf(player != null?player.posY:0.0D), Double.valueOf(player != null?player.posZ:0.0D),
                  Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posX:0.0D), Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posY:0.0D), Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posZ:0.0D),
                  Double.valueOf(desiredX), Double.valueOf(desiredY), Double.valueOf(desiredZ)});
   }


   public static void beginOrientCameraBypass(Minecraft mc, float partialTicks) {
      if(MCH_ReplayModCompat.isReplayPlaybackActive()) {
         MCH_ReplayModCompat.logBlockedCameraWrite("MCP_PlaneChaseCamera.beginOrientCameraBypass");
         return;
      }
      if(activeCamera == null || activeRenderPlane == null || activeDummy == null || mc == null || mc.gameSettings == null) {
         return;
      }
      float beforeDistance = W_Reflection.getThirdPersonDistance();
      float beforeDistanceTemp = W_Reflection.getThirdPersonDistanceTemp();
      savedThirdPersonView = mc.gameSettings.thirdPersonView;
      W_Reflection.setThirdPersonDistance(0.0F);
      W_Reflection.setThirdPersonDistanceTemp(0.0F);
      mc.gameSettings.thirdPersonView = 0;
      renderTickBypassActive = true;
      logPhase("ORIENT_CAMERA", mc, activeDummy, true, true);
      logOrientCameraBypass(mc, partialTicks, beforeDistance, beforeDistanceTemp, W_Reflection.getThirdPersonDistance(), W_Reflection.getThirdPersonDistanceTemp(), true);
   }

   public static void endOrientCameraBypass(Minecraft mc) {
      if(!renderTickBypassActive || mc == null || mc.gameSettings == null) {
         return;
      }
      mc.gameSettings.thirdPersonView = savedThirdPersonView;
      W_Reflection.setThirdPersonDistance(0.0F);
      W_Reflection.setThirdPersonDistanceTemp(0.0F);
      logPhase("RENDER_END", mc, activeDummy, false, false);
      renderStartAppliedThisPhase = false;
      renderTickBypassActive = false;
   }

   /** Drops only MCHeli-owned chase state; never writes renderViewEntity. */
   public static void releaseForReplayPlayback(Minecraft mc) {
      if(renderTickBypassActive) {
         endOrientCameraBypass(mc);
      }
      if(activeCamera != null) {
         activeCamera.reset();
      }
      activeCamera = null;
      activeRenderPlane = null;
      activeDummy = null;
      consumedByRenderHook = false;
      renderStartAppliedThisPhase = false;
      renderTickBypassActive = false;
      allowNextChaseDummyWrite = false;
      pendingFreelookMouseX = 0.0D;
      pendingFreelookMouseY = 0.0D;
      currentOwner = "NONE";
   }

   private static void logOrientCameraBypass(Minecraft mc, float partialTicks, float beforeDistance, float beforeDistanceTemp, float afterDistance, float afterDistanceTemp, boolean bypassApplied) {
      if(!MCH_Config.DebugFlightControl.prmBool || System.currentTimeMillis() < nextOrientDebugTime) {
         return;
      }
      nextOrientDebugTime = System.currentTimeMillis() + 1000L;
      Entity view = mc.renderViewEntity;
      double camX = view != null?view.prevPosX + (view.posX - view.prevPosX) * (double)partialTicks:0.0D;
      double camY = view != null?view.prevPosY + (view.posY - view.prevPosY) * (double)partialTicks:0.0D;
      double camZ = view != null?view.prevPosZ + (view.posZ - view.prevPosZ) * (double)partialTicks:0.0D;
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][orientCamera] entered=true thirdPersonView=%d thirdPersonDistanceBefore=%.3f thirdPersonDistanceTempBefore=%.3f thirdPersonDistanceAfter=%.3f thirdPersonDistanceTempAfter=%.3f renderView=%s pos=(%.3f, %.3f, %.3f) cameraWorld=(%.3f, %.3f, %.3f) bypassApplied=%s",
            new Object[]{Integer.valueOf(savedThirdPersonView), Float.valueOf(beforeDistance), Float.valueOf(beforeDistanceTemp), Float.valueOf(afterDistance), Float.valueOf(afterDistanceTemp),
                  view != null?view.getClass().getName():"null", Double.valueOf(view != null?view.posX:0.0D), Double.valueOf(view != null?view.posY:0.0D), Double.valueOf(view != null?view.posZ:0.0D),
                  Double.valueOf(camX), Double.valueOf(camY), Double.valueOf(camZ), Boolean.valueOf(bypassApplied)});
   }

   public static boolean isRenderCameraActiveFor(MCP_EntityPlane plane, EntityPlayer player) {
      return activeCamera != null && activeRenderPlane != null && activeRenderPlane == plane && player != null;
   }

   public static boolean shouldBlockNonChaseDummyWrite(String methodName) {
      if(activeCamera != null && !allowNextChaseDummyWrite) {
         MCH_Lib.Log("[MCHeli][PlaneChaseCamera][PROOF] blocked non-chase dummy writer %s while chase active", new Object[]{methodName});
         return true;
      }
      return false;
   }

   public static void recordDummyTransformWrite(String methodName, boolean chaseWriter) {
      if(allowNextChaseDummyWrite) {
         allowNextChaseDummyWrite = false;
         chaseWriter = true;
         methodName = methodName + "[CHASE]";
      }
      lastWriterMethod = methodName;
      ++dummyTransformWrites;
      if(chaseWriter) {
         currentOwner = "CHASE";
      }
      if(activeCamera != null && !chaseWriter) {
         currentOwner = methodName.indexOf("Gunner") >= 0?"GUNNER":"LEGACY_SEAT";
         if(MCH_Config.DebugFlightControl.prmBool) {
            MCH_Lib.Log("[MCHeli] WARNING: non-chase camera writer while chase camera active: %s", new Object[]{methodName});
         }
      }
   }

   private float smoothAngle(float current, float target, float smoothing) {
      float delta = MathHelper.wrapAngleTo180_float(target - current);
      return current + delta * MathHelper.clamp_float(smoothing, 0.01F, 1.0F);
   }

   private double smooth(double current, double target, double smoothing) {
      return current + (target - current) * MCH_Lib.RNG(smoothing, 0.01D, 1.0D);
   }
}
