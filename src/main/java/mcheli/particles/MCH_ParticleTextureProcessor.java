package mcheli.particles;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Pure image conversion for the smoke atlas. This class deliberately has no Minecraft or GL dependencies. */
public final class MCH_ParticleTextureProcessor {
   public static final int FRAME_COUNT = 8;
   public static final int BLUR_RADIUS = 2;
   public static final double BLUR_SIGMA = 1.0D;
   public static final double GAMMA = 0.8D;
   public static final String BINARY_DENSITY = "binary-density reconstruction";
   public static final String DIRECT_USE = "direct use";
   private static final int MIN_FRAME_SIZE = 32;
   private static final int MAX_FRAME_SIZE = 128;
   private static final int SOURCE_PADDING = 2;
   private static final int OUTPUT_PADDING = 2;

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
      if(frameWidth <= 0 || frameHeight <= 0) throw new IllegalArgumentException("source has an empty animation frame");

      if(hasIntermediateAlpha(source)) {
         return new Result(source, false, DIRECT_USE, frameWidth, frameHeight, 0,
            source.getWidth(), source.getHeight(), 0.0D);
      }

      int contentWidth = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameWidth));
      int contentHeight = Math.min(MAX_FRAME_SIZE, Math.max(MIN_FRAME_SIZE, frameHeight));
      int cellWidth = contentWidth + OUTPUT_PADDING * 2;
      int cellHeight = contentHeight + OUTPUT_PADDING * 2;
      int fieldWidth = frameWidth + SOURCE_PADDING * 2;
      int fieldHeight = frameHeight + SOURCE_PADDING * 2;
      double[][] fields = new double[FRAME_COUNT][];
      double normalizationReference = 0.0D;

      // Binary alpha is a low-resolution density sampling pattern, not a finished silhouette.
      // Blur radius 2 and sigma 1.0 merge nearby samples without rounding away the irregular outline.
      double[] kernel = gaussianKernel(BLUR_RADIUS, BLUR_SIGMA);
      for(int frame = 0; frame < FRAME_COUNT; ++frame) {
         double[] density = new double[fieldWidth * fieldHeight];
         for(int y = 0; y < frameHeight; ++y) {
            for(int x = 0; x < frameWidth; ++x) {
               if((source.getRGB(frame * frameWidth + x, y) >>> 24) != 0) {
                  density[(y + SOURCE_PADDING) * fieldWidth + x + SOURCE_PADDING] = 1.0D;
               }
            }
         }
         fields[frame] = gaussianBlur(density, fieldWidth, fieldHeight, kernel, BLUR_RADIUS);
         for(double value : fields[frame]) normalizationReference = Math.max(normalizationReference, value);
      }

      BufferedImage atlas = new BufferedImage(cellWidth * FRAME_COUNT, cellHeight, BufferedImage.TYPE_INT_ARGB);
      for(int frame = 0; frame < FRAME_COUNT; ++frame) {
         double[] opacity = new double[fields[frame].length];
         if(normalizationReference > 0.0D) {
            for(int i = 0; i < opacity.length; ++i) {
               opacity[i] = Math.pow(fields[frame][i] / normalizationReference, GAMMA);
            }
         }
         double[] scaled = scaleBilinear(opacity, fieldWidth, fieldHeight, cellWidth, cellHeight);
         for(int y = 0; y < cellHeight; ++y) {
            for(int x = 0; x < cellWidth; ++x) {
               int alpha = clamp((int)Math.round(scaled[y * cellWidth + x] * 255.0D));
               if(isProtectedTransparentBorder(x, y, cellWidth, cellHeight) || alpha <= 1) alpha = 0;
               atlas.setRGB(frame * cellWidth + x, y, alpha == 0 ? 0x00000000 : alpha << 24 | 0x00FFFFFF);
            }
         }
      }
      return new Result(atlas, true, BINARY_DENSITY, cellWidth, cellHeight, OUTPUT_PADDING,
         source.getWidth(), source.getHeight(), normalizationReference);
   }

   private static boolean hasIntermediateAlpha(BufferedImage image) {
      for(int y = 0; y < image.getHeight(); ++y) for(int x = 0; x < image.getWidth(); ++x) {
         int alpha = image.getRGB(x, y) >>> 24;
         if(alpha > 0 && alpha < 255) return true;
      }
      return false;
   }

   private static double[] gaussianKernel(int radius, double sigma) {
      double[] kernel = new double[radius * 2 + 1];
      double sum = 0.0D;
      for(int offset = -radius; offset <= radius; ++offset) {
         double value = Math.exp(-(offset * offset) / (2.0D * sigma * sigma));
         kernel[offset + radius] = value;
         sum += value;
      }
      for(int i = 0; i < kernel.length; ++i) kernel[i] /= sum;
      return kernel;
   }

   private static double[] gaussianBlur(double[] source, int width, int height, double[] kernel, int radius) {
      double[] horizontal = new double[source.length];
      double[] result = new double[source.length];
      for(int y = 0; y < height; ++y) for(int x = 0; x < width; ++x) {
         double sum = 0.0D;
         for(int offset = -radius; offset <= radius; ++offset) {
            int sampleX = x + offset;
            if(sampleX >= 0 && sampleX < width) sum += source[y * width + sampleX] * kernel[offset + radius];
         }
         horizontal[y * width + x] = sum;
      }
      for(int y = 0; y < height; ++y) for(int x = 0; x < width; ++x) {
         double sum = 0.0D;
         for(int offset = -radius; offset <= radius; ++offset) {
            int sampleY = y + offset;
            if(sampleY >= 0 && sampleY < height) sum += horizontal[sampleY * width + x] * kernel[offset + radius];
         }
         result[y * width + x] = sum;
      }
      return result;
   }

   private static double[] scaleBilinear(double[] source, int sourceWidth, int sourceHeight, int width, int height) {
      double[] scaled = new double[width * height];
      for(int y = 0; y < height; ++y) {
         double sourceY = (y + 0.5D) * sourceHeight / height - 0.5D;
         int y0 = (int)Math.floor(sourceY);
         double fy = sourceY - y0;
         for(int x = 0; x < width; ++x) {
            double sourceX = (x + 0.5D) * sourceWidth / width - 0.5D;
            int x0 = (int)Math.floor(sourceX);
            double fx = sourceX - x0;
            double top = sample(source, sourceWidth, sourceHeight, x0, y0) * (1.0D - fx) +
               sample(source, sourceWidth, sourceHeight, x0 + 1, y0) * fx;
            double bottom = sample(source, sourceWidth, sourceHeight, x0, y0 + 1) * (1.0D - fx) +
               sample(source, sourceWidth, sourceHeight, x0 + 1, y0 + 1) * fx;
            scaled[y * width + x] = top * (1.0D - fy) + bottom * fy;
         }
      }
      return scaled;
   }

   private static double sample(double[] source, int width, int height, int x, int y) {
      return x < 0 || x >= width || y < 0 || y >= height ? 0.0D : source[y * width + x];
   }

   private static boolean isProtectedTransparentBorder(int x, int y, int width, int height) {
      return x <= OUTPUT_PADDING || x >= width - OUTPUT_PADDING - 1 ||
         y <= OUTPUT_PADDING || y >= height - OUTPUT_PADDING - 1;
   }

   private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }

   public static final class Result {
      public final BufferedImage image;
      public final boolean processed;
      public final String reconstructionPath;
      public final int cellWidth;
      public final int cellHeight;
      public final int padding;
      public final int sourceWidth;
      public final int sourceHeight;
      public final double normalizationReference;

      private Result(BufferedImage image, boolean processed, String reconstructionPath, int cellWidth, int cellHeight,
         int padding, int sourceWidth, int sourceHeight, double normalizationReference) {
         this.image = image;
         this.processed = processed;
         this.reconstructionPath = reconstructionPath;
         this.cellWidth = cellWidth;
         this.cellHeight = cellHeight;
         this.padding = padding;
         this.sourceWidth = sourceWidth;
         this.sourceHeight = sourceHeight;
         this.normalizationReference = normalizationReference;
      }
   }
}
