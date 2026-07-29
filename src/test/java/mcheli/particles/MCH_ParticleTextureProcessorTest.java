package mcheli.particles;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.junit.Test;

public class MCH_ParticleTextureProcessorTest {
   @Test
   public void processesEightSmallFramesWithoutCrossingBoundaries() {
      BufferedImage source = new BufferedImage(64, 8, BufferedImage.TYPE_INT_ARGB);
      for(int frame = 0; frame < 8; ++frame) {
         for(int y = 1; y < 7; ++y) {
            for(int x = 1; x < 7; ++x) {
               source.setRGB(frame * 8 + x, y, 0xFF000000 | (frame + 1) * 0x10101);
            }
         }
      }

      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      assertTrue(result.processed);
      assertEquals(result.cellWidth * 8, result.image.getWidth());
      assertEquals(8, result.image.getWidth() / result.cellWidth);
      assertTrue(hasIntermediateAlpha(result.image));
      assertEquals(0, result.image.getRGB(0, 0) >>> 24);
      for(int frame = 0; frame < 8; ++frame) {
         int center = result.image.getRGB(frame * result.cellWidth + result.cellWidth / 2,
            result.cellHeight / 2) >>> 24;
         assertTrue("frame " + frame + " should retain its distinct coverage", center > 0);
         assertEquals(0, result.image.getRGB((frame + 1) * result.cellWidth - 1,
            result.cellHeight / 2) >>> 24);
      }
   }

   @Test
   public void bypassesAdequateSource() {
      BufferedImage source = new BufferedImage(256, 32, BufferedImage.TYPE_INT_ARGB);
      source.setRGB(0, 0, 0x80FFFFFF);
      MCH_ParticleTextureProcessor.Result result = MCH_ParticleTextureProcessor.process(source);
      assertFalse(result.processed);
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

   private static boolean hasIntermediateAlpha(BufferedImage image) {
      for(int y = 0; y < image.getHeight(); ++y) {
         for(int x = 0; x < image.getWidth(); ++x) {
            int alpha = image.getRGB(x, y) >>> 24;
            if(alpha > 0 && alpha < 255) return true;
         }
      }
      return false;
   }
}
