package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.MCH_ViewEntityDummy;
import mcheli.aircraft.MCH_BoundingBox;
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
      this.hardProofFrame = 0;
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

      Vec3 focus = this.getCameraFocusPoint(plane);
      Vec3 desired = this.computeDesiredCameraPosition(plane, focus);
      if(MCH_Config.DebugFlightControl.prmBool && MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool) {
         desired = Vec3.createVectorHelper(plane.posX, plane.posY + 20.0D, plane.posZ);
         this.lastCollisionAdjusted = false;
         this.lastCollisionHit = "debugAbovePlane";
      } else {
         if(MCH_Config.NewPlaneCameraCollision.prmBool) {
            desired = this.adjustForCollision(plane, focus, desired);
         }
         desired = this.escapeAircraftCollision(plane, focus, desired);
      }
      activeCamera = this;
      activeRenderPlane = plane;
      desiredX = desired.xCoord;
      desiredY = desired.yCoord;
      desiredZ = desired.zCoord;
      currentOwner = "CHASE";
      dummyTransformWrites = 0;
      consumedByRenderHook = false;

      this.yaw = plane.getRotYaw();
      this.pitch = 45.0F;
      this.posX = desired.xCoord;
      this.posY = desired.yCoord;
      this.posZ = desired.zCoord;

      plane.camera.prevRotationYaw = plane.camera.rotationYaw;
      plane.camera.prevRotationPitch = plane.camera.rotationPitch;
      plane.camera.rotationYaw = this.yaw;
      plane.camera.rotationPitch = this.pitch;
      plane.camera.setPosition(this.posX, this.posY, this.posZ);
      logPhase("CLIENT_TICK", mc, activeDummy, false, false);
      this.debugCamera(mc, plane, focus, desired, true);
      W_Reflection.setCameraRoll(plane.getRotRoll() * (float)MCH_Config.NewPlaneCameraRollInfluence.prmDouble);
   }

   private static void logShouldUseProof(Minecraft mc, MCP_EntityPlane plane, boolean result, boolean thirdPerson, boolean hasPlayer, boolean hasPlane, boolean isPilot, boolean enabled, boolean newFlight, boolean notGunner, boolean cameraOk, boolean notDestroyed) {
      if(System.currentTimeMillis() < nextProofLogTime) {
         return;
      }
      nextProofLogTime = System.currentTimeMillis() + 1000L;
      String renderView = mc != null && mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][PROOF] shouldUse=%s DebugFlightControl=%s NewPlaneCameraDebugAbovePlane=%s planeId=%d renderView=%s conditions thirdPersonView>0=%s player!=null=%s plane!=null=%s isPilot=%s EnableNewPlaneThirdPersonCamera=%s newFlight=%s notGunner=%s cameraId<=0=%s notDestroyed=%s",
            new Object[]{Boolean.valueOf(result), Boolean.valueOf(MCH_Config.DebugFlightControl.prmBool), Boolean.valueOf(MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool), Integer.valueOf(plane != null?plane.getEntityId():-1), renderView,
                  Boolean.valueOf(thirdPerson), Boolean.valueOf(hasPlayer), Boolean.valueOf(hasPlane), Boolean.valueOf(isPilot), Boolean.valueOf(enabled), Boolean.valueOf(newFlight), Boolean.valueOf(notGunner), Boolean.valueOf(cameraOk), Boolean.valueOf(notDestroyed)});
   }

   private static void logStageProof(Minecraft mc, String label, MCP_EntityPlane plane, boolean shouldUse) {
      if(System.currentTimeMillis() < nextStageProofLogTime) {
         return;
      }
      nextStageProofLogTime = System.currentTimeMillis() + 1000L;
      String renderView = mc != null && mc.renderViewEntity != null?mc.renderViewEntity.getClass().getName():"null";
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][PROOF] %s DebugFlightControl=%s NewPlaneCameraDebugAbovePlane=%s shouldUse=%s planeId=%d renderView=%s",
            new Object[]{label, Boolean.valueOf(MCH_Config.DebugFlightControl.prmBool), Boolean.valueOf(MCH_Config.NewPlaneCameraDebugAbovePlane.prmBool), Boolean.valueOf(shouldUse), Integer.valueOf(plane != null?plane.getEntityId():-1), renderView});
   }

   public static void logCameraWrite(String methodName, String stage) {
      Minecraft mc = Minecraft.getMinecraft();
      if((activeCamera != null || (MCH_Config.DebugFlightControl != null && MCH_Config.DebugFlightControl.prmBool)) && System.currentTimeMillis() >= nextWriteLogTime) {
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
      this.activePlane = plane;
      this.activeView = Minecraft.getMinecraft().gameSettings.thirdPersonView;
      this.initialized = true;
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
      double worldY = plane.posY + oy;
      double worldZ = plane.posZ + ox * sin + oz * cos;
      return Vec3.createVectorHelper(worldX, worldY, worldZ);
   }

   private double getCameraDistance() {
      if(MCH_Config.DebugFlightControl.prmBool && MCH_Config.NewPlaneCameraDebugDistance.prmDouble > 0.0D) {
         return MCH_Config.NewPlaneCameraDebugDistance.prmDouble;
      }
      return MCH_Lib.RNG(MCH_Config.NewPlaneCameraDistance.prmDouble, MCH_Config.NewPlaneCameraMinDistance.prmDouble, MCH_Config.NewPlaneCameraMaxDistance.prmDouble);
   }

   private double getCameraDistance(MCP_EntityPlane plane) {
      if(MCH_Config.DebugFlightControl.prmBool && MCH_Config.NewPlaneCameraDebugDistance.prmDouble > 0.0D) {
         return MCH_Config.NewPlaneCameraDebugDistance.prmDouble;
      }
      double dx = plane.posX - plane.prevPosX;
      double dy = plane.posY - plane.prevPosY;
      double dz = plane.posZ - plane.prevPosZ;
      double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
      return MCH_Lib.RNG(MCH_Config.NewPlaneCameraDistance.prmDouble + speed * MCH_Config.NewPlaneCameraSpeedDistanceScale.prmDouble, MCH_Config.NewPlaneCameraMinDistance.prmDouble, MCH_Config.NewPlaneCameraMaxDistance.prmDouble);
   }

   private Vec3 computeDesiredCameraPosition(MCP_EntityPlane plane, Vec3 focus) {
      double distance = this.getCameraDistance(plane);
      double height = MCH_Config.NewPlaneCameraHeight.prmDouble;
      double side = MCH_Config.NewPlaneCameraSideOffset.prmDouble;
      Vec3 forward = MCH_Lib.Rot2Vec3(plane.getRotYaw(), 0.0F);
      Vec3 right = MCH_Lib.Rot2Vec3(plane.getRotYaw() + 90.0F, 0.0F);
      double x = focus.xCoord - forward.xCoord * distance + right.xCoord * side;
      double y = focus.yCoord + height;
      double z = focus.zCoord - forward.zCoord * distance + right.zCoord * side;
      return Vec3.createVectorHelper(x, y, z);
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
         if(box != null && this.isPointInsideAabb(point, box.boundingBox)) {
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

   private double nearestAircraftBoxDistance(MCP_EntityPlane plane, Vec3 point) {
      double nearest = this.distanceToAabb(point, plane.boundingBox);
      MCH_BoundingBox[] boxes = plane.getCalculatedExtraBoundingBoxes();
      for(int i = 0; i < boxes.length; ++i) {
         MCH_BoundingBox box = boxes[i];
         if(box != null) {
            nearest = Math.min(nearest, this.distanceToAabb(point, box.boundingBox));
         }
      }
      return nearest;
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
         MCH_Lib.Log("CHASE_CAM: focus=(%.3f, %.3f, %.3f) desired=(%.3f, %.3f, %.3f) finalDummy=(%.3f, %.3f, %.3f) renderView=(%.3f, %.3f, %.3f) planeBB=(%.3f, %.3f, %.3f -> %.3f, %.3f, %.3f) dummyBB=(%.3f, %.3f, %.3f -> %.3f, %.3f, %.3f) dummyInsidePlaneBB=%s dummyInsidePartBB=%s collisionAdjusted=%s collisionHit=%s nearestAircraftBoxDistance=%.3f thirdPersonViewMasked=%s distanceToFocus=%.3f owner=%s lastWriter=%s writes=%d thirdPerson=%d",
               new Object[]{Double.valueOf(focus.xCoord), Double.valueOf(focus.yCoord), Double.valueOf(focus.zCoord),
                     Double.valueOf(desired.xCoord), Double.valueOf(desired.yCoord), Double.valueOf(desired.zCoord),
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

   public static boolean applyRenderStartCamera(Minecraft mc) {
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
      if(System.currentTimeMillis() < nextPhaseLogTime) {
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
      if(activeCamera == null || activeRenderPlane == null || mc == null || mc.theWorld == null || activeRenderPlane.isDead) {
         return false;
      }
      MCH_ViewEntityDummy dummy = MCH_ViewEntityDummy.getInstance(mc.theWorld);
      if(dummy == null) {
         return false;
      }
      activeDummy = dummy;
      allowNextChaseDummyWrite = true;
      dummy.update(activeRenderPlane.camera);
      W_Reflection.setThirdPersonDistance(0.0F);
      W_Reflection.setThirdPersonDistanceTemp(0.0F);
      mc.renderViewEntity = dummy;
      MCH_Lib.setRenderViewEntity(dummy);
      W_Reflection.setCameraRoll(activeRenderPlane.getRotRoll() * (float)MCH_Config.NewPlaneCameraRollInfluence.prmDouble);
      logStageProof(mc, "CHASE APPLY HIT", activeRenderPlane, true);
      consumedByRenderHook = true;
      logRenderViewEntityState(mc, dummy, "applyActiveRenderCamera");
      checkRenderViewEntityOwnership(mc, dummy);
      return true;
   }

   public static boolean applyActiveRiderCamera(MCP_EntityPlane plane) {
      if(activeCamera == null || activeRenderPlane == null || activeRenderPlane != plane) {
         return false;
      }
      MCH_ViewEntityDummy dummy = MCH_ViewEntityDummy.getInstance(plane.worldObj);
      if(dummy == null) {
         return false;
      }
      activeDummy = dummy;
      allowNextChaseDummyWrite = true;
      dummy.update(plane.camera);
      consumedByRenderHook = true;
      return true;
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
      MCH_Lib.Log("[MCHeli][PlaneChaseCamera][%s] renderView=%s id=%d isDummy=%s equalsChaseDummy=%s pos=(%.3f, %.3f, %.3f) prev=(%.3f, %.3f, %.3f) lastTick=(%.3f, %.3f, %.3f) yaw=%.2f pitch=%.2f chaseDummy=(%.3f, %.3f, %.3f) player=(%.3f, %.3f, %.3f) planeCamera=(%.3f, %.3f, %.3f) desired=(%.3f, %.3f, %.3f)",
            new Object[]{stage, view != null?view.getClass().getName():"null", Integer.valueOf(view != null?view.getEntityId():-1), Boolean.valueOf(view instanceof MCH_ViewEntityDummy), Boolean.valueOf(view == dummy),
                  Double.valueOf(view != null?view.posX:0.0D), Double.valueOf(view != null?view.posY:0.0D), Double.valueOf(view != null?view.posZ:0.0D),
                  Double.valueOf(view != null?view.prevPosX:0.0D), Double.valueOf(view != null?view.prevPosY:0.0D), Double.valueOf(view != null?view.prevPosZ:0.0D),
                  Double.valueOf(view != null?view.lastTickPosX:0.0D), Double.valueOf(view != null?view.lastTickPosY:0.0D), Double.valueOf(view != null?view.lastTickPosZ:0.0D),
                  Float.valueOf(view != null?view.rotationYaw:0.0F), Float.valueOf(view != null?view.rotationPitch:0.0F),
                  Double.valueOf(dummy != null?dummy.posX:0.0D), Double.valueOf(dummy != null?dummy.posY:0.0D), Double.valueOf(dummy != null?dummy.posZ:0.0D),
                  Double.valueOf(player != null?player.posX:0.0D), Double.valueOf(player != null?player.posY:0.0D), Double.valueOf(player != null?player.posZ:0.0D),
                  Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posX:0.0D), Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posY:0.0D), Double.valueOf(activeRenderPlane != null?activeRenderPlane.camera.posZ:0.0D),
                  Double.valueOf(desiredX), Double.valueOf(desiredY), Double.valueOf(desiredZ)});
   }


   public static void beginOrientCameraBypass(Minecraft mc, float partialTicks) {
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
