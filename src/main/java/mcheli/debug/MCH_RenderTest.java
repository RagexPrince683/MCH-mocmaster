package mcheli.debug;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderTest extends W_Render {

   protected MCH_ModelTest model;
   private float offsetX;
   private float offsetY;
   private float offsetZ;
   private String textureName;


   public MCH_RenderTest(float x, float y, float z, String texture_name) {
      this.offsetX = x;
      this.offsetY = y;
      this.offsetZ = z;
      this.textureName = texture_name;
      this.model = new MCH_ModelTest();
   }

   public void doRender(Entity e, double posX, double posY, double posZ, float par8, float par9) {
      MCH_Config var10000 = MCH_MOD.config;
      if(MCH_Config.TestMode.prmBool) {
         GL11.glPushMatrix();
         GL11.glTranslated(posX + (double)this.offsetX, posY + (double)this.offsetY, posZ + (double)this.offsetZ);
         GL11.glScalef(e.width, e.height, e.width);
         GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
         float prevYaw;
         if(e.rotationYaw - e.prevRotationYaw < -180.0F) {
            prevYaw = e.prevRotationYaw - 360.0F;
         } else if(e.prevRotationYaw - e.rotationYaw < -180.0F) {
            prevYaw = e.prevRotationYaw + 360.0F;
         } else {
            prevYaw = e.prevRotationYaw;
         }

         float yaw = -(prevYaw + (e.rotationYaw - prevYaw) * par9) - 180.0F;
         float pitch = -(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * par9);
         GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
         this.bindTexture("textures/" + this.textureName + ".png");
         this.model.renderModel(0.0D, 0.0D, 0.1F);
         GL11.glPopMatrix();
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
