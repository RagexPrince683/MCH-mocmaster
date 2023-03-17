package mcheli.tool.rangefinder;

import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_McClient;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class MCH_ItemRenderRangeFinder implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.ENTITY;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.ENTITY;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      GL11.glPushMatrix();
      W_McClient.MOD_bindTexture("textures/rangefinder.png");
      float size = 1.0F;
      switch(MCH_ItemRenderRangeFinder.NamelessClass956777817.$SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[type.ordinal()]) {
      case 1:
         size = 2.2F;
         GL11.glScalef(size, size, size);
         GL11.glRotatef(-130.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(70.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(5.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(0.0F, 0.0F, -0.0F);
         MCH_ModelManager.render("rangefinder");
         break;
      case 2:
         size = 2.2F;
         GL11.glScalef(size, size, size);
         GL11.glRotatef(-130.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(70.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(5.0F, 0.0F, 0.0F, 1.0F);
         if(Minecraft.getMinecraft().thePlayer.getItemInUseDuration() > 0) {
            GL11.glTranslatef(0.4F, -0.35F, -0.3F);
         } else {
            GL11.glTranslatef(0.2F, -0.35F, -0.3F);
         }

         MCH_ModelManager.render("rangefinder");
         break;
      case 3:
         if(!MCH_ItemRangeFinder.isUsingScope(Minecraft.getMinecraft().thePlayer)) {
            size = 2.2F;
            GL11.glScalef(size, size, size);
            GL11.glRotatef(-210.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-10.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.06F, 0.53F, -0.1F);
            MCH_ModelManager.render("rangefinder");
         }
      }

      GL11.glPopMatrix();
   }

   // $FF: synthetic class
   static class NamelessClass956777817 {

      // $FF: synthetic field
      static final int[] $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType = new int[ItemRenderType.values().length];


      static {
         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.ENTITY.ordinal()] = 1;
         } catch (NoSuchFieldError var3) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED.ordinal()] = 2;
         } catch (NoSuchFieldError var2) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED_FIRST_PERSON.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
            ;
         }

      }
   }
}
