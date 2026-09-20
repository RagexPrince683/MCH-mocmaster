package mcheli.aircraft;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import mcheli.MCH_Config;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

/**
 * Vehicle item renderer. Inventory views use persistent, face-count-independent
 * PNG snapshots; equipped and entity views retain the live model.
 */
public class MCH_VehicleItemModelRender implements IItemRenderer, IResourceManagerReloadListener {

   private static final int CACHE_SCHEMA = 1;
   private static final long VISIBLE_REQUEST_LIFETIME_MS = 750L;
   private static final Map SNAPSHOT_REQUESTS = new LinkedHashMap();
   private static final LinkedHashMap LOADED_SNAPSHOTS = new LinkedHashMap(16, 0.75F, true);
   private static long nextGenerationTime;
   private static long completedSnapshots;
   private static long failedSnapshots;
   private static long totalGenerationTime;
   private static long worstGenerationTime;
   private static long previousFrameTime;
   private static double recentFramesPerSecond = 60.0D;
   private static long cacheHits;
   private static long cacheMisses;
   private static long lastDiagnosticTime;

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      MCH_BaseVehicleInfo info = getInfo(item);
      if(info == null || !is3DIconEnabled(info)) {
         return false;
      }

      if(type == ItemRenderType.INVENTORY) {
         if(!snapshotsEnabled() || !OpenGlHelper.isFramebufferEnabled()) {
            return false;
         }
         SnapshotTexture snapshot = getSnapshot(info);
         if(snapshot != null) {
            return true;
         }
         requestSnapshot(info);
         return false;
      }

      mcheli.MCH_ClientProxy.ensureVehicleModel(info);
      return info.model != null;
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      return true;
   }

   public void onResourceManagerReload(IResourceManager resourceManager) {
      resetForReload();
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
      MCH_BaseVehicleInfo info = getInfo(item);
      if(info == null || !is3DIconEnabled(info)) {
         return;
      }

      if(type == ItemRenderType.INVENTORY) {
         SnapshotTexture snapshot = getSnapshot(info);
         if(snapshot != null) {
            renderSnapshot(snapshot.location);
         }
         return;
      }

      mcheli.MCH_ClientProxy.ensureVehicleModel(info);
      if(info.model == null) {
         return;
      }
      renderLiveModel(type, info);
   }

   /** Runs once from RenderTickEvent.END, never from an item-render callback. */
   public static void onRenderFrame() {
      long now = Minecraft.getSystemTime();
      if(previousFrameTime > 0L && now > previousFrameTime) {
         double instantaneousFps = 1000.0D / (double)(now - previousFrameTime);
         recentFramesPerSecond = recentFramesPerSecond * 0.9D + instantaneousFps * 0.1D;
      }
      previousFrameTime = now;
      removeStaleRequests(now);
      reportDiagnostics(now);
      if(!automaticGenerationEnabled() || SNAPSHOT_REQUESTS.isEmpty() || now < nextGenerationTime) {
         return;
      }
      if(recentFramesPerSecond < minimumGenerationFps()) {
         return;
      }

      SnapshotRequest request = newestVisibleRequest(now);
      if(request == null) {
         return;
      }
      SNAPSHOT_REQUESTS.remove(request.cacheKey);
      nextGenerationTime = now + generationIntervalMs();
      generateSnapshot(request);
   }

   /** Releases all owned GL resources and pending work on reload or shutdown. */
   public static void resetForReload() {
      TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
      for(Object value : LOADED_SNAPSHOTS.values()) {
         textureManager.deleteTexture(((SnapshotTexture)value).location);
      }
      LOADED_SNAPSHOTS.clear();
      SNAPSHOT_REQUESTS.clear();
      nextGenerationTime = 0L;
      previousFrameTime = 0L;
      recentFramesPerSecond = 60.0D;
   }

   /** Invalidates only the changed vehicle; unrelated persistent PNGs remain. */
   public static void invalidate(MCH_BaseVehicleInfo info) {
      if(info == null) {
         return;
      }
      String prefix = safeName(info.getDirectoryName()) + "-" + safeName(info.name) + "-";
      Iterator loadedIterator = LOADED_SNAPSHOTS.entrySet().iterator();
      while(loadedIterator.hasNext()) {
         Map.Entry entry = (Map.Entry)loadedIterator.next();
         if(((String)entry.getKey()).startsWith(prefix)) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(((SnapshotTexture)entry.getValue()).location);
            loadedIterator.remove();
         }
      }
      Iterator requestIterator = SNAPSHOT_REQUESTS.keySet().iterator();
      while(requestIterator.hasNext()) {
         if(((String)requestIterator.next()).startsWith(prefix)) {
            requestIterator.remove();
         }
      }
      File[] files = cacheDirectory().listFiles();
      if(files != null) {
         for(File file : files) {
            if(file.getName().startsWith(prefix)) {
               file.delete();
            }
         }
      }
   }

   /** Client action used by reload tooling and configuration screens. */
   public static void clearSnapshotCache() {
      resetForReload();
      File[] files = cacheDirectory().listFiles();
      if(files != null) {
         for(File file : files) {
            if(file.getName().endsWith(".png") || file.getName().endsWith(".tmp")) {
               file.delete();
            }
         }
      }
   }

   private static SnapshotTexture getSnapshot(MCH_BaseVehicleInfo info) {
      String cacheKey = cacheKey(info);
      SnapshotTexture loaded = (SnapshotTexture)LOADED_SNAPSHOTS.get(cacheKey);
      if(loaded != null) {
         ++cacheHits;
         return loaded;
      }

      File png = new File(cacheDirectory(), cacheKey + ".png");
      if(!png.isFile()) {
         ++cacheMisses;
         return null;
      }
      try {
         BufferedImage image = ImageIO.read(png);
         if(image == null || image.getWidth() != snapshotResolution() || image.getHeight() != snapshotResolution()) {
            png.delete();
            return null;
         }
         DynamicTexture texture = new DynamicTexture(image);
         ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                 .getDynamicTextureLocation("mcheli_vehicle_snapshot", texture);
         loaded = new SnapshotTexture(location);
         LOADED_SNAPSHOTS.put(cacheKey, loaded);
         evictLoadedTextures();
         diagnostic("loaded snapshot textures: %d", LOADED_SNAPSHOTS.size());
         return loaded;
      } catch(IOException failure) {
         png.delete();
         logFailure(info, "PNG load", failure);
         return null;
      }
   }

   private static void requestSnapshot(MCH_BaseVehicleInfo info) {
      if(!automaticGenerationEnabled()) {
         return;
      }
      String cacheKey = cacheKey(info);
      SnapshotRequest request = (SnapshotRequest)SNAPSHOT_REQUESTS.get(cacheKey);
      if(request == null) {
         request = new SnapshotRequest(info, cacheKey);
         SNAPSHOT_REQUESTS.put(cacheKey, request);
         diagnostic("queued snapshot: %s", info.name);
      }
      request.lastVisibleTime = Minecraft.getSystemTime();
   }

   private static void generateSnapshot(SnapshotRequest request) {
      long started = Minecraft.getSystemTime();
      MCH_BaseVehicleInfo info = request.info;
      File temporary = new File(cacheDirectory(), request.cacheKey + ".tmp");
      File destination = new File(cacheDirectory(), request.cacheKey + ".png");
      try {
         // A parser is monolithic and cannot be interrupted by the scheduler budget.
         mcheli.MCH_ClientProxy.ensureVehicleModel(info);
         if(info.model == null) {
            throw new IOException("model loader returned no body model");
         }
         BufferedImage immutablePixels = renderSnapshotImage(info);
         if(!ImageIO.write(immutablePixels, "png", temporary)) {
            throw new IOException("no PNG encoder is available");
         }
         Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
         ++completedSnapshots;
         diagnostic("completed snapshot: %s", info.name);
      } catch(Exception failure) {
         ++failedSnapshots;
         temporary.delete();
         logFailure(info, "generation", failure);
      } finally {
         long duration = Minecraft.getSystemTime() - started;
         totalGenerationTime += duration;
         worstGenerationTime = Math.max(worstGenerationTime, duration);
         if(diagnosticsEnabled()) {
            long average = totalGenerationTime / Math.max(1L, completedSnapshots + failedSnapshots);
            MCH_Lib.Log("Vehicle snapshot timing: average=%dms worst=%dms", average, worstGenerationTime);
         }
      }
   }

   private static BufferedImage renderSnapshotImage(MCH_BaseVehicleInfo info) throws IOException {
      int resolution = snapshotResolution();
      int previousFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
      int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      Framebuffer framebuffer = new Framebuffer(resolution, resolution, true);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPushMatrix();
      try {
         framebuffer.bindFramebuffer(true);
         GL11.glViewport(0, 0, resolution, resolution);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
         GL11.glMatrixMode(GL11.GL_PROJECTION);
         GL11.glLoadIdentity();
         GL11.glOrtho(-1.0D, 1.0D, -1.0D, 1.0D, -1000.0D, 1000.0D);
         GL11.glMatrixMode(GL11.GL_MODELVIEW);
         GL11.glLoadIdentity();
         GL11.glEnable(GL11.GL_DEPTH_TEST);
         GL11.glEnable(GL11.GL_TEXTURE_2D);
         GL11.glEnable(GL11.GL_ALPHA_TEST);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         renderLiveModel(ItemRenderType.INVENTORY, info);

         ByteBuffer pixels = BufferUtils.createByteBuffer(resolution * resolution * 4);
         GL11.glReadPixels(0, 0, resolution, resolution, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
         BufferedImage image = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
         for(int y = 0; y < resolution; ++y) {
            for(int x = 0; x < resolution; ++x) {
               int offset = (x + y * resolution) * 4;
               int red = pixels.get(offset) & 255;
               int green = pixels.get(offset + 1) & 255;
               int blue = pixels.get(offset + 2) & 255;
               int alpha = pixels.get(offset + 3) & 255;
               image.setRGB(x, resolution - y - 1, alpha << 24 | red << 16 | green << 8 | blue);
            }
         }
         return image;
      } finally {
         GL11.glMatrixMode(GL11.GL_MODELVIEW);
         GL11.glPopMatrix();
         GL11.glMatrixMode(GL11.GL_PROJECTION);
         GL11.glPopMatrix();
         GL11.glMatrixMode(GL11.GL_MODELVIEW);
         GL11.glPopAttrib();
         EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, previousFramebuffer);
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
         framebuffer.deleteFramebuffer();
      }
   }

   private static void renderLiveModel(ItemRenderType type, MCH_BaseVehicleInfo info) {
      GL11.glPushMatrix();
      try {
         transform(type, info);
         ResourceLocation original = new ResourceLocation("mcheli", "textures/" + info.getDirectoryName()
                 + "/" + MCH_RenderBaseVehicle.getBaseTextureName(info.name) + ".png");
         Minecraft.getMinecraft().getTextureManager().bindTexture(
                 mcheli.texture.MCH_ModelTextureRepairManager.resolve(original, info.model,
                         info.getDirectoryName() + "/" + info.name));
         MCH_RenderBaseVehicle.beginSkinOverlayRender(info.getDirectoryName(), info.name);
         try {
            info.model.renderAll();
         } finally {
            MCH_RenderBaseVehicle.endSkinOverlayRender();
         }
      } finally {
         GL11.glPopMatrix();
      }
   }

   private static void renderSnapshot(ResourceLocation location) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(location);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      Tessellator tessellator = Tessellator.instance;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV(-0.5D, -0.5D, 0.0D, 0.0D, 1.0D);
      tessellator.addVertexWithUV(0.5D, -0.5D, 0.0D, 1.0D, 1.0D);
      tessellator.addVertexWithUV(0.5D, 0.5D, 0.0D, 1.0D, 0.0D);
      tessellator.addVertexWithUV(-0.5D, 0.5D, 0.0D, 0.0D, 0.0D);
      tessellator.draw();
   }

   private static void removeStaleRequests(long now) {
      Iterator iterator = SNAPSHOT_REQUESTS.entrySet().iterator();
      while(iterator.hasNext()) {
         SnapshotRequest request = (SnapshotRequest)((Map.Entry)iterator.next()).getValue();
         if(now - request.lastVisibleTime > VISIBLE_REQUEST_LIFETIME_MS) {
            iterator.remove();
            diagnostic("stale snapshot removed: %s", request.info.name);
         }
      }
   }

   private static SnapshotRequest newestVisibleRequest(long now) {
      SnapshotRequest newest = null;
      for(Object value : SNAPSHOT_REQUESTS.values()) {
         SnapshotRequest request = (SnapshotRequest)value;
         if(now - request.lastVisibleTime <= VISIBLE_REQUEST_LIFETIME_MS
                 && (newest == null || request.lastVisibleTime > newest.lastVisibleTime)) {
            newest = request;
         }
      }
      return newest;
   }

   private static void evictLoadedTextures() {
      int limit = Math.max(1, MCH_Config.VehicleSnapshotTextureLimit.prmInt);
      Iterator iterator = LOADED_SNAPSHOTS.entrySet().iterator();
      while(LOADED_SNAPSHOTS.size() > limit && iterator.hasNext()) {
         SnapshotTexture snapshot = (SnapshotTexture)((Map.Entry)iterator.next()).getValue();
         Minecraft.getMinecraft().getTextureManager().deleteTexture(snapshot.location);
         iterator.remove();
         diagnostic("snapshot texture eviction");
      }
   }

   private static String cacheKey(MCH_BaseVehicleInfo info) {
      String identity = CACHE_SCHEMA + "|" + info.getDirectoryName() + "|" + info.name
              + "|model:" + info.getDirectoryName() + "/" + info.name
              + "|texture:" + MCH_RenderBaseVehicle.getBaseTextureName(info.name)
              + "|itemScale:" + info.itemIconScaleFactor + "|typeScale:" + getTypeScale(info)
              + "|resolution:" + snapshotResolution() + "|rotation:30,45";
      return safeName(info.getDirectoryName()) + "-" + safeName(info.name) + "-" + sha256(identity);
   }

   private static String sha256(String value) {
      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
         StringBuilder text = new StringBuilder();
         for(int index = 0; index < 12; ++index) {
            text.append(String.format("%02x", digest[index] & 255));
         }
         return text.toString();
      } catch(Exception impossible) {
         throw new IllegalStateException(impossible);
      }
   }

   private static String safeName(String value) {
      return value.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
   }

   private static File cacheDirectory() {
      File directory = new File(Minecraft.getMinecraft().mcDataDir, "mcheli/cache/vehicle-icons-v" + CACHE_SCHEMA);
      if(!directory.isDirectory()) {
         directory.mkdirs();
      }
      return directory;
   }

   private static MCH_BaseVehicleInfo getInfo(ItemStack item) {
      return item != null && item.getItem() instanceof MCH_ItemBaseVehicle
              ? ((MCH_ItemBaseVehicle)item.getItem()).getAircraftInfo() : null;
   }

   private static boolean is3DIconEnabled(MCH_BaseVehicleInfo info) {
      return info.enable3DItemIcon && (MCH_Config.Override3DItemIcon == null
              || !MCH_Config.Override3DItemIcon.prmBool);
   }

   private static boolean snapshotsEnabled() {
      return MCH_Config.EnableVehicleInventorySnapshots != null
              && MCH_Config.EnableVehicleInventorySnapshots.prmBool;
   }

   private static boolean automaticGenerationEnabled() {
      return snapshotsEnabled() && MCH_Config.AutoGenerateVehicleInventorySnapshots.prmBool;
   }

   private static int snapshotResolution() {
      return Math.max(32, Math.min(512, MCH_Config.VehicleSnapshotResolution.prmInt));
   }

   private static int minimumGenerationFps() {
      return Math.max(1, MCH_Config.VehicleSnapshotMinimumFps.prmInt);
   }

   private static long generationIntervalMs() {
      return Math.max(250, MCH_Config.VehicleSnapshotGenerationInterval.prmInt);
   }

   private static boolean diagnosticsEnabled() {
      return MCH_Config.DebugVehicleInventorySnapshots != null
              && MCH_Config.DebugVehicleInventorySnapshots.prmBool;
   }

   private static void diagnostic(String format, Object... values) {
      if(diagnosticsEnabled()) {
         MCH_Lib.Log(format, values);
      }
   }

   private static void reportDiagnostics(long now) {
      if(!diagnosticsEnabled() || now - lastDiagnosticTime < 5000L) {
         return;
      }
      lastDiagnosticTime = now;
      long generations = completedSnapshots + failedSnapshots;
      long average = totalGenerationTime / Math.max(1L, generations);
      MCH_Lib.Log("Vehicle snapshot diagnostics: hits=%d misses=%d queued=%d completed=%d failures=%d loaded=%d average=%dms worst=%dms",
              cacheHits, cacheMisses, SNAPSHOT_REQUESTS.size(), completedSnapshots, failedSnapshots,
              LOADED_SNAPSHOTS.size(), average, worstGenerationTime);
   }

   private static void logFailure(MCH_BaseVehicleInfo info, String stage, Exception failure) {
      MCH_Lib.Log("Vehicle snapshot failure: vehicle=%s stage=%s error=%s", info.name, stage, failure.getMessage());
   }

   private static void transform(ItemRenderType type, MCH_BaseVehicleInfo info) {
      if(type == ItemRenderType.ENTITY) {
         GL11.glTranslatef(0.0F, 0.35F, 0.0F);
         GL11.glRotatef(35.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
      } else if(type == ItemRenderType.EQUIPPED) {
         GL11.glTranslatef(0.25F, 0.45F, 0.55F);
         GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
      } else if(type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
         GL11.glTranslatef(0.65F, 0.35F, 0.35F);
         GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(125.0F, 0.0F, 1.0F, 0.0F);
      } else {
         GL11.glTranslatef(0.0F, -0.35F, 0.0F);
         GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
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
      if("helicopters".equalsIgnoreCase(directory)) return (float)MCH_Config.Heli3DItemIconScale.prmDouble;
      if("planes".equalsIgnoreCase(directory)) return (float)MCH_Config.Plane3DItemIconScale.prmDouble;
      if("ships".equalsIgnoreCase(directory)) return (float)MCH_Config.Ship3DItemIconScale.prmDouble;
      if("tanks".equalsIgnoreCase(directory)) return (float)MCH_Config.Tank3DItemIconScale.prmDouble;
      return (float)MCH_Config.Turret3DItemIconScale.prmDouble;
   }

   private static final class SnapshotRequest {
      private final MCH_BaseVehicleInfo info;
      private final String cacheKey;
      private long lastVisibleTime;

      private SnapshotRequest(MCH_BaseVehicleInfo info, String cacheKey) {
         this.info = info;
         this.cacheKey = cacheKey;
      }
   }

   private static final class SnapshotTexture {
      private final ResourceLocation location;

      private SnapshotTexture(ResourceLocation location) {
         this.location = location;
      }
   }
}
