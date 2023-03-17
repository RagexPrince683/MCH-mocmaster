package mcheli;

import mcheli.wrapper.W_Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;

public class MCH_ViewEntityDummy extends EntityPlayerSP {

   private static MCH_ViewEntityDummy instance = null;
   private float zoom;


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

   public void onUpdate() {}

   public void update(MCH_Camera camera) {
      if(camera != null) {
         this.zoom = camera.getCameraZoom();
         super.prevRotationYaw = super.rotationYaw;
         super.prevRotationPitch = super.rotationPitch;
         super.rotationYaw = camera.rotationYaw;
         super.rotationPitch = camera.rotationPitch;
         super.prevPosX = camera.posX;
         super.prevPosY = camera.posY;
         super.prevPosZ = camera.posZ;
         super.posX = camera.posX;
         super.posY = camera.posY;
         super.posZ = camera.posZ;
      }
   }

   public static void setCameraPosition(double x, double y, double z) {
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
      }
   }

   public float getFOVMultiplier() {
      return super.getFOVMultiplier() * (1.0F / this.zoom);
   }

}
