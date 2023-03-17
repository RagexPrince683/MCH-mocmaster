package mcheli.weapon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_Lib;
import mcheli.wrapper.W_Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class MCH_RenderAAMissile extends MCH_RenderBulletBase {

   public MCH_RenderAAMissile() {
      super.shadowSize = 0.5F;
   }

   public void renderBullet(Entity entity, double posX, double posY, double posZ, float par8, float par9) {
      if(entity instanceof MCH_EntityAAMissile) {
         MCH_EntityAAMissile aam = (MCH_EntityAAMissile)entity;
         double mx = aam.prevMotionX + (aam.motionX - aam.prevMotionX) * (double)par9;
         double my = aam.prevMotionY + (aam.motionY - aam.prevMotionY) * (double)par9;
         double mz = aam.prevMotionZ + (aam.motionZ - aam.prevMotionZ) * (double)par9;
         GL11.glPushMatrix();
         GL11.glTranslated(posX, posY, posZ);
         Vec3 v = MCH_Lib.getYawPitchFromVec(mx, my, mz);
         GL11.glRotatef((float)v.yCoord - 90.0F, 0.0F, -1.0F, 0.0F);
         GL11.glRotatef((float)v.zCoord, -1.0F, 0.0F, 0.0F);
         this.renderModel(aam);
         GL11.glPopMatrix();
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }
}
