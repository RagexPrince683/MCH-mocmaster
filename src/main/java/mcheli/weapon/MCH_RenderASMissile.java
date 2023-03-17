package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderASMissile extends MCH_RenderBulletBase {

   public MCH_RenderASMissile() {
      super.shadowSize = 0.5F;
   }

   public void renderBullet(Entity entity, double posX, double posY, double posZ, float yaw, float partialTickTime) {
      if(entity instanceof MCH_EntityBaseBullet) {
         MCH_EntityBaseBullet bullet = (MCH_EntityBaseBullet)entity;
         GL11.glPushMatrix();
         GL11.glTranslated(posX, posY, posZ);
         GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-entity.rotationPitch, -1.0F, 0.0F, 0.0F);
         this.renderModel(bullet);
         GL11.glPopMatrix();
      }

   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
