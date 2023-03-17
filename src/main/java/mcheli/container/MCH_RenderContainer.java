package mcheli.container;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.MCH_ModelManager;
import mcheli.aircraft.MCH_RenderAircraft;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class MCH_RenderContainer extends W_Render {

   public static final Random rand = new Random();


   public MCH_RenderContainer() {
      super.shadowSize = 0.5F;
   }

   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
      if(!MCH_RenderAircraft.shouldSkipRender(entity)) {
         GL11.glPushMatrix();
         GL11.glEnable(2884);
         GL11.glTranslated(posX, posY - 0.2D, posZ);
         float yaw = MCH_Lib.smoothRot(entity.rotationYaw, entity.prevRotationYaw, tickTime);
         float pitch = MCH_Lib.smoothRot(entity.rotationPitch, entity.prevRotationPitch, tickTime);
         GL11.glRotatef(yaw, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
         this.bindTexture("textures/container.png");
         MCH_ModelManager.render("container");
         GL11.glPopMatrix();
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }

}
