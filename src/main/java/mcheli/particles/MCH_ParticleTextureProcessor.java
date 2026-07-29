package mcheli.particles;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Pure image conversion for the smoke atlas.  This class deliberately has no Minecraft or GL dependencies. */
public final class MCH_ParticleTextureProcessor {
   public static final int FRAME_COUNT = 8;
   private static final int MIN_FRAME_SIZE = 32;
   private static final int MAX_FRAME_SIZE = 128;
   private static final int PADDING = 1;

   private MCH_ParticleTextureProcessor() {}

   public static Result readAndProcess(InputStream stream) throws IOException {
      if(stream == null) {
         throw new IOException("resource stream is missing");
      }
      BufferedImage source = ImageIO.read(stream);
      if(source == null) {
         throw new IOException("resource is not a supported image");
      }
      return process(source);
   }

   public static Result process(BufferedImage source) {
      if(source == null) {
         throw new IllegalArgumentException("source image is missing");
      }
      if(source.getWidth() % FRAME_COUNT != 0) {
         throw new IllegalArgumentException("source width " + source.getWidth() + " is not divisible by eight");
      }
      int frameWidth = source.getWidth() / FRAME_COUNT;
      int frameHeight = source.getHeight();
      if(frameWidth <= 0 || frameHeight <= 0) {
         throw new IllegalArgumentException("source has an empty animation frame");
      }

      boolean usefulAlpha = false;
      for(int y = 0; y < source.getHeight() && !usefulAlpha; ++y) {
         for(int x = 0; x < source.getWidth(); ++x) {
            int alpha = source.getRGB(x, y) >>> 24;
            if(alpha > 0 && alpha < 255) {
               usefulAlpha = true;
               break;
            }
         }
      }
      if(frameWidth >= MIN_FRAME_SIZE && frameHeight >= MIN_FRAME_SIZE && usefulAlpha) {
         return new Result(source, false, frameWidth, frameHeight, 0, source.getWidth(), source.getHeight());
      }

      int contentWidth = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameWidth));
      int contentHeight = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameHeight));
      int cellWidth = contentWidth + PADDING * 2;
      BufferedImage atlas = new BufferedImage(cellWidth * FRAME_COUNT, contentHeight + PADDING * 2,
         BufferedImage.TYPE_INT_ARGB);
      for(int frame = 0; frame < FRAME_COUNT; ++frame) {
         BufferedImage scaled = new BufferedImage(contentWidth, contentHeight, BufferedImage.TYPE_INT_ARGB);
         Graphics2D graphics = scaled.createGraphics();
         graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         graphics.drawImage(source, 0, 0, contentWidth, contentHeight,
            frame * frameWidth, 0, (frame + 1) * frameWidth, frameHeight, null);
         graphics.dispose();
         int[] alpha = rebuildAlpha(scaled);
         blurAlpha(alpha, contentWidth, contentHeight);
         for(int y = 0; y < contentHeight; ++y) {
            for(int x = 0; x < contentWidth; ++x) {
               atlas.setRGB(frame * cellWidth + PADDING + x, PADDING + y,
                  alpha[y * contentWidth + x] << 24 | 0x00FFFFFF);
            }
         }
      }
      return new Result(atlas, true, cellWidth, contentHeight + PADDING * 2, PADDING,
         source.getWidth(), source.getHeight());
   }

   private static int[] rebuildAlpha(BufferedImage image) {
      int width = image.getWidth();
      int height = image.getHeight();
      int[] result = new int[width * height];
      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int argb = image.getRGB(x, y);
            int originalAlpha = argb >>> 24;
            if(originalAlpha != 0) {
               int red = argb >> 16 & 255;
               int green = argb >> 8 & 255;
               int blue = argb & 255;
               int luminance = (red * 54 + green * 183 + blue * 19) >> 8;
               // Crunching commonly leaves binary alpha but useful coverage in the grey smoke shape.
               result[y * width + x] = originalAlpha * Math.max(24, luminance) / 255;
            }
         }
      }
      return result;
   }

   private static void blurAlpha(int[] alpha, int width, int height) {
      int[] horizontal = new int[alpha.length];
      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int left = alpha[y * width + Math.max(0, x - 1)];
            int center = alpha[y * width + x];
            int right = alpha[y * width + Math.min(width - 1, x + 1)];
            horizontal[y * width + x] = (left + center * 2 + right) / 4;
         }
      }
      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int original = alpha[y * width + x];
            if(original == 0) {
               // Never expand the silhouette into pixels which were fully transparent after scaling.
               alpha[y * width + x] = 0;
            } else {
               int top = horizontal[Math.max(0, y - 1) * width + x];
               int center = horizontal[y * width + x];
               int bottom = horizontal[Math.min(height - 1, y + 1) * width + x];
               alpha[y * width + x] = (top + center * 2 + bottom) / 4;
            }
         }
      }
   }

   public static final class Result {
      public final BufferedImage image;
      public final boolean processed;
      public final int cellWidth;
      public final int cellHeight;
      public final int padding;
      public final int sourceWidth;
      public final int sourceHeight;

      private Result(BufferedImage image, boolean processed, int cellWidth, int cellHeight, int padding,
         int sourceWidth, int sourceHeight) {
         this.image = image;
         this.processed = processed;
         this.cellWidth = cellWidth;
         this.cellHeight = cellHeight;
         this.padding = padding;
         this.sourceWidth = sourceWidth;
         this.sourceHeight = sourceHeight;
      }
   }
}
