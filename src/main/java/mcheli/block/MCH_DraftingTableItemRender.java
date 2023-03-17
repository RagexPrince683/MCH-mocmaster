package mcheli.block;

import mcheli.MCH_ModelManager;
import mcheli.wrapper.W_McClient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class MCH_DraftingTableItemRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      switch(MCH_DraftingTableItemRender.NamelessClass1811513524.$SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[type.ordinal()]) {
      case 1:
      case 2:
      case 3:
      case 4:
         return true;
      default:
         return false;
      }
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return true;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      GL11.glPushMatrix();
      W_McClient.MOD_bindTexture("textures/blocks/drafting_table.png");
      GL11.glEnable('\u803a');
      switch(MCH_DraftingTableItemRender.NamelessClass1811513524.$SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[type.ordinal()]) {
      case 1:
         GL11.glTranslatef(0.0F, 0.5F, 0.0F);
         GL11.glScalef(1.5F, 1.5F, 1.5F);
         break;
      case 2:
         GL11.glTranslatef(0.0F, 0.0F, 0.5F);
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         break;
      case 3:
         GL11.glTranslatef(0.75F, 0.0F, 0.0F);
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         GL11.glRotatef(90.0F, 0.0F, -1.0F, 0.0F);
         break;
      case 4:
         float INV_SIZE = 0.75F;
         GL11.glTranslatef(0.0F, -0.5F, 0.0F);
         GL11.glScalef(0.75F, 0.75F, 0.75F);
      }

      MCH_ModelManager.render("blocks", "drafting_table");
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
   }

   // $FF: synthetic class
   static class NamelessClass1811513524 {

      // $FF: synthetic field
      static final int[] $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType = new int[ItemRenderType.values().length];


      static {
         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.ENTITY.ordinal()] = 1;
         } catch (NoSuchFieldError var4) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.EQUIPPED_FIRST_PERSON.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
            ;
         }

         try {
            $SwitchMap$net$minecraftforge$client$IItemRenderer$ItemRenderType[ItemRenderType.INVENTORY.ordinal()] = 4;
         } catch (NoSuchFieldError var1) {
            ;
         }

      }
   }
}
