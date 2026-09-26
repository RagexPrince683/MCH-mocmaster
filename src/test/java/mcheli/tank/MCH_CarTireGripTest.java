package mcheli.tank;

import org.junit.Test;
import net.minecraft.util.AxisAlignedBB;
import java.util.Collections;

import static org.junit.Assert.*;

public class MCH_CarTireGripTest {
   @Test
   public void optionalSizesAndGripRemainCompatibleAndReloadable() {
      MCH_TankInfo info = new MCH_TankInfo("tire_test");
      assertNull(info.frontTireSize);
      assertNull(info.rearTireSize);
      assertEquals(0.06F, info.carLateralGrip, 0.0F);
      info.loadItemData("fronttiresize", "265/35 ZR19");
      info.loadItemData("REARTIRESIZE", "335/30R20");
      assertEquals(265, info.frontTireSize.widthMm);
      assertEquals(335, info.rearTireSize.widthMm);
      info.loadItemData("CarLateralGrip", "0");
      assertEquals(0.0F, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "NaN");
      assertEquals(0.06F, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "invalid");
      assertEquals(0.06F, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "999");
      assertEquals(0.25F, info.carLateralGrip, 0.0F);
      info.preReload();
      assertNull(info.frontTireSize);
      assertNull(info.rearTireSize);
      assertEquals(0.06F, info.carLateralGrip, 0.0F);
   }

   @Test
   public void contactNeedsSupportBelowTheActualWheelNotAWallOrNearbyTerrain() {
      AxisAlignedBB wheel = AxisAlignedBB.getBoundingBox(0.0D, 1.0D, 0.0D, 1.0D, 2.0D, 1.0D);
      assertTrue(MCH_EntityWheel.hasGroundSupport(wheel, Collections.singletonList(
              AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D))));
      assertFalse(MCH_EntityWheel.hasGroundSupport(wheel, Collections.emptyList()));
      assertFalse(MCH_EntityWheel.hasGroundSupport(wheel, Collections.singletonList(
              AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 1.0D, 0.90D, 1.0D))));
      assertFalse(MCH_EntityWheel.hasGroundSupport(wheel, Collections.singletonList(
              AxisAlignedBB.getBoundingBox(1.0D, 0.0D, 0.0D, 2.0D, 3.0D, 1.0D))));
      assertFalse(MCH_EntityWheel.hasGroundSupport(wheel, Collections.singletonList(
              AxisAlignedBB.getBoundingBox(0.0D, 2.05D, 0.0D, 1.0D, 3.0D, 1.0D))));
   }

   @Test
   public void malformedAndUnsupportedSizesUseNeutralResponse() {
      String[] invalid = {null, "", "unknown", "999/55R16", "205/00R16", "205/999R16", "205/55R99", "265/790R540", "7.00-16", "205/55R16 garbage"};
      for(String size : invalid) {
         assertNull(MCH_CarTireGrip.parseTireSize(size));
         assertEquals(1.0D, MCH_CarTireGrip.response(MCH_CarTireGrip.parseTireSize(size)), 0.0D);
      }
      assertEquals(0, MCH_CarTireGrip.parseTireSize("175 R14").aspectPercent);
   }

   @Test
   public void sizeOnlyModestlyChangesResponseAndCannotIncreaseGripLimit() {
      for(String size : new String[]{"175R14", "225/50R16", "265/35R19", "355/25R21", "100/100R10", "500/20R30"}) {
         double response = MCH_CarTireGrip.response(MCH_CarTireGrip.parseTireSize(size));
         assertTrue(response >= 0.95D && response <= 1.05D);
         assertEquals(0.06D, MCH_CarTireGrip.lateralCorrection(5.0D, 0.06D, 1.0D, response, 0.0D), 1.0E-12D);
      }
   }

   @Test
   public void airborneMissingWheelsAndDisabledGripGiveNoCorrection() {
      assertEquals(0.0D, correction(1.0D, 0.0D, 0.0D), 0.0D);
      assertEquals(0.0D, MCH_CarTireGrip.lateralCorrection(1.0D, 0.0D, 1.0D, 1.0D, 0.0D), 0.0D);
      assertEquals(correction(1.0D, 1.0D, 0.0D) * 0.25D, correction(1.0D, 0.25D, 0.0D), 1.0E-12D);
   }

   @Test
   public void accelerationAndBrakingConsumeTheSameBoundedBudget() {
      double coast = correction(1.0D, 1.0D, 0.0D);
      double accelerating = correction(1.0D, 1.0D, 0.10D);
      assertTrue(accelerating < coast);
      assertEquals(accelerating, correction(1.0D, 1.0D, -0.10D), 0.0D);
      assertTrue(correction(1.0D, 1.0D, 10.0D) > 0.0D);
      assertEquals(0.0D, correction(1.0D, 1.0D, Double.NaN), 0.0D);
   }

   @Test
   public void forwardAndReversePreserveForwardSpeedAtEveryYawWithoutAddingEnergy() {
      for(int yaw = -180; yaw <= 180; yaw += 15) {
         double fx = -Math.sin(Math.toRadians(yaw));
         double fz = Math.cos(Math.toRadians(yaw));
         for(double forward : new double[]{-2.0D, -0.00001D, 0.0D, 0.00001D, 2.0D}) {
            for(double side : new double[]{-2.0D, -0.00001D, 0.0D, 0.00001D, 2.0D}) {
               double x = forward * fx + side * fz;
               double z = forward * fz - side * fx;
               double delta = correction(x * fz - z * fx, 1.0D, 0.10D);
               double nx = x - delta * fz;
               double nz = z + delta * fx;
               assertEquals(forward, nx * fx + nz * fz, 1.0E-12D);
               assertTrue(nx * nx + nz * nz <= x * x + z * z + 1.0E-12D);
               assertTrue(Math.abs(side - delta) <= Math.abs(side) + 1.0E-12D);
               if(side != 0.0D) assertEquals(Math.signum(side), Math.signum(side - delta), 0.0D);
            }
         }
      }
      double side = 0.001D;
      for(int tick = 0; tick < 100; ++tick) {
         side -= correction(side, 1.0D, 0.0D);
         assertTrue(side > 0.0D);
      }
      assertTrue(side < 1.0E-12D);
   }

   private static double correction(double side, double contact, double demand) {
      return MCH_CarTireGrip.lateralCorrection(side, 0.06D, contact, 1.0D, demand);
   }
}
