package mcheli;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public final class MCH_SkinOverlayTextureManager {
   private static final Map CACHE = new HashMap();

   private MCH_SkinOverlayTextureManager() {}

   public static ResourceLocation getOrCreate(String basePath, String overlayPath) throws IOException {
      String key = basePath + "|" + overlayPath;
      ResourceLocation cached = (ResourceLocation)CACHE.get(key);
      if(cached != null) {
         return cached;
      }

      Minecraft mc = Minecraft.getMinecraft();
      BufferedImage base = readImage(mc, new ResourceLocation("mcheli", basePath));
      BufferedImage overlay = readImage(mc, new ResourceLocation("mcheli", overlayPath));
      BufferedImage combined = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = combined.createGraphics();
      try {
         graphics.drawImage(base, 0, 0, null);
         graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
         graphics.drawImage(overlay, 0, 0, base.getWidth(), base.getHeight(), null);
      } finally {
         graphics.dispose();
      }

      TextureManager textureManager = mc.getTextureManager();
      ResourceLocation location = textureManager.getDynamicTextureLocation("mcheli_skin_overlay", new DynamicTexture(combined));
      CACHE.put(key, location);
      return location;
   }

   private static BufferedImage readImage(Minecraft mc, ResourceLocation location) throws IOException {
      IResource resource = mc.getResourceManager().getResource(location);
      InputStream stream = resource.getInputStream();
      try {
         BufferedImage image = ImageIO.read(stream);
         if(image == null) {
            throw new IOException("Unsupported image format: " + location);
         }
         return image;
      } finally {
         stream.close();
      }
   }
}
