package mcheli.particles;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Pure image conversion for the smoke atlas. This class deliberately has no Minecraft or GL dependencies. */
public final class MCH_ParticleTextureProcessor {
   public static final int FRAME_COUNT = 8;
   private static final int MIN_FRAME_SIZE = 32;
   private static final int MAX_FRAME_SIZE = 128;
   /** Two texels keep a filtered frame sample isolated from the next frame. */
   private static final int PADDING = 2;
   /** Values at or below 4/255 are reconstruction haze, rather than visible smoke coverage. */
   private static final int ALPHA_CUTOFF = 4;

   private MCH_ParticleTextureProcessor() {}

   public static Result readAndProcess(InputStream stream) throws IOException {
      if(stream == null) throw new IOException("resource stream is missing");
      BufferedImage source = ImageIO.read(stream);
      if(source == null) throw new IOException("resource is not a supported image");
      return process(source);
   }

   public static Result process(BufferedImage source) {
      if(source == null) throw new IllegalArgumentException("source image is missing");
      if(source.getWidth() % FRAME_COUNT != 0) {
         throw new IllegalArgumentException("source width " + source.getWidth() + " is not divisible by eight");
      }
      int frameWidth = source.getWidth() / FRAME_COUNT;
      int frameHeight = source.getHeight();
      if(frameWidth <= 0 || frameHeight <= 0) {
         throw new IllegalArgumentException("source has an empty animation frame");
      }

      boolean usefulAlpha = hasIntermediateAlpha(source);
      if(frameWidth >= MIN_FRAME_SIZE && frameHeight >= MIN_FRAME_SIZE && usefulAlpha) {
         return new Result(source, false, frameWidth, frameHeight, 0, source.getWidth(), source.getHeight());
      }

      int contentWidth = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameWidth));
      int contentHeight = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameHeight));
      int cellWidth = contentWidth + PADDING * 2;
      int cellHeight = contentHeight + PADDING * 2;
      BufferedImage atlas = new BufferedImage(cellWidth * FRAME_COUNT, cellHeight, BufferedImage.TYPE_INT_ARGB);

      for(int frame = 0; frame < FRAME_COUNT; ++frame) {
         int[] sourceCoverage = extractCoverage(source, frame * frameWidth, frameWidth, frameHeight);
         int[] coverage = scaleBilinear(sourceCoverage, frameWidth, frameHeight, contentWidth, contentHeight);
         blurInsideSilhouette(coverage, contentWidth, contentHeight);
         finishCoverage(coverage, contentWidth, contentHeight);
         for(int y = 0; y < contentHeight; ++y) {
            for(int x = 0; x < contentWidth; ++x) {
               int alpha = coverage[y * contentWidth + x];
               int argb = alpha == 0 ? 0x00000000 : alpha << 24 | 0x00FFFFFF;
               atlas.setRGB(frame * cellWidth + PADDING + x, PADDING + y, argb);
            }
         }
      }
      return new Result(atlas, true, cellWidth, cellHeight, PADDING, source.getWidth(), source.getHeight());
   }

   private static boolean hasIntermediateAlpha(BufferedImage image) {
      for(int y = 0; y < image.getHeight(); ++y) {
         for(int x = 0; x < image.getWidth(); ++x) {
            int alpha = image.getRGB(x, y) >>> 24;
            if(alpha > 0 && alpha < 255) return true;
         }
      }
      return false;
   }

   private static int[] extractCoverage(BufferedImage source, int startX, int width, int height) {
      int[] coverage = new int[width * height];
      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int argb = source.getRGB(startX + x, y);
            int alpha = argb >>> 24;
            if(alpha == 0) continue;
            // Alpha is coverage. In particular, binary-alpha crunches must not turn incidental RGB into haze.
            coverage[y * width + x] = alpha;
         }
      }
      return coverage;
   }

   private static int[] scaleBilinear(int[] source, int sourceWidth, int sourceHeight,
      int width, int height) {
      int[] scaled = new int[width * height];
      for(int y = 0; y < height; ++y) {
         float sourceY = ((y + 0.5F) * sourceHeight / height) - 0.5F;
         int y0 = Math.max(0, (int)Math.floor(sourceY));
         int y1 = Math.min(sourceHeight - 1, y0 + 1);
         float fy = Math.max(0.0F, sourceY - y0);
         for(int x = 0; x < width; ++x) {
            float sourceX = ((x + 0.5F) * sourceWidth / width) - 0.5F;
            int x0 = Math.max(0, (int)Math.floor(sourceX));
            int x1 = Math.min(sourceWidth - 1, x0 + 1);
            float fx = Math.max(0.0F, sourceX - x0);
            float top = source[y0 * sourceWidth + x0] * (1.0F - fx) + source[y0 * sourceWidth + x1] * fx;
            float bottom = source[y1 * sourceWidth + x0] * (1.0F - fx) + source[y1 * sourceWidth + x1] * fx;
            scaled[y * width + x] = clamp(Math.round(top * (1.0F - fy) + bottom * fy));
         }
      }
      return scaled;
   }

   private static void blurInsideSilhouette(int[] alpha, int width, int height) {
      int[] original = alpha.clone();
      for(int y = 1; y < height - 1; ++y) {
         for(int x = 1; x < width - 1; ++x) {
            int index = y * width + x;
            if(original[index] == 0) continue;
            int sum = 0;
            for(int offsetY = -1; offsetY <= 1; ++offsetY) {
               for(int offsetX = -1; offsetX <= 1; ++offsetX) {
                  int weight = offsetX == 0 && offsetY == 0 ? 4 :
                     (offsetX == 0 || offsetY == 0 ? 2 : 1);
                  sum += original[index + offsetY * width + offsetX] * weight;
               }
            }
            alpha[index] = clamp((sum + 8) / 16);
         }
      }
   }

   private static void finishCoverage(int[] alpha, int width, int height) {
      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int index = y * width + x;
            if(x == 0 || y == 0 || x == width - 1 || y == height - 1 || alpha[index] <= ALPHA_CUTOFF) {
               alpha[index] = 0;
            } else {
               alpha[index] = clamp((alpha[index] - ALPHA_CUTOFF) * 255 / (255 - ALPHA_CUTOFF));
            }
         }
      }
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(255, value));
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
