package mcheli.weapon;

import mcheli.MCH_Color;
import mcheli.weapon.MCH_BulletModel;
import mcheli.weapon.MCH_EntityBaseBullet;
import mcheli.wrapper.W_Block;
import mcheli.wrapper.W_Render;
import mcheli.wrapper.W_WorldFunc;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public abstract class MCH_RenderBulletBase extends W_Render {

   public void doRender(Entity e, double result, double result2, double result3, float result4, float result5) {
      int dstBlend;
      if(e instanceof MCH_EntityBaseBullet && ((MCH_EntityBaseBullet)e).getInfo() != null) {
         MCH_Color srcBlend = ((MCH_EntityBaseBullet)e).getInfo().color;

         for(dstBlend = 0; dstBlend < 3; ++dstBlend) {
            Block b = W_WorldFunc.getBlock(e.worldObj, (int)(e.posX + 0.5D), (int)(e.posY + 1.5D - (double)dstBlend), (int)(e.posZ + 0.5D));
            if(b != null && b == W_Block.getWater()) {
               srcBlend = ((MCH_EntityBaseBullet)e).getInfo().colorInWater;
               break;
            }
         }

         GL11.glColor4f(srcBlend.r, srcBlend.g, srcBlend.b, srcBlend.a);
      } else {
         GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
      }

      GL11.glAlphaFunc(516, 0.001F);
      GL11.glEnable(2884);
      GL11.glEnable(3042);
      int color2 = GL11.glGetInteger(3041);
      dstBlend = GL11.glGetInteger(3040);
      GL11.glBlendFunc(770, 771);
      this.renderBullet(e, result, result2, result3, result4, result5);
      GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
      GL11.glBlendFunc(color2, dstBlend);
      GL11.glDisable(3042);
   }

   public void renderModel(MCH_EntityBaseBullet e) {
      MCH_BulletModel model = e.getBulletModel();
      if(model != null) {
         try {
            this.bindTexture("textures/bullets/" + model.name + ".png");
         } catch (Exception exception) {
            System.out.println("Texture not found : " + model.name);

         }
         model.model.renderAll();
      }

   }

   public abstract void renderBullet(Entity result1, double result2, double result4, double result6, float result8, float result9);
}
