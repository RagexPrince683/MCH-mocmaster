package mcheli.tank;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.Test;
import sun.misc.Unsafe;

import static org.junit.Assert.*;

/** Runs the real wheel collision movement against a flat collision surface, without a game client. */
public class MCH_CarWheelContactTest {
   static <T> T uninitialized(Class<T> type) throws Exception {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
   }

   static class RoadWheel extends MCH_EntityWheel {
      boolean road = true;
      RoadWheel(World world) { super(null); this.worldObj = world; }
      @Override public List getCollidingBoundingBoxes(Entity entity, AxisAlignedBB area) {
         AxisAlignedBB floor = AxisAlignedBB.getBoundingBox(-1000, -1, -1000, 1000, 0, 1000);
         return road && floor.intersectsWith(area) ? Collections.singletonList(floor) : Collections.emptyList();
      }
      @Override protected void updateFallState(double distance, boolean grounded) {}
      @Override protected void doBlockCollisions() {}
   }

   static RoadWheel[] settledWheels(double localY, double frontZ, double rearZ) throws Exception {
      // Only the profiler is accessed: collisions and block callbacks are supplied by RoadWheel.
      World world = uninitialized(WorldServer.class);
      Field profiler = World.class.getDeclaredField("theProfiler");
      profiler.setAccessible(true);
      profiler.set(world, new Profiler());
      // Only the parent geometry is accessed, avoiding mod/renderer initialization.
      MCH_EntityTank parent = uninitialized(MCH_EntityTank.class);
      parent.yOffset = 0.35F;
      parent.posY = parent.yOffset;
      RoadWheel[] wheels = new RoadWheel[4];
      for(int i = 0; i < wheels.length; ++i) {
         RoadWheel wheel = new RoadWheel(world);
         wheel.setParents(parent);
         wheel.setWheelPos(Vec3.createVectorHelper(i % 2 == 0 ? 0.7 : -0.7, localY,
                 i < 2 ? frontZ : rearZ), Vec3.createVectorHelper(0, 0, 0));
         double targetY = parent.yOffset + localY;
         wheel.setPosition(wheel.pos.xCoord, targetY + 1.0, wheel.pos.zCoord);
         for(int tick = 0; tick < 120; ++tick) {
            // Exactly the manager's soft vertical damping and final placement on a level road.
            wheel.moveEntity(0.03, (targetY - wheel.posY) * 0.15, 0.7);
            wheel.setPositionAndRotation(wheel.posX, wheel.posY, wheel.posZ, 0, 0);
         }
         wheels[i] = wheel;
      }
      return wheels;
   }

   @Test public void originalProbeMissesAllFourSettledPassengerWheels() throws Exception {
      for(double[] layout : new double[][]{{1.99, -1.70}, {1.851, -1.934}, {1.965, -1.789}}) {
         int count = 0;
         for(RoadWheel wheel : settledWheels(-0.24, layout[0], layout[1])) {
            assertEquals(0.11, wheel.boundingBox.minY, 1.0E-6);
            if(wheel.hasGroundContact()) ++count;
         }
         assertEquals(0, count);
      }
   }

   @Test public void gripProbeFindsFourAndDoesNotBorrowPairedOrStaleContact() throws Exception {
      for(double[] layout : new double[][]{{1.99, -1.70}, {1.851, -1.934}, {1.965, -1.789}}) {
         RoadWheel[] wheels = settledWheels(-0.24, layout[0], layout[1]);
         MCH_WheelManager manager = new MCH_WheelManager(wheels[0].getParents());
         manager.wheels = wheels;
         MCH_WheelManager.CarContact full = manager.getCarGroundContact(true);
         assertEquals(4, full.total);
         assertEquals(2, full.front);
         assertEquals(2, full.rear);
         assertEquals(0, full.rawProbe);
         assertEquals(1.0, full.fraction(), 0.0);
         wheels[0].road = false;
         wheels[1].road = false;
         wheels[0].onGround = wheels[1].onGround = true; // Synthetic suspension pairing is not support.
         assertEquals(0.5, manager.getCarGroundContact(true).fraction(), 0.0);
         wheels[2].setDead();
         assertEquals(0.25, manager.getCarGroundContact(true).fraction(), 0.0);
         wheels[3].getParents().posY += 2.0; // Wheel still near the road after body takeoff.
         assertEquals(0.0, manager.getCarGroundContact(true).fraction(), 0.0);
      }
   }

   @Test public void queryDoesNotChangeWheelPositionOrCollisionFlags() throws Exception {
      RoadWheel wheel = settledWheels(-0.24, 1.99, -1.70)[0];
      double y = wheel.posY, boxY = wheel.boundingBox.minY;
      boolean grounded = wheel.onGround;
      assertTrue(wheel.hasCarGroundContact());
      assertEquals(y, wheel.posY, 0.0);
      assertEquals(boxY, wheel.boundingBox.minY, 0.0);
      assertEquals(grounded, wheel.onGround);
   }

   static class RoadTank extends MCH_EntityTank {
      MCH_TankInfo info;
      RoadTank(World world) { super(world); } // Fixture uses uninitialized(), never this constructor.
      @Override public MCH_TankInfo getTankInfo() { return info; }
   }

   @Test public void serverCorrectionUsesExplicitOptInAndTracksABoundedTurn() throws Exception {
      RoadWheel[] wheels = settledWheels(-0.24, 1.99, -1.70);
      RoadTank tank = uninitialized(RoadTank.class);
      tank.info = new MCH_TankInfo("fixture");
      tank.info.weightType = 1;
      tank.worldObj = wheels[0].worldObj;
      tank.yOffset = 0.35F; tank.posY = tank.yOffset;
      MCH_WheelManager manager = new MCH_WheelManager(tank);
      manager.wheels = wheels;
      for(RoadWheel wheel : wheels) wheel.setParents(tank);
      Field managerField = MCH_EntityTank.class.getDeclaredField("WheelMng");
      managerField.setAccessible(true); managerField.set(tank, manager);
      tank.motionX = 0.1; tank.motionZ = 0.8;
      tank.applyCarLateralGrip();
      assertEquals(0.1, tank.motionX, 0.0); // WeightType=Car alone never selects grip.
      assertNull(tank.getCarGripDiagnostic());
      tank.info.civilianCarGrip = true;
      tank.motionX = 0; tank.motionZ = 0.8;
      tank.applyCarLateralGrip(); // Initialize the authoritative yaw at spawn.
      tank.setRotYaw(6);
      tank.applyCarLateralGrip();
      double radians = Math.toRadians(tank.getRotYaw());
      double residual = tank.motionX * Math.cos(radians) + tank.motionZ * Math.sin(radians);
      assertEquals(0.8 * Math.sin(Math.toRadians(6)) * 0.15, residual, 1.0E-9);
      assertTrue(tank.motionX < -0.07); // Momentum turns, rather than continuing down +Z.
      tank.setRotYaw(90); // Several rotation packets still share one tick's bounded steering budget.
      tank.applyCarLateralGrip();
      assertTrue(tank.getRotYaw() < 13);
      tank.posY += 2; // Airborne body, stale wheels near ground.
      double x = tank.motionX, z = tank.motionZ;
      tank.setRotYaw(tank.getRotYaw() + 20);
      tank.applyCarLateralGrip();
      assertEquals(x, tank.motionX, 0.0);
      assertEquals(z, tank.motionZ, 0.0);
      tank.info.civilianCarGrip = false;
      tank.info.weightType = 2;
      tank.motionX = 0.1;
      tank.applyCarLateralGrip();
      assertEquals(0.1, tank.motionX, 0.0); // Existing tracked/military configs remain untouched.
   }
}
