package mcheli.tank;

import java.util.ArrayList;
import java.util.List;
import mcheli.aircraft.MCH_BoundingBox;
import net.minecraft.util.AxisAlignedBB;

/**
 * Tank-only, side-effect-free physical-hull step planner.  The planner works on
 * disposable hull copies and block collision shapes; callers apply a returned
 * route only after all four segments have been validated.
 */
public final class MCH_TankStepSolver {
   public static final double COLLISION_EPSILON = 1.0E-4D;
   public static final double PRE_CONTACT_GAP = 0.01D;
   private static final int SEARCH_ITERATIONS = 14;
   private static final double SWEEP_INTERVAL = 0.025D;

   public static final class Transform {
      public final double x, y, z;
      public final float yaw, pitch, roll;
      public Transform(double x, double y, double z, float yaw, float pitch, float roll) {
         this.x=x; this.y=y; this.z=z; this.yaw=yaw; this.pitch=pitch; this.roll=roll;
      }
      Transform offset(double dx,double dy,double dz) {
         return new Transform(x+dx,y+dy,z+dz,yaw,pitch,roll);
      }
   }

   public static final class Contact {
      public final int hullIndex;
      public final MCH_BoundingBox sourceHull;
      public final AxisAlignedBB block;
      public final double normalX, normalY, normalZ, shapeTop;
      public final double safeFraction, blockedFraction;
      Contact(int hullIndex,MCH_BoundingBox hull,AxisAlignedBB block,double nx,double ny,double nz,
              double safe,double blocked) {
         this.hullIndex=hullIndex; this.sourceHull=hull; this.block=block;
         this.normalX=nx; this.normalY=ny; this.normalZ=nz; this.shapeTop=block.maxY;
         this.safeFraction=safe; this.blockedFraction=blocked;
      }
   }

   public static final class Route {
      public final boolean stepped;
      public final Transform preContact, raised, raisedEnd, end;
      public final double safeFraction, blockedFraction, preContactGap, requiredRise, lift;
      public final List contacts;
      Route(boolean stepped,Transform pre,Transform raised,Transform raisedEnd,Transform end,
            double safe,double blocked,double gap,double rise,double lift,List contacts) {
         this.stepped=stepped; this.preContact=pre; this.raised=raised; this.raisedEnd=raisedEnd; this.end=end;
         this.safeFraction=safe; this.blockedFraction=blocked; this.preContactGap=gap;
         this.requiredRise=rise; this.lift=lift; this.contacts=contacts;
      }
   }

   public static final class Result {
      public final Route normalRoute, stepRoute, selectedRoute;
      public final String failureSegment;
      public final AxisAlignedBB failureBlock;
      public final double failureNormalX, failureNormalY, failureNormalZ;
      Result(Route normal,Route step,Route selected,String segment,AxisAlignedBB block,double nx,double ny,double nz) {
         this.normalRoute=normal; this.stepRoute=step; this.selectedRoute=selected;
         this.failureSegment=segment; this.failureBlock=block;
         this.failureNormalX=nx; this.failureNormalY=ny; this.failureNormalZ=nz;
      }
   }

   private MCH_TankStepSolver() {}

   public static Result solve(Transform start,List hullDefinitions,List collisionShapes,
                              double moveX,double moveZ,double supportPlane,double stepHeight,
                              boolean supported) {
      ArrayList hulls=copyPhysicalHulls(hullDefinitions);
      if(hulls.isEmpty() || moveX*moveX+moveZ*moveZ <= COLLISION_EPSILON*COLLISION_EPSILON) {
         Route normal=normal(start,hulls,collisionShapes,moveX,moveZ,1.0D,1.0D);
         return new Result(normal,null,normal,"classification",null,0,0,0);
      }
      Sweep first=sweep(start,hulls,collisionShapes,moveX,0.0D,moveZ);
      Route normal=normal(start,hulls,collisionShapes,moveX,moveZ,first.safe,first.blocked);
      if(!first.blockedMovement || !supported || stepHeight <= 0.0D) return new Result(normal,null,normal,"classification",first.block,first.nx,first.ny,first.nz);

      ArrayList contacts=contactsAt(start,hulls,collisionShapes,moveX,moveZ,first);
      double requiredRise=0.0D;
      for(int i=0;i<contacts.size();++i) {
         Contact c=(Contact)contacts.get(i);
         double opposing=moveX*c.normalX+moveZ*c.normalZ;
         double rise=c.shapeTop-supportPlane;
         if(opposing >= -COLLISION_EPSILON || rise <= COLLISION_EPSILON || rise > stepHeight+COLLISION_EPSILON)
            return new Result(normal,null,normal,"classification",c.block,c.normalX,c.normalY,c.normalZ);
         requiredRise=Math.max(requiredRise,rise);
      }
      if(contacts.isEmpty()) return new Result(normal,null,normal,"classification",first.block,first.nx,first.ny,first.nz);

      // Start with a real world-space gap, then retreat farther if the complete vertical hull sweep needs it.
      double distance=Math.sqrt(moveX*moveX+moveZ*moveZ);
      double baseGapFraction=Math.min(first.safe,PRE_CONTACT_GAP/distance);
      Transform pre=null,raised=null,raisedEnd=null,end=null;
      double chosenSafe=first.safe, gap=0.0D, lift=Math.min(stepHeight+COLLISION_EPSILON,requiredRise+COLLISION_EPSILON);
      Failure failure=new Failure("lift",first.block,first.nx,first.ny,first.nz);
      for(int retreat=0;retreat<=SEARCH_ITERATIONS;++retreat) {
         double retreatFraction=baseGapFraction+(first.safe-baseGapFraction)*retreat/(double)SEARCH_ITERATIONS;
         chosenSafe=Math.max(0.0D,first.safe-retreatFraction);
         gap=(first.safe-chosenSafe)*distance;
         pre=start.offset(moveX*chosenSafe,0.0D,moveZ*chosenSafe);
         raised=pre.offset(0.0D,lift,0.0D);
         if(!segmentSafe(pre,raised,hulls,collisionShapes,failure,"lift")) continue;
         double remain=1.0D-chosenSafe;
         raisedEnd=raised.offset(moveX*remain,0.0D,moveZ*remain);
         if(!segmentSafe(raised,raisedEnd,hulls,collisionShapes,failure,"raised-horizontal")) continue;
         end=findHighestSupport(raisedEnd,hulls,collisionShapes,lift-requiredRise,
                 supportPlane+requiredRise,failure);
         if(end==null || !segmentSafe(raisedEnd,end,hulls,collisionShapes,failure,"lower")) continue;
         Route step=new Route(true,pre,raised,raisedEnd,end,chosenSafe,first.blocked,gap,requiredRise,lift,contacts);
         Route selected=normal==null || horizontalProgress(start,step.end)>horizontalProgress(start,normal.end)+COLLISION_EPSILON?step:normal;
         return new Result(normal,step,selected,null,null,0,0,0);
      }
      return new Result(normal,null,normal,failure.segment,failure.block,failure.nx,failure.ny,failure.nz);
   }

   private static Route normal(Transform start,List hulls,List blocks,double dx,double dz,double safe,double blocked) {
      Transform end=start.offset(dx*safe,0.0D,dz*safe);
      return new Route(false,end,end,end,end,safe,blocked,0,0,0,new ArrayList());
   }

   private static double horizontalProgress(Transform start,Transform end) {
      double dx=end.x-start.x,dz=end.z-start.z; return Math.sqrt(dx*dx+dz*dz);
   }

   private static ArrayList copyPhysicalHulls(List definitions) {
      ArrayList out=new ArrayList();
      for(int i=0;i<definitions.size();++i) out.add(((MCH_BoundingBox)definitions.get(i)).copy());
      return out;
   }

   private static final class Sweep { double safe=1.0D,blocked=1.0D; boolean blockedMovement; AxisAlignedBB block; int hull=-1; double nx,ny,nz; }
   private static Sweep sweep(Transform start,List hulls,List blocks,double dx,double dy,double dz) {
      Sweep result=new Sweep();
      double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
      int count=Math.max(1,(int)Math.ceil(length/SWEEP_INTERVAL));
      double previous=0.0D;
      for(int i=1;i<=count;++i) {
         double fraction=i/(double)count;
         Hit hit=firstHit(start.offset(dx*fraction,dy*fraction,dz*fraction),hulls,blocks);
         if(hit!=null) {
            double safe=previous,blocked=fraction;
            for(int n=0;n<SEARCH_ITERATIONS;++n) {
               double middle=(safe+blocked)*0.5D;
               if(firstHit(start.offset(dx*middle,dy*middle,dz*middle),hulls,blocks)==null) safe=middle; else blocked=middle;
            }
            Hit blockedHit=firstHit(start.offset(dx*blocked,dy*blocked,dz*blocked),hulls,blocks);
            result.safe=safe; result.blocked=blocked; result.blockedMovement=true;
            result.block=blockedHit.block; result.hull=blockedHit.hull;
            double horizontal=Math.sqrt(dx*dx+dz*dz);
            result.nx=horizontal>COLLISION_EPSILON?-dx/horizontal:0.0D;
            result.nz=horizontal>COLLISION_EPSILON?-dz/horizontal:0.0D;
            result.ny=horizontal<=COLLISION_EPSILON?(dy>0?-1:1):0.0D;
            return result;
         }
         previous=fraction;
      }
      return result;
   }

   private static ArrayList contactsAt(Transform start,List hulls,List blocks,double dx,double dz,Sweep sweep) {
      ArrayList result=new ArrayList();
      Transform blocked=start.offset(dx*sweep.blocked,0,dz*sweep.blocked);
      for(int h=0;h<hulls.size();++h) {
         MCH_BoundingBox hull=(MCH_BoundingBox)hulls.get(h); update(hull,blocked);
         for(int b=0;b<blocks.size();++b) {
            AxisAlignedBB block=(AxisAlignedBB)blocks.get(b);
            if(hull.intersectsWith(block)) result.add(new Contact(h,(MCH_BoundingBox)hull.copy(),block,
                    sweep.nx,sweep.ny,sweep.nz,sweep.safe,sweep.blocked));
         }
      }
      return result;
   }

   private static boolean segmentSafe(Transform a,Transform b,List hulls,List blocks,Failure failure,String name) {
      Sweep sweep=sweep(a,hulls,blocks,b.x-a.x,b.y-a.y,b.z-a.z);
      if(!sweep.blockedMovement) return true;
      failure.segment=name; failure.block=sweep.block; failure.nx=sweep.nx; failure.ny=sweep.ny; failure.nz=sweep.nz;
      return false;
   }

   private static Transform findHighestSupport(Transform raised,List hulls,List blocks,double drop,
                                                double expectedTop,Failure failure) {
      boolean supported=false;
      for(int i=0;i<blocks.size();++i) {
         AxisAlignedBB block=(AxisAlignedBB)blocks.get(i);
         for(int h=0;h<hulls.size();++h) {
            MCH_BoundingBox hull=(MCH_BoundingBox)hulls.get(h); update(hull,raised);
            if(horizontalOverlap(hull.boundingBox,block)&&Math.abs(block.maxY-expectedTop)<=COLLISION_EPSILON)
               supported=true;
         }
      }
      if(!supported) { failure.segment="support"; return null; }
      return raised.offset(0.0D,-Math.max(0.0D,drop),0.0D);
   }

   private static boolean horizontalOverlap(AxisAlignedBB a,AxisAlignedBB b) {
      return a.maxX>b.minX+COLLISION_EPSILON&&a.minX<b.maxX-COLLISION_EPSILON
              &&a.maxZ>b.minZ+COLLISION_EPSILON&&a.minZ<b.maxZ-COLLISION_EPSILON;
   }

   private static final class Hit { int hull; AxisAlignedBB block; Hit(int h,AxisAlignedBB b){hull=h;block=b;} }
   private static Hit firstHit(Transform t,List hulls,List blocks) {
      for(int h=0;h<hulls.size();++h) {
         MCH_BoundingBox hull=(MCH_BoundingBox)hulls.get(h); update(hull,t);
         for(int b=0;b<blocks.size();++b) if(hull.intersectsWith((AxisAlignedBB)blocks.get(b))) return new Hit(h,(AxisAlignedBB)blocks.get(b));
      }
      return null;
   }

   private static void update(MCH_BoundingBox hull,Transform t) { hull.updatePosition(t.x,t.y,t.z,t.yaw,t.pitch,t.roll); }
   private static final class Failure { String segment; AxisAlignedBB block; double nx,ny,nz; Failure(String s,AxisAlignedBB b,double x,double y,double z){segment=s;block=b;nx=x;ny=y;nz=z;} }
}
