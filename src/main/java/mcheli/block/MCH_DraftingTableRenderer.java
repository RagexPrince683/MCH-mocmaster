package mcheli.block;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class MCH_DraftingTableRenderer extends TileEntitySpecialRenderer {

   public void renderTileEntityAt(TileEntity tile, double posX, double posY, double posZ, float var8) {
      GL11.glPushMatrix();
      GL11.glEnable(2884);
      GL11.glTranslated(posX + 0.5D, posY, posZ + 0.5D);
      float yaw = (float)(-tile.getBlockMetadata() * 45 + 180);
      GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
      RenderHelper.enableStandardItemLighting();
      GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
      GL11.glEnable(3042);
      int srcBlend = GL11.glGetInteger(3041);
      int dstBlend = GL11.glGetInteger(3040);
      GL11.glBlendFunc(770, 771);
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.SmoothShading.prmBool) {
         GL11.glShadeModel(7425);
      }

      W_McClient.MOD_bindTexture("textures/blocks/drafting_table.png");
      MCH_ModelManager.render("blocks", "drafting_table");
      GL11.glBlendFunc(srcBlend, dstBlend);
      GL11.glDisable(3042);
      GL11.glShadeModel(7424);
      GL11.glPopMatrix();
   }
}
