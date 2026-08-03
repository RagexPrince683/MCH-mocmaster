package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.aircraft.MCH_BoundingBox;
import net.minecraft.util.AxisAlignedBB;
import org.junit.Test;
import static org.junit.Assert.*;

/** End-to-end route tests: real hull definitions and world collision AABBs enter the planner. */
public class MCH_TankStepSolverTest {
   private static final double M1_STEP_HEIGHT=1.8D;

   @Test public void m1a2ClimbsFullBlockHeadOnThroughFourSafeSegments() {
      MCH_TankStepSolver.Result r=solve(m1Hulls(),blocks(block(-1,0,5,1,1,6)),0,1,0,M1_STEP_HEIGHT,true);
      assertStep(r);
      assertEquals(1.0D,r.stepRoute.requiredRise,1.0E-8D);
      assertEquals(1.0001D,r.stepRoute.lift,1.0E-8D);
      assertTrue(r.stepRoute.safeFraction>0.48D&&r.stepRoute.safeFraction<0.51D);
      assertTrue(r.stepRoute.preContactGap>=MCH_TankStepSolver.PRE_CONTACT_GAP-1.0E-5D);
      assertTrue(r.stepRoute.preContact.z<0.5D);
      assertEquals(1.0D,r.stepRoute.end.z,1.0E-8D);
      assertEquals(1.0D,r.stepRoute.end.y,1.0E-8D);
   }

   @Test public void diagonalClimbPreservesSteeringAndDoesNotSnapBack() {
      MCH_TankStepSolver.Result r=solve(m1Hulls(),blocks(block(-1,0,5,2,1,6)),0.35,1,27,M1_STEP_HEIGHT,true);
      assertStep(r); assertEquals(27.0F,r.stepRoute.end.yaw,0.0F);
      MCH_TankStepSolver.Result next=MCH_TankStepSolver.solve(r.stepRoute.end,m1Hulls(),
              blocks(block(-1,0,5,2,1,6)),0.05,0.05,1.0D,M1_STEP_HEIGHT,true);
      assertNotNull(next.selectedRoute); assertTrue(next.selectedRoute.end.z>=r.stepRoute.end.z);
   }

   @Test public void longFrontHullContactsBeforeRootAndStopsOutsideRiser() {
      MCH_TankStepSolver.Result r=solve(m1Hulls(),blocks(block(-1,0,5,1,1,6)),0,1,0,M1_STEP_HEIGHT,true);
      assertStep(r); assertTrue(r.stepRoute.safeFraction<0.51D);
      MCH_BoundingBox front=(MCH_BoundingBox)m1Hulls().get(1);
      front.updatePosition(r.stepRoute.preContact.x,r.stepRoute.preContact.y,r.stepRoute.preContact.z,0,0,0);
      assertFalse(front.intersectsWith(block(-1,0,5,1,1,6)));
   }

   @Test public void ordinaryHorizontalRouteCannotPassThroughBlock() {
      MCH_TankStepSolver.Result r=solve(m1Hulls(),blocks(block(-1,0,5,1,1,6)),0,1,0,0,false);
      assertFalse(r.selectedRoute.stepped); assertTrue(r.selectedRoute.safeFraction<1.0D);
   }

   @Test public void shorterComparisonTankClimbsButInsufficientStepHeightDoesNot() {
      List shortHull=hulls(new MCH_BoundingBox(0,.6,1.0F,2.0F,.4F,2.0F,1));
      assertStep(solve(shortHull,blocks(block(-1,0,2.2,1,1,3.2)),0,1.5,0,1.2,true));
      MCH_TankStepSolver.Result low=solve(shortHull,blocks(block(-1,0,2.2,1,1,3.2)),0,1.5,0,.9,true);
      assertFalse(low.selectedRoute.stepped);
   }

   @Test public void tallWallAndWallBehindStepRemainBlocking() {
      assertBlocked(solve(m1Hulls(),blocks(block(-1,0,5,1,2,6)),0,1,0,M1_STEP_HEIGHT,true));
      assertBlocked(solve(m1Hulls(),blocks(block(-1,0,5,1,1,6),block(-1,0,6,1,2,7)),0,2,0,M1_STEP_HEIGHT,true));
   }

   @Test public void adjacentPillarCeilingAndOverhangRejectRoutes() {
      assertBlocked(solve(m1Hulls(),blocks(block(-1,0,5,1,1,6),block(1,0,5,2,3,6)),.4,1,0,M1_STEP_HEIGHT,true));
      assertBlocked(solve(m1Hulls(),blocks(block(-1,0,5,1,1,6),block(-3,1.7,3,3,2,5)),0,1,0,M1_STEP_HEIGHT,true));
      assertBlocked(solve(m1Hulls(),blocks(block(-1,0,5,1,1,6),block(-3,1.7,5,3,2,7)),0,1,0,M1_STEP_HEIGHT,true));
   }

   @Test public void parallelAwayAndUnsupportedMovementNeverStartsStep() {
      AxisAlignedBB wall=block(-1,0,5,1,1,6);
      assertFalse(solve(m1Hulls(),blocks(wall),1,0,0,M1_STEP_HEIGHT,true).selectedRoute.stepped);
      assertFalse(solve(m1Hulls(),blocks(wall),0,-1,0,M1_STEP_HEIGHT,true).selectedRoute.stepped);
      assertFalse(solve(m1Hulls(),blocks(wall),0,1,0,M1_STEP_HEIGHT,false).selectedRoute.stepped);
   }

   @Test public void slabAndStairUseActualShapeTops() {
      MCH_TankStepSolver.Result slab=solve(m1Hulls(),blocks(block(-1,0,5,1,.5,6)),0,1,0,M1_STEP_HEIGHT,true);
      assertStep(slab); assertEquals(.5,slab.stepRoute.requiredRise,1.0E-8);
      MCH_TankStepSolver.Result stairs=solve(m1Hulls(),blocks(block(-1,0,5,1,.5,5.5),block(-1,0,5.5,1,1,6)),0,.5,0,M1_STEP_HEIGHT,true);
      assertStep(stairs); assertEquals(.5,stairs.stepRoute.requiredRise,1.0E-8);
   }

   @Test public void occupiedAndUnoccupiedUseIdenticalNoPhasingResult() {
      List wall=blocks(block(-1,0,5,1,2,6));
      MCH_TankStepSolver.Result occupied=solve(m1Hulls(),wall,0,1,0,M1_STEP_HEIGHT,true);
      MCH_TankStepSolver.Result empty=solve(m1Hulls(),wall,0,1,0,M1_STEP_HEIGHT,true);
      assertBlocked(occupied); assertBlocked(empty);
      assertEquals(occupied.selectedRoute.end.z,empty.selectedRoute.end.z,0.0D);
   }

   @Test public void failedStepLeavesStartSafeAndRotationIntoWallIsNotAstep() {
      List wall=blocks(block(-1,0,5,1,2,6));
      MCH_TankStepSolver.Result r=solve(m1Hulls(),wall,0,1,0,M1_STEP_HEIGHT,true);
      assertFalse(r.selectedRoute.stepped); assertTrue(r.selectedRoute.end.z<.51D);
      MCH_TankStepSolver.Result rotation=solve(m1Hulls(),wall,0,0,45,M1_STEP_HEIGHT,true);
      assertFalse(rotation.selectedRoute.stepped);
   }

   @Test public void continuousFloorContactsDoNotRejectAbramsFullBlockClimb() {
      AxisAlignedBB floor=block(-8,-1, -8, 8,.401,8);
      AxisAlignedBB obstacle=block(-1,.4,5,1,1.4,6);
      MCH_TankStepSolver.Result r=MCH_TankStepSolver.solve(
              new MCH_TankStepSolver.Transform(0,0,0,0,0,0),m1Hulls(),blocks(floor,obstacle),
              0,1,.4,M1_STEP_HEIGHT,true);
      assertStep(r);
      assertEquals(1.0D,r.stepRoute.requiredRise,1.0E-8D);
      assertTrue(r.stepRoute.lift>=r.stepRoute.requiredRise);
      assertEquals(0.0F,r.stepRoute.end.yaw,0.0F);
   }

   @Test public void baselineSideContactMaySeparateOrSlideButCannotDeepen() {
      List hull=hulls(new MCH_BoundingBox(0,.5,0,1,1,1,1));
      List wall=blocks(block(.49,0,-2,1.49,1,2));
      MCH_TankStepSolver.Result away=solve(hull,wall,-.2,0,0,.6,true);
      MCH_TankStepSolver.Result parallel=solve(hull,wall,0,.2,0,.6,true);
      MCH_TankStepSolver.Result deeper=solve(hull,wall,.2,0,0,.6,true);
      assertEquals(-.2,away.selectedRoute.end.x,1.0E-6D);
      assertEquals(.2,parallel.selectedRoute.end.z,1.0E-6D);
      assertTrue(deeper.selectedRoute.safeFraction<1.0D);
      assertFalse(deeper.selectedRoute.stepped);
   }

   @Test public void tankStepHeightDefaultsAndExplicitConfiguration() {
      MCH_TankInfo defaults=new MCH_TankInfo("default-step-test");
      assertEquals(.6F,defaults.stepHeight,0.0F);
      defaults.loadItemData("StepHeight","1.25");
      assertEquals(1.25F,defaults.stepHeight,0.0F);
   }

   private static void assertStep(MCH_TankStepSolver.Result r){assertNotNull(r.stepRoute);assertTrue(r.selectedRoute.stepped);}
   private static void assertBlocked(MCH_TankStepSolver.Result r){assertNotNull(r.selectedRoute);assertFalse(r.selectedRoute.stepped);assertTrue(r.selectedRoute.safeFraction<1);}
   private static MCH_TankStepSolver.Result solve(List hulls,List blocks,double x,double z,float yaw,double step,boolean support){
      return MCH_TankStepSolver.solve(new MCH_TankStepSolver.Transform(0,0,0,yaw,0,0),hulls,blocks,x,z,0,step,support);
   }
   private static List m1Hulls(){ return hulls(
           new MCH_BoundingBox(0,.5,2.5,3,.2F,3,.02F),
           new MCH_BoundingBox(0,.9,3,3,.5F,3,.03F),
           new MCH_BoundingBox(0,1.1,2.5,3,.5F,3,.08F),
           new MCH_BoundingBox(0,1.2,1.8,3,.5F,3,.11F),
           new MCH_BoundingBox(0,1.3,1.4,3,.5F,3,.09F),
           new MCH_BoundingBox(0,2,.1,3,1,3,.02F),
           new MCH_BoundingBox(0,.9,0,3.01F,1.3F,3.01F,.41F),
           new MCH_BoundingBox(0,.9,-3,3,1.3F,3,.9F),
           new MCH_BoundingBox(0,.9,-3.8,3,1.3F,3,.9F),
           new MCH_BoundingBox(0,1.1,-4.1,2.99F,1.1F,2.99F,3)); }
   private static List hulls(MCH_BoundingBox... h){ArrayList l=new ArrayList();for(int i=0;i<h.length;i++)l.add(h[i]);return l;}
   private static List blocks(AxisAlignedBB... b){ArrayList l=new ArrayList();for(int i=0;i<b.length;i++)l.add(b[i]);return l;}
   private static AxisAlignedBB block(double a,double b,double c,double d,double e,double f){return AxisAlignedBB.getBoundingBox(a,b,c,d,e,f);}
}
