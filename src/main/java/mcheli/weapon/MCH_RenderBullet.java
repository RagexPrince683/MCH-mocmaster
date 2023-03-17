package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderBullet extends MCH_RenderBulletBase {

   public void renderBullet(Entity entity, double posX, double posY, double posZ, float yaw, float tickTime) {
      MCH_EntityBaseBullet blt = (MCH_EntityBaseBullet)entity;
      GL11.glPushMatrix();
      double var10000 = entity.prevPosX + entity.motionX * (double)tickTime;
      var10000 = entity.prevPosY + entity.motionY * (double)tickTime;
      var10000 = entity.prevPosZ + entity.motionZ * (double)tickTime;
      GL11.glTranslated(posX, posY, posZ);
      GL11.glRotatef(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
      this.renderModel(blt);
      GL11.glPopMatrix();
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
