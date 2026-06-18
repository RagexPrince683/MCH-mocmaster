package mcheli;

import mcheli.plane.MCP_PlaneChaseCamera;
import mcheli.wrapper.W_Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class MCH_ViewEntityDummy extends EntityPlayerSP {

   private static MCH_ViewEntityDummy instance = null;
   private float zoom = 1.0F;

   private MCH_ViewEntityDummy(World world) {
      super(Minecraft.getMinecraft(), world, W_Session.newSession(), 0);
      super.hurtTime = 0;
      super.maxHurtTime = 1;
      this.setSize(1.0F, 1.0F);
   }

   public static MCH_ViewEntityDummy getInstance(World w) {
      if((instance == null || instance.isDead) && w.isRemote) {
         instance = new MCH_ViewEntityDummy(w);

         if(Minecraft.getMinecraft().thePlayer != null) {
            instance.movementInput = Minecraft.getMinecraft().thePlayer.movementInput;
         }

         instance.setPosition(0.0D, -4.0D, 0.0D);
         w.spawnEntityInWorld(instance);
      }

      return instance;
   }

   public static void onUnloadWorld() {
      if(instance != null) {
         instance.setDead();
         instance = null;
      }
   }

   @Override
   public void onUpdate() {
   }

   public void update(MCH_Camera camera) {
      if(MCP_PlaneChaseCamera.shouldBlockNonChaseDummyWrite("MCH_ViewEntityDummy.update")) {
         return;
      }

      if(camera != null) {
         this.zoom = camera.getCameraZoom();

         super.prevRotationYaw = camera.rotationYaw;
         super.prevRotationPitch = camera.rotationPitch;
         super.rotationYaw = camera.rotationYaw;
         super.rotationPitch = camera.rotationPitch;

         super.prevPosX = camera.posX;
         super.prevPosY = camera.posY;
         super.prevPosZ = camera.posZ;
         super.lastTickPosX = camera.posX;
         super.lastTickPosY = camera.posY;
         super.lastTickPosZ = camera.posZ;
         super.posX = camera.posX;
         super.posY = camera.posY;
         super.posZ = camera.posZ;

         this.configureNoCollisionChaseDummy();

         MCP_PlaneChaseCamera.recordDummyTransformWrite("MCH_ViewEntityDummy.update", false);
         MCP_PlaneChaseCamera.logCameraWrite(
                 "MCH_ViewEntityDummy.update",
                 String.format("pos=(%.3f,%.3f,%.3f)", Double.valueOf(super.posX), Double.valueOf(super.posY), Double.valueOf(super.posZ))
         );
      }
   }

   public static void setCameraPosition(double x, double y, double z) {
      if(MCP_PlaneChaseCamera.shouldBlockNonChaseDummyWrite("MCH_ViewEntityDummy.setCameraPosition")) {
         return;
      }

      if(instance != null) {
         instance.prevPosX = x;
         instance.prevPosY = y;
         instance.prevPosZ = z;
         instance.lastTickPosX = x;
         instance.lastTickPosY = y;
         instance.lastTickPosZ = z;
         instance.posX = x;
         instance.posY = y;
         instance.posZ = z;

         instance.configureNoCollisionChaseDummy();

         MCP_PlaneChaseCamera.recordDummyTransformWrite("MCH_ViewEntityDummy.setCameraPosition", false);
         MCP_PlaneChaseCamera.logCameraWrite(
                 "MCH_ViewEntityDummy.setCameraPosition",
                 String.format("pos=(%.3f,%.3f,%.3f)", Double.valueOf(x), Double.valueOf(y), Double.valueOf(z))
         );
      }
   }

   @Override
   public void setPosition(double x, double y, double z) {
      super.setPosition(x, y, z);
      this.configureNoCollisionChaseDummy();
   }

   public void configureNoCollisionChaseDummy() {
      super.noClip = true;
      super.ridingEntity = null;
      super.riddenByEntity = null;
      super.ySize = 0.0F;
      this.setSize(0.01F, 0.01F);

      if(super.boundingBox != null) {
         super.boundingBox.setBounds(
                 super.posX - 0.005D,
                 super.posY - 0.005D,
                 super.posZ - 0.005D,
                 super.posX + 0.005D,
                 super.posY + 0.005D,
                 super.posZ + 0.005D
         );
      }
   }

   @Override
   public AxisAlignedBB getCollisionBox(Entity entity) {
      return null;
   }

   @Override
   public AxisAlignedBB getBoundingBox() {
      return null;
   }

   @Override
   public boolean canBePushed() {
      return false;
   }

   @Override
   public boolean canBeCollidedWith() {
      return false;
   }

   @Override
   public float getFOVMultiplier() {
      return super.getFOVMultiplier() * (1.0F / Math.max(this.zoom, 0.01F));
   }
}