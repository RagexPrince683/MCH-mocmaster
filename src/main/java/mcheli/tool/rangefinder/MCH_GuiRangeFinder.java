package mcheli.tool.rangefinder;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_KeyName;
import mcheli.MCH_Lib;
import mcheli.MCH_MOD;
import mcheli.gui.MCH_Gui;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_GuiRangeFinder extends MCH_Gui {

   public MCH_GuiRangeFinder(Minecraft minecraft) {
      super(minecraft);
   }

   public void initGui() {
      super.initGui();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public boolean isDrawGui(EntityPlayer player) {
      return MCH_ItemRangeFinder.canUse(player);
   }

   public void drawGui(EntityPlayer player, boolean isThirdPersonView) {
      if(!isThirdPersonView) {
         GL11.glLineWidth((float)MCH_Gui.scaleFactor);
         if(this.isDrawGui(player)) {
            GL11.glDisable(3042);
            if(MCH_ItemRangeFinder.isUsingScope(player)) {
               this.drawRF(player);
            }

         }
      }
   }

   void drawRF(EntityPlayer player) {
      GL11.glEnable(3042);
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glBlendFunc(770, 771);
      W_McClient.MOD_bindTexture("textures/gui/rangefinder.png");

      double size;
      for(size = 512.0D; size < (double)super.width || size < (double)super.height; size *= 2.0D) {
         ;
      }

      this.drawTexturedModalRectRotate(-(size - (double)super.width) / 2.0D, -(size - (double)super.height) / 2.0D, size, size, 0.0D, 0.0D, 256.0D, 256.0D, 0.0F);
      GL11.glBlendFunc(srcBlend, dstBlend);
      GL11.glDisable(3042);
      double factor = size / 512.0D;
      double SCALE_FACTOR = (double)MCH_Gui.scaleFactor * factor;
      double CX = (double)(super.mc.displayWidth / 2);
      double CY = (double)(super.mc.displayHeight / 2);
      double px = (CX - 80.0D * SCALE_FACTOR) / SCALE_FACTOR;
      double py = (CY + 55.0D * SCALE_FACTOR) / SCALE_FACTOR;
      GL11.glPushMatrix();
      GL11.glScaled(factor, factor, factor);
      ItemStack item = player.getCurrentEquippedItem();
      int damage = (int)((double)(item.getMaxDamage() - item.getItemDamage()) / (double)item.getMaxDamage() * 100.0D);
      this.drawDigit(String.format("%3d", new Object[]{Integer.valueOf(damage)}), (int)px, (int)py, 13, damage > 0?-15663328:-61424);
      if(damage <= 0) {
         this.drawString("Please craft", (int)px + 40, (int)py + 0, -65536);
         this.drawString("redstone", (int)px + 40, (int)py + 10, -65536);
      }

      px = (CX - 20.0D * SCALE_FACTOR) / SCALE_FACTOR;
      if(damage > 0) {
         Vec3 mode = Vec3.createVectorHelper(player.posX, player.posY + (double)player.getEyeHeight(), player.posZ);
         Vec3 s = MCH_Lib.Rot2Vec3(player.rotationYaw, player.rotationPitch);
         s = mode.addVector(s.xCoord * 300.0D, s.yCoord * 300.0D, s.zCoord * 300.0D);
         MovingObjectPosition mop = player.worldObj.rayTraceBlocks(mode, s, true);
         if(mop != null && mop.typeOfHit != MovingObjectType.MISS) {
            int range = (int)player.getDistance(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
            this.drawDigit(String.format("%4d", new Object[]{Integer.valueOf(range)}), (int)px, (int)py, 13, -15663328);
         } else {
            this.drawDigit(String.format("----", new Object[0]), (int)px, (int)py, 13, -61424);
         }
      }

      py -= 4.0D;
      px -= 80.0D;
      drawRect((int)px, (int)py, (int)px + 30, (int)py + 2, -15663328);
      drawRect((int)px, (int)py, (int)px + MCH_ItemRangeFinder.rangeFinderUseCooldown / 2, (int)py + 2, -61424);
      this.drawString(String.format("x%.1f", new Object[]{Float.valueOf(MCH_ItemRangeFinder.zoom)}), (int)px, (int)py - 20, -1);
      px += 130.0D;
      int mode1 = MCH_ItemRangeFinder.mode;
      this.drawString(">", (int)px, (int)py - 30 + mode1 * 10, -1);
      px += 10.0D;
      this.drawString("Players/Vehicles", (int)px, (int)py - 30, mode1 == 0?-1:-12566464);
      this.drawString("Monsters/Mobs", (int)px, (int)py - 20, mode1 == 1?-1:-12566464);
      this.drawString("Mark Point", (int)px, (int)py - 10, mode1 == 2?-1:-12566464);
      GL11.glPopMatrix();
      px = (CX - 160.0D * SCALE_FACTOR) / (double)MCH_Gui.scaleFactor;
      py = (CY - 100.0D * SCALE_FACTOR) / (double)MCH_Gui.scaleFactor;
      if(px < 10.0D) {
         px = 10.0D;
      }

      if(py < 10.0D) {
         py = 10.0D;
      }

      StringBuilder var10000 = (new StringBuilder()).append("Spot      : ");
      MCH_Config var10001 = MCH_MOD.config;
      String s1 = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyAttack.prmInt)).toString();
      this.drawString(s1, (int)px, (int)py + 0, -1);
      var10000 = (new StringBuilder()).append("Zoom in   : ");
      var10001 = MCH_MOD.config;
      s1 = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyZoom.prmInt)).toString();
      this.drawString(s1, (int)px, (int)py + 10, MCH_ItemRangeFinder.zoom < 10.0F?-1:-12566464);
      var10000 = (new StringBuilder()).append("Zoom out : ");
      var10001 = MCH_MOD.config;
      s1 = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeySwWeaponMode.prmInt)).toString();
      this.drawString(s1, (int)px, (int)py + 20, MCH_ItemRangeFinder.zoom > 1.2F?-1:-12566464);
      var10000 = (new StringBuilder()).append("Mode      : ");
      var10001 = MCH_MOD.config;
      s1 = var10000.append(MCH_KeyName.getDescOrName(MCH_Config.KeyFlare.prmInt)).toString();
      this.drawString(s1, (int)px, (int)py + 30, -1);
   }
}
