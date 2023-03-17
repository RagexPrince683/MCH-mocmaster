package mcheli.gltd;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.MCH_RenderLib;
import mcheli.wrapper.W_Lib;
import mcheli.wrapper.W_Render;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class MCH_RenderGLTD extends W_Render {

   public static final Random rand = new Random();
   public static IModelCustom model;


   public MCH_RenderGLTD() {
      super.shadowSize = 0.5F;
      model = null;
   }

   public void doRender(Entity entity, double posX, double posY, double posZ, float par8, float tickTime) {
      if(entity instanceof MCH_EntityGLTD) {
         MCH_EntityGLTD gltd = (MCH_EntityGLTD)entity;
         GL11.glPushMatrix();
         GL11.glTranslated(posX, posY, posZ);
         this.setCommonRenderParam(true, entity.getBrightnessForRender(tickTime));
         this.bindTexture("textures/gltd.png");
         Minecraft mc = Minecraft.getMinecraft();
         boolean isNotRenderHead = false;
         if(gltd.riddenByEntity != null) {
            gltd.isUsedPlayer = true;
            gltd.renderRotaionYaw = gltd.riddenByEntity.rotationYaw;
            gltd.renderRotaionPitch = gltd.riddenByEntity.rotationPitch;
            isNotRenderHead = mc.gameSettings.thirdPersonView == 0 && W_Lib.isClientPlayer(gltd.riddenByEntity);
         }

         if(gltd.isUsedPlayer) {
            GL11.glPushMatrix();
            GL11.glRotatef(-gltd.rotationYaw, 0.0F, 1.0F, 0.0F);
            model.renderPart("$body");
            GL11.glPopMatrix();
         } else {
            GL11.glRotatef(-gltd.rotationYaw, 0.0F, 1.0F, 0.0F);
            model.renderPart("$body");
         }

         GL11.glTranslatef(0.0F, 0.45F, 0.0F);
         if(gltd.isUsedPlayer) {
            GL11.glRotatef(gltd.renderRotaionYaw, 0.0F, -1.0F, 0.0F);
            GL11.glRotatef(gltd.renderRotaionPitch, 1.0F, 0.0F, 0.0F);
         }

         GL11.glTranslatef(0.0F, -0.45F, 0.0F);
         if(!isNotRenderHead) {
            model.renderPart("$head");
         }

         GL11.glTranslatef(0.0F, 0.45F, 0.0F);
         this.restoreCommonRenderParam();
         GL11.glDisable(2896);
         Vec3[] v = new Vec3[]{Vec3.createVectorHelper(0.0D, 0.2D, 0.0D), Vec3.createVectorHelper(0.0D, 0.2D, 100.0D)};
         int a = rand.nextInt(64);
         MCH_RenderLib.drawLine(v, 1619066752 | a << 24);
         GL11.glEnable(2896);
         GL11.glPopMatrix();
      }
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return W_Render.TEX_DEFAULT;
   }

}
