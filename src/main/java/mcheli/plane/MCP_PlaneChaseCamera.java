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

      Vec3 desired = this.computeDesiredCameraPosition(plane);
      if(MCH_Config.NewPlaneCameraCollision.prmBool) {
         desired = this.adjustForCollision(plane, desired);
      }

      float targetYaw = plane.getRotYaw();
      float targetPitch = MathHelper.clamp_float(plane.getRotPitch() * 0.65F, -35.0F, 35.0F);
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
      Vec3 desired = this.computeDesiredCameraPosition(plane);
      this.posX = desired.xCoord;
      this.posY = desired.yCoord;
      this.posZ = desired.zCoord;
      this.yaw = plane.getRotYaw();
      this.pitch = MathHelper.clamp_float(plane.getRotPitch() * 0.65F, -35.0F, 35.0F);
      this.activePlane = plane;
      this.activeView = Minecraft.getMinecraft().gameSettings.thirdPersonView;
      this.initialized = true;
   }

   private Vec3 computeDesiredCameraPosition(MCP_EntityPlane plane) {
      double distance = MCH_Config.NewPlaneCameraDistance.prmDouble;
      double height = MCH_Config.NewPlaneCameraHeight.prmDouble;
      double side = MCH_Config.NewPlaneCameraSideOffset.prmDouble;
      Vec3 forward = MCH_Lib.Rot2Vec3(plane.getRotYaw(), 0.0F);
      Vec3 right = MCH_Lib.Rot2Vec3(plane.getRotYaw() + 90.0F, 0.0F);
      double x = plane.posX - forward.xCoord * distance + right.xCoord * side;
      double y = plane.posY + height;
      double z = plane.posZ - forward.zCoord * distance + right.zCoord * side;
      return Vec3.createVectorHelper(x, y, z);
   }

   private Vec3 adjustForCollision(MCP_EntityPlane plane, Vec3 desired) {
      Vec3 start = Vec3.createVectorHelper(plane.posX, plane.posY + 1.0D, plane.posZ);
      MovingObjectPosition hit = plane.worldObj.rayTraceBlocks(start, desired, false, true, false);
      if(hit != null && hit.hitVec != null) {
         Vec3 away = Vec3.createVectorHelper(start.xCoord - hit.hitVec.xCoord, start.yCoord - hit.hitVec.yCoord, start.zCoord - hit.hitVec.zCoord).normalize();
         return hit.hitVec.addVector(away.xCoord * 0.35D, away.yCoord * 0.35D, away.zCoord * 0.35D);
      }
      return desired;
   }

   private float smoothAngle(float current, float target, float smoothing) {
      float delta = MathHelper.wrapAngleTo180_float(target - current);
      return current + delta * MathHelper.clamp_float(smoothing, 0.01F, 1.0F);
   }

   private double smooth(double current, double target, double smoothing) {
      return current + (target - current) * MCH_Lib.RNG(smoothing, 0.01D, 1.0D);
   }
}
