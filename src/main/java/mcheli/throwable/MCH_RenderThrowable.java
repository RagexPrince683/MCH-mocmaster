package mcheli.throwable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ClientCommonTickHandler;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render {
	   private static final ResourceLocation ir_strobe = new ResourceLocation("mcheli", "textures/ir_strobe.png");

   public MCH_RenderThrowable() {
      super.shadowSize = 0.0F;
   }

   public void renderStrobe(MCH_EntityThrowable throwable, float timer) {
	   int cm = MCH_ClientCommonTickHandler.cameraMode;
	   if(cm != 2) {
		  // System.out.println("Not rendering");
		   return;
		}
	   int ticks = throwable.ticksExisted % 20;
	   
	   if(ticks < timer) {

		   
		   float alpha = ticks != 2 && ticks != 1?0.5F:1.0F;
		   EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		   if(player != null) {
			   System.out.println("Am rendering");
			   short j = 240;
			   short k = 240;
			   OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j / 1.0F, (float)k / 1.0F);
			   GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			   float f1 = 0.080000006F;
			   GL11.glPushMatrix();
			   GL11.glTranslated(throwable.posX, throwable.posY + (double)((float)((double)throwable.height * 0.75D)), throwable.posZ);
			   GL11.glNormal3f(0.0F, 1.0F, 0.0F);
			   GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
			   GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
			   GL11.glScalef(-f1, -f1, f1);
			   GL11.glEnable(3042);
			   OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			   GL11.glEnable(3553);
			   RenderManager.instance.renderEngine.bindTexture(ir_strobe);
			   GL11.glAlphaFunc(516, 0.003921569F);
			   Tessellator tessellator = Tessellator.instance;
			   tessellator.startDrawingQuads();
			   tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha * (cm == 1?0.9F:0.5F));
			   int i = (int)Math.max(throwable.width*10, throwable.height*10) * 20;
			   tessellator.addVertexWithUV((double)(-i), (double)(-i), 0.1D, 0.0D, 0.0D);
			   tessellator.addVertexWithUV((double)(-i), (double)i, 0.1D, 0.0D, 1.0D);
			   tessellator.addVertexWithUV((double)i, (double)i, 0.1D, 1.0D, 1.0D);
			   tessellator.addVertexWithUV((double)i, (double)(-i), 0.1D, 1.0D, 0.0D);
			   tessellator.draw();
			   GL11.glEnable(2896);
			   GL11.glPopMatrix();
		   }
	   }
   }
   
   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
      MCH_EntityThrowable throwable = (MCH_EntityThrowable)entity;
      MCH_ThrowableInfo info = throwable.getInfo();
      if(info != null) {
    	 
         GL11.glPushMatrix();
         GL11.glTranslated(posX, posY, posZ);
         GL11.glRotatef(entity.rotationYaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
         this.setCommonRenderParam(true, entity.getBrightnessForRender(tickTime));
         if(info.bombletDiff > 0) { renderStrobe(throwable, info.bombletDiff);}
         if(info.model != null) {
            this.bindTexture("textures/throwable/" + info.name + ".png");
            info.model.renderAll();
         }

         this.restoreCommonRenderParam();
         GL11.glPopMatrix();
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
