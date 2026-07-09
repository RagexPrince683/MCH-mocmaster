package mcheli.aircraft;

import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;
import mcheli.wrapper.W_McClient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.IModelCustom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

/**
 * Renders placeable vehicle items with their loaded 3D vehicle model instead of the flat item icon.
 */
public class MCH_VehicleItemModelRender implements IItemRenderer {

   private static final Map MODEL_DISPLAY_LISTS = new HashMap();
   private static final LinkedList MODEL_BUILD_QUEUE = new LinkedList();
   private static final Set QUEUED_MODELS = new HashSet();
   private static final long MODEL_BUILD_INTERVAL_MS = 25L;
   private static long nextModelBuildTime;

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      MCH_BaseVehicleInfo info = getInfo(item);
      if(info == null || info.model == null || !is3DIconEnabled(info)) {
         return false;
      }

      queueModelBuild(info);
      processQueuedModelBuild();
      return isModelCached(info);
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return true;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object ... data) {
      MCH_BaseVehicleInfo info = getInfo(item);
      if(info == null || info.model == null || !is3DIconEnabled(info)) {
         return;
      }

      queueModelBuild(info);
      processQueuedModelBuild();
      if(!isModelCached(info)) {
         return;
      }

      GL11.glPushMatrix();
      GL11.glEnable('\u803a');
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      transform(type, info);
      W_McClient.MOD_bindTexture("textures/" + info.getDirectoryName() + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
      MCH_RenderBaseVehicle.beginSkinOverlayRender(info.getDirectoryName(), info.name);
      try {
         renderCachedModel(info);
      } finally {
         MCH_RenderBaseVehicle.endSkinOverlayRender();
         GL11.glPopMatrix();
      }
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
   }

   private static void renderCachedModel(MCH_BaseVehicleInfo info) {
      CachedDisplayList cached = (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);
      if(cached != null && cached.model == info.model) {
         cached.render();
      }
   }

   private static boolean isModelCached(MCH_BaseVehicleInfo info) {
      CachedDisplayList cached = (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);
      return cached != null && cached.model == info.model;
   }

   private static void queueModelBuild(MCH_BaseVehicleInfo info) {
      CachedDisplayList cached = (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);
      if(cached != null && cached.model != info.model) {
         cached.delete();
         MODEL_DISPLAY_LISTS.remove(info);
         cached = null;
      }
      if(cached == null && QUEUED_MODELS.add(info)) {
         MODEL_BUILD_QUEUE.add(info);
      }
   }

   private static void processQueuedModelBuild() {
      long now = Minecraft.getSystemTime();
      if(now < nextModelBuildTime || MODEL_BUILD_QUEUE.isEmpty()) {
         return;
      }

      MCH_BaseVehicleInfo info = (MCH_BaseVehicleInfo)MODEL_BUILD_QUEUE.removeFirst();
      QUEUED_MODELS.remove(info);
      if(info != null && info.model != null && is3DIconEnabled(info)) {
         MODEL_DISPLAY_LISTS.put(info, new CachedDisplayList(info.model));
         nextModelBuildTime = now + MODEL_BUILD_INTERVAL_MS;
      }
   }

   private static class CachedDisplayList {
      private final IModelCustom model;
      private final int displayList;

      private CachedDisplayList(IModelCustom model) {
         this.model = model;
         this.displayList = GL11.glGenLists(1);
         if(this.displayList != 0) {
            GL11.glNewList(this.displayList, GL11.GL_COMPILE);
            MCH_RenderBaseVehicle.renderAllModel(model);
            GL11.glEndList();
         }
      }

      private void render() {
         if(this.displayList != 0) {
            GL11.glCallList(this.displayList);
         } else {
            MCH_RenderBaseVehicle.renderAllModel(this.model);
         }
      }

      private void delete() {
         if(this.displayList != 0) {
            GL11.glDeleteLists(this.displayList, 1);
         }
      }
   }

   private static MCH_BaseVehicleInfo getInfo(ItemStack item) {
      return item != null && item.getItem() instanceof MCH_ItemBaseVehicle
         ? ((MCH_ItemBaseVehicle)item.getItem()).getAircraftInfo() : null;
   }

   private static boolean is3DIconEnabled(MCH_BaseVehicleInfo info) {
      return info.enable3DItemIcon && (MCH_Config.Override3DItemIcon == null || !MCH_Config.Override3DItemIcon.prmBool);
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
      scale *= getTypeScale(info) * info.itemIconScaleFactor;
      GL11.glScalef(scale, scale, scale);
   }

   private static float getTypeScale(MCH_BaseVehicleInfo info) {
      String directory = info.getDirectoryName();
      if("helicopters".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Heli3DItemIconScale);
      }
      if("planes".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Plane3DItemIconScale);
      }
      if("ships".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Ship3DItemIconScale);
      }
      if("tanks".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Tank3DItemIconScale);
      }
      if("vehicles".equalsIgnoreCase(directory) || "turrets".equalsIgnoreCase(directory)) {
         return getScale(MCH_Config.Turret3DItemIconScale);
      }
      return 1.0F;
   }

   private static float getScale(MCH_ConfigPrm prm) {
      return prm != null ? Math.max((float)prm.prmDouble, 0.01F) : 1.0F;
   }
}
