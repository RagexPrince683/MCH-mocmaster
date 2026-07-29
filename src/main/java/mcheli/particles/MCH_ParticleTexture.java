package mcheli.particles;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import mcheli.MCH_Lib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public final class MCH_ParticleTexture implements IResourceManagerReloadListener {
   private static final ResourceLocation SOURCE = new ResourceLocation("mcheli", "textures/particles/smoke.png");
   private static final MCH_ParticleTexture INSTANCE = new MCH_ParticleTexture();
   private ResourceLocation texture = SOURCE;
   private MCH_ParticleTextureProcessor.Result result;
   private boolean loaded;
   private boolean fallbackLogged;

   private MCH_ParticleTexture() {}

   public static void register() {
      IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
      if(manager instanceof IReloadableResourceManager) {
         ((IReloadableResourceManager)manager).registerReloadListener(INSTANCE);
      }
   }

   public static void bind() {
      INSTANCE.ensureLoaded();
      Minecraft.getMinecraft().getTextureManager().bindTexture(INSTANCE.texture);
   }

   public static float minU(int frame) {
      INSTANCE.ensureLoaded();
      if(INSTANCE.result == null) return frame / 8.0F;
      if(!INSTANCE.result.processed) {
         return (frame + 0.5F / INSTANCE.result.cellWidth) / 8.0F;
      }
      return (frame * INSTANCE.result.cellWidth + INSTANCE.result.padding + 0.5F) / INSTANCE.result.image.getWidth();
   }

   public static float maxU(int frame) {
      INSTANCE.ensureLoaded();
      if(INSTANCE.result == null) return (frame + 1) / 8.0F;
      if(!INSTANCE.result.processed) {
         return (frame + 1.0F - 0.5F / INSTANCE.result.cellWidth) / 8.0F;
      }
      return ((frame + 1) * INSTANCE.result.cellWidth - INSTANCE.result.padding - 0.5F) / INSTANCE.result.image.getWidth();
   }

   public static float minV() {
      INSTANCE.ensureLoaded();
      if(INSTANCE.result == null) return 0.0F;
      return INSTANCE.result.processed
         ? (INSTANCE.result.padding + 0.5F) / INSTANCE.result.image.getHeight() : 0.5F / INSTANCE.result.cellHeight;
   }

   public static float maxV() {
      INSTANCE.ensureLoaded();
      if(INSTANCE.result == null) return 1.0F;
      return INSTANCE.result.processed
         ? (INSTANCE.result.image.getHeight() - INSTANCE.result.padding - 0.5F) / INSTANCE.result.image.getHeight()
         : 1.0F - 0.5F / INSTANCE.result.cellHeight;
   }

   private void ensureLoaded() {
      if(loaded) return;
      loaded = true;
      int width = -1;
      int height = -1;
      InputStream stream = null;
      try {
         IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(SOURCE);
         stream = resource.getInputStream();
         result = MCH_ParticleTextureProcessor.readAndProcess(stream);
         width = result.sourceWidth;
         height = result.sourceHeight;
         if(result.processed) {
            TextureManager manager = Minecraft.getMinecraft().getTextureManager();
            texture = manager.getDynamicTextureLocation("mcheli_soft_smoke", new LinearDynamicTexture(result.image));
         } else {
            texture = SOURCE;
         }
         MCH_Lib.Log("Particle smoke texture %s: source/result %dx%d, atlas %dx%d.",
            result.processed ? "processed" : "used directly", width, height, result.image.getWidth(), result.image.getHeight());
      } catch(Exception error) {
         texture = SOURCE;
         result = null;
         if(!fallbackLogged) {
            fallbackLogged = true;
            MCH_Lib.Log("Particle smoke texture fallback (source %dx%d): %s", width, height, error.toString());
         }
      } finally {
         if(stream != null) try { stream.close(); } catch(Exception ignored) {}
      }
   }

   public void onResourceManagerReload(IResourceManager resourceManager) {
      if(texture != null && !SOURCE.equals(texture)) {
         Minecraft.getMinecraft().getTextureManager().deleteTexture(texture);
      }
      texture = SOURCE;
      result = null;
      loaded = false;
   }

   private static final class LinearDynamicTexture extends DynamicTexture {
      private final BufferedImage image;
      private LinearDynamicTexture(BufferedImage image) {
         super(image);
         this.image = image;
      }
      public void loadTexture(IResourceManager manager) {
         TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), image, true, false);
      }
   }
}
