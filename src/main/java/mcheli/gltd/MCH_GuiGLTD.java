package mcheli.gltd;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Camera;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_MOD;
import mcheli.gui.MCH_Gui;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiGLTD extends MCH_Gui {

   public MCH_GuiGLTD(Minecraft minecraft) {
      super(minecraft);
   }

   public void initGui() {
      super.initGui();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public boolean isDrawGui(EntityPlayer player) {
      return player.ridingEntity != null && player.ridingEntity instanceof MCH_EntityGLTD;
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      if(isThirdPersonView) {
         MCH_Config var10000 = MCH_MOD.config;
         if(!MCH_Config.DisplayHUDThirdPerson.prmBool) {
            return;
         }
      }

      GL11.glLineWidth((float)MCH_Gui.scaleFactor);
      if(this.isDrawGui(player)) {
         MCH_EntityGLTD gltd = (MCH_EntityGLTD)player.ridingEntity;
         if(gltd.camera.getMode(0) == 1) {
            GL11.glEnable(3042);
            GL11.glColor4f(0.0F, 1.0F, 0.0F, 0.3F);
            int srcBlend = GL11.glGetInteger(3041);
            int dstBlend = GL11.glGetInteger(3040);
            GL11.glBlendFunc(1, 1);
            W_McClient.MOD_bindTexture("textures/gui/alpha.png");
            this.drawTexturedModalRectRotate(0.0D, 0.0D, (double)super.width, (double)super.height, (double)super.rand.nextInt(256), (double)super.rand.nextInt(256), 256.0D, 256.0D, 0.0F);
            GL11.glBlendFunc(srcBlend, dstBlend);
            GL11.glDisable(3042);
         }

         this.drawString(String.format("x%.1f", new Object[]{Float.valueOf(gltd.camera.getCameraZoom())}), super.centerX - 70, super.centerY + 10, -805306369);
         this.drawString(gltd.weaponCAS.getName(), super.centerX - 200, super.centerY + 65, gltd.countWait == 0?-819986657:-807468024);
         this.drawCommonPosition(gltd, -819986657);
         this.drawString(gltd.camera.getModeName(0), super.centerX + 30, super.centerY - 50, -819986657);
         this.drawSight(gltd.camera, -819986657);
         this.drawTargetPosition(gltd, -819986657, -807468024);
         this.drawKeyBind(gltd.camera, -805306369, -813727873);
      }
   }

   public void drawKeyBind(MCH_Camera camera, int color, int colorCannotUse) {
      int OffX = super.centerX + 55;
      int OffY = super.centerY + 40;
      this.drawString("DISMOUNT :", OffX, OffY + 0, color);
      this.drawString("CAM MODE :", OffX, OffY + 10, color);
      this.drawString("ZOOM IN   :", OffX, OffY + 20, camera.getCameraZoom() < 10.0F?color:colorCannotUse);
      this.drawString("ZOOM OUT :", OffX, OffY + 30, camera.getCameraZoom() > 1.0F?color:colorCannotUse);
      OffX += 60;
      StringBuilder var10001 = (new StringBuilder()).append(MCH_KeyName.getDescOrName(42)).append(" or ");
      MCH_Config var10002 = MCH_MOD.config;
      this.drawString(var10001.append(MCH_KeyName.getDescOrName(MCH_Config.KeyUnmount.prmInt)).toString(), OffX, OffY + 0, color);
      MCH_Config var6 = MCH_MOD.config;
      this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyCameraMode.prmInt), OffX, OffY + 10, color);
      var6 = MCH_MOD.config;
      this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt), OffX, OffY + 20, camera.getCameraZoom() < 10.0F?color:colorCannotUse);
      var6 = MCH_MOD.config;
      this.drawString(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt), OffX, OffY + 30, camera.getCameraZoom() > 1.0F?color:colorCannotUse);
   }

   public void drawCommonPosition(MCH_EntityGLTD gltd, int color) {
      boolean OFFSETX = true;
      this.drawString(String.format("X: %+.1f", new Object[]{Double.valueOf(gltd.posX)}), super.centerX - 145, super.centerY + 0, color);
      this.drawString(String.format("Y: %+.1f", new Object[]{Double.valueOf(gltd.posY)}), super.centerX - 145, super.centerY + 10, color);
      this.drawString(String.format("Z: %+.1f", new Object[]{Double.valueOf(gltd.posZ)}), super.centerX - 145, super.centerY + 20, color);
      this.drawString(String.format("AX: %+.1f", new Object[]{Float.valueOf(gltd.riddenByEntity.rotationYaw)}), super.centerX - 145, super.centerY + 40, color);
      this.drawString(String.format("AY: %+.1f", new Object[]{Float.valueOf(gltd.riddenByEntity.rotationPitch)}), super.centerX - 145, super.centerY + 50, color);
   }

   public void drawTargetPosition(MCH_EntityGLTD gltd, int color, int colorDanger) {
      if(gltd.riddenByEntity != null) {
         World w = gltd.riddenByEntity.worldObj;
         float yaw = gltd.riddenByEntity.rotationYaw;
         float pitch = gltd.riddenByEntity.rotationPitch;
         double tX = (double)(-MathHelper.sin(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double tZ = (double)(MathHelper.cos(yaw / 180.0F * 3.1415927F) * MathHelper.cos(pitch / 180.0F * 3.1415927F));
         double tY = (double)(-MathHelper.sin(pitch / 180.0F * 3.1415927F));
         double dist = (double)MathHelper.sqrt_double(tX * tX + tY * tY + tZ * tZ);
         tX = tX * 80.0D / dist;
         tY = tY * 80.0D / dist;
         tZ = tZ * 80.0D / dist;
         MCH_Camera c = gltd.camera;
         Vec3 src = W_WorldFunc.getWorldVec3(w, c.posX, c.posY, c.posZ);
         Vec3 dst = W_WorldFunc.getWorldVec3(w, c.posX + tX, c.posY + tY, c.posZ + tZ);
         MovingObjectPosition m = W_WorldFunc.clip(w, src, dst);
         boolean OS_X = true;
         if(m != null) {
            this.drawString(String.format("X: %+.2fm", new Object[]{Double.valueOf(m.hitVec.xCoord)}), super.centerX + 50, super.centerY - 5 - 15, color);
            this.drawString(String.format("Y: %+.2fm", new Object[]{Double.valueOf(m.hitVec.yCoord)}), super.centerX + 50, super.centerY - 5, color);
            this.drawString(String.format("Z: %+.2fm", new Object[]{Double.valueOf(m.hitVec.zCoord)}), super.centerX + 50, super.centerY - 5 + 15, color);
            double x = m.hitVec.xCoord - c.posX;
            double y = m.hitVec.yCoord - c.posY;
            double z = m.hitVec.zCoord - c.posZ;
            double len = Math.sqrt(x * x + y * y + z * z);
            this.drawCenteredString(String.format("[%.2fm]", new Object[]{Double.valueOf(len)}), super.centerX, super.centerY + 30, len > 20.0D?color:colorDanger);
         } else {
            this.drawString("X: --.--m", super.centerX + 50, super.centerY - 5 - 15, color);
            this.drawString("Y: --.--m", super.centerX + 50, super.centerY - 5, color);
            this.drawString("Z: --.--m", super.centerX + 50, super.centerY - 5 + 15, color);
            this.drawCenteredString("[--.--m]", super.centerX, super.centerY + 30, colorDanger);
         }

      }
   }

   private void drawSight(MCH_Camera camera, int color) {
      double posX = (double)super.centerX;
      double posY = (double)super.centerY;
      boolean SW = true;
      boolean SH = true;
      boolean SINV = true;
      double[] line2 = new double[]{posX - 30.0D, posY - 10.0D, posX - 30.0D, posY - 20.0D, posX - 30.0D, posY - 20.0D, posX - 10.0D, posY - 20.0D, posX - 30.0D, posY + 10.0D, posX - 30.0D, posY + 20.0D, posX - 30.0D, posY + 20.0D, posX - 10.0D, posY + 20.0D, posX + 30.0D, posY - 10.0D, posX + 30.0D, posY - 20.0D, posX + 30.0D, posY - 20.0D, posX + 10.0D, posY - 20.0D, posX + 30.0D, posY + 10.0D, posX + 30.0D, posY + 20.0D, posX + 30.0D, posY + 20.0D, posX + 10.0D, posY + 20.0D};
      this.drawLine(line2, color);
   }
}
