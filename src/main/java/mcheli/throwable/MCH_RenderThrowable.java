package mcheli.throwable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.throwable.MCH_EntityThrowable;
import mcheli.throwable.MCH_ThrowableInfo;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderThrowable extends W_Render {

   public MCH_RenderThrowable() {
      super.shadowSize = 0.0F;
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
