package mcheli.particles;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

public class MCH_ParticleTextureProcessorTest {
   @Test
   public void processesBundledSmokeWithTransparentFrameBorders() throws IOException {
      InputStream stream = getClass().getResourceAsStream("/assets/mcheli/textures/particles/smoke.png");
      assertNotNull(stream);
      BufferedImage source = javax.imageio.ImageIO.read(stream);
      assertEquals(64, source.getWidth());
      assertEquals(8, source.getHeight());
      for(int x = 0; x < source.getWidth(); ++x) for(int y = 0; y < source.getHeight(); ++y) {
         int alpha = source.getRGB(x, y) >>> 24;
         assertTrue("source alpha must be binary", alpha == 0 || alpha == 255);
      }
      stream.close();
      stream = getClass().getResourceAsStream("/assets/mcheli/textures/particles/smoke.png");
      MCH_ParticleTextureProcessor.Result result;
      try {
         result = MCH_ParticleTextureProcessor.readAndProcess(stream);
      } finally {
         stream.close();
      }

      assertTrue(result.processed);
      assertEquals(MCH_ParticleTextureProcessor.BINARY_DENSITY, result.reconstructionPath);
      assertEquals(2, result.padding);
      assertEquals(288, result.image.getWidth());
      assertEquals(36, result.image.getHeight());
      for(int frame = 0; frame < 8; ++frame) {
         assertTransparentGutterAndContentEdge(result, frame);
         assertTrue("frame " + frame + " should remain visible", hasAlpha(result, frame, false));
         assertTrue("frame " + frame + " should retain soft alpha", hasIntermediateAlpha(result, frame));
      }
      assertTrue("first frame should be denser than final frame", alphaSum(result, 0) > alphaSum(result, 7));
      assertTrue("final frame must not be independently normalized", maxAlpha(result, 7) < maxAlpha(result, 0));
      assertZeroAlphaHasZeroRgb(result.image);
   }

   @Test
   public void blursBinarySamplesBeforeScaling() {
      BufferedImage source = new BufferedImage(64, 8, BufferedImage.TYPE_INT_ARGB);
      source.setRGB(2, 4, 0xFFFFFFFF);
      source.setRGB(4, 4, 0xFFFFFFFF);
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      int centerY = result.cellHeight / 2;
      int left = alpha(result.image, 11, centerY);
      int gap = alpha(result.image, 15, centerY);
      int right = alpha(result.image, 20, centerY);
      assertTrue("transparent source gap should receive blurred coverage", gap > 1);
      assertTrue("nearby samples should merge into a continuous gradient", left > 0 && right > 0);
      assertNotEquals("a source sample must not become a flat enlarged square", left, alpha(result.image, 12, centerY));
   }

   @Test
   public void keepsFramesIndependent() {
      BufferedImage source = new BufferedImage(64, 8, BufferedImage.TYPE_INT_ARGB);
      source.setRGB(4, 4, 0xFFFFFFFF);
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      assertTrue(hasAlpha(result, 0, false));
      for(int frame = 1; frame < 8; ++frame) {
         assertFalse("frame " + frame + " copied neighboring alpha", hasAlpha(result, frame, false));
      }
   }

   @Test
   public void centeredPixelCoverageDoesNotReachFrameBorder() {
      BufferedImage source = new BufferedImage(64, 8, BufferedImage.TYPE_INT_ARGB);
      for(int frame = 0; frame < 8; ++frame) source.setRGB(frame * 8 + 4, 4, 0xFFFFFFFF);
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      for(int frame = 0; frame < 8; ++frame) {
         assertTransparentGutterAndContentEdge(result, frame);
         assertTrue(hasAlpha(result, frame, false));
      }
   }

   @Test
   public void sourceEdgeCoverageStillGetsTransparentOutputBorder() {
      BufferedImage source = new BufferedImage(64, 8, BufferedImage.TYPE_INT_ARGB);
      for(int frame = 0; frame < 8; ++frame) {
         for(int y = 0; y < 8; ++y) {
            source.setRGB(frame * 8, y, 0xFFFFFFFF);
            source.setRGB(frame * 8 + 7, y, 0xFFFFFFFF);
         }
      }
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      for(int frame = 0; frame < 8; ++frame) assertTransparentGutterAndContentEdge(result, frame);
   }

   @Test
   public void bypassesAdequateSource() {
      BufferedImage source = new BufferedImage(256, 32, BufferedImage.TYPE_INT_ARGB);
      source.setRGB(0, 0, 0x80FFFFFF);
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      assertFalse(result.processed);
      assertEquals(MCH_ParticleTextureProcessor.DIRECT_USE, result.reconstructionPath);
      assertSame(source, result.image);
   }

   @Test(expected = IllegalArgumentException.class)
   public void rejectsInvalidWidth() {
      MCH_ParticleTextureProcessor.process(new BufferedImage(65, 8, BufferedImage.TYPE_INT_ARGB));
   }

   @Test(expected = IOException.class)
   public void rejectsMissingResource() throws IOException {
      MCH_ParticleTextureProcessor.readAndProcess(null);
   }

   private static void assertTransparentGutterAndContentEdge(MCH_ParticleTextureProcessor.Result result, int frame) {
      int cellStart = frame * result.cellWidth;
      int contentStartX = cellStart + result.padding;
      int contentEndX = cellStart + result.cellWidth - result.padding - 1;
      int contentStartY = result.padding;
      int contentEndY = result.cellHeight - result.padding - 1;
      for(int y = 0; y < result.cellHeight; ++y) {
         for(int x = cellStart; x < cellStart + result.cellWidth; ++x) {
            boolean gutter = x < contentStartX || x > contentEndX || y < contentStartY || y > contentEndY;
            boolean contentEdge = x == contentStartX || x == contentEndX || y == contentStartY || y == contentEndY;
            if(gutter || contentEdge) {
               assertEquals("nontransparent frame border at " + x + "," + y, 0,
                  result.image.getRGB(x, y));
            }
         }
      }
      assertEquals(0, result.image.getRGB(cellStart, 0));
      assertEquals(0, result.image.getRGB(cellStart + result.cellWidth - 1, 0));
      assertEquals(0, result.image.getRGB(cellStart, result.cellHeight - 1));
      assertEquals(0, result.image.getRGB(cellStart + result.cellWidth - 1, result.cellHeight - 1));
   }

   private static boolean hasAlpha(MCH_ParticleTextureProcessor.Result result, int frame, boolean intermediateOnly) {
      int start = frame * result.cellWidth;
      for(int y = 0; y < result.cellHeight; ++y) {
         for(int x = start; x < start + result.cellWidth; ++x) {
            int alpha = result.image.getRGB(x, y) >>> 24;
            if(intermediateOnly ? alpha > 0 && alpha < 255 : alpha > 0) return true;
         }
      }
      return false;
   }

   private static boolean hasIntermediateAlpha(MCH_ParticleTextureProcessor.Result result, int frame) {
      return hasAlpha(result, frame, true);
   }

   private static void assertZeroAlphaHasZeroRgb(BufferedImage image) {
      for(int y = 0; y < image.getHeight(); ++y) {
         for(int x = 0; x < image.getWidth(); ++x) {
            int argb = image.getRGB(x, y);
            if((argb >>> 24) == 0) assertEquals(0, argb);
         }
      }
   }

   private static int alpha(BufferedImage image, int x, int y) {
      return image.getRGB(x, y) >>> 24;
   }

   private static long alphaSum(MCH_ParticleTextureProcessor.Result result, int frame) {
      long sum = 0L;
      int start = frame * result.cellWidth;
      for(int y = 0; y < result.cellHeight; ++y) for(int x = start; x < start + result.cellWidth; ++x) {
         sum += result.image.getRGB(x, y) >>> 24;
      }
      return sum;
   }

   private static int maxAlpha(MCH_ParticleTextureProcessor.Result result, int frame) {
      int maximum = 0;
      int start = frame * result.cellWidth;
      for(int y = 0; y < result.cellHeight; ++y) for(int x = start; x < start + result.cellWidth; ++x) {
         maximum = Math.max(maximum, result.image.getRGB(x, y) >>> 24);
      }
      return maximum;
   }
}
