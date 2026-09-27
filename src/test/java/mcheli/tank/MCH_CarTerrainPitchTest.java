package mcheli.tank;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mcheli.aircraft.MCH_BoundingBox;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.Test;

import static org.junit.Assert.*;

public class MCH_CarTerrainPitchTest {
   private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1) {
      return AxisAlignedBB.getBoundingBox(x0, y0, z0, x1, y1, z1);
   }

   private static double surface(double x, double referenceY, double step, AxisAlignedBB... boxes) {
      return MCH_CarTerrainPitch.highestSurface(Arrays.asList(boxes),
            box(x - 0.00001, referenceY - step - 0.00001, 0.49,
                  x + 0.00001, referenceY + step + 0.70001, 0.51), referenceY, step, 0.7);
   }

   @Test public void rendererSignMatchesClimbsDescentsAndEqualHeights() {
      float up = MCH_CarTerrainPitch.angle(1.0, 0.0, 3.69);
      float down = MCH_CarTerrainPitch.angle(0.0, 1.0, 3.69);
      assertTrue(up < 0);
      assertTrue(down > 0);
      // GL positive X rotation maps model +Z to negative Y: y = -z * sin(pitch).
      assertTrue(-Math.sin(Math.toRadians(up)) > 0);
      assertTrue(-Math.sin(Math.toRadians(down)) < 0);
      assertEquals(0.0F, MCH_CarTerrainPitch.angle(20.0, 20.0, 3.69), 0.0F);
      assertEquals(-18.0F, MCH_CarTerrainPitch.angle(10.0, 0.0, 0.5), 0.0F);
   }

   @Test public void stairCollisionPartsAndSlabsSupplyTheHighestReachableTread() {
      AxisAlignedBB lower = box(0, 0, 0, 1, 0.5, 1);
      AxisAlignedBB upper = box(0.5, 0.5, 0, 1, 1, 1);
      assertEquals(0.5, surface(0.25, 0, 1.2, lower, upper), 0);
      assertEquals(1.0, surface(0.75, 0, 1.2, lower, upper), 0);
      assertEquals(0.5, surface(0.25, 0, 0.5, lower), 0);
      assertTrue(Double.isNaN(surface(0.75, 0, 0.6, lower, upper)));
      assertEquals(1.0, surface(0.75, 0, 1.0, lower, upper), 0);
   }

   @Test public void buriedTreadsWallsAndInsufficientClearanceAreNotSteps() {
      AxisAlignedBB floor = box(0, -1, 0, 1, 0, 1);
      AxisAlignedBB wall = box(0, 0, 0, 1, 4, 1);
      assertTrue(Double.isNaN(surface(0.5, 0, 1.2, floor, wall)));
      AxisAlignedBB step = box(0, 0, 0, 1, 0.5, 1);
      AxisAlignedBB ceiling = box(0, 1.1, 0, 1, 2.0, 1);
      assertTrue(Double.isNaN(surface(0.5, 0, 1.2, step, ceiling)));
      assertEquals(0.5, surface(0.5, 0, 1.2, step, box(0, 1.2, 0, 1, 2, 1)), 0);
   }

   @Test public void downwardReachAndMissingSamplesAreSeparateFromZeroAndNegativeHeights() {
      assertEquals(0, surface(0.5, 1, 1.2, box(0, -1, 0, 1, 0, 1)), 0);
      assertEquals(-1, surface(0.5, 0, 1.2, box(0, -2, 0, 1, -1, 1)), 0);
      assertTrue(Double.isNaN(surface(0.5, 2, 1.2, box(0, -1, 0, 1, 0, 1))));
      assertTrue(Double.isNaN(surface(0.5, 0, 1.2)));
   }

   @Test public void bodyWallClearanceUsesCopiesAndDistinguishesAReachableStep() {
      AxisAlignedBB body = box(-1, 0, -1, 1, 0.7, 1);
      AxisAlignedBB step = box(-2, 0, 1, 2, 1, 2);
      assertFalse(MCH_CarTerrainPitch.facesWall(Collections.singletonList(step), body, 1.2, 0, 0.05));
      assertTrue(MCH_CarTerrainPitch.facesWall(Collections.singletonList(
            box(-2, 0, 1, 2, 4, 2)), body, 1.2, 0, 0.05));
      // A reachable rise can also be blocked by headroom in the upward clearance path.
      assertTrue(MCH_CarTerrainPitch.facesWall(Arrays.asList(step,
            box(-2, 1.0, -2, 2, 1.2, 0.9)), body, 1.2, 0, 0.05));
      assertEquals(0.0, body.minY, 0);
      assertEquals(0.7, body.maxY, 0);
      assertEquals(1.0, body.maxZ, 0);
   }

   @Test public void smoothingCannotKeepTheWrongSignAndDoesNotLevelSupportedDescent() {
      for(float smoothing : new float[]{0.28F, 0.45F}) {
         assertTrue(MCH_CarTerrainPitch.approach(12, -8, smoothing) < 0);
         assertTrue(MCH_CarTerrainPitch.approach(-12, 8, smoothing) > 0);
         float descending = 0;
         for(int tick = 0; tick < 100; ++tick) {
            descending = MCH_CarTerrainPitch.approach(descending, 8, smoothing);
            assertTrue(descending > 0);
         }
         assertEquals(8, descending, 0.00001);
         float level = descending;
         for(int tick = 0; tick < 100; ++tick) level = MCH_CarTerrainPitch.approach(level, 0, smoothing);
         assertEquals(0, level, 0);
      }
   }

   static class TerrainManager extends MCH_WheelManager {
      List<AxisAlignedBB> terrain = new ArrayList<AxisAlignedBB>();
      TerrainManager(MCH_EntityTank parent) { super(parent); }
      @Override List<AxisAlignedBB> getCivilianTerrainBoxes(AxisAlignedBB area) {
         List<AxisAlignedBB> result = new ArrayList<AxisAlignedBB>();
         for(AxisAlignedBB shape : terrain) if(shape.intersectsWith(area)) result.add(shape);
         return result;
      }
   }

   private static TerrainManager manager(double bodyY) throws Exception {
      MCH_EntityTank car = MCH_CarWheelContactTest.uninitialized(MCH_EntityTank.class);
      car.posY = bodyY + 0.35;
      Field boundingBox = Entity.class.getDeclaredField("boundingBox");
      boundingBox.setAccessible(true);
      boundingBox.set(car, box(-1, bodyY, -1, 1, bodyY + 0.7, 1));
      car.motionX = 0.2; car.motionY = -0.03; car.motionZ = 0.7;
      TerrainManager manager = new TerrainManager(car);
      manager.wheels = new MCH_EntityWheel[4];
      for(int i = 0; i < 4; ++i) {
         MCH_EntityWheel wheel = new MCH_EntityWheel(null);
         wheel.setWheelPos(Vec3.createVectorHelper(i % 2 == 0 ? 0.7 : -0.7, -0.24,
               i < 2 ? 1.99 : -1.70), manager.weightedCenter);
         wheel.setPosition(wheel.pos.xCoord, 100 + i, wheel.pos.zCoord);
         // Deliberately unrelated compression/contact cannot supply the terrain pitch.
         wheel.suspensionCompression = 0.1F * i;
         wheel.prevSuspensionCompression = 0.05F * i;
         wheel.suspensionCompressionRate = -0.02F * i;
         wheel.suspensionRestCompression = 0.15F;
         wheel.suspensionSupported = i >= 2;
         wheel.suspensionCompressionInitialized = true;
         manager.wheels[i] = wheel;
      }
      return manager;
   }

   private static MCH_TankInfo info() {
      MCH_TankInfo info = new MCH_TankInfo("pitch_fixture");
      info.stepHeight = 1.2F;
      return info;
   }

   @Test public void realWheelLayoutFindsUpDownEqualAndWallWithoutSuspensionSupport() throws Exception {
      TerrainManager manager = manager(1.0);
      AxisAlignedBB low = box(-10, -1, -10, 10, 0, 10);
      AxisAlignedBB front = box(-10, 0, 1.5, 10, 1, 10);
      AxisAlignedBB rear = box(-10, 0, -10, 10, 1, -1.5);
      manager.terrain.add(low); manager.terrain.add(front);
      assertTrue(manager.getCivilianTerrainPitch(0, 0, info()) < 0);
      manager.terrain.set(1, rear);
      assertTrue(manager.getCivilianTerrainPitch(0, 0, info()) > 0);
      manager.terrain.remove(1);
      assertEquals(0, manager.getCivilianTerrainPitch(0, 0, info()), 0);
      manager.terrain.add(box(-10, 0, 1, 10, 5, 10));
      assertEquals(0, manager.getCivilianTerrainPitch(0, 0, info()), 0);
      manager.terrain.clear(); manager.terrain.add(rear);
      assertTrue(Float.isNaN(manager.getCivilianTerrainPitch(0, 0, info())));
   }

   @Test public void pitchQueryCannotWriteBodyOrWheelSuspensionState() throws Exception {
      TerrainManager manager = manager(1);
      manager.terrain.add(box(-10, -1, -10, 10, 0, 10));
      manager.terrain.add(box(-10, 0, -10, 10, 1, -1.5));
      Object[] entities = {manager.parent, manager.wheels[0], manager.wheels[1], manager.wheels[2], manager.wheels[3]};
      List<Field> fields = new ArrayList<Field>();
      List<Object> before = new ArrayList<Object>();
      for(Object entity : entities) {
         for(Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for(Field field : type.getDeclaredFields()) {
               if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
               field.setAccessible(true); fields.add(field); before.add(field.get(entity));
            }
         }
      }
      double bodyY = manager.parent.boundingBox.minY;
      double wheelY = manager.wheels[0].boundingBox.minY;
      for(int tick = 0; tick < 20; ++tick) assertTrue(manager.getCivilianTerrainPitch(0, 0, info()) > 0);
      int index = 0;
      for(Object entity : entities) {
         for(Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for(Field field : type.getDeclaredFields()) {
               if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
               assertEquals(field.getName(), before.get(index), fields.get(index).get(entity)); ++index;
            }
         }
      }
      assertEquals(bodyY, manager.parent.boundingBox.minY, 0);
      assertEquals(wheelY, manager.wheels[0].boundingBox.minY, 0);
   }

   static class ShapeBlock extends Block {
      List<AxisAlignedBB> shapes;
      ShapeBlock(List<AxisAlignedBB> shapes) { super(Material.rock); this.shapes = shapes; }
      @Override public void addCollisionBoxesToList(World world, int x, int y, int z,
            AxisAlignedBB area, List result, Entity entity) {
         for(AxisAlignedBB shape : shapes) if(shape.intersectsWith(area)) result.add(shape);
      }
   }

   static class ShapeWorld extends WorldServer {
      ShapeBlock block;
      boolean loaded = true;
      ShapeWorld() { super(null, null, "fixture", 0, null, null); } // Allocated without a constructor.
      @Override public Block getBlock(int x, int y, int z) { return block; }
      @Override public boolean blockExists(int x, int y, int z) { return loaded; }
      @Override public int getTopSolidOrLiquidBlock(int x, int z) {
         throw new AssertionError("Pitch must use collision shapes, not world-top height");
      }
      @Override public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB area) {
         return Collections.emptyList();
      }
   }

   private static ShapeWorld world(AxisAlignedBB... shapes) throws Exception {
      ShapeWorld world = MCH_CarWheelContactTest.uninitialized(ShapeWorld.class);
      world.loaded = true;
      world.block = new ShapeBlock(Arrays.asList(shapes));
      Field profiler = World.class.getDeclaredField("theProfiler");
      profiler.setAccessible(true); profiler.set(world, new Profiler());
      return world;
   }

   @Test public void productionCollectorReadsBlockCollisionShapesAndSkipsUnloadedColumns() throws Exception {
      TerrainManager fixture = manager(1);
      ShapeWorld world = world(box(-10, -1, -10, 10, 0, 10), box(-10, 0, -10, 10, 1, -1.5));
      fixture.parent.worldObj = world;
      MCH_WheelManager manager = new MCH_WheelManager(fixture.parent);
      manager.wheels = fixture.wheels;
      assertTrue(manager.getCivilianTerrainPitch(0, 0, info()) > 0);
      world.loaded = false;
      assertTrue(Float.isNaN(manager.getCivilianTerrainPitch(0, 0, info())));
   }

   static class CollisionCar extends MCH_EntityTank {
      CollisionCar(World world) { super(world); } // Allocated without a constructor.
      @Override protected void updateFallState(double distance, boolean grounded) {}
      @Override protected void doBlockCollisions() {}
   }

   private static CollisionCar collisionCar(boolean grounded, double vertical) throws Exception {
      CollisionCar car = MCH_CarWheelContactTest.uninitialized(CollisionCar.class);
      car.worldObj = world(box(-10, -1, -10, 10, 0, 10), box(-10, 0, 1.05, 10, 1, 10));
      Field boundingBox = Entity.class.getDeclaredField("boundingBox");
      boundingBox.setAccessible(true); boundingBox.set(car, box(-1, 0, -1, 1, 0.7, 1));
      car.extraBoundingBox = new MCH_BoundingBox[0];
      car.onGround = grounded; car.stepHeight = 1.2F; car.yOffset = 0.35F; car.posY = 0.35;
      car.motionY = vertical; car.motionZ = 0.2;
      return car;
   }

   @Test public void existingBodyCollisionStallsAtReachableStepWithoutBodyGroundGate() throws Exception {
      for(double vertical : new double[]{0, 0.02}) {
         CollisionCar unsupportedBody = collisionCar(false, vertical);
         unsupportedBody.moveEntity(0, vertical, 0.2);
         assertEquals(0.05, unsupportedBody.posZ, 0.00001);
         assertEquals(0, unsupportedBody.motionZ, 0);
         assertTrue(unsupportedBody.isCollidedHorizontally);
         CollisionCar groundedBody = collisionCar(true, vertical);
         groundedBody.moveEntity(0, vertical, 0.2);
         assertEquals(0.2, groundedBody.posZ, 0.00001);
         assertEquals(1.0, groundedBody.boundingBox.minY, 0.00001);
         assertFalse(groundedBody.isCollidedHorizontally);
      }
      // Downward motion clipped by the floor satisfies the other branch of the gate.
      CollisionCar descendingBody = collisionCar(false, -0.02);
      descendingBody.moveEntity(0, -0.02, 0.2);
      assertEquals(0.2, descendingBody.posZ, 0.00001);
      assertFalse(descendingBody.isCollidedHorizontally);
   }
}
