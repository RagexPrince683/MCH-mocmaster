package mcheli.hud;

import mcheli.MCH_Camera;
import mcheli.MCH_Lib;
import net.minecraft.entity.Entity;

public class MCH_HudItemCameraRot extends MCH_HudItem {

   private final String drawPosX;
   private final String drawPosY;


   public MCH_HudItemCameraRot(int fileLine, String posx, String posy) {
      super(fileLine);
      this.drawPosX = toFormula(posx);
      this.drawPosY = toFormula(posy);
   }

   public void execute() {
      this.drawCommonGunnerCamera(MCH_HudItem.ac, MCH_HudItem.ac.camera, MCH_HudItem.colorSetting, MCH_HudItem.centerX + calc(this.drawPosX), MCH_HudItem.centerY + calc(this.drawPosY));
   }

   private void drawCommonGunnerCamera(Entity ac, MCH_Camera camera, int color, double posX, double posY) {
      if(camera != null) {
         boolean WW = true;
         boolean WH = true;
         boolean LW = true;
         double[] line = new double[]{posX - 21.0D, posY - 11.0D, posX + 21.0D, posY - 11.0D, posX + 21.0D, posY + 11.0D, posX - 21.0D, posY + 11.0D};
         this.drawLine(line, color, 2);
         line = new double[]{posX - 21.0D, posY, posX, posY, posX + 21.0D, posY, posX, posY, posX, posY - 11.0D, posX, posY, posX, posY + 11.0D, posX, posY};
         this.drawLineStipple(line, color, 1, '\ucccc');
         float pitch = camera.rotationPitch;
         if(pitch < -30.0F) {
            pitch = -30.0F;
         }

         if(pitch > 70.0F) {
            pitch = 70.0F;
         }

         pitch -= 20.0F;
         pitch = (float)((double)pitch * 0.16D);
         float var10000 = ac.prevRotationYaw + (ac.rotationYaw - ac.prevRotationYaw) / 2.0F;
         var10000 = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) / 2.0F;
         float yaw = (float)MCH_Lib.getRotateDiff((double)ac.rotationYaw, (double)camera.rotationYaw);
         yaw *= 2.0F;
         if(yaw < -50.0F) {
            yaw = -50.0F;
         }

         if(yaw > 50.0F) {
            yaw = 50.0F;
         }

         yaw = (float)((double)yaw * 0.34D);
         line = new double[]{posX + (double)yaw - 3.0D, posY + (double)pitch - 2.0D, posX + (double)yaw + 3.0D, posY + (double)pitch - 2.0D, posX + (double)yaw + 3.0D, posY + (double)pitch + 2.0D, posX + (double)yaw - 3.0D, posY + (double)pitch + 2.0D};
         this.drawLine(line, color, 2);
      }
   }
}
