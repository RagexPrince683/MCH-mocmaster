package mcheli.flare;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderFlare extends W_Render {

   protected MCH_ModelFlare model = new MCH_ModelFlare();


   public void doRender(Entity entity, double posX, double posY, double posZ, float yaw, float partialTickTime) {
      GL11.glPushMatrix();
      GL11.glEnable(2884);
      double var10000 = entity.prevPosX + entity.motionX * (double)partialTickTime;
      var10000 = entity.prevPosY + entity.motionY * (double)partialTickTime;
      var10000 = entity.prevPosZ + entity.motionZ * (double)partialTickTime;
      GL11.glTranslated(posX, posY, posZ);
      GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);
      GL11.glColor4f(1.0F, 1.0F, 0.5F, 1.0F);
      this.bindTexture("textures/flare.png");
      this.model.renderModel(0.0D, 0.0D, 0.0625F);
      GL11.glPopMatrix();
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
