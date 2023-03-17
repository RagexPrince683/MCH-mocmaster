package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderA10 extends MCH_RenderBulletBase {

   public MCH_RenderA10() {
      super.shadowSize = 10.5F;
   }

   public void renderBullet(Entity e, double posX, double posY, double posZ, float par8, float tickTime) {
      if(e instanceof MCH_EntityA10) {
         if(((MCH_EntityA10)e).isRender()) {
        	MCH_EntityA10 a10 = (MCH_EntityA10)e;
            GL11.glPushMatrix();
            GL11.glTranslated(posX, posY, posZ);
            
            float yaw = -(e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * tickTime);
            float pitch = -(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * tickTime);
            GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            this.bindTexture("textures/planes/" + a10.weaponInfo.bombletModelName + ".png");       
            MCH_ModelManager.render("models/planes/"+a10.weaponInfo.bombletModelName);
            GL11.glPopMatrix();
         }
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
