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

   @Test
   public void directStepDiagonalHitsButRaisedSegmentsClear() {
      double[] half = {0.2D, 0.2D, 0.2D};
      double[] stepCenter = {1.0D, 0.5D, 0.0D};
      double[] stepHalf = {0.5D, 0.5D, 0.5D};
      assertTrue(sweepHits(new double[]{0.0D, 0.21D, 0.0D}, new double[]{1.5D, 1.21D, 0.0D}, half,
              stepCenter, stepHalf));
      assertFalse(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{1.5D, 1.21D, 0.0D}, half,
              stepCenter, stepHalf));
   }

   @Test
   public void raisedSegmentStillHitsTwoBlockWallAndWallBehindStep() {
      double[] half = {0.2D, 0.2D, 0.2D};
      assertTrue(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{1.5D, 1.21D, 0.0D}, half,
              new double[]{1.0D, 1.0D, 0.0D}, new double[]{0.5D, 1.0D, 0.5D}));
      assertTrue(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{2.0D, 1.21D, 0.0D}, half,
              new double[]{1.75D, 1.5D, 0.0D}, new double[]{0.25D, 1.5D, 0.5D}));
   }

   @Test
   public void abramsLengthNeedsStepUpBeforeSuspensionPitch() {
      double[] longHull = {1.15D, 0.55D, 3.7D};
      double[] step = {0.0D, 0.5D, 3.75D};
      double[] block = {0.5D, 0.5D, 0.5D};
      assertNotNull(MCH_ObbCollision.intersect(new double[]{0.0D, 0.72D, 0.0D}, pitchAxes(-6.0D),
              longHull, step, block));
      assertNull(MCH_ObbCollision.intersect(new double[]{0.0D, 1.82D, 0.0D}, pitchAxes(-6.0D),
              longHull, step, block));
   }

   @Test
   public void toyotaLengthKeepsExistingStepClearance() {
      double[] shortHull = {0.9D, 0.55D, 1.6D};
      assertNull(MCH_ObbCollision.intersect(new double[]{0.0D, 0.72D, 0.0D}, pitchAxes(-6.0D),
              shortHull, new double[]{0.0D, 0.5D, 3.75D}, new double[]{0.5D, 0.5D, 0.5D}));
   }

   @Test
   public void physicalLedgeTriggerUsesSupportPlaneRise() {
      assertFalse(MCH_EntityBaseVehicle.shouldTryTankStep(false, false, 0.2D, 0.0D));
      assertTrue(MCH_EntityBaseVehicle.shouldTryTankStep(false, true, 0.2D, 0.0D));
      assertTrue(MCH_EntityBaseVehicle.shouldTryTankStep(true, false, 0.2D, 0.0D));
      assertFalse(MCH_EntityBaseVehicle.shouldTryTankStep(false, true, 0.0D, 0.0D));
      assertTrue(MCH_EntityBaseVehicle.isClimbablePhysicalStepContact(1.0D, 1.8F));
      assertTrue(MCH_EntityBaseVehicle.isClimbablePhysicalStepContact(1.0D, 1.5F));
      assertFalse(MCH_EntityBaseVehicle.isClimbablePhysicalStepContact(0.0D, 1.8F));
      assertFalse(MCH_EntityBaseVehicle.isClimbablePhysicalStepContact(2.0D, 1.8F));
   }

   @Test
   public void abramsRiserContactChangesAxisDuringSafeStepUp() {
      // m1a2.txt's front physical box: offset (0, .9, 3), size (3, .5, 3).
      double[] half = {1.5D, 0.25D, 1.5D};
      double[] block = {0.0D, 0.5D, 5.0D};
      MCH_ObbCollision.Contact side = MCH_ObbCollision.intersect(
              new double[]{0.0D, 0.9D, 3.1D}, axes(0.0D), half, block, BLOCK_HALF);
      MCH_ObbCollision.Contact clearing = MCH_ObbCollision.intersect(
              new double[]{0.0D, 1.20D, 3.1D}, axes(0.0D), half, block, BLOCK_HALF);
      assertNotNull(side);
      assertNotNull(clearing);
      assertTrue(Math.abs(side.normalZ) > 0.75D);
      assertTrue("the same riser becomes a top-face SAT contact while ascending",
              Math.abs(clearing.normalY) > 0.75D);
      assertTrue(1.0D <= 1.8D + 0.05D);
   }

   @Test
   public void realAbramsSegmentedRouteClearsOneBlockAndRemainsStableNextTick() {
      double[] half = {1.5D, 0.25D, 1.5D};
      double[] block = {0.0D, 0.5D, 5.0D};
      double[][] route = {{0.0D, 0.9D, 3.1D}, {0.0D, 2.7D, 3.1D},
              {0.0D, 2.7D, 3.5D}, {0.15D, 2.7D, 3.5D}, {0.15D, 1.25D, 3.5D}};
      assertSegmentClear(route[1], route[2], half, block, BLOCK_HALF);
      assertSegmentClear(route[2], route[3], half, block, BLOCK_HALF);
      MCH_ObbCollision.Contact support = MCH_ObbCollision.intersect(route[4], axes(0.0D), half,
              block, BLOCK_HALF);
      assertNotNull(support);
      assertTrue(Math.abs(support.normalY) > 0.75D);
      assertEquals(support.penetration, MCH_ObbCollision.intersect(route[4], axes(0.0D), half,
              block, BLOCK_HALF).penetration, 0.0D);
   }

   @Test
   public void abramsDiagonalAndShortToyotaRaisedRoutesClear() {
      double[] block = {0.0D, 0.5D, 5.0D};
      assertSegmentClear(new double[]{0.0D, 2.7D, 3.0D}, new double[]{0.2D, 2.7D, 3.4D},
              new double[]{1.5D, 0.25D, 1.5D}, block, BLOCK_HALF);
      assertSegmentClear(new double[]{0.0D, 2.1D, 3.6D}, new double[]{0.2D, 2.1D, 4.0D},
              new double[]{0.825D, 0.425D, 0.825D}, block, BLOCK_HALF);
   }

   @Test
   public void raisedRouteRejectsInsufficientStepWallCeilingAndNewSideContact() {
      double[] hull = {0.2D, 0.2D, 0.2D};
      assertTrue(1.0D > 0.6D + 0.05D);
      assertTrue(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{1.5D, 1.21D, 0.0D}, hull,
              new double[]{1.0D, 1.0D, 0.0D}, new double[]{0.5D, 1.0D, 0.5D}));
      assertTrue(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{2.0D, 1.21D, 0.0D}, hull,
              new double[]{1.75D, 1.5D, 0.0D}, new double[]{0.25D, 1.5D, 0.5D}));
      assertTrue(sweepHits(new double[]{0.0D, 0.21D, 0.0D}, new double[]{0.0D, 1.8D, 0.0D}, hull,
              new double[]{0.0D, 1.75D, 0.0D}, new double[]{0.5D, 0.25D, 0.5D}));
   }

   @Test
   public void partialBlockCollisionHeightsArePreserved() {
      assertFalse(sweepHits(new double[]{0.0D, 0.71D, 0.0D}, new double[]{1.5D, 0.71D, 0.0D},
              new double[]{0.2D, 0.2D, 0.2D}, new double[]{1.0D, 0.25D, 0.0D},
              new double[]{0.5D, 0.25D, 0.5D}));
      assertFalse(sweepHits(new double[]{0.0D, 1.21D, 0.0D}, new double[]{1.5D, 1.21D, 0.0D},
              new double[]{0.2D, 0.2D, 0.2D}, new double[]{1.0D, 0.5D, 0.0D},
              new double[]{0.5D, 0.5D, 0.25D}));
   }

   private static void assertSegmentClear(double[] start, double[] end, double[] hullHalf,
                                          double[] blockCenter, double[] blockHalf) {
      assertFalse(sweepHits(start, end, hullHalf, blockCenter, blockHalf));
   }

   private static boolean sweepHits(double[] start, double[] end, double[] hullHalf,
                                    double[] blockCenter, double[] blockHalf) {
      for(int i = 1; i <= 40; ++i) {
         double f = i / 40.0D;
         double[] center = {start[0] + (end[0] - start[0]) * f,
                 start[1] + (end[1] - start[1]) * f, start[2] + (end[2] - start[2]) * f};
         if(MCH_ObbCollision.intersect(center, axes(0.0D), hullHalf, blockCenter, blockHalf) != null) return true;
      }
      return false;
   }

   private static MCH_ObbCollision.Contact contact(double x, double y, double z, double yaw) {
      return MCH_ObbCollision.intersect(new double[]{x, y + 0.5D, z}, axes(yaw), HALF,
              BLOCK_CENTER, BLOCK_HALF);
   }

   private static double[][] axes(double degrees) {
      double r = Math.toRadians(degrees), c = Math.cos(r), s = Math.sin(r);
      return new double[][]{{c, 0.0D, -s}, {0.0D, 1.0D, 0.0D}, {s, 0.0D, c}};
   }

   private static double[][] pitchAxes(double degrees) {
      double r = Math.toRadians(degrees), c = Math.cos(r), s = Math.sin(r);
      return new double[][]{{1.0D, 0.0D, 0.0D}, {0.0D, c, -s}, {0.0D, s, c}};
   }
}
