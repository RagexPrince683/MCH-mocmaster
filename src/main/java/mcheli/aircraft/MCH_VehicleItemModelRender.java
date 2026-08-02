package mcheli.aircraft;

import mcheli.MCH_Config;
import mcheli.MCH_ConfigPrm;
import mcheli.wrapper.W_McClient;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.IModelCustom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

/**
 * Renders placeable vehicle items with their loaded 3D vehicle model instead of
 * the flat item icon.
 */
public class MCH_VehicleItemModelRender implements IItemRenderer {

   private static final Map MODEL_DISPLAY_LISTS = new HashMap();
   private static final LinkedList MODEL_BUILD_QUEUE = new LinkedList();
   private static final Set QUEUED_MODELS = new HashSet();

   private static final int MODEL_BUILD_FACES_PER_STEP = 64;
   private static final long MODEL_BUILD_INTERVAL_MS = 5L;

   /**
    * The one model currently being compiled.
    *
    * This model remains active until its display list is completely built.
    * The scheduler will not rotate between partially built models.
    */
   private static MCH_BaseVehicleInfo activeModelBuild;

   private static long nextModelBuildTime;

   /** Releases reload-stale GL state. Must only be called on the client thread. */
   public static void resetForReload() {
      for(Object value : MODEL_DISPLAY_LISTS.values()) {
         ((CachedDisplayList)value).delete();
      }
      MODEL_DISPLAY_LISTS.clear();
      MODEL_BUILD_QUEUE.clear();
      QUEUED_MODELS.clear();
      activeModelBuild = null;
      nextModelBuildTime = 0L;
   }

   /** Releases only the display lists associated with one definition snapshot. */
   public static void invalidate(MCH_BaseVehicleInfo info) {
      if(info == null) return;
      CachedDisplayList cached = (CachedDisplayList)MODEL_DISPLAY_LISTS.remove(info);
      if(cached != null) cached.delete();
      MODEL_BUILD_QUEUE.remove(info);
      QUEUED_MODELS.remove(info);
      if(activeModelBuild == info) activeModelBuild = null;
   }

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      MCH_BaseVehicleInfo info = getInfo(item);

      if(info == null || info.model == null || !is3DIconEnabled(info)) {
         return false;
      }

      queueModelBuild(info);
      processQueuedModelBuild();

      return hasRenderableModel(info);
   }

   public boolean shouldUseRenderHelper(
           ItemRenderType type,
           ItemStack item,
           ItemRendererHelper helper
   ) {
      return true;
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
      MCH_BaseVehicleInfo info = getInfo(item);

      if(info == null || info.model == null || !is3DIconEnabled(info)) {
         return;
      }

      queueModelBuild(info);
      processQueuedModelBuild();

      if(!hasRenderableModel(info)) {
         return;
      }

      GL11.glPushMatrix();
      GL11.glEnable('\u803a');
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

      transform(type, info);

      net.minecraft.util.ResourceLocation original = new net.minecraft.util.ResourceLocation("mcheli", "textures/" + info.getDirectoryName() + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
      Minecraft.getMinecraft().getTextureManager().bindTexture(mcheli.texture.MCH_ModelTextureRepairManager.resolve(original, info.model, info.getDirectoryName() + "/" + info.name));

      MCH_RenderBaseVehicle.beginSkinOverlayRender(
              info.getDirectoryName(),
              info.name
      );

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
      CachedDisplayList cached =
              (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);

      if(cached != null && cached.model == info.model) {
         cached.render();
      }
   }

   private static boolean hasRenderableModel(MCH_BaseVehicleInfo info) {
      CachedDisplayList cached =
              (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);

      return cached != null
              && cached.model == info.model
              && cached.hasRenderableChunks();
   }

   private static void queueModelBuild(MCH_BaseVehicleInfo info) {
      CachedDisplayList cached =
              (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);

      if(cached != null && cached.model != info.model) {
         cached.delete();
         MODEL_DISPLAY_LISTS.remove(info);
         cached = null;
      }

      /*
       * The active model is already scheduled and must not be inserted into
       * the waiting queue again every time its item icon renders.
       */
      if(info == activeModelBuild) {
         return;
      }

      if((cached == null || !cached.isComplete())
              && QUEUED_MODELS.add(info)) {

         MODEL_BUILD_QUEUE.add(info);
      }
   }

   /**
    * Builds exactly one chunk per interval while keeping one model active
    * until that entire model is complete.
    */
   private static void processQueuedModelBuild() {
      long now = Minecraft.getSystemTime();

      if(now < nextModelBuildTime) {
         return;
      }

      /*
       * Only select another model after the current model has finished or
       * become invalid.
       */
      if(activeModelBuild == null) {
         if(MODEL_BUILD_QUEUE.isEmpty()) {
            return;
         }

         activeModelBuild =
                 (MCH_BaseVehicleInfo)MODEL_BUILD_QUEUE.removeFirst();
      }

      MCH_BaseVehicleInfo info = activeModelBuild;

      if(info == null
              || info.model == null
              || !is3DIconEnabled(info)) {

         finishActiveModelBuild(info);
         nextModelBuildTime = now + MODEL_BUILD_INTERVAL_MS;
         return;
      }

      CachedDisplayList cached =
              (CachedDisplayList)MODEL_DISPLAY_LISTS.get(info);

      if(cached == null || cached.model != info.model) {
         if(cached != null) {
            cached.delete();
         }

         cached = new CachedDisplayList(info.model);
         MODEL_DISPLAY_LISTS.put(info, cached);
      }

      cached.buildNextChunk();

      /*
       * Do not rotate this incomplete model to the back of the queue.
       * It stays active and receives the next scheduled build step.
       */
      if(cached.isComplete()) {
         finishActiveModelBuild(info);
      }

      nextModelBuildTime = now + MODEL_BUILD_INTERVAL_MS;
   }

   private static void finishActiveModelBuild(MCH_BaseVehicleInfo info) {
      if(info != null) {
         QUEUED_MODELS.remove(info);
      }

      activeModelBuild = null;
   }

   private static class CachedDisplayList {

      private final IModelCustom model;
      private final List displayLists = new ArrayList();
      private final int faceCount;

      private int nextFace = 1;
      private boolean complete;

      private CachedDisplayList(IModelCustom model) {
         this.model = model;
         this.faceCount = getFaceCount(model);
         this.complete = this.faceCount <= 0;
      }

      private boolean hasRenderableChunks() {
         return !this.displayLists.isEmpty();
      }

      private boolean isComplete() {
         return this.complete;
      }

      private void buildNextChunk() {
         if(this.complete) {
            return;
         }

         int displayList = GL11.glGenLists(1);

         if(displayList == 0) {
            this.complete = true;
            return;
         }

         int endFace = Math.min(
                 this.nextFace + MODEL_BUILD_FACES_PER_STEP - 1,
                 this.faceCount
         );

         GL11.glNewList(displayList, GL11.GL_COMPILE);
         renderModelRange(this.model, this.nextFace, endFace);
         GL11.glEndList();

         this.displayLists.add(Integer.valueOf(displayList));
         this.nextFace = endFace + 1;
         this.complete = this.nextFace > this.faceCount;
      }

      private void render() {
         for(int i = 0; i < this.displayLists.size(); ++i) {
            GL11.glCallList(
                    ((Integer)this.displayLists.get(i)).intValue()
            );
         }
      }

      private void delete() {
         for(int i = 0; i < this.displayLists.size(); ++i) {
            GL11.glDeleteLists(
                    ((Integer)this.displayLists.get(i)).intValue(),
                    1
            );
         }

         this.displayLists.clear();
      }
   }

   private static int getFaceCount(IModelCustom model) {
      if(model instanceof W_WavefrontObject) {
         return ((W_WavefrontObject)model).getFaceNum();
      }

      if(model instanceof W_MetasequoiaObject) {
         return ((W_MetasequoiaObject)model).getFaceNum();
      }

      return 0;
   }

   private static void renderModelRange(
           IModelCustom model,
           int startFace,
           int endFace
   ) {
      if(model instanceof W_WavefrontObject) {
         ((W_WavefrontObject)model).renderAll(startFace, endFace);
      } else if(model instanceof W_MetasequoiaObject) {
         ((W_MetasequoiaObject)model).renderAll(startFace, endFace);
      }
   }

   private static MCH_BaseVehicleInfo getInfo(ItemStack item) {
      return item != null
              && item.getItem() instanceof MCH_ItemBaseVehicle
              ? ((MCH_ItemBaseVehicle)item.getItem()).getAircraftInfo()
              : null;
   }

   private static boolean is3DIconEnabled(MCH_BaseVehicleInfo info) {
      return info.enable3DItemIcon
              && (
              MCH_Config.Override3DItemIcon == null
                      || !MCH_Config.Override3DItemIcon.prmBool
      );
   }

   private static void transform(
           ItemRenderType type,
           MCH_BaseVehicleInfo info
   ) {
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

      float largest = Math.max(
              Math.max(info.bodyWidth, info.bodyHeight),
              1.0F
      );

      float scale = type == ItemRenderType.INVENTORY
              ? 1.35F / largest
              : 0.75F / largest;

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

      if("vehicles".equalsIgnoreCase(directory)
              || "turrets".equalsIgnoreCase(directory)) {

         return getScale(MCH_Config.Turret3DItemIconScale);
      }

      return 1.0F;
   }

   private static float getScale(MCH_ConfigPrm prm) {
      return prm != null
              ? Math.max((float)prm.prmDouble, 0.01F)
              : 1.0F;
   }
}
