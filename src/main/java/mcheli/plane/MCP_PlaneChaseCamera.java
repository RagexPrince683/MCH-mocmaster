package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import mcheli.wrapper.W_Reflection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MCP_PlaneChaseCamera {

   private MCP_EntityPlane activePlane;
   private int activeView = -1;
   private double posX;
   private double posY;
   private double posZ;
   private float yaw;
   private float pitch;
   private boolean initialized;

   public boolean shouldUse(Minecraft mc, EntityPlayer player, MCP_EntityPlane plane, boolean isPilot) {
      return mc != null && mc.gameSettings != null && mc.gameSettings.thirdPersonView > 0 && player != null && plane != null
            && isPilot && MCH_Config.EnableNewPlaneThirdPersonCamera.prmBool && plane.isNewFlightModelEnabled()
            && !plane.getIsGunnerMode(player) && plane.getCameraId() <= 0 && !plane.isDestroyed();
   }

   public void reset() {
      this.activePlane = null;
      this.activeView = -1;
      this.initialized = false;
      W_Reflection.setCameraRoll(0.0F);
   }

   public void update(Minecraft mc, EntityPlayer player, MCP_EntityPlane plane) {
      if(!this.initialized || this.activePlane != plane || this.activeView != mc.gameSettings.thirdPersonView) {
         this.initialize(plane);
      }

      Vec3 focus = this.getCameraFocusPoint(plane);
      Vec3 desired = this.computeDesiredCameraPosition(plane, focus);
      if(MCH_Config.NewPlaneCameraCollision.prmBool) {
         desired = this.adjustForCollision(plane, focus, desired);
      }
      this.debugCamera(plane, focus, desired);

      float targetYaw = plane.getRotYaw();
      float targetPitch = this.computeLookPitch(desired, focus);
      this.yaw = this.smoothAngle(this.yaw, targetYaw, (float)MCH_Config.NewPlaneCameraRotationSmoothing.prmDouble);
      this.pitch = this.smoothAngle(this.pitch, targetPitch, (float)MCH_Config.NewPlaneCameraRotationSmoothing.prmDouble);

      double posSmoothing = MCH_Config.NewPlaneCameraPositionSmoothing.prmDouble;
      this.posX = this.smooth(this.posX, desired.xCoord, posSmoothing);
      this.posY = this.smooth(this.posY, desired.yCoord, posSmoothing);
      this.posZ = this.smooth(this.posZ, desired.zCoord, posSmoothing);

      plane.camera.prevRotationYaw = plane.camera.rotationYaw;
      plane.camera.prevRotationPitch = plane.camera.rotationPitch;
      plane.camera.rotationYaw = this.yaw;
      plane.camera.rotationPitch = this.pitch;
      plane.camera.setPosition(this.posX, this.posY, this.posZ);
      W_Reflection.setCameraRoll(plane.getRotRoll() * (float)MCH_Config.NewPlaneCameraRollInfluence.prmDouble);
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

   private Vec3 computeDesiredCameraPosition(MCP_EntityPlane plane, Vec3 focus) {
      double distance = MCH_Config.NewPlaneCameraDistance.prmDouble;
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
      if(hit != null && hit.hitVec != null) {
         Vec3 away = Vec3.createVectorHelper(start.xCoord - hit.hitVec.xCoord, start.yCoord - hit.hitVec.yCoord, start.zCoord - hit.hitVec.zCoord).normalize();
         return hit.hitVec.addVector(away.xCoord * 0.35D, away.yCoord * 0.35D, away.zCoord * 0.35D);
      }
      return desired;
   }

   private float computeLookPitch(Vec3 camera, Vec3 focus) {
      double dx = focus.xCoord - camera.xCoord;
      double dy = focus.yCoord - camera.yCoord;
      double dz = focus.zCoord - camera.zCoord;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      return MathHelper.clamp_float((float)(-Math.atan2(dy, horizontal) * 57.29577951308232D), -35.0F, 35.0F);
   }

   private void debugCamera(MCP_EntityPlane plane, Vec3 focus, Vec3 desired) {
      if(MCH_Config.DebugFlightControl.prmBool) {
         MCP_PlaneInfo info = plane.getPlaneInfo();
         float ox = info != null?info.newPlaneCameraFocusOffsetX:0.0F;
         float oy = info != null?info.newPlaneCameraFocusOffsetY:1.0F;
         float oz = info != null?info.newPlaneCameraFocusOffsetZ:0.0F;
         MCH_Lib.Log("[MCHeli][PlaneChaseCamera] entity=(%.3f, %.3f, %.3f) focus=(%.3f, %.3f, %.3f) offset=(%.3f, %.3f, %.3f) desired=(%.3f, %.3f, %.3f)",
               new Object[]{Double.valueOf(plane.posX), Double.valueOf(plane.posY), Double.valueOf(plane.posZ),
                     Double.valueOf(focus.xCoord), Double.valueOf(focus.yCoord), Double.valueOf(focus.zCoord),
                     Float.valueOf(ox), Float.valueOf(oy), Float.valueOf(oz),
                     Double.valueOf(desired.xCoord), Double.valueOf(desired.yCoord), Double.valueOf(desired.zCoord)});
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
