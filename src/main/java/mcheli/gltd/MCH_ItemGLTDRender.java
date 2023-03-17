package mcheli.gltd;

import mcheli.wrapper.W_McClient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class MCH_ItemGLTDRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.ENTITY;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return type == ItemRenderType.ENTITY;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      GL11.glPushMatrix();
      GL11.glEnable(2884);
      W_McClient.MOD_bindTexture("textures/gltd.png");
      switch(MCH_ItemGLTDRender.NamelessClass334173775.$SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[type.ordinal()]) {
      case 1:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         MCH_RenderGLTD.model.renderAll();
         GL11.glDisable('\u803a');
         break;
      case 2:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glTranslatef(0.0F, 0.005F, -0.165F);
         GL11.glRotatef(-10.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
         MCH_RenderGLTD.model.renderAll();
         GL11.glDisable('\u803a');
         break;
      case 3:
         GL11.glEnable('\u803a');
         GL11.glEnable(2903);
         GL11.glTranslatef(0.3F, 0.5F, -0.5F);
         GL11.glScalef(0.5F, 0.5F, 0.5F);
         GL11.glRotatef(10.0F, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
         MCH_RenderGLTD.model.renderAll();
         GL11.glDisable('\u803a');
      case 4:
      case 5:
      }

      GL11.glPopMatrix();
   }

   // $FF: synthetic class
   static class NamelessClass334173775 {

      // $FF: synthetic field
      static final int[] $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType = new int[ItemRenderType.values().length];


      static {
         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.ENTITY.ordinal()] = 1;
         } catch (NoSuchFieldError var5) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED.ordinal()] = 2;
         } catch (NoSuchFieldError var4) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED_FIRST_PERSON.ordinal()] = 3;
         } catch (NoSuchFieldError var3) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.INVENTORY.ordinal()] = 4;
         } catch (NoSuchFieldError var2) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.FIRST_PERSON_MAP.ordinal()] = 5;
         } catch (NoSuchFieldError var1) {
            ;
         }

      }
   }
}
