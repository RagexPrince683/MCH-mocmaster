package mcheli.aircraft;

import mcheli.wrapper.W_McClient;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

/**
 * Renders placeable vehicle items with their loaded 3D vehicle model instead of the flat item icon.
 */
public class MCH_VehicleItemModelRender implements IItemRenderer {

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      return getInfo(item) != null;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return true;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      MCH_BaseVehicleInfo info = getInfo(item);
      if(info == null || info.model == null) {
         return;
      }

      GL11.glPushMatrix();
      GL11.glEnable('\u803a');
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      transform(type, info);
      W_McClient.MOD_bindTexture("textures/" + info.getDirectoryName() + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
      MCH_RenderBaseVehicle.beginSkinOverlayRender(info.getDirectoryName(), info.name);
      try {
         MCH_RenderBaseVehicle.renderAllModel(info.model);
      } finally {
         MCH_RenderBaseVehicle.endSkinOverlayRender();
         GL11.glPopMatrix();
      }
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
   }

   private static MCH_BaseVehicleInfo getInfo(ItemStack item) {
      return item != null && item.getItem() instanceof MCH_ItemBaseVehicle
         ? ((MCH_ItemBaseVehicle)item.getItem()).getAircraftInfo() : null;
   }

   private static void transform(ItemRenderType type, MCH_BaseVehicleInfo info) {
      switch(type) {
      case ENTITY:
         GL11.glTranslatef(0.0F, 0.35F, 0.0F);
         GL11.glRotatef(35.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
         break;
      case EQUIPPED:
         GL11.glTranslatef(0.25F, 0.45F, 0.55F);
         GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
         break;
      case EQUIPPED_FIRST_PERSON:
         GL11.glTranslatef(0.65F, 0.35F, 0.35F);
         GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(125.0F, 0.0F, 1.0F, 0.0F);
         break;
      case INVENTORY:
         GL11.glTranslatef(0.0F, -0.35F, 0.0F);
         GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
         break;
      default:
         break;
      }

      float largest = Math.max(Math.max(info.bodyWidth, info.bodyHeight), 1.0F);
      float scale = type == ItemRenderType.INVENTORY ? 1.35F / largest : 0.75F / largest;
      if(type == ItemRenderType.ENTITY) {
         scale = 1.0F / largest;
      }
      GL11.glScalef(scale, scale, scale);
   }
}
