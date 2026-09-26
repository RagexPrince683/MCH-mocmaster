package mcheli.tank;

import org.junit.Test;
import net.minecraft.util.AxisAlignedBB;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;

import static org.junit.Assert.*;

public class MCH_CarTireGripTest {
   @Test
   public void optionalSizesAndGripRemainCompatibleAndReloadable() {
      MCH_TankInfo info = new MCH_TankInfo("tire_test");
      assertFalse(info.civilianCarGrip);
      assertFalse(info.carGripDiagnostics);
      assertNull(info.frontTireSize);
      assertNull(info.rearTireSize);
      assertEquals(MCH_CarTireGrip.DEFAULT_GRIP, info.carLateralGrip, 0.0F);
      info.loadItemData("civilianCARgrip", "true");
      info.loadItemData("CarGripDiagnostics", "true");
      assertTrue(info.civilianCarGrip);
      assertTrue(info.carGripDiagnostics);
      info.loadItemData("fronttiresize", "265/35 ZR19");
      info.loadItemData("REARTIRESIZE", "335/30R20");
      assertEquals(265, info.frontTireSize.widthMm);
      assertEquals(335, info.rearTireSize.widthMm);
      info.loadItemData("CarLateralGrip", "0");
      assertEquals(0.0F, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "NaN");
      assertEquals(MCH_CarTireGrip.DEFAULT_GRIP, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "invalid");
      assertEquals(MCH_CarTireGrip.DEFAULT_GRIP, info.carLateralGrip, 0.0F);
      info.loadItemData("CarLateralGrip", "999");
      assertEquals(0.25F, info.carLateralGrip, 0.0F);
      info.preReload();
      assertFalse(info.civilianCarGrip);
      assertFalse(info.carGripDiagnostics);
      assertNull(info.frontTireSize);
      assertNull(info.rearTireSize);
      assertEquals(MCH_CarTireGrip.DEFAULT_GRIP, info.carLateralGrip, 0.0F);
   }

   @Test public void onlyAuditedCivilianDefinitionsOptInAndReferencesMatch() throws Exception {
      Set<String> expected = new HashSet<String>(Arrays.asList("2102", "2105", "350z", "ae86", "altis", "bcnr33",
              "bnr32", "bnr34", "bugattichiron", "carrera_gt", "challenger", "dacia", "delorean", "fresh_auto",
              "impreza", "phantom", "rx-8", "rx7", "s15", "silvia_s14", "starion", "w123"));
      for(String directory : new String[]{"src/main/resources/assets/mcheli/tanks", "configreference/tanks"}) {
         Set<String> actual = new HashSet<String>();
         try(DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(directory), "*.txt")) {
            for(Path file : files) {
               String name = file.getFileName().toString().replace(".txt", "");
               MCH_TankInfo info = new MCH_TankInfo(name);
               for(String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                  String[] entry = line.split("=", 2);
                  if(entry.length != 2) continue;
                  String key = entry[0].trim();
                  if(key.equalsIgnoreCase("CivilianCarGrip") || key.equalsIgnoreCase("CarGripDiagnostics")
                          || key.equalsIgnoreCase("FrontTireSize") || key.equalsIgnoreCase("RearTireSize")
                          || key.equalsIgnoreCase("CarLateralGrip")) info.loadItemData(key, entry[1].trim());
               }
               if(info.civilianCarGrip) {
                  actual.add(name);
                  assertEquals(MCH_CarTireGrip.DEFAULT_GRIP, info.carLateralGrip, 0.0F);
               }
               assertFalse("diagnostics enabled by default: " + file, info.carGripDiagnostics);
               if(name.equals("ae86")) {
                  assertTrue(info.civilianCarGrip);
                  assertNull(info.frontTireSize);
                  assertNull(info.rearTireSize);
               }
               if(name.equals("challenger")) {
                  assertEquals(245, info.frontTireSize.widthMm);
                  assertEquals(45, info.frontTireSize.aspectPercent);
                  assertEquals(20, info.rearTireSize.rimInches, 0);
                  assertTrue(Files.readAllLines(file, StandardCharsets.UTF_8).contains("TechYear = 2018"));
               }
            }
         }
         assertEquals(expected, actual); // Includes explicit military/police/ATV/truck exclusion.
      }
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
         assertEquals(0.06D, MCH_CarTireGrip.lateralCorrection(5.0D, 0.06D, 1.0D, response), 1.0E-12D);
      }
   }

   @Test
   public void airborneMissingWheelsAndDisabledGripGiveNoCorrection() {
      assertEquals(0.0D, correction(1.0D, 0.0D), 0.0D);
      assertEquals(0.0D, MCH_CarTireGrip.lateralCorrection(1.0D, 0.0D, 1.0D, 1.0D), 0.0D);
      assertEquals(correction(1.0D, 1.0D) * 0.25D, correction(1.0D, 0.25D), 1.0E-12D);
   }

   @Test
   public void diagnosticExplainsZeroAndPreservesUnboundedRequest() {
      assertEquals("no_contact", MCH_CarTireGrip.calculate(0.3, 0.12, 0, 1).reason);
      assertEquals("grip_disabled", MCH_CarTireGrip.calculate(0.3, 0, 1, 1).reason);
      assertEquals("no_sideways_speed", MCH_CarTireGrip.calculate(0, 0.12, 1, 1).reason);
      assertEquals("invalid_input", MCH_CarTireGrip.calculate(Double.NaN, 0.12, 1, 1).reason);
      MCH_CarTireGrip.Result result = MCH_CarTireGrip.calculate(0.3, 0.12, 1, 1);
      assertEquals(0.255, result.requested, 1.0E-12);
      assertEquals(0.12, result.applied, 1.0E-12);
      assertEquals("applied", result.reason);
   }

   @Test
   public void normalTurnsTrackHeadingInForwardAndReverseAtBothDrivingSpeeds() {
      for(double speed : new double[]{0.8, 1.5}) {
         for(int direction : new int[]{-1, 1}) {
            double x = 0, z = direction * speed, yaw = 0;
            for(int tick = 0; tick < 40; ++tick) {
               double delta = MCH_CarTireGrip.steeringDelta(6, Math.hypot(x, z), 0.12, 1, 1);
               assertTrue(delta > 0);
               yaw += delta;
               double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
               double forward = x * fx + z * fz;
               double side = x * fz - z * fx;
               double correction = MCH_CarTireGrip.lateralCorrection(side, 0.12, 1, 1);
               assertTrue(Math.abs(correction) <= 0.12);
               x -= correction * fz; z += correction * fx;
               double slipAngle = Math.toDegrees(Math.atan2(Math.abs(x * fz - z * fx), Math.abs(forward)));
               assertTrue("velocity lags yaw by " + slipAngle, slipAngle < 1.5);
               double scale = speed / Math.hypot(x, z); // Maintain driving speed for the next steering tick.
               x *= scale; z *= scale;
            }
         }
      }
      assertEquals(0, MCH_CarTireGrip.steeringDelta(6, 0.8, 0.12, 0, 1), 0);
      assertEquals(0, MCH_CarTireGrip.steeringDelta(6, 0, 0.12, 1, 1), 0);
      assertTrue(MCH_CarTireGrip.steeringDelta(6, 1.0E-6, 0.12, 1, 1) < 0.002);
      assertEquals(MCH_CarTireGrip.steeringDelta(6, 1.5, 0.12, 1, 1),
              MCH_CarTireGrip.steeringDelta(6, 1.5, 0.12, 1, 0.25F) * 4, 1.0E-6);
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
               double delta = correction(x * fz - z * fx, 1.0D);
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
         side -= correction(side, 1.0D);
         assertTrue(side > 0.0D);
      }
      assertTrue(side < 1.0E-12D);
   }

   private static double correction(double side, double contact) {
      return MCH_CarTireGrip.lateralCorrection(side, 0.06D, contact, 1.0D);
   }
}
