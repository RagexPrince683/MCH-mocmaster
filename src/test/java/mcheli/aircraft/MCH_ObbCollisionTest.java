package mcheli.aircraft;

import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_ObbCollisionTest {
   private static final double[] HALF = {1.0D, 0.5D, 2.0D};
   private static final double[] BLOCK_CENTER = {2.25D, 0.5D, 0.0D};
   private static final double[] BLOCK_HALF = {0.5D, 0.5D, 0.5D};

   @Test
   public void longHullClearAndWallTurns() {
      assertNull(contact(0.0D, 0.0D, 0.0D, 0.0D));
      assertNotNull(contact(0.0D, 0.0D, 0.0D, 45.0D));
      assertNull(contact(-0.5D, 0.0D, 0.0D, -20.0D));
   }

   @Test
   public void fullSatSeparatesFloorFromSideWallMtv() {
      MCH_ObbCollision.Contact floor = MCH_ObbCollision.intersect(
              new double[]{0.0D, 1.49D, 0.0D}, axes(27.0D), HALF,
              new double[]{0.0D, 0.5D, 0.0D}, new double[]{4.0D, 0.5D, 4.0D});
      assertNotNull(floor);
      assertTrue(Math.abs(floor.normalY) > 0.9D);
      MCH_ObbCollision.Contact wall = contact(0.8D, 0.0D, 0.0D, 25.0D);
      assertNotNull(wall);
      assertTrue(Math.abs(wall.normalY) < 0.75D);
   }

   @Test
   public void combinedTranslationAndRotationFindsFirstContact() {
      boolean touched = false;
      for(int i = 0; i <= 20; ++i) {
         double f = i / 20.0D;
         if(contact(f * 0.8D, 0.0D, 0.0D, f * 35.0D) != null) {
            touched = true;
            break;
         }
      }
      assertTrue(touched);
   }

   @Test
   public void yawWrappingUsesShortArc() {
      float delta = net.minecraft.util.MathHelper.wrapAngleTo180_float(-179.0F - 179.0F);
      assertEquals(2.0F, delta, 0.0F);
   }

   private static MCH_ObbCollision.Contact contact(double x, double y, double z, double yaw) {
      return MCH_ObbCollision.intersect(new double[]{x, y + 0.5D, z}, axes(yaw), HALF,
              BLOCK_CENTER, BLOCK_HALF);
   }

   private static double[][] axes(double degrees) {
      double r = Math.toRadians(degrees), c = Math.cos(r), s = Math.sin(r);
      return new double[][]{{c, 0.0D, -s}, {0.0D, 1.0D, 0.0D}, {s, 0.0D, c}};
   }
}
